# ZedsTekkitOverhaul

## Survival 0.2.1 · The Enderkin bestiary

A **Minecraft 1.2.5** overhaul for **Tekkit Classic 3.1.2**. Territory grows more dangerous as you spend time in it, full moons bring assaults, and five Enderkin turn familiar ground into a reason to prepare, fortify, and explore.

![The five Enderkin in Survival 0.2.1: Skirmisher, Breacher, Tank, Thief and Saboteur](docs/agents/images/enderkin-021-lineup.png)

*The full bestiary, rendered from the same models and textures used in the game.*

| Enderkin | Encounter |
| --- | --- |
| **Skirmisher** | Repositions with short blinks and telegraphs a ranged Ender Pulse. |
| **Breacher** | Closes distance, pressures entrances, and can reach low platforms. |
| **Tank** | Commits to heavy frontal strikes. On Hard it can open localized breaches in suitable walls when pursuit stalls. |
| **Thief** | Steals an item from your inventory or hotbar and flees. Recover the loot by intercepting it or finding its cache. Sneak with a vanilla compass for a temporary **Ender Trail** toward the last recorded location. |
| **Saboteur** | Briefly disrupts supported machinery. It can also dismantle an exposed, empty BuildCraft stone or cobblestone item pipe and flee with the recoverable component. |

All five have Creative spawn eggs and can appear in ordinary Overworld Survival with ZTOAgent. Press **Numpad 1** in singleplayer to open the Debug Hub and inspect current territorial activity, ordinary hostile pressure, and lunar encounter decisions. [Survival guide](docs/agents/survival-021-implementation.md) · [Changelog](CHANGELOG.md) · [Integration results](docs/agents/survival-021-integration.md)

## Gameplay

- **Territorial threat** raises the ordinary hostile cap with sustained activity. Activity decays while you are away, and ordinary spawning still checks terrain and lighting.
- **Lunar Cycle** persists eight phases per save. Full-moon assaults and smaller patrols occur on natural lunar boundaries. Terrain, cover, and available population space shape each encounter. [Lunar Cycle](docs/agents/lunar-cycle-implementation.md)
- **Industry and alchemy** include directional Hoppers, IC2-powered fractional distillation with BuildCraft connections, and an Alchemical Converter that feeds IC2 from EE2 collectors. [Hopper](docs/engineering/hopper-backport.md) · [Distillation](docs/agents/distillation.md) · [Alchemical power](docs/engineering/alchemical-power-v1.md) · [EMC profiles](docs/engineering/emc-profile.md)
- **Inventory and crafting** include a Creative inventory with tabs and search, recipe preparation, drag distribution, and Shift-click armor equip in supported screens. [Creative inventory](docs/engineering/creative-inventory-backport.md) · [Inventory gestures](docs/engineering/inventory-drag.md)

## Install on Tekkit Classic

Use **Tekkit Classic 3.1.2** and **Java 8**. Close Prism and Minecraft, then install the two matching build artifacts into **Tekkit Dev** from the workspace root.

```powershell
.\zto-tools\install-zto-prism.ps1 -Instance "$env:APPDATA\PrismLauncher\instances\Tekkit Dev"
```

The installer adds `ZedsTekkitOverhaul.jar` to the instance and registers `ZTOAgent.jar` as a managed Prism component. The agent enables territorial ordinary spawning, natural Enderkin substitutions, custom moon rendering, inventory gestures, and supported direct industrial hooks. [Installation and staging](docs/deploy-prism.md)

Edit `minecraft/config/ZedsTekkitOverhaul.cfg` with Minecraft closed to tune territorial pressure, ordinary Enderkin spawning, and lunar encounters. [Settings and migration](docs/agents/configuration.md)

## Build from source

Use **FoxyRetroMDK 1.3.6**, **MCP 6.2**, and **JDK 8**. The development environment uses Forge **3.4.9.171**. The supported Tekkit pack runs Forge **3.3.8.164** and FML **2.2.106.176**.

```powershell
.\zto-tools\build-and-deploy.bat
```

The script places both JARs in `dist-zto` and installs them to **Tekkit Dev** by default. Set `ZTO_PRISM_MODS` to an existing staging directory to build outside the Prism profile. Keep the original obfuscated Tekkit mod JARs in the separate pack runtime. [Environment guide](docs/agents/environment.md)

## Project resources

- [Project vision](PROJECT_VISION.md) and [contributor handbook](docs/agents/README.md)
- [Survival playtest checklist](docs/agents/survival-021-integration.md#owner-verification) and [compatibility evidence](docs/agents/compatibility-acceptance.md)
- [Developer diagnostics and disposable-world testing](docs/agents/test-automation.md)
- [Tekkit Classic API reference](docs/reference/tekkit-classic-3.1.2/README.md) and [asset credits](CREDITS.md)
