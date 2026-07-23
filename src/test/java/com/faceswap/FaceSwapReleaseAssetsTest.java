package com.faceswap;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
	public void debugOnlyAssetsRemainOnDeveloperClasspath()
	{
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators/odablock_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/content_creators/odablock_back.png") != null);
	}

	@Test
	public void fictionalAssetsUseTheirReleaseDirectory()
	{
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/fictional_characters/agent_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/fictional_characters/agent_back.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/fictional_characters/monkey_front.png") != null);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			"/heads/fictional_characters/monkey_back.png") != null);
	}

	@Test
	public void sidePanelIconUsesPluginSpecificResource()
	{
		assertEquals("/face_swap_icon.png", FaceSwapPlugin.SIDEPANEL_ICON_RESOURCE);
		assertTrue(FaceSwapReleaseAssetsTest.class.getResource(
			FaceSwapPlugin.SIDEPANEL_ICON_RESOURCE) != null);
	}
}
