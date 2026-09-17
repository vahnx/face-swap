package com.faceswap;

import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Actor;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Hitsplat;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.JagexColor;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.kit.KitType;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Face Swap",
	description = "Swap face appearances for selected players",
	tags = {"face", "swap", "appearance", "cosmetic"}
)
public class FaceSwapPlugin extends Plugin
{
	private static final String CONFIG_GROUP = "faceswap";
	private static final String TRIANGLE_OVERRIDES_KEY = "prototype3dTriangleOverrides";
	static final String SIDEPANEL_ICON_RESOURCE = "/face_swap_icon.png";
	private static final int NO_HELMET_GLOBAL_Y_SHIFT = 7;
	private static final Set<String> HELMET_PROFILE_CONFIG_KEYS = Set.of(
		"prototype3dY",
		"prototype3dScale",
		"prototype3dX",
		"prototype3dZ",
		"prototype3dPitch",
		"prototype3dYaw",
		"prototype3dRoll",
		"prototype3dWidth",
		"prototype3dFaceHeight",
		"prototype3dDepth",
		"prototypeAnimationFrameOffset"
	);
	private static final Set<String> FACE_COVERING_ITEM_NAME_PARTS = Set.of(
		"full helm",
		"full helmet",
		"slayer helmet",
		"serpentine helm",
		"tanzanite helm",
		"magma helm",
		"faceguard",
		"guthan's helm",
		"torag's helm",
		"verac's helm"
	);
	private static final Set<String> FACE_EXPOSING_ITEM_NAME_PARTS = Set.of(
		"eclipse moon helm",
		"zombie helmet",
		"dharok's helm",
		"blood moon helm",
		"crystal helm",
		"blue moon helm",
		"graceful hood",
		"frog mask"
	);
	private static final int RIGGED_HEAD_MODEL_ID = 47666;
	private static final int RIGGED_JAW_MODEL_ID = 249;
	private static final int DENSE_DONOR_MODEL_ID = 50805;
	private static final int DENSE_DONOR_VERTEX_COUNT = 437;
	private static final int DENSE_ANCHOR_COPIES = 14;
	private static final int[] DENSE_DONOR_BASE_GROUP_2 = {118, 119, 122, 124};
	private static final int[] DENSE_DONOR_BASE_GROUP_3 = {22, 30, 120, 121, 123, 199, 208, 209, 291};
	private static final int[] MALE_ANCHOR_MODEL_IDS = {28515, 26632, 176, 28285, 181};
	private static final String EDITED_HEAD_VERTICES = "/models/dense_rigged_player_head_vertices.csv";
	private static final int MED_HELM_FACE_DROP = 10;
	private static final Set<String> HAT_LIKE_ITEM_NAME_PARTS = Set.of(
		"hat",
		"helmet",
		"helm",
		"hood",
		"cap",
		"crown",
		"tiara",
		"halo",
		"bandana",
		"headband",
		"circlet",
		"wig",
		"beret",
		"headdress",
		"mitre",
		"coif");
	private static final int MOVEMENT_STYLE_INTERVAL_CLIENT_TICKS = 40;
	private static final int EXPRESSION_REACTION_HOLD_CLIENT_TICKS = 30;
	private static final List<String> MOVEMENT_STYLE_IDS = Collections.unmodifiableList(Arrays.asList(
		FaceSwapAssignment.DEFAULT_STYLE_ID,
		"blushing",
		"in_love"));
	private static final Set<Integer> FACE_COVERING_HEAD_ITEMS = Set.of(
		ItemID.IRON_FULL_HELM,
		ItemID.BRONZE_FULL_HELM,
		ItemID.STEEL_FULL_HELM,
		ItemID.MITHRIL_FULL_HELM,
		ItemID.ADAMANT_FULL_HELM,
		ItemID.RUNE_FULL_HELM,
		ItemID.BLACK_FULL_HELM,
		ItemID.BLACK_FULL_HELM_T,
		ItemID.BLACK_FULL_HELM_G,
		ItemID.ADAMANT_FULL_HELM_T,
		ItemID.ADAMANT_FULL_HELM_G,
		ItemID.RUNE_FULL_HELM_G,
		ItemID.RUNE_FULL_HELM_T,
		ItemID.ZAMORAK_FULL_HELM,
		ItemID.SARADOMIN_FULL_HELM,
		ItemID.GUTHIX_FULL_HELM,
		ItemID.GILDED_FULL_HELM,
		ItemID.WHITE_FULL_HELM,
		ItemID._3RD_AGE_FULL_HELMET,
		ItemID.DRAGON_FULL_HELM,
		ItemID.DRAGON_FULL_HELM_G,
		ItemID.ANCIENT_FULL_HELM,
		ItemID.ARMADYL_FULL_HELM,
		ItemID.BANDOS_FULL_HELM,
		ItemID.SERPENTINE_HELM_UNCHARGED,
		ItemID.SERPENTINE_HELM,
		ItemID.JUSTICIAR_FACEGUARD,
		ItemID.STATIUSS_FULL_HELM,
		ItemID.DRAGONSTONE_FULL_HELM,
		ItemID.NEITIZNOT_FACEGUARD,
		ItemID.DECORATIVE_FULL_HELM_BROKEN,
		ItemID.DECORATIVE_FULL_HELM,
		ItemID.DECORATIVE_FULL_HELM_25169,
		ItemID.DECORATIVE_FULL_HELM_25174,
		ItemID.DECORATIVE_FULL_HELM_L,
		ItemID.PINK_STAINED_FULL_HELM,
		ItemID.CLEAN_FULL_HELM,
		ItemID.TORVA_FULL_HELM_DAMAGED,
		ItemID.TORVA_FULL_HELM,
		ItemID.STATIUSS_FULL_HELM_BH,
		ItemID.CORRUPTED_STATIUSS_FULL_HELM_BH,
		ItemID.STATIUSS_FULL_HELM_BHINACTIVE,
		ItemID.CORRUPTED_STATIUSS_FULL_HELM_BHINACTIVE,
		ItemID.SANGUINE_TORVA_FULL_HELM,
		ItemID.ELITE_BLACK_FULL_HELM,
		ItemID.TORVA_FULL_HELM_30302,
		ItemID.SLAYER_HELMET,
		ItemID.SLAYER_HELMET_I,
		ItemID.BLACK_SLAYER_HELMET,
		ItemID.BLACK_SLAYER_HELMET_I,
		ItemID.GREEN_SLAYER_HELMET,
		ItemID.GREEN_SLAYER_HELMET_I,
		ItemID.RED_SLAYER_HELMET,
		ItemID.RED_SLAYER_HELMET_I
	);
	private static final Set<Integer> MED_HELM_ITEMS = Set.of(
		ItemID.IRON_MED_HELM,
		ItemID.BRONZE_MED_HELM,
		ItemID.STEEL_MED_HELM,
		ItemID.MITHRIL_MED_HELM,
		ItemID.ADAMANT_MED_HELM,
		ItemID.RUNE_MED_HELM,
		ItemID.DRAGON_MED_HELM,
		ItemID.BLACK_MED_HELM,
		ItemID.WHITE_MED_HELM,
		ItemID.ADAMANT_MED_HELM_6895,
		ItemID.DRAGON_MED_HELM_6967,
		ItemID.GILDED_MED_HELM,
		ItemID.MAOMAS_MED_HELM_BROKEN,
		ItemID.MAOMAS_MED_HELM,
		ItemID.MAOMAS_MED_HELM_L,
		ItemID.DRAGON_MED_HELM_CR
	);

	@Inject
	private ConfigManager configManager;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MouseManager mouseManager;

	private FaceSwapConfig config;

	@Inject
	private FaceSwapOverlay overlay;

	@Inject
	private FaceSwapCalibrationOverlay calibrationOverlay;

	@Inject
	@Named("developerMode")
	private boolean developerMode;

	private FaceSwapPanel panel;
	private NavigationButton navigationButton;
	private FaceSwapHotkeyListener hotkeyListener;
	private boolean pickPlayerMode;
	private Actor hoveredPickActor;
	private final Map<Actor, Prototype3dInstance> prototype3dInstances = new IdentityHashMap<>();
	private final Map<String, Model> prototype3dModelCache = new HashMap<>();
	private final Map<Actor, MaskRigInstance> maskRigInstances = new IdentityHashMap<>();
	private RuneLiteObject prototype3dObject;
	private Model maskRigBaseModel;
	private int prototype3dEquippedHeadItemId = Integer.MIN_VALUE;
	private int prototype3dLiveCalibrationItemId = Integer.MIN_VALUE;
	private int interactiveHelmetCalibrationItemId = Integer.MIN_VALUE;
	private HelmetCalibration interactiveHelmetCalibration;
	private int prototype3dDebugHeadItemId = Integer.MIN_VALUE;
	private String prototype3dDebugHeadItemText;
	private String lastPanelStatus;
	private String lastPanelTargetNames;
	private final AtomicBoolean panelRefreshQueued = new AtomicBoolean();
	private FaceSwapHead movementCycleHead;
	private String movementCycleStyleId = FaceSwapAssignment.DEFAULT_STYLE_ID;
	private int movementCycleTicks;
	private int lastMovementX = Integer.MIN_VALUE;
	private int lastMovementY = Integer.MIN_VALUE;
	private boolean playerWasMoving;
	private final Map<Player, MovementCycleState> otherMovementCycles = new IdentityHashMap<>();
	private final Set<NPC> movementTrackedNpcs = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Map<NPC, MovementCycleState> npcMovementCycles = new IdentityHashMap<>();
	private boolean movementTrackedNpcsInitialized;
	private String localReactionStyleId;
	private int localReactionTicks;
	private boolean nextDamageTakenCrying = true;
	private boolean nextDamageDealtAngry = true;
	private ExecutorService customImageExecutor;
	private FaceSwapCalibrationMouseListener calibrationMouseListener;

	private static ExecutorService createCustomImageExecutor()
	{
		return Executors.newSingleThreadExecutor(runnable ->
		{
			Thread thread = new Thread(runnable, "face-swap-custom-images");
			thread.setDaemon(true);
			return thread;
		});
	}
	private final FaceSwapCustomImageStore customImageStore = new FaceSwapCustomImageStore();
	private volatile String radiusPlayerNames = "";
	private volatile Map<String, FaceSwapAssignment> playerHeadAssignments;
	private volatile Map<String, FaceSwapAssignment> npcHeadAssignments;

	@Inject
	void initializeInternalConfig(ConfigManager configManager)
	{
		config = configManager.getConfig(FaceSwapConfig.class);
	}

	@Provides
	Config provideConfig(ConfigManager configManager, @Named("developerMode") boolean developerMode)
	{
		return developerMode
			? configManager.getConfig(FaceSwapConfig.class)
			: configManager.getConfig(FaceSwapReleaseConfig.class);
	}

	@Override
	protected void startUp()
	{
		customImageExecutor = createCustomImageExecutor();
		customImageExecutor.submit(() ->
		{
			customImageStore.load();
			ensureTargetCustomImages(
				FaceSwapHead.CUSTOM, getConfiguredTargetNames(FaceSwapHead.CUSTOM), false);
			ensureTargetCustomImages(
				FaceSwapHead.CUSTOM, getConfiguredNpcTargetNames(FaceSwapHead.CUSTOM), true);
			try
			{
				HelmetProfiles.loadRuntimeProfiles();
			}
			catch (IOException | RuntimeException ex)
			{
				log.debug("Unable to load runtime helmet profiles; using packaged defaults", ex);
			}
			try
			{
				HelmetMeshCalibrations.loadRuntime();
			}
			catch (IOException | RuntimeException ex)
			{
				log.debug("Unable to load runtime helmet mesh calibrations; using defaults", ex);
			}
			clientThread.invoke(() ->
			{
				SwingUtilities.invokeLater(() ->
				{
					FaceSwapPanel currentPanel = panel;
					if (currentPanel != null)
					{
						currentPanel.refreshCustomHeadPickerIfOpen();
					}
				});
				if (config.interactiveHelmetCalibration())
				{
					ensureInteractiveHelmetCalibration(client.getLocalPlayer(),
						getLocalHeadItemId(client.getLocalPlayer()));
				}
				clearPrototype3dModelCache();
				removePrototype3dInstances();
				refreshPanel();
			});
		});
		migrateHeadNames();
		migrateHelmetTextureLift();
		migrateRenderMode();
		migratePrototypeTriangleCount();
		migrateTargetSettings();
		panel = new FaceSwapPanel(this, createPanelState());
		navigationButton = NavigationButton.builder()
			.tooltip("Face Swap")
			.icon(createIcon())
			.priority(8)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(overlay);
		overlayManager.add(calibrationOverlay);
		hotkeyListener = new FaceSwapHotkeyListener(this);
		keyManager.registerKeyListener(hotkeyListener);
		calibrationMouseListener = new FaceSwapCalibrationMouseListener(this, calibrationOverlay);
		mouseManager.registerMouseListener(calibrationMouseListener);
		mouseManager.registerMouseWheelListener(calibrationMouseListener);
		log.debug("Face Swap started");
	}

	@Override
	protected void shutDown()
	{
		if (customImageExecutor != null)
		{
			customImageExecutor.shutdownNow();
			customImageExecutor = null;
		}
		FaceSwapHeadImages.setCustomImage(null);
		pickPlayerMode = false;
		hoveredPickActor = null;
		if (hotkeyListener != null)
		{
			keyManager.unregisterKeyListener(hotkeyListener);
			hotkeyListener = null;
		}
		if (calibrationMouseListener != null)
		{
			mouseManager.unregisterMouseListener(calibrationMouseListener);
			mouseManager.unregisterMouseWheelListener(calibrationMouseListener);
			calibrationMouseListener = null;
		}
		interactiveHelmetCalibration = null;
		interactiveHelmetCalibrationItemId = Integer.MIN_VALUE;
		removePrototype3dInstances();
		removeMaskTrackingRigs();
		if (clientToolbar != null && navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}
		if (overlayManager != null && overlay != null)
		{
			overlayManager.remove(overlay);
		}
		if (overlayManager != null && calibrationOverlay != null)
		{
			overlayManager.remove(calibrationOverlay);
		}
		if (panel != null)
		{
			panel.disposePanel();
		}

		panel = null;
		navigationButton = null;
		log.debug("Face Swap stopped");
	}

	@Override
	public void resetConfiguration()
	{
		for (String key : new ArrayList<>(configManager.getConfigurationKeys(CONFIG_GROUP)))
		{
			configManager.unsetConfiguration(CONFIG_GROUP, key);
		}

		playerHeadAssignments = null;
		npcHeadAssignments = null;
		pickPlayerMode = false;
		hoveredPickActor = null;
		movementTrackedNpcs.clear();
		movementTrackedNpcsInitialized = false;
		resetMovementStyleCycle();
		clearPrototype3dModelCache();
		removePrototype3dInstances();
		refreshPanel();
	}

	private void migrateHeadNames()
	{
		// migrateSelectedHeadName("JAMES_BOND", FaceSwapHead.AGENT);
		// migrateSelectedHeadName("DONKEY_KONG", FaceSwapHead.MONKEY);
		// migrateHeadTargetKeys("james_bond", "agent");
		// migrateHeadTargetKeys("donkey_kong", "monkey");

		String overrides = configManager.getConfiguration(CONFIG_GROUP, TRIANGLE_OVERRIDES_KEY);
		String migratedOverrides = migrateHeadQualityOverrideNames(overrides);
		if (!Objects.equals(overrides, migratedOverrides))
		{
			configManager.setConfiguration(CONFIG_GROUP, TRIANGLE_OVERRIDES_KEY, migratedOverrides);
		}
	}

	private void migrateSelectedHeadName(String legacyName, FaceSwapHead replacement)
	{
		if (legacyName.equals(configManager.getConfiguration(CONFIG_GROUP, "selectedHead")))
		{
			configManager.setConfiguration(CONFIG_GROUP, "selectedHead", replacement);
		}
	}

	private void migrateHeadTargetKeys(String legacySuffix, String replacementSuffix)
	{
		for (String prefix : new String[] {"targetNames_", "npcTargetNames_"})
		{
			String legacyKey = prefix + legacySuffix;
			String legacyNames = configManager.getConfiguration(CONFIG_GROUP, legacyKey);
			if (legacyNames == null)
			{
				continue;
			}

			String replacementKey = prefix + replacementSuffix;
			String replacementNames = configManager.getConfiguration(CONFIG_GROUP, replacementKey);
			String combined = replacementNames == null || replacementNames.isBlank()
				? legacyNames
				: replacementNames + '\n' + legacyNames;
			configManager.setConfiguration(
				CONFIG_GROUP, replacementKey, normalizeTargetNames(combined));
			configManager.unsetConfiguration(CONFIG_GROUP, legacyKey);
		}
	}

	static String migrateHeadQualityOverrideNames(String overrides)
	{
		if (overrides == null || overrides.isBlank())
		{
			return overrides;
		}

		Map<String, String> migrated = new LinkedHashMap<>();
		for (String entry : overrides.split(","))
		{
			String[] fields = entry.split("=", 2);
			if (fields.length != 2)
			{
				continue;
			}
			String headName = fields[0].trim();
			boolean legacyName = true;
			if ("JAMES_BOND".equals(headName) || "DONKEY_KONG".equals(headName))
			{
				// Removed fictional-character aliases remain stale and are ignored below.
				continue;
			}
			else
			{
				legacyName = false;
			}
			if (legacyName)
			{
				migrated.putIfAbsent(headName, fields[1].trim());
			}
			else
			{
				migrated.put(headName, fields[1].trim());
			}
		}
		return migrated.entrySet().stream()
			.map(entry -> entry.getKey() + "=" + entry.getValue())
			.collect(Collectors.joining(","));
	}

	FaceSwapHead getSelectedHead()
	{
		return getEffectiveSelectedHead();
	}

	boolean isHeadAvailable(FaceSwapHead head)
	{
		if (head == FaceSwapHead.CUSTOM)
		{
			return customImageStore.hasSelectedImage();
		}
		return head != null
			&& (head.isReleaseAvailable() || (head.isDebugAvailable() && isDebugLaunch()));
	}

	private FaceSwapHead getEffectiveSelectedHead()
	{
		FaceSwapHead selectedHead = config.selectedHead();
		return isHeadAvailable(selectedHead) ? selectedHead : FaceSwapHead.SARDACO;
	}

	FaceSwapAssignment getEffectiveSelectedAssignment()
	{
		return getLocalDynamicAssignment(getConfiguredSelectedAssignment());
	}

	String getDefaultStyleId(FaceSwapHead head)
	{
		return getConfiguredDefaultStyleId(head);
	}

	private FaceSwapAssignment getConfiguredSelectedAssignment()
	{
		FaceSwapHead selectedHead = getEffectiveSelectedHead();
		String customImageId = selectedHead == FaceSwapHead.CUSTOM ? customImageStore.getSelectedId() : null;
		return new FaceSwapAssignment(
			selectedHead, getConfiguredDefaultStyleId(selectedHead), getRenderMode(), customImageId);
	}

	BufferedImage getAssignmentImage(FaceSwapAssignment assignment, FaceSwapHeadDirection direction)
	{
		return assignment == null ? null : getAssignmentImage(
			assignment.getHead(), assignment.getStyleId(), assignment.getCustomImageId(), direction);
	}

