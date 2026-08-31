package com.faceswap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.awt.geom.Ellipse2D;
import java.util.EnumMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.awt.geom.Path2D;
import javax.imageio.ImageIO;

final class FaceSwapHeadImages
{
	private static final int IMAGE_SIZE = 42;
	private static final List<String> CREATOR_STYLE_IDS = Collections.unmodifiableList(Arrays.asList(
		FaceSwapAssignment.DEFAULT_STYLE_ID,
		"sad",
		"crying",
		"angry",
		"angel",
		"furious",
		"blushing",
		"sick",
		"in_love"));
	private static final List<String> ALFIE_STYLE_IDS = Collections.unmodifiableList(Arrays.asList(
		FaceSwapAssignment.DEFAULT_STYLE_ID,
		"sad",
		"crying",
		"angry",
		"angel",
		"blushing",
		"sick",
		"in_love"));
	private static final List<String> ODABLOCK_STYLE_IDS = Collections.unmodifiableList(Arrays.asList(
		FaceSwapAssignment.DEFAULT_STYLE_ID,
		"sad",
		"crying",
		"angry",
		"angel",
		"furious",
		"blushing",
		"sick",
		"in_love",
		"gamon",
		"smug",
		"relaxed",
		"sus"));
	private static final List<String> GNOMONKEY_STYLE_IDS = Collections.unmodifiableList(Arrays.asList(
		FaceSwapAssignment.DEFAULT_STYLE_ID,
		"sad",
		"crying",
		"angry",
		"angel",
		"furious",
		"blushing",
		"sick",
		"in_love",
		"7tv"));
	private static final Set<String> PROCEDURAL_STYLE_IDS = Set.of(
		"in_love",
		"angel");
	private static final Map<FaceSwapHead, BufferedImage> CACHE = new EnumMap<>(FaceSwapHead.class);
	private static final Map<FaceSwapHead, Map<String, BufferedImage>> STYLE_CACHE = new EnumMap<>(FaceSwapHead.class);
	private static final Map<FaceSwapHead, Map<FaceSwapHeadDirection, BufferedImage>> DIRECTIONAL_CACHE = new EnumMap<>(FaceSwapHead.class);
	private static final Map<FaceSwapHead, Map<String, Map<FaceSwapHeadDirection, BufferedImage>>> STYLE_DIRECTIONAL_CACHE = new EnumMap<>(FaceSwapHead.class);
	private static final Map<FaceSwapHead, Map<FaceSwapHeadDirection, Color>> AVERAGE_COLOR_CACHE = new EnumMap<>(FaceSwapHead.class);
	private static final Map<FaceSwapHead, Map<String, Map<FaceSwapHeadDirection, Color>>> STYLE_AVERAGE_COLOR_CACHE = new EnumMap<>(FaceSwapHead.class);
	private static final Map<BufferedImage, Map<BufferedImage, Map<FaceSwapHeadDirection, BufferedImage>>> CUSTOM_SIDE_CACHE = new WeakHashMap<>();
	private static final Map<BufferedImage, Color> CUSTOM_AVERAGE_COLOR_CACHE = new WeakHashMap<>();
	private static volatile BufferedImage customImage;
	private static volatile BufferedImage customBackImage;

	private FaceSwapHeadImages()
	{
	}

	static void setCustomImage(BufferedImage image)
	{
		setCustomImages(image, null);
	}

	static void setCustomImages(BufferedImage image, BufferedImage backImage)
	{
		customImage = image;
		customBackImage = backImage;
		CACHE.remove(FaceSwapHead.CUSTOM);
		STYLE_CACHE.remove(FaceSwapHead.CUSTOM);
		DIRECTIONAL_CACHE.remove(FaceSwapHead.CUSTOM);
		STYLE_DIRECTIONAL_CACHE.remove(FaceSwapHead.CUSTOM);
		AVERAGE_COLOR_CACHE.remove(FaceSwapHead.CUSTOM);
		STYLE_AVERAGE_COLOR_CACHE.remove(FaceSwapHead.CUSTOM);
		synchronized (CUSTOM_SIDE_CACHE)
		{
			CUSTOM_SIDE_CACHE.clear();
			CUSTOM_AVERAGE_COLOR_CACHE.clear();
		}
	}

