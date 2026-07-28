package com.faceswap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class FaceSwapOverlay extends Overlay
{
	private static final int MAX_DEBUG_POINTS = 80;
	private static final float HEAD_WIDTH_RATIO = 0.50f;
	private static final float HEAD_DEPTH_RATIO = 0.50f;
	private static final float PLAYER_MASK_MAX_HALF_WIDTH = 32f;
	private static final float PLAYER_MASK_MAX_DEPTH = 32f;
	private static final float ANIMATED_HEAD_CENTER_LIMIT = 40f;
	private static final float MASK_BINDING_MAX_VERTICAL_DRIFT = 36f;
	private static final float MASK_TRANSIENT_MAX_CENTER_DISTANCE = 128f;
	private static final float MASK_RIG_MAX_CENTER_DISTANCE = 192f;
	private static final float MASK_RIG_HEAD_REGION_HEIGHT = 72f;
	private static final float MASK_RIG_HEAD_HALF_WIDTH = 42f;
	private static final float MASK_RIG_HEAD_DEPTH = 42f;
	private static final float EQUIPPED_ACTION_MASK_PITCH_DEGREES = 7f;
	private static final float HELMET_MASK_BINDING_CORE_RADIUS = 28f;
	private static final int HELMET_MASK_BINDING_MIN_CORE_VERTICES = 12;
	private static final float SHIELD_WIDTH_SCALE = 1.24f;
	private static final float SHIELD_DEPTH_SCALE = 1.18f;
	private static final float SHIELD_TOP_PADDING = 12f;
	private static final float SHIELD_BOTTOM_PADDING = 14f;
	private static final float HEAD_VERTEX_MARGIN = 2f;
	private static final float TEXTURE_VERTICAL_RANGE = 0.84f;
	private static final int HELMET_OCCLUSION_EXPANSION = 1;
	private static final double WRAP_LIFT_REFERENCE_PROJECTED_HEIGHT = 420d;
	private static final int NO_APPEARANCE_FINGERPRINT = Integer.MIN_VALUE;
	private static final Color TONGUE_FILL = new Color(226, 84, 116, 235);
	private static final Color TONGUE_OUTLINE = new Color(118, 25, 49, 235);
	private static final Color INNER_EAR_FILL = new Color(255, 180, 208, 235);
	private static final Color OUTER_EAR_FILL = new Color(248, 248, 248, 235);
	private static final Color EAR_OUTLINE = new Color(46, 46, 46, 235);

	private final Client client;
	private final FaceSwapPlugin plugin;
	private final Map<Actor, MaskHeadBinding> maskHeadBindings = new WeakHashMap<>();
	private final Map<Actor, MaskHeadPose> maskHeadPoses = new WeakHashMap<>();
	private final Map<Actor, MaskHeadBinding> maskRigBindings = new WeakHashMap<>();
	private final Map<Actor, MaskHeadPose> maskRigHeadPoses = new WeakHashMap<>();
	private final MaskPoseTracker animatedRigMaskPoseTracker = this::getAnimatedRigMaskHeadPose;
	private final MaskPoseTracker mergedModelMaskPoseTracker = this::getMergedModelMaskHeadPose;

	@Inject
	FaceSwapOverlay(Client client, FaceSwapPlugin plugin)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN || isBlockedByInterface())
		{
			return null;
		}

		renderPickTargetOutline(graphics);
		if (plugin.isPrototype3dEnabled())
		{
			return null;
		}

		for (Player player : client.getPlayers())
		{
			FaceSwapHead assignedHead = plugin.getAssignedHead(player);
			if (assignedHead == null)
			{
				continue;
			}

			FaceSwapRenderMode renderMode = plugin.getRenderMode();
			if (renderMode == FaceSwapRenderMode.TWO_D && plugin.hasHeadgear(player))
			{
				continue;
			}
			if (renderMode != FaceSwapRenderMode.MASK && renderEquipmentDebugIfHidden(graphics, player))
			{
				continue;
			}
			if (renderMode == FaceSwapRenderMode.TWO_D && renderProjectedHeadTriangles(graphics, player, assignedHead))
			{
				renderEmoteAccessories(graphics, player);
				if (plugin.isDebugProjection())
				{
					renderProjectionDebug(graphics, player);
				}
				continue;
			}
			if (renderMode == FaceSwapRenderMode.MASK)
			{
				renderProjectedFaceMask(graphics, player, assignedHead);
				continue;
			}

			FaceSwapHeadDirection direction = getHeadDirection(player);
			BufferedImage headImage = FaceSwapHeadImages.get(assignedHead, direction);
			renderBillboard(graphics, player, headImage);
			renderEmoteAccessories(graphics, player);
			if (plugin.isDebugProjection())
			{
				renderProjectionDebug(graphics, player);
			}
		}
		for (NPC npc : client.getNpcs())
		{
			FaceSwapHead assignedHead = plugin.getAssignedHead(npc);
			if (assignedHead == null)
			{
				continue;
			}
			if (plugin.getRenderMode() == FaceSwapRenderMode.MASK)
			{
				renderProjectedFaceMask(graphics, npc, assignedHead);
			}
			else if (plugin.getRenderMode() == FaceSwapRenderMode.TWO_D)
			{
				if (renderProjectedHeadTriangles(graphics, npc, assignedHead))
				{
					renderEmoteAccessories(graphics, npc);
				}
			}
		}
		return null;
	}

	private void renderEmoteAccessories(Graphics2D graphics, Actor actor)
	{
		int animation = actor.getAnimation();
		boolean raspberry = isRaspberryAnimation(animation);
		boolean bunnyHop = isBunnyHopAnimation(animation);
		if (!raspberry && !bunnyHop)
		{
			return;
		}

		Point anchor = getFaceAnchor(actor);
		if (anchor == null)
		{
			return;
		}

		double offsetScale = getProjectedOffsetScale(actor);
		int overlaySize = Math.max(12, Math.min(128, (int) Math.round(plugin.getOverlaySize() * offsetScale)));
		FaceSwapHeadDirection direction = getHeadDirection(actor);
		if (bunnyHop)
		{
			drawBunnyEars(graphics, anchor, overlaySize, direction);
		}
		if (raspberry && direction != FaceSwapHeadDirection.BACK)
		{
			drawTongue(graphics, anchor, overlaySize, direction);
		}
	}

	private void renderMaskEmoteAccessories(
		Graphics2D graphics,
		Actor actor,
		FaceSwapHeadDirection direction,
		WorldView worldView,
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		MaskHeadPose headPose,
		float top,
		float bottom,
		float frontZ,
		Point topLeft,
		Point topRight,
		Point bottomLeft,
		Point bottomRight)
	{
		int animation = actor.getAnimation();
		boolean raspberry = isRaspberryAnimation(animation);
		boolean bunnyHop = isBunnyHopAnimation(animation);
		if (!raspberry && !bunnyHop)
		{
			return;
		}

		int topCenterX = (topLeft.getX() + topRight.getX()) / 2;
		int topCenterY = (topLeft.getY() + topRight.getY()) / 2;
		int bottomCenterX = (bottomLeft.getX() + bottomRight.getX()) / 2;
		int bottomCenterY = (bottomLeft.getY() + bottomRight.getY()) / 2;
		int overlaySize = Math.max(
			12,
			Math.max(
				Math.max(Math.abs(topRight.getX() - topLeft.getX()), Math.abs(bottomRight.getX() - bottomLeft.getX())),
				Math.max(Math.abs(bottomLeft.getY() - topLeft.getY()), Math.abs(bottomRight.getY() - topRight.getY()))));
		Point earsAnchor = projectMaskVertex(
			worldView, localLocation, tileHeight, orientation, headPose, 0f, top - (bottom - top) * 0.20f, frontZ);
		if (earsAnchor == null)
		{
			earsAnchor = new Point(topCenterX, topCenterY);
		}
		Point tongueAnchor = projectMaskVertex(
			worldView, localLocation, tileHeight, orientation, headPose, 0f, bottom * 0.18f, frontZ);
		if (tongueAnchor == null)
		{
			tongueAnchor = new Point(
				(topCenterX + bottomCenterX) / 2,
				(topCenterY + bottomCenterY) / 2);
		}
		if (bunnyHop)
		{
			drawBunnyEars(graphics, earsAnchor, overlaySize, direction);
		}
		if (raspberry && direction != FaceSwapHeadDirection.BACK)
		{
			drawTongue(graphics, tongueAnchor, overlaySize, direction);
		}
	}

	private void drawTongue(Graphics2D graphics, Point anchor, int overlaySize, FaceSwapHeadDirection direction)
	{
		int width = Math.max(6, overlaySize / 6);
		int height = Math.max(10, overlaySize / 4);
		int x = anchor.getX() - width / 2;
		if (direction == FaceSwapHeadDirection.LEFT)
		{
			x -= Math.max(1, width / 4);
		}
		else if (direction == FaceSwapHeadDirection.RIGHT)
		{
			x += Math.max(1, width / 4);
		}
		int y = anchor.getY() + Math.max(2, overlaySize / 10);
		int[] tongueX = {
			x,
			x + width,
			x + width - Math.max(2, width / 5),
			x + width / 2,
			x + Math.max(2, width / 5)
		};
		int[] tongueY = {
			y,
			y,
			y + height - Math.max(2, height / 5),
			y + height,
			y + height - Math.max(2, height / 5)
		};
		Color previousColor = graphics.getColor();
		java.awt.Stroke previousStroke = graphics.getStroke();
		try
		{
			graphics.setColor(TONGUE_FILL);
			graphics.fillPolygon(tongueX, tongueY, tongueX.length);
			graphics.setColor(TONGUE_OUTLINE);
			graphics.setStroke(new BasicStroke(Math.max(1f, overlaySize / 24f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.drawPolyline(tongueX, tongueY, tongueX.length);
			graphics.drawLine(x + width / 2, y + Math.max(2, height / 6), x + width / 2, y + height - Math.max(2, height / 5));
		}
		finally
		{
			graphics.setColor(previousColor);
			graphics.setStroke(previousStroke);
		}
	}

	private void drawBunnyEars(Graphics2D graphics, Point anchor, int overlaySize, FaceSwapHeadDirection direction)
	{
		int baseY = anchor.getY() - Math.max(10, overlaySize / 2);
		int outerWidth = Math.max(8, overlaySize / 7);
		int outerHeight = Math.max(20, overlaySize / 3);
		int innerWidth = Math.max(4, outerWidth / 2);
		int innerHeight = Math.max(10, outerHeight / 2);
		int spacing = Math.max(4, overlaySize / 7);
		int tilt = direction == FaceSwapHeadDirection.LEFT
			? -Math.max(2, outerWidth / 3)
			: direction == FaceSwapHeadDirection.RIGHT
				? Math.max(2, outerWidth / 3)
				: 0;
		int leftX = anchor.getX() - spacing - outerWidth;
		int rightX = anchor.getX() + spacing;
		Color previousColor = graphics.getColor();
		java.awt.Stroke previousStroke = graphics.getStroke();
		try
		{
			graphics.setStroke(new BasicStroke(Math.max(1f, overlaySize / 26f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			drawSingleEar(graphics, leftX, baseY, outerWidth, outerHeight, innerWidth, innerHeight, -tilt);
			drawSingleEar(graphics, rightX, baseY, outerWidth, outerHeight, innerWidth, innerHeight, tilt);
		}
		finally
		{
			graphics.setColor(previousColor);
			graphics.setStroke(previousStroke);
		}
	}

	private void drawSingleEar(
		Graphics2D graphics,
		int x,
		int y,
		int outerWidth,
		int outerHeight,
		int innerWidth,
		int innerHeight,
		int tilt)
	{
		int[] outerX = {
			x + outerWidth / 2,
			x + outerWidth + tilt,
			x + outerWidth - Math.max(2, outerWidth / 5),
			x + Math.max(2, outerWidth / 5)
		};
		int[] outerY = {
			y,
			y + outerHeight,
			y + outerHeight + Math.max(2, outerHeight / 8),
			y + outerHeight + Math.max(2, outerHeight / 8)
		};
		int innerXOffset = (outerWidth - innerWidth) / 2;
		int[] innerX = {
			x + innerXOffset + innerWidth / 2,
			x + innerXOffset + innerWidth + tilt / 2,
			x + innerXOffset + innerWidth - Math.max(1, innerWidth / 5),
			x + innerXOffset + Math.max(1, innerWidth / 5)
		};
		int[] innerY = {
			y + Math.max(2, outerHeight / 10),
			y + innerHeight + Math.max(6, outerHeight / 3),
			y + innerHeight + Math.max(8, outerHeight / 2),
			y + innerHeight + Math.max(8, outerHeight / 2)
		};
		graphics.setColor(OUTER_EAR_FILL);
		graphics.fillPolygon(outerX, outerY, outerX.length);
		graphics.setColor(INNER_EAR_FILL);
		graphics.fillPolygon(innerX, innerY, innerX.length);
		graphics.setColor(EAR_OUTLINE);
		graphics.drawPolygon(outerX, outerY, outerX.length);
	}

	private void renderPickTargetOutline(Graphics2D graphics)
	{
		Actor actor = plugin.getHoveredPickActor();
		Shape hull = actor == null ? null : actor.getConvexHull();
		if (hull == null)
		{
			return;
		}

		Color previousColor = graphics.getColor();
		java.awt.Stroke previousStroke = graphics.getStroke();
		try
		{
			graphics.setColor(new Color(255, 220, 35, 245));
			graphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.draw(hull);
		}
		finally
		{
			graphics.setColor(previousColor);
			graphics.setStroke(previousStroke);
		}
	}

	private boolean renderEquipmentDebugIfHidden(Graphics2D graphics, Player player)
	{
		boolean hidden = plugin.isHeadFullyCovered(player);
		if (plugin.isDebugEquipment())
		{
			renderEquipmentDebug(graphics, player, hidden);
		}
		return hidden;
	}

	private void renderEquipmentDebug(Graphics2D graphics, Player player, boolean hidden)
	{
		Point anchor = getFaceAnchor(player);
		if (anchor == null)
		{
			return;
		}

		graphics.setColor(hidden ? Color.ORANGE : Color.WHITE);
		String[] lines = plugin.getEquipmentDebugText(player).split("\\|", -1);
		int x = Math.max(8, anchor.getX() - 220);
		int y = Math.max(30, anchor.getY() - 86);
		for (int line = 0; line < lines.length; line++)
		{
			graphics.drawString(lines[line], x, y + line * 14);
		}
	}

	private boolean isBlockedByInterface()
	{
		return isVisible(InterfaceID.WelcomeScreen.UNIVERSE)
			|| isVisible(InterfaceID.Bankmain.UNIVERSE)
			|| isVisible(InterfaceID.BankpinKeypad.UNIVERSE)
			|| isVisible(InterfaceID.SharedBank.FRAME)
			|| isVisible(InterfaceID.BankDepositbox.INVENTORY);
	}

	private boolean isVisible(int componentId)
	{
		Widget widget = client.getWidget(componentId);
		return widget != null && !widget.isHidden();
	}

	private void renderBillboard(Graphics2D graphics, Actor player, BufferedImage headImage)
	{
		Point point = getFaceAnchor(player);
		if (point == null)
		{
			return;
		}

		double offsetScale = getProjectedOffsetScale(player);
		int overlaySize = Math.max(8, Math.min(128, (int) Math.round(plugin.getOverlaySize() * offsetScale)));
		int x = point.getX() + (int) Math.round(plugin.getXOffset() * offsetScale) - overlaySize / 2;
		int y = point.getY() + (int) Math.round(plugin.getYOffset() * offsetScale) - overlaySize / 2;
		graphics.drawImage(headImage, x, y, overlaySize, overlaySize, null);
	}

	private boolean renderProjectedFaceMask(Graphics2D graphics, Actor player, FaceSwapHead assignedHead)
	{
		Model model = player.getModel();
		LocalPoint localLocation = player.getLocalLocation();
		WorldView worldView = player.getWorldView();
		if (model == null || localLocation == null || worldView == null)
		{
			return false;
		}
		ModelBounds bounds = getModelBounds(model);
		if (bounds == null)
		{
			return false;
		}

		int orientation = player.getCurrentOrientation() & 2047;
		int tileHeight = Perspective.getTileHeight(client, localLocation, worldView.getPlane())
			- player.getAnimationHeightOffset();
		float maskScale = plugin.getOverlaySize() / 32f;
		float maskHeight = plugin.getWrapRegionHeight() * maskScale;
		MaskHeadPose headPose = getAnimatedMaskHeadPose(player, model, bounds);
		MaskTrackingMode trackingMode = plugin.getMaskTrackingMode();
		HelmetProfile helmetProfile = usesLegacyHelmetMaskCalibration(
			trackingMode, player instanceof Player) ? getHelmetProfile(player) : null;
		headPose = headPose.withPitchDegrees(
			resolveMaskPitchCalibration(
				plugin.getMaskPitch(), helmetProfile == null ? 0 : helmetProfile.getMaskPitch()));
		headPose = headPose.withYawDegrees(
			resolveMaskCalibration(
				plugin.getMaskYaw(), helmetProfile == null ? 0 : helmetProfile.getMaskYaw()));
		headPose = headPose.withRollDegrees(
			resolveMaskCalibration(
				plugin.getMaskRoll(), helmetProfile == null ? 0 : helmetProfile.getMaskRoll()));
		headPose = headPose.withCenterY(
			headPose.centerY + plugin.getYOffset() + (helmetProfile == null ? 0 : helmetProfile.getMaskY()));
		HeadRegion maskHeadRegion = getMaskHeadRegion(bounds, player);
		float top = -maskHeight / 2f;
		float bottom = maskHeight / 2f;
		float halfWidth = maskHeadRegion.halfWidth
			* maskScale * plugin.getMaskWidth() / 100f;
		float headHalfWidth = maskHeadRegion.halfWidth;
		float headDepth = maskHeadRegion.maxDepth;
		float frontZ = -headDepth - plugin.getMaskForwardOffset();
		float backZ = headDepth * plugin.getMaskBacking() / 100f;

		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		ModelProjection modelProjection = null;
		if (verticesX != null && verticesY != null && verticesZ != null)
		{
			modelProjection = projectModelVertices(
				worldView, localLocation, tileHeight, orientation, model, verticesX, verticesY, verticesZ);
		}
		int screenX = plugin.getXOffset();
		int screenY = 0;

		Point topLeft = projectMaskVertex(worldView, localLocation, tileHeight, orientation, headPose, -halfWidth, top, frontZ);
		Point topRight = projectMaskVertex(worldView, localLocation, tileHeight, orientation, headPose, halfWidth, top, frontZ);
		Point bottomLeft = projectMaskVertex(worldView, localLocation, tileHeight, orientation, headPose, -halfWidth, bottom, frontZ);
		Point bottomRight = projectMaskVertex(worldView, localLocation, tileHeight, orientation, headPose, halfWidth, bottom, frontZ);
		if (topLeft == null || topRight == null || bottomLeft == null || bottomRight == null)
		{
			return false;
		}
		topLeft = new Point(topLeft.getX() + screenX, topLeft.getY() + screenY);
		topRight = new Point(topRight.getX() + screenX, topRight.getY() + screenY);
		bottomLeft = new Point(bottomLeft.getX() + screenX, bottomLeft.getY() + screenY);
		bottomRight = new Point(bottomRight.getX() + screenX, bottomRight.getY() + screenY);
		long maskWinding = (long) (topRight.getX() - topLeft.getX()) * (bottomLeft.getY() - topLeft.getY())
			- (long) (topRight.getY() - topLeft.getY()) * (bottomLeft.getX() - topLeft.getX());
		boolean backFacing = maskWinding < 0;
		int projectedWidth = Math.max(
			Math.abs(topRight.getX() - topLeft.getX()),
			Math.abs(bottomRight.getX() - bottomLeft.getX()));
		if (projectedWidth < 4)
		{
			int topCenterX = (topLeft.getX() + topRight.getX()) / 2;
			int bottomCenterX = (bottomLeft.getX() + bottomRight.getX()) / 2;
			topLeft = new Point(topCenterX - 2, topLeft.getY());
			topRight = new Point(topCenterX + 2, topRight.getY());
			bottomLeft = new Point(bottomCenterX - 2, bottomLeft.getY());
			bottomRight = new Point(bottomCenterX + 2, bottomRight.getY());
		}
		renderMaskEmoteAccessories(
			graphics,
			player,
			getHeadDirection(player),
			worldView,
			localLocation,
			tileHeight,
			orientation,
			headPose,
			top,
			bottom,
			frontZ,
			topLeft,
			topRight,
			bottomLeft,
			bottomRight);

		float strapY = top + (bottom - top) * 0.56f;
		float rearStrapY = strapY - (bottom - top) * 0.12f;
		float middleStrapY = strapY - (bottom - top) * 0.07f;
		float frontStrapX = halfWidth * 0.68f;
		float middleStrapX = headHalfWidth * 0.82f;
		float rearStrapX = headHalfWidth * 0.65f;
		java.awt.Stroke previousStroke = graphics.getStroke();
		graphics.setColor(new Color(8, 8, 8, 235));
		graphics.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Point leftFront = projectMaskVertex(worldView, localLocation, tileHeight, orientation,
			headPose, -frontStrapX, strapY, frontZ);
		Point rightFront = projectMaskVertex(worldView, localLocation, tileHeight, orientation,
			headPose, frontStrapX, strapY, frontZ);
		Point leftMiddle = projectMaskVertex(worldView, localLocation, tileHeight, orientation,
			headPose, -middleStrapX, middleStrapY, (frontZ + backZ) / 2f);
		Point rightMiddle = projectMaskVertex(worldView, localLocation, tileHeight, orientation,
			headPose, middleStrapX, middleStrapY, (frontZ + backZ) / 2f);
		Point leftBack = projectMaskVertex(worldView, localLocation, tileHeight, orientation,
			headPose, -rearStrapX, rearStrapY, backZ);
		Point rightBack = projectMaskVertex(worldView, localLocation, tileHeight, orientation,
			headPose, rearStrapX, rearStrapY, backZ);
		Point leftRearMiddle = projectMaskVertex(worldView, localLocation, tileHeight, orientation,
			headPose, -rearStrapX * 0.45f, rearStrapY - (bottom - top) * 0.04f, backZ + headDepth * 0.06f);
		Point rightRearMiddle = projectMaskVertex(worldView, localLocation, tileHeight, orientation,
			headPose, rearStrapX * 0.45f, rearStrapY - (bottom - top) * 0.04f, backZ + headDepth * 0.06f);
		Point backMiddle = projectMaskVertex(worldView, localLocation, tileHeight, orientation,
			headPose, 0f, rearStrapY - (bottom - top) * 0.05f, backZ + headDepth * 0.08f);
		if (leftFront != null)
		{
			leftFront = new Point(leftFront.getX() + screenX, leftFront.getY() + screenY);
		}
		if (rightFront != null)
		{
			rightFront = new Point(rightFront.getX() + screenX, rightFront.getY() + screenY);
		}
		if (leftMiddle != null)
		{
			leftMiddle = new Point(leftMiddle.getX() + screenX, leftMiddle.getY() + screenY);
		}
		if (rightMiddle != null)
		{
			rightMiddle = new Point(rightMiddle.getX() + screenX, rightMiddle.getY() + screenY);
		}
		if (leftBack != null)
		{
			leftBack = new Point(leftBack.getX() + screenX, leftBack.getY() + screenY);
		}
		if (rightBack != null)
		{
			rightBack = new Point(rightBack.getX() + screenX, rightBack.getY() + screenY);
		}
		if (leftRearMiddle != null)
		{
			leftRearMiddle = new Point(leftRearMiddle.getX() + screenX, leftRearMiddle.getY() + screenY);
		}
		if (rightRearMiddle != null)
		{
			rightRearMiddle = new Point(rightRearMiddle.getX() + screenX, rightRearMiddle.getY() + screenY);
		}
		if (backMiddle != null)
		{
			backMiddle = new Point(backMiddle.getX() + screenX, backMiddle.getY() + screenY);
		}
		FaceSwapHeadDirection cameraDirection = getHeadDirection(player);
		boolean sideView = cameraDirection == FaceSwapHeadDirection.LEFT
			|| cameraDirection == FaceSwapHeadDirection.RIGHT;
		double leftStrapDistance = getMaskCameraDistance(localLocation, tileHeight, orientation,
			headPose, -middleStrapX, middleStrapY, (frontZ + backZ) / 2f);
		double rightStrapDistance = getMaskCameraDistance(localLocation, tileHeight, orientation,
			headPose, middleStrapX, middleStrapY, (frontZ + backZ) / 2f);
		boolean drawLeftSide = !sideView || leftStrapDistance <= rightStrapDistance;
		boolean drawRightSide = !sideView || rightStrapDistance < leftStrapDistance;
		if (drawLeftSide && leftFront != null && leftMiddle != null && leftBack != null)
		{
			graphics.draw(new QuadCurve2D.Float(
				leftFront.getX(), leftFront.getY(),
				leftMiddle.getX(), leftMiddle.getY(),
				leftBack.getX(), leftBack.getY()));
		}
		if (drawRightSide && rightFront != null && rightMiddle != null && rightBack != null)
		{
			graphics.draw(new QuadCurve2D.Float(
				rightFront.getX(), rightFront.getY(),
				rightMiddle.getX(), rightMiddle.getY(),
				rightBack.getX(), rightBack.getY()));
		}
		if (drawLeftSide && leftBack != null && leftRearMiddle != null && backMiddle != null)
		{
			graphics.draw(new QuadCurve2D.Float(
				leftBack.getX(), leftBack.getY(),
				leftRearMiddle.getX(), leftRearMiddle.getY(),
				backMiddle.getX(), backMiddle.getY()));
		}
		if (drawRightSide && rightBack != null && rightRearMiddle != null && backMiddle != null)
		{
			graphics.draw(new QuadCurve2D.Float(
				rightBack.getX(), rightBack.getY(),
				rightRearMiddle.getX(), rightRearMiddle.getY(),
				backMiddle.getX(), backMiddle.getY()));
		}
		graphics.setStroke(previousStroke);

		// Overlays have no scene depth buffer. Clip a rear-facing mask against
		// the projected head triangles while retaining any exposed mask edges.
		double maskDistance = getMaskCameraDistance(localLocation, tileHeight, orientation,
			headPose, 0f, 0f, frontZ);
		double headDistance = getMaskCameraDistance(localLocation, tileHeight, orientation,
			headPose, 0f, 0f, 0f);
		Shape previousClip = graphics.getClip();
		if (player.getAnimation() < 0 && maskDistance > headDistance && modelProjection != null)
		{
			Area visibleMask = new Area(new Polygon(
				new int[] {topLeft.getX(), topRight.getX(), bottomRight.getX(), bottomLeft.getX()},
				new int[] {topLeft.getY(), topRight.getY(), bottomRight.getY(), bottomLeft.getY()},
				4));
			visibleMask.subtract(getProjectedMaskHeadOcclusion(modelProjection, player, model, bounds));
			graphics.clip(visibleMask);
		}

		try
		{
			FaceSwapHeadDirection imageDirection = backFacing
				? FaceSwapHeadDirection.BACK
				: FaceSwapHeadDirection.FRONT;
			BufferedImage faceImage = FaceSwapHeadImages.get(assignedHead, imageDirection);
			float imageWidth = Math.max(1, faceImage.getWidth() - 1);
			float imageHeight = Math.max(1, faceImage.getHeight() - 1);
			Color backing = FaceSwapHeadImages.getAverageColor(assignedHead, imageDirection);
			Point imageTopLeft = backFacing ? topRight : topLeft;
			Point imageTopRight = backFacing ? topLeft : topRight;
			Point imageBottomLeft = backFacing ? bottomRight : bottomLeft;
			Point imageBottomRight = backFacing ? bottomLeft : bottomRight;
			boolean first = drawTexturedTriangle(graphics, faceImage, backing, false,
				new float[] {0f, 0f}, new float[] {imageWidth, 0f}, new float[] {0f, imageHeight},
				imageTopLeft, imageTopRight, imageBottomLeft);
			boolean second = drawTexturedTriangle(graphics, faceImage, backing, false,
				new float[] {imageWidth, 0f}, new float[] {imageWidth, imageHeight}, new float[] {0f, imageHeight},
				imageTopRight, imageBottomRight, imageBottomLeft);
			return first && second;
		}
		finally
		{
			graphics.setClip(previousClip);
		}
	}

	private MaskHeadPose getAnimatedMaskHeadPose(Actor player, Model model, ModelBounds bounds)
	{
		MaskTrackingMode trackingMode = plugin.getMaskTrackingMode();
		if (trackingMode == MaskTrackingMode.MERGED_MODEL
			|| (trackingMode == MaskTrackingMode.AUTO && !(player instanceof Player)))
		{
			return mergedModelMaskPoseTracker.getPose(player, model, bounds);
		}

		MaskHeadPose rigPose = animatedRigMaskPoseTracker.getPose(player, model, bounds);
		if (rigPose != null)
		{
			return rigPose;
		}
		if (trackingMode == MaskTrackingMode.ANIMATED_RIG)
		{
			MaskHeadPose previousPose = maskRigHeadPoses.get(player);
			return previousPose == null
				? MaskHeadPose.DEFAULT.withCenterY(-plugin.getHeightOffset())
				: previousPose;
		}
		return mergedModelMaskPoseTracker.getPose(player, model, bounds);
	}

	private MaskHeadPose getAnimatedRigMaskHeadPose(Actor actor, Model ignoredModel, ModelBounds actorBounds)
	{
		Model rigModel = plugin.getMaskRigModel(actor);
		Model rigBaseModel = plugin.getMaskRigBaseModel();
		if (rigModel == null || rigBaseModel == null)
		{
			return null;
		}
		float[] verticesX = rigModel.getVerticesX();
		float[] verticesY = rigModel.getVerticesY();
		float[] verticesZ = rigModel.getVerticesZ();
		if (verticesX == null || verticesY == null || verticesZ == null)
		{
			return null;
		}

		MaskHeadBinding binding = maskRigBindings.get(actor);
		if (binding == null || !binding.matches(rigModel, -1, NO_APPEARANCE_FINGERPRINT))
		{
			ModelBounds rigBounds = getModelBounds(rigBaseModel);
			if (rigBounds == null)
			{
				return null;
			}
			HeadRegion rigHeadRegion = new HeadRegion(
				rigBounds.minY,
				rigBounds.minY + MASK_RIG_HEAD_REGION_HEIGHT,
				MASK_RIG_HEAD_HALF_WIDTH,
				MASK_RIG_HEAD_DEPTH,
				MASK_RIG_HEAD_HALF_WIDTH,
				MASK_RIG_HEAD_DEPTH,
				MASK_RIG_HEAD_DEPTH,
				false,
				0);
			binding = createMaskHeadBinding(
				rigBaseModel, rigHeadRegion, -1, NO_APPEARANCE_FINGERPRINT);
			if (binding == null || !binding.canRead(rigModel))
			{
				return null;
			}
			maskRigBindings.put(actor, binding);
		}

		MaskHeadPose pose = binding.getPose(
			verticesX, verticesY, verticesZ, -plugin.getHeightOffset());
		MaskHeadPose previousPose = maskRigHeadPoses.get(actor);
		if (!isValidRigPose(pose, actorBounds, previousPose))
		{
			return null;
		}
		maskRigHeadPoses.put(actor, pose);
		return pose;
	}

	private static boolean isValidRigPose(MaskHeadPose pose, ModelBounds actorBounds, MaskHeadPose previousPose)
	{
		if (pose == null || actorBounds == null || !pose.isFinite())
		{
			return false;
		}
		float horizontalLimit = Math.max(
			Math.max(Math.abs(actorBounds.minX), Math.abs(actorBounds.maxX)),
			Math.max(Math.abs(actorBounds.minZ), Math.abs(actorBounds.maxZ))) + 128f;
		if (Math.abs(pose.centerX) > horizontalLimit
			|| Math.abs(pose.centerZ) > horizontalLimit
			|| pose.centerY < actorBounds.minY - 128f
			|| pose.centerY > actorBounds.maxY + 64f)
		{
			return false;
		}
		return previousPose == null
			|| pose.centerDistanceSquared(previousPose)
				<= MASK_RIG_MAX_CENTER_DISTANCE * MASK_RIG_MAX_CENTER_DISTANCE;
	}

	private MaskHeadPose getMergedModelMaskHeadPose(Actor player, Model model, ModelBounds bounds)
	{
		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		if (verticesX == null || verticesY == null || verticesZ == null)
		{
			return MaskHeadPose.DEFAULT.withCenterY(-plugin.getHeightOffset());
		}

		int headItemId = -1;
		int weaponItemId = -1;
		PlayerComposition composition = player instanceof Player
			? ((Player) player).getPlayerComposition()
			: null;
		if (composition != null)
		{
			headItemId = composition.getEquipmentId(KitType.HEAD);
			weaponItemId = composition.getEquipmentId(KitType.WEAPON);
		}
		int appearanceFingerprint = getAppearanceFingerprint(composition);
		MaskHeadBinding binding = maskHeadBindings.get(player);
		boolean bindingMatches = binding != null
			&& binding.matches(model, headItemId, appearanceFingerprint);
		if (!bindingMatches)
		{
			MaskHeadPose previousPose = maskHeadPoses.get(player);
			if (binding != null && player.getAnimation() >= 0 && previousPose != null)
			{
				if (binding.canRead(model))
				{
					MaskHeadPose animatedPose = binding.getPose(
						verticesX, verticesY, verticesZ, -plugin.getHeightOffset());
					if (animatedPose.centerDistanceSquared(previousPose)
						<= MASK_TRANSIENT_MAX_CENTER_DISTANCE * MASK_TRANSIENT_MAX_CENTER_DISTANCE)
					{
						// Temporary action models can reorder peripheral vertices. Follow
						// the bound head center, but do not let a tool rotate the mask.
						animatedPose = animatedPose.withAxesFrom(previousPose);
						animatedPose = stabilizeMaskPoseForEquipment(
							animatedPose, headItemId, weaponItemId, player.getAnimation());
						maskHeadPoses.put(player, animatedPose);
						return animatedPose;
					}
				}
				MaskHeadBinding transientBinding = createMaskHeadBinding(
					model,
					getAnimatedMaskHeadRegion(
						model, bounds, player, previousPose.centerX, previousPose.centerZ),
					headItemId,
					appearanceFingerprint,
					previousPose.centerX,
					previousPose.centerZ);
				if (transientBinding != null)
				{
					MaskHeadPose transientPose = transientBinding.getAbsolutePose(
						verticesX, verticesY, verticesZ, -plugin.getHeightOffset());
					if (transientPose.centerDistanceSquared(previousPose)
						<= MASK_TRANSIENT_MAX_CENTER_DISTANCE * MASK_TRANSIENT_MAX_CENTER_DISTANCE)
					{
						transientPose = transientPose.withAxesFrom(previousPose);
						transientPose = stabilizeMaskPoseForEquipment(
							transientPose, headItemId, weaponItemId, player.getAnimation());
						maskHeadPoses.put(player, transientPose);
						return transientPose;
					}
				}
				return stabilizeMaskPoseForEquipment(
					previousPose, headItemId, weaponItemId, player.getAnimation());
			}
			binding = createMaskHeadBinding(
				model,
				player.getAnimation() >= 0
					? getAnimatedMaskHeadRegion(model, bounds, player)
					: getMaskHeadRegion(bounds, player),
				headItemId,
				appearanceFingerprint);
			if (binding == null)
			{
				return MaskHeadPose.DEFAULT.withCenterY(-plugin.getHeightOffset());
			}
			// A transient absolute anchor follows actors first seen mid-emote.
			// Store the binding only after a non-action frame establishes baseline.
			if (player.getAnimation() >= 0)
			{
				MaskHeadPose transientPose = binding.getAbsolutePose(verticesX, verticesY, verticesZ,
					-plugin.getHeightOffset());
				transientPose = stabilizeMaskPoseForEquipment(
					transientPose, headItemId, weaponItemId, player.getAnimation());
				maskHeadPoses.put(player, transientPose);
				return transientPose;
			}
			maskHeadBindings.put(player, binding);
		}

		MaskHeadPose pose = binding.getPose(
			verticesX, verticesY, verticesZ, -plugin.getHeightOffset());
		if (player.getAnimation() >= 0)
		{
			HeadRegion animatedRegion = getAnimatedMaskHeadRegion(model, bounds, player);
			float animatedCenterY = (animatedRegion.top + animatedRegion.bottom) / 2f;
			if (Math.abs(pose.centerY - animatedCenterY) > MASK_BINDING_MAX_VERTICAL_DRIFT)
			{
				MaskHeadBinding transientBinding = createMaskHeadBinding(
					model, animatedRegion, headItemId, appearanceFingerprint);
				if (transientBinding != null)
				{
					MaskHeadPose transientPose = transientBinding.getAbsolutePose(
						verticesX, verticesY, verticesZ, -plugin.getHeightOffset());
					MaskHeadPose previousPose = maskHeadPoses.get(player);
					if (previousPose == null
						|| transientPose.centerDistanceSquared(previousPose)
							< pose.centerDistanceSquared(previousPose))
					{
						pose = transientPose;
					}
				}
			}
		}
		pose = stabilizeMaskPoseForEquipment(
			pose, headItemId, weaponItemId, player.getAnimation());
		maskHeadPoses.put(player, pose);
		return pose;
	}

	private static HelmetProfile getHelmetProfile(Actor actor)
	{
		if (!(actor instanceof Player))
		{
			return null;
		}
		PlayerComposition composition = ((Player) actor).getPlayerComposition();
		if (composition == null)
		{
			return null;
		}
		return HelmetProfiles.find(composition.getEquipmentId(KitType.HEAD));
	}

	private static MaskHeadPose stabilizeMaskPoseForEquipment(
		MaskHeadPose pose,
		int headItemId,
		int weaponItemId,
		int animationId)
	{
		boolean actionAnimation = animationId >= 0;
		MaskHeadPose stabilizedPose;
		if (isSoulreaperAxe(weaponItemId)
			|| (actionAnimation && headItemId == ItemID.MENAPHITE_PURPLE_HAT))
		{
			stabilizedPose = pose.withActorAxes();
		}
		else if (animationId == AnimationID.EMOTE_DANCE_HEADBANG
			|| animationId == AnimationID.EMOTE_DANCE_HEADBANG_LOOP)
		{
			stabilizedPose = pose.withYawOnlyAxes();
		}
		else
		{
			stabilizedPose = FaceSwapPlugin.isMedHelmItem(headItemId)
				? pose.withYawOnlyAxes()
				: pose;
		}
		return actionAnimation && (headItemId >= 0 || weaponItemId >= 0)
			? stabilizedPose.withPitchDegrees(EQUIPPED_ACTION_MASK_PITCH_DEGREES)
			: stabilizedPose;
	}

	private static boolean isSoulreaperAxe(int itemId)
	{
		return itemId == ItemID.SOULREAPER_AXE
			|| itemId == ItemID.SOULREAPER_AXE_28338
			|| itemId == ItemID.SOULREAPER_AXE_O;
	}

	private Point projectMaskVertex(
		WorldView worldView,
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		MaskHeadPose pose,
		float x,
		float y,
		float z)
	{
		MaskVector point = pose.transform(x, y, z);
		return projectModelVertex(worldView, localLocation, tileHeight, orientation, point.x, point.y, point.z);
	}

	private double getMaskCameraDistance(
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		MaskHeadPose pose,
		float x,
		float y,
		float z)
	{
		MaskVector point = pose.transform(x, y, z);
		return getTriangleCameraDistance(localLocation, tileHeight, orientation, point.x, point.y, point.z);
	}

	private MaskHeadBinding createMaskHeadBinding(
		Model model,
		HeadRegion headRegion,
		int headItemId,
		int appearanceFingerprint)
	{
		return createMaskHeadBinding(
			model, headRegion, headItemId, appearanceFingerprint, 0f, 0f);
	}

	private MaskHeadBinding createMaskHeadBinding(
		Model model,
		HeadRegion headRegion,
		int headItemId,
		int appearanceFingerprint,
		float centerX,
		float centerZ)
	{
		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		int[] face1 = model.getFaceIndices1();
		int[] face2 = model.getFaceIndices2();
		int[] face3 = model.getFaceIndices3();
		if (verticesX == null || verticesY == null || verticesZ == null
			|| face1 == null || face2 == null || face3 == null)
		{
			return null;
		}

		boolean[] selected = new boolean[model.getVerticesCount()];
		int selectedCount = 0;
		for (int face = 0; face < model.getFaceCount(); face++)
		{
			int a = face1[face];
			int b = face2[face];
			int c = face3[face];
			if (!isHeadTriangle(
				verticesX[a] - centerX, verticesY[a], verticesZ[a] - centerZ,
				verticesX[b] - centerX, verticesY[b], verticesZ[b] - centerZ,
				verticesX[c] - centerX, verticesY[c], verticesZ[c] - centerZ,
				headRegion))
			{
				continue;
			}
			if (!selected[a])
			{
				selected[a] = true;
				selectedCount++;
			}
			if (!selected[b])
			{
				selected[b] = true;
				selectedCount++;
			}
			if (!selected[c])
			{
				selected[c] = true;
				selectedCount++;
			}
		}
		if (selectedCount == 0)
		{
			return null;
		}

		int[] indices = new int[selectedCount];
		for (int vertex = 0, index = 0; vertex < selected.length; vertex++)
		{
			if (selected[vertex])
			{
				indices[index++] = vertex;
			}
		}
		if (headItemId >= 0 && !FaceSwapPlugin.isMedHelmItem(headItemId))
		{
			int[] coreIndices = selectMaskBindingCore(
				indices, verticesX, verticesZ, centerX, centerZ);
			if (coreIndices.length >= HELMET_MASK_BINDING_MIN_CORE_VERTICES)
			{
				indices = coreIndices;
			}
		}
		return new MaskHeadBinding(model.getVerticesCount(), model.getFaceCount(), headItemId,
			appearanceFingerprint, indices, verticesX, verticesY, verticesZ);
	}

	private static int[] selectMaskBindingCore(
		int[] indices,
		float[] verticesX,
		float[] verticesZ,
		float centerX,
		float centerZ)
	{
		int count = 0;
		for (int index : indices)
		{
			if (Math.abs(verticesX[index] - centerX) <= HELMET_MASK_BINDING_CORE_RADIUS
				&& Math.abs(verticesZ[index] - centerZ) <= HELMET_MASK_BINDING_CORE_RADIUS)
			{
				count++;
			}
		}

		int[] core = new int[count];
		int destination = 0;
		for (int index : indices)
		{
			if (Math.abs(verticesX[index] - centerX) <= HELMET_MASK_BINDING_CORE_RADIUS
				&& Math.abs(verticesZ[index] - centerZ) <= HELMET_MASK_BINDING_CORE_RADIUS)
			{
				core[destination++] = index;
			}
		}
		return core;
	}

	private HeadRegion getAnimatedMaskHeadRegion(Model model, ModelBounds bounds, Actor actor)
	{
		return getAnimatedMaskHeadRegion(model, bounds, actor, 0f, 0f);
	}

	private HeadRegion getAnimatedMaskHeadRegion(
		Model model,
		ModelBounds bounds,
		Actor actor,
		float centerX,
		float centerZ)
	{
		HeadRegion fallback = getMaskHeadRegion(bounds, actor);
		if (!(actor instanceof Player))
		{
			return fallback;
		}

		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		if (verticesX == null || verticesY == null || verticesZ == null)
		{
			return fallback;
		}

		float[] centralY = new float[model.getVerticesCount()];
		int centralCount = 0;
		for (int vertex = 0; vertex < model.getVerticesCount(); vertex++)
		{
			if (Math.abs(verticesX[vertex] - centerX) <= ANIMATED_HEAD_CENTER_LIMIT
				&& Math.abs(verticesZ[vertex] - centerZ) <= ANIMATED_HEAD_CENTER_LIMIT)
			{
				centralY[centralCount++] = verticesY[vertex];
			}
		}
		if (centralCount < 20)
		{
			return fallback;
		}

		Arrays.sort(centralY, 0, centralCount);
		float height = fallback.bottom - fallback.top;
		int upperPercentileIndex = Math.min(centralCount - 1, Math.max(1, centralCount / 20));
		float top = centralY[upperPercentileIndex] - height * 0.2f;
		return new HeadRegion(
			top,
			top + height,
			fallback.halfWidth,
			fallback.maxDepth,
			fallback.faceHalfWidth,
			fallback.faceDepth,
			fallback.frontFaceDepth,
			fallback.headgearAdjusted,
			fallback.helmetDrop);
	}

	private static int getAppearanceFingerprint(PlayerComposition composition)
	{
		if (composition == null)
		{
			return NO_APPEARANCE_FINGERPRINT;
		}

		int fingerprint = Arrays.hashCode(composition.getEquipmentIds());
		fingerprint = 31 * fingerprint + Arrays.hashCode(composition.getColors());
		fingerprint = 31 * fingerprint + composition.getGender();
		fingerprint = 31 * fingerprint + composition.getTransformedNpcId();
		return fingerprint;
	}

	private HeadRegion getMaskHeadRegion(ModelBounds bounds, Actor actor)
	{
		HeadRegion region = getHeadRegion(bounds, actor);
		if (!(actor instanceof Player))
		{
			return region;
		}

		// Weapons can extend above the player and corrupt bounds.minY. Seed the
		// binding around the configured anatomical head center instead.
		float height = region.bottom - region.top;
		float top = -plugin.getHeightOffset() - height / 2f;
		float halfWidth = Math.min(region.halfWidth, PLAYER_MASK_MAX_HALF_WIDTH);
		float maxDepth = Math.min(region.maxDepth, PLAYER_MASK_MAX_DEPTH);
		return new HeadRegion(
			top,
			top + height,
			halfWidth,
			maxDepth,
			Math.min(region.faceHalfWidth, halfWidth),
			Math.min(region.faceDepth, maxDepth),
			Math.min(region.frontFaceDepth, maxDepth),
			region.headgearAdjusted,
			region.helmetDrop);
	}

	private Area getProjectedMaskHeadOcclusion(
		ModelProjection projection,
		Actor player,
		Model model,
		ModelBounds bounds)
	{
		MaskHeadBinding binding = maskHeadBindings.get(player);
		PlayerComposition composition = player instanceof Player
			? ((Player) player).getPlayerComposition()
			: null;
		int headItemId = composition == null ? -1 : composition.getEquipmentId(KitType.HEAD);
		int appearanceFingerprint = getAppearanceFingerprint(composition);
		if (binding == null || !binding.matches(model, headItemId, appearanceFingerprint))
		{
			binding = createMaskHeadBinding(
				model, getMaskHeadRegion(bounds, player), headItemId, appearanceFingerprint);
			if (binding != null)
			{
				maskHeadBindings.put(player, binding);
			}
		}
		if (binding == null)
		{
			return new Area();
		}

		List<java.awt.Point> points = new ArrayList<>(binding.vertexIndices.length);
		for (int vertex : binding.vertexIndices)
		{
			Point point = projection.getPoint(vertex);
			if (point != null)
			{
				points.add(new java.awt.Point(point.getX(), point.getY()));
			}
		}
		return points.size() < 3
			? new Area()
			: new Area(expandPolygon(convexHull(points), 1));
	}

	private boolean renderProjectedHeadTriangles(
		Graphics2D graphics,
		Actor player,
		FaceSwapHead assignedHead)
	{
		Model model = player.getModel();
		LocalPoint localLocation = player.getLocalLocation();
		WorldView worldView = player.getWorldView();
		if (model == null || localLocation == null || worldView == null)
		{
			return false;
		}
		ModelBounds bounds = getModelBounds(model);
		if (bounds == null)
		{
			return false;
		}

		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		int[] face1 = model.getFaceIndices1();
		int[] face2 = model.getFaceIndices2();
		int[] face3 = model.getFaceIndices3();
		if (verticesX == null || verticesY == null || verticesZ == null || face1 == null || face2 == null || face3 == null)
		{
			return false;
		}

		HeadRegion headRegion = getHeadRegion(bounds, player);
		PlayerComposition composition = player instanceof Player
			? ((Player) player).getPlayerComposition()
			: null;
		int headItemId = composition == null ? -1 : composition.getEquipmentId(KitType.HEAD);
		boolean berserkerHelm = headItemId == ItemID.BERSERKER_HELM
			|| headItemId == ItemID.BERSERKER_HELM_27169;
		float headMinX = Float.MAX_VALUE;
		float headMaxX = -Float.MAX_VALUE;
		float headMinZ = Float.MAX_VALUE;
		float headMaxZ = -Float.MAX_VALUE;
		int headCenterVertices = 0;
		for (int vertex = 0; vertex < model.getVerticesCount(); vertex++)
		{
			if (verticesY[vertex] >= headRegion.top
				&& verticesY[vertex] <= headRegion.bottom
				&& Math.abs(verticesX[vertex]) <= headRegion.halfWidth * 1.5f
				&& Math.abs(verticesZ[vertex]) <= headRegion.maxDepth * 1.5f)
			{
				headMinX = Math.min(headMinX, verticesX[vertex]);
				headMaxX = Math.max(headMaxX, verticesX[vertex]);
				headMinZ = Math.min(headMinZ, verticesZ[vertex]);
				headMaxZ = Math.max(headMaxZ, verticesZ[vertex]);
				headCenterVertices++;
			}
		}
		float headCenterX = 0f;
		float headCenterZ = 0f;
		if (headCenterVertices > 0)
		{
			headCenterX = (headMinX + headMaxX) / 2f;
			headCenterZ = (headMinZ + headMaxZ) / 2f;
		}
		int tileHeight = Perspective.getTileHeight(client, localLocation, worldView.getPlane())
			- player.getAnimationHeightOffset();
		int orientation = player.getCurrentOrientation() & 2047;
		if (player instanceof Player && plugin.hasMedHelm((Player) player))
		{
			double frontDistance = getTriangleCameraDistance(
				localLocation, tileHeight, orientation,
				headCenterX, headRegion.top, headCenterZ - headRegion.maxDepth);
			double backDistance = getTriangleCameraDistance(
				localLocation, tileHeight, orientation,
				headCenterX, headRegion.top, headCenterZ + headRegion.maxDepth);
			if (backDistance < frontDistance)
			{
				return true;
			}
		}
		ModelProjection projection = projectModelVertices(worldView, localLocation, tileHeight, orientation, model, verticesX, verticesY, verticesZ);
		if (projection == null)
		{
			return false;
		}
		Shape playerHull = player.getConvexHull();
		Rectangle projectedBounds = projection.getBounds();
		if (playerHull != null && projectedBounds != null)
		{
			Rectangle playerBounds = playerHull.getBounds();
			int renderOffsetY = (int) Math.round(playerBounds.getCenterY() - projectedBounds.getCenterY());
			for (int vertex = 0; vertex < projection.y2d.length; vertex++)
			{
				if (projection.y2d[vertex] != Integer.MIN_VALUE)
				{
					projection.y2d[vertex] += renderOffsetY;
				}
			}
		}
		double liftScale = getWrapLiftScale(projection);

		List<TexturedTriangle> triangles = new ArrayList<>();
		List<java.awt.Point> backingPoints = new ArrayList<>();

		for (int face = 0; face < model.getFaceCount(); face++)
		{
			int a = face1[face];
			int b = face2[face];
			int c = face3[face];
			if (!isHeadTriangle(
				verticesX[a] - headCenterX, verticesY[a], verticesZ[a] - headCenterZ,
				verticesX[b] - headCenterX, verticesY[b], verticesZ[b] - headCenterZ,
				verticesX[c] - headCenterX, verticesY[c], verticesZ[c] - headCenterZ,
				headRegion))
			{
				continue;
			}

			Point pa = liftWrapPoint(projection.getPoint(a), liftScale);
			Point pb = liftWrapPoint(projection.getPoint(b), liftScale);
			Point pc = liftWrapPoint(projection.getPoint(c), liftScale);
			if (pa == null || pb == null || pc == null)
			{
				continue;
			}
			FaceSwapHeadDirection direction = getTriangleDirection(
				verticesX[a] - headCenterX,
				verticesZ[a] - headCenterZ,
				verticesX[b] - headCenterX,
				verticesZ[b] - headCenterZ,
				verticesX[c] - headCenterX,
				verticesZ[c] - headCenterZ);
			BufferedImage headImage = FaceSwapHeadImages.get(assignedHead, direction);
			Color backingColor = FaceSwapHeadImages.getAverageColor(assignedHead, direction);
			float[] uva = getTexturePoint(verticesX[a] - headCenterX, verticesY[a], verticesZ[a] - headCenterZ, direction, headRegion, headImage);
			float[] uvb = getTexturePoint(verticesX[b] - headCenterX, verticesY[b], verticesZ[b] - headCenterZ, direction, headRegion, headImage);
			float[] uvc = getTexturePoint(verticesX[c] - headCenterX, verticesY[c], verticesZ[c] - headCenterZ, direction, headRegion, headImage);
			if (berserkerHelm)
			{
				uva[1] -= 12f;
				uvb[1] -= 12f;
				uvc[1] -= 12f;
			}
			double cameraDistance = getTriangleCameraDistance(
				localLocation,
				tileHeight,
				orientation,
				(verticesX[a] + verticesX[b] + verticesX[c]) / 3f,
				(verticesY[a] + verticesY[b] + verticesY[c]) / 3f,
				(verticesZ[a] + verticesZ[b] + verticesZ[c]) / 3f);
			triangles.add(new TexturedTriangle(headImage, backingColor, uva, uvb, uvc, pa, pb, pc, cameraDistance));
			backingPoints.add(new java.awt.Point(pa.getX(), pa.getY()));
			backingPoints.add(new java.awt.Point(pb.getX(), pb.getY()));
			backingPoints.add(new java.awt.Point(pc.getX(), pc.getY()));
		}

		if (triangles.isEmpty())
		{
			return false;
		}

		triangles.sort(Comparator.comparingDouble((TexturedTriangle triangle) -> triangle.cameraDistance).reversed());
		Area helmetOcclusion = headRegion.headgearAdjusted && plugin.isHelmetOcclusionEnabled()
			? getHelmetOcclusion(
				model,
				projection,
				verticesX,
				verticesY,
				verticesZ,
				face1,
				face2,
				face3,
				headRegion,
				localLocation,
				tileHeight,
				orientation,
				liftScale)
			: null;
		Shape previousClip = graphics.getClip();
		Shape renderClip = getPreservedRenderClip(previousClip, helmetOcclusion);
		if (headRegion.headgearAdjusted)
		{
			float apertureTop = headRegion.top + Math.max(
				plugin.getPartialHelmetClipTop(),
				plugin.getPartialHelmetTopPreserve() + (berserkerHelm ? 7 : 0));
			float apertureBottom = headRegion.bottom - plugin.getPartialHelmetClipBottom();
			List<java.awt.Point> aperturePoints = new ArrayList<>();
			if (apertureBottom > apertureTop)
			{
				float apertureX = headRegion.faceHalfWidth * (berserkerHelm ? 0.74f : 1f);
				float apertureZ = headCenterZ - headRegion.frontFaceDepth;
				addShieldPoint(aperturePoints, worldView, localLocation, tileHeight, orientation, headCenterX - apertureX, apertureTop, apertureZ, liftScale);
				addShieldPoint(aperturePoints, worldView, localLocation, tileHeight, orientation, headCenterX + apertureX, apertureTop, apertureZ, liftScale);
				addShieldPoint(aperturePoints, worldView, localLocation, tileHeight, orientation, headCenterX - apertureX, apertureBottom, apertureZ, liftScale);
				addShieldPoint(aperturePoints, worldView, localLocation, tileHeight, orientation, headCenterX + apertureX, apertureBottom, apertureZ, liftScale);
			}
			if (aperturePoints.size() >= 3)
			{
				Area aperture = new Area(convexHull(aperturePoints));
				if (previousClip != null)
				{
					aperture.intersect(new Area(previousClip));
				}
				if (helmetOcclusion != null)
				{
					aperture.subtract(helmetOcclusion);
				}
				renderClip = aperture;
			}
		}
		boolean opaqueFace = plugin.isOpaqueBacking() || headRegion.headgearAdjusted;
		int rendered = 0;

		try
		{
			if (renderClip != null)
			{
				graphics.setClip(renderClip);
			}

			if (opaqueFace)
			{
				Color backingColor = FaceSwapHeadImages.getAverageColor(assignedHead, FaceSwapHeadDirection.FRONT);
				if (plugin.isOpaqueBacking() && !headRegion.headgearAdjusted)
				{
					fillProjectedHeadShield(graphics, worldView, localLocation, tileHeight, orientation, headRegion, backingColor, liftScale);
				}

				int backingExpansion = headRegion.headgearAdjusted ? plugin.getPartialHelmetBackingExpansion() : plugin.getWrapBackingExpansion();
				fillBackingHull(graphics, backingPoints, backingColor, backingExpansion);
			}

			for (TexturedTriangle triangle : triangles)
			{
				if (drawTexturedTriangle(
					graphics,
					triangle.image,
					triangle.backingColor,
					opaqueFace,
					triangle.sourceA,
					triangle.sourceB,
					triangle.sourceC,
					triangle.targetA,
					triangle.targetB,
					triangle.targetC))
				{
					rendered++;
				}
			}
		}
		finally
		{
			graphics.setClip(previousClip);
		}
		return rendered > 0;
	}

	private Shape getPreservedRenderClip(Shape previousClip, Area helmetOcclusion)
	{
		if (helmetOcclusion == null || helmetOcclusion.isEmpty())
		{
			return previousClip;
		}

		Area renderArea = previousClip == null
			? new Area(new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight()))
			: new Area(previousClip);
		renderArea.subtract(helmetOcclusion);
		return renderArea;
	}

	private Area getHelmetOcclusion(
		Model model,
		ModelProjection projection,
		float[] verticesX,
		float[] verticesY,
		float[] verticesZ,
		int[] face1,
		int[] face2,
		int[] face3,
		HeadRegion headRegion,
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		double liftScale)
	{
		Area occlusion = new Area();
		float top = headRegion.top + plugin.getPartialHelmetClipTop();
		float bottom = headRegion.bottom - plugin.getPartialHelmetClipBottom();
		if (bottom <= top)
		{
			return occlusion;
		}

		float headMinX = Float.MAX_VALUE;
		float headMaxX = -Float.MAX_VALUE;
		float headMinZ = Float.MAX_VALUE;
		float headMaxZ = -Float.MAX_VALUE;
		int headCenterVertices = 0;
		for (int vertex = 0; vertex < model.getVerticesCount(); vertex++)
		{
			if (verticesY[vertex] >= headRegion.top
				&& verticesY[vertex] <= headRegion.bottom
				&& Math.abs(verticesX[vertex]) <= headRegion.halfWidth * 1.5f
				&& Math.abs(verticesZ[vertex]) <= headRegion.maxDepth * 1.5f)
			{
				headMinX = Math.min(headMinX, verticesX[vertex]);
				headMaxX = Math.max(headMaxX, verticesX[vertex]);
				headMinZ = Math.min(headMinZ, verticesZ[vertex]);
				headMaxZ = Math.max(headMaxZ, verticesZ[vertex]);
				headCenterVertices++;
			}
		}
		float headCenterX = 0f;
		float headCenterZ = 0f;
		if (headCenterVertices > 0)
		{
			headCenterX = (headMinX + headMaxX) / 2f;
			headCenterZ = (headMinZ + headMaxZ) / 2f;
		}

		for (int face = 0; face < model.getFaceCount(); face++)
		{
			int a = face1[face];
			int b = face2[face];
			int c = face3[face];
			float modelCenterX = (verticesX[a] + verticesX[b] + verticesX[c]) / 3f;
			float centerY = (verticesY[a] + verticesY[b] + verticesY[c]) / 3f;
			float modelCenterZ = (verticesZ[a] + verticesZ[b] + verticesZ[c]) / 3f;
			float centerX = modelCenterX - headCenterX;
			float centerZ = modelCenterZ - headCenterZ;
			if (centerY < top || centerY > bottom)
			{
				continue;
			}

			boolean topPreserve = centerY <= top + plugin.getPartialHelmetTopPreserve();
			double normalizedRadius = 1d;
			if (!topPreserve)
			{
				float faceDepth = centerZ <= 0
					? headRegion.frontFaceDepth
					: headRegion.faceDepth;
				double centerRadius = Math.sqrt(
					(centerX * centerX) / (headRegion.faceHalfWidth * headRegion.faceHalfWidth)
						+ (centerZ * centerZ) / (faceDepth * faceDepth));
				double radiusA = Math.sqrt(
					((verticesX[a] - headCenterX) * (verticesX[a] - headCenterX)) / (headRegion.faceHalfWidth * headRegion.faceHalfWidth)
						+ ((verticesZ[a] - headCenterZ) * (verticesZ[a] - headCenterZ)) / (faceDepth * faceDepth));
				double radiusB = Math.sqrt(
					((verticesX[b] - headCenterX) * (verticesX[b] - headCenterX)) / (headRegion.faceHalfWidth * headRegion.faceHalfWidth)
						+ ((verticesZ[b] - headCenterZ) * (verticesZ[b] - headCenterZ)) / (faceDepth * faceDepth));
				double radiusC = Math.sqrt(
					((verticesX[c] - headCenterX) * (verticesX[c] - headCenterX)) / (headRegion.faceHalfWidth * headRegion.faceHalfWidth)
						+ ((verticesZ[c] - headCenterZ) * (verticesZ[c] - headCenterZ)) / (faceDepth * faceDepth));
				if (Math.max(centerRadius, Math.max(radiusA, Math.max(radiusB, radiusC))) <= 1d)
				{
					continue;
				}
				normalizedRadius = Math.max(centerRadius, 0.001d);
			}

			if (!topPreserve)
			{
				float shellX = headCenterX + (float) (centerX / normalizedRadius);
				float shellZ = headCenterZ + (float) (centerZ / normalizedRadius);
				double triangleDistance = getTriangleCameraDistance(
					localLocation,
					tileHeight,
					orientation,
					modelCenterX,
					centerY,
					modelCenterZ);
				double shellDistance = getTriangleCameraDistance(
					localLocation,
					tileHeight,
					orientation,
					shellX,
					centerY,
					shellZ);
				if (triangleDistance >= shellDistance)
				{
					continue;
				}
			}

			Point pa = liftWrapPoint(projection.getPoint(a), liftScale);
			Point pb = liftWrapPoint(projection.getPoint(b), liftScale);
			Point pc = liftWrapPoint(projection.getPoint(c), liftScale);
			if (pa == null || pb == null || pc == null)
			{
				continue;
			}

			Polygon polygon = new Polygon(
				new int[]{pa.getX(), pb.getX(), pc.getX()},
				new int[]{pa.getY(), pb.getY(), pc.getY()},
				3);
			occlusion.add(new Area(expandPolygon(polygon, HELMET_OCCLUSION_EXPANSION + 1)));
		}
		return occlusion;
	}

	private HeadRegion getHeadRegion(ModelBounds bounds, Actor actor)
	{
		Player player = actor instanceof Player ? (Player) actor : null;
		boolean visibleHeadgear = player != null
			&& plugin.hasHeadgear(player) && !plugin.isHeadFullyCovered(player);
		int helmetDrop = visibleHeadgear && !plugin.isHelmetOcclusionEnabled() ? plugin.getHelmetFaceDrop(player) : 0;
		float top = bounds.minY + plugin.getWrapHeightOffset() + helmetDrop;
		float height = visibleHeadgear ? Math.min(plugin.getWrapRegionHeight(), plugin.getPartialHelmetRegionHeight()) : plugin.getWrapRegionHeight();
		float halfWidth = Math.max(22f, Math.max(Math.abs(bounds.minX), Math.abs(bounds.maxX)) * HEAD_WIDTH_RATIO);
		float maxDepth = Math.max(22f, Math.max(Math.abs(bounds.minZ), Math.abs(bounds.maxZ)) * HEAD_DEPTH_RATIO);
		float faceHalfWidth = visibleHeadgear
			? Math.max(12f, halfWidth * plugin.getPartialHelmetWidth() / 100f)
			: halfWidth;
		float faceDepth = visibleHeadgear
			? Math.max(10f, maxDepth * plugin.getPartialHelmetDepth() / 100f)
			: maxDepth;
		float frontFaceDepth = visibleHeadgear
			? Math.max(10f, maxDepth * plugin.getPartialHelmetFrontDepth() / 100f)
			: maxDepth;

		return new HeadRegion(
			top,
			top + height,
			halfWidth,
			maxDepth,
			faceHalfWidth,
			faceDepth,
			frontFaceDepth,
			visibleHeadgear,
			helmetDrop);
	}

	private static FaceSwapHeadDirection getTriangleDirection(float ax, float az, float bx, float bz, float cx, float cz)
	{
		float centerX = (ax + bx + cx) / 3f;
		float centerZ = (az + bz + cz) / 3f;
		if (Math.abs(centerX) > Math.abs(centerZ))
		{
			return centerX < 0 ? FaceSwapHeadDirection.LEFT : FaceSwapHeadDirection.RIGHT;
		}
		return centerZ <= 0 ? FaceSwapHeadDirection.FRONT : FaceSwapHeadDirection.BACK;
	}

	private static boolean isHeadVertex(float x, float y, float z, float headTop, float headBottom, float halfWidth, float maxDepth)
	{
		return y >= headTop
			&& y <= headBottom
			&& Math.abs(x) <= halfWidth
			&& Math.abs(z) <= maxDepth;
	}

	private static boolean isHeadTriangle(
		float ax,
		float ay,
		float az,
		float bx,
		float by,
		float bz,
		float cx,
		float cy,
		float cz,
		HeadRegion headRegion)
	{
		float centerY = (ay + by + cy) / 3f;
		float centerX = (ax + bx + cx) / 3f;
		float centerZ = (az + bz + cz) / 3f;
		return centerY >= headRegion.top
			&& centerY <= headRegion.bottom
			&& Math.abs(centerX) <= headRegion.halfWidth
			&& Math.abs(centerZ) <= headRegion.maxDepth
			&& isHeadVertex(ax, ay, az, headRegion.top - HEAD_VERTEX_MARGIN, headRegion.bottom + HEAD_VERTEX_MARGIN, headRegion.halfWidth + 4f, headRegion.maxDepth + 4f)
			&& isHeadVertex(bx, by, bz, headRegion.top - HEAD_VERTEX_MARGIN, headRegion.bottom + HEAD_VERTEX_MARGIN, headRegion.halfWidth + 4f, headRegion.maxDepth + 4f)
			&& isHeadVertex(cx, cy, cz, headRegion.top - HEAD_VERTEX_MARGIN, headRegion.bottom + HEAD_VERTEX_MARGIN, headRegion.halfWidth + 4f, headRegion.maxDepth + 4f);
	}

	private float[] getTexturePoint(
		float x,
		float y,
		float z,
		FaceSwapHeadDirection direction,
		HeadRegion headRegion,
		BufferedImage image)
	{
		float textureHalfWidth = headRegion.headgearAdjusted ? headRegion.faceHalfWidth : headRegion.halfWidth;
		float horizontal;
		if (direction == FaceSwapHeadDirection.LEFT || direction == FaceSwapHeadDirection.RIGHT)
		{
			horizontal = (z + headRegion.maxDepth) / (headRegion.maxDepth * 2f);
		}
		else
		{
			horizontal = (x + textureHalfWidth) / (textureHalfWidth * 2f);
			if (direction == FaceSwapHeadDirection.BACK)
			{
				horizontal = 1f - horizontal;
			}
		}

		float normalizedVertical = clamp01((y - headRegion.top) / Math.max(1f, headRegion.bottom - headRegion.top));
		float textureTop = headRegion.headgearAdjusted ? plugin.getPartialHelmetTextureTop() / 100f : 0f;
		float textureBottom = headRegion.headgearAdjusted ? plugin.getPartialHelmetTextureBottom() / 100f : TEXTURE_VERTICAL_RANGE;
		float vertical = textureTop + normalizedVertical * (textureBottom - textureTop);
		float verticalCenter = textureTop + (textureBottom - textureTop) / 2f;
		float verticalScale = plugin.getWrapTextureHeightScale() / 100f;
		float scaledVertical = verticalCenter + (vertical - verticalCenter) / verticalScale;
		float topInfluence = clamp01((verticalCenter - scaledVertical) / verticalCenter);
		int textureLift = headRegion.headgearAdjusted
			? plugin.getPartialHelmetTextureLift()
			: plugin.getWrapTextureLift();
		return new float[]
		{
			clamp01(horizontal) * image.getWidth() + plugin.getWrapTextureXOffset(),
			scaledVertical * image.getHeight() + textureLift + topInfluence * plugin.getWrapTextureTopBias()
		};
	}

	private static float clamp01(float value)
	{
		return Math.max(0f, Math.min(1f, value));
	}

	private Point liftWrapPoint(Point point, double liftScale)
	{
		if (point == null)
		{
			return null;
		}
		int scaledLift = (int) Math.round(plugin.getWrapScreenLift() * liftScale);
		return new Point(point.getX(), point.getY() - scaledLift);
	}

	private void fillProjectedHeadShield(
		Graphics2D graphics,
		WorldView worldView,
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		HeadRegion headRegion,
		Color backingColor,
		double liftScale)
	{
		List<java.awt.Point> points = getProjectedShieldPoints(worldView, localLocation, tileHeight, orientation, headRegion, liftScale);
		int expansion = plugin.getWrapBackingExpansion();
		fillBackingOval(graphics, points, backingColor, expansion);
		fillBackingHull(graphics, points, backingColor, expansion);
	}

	private List<java.awt.Point> getProjectedShieldPoints(
		WorldView worldView,
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		HeadRegion headRegion,
		double liftScale)
	{
		float x = headRegion.halfWidth * SHIELD_WIDTH_SCALE;
		float z = headRegion.maxDepth * SHIELD_DEPTH_SCALE;
		float top = headRegion.top - SHIELD_TOP_PADDING;
		float bottom = headRegion.bottom + SHIELD_BOTTOM_PADDING;
		List<java.awt.Point> points = new ArrayList<>();
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, -x, top, -z, liftScale);
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, x, top, -z, liftScale);
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, -x, bottom, -z, liftScale);
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, x, bottom, -z, liftScale);
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, -x, top, z, liftScale);
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, x, top, z, liftScale);
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, -x, bottom, z, liftScale);
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, x, bottom, z, liftScale);
		return points;
	}

	private Shape getPartialHelmetClip(
		WorldView worldView,
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		HeadRegion headRegion,
		double liftScale)
	{
		float x = headRegion.faceHalfWidth;
		float z = -headRegion.frontFaceDepth;
		float top = headRegion.top + plugin.getPartialHelmetClipTop();
		float bottom = headRegion.bottom - plugin.getPartialHelmetClipBottom();
		if (bottom <= top)
		{
			return null;
		}

		List<java.awt.Point> points = new ArrayList<>();
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, -x, top, z, liftScale);
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, x, top, z, liftScale);
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, -x, bottom, z, liftScale);
		addShieldPoint(points, worldView, localLocation, tileHeight, orientation, x, bottom, z, liftScale);
		if (points.size() < 3)
		{
			return null;
		}

		Rectangle bounds = getPointBounds(points);
		bounds.grow(plugin.getPartialHelmetBackingExpansion(), plugin.getPartialHelmetBackingExpansion());
		if (bounds.width < 3 || bounds.height < 3)
		{
			return null;
		}
		return convexHull(points);
	}

	private void addShieldPoint(
		List<java.awt.Point> points,
		WorldView worldView,
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		float x,
		float y,
		float z,
		double liftScale)
	{
		Point point = projectModelVertex(worldView, localLocation, tileHeight, orientation, x, y, z, liftScale);
		if (point != null)
		{
			points.add(new java.awt.Point(point.getX(), point.getY()));
		}
	}

	private ModelProjection projectModelVertices(
		WorldView worldView,
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		Model model,
		float[] verticesX,
		float[] verticesY,
		float[] verticesZ)
	{
		int vertexCount = model.getVerticesCount();
		int[] x2d = new int[vertexCount];
		int[] y2d = new int[vertexCount];
		Perspective.modelToCanvas(
			client,
			worldView,
			vertexCount,
			localLocation.getX(),
			localLocation.getY(),
			tileHeight,
			orientation,
			verticesX,
			verticesZ,
			verticesY,
			x2d,
			y2d);
		return new ModelProjection(x2d, y2d);
	}

	private static double getWrapLiftScale(ModelProjection projection)
	{
		Rectangle bounds = projection.getBounds();
		if (bounds == null)
		{
			return 1d;
		}

		double projectedHeight = Math.max(1d, bounds.getHeight());
		return clamp(projectedHeight / WRAP_LIFT_REFERENCE_PROJECTED_HEIGHT, 0.20d, 1.50d);
	}

	private static double clamp(double value, double min, double max)
	{
		return Math.max(min, Math.min(max, value));
	}

	private void fillBackingHull(Graphics2D graphics, List<java.awt.Point> points, Color backingColor, int expandPixels)
	{
		if (points.size() < 3)
		{
			return;
		}

		Polygon hull = expandPolygon(convexHull(new ArrayList<>(points)), expandPixels);
		if (hull.npoints < 3)
		{
			return;
		}

		graphics.setColor(backingColor);
		graphics.fillPolygon(hull);
	}

	private static void fillBackingOval(Graphics2D graphics, List<java.awt.Point> points, Color backingColor, int expandPixels)
	{
		if (points.size() < 3)
		{
			return;
		}

		Rectangle bounds = getPointBounds(points);
		int x = bounds.x - expandPixels;
		int y = bounds.y - expandPixels;
		int width = bounds.width + expandPixels * 2;
		int height = bounds.height + expandPixels * 2;
		if (width < 3 || height < 3)
		{
			return;
		}

		graphics.setColor(backingColor);
		graphics.fillOval(x, y, width, height);
	}

	private static Rectangle getPointBounds(List<java.awt.Point> points)
	{
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (java.awt.Point point : points)
		{
			minX = Math.min(minX, point.x);
			minY = Math.min(minY, point.y);
			maxX = Math.max(maxX, point.x);
			maxY = Math.max(maxY, point.y);
		}
		return new Rectangle(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
	}

	private static Polygon expandPolygon(Polygon polygon, int pixels)
	{
		Rectangle bounds = polygon.getBounds();
		double centerX = bounds.getCenterX();
		double centerY = bounds.getCenterY();
		Polygon expanded = new Polygon();
		for (int i = 0; i < polygon.npoints; i++)
		{
			double dx = polygon.xpoints[i] - centerX;
			double dy = polygon.ypoints[i] - centerY;
			double length = Math.max(1d, Math.hypot(dx, dy));
			expanded.addPoint(
				(int) Math.round(polygon.xpoints[i] + dx / length * pixels),
				(int) Math.round(polygon.ypoints[i] + dy / length * pixels));
		}
		return expanded;
	}

	private double getTriangleCameraDistance(
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		float vertexX,
		float vertexY,
		float vertexZ)
	{
		double sin = Math.sin(orientation * Perspective.UNIT);
		double cos = Math.cos(orientation * Perspective.UNIT);
		double worldX = localLocation.getX() + vertexX * cos + vertexZ * sin;
		double worldY = localLocation.getY() + vertexZ * cos - vertexX * sin;
		double worldZ = tileHeight + vertexY;
		double dx = worldX - client.getCameraX();
		double dy = worldY - client.getCameraY();
		double dz = worldZ - client.getCameraZ();
		return dx * dx + dy * dy + dz * dz;
	}

	private static boolean drawTexturedTriangle(
		Graphics2D graphics,
		BufferedImage image,
		Color backingColor,
		boolean opaqueBacking,
		float[] sourceA,
		float[] sourceB,
		float[] sourceC,
		Point targetA,
		Point targetB,
		Point targetC)
	{
		AffineTransform source = new AffineTransform(
			sourceB[0] - sourceA[0],
			sourceB[1] - sourceA[1],
			sourceC[0] - sourceA[0],
			sourceC[1] - sourceA[1],
			sourceA[0],
			sourceA[1]);
		AffineTransform target = new AffineTransform(
			targetB.getX() - targetA.getX(),
			targetB.getY() - targetA.getY(),
			targetC.getX() - targetA.getX(),
			targetC.getY() - targetA.getY(),
			targetA.getX(),
			targetA.getY());

		AffineTransform transform;
		try
		{
			transform = new AffineTransform(target);
			transform.concatenate(source.createInverse());
		}
		catch (NoninvertibleTransformException ex)
		{
			return false;
		}

		Polygon triangle = new Polygon(
			new int[] {targetA.getX(), targetB.getX(), targetC.getX()},
			new int[] {targetA.getY(), targetB.getY(), targetC.getY()},
			3);
		Shape previousClip = graphics.getClip();
		try
		{
			graphics.clip(triangle);
			if (opaqueBacking)
			{
				graphics.setColor(backingColor);
				graphics.fill(triangle);
			}
			graphics.drawImage(image, transform, null);
		}
		finally
		{
			graphics.setClip(previousClip);
		}
		return true;
	}

	private boolean renderProjectedHeadPolygon(Graphics2D graphics, Player player, BufferedImage headImage)
	{
		ProjectedHeadProjection projection = getProjectedHeadProjection(player);
		if (projection == null)
		{
			return false;
		}

		Polygon polygon = projection.polygon;
		if (polygon == null || polygon.npoints < 3)
		{
			return false;
		}

		Rectangle bounds = projection.imageBounds;
		if (bounds.width < 4 || bounds.height < 4)
		{
			return false;
		}

		Shape previousClip = graphics.getClip();
		try
		{
			graphics.setClip(polygon);
			graphics.drawImage(
				headImage,
				bounds.x,
				bounds.y,
				bounds.width,
				bounds.height,
				null);
		}
		finally
		{
			graphics.setClip(previousClip);
		}
		return true;
	}

	private Polygon getProjectedHeadPolygon(Player player)
	{
		ProjectedHeadProjection projection = getProjectedHeadProjection(player);
		return projection == null ? null : projection.polygon;
	}

	private ProjectedHeadProjection getProjectedHeadProjection(Player player)
	{
		Model model = player.getModel();
		LocalPoint localLocation = player.getLocalLocation();
		WorldView worldView = player.getWorldView();
		if (model == null || localLocation == null || worldView == null)
		{
			return null;
		}

		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		int vertexCount = model.getVerticesCount();
		if (verticesX == null || verticesY == null || verticesZ == null || vertexCount <= 0)
		{
			return null;
		}

		float minY = Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		for (int i = 0; i < vertexCount; i++)
		{
			minY = Math.min(minY, verticesY[i]);
			maxY = Math.max(maxY, verticesY[i]);
		}

		float height = maxY - minY;
		if (height <= 0)
		{
			return null;
		}

		float lowerHeadY = minY - height * 0.02f;
		float upperHeadY = minY + height * 0.12f;
		float maxHalfWidth = Math.max(22f, model.getAABB(player.getCurrentOrientation() & 2047).getExtremeX() * 0.48f);
		List<java.awt.Point> points = new ArrayList<>();
		int tileHeight = Perspective.getTileHeight(client, localLocation, worldView.getPlane())
			- player.getAnimationHeightOffset();
		int orientation = player.getCurrentOrientation() & 2047;
		double sin = Math.sin(orientation * Perspective.UNIT);
		double cos = Math.cos(orientation * Perspective.UNIT);
		float faceForward = getModelFaceForwardZ(getHeadDirection(player));

		for (int i = 0; i < vertexCount; i++)
		{
			if (verticesY[i] < lowerHeadY
				|| verticesY[i] > upperHeadY
				|| Math.abs(verticesX[i]) > maxHalfWidth
				|| !isOnVisibleHeadSide(verticesZ[i], faceForward))
			{
				continue;
			}

			Point point = projectModelVertex(
				worldView,
				localLocation,
				tileHeight,
				orientation,
				verticesX[i],
				verticesY[i],
				verticesZ[i]);
			if (point != null)
			{
				points.add(new java.awt.Point(point.getX(), point.getY()));
			}
		}

		if (points.size() < 3)
		{
			return null;
		}

		Polygon polygon = convexHull(points);
		Rectangle imageBounds = getProjectedImageBounds(
			worldView,
			localLocation,
			tileHeight,
			sin,
			cos,
			minY,
			height,
			maxHalfWidth,
			faceForward);
		if (imageBounds == null)
		{
			imageBounds = polygon.getBounds();
		}
		return new ProjectedHeadProjection(polygon, imageBounds);
	}

	private static boolean isOnVisibleHeadSide(float vertexZ, float faceForward)
	{
		if (Math.abs(faceForward) < 0.1f)
		{
			return true;
		}
		return Math.signum(vertexZ) == Math.signum(faceForward) || Math.abs(vertexZ) < 8f;
	}

	private static float getModelFaceForwardZ(FaceSwapHeadDirection direction)
	{
		switch (direction)
		{
			case BACK:
				return 1f;
			case FRONT:
				return -1f;
			case LEFT:
			case RIGHT:
			default:
				return 0f;
		}
	}

	private Rectangle getProjectedImageBounds(
		WorldView worldView,
		LocalPoint localLocation,
		int tileHeight,
		double sin,
		double cos,
		float minY,
		float height,
		float halfWidth,
		float faceForward)
	{
		float topY = minY + height * 0.04f;
		float bottomY = minY + height * 0.26f;
		float centerZ = faceForward * 18f;
		Point topLeft = projectModelPoint(worldView, localLocation, tileHeight, sin, cos, -halfWidth, topY, centerZ);
		Point topRight = projectModelPoint(worldView, localLocation, tileHeight, sin, cos, halfWidth, topY, centerZ);
		Point bottomLeft = projectModelPoint(worldView, localLocation, tileHeight, sin, cos, -halfWidth, bottomY, centerZ);
		Point bottomRight = projectModelPoint(worldView, localLocation, tileHeight, sin, cos, halfWidth, bottomY, centerZ);
		if (topLeft == null || topRight == null || bottomLeft == null || bottomRight == null)
		{
			return null;
		}

		int minX = Math.min(Math.min(topLeft.getX(), topRight.getX()), Math.min(bottomLeft.getX(), bottomRight.getX()));
		int maxX = Math.max(Math.max(topLeft.getX(), topRight.getX()), Math.max(bottomLeft.getX(), bottomRight.getX()));
		int minScreenY = Math.min(Math.min(topLeft.getY(), topRight.getY()), Math.min(bottomLeft.getY(), bottomRight.getY()));
		int maxScreenY = Math.max(Math.max(topLeft.getY(), topRight.getY()), Math.max(bottomLeft.getY(), bottomRight.getY()));
		return new Rectangle(minX, minScreenY, Math.max(1, maxX - minX), Math.max(1, maxScreenY - minScreenY));
	}

	private Point projectModelPoint(
		WorldView worldView,
		LocalPoint localLocation,
		int tileHeight,
		double sin,
		double cos,
		float vertexX,
		float vertexY,
		float vertexZ)
	{
		int worldX = localLocation.getX() + (int) Math.round(vertexX * cos + vertexZ * sin);
		int worldY = localLocation.getY() + (int) Math.round(vertexZ * cos - vertexX * sin);
		int worldZ = tileHeight + Math.round(vertexY);
		return Perspective.localToCanvas(client, worldView.getId(), worldX, worldY, worldZ);
	}

	private Point projectModelVertex(
		WorldView worldView,
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		float vertexX,
		float vertexY,
		float vertexZ)
	{
		// Only Wraparound geometry uses its screen-space polygon lift. Applying a
		// fixed pixel lift to masks makes the offset grow relative to the actor as
		// the camera zooms out.
		return projectModelVertex(worldView, localLocation, tileHeight, orientation, vertexX, vertexY, vertexZ, 0d);
	}

	private Point projectModelVertex(
		WorldView worldView,
		LocalPoint localLocation,
		int tileHeight,
		int orientation,
		float vertexX,
		float vertexY,
		float vertexZ,
		double liftScale)
	{
		float[] x3d = new float[] {vertexX};
		float[] y3d = new float[] {vertexZ};
		float[] z3d = new float[] {vertexY};
		int[] x2d = new int[1];
		int[] y2d = new int[1];
		Perspective.modelToCanvas(
			client,
			worldView,
			1,
			localLocation.getX(),
			localLocation.getY(),
			tileHeight,
			orientation,
			x3d,
			y3d,
			z3d,
			x2d,
			y2d);
		if (x2d[0] == Integer.MIN_VALUE || y2d[0] == Integer.MIN_VALUE)
		{
			return null;
		}
		return liftWrapPoint(new Point(x2d[0], y2d[0]), liftScale);
	}

	private static Polygon convexHull(List<java.awt.Point> points)
	{
		points.sort(Comparator.comparingInt((java.awt.Point point) -> point.x).thenComparingInt(point -> point.y));
		List<java.awt.Point> hull = new ArrayList<>();
		for (java.awt.Point point : points)
		{
			while (hull.size() >= 2 && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), point) <= 0)
			{
				hull.remove(hull.size() - 1);
			}
			hull.add(point);
		}
		int lowerSize = hull.size();
		for (int i = points.size() - 2; i >= 0; i--)
		{
			java.awt.Point point = points.get(i);
			while (hull.size() > lowerSize && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), point) <= 0)
			{
				hull.remove(hull.size() - 1);
			}
			hull.add(point);
		}
		if (!hull.isEmpty())
		{
			hull.remove(hull.size() - 1);
		}

		Polygon polygon = new Polygon();
		for (java.awt.Point point : hull)
		{
			polygon.addPoint(point.x, point.y);
		}
		return polygon;
	}

	private static long cross(java.awt.Point first, java.awt.Point second, java.awt.Point third)
	{
		return (long) (second.x - first.x) * (third.y - first.y)
			- (long) (second.y - first.y) * (third.x - first.x);
	}

	private void renderProjectionDebug(Graphics2D graphics, Player player)
	{
		Model model = player.getModel();
		LocalPoint localLocation = player.getLocalLocation();
		WorldView worldView = player.getWorldView();
		if (model == null || localLocation == null || worldView == null)
		{
			return;
		}

		int tileHeight = Perspective.getTileHeight(client, localLocation, worldView.getPlane())
			- player.getAnimationHeightOffset();
		int orientation = player.getCurrentOrientation() & 2047;
		Point ground = Perspective.localToCanvas(client, worldView.getId(), localLocation.getX(), localLocation.getY(), tileHeight);
		Point anchor = getFaceAnchor(player);
		if (ground != null)
		{
			drawDebugPoint(graphics, ground.getX(), ground.getY(), Color.WHITE, "ground");
		}
		if (anchor != null)
		{
			drawDebugPoint(graphics, anchor.getX(), anchor.getY(), Color.RED, "anchor");
		}

		ModelBounds bounds = getModelBounds(model);
		if (bounds == null)
		{
			return;
		}
		HeadRegion headRegion = getHeadRegion(bounds, player);

		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		if (verticesX == null || verticesY == null || verticesZ == null)
		{
			return;
		}

		ModelProjection modelProjection = projectModelVertices(worldView, localLocation, tileHeight, orientation, model, verticesX, verticesY, verticesZ);
		double liftScale = modelProjection == null ? 1d : getWrapLiftScale(modelProjection);
		WrapDebugProjection wrapProjection = getWrapDebugProjection(player, model, localLocation, worldView, bounds, tileHeight, orientation, liftScale);
		if (wrapProjection != null)
		{
			renderWrapDebugProjection(graphics, wrapProjection);
		}

		List<DebugPoint> debugPoints = new ArrayList<>();
		for (int i = 0; i < model.getVerticesCount() && debugPoints.size() < MAX_DEBUG_POINTS; i++)
		{
			if (!isDebugHeadCandidate(verticesX[i], verticesY[i], verticesZ[i], headRegion))
			{
				continue;
			}

			Point point = projectModelVertex(
				worldView,
				localLocation,
				tileHeight,
				orientation,
				verticesX[i],
				verticesY[i],
				verticesZ[i],
				liftScale);
			if (point == null)
			{
				continue;
			}

			debugPoints.add(new DebugPoint(
				point.getX(),
				point.getY(),
				Math.round(verticesX[i]),
				Math.round(verticesY[i]),
				Math.round(verticesZ[i])));
		}
		renderDebugPointList(graphics, debugPoints);

		if (anchor != null)
		{
			graphics.setColor(Color.WHITE);
			graphics.drawString(
				"x[" + Math.round(bounds.minX) + "," + Math.round(bounds.maxX) + "] "
					+ "y[" + Math.round(bounds.minY) + "," + Math.round(bounds.maxY) + "] "
					+ "z[" + Math.round(bounds.minZ) + "," + Math.round(bounds.maxZ) + "] "
					+ "orient " + orientation,
				Math.max(8, anchor.getX() - 190),
				Math.max(16, anchor.getY() - 48));
		}
	}

	private WrapDebugProjection getWrapDebugProjection(
		Player player,
		Model model,
		LocalPoint localLocation,
		WorldView worldView,
		ModelBounds bounds,
		int tileHeight,
		int orientation,
		double liftScale)
	{
		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		int[] face1 = model.getFaceIndices1();
		int[] face2 = model.getFaceIndices2();
		int[] face3 = model.getFaceIndices3();
		if (verticesX == null || verticesY == null || verticesZ == null || face1 == null || face2 == null || face3 == null)
		{
			return null;
		}

		HeadRegion headRegion = getHeadRegion(bounds, player);
		boolean[] selectedVertices = new boolean[model.getVerticesCount()];
		List<java.awt.Point> projectedPoints = new ArrayList<>();
		for (int face = 0; face < model.getFaceCount(); face++)
		{
			int a = face1[face];
			int b = face2[face];
			int c = face3[face];
			if (!isHeadTriangle(
				verticesX[a], verticesY[a], verticesZ[a],
				verticesX[b], verticesY[b], verticesZ[b],
				verticesX[c], verticesY[c], verticesZ[c],
				headRegion))
			{
				continue;
			}

			selectedVertices[a] = true;
			selectedVertices[b] = true;
			selectedVertices[c] = true;
		}

		List<DebugPoint> selectedPoints = new ArrayList<>();
		for (int i = 0; i < selectedVertices.length; i++)
		{
			if (!selectedVertices[i])
			{
				continue;
			}

			Point point = projectModelVertex(
				worldView,
				localLocation,
				tileHeight,
				orientation,
				verticesX[i],
				verticesY[i],
				verticesZ[i],
				liftScale);
			if (point == null)
			{
				continue;
			}

			selectedPoints.add(new DebugPoint(
				point.getX(),
				point.getY(),
				Math.round(verticesX[i]),
				Math.round(verticesY[i]),
				Math.round(verticesZ[i])));
			projectedPoints.add(new java.awt.Point(point.getX(), point.getY()));
		}

		if (projectedPoints.size() < 3)
		{
			return null;
		}

		Polygon hull = convexHull(projectedPoints);
		Polygon expandedHull = expandPolygon(hull, plugin.getWrapBackingExpansion());
		List<java.awt.Point> shieldPoints = getProjectedShieldPoints(worldView, localLocation, tileHeight, orientation, headRegion, liftScale);
		Rectangle occluderBounds = getPointBounds(shieldPoints);
		occluderBounds.grow(plugin.getWrapBackingExpansion(), plugin.getWrapBackingExpansion());
		Shape helmetFaceShell = headRegion.headgearAdjusted
			? getPartialHelmetClip(worldView, localLocation, tileHeight, orientation, headRegion, liftScale)
			: null;
		ModelProjection modelProjection = projectModelVertices(
			worldView,
			localLocation,
			tileHeight,
			orientation,
			model,
			verticesX,
			verticesY,
			verticesZ);
		Shape helmetOcclusion = headRegion.headgearAdjusted && plugin.isHelmetOcclusionEnabled() && modelProjection != null
			? getHelmetOcclusion(
				model,
				modelProjection,
				verticesX,
				verticesY,
				verticesZ,
				face1,
				face2,
				face3,
				headRegion,
				localLocation,
				tileHeight,
				orientation,
				liftScale)
			: null;
		return new WrapDebugProjection(
			hull,
			expandedHull,
			occluderBounds,
			helmetFaceShell,
			helmetOcclusion,
			selectedPoints,
			headRegion,
			player.getCurrentOrientation() & 2047,
			liftScale);
	}

	private static boolean isDebugHeadCandidate(float x, float y, float z, HeadRegion headRegion)
	{
		return y >= headRegion.top - 8f
			&& y <= headRegion.bottom + 8f
			&& Math.abs(x) <= headRegion.halfWidth + 8f
			&& Math.abs(z) <= headRegion.maxDepth + 8f;
	}

	private static void drawDebugPoint(Graphics2D graphics, int x, int y, Color color, String label)
	{
		graphics.setColor(color);
		graphics.fillOval(x - 3, y - 3, 6, 6);
		graphics.drawString(label + " " + x + "," + y, x + 6, y - 6);
	}

	private static void renderDebugPointList(Graphics2D graphics, List<DebugPoint> points)
	{
		int count = Math.min(points.size(), 16);
		int listX = 12;
		int listY = 52;
		for (int i = 0; i < count; i++)
		{
			DebugPoint point = points.get(i);
			int labelY = listY + i * 14;
			graphics.setColor(Color.CYAN);
			graphics.fillOval(point.screenX - 2, point.screenY - 2, 4, 4);
			graphics.drawLine(point.screenX, point.screenY, listX + 78, labelY - 4);
			graphics.setColor(Color.WHITE);
			graphics.drawString(i + ": x" + point.modelX + " y" + point.modelY + " z" + point.modelZ, listX, labelY);
		}
	}

	private void renderWrapDebugProjection(Graphics2D graphics, WrapDebugProjection projection)
	{
		graphics.setColor(new Color(255, 145, 0, 220));
		graphics.drawPolygon(projection.hull);
		graphics.setColor(new Color(255, 70, 210, 220));
		graphics.drawPolygon(projection.expandedHull);
		graphics.setColor(new Color(80, 255, 120, 220));
		graphics.drawOval(
			projection.occluderBounds.x,
			projection.occluderBounds.y,
			projection.occluderBounds.width,
			projection.occluderBounds.height);
		if (projection.helmetFaceShell != null)
		{
			graphics.setColor(new Color(40, 180, 255, 230));
			graphics.draw(projection.helmetFaceShell);
		}
		if (projection.helmetOcclusion != null)
		{
			graphics.setColor(new Color(255, 225, 40, 230));
			graphics.draw(projection.helmetOcclusion);
		}

		List<DebugPoint> points = projection.selectedPoints;
		int count = Math.min(points.size(), 20);
		int listX = Math.max(12, client.getCanvasWidth() - 245);
		int listY = 52;
		graphics.setColor(Color.WHITE);
		graphics.drawString("wrap head polygon", listX, listY - 18);
		graphics.drawString(
			"y[" + Math.round(projection.headRegion.top) + "," + Math.round(projection.headRegion.bottom) + "] "
				+ "w" + Math.round(projection.headRegion.halfWidth) + " d" + Math.round(projection.headRegion.maxDepth)
				+ " o" + projection.orientation + " liftx" + String.format("%.2f", projection.liftScale)
				+ " helm=" + projection.headRegion.headgearAdjusted + " drop=" + projection.headRegion.helmetDrop,
			listX,
			listY - 4);
		if (projection.headRegion.headgearAdjusted)
		{
			graphics.drawString(
				"hw" + plugin.getPartialHelmetWidth()
					+ " hd" + plugin.getPartialHelmetDepth()
					+ " hfd" + plugin.getPartialHelmetFrontDepth()
					+ " hp" + plugin.getPartialHelmetTopPreserve()
					+ " hh" + plugin.getPartialHelmetRegionHeight()
					+ " ht" + plugin.getPartialHelmetTextureTop()
					+ " hb" + plugin.getPartialHelmetTextureBottom()
					+ " ct" + plugin.getPartialHelmetClipTop()
					+ " cb" + plugin.getPartialHelmetClipBottom()
					+ " be" + plugin.getPartialHelmetBackingExpansion()
					+ " occ=" + plugin.isHelmetOcclusionEnabled(),
				listX,
				listY + 10);
		}

		for (int i = 0; i < count; i++)
		{
			DebugPoint point = points.get(i);
			int labelY = listY + (projection.headRegion.headgearAdjusted ? 14 : 0) + i * 14;
			graphics.setColor(new Color(255, 70, 210, 240));
			graphics.fillOval(point.screenX - 3, point.screenY - 3, 6, 6);
			graphics.drawLine(point.screenX, point.screenY, listX - 8, labelY - 4);
			graphics.setColor(Color.WHITE);
			graphics.drawString(i + ": sx" + point.screenX + " sy" + point.screenY
				+ " x" + point.modelX + " y" + point.modelY + " z" + point.modelZ, listX, labelY);
		}
	}

	private static ModelBounds getModelBounds(Model model)
	{
		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		if (verticesX == null || verticesY == null || verticesZ == null || model.getVerticesCount() <= 0)
		{
			return null;
		}

		ModelBounds bounds = new ModelBounds();
		for (int i = 0; i < model.getVerticesCount(); i++)
		{
			bounds.minX = Math.min(bounds.minX, verticesX[i]);
			bounds.maxX = Math.max(bounds.maxX, verticesX[i]);
			bounds.minY = Math.min(bounds.minY, verticesY[i]);
			bounds.maxY = Math.max(bounds.maxY, verticesY[i]);
			bounds.minZ = Math.min(bounds.minZ, verticesZ[i]);
			bounds.maxZ = Math.max(bounds.maxZ, verticesZ[i]);
		}
		return bounds;
	}

	private Point getFaceAnchor(Actor player)
	{
		LocalPoint localLocation = player.getLocalLocation();
		WorldView worldView = player.getWorldView();
		if (localLocation == null || worldView == null)
		{
			return null;
		}

		int orientation = player.getCurrentOrientation() & 2047;
		int forwardOffset = plugin.getMaskForwardOffset();
		int anchorX = localLocation.getX() + (int) Math.round(-Math.sin(orientation * Perspective.UNIT) * forwardOffset);
		int anchorY = localLocation.getY() + (int) Math.round(-Math.cos(orientation * Perspective.UNIT) * forwardOffset);
		int anchorZ = Perspective.getTileHeight(client, localLocation, worldView.getPlane())
			- player.getAnimationHeightOffset() - plugin.getHeightOffset();
		return Perspective.localToCanvas(client, worldView.getId(), anchorX, anchorY, anchorZ);
	}

	private double getProjectedOffsetScale(Actor player)
	{
		LocalPoint localLocation = player.getLocalLocation();
		WorldView worldView = player.getWorldView();
		if (localLocation == null || worldView == null)
		{
			return 1.0;
		}

		int tileHeight = Perspective.getTileHeight(client, localLocation, worldView.getPlane())
			- player.getAnimationHeightOffset();
		Point ground = Perspective.localToCanvas(
			client,
			worldView.getId(),
			localLocation.getX(),
			localLocation.getY(),
			tileHeight);
		Point head = Perspective.localToCanvas(
			client,
			worldView.getId(),
			localLocation.getX(),
			localLocation.getY(),
			tileHeight - plugin.getHeightOffset());
		if (ground == null || head == null)
		{
			return 1.0;
		}

		double projectedHeight = Math.abs(ground.getY() - head.getY());
		return Math.max(0.25, Math.min(2.5, projectedHeight / 72.0));
	}

	static int resolveMaskCalibration(int configuredValue, int profileValue)
	{
		return configuredValue == 0 ? profileValue : configuredValue;
	}

	static int resolveMaskPitchCalibration(int configuredValue, int profileValue)
	{
		return profileValue != 0 && configuredValue == 0 ? profileValue : configuredValue;
	}

	static boolean usesLegacyHelmetMaskCalibration(MaskTrackingMode trackingMode, boolean playerActor)
	{
		return trackingMode == MaskTrackingMode.MERGED_MODEL
			|| (trackingMode == MaskTrackingMode.AUTO && !playerActor);
	}

	private FaceSwapHeadDirection getHeadDirection(Actor player)
	{
		int cameraOrientation = (client.getCameraYaw() >> 3) & 2047;
		int relativeAngle = (player.getCurrentOrientation() - cameraOrientation) & 2047;
		if (relativeAngle < 256 || relativeAngle >= 1792)
		{
			return FaceSwapHeadDirection.FRONT;
		}
		if (relativeAngle < 768)
		{
			return FaceSwapHeadDirection.LEFT;
		}
		if (relativeAngle < 1280)
		{
			return FaceSwapHeadDirection.BACK;
		}
		return FaceSwapHeadDirection.RIGHT;
	}

	static boolean isRaspberryAnimation(int animationId)
	{
		return animationId == AnimationID.EMOTE_YA_BOO_SUCKS
			|| animationId == AnimationID.EMOTE_YA_BOO_SUCKS_LOOP;
	}

	static boolean isBunnyHopAnimation(int animationId)
	{
		return animationId == AnimationID.RABBIT_EMOTE;
	}

	private static final class ModelBounds
	{
		private float minX = Float.MAX_VALUE;
		private float maxX = -Float.MAX_VALUE;
		private float minY = Float.MAX_VALUE;
		private float maxY = -Float.MAX_VALUE;
		private float minZ = Float.MAX_VALUE;
		private float maxZ = -Float.MAX_VALUE;
	}

	private static final class ProjectedHeadProjection
	{
		private final Polygon polygon;
		private final Rectangle imageBounds;

		private ProjectedHeadProjection(Polygon polygon, Rectangle imageBounds)
		{
			this.polygon = polygon;
			this.imageBounds = imageBounds;
		}
	}

	private static final class HeadRegion
	{
		private final float top;
		private final float bottom;
		private final float halfWidth;
		private final float maxDepth;
		private final float faceHalfWidth;
		private final float faceDepth;
		private final float frontFaceDepth;
		private final boolean headgearAdjusted;
		private final int helmetDrop;

		private HeadRegion(
			float top,
			float bottom,
			float halfWidth,
			float maxDepth,
			float faceHalfWidth,
			float faceDepth,
			float frontFaceDepth,
			boolean headgearAdjusted,
			int helmetDrop)
		{
			this.top = top;
			this.bottom = bottom;
			this.halfWidth = halfWidth;
			this.maxDepth = maxDepth;
			this.faceHalfWidth = faceHalfWidth;
			this.faceDepth = faceDepth;
			this.frontFaceDepth = frontFaceDepth;
			this.headgearAdjusted = headgearAdjusted;
			this.helmetDrop = helmetDrop;
		}
	}

	private static final class TexturedTriangle
	{
		private final BufferedImage image;
		private final Color backingColor;
		private final float[] sourceA;
		private final float[] sourceB;
		private final float[] sourceC;
		private final Point targetA;
		private final Point targetB;
		private final Point targetC;
		private final double cameraDistance;

		private TexturedTriangle(
			BufferedImage image,
			Color backingColor,
			float[] sourceA,
			float[] sourceB,
			float[] sourceC,
			Point targetA,
			Point targetB,
			Point targetC,
			double cameraDistance)
		{
			this.image = image;
			this.backingColor = backingColor;
			this.sourceA = sourceA;
			this.sourceB = sourceB;
			this.sourceC = sourceC;
			this.targetA = targetA;
			this.targetB = targetB;
			this.targetC = targetC;
			this.cameraDistance = cameraDistance;
		}
	}

	private static final class ModelProjection
	{
		private final int[] x2d;
		private final int[] y2d;

		private ModelProjection(int[] x2d, int[] y2d)
		{
			this.x2d = x2d;
			this.y2d = y2d;
		}

		private Point getPoint(int vertex)
		{
			int x = x2d[vertex];
			int y = y2d[vertex];
			if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
			{
				return null;
			}
			return new Point(x, y);
		}

		private Rectangle getBounds()
		{
			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxY = Integer.MIN_VALUE;
			for (int i = 0; i < x2d.length; i++)
			{
				int x = x2d[i];
				int y = y2d[i];
				if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE)
				{
					continue;
				}

				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
			}

			if (minX == Integer.MAX_VALUE)
			{
				return null;
			}
			return new Rectangle(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
		}
	}

	@FunctionalInterface
	private interface MaskPoseTracker
	{
		MaskHeadPose getPose(Actor actor, Model model, ModelBounds bounds);
	}

	private static final class MaskHeadBinding
	{
		private final int vertexCount;
		private final int faceCount;
		private final int headItemId;
		private final int appearanceFingerprint;
		private final int[] vertexIndices;
		private final int[] trackingIndices;
		private final int maximumVertexIndex;
		private final float baselineX;
		private final float baselineY;
		private final float baselineZ;
		private final int[] leftIndices;
		private final int[] rightIndices;
		private final int[] topIndices;
		private final int[] bottomIndices;
		private final int[] frontIndices;
		private final int[] backIndices;

		private MaskHeadBinding(
			int vertexCount,
			int faceCount,
			int headItemId,
			int appearanceFingerprint,
			int[] vertexIndices,
			float[] verticesX,
			float[] verticesY,
			float[] verticesZ)
		{
			this.vertexCount = vertexCount;
			this.faceCount = faceCount;
			this.headItemId = headItemId;
			this.appearanceFingerprint = appearanceFingerprint;
			this.vertexIndices = vertexIndices;
			this.trackingIndices = selectUpperTrackingIndices(vertexIndices, verticesY);
			this.maximumVertexIndex = Arrays.stream(vertexIndices).max().orElse(-1);
			MaskVector baseline = getCenter(trackingIndices, verticesX, verticesY, verticesZ);
			this.baselineX = baseline.x;
			this.baselineY = baseline.y;
			this.baselineZ = baseline.z;
			this.leftIndices = selectHalf(vertexIndices, verticesX, baseline.x, true);
			this.rightIndices = selectHalf(vertexIndices, verticesX, baseline.x, false);
			this.topIndices = selectHalf(vertexIndices, verticesY, baseline.y, true);
			this.bottomIndices = selectHalf(vertexIndices, verticesY, baseline.y, false);
			this.frontIndices = selectHalf(vertexIndices, verticesZ, baseline.z, true);
			this.backIndices = selectHalf(vertexIndices, verticesZ, baseline.z, false);
		}

		private boolean matches(Model model, int currentHeadItemId, int currentAppearanceFingerprint)
		{
			if (headItemId != currentHeadItemId
				|| vertexCount != model.getVerticesCount()
				|| faceCount != model.getFaceCount()
				|| maximumVertexIndex >= model.getVerticesCount())
			{
				return false;
			}
			if (appearanceFingerprint != NO_APPEARANCE_FINGERPRINT)
			{
				return appearanceFingerprint == currentAppearanceFingerprint;
			}
			return vertexCount == model.getVerticesCount() && faceCount == model.getFaceCount();
		}

		private boolean canRead(Model model)
		{
			return maximumVertexIndex < model.getVerticesCount();
		}

		private MaskHeadPose getPose(
			float[] verticesX,
			float[] verticesY,
			float[] verticesZ,
			float configuredCenterY)
		{
			MaskVector center = getCenter(trackingIndices, verticesX, verticesY, verticesZ);
			return createPose(verticesX, verticesY, verticesZ,
				center.x - baselineX,
				configuredCenterY + center.y - baselineY,
				center.z - baselineZ);
		}

		private MaskHeadPose getAbsolutePose(
			float[] verticesX,
			float[] verticesY,
			float[] verticesZ,
			float configuredCenterY)
		{
			MaskVector center = getCenter(trackingIndices, verticesX, verticesY, verticesZ);
			return createPose(verticesX, verticesY, verticesZ, center.x, center.y, center.z);
		}

		private MaskHeadPose createPose(
			float[] verticesX,
			float[] verticesY,
			float[] verticesZ,
			float centerX,
			float centerY,
			float centerZ)
		{
			MaskVector left = getCenter(leftIndices, verticesX, verticesY, verticesZ);
			MaskVector right = getCenter(rightIndices, verticesX, verticesY, verticesZ);
			MaskVector top = getCenter(topIndices, verticesX, verticesY, verticesZ);
			MaskVector bottom = getCenter(bottomIndices, verticesX, verticesY, verticesZ);
			MaskVector front = getCenter(frontIndices, verticesX, verticesY, verticesZ);
			MaskVector back = getCenter(backIndices, verticesX, verticesY, verticesZ);
			MaskVector xAxis = right.subtract(left).normalize(MaskVector.X_AXIS);
			MaskVector yRaw = bottom.subtract(top);
			MaskVector yAxis = yRaw.subtract(xAxis.scale(yRaw.dot(xAxis))).normalize(MaskVector.Y_AXIS);
			MaskVector zAxis = xAxis.cross(yAxis).normalize(MaskVector.Z_AXIS);
			if (zAxis.dot(back.subtract(front)) < 0f)
			{
				zAxis = zAxis.scale(-1f);
			}
			return new MaskHeadPose(centerX, centerY, centerZ, xAxis, yAxis, zAxis);
		}

		private static MaskVector getCenter(
			int[] indices,
			float[] verticesX,
			float[] verticesY,
			float[] verticesZ)
		{
			float x = 0f;
			float y = 0f;
			float z = 0f;
			for (int index : indices)
			{
				x += verticesX[index];
				y += verticesY[index];
				z += verticesZ[index];
			}
			float divisor = Math.max(1, indices.length);
			return new MaskVector(x / divisor, y / divisor, z / divisor);
		}

		private static int[] selectHalf(int[] indices, float[] values, float center, boolean lower)
		{
			int count = 0;
			for (int index : indices)
			{
				if ((values[index] <= center) == lower)
				{
					count++;
				}
			}
			if (count == 0)
			{
				return indices;
			}
			int[] selected = new int[count];
			for (int index : indices)
			{
				if ((values[index] <= center) == lower)
				{
					selected[selected.length - count--] = index;
				}
			}
			return selected;
		}

		private static int[] selectUpperTrackingIndices(int[] indices, float[] verticesY)
		{
			float minimumY = Float.POSITIVE_INFINITY;
			float maximumY = Float.NEGATIVE_INFINITY;
			for (int index : indices)
			{
				minimumY = Math.min(minimumY, verticesY[index]);
				maximumY = Math.max(maximumY, verticesY[index]);
			}

			float cutoffY = minimumY + (maximumY - minimumY) * 0.45f;
			int count = 0;
			for (int index : indices)
			{
				if (verticesY[index] <= cutoffY)
				{
					count++;
				}
			}
			if (count < 6)
			{
				return indices;
			}

			int[] selected = new int[count];
			int destination = 0;
			for (int index : indices)
			{
				if (verticesY[index] <= cutoffY)
				{
					selected[destination++] = index;
				}
			}
			return selected;
		}
	}

	private static final class MaskHeadPose
	{
		private static final MaskHeadPose DEFAULT = new MaskHeadPose(
			0f, 0f, 0f, MaskVector.X_AXIS, MaskVector.Y_AXIS, MaskVector.Z_AXIS);
		private final float centerX;
		private final float centerY;
		private final float centerZ;
		private final MaskVector xAxis;
		private final MaskVector yAxis;
		private final MaskVector zAxis;

		private MaskHeadPose(
			float centerX,
			float centerY,
			float centerZ,
			MaskVector xAxis,
			MaskVector yAxis,
			MaskVector zAxis)
		{
			this.centerX = centerX;
			this.centerY = centerY;
			this.centerZ = centerZ;
			this.xAxis = xAxis;
			this.yAxis = yAxis;
			this.zAxis = zAxis;
		}

		private float centerDistanceSquared(MaskHeadPose other)
		{
			float deltaX = centerX - other.centerX;
			float deltaY = centerY - other.centerY;
			float deltaZ = centerZ - other.centerZ;
			return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
		}

		private MaskHeadPose withCenterY(float y)
		{
			return new MaskHeadPose(centerX, y, centerZ, xAxis, yAxis, zAxis);
		}

		private boolean isFinite()
		{
			return Float.isFinite(centerX)
				&& Float.isFinite(centerY)
				&& Float.isFinite(centerZ)
				&& xAxis.isFinite()
				&& yAxis.isFinite()
				&& zAxis.isFinite();
		}

		private MaskHeadPose withAxesFrom(MaskHeadPose other)
		{
			return new MaskHeadPose(centerX, centerY, centerZ, other.xAxis, other.yAxis, other.zAxis);
		}

		private MaskHeadPose withYawOnlyAxes()
		{
			MaskVector horizontalX = new MaskVector(xAxis.x, 0f, xAxis.z).normalize(MaskVector.X_AXIS);
			MaskVector horizontalZ = horizontalX.cross(MaskVector.Y_AXIS).normalize(MaskVector.Z_AXIS);
			if (horizontalZ.dot(zAxis) < 0f)
			{
				horizontalX = horizontalX.scale(-1f);
				horizontalZ = horizontalZ.scale(-1f);
			}
			return new MaskHeadPose(
				centerX, centerY, centerZ, horizontalX, MaskVector.Y_AXIS, horizontalZ);
		}

		private MaskHeadPose withActorAxes()
		{
			return new MaskHeadPose(
				centerX, centerY, centerZ,
				MaskVector.X_AXIS, MaskVector.Y_AXIS, MaskVector.Z_AXIS);
		}

		private MaskHeadPose withPitchDegrees(float degrees)
		{
			double radians = Math.toRadians(degrees);
			float sine = (float) Math.sin(radians);
			float cosine = (float) Math.cos(radians);
			MaskVector pitchedY = new MaskVector(
				yAxis.x * cosine + zAxis.x * sine,
				yAxis.y * cosine + zAxis.y * sine,
				yAxis.z * cosine + zAxis.z * sine).normalize(yAxis);
			MaskVector pitchedZ = new MaskVector(
				zAxis.x * cosine - yAxis.x * sine,
				zAxis.y * cosine - yAxis.y * sine,
				zAxis.z * cosine - yAxis.z * sine).normalize(zAxis);
			return new MaskHeadPose(centerX, centerY, centerZ, xAxis, pitchedY, pitchedZ);
		}

		private MaskHeadPose withYawDegrees(float degrees)
		{
			double radians = Math.toRadians(degrees);
			float sine = (float) Math.sin(radians);
			float cosine = (float) Math.cos(radians);
			MaskVector yawedX = new MaskVector(
				xAxis.x * cosine - zAxis.x * sine,
				xAxis.y * cosine - zAxis.y * sine,
				xAxis.z * cosine - zAxis.z * sine).normalize(xAxis);
			MaskVector yawedZ = new MaskVector(
				xAxis.x * sine + zAxis.x * cosine,
				xAxis.y * sine + zAxis.y * cosine,
				xAxis.z * sine + zAxis.z * cosine).normalize(zAxis);
			return new MaskHeadPose(centerX, centerY, centerZ, yawedX, yAxis, yawedZ);
		}

		private MaskHeadPose withRollDegrees(float degrees)
		{
			double radians = Math.toRadians(degrees);
			float sine = (float) Math.sin(radians);
			float cosine = (float) Math.cos(radians);
			MaskVector rolledX = new MaskVector(
				xAxis.x * cosine + yAxis.x * sine,
				xAxis.y * cosine + yAxis.y * sine,
				xAxis.z * cosine + yAxis.z * sine).normalize(xAxis);
			MaskVector rolledY = new MaskVector(
				yAxis.x * cosine - xAxis.x * sine,
				yAxis.y * cosine - xAxis.y * sine,
				yAxis.z * cosine - xAxis.z * sine).normalize(yAxis);
			return new MaskHeadPose(centerX, centerY, centerZ, rolledX, rolledY, zAxis);
		}

		private MaskVector transform(float x, float y, float z)
		{
			return new MaskVector(
				centerX + xAxis.x * x + yAxis.x * y + zAxis.x * z,
				centerY + xAxis.y * x + yAxis.y * y + zAxis.y * z,
				centerZ + xAxis.z * x + yAxis.z * y + zAxis.z * z);
		}
	}

	private static final class MaskVector
	{
		private static final MaskVector X_AXIS = new MaskVector(1f, 0f, 0f);
		private static final MaskVector Y_AXIS = new MaskVector(0f, 1f, 0f);
		private static final MaskVector Z_AXIS = new MaskVector(0f, 0f, 1f);
		private final float x;
		private final float y;
		private final float z;

		private boolean isFinite()
		{
			return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z);
		}

		private MaskVector(float x, float y, float z)
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}

		private MaskVector subtract(MaskVector other)
		{
			return new MaskVector(x - other.x, y - other.y, z - other.z);
		}

		private MaskVector scale(float factor)
		{
			return new MaskVector(x * factor, y * factor, z * factor);
		}

		private float dot(MaskVector other)
		{
			return x * other.x + y * other.y + z * other.z;
		}

		private MaskVector cross(MaskVector other)
		{
			return new MaskVector(
				y * other.z - z * other.y,
				z * other.x - x * other.z,
				x * other.y - y * other.x);
		}

		private MaskVector normalize(MaskVector fallback)
		{
			float length = (float) Math.sqrt(dot(this));
			return length < 0.001f ? fallback : scale(1f / length);
		}
	}

	private static final class WrapDebugProjection
	{
		private final Polygon hull;
		private final Polygon expandedHull;
		private final Rectangle occluderBounds;
		private final Shape helmetFaceShell;
		private final Shape helmetOcclusion;
		private final List<DebugPoint> selectedPoints;
		private final HeadRegion headRegion;
		private final int orientation;
		private final double liftScale;

		private WrapDebugProjection(
			Polygon hull,
			Polygon expandedHull,
			Rectangle occluderBounds,
			Shape helmetFaceShell,
			Shape helmetOcclusion,
			List<DebugPoint> selectedPoints,
			HeadRegion headRegion,
			int orientation,
			double liftScale)
		{
			this.hull = hull;
			this.expandedHull = expandedHull;
			this.occluderBounds = occluderBounds;
			this.helmetFaceShell = helmetFaceShell;
			this.helmetOcclusion = helmetOcclusion;
			this.selectedPoints = selectedPoints;
			this.headRegion = headRegion;
			this.orientation = orientation;
			this.liftScale = liftScale;
		}
	}

	private static final class DebugPoint
	{
		private final int screenX;
		private final int screenY;
		private final int modelX;
		private final int modelY;
		private final int modelZ;

		private DebugPoint(int screenX, int screenY, int modelX, int modelY, int modelZ)
		{
			this.screenX = screenX;
			this.screenY = screenY;
			this.modelX = modelX;
			this.modelY = modelY;
			this.modelZ = modelZ;
		}
	}
}
