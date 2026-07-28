# Face Swap Agent Guidelines

Read the shared RuneLite rules in `../runelite-plugin-notes/guidelines.md` and
`../runelite-plugin-notes/release-checklist.md` before changing this plugin. This
file contains only Face Swap-specific guidance.

## Scope And Safety

- Keep this plugin client-side and cosmetic.
- Never automate RuneScape input, send game actions, alter PvP behavior, expose player data, or add features prohibited by Jagex or RuneLite policy.
- Do not use adult or overtly sexual content.
- Only include creator likenesses with recorded permission. Keep private permission evidence outside the public source repository.

## Assets

- Runtime creator assets belong in `src/main/resources/heads/content_creators/`.
- Runtime fictional assets belong in `src/main/resources/heads/fictional_characters/`.
- Debug-only assets belong in the matching ignored path under `dev-assets/heads/`, never in `src/main/resources`.
- Keep front/back assets as real optimized `512x512` transparent PNGs with comparable scale and placement.
- Do not use mirrored faces as rear textures or leave green chroma-key fringes.
- The runtime toolbar icon is `face_swap_icon.png`, not `icon.png`.

## Rendering And Calibration

- Supported styles are `3D`, `Mask`, and `Wraparound`; document their limitations honestly.
- Use RuneLite's injected `@Named("developerMode")` flag for developer-mode behavior; do not inspect JVM arguments or use `ManagementFactory`.
- 3D mode targets players and humanoid single-head NPCs. Pets, non-humanoid NPCs, and multi-head bosses may stretch or misplace geometry.
- Tested helmet profiles override global 3D calibration values. Do not assume changing global `3D Y`, `3D Z`, width, face height, or depth changes a helmet with an active profile.
- `3D Back Depth` and `3D Chin Height` are global weighted geometry controls and must be included in model cache keys and cache invalidation.
- Keep model generation and RuneLite model/object access on the client thread. Cache generated models and avoid rebuilding them per frame.
- Overlays do not participate in scene depth; do not describe projected `Mask` or `Wraparound` geometry as equivalent to a scene-rendered model.

## Face Swap UI

- The sidepanel should expose user-facing choices such as head, style, target scope, quality, and mode-specific sizing.
- Developer-only calibration and diagnostic controls belong in the developer config unless an explicit end-user override design makes their precedence clear.
- `Tabbed Head Picker` is a release-facing sidepanel preference under the collapsible `Sidepanel` config section.
- Target selection must remain manual and must exit on `ESC`.

## Local Persistence

- Face Swap custom-image persistence must remain under `.runelite/face-swap/` or use a user-initiated `JFileChooser` operation.
- Config keys and helmet-profile formats are persistent interfaces; add migrations before changing names or column meanings.

## Verification

- Run `./gradlew.bat test` after code changes and `./gradlew.bat clean test` before release.
- Offer to launch the development client with `./gradlew.bat run` after changes.
- The user must log in through the [Using Jagex Accounts instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
- Tell the user exactly what to test, including front/side/back views, camera movement, scene changes, equipment, target scopes, and plugin disable/enable when relevant.
- A passing build or clean JVM start is not in-game verification. Wait for user confirmation of visual behavior.
