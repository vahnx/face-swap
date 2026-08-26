package com.faceswap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Collections;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class FaceSwapCalibrationOverlay extends Overlay
{
	private static final int HANDLE_RADIUS = 8;
	private static final Color GUIDE = new Color(80, 220, 255, 225);
	private static final Color HANDLE = new Color(255, 214, 64, 245);
	private static final Color PANEL = new Color(0, 0, 0, 185);
	private static final Color BUTTON = new Color(38, 118, 62, 230);
	private static final Color RESET_BUTTON = new Color(105, 72, 35, 230);

	private final Client client;
	private final FaceSwapPlugin plugin;
	private volatile CalibrationLayout layout = CalibrationLayout.EMPTY;
	private volatile MeshControlPoint selectedPoint;

	@Inject
	FaceSwapCalibrationOverlay(Client client, FaceSwapPlugin plugin)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setMovable(false);
		setSnappable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInteractiveCalibrationActive())
		{
			layout = CalibrationLayout.EMPTY;
			return null;
		}

		Player player = client.getLocalPlayer();
		java.awt.Point anchor = getHeadAnchor(player);
		if (anchor == null)
		{
			layout = CalibrationLayout.EMPTY;
			return null;
		}

		HelmetCalibration calibration = plugin.getInteractiveCalibration();
		if (calibration == null)
		{
			layout = CalibrationLayout.EMPTY;
			return null;
		}
		float scale = getProjectionScale(player);
		CalibrationLayout currentLayout = CalibrationLayout.create(anchor, scale, calibration.getMeshCalibration());

		Object oldAntialiasing = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Color oldColor = graphics.getColor();
		java.awt.Stroke oldStroke = graphics.getStroke();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(GUIDE);
			graphics.setStroke(new BasicStroke(2f));
			graphics.draw(currentLayout.bounds);
			FontMetrics metrics = graphics.getFontMetrics();
			for (CalibrationHandle handle : CalibrationHandle.values())
			{
				if (handle == CalibrationHandle.MOVE
					|| handle == CalibrationHandle.SAVE
					|| handle == CalibrationHandle.RESET)
				{
					continue;
				}
				java.awt.Point point = currentLayout.points.get(handle);
				graphics.setColor(HANDLE);
				graphics.fillOval(point.x - HANDLE_RADIUS, point.y - HANDLE_RADIUS,
					HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
				graphics.setColor(Color.BLACK);
				graphics.drawOval(point.x - HANDLE_RADIUS, point.y - HANDLE_RADIUS,
					HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
			}
			graphics.setColor(HANDLE);
			graphics.fillOval(currentLayout.anchor.x - HANDLE_RADIUS,
				currentLayout.anchor.y - HANDLE_RADIUS, HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
			graphics.setColor(Color.BLACK);
			graphics.drawOval(currentLayout.anchor.x - HANDLE_RADIUS,
				currentLayout.anchor.y - HANDLE_RADIUS, HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
			for (MeshControlPoint point : MeshControlPoint.values())
			{
				java.awt.Point projectedPoint = currentLayout.meshPoints.get(point);
				graphics.setColor(point == selectedPoint ? Color.WHITE : HANDLE);
				graphics.fillOval(projectedPoint.x - HANDLE_RADIUS, projectedPoint.y - HANDLE_RADIUS,
					HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
				graphics.setColor(Color.BLACK);
				graphics.drawOval(projectedPoint.x - HANDLE_RADIUS, projectedPoint.y - HANDLE_RADIUS,
					HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
				drawLabel(graphics, metrics, point.getLabel(), projectedPoint, 9, 2);
			}

			drawLabel(graphics, metrics, "MOVE", currentLayout.anchor, -22, -12);
			drawLabel(graphics, metrics, "W", currentLayout.points.get(CalibrationHandle.WIDTH), 10, 4);
			drawLabel(graphics, metrics, "H", currentLayout.points.get(CalibrationHandle.HEIGHT), 10, 4);
			drawLabel(graphics, metrics, "S", currentLayout.points.get(CalibrationHandle.SCALE), 10, 4);
			drawLabel(graphics, metrics, "D", currentLayout.points.get(CalibrationHandle.DEPTH), 10, 4);

			int panelX = Math.max(8, currentLayout.bounds.x - 92);
			int panelY = Math.max(8, currentLayout.bounds.y - 120);
			Rectangle panel = new Rectangle(panelX, panelY, 224, 104);
			graphics.setColor(PANEL);
			graphics.fillRoundRect(panel.x, panel.y, panel.width, panel.height, 8, 8);
			graphics.setColor(Color.WHITE);
			graphics.drawString("Interactive helmet calibration", panel.x + 8, panel.y + 16);
			graphics.drawString("Drag points: local X/Y   Wheel over point: depth", panel.x + 8, panel.y + 32);
			graphics.drawString("Center/W/H/S/D: global fit   Wheel elsewhere: global Z", panel.x + 8, panel.y + 47);
			graphics.drawString("Pitch/Yaw/Roll remain numeric controls", panel.x + 8, panel.y + 62);
			currentLayout.saveButton = new Rectangle(panel.x + 8, panel.y + 74, 88, 16);
			currentLayout.resetButton = new Rectangle(panel.x + 104, panel.y + 74, 88, 16);
			layout = currentLayout;
			drawButton(graphics, currentLayout.saveButton, "Save preset", BUTTON);
			drawButton(graphics, currentLayout.resetButton, "Reset", RESET_BUTTON);

			int textX = Math.max(8, currentLayout.bounds.x - 92);
			int textY = Math.min(client.getCanvasHeight() - 8, currentLayout.bounds.y + currentLayout.bounds.height + 18);
			graphics.setColor(Color.WHITE);
			graphics.drawString(String.format("X %d  Y %d  Z %d  W %d  H %d  S %d  D %d",
				calibration.getX(), calibration.getY(), calibration.getZ(), calibration.getWidth(),
				calibration.getFaceHeight(), calibration.getScale(), calibration.getDepth()), textX, textY);
		}
		finally
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
			graphics.setColor(oldColor);
			graphics.setStroke(oldStroke);
		}
		return null;
	}

	MeshControlPoint getMeshControlPointAt(java.awt.Point point)
	{
		CalibrationLayout currentLayout = layout;
		if (currentLayout == CalibrationLayout.EMPTY || point == null)
		{
			return null;
		}
		for (MeshControlPoint controlPoint : MeshControlPoint.values())
		{
			java.awt.Point handlePoint = currentLayout.meshPoints.get(controlPoint);
			if (handlePoint != null && handlePoint.distance(point) <= HANDLE_RADIUS + 5)
			{
				return controlPoint;
			}
		}
		return null;
	}

	float[] getMeshDragDelta(MeshControlPoint point, int deltaX, int deltaY)
	{
		CalibrationLayout currentLayout = layout;
		if (currentLayout == CalibrationLayout.EMPTY || point == null)
		{
			return new float[] {0f, 0f};
		}
		return new float[] {
			deltaX / (2.6f * currentLayout.scale),
			deltaY / (2.4f * currentLayout.scale)
		};
	}

	void setSelectedPoint(MeshControlPoint point)
	{
		selectedPoint = point;
	}

	CalibrationHandle getHandleAt(java.awt.Point point)
	{
		CalibrationLayout currentLayout = layout;
		if (currentLayout == CalibrationLayout.EMPTY || point == null)
		{
			return null;
		}
		if (currentLayout.saveButton.contains(point))
		{
			return CalibrationHandle.SAVE;
		}
		if (currentLayout.resetButton.contains(point))
		{
			return CalibrationHandle.RESET;
		}
		for (CalibrationHandle handle : new CalibrationHandle[] {
			CalibrationHandle.MOVE, CalibrationHandle.WIDTH, CalibrationHandle.HEIGHT,
			CalibrationHandle.SCALE, CalibrationHandle.DEPTH})
		{
			java.awt.Point handlePoint = handle == CalibrationHandle.MOVE
				? currentLayout.anchor : currentLayout.points.get(handle);
			if (handlePoint != null && handlePoint.distance(point) <= HANDLE_RADIUS + 5)
			{
				return handle;
			}
		}
		return null;
	}

	boolean isOverCalibrationHead(java.awt.Point point)
	{
		CalibrationLayout currentLayout = layout;
		return currentLayout != CalibrationLayout.EMPTY
			&& point != null && currentLayout.bounds.contains(point);
	}

	private java.awt.Point getHeadAnchor(Player player)
	{
		if (player == null || player.getLocalLocation() == null || player.getWorldView() == null)
		{
			return null;
		}
		LocalPoint location = player.getLocalLocation();
		WorldView worldView = player.getWorldView();
		int tileHeight = Perspective.getTileHeight(client, location, worldView.getPlane());
		net.runelite.api.Point projected = Perspective.localToCanvas(client, worldView.getId(), location.getX(), location.getY(),
			tileHeight - player.getAnimationHeightOffset() - plugin.getHeightOffset());
		return projected == null ? null : new java.awt.Point(projected.getX(), projected.getY());
	}

	private float getProjectionScale(Player player)
	{
		if (player == null || player.getLocalLocation() == null || player.getWorldView() == null)
		{
			return 1f;
		}
		LocalPoint location = player.getLocalLocation();
		WorldView worldView = player.getWorldView();
		int tileHeight = Perspective.getTileHeight(client, location, worldView.getPlane())
			- player.getAnimationHeightOffset();
		net.runelite.api.Point ground = Perspective.localToCanvas(client, worldView.getId(), location.getX(), location.getY(), tileHeight);
		net.runelite.api.Point head = Perspective.localToCanvas(client, worldView.getId(), location.getX(), location.getY(),
			tileHeight - plugin.getHeightOffset());
		if (ground == null || head == null)
		{
			return 1f;
		}
		return Math.max(.5f, Math.min(2f, Math.abs(ground.getY() - head.getY()) / 72f));
	}

	private static void drawLabel(Graphics2D graphics, FontMetrics metrics, String text, java.awt.Point point, int dx, int dy)
	{
		graphics.setColor(Color.WHITE);
		graphics.drawString(text, point.x + dx, point.y + dy + metrics.getAscent() / 2);
	}

	private static void drawButton(Graphics2D graphics, Rectangle bounds, String text, Color color)
	{
		graphics.setColor(color);
		graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
		graphics.setColor(Color.WHITE);
		graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
		graphics.drawString(text, bounds.x + 8, bounds.y + 12);
	}

	private static final class CalibrationLayout
	{
		private static final CalibrationLayout EMPTY = new CalibrationLayout(null, null, 0, 0, 1f);
		private final java.awt.Point anchor;
		private final java.util.Map<CalibrationHandle, java.awt.Point> points;
		private java.util.Map<MeshControlPoint, java.awt.Point> meshPoints;
		private final Rectangle bounds;
		private final float scale;
		private Rectangle saveButton = new Rectangle();
		private Rectangle resetButton = new Rectangle();

		private CalibrationLayout(java.awt.Point anchor, java.util.Map<CalibrationHandle, java.awt.Point> points,
			int width, int height, float scale)
		{
			this.anchor = anchor;
			this.points = points == null ? Collections.emptyMap() : points;
			this.meshPoints = Collections.emptyMap();
			this.scale = scale;
			this.bounds = anchor == null ? new Rectangle() : new Rectangle(anchor.x - width, anchor.y - height,
				width * 2, height * 2);
		}

		private static CalibrationLayout create(java.awt.Point anchor, float scale,
			HelmetMeshCalibration meshCalibration)
		{
			int halfWidth = Math.max(26, Math.round(30 * scale));
			int halfHeight = Math.max(36, Math.round(42 * scale));
			java.util.Map<CalibrationHandle, java.awt.Point> points = new java.util.EnumMap<>(CalibrationHandle.class);
			points.put(CalibrationHandle.WIDTH, new java.awt.Point(anchor.x + halfWidth, anchor.y));
			points.put(CalibrationHandle.HEIGHT, new java.awt.Point(anchor.x, anchor.y - halfHeight));
			points.put(CalibrationHandle.SCALE, new java.awt.Point(anchor.x, anchor.y + halfHeight));
			points.put(CalibrationHandle.DEPTH, new java.awt.Point(anchor.x + halfWidth + 22, anchor.y + 18));
			CalibrationLayout layout = new CalibrationLayout(anchor, points, halfWidth + 42, halfHeight + 12, scale);
			java.util.Map<MeshControlPoint, java.awt.Point> meshPoints =
				new java.util.EnumMap<>(MeshControlPoint.class);
			for (MeshControlPoint point : MeshControlPoint.values())
			{
				meshPoints.put(point, new java.awt.Point(
					anchor.x + Math.round((point.getX() + meshCalibration.getX(point)) * 2.6f * scale),
					anchor.y + Math.round((point.getY() + meshCalibration.getY(point) + 181f) * 2.4f * scale)));
			}
			layout.meshPoints = meshPoints;
			return layout;
		}
	}
}