	static BufferedImage getCustomImage(
		BufferedImage front, BufferedImage back, FaceSwapHeadDirection direction)
	{
		if (front == null)
		{
			return null;
		}
		if (direction == FaceSwapHeadDirection.BACK && back != null)
		{
			return back;
		}
		if ((direction == FaceSwapHeadDirection.LEFT || direction == FaceSwapHeadDirection.RIGHT)
			&& back != null)
		{
			synchronized (CUSTOM_SIDE_CACHE)
			{
				Map<BufferedImage, Map<FaceSwapHeadDirection, BufferedImage>> backImages = CUSTOM_SIDE_CACHE.computeIfAbsent(
					front, ignored -> new WeakHashMap<>());
				Map<FaceSwapHeadDirection, BufferedImage> sides = backImages.computeIfAbsent(
					back, ignored -> new EnumMap<>(FaceSwapHeadDirection.class));
				return sides.computeIfAbsent(direction, selectedDirection -> createEstimatedSide(front, back, selectedDirection));
			}
		}
		return front;
	}

	static Color getCustomAverageColor(
		BufferedImage front, BufferedImage back, FaceSwapHeadDirection direction)
	{
		BufferedImage image = getCustomImage(front, back, direction);
		if (image == null)
		{
			return Color.BLACK;
		}
		synchronized (CUSTOM_SIDE_CACHE)
		{
			return CUSTOM_AVERAGE_COLOR_CACHE.computeIfAbsent(image, FaceSwapHeadImages::averageColor);
		}
	}

	static BufferedImage get(FaceSwapHead head)
	{
		return get(head, FaceSwapAssignment.DEFAULT_STYLE_ID);
	}

	static BufferedImage get(FaceSwapHead head, String styleId)
	{
		String normalizedStyleId = FaceSwapAssignment.normalizeStyleId(styleId);
		if (FaceSwapAssignment.DEFAULT_STYLE_ID.equals(normalizedStyleId))
		{
			return CACHE.computeIfAbsent(head, FaceSwapHeadImages::loadOrCreate);
		}
		return STYLE_CACHE
			.computeIfAbsent(head, ignored -> new java.util.HashMap<>())
			.computeIfAbsent(normalizedStyleId, selectedStyle -> loadOrCreate(head, selectedStyle));
	}

	static BufferedImage get(FaceSwapHead head, FaceSwapHeadDirection direction)
	{
		return get(head, FaceSwapAssignment.DEFAULT_STYLE_ID, direction);
	}

	static BufferedImage get(FaceSwapHead head, String styleId, FaceSwapHeadDirection direction)
	{
		String normalizedStyleId = FaceSwapAssignment.normalizeStyleId(styleId);
		if (FaceSwapAssignment.DEFAULT_STYLE_ID.equals(normalizedStyleId))
		{
			return DIRECTIONAL_CACHE
				.computeIfAbsent(head, ignored -> new EnumMap<>(FaceSwapHeadDirection.class))
				.computeIfAbsent(direction, selectedDirection -> loadDirectionalOrFallback(
					head, FaceSwapAssignment.DEFAULT_STYLE_ID, selectedDirection));
		}
		return STYLE_DIRECTIONAL_CACHE
			.computeIfAbsent(head, ignored -> new java.util.HashMap<>())
			.computeIfAbsent(normalizedStyleId, ignored -> new EnumMap<>(FaceSwapHeadDirection.class))
			.computeIfAbsent(direction, selectedDirection -> loadDirectionalOrFallback(
				head, normalizedStyleId, selectedDirection));
	}

	static Color getAverageColor(FaceSwapHead head, FaceSwapHeadDirection direction)
	{
		return getAverageColor(head, FaceSwapAssignment.DEFAULT_STYLE_ID, direction);
	}

	static Color getAverageColor(FaceSwapHead head, String styleId, FaceSwapHeadDirection direction)
	{
		String normalizedStyleId = FaceSwapAssignment.normalizeStyleId(styleId);
		if (FaceSwapAssignment.DEFAULT_STYLE_ID.equals(normalizedStyleId))
		{
			return AVERAGE_COLOR_CACHE
				.computeIfAbsent(head, ignored -> new EnumMap<>(FaceSwapHeadDirection.class))
				.computeIfAbsent(direction, selectedDirection -> averageColor(get(head, selectedDirection)));
		}
		return STYLE_AVERAGE_COLOR_CACHE
			.computeIfAbsent(head, ignored -> new java.util.HashMap<>())
			.computeIfAbsent(normalizedStyleId, ignored -> new EnumMap<>(FaceSwapHeadDirection.class))
			.computeIfAbsent(direction, selectedDirection -> averageColor(get(head, normalizedStyleId, selectedDirection)));
	}

