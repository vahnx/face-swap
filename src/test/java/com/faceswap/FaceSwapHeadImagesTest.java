package com.faceswap;

import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

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
	public void baseOnlyEmojiAssetsWorkForEveryDirection()
	{
		BufferedImage base = FaceSwapHeadImages.get(FaceSwapHead.HEART, FaceSwapHeadDirection.FRONT);
		assertSame(base, FaceSwapHeadImages.get(FaceSwapHead.HEART, FaceSwapHeadDirection.BACK));
		assertSame(base, FaceSwapHeadImages.get(FaceSwapHead.HEART, FaceSwapHeadDirection.LEFT));
		assertSame(base, FaceSwapHeadImages.get(FaceSwapHead.HEART, FaceSwapHeadDirection.RIGHT));
	}

	@Test
	public void customBackImageIsUsedWithFrontFallback()
	{
		BufferedImage front = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		BufferedImage back = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		try
		{
			FaceSwapHeadImages.setCustomImages(front, back);
			assertSame(front, FaceSwapHeadImages.get(FaceSwapHead.CUSTOM, FaceSwapHeadDirection.FRONT));
			assertSame(back, FaceSwapHeadImages.get(FaceSwapHead.CUSTOM, FaceSwapHeadDirection.BACK));
			BufferedImage left = FaceSwapHeadImages.get(FaceSwapHead.CUSTOM, FaceSwapHeadDirection.LEFT);
			BufferedImage right = FaceSwapHeadImages.get(FaceSwapHead.CUSTOM, FaceSwapHeadDirection.RIGHT);
			assertNotSame(front, left);
			assertNotSame(front, right);
			assertSame(left, FaceSwapHeadImages.get(FaceSwapHead.CUSTOM, FaceSwapHeadDirection.LEFT));
			assertSame(right, FaceSwapHeadImages.get(FaceSwapHead.CUSTOM, FaceSwapHeadDirection.RIGHT));

			FaceSwapHeadImages.setCustomImages(front, null);
			assertSame(front, FaceSwapHeadImages.get(FaceSwapHead.CUSTOM, FaceSwapHeadDirection.BACK));
		}
		finally
		{
			FaceSwapHeadImages.setCustomImages(null, null);
		}
	}

	@Test
	public void creatorStylesUseTheirFrontAndDefaultBackAssets()
	{
		java.util.List<String> expectedStyles = java.util.List.of(
			"default", "sad", "crying", "angry", "angel", "furious", "blushing", "sick", "in_love");
		java.util.List<String> expectedAlfieStyles = java.util.List.of(
			"default", "sad", "crying", "angry", "angel", "blushing", "sick", "in_love");
		java.util.List<String> expectedOdablockStyles = java.util.List.of(
			"default", "sad", "crying", "angry", "angel", "furious", "blushing", "sick", "in_love",
			"gamon", "smug", "relaxed", "sus");
		java.util.List<String> expectedGnomonkeyStyles = java.util.List.of(
			"default", "sad", "crying", "angry", "angel", "furious", "blushing", "sick", "in_love", "7tv");
		java.util.List<String> expectedMaskStyles = java.util.List.of("default");
		for (FaceSwapHead head : FaceSwapHead.values())
		{
			if (head.getCategory() != FaceSwapHeadCategory.CONTENT_CREATOR || !head.isReleaseAvailable())
			{
				continue;
			}
			java.util.List<String> styles = head == FaceSwapHead.ALFIE
				? expectedAlfieStyles
				: head == FaceSwapHead.ODABLOCK
				? expectedOdablockStyles
				: head == FaceSwapHead.GNOMONKEY ? expectedGnomonkeyStyles
				: head == FaceSwapHead.MRNOSLEEP_MASK ? expectedMaskStyles : expectedStyles;
			assertEquals(head + " styles", styles, FaceSwapHeadImages.getAvailableStyleIds(head));
			for (String styleId : styles)
			{
				String resourceName = head.name().toLowerCase(Locale.ROOT)
					+ ("default".equals(styleId) ? "" : "_" + styleId) + "_front.png";
				if (FaceSwapHeadImages.isProceduralStyle(styleId))
				{
					assertEquals(head + " " + styleId + " should not need a bundled resource", null,
						FaceSwapHeadImages.class.getResource("/heads/content_creators/" + resourceName));
				}
				else
				{
					assertNotNull(head + " " + styleId + " resource",
						FaceSwapHeadImages.class.getResource(
							"/heads/content_creators/" + resourceName));
				}
				BufferedImage image = FaceSwapHeadImages.get(head, styleId, FaceSwapHeadDirection.FRONT);
				assertEquals(head + " " + styleId + " width", 512, image.getWidth());
				assertEquals(head + " " + styleId + " height", 512, image.getHeight());
			}
		}

		BufferedImage defaultFront = FaceSwapHeadImages.get(
			FaceSwapHead.TPAPASLICE, FaceSwapHeadDirection.FRONT);
		BufferedImage sadFront = FaceSwapHeadImages.get(
			FaceSwapHead.TPAPASLICE, "sad", FaceSwapHeadDirection.FRONT);
		BufferedImage defaultBack = FaceSwapHeadImages.get(
			FaceSwapHead.TPAPASLICE, FaceSwapHeadDirection.BACK);
		BufferedImage sadBack = FaceSwapHeadImages.get(
			FaceSwapHead.TPAPASLICE, "sad", FaceSwapHeadDirection.BACK);

		assertNotSame(defaultFront, sadFront);
		assertSame(defaultBack, sadBack);
		assertEquals(512, sadFront.getWidth());
		assertEquals(512, sadFront.getHeight());
		assertEquals(512, FaceSwapHeadImages.get(
			FaceSwapHead.TPAPASLICE, "crying", FaceSwapHeadDirection.FRONT).getWidth());
		assertEquals(512, FaceSwapHeadImages.get(
			FaceSwapHead.TASTYLIFE, "crying", FaceSwapHeadDirection.FRONT).getHeight());
		for (String styleId : List.of("furious", "blushing", "sick", "in_love"))
		{
			assertNotSame(styleId + " should transform the base image",
				defaultFront, FaceSwapHeadImages.get(
					FaceSwapHead.TPAPASLICE, styleId, FaceSwapHeadDirection.FRONT));
		}
		assertEquals("angel should be procedural", null,
			FaceSwapHeadImages.class.getResource("/heads/content_creators/tpapaslice_angel_front.png"));
		for (String styleId : FaceSwapHeadImages.getAvailableStyleIds(FaceSwapHead.TASTYLIFE))
		{
			assertEquals(512, FaceSwapHeadImages.get(
				FaceSwapHead.TASTYLIFE, styleId, FaceSwapHeadDirection.FRONT).getWidth());
			assertEquals(512, FaceSwapHeadImages.get(
				FaceSwapHead.TASTYLIFE, styleId, FaceSwapHeadDirection.FRONT).getHeight());
		}
	}

	@Test
	public void gnomonkeyStretchStyleUsesFriendlyDisplayName()
	{
		assertEquals("Stretched", FaceSwapHeadImages.styleDisplayName("7tv"));
	}

	@Test
	public void releaseHeadsUseBundledDirectionalAssets()
	{
		for (FaceSwapHead head : FaceSwapHead.values())
		{
			if (head == FaceSwapHead.CUSTOM)
			{
				continue;
			}
			if (!head.isReleaseAvailable())
			{
				continue;
			}

			String resourceBase = "/heads/" + head.getCategory().getResourceDirectory()
				+ "/" + head.name().toLowerCase(Locale.ROOT);
			if (head.getCategory() == FaceSwapHeadCategory.EMOJI)
			{
				assertNotNull(head + " base resource", FaceSwapHeadImages.class.getResource(resourceBase + ".png"));
				BufferedImage image = FaceSwapHeadImages.get(head, FaceSwapHeadDirection.FRONT);
				assertEquals(head + " width", 512, image.getWidth());
				assertEquals(head + " height", 512, image.getHeight());
				assertSame(head + " should use the same base image for both directions", image,
					FaceSwapHeadImages.get(head, FaceSwapHeadDirection.BACK));
				continue;
			}
			assertNotNull(head + " front resource", FaceSwapHeadImages.class.getResource(resourceBase + "_front.png"));
			assertNotNull(head + " back resource", FaceSwapHeadImages.class.getResource(resourceBase + "_back.png"));

			BufferedImage front = FaceSwapHeadImages.get(head, FaceSwapHeadDirection.FRONT);
			BufferedImage back = FaceSwapHeadImages.get(head, FaceSwapHeadDirection.BACK);

			assertNotSame(head + " must have separate front and back assets", front, back);
			assertEquals(head + " front width", 512, front.getWidth());
			assertEquals(head + " front height", 512, front.getHeight());
			assertEquals(head + " back width", 512, back.getWidth());
			assertEquals(head + " back height", 512, back.getHeight());
		}
	}

	// Removed fictional-character asset tests remain here as a reference for restoration.
}
