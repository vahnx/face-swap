package com.faceswap;

public enum FaceSwapHead
{
	SARDACO("Sardaco", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	ODABLOCK("Odablock", FaceSwapHeadCategory.CONTENT_CREATOR, false, true),
	KING_CONDOR("King Condor", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	ALFIE("Alfie", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	DEARLOLA("DearLola", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	BEGGAR("Beggar", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	TPAPASLICE("TPapaSlice", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	ZECOOKIES("ZeCookies", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	PURESPAM("Purespam", FaceSwapHeadCategory.CONTENT_CREATOR, false, false),
	TORVESTA("Torvesta", FaceSwapHeadCategory.CONTENT_CREATOR, false, false),
	AGENT("Agent", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	MONKEY("Monkey", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true);

	private final String displayName;
	private final FaceSwapHeadCategory category;
	private final boolean releaseAvailable;
	private final boolean debugAvailable;

	FaceSwapHead(
		String displayName,
		FaceSwapHeadCategory category,
		boolean releaseAvailable,
		boolean debugAvailable)
	{
		this.displayName = displayName;
		this.category = category;
		this.releaseAvailable = releaseAvailable;
		this.debugAvailable = debugAvailable;
	}

	FaceSwapHeadCategory getCategory()
	{
		return category;
	}

	boolean isReleaseAvailable()
	{
		return releaseAvailable;
	}

	boolean isDebugOnly()
	{
		return !releaseAvailable && debugAvailable;
	}

	boolean isDebugAvailable()
	{
		return debugAvailable;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