	Color getAssignmentAverageColor(FaceSwapAssignment assignment, FaceSwapHeadDirection direction)
	{
		return assignment == null ? Color.BLACK : getAssignmentAverageColor(
			assignment.getHead(), assignment.getStyleId(), assignment.getCustomImageId(), direction);
	}

	private BufferedImage getAssignmentImage(
		FaceSwapHead head, String styleId, String customImageId, FaceSwapHeadDirection direction)
	{
		if (head == FaceSwapHead.CUSTOM)
		{
			return FaceSwapHeadImages.getCustomImage(
				customImageStore.getImage(customImageId), customImageStore.getBackImage(customImageId), direction);
		}
		return FaceSwapHeadImages.get(head, styleId, direction);
	}

	private Color getAssignmentAverageColor(
		FaceSwapHead head, String styleId, String customImageId, FaceSwapHeadDirection direction)
	{
		if (head == FaceSwapHead.CUSTOM)
		{
			return FaceSwapHeadImages.getCustomAverageColor(
				customImageStore.getImage(customImageId), customImageStore.getBackImage(customImageId), direction);
		}
		return FaceSwapHeadImages.getAverageColor(head, styleId, direction);
	}

	FaceSwapRenderMode getAssignmentRenderMode(FaceSwapAssignment assignment)
	{
		return assignment == null || assignment.getRenderMode() == null
			? getRenderMode()
			: assignment.getRenderMode();
	}

	private static FaceSwapAssignment withStyle(FaceSwapAssignment assignment, String styleId)
	{
		return new FaceSwapAssignment(
			assignment.getHead(), styleId, assignment.getRenderMode(), assignment.getCustomImageId());
	}

	private FaceSwapAssignment getLocalDynamicAssignment(FaceSwapAssignment assignment)
	{
		if (assignment == null || !shouldCycleCreatorStyles(assignment.getHead()))
		{
			return assignment;
		}

		FaceSwapHead head = assignment.getHead();
		if (client.getVarpValue(VarPlayerID.POISON) > 0
			&& FaceSwapHeadImages.isStyleAvailable(head, "sick"))
		{
			return withStyle(assignment, "sick");
		}
		if (localReactionTicks > 0
			&& FaceSwapHeadImages.isStyleAvailable(head, localReactionStyleId))
		{
			return withStyle(assignment, localReactionStyleId);
		}
		if (playerWasMoving && movementCycleHead == head
			&& FaceSwapHeadImages.isStyleAvailable(head, movementCycleStyleId))
		{
			return withStyle(assignment, movementCycleStyleId);
		}
		return assignment;
	}

	private FaceSwapAssignment getLocalDynamicAssignment(Player player, FaceSwapAssignment assignment)
	{
		if (player == client.getLocalPlayer())
		{
			return getLocalDynamicAssignment(assignment);
		}
		return getOtherMovementAssignment(player, assignment);
	}

	private FaceSwapAssignment getOtherMovementAssignment(Player player, FaceSwapAssignment assignment)
	{
		if (assignment == null || !shouldCycleCreatorStyles(assignment.getHead()))
		{
			return assignment;
		}

		MovementCycleState state = otherMovementCycles.get(player);
		if (state != null && state.moving && state.head == assignment.getHead()
			&& FaceSwapHeadImages.isStyleAvailable(assignment.getHead(), state.styleId))
		{
			return withStyle(assignment, state.styleId);
		}
		return assignment;
	}

	private FaceSwapAssignment getNpcMovementAssignment(NPC npc, FaceSwapAssignment assignment)
	{
		if (assignment == null || !shouldCycleCreatorStyles(assignment.getHead()))
		{
			return assignment;
		}

		MovementCycleState state = npcMovementCycles.get(npc);
		if (state != null && state.moving && state.head == assignment.getHead()
			&& FaceSwapHeadImages.isStyleAvailable(assignment.getHead(), state.styleId))
		{
			return withStyle(assignment, state.styleId);
		}
		return assignment;
	}

	private String getConfiguredDefaultStyleId(FaceSwapHead head)
	{
		String configuredStyle = configManager.getConfiguration(CONFIG_GROUP, defaultStyleKey(head));
		String normalizedStyle = FaceSwapAssignment.normalizeStyleId(configuredStyle);
		return FaceSwapHeadImages.isStyleAvailable(head, normalizedStyle)
			? normalizedStyle
			: FaceSwapAssignment.DEFAULT_STYLE_ID;
	}

	private boolean shouldCycleCreatorStyles(FaceSwapHead head)
	{
		return config != null
			&& cycleCreatorStylesWhileMoving()
			&& head != null
			&& FaceSwapHeadImages.getAvailableStyleIds(head).size() > 1;
	}

	boolean cycleCreatorStylesWhileMoving()
	{
		return Boolean.parseBoolean(configManager.getConfiguration(
			CONFIG_GROUP, "cycleCreatorStylesWhileMoving"));
	}

	static String nextMovementStyleId(List<String> styleIds, String currentStyleId, int selection)
	{
		String normalizedCurrent = FaceSwapAssignment.normalizeStyleId(currentStyleId);
		List<String> candidates = styleIds.stream()
			.filter(styleId -> !styleId.equals(normalizedCurrent))
			.collect(Collectors.toList());
		if (candidates.isEmpty())
		{
			return normalizedCurrent;
		}
		return candidates.get(Math.floorMod(selection, candidates.size()));
	}

	private void updateMovementStyle(Player localPlayer)
	{
		FaceSwapHead selectedHead = getEffectiveSelectedHead();
		if (client.getGameState() != GameState.LOGGED_IN || localPlayer == null
			|| !shouldCycleCreatorStyles(selectedHead))
		{
			resetMovementStyleCycle();
			return;
		}

		LocalPoint location = localPlayer.getLocalLocation();
		if (location == null)
		{
			return;
		}
		boolean moved = lastMovementX != Integer.MIN_VALUE
			&& (location.getX() != lastMovementX || location.getY() != lastMovementY);
		lastMovementX = location.getX();
		lastMovementY = location.getY();
		if (!moved)
		{
			if (playerWasMoving)
			{
				playerWasMoving = false;
				movementCycleHead = selectedHead;
				movementCycleStyleId = getConfiguredDefaultStyleId(selectedHead);
				movementCycleTicks = 0;
				refreshPanel();
			}
			return;
		}

		if (!playerWasMoving || movementCycleHead != selectedHead)
		{
			playerWasMoving = true;
			movementCycleHead = selectedHead;
			String configuredStyleId = getConfiguredDefaultStyleId(selectedHead);
			movementCycleStyleId = MOVEMENT_STYLE_IDS.contains(configuredStyleId)
				? configuredStyleId
				: FaceSwapAssignment.DEFAULT_STYLE_ID;
			movementCycleTicks = 0;
		}
		movementCycleTicks++;
		if (movementCycleTicks >= MOVEMENT_STYLE_INTERVAL_CLIENT_TICKS)
		{
			movementCycleTicks = 0;
			movementCycleStyleId = nextMovementCycleStyleId(movementCycleStyleId);
			refreshPanel();
		}
	}

	private void updateOtherMovementStyles()
	{
		if (client.getGameState() != GameState.LOGGED_IN || !cycleCreatorStylesWhileMoving())
		{
			otherMovementCycles.clear();
			return;
		}

		Set<Player> visiblePlayers = Collections.newSetFromMap(new IdentityHashMap<>());
		visiblePlayers.addAll(client.getPlayers());
		Player localPlayer = client.getLocalPlayer();
		for (Player player : visiblePlayers)
		{
			if (player == localPlayer)
			{
				continue;
			}

			FaceSwapAssignment assignment = getConfiguredAssignedAssignment(player);
			if (assignment == null || !shouldCycleCreatorStyles(assignment.getHead()))
			{
				otherMovementCycles.remove(player);
				continue;
			}

			LocalPoint location = player.getLocalLocation();
			if (location == null)
			{
				otherMovementCycles.remove(player);
				continue;
			}

			MovementCycleState state = otherMovementCycles.computeIfAbsent(
				player, ignored -> new MovementCycleState());
			boolean moved = state.lastX != Integer.MIN_VALUE
				&& (location.getX() != state.lastX || location.getY() != state.lastY);
			state.lastX = location.getX();
			state.lastY = location.getY();
			if (!moved)
			{
				if (state.moving)
				{
					state.moving = false;
					state.head = assignment.getHead();
					state.styleId = movementStartingStyle(assignment.getStyleId());
					state.ticks = 0;
				}
				continue;
			}

			if (!state.moving || state.head != assignment.getHead())
			{
				state.moving = true;
				state.head = assignment.getHead();
				state.styleId = movementStartingStyle(assignment.getStyleId());
				state.ticks = 0;
			}
			state.ticks++;
			if (state.ticks >= MOVEMENT_STYLE_INTERVAL_CLIENT_TICKS)
			{
				state.ticks = 0;
				state.styleId = nextMovementCycleStyleId(state.styleId);
			}
		}

		for (java.util.Iterator<Player> iterator = otherMovementCycles.keySet().iterator(); iterator.hasNext();)
		{
			if (!visiblePlayers.contains(iterator.next()))
			{
				iterator.remove();
			}
		}
	}

	private void updateNpcMovementStyles()
	{
		if (client.getGameState() != GameState.LOGGED_IN || !cycleCreatorStylesWhileMoving())
		{
			npcMovementCycles.clear();
			return;
		}

		if (!movementTrackedNpcsInitialized)
		{
			for (NPC npc : client.getNpcs())
			{
				if (npc != null)
				{
					movementTrackedNpcs.add(npc);
				}
			}
			movementTrackedNpcsInitialized = true;
		}

		for (NPC npc : new ArrayList<>(movementTrackedNpcs))
		{
			FaceSwapAssignment assignment = getConfiguredNpcAssignment(npc);
			if (assignment == null || !shouldCycleCreatorStyles(assignment.getHead()))
			{
				npcMovementCycles.remove(npc);
				continue;
			}

			LocalPoint location = npc.getLocalLocation();
			if (location == null)
			{
				npcMovementCycles.remove(npc);
				continue;
			}

			MovementCycleState state = npcMovementCycles.computeIfAbsent(
				npc, ignored -> new MovementCycleState());
			boolean moved = state.lastX != Integer.MIN_VALUE
				&& (location.getX() != state.lastX || location.getY() != state.lastY);
			state.lastX = location.getX();
			state.lastY = location.getY();
			if (!moved)
			{
				if (state.moving)
				{
					state.moving = false;
					state.head = assignment.getHead();
					state.styleId = movementStartingStyle(assignment.getStyleId());
					state.ticks = 0;
				}
				continue;
			}

			if (!state.moving || state.head != assignment.getHead())
			{
				state.moving = true;
				state.head = assignment.getHead();
				state.styleId = movementStartingStyle(assignment.getStyleId());
				state.ticks = 0;
			}
			state.ticks++;
			if (state.ticks >= MOVEMENT_STYLE_INTERVAL_CLIENT_TICKS)
			{
				state.ticks = 0;
				state.styleId = nextMovementCycleStyleId(state.styleId);
			}
		}
	}

	private String movementStartingStyle(String styleId)
	{
		String normalizedStyleId = FaceSwapAssignment.normalizeStyleId(styleId);
		return MOVEMENT_STYLE_IDS.contains(normalizedStyleId)
			? normalizedStyleId
			: FaceSwapAssignment.DEFAULT_STYLE_ID;
	}

	static String nextMovementCycleStyleId(String currentStyleId)
	{
		int currentIndex = MOVEMENT_STYLE_IDS.indexOf(FaceSwapAssignment.normalizeStyleId(currentStyleId));
		return MOVEMENT_STYLE_IDS.get((currentIndex + 1) % MOVEMENT_STYLE_IDS.size());
	}

	private void resetMovementStyleCycle()
	{
		boolean changed = movementCycleHead != null
			&& !FaceSwapAssignment.DEFAULT_STYLE_ID.equals(movementCycleStyleId);
		movementCycleHead = null;
		movementCycleStyleId = FaceSwapAssignment.DEFAULT_STYLE_ID;
		movementCycleTicks = 0;
		lastMovementX = Integer.MIN_VALUE;
		lastMovementY = Integer.MIN_VALUE;
		playerWasMoving = false;
		otherMovementCycles.clear();
		npcMovementCycles.clear();
		localReactionStyleId = null;
		localReactionTicks = 0;
		nextDamageTakenCrying = true;
		nextDamageDealtAngry = true;
		if (changed)
		{
			refreshPanel();
		}
	}

	private void updateLocalReaction()
	{
		if (localReactionTicks > 0)
		{
			localReactionTicks--;
			if (localReactionTicks == 0)
			{
				localReactionStyleId = null;
				refreshPanel();
			}
		}
	}

	private boolean isDebugLaunch()
	{
		return developerMode;
	}

	FaceSwapRenderMode getRenderMode()
	{
		return config.renderMode();
	}

	boolean useTabbedHeadPicker()
	{
		return config.tabbedHeadPicker();
	}

	FaceSwapHeadPickerLayout getHeadPickerLayout()
	{
		return config.headPickerLayout();
	}

	FaceSwapHeadCategory getLastHeadPickerCategory()
	{
		FaceSwapHeadCategory category = config.lastHeadPickerCategory();
		return category == null ? FaceSwapHeadCategory.CONTENT_CREATOR : category;
	}

	void setLastHeadPickerCategory(FaceSwapHeadCategory category)
	{
		if (category != null)
		{
			configManager.setConfiguration(CONFIG_GROUP, "lastHeadPickerCategory", category);
		}
	}

	boolean shouldOpenHeadPickerOnFirstUse()
	{
		return configManager.getConfiguration(CONFIG_GROUP, "headPickerOpened") == null;
	}

	void markHeadPickerOpened()
	{
		configManager.setConfiguration(CONFIG_GROUP, "headPickerOpened", true);
	}

	boolean isPrototype3dEnabled()
	{
		return config.renderMode() == FaceSwapRenderMode.THREE_D;
	}

	boolean isDkMode()
	{
		return config.dkMode();
	}

	boolean shouldRenderDkModeHat(Player player)
	{
		if (!config.dkMode() || player == null)
		{
			return false;
		}

		int itemId = getPlayerHeadItemId(player);
		if (itemId <= 0 || isFaceCoveringHeadItem(itemId))
		{
			return false;
		}

		ItemComposition itemComposition = client.getItemDefinition(itemId);
		return itemComposition != null && isHatLikeHeadItemName(itemComposition.getName());
	}

	static boolean isHatLikeHeadItemName(String itemName)
	{
		if (itemName == null)
		{
			return false;
		}

		String normalizedName = itemName.toLowerCase(Locale.ROOT);
		if (normalizedName.contains("full helm") || normalizedName.contains("full helmet"))
		{
			return false;
		}
		for (String namePart : HAT_LIKE_ITEM_NAME_PARTS)
		{
			if (normalizedName.contains(namePart))
			{
				return true;
			}
		}
		return false;
	}

	boolean isInteractiveCalibrationActive()
	{
		Player player = client.getLocalPlayer();
		FaceSwapAssignment assignment = getAssignedAssignment(player);
		return developerMode
			&& config.interactiveHelmetCalibration()
			&& getAssignmentRenderMode(assignment) == FaceSwapRenderMode.THREE_D
			&& client.getGameState() == GameState.LOGGED_IN
			&& player != null
			&& interactiveHelmetCalibration != null
			&& interactiveHelmetCalibrationItemId == getLocalHeadItemId(player)
			&& assignment != null
			&& !hasFaceCoveringHeadgear(player);
	}

	HelmetCalibration getInteractiveCalibration()
	{
		return interactiveHelmetCalibration == null ? null : interactiveHelmetCalibration.copy();
	}

	boolean isCanvasEvent(MouseEvent event)
	{
		return event != null && client.getCanvas() != null && event.getComponent() == client.getCanvas();
	}

	private void ensureInteractiveHelmetCalibration(Player player, int headItemId)
	{
		if (!developerMode || !config.interactiveHelmetCalibration() || headItemId <= 0)
		{
			interactiveHelmetCalibration = null;
			interactiveHelmetCalibrationItemId = Integer.MIN_VALUE;
			return;
		}
		if (interactiveHelmetCalibrationItemId == headItemId && interactiveHelmetCalibration != null)
		{
			return;
		}

		interactiveHelmetCalibrationItemId = headItemId;
		interactiveHelmetCalibration = HelmetCalibration.fromProfileOrConfig(
			HelmetProfiles.find(headItemId), config);
		interactiveHelmetCalibration.setMeshCalibration(HelmetMeshCalibrations.get(headItemId));
		prototype3dLiveCalibrationItemId = headItemId;
		clearPrototype3dModelCache();
		removePrototype3dInstances();
	}

	void requestInteractiveDrag(CalibrationHandle handle, int deltaX, int deltaY)
	{
		clientThread.invoke(() ->
		{
			if (!isInteractiveCalibrationActive() || interactiveHelmetCalibration == null)
			{
				return;
			}
			interactiveHelmetCalibration.applyDrag(handle, deltaX, deltaY);
			clearPrototype3dModelCache();
			removePrototype3dInstances();
		});
	}

	void requestInteractiveMeshDrag(MeshControlPoint point, float deltaX, float deltaY)
	{
		clientThread.invoke(() ->
		{
			if (!isInteractiveCalibrationActive() || interactiveHelmetCalibration == null)
			{
				return;
			}
			interactiveHelmetCalibration.applyMeshDrag(point, deltaX, deltaY);
			clearPrototype3dModelCache();
			removePrototype3dInstances();
		});
	}

	void requestInteractiveMeshDepthAdjustment(MeshControlPoint point, int wheelRotation)
	{
		clientThread.invoke(() ->
		{
			if (!isInteractiveCalibrationActive() || interactiveHelmetCalibration == null)
			{
				return;
			}
			interactiveHelmetCalibration.adjustMeshDepth(point, wheelRotation);
			clearPrototype3dModelCache();
			removePrototype3dInstances();
		});
	}

	void requestInteractiveZAdjustment(int wheelRotation)
	{
		clientThread.invoke(() ->
		{
			if (!isInteractiveCalibrationActive() || interactiveHelmetCalibration == null)
			{
				return;
			}
			interactiveHelmetCalibration.adjustZ(wheelRotation);
			clearPrototype3dModelCache();
			removePrototype3dInstances();
		});
	}

	void requestSaveInteractiveHelmetPreset()
	{
		clientThread.invoke(() ->
		{
			if (isInteractiveCalibrationActive())
			{
				saveCurrentHelmetPreset();
			}
		});
	}

