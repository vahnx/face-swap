package com.faceswap;

import net.runelite.client.RuneLite;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HelmetMeshCalibrationTest
{
	@Test
	public void exposesStableFaceLandmarks()
	{
		assertEquals(16, MeshControlPoint.values().length);
		assertEquals("Crown", MeshControlPoint.CROWN.getLabel());
		assertTrue(MeshControlPoint.NOSE_TIP.getZ() < MeshControlPoint.CHEEK_LEFT.getZ());
	}

	@Test
	public void localOffsetsAreClampedAndCopied()
	{
		HelmetMeshCalibration calibration = new HelmetMeshCalibration();
		calibration.setOffset(MeshControlPoint.CHEEK_LEFT, 100, -100, 40);

		assertEquals(32, calibration.getX(MeshControlPoint.CHEEK_LEFT));
		assertEquals(-32, calibration.getY(MeshControlPoint.CHEEK_LEFT));
		assertEquals(32, calibration.getZ(MeshControlPoint.CHEEK_LEFT));
		assertFalse(calibration.isDefault());
		assertEquals(calibration.key(), calibration.copy().key());
	}

	@Test
	public void screenDragDirectionMatchesLandmarkDirection()
	{
		HelmetMeshCalibration calibration = new HelmetMeshCalibration();
		calibration.applyDrag(MeshControlPoint.NOSE_TIP, 5f, 5f);

		assertEquals(5, calibration.getX(MeshControlPoint.NOSE_TIP));
		assertEquals(5, calibration.getY(MeshControlPoint.NOSE_TIP));
	}

	@Test
	public void weightedDeformationLeavesNeckSeamAnchored()
	{
		HelmetMeshCalibration calibration = new HelmetMeshCalibration();
		calibration.setOffset(MeshControlPoint.NOSE_TIP, 10, 0, 0);
		float[] x = {0f, 0f, 0f};
		float[] y = {-180f, -180f, -164f};
		float[] z = {-10f, 10f, 0f};

		calibration.applyTo(x, y, z);

		assertTrue(x[0] > 0f);
		assertEquals(0f, x[2], 0.001f);
	}

	@Test
	public void storesRuntimeMeshCalibrationInsidePluginDirectory()
	{
		assertEquals(
			RuneLite.RUNELITE_DIR.toPath().resolve("face-swap").resolve("helmet_mesh_calibration.csv"),
			HelmetMeshCalibrations.RUNTIME_FILE);
	}
}
