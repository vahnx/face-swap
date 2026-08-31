package com.faceswap;

public enum FaceSwapHeadCategory
{
	FICTIONAL_CHARACTER("Fictional Characters", "fictional_characters"),
	EMOJI("Emojis", "emojis"),
	CONTENT_CREATOR("Content Creators", "content_creators"),
	CONTENT_CREATOR_3D("3D Block Variants", "content_creators_3d"),
	CUSTOM("Custom", "custom_heads");

	private final String displayName;
	private final String resourceDirectory;

	FaceSwapHeadCategory(String displayName, String resourceDirectory)
	{
		this.displayName = displayName;
		this.resourceDirectory = resourceDirectory;
	}

	String getResourceDirectory()
	{
		return resourceDirectory;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
