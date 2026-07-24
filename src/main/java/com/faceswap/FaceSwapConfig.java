package com.faceswap;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("faceswap")
public interface FaceSwapConfig extends Config
{
	@ConfigSection(
		name = "Wraparound",
		description = "Projected head wrap calibration and debug settings",
		position = 1,
		closedByDefault = true
	)
	String WRAPAROUND_SECTION = "wraparound";

	@ConfigSection(
		name = "Mask",
		description = "Front-worn mask calibration settings",
		position = 3,
		closedByDefault = true
	)
	String FACE_MASK_SECTION = "faceMask";

	@ConfigSection(
		name = "Wraparound Helmets",
		description = "Open-face helmet calibration for Wraparound mode",
		position = 2,
		closedByDefault = true
	)
	String PARTIAL_HELMET_SECTION = "partialHelmet";

	@ConfigSection(
		name = "3D Mode",
		description = "3D model, helmet profile, and animation diagnostics",
		position = 0
	)
	String PROTOTYPE_3D_SECTION = "prototype3d";

	@ConfigItem(
		keyName = "selectedHead",
		name = "Selected Head",
		description = "The creator head to apply",
		hidden = true
	)
	default FaceSwapHead selectedHead()
	{
		return FaceSwapHead.SARDACO;
	}

	@ConfigItem(
		keyName = "targetScope",
		name = "Apply To",
		description = "Which players should receive the selected head",
		hidden = true
	)
	default FaceSwapTargetScope targetScope()
	{
		return FaceSwapTargetScope.SELF;
	}

