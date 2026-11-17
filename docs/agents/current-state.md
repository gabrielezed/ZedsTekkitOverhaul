# Current implementation

**0.2.0-alpha.5** adds a Creative-spawned Enderkin Saboteur with approach and evasive blinks and interruptible machine disruption. Its single mod JAR supports specific BuildCraft, RedPower and EE2-to-IC2 lines through ZTO-owned transfer boundaries. Acceptance targets Tekkit Classic 3.1.2 singleplayer. [Industrial layouts and evidence](saboteur-industrial.md)

| Owner | Current responsibility |
| --- | --- |
| `mod_ZedsTekkitOverhaul`, `config` | Bootstrap and typed startup settings. Edit `minecraft/config/ZedsTekkitOverhaul.cfg`. [Configuration](configuration.md) |
| `lunar`, `lunar.client` | Eight-day calendar, natural events, alignment, replay watermark, save and dimension lifecycle. [Lunar contract](lunar-cycle-implementation.md) |
| `assault` | Full-moon PLAYER_ASSAULT with frozen regional wave and vanilla slot plans, first-quarter and waxing-gibbous patrols, bounded outdoor placement. [Alpha.4 pressure](survival-02-alpha4.md) |
| `territory` | Qualified activity sampling, bounded Overworld save data, lazy decay and biome-aware local threat. `assault` can use one frozen assessment at natural admission. [Alpha.2 contract](survival-02-alpha2.md) |
| `survival`, `equipment` | Local bed anchors, equipped mobs and tactical AI. Experimental Enderkin substitutions default to off. Creative eggs are available either way. The Saboteur prototype uses its own egg. [Saboteur guide](survival-02-alpha5-saboteur.md) |
| `creative`, `crafting`, `integration.inventory` | Asset-backed Creative UI, crafting assistance and legacy mouse dispatch. [Creative guide](../engineering/creative-inventory-backport.md) |
| `hopper`, `render`, `power`, `distillation`, `fluids`, `emc` | Experimental machines, Foundry visuals and pack adapters. Preserve IDs, NBT and economic behavior. [Integration boundaries](integrations.md) |
| `devbridge`, lunar debug views | Optional observations and gated disposable-world tools. [Diagnostics protocol](dev-bridge.md) |

Run `zto-tools/test-config.ps1` and `zto-tools/test-lunar-calendar.bat` for fast settings and lunar checks. Use `zto-tools/build-and-deploy.bat` for compilation, reobfuscation and packaging. Select the relevant silent Tekkit Dev probes from the [test map](testing.md).

Owner playtests still cover unrestricted Enderkin combat, powered RedPower entrances and live route selection. Dedicated multiplayer authority and pack variants need separate verification.