	void requestResetInteractiveHelmetCalibration()
	{
		clientThread.invoke(() ->
		{
			if (!isInteractiveCalibrationActive())
			{
				return;
			}
			int itemId = interactiveHelmetCalibrationItemId;
			interactiveHelmetCalibration = HelmetCalibration.fromProfileOrConfig(
				HelmetProfiles.find(itemId), config);
			interactiveHelmetCalibration.setMeshCalibration(HelmetMeshCalibrations.get(itemId));
			clearPrototype3dModelCache();
			removePrototype3dInstances();
			addDebugChatMessage("Reset interactive calibration for item " + itemId + ".");
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			// Scene rebuilds discard RuneLiteObjects even when their Java wrappers
			// remain active. Clear the instances so the next logged-in tick recreates them.
			removePrototype3dInstances();
			removeMaskTrackingRigs();
			resetMovementStyleCycle();
			movementTrackedNpcs.clear();
			movementTrackedNpcsInitialized = false;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (npc != null)
		{
			movementTrackedNpcs.add(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (npc != null)
		{
			movementTrackedNpcs.remove(npc);
			npcMovementCycles.remove(npc);
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		Player localPlayer = client.getLocalPlayer();
		FaceSwapAssignment localAssignment = getAssignedAssignment(localPlayer);
		boolean localUses3d = getAssignmentRenderMode(localAssignment) == FaceSwapRenderMode.THREE_D;
		updateMovementStyle(localPlayer);
		updateOtherMovementStyles();
		updateNpcMovementStyles();
		updateLocalReaction();
		updateHoveredPickActor();
		updatePrototype3dHeadItemDebug(localPlayer);
		updateRadiusPlayerNames();
		refreshPanelStatusIfChanged();
		if (getMaskTrackingMode() != MaskTrackingMode.MERGED_MODEL
			&& client.getGameState() == GameState.LOGGED_IN
			&& localPlayer != null)
		{
			updateMaskTrackingRigs();
		}
		else
		{
			removeMaskTrackingRigs();
		}
		if (client.getGameState() != GameState.LOGGED_IN || localPlayer == null)
		{
			removePrototype3dInstances();
			return;
		}

		int localHeadItemId = getPlayerHeadItemId(localPlayer);
		if (localHeadItemId != prototype3dEquippedHeadItemId)
		{
			prototype3dEquippedHeadItemId = localHeadItemId;
			prototype3dLiveCalibrationItemId = Integer.MIN_VALUE;
			interactiveHelmetCalibrationItemId = Integer.MIN_VALUE;
			interactiveHelmetCalibration = null;
		}
		if (localUses3d)
		{
			ensureInteractiveHelmetCalibration(localPlayer, localHeadItemId);
		}
		else
		{
			interactiveHelmetCalibration = null;
			interactiveHelmetCalibrationItemId = Integer.MIN_VALUE;
		}

		Set<Actor> activeActors = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Player player : client.getPlayers())
		{
			updatePrototype3dInstance(player, getAssignedAssignment(player), localPlayer, activeActors);
		}
		for (NPC npc : client.getNpcs())
		{
			updatePrototype3dInstance(npc, getAssignedAssignment(npc), localPlayer, activeActors);
		}

		for (Actor actor : new ArrayList<>(prototype3dInstances.keySet()))
		{
			if (!activeActors.contains(actor))
			{
				removePrototype3dInstance(actor);
			}
		}
		Prototype3dInstance localInstance = prototype3dInstances.get(localPlayer);
		prototype3dObject = localInstance == null ? null : localInstance.object;
	}

	private void updatePrototype3dInstance(
		Actor actor,
		FaceSwapAssignment assignment,
		Player localPlayer,
		Set<Actor> activeActors)
	{
		FaceSwapHead assignedHead = assignment == null ? null : assignment.getHead();
		if (assignedHead == null
			|| getAssignmentRenderMode(assignment) != FaceSwapRenderMode.THREE_D
			|| actor.getLocalLocation() == null)
		{
			return;
		}

		Player player = actor instanceof Player ? (Player) actor : null;
		int headItemId = player == null ? -1 : getPlayerHeadItemId(player);
		HelmetProfile helmetProfile = player == null ? null : HelmetProfiles.find(headItemId);
		if (player != null && ((helmetProfile != null && helmetProfile.isHidden()) || hasFaceCoveringHeadgear(player)))
		{
			return;
		}
		boolean liveHelmetCalibration = player == localPlayer
			&& (prototype3dLiveCalibrationItemId == headItemId
				|| (developerMode && config.saveHelmetPreset())
				|| interactiveHelmetCalibrationItemId == headItemId);
		boolean useHelmetProfile = helmetProfile != null && helmetProfile.isTested() && !liveHelmetCalibration;
		HelmetCalibration interactiveCalibration = player == localPlayer
			&& interactiveHelmetCalibrationItemId == headItemId ? interactiveHelmetCalibration : null;
		int calibratedHeight = interactiveCalibration != null ? interactiveCalibration.getY()
			: useHelmetProfile ? helmetProfile.getModelY() : config.prototype3dY();
		int globalYShift = resolvePrototypeGlobalYShift(headItemId > 0, config.prototype3dGlobalYShift());
		boolean dkMode = config.dkMode();
		int height = clamp(calibratedHeight - clamp(globalYShift, -32, 32), -128, 128);
		int scale = clamp(dkMode ? 200
			: interactiveCalibration != null ? interactiveCalibration.getScale()
			: useHelmetProfile ? helmetProfile.getModelScale() : config.prototype3dScale(), 25, 250);
		int x = clamp(interactiveCalibration != null ? interactiveCalibration.getX()
			: useHelmetProfile ? helmetProfile.getModelX() : config.prototype3dX(), -128, 128);
		int z = clamp(interactiveCalibration != null ? interactiveCalibration.getZ()
			: useHelmetProfile ? helmetProfile.getModelZ() : config.prototype3dZ(), -128, 128);
		int pitch = clamp(interactiveCalibration != null ? interactiveCalibration.getPitch()
			: useHelmetProfile ? helmetProfile.getModelPitch() : config.prototype3dPitch(), -180, 180);
		int yaw = clamp(interactiveCalibration != null ? interactiveCalibration.getYaw()
			: useHelmetProfile ? helmetProfile.getModelYaw() : config.prototype3dYaw(), -180, 180);
		int roll = clamp(interactiveCalibration != null ? interactiveCalibration.getRoll()
			: useHelmetProfile ? helmetProfile.getModelRoll() : config.prototype3dRoll(), -180, 180);
		int widthScale = clamp(dkMode ? 130
			: interactiveCalibration != null ? interactiveCalibration.getWidth()
			: useHelmetProfile ? helmetProfile.getModelWidth() : config.prototype3dWidth(), 50, 200);
		int textureWidthScale = clamp(config.prototype3dTextureWidth(), 50, 200);
		int faceHeightScale = clamp(interactiveCalibration != null ? interactiveCalibration.getFaceHeight()
			: useHelmetProfile ? helmetProfile.getModelFaceHeight() : config.prototype3dFaceHeight(), 50, 200);
		int depthScale = clamp(interactiveCalibration != null ? interactiveCalibration.getDepth()
			: useHelmetProfile ? helmetProfile.getModelDepth() : config.prototype3dDepth(), 50, 200);
		int backDepthScale = clamp(config.prototype3dBackDepth(), 50, 200);
		int chinHeightScale = clamp(config.prototype3dChinHeight(), 50, 200);
		HelmetMeshCalibration meshCalibration = interactiveCalibration != null
			? interactiveCalibration.getMeshCalibration()
			: HelmetMeshCalibrations.get(headItemId);
		FaceSwapTriangleCount triangleCount = getTriangleCount(assignedHead);
		int animationFrameOffset = clamp(interactiveCalibration != null ? interactiveCalibration.getAnimationFrameOffset()
			: useHelmetProfile ? helmetProfile.getAnimationFrameOffset()
			: config.prototypeAnimationFrameOffset(), -3, 3);
		String modelKey = assignedHead.name() + ':' + assignment.getStyleId() + ':'
			+ String.valueOf(assignment.getCustomImageId()) + ':' + height + ':' + scale + ':' + x + ':' + z + ':'
			+ pitch + ':' + yaw + ':' + roll + ':' + widthScale + ':' + faceHeightScale + ':'
			+ depthScale + ':' + backDepthScale + ':' + chinHeightScale + ':'
			+ textureWidthScale + ':' + triangleCount.name() + meshCalibration.key();
		Prototype3dInstance instance = prototype3dInstances.get(actor);
		if (instance == null || !modelKey.equals(instance.modelKey))
		{
			removePrototype3dInstance(actor);
			Model model = prototype3dModelCache.get(modelKey);
			if (model == null)
			{
				model = createPrototype3dModel(height, scale, x, z, pitch, yaw, roll,
					widthScale, faceHeightScale, depthScale, backDepthScale, chinHeightScale,
					textureWidthScale,
					triangleCount, assignedHead, assignment.getStyleId(), assignment.getCustomImageId(), meshCalibration);
				if (model == null)
				{
					return;
				}
				prototype3dModelCache.put(modelKey, model);
			}
			instance = createPrototype3dInstance(actor, model, modelKey);
			if (instance == null)
			{
				return;
			}
			prototype3dInstances.put(actor, instance);
		}
		instance.animationFrameOffset = animationFrameOffset;
		activeActors.add(actor);
	}

	private void updatePrototype3dHeadItemDebug(Player player)
	{
		if (!config.prototype3dHeadItemDebug())
		{
			setPrototype3dHeadItemDebugText(Integer.MIN_VALUE, "Disabled");
			return;
		}

		if (client.getGameState() != GameState.LOGGED_IN || player == null)
		{
			setPrototype3dHeadItemDebugText(-1, "None");
			return;
		}

		int itemId = getLocalHeadItemId(player);
		if (itemId <= 0)
		{
			setPrototype3dHeadItemDebugText(-1, "None");
			return;
		}
		if (isFaceCoveringHeadItem(itemId))
		{
			addDebugChatMessage("No preset is needed for this full-face helmet; the replacement model is hidden.");
			return;
		}
		if (itemId == prototype3dDebugHeadItemId && prototype3dDebugHeadItemText != null)
		{
			setPrototype3dHeadItemDebugText(itemId, prototype3dDebugHeadItemText);
			return;
		}

		ItemComposition itemComposition = client.getItemDefinition(itemId);
		String itemName = itemComposition == null ? "Unknown item" : itemComposition.getName();
		String debugText = itemName + " (" + itemId + ")";
		if (setPrototype3dHeadItemDebugText(itemId, debugText))
		{
			addDebugChatMessage("Equipped head item: " + debugText);
		}
	}

	private boolean setPrototype3dHeadItemDebugText(int itemId, String text)
	{
		if (itemId == prototype3dDebugHeadItemId
			&& text.equals(prototype3dDebugHeadItemText)
			&& text.equals(config.prototype3dEquippedHeadItem()))
		{
			return false;
		}

		prototype3dDebugHeadItemId = itemId;
		prototype3dDebugHeadItemText = text;
		configManager.setConfiguration(CONFIG_GROUP, "prototype3dEquippedHeadItem", text);
		return true;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}
		if ("selectedHead".equals(event.getKey()))
		{
			playerHeadAssignments = null;
			npcHeadAssignments = null;
		}
		else if (event.getKey().startsWith("defaultStyle_"))
		{
			playerHeadAssignments = null;
			npcHeadAssignments = null;
		}
		else if (event.getKey().startsWith("targetNames_")
			|| event.getKey().startsWith("targetStyles_")
			|| event.getKey().startsWith("targetModes_")
			|| event.getKey().startsWith("targetCustomImages_"))
		{
			playerHeadAssignments = null;
		}
		else if (event.getKey().startsWith("npcTargetNames_")
			|| event.getKey().startsWith("npcTargetStyles_")
			|| event.getKey().startsWith("npcTargetModes_")
			|| event.getKey().startsWith("npcTargetCustomImages_"))
		{
			npcHeadAssignments = null;
		}
		else if ("cycleCreatorStylesWhileMoving".equals(event.getKey()))
		{
			resetMovementStyleCycle();
		}

		if ("renderMode".equals(event.getKey())
			|| TRIANGLE_OVERRIDES_KEY.equals(event.getKey())
			|| "targetScope".equals(event.getKey())
			|| "npcTargetScope".equals(event.getKey())
			|| event.getKey().startsWith("defaultStyle_")
			|| "headPickerLayout".equals(event.getKey())
			|| "cycleCreatorStylesWhileMoving".equals(event.getKey()))
		{
			refreshPanel();
		}
		else if ("prototype3dHeadItemDebug".equals(event.getKey()))
		{
			clientThread.invoke(() ->
			{
				prototype3dDebugHeadItemId = Integer.MIN_VALUE;
				prototype3dDebugHeadItemText = null;
				updatePrototype3dHeadItemDebug(client.getLocalPlayer());
			});
		}
		else if ("saveHelmetPreset".equals(event.getKey()) && Boolean.parseBoolean(event.getNewValue()))
		{
			clientThread.invoke(this::saveCurrentHelmetPreset);
		}
		else if ("interactiveHelmetCalibration".equals(event.getKey()))
		{
			clientThread.invoke(() ->
			{
				if (config.interactiveHelmetCalibration())
				{
					ensureInteractiveHelmetCalibration(client.getLocalPlayer(),
						getLocalHeadItemId(client.getLocalPlayer()));
				}
				else
				{
					interactiveHelmetCalibration = null;
					interactiveHelmetCalibrationItemId = Integer.MIN_VALUE;
				}
				clearPrototype3dModelCache();
				removePrototype3dInstances();
			});
		}
		else if (HELMET_PROFILE_CONFIG_KEYS.contains(event.getKey()))
		{
			clientThread.invoke(() ->
			{
				prototype3dLiveCalibrationItemId = getLocalHeadItemId(client.getLocalPlayer());
				if (config.saveHelmetPreset())
				{
					saveCurrentHelmetPreset();
				}
			});
		}

		if ("renderMode".equals(event.getKey())
			|| "selectedHead".equals(event.getKey())
			|| event.getKey().startsWith("defaultStyle_"))
		{
			clientThread.invoke(this::removePrototype3dInstances);
		}
		else if (TRIANGLE_OVERRIDES_KEY.equals(event.getKey()))
		{
			Set<FaceSwapHead> changedHeads =
				FaceSwapHeadQualityProfiles.findChangedHeads(event.getOldValue(), event.getNewValue());
			clientThread.invoke(() ->
			{
				invalidatePrototype3dModelCache(changedHeads);
				removePrototype3dInstances();
			});
		}
		else if ("dkMode".equals(event.getKey())
			|| "prototype3dTextureWidth".equals(event.getKey())
			|| "prototype3dBackDepth".equals(event.getKey())
			|| "prototype3dChinHeight".equals(event.getKey())
			|| HELMET_PROFILE_CONFIG_KEYS.contains(event.getKey())
			|| "interactiveHelmetCalibration".equals(event.getKey()))
		{
			clientThread.invoke(() ->
			{
				clearPrototype3dModelCache();
				removePrototype3dInstances();
			});
		}
		if ("renderMode".equals(event.getKey()) || "maskTrackingMode".equals(event.getKey()))
		{
			clientThread.invoke(this::removeMaskTrackingRigs);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!pickPlayerMode)
		{
			return;
		}

		Actor actor = event.getMenuEntry().getActor();
		if (actor == null)
		{
			net.runelite.api.MenuEntry[] entries = client.getMenuEntries();
			for (int index = entries.length - 1; index >= 0; index--)
			{
				actor = entries[index].getActor();
				if (actor != null)
				{
					break;
				}
			}
		}
		if (actor == null || actor.getName() == null)
		{
			actor = hoveredPickActor;
			if (actor == null || actor.getName() == null)
			{
				return;
			}
		}
		if (actor instanceof Player && config.targetScope() != FaceSwapTargetScope.SPECIFIC_PLAYERS)
		{
			return;
		}
		if (actor instanceof NPC && config.npcTargetScope() != FaceSwapNpcTargetScope.SPECIFIC_NPCS)
		{
			return;
		}
		if (!(actor instanceof Player) && !(actor instanceof NPC))
		{
			return;
		}

		event.consume();
		if (actor instanceof Player)
		{
			assignPlayerToHead(actor.getName(), getEffectiveSelectedHead());
		}
		else if (actor instanceof NPC)
		{
			assignNpcToHead(actor.getName(), getEffectiveSelectedHead());
		}
		pickPlayerMode = false;
		hoveredPickActor = null;
		refreshPanel();
	}

	private void updateHoveredPickActor()
	{
		if (!pickPlayerMode)
		{
			hoveredPickActor = null;
			return;
		}
		hoveredPickActor = null;
		net.runelite.api.MenuEntry[] entries = client.getMenuEntries();
		for (int index = entries.length - 1; index >= 0; index--)
		{
			Actor actor = entries[index].getActor();
			if ((actor instanceof Player && config.targetScope() == FaceSwapTargetScope.SPECIFIC_PLAYERS)
				|| (actor instanceof NPC && config.npcTargetScope() == FaceSwapNpcTargetScope.SPECIFIC_NPCS))
			{
				hoveredPickActor = actor;
				break;
			}
		}
		if (hoveredPickActor == null)
		{
			Player localPlayer = client.getLocalPlayer();
			net.runelite.api.Point mousePosition = client.getMouseCanvasPosition();
			Shape localPlayerHull = localPlayer == null ? null : localPlayer.getConvexHull();
			if (mousePosition != null && localPlayerHull != null
				&& localPlayerHull.contains(mousePosition.getX(), mousePosition.getY()))
			{
				hoveredPickActor = localPlayer;
			}
		}
	}

	private void saveCurrentHelmetPreset()
	{
		if (!developerMode)
		{
			addDebugChatMessage("Helmet presets can only be saved in developer mode.");
			return;
		}

		Player player = client.getLocalPlayer();
		int itemId = getLocalHeadItemId(player);
		if (itemId <= 0)
		{
			addDebugChatMessage("Equip a head-slot item before saving a helmet preset.");
			return;
		}

		ItemComposition itemComposition = client.getItemDefinition(itemId);
		String itemName = itemComposition == null ? "Unknown item" : sanitizeCsvText(itemComposition.getName());
		HelmetCalibration calibration = interactiveHelmetCalibrationItemId == itemId
			&& interactiveHelmetCalibration != null
			? interactiveHelmetCalibration.copy()
			: HelmetCalibration.fromConfig(config);
		if (interactiveHelmetCalibrationItemId != itemId || interactiveHelmetCalibration == null)
		{
			calibration.setMeshCalibration(HelmetMeshCalibrations.get(itemId));
		}
		HelmetPresetData preset = new HelmetPresetData(
			itemId,
			itemName,
			calibration.getY(),
			calibration.getScale(),
			calibration.getX(),
			calibration.getZ(),
			calibration.getPitch(),
			calibration.getYaw(),
			calibration.getRoll(),
			calibration.getWidth(),
			calibration.getFaceHeight(),
			calibration.getDepth(),
			calibration.getAnimationFrameOffset(),
			config.maskAngle(),
			config.maskYaw(),
			config.maskRoll(),
			calibration.getMeshCalibration());

		ExecutorService executor = customImageExecutor;
		if (executor == null)
		{
			addDebugChatMessage("Unable to save helmet preset while the plugin is stopped.");
			return;
		}
		executor.submit(() -> saveHelmetPreset(preset));
	}

	private void saveHelmetPreset(HelmetPresetData preset)
	{
		try
		{
			List<String> lines = HelmetProfiles.readProfileLines();
			int row = findHelmetProfileRow(lines, preset.itemId);
			String[] existingValues = row < 0 ? null : lines.get(row).split(",", -1);
			String itemIds = row < 0 ? Integer.toString(preset.itemId) : existingValues[0];
			String maskY = row < 0 ? "0" : existingValues[13];
			String maskPitch = resolveSavedMaskCalibration(
				preset.maskAngle, row < 0 ? "0" : existingValues[14]);
			String maskYaw = resolveSavedMaskCalibration(
				preset.maskYaw, row < 0 ? "0" : existingValues[15]);
			String maskRoll = resolveSavedMaskCalibration(
				preset.maskRoll, row < 0 ? "0" : existingValues[16]);
			String profile = buildHelmetProfileRow(
				preset, itemIds, maskY, maskPitch, maskYaw, maskRoll);
			if (row < 0)
			{
				lines.add(profile);
			}
			else
			{
				lines.set(row, profile);
			}

			HelmetProfiles.writeRuntimeProfiles(lines);
			HelmetMeshCalibrations.save(preset.itemId, preset.meshCalibration);
			clientThread.invoke(() ->
			{
				clearPrototype3dModelCache();
				removePrototype3dInstances();
				addDebugChatMessage("Saved helmet preset for " + preset.itemName + " (" + preset.itemId + ").");
			});
		}
		catch (IOException | RuntimeException ex)
		{
			log.debug("Unable to save helmet preset", ex);
			clientThread.invoke(() -> addDebugChatMessage("Unable to save helmet preset: " + ex.getMessage()));
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (config == null || !cycleCreatorStylesWhileMoving()
			|| client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		Hitsplat hitsplat = event.getHitsplat();
		if (localPlayer == null || hitsplat == null || hitsplat.getAmount() <= 0)
		{
			return;
		}

		FaceSwapAssignment assignment = getAssignedAssignment(localPlayer);
		if (assignment == null || !shouldCycleCreatorStyles(assignment.getHead()))
		{
			return;
		}

		if (event.getActor() == localPlayer)
		{
			localReactionStyleId = nextDamageTakenCrying ? "crying" : "sad";
			nextDamageTakenCrying = !nextDamageTakenCrying;
		}
		else if (hitsplat.isMine())
		{
			localReactionStyleId = nextDamageDealtAngry ? "angry" : FaceSwapAssignment.DEFAULT_STYLE_ID;
			nextDamageDealtAngry = !nextDamageDealtAngry;
		}
		else
		{
			return;
		}

		localReactionTicks = EXPRESSION_REACTION_HOLD_CLIENT_TICKS;
		refreshPanel();
	}

	private static int findHelmetProfileRow(List<String> lines, int itemId)
	{
		String target = Integer.toString(itemId);
		for (int row = 1; row < lines.size(); row++)
		{
			String line = lines.get(row);
			if (line.isBlank() || line.startsWith("#"))
			{
				continue;
			}
			String[] values = line.split(",", -1);
			if (values.length == 19 && Arrays.asList(values[0].split(";")).contains(target))
			{
				return row;
			}
		}
		return -1;
	}

	private static String resolveSavedMaskCalibration(int configuredValue, String existingValue)
	{
		return configuredValue == 0 ? existingValue : Integer.toString(configuredValue);
	}

	private static String buildHelmetProfileRow(
		HelmetPresetData preset,
		String itemIds,
		String maskY,
		String maskPitch,
		String maskYaw,
		String maskRoll)
	{
		return String.join(",",
			itemIds,
			preset.itemName,
			Integer.toString(preset.prototype3dY),
			Integer.toString(preset.prototype3dScale),
			Integer.toString(preset.prototype3dX),
			Integer.toString(preset.prototype3dZ),
			Integer.toString(preset.prototype3dPitch),
			Integer.toString(preset.prototype3dYaw),
			Integer.toString(preset.prototype3dRoll),
			Integer.toString(preset.prototype3dWidth),
			Integer.toString(preset.prototype3dFaceHeight),
			Integer.toString(preset.prototype3dDepth),
			Integer.toString(preset.prototypeAnimationFrameOffset),
			maskY,
			maskPitch,
			maskYaw,
			maskRoll,
			"tested",
			"Saved from live config on " + LocalDate.now());
	}

	private static String sanitizeCsvText(String value)
	{
		return value.replace(',', ' ').replace('\r', ' ').replace('\n', ' ').trim();
	}

	private static final class HelmetPresetData
	{
		private final int itemId;
		private final String itemName;
		private final int prototype3dY;
		private final int prototype3dScale;
		private final int prototype3dX;
		private final int prototype3dZ;
		private final int prototype3dPitch;
		private final int prototype3dYaw;
		private final int prototype3dRoll;
		private final int prototype3dWidth;
		private final int prototype3dFaceHeight;
		private final int prototype3dDepth;
		private final int prototypeAnimationFrameOffset;
		private final int maskAngle;
		private final int maskYaw;
		private final int maskRoll;
		private final HelmetMeshCalibration meshCalibration;

		private HelmetPresetData(
			int itemId,
			String itemName,
			int prototype3dY,
			int prototype3dScale,
			int prototype3dX,
			int prototype3dZ,
			int prototype3dPitch,
			int prototype3dYaw,
			int prototype3dRoll,
			int prototype3dWidth,
			int prototype3dFaceHeight,
			int prototype3dDepth,
			int prototypeAnimationFrameOffset,
			int maskAngle,
			int maskYaw,
			int maskRoll,
			HelmetMeshCalibration meshCalibration)
		{
			this.itemId = itemId;
			this.itemName = itemName;
			this.prototype3dY = prototype3dY;
			this.prototype3dScale = prototype3dScale;
			this.prototype3dX = prototype3dX;
			this.prototype3dZ = prototype3dZ;
			this.prototype3dPitch = prototype3dPitch;
			this.prototype3dYaw = prototype3dYaw;
			this.prototype3dRoll = prototype3dRoll;
			this.prototype3dWidth = prototype3dWidth;
			this.prototype3dFaceHeight = prototype3dFaceHeight;
			this.prototype3dDepth = prototype3dDepth;
			this.prototypeAnimationFrameOffset = prototypeAnimationFrameOffset;
			this.maskAngle = maskAngle;
			this.maskYaw = maskYaw;
			this.maskRoll = maskRoll;
			this.meshCalibration = meshCalibration == null ? new HelmetMeshCalibration() : meshCalibration.copy();
		}
	}

	private void addDebugChatMessage(String message)
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Face Swap: " + message, null);
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		for (Map.Entry<Actor, Prototype3dInstance> entry : prototype3dInstances.entrySet())
		{
			Actor actor = entry.getKey();
			Prototype3dInstance instance = entry.getValue();
			if (actor.getLocalLocation() == null)
			{
				continue;
			}
			int plane = actor.getWorldView().getPlane();
			instance.object.setLocation(actor.getLocalLocation(), plane);
			instance.object.setZ(Perspective.getFootprintTileHeight(client, actor.getLocalLocation(), plane,
				actor.getFootprintSize()) - actor.getAnimationHeightOffset());
			instance.object.setOrientation(actor.getCurrentOrientation());
			syncPrototypeAnimation(actor, instance);
		}
		for (Map.Entry<Actor, MaskRigInstance> entry : maskRigInstances.entrySet())
		{
			syncMaskRigAnimation(entry.getKey(), entry.getValue());
		}
	}

	private void updateMaskTrackingRigs()
	{
		Set<Actor> activeActors = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Player player : client.getPlayers())
		{
			FaceSwapAssignment assignment = getAssignedAssignment(player);
			if (assignment != null && getAssignmentRenderMode(assignment) == FaceSwapRenderMode.MASK)
			{
				updateMaskTrackingRig(player);
				activeActors.add(player);
			}
		}
		for (NPC npc : client.getNpcs())
		{
			FaceSwapAssignment assignment = getAssignedAssignment(npc);
			if (getMaskTrackingMode() == MaskTrackingMode.ANIMATED_RIG
				&& assignment != null
				&& getAssignmentRenderMode(assignment) == FaceSwapRenderMode.MASK)
			{
				updateMaskTrackingRig(npc);
				activeActors.add(npc);
			}
		}
		maskRigInstances.keySet().removeIf(actor -> !activeActors.contains(actor));
	}

	private void updateMaskTrackingRig(Actor actor)
	{
		if (maskRigInstances.containsKey(actor))
		{
			return;
		}
		if (maskRigBaseModel == null)
		{
			maskRigBaseModel = createMaskTrackingRigModel();
		}
		if (maskRigBaseModel == null)
		{
			return;
		}

		RuneLiteObject object = client.createRuneLiteObject();
		object.setModel(maskRigBaseModel);
		maskRigInstances.put(actor, new MaskRigInstance(object));
	}

	private Model createMaskTrackingRigModel()
	{
		ModelData[] rigParts = new ModelData[MALE_ANCHOR_MODEL_IDS.length + 2];
		rigParts[0] = clonePrototypeModel(RIGGED_HEAD_MODEL_ID);
		rigParts[1] = clonePrototypeModel(RIGGED_JAW_MODEL_ID);
		if (rigParts[0] == null || rigParts[1] == null)
		{
			return null;
		}
		for (int index = 0; index < MALE_ANCHOR_MODEL_IDS.length; index++)
		{
			rigParts[index + 2] = clonePrototypeModel(MALE_ANCHOR_MODEL_IDS[index]);
			if (rigParts[index + 2] == null)
			{
				return null;
			}
		}
		return client.mergeModels(rigParts).light();
	}

	Model getMaskRigModel(Actor actor)
	{
		MaskRigInstance instance = maskRigInstances.get(actor);
		if (instance == null)
		{
			return null;
		}
		syncMaskRigAnimation(actor, instance);
		return instance.object.getModel();
	}

	Model getMaskRigBaseModel()
	{
		return maskRigBaseModel;
	}

	MaskTrackingMode getMaskTrackingMode()
	{
		return resolveMaskTrackingMode(isDebugLaunch(), config.maskTrackingMode());
	}

	static MaskTrackingMode resolveMaskTrackingMode(boolean debugLaunch, MaskTrackingMode configuredMode)
	{
		return debugLaunch && configuredMode != null ? configuredMode : MaskTrackingMode.AUTO;
	}

	private Model createPrototype3dModel(int height, int scale, int x, int z, int pitch, int yaw, int roll,
		int widthScale, int faceHeightScale, int depthScale, int backDepthScale, int chinHeightScale,
		int textureWidthScale,
		FaceSwapTriangleCount triangleCount, FaceSwapHead selectedHead, String styleId, String customImageId,
		HelmetMeshCalibration meshCalibration)
	{
		ModelData modelData = createDenseHeadModelData(triangleCount, meshCalibration);
		if (modelData == null)
		{
			ModelData headModelData = clonePrototypeModel(RIGGED_HEAD_MODEL_ID);
			ModelData jawModelData = clonePrototypeModel(RIGGED_JAW_MODEL_ID);
			if (headModelData == null || jawModelData == null)
			{
				return null;
			}
			applyEditedHeadVertices(headModelData, jawModelData);
			modelData = client.mergeModels(headModelData, jawModelData);
		}
		rotatePrototypeVertices(modelData, pitch, yaw, roll);
		float[] verticesX = modelData.getVerticesX();
		float[] verticesY = modelData.getVerticesY();
		float[] verticesZ = modelData.getVerticesZ();
		float minX = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE;
		float maxZ = -Float.MAX_VALUE;
		for (int vertex = 0; vertex < modelData.getVerticesCount(); vertex++)
		{
			minX = Math.min(minX, verticesX[vertex]);
			maxX = Math.max(maxX, verticesX[vertex]);
			minY = Math.min(minY, verticesY[vertex]);
			maxY = Math.max(maxY, verticesY[vertex]);
			minZ = Math.min(minZ, verticesZ[vertex]);
			maxZ = Math.max(maxZ, verticesZ[vertex]);
		}

		BufferedImage frontImage = getAssignmentImage(
			selectedHead, styleId, customImageId, FaceSwapHeadDirection.FRONT);
		BufferedImage backImage = getAssignmentImage(
			selectedHead, styleId, customImageId, FaceSwapHeadDirection.BACK);
		Color frontFallback = getAssignmentAverageColor(
			selectedHead, styleId, customImageId, FaceSwapHeadDirection.FRONT);
		Color backFallback = getAssignmentAverageColor(
			selectedHead, styleId, customImageId, FaceSwapHeadDirection.BACK);
		int[] faceA = modelData.getFaceIndices1();
		int[] faceB = modelData.getFaceIndices2();
		int[] faceC = modelData.getFaceIndices3();
		short[] faceColors = modelData.getFaceColors();
		float width = Math.max(1f, maxX - minX);
		float heightRange = Math.max(1f, maxY - minY);
		float depth = Math.max(1f, maxZ - minZ);
		float centerZ = (minZ + maxZ) / 2f;
		for (int face = 0; face < modelData.getFaceCount(); face++)
		{
			int a = faceA[face];
			int b = faceB[face];
			int c = faceC[face];
			float faceX = (verticesX[a] + verticesX[b] + verticesX[c]) / 3f;
			float faceY = (verticesY[a] + verticesY[b] + verticesY[c]) / 3f;
			float faceZ = (verticesZ[a] + verticesZ[b] + verticesZ[c]) / 3f;
			boolean front = faceZ <= centerZ;
			float u = (faceX - minX) / width;
			if (!front)
			{
				u = 1f - u;
			}
			u = scalePrototypeTextureU(u, textureWidthScale);
			float v = (faceY - minY) / heightRange;
			BufferedImage image = front ? frontImage : backImage;
			Color fallback = front ? frontFallback : backFallback;
			int rgb = samplePrototypeColor(image, u, v, fallback, triangleCount.usesEnhancedSampling());
			faceColors[face] = toPrototypeHsl(selectedHead, rgb);
		}

		float transformCenterX = (minX + maxX) / 2f;
		// OSRS model Y decreases upward. Keep the lower head seam fixed so scaling
		// enlarges the replacement toward the skull instead of into the neck.
		float transformBaseY = maxY;
		float transformCenterZ = (minZ + maxZ) / 2f;
		float xFactor = scale / 100f * widthScale / 100f;
		float yFactor = scale / 100f * faceHeightScale / 100f;
		float zFactor = scale / 100f * depthScale / 100f;
		float backFactor = backDepthScale / 100f;
		float chinFactor = chinHeightScale / 100f;
		float backHalfDepth = Math.max(1f, maxZ - transformCenterZ);
		float headHeight = Math.max(1f, maxY - minY);
		for (int vertex = 0; vertex < modelData.getVerticesCount(); vertex++)
		{
			float backWeight = smoothStep((verticesZ[vertex] - transformCenterZ) / backHalfDepth);
			float chinWeight = smoothStep((verticesY[vertex] - minY) / headHeight);
			float localZFactor = zFactor * lerp(1f, backFactor, backWeight);
			float localYFactor = yFactor * lerp(1f, chinFactor, chinWeight);
			verticesX[vertex] = transformCenterX + (verticesX[vertex] - transformCenterX) * xFactor + x;
			verticesY[vertex] = transformBaseY + (verticesY[vertex] - transformBaseY) * localYFactor - height;
			verticesZ[vertex] = transformCenterZ + (verticesZ[vertex] - transformCenterZ) * localZFactor + z;
		}

		ModelData[] rigParts = new ModelData[MALE_ANCHOR_MODEL_IDS.length * DENSE_ANCHOR_COPIES + 1];
		rigParts[0] = modelData;
		int rigPart = 1;
		for (int copy = 0; copy < DENSE_ANCHOR_COPIES; copy++)
		{
			int anchorOffset = copy < DENSE_ANCHOR_COPIES / 2
				? copy - DENSE_ANCHOR_COPIES / 2
				: copy - DENSE_ANCHOR_COPIES / 2 + 1;
			for (int anchorModelId : MALE_ANCHOR_MODEL_IDS)
			{
				ModelData anchor = cloneTransparentAnchorModel(anchorModelId);
				if (anchor == null)
				{
					return null;
				}
				anchor.translate(anchorOffset, 0, 0);
				rigParts[rigPart++] = anchor;
			}
		}
		modelData = client.mergeModels(rigParts);
		return modelData.light();
	}

	static float scalePrototypeTextureU(float u, int textureWidthScale)
	{
		float width = Math.max(1, textureWidthScale);
		return 0.5f + (u - 0.5f) * 100f / width;
	}

	private Prototype3dInstance createPrototype3dInstance(Actor actor, Model model, String modelKey)
	{
		if (actor == null || actor.getLocalLocation() == null)
		{
			return null;
		}
		RuneLiteObject object = client.createRuneLiteObject();
		object.setModel(model);
		object.setRadius(60);
		int plane = actor.getWorldView().getPlane();
		object.setLocation(actor.getLocalLocation(), plane);
		object.setZ(Perspective.getFootprintTileHeight(client, actor.getLocalLocation(), plane,
			actor.getFootprintSize()) - actor.getAnimationHeightOffset());
		object.setOrientation(actor.getCurrentOrientation());
		object.setActive(true);
		return new Prototype3dInstance(object, modelKey);
	}

	private ModelData clonePrototypeModel(int modelId)
	{
		ModelData modelData = client.loadModelData(modelId);
		if (modelData == null)
		{
			return null;
		}

		modelData = modelData.cloneVertices().cloneColors();
		if (modelData.getFaceTextures() != null)
		{
			modelData = modelData.cloneTextures();
			Arrays.fill(modelData.getFaceTextures(), (short) -1);
		}
		return modelData;
	}

	private ModelData createDenseHeadModelData(FaceSwapTriangleCount triangleCount,
		HelmetMeshCalibration meshCalibration)
	{
		int donorCopies = triangleCount.getDonorCopies();
		int headSegments = triangleCount.getSegments();
		int headRings = triangleCount.getRings();
		boolean enhancedQuality = triangleCount.usesEnhancedSampling();
		ModelData[] donors = new ModelData[donorCopies];
		for (int donor = 0; donor < donors.length; donor++)
		{
			donors[donor] = clonePrototypeModel(DENSE_DONOR_MODEL_ID);
			if (donors[donor] == null || donors[donor].getVerticesCount() != DENSE_DONOR_VERTEX_COUNT)
			{
				return null;
			}
			// Prevent mergeModels from deduplicating the identical donor vertices.
			donors[donor].translate(1000 * donor, 0, 0);
		}

		// mergeModels gives this plugin private face-index arrays to rewrite.
		ModelData modelData = client.mergeModels(donors).cloneTransparencies(true);
		int requiredVertices = headSegments * headRings + 2;
		int requiredFaces = headSegments * 2
			+ headSegments * (headRings - 1) * 2;
		if (modelData.getVerticesCount() < requiredVertices || modelData.getFaceCount() < requiredFaces)
		{
			return null;
		}

		float[] verticesX = modelData.getVerticesX();
		float[] verticesY = modelData.getVerticesY();
		float[] verticesZ = modelData.getVerticesZ();
		Arrays.fill(verticesX, 0f);
		Arrays.fill(verticesY, -181f);
		Arrays.fill(verticesZ, 0f);
		float[] logicalX = new float[requiredVertices];
		float[] logicalY = new float[requiredVertices];
		float[] logicalZ = new float[requiredVertices];
		int vertex = 0;
		logicalY[vertex++] = -198f;
		for (int ring = 1; ring <= headRings; ring++)
		{
			double theta = Math.PI * ring / (headRings + 1);
			double vertical = Math.cos(theta);
			double ringRadius = Math.sin(theta);
			for (int segment = 0; segment < headSegments; segment++)
			{
				double phi = Math.PI * 2d * segment / headSegments;
				double normalizedX = ringRadius * Math.sin(phi);
				double normalizedZ = ringRadius * Math.cos(phi);
				double front = Math.max(0d, -normalizedZ);
				double nose = Math.exp(-Math.pow(normalizedX / 0.28d, 2d)
					- Math.pow((vertical - 0.08d) / 0.30d, 2d));
				double chin = Math.exp(-Math.pow(normalizedX / 0.45d, 2d)
					- Math.pow((vertical + 0.72d) / 0.22d, 2d));
				logicalX[vertex] = (float) (normalizedX * 11d);
				logicalY[vertex] = (float) (-181d - vertical * 17d);
				logicalZ[vertex] = (float) (normalizedZ * 12d - front * (nose * 3d + chin * 1.2d));
				vertex++;
			}
		}
		int bottomVertex = vertex;
		logicalY[vertex] = -164f;
		if (meshCalibration != null)
		{
			meshCalibration.applyTo(logicalX, logicalY, logicalZ);
		}
		int[] logicalToPhysical = createDenseHeadVertexMap(requiredVertices, bottomVertex,
			donorCopies, headSegments, headRings);
		for (int logicalVertex = 0; logicalVertex < requiredVertices; logicalVertex++)
		{
			int physicalVertex = logicalToPhysical[logicalVertex];
			verticesX[physicalVertex] = logicalX[logicalVertex];
			verticesY[physicalVertex] = logicalY[logicalVertex];
			verticesZ[physicalVertex] = logicalZ[logicalVertex];
		}

		int[] faceA = modelData.getFaceIndices1();
		int[] faceB = modelData.getFaceIndices2();
		int[] faceC = modelData.getFaceIndices3();
		int face = 0;
		for (int segment = 0; segment < headSegments; segment++)
		{
			int current = 1 + segment;
			int next = 1 + (segment + 1) % headSegments;
			faceA[face] = logicalToPhysical[0];
			faceB[face] = logicalToPhysical[next];
			faceC[face++] = logicalToPhysical[current];
		}
		for (int ring = 0; ring < headRings - 1; ring++)
		{
			int currentRing = 1 + ring * headSegments;
			int nextRing = currentRing + headSegments;
			for (int segment = 0; segment < headSegments; segment++)
			{
				int nextSegment = (segment + 1) % headSegments;
				int a = currentRing + segment;
				int b = currentRing + nextSegment;
				int c = nextRing + segment;
				int d = nextRing + nextSegment;
				if (enhancedQuality && ((ring + segment) & 1) != 0)
				{
					faceA[face] = logicalToPhysical[a];
					faceB[face] = logicalToPhysical[b];
					faceC[face++] = logicalToPhysical[d];
					faceA[face] = logicalToPhysical[a];
					faceB[face] = logicalToPhysical[d];
					faceC[face++] = logicalToPhysical[c];
				}
				else
				{
					faceA[face] = logicalToPhysical[a];
					faceB[face] = logicalToPhysical[b];
					faceC[face++] = logicalToPhysical[c];
					faceA[face] = logicalToPhysical[b];
					faceB[face] = logicalToPhysical[d];
					faceC[face++] = logicalToPhysical[c];
				}
			}
		}
		int lastRing = 1 + (headRings - 1) * headSegments;
		for (int segment = 0; segment < headSegments; segment++)
		{
			faceA[face] = logicalToPhysical[bottomVertex];
			faceB[face] = logicalToPhysical[lastRing + segment];
			faceC[face++] = logicalToPhysical[lastRing + (segment + 1) % headSegments];
		}
		while (face < modelData.getFaceCount())
		{
			faceA[face] = 0;
			faceB[face] = 0;
			faceC[face++] = 0;
		}

		byte[] transparencies = modelData.getFaceTransparencies();
		Arrays.fill(transparencies, (byte) 0);
		for (int unusedFace = requiredFaces; unusedFace < transparencies.length; unusedFace++)
		{
			transparencies[unusedFace] = (byte) -1;
		}
		return modelData;
	}

	private static int[] createDenseHeadVertexMap(int vertexCount, int bottomVertex,
		int donorCopies, int headSegments, int headRings)
	{
		int[] logicalToPhysical = new int[vertexCount];
		Arrays.fill(logicalToPhysical, -1);
		boolean[] usedPhysical = new boolean[vertexCount];
		int[] group2Physical = expandDenseDonorGroup(DENSE_DONOR_BASE_GROUP_2, vertexCount, donorCopies);
		int[] group3Physical = expandDenseDonorGroup(DENSE_DONOR_BASE_GROUP_3, vertexCount, donorCopies);
		int[] group2Logical = createDenseSeamVertices(group2Physical.length, bottomVertex, headSegments, headRings);
		int[] group3Logical = createDenseJawVertices(group3Physical.length, headSegments, headRings);
		mapDenseHeadGroup(logicalToPhysical, usedPhysical, group2Logical, group2Physical);
		mapDenseHeadGroup(logicalToPhysical, usedPhysical, group3Logical, group3Physical);

		int physicalVertex = 0;
		for (int logicalVertex = 0; logicalVertex < vertexCount; logicalVertex++)
		{
			if (logicalToPhysical[logicalVertex] >= 0)
			{
				continue;
			}
			while (usedPhysical[physicalVertex])
			{
				physicalVertex++;
			}
			logicalToPhysical[logicalVertex] = physicalVertex;
			usedPhysical[physicalVertex++] = true;
		}
		return logicalToPhysical;
	}

	private static int[] expandDenseDonorGroup(int[] baseGroup, int vertexCount, int donorCopies)
	{
		int count = 0;
		for (int copy = 0; copy < donorCopies; copy++)
		{
			for (int baseVertex : baseGroup)
			{
				if (copy * DENSE_DONOR_VERTEX_COUNT + baseVertex < vertexCount)
				{
					count++;
				}
			}
		}
		int[] expanded = new int[count];
		int index = 0;
		for (int copy = 0; copy < donorCopies; copy++)
		{
			for (int baseVertex : baseGroup)
			{
				int physicalVertex = copy * DENSE_DONOR_VERTEX_COUNT + baseVertex;
				if (physicalVertex < vertexCount)
				{
					expanded[index++] = physicalVertex;
				}
			}
		}
		return expanded;
	}

	private static int[] createDenseSeamVertices(int count, int bottomVertex, int headSegments, int headRings)
	{
		int[] vertices = new int[count];
		vertices[0] = bottomVertex;
		int lastRing = 1 + (headRings - 1) * headSegments;
		int centerSegment = headSegments / 2;
		for (int index = 1; index < count; index++)
		{
			int distance = (index + 1) / 2;
			int direction = index % 2 == 1 ? -1 : 1;
			vertices[index] = lastRing + Math.floorMod(centerSegment + direction * distance,
				headSegments);
		}
		return vertices;
	}

	private static int[] createDenseJawVertices(int count, int headSegments, int headRings)
	{
		int[] vertices = new int[count];
		int centerSegment = headSegments / 2;
		int startRing = Math.max(1, (int) Math.round(headRings * 0.60d));
		int index = 0;
		for (int ring = startRing; index < count && ring < headRings; ring++)
		{
			int ringStart = 1 + (ring - 1) * headSegments;
			for (int offset = 0; index < count && offset <= headSegments / 4; offset++)
			{
				if (offset == 0)
				{
					vertices[index++] = ringStart + centerSegment;
				}
				else
				{
					vertices[index++] = ringStart + centerSegment - offset;
					if (index < count)
					{
						vertices[index++] = ringStart + centerSegment + offset;
					}
				}
			}
		}
		return vertices;
	}

	private static void mapDenseHeadGroup(int[] logicalToPhysical, boolean[] usedPhysical,
		int[] logicalVertices, int[] physicalVertices)
	{
		for (int index = 0; index < logicalVertices.length; index++)
		{
			logicalToPhysical[logicalVertices[index]] = physicalVertices[index];
			usedPhysical[physicalVertices[index]] = true;
		}
	}

	private ModelData cloneTransparentAnchorModel(int modelId)
	{
		ModelData modelData = clonePrototypeModel(modelId);
		if (modelData == null)
		{
			return null;
		}

		modelData = modelData.cloneTransparencies(true);
		Arrays.fill(modelData.getFaceTransparencies(), (byte) -1);
		return modelData;
	}

	private void syncPrototypeAnimation(Actor actor, Prototype3dInstance instance)
	{
		int actionAnimation = actor.getAnimation();
		int poseAnimation = actor.getPoseAnimation();
		boolean suppressIdlePose = actionAnimation >= 0 && poseAnimation == actor.getIdlePoseAnimation();
		if (poseAnimation < 0 || suppressIdlePose)
		{
			instance.poseController = null;
			instance.object.setPoseAnimationController(null);
		}
		else
		{
			if (instance.poseController == null
				|| instance.poseController.getAnimation() == null
				|| instance.poseController.getAnimation().getId() != poseAnimation)
			{
				instance.poseController = new AnimationController(client, poseAnimation);
				instance.object.setPoseAnimationController(instance.poseController);
			}
			instance.poseController.setFrame(offsetAnimationFrame(instance.poseController.getAnimation(),
				actor.getPoseAnimationFrame(), instance.animationFrameOffset));
		}

		if (actionAnimation < 0)
		{
			instance.actionController = null;
			instance.object.setAnimationController(null);
		}
		else
		{
			if (instance.actionController == null
				|| instance.actionController.getAnimation() == null
				|| instance.actionController.getAnimation().getId() != actionAnimation)
			{
				instance.actionController = new AnimationController(client, actionAnimation);
				instance.object.setAnimationController(instance.actionController);
			}
			instance.actionController.setFrame(offsetAnimationFrame(instance.actionController.getAnimation(),
				actor.getAnimationFrame(), instance.animationFrameOffset));
		}
	}

	private void syncMaskRigAnimation(Actor actor, MaskRigInstance instance)
	{
		int actionAnimation = actor.getAnimation();
		int poseAnimation = actor.getPoseAnimation();
		boolean suppressIdlePose = actionAnimation >= 0 && poseAnimation == actor.getIdlePoseAnimation();
		if (poseAnimation < 0 || suppressIdlePose)
		{
			instance.poseController = null;
			instance.object.setPoseAnimationController(null);
		}
		else
		{
			if (instance.poseController == null
				|| instance.poseController.getAnimation() == null
				|| instance.poseController.getAnimation().getId() != poseAnimation)
			{
				instance.poseController = new AnimationController(client, poseAnimation);
				instance.object.setPoseAnimationController(instance.poseController);
			}
			instance.poseController.setFrame(offsetAnimationFrame(
				instance.poseController.getAnimation(), actor.getPoseAnimationFrame(), 0));
		}

		if (actionAnimation < 0)
		{
			instance.actionController = null;
			instance.object.setAnimationController(null);
		}
		else
		{
			if (instance.actionController == null
				|| instance.actionController.getAnimation() == null
				|| instance.actionController.getAnimation().getId() != actionAnimation)
			{
				instance.actionController = new AnimationController(client, actionAnimation);
				instance.object.setAnimationController(instance.actionController);
			}
			instance.actionController.setFrame(offsetAnimationFrame(
				instance.actionController.getAnimation(), actor.getAnimationFrame(), 0));
		}
	}

	private static int offsetAnimationFrame(Animation animation, int frame, int offset)
	{
		if (animation == null)
		{
			return 0;
		}

		int frameCount = animation.isMayaAnim() ? animation.getDuration() : animation.getFrameLengths().length;
		if (frameCount <= 0)
		{
			return 0;
		}
		return Math.floorMod(Math.max(0, frame) + offset, frameCount);
	}

	private static void rotatePrototypeVertices(ModelData modelData, int pitch, int yaw, int roll)
	{
		if (pitch == 0 && yaw == 0 && roll == 0)
		{
			return;
		}

		double pitchRadians = Math.toRadians(pitch);
		double yawRadians = Math.toRadians(yaw);
		double rollRadians = Math.toRadians(roll);
		double pitchSin = Math.sin(pitchRadians);
		double pitchCos = Math.cos(pitchRadians);
		double yawSin = Math.sin(yawRadians);
		double yawCos = Math.cos(yawRadians);
		double rollSin = Math.sin(rollRadians);
		double rollCos = Math.cos(rollRadians);
		float[] x = modelData.getVerticesX();
		float[] y = modelData.getVerticesY();
		float[] z = modelData.getVerticesZ();
		float centerX = 0;
		float centerY = 0;
		float centerZ = 0;
		for (int vertex = 0; vertex < modelData.getVerticesCount(); vertex++)
		{
			centerX += x[vertex];
			centerY += y[vertex];
			centerZ += z[vertex];
		}
		centerX /= modelData.getVerticesCount();
		centerY /= modelData.getVerticesCount();
		centerZ /= modelData.getVerticesCount();

		for (int vertex = 0; vertex < modelData.getVerticesCount(); vertex++)
		{
			double relativeX = x[vertex] - centerX;
			double relativeY = y[vertex] - centerY;
			double relativeZ = z[vertex] - centerZ;
			double rotatedY = relativeY * pitchCos - relativeZ * pitchSin;
			double rotatedZ = relativeY * pitchSin + relativeZ * pitchCos;
			double rotatedX = relativeX * yawCos + rotatedZ * yawSin;
			rotatedZ = -relativeX * yawSin + rotatedZ * yawCos;
			double rolledX = rotatedX * rollCos - rotatedY * rollSin;
			double rolledY = rotatedX * rollSin + rotatedY * rollCos;
			x[vertex] = centerX + (float) rolledX;
			y[vertex] = centerY + (float) rolledY;
			z[vertex] = centerZ + (float) rotatedZ;
		}
	}

	private void applyEditedHeadVertices(ModelData... modelParts)
	{
		try (InputStream input = FaceSwapPlugin.class.getResourceAsStream(EDITED_HEAD_VERTICES))
		{
			if (input == null)
			{
				return;
			}

			int vertexCount = Arrays.stream(modelParts).mapToInt(ModelData::getVerticesCount).sum();
			float[] editedX = new float[vertexCount];
			float[] editedY = new float[vertexCount];
			float[] editedZ = new float[vertexCount];
			int vertex = 0;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.US_ASCII)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#"))
					{
						continue;
					}
					if (vertex >= vertexCount)
					{
						throw new IOException("Edited head has too many vertices");
					}

					String[] coordinates = line.split(",");
					if (coordinates.length != 3)
					{
						throw new IOException("Invalid edited head vertex: " + line);
					}
					editedX[vertex] = Float.parseFloat(coordinates[0]);
					editedY[vertex] = Float.parseFloat(coordinates[1]);
					editedZ[vertex] = Float.parseFloat(coordinates[2]);
					vertex++;
				}
			}

			if (vertex != vertexCount)
			{
				throw new IOException("Edited head has " + vertex + " vertices; expected "
					+ vertexCount);
			}

			int offset = 0;
			for (ModelData modelPart : modelParts)
			{
				int partVertices = modelPart.getVerticesCount();
				System.arraycopy(editedX, offset, modelPart.getVerticesX(), 0, partVertices);
				System.arraycopy(editedY, offset, modelPart.getVerticesY(), 0, partVertices);
				System.arraycopy(editedZ, offset, modelPart.getVerticesZ(), 0, partVertices);
				offset += partVertices;
			}
		}
		catch (IOException | NumberFormatException ex)
		{
			log.debug("Unable to load edited prototype head vertices", ex);
		}
	}

