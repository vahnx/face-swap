package com.faceswap;

public enum MaskTrackingMode
{
	AUTO("Automatic"),
	ANIMATED_RIG("Animated Rig"),
	MERGED_MODEL("Merged Model (Fallback)");

	private final String displayName;

	MaskTrackingMode(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
