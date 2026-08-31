package com.faceswap;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseWheelListener;

final class FaceSwapCalibrationMouseListener extends MouseAdapter implements MouseWheelListener
{
	private final FaceSwapPlugin plugin;
	private final FaceSwapCalibrationOverlay overlay;
	private CalibrationHandle dragHandle;
	private MeshControlPoint dragPoint;
	private Point lastDragPoint;
	private boolean consumeNextClick;

	FaceSwapCalibrationMouseListener(FaceSwapPlugin plugin, FaceSwapCalibrationOverlay overlay)
	{
		this.plugin = plugin;
		this.overlay = overlay;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		if (!isCanvasEvent(event) || event.getButton() != MouseEvent.BUTTON1)
		{
			return event;
		}
		MeshControlPoint point = overlay.getMeshControlPointAt(event.getPoint());
		if (point != null)
		{
			event.consume();
			consumeNextClick = true;
			dragPoint = point;
			dragHandle = null;
			overlay.setSelectedPoint(point);
			lastDragPoint = event.getPoint();
			return event;
		}
		FaceSwapCalibrationOverlay.CalibrationControl control = overlay.getControlAt(event.getPoint());
		if (control != null)
		{
			event.consume();
			consumeNextClick = true;
			control.apply(plugin);
			return event;
		}
		CalibrationHandle handle = overlay.getHandleAt(event.getPoint());
		if (handle == null)
		{
			return event;
		}
		event.consume();
		consumeNextClick = true;
		if (handle == CalibrationHandle.SAVE)
		{
			plugin.requestSaveInteractiveHelmetPreset();
		}
		else if (handle == CalibrationHandle.RESET)
		{
			plugin.requestResetInteractiveHelmetCalibration();
		}
		else
		{
			dragHandle = handle;
			lastDragPoint = event.getPoint();
		}
		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		if ((dragHandle == null && dragPoint == null) || lastDragPoint == null || !isCanvasEvent(event))
		{
			return event;
		}
		event.consume();
		int deltaX = event.getX() - lastDragPoint.x;
		int deltaY = event.getY() - lastDragPoint.y;
		lastDragPoint = event.getPoint();
		if (deltaX != 0 || deltaY != 0)
		{
			if (dragPoint != null)
			{
				float[] meshDelta = overlay.getMeshDragDelta(dragPoint, deltaX, deltaY);
				plugin.requestInteractiveMeshDrag(dragPoint, meshDelta[0], meshDelta[1]);
			}
			else
			{
				plugin.requestInteractiveDrag(dragHandle, deltaX, deltaY);
			}
		}
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		if ((dragHandle != null || dragPoint != null || consumeNextClick) && isCanvasEvent(event)
			&& event.getButton() == MouseEvent.BUTTON1)
		{
			event.consume();
		}
		dragHandle = null;
		dragPoint = null;
		lastDragPoint = null;
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		if (isCanvasEvent(event) && event.getButton() == MouseEvent.BUTTON1 && consumeNextClick)
		{
			event.consume();
			consumeNextClick = false;
		}
		return event;
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent event)
	{
		if (isCanvasEvent(event) && overlay.isOverCalibrationHead(event.getPoint()))
		{
			event.consume();
			MeshControlPoint point = overlay.getMeshControlPointAt(event.getPoint());
			if (point != null)
			{
				overlay.setSelectedPoint(point);
				plugin.requestInteractiveMeshDepthAdjustment(point, event.getWheelRotation());
			}
			else
			{
				plugin.requestInteractiveZAdjustment(event.getWheelRotation());
			}
		}
		return event;
	}

	private boolean isCanvasEvent(MouseEvent event)
	{
		return event != null && plugin.isCanvasEvent(event);
	}
}
