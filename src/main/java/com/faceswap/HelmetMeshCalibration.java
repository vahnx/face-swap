package com.faceswap;

import java.util.EnumMap;
import java.util.Map;

/** Local weighted offsets for the stable landmarks on one helmet profile. */
final class HelmetMeshCalibration
{
	private static final int MIN_OFFSET = -32;
	private static final int MAX_OFFSET = 32;
	private static final float INFLUENCE_RADIUS_X = 8f;
	private static final float INFLUENCE_RADIUS_Y = 9f;
	private static final float INFLUENCE_RADIUS_Z = 11f;
	private final Map<MeshControlPoint, Offset> offsets = new EnumMap<>(MeshControlPoint.class);

	HelmetMeshCalibration()
	{
		for (MeshControlPoint point : MeshControlPoint.values())
		{
			offsets.put(point, new Offset());
		}
	}

	HelmetMeshCalibration copy()
	{
		HelmetMeshCalibration copy = new HelmetMeshCalibration();
		for (MeshControlPoint point : MeshControlPoint.values())
		{
			Offset offset = offsets.get(point);
			copy.offsets.put(point, new Offset(offset.x, offset.y, offset.z));
		}
		return copy;
	}

	void applyDrag(MeshControlPoint point, int deltaX, int deltaY)
	{
		applyDrag(point, (float) deltaX, (float) deltaY);
	}

	void applyDrag(MeshControlPoint point, float deltaX, float deltaY)
	{
		if (point == null)
		{
			return;
		}
		Offset offset = offsets.get(point);
		offset.x = clamp(offset.x + deltaX);
		// Model Y increases downward in RuneLite's model coordinates, matching
		// the canvas Y direction used by the calibration overlay.
		offset.y = clamp(offset.y + deltaY);
	}

	void adjustDepth(MeshControlPoint point, int wheelRotation)
	{
		if (point != null)
		{
			Offset offset = offsets.get(point);
			offset.z = clamp(offset.z - wheelRotation);
		}
	}

	void setOffset(MeshControlPoint point, int x, int y, int z)
	{
		if (point != null)
		{
			offsets.put(point, new Offset(clamp(x), clamp(y), clamp(z)));
		}
	}

	int getX(MeshControlPoint point)
	{
		return Math.round(offsets.get(point).x);
	}

	float getXOffset(MeshControlPoint point)
	{
		return offsets.get(point).x;
	}

	int getY(MeshControlPoint point)
	{
		return Math.round(offsets.get(point).y);
	}

	float getYOffset(MeshControlPoint point)
	{
		return offsets.get(point).y;
	}

	int getZ(MeshControlPoint point)
	{
		return offsets.get(point).z;
	}

	private static float clamp(float value)
	{
		return Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, value));
	}

	boolean isDefault()
	{
		for (Offset offset : offsets.values())
		{
			if (offset.x != 0 || offset.y != 0 || offset.z != 0)
			{
				return false;
			}
		}
		return true;
	}

	String key()
	{
		StringBuilder key = new StringBuilder();
		for (MeshControlPoint point : MeshControlPoint.values())
		{
			Offset offset = offsets.get(point);
			key.append(':').append(offset.x).append(':').append(offset.y).append(':').append(offset.z);
		}
		return key.toString();
	}

	/** Applies smooth local movement to the generated logical head vertices. */
	void applyTo(float[] verticesX, float[] verticesY, float[] verticesZ)
	{
		for (int vertex = 0; vertex < verticesX.length; vertex++)
		{
			// The final logical vertex is the neck seam and must remain anchored.
			if (vertex == verticesX.length - 1)
			{
				continue;
			}
			float totalWeight = 0f;
			float offsetX = 0f;
			float offsetY = 0f;
			float offsetZ = 0f;
			for (MeshControlPoint point : MeshControlPoint.values())
			{
				Offset offset = offsets.get(point);
				float dx = (verticesX[vertex] - point.getX()) / INFLUENCE_RADIUS_X;
				float dy = (verticesY[vertex] - point.getY()) / INFLUENCE_RADIUS_Y;
				float dz = (verticesZ[vertex] - point.getZ()) / INFLUENCE_RADIUS_Z;
				float weight = (float) Math.exp(-0.5f * (dx * dx + dy * dy + dz * dz));
				if (weight < 0.01f)
				{
					continue;
				}
				totalWeight += weight;
				offsetX += weight * offset.x;
				offsetY += weight * offset.y;
				offsetZ += weight * offset.z;
			}
			if (totalWeight > 0f)
			{
				verticesX[vertex] += offsetX / totalWeight;
				verticesY[vertex] += offsetY / totalWeight;
				verticesZ[vertex] += offsetZ / totalWeight;
			}
		}
	}

	private static int clamp(int value)
	{
		return Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, value));
	}

	private static final class Offset
	{
		private float x;
		private float y;
		private int z;

		private Offset()
		{
		}

		private Offset(float x, float y, int z)
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
}
