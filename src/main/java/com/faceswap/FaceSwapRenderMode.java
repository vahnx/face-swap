package com.faceswap;

public enum FaceSwapRenderMode
{
	THREE_D("3D"),
	MASK("Mask"),
	TWO_D("Wraparound");

	private final String displayName;

	FaceSwapRenderMode(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
