package com.faceswap;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FaceSwapPanelTest
{
	@Test
	public void fictionalPickerOrdering()
	{
		List<FaceSwapHead> heads = FaceSwapPanel.orderedHeadsForPicker(FaceSwapHeadCategory.FICTIONAL_CHARACTER);

		assertEquals(FaceSwapHead.PUG, heads.get(0));
		assertEquals(FaceSwapHead.CLASSIC_ADVENTURER, heads.get(6));
		assertEquals(FaceSwapHead.PEPE, heads.get(7));
		assertEquals(FaceSwapHead.JD_VANCE, heads.get(8));
		assertFalse(heads.stream().anyMatch(head -> head.name().equals("AGENT")));
		assertFalse(heads.stream().anyMatch(head -> head.name().equals("MONKEY")));
	}

	@Test
	public void contentCreatorPickerUsesPopularityOrdering()
	{
		List<FaceSwapHead> heads = FaceSwapPanel.orderedHeadsForPicker(FaceSwapHeadCategory.CONTENT_CREATOR);

		FaceSwapHead[] expectedCreators = {
			FaceSwapHead.GNOMONKEY,
			FaceSwapHead.ODABLOCK,
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
			FaceSwapHead.ASIAN_ANDY,
			FaceSwapHead.MINT_MADCOW,
			FaceSwapHead.JOSH_PILLAUT,
			FaceSwapHead.MINT_MADCOW_OSRS,
			FaceSwapHead.BRETTDOG_OSRS,
			FaceSwapHead.MMORPG_OSRS,
			FaceSwapHead.JOSH_PILLAUT_OSRS,
			FaceSwapHead.ODABLOCK_OSRS,
			FaceSwapHead.MRNOSLEEP_OSRS,
			FaceSwapHead.SICK_NERD_OSRS
		};

		for (FaceSwapHead creator : expectedCreators)
		{
			assertTrue(heads.contains(creator));
		}
		assertEquals(FaceSwapHead.ODABLOCK, heads.get(0));
		assertEquals(FaceSwapHead.GNOMONKEY, heads.get(6));
		assertEquals(FaceSwapHead.ODABLOCK_OSRS, heads.get(22));
		assertEquals(FaceSwapHead.GNOMONKEY_OSRS, heads.get(28));
		assertTrue(heads.contains(FaceSwapHead.MRNOSLEEP));
		assertTrue(heads.contains(FaceSwapHead.SICK_NERD));
		assertTrue(heads.contains(FaceSwapHead.JOSH_PILLAUT));
		assertTrue(heads.contains(FaceSwapHead.MINT_MADCOW));
		assertFalse(heads.contains(FaceSwapHead.MRNOSLEEP_MASK));
		assertEquals(FaceSwapHead.MRNOSLEEP, FaceSwapHead.MRNOSLEEP_MASK.getBaseVariant());
		assertEquals(FaceSwapHead.GNOMONKEY_OSRS, FaceSwapHead.GNOMONKEY.getOsrsVariant());
		assertEquals(FaceSwapHead.GNOMONKEY, FaceSwapHead.GNOMONKEY_OSRS.getBaseVariant());
	}

	@Test
	public void osrsVariantsBelongToTheirBaseCreator()
	{
		assertEquals("The RS Felon", FaceSwapHead.JOSH_PILLAUT.toString());
		assertEquals("The RS Felon (OSRS)", FaceSwapHead.JOSH_PILLAUT_OSRS.toString());
		assertEquals(FaceSwapHead.TPAPASLICE_OSRS, FaceSwapHead.TPAPASLICE.getOsrsVariant());
		assertEquals(FaceSwapHead.TPAPASLICE, FaceSwapHead.TPAPASLICE_OSRS.getBaseVariant());
		assertEquals(FaceSwapHead.TASTYLIFE_OSRS, FaceSwapHead.TASTYLIFE.getOsrsVariant());
		assertEquals(FaceSwapHead.BRETTDOG_OSRS, FaceSwapHead.BRETTDOG.getOsrsVariant());
		assertEquals(FaceSwapHead.BRETTDOG, FaceSwapHead.BRETTDOG_OSRS.getBaseVariant());
		assertEquals(FaceSwapHead.MINT_MADCOW_OSRS, FaceSwapHead.MINT_MADCOW.getOsrsVariant());
		assertEquals(FaceSwapHead.MINT_MADCOW, FaceSwapHead.MINT_MADCOW_OSRS.getBaseVariant());
		assertEquals(FaceSwapHead.MMORPG_OSRS, FaceSwapHead.MMORPG.getOsrsVariant());
		assertEquals(FaceSwapHead.MMORPG, FaceSwapHead.MMORPG_OSRS.getBaseVariant());
		assertEquals(FaceSwapHead.JOSH_PILLAUT_OSRS, FaceSwapHead.JOSH_PILLAUT.getOsrsVariant());
		assertEquals(FaceSwapHead.JOSH_PILLAUT, FaceSwapHead.JOSH_PILLAUT_OSRS.getBaseVariant());
		assertEquals(FaceSwapHead.ODABLOCK_OSRS, FaceSwapHead.ODABLOCK.getOsrsVariant());
		assertEquals(FaceSwapHead.ODABLOCK, FaceSwapHead.ODABLOCK_OSRS.getBaseVariant());
		assertEquals(FaceSwapHead.MRNOSLEEP_OSRS, FaceSwapHead.MRNOSLEEP.getOsrsVariant());
		assertEquals(FaceSwapHead.MRNOSLEEP, FaceSwapHead.MRNOSLEEP_OSRS.getBaseVariant());
		assertEquals(FaceSwapHead.SICK_NERD_OSRS, FaceSwapHead.SICK_NERD.getOsrsVariant());
		assertEquals(FaceSwapHead.SICK_NERD, FaceSwapHead.SICK_NERD_OSRS.getBaseVariant());
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
	public void selectedCustomPreviewUsesTheLoadedImage()
	{
		BufferedImage customImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		customImage.setRGB(4, 4, 0xFFFF0000);

		BufferedImage thumbnail = FaceSwapPanel.createSelectedHeadPreviewThumbnail(
			FaceSwapHead.CUSTOM, customImage);

		assertEquals(96, thumbnail.getWidth());
		assertEquals(96, thumbnail.getHeight());
		assertTrue((thumbnail.getRGB(48, 48) & 0x00FF0000) > 0);
	}

	@Test
	public void styleButtonsDoNotCaptureArrowKeysButDropdownsRemainFocusable()
	{
		FaceSwapPanel panel = new FaceSwapPanel(null, new FaceSwapPanelState(
			FaceSwapHead.SARDACO,
			FaceSwapAssignment.DEFAULT_STYLE_ID,
			FaceSwapHeadPickerLayout.POPUP,
			false,
			FaceSwapTargetScope.SELF,
			"",
			10,
			FaceSwapNpcTargetScope.DISABLED,
			"",
			FaceSwapRenderMode.THREE_D,
			1,
			false,
			70,
			true,
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

	@Test
	public void inlineTabHeaderUsesTwoByTwoGridAndSwitchesSelection()
	{
		int[] selectedIndex = {-1};
		JPanel header = FaceSwapPanel.createTwoByTwoTabHeader(
			new String[] {"Creators", "Fictional", "Emoji", "Custom"}, 0,
			index -> selectedIndex[0] = index);

		assertEquals(2, ((GridLayout) header.getLayout()).getRows());
		assertEquals(2, ((GridLayout) header.getLayout()).getColumns());
		assertEquals(4, header.getComponentCount());
		assertEquals("Creators", ((JToggleButton) header.getComponent(0)).getText());
		assertNull(((JToggleButton) header.getComponent(0)).getToolTipText());
		assertNull(((JToggleButton) header.getComponent(1)).getToolTipText());
		assertNull(((JToggleButton) header.getComponent(2)).getToolTipText());
		assertEquals("Select images from your PC", ((JToggleButton) header.getComponent(3)).getToolTipText());
		((JToggleButton) header.getComponent(1)).doClick();
		assertEquals(1, selectedIndex[0]);
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
