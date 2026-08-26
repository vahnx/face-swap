package com.faceswap;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FaceSwapPanelTest
{
	// Odablock's demo-thumbnail coverage is disabled while the head is commented out.

	@Test
	public void fictionalPickerOrdering()
	{
		List<FaceSwapHead> heads = FaceSwapPanel.orderedHeadsForPicker(FaceSwapHeadCategory.FICTIONAL_CHARACTER);

		assertEquals(FaceSwapHead.PUG, heads.get(0));
		assertEquals(FaceSwapHead.CLASSIC_ADVENTURER, heads.get(6));
		assertFalse(heads.stream().anyMatch(head -> head.name().equals("AGENT")));
		assertFalse(heads.stream().anyMatch(head -> head.name().equals("MONKEY")));
	}

	@Test
	public void contentCreatorPickerUsesPopularityOrdering()
	{
		List<FaceSwapHead> heads = FaceSwapPanel.orderedHeadsForPicker(FaceSwapHeadCategory.CONTENT_CREATOR);

		FaceSwapHead[] expectedCreators = {
			FaceSwapHead.SARDACO,
			FaceSwapHead.SKILL_SPECS,
			FaceSwapHead.TASTYLIFE,
			FaceSwapHead.TPAPASLICE,
			FaceSwapHead.PRISONJOE,
			FaceSwapHead.ZECOOKIES,
			FaceSwapHead.ALFIE,
			FaceSwapHead.FOX_OSRS,
			FaceSwapHead.KING_CONDOR,
			FaceSwapHead.DEARLOLA,
			FaceSwapHead.ELIOP14,
			FaceSwapHead.JILLYFISH,
			FaceSwapHead.BEGGAR,
			FaceSwapHead.GRIM,
			FaceSwapHead.ASIAN_ANDY
		};

		for (FaceSwapHead creator : expectedCreators)
		{
			assertTrue(heads.contains(creator));
		}
	}

	@Test
	public void emojiPickerOmitsVisualDuplicates()
	{
		assertEquals(List.of(
			FaceSwapHead.SMILEY,
			FaceSwapHead.HEART_EYES,
			FaceSwapHead.POOP,
			FaceSwapHead.COOL,
			FaceSwapHead.ANGRY,
			FaceSwapHead.SAD,
			FaceSwapHead.SURPRISED,
			FaceSwapHead.HEART,
			FaceSwapHead.ROBOT),
			FaceSwapPanel.orderedHeadsForPicker(FaceSwapHeadCategory.EMOJI));
	}

	@Test
	public void renderModeArtworkIsPackagedAndBuildsCompactThumbnails() throws Exception
	{
		assertModeArtwork("/mode_icons/mode_3d.png", FaceSwapRenderMode.THREE_D);
		assertModeArtwork("/mode_icons/mode_mask.png", FaceSwapRenderMode.MASK);
		assertModeArtwork("/mode_icons/mode_wraparound.png", FaceSwapRenderMode.TWO_D);
	}

	@Test
	public void styleButtonsDoNotCaptureArrowKeysButDropdownsRemainFocusable()
	{
		FaceSwapPanel panel = new FaceSwapPanel(null, new FaceSwapPanelState(
			FaceSwapHead.SARDACO,
			FaceSwapTargetScope.SELF,
			"",
			10,
			FaceSwapNpcTargetScope.DISABLED,
			"",
			FaceSwapRenderMode.THREE_D,
			1,
			false,
			70,
			false,
			""));

		List<JToggleButton> styleButtons = findComponents(panel, JToggleButton.class);
		styleButtons.removeIf(button -> button.getAccessibleContext().getAccessibleName() == null
			|| !button.getAccessibleContext().getAccessibleName().endsWith(" style"));
		assertEquals(3, styleButtons.size());
		assertTrue(styleButtons.stream().noneMatch(Component::isFocusable));

		List<JComboBox> dropdowns = findComponents(panel, JComboBox.class);
		assertEquals(2, dropdowns.size());
		assertFalse(dropdowns.stream().anyMatch(dropdown -> !dropdown.isFocusable()));
	}

	private static void assertModeArtwork(String resourcePath, FaceSwapRenderMode renderMode) throws Exception
	{
		try (InputStream input = FaceSwapPanelTest.class.getResourceAsStream(resourcePath))
		{
			assertNotNull(input);
			BufferedImage artwork = ImageIO.read(input);
			assertNotNull(artwork);
			assertEquals(128, artwork.getWidth());
			assertEquals(128, artwork.getHeight());
		}

		BufferedImage thumbnail = FaceSwapPanel.createModeThumbnail(resourcePath, renderMode);
		assertEquals(56, thumbnail.getWidth());
		assertEquals(72, thumbnail.getHeight());
	}

	private static <T extends Component> List<T> findComponents(Container root, Class<T> componentType)
	{
		List<T> matches = new ArrayList<>();
		for (Component child : root.getComponents())
		{
			if (componentType.isInstance(child))
			{
				matches.add(componentType.cast(child));
			}
			if (child instanceof Container)
			{
				matches.addAll(findComponents((Container) child, componentType));
			}
		}
		return matches;
	}
}