	private static int samplePrototypeColor(BufferedImage image, float u, float v, Color fallback,
		boolean enhancedQuality)
	{
		if (!enhancedQuality)
		{
			return sampleStandardPrototypeColor(image, u, v, fallback);
		}

		double imageX = Math.max(0d, Math.min(1d, u)) * (image.getWidth() - 1);
		double imageY = Math.max(0d, Math.min(1d, v)) * (image.getHeight() - 1);
		int minX = (int) Math.floor(imageX);
		int minY = (int) Math.floor(imageY);
		int maxX = Math.min(image.getWidth() - 1, minX + 1);
		int maxY = Math.min(image.getHeight() - 1, minY + 1);
		double xFraction = imageX - minX;
		double yFraction = imageY - minY;
		double red = 0d;
		double green = 0d;
		double blue = 0d;
		double totalWeight = 0d;
		for (int y = minY; y <= maxY; y++)
		{
			double yWeight = y == minY ? 1d - yFraction : yFraction;
			for (int x = minX; x <= maxX; x++)
			{
				int argb = image.getRGB(x, y);
				double opacity = (argb >>> 24) / 255d;
				if (opacity == 0d)
				{
					continue;
				}
				double xWeight = x == minX ? 1d - xFraction : xFraction;
				double weight = xWeight * yWeight * opacity;
				red += ((argb >>> 16) & 0xFF) * weight;
				green += ((argb >>> 8) & 0xFF) * weight;
				blue += (argb & 0xFF) * weight;
				totalWeight += weight;
			}
		}
		if (totalWeight == 0d)
		{
			return fallback.getRGB();
		}
		return ((int) Math.round(red / totalWeight) << 16)
			| ((int) Math.round(green / totalWeight) << 8)
			| (int) Math.round(blue / totalWeight);
	}

