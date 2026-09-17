package com.faceswap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FaceSwapCustomImageStoreTest
{
	@Test
	public void missingImageIdsAreTreatedAsUnavailable()
		throws IOException
	{
		Path directory = Files.createTempDirectory("face-swap-custom-heads-null");
		try
		{
			FaceSwapCustomImageStore store = new FaceSwapCustomImageStore(directory);
			assertNull(store.getImage(null));
			assertNull(store.getBackImage(null));
		}
		finally
		{
			Files.deleteIfExists(directory.resolve("recent.properties"));
			Files.deleteIfExists(directory);
		}
	}

	@Test
	public void importsUserSelectedImagesIntoPluginDirectory() throws IOException
	{
		Path directory = Files.createTempDirectory("face-swap-custom-heads");
		Path source = Files.createTempFile("face-swap-source", ".png");
		Path backSource = Files.createTempFile("face-swap-back-source", ".png");
		FaceSwapCustomImageStore store = new FaceSwapCustomImageStore(directory);
		try
		{
			assertTrue(ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), "png", source.toFile()));
			assertTrue(ImageIO.write(new BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB), "png", backSource.toFile()));
			FaceSwapCustomImageStore.Entry entry = store.importImage(source);

			assertNotNull(entry);
			assertTrue(Files.isRegularFile(directory.resolve(entry.id + ".png")));
			assertSame(store.getImage(entry.id), store.getSelectedImage());

			store.importBackImage(entry.id, backSource);
			assertTrue(Files.isRegularFile(directory.resolve(entry.id + "_back.png")));
			assertNotNull(store.getBackImage(entry.id));
			assertSame(store.getBackImage(entry.id), store.getSelectedBackImage());

			FaceSwapCustomImageStore reloaded = new FaceSwapCustomImageStore(directory);
			reloaded.load();
			assertNotNull(reloaded.getBackImage(entry.id));
		}
		finally
		{
			store.clear();
			Files.deleteIfExists(source);
			Files.deleteIfExists(backSource);
			Files.deleteIfExists(directory.resolve("recent.properties"));
			Files.deleteIfExists(directory);
		}
	}

	@Test
	public void replacesFrontImageWithoutLosingItsBackPair() throws IOException
	{
		Path directory = Files.createTempDirectory("face-swap-custom-heads-replace");
		Path source = Files.createTempFile("face-swap-front", ".png");
		Path replacement = Files.createTempFile("face-swap-front-replacement", ".png");
		Path backSource = Files.createTempFile("face-swap-back", ".png");
		FaceSwapCustomImageStore store = new FaceSwapCustomImageStore(directory);
		try
		{
			BufferedImage front = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
			front.setRGB(1, 1, 0xFFFF0000);
			BufferedImage replacementImage = new BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < replacementImage.getHeight(); y++)
			{
				for (int x = 0; x < replacementImage.getWidth(); x++)
				{
					replacementImage.setRGB(x, y, 0xFF00FF00);
				}
			}
			assertTrue(ImageIO.write(front, "png", source.toFile()));
			assertTrue(ImageIO.write(replacementImage, "png", replacement.toFile()));
			assertTrue(ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB), "png", backSource.toFile()));

			FaceSwapCustomImageStore.Entry entry = store.importImage(source);
			store.importBackImage(entry.id, backSource);
			store.replaceImage(entry.id, replacement);

			assertEquals(entry.id, store.getSelectedId());
			assertEquals(replacement.getFileName().toString(), store.getRecents().get(0).name);
			assertTrue(store.getRecents().get(0).hasBack);
			assertEquals(0xFF00FF00, store.getImage(entry.id).getRGB(256, 256));
			assertNotNull(store.getBackImage(entry.id));

			FaceSwapCustomImageStore reloaded = new FaceSwapCustomImageStore(directory);
			reloaded.load();
			assertEquals(entry.id, reloaded.getSelectedId());
			assertEquals(replacement.getFileName().toString(), reloaded.getRecents().get(0).name);
			assertTrue(reloaded.getRecents().get(0).hasBack);
			assertNotNull(reloaded.getBackImage(entry.id));
		}
		finally
		{
			store.clear();
			Files.deleteIfExists(source);
			Files.deleteIfExists(replacement);
			Files.deleteIfExists(backSource);
			Files.deleteIfExists(directory.resolve("recent.properties"));
			Files.deleteIfExists(directory.resolve(""));
		}
	}

	@Test
	public void removesPairAndClearsSelectedImages() throws IOException
	{
		Path directory = Files.createTempDirectory("face-swap-custom-heads-remove");
		Path source = Files.createTempFile("face-swap-remove-front", ".png");
		Path backSource = Files.createTempFile("face-swap-remove-back", ".png");
		FaceSwapCustomImageStore store = new FaceSwapCustomImageStore(directory);
		try
		{
			assertTrue(ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), "png", source.toFile()));
			assertTrue(ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), "png", backSource.toFile()));
			FaceSwapCustomImageStore.Entry entry = store.importImage(source);
			store.importBackImage(entry.id, backSource);

			assertTrue(store.remove(entry.id));
			assertTrue(store.getRecents().isEmpty());
			assertNull(store.getSelectedId());
			assertNull(store.getImage(entry.id));
			assertNull(store.getBackImage(entry.id));
			assertFalse(Files.exists(directory.resolve(entry.id + ".png")));
			assertFalse(Files.exists(directory.resolve(entry.id + "_back.png")));
		}
		finally
		{
			store.clear();
			Files.deleteIfExists(source);
			Files.deleteIfExists(backSource);
			Files.deleteIfExists(directory.resolve("recent.properties"));
			Files.deleteIfExists(directory);
		}
	}
}