	static List<String> getAvailableStyleIds(FaceSwapHead head)
	{
		if (head == FaceSwapHead.MRNOSLEEP_MASK)
		{
			return Collections.singletonList(FaceSwapAssignment.DEFAULT_STYLE_ID);
		}
		if (head != null && head.isReleaseAvailable()
			&& head.getCategory() == FaceSwapHeadCategory.CONTENT_CREATOR)
		{
			if (head == FaceSwapHead.ODABLOCK)
			{
				return ODABLOCK_STYLE_IDS;
			}
			if (head == FaceSwapHead.ALFIE)
			{
				return ALFIE_STYLE_IDS;
			}
			return head == FaceSwapHead.GNOMONKEY ? GNOMONKEY_STYLE_IDS : CREATOR_STYLE_IDS;
		}
		return Collections.singletonList(FaceSwapAssignment.DEFAULT_STYLE_ID);
	}

	static boolean isStyleAvailable(FaceSwapHead head, String styleId)
	{
		return head != null && getAvailableStyleIds(head)
			.contains(FaceSwapAssignment.normalizeStyleId(styleId));
	}

	static boolean isProceduralStyle(String styleId)
	{
		return PROCEDURAL_STYLE_IDS.contains(FaceSwapAssignment.normalizeStyleId(styleId));
	}

	static String styleDisplayName(String styleId)
	{
		String normalizedStyleId = FaceSwapAssignment.normalizeStyleId(styleId);
		if (FaceSwapAssignment.DEFAULT_STYLE_ID.equals(normalizedStyleId))
		{
			return "Standard";
		}
		if ("sad".equals(normalizedStyleId))
		{
			return "Sad";
		}
		if ("crying".equals(normalizedStyleId))
		{
			return "Crying";
		}
		if ("angry".equals(normalizedStyleId))
		{
			return "Angry";
		}
		if ("furious".equals(normalizedStyleId))
		{
			return "Furious";
		}
		if ("blushing".equals(normalizedStyleId))
		{
			return "Blushing";
		}
		if ("sick".equals(normalizedStyleId))
		{
			return "Sick";
		}
		if ("in_love".equals(normalizedStyleId))
		{
			return "In Love";
		}
		if ("angel".equals(normalizedStyleId))
		{
			return "Angel";
		}
		if ("gamon".equals(normalizedStyleId))
		{
			return "Gamon";
		}
		if ("smug".equals(normalizedStyleId))
		{
			return "Smug";
		}
		if ("relaxed".equals(normalizedStyleId))
		{
			return "Relaxed";
		}
		if ("sus".equals(normalizedStyleId))
		{
			return "Sus";
		}
		if ("7tv".equals(normalizedStyleId))
		{
			return "Stretched";
		}
		return normalizedStyleId;
	}

	private static BufferedImage loadOrCreate(FaceSwapHead head)
	{
		return loadOrCreate(head, FaceSwapAssignment.DEFAULT_STYLE_ID);
	}

	private static BufferedImage loadOrCreate(FaceSwapHead head, String styleId)
	{
		if (head == FaceSwapHead.CUSTOM && customImage != null)
		{
			return customImage;
		}
		if (isProceduralStyle(styleId))
		{
			return createProceduralStyle(head, styleId, FaceSwapHeadDirection.FRONT);
		}
		BufferedImage image;
		if (FaceSwapAssignment.DEFAULT_STYLE_ID.equals(styleId))
		{
			image = loadResource(head);
		}
		else
		{
			image = loadResource(head, getResourceName(head) + "_" + styleId);
			if (image == null)
			{
				image = loadResource(head, getResourceName(head) + "_" + styleId + "_front");
			}
		}
		if (image != null)
		{
			return image;
		}
		return FaceSwapAssignment.DEFAULT_STYLE_ID.equals(styleId)
			? createPlaceholder(head)
			: get(head, FaceSwapAssignment.DEFAULT_STYLE_ID);
	}

