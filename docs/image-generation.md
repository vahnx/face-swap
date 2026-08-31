# Image Generation Rules

These rules apply to every generated face/head asset for this plugin.

For project-specific expression, transparency, archive, and compression decisions, also read [Face Asset Standards](asset-standards.md).

## Required Output

- Final plugin assets must be transparent PNG files.
- Runtime head assets must use a `512x512` transparent canvas.
- Fit the visible head proportionally within a `480x480` content area, leaving at least 16 pixels of transparent padding.
- Trim excess transparent whitespace before resizing, but do not stretch, distort, regenerate, or crop visible parts of the head.
- Keep front and back assets centered with comparable head scale and vertical placement.
- Store creator runtime assets in `src/main/resources/heads/content_creators/`.
- Store fictional-character runtime assets in `src/main/resources/heads/fictional_characters/`.
- Store local debug-only assets under the matching `dev-assets/heads/<category>/` directory and follow `docs/development-assets.md`.
- Use lower-case snake-case filenames matching the loader naming pattern:
  - Base asset: `king_condor.png`
  - Directional assets: `king_condor_front.png`, `king_condor_back.png`
- Do not commit source images with solid backgrounds unless they are intentionally kept as references outside `src/main/resources/heads/`.

## Creator Cutout Style

Creator assets must match the existing streamer assets in the destination folder: realistic, isolated cardboard-cutout portraits with natural photographic or digitally painted facial, hair, and skin detail.

- Use a front-facing portrait for `front` assets and a straight-on rear head portrait for `back` assets.
- Include the head only. End the cutout cleanly at the jawline in front views and the hairline/nape in back views; do not include a neck, shoulders, shirt, or collar.
- Keep the subject as one clean isolated cutout with natural contours and no scene, props, clothing logos, text, or other people.
- Keep front and back assets at comparable scale and vertical placement.
- Treat the result as a realistic portrait cutout, not a game-model render.

Every creator prompt must explicitly say:

```text
Match the realistic cardboard-cutout portrait style of the existing streamer assets in the destination folder. Use natural photographic or digitally painted facial, hair, and skin detail. Do not create a low-poly, faceted, voxel, blocky, clay, cartoon, illustrated, or 3D game-model render. Do not add geometric facets or hard-surface modeling.
Keep the asset head-only. Do not include a lower neck, shoulders, shirt, or collar; terminate the transparent cutout cleanly at the jawline or hairline/nape.
```

If the output looks faceted, low-poly, blocky, voxel-like, or like a 3D game model, discard it and regenerate it as a realistic portrait cutout. Do not post-process a block-style output into a creator asset.

When adding a new creator, update `docs/creator-statistics.md` with a dated audience snapshot and place the creator in the popup order using the largest attributable public count from the linked Twitch, Kick, YouTube, or other listed social accounts. If a count cannot be verified, record that explicitly and preserve the existing relative order rather than guessing.

## Prompt Requirements

Every image-generation prompt for a plugin asset must include this language or an equivalent stricter version:

```text
Create the subject on a perfectly flat solid #00ff00 chroma-key background for background removal.
The background must be one uniform color with no shadows, gradients, texture, reflections, floor plane, or lighting variation.
Keep the subject fully separated from the background with crisp edges and generous padding.
Do not use #00ff00 anywhere in the subject.
No cast shadow, no contact shadow, no reflection, no watermark, and no text unless explicitly requested.
The final project asset must be a transparent PNG.
```

## Generation Workflow

- Generate assets sequentially: submit one image-generation request, wait for it to finish, inspect/save the result, and only then submit the next request.
- Generate each direction separately and complete the front/back pair before moving to the next subject.
- Do not batch many subjects or front/back requests into one tool call. Large batches can exceed orchestration or execution time limits and may leave the run incomplete; this is a reliability precaution, not a claim that the image model cannot generate multiple images.
- If a request fails or times out, retry only the unfinished asset rather than restarting the entire batch.

## Post-Processing

After generation, remove the chroma-key background before adding the asset to `src/main/resources/heads/`.

Preferred command:

```powershell
python tools/create_head_overlay.py path\to\source.png src\main\resources\heads\content_creators\king_condor_front.png
```

Normalize completed runtime assets with:

```powershell
python tools/normalize_head_assets.py src/main/resources/heads/content_creators
python tools/normalize_head_assets.py src/main/resources/heads/fictional_characters
```

The normalizer uses high-quality Lanczos resampling, preserves alpha, centers the visible content, and applies lossless PNG optimization. PNG DPI metadata is irrelevant; validate pixel dimensions instead.

### Plugin Hub Size Optimization

When the packaged plugin approaches the Plugin Hub size limit, recompress runtime PNGs with `pngquant`. This is a separate release-size pass from the lossless normalizer above: `pngquant` is lossy and can substantially reduce indexed-color PNG sizes.

Use the creator tier for content-creator faces and the lower-priority tier only for emojis and fictional characters:

From the repository root, run:

```powershell
Get-ChildItem src/main/resources/heads/content_creators,src/main/resources/heads/content_creators_3d -Recurse -Filter *.png | ForEach-Object {
    pngquant --quality 0-50 --speed 1 --strip --skip-if-larger --force --ext .png -- $_.FullName
}

Get-ChildItem src/main/resources/heads/emojis,src/main/resources/heads/fictional_characters -Recurse -Filter *.png | ForEach-Object {
    pngquant --quality 0-30 --speed 1 --strip --skip-if-larger --force --ext .png -- $_.FullName
}
```

The options mean:

- `--quality 0-50`: current creator compression tier; inspect the result visually.
- `--quality 0-30`: more aggressive tier reserved for emojis and fictional characters.
- `--speed 1`: prioritizes compression quality over processing speed.
- `--strip`: removes nonessential metadata.
- `--skip-if-larger`: keeps the original when the optimized file would be larger.
- `--force --ext .png`: replaces each file while preserving its path and filename.

This preserves dimensions, alpha transparency, and code references, but it may reduce color precision through indexed-color quantization. Afterward, run the asset tests and build, inspect representative front/back assets and screenshots, and check the final JAR size. See [Face Asset Standards](asset-standards.md) for the project’s visual acceptance rules and expression references.

Do not accept a compressed expression variant if quantization creates transparent holes through the subject. Restore the higher-quality source, repair the exterior-only alpha mask, or keep that individual asset out of the lossy pass.

Historical result from the first size reduction pass:

- Tracked PNGs: `23,989,598` bytes -> `7,021,286` bytes.
- Total tracked repository files: `25,898,337` bytes -> `8,952,696` bytes.

Do not delete assets or change resource paths as a size workaround. Revert any visually degraded asset individually and recheck the packaged artifact.

If using another cleanup tool, verify:

- The output has an alpha channel.
- The output is exactly `512x512`.
- The corner pixels are transparent.
- No green fringe remains around the head.
- The asset still reads clearly at RuneLite in-game scale.

## Directional Assets

When creating wraparound-style assets, generate front and back directions separately. Do not ask for a single folded box/paper texture unless the implementation changes to a different texture layout.

Emoji assets are an exception: because their front and back are identical, store one base file such as `smiley.png` without `_front` or `_back`. The runtime loader uses that base file for every direction.

Recommended progression:

- Use `front` and `back`.
- Missing side variants are estimated at runtime from the front and back assets.
- Add explicit side variants when available to override the runtime estimates.
