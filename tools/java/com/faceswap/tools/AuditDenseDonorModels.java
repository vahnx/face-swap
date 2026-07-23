package com.faceswap.tools;

import java.nio.file.Path;
import java.util.Arrays;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Store;

public final class AuditDenseDonorModels
{
	private static final int MIN_FACES = 1900;
	private static final int MAX_FACES = 2400;

	private AuditDenseDonorModels()
	{
	}

	public static void main(String[] args) throws Exception
	{
		try (Store store = new Store(Path.of(args[0]).toFile()))
		{
			store.load();
			Index models = store.getIndex(IndexType.MODELS);
			ModelLoader loader = new ModelLoader();
			System.out.println("model,vertices,faces,groups,groupedVertices,spanX,spanY,spanZ");
			for (Archive archive : models.getArchives())
			{
				try
				{
					byte[] packed = store.getStorage().loadArchive(archive);
					ModelDefinition model = loader.load(archive.getArchiveId(), archive.decompress(packed));
					if (model.faceCount < MIN_FACES || model.faceCount > MAX_FACES)
					{
						continue;
					}

					int[][] groups = model.getVertexGroups();
					if (groups == null || groups.length != 4)
					{
						continue;
					}
					int groupedVertices = Arrays.stream(groups).mapToInt(group -> group.length).sum();
					if (groupedVertices < model.vertexCount)
					{
						continue;
					}

					int spanX = span(model.vertexX);
					int spanY = span(model.vertexY);
					int spanZ = span(model.vertexZ);
					System.out.printf("%d,%d,%d,%d,%d,%d,%d,%d%n", model.id, model.vertexCount,
						model.faceCount, groups.length, groupedVertices, spanX, spanY, spanZ);
				}
				catch (Exception ignored)
				{
					// Some cache models use encodings unsupported by this cache library version.
				}
			}
		}
	}

	private static int span(int[] values)
	{
		return Arrays.stream(values).max().orElse(0) - Arrays.stream(values).min().orElse(0);
	}
}
