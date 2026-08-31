# Face Asset Standards

This document records the project-specific visual and packaging decisions for runtime face assets. It supplements [Image Generation Rules](image-generation.md) and should be consulted before adding a creator, expression, or OSRS/3D variant.

## Source, approval, and archive policy

- Runtime creator assets belong in `src/main/resources/heads/content_creators/`.
- Runtime fictional assets belong in `src/main/resources/heads/fictional_characters/`.
- OSRS/3D creator assets remain in the existing `src/main/resources/heads/content_creators_3d/` resource location. The picker may expose these as creator variants without moving the physical files or changing resource paths.
- `dev-assets/` is a debug staging area, not a permanent high-resolution archive. An approved creator may have no corresponding `dev-assets` copy.
- Keep high-resolution originals and personal backups outside the repository when desired. Repository cleanup must not delete or overwrite those user-owned archives.
- Only add a creator likeness to runtime resources after recorded permission has been obtained. Keep private permission evidence outside the public repository.

## Geometry and transparency

- Every runtime head is a real `512x512` transparent PNG.
- Keep the visible head centered, similarly scaled between front and back, and within the normal padded content area.
- Front views end at the jawline; back views end at the hairline/nape. Do not include a neck, shoulders, shirt, collar, or unrelated props.
- Only the outside of the cutout should be transparent. Do not punch transparent holes through the face, hair, eyes, tears, or other intended artwork. Anti-aliased edge pixels are fine; green chroma-key fringes are not.
- Expression variants are not exempt from this rule. Furious flames may have transparent gaps where they sit outside the head, but the face, hair, eyes, and other photographed or illustrated subject areas must remain visually solid.
- Inspect both front and back assets at RuneLite in-game scale. A technically valid PNG can still be a bad asset if the crop, scale, transparency, or colors are visibly wrong.

## Baseline and expression conventions

- The unmodified creator face is the `Standard` baseline. Expressions should preserve the creator's identity, crop, scale, and lighting rather than redraw the entire head.
- Most expressions are front-only variants; the normal back asset remains the directional fallback unless the expression genuinely changes the rear of the head.
- If a requested style does not exist for a creator, runtime selection should fall back to `Standard` rather than inventing a mismatched substitute.

### Visual references

- **Furious:** use TpapaSLICE's furious face as the reference for a controlled, natural warm-red flush. Preserve skin undertones and facial detail. Avoid a flat red overlay, excessive crimson saturation, or a hue that makes every creator look identical.
- **Sick:** use the TastyLife/TpapaSLICE sick faces as the reference for a clearly green, unhealthy tint while retaining recognizable shading and facial detail.
- **Sad and crying:** use a readable squint or closed-eye expression. Crying should add clear tears and a naturally sad, open mouth without cutting holes into the face.
- **Blushing and in love:** keep the effect restrained and preserve hair and skin colors. Decorative hearts or blush should not recolor unrelated parts of the source.
- **Angel:** preserve the source creator's natural colors and hair. Add the halo without recoloring the subject; a procedural halo is preferred when it avoids another full PNG per creator.
- **Other themed variants:** keep the identity, transparency boundary, and head-only crop consistent with `Standard`. Discard outputs with necks, shoulders, internal transparency, green edges, or a game-model/cartoon style when a realistic creator cutout is required.

## Compression tiers

Compression is a release-size optimization, not part of image generation. Normalize and visually inspect an asset first, then compress a copy and compare it with the source.

- Use the current creator baseline of `pngquant --quality=0-50 --speed 1 --strip`, keeping the result only when it is smaller and visually acceptable.
- Emojis and fictional characters are lower priority and may use the more aggressive `--quality=0-30` tier.
- Do not apply the lower-priority tier to creator likenesses. Skin gradients, hair, tears, glasses, and fine facial detail show degradation sooner.
- `pngquant` is lossy even though the output remains a PNG. Inspect representative creator fronts, backs, sick/crying/furious variants, hair edges, and transparent corners after compression.
- A valid PNG can still be visually broken: the earlier Brett Dog transparency regressions showed why file readability and dimensions are not enough. For sensitive creator assets, compare the pre- and post-compression alpha masks and reject any new transparent pixels inside the intended face, eyes, mouth, hair, tears, or other subject area.
- Use `--skip-if-larger` or an explicit size comparison. Never delete the source asset as a size workaround, and never change a resource path solely to reduce the JAR.
- The final check is the packaged JAR, not the size of Git history or an ignored archive. Rebuild after compression and keep practical headroom for the next approved creator.

Before a lossy batch, make an external, recoverable copy of the exact files being changed. This is especially important for the full Brett Dog set, the `*_sick_front.png` set, and creator 3D/OSRS pairs. A safe compression batch preserves each filename, resource path, `512x512` dimensions, outside-only transparency, and front/back direction. If one image degrades, restore that image from the backup and exclude it from the lossy pass rather than weakening the acceptance rule for the whole batch.

The creator compression tier applies to both ordinary creator assets and creator 3D/OSRS assets. The more aggressive fictional/emoji tier must not be used for creator likenesses. After compression, inspect at least one standard front, standard back, expression front, and 3D/OSRS front/back from every affected creator family; Brett Dog's eyes and mouth are mandatory visual checks because those areas previously exposed the transparency defect.

The latest checked working-tree baseline used creator `0-50` compression and lower-priority `0-30` compression. It produced a `face-swap-0.9.0.jar` of `9,757,915` bytes on 2026-08-29; this is a measurement, not a permanent guarantee.

## Validation checklist

Before committing an asset batch:

1. Confirm the filename and resource location match the loader.
2. Confirm the image is `512x512`, has transparency, and has no internal holes or green fringe.
3. Inspect the front/back pair and at least the most detailed expression variants visually.
4. Run `./gradlew.bat clean test jar`.
5. Check the resulting JAR size against the exact `10,485,760`-byte Plugin Hub ceiling and run `git diff --check`.

## Release-facing asset changes

- Removing an asset requires removing or narrowing every style list that advertises it. For example, deleting `alfie_furious_front.png` also requires the Alfie style availability list and its expectations to omit `furious`.
- Keep release asset tests synchronized with intentional removals. A stale resource assertion is a test-maintenance problem, not a reason to add a replacement asset.
- Display-name changes do not automatically require resource or configuration-key renames. Stable enum IDs, filenames, and persisted keys should remain unchanged unless a migration is added; the current `JOSH_PILLAUT` ID is displayed as `The RS Felon`.
- A creator added with an OSRS/3D variant needs the complete directional pair in the 3D resource directory as well as the normal creator pair where applicable. Verify both paths and the picker mapping.