	private static BufferedImage loadDirectionalOrFallback(
		FaceSwapHead head, String styleId, FaceSwapHeadDirection direction)
	{
		if (head == FaceSwapHead.CUSTOM && customImage != null)
		{
			if (direction == FaceSwapHeadDirection.BACK && customBackImage != null)
			{
				return customBackImage;
			}
			if ((direction == FaceSwapHeadDirection.LEFT || direction == FaceSwapHeadDirection.RIGHT)
				&& customBackImage != null)
			{
				return createEstimatedSide(customImage, customBackImage, direction);
			}
			return customImage;
		}
		if (isProceduralStyle(styleId))
		{
			return createProceduralStyle(head, styleId, direction);
		}
		String resourceName = FaceSwapAssignment.DEFAULT_STYLE_ID.equals(styleId)
			? getResourceName(head)
			: getResourceName(head) + "_" + styleId;
		BufferedImage image = loadResource(head, resourceName + "_" + direction.getFileSuffix());
		if (image != null)
		{
			return image;
		}
		if (direction != FaceSwapHeadDirection.LEFT && direction != FaceSwapHeadDirection.RIGHT)
		{
			return FaceSwapAssignment.DEFAULT_STYLE_ID.equals(styleId)
				? get(head)
				: get(head, FaceSwapAssignment.DEFAULT_STYLE_ID, direction);
		}

		BufferedImage front = loadResource(head, resourceName + "_front");
		BufferedImage back = loadResource(head, resourceName + "_back");
		if (front == null)
		{
			front = loadResource(head, getResourceName(head) + "_front");
		}
		if (back == null)
		{
			back = loadResource(head, getResourceName(head) + "_back");
		}
		if (front != null && back != null)
		{
			return createEstimatedSide(front, back, direction);
		}
		if (front != null || back != null)
		{
			return front != null ? front : back;
		}
		// Base-only assets, such as emojis, use the same image for every view.
		return get(head, styleId);
	}

	private static BufferedImage createProceduralStyle(
		FaceSwapHead head, String styleId, FaceSwapHeadDirection direction)
	{
		BufferedImage base = get(head, FaceSwapAssignment.DEFAULT_STYLE_ID, direction);
		BufferedImage styled = copyImage(base);
		Rectangle bounds = opaqueBounds(base);
		if (bounds.width == 0 || bounds.height == 0)
		{
			return styled;
		}
		if ("angel".equals(styleId))
		{
			return createAngelStyle(base, bounds);
		}

		switch (styleId)
		{
			case "sick":
				applySickTint(styled);
				drawTearDrop(styled, bounds, 0.31f, 0.32f, 0.065f);
				drawTearDrop(styled, bounds, 0.69f, 0.36f, 0.055f);
				break;
			case "blushing":
				applyBlush(styled, bounds);
				break;
			case "in_love":
				drawLoveHearts(styled, bounds);
				break;
			case "furious":
				applyFuriousTint(styled);
				drawFuriousDetails(styled, bounds);
				break;
			default:
				break;
		}
		return styled;
	}

