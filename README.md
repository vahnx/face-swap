# Face Swap

Swap faces onto OSRS player or NPC models.

## Features

- Pick from a selection of your favorite content creators
- Target your player, friends, clanmates or anyone around you in-game to apply them
- Apply them a 3D model or mask
- Load your own creations from the local Face Swap data directory

## Creators
Current release creators:

- **Sardaco:** [Kick](https://kick.com/sardaco) | [YouTube](https://www.youtube.com/@SardacoTV) | [Twitch](https://www.twitch.tv/sardaco) | [X](https://x.com/Sardaco)
- **Skill Specs:** [Kick](https://kick.com/skillspecs) | [YouTube](https://www.youtube.com/@skillspecs) | [Twitch](https://www.twitch.tv/skillspecs) | [X](https://x.com/skill_specs)
- **TastyLife:** [Kick](https://kick.com/tastylife) | [YouTube](https://www.youtube.com/@TastyLifeRS) | [Twitch](https://www.twitch.tv/tastylife) | [X](https://x.com/TastyOSRS)
- **TpapaSLICE:** [Kick](https://kick.com/tpapaslice) | [YouTube](https://www.youtube.com/@TpapaSLICE) | [Twitch](https://www.twitch.tv/tpapaslice) | [X](https://x.com/TpapaTV)
- **Prison Joe:** [YouTube](https://www.youtube.com/@PJWIDTH) | [Twitch](https://www.twitch.tv/prisonjoe) | [X](https://x.com/Prison_Joseph)
- **ZeCookies:** [Kick](https://kick.com/zecookies) | [YouTube](https://www.youtube.com/@zecookiess) | [Twitch](https://www.twitch.tv/zecookies) | [X](https://x.com/zecookiess)
- **Alfie:** [Kick](https://kick.com/alfie) | [YouTube](https://www.youtube.com/channel/UCEQoTuUGfulxInScbck1_Xw) | [Twitch](https://www.twitch.tv/alfie) | [X](https://x.com/RSAlfierules)
- **King Condor:** [Kick](https://kick.com/kingcondor) | [YouTube](https://www.youtube.com/@KingCondor) | [Twitch](https://www.twitch.tv/kingcondor6969) | [X](https://x.com/KingCondor69)
- **DearLola:** [Kick](https://kick.com/dearlola1) | [YouTube](https://www.youtube.com/thedearlola) | [Twitch](https://www.twitch.tv/dearlola1) | [X](https://x.com/DearLola1)
- **eliop14:** [Kick](https://kick.com/eliop14) | [YouTube](https://www.youtube.com/@eliop14) | [Twitch](https://www.twitch.tv/eliop14) | [X](https://x.com/eliopdagod)
- **jillyfish:** [YouTube](https://www.youtube.com/c/jillyfish) | [Twitch](https://www.twitch.tv/jillyfish) | [X](https://x.com/jillyfishs)
- **Beggar Official:** [YouTube](https://www.youtube.com/@beggarofficial) | [Twitch](https://www.twitch.tv/beggarofficial) | [X](https://x.com/BeggarOfficial)
- **Grim:** [Kick](https://kick.com/grimosrs) | [YouTube](https://www.youtube.com/@GrimOSRS) | [X](https://x.com/osrs_grim)
- **Asian Andy:** [Kick](https://kick.com/asianandy) | [YouTube](https://www.youtube.com/channel/UCovb8rgpCANx6nwDwnW0Uqg)| [X](https://x.com/AsianAndyFilms)

..and more to come upon request and approval

## Screenshots

Swag-up your favorite creators with your BiS fashon-scape!

![Helms](images/fs-3d-helm.png)

Pick from an exhaustive list!

![Creators](images/fs-content-creator-list.png)

Warm up with your besties!

![Masks](images/fs-masks.png)

Easy to use, modern interface!

![Config](images/fs-config-menu.png)

Target other players -or- NPCs!

![Target](images/fs-target.png)

Have lots of fun!

![Fun](images/fs-fun.png)

## Usage

Open the Face Swap sidepanel, choose a creator head, then use the Target button to select who the head should apply to. 

Use `Browse...` in the Custom Images tab to select an image from your computer. Face Swap copies the imported image into `.runelite/face-swap/custom-heads/`; runtime helmet preset edits are stored in `.runelite/face-swap/helmet_profiles.csv`. The packaged `helmet_profiles.csv` is read-only default data.

When running the local launcher with `--developer-mode`, enable `Interactive Helmet Calibration` in the developer configuration while using 3D mode. Drag the yellow face landmarks to deform nearby mesh regions in local X/Y; use the mouse wheel over a landmark for its local depth, or elsewhere over the head for global Z. The center and W/H/S/D handles remain available for broad global fitting. Use `Save preset` only after the fit is correct; `Reset` restores the packaged/runtime profile and saved mesh offsets for the equipped head item. Pitch, yaw, and roll remain available as numeric developer controls.

Player and NPC target assignments are prepared for future alternate facial styles: existing target-name settings remain compatible, while optional style IDs default to `default` until alternate style assets and UI are added.

Render modes:

- `3D`
- `Mask`
- `Wraparound`

## Asset Permissions

Content creators used in this plugin have granted permission for their likeness to be used in this plugin. The project owner maintains private, dated video recordings of creator consent and can provide proof to RuneLite Plugin Hub reviewers upon request.


The private permission log records the creator name, approval date/time, platform, and approved use.
## Known Issues

- `Wraparound` mode does not work properly with helmets and weapons equipped
- When masked players overlap while facing different directions, all of their masks may remain visible
- `Mask` and `Wraparound` overlays do not participate in scene depth and can be visible through walls
- `3D` mode is designed primarily for players and humanoid, single-head NPCs
- Non-humanoid NPCs, pets, and some enemies can render with stretched geometry, misplaced heads, or incorrect mask placement in `3D` mode
- Multi-head NPCs and bosses, such as the King Black Dragon, are not yet supported correctly in `3D` mode

## Submissions & Requests

If you wish to have your face added,  submit feature requests, or other inquiries, email [osrsfaceswap@gmail.com](mailto:osrsfaceswap@gmail.com) or [open a GitHub issue](https://github.com/vahnx/face-swap/issues/new).

## Support Development

If you find this plugin useful and would like to support its continued development:

- [Sponsor me on GitHub](https://github.com/sponsors/vahnx)
- [Support me through PayPal](https://paypal.me/twitchplaying)
- Bitcoin: `bc1qnl9nvwldzzdnlglqhvlhdm5l04rxawgkezr2ru`

Join the [vahnx Projects Discord](https://discord.gg/bn4HcQWTp) for general support, suggestions and project discussion.

Thank you for your support!