	static short toPrototypeHsl(FaceSwapHead head, int rgb)
	{
		return JagexColor.rgbToHSL(rgb, 1d);
	}

	static int resolvePrototypeGlobalYShift(boolean helmetEquipped, int configuredShift)
	{
		return helmetEquipped ? configuredShift : NO_HELMET_GLOBAL_Y_SHIFT;
	}

	private static int sampleStandardPrototypeColor(BufferedImage image, float u, float v, Color fallback)
	{
		int centerX = Math.max(0, Math.min(image.getWidth() - 1, Math.round(u * (image.getWidth() - 1))));
		int centerY = Math.max(0, Math.min(image.getHeight() - 1, Math.round(v * (image.getHeight() - 1))));
		long red = 0;
		long green = 0;
		long blue = 0;
		int samples = 0;
		for (int y = Math.max(0, centerY - 1); y <= Math.min(image.getHeight() - 1, centerY + 1); y++)
		{
			for (int x = Math.max(0, centerX - 1); x <= Math.min(image.getWidth() - 1, centerX + 1); x++)
			{
				int argb = image.getRGB(x, y);
				if ((argb >>> 24) == 0)
				{
					continue;
				}
				red += (argb >>> 16) & 0xFF;
				green += (argb >>> 8) & 0xFF;
				blue += argb & 0xFF;
				samples++;
			}
		}
		if (samples == 0)
		{
			return fallback.getRGB();
		}
		return ((int) (red / samples) << 16)
			| ((int) (green / samples) << 8)
			| (int) (blue / samples);
	}

	private void removePrototype3dInstances()
	{
		List<RuneLiteObject> objects = prototype3dInstances.values().stream()
			.map(instance -> instance.object)
			.collect(Collectors.toList());
		prototype3dInstances.clear();
		clearPrototype3dModelCache();
		prototype3dObject = null;
		if (!objects.isEmpty())
		{
			clientThread.invoke(() ->
			{
				for (RuneLiteObject object : objects)
				{
					if (object.isActive())
					{
						object.setActive(false);
					}
				}
			});
		}
	}

	private void clearPrototype3dModelCache()
	{
		prototype3dModelCache.clear();
	}

	private void invalidatePrototype3dModelCache(Set<FaceSwapHead> heads)
	{
		if (heads == null || heads.isEmpty())
		{
			return;
		}
		prototype3dModelCache.entrySet().removeIf(entry -> matchesPrototype3dCacheHead(entry.getKey(), heads));
	}

	private static boolean matchesPrototype3dCacheHead(String modelKey, Set<FaceSwapHead> heads)
	{
		if (modelKey == null || heads == null || heads.isEmpty())
		{
			return false;
		}
		for (FaceSwapHead head : heads)
		{
			if (modelKey.startsWith(head.name() + ':'))
			{
				return true;
			}
		}
		return false;
	}

