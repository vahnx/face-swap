package com.faceswap;

import java.util.Locale;

/**
 * The persisted assignment boundary for a target. The style ID is intentionally
 * a string so future facial styles can be added without changing the assignment
 * or config schema again. A null render mode means that the assignment should
 * use the current global mode (the legacy/default behavior).
 */
final class FaceSwapAssignment
{
	static final String DEFAULT_STYLE_ID = "default";

	private final FaceSwapHead head;
	private final String styleId;
	private final FaceSwapRenderMode renderMode;
	private final String customImageId;

	FaceSwapAssignment(FaceSwapHead head, String styleId)
	{
		this(head, styleId, null);
	}

	FaceSwapAssignment(FaceSwapHead head, String styleId, FaceSwapRenderMode renderMode)
	{
		this(head, styleId, renderMode, null);
	}

	FaceSwapAssignment(
		FaceSwapHead head, String styleId, FaceSwapRenderMode renderMode, String customImageId)
	{
		this.head = head;
		this.styleId = normalizeStyleId(styleId);
		this.renderMode = renderMode;
		this.customImageId = customImageId;
	}

	static FaceSwapAssignment defaultStyle(FaceSwapHead head)
	{
		return new FaceSwapAssignment(head, DEFAULT_STYLE_ID);
	}

	FaceSwapHead getHead()
	{
		return head;
	}

	String getStyleId()
	{
		return styleId;
	}

	FaceSwapRenderMode getRenderMode()
	{
		return renderMode;
	}

	String getCustomImageId()
	{
		return customImageId;
	}

	static String normalizeStyleId(String styleId)
	{
		if (styleId == null || styleId.trim().isEmpty())
		{
			return DEFAULT_STYLE_ID;
		}
		return styleId.trim().toLowerCase(Locale.ROOT);
	}
}
