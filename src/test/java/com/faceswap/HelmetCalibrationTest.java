package com.faceswap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HelmetCalibrationTest
{
	private final FaceSwapConfig config = new FaceSwapConfig()
	{
	};

	@Test
	public void startsFromConfiguredCalibration()
	{
		HelmetCalibration calibration = HelmetCalibration.fromConfig(config);

		assertEquals(6, calibration.getY());
		assertEquals(100, calibration.getScale());
		assertEquals(0, calibration.getX());
		assertEquals(-8, calibration.getZ());
		assertEquals(100, calibration.getWidth());
		assertEquals(100, calibration.getFaceHeight());
		assertEquals(120, calibration.getDepth());
	}

	@Test
	public void dragHandlesChangeOnlyTheirAssignedDimension()
	{
		HelmetCalibration calibration = HelmetCalibration.fromConfig(config);

		calibration.applyDrag(CalibrationHandle.MOVE, 10, -6);
		assertEquals(5, calibration.getX());
		assertEquals(9, calibration.getY());
		assertEquals(100, calibration.getWidth());

		calibration.applyDrag(CalibrationHandle.WIDTH, 10, 20);
		assertEquals(105, calibration.getWidth());
		assertEquals(100, calibration.getFaceHeight());

		calibration.applyDrag(CalibrationHandle.HEIGHT, 20, -10);
		assertEquals(105, calibration.getFaceHeight());
		assertEquals(100, calibration.getScale());

		calibration.applyDrag(CalibrationHandle.SCALE, 8, 0);
		assertEquals(104, calibration.getScale());
		calibration.applyDrag(CalibrationHandle.DEPTH, -6, 0);
		assertEquals(117, calibration.getDepth());
	}

	@Test
	public void zWheelAndDragValuesAreClamped()
	{
		HelmetCalibration calibration = HelmetCalibration.fromConfig(config);

		calibration.adjustZ(-500);
		assertEquals(128, calibration.getZ());
		calibration.applyDrag(CalibrationHandle.WIDTH, -1000, 0);
		assertEquals(50, calibration.getWidth());
		calibration.applyDrag(CalibrationHandle.SCALE, 1000, 0);
		assertEquals(250, calibration.getScale());
	}
}
