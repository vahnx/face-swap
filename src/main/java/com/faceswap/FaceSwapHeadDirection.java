package com.faceswap;

enum FaceSwapHeadDirection
{
	FRONT("front"),
	BACK("back"),
	LEFT("left"),
	RIGHT("right");

	private final String fileSuffix;

	FaceSwapHeadDirection(String fileSuffix)
	{
		this.fileSuffix = fileSuffix;
	}

	String getFileSuffix()
	{
		return fileSuffix;
	}
}
