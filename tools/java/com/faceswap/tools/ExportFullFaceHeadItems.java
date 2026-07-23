package com.faceswap.tools;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.runelite.cache.ItemManager;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.fs.Store;

public final class ExportFullFaceHeadItems
{
	private static final List<String> FACE_EXPOSING_NAME_PARTS = List.of(
		"eclipse moon helm",
		"zombie helmet",
		"dharok's helm",
		"blood moon helm",
		"crystal helm",
		"blue moon helm",
		"graceful hood"
	);

	private ExportFullFaceHeadItems()
	{
	}

	public static void main(String[] args) throws Exception
	{
		Path output = Path.of(args[1]);
		try (Store store = new Store(Path.of(args[0]).toFile()))
		{
			store.load();
			ItemManager itemManager = new ItemManager(store);
			itemManager.load();
			itemManager.link();
			try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8))
			{
				writer.write("item_id,item_name\n");
				itemManager.getItems().stream()
					.filter(ExportFullFaceHeadItems::suppressesHeadAppearance)
					.sorted(Comparator.comparingInt(item -> item.id))
					.forEach(item -> writeItem(writer, item));
			}
		}
	}

	private static boolean suppressesHeadAppearance(ItemDefinition item)
	{
		String itemName = item.name.toLowerCase(Locale.ROOT);
		if (itemName.contains(" med helm")
			|| FACE_EXPOSING_NAME_PARTS.stream().anyMatch(itemName::contains))
		{
			return false;
		}

		boolean hidesHair = item.wearPos2 == 8 || item.wearPos3 == 8;
		boolean hidesJaw = item.wearPos2 == 11 || item.wearPos3 == 11;
		return item.wearPos1 == 0 && hidesHair && hidesJaw;
	}

	private static void writeItem(BufferedWriter writer, ItemDefinition item)
	{
		try
		{
			writer.write(item.id + "," + item.name.replace(',', ' ') + "\n");
		}
		catch (Exception ex)
		{
			throw new IllegalStateException("Unable to export item " + item.id, ex);
		}
	}
}
