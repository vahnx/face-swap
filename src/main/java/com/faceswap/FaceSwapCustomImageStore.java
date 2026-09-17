package com.faceswap;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.runelite.client.RuneLite;

final class FaceSwapCustomImageStore
{
	static final int IMAGE_SIZE = 512;
	private static final int MAX_RECENTS = 12;
	private final Path directory;
	private final Path metadataFile;
	private final List<Entry> recents = new ArrayList<>();
	private final Map<String, BufferedImage> images = new ConcurrentHashMap<>();
	private final Map<String, BufferedImage> backImages = new ConcurrentHashMap<>();
	private volatile String selectedId;

	FaceSwapCustomImageStore()
	{
		this(RuneLite.RUNELITE_DIR.toPath().resolve("face-swap").resolve("custom-heads"));
	}

	FaceSwapCustomImageStore(Path directory)
	{
		this.directory = directory.toAbsolutePath().normalize();
		this.metadataFile = this.directory.resolve("recent.properties");
	}

	Path getDirectory()
	{
		return directory;
	}

	void load()
	{
		try
		{
			Files.createDirectories(directory);
			Properties properties = new Properties();
			if (Files.exists(metadataFile))
			{
				try (InputStream input = Files.newInputStream(metadataFile))
				{
					properties.load(input);
				}
			}

			int count = Integer.parseInt(properties.getProperty("count", "0"));
			for (int index = 0; index < count; index++)
			{
				String id = properties.getProperty("entry." + index + ".id");
				String name = properties.getProperty("entry." + index + ".name");
				if (id != null && name != null && Files.isRegularFile(imagePath(id)))
				{
					BufferedImage image = ImageIO.read(imagePath(id).toFile());
					if (image != null)
					{
						images.put(id, image);
						BufferedImage backImage = Files.isRegularFile(backImagePath(id))
							? ImageIO.read(backImagePath(id).toFile()) : null;
						if (backImage != null)
						{
							backImages.put(id, backImage);
						}
						recents.add(new Entry(id, name, backImage != null));
					}
				}
			}
			selectedId = properties.getProperty("selected");
			if (selectedId != null)
			{
				loadSelected();
			}
		}
		catch (IOException | NumberFormatException ignored)
		{
			// A damaged local history should not prevent the plugin from starting.
		}
	}

	Entry importImage(Path source) throws IOException
	{
		if (source == null)
		{
			throw new IOException("No image was selected");
		}

		Files.createDirectories(directory);
		BufferedImage decoded = ImageIO.read(source.toFile());
		if (decoded == null)
		{
			throw new IOException("Unsupported image format");
		}

		BufferedImage normalized = normalize(decoded);
		Files.createDirectories(directory);
		String id = UUID.randomUUID().toString();
		ImageIO.write(normalized, "png", imagePath(id).toFile());
		Entry entry = new Entry(id, source.getFileName().toString());
		recents.removeIf(existing -> existing.id.equals(id));
		recents.add(0, entry);
		selectedId = id;
		images.put(id, normalized);
		trimRecents();
		persist();
		applySelectedImages();
		return entry;
	}

	void importBackImage(String id, Path source) throws IOException
	{
		if (id == null || !images.containsKey(id) || !Files.isRegularFile(imagePath(id)))
		{
			throw new IOException("Select a custom front image first");
		}
		if (source == null)
		{
			throw new IOException("No image was selected");
		}

		BufferedImage decoded = ImageIO.read(source.toFile());
		if (decoded == null)
		{
			throw new IOException("Unsupported image format");
		}

		BufferedImage normalized = normalize(decoded);
		ImageIO.write(normalized, "png", backImagePath(id).toFile());
		backImages.put(id, normalized);
		for (int index = 0; index < recents.size(); index++)
		{
			Entry entry = recents.get(index);
			if (entry.id.equals(id))
			{
				recents.set(index, new Entry(entry.id, entry.name, true));
				break;
			}
		}
		selectedId = id;
		persist();
		applySelectedImages();
	}

	void replaceImage(String id, Path source) throws IOException
	{
		if (id == null || !images.containsKey(id) || !Files.isRegularFile(imagePath(id)))
		{
			throw new IOException("Select a custom image pair first");
		}
		if (source == null)
		{
			throw new IOException("No image was selected");
		}

		BufferedImage decoded = ImageIO.read(source.toFile());
		if (decoded == null)
		{
			throw new IOException("Unsupported image format");
		}

		BufferedImage normalized = normalize(decoded);
		ImageIO.write(normalized, "png", imagePath(id).toFile());
		images.put(id, normalized);
		selectedId = id;
		for (int index = 0; index < recents.size(); index++)
		{
			Entry entry = recents.get(index);
			if (entry.id.equals(id))
			{
				recents.set(index, new Entry(id, source.getFileName().toString(), entry.hasBack));
				if (index > 0)
				{
					recents.add(0, recents.remove(index));
				}
				break;
			}
		}
		persist();
		applySelectedImages();
	}

