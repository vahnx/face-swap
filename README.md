# Face Swap

Swap faces onto OSRS player or NPC models.

## Features

- Pick from a selection of your favorite content creators
- Target scope options for your player, friends, clanmates, other players, or specific player names
- Persistent per-client configuration for selected head, target scope, and player name list
- Rigged 3D replacement heads with Low, Medium, High, and Ultra quality levels
- Optional projected wraparound and front-worn mask modes
- In-game apply mode scaffold for selecting a player and assigning the current head
- The release picker includes King Condor, Alfie, Sardaco, DearLola, Beggar, TPapaSlice, and ZeCookies
- Odablock remains available only in debugger-attached developer launches

## Screenshots & Demos

## Usage

Open the Face Swap sidepanel, choose a creator head, then choose who the head should apply to.

For specific players, enter comma-separated player names in the target list. The planned in-game assignment flow is to select a head, click **Apply this head to a player in-game**, then click a visible player to assign that head.

The rendering is a visual overwrite only on your client and does not change game state. Style and 3D quality are selected from the plugin sidebar; model and helmet calibration values are maintained internally.

Render modes:

- `3D` is the default. It attaches the selected creator texture to a rigged head model that follows player movement and animation.
- `Wraparound` maps front/back PNG assets across the projected upper-head triangles.
- `Mask` projects the front and back assets onto opposite surfaces of one face-worn panel with a single adjustable strap around the head.

Run the plugin in developer mode with:

```bash
./gradlew run
```

On Windows, use:

```bat
gradlew.bat run
```

## Asset Pipeline

Release-approved head overlays are transparent PNG files stored by category in `src/main/resources/heads/content_creators/` or `src/main/resources/heads/fictional_characters/`, using the lower-case enum name such as `king_condor_front.png`. Local debug-only assets use the matching category under the gitignored `dev-assets/heads/` directory and are never included in Plugin Hub builds.

To convert a generated chroma-key source image into a 42x42 transparent overlay:

```powershell
python tools/create_head_overlay.py path\to\source.png src\main\resources\heads\content_creators\king_condor_front.png
```

See `docs/development-assets.md` before adding any asset that is not approved for release.

## Asset Permissions

Creator likeness assets are included in releases only after the creator has given explicit permission for their use in Face Swap. The project owner maintains private, dated video recordings of creator consent and can provide proof to RuneLite Plugin Hub reviewers upon request.

Confirmed permissions:

- King Condor
- Alfie
- Sardaco
- DearLola
- Beggar
- TPapaSlice
- ZeCookies

The private permission log records the creator name, approval date, platform, approved use, and corresponding recording filename. Development placeholders and creator entries without documented consent must not be included in a distributed release.

## Helmet Profiles

Helmet-specific 3D calibration is stored in `src/main/resources/helmet_profiles.csv`. A row marked `tested` overrides the live 3D settings for its item IDs; a row marked `untested` remains a test checklist and continues using the live RuneLite config values. Keep alternate item states in the same row as semicolon-separated IDs, then mark a profile `tested` only after verifying its alignment in game.

## Head Quality Profiles

Developer-defined 3D quality defaults are stored per creator in `src/main/resources/head_quality_profiles.csv`. The sidebar remembers a user's quality selection separately for each head. Resetting the plugin configuration clears those user overrides and restores the values from this CSV.

## Screenshots

Screenshots will be added after the sidepanel and in-game model replacement are visually complete.

## Known Issues

- Creator likenesses without documented permission must be removed from release builds.
- Current overlay art is generated placeholder imagery until approved creator assets are available.
- Hair is intentionally overwritten by the wraparound asset. Open-face helmet preservation is projection-based and may need per-helmet calibration as equipment geometry varies.
- A Java2D overlay cannot participate directly in RuneLite's scene depth buffer. The plugin approximates correct open-face helmet depth by restoring the original projected helmet triangles after rendering the face.
- Specific player click assignment is scaffolded in the UI but does not yet hook into RuneLite player picking.

## Featured Creators

These are the verified public channels for creators featured in the release. Only platforms that could be confidently attributed to the creator are listed.

- **King Condor:** [Kick](https://kick.com/kingcondor) | [YouTube](https://www.youtube.com/@KingCondor) | [Twitch](https://www.twitch.tv/kingcondor6969)
- **Alfie:** [Kick](https://kick.com/alfie) | [YouTube](https://www.youtube.com/channel/UCEQoTuUGfulxInScbck1_Xw) | [Twitch](https://www.twitch.tv/alfie)
- **Sardaco:** [Kick](https://kick.com/sardaco) | [YouTube](https://www.youtube.com/@SardacoTV) | [Twitch](https://www.twitch.tv/sardaco)
- **DearLola:** [Kick](https://kick.com/dearlola1) | [YouTube](https://www.youtube.com/thedearlola) | [Twitch](https://www.twitch.tv/dearlola1)
- **Beggar:** [YouTube](https://www.youtube.com/@beggarofficial) | [Twitch](https://www.twitch.tv/beggarofficial)
- **TPapaSlice:** [YouTube](https://www.youtube.com/@TpapaSLICE) | [Twitch](https://www.twitch.tv/tpapaslice)
- **ZeCookies:** [Kick](https://kick.com/zecookies) | [YouTube](https://www.youtube.com/@zecookiess) | [Twitch](https://www.twitch.tv/zecookies)

## Support Development

If you find this plugin useful and would like to support its continued development:

- [Sponsor me on GitHub](https://github.com/sponsors/vahnx)
- [Support me through PayPal](https://paypal.me/twitchplaying)

Thank you for your support!
