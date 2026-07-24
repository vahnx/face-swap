# Face Swap Agent Guidelines

This repository is a RuneLite external plugin that renders approved creator face assets on player models client-side.

## Scope

- Keep all guidance and implementation specific to Face Swap.
- Do not copy or preserve rules from unrelated plugin repositories unless they apply directly to this project.
- Creator likeness assets require explicit permission before bundling or distribution.

## RuneLite Rules

- Target Java 11.
- Do not use reflection, JNI, JNA, Unsafe, runtime code generation, dynamic classloading, or Java serialization.
- Do not include `META-INF/services/net.runelite.client.plugins.Plugin`.
- Keep `build.gradle`, `settings.gradle`, package names, and `runelite-plugin.properties` aligned with `face-swap`.
- Use `log.debug()` for diagnostic logging. Avoid high-frequency `log.info()` calls.
- Keep overlay render work lightweight; overlays run every frame.
- Clean up overlays, toolbar buttons, listeners, and other registered resources in `shutDown()`.

## Config

- The config group is `faceswap`.
- Do not rename config keys without a migration plan.
- Sidepanel-managed settings may be persisted as hidden config items.
- Calibration settings may remain visible in RuneLite config if they are useful for testing.

## Mask Tracking

- Preserve the current merged-player-model mask tracker as a legacy fallback when implementing equipment-independent or rig-based tracking.
- Do not remove the fallback until the replacement has been verified across normal movement, zoom levels, equipment changes, and representative emotes.
- Follow `docs/mask-tracking.md` for the current behavior, known limitations, and migration requirements.

## Assets

- Never name a sidepanel or runtime classpath icon `icon.png`. Generic classpath resource names can collide with other plugins and display the wrong icon; use a plugin-specific name such as `face_swap_icon.png`. This does not prohibit RuneLite's repository-root `icon.png` used for the Plugin Hub listing.
- Follow `docs/image-generation.md` for every generated head asset.
- Follow `docs/development-assets.md` for every debug-only head asset.
- Final runtime assets must be transparent PNGs under the matching category directory:
  - Content creators: `src/main/resources/heads/content_creators/`
  - Fictional characters: `src/main/resources/heads/fictional_characters/`
- Never place debug-only assets under `src/main/resources`; keep them under the matching gitignored `dev-assets/heads/<category>/` directory.
- Use lower-case snake-case filenames:
  - Base: `king_condor.png`
  - Directional: `king_condor_front.png`, `king_condor_back.png`, `king_condor_left.png`, `king_condor_right.png`
- Ensure PNGs are actual PNG files with alpha; do not rename JPEGs or opaque files to `.png`.
- Keep generated source/chroma-key images out of `src/main/resources/heads/` unless intentionally used at runtime.

## Testing

- You cannot verify in-game behavior yourself.
- Do not automate RuneScape input.
- After code changes, run `.\gradlew.bat test`.
- For visual placement changes, tell the user exactly what to test in-game and wait for confirmation.

## Plugin Hub Safety

- Do not add features that send game actions, modify PvP behavior, automate input, expose player data, or violate Jagex/RuneLite third-party client rules.
- This plugin should remain a client-side visual/cosmetic overlay.