	boolean select(String id)
	{
		if (id == null || !Files.isRegularFile(imagePath(id)))
		{
			return false;
		}
		if (!images.containsKey(id))
		{
			return false;
		}
		selectedId = id;
		applySelectedImages();
		try
		{
			persist();
		}
		catch (IOException ignored)
		{
			// The image is still usable for this session.
		}
		return true;
	}

	List<Entry> getRecents()
	{
		return Collections.unmodifiableList(new ArrayList<>(recents));
	}

	BufferedImage getImage(String id)
	{
		return id == null ? null : images.get(id);
	}

	BufferedImage getSelectedImage()
	{
		return selectedId == null ? null : images.get(selectedId);
	}

	BufferedImage getBackImage(String id)
	{
		return id == null ? null : backImages.get(id);
	}

	BufferedImage getSelectedBackImage()
	{
		return selectedId == null ? null : backImages.get(selectedId);
	}

	String getSelectedId()
	{
		return selectedId;
	}

	boolean hasSelectedImage()
	{
		return selectedId != null && images.containsKey(selectedId);
	}

	void clear()
	{
		for (Entry entry : recents)
		{
			try
			{
				Files.deleteIfExists(imagePath(entry.id));
				Files.deleteIfExists(backImagePath(entry.id));
			}
			catch (IOException ignored)
			{
				// Best-effort cleanup of user-owned cache files.
			}
		}
		recents.clear();
		images.clear();
		backImages.clear();
		selectedId = null;
		FaceSwapHeadImages.setCustomImages(null, null);
		try
		{
			persist();
		}
		catch (IOException ignored)
		{
			// The in-memory history has still been cleared.
		}
	}

	boolean remove(String id)
	{
		if (id == null)
		{
			return false;
		}

		Entry removed = null;
		for (Entry entry : recents)
		{
			if (entry.id.equals(id))
			{
				removed = entry;
				break;
			}
		}
		if (removed == null)
		{
			return false;
		}

		try
		{
			Files.deleteIfExists(imagePath(id));
			Files.deleteIfExists(backImagePath(id));
		}
		catch (IOException ignored)
		{
			// Keep the in-memory history usable even if a stale cache file cannot be deleted.
		}
		recents.remove(removed);
		images.remove(id);
		backImages.remove(id);
		if (id.equals(selectedId))
		{
			selectedId = null;
			FaceSwapHeadImages.setCustomImages(null, null);
		}
		try
		{
			persist();
		}
		catch (IOException ignored)
		{
			// The in-memory history has still been updated.
		}
		return true;
	}

	static BufferedImage normalize(BufferedImage source)
	{
		BufferedImage normalized = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = normalized.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			double scale = Math.min(480d / Math.max(1, source.getWidth()), 480d / Math.max(1, source.getHeight()));
			int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
			int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
			graphics.drawImage(source, (IMAGE_SIZE - width) / 2, (IMAGE_SIZE - height) / 2, width, height, null);
		}
		finally
		{
			graphics.dispose();
		}
		return normalized;
	}

	private void loadSelected()
	{
		applySelectedImages();
	}

	private void applySelectedImages()
	{
		FaceSwapHeadImages.setCustomImages(
			selectedId == null ? null : images.get(selectedId),
			selectedId == null ? null : backImages.get(selectedId));
	}

	private void trimRecents()
	{
		while (recents.size() > MAX_RECENTS)
		{
			Entry removed = recents.remove(recents.size() - 1);
			try
			{
				Files.deleteIfExists(imagePath(removed.id));
				Files.deleteIfExists(backImagePath(removed.id));
			}
			catch (IOException ignored)
			{
				// Keep the metadata bounded even if a stale file cannot be deleted.
			}
		}
	}

	private void persist() throws IOException
	{
		Files.createDirectories(directory);
		Properties properties = new Properties();
		properties.setProperty("count", Integer.toString(recents.size()));
		if (selectedId != null)
		{
			properties.setProperty("selected", selectedId);
		}
		for (int index = 0; index < recents.size(); index++)
		{
			Entry entry = recents.get(index);
			properties.setProperty("entry." + index + ".id", entry.id);
			properties.setProperty("entry." + index + ".name", entry.name);
		}
		try (OutputStream output = Files.newOutputStream(metadataFile,
			StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
		{
			properties.store(output, "Face Swap custom image history");
		}
	}

	private Path imagePath(String id)
	{
		return directory.resolve(id + ".png");
	}

	private Path backImagePath(String id)
	{
		return directory.resolve(id + "_back.png");
	}

	static final class Entry
	{
		final String id;
		final String name;
		final boolean hasBack;

		Entry(String id, String name)
		{
			this(id, name, false);
		}

		Entry(String id, String name, boolean hasBack)
		{
			this.id = id;
			this.name = name;
			this.hasBack = hasBack;
		}
	}
}