	private void removeMaskTrackingRigs()
	{
		maskRigInstances.clear();
		maskRigBaseModel = null;
	}

	private void removePrototype3dInstance(Actor actor)
	{
		Prototype3dInstance instance = prototype3dInstances.remove(actor);
		if (instance != null)
		{
			clientThread.invoke(() ->
			{
				if (instance.object.isActive())
				{
					instance.object.setActive(false);
				}
			});
		}
	}

	int getOverlaySize()
	{
		return clamp(config.overlaySize(), 8, 128);
	}

	int getMaskWidth()
	{
		return clamp(config.maskWidth(), 50, 120);
	}

	boolean showMaskStrap()
	{
		return config.showMaskStrap();
	}

	int getHeightOffset()
	{
		return clamp(config.heightOffset(), 0, 300);
	}

	int getMaskForwardOffset()
	{
		int configuredDepth = config.maskForwardOffset();
		if (configManager.getConfiguration(CONFIG_GROUP, "projectedMaskCalibration") == null
			&& (configuredDepth < -24 || configuredDepth > 24))
		{
			return 0;
		}
		return clamp(configuredDepth, -24, 24);
	}

	int getMaskBacking()
	{
		return clamp(config.maskBacking(), 20, 100);
	}

	int getXOffset()
	{
		return clamp(config.xOffset(), -100, 100);
	}

	int getYOffset()
	{
		int configuredY = config.yOffset();
		if (configManager.getConfiguration(CONFIG_GROUP, "projectedMaskCalibration") == null
			&& configuredY == -25)
		{
			return 10;
		}
		return clamp(configuredY, -100, 100);
	}

	int getMaskPitch()
	{
		return clamp(config.maskAngle(), -45, 45);
	}

	int getMaskYaw()
	{
		return clamp(config.maskYaw(), -45, 45);
	}

	int getMaskRoll()
	{
		return clamp(config.maskRoll(), -45, 45);
	}

	boolean isDebugProjection()
	{
		return config.debugProjection();
	}

	boolean isOpaqueBacking()
	{
		return config.opaqueBacking();
	}

	int getWrapHeightOffset()
	{
		return clamp(config.wrapHeightOffset(), -40, 40);
	}

	int getHelmetFaceDrop()
	{
		return clamp(config.helmetFaceDrop(), 0, 90);
	}

	int getHelmetFaceDrop(Player player)
	{
		if (hasMedHelm(player))
		{
			return Math.min(getHelmetFaceDrop(), MED_HELM_FACE_DROP);
		}
		return getHelmetFaceDrop();
	}

	int getWrapRegionHeight()
	{
		return clamp(config.wrapRegionHeight(), 20, 70);
	}

	int getWrapScreenLift()
	{
		return clamp(config.wrapScreenLift(), 0, 120);
	}

	int getWrapTextureLift()
	{
		return clamp(config.wrapTextureLift(), -40, 40);
	}

	int getWrapTextureXOffset()
	{
		return clamp(config.wrapTextureXOffset(), -40, 40);
	}

	int getWrapTextureHeightScale()
	{
		return clamp(config.wrapTextureHeightScale(), 50, 150);
	}

	int getWrapTextureTopBias()
	{
		return clamp(config.wrapTextureTopBias(), -40, 40);
	}

	int getWrapBackingExpansion()
	{
		return clamp(config.wrapBackingExpansion(), 0, 40);
	}

	int getPartialHelmetRegionHeight()
	{
		return clamp(config.partialHelmetRegionHeight(), 12, 60);
	}

	boolean isHelmetOcclusionEnabled()
	{
		return config.helmetOcclusion();
	}

	int getPartialHelmetWidth()
	{
		return clamp(config.partialHelmetWidth(), 30, 120);
	}

	int getPartialHelmetDepth()
	{
		return clamp(config.partialHelmetDepth(), 20, 120);
	}

	int getPartialHelmetFrontDepth()
	{
		return clamp(config.partialHelmetFrontDepth(), 20, 120);
	}

	int getPartialHelmetTopPreserve()
	{
		return clamp(config.partialHelmetTopPreserve(), 0, 40);
	}

	int getPartialHelmetTextureTop()
	{
		return clamp(config.partialHelmetTextureTop(), 0, 70);
	}

	int getPartialHelmetTextureBottom()
	{
		return clamp(config.partialHelmetTextureBottom(), 40, 120);
	}

	int getPartialHelmetTextureLift()
	{
		return clamp(config.partialHelmetTextureLift(), -80, 40);
	}

	int getPartialHelmetClipTop()
	{
		return clamp(config.partialHelmetClipTop(), -60, 30);
	}

	int getPartialHelmetClipBottom()
	{
		return clamp(config.partialHelmetClipBottom(), -30, 30);
	}

	int getPartialHelmetBackingExpansion()
	{
		return clamp(config.partialHelmetBackingExpansion(), 0, 20);
	}

	boolean isDebugEquipment()
	{
		return config.debugEquipment();
	}

	private void migrateHelmetTextureLift()
	{
		if (configManager.getConfiguration(CONFIG_GROUP, "partialHelmetTextureLift") != null)
		{
			return;
		}

		String existingTextureLift = configManager.getConfiguration(CONFIG_GROUP, "wrapTextureLift");
		if (existingTextureLift == null || "0".equals(existingTextureLift))
		{
			return;
		}

		configManager.setConfiguration(CONFIG_GROUP, "partialHelmetTextureLift", existingTextureLift);
		configManager.setConfiguration(CONFIG_GROUP, "wrapTextureLift", 0);
	}

	private void migratePrototypeTriangleCount()
	{
		String enhancedQuality = configManager.getConfiguration(CONFIG_GROUP, "prototype3dEnhancedQuality");
		String legacyTriangleCount = configManager.getConfiguration(CONFIG_GROUP, "prototype3dTriangles");
		if (legacyTriangleCount == null && enhancedQuality != null && Boolean.parseBoolean(enhancedQuality))
		{
			legacyTriangleCount = FaceSwapTriangleCount.TRIANGLES_8000.name();
		}

		String overrides = configManager.getConfiguration(CONFIG_GROUP, TRIANGLE_OVERRIDES_KEY);
		if ((overrides == null || overrides.isBlank()) && legacyTriangleCount != null)
		{
			try
			{
				FaceSwapTriangleCount legacy = FaceSwapTriangleCount.valueOf(legacyTriangleCount);
				if (legacy != FaceSwapTriangleCount.AUTO)
				{
					configManager.setConfiguration(CONFIG_GROUP, TRIANGLE_OVERRIDES_KEY,
						FaceSwapHeadQualityProfiles.setOverride("", getEffectiveSelectedHead(), legacy));
				}
			}
			catch (IllegalArgumentException ignored)
			{
				// Ignore obsolete values and use the shipped per-head defaults.
			}
		}
		configManager.unsetConfiguration(CONFIG_GROUP, "prototype3dTriangles");
		configManager.unsetConfiguration(CONFIG_GROUP, "prototype3dEnhancedQuality");
	}

	private void migrateRenderMode()
	{
		String renderMode = configManager.getConfiguration(CONFIG_GROUP, "renderMode");
		String prototypeEnabled = configManager.getConfiguration(CONFIG_GROUP, "prototype3dEnabled");
		if (renderMode == null)
		{
			configManager.setConfiguration(CONFIG_GROUP, "renderMode", FaceSwapRenderMode.MASK);
		}
		else if ("WRAPAROUND".equals(renderMode) || "FACE_OVERLAY".equals(renderMode)
			|| ("MASK".equals(renderMode) && prototypeEnabled != null))
		{
			FaceSwapRenderMode migratedMode = prototypeEnabled == null || Boolean.parseBoolean(prototypeEnabled)
				? FaceSwapRenderMode.THREE_D
				: "WRAPAROUND".equals(renderMode) ? FaceSwapRenderMode.TWO_D : FaceSwapRenderMode.MASK;
			configManager.setConfiguration(CONFIG_GROUP, "renderMode", migratedMode);
		}
		configManager.unsetConfiguration(CONFIG_GROUP, "prototype3dEnabled");

		if (configManager.getConfiguration(CONFIG_GROUP, "projectedMaskCalibration") == null)
		{
			String maskDepth = configManager.getConfiguration(CONFIG_GROUP, "maskForwardOffset");
			if (maskDepth != null)
			{
				configManager.setConfiguration(CONFIG_GROUP, "maskForwardOffset", 0);
			}
			String maskY = configManager.getConfiguration(CONFIG_GROUP, "yOffset");
			if ("-25".equals(maskY))
			{
				configManager.setConfiguration(CONFIG_GROUP, "yOffset", 10);
			}
			configManager.setConfiguration(CONFIG_GROUP, "projectedMaskCalibration", 1);
		}
	}

	private void migrateTargetSettings()
	{
		if ("OTHERS".equals(configManager.getConfiguration(CONFIG_GROUP, "targetScope")))
		{
			configManager.setConfiguration(CONFIG_GROUP, "targetScope", FaceSwapTargetScope.ALL_PLAYERS);
		}

		String legacyNames = configManager.getConfiguration(CONFIG_GROUP, "targetNames");
		String selectedKey = targetNamesKey(config.selectedHead());
		if (legacyNames != null && !legacyNames.trim().isEmpty()
			&& configManager.getConfiguration(CONFIG_GROUP, selectedKey) == null)
		{
			configManager.setConfiguration(CONFIG_GROUP, selectedKey, normalizeTargetNames(legacyNames));
		}

		String legacyNpcNames = configManager.getConfiguration(CONFIG_GROUP, "npcTargetNames");
		String selectedNpcKey = npcTargetNamesKey(config.selectedHead());
		if (legacyNpcNames != null && !legacyNpcNames.trim().isEmpty()
			&& configManager.getConfiguration(CONFIG_GROUP, selectedNpcKey) == null)
		{
			configManager.setConfiguration(CONFIG_GROUP, selectedNpcKey, normalizeTargetNames(legacyNpcNames));
		}

		for (FaceSwapHead head : FaceSwapHead.values())
		{
			ensureTargetStyles(head, getConfiguredTargetNames(head), false);
			ensureTargetStyles(head, getConfiguredNpcTargetNames(head), true);
			ensureTargetModes(head, getConfiguredTargetNames(head), false);
			ensureTargetModes(head, getConfiguredNpcTargetNames(head), true);
		}
	}

	boolean shouldRenderOn(Player player)
	{
		return getAssignedHead(player) != null;
	}

	FaceSwapHead getAssignedHead(Player player)
	{
		FaceSwapAssignment assignment = getAssignedAssignment(player);
		return assignment == null ? null : assignment.getHead();
	}

	FaceSwapAssignment getAssignedAssignment(Player player)
	{
		return getLocalDynamicAssignment(player, getConfiguredAssignedAssignment(player));
	}

	private FaceSwapAssignment getConfiguredAssignedAssignment(Player player)
	{
		if (player == null)
		{
			return null;
		}

		FaceSwapTargetScope scope = config.targetScope();
		if (scope == FaceSwapTargetScope.SPECIFIC_PLAYERS)
		{
			String playerName = normalizePlayerName(player.getName());
			return usableAssignment(resolveSpecificPlayerAssignment(
				player == client.getLocalPlayer(), pickPlayerMode,
				getConfiguredSelectedAssignment(),
				getPlayerHeadAssignments().get(playerName)));
		}

		Player localPlayer = client.getLocalPlayer();
		boolean matches;
		switch (scope)
		{
			case DISABLED:
				matches = false;
				break;
			case SELF:
				matches = player == localPlayer;
				break;
			case FRIENDS:
				matches = player.isFriend();
				break;
			case CHAT_CHANNEL:
				matches = player.isFriendsChatMember();
				break;
			case CLANMATES:
				matches = player.isClanMember();
				break;
			case IGNORE_LIST:
				matches = player.getName() != null
					&& client.getIgnoreContainer() != null
					&& client.getIgnoreContainer().findByName(player.getName()) != null;
				break;
			case RADIUS:
				matches = isPlayerWithinTargetRadius(player);
				break;
			case ALL_PLAYERS:
			case OTHERS:
				matches = true;
				break;
			default:
				matches = false;
				break;
		}
		if (!matches)
		{
			return null;
		}
		FaceSwapAssignment assignment = getConfiguredSelectedAssignment();
		return assignment;
	}

	private FaceSwapAssignment usableAssignment(FaceSwapAssignment assignment)
	{
		return assignment != null && (assignment.getHead() != FaceSwapHead.CUSTOM
			|| customImageStore.getImage(assignment.getCustomImageId()) != null) ? assignment : null;
	}

	static FaceSwapAssignment resolveSpecificPlayerAssignment(
		boolean localPlayer,
		FaceSwapAssignment selectedAssignment,
		FaceSwapAssignment targetAssignment)
	{
		return resolveSpecificPlayerAssignment(localPlayer, false, selectedAssignment, targetAssignment);
	}

	static FaceSwapAssignment resolveSpecificPlayerAssignment(
		boolean localPlayer,
		boolean pickPlayerMode,
		FaceSwapAssignment selectedAssignment,
		FaceSwapAssignment targetAssignment)
	{
		if (localPlayer && pickPlayerMode)
		{
			return targetAssignment;
		}
		return localPlayer ? targetAssignment == null ? null : selectedAssignment : targetAssignment;
	}

	FaceSwapHead getAssignedHead(NPC npc)
	{
		FaceSwapAssignment assignment = getAssignedAssignment(npc);
		return assignment == null ? null : assignment.getHead();
	}

	FaceSwapAssignment getAssignedAssignment(NPC npc)
	{
		return getNpcMovementAssignment(npc, getConfiguredNpcAssignment(npc));
	}

	private FaceSwapAssignment getConfiguredNpcAssignment(NPC npc)
	{
		if (npc == null || npc.getName() == null)
		{
			return null;
		}
		 switch (config.npcTargetScope())
		{
			case ALL_NPCS:
				return getConfiguredSelectedAssignment();
			case SPECIFIC_NPCS:
				String npcName = normalizePlayerName(npc.getName());
				return usableAssignment(getNpcHeadAssignments().get(npcName));
			case DISABLED:
			default:
				return null;
		}
	}

	private boolean isPlayerWithinTargetRadius(Player player)
	{
		Player localPlayer = client.getLocalPlayer();
		return localPlayer != null && player.getWorldView() == localPlayer.getWorldView()
			&& player.getWorldLocation().distanceTo(localPlayer.getWorldLocation()) <= getTargetRadius();
	}

	boolean isHeadFullyCovered(Player player)
	{
		return hasFaceCoveringHeadgear(player) || (config.hideWithHeadgear() && hasHeadgear(player));
	}

	boolean hasHeadgear(Player player)
	{
		if (player == null)
		{
			return false;
		}

		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return false;
		}

