package com.faceswap;

import net.runelite.api.JagexColor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FaceSwapPrototypeColorTest
{
	@Test
	public void mapsAlfiePixelsToStableNeutralPalette()
	{
		short light = FaceSwapPlugin.toPrototypeHsl(FaceSwapHead.ALFIE, 0xC8C8E6);
		short dark = FaceSwapPlugin.toPrototypeHsl(FaceSwapHead.ALFIE, 0x202851);
		assertEquals(0, JagexColor.unpackHue(light));
		assertEquals(1, JagexColor.unpackSaturation(light));
		assertEquals(0, JagexColor.unpackHue(dark));
		assertEquals(1, JagexColor.unpackSaturation(dark));
		assertEquals(JagexColor.unpackLuminance(JagexColor.rgbToHSL(0xC8C8E6, 1d)),
			JagexColor.unpackLuminance(light));
	}

	@Test
	public void leavesOtherCreatorsUnchanged()
	{
		assertEquals(JagexColor.rgbToHSL(0x896169, 1d),
			FaceSwapPlugin.toPrototypeHsl(FaceSwapHead.KING_CONDOR, 0x896169));
	}
}
