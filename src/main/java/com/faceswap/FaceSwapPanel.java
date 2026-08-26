package com.faceswap;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class FaceSwapPanel extends PluginPanel
{
	private static final Dimension ROW_LABEL_DIMENSION = new Dimension(72, 24);
	private static final int PLAYER_TARGET_LABEL_WIDTH = 56;
	private static final int NPC_TARGET_LABEL_WIDTH = 96;
	private static final Dimension VALUE_FIELD_DIMENSION = new Dimension(44, 24);
	private static final int PICKER_THUMBNAIL_SIZE = 96;
	private static final int PICKER_LABEL_HEIGHT = 20;
	private static final int MODE_THUMBNAIL_WIDTH = 56;
	private static final int MODE_ARTWORK_HEIGHT = 54;
	private static final int MODE_LABEL_HEIGHT = 18;
	private static final Map<FaceSwapHead, BufferedImage> PICKER_THUMBNAIL_CACHE = new ConcurrentHashMap<>();
	private static volatile CompletableFuture<Void> pickerThumbnailPreload = CompletableFuture.completedFuture(null);
	private static final ImageIcon TARGET_PICK_ICON = new ImageIcon(createTargetPickerThumbnail(false));
	private static final ImageIcon TARGET_PICK_ACTIVE_ICON = new ImageIcon(createTargetPickerThumbnail(true));
	private static final ImageIcon THREE_D_MODE_ICON =
		new ImageIcon(createModeThumbnail("/mode_icons/mode_3d.png", FaceSwapRenderMode.THREE_D));
	private static final ImageIcon MASK_MODE_ICON =
		new ImageIcon(createModeThumbnail("/mode_icons/mode_mask.png", FaceSwapRenderMode.MASK));
	private static final ImageIcon WRAPAROUND_MODE_ICON =
		new ImageIcon(createModeThumbnail("/mode_icons/mode_wraparound.png", FaceSwapRenderMode.TWO_D));

	private final FaceSwapPlugin plugin;
	private final JButton selectedHeadPreview = new JButton();
	private final JComboBox<FaceSwapTargetScope> targetScopeBox = new JComboBox<>(new FaceSwapTargetScope[]
	{
		FaceSwapTargetScope.DISABLED,
		FaceSwapTargetScope.SELF,
		FaceSwapTargetScope.FRIENDS,
		FaceSwapTargetScope.CHAT_CHANNEL,
		FaceSwapTargetScope.CLANMATES,
		FaceSwapTargetScope.IGNORE_LIST,
		FaceSwapTargetScope.RADIUS,
		FaceSwapTargetScope.ALL_PLAYERS,
		FaceSwapTargetScope.SPECIFIC_PLAYERS
	});
	private final JComboBox<FaceSwapNpcTargetScope> npcTargetScopeBox =
		new JComboBox<>(FaceSwapNpcTargetScope.values());
	private final JToggleButton threeDModeButton = new JToggleButton(THREE_D_MODE_ICON);
	private final JToggleButton maskModeButton = new JToggleButton(MASK_MODE_ICON);
	private final JToggleButton wraparoundModeButton = new JToggleButton(WRAPAROUND_MODE_ICON);
	private final JSlider qualitySlider = new JSlider(0, 4, 1);
	private final JLabel qualityLabel = new JLabel("Quality");
	private final JPanel qualityControls = columnPanel();
	private final JCheckBox dkModeCheck = new JCheckBox("DK Mode");
	private final JPanel maskWidthControls = columnPanel();
	private final JSlider maskWidthSlider = new JSlider(50, 120, 70);
	private final JTextField maskWidthField = new JTextField(3);
	private final JTextArea targetNamesArea = new JTextArea(5, 18);
	private final JPanel radiusControls = columnPanel();
	private final JSlider radiusSlider = new JSlider(1, 50, 10);
	private final JTextField radiusField = new JTextField(3);
	private final JTextArea npcTargetNamesArea = new JTextArea(5, 18);
	private final JToggleButton pickPlayerButton = new JToggleButton(TARGET_PICK_ICON);
	private final JLabel statusLabel = new JLabel();
	private JPanel targetNamesRow;
	private JPanel npcTargetNamesRow;

	private JDialog activeHeadPicker;
	private boolean openingHeadPicker;
	private boolean refreshing;
	private int lastHeadPickerTabIndex = 1;

	FaceSwapPanel(FaceSwapPlugin plugin, FaceSwapPanelState initialState)
	{
		this.plugin = plugin;
		preloadPickerThumbnailsAsync();

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel content = columnPanel();

		JPanel headSection = sectionPanel("Pick a Character and Target");
		selectedHeadPreview.setPreferredSize(new Dimension(96, 96));
		selectedHeadPreview.setHorizontalAlignment(SwingConstants.CENTER);
		selectedHeadPreview.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		selectedHeadPreview.addActionListener(e -> openHeadPicker());
		pickPlayerButton.setPreferredSize(new Dimension(PICKER_THUMBNAIL_SIZE, PICKER_THUMBNAIL_SIZE));
		pickPlayerButton.setToolTipText("Pick a player or NPC in-game");
		pickPlayerButton.getAccessibleContext().setAccessibleName("Pick a Target");
		pickPlayerButton.addActionListener(e ->
		{
			if (!refreshing)
			{
				plugin.setPickPlayerMode(pickPlayerButton.isSelected());
			}
		});
		JPanel headPickerRow = new JPanel(new GridLayout(1, 2, 8, 0));
		headPickerRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		headPickerRow.add(selectedHeadPreview);
		headPickerRow.add(pickPlayerButton);
		headSection.add(fullWidthRow(headPickerRow));

		ButtonGroup renderModeGroup = new ButtonGroup();
		configureModeButton(threeDModeButton, FaceSwapRenderMode.THREE_D, renderModeGroup);
		configureModeButton(maskModeButton, FaceSwapRenderMode.MASK, renderModeGroup);
		configureModeButton(wraparoundModeButton, FaceSwapRenderMode.TWO_D, renderModeGroup);
		JPanel renderModeRow = new JPanel(new GridLayout(1, 3, 4, 0));
		renderModeRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		renderModeRow.add(maskModeButton);
		renderModeRow.add(threeDModeButton);
		renderModeRow.add(wraparoundModeButton);
		headSection.add(fullWidthRow(renderModeRow));

		qualityLabel.setForeground(Color.WHITE);
		qualityControls.add(labelRow(qualityLabel));
		qualitySlider.setMajorTickSpacing(1);
		qualitySlider.setSnapToTicks(true);
		qualitySlider.setPaintTicks(true);
		qualitySlider.setPaintLabels(true);
		qualitySlider.setBackground(ColorScheme.DARK_GRAY_COLOR);
		qualitySlider.setUI(new BasicSliderUI(qualitySlider)
		{
			@Override
			public void paintTrack(Graphics graphics)
			{
				int centerY = trackRect.y + trackRect.height / 2;
				int selectedX = xPositionForValue(slider.getValue());
				graphics.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				graphics.fillRoundRect(trackRect.x, centerY - 2, trackRect.width, 4, 4, 4);
				graphics.setColor(ColorScheme.BRAND_ORANGE);
				graphics.fillRoundRect(trackRect.x, centerY - 2,
					Math.max(4, selectedX - trackRect.x), 4, 4, 4);
			}

			@Override
			public void paintThumb(Graphics graphics)
			{
				Rectangle thumb = thumbRect;
				graphics.setColor(ColorScheme.BRAND_ORANGE);
				graphics.fillOval(thumb.x, thumb.y, thumb.width, thumb.height);
				graphics.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				graphics.drawOval(thumb.x, thumb.y, thumb.width - 1, thumb.height - 1);
			}
		});
		Hashtable<Integer, JLabel> qualityLabels = new Hashtable<>();
		qualityLabels.put(0, sliderLabel("Low"));
		qualityLabels.put(1, sliderLabel("Medium"));
		qualityLabels.put(2, sliderLabel("High"));
		qualityLabels.put(3, sliderLabel("Ultra"));
		qualityLabels.put(4, sliderLabel("Max"));
		qualitySlider.setLabelTable(qualityLabels);
		qualitySlider.addChangeListener(e ->
		{
			if (!refreshing && !qualitySlider.getValueIsAdjusting())
			{
				plugin.setQualityLevel(qualitySlider.getValue());
			}
		});
		qualityControls.add(fullWidthRow(qualitySlider));
		dkModeCheck.setForeground(Color.WHITE);
		dkModeCheck.setBackground(ColorScheme.DARK_GRAY_COLOR);
		dkModeCheck.addActionListener(e ->
		{
			if (!refreshing)
			{
				plugin.setDkMode(dkModeCheck.isSelected());
			}
		});
		qualityControls.add(fullWidthRow(dkModeCheck));
		headSection.add(qualityControls);

		maskWidthSlider.setBackground(ColorScheme.DARK_GRAY_COLOR);
		maskWidthSlider.setUI(new BasicSliderUI(maskWidthSlider)
		{
			@Override
			public void paintTrack(Graphics graphics)
			{
				int centerY = trackRect.y + trackRect.height / 2;
				int selectedX = xPositionForValue(slider.getValue());
				graphics.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				graphics.fillRoundRect(trackRect.x, centerY - 2, trackRect.width, 4, 4, 4);
				graphics.setColor(ColorScheme.BRAND_ORANGE);
				graphics.fillRoundRect(trackRect.x, centerY - 2,
					Math.max(4, selectedX - trackRect.x), 4, 4, 4);
			}

			@Override
			public void paintThumb(Graphics graphics)
			{
				Rectangle thumb = thumbRect;
				graphics.setColor(ColorScheme.BRAND_ORANGE);
				graphics.fillOval(thumb.x, thumb.y, thumb.width, thumb.height);
				graphics.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				graphics.drawOval(thumb.x, thumb.y, thumb.width - 1, thumb.height - 1);
			}
		});
		maskWidthField.setPreferredSize(VALUE_FIELD_DIMENSION);
		maskWidthField.setMinimumSize(VALUE_FIELD_DIMENSION);
		maskWidthField.setMaximumSize(VALUE_FIELD_DIMENSION);
		maskWidthField.setHorizontalAlignment(JTextField.CENTER);
		maskWidthSlider.addChangeListener(e ->
		{
			if (!refreshing)
			{
				int value = maskWidthSlider.getValue();
				maskWidthField.setText(Integer.toString(value));
				plugin.setMaskWidth(value);
			}
		});
		maskWidthField.addActionListener(e -> commitMaskWidthField());
		maskWidthField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				commitMaskWidthField();
			}
		});
		JPanel maskWidthRow = new JPanel(new BorderLayout(8, 0));
		maskWidthRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		maskWidthRow.add(maskWidthSlider, BorderLayout.CENTER);
		maskWidthRow.add(maskWidthField, BorderLayout.EAST);
		maskWidthControls.add(row("Mask Width", maskWidthRow));
		headSection.add(maskWidthControls);
		content.add(headSection);

		JPanel targetSection = sectionPanel("Targets");
		targetScopeBox.addActionListener(e ->
		{
			if (!refreshing)
			{
				plugin.setTargetScope((FaceSwapTargetScope) targetScopeBox.getSelectedItem());
			}
		});
		targetSection.add(row("Apply To", targetScopeBox, PLAYER_TARGET_LABEL_WIDTH));
		configureRadiusControls();
		targetSection.add(radiusControls);

		targetNamesArea.setLineWrap(true);
		targetNamesArea.setWrapStyleWord(true);
		targetNamesArea.setToolTipText("One player name per line; commas are also accepted");
		targetNamesArea.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				plugin.setTargetNames(targetNamesArea.getText());
			}
		});
		targetNamesRow = fullWidthRow(new JScrollPane(targetNamesArea));
		targetSection.add(targetNamesRow);

		npcTargetScopeBox.addActionListener(e ->
		{
			if (!refreshing)
			{
				plugin.setNpcTargetScope((FaceSwapNpcTargetScope) npcTargetScopeBox.getSelectedItem());
			}
		});
		targetSection.add(row("Apply To NPCs", npcTargetScopeBox, NPC_TARGET_LABEL_WIDTH));
		npcTargetNamesArea.setLineWrap(true);
		npcTargetNamesArea.setWrapStyleWord(true);
		npcTargetNamesArea.setToolTipText("One NPC name per line; commas are also accepted");
		npcTargetNamesArea.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				plugin.setNpcTargetNames(npcTargetNamesArea.getText());
			}
		});
		npcTargetNamesRow = fullWidthRow(new JScrollPane(npcTargetNamesArea));
		targetSection.add(npcTargetNamesRow);
		content.add(targetSection);

		JPanel statusSection = sectionPanel("Status");
		statusLabel.setForeground(Color.WHITE);
		statusSection.add(labelRow(statusLabel));
		content.add(statusSection);

		add(content, BorderLayout.NORTH);
		refreshState(initialState);
	}

	void refreshState(FaceSwapPanelState state)
	{
		if (state == null)
		{
			return;
		}

		refreshing = true;
		try
		{
			selectedHeadPreview.setIcon(new ImageIcon(createPickerThumbnail(state.selectedHead)));
			selectedHeadPreview.setToolTipText(state.selectedHead.toString());
			updateModeButtons(state.renderMode);
			qualitySlider.setValue(state.qualityLevel);
			qualityControls.setVisible(state.renderMode == FaceSwapRenderMode.THREE_D);
			dkModeCheck.setSelected(state.dkMode);
			maskWidthControls.setVisible(state.renderMode == FaceSwapRenderMode.MASK);
			if (!maskWidthSlider.getValueIsAdjusting())
			{
				maskWidthSlider.setValue(state.maskWidth);
			}
			if (!maskWidthField.isFocusOwner()
				&& !maskWidthField.getText().equals(Integer.toString(state.maskWidth)))
			{
				maskWidthField.setText(Integer.toString(state.maskWidth));
			}
			targetScopeBox.setSelectedItem(state.targetScope);
			boolean specificPlayers = state.targetScope == FaceSwapTargetScope.SPECIFIC_PLAYERS;
			boolean radiusPlayers = state.targetScope == FaceSwapTargetScope.RADIUS;
			radiusControls.setVisible(radiusPlayers);
			if (!radiusSlider.getValueIsAdjusting())
			{
				radiusSlider.setValue(state.targetRadius);
			}
			if (!radiusField.isFocusOwner())
			{
				radiusField.setText(Integer.toString(state.targetRadius));
			}
			targetNamesRow.setVisible(specificPlayers || radiusPlayers);
			targetNamesArea.setEditable(specificPlayers);
			if (!targetNamesArea.isFocusOwner() && !targetNamesArea.getText().equals(state.targetNames))
			{
				targetNamesArea.setText(state.targetNames);
			}
			npcTargetScopeBox.setSelectedItem(state.npcTargetScope);
			boolean specificNpcs = state.npcTargetScope == FaceSwapNpcTargetScope.SPECIFIC_NPCS;
			npcTargetNamesRow.setVisible(specificNpcs);
			if (!npcTargetNamesArea.isFocusOwner() && !npcTargetNamesArea.getText().equals(state.npcTargetNames))
			{
				npcTargetNamesArea.setText(state.npcTargetNames);
			}
			pickPlayerButton.setSelected(state.pickPlayerMode);
			pickPlayerButton.setIcon(state.pickPlayerMode ? TARGET_PICK_ACTIVE_ICON : TARGET_PICK_ICON);
			pickPlayerButton.setToolTipText(state.pickPlayerMode
				? "Click a highlighted player or NPC"
				: "Pick a player or NPC in-game");
			statusLabel.setText(html(state.statusText));
		}
		finally
		{
			refreshing = false;
		}
		revalidate();
		repaint();
	}

	private void configureModeButton(
		JToggleButton button,
		FaceSwapRenderMode renderMode,
		ButtonGroup buttonGroup)
	{
		buttonGroup.add(button);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setFocusable(false);
		button.setFocusPainted(false);
		button.setContentAreaFilled(true);
		button.setOpaque(true);
		button.setToolTipText("Use " + renderMode + " style");
		button.getAccessibleContext().setAccessibleName(renderMode + " style");
		button.addActionListener(e ->
		{
			if (!refreshing)
			{
				updateModeButtons(renderMode);
				plugin.setRenderMode(renderMode);
			}
		});
	}

	private void updateModeButtons(FaceSwapRenderMode selectedMode)
	{
		updateModeButton(threeDModeButton, selectedMode == FaceSwapRenderMode.THREE_D);
		updateModeButton(maskModeButton, selectedMode == FaceSwapRenderMode.MASK);
		updateModeButton(wraparoundModeButton, selectedMode == FaceSwapRenderMode.TWO_D);
	}

	private static void updateModeButton(JToggleButton button, boolean selected)
	{
		button.setSelected(selected);
		button.setBackground(selected ? new Color(82, 63, 20) : ColorScheme.DARKER_GRAY_COLOR);
		button.setBorder(BorderFactory.createLineBorder(
			selected ? ColorScheme.BRAND_ORANGE : ColorScheme.MEDIUM_GRAY_COLOR,
			selected ? 3 : 1));
	}

	void disposePanel()
	{
		closeActiveHeadPicker();
	}

	private void commitMaskWidthField()
	{
		if (refreshing)
		{
			return;
		}

		try
		{
			int value = Math.max(50, Math.min(120, Integer.parseInt(maskWidthField.getText().trim())));
			maskWidthSlider.setValue(value);
			plugin.setMaskWidth(value);
		}
		catch (NumberFormatException ex)
		{
			maskWidthField.setText(Integer.toString(maskWidthSlider.getValue()));
		}
	}

	private void configureRadiusControls()
	{
		radiusSlider.setBackground(ColorScheme.DARK_GRAY_COLOR);
		radiusSlider.setUI(new BasicSliderUI(radiusSlider)
		{
			@Override
			public void paintTrack(Graphics graphics)
			{
				int centerY = trackRect.y + trackRect.height / 2;
				int selectedX = xPositionForValue(slider.getValue());
				graphics.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				graphics.fillRoundRect(trackRect.x, centerY - 2, trackRect.width, 4, 4, 4);
				graphics.setColor(ColorScheme.BRAND_ORANGE);
				graphics.fillRoundRect(trackRect.x, centerY - 2,
					Math.max(4, selectedX - trackRect.x), 4, 4, 4);
			}

			@Override
			public void paintThumb(Graphics graphics)
			{
				Rectangle thumb = thumbRect;
				graphics.setColor(ColorScheme.BRAND_ORANGE);
				graphics.fillOval(thumb.x, thumb.y, thumb.width, thumb.height);
				graphics.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				graphics.drawOval(thumb.x, thumb.y, thumb.width - 1, thumb.height - 1);
			}
		});
		radiusField.setPreferredSize(VALUE_FIELD_DIMENSION);
		radiusField.setHorizontalAlignment(JTextField.CENTER);
		radiusSlider.addChangeListener(e ->
		{
			if (!refreshing)
			{
				int value = radiusSlider.getValue();
				radiusField.setText(Integer.toString(value));
				plugin.setTargetRadius(value);
			}
		});
		radiusField.addActionListener(e -> commitRadiusField());
		radiusField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				commitRadiusField();
			}
		});
		JPanel radiusRow = new JPanel(new BorderLayout(8, 0));
		radiusRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		radiusRow.add(radiusSlider, BorderLayout.CENTER);
		radiusRow.add(radiusField, BorderLayout.EAST);
		radiusControls.add(row("Radius", radiusRow));
	}

	private void commitRadiusField()
	{
		if (refreshing)
		{
			return;
		}
		try
		{
			int value = Math.max(1, Math.min(50, Integer.parseInt(radiusField.getText().trim())));
			radiusSlider.setValue(value);
			plugin.setTargetRadius(value);
		}
		catch (NumberFormatException ex)
		{
			radiusField.setText(Integer.toString(radiusSlider.getValue()));
		}
	}

	private void openHeadPicker()
	{
		if (openingHeadPicker)
		{
			return;
		}

		JDialog existingDialog = activeHeadPicker;
		if (existingDialog != null && existingDialog.isShowing())
		{
			existingDialog.toFront();
			existingDialog.requestFocus();
			return;
		}

		openingHeadPicker = true;
		closeActiveHeadPicker();

		try
		{
			JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Choose Face Swap Character");
			activeHeadPicker = dialog;

			dialog.setContentPane(createLoadingHeadPickerContent());
			dialog.getRootPane().registerKeyboardAction(
				event -> plugin.handleEscapePressed(),
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
			dialog.pack();
			dialog.setLocationRelativeTo(this);
			dialog.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent event)
				{
					clearActiveHeadPicker(dialog);
				}

				@Override
				public void windowClosed(WindowEvent event)
				{
					clearActiveHeadPicker(dialog);
				}
			});
			dialog.setVisible(true);
			preloadPickerThumbnailsAsync().thenRun(() -> SwingUtilities.invokeLater(() ->
			{
				if (activeHeadPicker != dialog)
				{
					return;
				}

				dialog.setContentPane(createHeadPickerContent());
				dialog.pack();
				dialog.setLocationRelativeTo(this);
				dialog.revalidate();
				dialog.repaint();
			}));
		}
		finally
		{
			openingHeadPicker = false;
		}
	}

	private JPanel headPickerGrid(FaceSwapHeadCategory category)
	{
		JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
		grid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		grid.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), category.toString()),
			BorderFactory.createEmptyBorder(8, 8, 8, 8)));

		int availableHeads = 0;
		for (FaceSwapHead head : orderedHeadsForPicker(category))
		{
			if (!isHeadForPickerCategory(head, category) || !plugin.isHeadAvailable(head))
			{
				continue;
			}

			JToggleButton button = new JToggleButton(new ImageIcon(createPickerThumbnail(head)));
			button.setSelected(head == plugin.getSelectedHead());
			button.setToolTipText(head.toString());
			button.setPreferredSize(new Dimension(108, 108));
			button.addActionListener(e ->
			{
				plugin.setSelectedHead(head);
				closeActiveHeadPicker();
			});
			grid.add(button);
			availableHeads++;
		}

		if (availableHeads == 0)
		{
			JLabel emptyLabel = new JLabel("No characters available yet", SwingConstants.CENTER);
			emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			grid.add(emptyLabel);
		}
		return grid;
	}

	private JPanel customHeadPicker()
	{
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), "Custom Images"),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)));

		JPanel controls = new JPanel(new BorderLayout(8, 0));
		controls.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JButton browseButton = new JButton("Browse...");
		JTextArea status = new JTextArea(
			"Choose an image from your computer. It will be copied into Face Swap storage. Preferred: transparent 512x512 PNG. JPG/BMP accepted; background removal is not automatic.");
		status.setLineWrap(true);
		status.setWrapStyleWord(true);
		status.setRows(2);
		status.setEditable(false);
		status.setFocusable(false);
		status.setOpaque(false);
		status.setBorder(BorderFactory.createEmptyBorder());
		status.setMinimumSize(new Dimension(0, 36));
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		controls.add(browseButton, BorderLayout.WEST);
		controls.add(status, BorderLayout.CENTER);
		panel.add(controls, BorderLayout.NORTH);

		JPanel recents = new JPanel(new GridLayout(0, 2, 8, 8));
		recents.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (FaceSwapCustomImageStore.Entry entry : plugin.getCustomImages())
		{
			BufferedImage image = plugin.getCustomImage(entry.id);
			JButton imageButton = new JButton(new ImageIcon(createCustomThumbnail(image)));
			imageButton.setToolTipText(entry.name);
			imageButton.setPreferredSize(new Dimension(108, 108));
			imageButton.addActionListener(event ->
			{
				plugin.selectCustomImage(entry.id);
				closeActiveHeadPicker();
			});
			recents.add(imageButton);
		}
		if (plugin.getCustomImages().isEmpty())
		{
			JLabel empty = new JLabel("No local images imported yet", SwingConstants.CENTER);
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			recents.add(empty);
		}
		panel.add(recents, BorderLayout.CENTER);

		JButton clearButton = new JButton("Clear");
		clearButton.setToolTipText("Remove all imported custom images and clear this panel");
		clearButton.addActionListener(event -> plugin.clearCustomImages(this::refreshActiveHeadPicker));
		panel.add(clearButton, BorderLayout.SOUTH);
		browseButton.addActionListener(event -> browseForCustomImage(status));
		return panel;
	}

	private void browseForCustomImage(JTextArea status)
	{
		JFileChooser chooser = new JFileChooser(plugin.getCustomImageDirectory().toFile());
		chooser.setDialogTitle("Choose a custom face image");
		chooser.setFileFilter(new FileNameExtensionFilter("Image files (PNG, JPG, JPEG, BMP)", "png", "jpg", "jpeg", "bmp"));
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		status.setText("Importing...");
		plugin.importCustomImage(chooser.getSelectedFile().toPath(), message ->
		{
			status.setText(message);
			refreshActiveHeadPicker();
		});
	}

	private JPanel createHeadPickerContent()
	{
		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		if (plugin.useTabbedHeadPicker())
		{
			JTabbedPane tabs = new JTabbedPane();
			tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
			tabs.addTab("Fictional",
				headPickerScrollPane(headPickerGrid(FaceSwapHeadCategory.FICTIONAL_CHARACTER)));
			tabs.addTab(FaceSwapHeadCategory.CONTENT_CREATOR.toString(),
				headPickerScrollPane(headPickerGrid(FaceSwapHeadCategory.CONTENT_CREATOR)));
			tabs.addTab(FaceSwapHeadCategory.EMOJI.toString(),
				headPickerScrollPane(headPickerGrid(FaceSwapHeadCategory.EMOJI)));
			tabs.addTab(FaceSwapHeadCategory.CUSTOM.toString(),
				headPickerScrollPane(customHeadPicker()));
			tabs.addChangeListener(event -> lastHeadPickerTabIndex = tabs.getSelectedIndex());
			tabs.setSelectedIndex(Math.max(0, Math.min(lastHeadPickerTabIndex, tabs.getTabCount() - 1)));
			content.add(tabs, BorderLayout.CENTER);
		}
		else
		{
			JPanel columns = new JPanel(new GridLayout(1, 4, 12, 0));
			columns.setBackground(ColorScheme.DARK_GRAY_COLOR);
			columns.add(headPickerScrollPane(headPickerGrid(FaceSwapHeadCategory.FICTIONAL_CHARACTER)));
			columns.add(headPickerScrollPane(headPickerGrid(FaceSwapHeadCategory.CONTENT_CREATOR)));
			columns.add(headPickerScrollPane(headPickerGrid(FaceSwapHeadCategory.EMOJI)));
			columns.add(headPickerScrollPane(customHeadPicker()));
			content.add(columns, BorderLayout.CENTER);
		}
		return content;
	}

	private static JPanel createLoadingHeadPickerContent()
	{
		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel loadingLabel = new JLabel("Loading characters...", SwingConstants.CENTER);
		loadingLabel.setForeground(Color.WHITE);
		content.add(loadingLabel, BorderLayout.CENTER);
		content.setPreferredSize(new Dimension(360, 180));
		return content;
	}

	static List<FaceSwapHead> orderedHeadsForPicker(FaceSwapHeadCategory category)
	{
		List<FaceSwapHead> ordered = new ArrayList<>();
		if (category == FaceSwapHeadCategory.CONTENT_CREATOR)
		{
			addAll(ordered,
				// FaceSwapHead.ODABLOCK,
				FaceSwapHead.SARDACO,
				FaceSwapHead.SKILL_SPECS,
				FaceSwapHead.TASTYLIFE,
				FaceSwapHead.TPAPASLICE,
				FaceSwapHead.PRISONJOE,
				FaceSwapHead.ZECOOKIES,
				FaceSwapHead.ALFIE,
				FaceSwapHead.KING_CONDOR,
				FaceSwapHead.DEARLOLA,
				FaceSwapHead.ELIOP14,
				FaceSwapHead.JILLYFISH,
				FaceSwapHead.BEGGAR,
				FaceSwapHead.GRIM,
				FaceSwapHead.ASIAN_ANDY,

				FaceSwapHead.SARDACO_OSRS,
				FaceSwapHead.SKILL_SPECS_OSRS,
				FaceSwapHead.TASTYLIFE_OSRS,
				FaceSwapHead.TPAPASLICE_OSRS,
				FaceSwapHead.PRISONJOE_OSRS,
				FaceSwapHead.ZECOOKIES_OSRS,
				FaceSwapHead.ALFIE_OSRS,
				FaceSwapHead.FOX_OSRS,
				FaceSwapHead.KING_CONDOR_OSRS,
				FaceSwapHead.DEARLOLA_OSRS,
				FaceSwapHead.ELIOP14_OSRS,
				FaceSwapHead.JILLYFISH_OSRS,
				FaceSwapHead.BEGGAR_OSRS,
				FaceSwapHead.GRIM_OSRS,
				FaceSwapHead.ASIAN_ANDY_OSRS
				);
		}
		else if (category == FaceSwapHeadCategory.FICTIONAL_CHARACTER)
		{
			addAll(ordered,
				FaceSwapHead.PUG,
				FaceSwapHead.HORSE,
				FaceSwapHead.RABBIT,
				FaceSwapHead.PENGUIN,
				FaceSwapHead.CAT,
				FaceSwapHead.MONKEY_PHOTO,
				// FaceSwapHead.CLOWN,
				// FaceSwapHead.ORANGE_PARKA,
				FaceSwapHead.CLASSIC_ADVENTURER
				// FaceSwapHead.PURPLE_DINOSAUR,
				// FaceSwapHead.SPACE_MARINE,
				// FaceSwapHead.HALFLING,
				// FaceSwapHead.BANDICOOT,
				// FaceSwapHead.AGENT,
				// FaceSwapHead.MONKEY,
				// FaceSwapHead.CHOSEN_ONE,
				// FaceSwapHead.MARTIAL_ARTIST,
				// FaceSwapHead.BOSS
				);
		}
		else if (category == FaceSwapHeadCategory.EMOJI)
		{
			addAll(ordered,
				FaceSwapHead.SMILEY,
				FaceSwapHead.HEART_EYES,
				FaceSwapHead.POOP,
				FaceSwapHead.COOL,
				FaceSwapHead.ANGRY,
				FaceSwapHead.SAD,
				FaceSwapHead.SURPRISED,
				FaceSwapHead.HEART,
				FaceSwapHead.ROBOT);
		}

		for (FaceSwapHead head : FaceSwapHead.values())
		{
			if (category != FaceSwapHeadCategory.EMOJI
				&& head.getCategory() == category && !ordered.contains(head))
			{
				ordered.add(head);
			}
		}
		return ordered;
	}

	private static boolean isHeadForPickerCategory(FaceSwapHead head, FaceSwapHeadCategory category)
	{
		if (category == FaceSwapHeadCategory.CONTENT_CREATOR)
		{
			return head.getCategory() == FaceSwapHeadCategory.CONTENT_CREATOR
				|| head.getCategory() == FaceSwapHeadCategory.CONTENT_CREATOR_3D;
		}

		return head.getCategory() == category;
	}

	private static void addAll(List<FaceSwapHead> heads, FaceSwapHead... entries)
	{
		for (FaceSwapHead entry : entries)
		{
			heads.add(entry);
		}
	}

	private static JScrollPane headPickerScrollPane(JPanel grid)
	{
		JPanel viewportContent = new JPanel(new BorderLayout());
		viewportContent.setBackground(ColorScheme.DARK_GRAY_COLOR);
		viewportContent.add(grid, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(
			viewportContent,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(360, 360));
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		return scrollPane;
	}

	private void clearActiveHeadPicker(JDialog dialog)
	{
		if (activeHeadPicker == dialog)
		{
			activeHeadPicker = null;
		}
	}

	boolean closeActiveHeadPickerIfOpen()
	{
		JDialog dialog = activeHeadPicker;
		activeHeadPicker = null;
		if (dialog == null)
		{
			return false;
		}

		dialog.setVisible(false);
		dialog.dispose();
		return true;
	}

	private void closeActiveHeadPicker()
	{
		closeActiveHeadPickerIfOpen();
	}

	private static JPanel row(String labelText, Component component)
	{
		return row(labelText, component, ROW_LABEL_DIMENSION.width);
	}

	private static JPanel row(String labelText, Component component, int labelWidth)
	{
		JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

		JLabel label = new JLabel(labelText);
		label.setForeground(Color.WHITE);
		label.setPreferredSize(new Dimension(labelWidth, ROW_LABEL_DIMENSION.height));
		panel.add(label, BorderLayout.WEST);
		panel.add(component, BorderLayout.CENTER);
		return panel;
	}

	private static JPanel fullWidthRow(Component component)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		panel.add(component, BorderLayout.CENTER);
		return panel;
	}

	private static JPanel labelRow(JLabel label)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}

	private static JLabel sliderLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(Color.WHITE);
		return label;
	}

	static BufferedImage createPickerThumbnail(FaceSwapHead head)
	{
		return PICKER_THUMBNAIL_CACHE.computeIfAbsent(head, FaceSwapPanel::createPickerThumbnailUncached);
	}

	private static BufferedImage createPickerThumbnailUncached(FaceSwapHead head)
	{
		BufferedImage source = FaceSwapHeadImages.get(head, FaceSwapHeadDirection.FRONT);
		BufferedImage thumbnail = new BufferedImage(
			PICKER_THUMBNAIL_SIZE, PICKER_THUMBNAIL_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = thumbnail.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			double scale = Math.min(
				(double) PICKER_THUMBNAIL_SIZE / Math.max(1, source.getWidth()),
				(double) PICKER_THUMBNAIL_SIZE / Math.max(1, source.getHeight()));
			int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
			int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
			int x = (PICKER_THUMBNAIL_SIZE - width) / 2;
			int y = (PICKER_THUMBNAIL_SIZE - height) / 2;
			graphics.drawImage(source, x, y, width, height, null);

			if (head.getCategory() == FaceSwapHeadCategory.EMOJI)
			{
				return thumbnail;
			}

			if (head.isDebugOnly())
			{
				drawPickerLabel(graphics, "(Demo)", 0);
			}

			int labelY = PICKER_THUMBNAIL_SIZE - PICKER_LABEL_HEIGHT;
			drawPickerLabel(graphics, head.toString(), labelY);
		}
		finally
		{
			graphics.dispose();
		}
		return thumbnail;
	}

	private void refreshActiveHeadPicker()
	{
		if (activeHeadPicker == null)
		{
			return;
		}
		activeHeadPicker.setContentPane(createHeadPickerContent());
		activeHeadPicker.pack();
		activeHeadPicker.setLocationRelativeTo(this);
		activeHeadPicker.revalidate();
		activeHeadPicker.repaint();
	}

	private static BufferedImage createCustomThumbnail(BufferedImage source)
	{
		if (source == null)
		{
			return createPickerThumbnailUncached(FaceSwapHead.CUSTOM);
		}
		BufferedImage thumbnail = new BufferedImage(
			PICKER_THUMBNAIL_SIZE, PICKER_THUMBNAIL_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = thumbnail.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			double scale = Math.min(
				(double) PICKER_THUMBNAIL_SIZE / Math.max(1, source.getWidth()),
				(double) PICKER_THUMBNAIL_SIZE / Math.max(1, source.getHeight()));
			int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
			int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
			graphics.drawImage(source,
				(PICKER_THUMBNAIL_SIZE - width) / 2,
				(PICKER_THUMBNAIL_SIZE - height) / 2,
				width, height, null);
		}
		finally
		{
			graphics.dispose();
		}
		return thumbnail;
	}

	private static CompletableFuture<Void> preloadPickerThumbnailsAsync()
	{
		CompletableFuture<Void> preload = pickerThumbnailPreload;
		if (!preload.isDone())
		{
			return preload;
		}

		synchronized (FaceSwapPanel.class)
		{
			preload = pickerThumbnailPreload;
			if (!preload.isDone())
			{
				return preload;
			}

			pickerThumbnailPreload = CompletableFuture.runAsync(() ->
			{
				for (FaceSwapHead head : FaceSwapHead.values())
				{
					createPickerThumbnail(head);
				}
			});
			return pickerThumbnailPreload;
		}
	}

	private static void drawPickerLabel(Graphics2D graphics, String text, int labelY)
	{
		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, labelY, PICKER_THUMBNAIL_SIZE, PICKER_LABEL_HEIGHT);
		Font font = new Font(Font.SANS_SERIF, Font.BOLD, 12);
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();
		while (metrics.stringWidth(text) > PICKER_THUMBNAIL_SIZE - 8 && font.getSize() > 9)
		{
			font = font.deriveFont((float) font.getSize() - 1f);
			graphics.setFont(font);
			metrics = graphics.getFontMetrics();
		}
		graphics.setColor(Color.WHITE);
		int textX = (PICKER_THUMBNAIL_SIZE - metrics.stringWidth(text)) / 2;
		int textY = labelY + (PICKER_LABEL_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();
		graphics.drawString(text, textX, textY);
	}

	private static BufferedImage createTargetPickerThumbnail(boolean active)
	{
		BufferedImage thumbnail = new BufferedImage(
			PICKER_THUMBNAIL_SIZE, PICKER_THUMBNAIL_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = thumbnail.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(active ? new Color(92, 75, 20) : ColorScheme.DARKER_GRAY_COLOR);
			graphics.fillRect(0, 0, PICKER_THUMBNAIL_SIZE, PICKER_THUMBNAIL_SIZE);

			int centerX = PICKER_THUMBNAIL_SIZE / 2;
			int centerY = (PICKER_THUMBNAIL_SIZE - PICKER_LABEL_HEIGHT) / 2;
			int[] diameters = {54, 40, 26, 12};
			Color[] colors = {
				new Color(245, 245, 235),
				new Color(194, 52, 43),
				new Color(245, 245, 235),
				active ? new Color(255, 220, 35) : new Color(194, 52, 43)
			};
			for (int index = 0; index < diameters.length; index++)
			{
				int diameter = diameters[index];
				graphics.setColor(colors[index]);
				graphics.fillOval(centerX - diameter / 2, centerY - diameter / 2, diameter, diameter);
			}
			graphics.setColor(active ? new Color(255, 220, 35) : ColorScheme.LIGHT_GRAY_COLOR);
			graphics.setStroke(new BasicStroke(2f));
			graphics.drawOval(centerX - 29, centerY - 29, 58, 58);

			int labelY = PICKER_THUMBNAIL_SIZE - PICKER_LABEL_HEIGHT;
			graphics.setColor(Color.BLACK);
			graphics.fillRect(0, labelY, PICKER_THUMBNAIL_SIZE, PICKER_LABEL_HEIGHT);
			graphics.setColor(Color.WHITE);
			graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
			FontMetrics metrics = graphics.getFontMetrics();
			String label = "Pick a Target";
			int textX = (PICKER_THUMBNAIL_SIZE - metrics.stringWidth(label)) / 2;
			int textY = labelY + (PICKER_LABEL_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();
			graphics.drawString(label, textX, textY);
		}
		finally
		{
			graphics.dispose();
		}
		return thumbnail;
	}

	static BufferedImage createModeThumbnail(String resourcePath, FaceSwapRenderMode renderMode)
	{
		BufferedImage artwork = loadModeArtwork(resourcePath);
		BufferedImage thumbnail = new BufferedImage(
			MODE_THUMBNAIL_WIDTH,
			MODE_ARTWORK_HEIGHT + MODE_LABEL_HEIGHT,
			BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = thumbnail.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setColor(ColorScheme.DARKER_GRAY_COLOR);
			graphics.fillRect(0, 0, MODE_THUMBNAIL_WIDTH, MODE_ARTWORK_HEIGHT);
			graphics.drawImage(artwork, 1, 0, MODE_THUMBNAIL_WIDTH - 2, MODE_ARTWORK_HEIGHT, null);

			graphics.setColor(Color.BLACK);
			graphics.fillRect(0, MODE_ARTWORK_HEIGHT, MODE_THUMBNAIL_WIDTH, MODE_LABEL_HEIGHT);
			Font font = new Font(Font.SANS_SERIF, Font.BOLD, 10);
			graphics.setFont(font);
			FontMetrics metrics = graphics.getFontMetrics();
			String label = renderMode.toString();
			while (metrics.stringWidth(label) > MODE_THUMBNAIL_WIDTH - 4 && font.getSize() > 7)
			{
				font = font.deriveFont((float) font.getSize() - 1f);
				graphics.setFont(font);
				metrics = graphics.getFontMetrics();
			}
			graphics.setColor(Color.WHITE);
			int textX = (MODE_THUMBNAIL_WIDTH - metrics.stringWidth(label)) / 2;
			int textY = MODE_ARTWORK_HEIGHT
				+ (MODE_LABEL_HEIGHT - metrics.getHeight()) / 2
				+ metrics.getAscent();
			graphics.drawString(label, textX, textY);
		}
		finally
		{
			graphics.dispose();
		}
		return thumbnail;
	}

	private static BufferedImage loadModeArtwork(String resourcePath)
	{
		try (InputStream input = FaceSwapPanel.class.getResourceAsStream(resourcePath))
		{
			if (input != null)
			{
				BufferedImage artwork = ImageIO.read(input);
				if (artwork != null)
				{
					return artwork;
				}
			}
		}
		catch (IOException ignored)
		{
			// The fallback below keeps the panel usable if a packaged icon cannot be decoded.
		}
		return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	}

	private static JPanel columnPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		return panel;
	}

	private static JPanel sectionPanel(String title)
	{
		JPanel panel = columnPanel();
		Border line = BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createTitledBorder(line, title),
			BorderFactory.createEmptyBorder(8, 8, 4, 8)));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		return panel;
	}

	private static String html(String text)
	{
		String escaped = text
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\n", "<br>");
		return "<html><div style='width:100%'>" + escaped + "</div></html>";
	}
}
