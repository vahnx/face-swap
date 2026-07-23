package com.faceswap;

enum FaceSwapHeadCategory
{
	FICTIONAL_CHARACTER("Fictional Characters", "fictional_characters"),
	CONTENT_CREATOR("Content Creators", "content_creators");

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
