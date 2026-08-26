# RuneLite Plugin Development — Agent Guidelines

This file is split into shared RuneLite Plugin Hub guidance and repo-specific overrides for this repository.

## Shared RuneLite Rules

## Logging

- Use `log.debug()` for developer/diagnostic logging.
- Do not use `log.info` for per-frame or per-event logging - RuneLite runs at INFO level in production, so high-frequency info logs will pollute user logs. `log.info()` is fine for one-time startup/shutdown messages or infrequent events.

## Threading & Concurrency

- Never use `Thread.sleep()`. Use a `ScheduledExecutorService`, RuneLite event callbacks, or other non-blocking scheduling instead.
- Never call `Thread.interrupt()` or rely on thread interruption for normal plugin control flow.
- Never block on `shutDown()` or `startUp()` — don't call `executor.awaitTermination()` in shutdown, just use `shutdownNow()`.
- Never do blocking network IO or disk IO on the client thread. The OkHttp thread pool can be used for blocking network requests.
  If you need to call back into `client` from the okhttp threadpool, such as from the response queued with `enqueue()`, use `clientThread.invoke()`
- Explicitly cancel scheduled tasks (e.g. `ScheduledFuture`) on shutdown, in addition to shutting down the executor.
- For batching async work, use `CompletableFuture.allOf()` — not `CountDownLatch`.
- If you must use `Process.waitFor()`, always pass a reasonable timeout.

## Performance

- Don't scan the entire scene every tick or frame. Use events such as object and npc (de)spawn to track what you care about and maintain your own collection.
- Keep the computations in Overlays, which are run each frame, to a minimum.

## API Usage

- Use `net.runelite.api.gameval` package constants — `ItemID`, `InterfaceID`, `ObjectID`, etc. Never hardcode magic numbers when gameval constants can be used instead.
- Use `LinkBrowser` to open URLs, not `java.awt.Desktop`
- When looking up Widgets, pass the component ID from gamevals (eg `client.getWidget(InterfaceID.DomEndLevelUi.LOOT_VALUE)`) - do not manually combine interface + component child IDs.
- Use of Java reflection is forbidden.

## Data Sources

- Prefer RuneLite-provided data first: `net.runelite.api.gameval` constants, enums, client APIs, and the RuneLite source tree.
- Use the Old School RuneScape Wiki to look up canonical game data when needed, especially names, IDs, locations, drop sources, and other content reference details.
- When a wiki lookup conflicts with RuneLite-exposed constants or in-client behavior, treat RuneLite/client-observed behavior as authoritative for plugin implementation and verify the discrepancy before coding around it.

## Input Handling

- Do not use `KeyboardFocusManager` or `KeyEventDispatcher` for plugin hotkeys. Use RuneLite input APIs instead.
- Register plugin hotkeys through RuneLite input APIs (`KeyManager` with a `KeyListener`).
- Never capture or consume plugin hotkeys while the user is on the login screen or otherwise not logged into the game. Login UI input must remain unaffected.
- Treat RuneLite/Plugin Hub input restrictions as a release blocker. If a hotkey implementation bypasses normal RuneLite input handling, assume it is not acceptable until verified otherwise.

## HTTP & JSON

- Use OkHttp for all HTTP requests. `@Inject OkHttpClient` to get the HTTP client. Do not use `HttpURLConnection`, `java.net.http.HttpClient`, or Apache HttpClient.
- Use `@Inject Gson` to get a Gson instead, never create your own from scratch. You can use `.newBuilder()` to create one derived from the base `Gson.`
- Do not add transitive dependencies from `runelite-client` directly to `build.gradle`, such as gson, guice, or okhttp.
- Never execute okhttp calls on the client thread. Prefer using `enqueue()` which places the request on the okhttp threadpool.

## File I/O

- Only read/write files inside the `.runelite` directory. Create a subdirectory for your plugin (e.g. `.runelite/your-plugin-name/`) if you need to store data on disk.
- Use `RuneLite.RUNELITE_DIR` to get the path.
- A user-initiated `JFileChooser` may read the explicitly selected source path for an import, then copy the result into the plugin-specific `.runelite` directory; do not scan or read arbitrary paths without user action.

## Config

- Config group names must be specific — e.g. `"deadman-prices"`, not `"deadman"`.
- Never rename a config key or config group without providing a migration. Renaming silently resets users' saved settings.
- If you add a `@ConfigItem` that toggles a feature involving a third-party server, it must:
  - Be **disabled by default** (opt-in)
  - Have a `warning` field set to: `"This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"`

## Plugin Setup & Packaging

- Rename everything from the template. Do not leave template package names, class names, or placeholder config groups in place. Rename the package path, class names, config group, `build.gradle` group, `settings.gradle` project name, and `runelite-plugin.properties`.
- Do not include a `META-INF/services/net.runelite.client.plugins.Plugin` file.
- Do not commit build artifacts — no `.class` files, `out/` directories, or `.tmp` directories.
- `build.gradle` must target Java 11** and match the structure of the example-plugin template.
- Retain a permissive license, such as BSD-2.

## Resources & Assets

- Optimize icon PNGs. Java loads images at full resolution in memory (`width × height × 4` bytes), so a seemingly small file can use significant memory.
- Ensure PNGs are actually PNGs — do not rename JPEGs or ICOs to `.png`.

## Cleanup

- Remove unused config classes, fields, and imports.
- Clean up subscriptions, listeners, and overlays in `shutDown()`.
- Do not mix code reformatting with feature changes in the same commit — it makes diffs unreadable for reviewers.

## Testing

