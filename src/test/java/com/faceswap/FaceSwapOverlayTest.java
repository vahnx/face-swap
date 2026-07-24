package com.faceswap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FaceSwapOverlayTest
{
	@Test
	public void nonzeroDebugCalibrationOverridesHelmetProfile()
	{
		assertEquals(20, FaceSwapOverlay.resolveMaskCalibration(0, 20));
		assertEquals(12, FaceSwapOverlay.resolveMaskCalibration(12, 20));
		assertEquals(25, FaceSwapOverlay.resolveMaskPitchCalibration(20, 25));
		assertEquals(12, FaceSwapOverlay.resolveMaskPitchCalibration(12, 25));
	}
}
