package com.faceswap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FaceSwapOverlayTest
{
	@Test
	public void nonzeroDebugCalibrationOverridesHelmetProfile()
	{
		assertEquals(20, FaceSwapOverlay.resolveMaskCalibration(0, 20));
		assertEquals(12, FaceSwapOverlay.resolveMaskCalibration(12, 20));
		assertEquals(25, FaceSwapOverlay.resolveMaskPitchCalibration(0, 25));
		assertEquals(12, FaceSwapOverlay.resolveMaskPitchCalibration(12, 25));
	}

	@Test
	public void helmetCalibrationIsReservedForMergedModelTracking()
	{
		assertFalse(FaceSwapOverlay.usesLegacyHelmetMaskCalibration(MaskTrackingMode.ANIMATED_RIG, true));
		assertFalse(FaceSwapOverlay.usesLegacyHelmetMaskCalibration(MaskTrackingMode.AUTO, true));
		assertTrue(FaceSwapOverlay.usesLegacyHelmetMaskCalibration(MaskTrackingMode.MERGED_MODEL, true));
		assertTrue(FaceSwapOverlay.usesLegacyHelmetMaskCalibration(MaskTrackingMode.AUTO, false));
	}
}