You cannot verify plugin behavior yourself. Even if you have screen-capture or computer-use tools available, **do not use them to interact with RuneScape** — automating game input violates Jagex's third-party client guidelines and will get the user's account banned. Only the user can confirm a plugin works in-game.

After completing a task, do not declare it done. Instead:

1. Offer to launch RuneLite for the user by running `./gradlew run` from the plugin's root directory.
2. Instruct the user to follow the "Using Jagex Accounts" instructions found at https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts to login to the development client.
3. Tell the user *what to test* — the specific behavior you changed, the golden path, and any edge cases worth exercising.
4. Wait for the user to confirm the feature works in-game before considering the task complete. A clean JVM start is not a passing test.

## Repo-Specific Overrides

These rules are specific to this repository and should be kept separate from the shared RuneLite guidance above.

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

- Face Swap custom-image persistence must remain under `.runelite/face-swap/`. A user-initiated `JFileChooser` may select a source anywhere, but the plugin must copy the imported result into that directory and must not perform unprompted external reads.
- `src/main/resources/helmet_profiles.csv` is packaged read-only default data. Runtime helmet-profile edits must use `.runelite/face-swap/helmet_profiles.csv`.
- Runtime local 3D landmark offsets must use `.runelite/face-swap/helmet_mesh_calibration.csv`; keep the packaged profile CSV schema compatible.
- Never derive runtime plugin paths from `user.dir` or assume that `src/main/resources` exists after Plugin Hub packaging.
- Keep runtime file I/O off the client thread, including reads and writes triggered by developer calibration controls.
- Config keys and helmet-profile formats are persistent interfaces; add migrations before changing names or column meanings.

## Verification

- Run `./gradlew.bat test` after code changes and `./gradlew.bat clean test` before release.
- Offer to launch the development client with `./gradlew.bat run` after changes.
- The user must log in through the [Using Jagex Accounts instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
- Tell the user exactly what to test, including front/side/back views, camera movement, scene changes, equipment, target scopes, and plugin disable/enable when relevant.
- A passing build or clean JVM start is not in-game verification. Wait for user confirmation of visual behavior.

---

# Plugin Rules & Restrictions

Features that are **forbidden or restricted** in RuneLite hub plugins.
Sourced from [Jagex's Third-Party Client Guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) and RuneLite's [Rejected or Rolled-Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features).

**If your plugin does any of the things listed below, it will be rejected.**

## Forbidden Language Features

- All code must be Java 11 compatible
- No use of reflection
- No use of JNI or JNA
- No direct access to native memory access via Unsafe or LWJGL
- No executing external processes, including with Process or ProcessBuilder
- No downloading or dynamic loading of code, including classloading
- No runtime generation of code
- No use of Java (de)serialization

## Boss & Combat Restrictions

Applies to all bosses, Raids sub-bosses, Slayer bosses, Demi-bosses, and wave-based minigames (Fight Caves, Inferno, etc.):

- No next-attack prediction (timing or attack style)
- No projectile target/landing indicators
- No prayer switching indicators
- No attack counters
- No automatic indicators showing where to stand or not stand (manual tile marking is allowed)
- No additional visual or audio indicators of a boss mechanic, unless it is a manually triggered external helper
- No advance warning of future hazards (highlighting currently active hazards is OK)
- No "flinch" timing helpers
- No combat prayer recommendations
- No NPC focus identification (which player the NPC is targeting)
- No content simulation (e.g. boss fight simulators)

New high-end PvM boss plugins are not accepted as a blanket policy.

## PvP Restrictions

- No removing or deprioritising attack/cast options in PvP
- No opponent freeze duration indicators
- No PvP clan opponent identification
- No PvP loot drop previews
- No identifying an opponent's opponent
- No PvP target scouting information
- No player group summaries (attackable counts, prayer usage, etc.)
- No level-based PvP player indicators (highlighting attackable players or those within level range)
- No spell targeting simplification (removing menu options to make targeting easier)

## Menu Restrictions

- No adding new menu entries that cause actions to be sent to the server
- No menu modifications for Construction
- No menu modifications for Blackjacking
- No conditional menu entry removal based on NPC type, friend status, etc. (can be overpowered)

## Interface Restrictions

- No unhiding hidden interface components (special attack bar, minimap)
- No moving or resizing click zones for 3D components
- No moving or resizing click zones for combat options, inventory, equipment, or spellbook
- No resizing prayer book click zones
- No resizing spellbook components
- No removing inventory pane background or making it click-through
- No detached camera world interaction (interacting with the game world from a camera position that isn't the player's)

## Input Restrictions

- No injecting input events, including mouse and keyboard events
- No autotyping — plugins must not programmatically insert text into the chatbox input (includes pasting, shorthand expansion)
- No modifying outgoing chat messages after the user sends them

## Data & Privacy Restrictions

- No exposing player information over HTTP
- No crowdsourcing data about other players (locations, gear, names, etc.)
- No credential manager plugins that stores account credentials

## Content Restrictions

- No adult or overtly sexual content
- No plugins that use player-provided IDs for their entire functionality (causes moderation issues)

## Shared Plugin UI Design

- Keep plugin UI feeling native to RuneLite: simple layouts, clear hierarchy, predictable controls, and minimal visual noise.
- Group controls by user task, not by implementation detail. Keep frequent actions obvious and low-friction.
- Reserve advanced, destructive, or developer-only controls for clearly separated areas.
- Prefer concise labels that describe user intent. Avoid overexplaining internal mechanics in the main UI.
- Read [docs/PLUGIN_UI_GUIDELINES.md](docs/PLUGIN_UI_GUIDELINES.md) for the full shared sidepanel, button, tab, typography, layout, and input-mode conventions.
