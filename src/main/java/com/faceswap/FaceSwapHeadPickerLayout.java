package com.faceswap;

public enum FaceSwapHeadPickerLayout
{
	POPUP("Popup"),
	INLINE("Inline");

	private final String displayName;

	FaceSwapHeadPickerLayout(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
