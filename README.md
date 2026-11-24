# VOIDNEX CLIENT

A modular Fabric client mod for **Minecraft 26.2** (Java 25): animated menu hub, 29 modules,
drag-and-drop HUD editor, themes, configs, profiles, keybind manager and update checker.
Client-side only. No cheats: nothing that gives a combat or movement advantage over other players.

## Install (players)
1. Install the [Fabric Loader](https://fabricmc.net/use/installer/) (0.19.3 or newer) for Minecraft **26.2**. The launcher bundles Java 25.
2. Put the **Fabric API for 26.2** (e.g. `fabric-api-0.160.0+26.2.jar` or newer) and `voidnex-client-1.0.0.jar` in `.minecraft/mods`.
3. Launch the Fabric 26.2 profile. Press **Right Shift** in game (or click **VOIDNEX** on the title / pause screen).

## Build (developers)
```bash
# Needs JDK 25. One-time: create the Gradle wrapper (Gradle 9.5+ installed), or open the folder in IntelliJ IDEA 2025.3+
gradle wrapper --gradle-version 9.5.1
./gradlew build          # jar in build/libs/voidnex-client-1.0.0.jar
./gradlew runClient      # start a dev client
```
Versions live in `gradle.properties`. Check https://fabricmc.net/develop for the newest matching versions
(Loom, Loader, Fabric API). Minecraft 26.x is unobfuscated: the code uses Mojang names, not Yarn.

## Using it
| Where | What |
|---|---|
| Home | Welcome message, live FPS / Ping / CPS, pinned (favorite) modules, quick actions |
| Combat / Movement / Player / Render / World / Utility / HUD / Misc | Module cards: switch, star, expand for keybind + settings, right-click to toggle |
| Search bar | Filters every module by name / description / category |
| HUD Editor | Drag to move, mouse wheel to scale, right-click to hide, chips to show, snap guides |
| Appearance | Dark/light, accent (purple, blue, red, green), backgrounds, blur, glow, particles, radius, opacity, notifications |
| Configs | Save / load / delete full configs (`config/voidnex/configs/*.json`) |
| Profiles | Quick module-state snapshots stored in `config.json` |
| Keybinds | Rebind every module, conflicts are marked. Zoom is hold-to-zoom on **C** by default |
| Info | Version, account, server, ping, update checker |

Custom logo / background: put `logo.png` / `background.png` in `.minecraft/config/voidnex/`,
enable them in Appearance and press **Reload Custom Images**.

Update checker: set **Update URL** (Info page) to a JSON file like
`{"version": "1.0.1", "url": "https://your.download/page"}`.

## Modules (29)
- **HUD:** FPS, CPS, Ping, Coordinates, Keystrokes, Armor Status, Potion Effects, Target Info, Speed, Clock, Session Stats, Custom Text
- **Combat:** Combo Counter, Reach Display (measures your own hits)
- **Movement:** Toggle Sprint, Toggle Sneak, Auto Jump
- **Player:** Health Alert, Armor Alert, Death Coords
- **Render:** Fullbright, Zoom, Custom Crosshair
- **World:** World Clock (in-game time display), Clear Weather (client-side visual)
- **Utility:** Auto Respawn, Performance
- **Misc:** Window Title, Mute Unfocused

Fullbright, Zoom and Auto Respawn are allowed on many servers but not all; check yours.

## Project layout
```
core/     Config, ClientSettings, Theme, Notifications, UpdateChecker
setting/  Bool, Number, Mode, Color, String settings (all saved to JSON)
module/   ClientModule, HudModule, TextHud, ModuleManager + one class per module (hud/ combat/ ...)
gui/      MenuScreen (hub), HudEditorScreen
util/     Anim, RenderUtil (rounded rects, glow), Images, Cps, Info, Keys, SessionStats
(no mixins: CPS is polled per frame, the crosshair uses Fabric's HudElementRegistry.replaceElement)
```
Add a module: create a class extending `ClientModule` (or `TextHud`), add settings with `add(...)`,
register it in `ModuleManager.init()`. The menu, search, keybinds, config and profiles pick it up automatically.

## Notes (26.2 port)
Minecraft 26.x renamed a lot of rendering and input API (GuiGraphicsExtractor, extractRenderState, MouseButtonEvent / KeyEvent,
gui.setScreen, Mojang names). This port is written against the official 26.2 docs but has **not been compiled** by the author.
If the first build reports errors, the most likely places are:
- `VoidnexClient`: `HudElementRegistry.replaceElement` / `HudElement.extractRenderState` (crosshair), `KeyMapping.Category.register`
- `Images`: `DynamicTexture` constructor and `GuiGraphicsExtractor.blit` overloads
- `Performance`: `Options.graphicsMode()` / `ParticleStatus`, `ClearWeather`: `setRainLevel` / `setThunderLevel`
- `ToggleSneak`: `KeyMappingHelper.getBoundKeyOf(...).getValue()`, `SessionStats` / `CoordsHud`: `ResourceKey.identifier()`
- `WorldClock`: `ClientLevel.getDefaultClockTime()`
The old Time Changer module was removed: 26.1 replaced time-of-day with world clocks, which cannot be overridden safely without version-specific internals.

## Get the jar without installing anything (GitHub Actions)
1. Create a free GitHub account and a new empty repository.
2. Upload the contents of this folder (including the hidden `.github` folder) to it.
3. Open the **Actions** tab, wait for **Build VOIDNEX jar** to finish (about 3-5 minutes).
4. Open the finished run and download **voidnex-client-jar** under *Artifacts*. Unzip it to get `voidnex-client-1.0.0.jar`.
If the run fails, open it, copy the red error lines and send them over.
