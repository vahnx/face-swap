package com.faceswap.tools;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.cache.ItemManager;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.fs.Store;

public final class AuditHelmetCoverage
{
	private AuditHelmetCoverage()
	{
	}

	public static void main(String[] args) throws Exception
	{
		Set<Integer> requestedIds = new HashSet<>();
		Arrays.stream(args).skip(1).mapToInt(Integer::parseInt).forEach(requestedIds::add);
		try (Store store = new Store(Path.of(args[0]).toFile()))
		{
			store.load();
			ItemManager itemManager = new ItemManager(store);
			itemManager.load();
			itemManager.link();
			System.out.println("id,name,wearPos1,wearPos2,wearPos3,hidesHair,hidesJaw");
			for (int itemId : requestedIds)
			{
				ItemDefinition item = itemManager.getItem(itemId);
				if (item != null)
				{
					boolean hidesHair = item.wearPos2 == 8 || item.wearPos3 == 8;
					boolean hidesJaw = item.wearPos2 == 11 || item.wearPos3 == 11;
					System.out.printf("%d,%s,%d,%d,%d,%s,%s%n", item.id,
						item.name.replace(',', ' '), item.wearPos1, item.wearPos2, item.wearPos3,
						hidesHair, hidesJaw);
				}
			}
		}
	}
}
