package com.faceswap;

public enum FaceSwapNpcTargetScope
{
	DISABLED("Disabled"),
	SPECIFIC_NPCS("Specific NPCs"),
	ALL_NPCS("All NPCs");

	private final String displayName;

	FaceSwapNpcTargetScope(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
