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
	private static final Color PANEL = new Color(0, 0, 0, 225);
	private static final Color LABEL_BACKGROUND = new Color(8, 8, 8, 240);
	private static final Color LABEL_BORDER = new Color(255, 214, 64, 230);
	private static final Color BUTTON = new Color(38, 118, 62, 230);
	private static final Color RESET_BUTTON = new Color(105, 72, 35, 230);
	private static final Color CONTROL_BUTTON = new Color(35, 55, 85, 235);
	private static final Color SELECTED = new Color(255, 214, 64, 100);
	private static final java.awt.Font POINT_LABEL_FONT =
		new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 9);
	private static final java.awt.Font PANEL_TITLE_FONT =
		new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 11);
	private static final java.awt.Font PANEL_BODY_FONT =
		new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 9);
	private static final java.awt.Font LEGEND_FONT =
		new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 8);
	private static final java.awt.Font CONTROL_FONT =
		new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 8);

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
		java.awt.Font oldFont = graphics.getFont();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setFont(POINT_LABEL_FONT);
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
			int meshPointIndex = 1;
			for (MeshControlPoint point : MeshControlPoint.values())
			{
				java.awt.Point projectedPoint = currentLayout.meshPoints.get(point);
				graphics.setColor(point == selectedPoint ? Color.WHITE : HANDLE);
				graphics.fillOval(projectedPoint.x - HANDLE_RADIUS, projectedPoint.y - HANDLE_RADIUS,
					HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
				graphics.setColor(Color.BLACK);
				graphics.drawOval(projectedPoint.x - HANDLE_RADIUS, projectedPoint.y - HANDLE_RADIUS,
					HANDLE_RADIUS * 2, HANDLE_RADIUS * 2);
				drawMeshHandleMarker(graphics, meshPointIndex++, projectedPoint);
			}

			drawLabel(graphics, metrics, "MOVE", currentLayout.anchor, -22, -12);
			drawLabel(graphics, metrics, "W", currentLayout.points.get(CalibrationHandle.WIDTH), 10, 4);
			drawLabel(graphics, metrics, "H", currentLayout.points.get(CalibrationHandle.HEIGHT), 10, 4);
			drawLabel(graphics, metrics, "S", currentLayout.points.get(CalibrationHandle.SCALE), 10, 4);
			drawLabel(graphics, metrics, "D", currentLayout.points.get(CalibrationHandle.DEPTH), 10, 4);

			int panelX = 8;
			int panelY = 8;
			Rectangle panel = new Rectangle(panelX, panelY, 320, 286);
			graphics.setColor(PANEL);
			graphics.fillRoundRect(panel.x, panel.y, panel.width, panel.height, 8, 8);
			graphics.setColor(new Color(255, 255, 255, 220));
			graphics.drawRoundRect(panel.x, panel.y, panel.width, panel.height, 8, 8);
			graphics.setFont(PANEL_TITLE_FONT);
			graphics.setColor(Color.WHITE);
			graphics.drawString("Interactive helmet calibration", panel.x + 10, panel.y + 20);
			graphics.setFont(PANEL_BODY_FONT);
			graphics.drawString("Drag CENTER to move the head", panel.x + 10, panel.y + 37);
			graphics.drawString("Drag W / H / S / D to change the fit", panel.x + 10, panel.y + 50);
			graphics.drawString("Drag a numbered mesh point to reshape", panel.x + 10, panel.y + 63);
			graphics.drawString("Wheel on a mesh point changes its depth", panel.x + 10, panel.y + 76);
			graphics.drawString("Wheel elsewhere changes global Z", panel.x + 10, panel.y + 89);
			graphics.drawString("Pitch / Yaw / Roll: use the config panel", panel.x + 10, panel.y + 102);
			graphics.setColor(new Color(255, 255, 255, 110));
			graphics.drawLine(panel.x + 10, panel.y + 111, panel.x + panel.width - 10, panel.y + 111);
			graphics.setColor(Color.WHITE);
			graphics.drawString("Mesh points (drag the numbered handles)", panel.x + 10, panel.y + 124);
			graphics.setFont(LEGEND_FONT);
			drawMeshLegend(graphics, panel.x + 10, panel.y + 130, selectedPoint);
			graphics.setFont(PANEL_BODY_FONT);
			graphics.setColor(Color.WHITE);
			graphics.drawLine(panel.x + 10, panel.y + 164, panel.x + panel.width - 10, panel.y + 164);
			graphics.drawString("Nudge fit", panel.x + 10, panel.y + 177);
			graphics.setFont(CONTROL_FONT);
			drawControlButton(graphics, currentLayout, CalibrationControl.MOVE_LEFT,
				panel.x + 10, panel.y + 183, 28, 22);
			drawControlButton(graphics, currentLayout, CalibrationControl.MOVE_UP,
				panel.x + 42, panel.y + 183, 28, 22);
			drawControlButton(graphics, currentLayout, CalibrationControl.MOVE_DOWN,
				panel.x + 74, panel.y + 183, 28, 22);
			drawControlButton(graphics, currentLayout, CalibrationControl.MOVE_RIGHT,
				panel.x + 106, panel.y + 183, 28, 22);
			int adjustmentX = panel.x + 10;
			for (CalibrationControl control : CalibrationControl.adjustments())
			{
				drawControlButton(graphics, currentLayout, control, adjustmentX, panel.y + 210, 27, 22);
				adjustmentX += 30;
			}
			graphics.setFont(PANEL_BODY_FONT);
			String selected = selectedPoint == null
				? "Selected: none"
				: String.format("Selected: %02d %s", getMeshPointIndex(selectedPoint), selectedPoint.getLabel());
			graphics.drawString(selected, panel.x + 10, panel.y + 248);

			String status = String.format("X %d  Y %d  Z %d  W %d  H %d  S %d  D %d",
				calibration.getX(), calibration.getY(), calibration.getZ(), calibration.getWidth(),
				calibration.getFaceHeight(), calibration.getScale(), calibration.getDepth());
			graphics.drawString(status, panel.x + 10, panel.y + 262);

			int buttonY = panel.y + 263;
			currentLayout.saveButton = new Rectangle(panel.x + 10, buttonY, 132, 22);
			currentLayout.resetButton = new Rectangle(panel.x + 154, buttonY, 96, 22);
			layout = currentLayout;
			drawButton(graphics, currentLayout.saveButton, "Save preset", BUTTON);
			drawButton(graphics, currentLayout.resetButton, "Reset", RESET_BUTTON);

			if (selectedPoint != null)
			{
				graphics.setFont(POINT_LABEL_FONT);
				drawSelectedMeshLabel(graphics, graphics.getFontMetrics(), currentLayout,
					selectedPoint, client.getCanvasWidth(), client.getCanvasHeight());
			}
		}
		finally
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
			graphics.setColor(oldColor);
			graphics.setStroke(oldStroke);
			graphics.setFont(oldFont);
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

	CalibrationControl getControlAt(java.awt.Point point)
	{
		CalibrationLayout currentLayout = layout;
		if (currentLayout == CalibrationLayout.EMPTY || point == null)
		{
			return null;
		}
		for (CalibrationControl control : CalibrationControl.values())
		{
			Rectangle button = currentLayout.controlButtons.get(control);
			if (button != null && button.contains(point))
			{
				return control;
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
		int baseline = point.y + dy + metrics.getAscent() / 2;
		int width = metrics.stringWidth(text) + 8;
		int height = metrics.getHeight() + 6;
		int x = point.x + dx;
		int y = baseline - metrics.getAscent() - 3;
		graphics.setColor(LABEL_BACKGROUND);
		graphics.fillRoundRect(x, y, width, height, 5, 5);
		graphics.setColor(LABEL_BORDER);
		graphics.drawRoundRect(x, y, width, height, 5, 5);
		graphics.setColor(Color.WHITE);
		graphics.drawString(text, x + 4, baseline);
	}

	private static void drawMeshLegend(Graphics2D graphics, int x, int y, MeshControlPoint selectedPoint)
	{
		java.awt.FontMetrics metrics = graphics.getFontMetrics();
		MeshControlPoint[] points = MeshControlPoint.values();
		int rowHeight = metrics.getHeight() + 1;
		int columnWidth = 75;
		for (int index = 0; index < points.length; index++)
		{
			MeshControlPoint point = points[index];
			int column = index / 4;
			int row = index % 4;
			int entryX = x + column * columnWidth;
			int entryY = y + row * rowHeight;
			if (point == selectedPoint)
			{
				graphics.setColor(SELECTED);
				graphics.fillRoundRect(entryX - 3, entryY - metrics.getAscent(), columnWidth - 6,
					metrics.getHeight(), 4, 4);
			}
			graphics.setColor(point == selectedPoint ? Color.YELLOW : Color.WHITE);
			graphics.drawString(String.format("%02d %s", index + 1, point.getLabel()), entryX, entryY);
		}
	}

	private static int getMeshPointIndex(MeshControlPoint point)
	{
		MeshControlPoint[] points = MeshControlPoint.values();
		for (int index = 0; index < points.length; index++)
		{
			if (points[index] == point)
			{
				return index + 1;
			}
		}
		return 0;
	}

	private static void drawMeshHandleMarker(Graphics2D graphics, int index, java.awt.Point point)
	{
		String text = String.valueOf(index);
		FontMetrics metrics = graphics.getFontMetrics();
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, point.x - metrics.stringWidth(text) / 2,
			point.y + metrics.getAscent() / 2 - 1);
	}

	private static void drawSelectedMeshLabel(Graphics2D graphics, FontMetrics metrics,
		CalibrationLayout layout, MeshControlPoint point, int canvasWidth, int canvasHeight)
	{
		java.awt.Point projectedPoint = layout.meshPoints.get(point);
		String text = String.format("%02d %s", getMeshPointIndex(point), point.getLabel());
		int width = metrics.stringWidth(text) + 10;
		int height = metrics.getHeight() + 6;
		int x = projectedPoint.x + HANDLE_RADIUS + 6;
		if (x + width > canvasWidth - 8)
		{
			x = projectedPoint.x - HANDLE_RADIUS - width - 6;
		}
		x = Math.max(8, Math.min(x, canvasWidth - width - 8));
		int y = projectedPoint.y - height - 8;
		if (y < 8)
		{
			y = projectedPoint.y + HANDLE_RADIUS + 8;
		}
		y = Math.max(8, Math.min(y, canvasHeight - height - 8));
		graphics.setColor(LABEL_BACKGROUND);
		graphics.fillRoundRect(x, y, width, height, 5, 5);
		graphics.setColor(LABEL_BORDER);
		graphics.drawRoundRect(x, y, width, height, 5, 5);
		graphics.setColor(Color.WHITE);
		graphics.drawString(text, x + 5, y + metrics.getAscent() + 3);
	}

	private static void drawButton(Graphics2D graphics, Rectangle bounds, String text, Color color)
	{
		graphics.setColor(color);
		graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
		graphics.setColor(Color.WHITE);
		graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
		graphics.drawString(text, bounds.x + 8, bounds.y + 12);
	}

	private static void drawControlButton(Graphics2D graphics, CalibrationLayout layout,
		CalibrationControl control, int x, int y, int width, int height)
	{
		Rectangle bounds = new Rectangle(x, y, width, height);
		layout.controlButtons.put(control, bounds);
		graphics.setColor(CONTROL_BUTTON);
		graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 4, 4);
		graphics.setColor(new Color(255, 255, 255, 210));
		graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 4, 4);
		FontMetrics metrics = graphics.getFontMetrics();
		graphics.setColor(Color.WHITE);
		graphics.drawString(control.label, bounds.x + (bounds.width - metrics.stringWidth(control.label)) / 2,
			bounds.y + (bounds.height - metrics.getHeight()) / 2 + metrics.getAscent());
	}

	enum CalibrationControl
	{
		MOVE_LEFT("←", CalibrationHandle.MOVE, -2, 0, 0),
		MOVE_UP("↑", CalibrationHandle.MOVE, 0, -2, 0),
		MOVE_DOWN("↓", CalibrationHandle.MOVE, 0, 2, 0),
		MOVE_RIGHT("→", CalibrationHandle.MOVE, 2, 0, 0),
		WIDTH_DOWN("W-", CalibrationHandle.WIDTH, -2, 0, 0),
		WIDTH_UP("W+", CalibrationHandle.WIDTH, 2, 0, 0),
		HEIGHT_DOWN("H-", CalibrationHandle.HEIGHT, 0, 2, 0),
		HEIGHT_UP("H+", CalibrationHandle.HEIGHT, 0, -2, 0),
		SCALE_DOWN("S-", CalibrationHandle.SCALE, -2, 0, 0),
		SCALE_UP("S+", CalibrationHandle.SCALE, 2, 0, 0),
		DEPTH_DOWN("D-", CalibrationHandle.DEPTH, -2, 0, 0),
		DEPTH_UP("D+", CalibrationHandle.DEPTH, 2, 0, 0),
		Z_DOWN("Z-", null, 0, 0, 1),
		Z_UP("Z+", null, 0, 0, -1);

		private final String label;
		private final CalibrationHandle handle;
		private final int deltaX;
		private final int deltaY;
		private final int wheelRotation;

		CalibrationControl(String label, CalibrationHandle handle, int deltaX, int deltaY, int wheelRotation)
		{
			this.label = label;
			this.handle = handle;
			this.deltaX = deltaX;
			this.deltaY = deltaY;
			this.wheelRotation = wheelRotation;
		}

		static CalibrationControl[] adjustments()
		{
			return new CalibrationControl[] {
				WIDTH_DOWN, WIDTH_UP, HEIGHT_DOWN, HEIGHT_UP, SCALE_DOWN, SCALE_UP,
				DEPTH_DOWN, DEPTH_UP, Z_DOWN, Z_UP
			};
		}

		void apply(FaceSwapPlugin plugin)
		{
			if (handle == null)
			{
				plugin.requestInteractiveZAdjustment(wheelRotation);
			}
			else
			{
				plugin.requestInteractiveDrag(handle, deltaX, deltaY);
			}
		}
	}

	private static final class CalibrationLayout
	{
		private static final CalibrationLayout EMPTY = new CalibrationLayout(null, null, 0, 0, 1f);
		private final java.awt.Point anchor;
		private final java.util.Map<CalibrationHandle, java.awt.Point> points;
		private java.util.Map<MeshControlPoint, java.awt.Point> meshPoints;
		private final Rectangle bounds;
		private final float scale;
		private final java.util.Map<CalibrationControl, Rectangle> controlButtons;
		private Rectangle saveButton = new Rectangle();
		private Rectangle resetButton = new Rectangle();

		private CalibrationLayout(java.awt.Point anchor, java.util.Map<CalibrationHandle, java.awt.Point> points,
			int width, int height, float scale)
		{
			this.anchor = anchor;
			this.points = points == null ? Collections.emptyMap() : points;
			this.meshPoints = Collections.emptyMap();
			this.scale = scale;
			this.controlButtons = new java.util.EnumMap<>(CalibrationControl.class);
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
