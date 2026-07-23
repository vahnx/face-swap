package com.faceswap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class FullFaceHeadItems
{
	private static final String RESOURCE = "/full_face_head_items.csv";
	private static final Set<Integer> ITEM_IDS = load();

	private FullFaceHeadItems()
	{
	}

	static boolean contains(int itemId)
	{
		return ITEM_IDS.contains(itemId);
	}

	static int size()
	{
		return ITEM_IDS.size();
	}

	private static Set<Integer> load()
	{
		InputStream stream = FullFaceHeadItems.class.getResourceAsStream(RESOURCE);
		if (stream == null)
		{
			throw new IllegalStateException("Missing full-face item resource " + RESOURCE);
		}

		Set<Integer> itemIds = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
		{
			String header = reader.readLine();
			if (!"item_id,item_name".equals(header))
			{
				throw new IllegalStateException("Unexpected full-face item header: " + header);
			}

			String line;
			while ((line = reader.readLine()) != null)
			{
				if (!line.isBlank())
				{
					itemIds.add(Integer.parseInt(line.substring(0, line.indexOf(','))));
				}
			}
		}
		catch (IOException | NumberFormatException ex)
		{
			throw new IllegalStateException("Unable to load full-face items", ex);
		}
		return Collections.unmodifiableSet(itemIds);
	}
}
