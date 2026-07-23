package com.faceswap.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import net.runelite.cache.ConfigType;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.KitDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.KitLoader;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.ArchiveFiles;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Store;

public final class AuditHeadModels
{
	private AuditHeadModels()
	{
	}

	public static void main(String[] args) throws Exception
	{
		Path cachePath = Path.of(args[0]);
		try (Store store = new Store(cachePath.toFile()))
		{
			store.load();
			System.out.println("kind,kit,bodyPart,model,vertices,faces,groups,groupedVertices,spanX,spanY,spanZ");
			auditModel(store, "elf", -1, -1, 30539);

			Index configs = store.getIndex(IndexType.CONFIGS);
			Archive archive = configs.getArchive(ConfigType.IDENTKIT.getId());
			ArchiveFiles files = archive.getFiles(store.getStorage().loadArchive(archive));
			KitLoader loader = new KitLoader();
			for (FSFile file : files.getFiles())
			{
				KitDefinition kit = loader.load(file.getFileId(), file.getContents());
				if (!kit.nonSelectable && kit.bodyPartId >= 0 && kit.bodyPartId <= 6 && kit.models != null)
				{
					for (int modelId : kit.models)
					{
						auditModel(store, "kit", kit.getId(), kit.bodyPartId, modelId);
					}
				}
			}
		}
	}

	private static void auditModel(Store store, String kind, int kitId, int bodyPart, int modelId)
		throws IOException
	{
		Index models = store.getIndex(IndexType.MODELS);
		Archive archive = models.getArchive(modelId);
		if (archive == null)
		{
			return;
		}

		byte[] packed = store.getStorage().loadArchive(archive);
		ModelDefinition model = new ModelLoader().load(modelId, archive.decompress(packed));
		int[][] groups = model.getVertexGroups();
		int groupedVertices = groups == null ? 0 : Arrays.stream(groups).mapToInt(group -> group.length).sum();
		int minX = Arrays.stream(model.vertexX).min().orElse(0);
		int maxX = Arrays.stream(model.vertexX).max().orElse(0);
		int minY = Arrays.stream(model.vertexY).min().orElse(0);
		int maxY = Arrays.stream(model.vertexY).max().orElse(0);
		int minZ = Arrays.stream(model.vertexZ).min().orElse(0);
		int maxZ = Arrays.stream(model.vertexZ).max().orElse(0);
		System.out.printf("%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n", kind, kitId, bodyPart, modelId,
			model.vertexCount, model.faceCount, groups == null ? 0 : groups.length, groupedVertices,
			maxX - minX, maxY - minY, maxZ - minZ);
	}
}
