package com.faceswap;

import java.awt.image.BufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class FaceSwapHeadImagesTest
{
	@Test
	public void alfieUsesDirectionalAssets()
	{
		BufferedImage front = FaceSwapHeadImages.get(FaceSwapHead.ALFIE, FaceSwapHeadDirection.FRONT);
		BufferedImage back = FaceSwapHeadImages.get(FaceSwapHead.ALFIE, FaceSwapHeadDirection.BACK);

		assertEquals(512, front.getWidth());
		assertEquals(512, front.getHeight());
		assertEquals(512, back.getWidth());
		assertEquals(512, back.getHeight());
	}

	@Test
	public void estimatesMissingSideAssetsFromFrontAndBack()
	{
		BufferedImage front = FaceSwapHeadImages.get(FaceSwapHead.ALFIE, FaceSwapHeadDirection.FRONT);
		BufferedImage left = FaceSwapHeadImages.get(FaceSwapHead.ALFIE, FaceSwapHeadDirection.LEFT);
		BufferedImage right = FaceSwapHeadImages.get(FaceSwapHead.ALFIE, FaceSwapHeadDirection.RIGHT);

		assertNotSame(front, left);
		assertNotSame(front, right);
		assertEquals(512, left.getWidth());
		assertEquals(512, left.getHeight());
		assertEquals(512, right.getWidth());
		assertEquals(512, right.getHeight());
	}

	@Test
	public void approvedHeadsUseBundledDirectionalAssets()
	{
		FaceSwapHead[] approvedHeads = {
			FaceSwapHead.SARDACO,
			FaceSwapHead.DEARLOLA,
			FaceSwapHead.BEGGAR,
			FaceSwapHead.TPAPASLICE,
			FaceSwapHead.ZECOOKIES,
			FaceSwapHead.PRISONJOE
		};

		for (FaceSwapHead head : approvedHeads)
		{
			BufferedImage front = FaceSwapHeadImages.get(head, FaceSwapHeadDirection.FRONT);
			BufferedImage back = FaceSwapHeadImages.get(head, FaceSwapHeadDirection.BACK);

			assertNotSame(head + " must have separate front and back assets", front, back);
			assertEquals(head + " front width", 512, front.getWidth());
			assertEquals(head + " front height", 512, front.getHeight());
			assertEquals(head + " back width", 512, back.getWidth());
			assertEquals(head + " back height", 512, back.getHeight());
		}
	}

	@Test
	public void agentUsesFictionalDirectionalAssets()
	{
		BufferedImage front = FaceSwapHeadImages.get(
			FaceSwapHead.AGENT, FaceSwapHeadDirection.FRONT);
		BufferedImage back = FaceSwapHeadImages.get(
			FaceSwapHead.AGENT, FaceSwapHeadDirection.BACK);

		assertNotSame(front, back);
		assertEquals(512, front.getWidth());
		assertEquals(512, front.getHeight());
		assertEquals(512, back.getWidth());
		assertEquals(512, back.getHeight());
	}

	@Test
	public void monkeyUsesFictionalDirectionalAssets()
	{
		BufferedImage front = FaceSwapHeadImages.get(
			FaceSwapHead.MONKEY, FaceSwapHeadDirection.FRONT);
		BufferedImage back = FaceSwapHeadImages.get(
			FaceSwapHead.MONKEY, FaceSwapHeadDirection.BACK);

		assertNotSame(front, back);
		assertEquals(512, front.getWidth());
		assertEquals(512, front.getHeight());
		assertEquals(512, back.getWidth());
		assertEquals(512, back.getHeight());
	}
}
