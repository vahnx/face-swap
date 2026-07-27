package com.faceswap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

final class FaceSwapHeadImages
{
	private static final int IMAGE_SIZE = 42;
	private static final Map<FaceSwapHead, BufferedImage> CACHE = new EnumMap<>(FaceSwapHead.class);
	private static final Map<FaceSwapHead, Map<FaceSwapHeadDirection, BufferedImage>> DIRECTIONAL_CACHE = new EnumMap<>(FaceSwapHead.class);
	private static final Map<FaceSwapHead, Map<FaceSwapHeadDirection, Color>> AVERAGE_COLOR_CACHE = new EnumMap<>(FaceSwapHead.class);
	private static volatile BufferedImage customImage;

	private FaceSwapHeadImages()
	{
	}

	static void setCustomImage(BufferedImage image)
	{
		customImage = image;
		CACHE.remove(FaceSwapHead.CUSTOM);
		DIRECTIONAL_CACHE.remove(FaceSwapHead.CUSTOM);
		AVERAGE_COLOR_CACHE.remove(FaceSwapHead.CUSTOM);
	}

	static BufferedImage get(FaceSwapHead head)
	{
		return CACHE.computeIfAbsent(head, FaceSwapHeadImages::loadOrCreate);
	}

	static BufferedImage get(FaceSwapHead head, FaceSwapHeadDirection direction)
	{
		return DIRECTIONAL_CACHE
			.computeIfAbsent(head, ignored -> new EnumMap<>(FaceSwapHeadDirection.class))
			.computeIfAbsent(direction, selectedDirection -> loadDirectionalOrFallback(head, selectedDirection));
	}

	static Color getAverageColor(FaceSwapHead head, FaceSwapHeadDirection direction)
	{
		return AVERAGE_COLOR_CACHE
			.computeIfAbsent(head, ignored -> new EnumMap<>(FaceSwapHeadDirection.class))
			.computeIfAbsent(direction, selectedDirection -> averageColor(get(head, selectedDirection)));
	}

	private static BufferedImage loadOrCreate(FaceSwapHead head)
	{
		if (head == FaceSwapHead.CUSTOM && customImage != null)
		{
			return customImage;
		}
		BufferedImage image = loadResource(head);
		return image == null ? createPlaceholder(head) : image;
	}

	private static BufferedImage loadDirectionalOrFallback(FaceSwapHead head, FaceSwapHeadDirection direction)
	{
		if (head == FaceSwapHead.CUSTOM && customImage != null)
		{
			return customImage;
		}
		BufferedImage image = loadResource(
			head, getResourceName(head) + "_" + direction.getFileSuffix());
		if (image != null)
		{
			return image;
		}

		if (direction == FaceSwapHeadDirection.LEFT || direction == FaceSwapHeadDirection.RIGHT)
		{
			BufferedImage front = loadResource(head, getResourceName(head) + "_front");
			BufferedImage back = loadResource(head, getResourceName(head) + "_back");
			if (front != null && back != null)
			{
				return createEstimatedSide(front, back, direction);
			}
			return front != null ? front : back;
		}

		return get(head);
	}

	private static BufferedImage createEstimatedSide(
		BufferedImage front,
		BufferedImage back,
		FaceSwapHeadDirection direction)
	{
		int width = Math.max(front.getWidth(), back.getWidth());
		int height = Math.max(front.getHeight(), back.getHeight());
		BufferedImage estimated = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		float frontX = direction == FaceSwapHeadDirection.LEFT ? 0.25f : 0.75f;
		float backX = 1f - frontX;
		for (int y = 0; y < height; y++)
		{
			float vertical = height == 1 ? 0f : y / (float) (height - 1);
			for (int x = 0; x < width; x++)
			{
				float depth = width == 1 ? 0f : x / (float) (width - 1);
				float backWeight = depth * depth * (3f - 2f * depth);
				int frontArgb = sampleNormalized(front, frontX, vertical);
				int backArgb = sampleNormalized(back, backX, vertical);
				estimated.setRGB(x, y, blendOpaquePixels(frontArgb, backArgb, backWeight));
			}
		}
		return estimated;
	}

