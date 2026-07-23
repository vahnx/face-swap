package com.faceswap.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.runelite.cache.IndexType;
import net.runelite.cache.ItemManager;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Store;

public final class ExportElfHead
{
	private static final int ENSOULED_ELF_HEAD = 13480;

	private ExportElfHead()
	{
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length != 2)
		{
			throw new IllegalArgumentException("Usage: ExportElfHead <cache-directory> <output-directory>");
		}

		Path cachePath = Path.of(args[0]);
		Path outputPath = Path.of(args[1]);
		Files.createDirectories(outputPath);

		try (Store store = new Store(cachePath.toFile()))
		{
			store.load();
			ItemManager items = new ItemManager(store);
			items.load();
			items.link();

			ItemDefinition item = items.getItem(ENSOULED_ELF_HEAD);
			if (item == null)
			{
				throw new IOException("Ensouled elf head item " + ENSOULED_ELF_HEAD + " was not found");
			}

			ModelDefinition model = loadModel(store, item.inventoryModel);
			applyItemTransforms(model, item);
			writeJson(outputPath.resolve("ensouled_elf_head.json"), model, item);
			writeObj(outputPath.resolve("ensouled_elf_head.obj"), model, item);

			System.out.printf("Exported item %d, model %d: %d vertices, %d triangles%n",
				item.id, model.id, model.vertexCount, model.faceCount);
		}
	}

	private static ModelDefinition loadModel(Store store, int modelId) throws IOException
	{
		Index models = store.getIndex(IndexType.MODELS);
		Archive archive = models.getArchive(modelId);
		if (archive == null)
		{
			throw new IOException("Model " + modelId + " was not found");
		}

		byte[] packed = store.getStorage().loadArchive(archive);
		return new ModelLoader().load(modelId, archive.decompress(packed));
	}

	private static void applyItemTransforms(ModelDefinition model, ItemDefinition item)
	{
		model.resize(item.resizeX, item.resizeY, item.resizeZ);
		if (item.colorFind != null)
		{
			for (int i = 0; i < item.colorFind.length; i++)
			{
				model.recolor(item.colorFind[i], item.colorReplace[i]);
			}
		}
	}

	private static void writeJson(Path path, ModelDefinition model, ItemDefinition item) throws IOException
	{
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
		{
			writer.write("{\n  \"itemId\": " + item.id + ",\n");
			writer.write("  \"modelId\": " + model.id + ",\n");
			writer.write("  \"vertices\": [\n");
			for (int i = 0; i < model.vertexCount; i++)
			{
				writer.write(String.format("    [%d, %d, %d]%s%n", model.vertexX[i], model.vertexY[i],
					model.vertexZ[i], i + 1 == model.vertexCount ? "" : ","));
			}
			writer.write("  ],\n  \"faces\": [\n");
			for (int i = 0; i < model.faceCount; i++)
			{
				writer.write(String.format("    [%d, %d, %d]%s%n", model.faceIndices1[i], model.faceIndices2[i],
					model.faceIndices3[i], i + 1 == model.faceCount ? "" : ","));
			}
			writer.write("  ],\n  \"faceColors\": [");
			for (int i = 0; i < model.faceCount; i++)
			{
				writer.write(Integer.toString(model.faceColors[i] & 0xffff));
				if (i + 1 != model.faceCount)
				{
					writer.write(", ");
				}
			}
			writer.write("]\n}\n");
		}
	}

	private static void writeObj(Path path, ModelDefinition model, ItemDefinition item) throws IOException
	{
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
		{
			writer.write("# Ensouled elf head item " + item.id + ", cache model " + model.id + "\n");
			writer.write("# Do not reorder vertices or triangles when editing for Face Swap.\n");
			writer.write("o EnsouledElfHead_Base\n");
			for (int i = 0; i < model.vertexCount; i++)
			{
				writer.write(String.format("v %d %d %d%n", model.vertexX[i], -model.vertexZ[i], -model.vertexY[i]));
			}
			for (int i = 0; i < model.faceCount; i++)
			{
				writer.write(String.format("f %d %d %d%n", model.faceIndices1[i] + 1,
					model.faceIndices2[i] + 1, model.faceIndices3[i] + 1));
			}
		}
	}
}
