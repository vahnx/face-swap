package com.faceswap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import javax.imageio.ImageIO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FaceSwapReleaseAssetsTest
{
	@Test
	public void debugOnlyAssetsStayOutsideMainResources()
	{
		for (FaceSwapHead head : FaceSwapHead.values())
		{
			if (!head.isDebugOnly())
			{
				continue;
			}

			String stem = head.name().toLowerCase(java.util.Locale.ROOT);
			String category = head.getCategory().getResourceDirectory();
			for (String direction : new String[] {"front", "back"})
			{
				String filename = stem + "_" + direction + ".png";
				assertFalse(filename + " must not be packaged in the release",
					Files.exists(Path.of("src", "main", "resources", "heads", category, filename)));
				assertTrue(filename + " must remain available to local debug launches",
					Files.exists(Path.of("dev-assets", "heads", category, filename)));
			}
		}
	}

	@Test
	public void fictionalAssetsUseTheirReleaseDirectory()
	{
		// Removed fictional-character assets are intentionally no longer packaged.
		for (String head : new String[] {"pug", "horse", "rabbit", "penguin", "cat", "monkey_photo", "pepe", "jd_vance"})
		{
			assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
				"/heads/fictional_characters/" + head + "_front.png") != null);
			assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
				"/heads/fictional_characters/" + head + "_back.png") != null);
		}
	}

	@Test
	public void contentCreatorReleaseAssetsIncludeNewestReleaseHeads()
	{
		for (String filename : new String[] {
			"odablock_front.png",
			"odablock_back.png",
			"odablock_sad_front.png",
			"odablock_crying_front.png",
			"odablock_angry_front.png",
			"odablock_furious_front.png",
			"odablock_blushing_front.png",
			"odablock_sick_front.png",
			"josh_pillaut_front.png",
			"josh_pillaut_back.png",
			"josh_pillaut_sad_front.png",
			"josh_pillaut_crying_front.png",
			"josh_pillaut_angry_front.png",
			"josh_pillaut_furious_front.png",
			"josh_pillaut_blushing_front.png",
			"josh_pillaut_sick_front.png",
			"mint_madcow_front.png",
			"mint_madcow_back.png",
			"mint_madcow_sad_front.png",
			"mint_madcow_crying_front.png",
			"mint_madcow_angry_front.png",
			"mint_madcow_furious_front.png",
			"mint_madcow_blushing_front.png",
			"mint_madcow_sick_front.png"
		})
		{
			assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
				"/heads/content_creators/" + filename) != null);
		}
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators_3d/fox_osrs_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators_3d/fox_osrs_back.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators/grim_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators/grim_back.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators/brettdog_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators/brettdog_back.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators/mmorpg_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators/mmorpg_back.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators/gnomonkey_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators/gnomonkey_back.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators/gnomonkey_7tv_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators_3d/brettdog_osrs_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators_3d/brettdog_osrs_back.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators_3d/mmorpg_osrs_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators_3d/mmorpg_osrs_back.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators_3d/josh_pillaut_osrs_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators_3d/josh_pillaut_osrs_back.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators_3d/mint_madcow_osrs_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators_3d/mint_madcow_osrs_back.png") != null);
	}

	@Test
	public void brettDogAssetsDoNotContainEnclosedTransparency() throws IOException
	{
		for (String filename : new String[] {
			"brettdog_front.png",
			"brettdog_back.png",
			"brettdog_sad_front.png",
			"brettdog_crying_front.png",
			"brettdog_angry_front.png",
			"brettdog_furious_front.png",
			"brettdog_blushing_front.png",
			"brettdog_sick_front.png",
			"brettdog_clown.png"
		})
		{
			BufferedImage image = ImageIO.read(FaceSwapReleaseAssetsTest.class.getResource(
				"/heads/content_creators/" + filename));
			assertNotNull(filename + " must be readable", image);
			assertNoEnclosedTransparency(filename, image);
		}
	}

	@Test
	public void brettDogOsrsAssetsHaveTransparentBackgrounds() throws IOException
	{
		for (String filename : new String[] {"brettdog_osrs_front.png", "brettdog_osrs_back.png"})
		{
			BufferedImage image = ImageIO.read(FaceSwapReleaseAssetsTest.class.getResource(
				"/heads/content_creators_3d/" + filename));
			assertNotNull(filename + " must be readable", image);
			for (int corner : new int[] {
				image.getRGB(0, 0),
				image.getRGB(image.getWidth() - 1, 0),
				image.getRGB(0, image.getHeight() - 1),
				image.getRGB(image.getWidth() - 1, image.getHeight() - 1)
			})
			{
				assertEquals(filename + " must have transparent corners", 0, corner >>> 24);
			}

			int opaqueGreenPixels = 0;
			for (int y = 0; y < image.getHeight(); y++)
			{
				for (int x = 0; x < image.getWidth(); x++)
				{
					int argb = image.getRGB(x, y);
					int alpha = argb >>> 24;
					int red = (argb >>> 16) & 0xFF;
					int green = (argb >>> 8) & 0xFF;
					int blue = argb & 0xFF;
					if (alpha > 24 && green > 100 && green > red * 1.35 && green > blue * 1.35)
					{
						opaqueGreenPixels++;
					}
				}
			}
			assertEquals(filename + " must not contain opaque chroma-key pixels", 0, opaqueGreenPixels);
		}
	}

	private static void assertNoEnclosedTransparency(String filename, BufferedImage image)
	{
		int width = image.getWidth();
		int height = image.getHeight();
		boolean[] transparent = new boolean[width * height];
		boolean[] outside = new boolean[width * height];
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int index = y * width + x;
				transparent[index] = (image.getRGB(x, y) >>> 24) <= 24;
				if (transparent[index] && (x == 0 || y == 0 || x == width - 1 || y == height - 1))
				{
					outside[index] = true;
					queue.add(index);
				}
			}
		}

		while (!queue.isEmpty())
		{
			int index = queue.remove();
			int x = index % width;
			int y = index / width;
			for (int neighbor : new int[] {
				x > 0 ? index - 1 : -1,
				x + 1 < width ? index + 1 : -1,
				y > 0 ? index - width : -1,
				y + 1 < height ? index + width : -1
			})
			{
				if (neighbor >= 0 && transparent[neighbor] && !outside[neighbor])
				{
					outside[neighbor] = true;
					queue.add(neighbor);
				}
			}
		}

		for (int index = 0; index < transparent.length; index++)
		{
			if (!outside[index])
			{
				assertEquals(filename + " contains semi-transparent artwork at pixel " + index,
					255, image.getRGB(index % width, index / width) >>> 24);
			}
		}
	}

	@Test
	public void sidePanelIconUsesPluginSpecificResource()
	{
		assertEquals("/face_swap_icon.png", FaceSwapPlugin.SIDEPANEL_ICON_RESOURCE);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			FaceSwapPlugin.SIDEPANEL_ICON_RESOURCE) != null);
	}
}
