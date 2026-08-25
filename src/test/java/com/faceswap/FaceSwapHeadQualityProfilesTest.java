package com.faceswap;

import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FaceSwapHeadQualityProfilesTest
{
	@Test
	public void findChangedHeadsDetectsOnlyModifiedOverrides()
	{
		Set<FaceSwapHead> changed = FaceSwapHeadQualityProfiles.findChangedHeads(
			"SARDACO=TRIANGLES_4000,ALFIE=TRIANGLES_6000",
			"SARDACO=TRIANGLES_4000,ALFIE=TRIANGLES_8000,PUG=TRIANGLES_2000");

		assertEquals(Set.of(FaceSwapHead.ALFIE, FaceSwapHead.PUG), changed);
	}

	@Test
	public void findChangedHeadsIgnoresOrderAndEquivalentEmptyValues()
	{
		Set<FaceSwapHead> changed = FaceSwapHeadQualityProfiles.findChangedHeads(
			"",
			null);
		assertEquals(Set.of(), changed);

		changed = FaceSwapHeadQualityProfiles.findChangedHeads(
			"PUG=TRIANGLES_2000,SARDACO=TRIANGLES_4000",
			"SARDACO=TRIANGLES_4000,PUG=TRIANGLES_2000");
		assertEquals(Set.of(), changed);
	}
}
