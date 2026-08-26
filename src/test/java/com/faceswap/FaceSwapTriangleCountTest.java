package com.faceswap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FaceSwapTriangleCountTest
{
	@Test
	public void presetsHaveExpectedTriangleCountsAndCapacity()
	{
		for (FaceSwapTriangleCount preset : FaceSwapTriangleCount.values())
		{
			if (preset == FaceSwapTriangleCount.AUTO)
			{
				continue;
			}
			assertEquals(Integer.parseInt(preset.toString().replaceAll("[^0-9]", "").substring(0, 4)), preset.getTriangleCount());
			assertTrue(preset.getDonorCopies() * 437 >= preset.getSegments() * preset.getRings() + 2);
			assertTrue(preset.getDonorCopies() * 868 >= preset.getTriangleCount());
		}
	}

	@Test
	public void autoUsesMediumPresetForEveryCreator()
	{
		assertEquals(FaceSwapTriangleCount.TRIANGLES_4000,
			FaceSwapTriangleCount.AUTO.resolve(FaceSwapHead.PURESPAM));
		assertEquals(FaceSwapTriangleCount.TRIANGLES_4000,
			FaceSwapTriangleCount.AUTO.resolve(FaceSwapHead.KING_CONDOR));
		assertEquals(FaceSwapTriangleCount.TRIANGLES_4000,
			FaceSwapTriangleCount.AUTO.resolve(FaceSwapHead.ALFIE));
	}

	@Test
	public void mediumAndHigherPresetsUseEnhancedMapping()
	{
		assertTrue(FaceSwapTriangleCount.TRIANGLES_4000.usesEnhancedSampling());
		assertTrue(FaceSwapTriangleCount.TRIANGLES_6000.usesEnhancedSampling());
		assertTrue(FaceSwapTriangleCount.TRIANGLES_8000.usesEnhancedSampling());
	}
}