		return hasLocalHeadEquipment(player)
			|| isHeadSlotCovered(composition.getEquipmentId(KitType.HEAD))
			|| isItemEquipment(composition.getEquipmentId(KitType.HAIR))
			|| isHeadSlotCovered(composition.getEquipmentId(KitType.JAW));
	}

	boolean hasFaceCoveringHeadgear(Player player)
	{
		if (player == null)
		{
			return false;
		}

		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return false;
		}

		return isFaceCoveringHeadItem(getLocalHeadItemId(player))
			|| isFaceCoveringHeadItem(getLocalJawItemId(player))
			|| isFaceCoveringHeadItem(getCompositionItemId(composition, KitType.HEAD))
			|| isFaceCoveringHeadItem(getCompositionItemId(composition, KitType.HAIR))
			|| isFaceCoveringHeadItem(getCompositionItemId(composition, KitType.JAW));
	}

	boolean hasMedHelm(Player player)
	{
		if (player == null)
		{
			return false;
		}

		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return false;
		}

		return isMedHelmItem(getLocalHeadItemId(player))
			|| isMedHelmItem(getCompositionItemId(composition, KitType.HEAD));
	}

	private boolean hasLocalHeadEquipment(Player player)
	{
		if (player != client.getLocalPlayer())
		{
			return false;
		}

		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null)
		{
			return false;
		}

		return hasEquippedItem(equipment, EquipmentInventorySlot.HEAD)
			|| hasEquippedItem(equipment, EquipmentInventorySlot.JAW);
	}

	private static boolean hasEquippedItem(ItemContainer equipment, EquipmentInventorySlot slot)
	{
		Item item = equipment.getItem(slot.getSlotIdx());
		return item != null && item.getId() > 0;
	}

	private static boolean isItemEquipment(int equipmentId)
	{
		return equipmentId >= PlayerComposition.ITEM_OFFSET;
	}

	private static boolean isHeadSlotCovered(int equipmentId)
	{
		return equipmentId > 0;
	}

	private boolean isFaceCoveringHeadItem(int itemId)
	{
		if (isFaceExposingHeadItem(itemId))
		{
			return false;
		}

		if (FullFaceHeadItems.contains(itemId)
			|| FACE_COVERING_HEAD_ITEMS.contains(itemId)
			|| isBarrowsFaceCoveringHelm(itemId))
		{
			return true;
		}

		HelmetProfile profile = HelmetProfiles.find(itemId);
		if (profile != null && profile.isHidden())
		{
			return true;
		}

		if (itemId <= 0)
		{
			return false;
		}
		ItemComposition itemComposition = client.getItemDefinition(itemId);
		if (itemComposition == null || itemComposition.getName() == null)
		{
			return false;
		}
		String itemName = itemComposition.getName().toLowerCase(Locale.ROOT);
		for (String namePart : FACE_COVERING_ITEM_NAME_PARTS)
		{
			if (itemName.contains(namePart))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isFaceExposingHeadItem(int itemId)
	{
		if (itemId <= 0)
		{
			return false;
		}
		if (isMedHelmItem(itemId))
		{
			return true;
		}

		ItemComposition itemComposition = client.getItemDefinition(itemId);
		if (itemComposition == null || itemComposition.getName() == null)
		{
			return false;
		}
		String itemName = itemComposition.getName().toLowerCase(Locale.ROOT);
		if (itemName.contains(" med helm"))
		{
			return true;
		}
		for (String namePart : FACE_EXPOSING_ITEM_NAME_PARTS)
		{
			if (itemName.contains(namePart))
			{
				return true;
			}
		}
		return false;
	}

	static boolean isMedHelmItem(int itemId)
	{
		return MED_HELM_ITEMS.contains(itemId);
	}

	private static boolean isBarrowsFaceCoveringHelm(int itemId)
	{
		return isWithin(itemId, ItemID.GUTHANS_HELM_100, ItemID.GUTHANS_HELM_0)
			|| isWithin(itemId, ItemID.TORAGS_HELM_100, ItemID.TORAGS_HELM_0)
			|| isWithin(itemId, ItemID.VERACS_HELM_100, ItemID.VERACS_HELM_0);
	}

	private static boolean isWithin(int itemId, int min, int max)
	{
		return itemId >= min && itemId <= max;
	}

	String getEquipmentDebugText(Player player)
	{
		if (player == null)
		{
			return "equip: no player";
		}

		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return "equip: no composition";
		}

		int head = composition.getEquipmentId(KitType.HEAD);
		int hair = composition.getEquipmentId(KitType.HAIR);
		int jaw = composition.getEquipmentId(KitType.JAW);
		String equipment = "equip wornHead=" + getLocalHeadItemId(player) + " head=" + head + " hair=" + hair + " jaw=" + jaw
			+ " covered=" + hasHeadgear(player)
			+ " faceCover=" + hasFaceCoveringHeadgear(player)
			+ " med=" + hasMedHelm(player)
			+ " drop=" + getHelmetFaceDrop(player)
			+ " cfg=" + config.hideWithHeadgear()
			+ " skip=" + isHeadFullyCovered(player);
		LocalPoint location = player.getLocalLocation();
		if (location == null)
		{
			return equipment;
		}

		int plane = player.getWorldView().getPlane();
		int tileHeight = player.getWorldView().getTileHeight(location.getX(), location.getY(), plane);
		int footprintHeight = Perspective.getFootprintTileHeight(client, location, plane, player.getFootprintSize());
		int objectZ = prototype3dObject == null ? Integer.MIN_VALUE : prototype3dObject.getZ();
		return equipment + "|anchor plane=" + plane + " tile=" + tileHeight + " footprint=" + footprintHeight
			+ " animHeight=" + player.getAnimationHeightOffset() + " objectZ=" + objectZ
			+ " pose=" + player.getPoseAnimation() + ":" + player.getPoseAnimationFrame()
			+ " action=" + player.getAnimation() + ":" + player.getAnimationFrame();
	}

	private int getLocalHeadItemId(Player player)
	{
		if (player != client.getLocalPlayer())
		{
			return -1;
		}

		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null)
		{
			return -1;
		}

		Item item = equipment.getItem(EquipmentInventorySlot.HEAD.getSlotIdx());
		return item == null ? -1 : item.getId();
	}

	private int getPlayerHeadItemId(Player player)
	{
		if (player == client.getLocalPlayer())
		{
			return getLocalHeadItemId(player);
		}
		PlayerComposition composition = player == null ? null : player.getPlayerComposition();
		return composition == null ? -1 : getCompositionItemId(composition, KitType.HEAD);
	}

	private static int getCompositionItemId(PlayerComposition composition, KitType kitType)
	{
		int equipmentId = composition.getEquipmentId(kitType);
		return equipmentId >= PlayerComposition.ITEM_OFFSET
			? equipmentId - PlayerComposition.ITEM_OFFSET
			: -1;
	}

	private int getLocalJawItemId(Player player)
	{
		if (player != client.getLocalPlayer())
		{
			return -1;
		}

		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null)
		{
			return -1;
		}

		Item item = equipment.getItem(EquipmentInventorySlot.JAW.getSlotIdx());
		return item == null ? -1 : item.getId();
	}

	void setSelectedHead(FaceSwapHead selectedHead)
	{
		if (selectedHead == null || !isHeadAvailable(selectedHead))
		{
			return;
		}
		playerHeadAssignments = null;
		npcHeadAssignments = null;
		resetMovementStyleCycle();
		configManager.setConfiguration(CONFIG_GROUP, "selectedHead", selectedHead);
		refreshPanel();
	}

	void setSelectedStyle(FaceSwapHead head, String styleId)
	{
		if (head == null || !isHeadAvailable(head))
		{
			return;
		}
		String normalizedStyleId = FaceSwapAssignment.normalizeStyleId(styleId);
		if (!FaceSwapHeadImages.isStyleAvailable(head, normalizedStyleId))
		{
			return;
		}
		playerHeadAssignments = null;
		npcHeadAssignments = null;
		resetMovementStyleCycle();
		configManager.setConfiguration(CONFIG_GROUP, defaultStyleKey(head), normalizedStyleId);
		configManager.setConfiguration(CONFIG_GROUP, "selectedHead", head);
		refreshPanel();
	}

	void setCycleCreatorStylesWhileMoving(boolean enabled)
	{
		configManager.setConfiguration(CONFIG_GROUP, "cycleCreatorStylesWhileMoving", enabled);
		resetMovementStyleCycle();
		refreshPanel();
	}

	List<FaceSwapCustomImageStore.Entry> getCustomImages()
	{
		return customImageStore.getRecents();
	}

	Path getCustomImageDirectory()
	{
		return customImageStore.getDirectory();
	}

	BufferedImage getCustomImage(String id)
	{
		return customImageStore.getImage(id);
	}

	BufferedImage getSelectedCustomImage()
	{
		return customImageStore.getSelectedImage();
	}

	BufferedImage getCustomBackImage(String id)
	{
		return customImageStore.getBackImage(id);
	}

	BufferedImage getSelectedCustomBackImage()
	{
		return customImageStore.getSelectedBackImage();
	}

	String getSelectedCustomImageId()
	{
		return customImageStore.getSelectedId();
	}

	void importCustomImage(java.nio.file.Path source, Consumer<String> callback)
	{
		customImageExecutor.submit(() ->
		{
			String status;
			try
			{
				customImageStore.importImage(source);
				configManager.setConfiguration(CONFIG_GROUP, "selectedHead", FaceSwapHead.CUSTOM);
				status = "Imported " + source.getFileName();
			}
			catch (IOException | RuntimeException ex)
			{
				status = "Could not import image: " + ex.getMessage();
			}
			String result = status;
			SwingUtilities.invokeLater(() ->
			{
				callback.accept(result);
				refreshPanel();
			});
		});
	}

	void importCustomBackImage(String id, java.nio.file.Path source, Consumer<String> callback)
	{
		customImageExecutor.submit(() ->
		{
			String status;
			try
			{
				customImageStore.importBackImage(id, source);
				configManager.setConfiguration(CONFIG_GROUP, "selectedHead", FaceSwapHead.CUSTOM);
				status = "Imported optional back image " + source.getFileName();
			}
			catch (IOException | RuntimeException ex)
			{
				status = "Could not import back image: " + ex.getMessage();
			}
			String result = status;
			SwingUtilities.invokeLater(() ->
			{
				callback.accept(result);
				refreshPanel();
			});
		});
	}

	void replaceCustomImage(String id, java.nio.file.Path source, Consumer<String> callback)
	{
		customImageExecutor.submit(() ->
		{
			String status;
			try
			{
				customImageStore.replaceImage(id, source);
				configManager.setConfiguration(CONFIG_GROUP, "selectedHead", FaceSwapHead.CUSTOM);
				status = "Replaced custom front image with " + source.getFileName();
			}
			catch (IOException | RuntimeException ex)
			{
				status = "Could not replace front image: " + ex.getMessage();
			}
			String result = status;
			SwingUtilities.invokeLater(() ->
			{
				callback.accept(result);
				refreshPanel();
			});
		});
	}

	void selectCustomImage(String id)
	{
		customImageExecutor.submit(() ->
		{
			if (customImageStore.select(id))
			{
				configManager.setConfiguration(CONFIG_GROUP, "selectedHead", FaceSwapHead.CUSTOM);
			}
			SwingUtilities.invokeLater(this::refreshPanel);
		});
	}

	void removeCustomImage(String id, Runnable callback)
	{
		customImageExecutor.submit(() ->
		{
			if (customImageStore.remove(id) && !customImageStore.hasSelectedImage())
			{
				configManager.setConfiguration(CONFIG_GROUP, "selectedHead", FaceSwapHead.SARDACO);
			}
			SwingUtilities.invokeLater(() ->
			{
				callback.run();
				refreshPanel();
			});
		});
	}

	void setTargetScope(FaceSwapTargetScope targetScope)
	{
		if (targetScope == null)
		{
			return;
		}
		if (targetScope != FaceSwapTargetScope.SPECIFIC_PLAYERS)
		{
			pickPlayerMode = false;
			hoveredPickActor = null;
		}
		configManager.setConfiguration(CONFIG_GROUP, "targetScope", targetScope);
		refreshPanel();
	}

	void setTargetRadius(int targetRadius)
	{
		configManager.setConfiguration(CONFIG_GROUP, "targetRadius", clamp(targetRadius, 1, 50));
		refreshPanel();
	}

	private int getTargetRadius()
	{
		return clamp(config.targetRadius(), 1, 50);
	}

	void setNpcTargetScope(FaceSwapNpcTargetScope targetScope)
	{
		if (targetScope == null)
		{
			return;
		}
		if (targetScope != FaceSwapNpcTargetScope.SPECIFIC_NPCS
			&& config.targetScope() != FaceSwapTargetScope.SPECIFIC_PLAYERS)
		{
			pickPlayerMode = false;
			hoveredPickActor = null;
		}
		configManager.setConfiguration(CONFIG_GROUP, "npcTargetScope", targetScope);
		refreshPanel();
	}

	void setTargetNames(String targetNames)
	{
		playerHeadAssignments = null;
		FaceSwapHead selectedHead = getEffectiveSelectedHead();
		String normalizedNames = normalizeTargetNames(targetNames);
		Set<String> assignedNames = parseTargetNames(normalizedNames).keySet();
		Set<String> removedNames = new java.util.HashSet<>(getConfiguredTargetNames(selectedHead));
		removedNames.removeAll(assignedNames);
		if (!assignedNames.isEmpty())
		{
			for (FaceSwapHead head : FaceSwapHead.values())
			{
				if (head == selectedHead)
				{
					continue;
				}
				LinkedHashMap<String, String> otherNames = parseTargetNames(getTargetNames(head));
				if (otherNames.keySet().removeAll(assignedNames))
				{
					configManager.setConfiguration(CONFIG_GROUP, targetNamesKey(head),
						String.join("\n", otherNames.values()));
					removeTargetStyles(head, assignedNames, false);
					removeTargetModes(head, assignedNames, false);
					removeTargetCustomImages(head, assignedNames, false);
				}
			}
		}
		configManager.setConfiguration(CONFIG_GROUP, targetNamesKey(selectedHead), normalizedNames);
		removeTargetStyles(selectedHead, removedNames, false);
		removeTargetModes(selectedHead, removedNames, false);
		removeTargetCustomImages(selectedHead, removedNames, false);
		ensureTargetStyles(selectedHead, assignedNames, false);
		ensureTargetModes(selectedHead, assignedNames, false);
		ensureTargetCustomImages(selectedHead, assignedNames, false);
		refreshPanel();
	}

	private void assignPlayerToHead(String playerName, FaceSwapHead head)
	{
		if (head == null || normalizePlayerName(playerName).isEmpty())
		{
			return;
		}
		LinkedHashMap<String, String> names = parseTargetNames(getTargetNames(head));
		names.put(normalizePlayerName(playerName), playerName.replace('\u00A0', ' ').trim());
		setTargetNames(String.join("\n", names.values()));
		setTargetStyle(playerName, head, getDefaultStyleId(head), false);
		setTargetMode(playerName, head, getRenderMode(), false);
	}

	void setNpcTargetNames(String targetNames)
	{
		npcHeadAssignments = null;
		FaceSwapHead selectedHead = getEffectiveSelectedHead();
		String normalizedNames = normalizeTargetNames(targetNames);
		Set<String> assignedNames = parseTargetNames(normalizedNames).keySet();
		Set<String> removedNames = new java.util.HashSet<>(getConfiguredNpcTargetNames(selectedHead));
		removedNames.removeAll(assignedNames);
		if (!assignedNames.isEmpty())
		{
			for (FaceSwapHead head : FaceSwapHead.values())
			{
				if (head == selectedHead)
				{
					continue;
				}
				LinkedHashMap<String, String> otherNames = parseTargetNames(getNpcTargetNames(head));
				if (otherNames.keySet().removeAll(assignedNames))
				{
					configManager.setConfiguration(CONFIG_GROUP, npcTargetNamesKey(head),
						String.join("\n", otherNames.values()));
					removeTargetStyles(head, assignedNames, true);
					removeTargetModes(head, assignedNames, true);
					removeTargetCustomImages(head, assignedNames, true);
				}
			}
		}
		configManager.setConfiguration(CONFIG_GROUP, npcTargetNamesKey(selectedHead), normalizedNames);
		removeTargetStyles(selectedHead, removedNames, true);
		removeTargetModes(selectedHead, removedNames, true);
		removeTargetCustomImages(selectedHead, removedNames, true);
		ensureTargetStyles(selectedHead, assignedNames, true);
		ensureTargetModes(selectedHead, assignedNames, true);
		ensureTargetCustomImages(selectedHead, assignedNames, true);
		refreshPanel();
	}

	private void removeTargetStyles(FaceSwapHead head, Set<String> names, boolean npc)
	{
		if (head == null || names == null || names.isEmpty())
		{
			return;
		}
		String key = npc ? npcTargetStylesKey(head) : targetStylesKey(head);
		Map<String, String> styles = parseTargetStyles(configManager.getConfiguration(CONFIG_GROUP, key));
		if (styles.keySet().removeAll(names))
		{
			configManager.setConfiguration(CONFIG_GROUP, key, serializeTargetStyles(styles));
		}
	}

	private void ensureTargetStyles(FaceSwapHead head, Set<String> names, boolean npc)
	{
		if (head == null || names == null || names.isEmpty())
		{
			return;
		}
		String key = npc ? npcTargetStylesKey(head) : targetStylesKey(head);
		Map<String, String> styles = parseTargetStyles(configManager.getConfiguration(CONFIG_GROUP, key));
		String defaultStyleId = getDefaultStyleId(head);
		boolean changed = false;
		for (String name : names)
		{
			if (!styles.containsKey(name))
			{
				styles.put(name, defaultStyleId);
				changed = true;
			}
		}
		if (changed)
		{
			configManager.setConfiguration(CONFIG_GROUP, key, serializeTargetStyles(styles));
		}
	}

	private void removeTargetModes(FaceSwapHead head, Set<String> names, boolean npc)
	{
		if (head == null || names == null || names.isEmpty())
		{
			return;
		}
		String key = npc ? npcTargetModesKey(head) : targetModesKey(head);
		Map<String, FaceSwapRenderMode> modes = parseTargetModes(configManager.getConfiguration(CONFIG_GROUP, key));
		if (modes.keySet().removeAll(names))
		{
			configManager.setConfiguration(CONFIG_GROUP, key, serializeTargetModes(modes));
		}
	}

	private void ensureTargetModes(FaceSwapHead head, Set<String> names, boolean npc)
	{
		if (head == null || names == null || names.isEmpty())
		{
			return;
		}
		String key = npc ? npcTargetModesKey(head) : targetModesKey(head);
		Map<String, FaceSwapRenderMode> modes = parseTargetModes(configManager.getConfiguration(CONFIG_GROUP, key));
		boolean changed = false;
		for (String name : names)
		{
			if (!modes.containsKey(name))
			{
				modes.put(name, getRenderMode());
				changed = true;
			}
		}
		if (changed)
		{
			configManager.setConfiguration(CONFIG_GROUP, key, serializeTargetModes(modes));
		}
	}

	private void removeTargetCustomImages(FaceSwapHead head, Set<String> names, boolean npc)
	{
		if (head != FaceSwapHead.CUSTOM || names == null || names.isEmpty())
		{
			return;
		}
		String key = npc ? npcTargetCustomImagesKey(head) : targetCustomImagesKey(head);
		Map<String, String> images = parseTargetCustomImages(configManager.getConfiguration(CONFIG_GROUP, key));
		if (images.keySet().removeAll(names))
		{
			configManager.setConfiguration(CONFIG_GROUP, key, serializeTargetCustomImages(images));
		}
	}

	private void ensureTargetCustomImages(FaceSwapHead head, Set<String> names, boolean npc)
	{
		if (head != FaceSwapHead.CUSTOM || names == null || names.isEmpty())
		{
			return;
		}
		String selectedId = customImageStore.getSelectedId();
		if (selectedId == null)
		{
			return;
		}
		String key = npc ? npcTargetCustomImagesKey(head) : targetCustomImagesKey(head);
		Map<String, String> images = parseTargetCustomImages(configManager.getConfiguration(CONFIG_GROUP, key));
		boolean changed = false;
		for (String name : names)
		{
			if (!images.containsKey(name))
			{
				images.put(name, selectedId);
				changed = true;
			}
		}
		if (changed)
		{
			configManager.setConfiguration(CONFIG_GROUP, key, serializeTargetCustomImages(images));
		}
	}

	private void setTargetStyle(String name, FaceSwapHead head, String styleId, boolean npc)
	{
		if (head == null)
		{
			return;
		}
		String normalizedName = normalizePlayerName(name);
		Set<String> configuredNames = npc ? getConfiguredNpcTargetNames(head) : getConfiguredTargetNames(head);
		if (!configuredNames.contains(normalizedName))
		{
			return;
		}

		String key = npc ? npcTargetStylesKey(head) : targetStylesKey(head);
		Map<String, String> styles = parseTargetStyles(configManager.getConfiguration(CONFIG_GROUP, key));
		String normalizedStyleId = FaceSwapAssignment.normalizeStyleId(styleId);
		if (!FaceSwapHeadImages.isStyleAvailable(head, normalizedStyleId))
		{
			normalizedStyleId = FaceSwapAssignment.DEFAULT_STYLE_ID;
		}
		styles.put(normalizedName, normalizedStyleId);
		configManager.setConfiguration(CONFIG_GROUP, key, serializeTargetStyles(styles));
		if (npc)
		{
			npcHeadAssignments = null;
		}
		else
		{
			playerHeadAssignments = null;
		}
	}

	private void setTargetMode(String name, FaceSwapHead head, FaceSwapRenderMode renderMode, boolean npc)
	{
		if (head == null || renderMode == null)
		{
			return;
		}
		String normalizedName = normalizePlayerName(name);
		Set<String> configuredNames = npc ? getConfiguredNpcTargetNames(head) : getConfiguredTargetNames(head);
		if (!configuredNames.contains(normalizedName))
		{
			return;
		}

		String key = npc ? npcTargetModesKey(head) : targetModesKey(head);
		Map<String, FaceSwapRenderMode> modes = parseTargetModes(configManager.getConfiguration(CONFIG_GROUP, key));
		modes.put(normalizedName, renderMode);
		configManager.setConfiguration(CONFIG_GROUP, key, serializeTargetModes(modes));
		if (npc)
		{
			npcHeadAssignments = null;
		}
		else
		{
			playerHeadAssignments = null;
		}
	}

	private void assignNpcToHead(String npcName, FaceSwapHead head)
	{
		if (head == null || normalizePlayerName(npcName).isEmpty())
		{
			return;
		}
		LinkedHashMap<String, String> names = parseTargetNames(getNpcTargetNames(head));
		names.put(normalizePlayerName(npcName), npcName.replace('\u00A0', ' ').trim());
		setNpcTargetNames(String.join("\n", names.values()));
		setTargetStyle(npcName, head, getDefaultStyleId(head), true);
		setTargetMode(npcName, head, getRenderMode(), true);
	}

	void setRenderMode(FaceSwapRenderMode renderMode)
	{
		if (renderMode == null)
		{
			return;
		}
		configManager.setConfiguration(CONFIG_GROUP, "renderMode", renderMode);
		refreshPanel();
	}

	void setQualityLevel(int qualityLevel)
	{
		FaceSwapTriangleCount triangleCount;
		switch (clamp(qualityLevel, 0, 4))
		{
			case 0:
				triangleCount = FaceSwapTriangleCount.TRIANGLES_2000;
				break;
			case 1:
				triangleCount = FaceSwapTriangleCount.TRIANGLES_4000;
				break;
			case 2:
				triangleCount = FaceSwapTriangleCount.TRIANGLES_6000;
				break;
			case 3:
				triangleCount = FaceSwapTriangleCount.TRIANGLES_7000;
				break;
			case 4:
			default:
				triangleCount = FaceSwapTriangleCount.TRIANGLES_8000;
				break;
		}
		String overrides = configManager.getConfiguration(CONFIG_GROUP, TRIANGLE_OVERRIDES_KEY);
		configManager.setConfiguration(CONFIG_GROUP, TRIANGLE_OVERRIDES_KEY,
			FaceSwapHeadQualityProfiles.setOverride(overrides, getEffectiveSelectedHead(), triangleCount));
		refreshPanel();
	}

	void setDkMode(boolean enabled)
	{
		configManager.setConfiguration(CONFIG_GROUP, "dkMode", enabled);
		refreshPanel();
	}

	void setMaskWidth(int maskWidth)
	{
		configManager.setConfiguration(CONFIG_GROUP, "maskWidth", clamp(maskWidth, 50, 120));
		refreshPanel();
	}

	void setShowMaskStrap(boolean show)
	{
		configManager.setConfiguration(CONFIG_GROUP, "showMaskStrap", show);
		refreshPanel();
	}

	private int getQualityLevel()
	{
		FaceSwapTriangleCount resolved = getTriangleCount(getEffectiveSelectedHead());
		int triangles = resolved.getTriangleCount();
		if (triangles <= 3000)
		{
			return 0;
		}
		if (triangles <= 5000)
		{
			return 1;
		}
		if (triangles <= 6000)
		{
			return 2;
		}
		if (triangles <= 7000)
		{
			return 3;
		}
		return 4;
	}

	private FaceSwapTriangleCount getTriangleCount(FaceSwapHead head)
	{
		String overrides = configManager.getConfiguration(CONFIG_GROUP, TRIANGLE_OVERRIDES_KEY);
		return FaceSwapHeadQualityProfiles.resolve(head, overrides);
	}

	void setPickPlayerMode(boolean pickPlayerMode)
	{
		if (pickPlayerMode && configManager != null)
		{
			configManager.setConfiguration(
				CONFIG_GROUP, "targetScope", FaceSwapTargetScope.SPECIFIC_PLAYERS);
			configManager.setConfiguration(
				CONFIG_GROUP, "npcTargetScope", FaceSwapNpcTargetScope.SPECIFIC_NPCS);
		}
		this.pickPlayerMode = pickPlayerMode;
		if (!pickPlayerMode)
		{
			hoveredPickActor = null;
		}
		refreshPanel();
		if (pickPlayerMode)
		{
			focusGameCanvasForTargeting();
		}
	}

	private void focusGameCanvasForTargeting()
	{
		if (client == null || client.getCanvas() == null)
		{
			return;
		}

		java.awt.Canvas canvas = client.getCanvas();
		SwingUtilities.invokeLater(() ->
		{
			if (!canvas.isDisplayable())
			{
				return;
			}
			if (!canvas.requestFocusInWindow())
			{
				canvas.requestFocus();
			}
		});
	}

	boolean handleKeyPressed(KeyEvent event)
	{
		return client != null && handleKeyPressed(client.getGameState(), event);
	}

	boolean handleKeyPressed(GameState gameState, KeyEvent event)
	{
		return gameState == GameState.LOGGED_IN
			&& event != null
			&& event.getKeyCode() == KeyEvent.VK_ESCAPE
			&& handleEscapePressed();
	}

	boolean handleEscapePressed()
	{
		boolean handled = false;
		FaceSwapPanel currentPanel = panel;
		if (currentPanel != null && currentPanel.closeActiveHeadPickerIfOpen())
		{
			handled = true;
		}
		if (pickPlayerMode)
		{
			pickPlayerMode = false;
			hoveredPickActor = null;
			refreshPanel();
			handled = true;
		}
		if (handled && client != null && client.getCanvas() != null)
		{
			SwingUtilities.invokeLater(() -> client.getCanvas().requestFocusInWindow());
		}
		return handled;
	}

	Actor getHoveredPickActor()
	{
		return pickPlayerMode ? hoveredPickActor : null;
	}

	private void refreshPanel()
	{
		if (panel == null || !panelRefreshQueued.compareAndSet(false, true))
		{
			return;
		}

		clientThread.invoke(() ->
		{
			panelRefreshQueued.set(false);
			refreshPanelOnClientThread();
		});
	}

	private void refreshPanelOnClientThread()
	{
		FaceSwapPanel currentPanel = panel;
		if (currentPanel != null)
		{
			FaceSwapPanelState state = createPanelState();
			lastPanelStatus = state.statusText;
			lastPanelTargetNames = state.targetNames;
			SwingUtilities.invokeLater(() ->
			{
				if (panel == currentPanel)
				{
					currentPanel.refreshState(state);
				}
			});
		}
	}

	private void refreshPanelStatusIfChanged()
	{
		String status = buildStatusText();
		String targetNames = getPanelTargetNames();
		if (!Objects.equals(status, lastPanelStatus) || !Objects.equals(targetNames, lastPanelTargetNames))
		{
			refreshPanel();
		}
	}

	private String getPanelTargetNames()
	{
		return config.targetScope() == FaceSwapTargetScope.RADIUS
			? radiusPlayerNames
			: getTargetNames(getEffectiveSelectedHead());
	}

	private FaceSwapPanelState createPanelState()
	{
		return new FaceSwapPanelState(
			getEffectiveSelectedHead(),
			getEffectiveSelectedAssignment().getStyleId(),
			getHeadPickerLayout(),
			cycleCreatorStylesWhileMoving(),
			config.targetScope(),
			getPanelTargetNames(),
			getTargetRadius(),
			config.npcTargetScope(),
			getNpcTargetNames(getEffectiveSelectedHead()),
			config.renderMode(),
			getQualityLevel(),
			config.dkMode(),
			getMaskWidth(),
			showMaskStrap(),
			pickPlayerMode,
			buildStatusText());
	}

	private String buildStatusText()
	{
		String wraparoundStatus = config.renderMode() == FaceSwapRenderMode.TWO_D
			? "Wraparounds will disappear when a player is wearing a helmet. Wraparound will also glitch out while using a weapon."
			: "";
		String targetStatus;
		if (pickPlayerMode)
		{
			FaceSwapHead selectedHead = getEffectiveSelectedHead();
			String captureDescription = " using "
				+ FaceSwapHeadImages.styleDisplayName(getDefaultStyleId(selectedHead))
				+ " / " + getRenderMode();
			targetStatus = hoveredPickActor == null
				? "Hover and click an eligible player or NPC to add them to " + selectedHead + captureDescription + "."
				: "Click " + hoveredPickActor.getName() + " to add them to " + selectedHead + captureDescription + ".";
			targetStatus += "\n\nPress ESC to exit target mode.";
		}
		else
		{
			targetStatus = "Pick a head above, choose the Style and Pick a Target or select a dropdown";
		}
		return joinStatus(targetStatus, wraparoundStatus);
	}

	private static String joinStatus(String first, String second)
	{
		return first.isEmpty() ? second : first + '\n' + second;
	}

	static String normalizeTargetNames(String targetNames)
	{
		return String.join("\n", parseTargetNames(targetNames).values());
	}

	private String getTargetNames(FaceSwapHead head)
	{
		String names = configManager.getConfiguration(CONFIG_GROUP, targetNamesKey(head));
		return normalizeTargetNames(names);
	}

	private Set<String> getConfiguredTargetNames(FaceSwapHead head)
	{
		return parseTargetNames(getTargetNames(head)).keySet();
	}

	private FaceSwapAssignment createConfiguredAssignment(FaceSwapHead head, String name, boolean npc)
	{
		String styleKey = npc ? npcTargetStylesKey(head) : targetStylesKey(head);
		String styleId = parseTargetStyles(configManager.getConfiguration(CONFIG_GROUP, styleKey)).get(name);
		if (!FaceSwapHeadImages.isStyleAvailable(head, styleId))
		{
			styleId = getDefaultStyleId(head);
		}

		String modeKey = npc ? npcTargetModesKey(head) : targetModesKey(head);
		FaceSwapRenderMode renderMode = parseTargetModes(
			configManager.getConfiguration(CONFIG_GROUP, modeKey)).get(name);
		String customImageId = head == FaceSwapHead.CUSTOM
			? parseTargetCustomImages(configManager.getConfiguration(CONFIG_GROUP,
				npc ? npcTargetCustomImagesKey(head) : targetCustomImagesKey(head))).get(name)
			: null;
		return new FaceSwapAssignment(head, styleId, renderMode, customImageId);
	}

	List<String> getConfiguredPlayerNames(FaceSwapHead head)
	{
		return new ArrayList<>(parseTargetNames(getTargetNames(head)).values());
	}

	private Map<String, FaceSwapAssignment> getPlayerHeadAssignments()
	{
		Map<String, FaceSwapAssignment> assignments = playerHeadAssignments;
		if (assignments != null)
		{
			return assignments;
		}

		Map<String, FaceSwapAssignment> rebuilt = new HashMap<>();
		FaceSwapHead selectedHead = getEffectiveSelectedHead();
		for (FaceSwapHead head : FaceSwapHead.values())
		{
			if (head == selectedHead || !isHeadAvailable(head))
			{
				continue;
			}
			for (String name : getConfiguredTargetNames(head))
			{
				rebuilt.putIfAbsent(name, createConfiguredAssignment(head, name, false));
			}
		}
		for (String name : getConfiguredTargetNames(selectedHead))
		{
			rebuilt.put(name, createConfiguredAssignment(selectedHead, name, false));
		}
		assignments = Collections.unmodifiableMap(rebuilt);
		playerHeadAssignments = assignments;
		return assignments;
	}

	private void updateRadiusPlayerNames()
	{
		if (config.targetScope() != FaceSwapTargetScope.RADIUS)
		{
			radiusPlayerNames = "";
			return;
		}

		radiusPlayerNames = client.getPlayers().stream()
			.filter(this::isPlayerWithinTargetRadius)
			.map(Player::getName)
			.filter(Objects::nonNull)
			.distinct()
			.sorted(String.CASE_INSENSITIVE_ORDER)
			.collect(Collectors.joining("\n"));
	}

	private String getNpcTargetNames(FaceSwapHead head)
	{
		String names = configManager.getConfiguration(CONFIG_GROUP, npcTargetNamesKey(head));
		return normalizeTargetNames(names);
	}

	private Set<String> getConfiguredNpcTargetNames(FaceSwapHead head)
	{
		return parseTargetNames(getNpcTargetNames(head)).keySet();
	}

	private Map<String, FaceSwapAssignment> getNpcHeadAssignments()
	{
		Map<String, FaceSwapAssignment> assignments = npcHeadAssignments;
		if (assignments != null)
		{
			return assignments;
		}

		Map<String, FaceSwapAssignment> rebuilt = new HashMap<>();
		FaceSwapHead selectedHead = getEffectiveSelectedHead();
		for (FaceSwapHead head : FaceSwapHead.values())
		{
			if (head == selectedHead || !isHeadAvailable(head))
			{
				continue;
			}
			for (String name : getConfiguredNpcTargetNames(head))
			{
				rebuilt.putIfAbsent(name, createConfiguredAssignment(head, name, true));
			}
		}
		for (String name : getConfiguredNpcTargetNames(selectedHead))
		{
			rebuilt.put(name, createConfiguredAssignment(selectedHead, name, true));
		}
		assignments = Collections.unmodifiableMap(rebuilt);
		npcHeadAssignments = assignments;
		return assignments;
	}

	private static LinkedHashMap<String, String> parseTargetNames(String targetNames)
	{
		LinkedHashMap<String, String> names = new LinkedHashMap<>();
		if (targetNames == null)
		{
			return names;
		}
		for (String name : targetNames.split("[,\\r\\n]+"))
		{
			String displayName = name.replace('\u00A0', ' ').trim();
			String normalizedName = normalizePlayerName(displayName);
			if (!normalizedName.isEmpty())
			{
				names.putIfAbsent(normalizedName, displayName);
			}
		}
		return names;
	}

	static String targetNamesKey(FaceSwapHead head)
	{
		return "targetNames_" + head.name().toLowerCase(Locale.ROOT);
	}

	static String npcTargetNamesKey(FaceSwapHead head)
	{
		return "npcTargetNames_" + head.name().toLowerCase(Locale.ROOT);
	}

	static String defaultStyleKey(FaceSwapHead head)
	{
		return "defaultStyle_" + head.name().toLowerCase(Locale.ROOT);
	}

	static String targetStylesKey(FaceSwapHead head)
	{
		return "targetStyles_" + head.name().toLowerCase(Locale.ROOT);
	}

	static String npcTargetStylesKey(FaceSwapHead head)
	{
		return "npcTargetStyles_" + head.name().toLowerCase(Locale.ROOT);
	}

	static String targetModesKey(FaceSwapHead head)
	{
		return "targetModes_" + head.name().toLowerCase(Locale.ROOT);
	}

	static String npcTargetModesKey(FaceSwapHead head)
	{
		return "npcTargetModes_" + head.name().toLowerCase(Locale.ROOT);
	}

	static String targetCustomImagesKey(FaceSwapHead head)
	{
		return "targetCustomImages_" + head.name().toLowerCase(Locale.ROOT);
	}

	static String npcTargetCustomImagesKey(FaceSwapHead head)
	{
		return "npcTargetCustomImages_" + head.name().toLowerCase(Locale.ROOT);
	}

	static Map<String, String> parseTargetStyles(String serializedStyles)
	{
		Map<String, String> styles = new HashMap<>();
		if (serializedStyles == null || serializedStyles.isBlank())
		{
			return styles;
		}
		for (String entry : serializedStyles.split(","))
		{
			String[] fields = entry.split("=", 2);
			if (fields.length == 2)
			{
				String name = normalizePlayerName(fields[0]);
				String styleId = FaceSwapAssignment.normalizeStyleId(fields[1]);
				if (!name.isEmpty())
				{
					styles.put(name, styleId);
				}
			}
		}
		return styles;
	}

	static String serializeTargetStyles(Map<String, String> styles)
	{
		if (styles == null || styles.isEmpty())
		{
			return "";
		}
		return styles.entrySet().stream()
			.filter(entry -> !entry.getKey().isBlank())
			.map(entry -> normalizePlayerName(entry.getKey()) + "="
				+ FaceSwapAssignment.normalizeStyleId(entry.getValue()))
			.sorted()
			.collect(Collectors.joining(","));
	}

	static Map<String, String> parseTargetCustomImages(String serializedImages)
	{
		Map<String, String> images = new HashMap<>();
		if (serializedImages == null || serializedImages.isBlank())
		{
			return images;
		}
		for (String entry : serializedImages.split(","))
		{
			String[] fields = entry.split("=", 2);
			if (fields.length != 2)
			{
				continue;
			}
			String name = normalizePlayerName(fields[0]);
			String imageId = fields[1].trim();
			if (!name.isEmpty() && !imageId.isEmpty())
			{
				images.put(name, imageId);
			}
		}
		return images;
	}

	static String serializeTargetCustomImages(Map<String, String> images)
	{
		if (images == null || images.isEmpty())
		{
			return "";
		}
		return images.entrySet().stream()
			.filter(entry -> !entry.getKey().isBlank()
				&& entry.getValue() != null && !entry.getValue().isBlank())
			.map(entry -> normalizePlayerName(entry.getKey()) + "=" + entry.getValue().trim())
			.sorted()
			.collect(Collectors.joining(","));
	}

	static Map<String, FaceSwapRenderMode> parseTargetModes(String serializedModes)
	{
		Map<String, FaceSwapRenderMode> modes = new HashMap<>();
		if (serializedModes == null || serializedModes.isBlank())
		{
			return modes;
		}
		for (String entry : serializedModes.split(","))
		{
			String[] fields = entry.split("=", 2);
			if (fields.length != 2)
			{
				continue;
			}
			String name = normalizePlayerName(fields[0]);
			FaceSwapRenderMode renderMode = parseTargetRenderMode(fields[1]);
			if (!name.isEmpty() && renderMode != null)
			{
				modes.put(name, renderMode);
			}
		}
		return modes;
	}

	static String serializeTargetModes(Map<String, FaceSwapRenderMode> modes)
	{
		if (modes == null || modes.isEmpty())
		{
			return "";
		}
		return modes.entrySet().stream()
			.filter(entry -> !entry.getKey().isBlank() && entry.getValue() != null)
			.map(entry -> normalizePlayerName(entry.getKey()) + "=" + entry.getValue().name())
			.sorted()
			.collect(Collectors.joining(","));
	}

	private static FaceSwapRenderMode parseTargetRenderMode(String renderMode)
	{
		if (renderMode == null || renderMode.isBlank())
		{
			return null;
		}
		try
		{
			return FaceSwapRenderMode.valueOf(renderMode.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	private static String normalizePlayerName(String name)
	{
		if (name == null)
		{
			return "";
		}
		return name.replace('\u00A0', ' ').trim().toLowerCase(Locale.ROOT);
	}

	private static final class FaceSwapHotkeyListener implements net.runelite.client.input.KeyListener
	{
		private final FaceSwapPlugin plugin;

		private FaceSwapHotkeyListener(FaceSwapPlugin plugin)
		{
			this.plugin = plugin;
		}

		@Override
		public void keyTyped(KeyEvent event)
		{
		}

		@Override
		public void keyPressed(KeyEvent event)
		{
			if (plugin.handleKeyPressed(event))
			{
				event.consume();
			}
		}

		@Override
		public void keyReleased(KeyEvent event)
		{
		}
	}

	private static final class MovementCycleState
	{
		private FaceSwapHead head;
		private String styleId = FaceSwapAssignment.DEFAULT_STYLE_ID;
		private int ticks;
		private int lastX = Integer.MIN_VALUE;
		private int lastY = Integer.MIN_VALUE;
		private boolean moving;
	}

	private static final class Prototype3dInstance
	{
		private final RuneLiteObject object;
		private final String modelKey;
		private AnimationController poseController;
		private AnimationController actionController;
		private int animationFrameOffset;

		private Prototype3dInstance(RuneLiteObject object, String modelKey)
		{
			this.object = object;
			this.modelKey = modelKey;
		}
	}

	private static final class MaskRigInstance
	{
		private final RuneLiteObject object;
		private AnimationController poseController;
		private AnimationController actionController;

		private MaskRigInstance(RuneLiteObject object)
		{
			this.object = object;
		}
	}

	private static int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}

	private static float smoothStep(float value)
	{
		float t = Math.max(0f, Math.min(1f, value));
		return t * t * (3f - 2f * t);
	}

	private static float lerp(float start, float end, float amount)
	{
		return start + (end - start) * amount;
	}

	private static BufferedImage createIcon()
	{
		try (InputStream iconStream = FaceSwapPlugin.class.getResourceAsStream(SIDEPANEL_ICON_RESOURCE))
		{
			if (iconStream != null)
			{
				BufferedImage resourceIcon = ImageIO.read(iconStream);
				if (resourceIcon != null)
				{
					return resourceIcon;
				}
			}
		}
		catch (IOException ex)
		{
			log.warn("Unable to load face swap icon resource", ex);
		}

		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(new Color(24, 24, 24, 230));
			graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
			graphics.setColor(new Color(255, 205, 130));
			graphics.fillOval(4, 3, 8, 8);
			graphics.setColor(Color.WHITE);
			graphics.setStroke(new BasicStroke(2f));
			graphics.drawArc(3, 6, 10, 7, 200, 140);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}
}
