package com.faceswap;

import java.awt.Color;
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class FaceSwapPanelTest
{
	@Test
	public void demoHeadsReceiveTopPickerBadge()
	{
		BufferedImage demo = FaceSwapPanel.createPickerThumbnail(FaceSwapHead.ODABLOCK);
		BufferedImage release = FaceSwapPanel.createPickerThumbnail(FaceSwapHead.SARDACO);

		assertEquals(Color.BLACK.getRGB(), demo.getRGB(2, 2));
		assertNotEquals(Color.BLACK.getRGB(), release.getRGB(2, 2));
	}

	@Test
	public void fictionalPickerPlacesAgentAndMonkeyOnSecondLastRow()
	{
		List<FaceSwapHead> heads = FaceSwapPanel.orderedHeadsForPicker(FaceSwapHeadCategory.FICTIONAL_CHARACTER);

		assertEquals(FaceSwapHead.AGENT, heads.get(6));
		assertEquals(FaceSwapHead.MONKEY, heads.get(7));
		assertEquals(FaceSwapHead.CHOSEN_ONE, heads.get(8));
		assertEquals(FaceSwapHead.MARTIAL_ARTIST, heads.get(9));
	}

	@Test
	public void contentCreatorPickerUsesPopularityOrdering()
	{
		List<FaceSwapHead> heads = FaceSwapPanel.orderedHeadsForPicker(FaceSwapHeadCategory.CONTENT_CREATOR);

		assertEquals(FaceSwapHead.ODABLOCK, heads.get(0));
		assertEquals(FaceSwapHead.FAUX_OSRS, heads.get(1));
		assertEquals(FaceSwapHead.ALFIE, heads.get(3));
		assertEquals(FaceSwapHead.SARDACO, heads.get(4));
		assertEquals(FaceSwapHead.KING_CONDOR, heads.get(5));
		assertEquals(FaceSwapHead.GRIM, heads.get(11));
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