	private static int sampleNormalized(BufferedImage image, float x, float y)
	{
		int pixelX = Math.max(0, Math.min(image.getWidth() - 1, Math.round(x * (image.getWidth() - 1))));
		int pixelY = Math.max(0, Math.min(image.getHeight() - 1, Math.round(y * (image.getHeight() - 1))));
		return image.getRGB(pixelX, pixelY);
	}

	private static int blendOpaquePixels(int frontArgb, int backArgb, float backWeight)
	{
		boolean hasFront = (frontArgb >>> 24) != 0;
		boolean hasBack = (backArgb >>> 24) != 0;
		if (!hasFront && !hasBack)
		{
			return 0;
		}
		if (!hasFront)
		{
			return 0xFF000000 | (backArgb & 0xFFFFFF);
		}
		if (!hasBack)
		{
			return 0xFF000000 | (frontArgb & 0xFFFFFF);
		}

		float frontWeight = 1f - backWeight;
		int red = Math.round(((frontArgb >>> 16) & 0xFF) * frontWeight
			+ ((backArgb >>> 16) & 0xFF) * backWeight);
		int green = Math.round(((frontArgb >>> 8) & 0xFF) * frontWeight
			+ ((backArgb >>> 8) & 0xFF) * backWeight);
		int blue = Math.round((frontArgb & 0xFF) * frontWeight
			+ (backArgb & 0xFF) * backWeight);
		return 0xFF000000 | (red << 16) | (green << 8) | blue;
	}

	private static BufferedImage loadResource(FaceSwapHead head)
	{
		return loadResource(head, getResourceName(head));
	}

	private static BufferedImage loadResource(FaceSwapHead head, String resourceName)
	{
		String resourcePath = "/heads/" + head.getCategory().getResourceDirectory()
			+ "/" + resourceName + ".png";
		try (InputStream inputStream = FaceSwapHeadImages.class.getResourceAsStream(resourcePath))
		{
			return inputStream == null ? null : makeOpaque(ImageIO.read(inputStream));
		}
		catch (IOException ex)
		{
			return null;
		}
	}

	private static BufferedImage makeOpaque(BufferedImage source)
	{
		if (source == null)
		{
			return null;
		}

		BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				int argb = source.getRGB(x, y);
				int alpha = argb >>> 24;
				if (alpha > 24)
				{
					image.setRGB(x, y, 0xFF000000 | (argb & 0x00FFFFFF));
				}
			}
		}
		return image;
	}

	private static Color averageColor(BufferedImage image)
	{
		long red = 0;
		long green = 0;
		long blue = 0;
		long count = 0;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int argb = image.getRGB(x, y);
				if ((argb >>> 24) == 0)
				{
					continue;
				}

				red += (argb >> 16) & 0xFF;
				green += (argb >> 8) & 0xFF;
				blue += argb & 0xFF;
				count++;
			}
		}

		if (count == 0)
		{
			return new Color(220, 165, 150);
		}
		return new Color((int) (red / count), (int) (green / count), (int) (blue / count));
	}

	private static String getResourceName(FaceSwapHead head)
	{
		return head.name().toLowerCase(Locale.ROOT);
	}

	private static BufferedImage createPlaceholder(FaceSwapHead head)
	{
		BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(colorFor(head));
			graphics.fillOval(4, 3, 34, 36);
			graphics.setColor(new Color(45, 28, 18, 230));
			graphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.drawArc(8, 6, 26, 16, 20, 140);
			graphics.setColor(Color.BLACK);
			graphics.fillOval(14, 18, 4, 4);
			graphics.fillOval(25, 18, 4, 4);
			graphics.drawArc(15, 24, 13, 6, 190, 160);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	private static Color colorFor(FaceSwapHead head)
	{
		switch (head)
		{
			case KING_CONDOR:
				return new Color(205, 160, 120);
			case ODABLOCK:
				return new Color(240, 170, 112);
			case ALFIE:
				return new Color(215, 185, 135);
			case PURESPAM:
				return new Color(232, 152, 118);
			case TORVESTA:
				return new Color(225, 178, 128);
			default:
				return new Color(238, 190, 145);
		}
	}
}
