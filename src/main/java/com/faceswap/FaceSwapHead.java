package com.faceswap;

public enum FaceSwapHead
{
	SARDACO("Sardaco", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	ASIAN_ANDY("Asian Andy", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	// ODABLOCK("Odablock", FaceSwapHeadCategory.CONTENT_CREATOR, false, true),
	FOX_OSRS("Fox (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	GRIM("Grim", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	KING_CONDOR("King Condor", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	ALFIE("Alfie", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	DEARLOLA("DearLola", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	BEGGAR("Beggar", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	TPAPASLICE("TPapaSlice", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	ZECOOKIES("ZeCookies", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	PRISONJOE("Prison Joe", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	ELIOP14("eliop14", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	JILLYFISH("jillyfish", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	SKILL_SPECS("Skill Specs", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	TASTYLIFE("TastyLife", FaceSwapHeadCategory.CONTENT_CREATOR, true, true),
	SARDACO_OSRS("Sardaco (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	ASIAN_ANDY_OSRS("Asian Andy (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	ALFIE_OSRS("Alfie (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	BEGGAR_OSRS("Beggar (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	DEARLOLA_OSRS("DearLola (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	GRIM_OSRS("Grim (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	KING_CONDOR_OSRS("King Condor (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	PRISONJOE_OSRS("Prison Joe (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	TPAPASLICE_OSRS("TPapaSlice (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	ZECOOKIES_OSRS("ZeCookies (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	ELIOP14_OSRS("eliop14 (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	JILLYFISH_OSRS("jillyfish (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	SKILL_SPECS_OSRS("Skill Specs (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	TASTYLIFE_OSRS("TastyLife (OSRS)", FaceSwapHeadCategory.CONTENT_CREATOR_3D, true, true),
	PURESPAM("Purespam", FaceSwapHeadCategory.CONTENT_CREATOR, false, false),
	TORVESTA("Torvesta", FaceSwapHeadCategory.CONTENT_CREATOR, false, false),
	SMILEY("😀", FaceSwapHeadCategory.EMOJI, true, true),
	HEART_EYES("😍", FaceSwapHeadCategory.EMOJI, true, true),
	POOP("💩", FaceSwapHeadCategory.EMOJI, true, true),
	COOL("😎", FaceSwapHeadCategory.EMOJI, true, true),
	ANGRY("😡", FaceSwapHeadCategory.EMOJI, true, true),
	SAD("😞", FaceSwapHeadCategory.EMOJI, true, true),
	SURPRISED("😮", FaceSwapHeadCategory.EMOJI, true, true),
	HEART("❤️", FaceSwapHeadCategory.EMOJI, true, true),
	ROBOT("🤖", FaceSwapHeadCategory.EMOJI, true, true),
	PUG("Pug", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	HORSE("Horse", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	RABBIT("Rabbit", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	PENGUIN("Penguin", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	CAT("Cat", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	MONKEY_PHOTO("Monkey (Photo)", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	// CLOWN("Clown", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	// AGENT("Agent", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	// MONKEY("Monkey", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	// ORANGE_PARKA("Orange Parka", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	CLASSIC_ADVENTURER("Classic Adventurer", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	// PURPLE_DINOSAUR("Purple Dinosaur", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	// SPACE_MARINE("Space Marine", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	// HALFLING("Halfling", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	// BANDICOOT("Bandicoot", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	// CHOSEN_ONE("Chosen One", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	// MARTIAL_ARTIST("Martial Artist", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	// BOSS("Boss", FaceSwapHeadCategory.FICTIONAL_CHARACTER, true, true),
	CUSTOM("Custom Image", FaceSwapHeadCategory.CUSTOM, true, true);

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
