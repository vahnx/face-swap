package com.faceswap;

import net.runelite.api.JagexColor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FaceSwapPrototypeColorTest
{
	@Test
	public void preservesAlfiePixelColors()
	{
		short light = FaceSwapPlugin.toPrototypeHsl(FaceSwapHead.ALFIE, 0xC8C8E6);
		short dark = FaceSwapPlugin.toPrototypeHsl(FaceSwapHead.ALFIE, 0x202851);
		assertEquals(JagexColor.rgbToHSL(0xC8C8E6, 1d), light);
		assertEquals(JagexColor.rgbToHSL(0x202851, 1d), dark);
	}

	@Test
	public void leavesOtherCreatorsUnchanged()
	{
		assertEquals(JagexColor.rgbToHSL(0x896169, 1d),
			FaceSwapPlugin.toPrototypeHsl(FaceSwapHead.KING_CONDOR, 0x896169));
	}
}
