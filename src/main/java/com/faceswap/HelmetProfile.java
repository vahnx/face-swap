package com.faceswap;

final class HelmetProfile
{
	private final String name;
	private final int modelY;
	private final int modelScale;
	private final int modelX;
	private final int modelZ;
	private final int modelPitch;
	private final int modelYaw;
	private final int modelRoll;
	private final int modelWidth;
	private final int modelFaceHeight;
	private final int modelDepth;
	private final int animationFrameOffset;
	private final int maskY;
	private final boolean tested;
	private final boolean hidden;

	HelmetProfile(
		String name,
		int modelY,
		int modelScale,
		int modelX,
		int modelZ,
		int modelPitch,
		int modelYaw,
		int modelRoll,
		int modelWidth,
		int modelFaceHeight,
		int modelDepth,
		int animationFrameOffset,
		int maskY,
		boolean tested,
		boolean hidden)
	{
		this.name = name;
		this.modelY = modelY;
		this.modelScale = modelScale;
		this.modelX = modelX;
		this.modelZ = modelZ;
		this.modelPitch = modelPitch;
		this.modelYaw = modelYaw;
		this.modelRoll = modelRoll;
		this.modelWidth = modelWidth;
		this.modelFaceHeight = modelFaceHeight;
		this.modelDepth = modelDepth;
		this.animationFrameOffset = animationFrameOffset;
		this.maskY = maskY;
		this.tested = tested;
		this.hidden = hidden;
	}

	String getName()
	{
		return name;
	}

	int getModelY()
	{
		return modelY;
	}

	int getModelScale()
	{
		return modelScale;
	}

	int getModelX()
	{
		return modelX;
	}

	int getModelZ()
	{
		return modelZ;
	}

	int getModelPitch()
	{
		return modelPitch;
	}

	int getModelYaw()
	{
		return modelYaw;
	}

	int getModelRoll()
	{
		return modelRoll;
	}

	int getModelWidth()
	{
		return modelWidth;
	}

	int getModelFaceHeight()
	{
		return modelFaceHeight;
	}

	int getModelDepth()
	{
		return modelDepth;
	}

	int getAnimationFrameOffset()
	{
		return animationFrameOffset;
	}

	int getMaskY()
	{
		return maskY;
	}

	boolean isTested()
	{
		return tested;
	}

	boolean isHidden()
	{
		return hidden;
	}
}