	@ConfigItem(
		keyName = "targetNames",
		name = "Player Names",
		description = "Comma-separated player names used by the Specific Players scope",
		hidden = true
	)
	default String targetNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "targetRadius",
		name = "Player Radius",
		description = "Tile radius used by Everyone in Radius",
		hidden = true
	)
	default int targetRadius()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "npcTargetScope",
		name = "Apply To NPCs",
		description = "Which NPCs should receive the selected head",
		hidden = true
	)
	default FaceSwapNpcTargetScope npcTargetScope()
	{
		return FaceSwapNpcTargetScope.DISABLED;
	}

	@ConfigItem(
		keyName = "npcTargetNames",
		name = "NPC Names",
		description = "NPC names used by the Specific NPCs scope",
		hidden = true
	)
	default String npcTargetNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "renderMode",
		name = "Style",
		description = "Renders a rigged 3D head, a projected wraparound, or a front face mask with straps.",
		position = -100,
		hidden = true
	)
	default FaceSwapRenderMode renderMode()
	{
		return FaceSwapRenderMode.THREE_D;
	}

	@ConfigItem(
		keyName = "dkMode",
		name = "DK Mode",
		description = "Uses an oversized 3D head with scale 200 and width 130.",
		position = -99,
		hidden = true
	)
	default boolean dkMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "debugProjection",
		name = "Projection Debug",
		description = "Shows model projection points and bounds for troubleshooting wraparound rendering",
		position = 0,
		section = WRAPAROUND_SECTION
	)
	default boolean debugProjection()
	{
		return false;
	}

	@ConfigItem(
		keyName = "opaqueBacking",
		name = "Opaque Backing",
		description = "Fills wraparound head triangles behind PNG assets to prevent the player model showing through",
		position = 1,
		section = WRAPAROUND_SECTION
	)
	default boolean opaqueBacking()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideWithHeadgear",
		name = "Hide With Headgear",
		description = "Skips rendering when the player has head or jaw-slot equipment. Face-covering helmets are always hidden.",
		position = 2,
		section = WRAPAROUND_SECTION
	)
	default boolean hideWithHeadgear()
	{
		return false;
	}

	@ConfigItem(
		keyName = "debugEquipment",
		name = "Equipment Debug",
		description = "Shows head, hair, and jaw equipment ids used for headgear detection.",
		position = 4,
		section = WRAPAROUND_SECTION
	)
	default boolean debugEquipment()
	{
		return false;
	}

	@ConfigItem(
		keyName = "helmetFaceDrop",
		name = "Helmet Face Drop",
		description = "Fallback model-space downward shift used with open-face headgear when Preserve Helmet is off.",
		position = 3,
		section = WRAPAROUND_SECTION
	)
	@Range(
		min = 0,
		max = 90
	)
	default int helmetFaceDrop()
	{
		return 34;
	}

	@ConfigItem(
		keyName = "wrapHeightOffset",
		name = "Wrap Y",
		description = "Model-space vertical position for wraparound rendering. Negative values move the mapped head higher.",
		position = 5,
		section = WRAPAROUND_SECTION
	)
	@Range(
		min = -40,
		max = 40
	)
	default int wrapHeightOffset()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "wrapRegionHeight",
		name = "Wrap Region Height",
		description = "Model-space height of the head region used for wraparound rendering. Lower values avoid neck/body triangles.",
		position = 6,
		section = WRAPAROUND_SECTION
	)
	@Range(
		min = 20,
		max = 70
	)
	default int wrapRegionHeight()
	{
		return 27;
	}

	@ConfigItem(
		keyName = "wrapScreenLift",
		name = "Polygon Lift",
		description = "Reference upward adjustment for the entire wrap polygon. It scales down as the projected player gets smaller.",
		position = 7,
		section = WRAPAROUND_SECTION
	)
	@Range(
		min = 0,
		max = 120
	)
	default int wrapScreenLift()
	{
		return 6;
	}

	@ConfigItem(
		keyName = "wrapTextureLift",
		name = "Texture Lift",
		description = "Moves the PNG artwork upward over the selected wrap geometry without changing the model points.",
		position = 8,
		section = WRAPAROUND_SECTION
	)
	@Range(
		min = -40,
		max = 40
	)
	default int wrapTextureLift()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "wrapTextureXOffset",
		name = "Texture X",
		description = "Horizontal texture sampling adjustment for the PNG artwork inside the wrap polygon.",
		position = 9,
		section = WRAPAROUND_SECTION
	)
	@Range(
		min = -40,
		max = 40
	)
	default int wrapTextureXOffset()
	{
		return 27;
	}

	@ConfigItem(
		keyName = "wrapTextureHeightScale",
		name = "Texture Height",
		description = "Vertical scale for the PNG artwork inside the wrap polygon. Lower values squish the face vertically.",
		position = 10,
		section = WRAPAROUND_SECTION
	)
	@Range(
		min = 50,
		max = 150
	)
	default int wrapTextureHeightScale()
	{
		return 95;
	}

	@ConfigItem(
		keyName = "wrapTextureTopBias",
		name = "Texture Top",
		description = "Top-half texture adjustment. Negative values pull more hair/top of the PNG into the wrap.",
		position = 11,
		section = WRAPAROUND_SECTION
	)
	@Range(
		min = -40,
		max = 40
	)
	default int wrapTextureTopBias()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "wrapBackingExpansion",
		name = "Backing Expansion",
		description = "Extra screen-space padding for the opaque wraparound backing. Increase this if the original face still shows through.",
		position = 12,
		section = WRAPAROUND_SECTION
	)
	@Range(
		min = 0,
		max = 40
	)
	default int wrapBackingExpansion()
	{
		return 12;
	}

	@ConfigItem(
		keyName = "partialHelmetFaceWindow",
		name = "Preserve Helmet",
		description = "Restores helmet geometry over the wrapped face so the face remains on the player and below open-face headgear.",
		position = 0,
		section = PARTIAL_HELMET_SECTION
	)
	default boolean helmetOcclusion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "partialHelmetRegionHeight",
		name = "Helmet Region Height",
		description = "Model-space height of the exposed face band when rendering through open-face helmets.",
		position = 1,
		section = PARTIAL_HELMET_SECTION
	)
	@Range(
		min = 12,
		max = 60
	)
	default int partialHelmetRegionHeight()
	{
		return 34;
	}

	@ConfigItem(
		keyName = "partialHelmetWidth",
		name = "Helmet Width",
		description = "Width of the inner face shell. Helmet triangles outside this boundary are preserved over the face.",
		position = 2,
		section = PARTIAL_HELMET_SECTION
	)
	@Range(
		min = 30,
		max = 120
	)
	default int partialHelmetWidth()
	{
		return 82;
	}

	@ConfigItem(
		keyName = "partialHelmetDepth",
		name = "Helmet Depth",
		description = "Depth of the inner face shell. Helmet triangles outside this boundary are preserved over the face.",
		position = 3,
		section = PARTIAL_HELMET_SECTION
	)
	@Range(
		min = 20,
		max = 120
	)
	default int partialHelmetDepth()
	{
		return 85;
	}

	@ConfigItem(
		keyName = "partialHelmetFrontDepth",
		name = "Helmet Front Depth",
		description = "Front-side depth of the inner face shell. Lower values preserve more front helmet geometry over the face.",
		position = 4,
		section = PARTIAL_HELMET_SECTION
	)
	@Range(
		min = 20,
		max = 120
	)
	default int partialHelmetFrontDepth()
	{
		return 90;
	}

	@ConfigItem(
		keyName = "partialHelmetTopPreserve",
		name = "Helmet Top Preserve",
		description = "Model-space height from the top of the helmet band that is always preserved over the face.",
		position = 5,
		section = PARTIAL_HELMET_SECTION
	)
	@Range(
		min = 0,
		max = 40
	)
	default int partialHelmetTopPreserve()
	{
		return 8;
	}

	@ConfigItem(
		keyName = "partialHelmetTextureTop",
		name = "Helmet Texture Top",
		description = "Top texture crop percentage for open-face helmet rendering.",
		position = 6,
		section = PARTIAL_HELMET_SECTION
	)
	@Range(
		min = 0,
		max = 70
	)
	default int partialHelmetTextureTop()
	{
		return 18;
	}

	@ConfigItem(
		keyName = "partialHelmetTextureBottom",
		name = "Helmet Texture Bottom",
		description = "Bottom texture crop percentage for open-face helmet rendering.",
		position = 7,
		section = PARTIAL_HELMET_SECTION
	)
	@Range(
		min = 40,
		max = 120
	)
	default int partialHelmetTextureBottom()
	{
		return 98;
	}

	@ConfigItem(
		keyName = "partialHelmetTextureLift",
		name = "Helmet Texture Lift",
		description = "Moves only the open-helmet PNG artwork. Negative values move facial features downward without affecting bare heads.",
		position = 8,
		section = PARTIAL_HELMET_SECTION
	)
	@Range(
		min = -80,
		max = 40
	)
	default int partialHelmetTextureLift()
	{
		return -40;
	}

	@ConfigItem(
		keyName = "partialHelmetClipTop",
		name = "Helmet Clip Top",
		description = "Top inset of the model-space band where helmet geometry is preserved. Negative values extend it upward.",
		position = 9,
		section = PARTIAL_HELMET_SECTION
	)
	@Range(
		min = -60,
		max = 30
	)
	default int partialHelmetClipTop()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "partialHelmetClipBottom",
		name = "Helmet Clip Bottom",
		description = "Bottom inset of the model-space band where helmet geometry is preserved. Negative values extend it downward.",
		position = 10,
		section = PARTIAL_HELMET_SECTION
	)
	@Range(
		min = -30,
		max = 30
	)
	default int partialHelmetClipBottom()
	{
		return -2;
	}

	@ConfigItem(
		keyName = "partialHelmetBackingExpansion",
		name = "Helmet Backing",
		description = "Backing expansion for open-face helmet rendering. Keep low to avoid painting onto helmet geometry.",
		position = 11,
		section = PARTIAL_HELMET_SECTION
	)
	@Range(
		min = 0,
		max = 20
	)
	default int partialHelmetBackingExpansion()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "overlaySize",
		name = "Mask Size",
		description = "Size of the projected mask panel relative to the default value of 32",
		position = 0,
		section = FACE_MASK_SECTION
	)
	@Range(
		min = 8,
		max = 128
	)
	default int overlaySize()
	{
		return 32;
	}

	@ConfigItem(
		keyName = "maskWidth",
		name = "Mask Width",
		description = "Horizontal size of the projected mask as a percentage of the detected face width.",
		position = 1,
		section = FACE_MASK_SECTION
	)
	@Range(
		min = 50,
		max = 120
	)
	default int maskWidth()
	{
		return 70;
	}

	@ConfigItem(
		keyName = "heightOffset",
		name = "Mask Height",
		description = "Model-space mask height above the player origin. Lower values move the mask down.",
		position = 2,
		section = FACE_MASK_SECTION
	)
	@Range(
		min = 0,
		max = 300
	)
	default int heightOffset()
	{
		return 190;
	}

	@ConfigItem(
		keyName = "maskForwardOffset",
		name = "Mask Depth",
		description = "Distance outside the detected face surface. Positive values move the mask outward.",
		position = 3,
		section = FACE_MASK_SECTION
	)
	@Range(
		min = -24,
		max = 24
	)
	default int maskForwardOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "maskBacking",
		name = "Mask Backing",
		description = "Rear strap anchor as a percentage of detected head depth. Lower values close the strap nearer the head center.",
		position = 4,
		section = FACE_MASK_SECTION
	)
	@Range(
		min = 20,
		max = 100
	)
	default int maskBacking()
	{
		return 28;
	}

	@ConfigItem(
		keyName = "xOffset",
		name = "Mask X",
		description = "Horizontal mask adjustment in screen pixels after projection",
		position = 5,
		section = FACE_MASK_SECTION
	)
	@Range(
		min = -100,
		max = 100
	)
	default int xOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "yOffset",
		name = "Mask Y",
		description = "Vertical mask adjustment in screen pixels. Positive values move the mask down.",
		position = 6,
		section = FACE_MASK_SECTION
	)
	@Range(
		min = -100,
		max = 100
	)
	default int yOffset()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "maskAngle",
		name = "Mask Pitch",
		description = "Additional rotation around the local X axis. Positive values tilt the mask downward.",
		position = 7,
		section = FACE_MASK_SECTION
	)
	@Range(
		min = -45,
		max = 45
	)
	default int maskAngle()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "maskYaw",
		name = "Mask Yaw",
		description = "Additional rotation around the local Y axis. Use this to turn the mask left or right.",
		position = 8,
		section = FACE_MASK_SECTION
	)
	@Range(
		min = -45,
		max = 45
	)
	default int maskYaw()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "maskRoll",
		name = "Mask Roll",
		description = "Additional rotation around the local Z axis. Use this to spin the mask clockwise or counterclockwise.",
		position = 9,
		section = FACE_MASK_SECTION
	)
	@Range(
		min = -45,
		max = 45
	)
	default int maskRoll()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "prototype3dEnabled",
		name = "Enable 3D Prototype",
		description = "Replaces the PNG overlay with a cache-backed 3D test model attached to your player.",
		position = 0,
		hidden = true,
		section = PROTOTYPE_3D_SECTION
	)
	default boolean prototype3dEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prototype3dTriangles",
		name = "3D Triangles",
		description = "Legacy global triangle selection retained for configuration migration.",
		position = 1,
		hidden = true,
		section = PROTOTYPE_3D_SECTION
	)
	default FaceSwapTriangleCount prototype3dTriangles()
	{
		return FaceSwapTriangleCount.AUTO;
	}

	@ConfigItem(
		keyName = "prototype3dTriangleOverrides",
		name = "3D Quality Overrides",
		description = "Persistent per-head quality choices managed by the Face Swap sidebar.",
		position = 2,
		hidden = true,
		section = PROTOTYPE_3D_SECTION
	)
	default String prototype3dTriangleOverrides()
	{
		return "";
	}

	@ConfigItem(
		keyName = "prototype3dHeadItemDebug",
		name = "3D Head Item Debug",
		description = "Shows the equipped head-slot item name and ID below.",
		position = 2,
		section = PROTOTYPE_3D_SECTION
	)
	default boolean prototype3dHeadItemDebug()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prototype3dEquippedHeadItem",
		name = "3D Equipped Head Item",
		description = "Automatically managed diagnostic value. Manual edits are overwritten.",
		position = 3,
		section = PROTOTYPE_3D_SECTION
	)
	default String prototype3dEquippedHeadItem()
	{
		return "Disabled";
	}

	@ConfigItem(
		keyName = "saveHelmetPreset",
		name = "3D Auto-save Helmet Preset",
		description = "Developer-only: saves the equipped item's current 3D settings now and whenever calibration changes.",
		position = 4,
		section = PROTOTYPE_3D_SECTION
	)
	default boolean saveHelmetPreset()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prototype3dGlobalYShift",
		name = "3D Global Y Shift",
		description = "Moves every replacement head vertically after helmet calibration. Positive values move it down.",
		position = 5,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = -32,
		max = 32
	)
	default int prototype3dGlobalYShift()
	{
		return 12;
	}

	@ConfigItem(
		keyName = "prototype3dY",
		name = "3D Y",
		description = "Per-helmet vertical offset saved in its calibration profile.",
		position = 6,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = -128,
		max = 128
	)
	default int prototype3dY()
	{
		return 7;
	}

	@ConfigItem(
		keyName = "prototype3dScale",
		name = "3D Scale",
		description = "Scale percentage for the temporary 3D model.",
		position = 7,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = 25,
		max = 250
	)
	default int prototype3dScale()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "prototype3dX",
		name = "3D X",
		description = "Model-space sideways offset.",
		position = 8,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = -128,
		max = 128
	)
	default int prototype3dX()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "prototype3dZ",
		name = "3D Z",
		description = "Model-space forward/back offset.",
		position = 9,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = -128,
		max = 128
	)
	default int prototype3dZ()
	{
		return -9;
	}

	@ConfigItem(
		keyName = "prototype3dPitch",
		name = "3D Pitch",
		description = "Rotates the model forward or backward around its left/right axis.",
		position = 10,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = -180,
		max = 180
	)
	default int prototype3dPitch()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "prototype3dYaw",
		name = "3D Yaw",
		description = "Rotates the model left or right relative to the player.",
		position = 11,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = -180,
		max = 180
	)
	default int prototype3dYaw()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "prototype3dRoll",
		name = "3D Roll",
		description = "Tilts the model clockwise or counter-clockwise.",
		position = 12,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = -180,
		max = 180
	)
	default int prototype3dRoll()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "prototype3dWidth",
		name = "3D Width",
		description = "Independent left/right size used to cover the original head.",
		position = 13,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = 50,
		max = 200
	)
	default int prototype3dWidth()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "prototype3dTextureWidth",
		name = "3D Texture Width",
		description = "Expands or compresses the head PNG horizontally without changing the physical model.",
		position = 14,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = 50,
		max = 200
	)
	default int prototype3dTextureWidth()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "prototype3dFaceHeight",
		name = "3D Face Height",
		description = "Independent vertical size of the replacement head.",
		position = 15,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = 50,
		max = 200
	)
	default int prototype3dFaceHeight()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "prototype3dDepth",
		name = "3D Depth",
		description = "Independent front/back size used to occlude the original face and hair.",
		position = 16,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = 50,
		max = 200
	)
	default int prototype3dDepth()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "prototypeAnimationFrameOffset",
		name = "3D Animation Frame Offset",
		description = "Advances or delays the replacement animation relative to the player.",
		position = 17,
		section = PROTOTYPE_3D_SECTION
	)
	@Range(
		min = -3,
		max = 3
	)
	default int prototypeAnimationFrameOffset()
	{
		return 0;
	}
}