	private static BufferedImage createAngelStyle(BufferedImage base, Rectangle bounds)
	{
		BufferedImage angel = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = angel.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			int topMargin = Math.max(48, base.getHeight() / 8);
			int bottomMargin = Math.max(8, base.getHeight() / 32);
			int targetHeight = Math.min(bounds.height, base.getHeight() - topMargin - bottomMargin);
			int targetWidth = Math.round(bounds.width * (targetHeight / (float) bounds.height));
			int targetX = (base.getWidth() - targetWidth) / 2;
			int targetY = base.getHeight() - bottomMargin - targetHeight;
			graphics.drawImage(base, targetX, targetY, targetX + targetWidth, targetY + targetHeight,
				bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, null);
			drawAngelHalo(graphics, base.getWidth() / 2f, topMargin * 0.48f, targetWidth);
		}
		finally
		{
			graphics.dispose();
		}
		return angel;
	}

	private static void drawAngelHalo(Graphics2D graphics, float centerX, float centerY, int headWidth)
	{
		float haloWidth = Math.max(34f, headWidth * 0.36f);
		float haloHeight = Math.max(10f, haloWidth * 0.28f);
		Ellipse2D halo = new Ellipse2D.Float(
			centerX - haloWidth / 2f,
			centerY - haloHeight / 2f,
			haloWidth,
			haloHeight);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(255, 214, 92, 70));
		graphics.setStroke(new BasicStroke(Math.max(5f, haloHeight * 0.55f)));
		graphics.draw(halo);
		graphics.setColor(new Color(255, 226, 125, 245));
		graphics.setStroke(new BasicStroke(Math.max(2f, haloHeight * 0.22f)));
		graphics.draw(halo);
	}

	private static BufferedImage copyImage(BufferedImage source)
	{
		BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = copy.createGraphics();
		try
		{
			graphics.drawImage(source, 0, 0, null);
		}
		finally
		{
			graphics.dispose();
		}
		return copy;
	}

	private static Rectangle opaqueBounds(BufferedImage image)
	{
		int minX = image.getWidth();
		int minY = image.getHeight();
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if ((image.getRGB(x, y) >>> 24) == 0)
				{
					continue;
				}
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
			}
		}
		return maxX < minX ? new Rectangle() : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}

	private static void applySickTint(BufferedImage image)
	{
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int argb = image.getRGB(x, y);
				if ((argb >>> 24) == 0)
				{
					continue;
				}
				int red = (argb >> 16) & 0xFF;
				int green = (argb >> 8) & 0xFF;
				int blue = argb & 0xFF;
				boolean warmPixel = red > green + 6 && red > blue + 6;
				float amount = warmPixel ? 0.68f : 0.20f;
				image.setRGB(x, y, withRgb(argb,
					blend(red, 105, amount),
					blend(green, 165, amount),
					blend(blue, 75, amount)));
			}
		}
	}

	private static void applyFuriousTint(BufferedImage image)
	{
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int argb = image.getRGB(x, y);
				if ((argb >>> 24) == 0)
				{
					continue;
				}
				int red = (argb >> 16) & 0xFF;
				int green = (argb >> 8) & 0xFF;
				int blue = argb & 0xFF;
				image.setRGB(x, y, withRgb(argb,
					blend(red, 185, 0.22f),
					blend(green, 55, 0.22f),
					blend(blue, 45, 0.22f)));
			}
		}
	}

	private static void applyBlush(BufferedImage image, Rectangle bounds)
	{
		applyEllipseTint(image, bounds, 0.30f, 0.64f, 0.17f, 0.10f, new Color(232, 75, 105), 0.72f);
		applyEllipseTint(image, bounds, 0.70f, 0.64f, 0.17f, 0.10f, new Color(232, 75, 105), 0.72f);
	}

	private static void applyEllipseTint(
		BufferedImage image, Rectangle bounds, float centerX, float centerY,
		float radiusX, float radiusY, Color color, float strength)
	{
		float actualCenterX = bounds.x + bounds.width * centerX;
		float actualCenterY = bounds.y + bounds.height * centerY;
		float actualRadiusX = bounds.width * radiusX;
		float actualRadiusY = bounds.height * radiusY;
		for (int y = Math.max(0, bounds.y); y < Math.min(image.getHeight(), bounds.y + bounds.height); y++)
		{
			for (int x = Math.max(0, bounds.x); x < Math.min(image.getWidth(), bounds.x + bounds.width); x++)
			{
				float dx = (x - actualCenterX) / actualRadiusX;
				float dy = (y - actualCenterY) / actualRadiusY;
				float distance = dx * dx + dy * dy;
				if (distance >= 1f)
				{
					continue;
				}
				int argb = image.getRGB(x, y);
				if ((argb >>> 24) == 0)
				{
					continue;
				}
				float amount = (1f - distance) * strength;
				image.setRGB(x, y, withRgb(argb,
					blend((argb >> 16) & 0xFF, color.getRed(), amount),
					blend((argb >> 8) & 0xFF, color.getGreen(), amount),
					blend(argb & 0xFF, color.getBlue(), amount)));
			}
		}
	}

	private static void drawLoveHearts(BufferedImage image, Rectangle bounds)
	{
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(new Color(235, 70, 125, 235));
			drawHeart(graphics, bounds.x + bounds.width * 0.16f, bounds.y + bounds.height * 0.23f, bounds.width * 0.075f);
			drawHeart(graphics, bounds.x + bounds.width * 0.84f, bounds.y + bounds.height * 0.28f, bounds.width * 0.060f);
			drawHeart(graphics, bounds.x + bounds.width * 0.80f, bounds.y + bounds.height * 0.78f, bounds.width * 0.045f);
		}
		finally
		{
			graphics.dispose();
		}
	}

	private static void drawHeart(Graphics2D graphics, float centerX, float centerY, float size)
	{
		Path2D heart = new Path2D.Float();
		heart.moveTo(centerX, centerY + size);
		heart.curveTo(centerX - size * 1.35f, centerY, centerX - size, centerY - size, centerX - size * 0.45f, centerY - size * 0.72f);
		heart.curveTo(centerX, centerY - size * 1.18f, centerX, centerY - size * 1.18f, centerX, centerY - size * 0.45f);
		heart.curveTo(centerX, centerY - size * 1.18f, centerX, centerY - size * 1.18f, centerX + size * 0.45f, centerY - size * 0.72f);
		heart.curveTo(centerX + size, centerY - size, centerX + size * 1.35f, centerY, centerX, centerY + size);
		heart.closePath();
		graphics.fill(heart);
	}

	private static void drawTearDrop(BufferedImage image, Rectangle bounds, float x, float y, float size)
	{
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(new Color(80, 185, 235, 225));
			float centerX = bounds.x + bounds.width * x;
			float centerY = bounds.y + bounds.height * y;
			float radius = bounds.width * size;
			Path2D drop = new Path2D.Float();
			drop.moveTo(centerX, centerY - radius);
			drop.curveTo(centerX - radius, centerY, centerX - radius, centerY + radius, centerX, centerY + radius);
			drop.curveTo(centerX + radius, centerY + radius, centerX + radius, centerY, centerX, centerY - radius);
			drop.closePath();
			graphics.fill(drop);
		}
		finally
		{
			graphics.dispose();
		}
	}

	private static void drawFuriousDetails(BufferedImage image, Rectangle bounds)
	{
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(new Color(95, 20, 25, 245));
			drawHorn(graphics, bounds.x + bounds.width * 0.23f, bounds.y + bounds.height * 0.14f, bounds.width * 0.13f, false);
			drawHorn(graphics, bounds.x + bounds.width * 0.77f, bounds.y + bounds.height * 0.14f, bounds.width * 0.13f, true);
			graphics.setColor(new Color(245, 80, 25, 235));
			drawFlame(graphics, bounds.x + bounds.width * 0.08f, bounds.y + bounds.height * 0.52f, bounds.width * 0.10f);
			drawFlame(graphics, bounds.x + bounds.width * 0.92f, bounds.y + bounds.height * 0.58f, bounds.width * 0.09f);
		}
		finally
		{
			graphics.dispose();
		}
	}

	private static void drawHorn(Graphics2D graphics, float baseX, float baseY, float size, boolean mirror)
	{
		float direction = mirror ? 1f : -1f;
		Path2D horn = new Path2D.Float();
		horn.moveTo(baseX, baseY + size * 0.8f);
		horn.curveTo(baseX + direction * size * 0.9f, baseY + size * 0.5f,
			baseX + direction * size * 0.9f, baseY - size * 0.6f,
			baseX + direction * size * 0.35f, baseY - size);
		horn.curveTo(baseX + direction * size * 0.45f, baseY - size * 0.20f,
			baseX + direction * size * 0.25f, baseY + size * 0.30f, baseX, baseY + size * 0.8f);
		horn.closePath();
		graphics.fill(horn);
	}

	private static void drawFlame(Graphics2D graphics, float centerX, float centerY, float size)
	{
		Path2D flame = new Path2D.Float();
		flame.moveTo(centerX, centerY + size);
		flame.curveTo(centerX - size, centerY + size * 0.45f, centerX - size * 0.75f, centerY - size * 0.45f, centerX, centerY - size);
		flame.curveTo(centerX - size * 0.15f, centerY - size * 0.15f, centerX + size * 0.65f, centerY - size * 0.40f, centerX, centerY + size);
		flame.closePath();
		graphics.fill(flame);
	}

	private static int withRgb(int argb, int red, int green, int blue)
	{
		return (argb & 0xFF000000) | (red << 16) | (green << 8) | blue;
	}

	private static int blend(int from, int to, float amount)
	{
		return Math.max(0, Math.min(255, Math.round(from + (to - from) * amount)));
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
			case ODABLOCK:
				return new Color(240, 170, 112);
			case KING_CONDOR:
				return new Color(205, 160, 120);
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
