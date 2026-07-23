package com.faceswap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FaceSwapHeadQualityProfilesTest
{
	@Test
	public void csvDefinesEveryHeadDefault()
	{
		for (FaceSwapHead head : FaceSwapHead.values())
		{
			assertTrue(head + " is missing from " + FaceSwapHeadQualityProfiles.RESOURCE,
				FaceSwapHeadQualityProfiles.hasConfiguredDefault(head));
		}
		for (FaceSwapHead head : FaceSwapHead.values())
		{
			assertEquals(head + " must default to Medium",
				FaceSwapTriangleCount.TRIANGLES_4000,
				FaceSwapHeadQualityProfiles.getDefault(head));
		}
	}

	@Test
	public void userOverridesAreStoredIndependentlyPerHead()
	{
		String overrides = FaceSwapHeadQualityProfiles.setOverride(
			"", FaceSwapHead.KING_CONDOR, FaceSwapTriangleCount.TRIANGLES_8000);
		overrides = FaceSwapHeadQualityProfiles.setOverride(
			overrides, FaceSwapHead.ALFIE, FaceSwapTriangleCount.TRIANGLES_2000);

		assertEquals(FaceSwapTriangleCount.TRIANGLES_8000,
			FaceSwapHeadQualityProfiles.resolve(FaceSwapHead.KING_CONDOR, overrides));
		assertEquals(FaceSwapTriangleCount.TRIANGLES_2000,
			FaceSwapHeadQualityProfiles.resolve(FaceSwapHead.ALFIE, overrides));
		assertEquals(FaceSwapHeadQualityProfiles.getDefault(FaceSwapHead.SARDACO),
			FaceSwapHeadQualityProfiles.resolve(FaceSwapHead.SARDACO, overrides));
	}

	@Test
	public void emptyOverridesRestoreCsvDefaults()
	{
		assertEquals(FaceSwapHeadQualityProfiles.getDefault(FaceSwapHead.KING_CONDOR),
			FaceSwapHeadQualityProfiles.resolve(FaceSwapHead.KING_CONDOR, ""));
		assertEquals(FaceSwapHeadQualityProfiles.getDefault(FaceSwapHead.ALFIE),
			FaceSwapHeadQualityProfiles.resolve(FaceSwapHead.ALFIE, null));
	}

	@Test
	public void legacyFictionalQualityOverridesAreRenamed()
	{
		assertEquals(
			"AGENT=TRIANGLES_8000,MONKEY=TRIANGLES_2000,SARDACO=TRIANGLES_4000",
			FaceSwapPlugin.migrateHeadQualityOverrideNames(
				"JAMES_BOND=TRIANGLES_8000,DONKEY_KONG=TRIANGLES_2000,SARDACO=TRIANGLES_4000"));
		assertEquals(
			"AGENT=TRIANGLES_6000",
			FaceSwapPlugin.migrateHeadQualityOverrideNames(
				"JAMES_BOND=TRIANGLES_8000,AGENT=TRIANGLES_6000"));
	}

}
