# Image Generation Rules

These rules apply to every generated face/head asset for this plugin.

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

If using another cleanup tool, verify:

- The output has an alpha channel.
- The output is exactly `512x512`.
- The corner pixels are transparent.
- No green fringe remains around the head.
- The asset still reads clearly at RuneLite in-game scale.

## Directional Assets

When creating wraparound-style assets, generate front and back directions separately. Do not ask for a single folded box/paper texture unless the implementation changes to a different texture layout.

Recommended progression:

- Use `front` and `back`.
- Missing side variants are estimated at runtime from the front and back assets.
- Add explicit side variants when available to override the runtime estimates.
