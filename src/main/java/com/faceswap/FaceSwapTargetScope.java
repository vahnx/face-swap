package com.faceswap;

public enum FaceSwapTargetScope
{
	DISABLED("Disabled"),
	SELF("Your Player"),
	FRIENDS("Friends"),
	CHAT_CHANNEL("Chat Channel"),
	CLANMATES("Clanmates"),
	IGNORE_LIST("Ignore List"),
	RADIUS("Everyone in Radius"),
	ALL_PLAYERS("All Players"),
	@Deprecated
	OTHERS("Other Players"),
	SPECIFIC_PLAYERS("Specific Players");

	private final String displayName;

	FaceSwapTargetScope(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
