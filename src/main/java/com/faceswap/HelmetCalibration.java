package com.faceswap;

final class HelmetCalibration
{
	private static final int MIN_OFFSET = -128;
	private static final int MAX_OFFSET = 128;
	private static final int MIN_SCALE = 25;
	private static final int MAX_SCALE = 250;
	private static final int MIN_DIMENSION = 50;
	private static final int MAX_DIMENSION = 200;

	private int y;
	private int scale;
	private int x;
	private int z;
	private int pitch;
	private int yaw;
	private int roll;
	private int width;
	private int faceHeight;
	private int depth;
	private int animationFrameOffset;
	private HelmetMeshCalibration meshCalibration;

	private HelmetCalibration(
		int y,
		int scale,
		int x,
		int z,
		int pitch,
		int yaw,
		int roll,
		int width,
		int faceHeight,
		int depth,
		int animationFrameOffset)
	{
		this.y = clamp(y, MIN_OFFSET, MAX_OFFSET);
		this.scale = clamp(scale, MIN_SCALE, MAX_SCALE);
		this.x = clamp(x, MIN_OFFSET, MAX_OFFSET);
		this.z = clamp(z, MIN_OFFSET, MAX_OFFSET);
		this.pitch = clamp(pitch, -180, 180);
		this.yaw = clamp(yaw, -180, 180);
		this.roll = clamp(roll, -180, 180);
		this.width = clamp(width, MIN_DIMENSION, MAX_DIMENSION);
		this.faceHeight = clamp(faceHeight, MIN_DIMENSION, MAX_DIMENSION);
		this.depth = clamp(depth, MIN_DIMENSION, MAX_DIMENSION);
		this.animationFrameOffset = clamp(animationFrameOffset, -3, 3);
		this.meshCalibration = new HelmetMeshCalibration();
	}

	static HelmetCalibration fromConfig(FaceSwapConfig config)
	{
		return new HelmetCalibration(
			config.prototype3dY(),
			config.prototype3dScale(),
			config.prototype3dX(),
			config.prototype3dZ(),
			config.prototype3dPitch(),
			config.prototype3dYaw(),
			config.prototype3dRoll(),
			config.prototype3dWidth(),
			config.prototype3dFaceHeight(),
			config.prototype3dDepth(),
			config.prototypeAnimationFrameOffset());
	}

	static HelmetCalibration fromProfileOrConfig(HelmetProfile profile, FaceSwapConfig config)
	{
		if (profile == null || !profile.isTested())
		{
			return fromConfig(config);
		}

		return new HelmetCalibration(
			profile.getModelY(),
			profile.getModelScale(),
			profile.getModelX(),
			profile.getModelZ(),
			profile.getModelPitch(),
			profile.getModelYaw(),
			profile.getModelRoll(),
			profile.getModelWidth(),
			profile.getModelFaceHeight(),
			profile.getModelDepth(),
			profile.getAnimationFrameOffset());
	}

	HelmetCalibration copy()
	{
		HelmetCalibration copy = new HelmetCalibration(y, scale, x, z, pitch, yaw, roll, width, faceHeight, depth,
			animationFrameOffset);
		copy.meshCalibration = meshCalibration.copy();
		return copy;
	}

	void setMeshCalibration(HelmetMeshCalibration meshCalibration)
	{
		this.meshCalibration = meshCalibration == null ? new HelmetMeshCalibration() : meshCalibration.copy();
	}

	HelmetMeshCalibration getMeshCalibration()
	{
		return meshCalibration.copy();
	}

	void applyMeshDrag(MeshControlPoint point, float deltaX, float deltaY)
	{
		meshCalibration.applyDrag(point, deltaX, deltaY);
	}

	void adjustMeshDepth(MeshControlPoint point, int wheelRotation)
	{
		meshCalibration.adjustDepth(point, wheelRotation);
	}

	void applyDrag(CalibrationHandle handle, int deltaX, int deltaY)
	{
		int horizontal = Math.round(deltaX / 2f);
		int vertical = Math.round(deltaY / 2f);
		switch (handle)
		{
			case MOVE:
				x = clamp(x + horizontal, MIN_OFFSET, MAX_OFFSET);
				y = clamp(y - vertical, MIN_OFFSET, MAX_OFFSET);
				break;
			case WIDTH:
				width = clamp(width + horizontal, MIN_DIMENSION, MAX_DIMENSION);
				break;
			case HEIGHT:
				faceHeight = clamp(faceHeight - vertical, MIN_DIMENSION, MAX_DIMENSION);
				break;
			case SCALE:
				scale = clamp(scale + horizontal, MIN_SCALE, MAX_SCALE);
				break;
			case DEPTH:
				depth = clamp(depth + horizontal, MIN_DIMENSION, MAX_DIMENSION);
				break;
			default:
				break;
		}
	}

	void adjustZ(int wheelRotation)
	{
		z = clamp(z - wheelRotation, MIN_OFFSET, MAX_OFFSET);
	}

	int getY()
	{
		return y;
	}

	int getScale()
	{
		return scale;
	}

	int getX()
	{
		return x;
	}

	int getZ()
	{
		return z;
	}

	int getPitch()
	{
		return pitch;
	}

	int getYaw()
	{
		return yaw;
	}

	int getRoll()
	{
		return roll;
	}

	int getWidth()
	{
		return width;
	}

	int getFaceHeight()
	{
		return faceHeight;
	}

	int getDepth()
	{
		return depth;
	}

	int getAnimationFrameOffset()
	{
		return animationFrameOffset;
	}

	private static int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}
}
