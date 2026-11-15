# Current implementation

**0.2.0-alpha.1** adds local Overworld activity and experimental threat-based assault budgets after Survival Foundations. Acceptance targets Tekkit Classic 3.1.2 singleplayer. [Environment and build](environment.md)

| Owner | Current responsibility |
| --- | --- |
| `mod_ZedsTekkitOverhaul`, `config` | Bootstrap and typed startup settings. Edit `minecraft/config/ZedsTekkitOverhaul.cfg`. [Configuration](configuration.md) |
| `lunar`, `lunar.client` | Eight-day calendar, natural events, alignment, replay watermark, save and dimension lifecycle. [Lunar contract](lunar-cycle-implementation.md) |
| `assault` | Full-moon PLAYER_ASSAULT, first-quarter and waxing-gibbous patrols, bounded outdoor placement. [Alpha.8 and recurrence](survival-alpha8.md) |
| `territory` | Sparse activity sampling, bounded Overworld save data, lazy decay and local threat. `assault` can use one frozen assessment at natural admission. [Alpha.1 contract](survival-02-alpha1.md) |
| `survival`, `equipment` | Local bed anchors, equipped mobs and tactical AI. Experimental Enderkin substitutions default to off. Creative eggs are available either way. [Tactical guide](survival-alpha7.md) |
| `creative`, `crafting`, `integration.inventory` | Asset-backed Creative UI, crafting assistance and legacy mouse dispatch. [Creative guide](../engineering/creative-inventory-backport.md) |
| `hopper`, `render`, `power`, `distillation`, `fluids`, `emc` | Experimental machines, Foundry visuals and pack adapters. Preserve IDs, NBT and economic behavior. [Integration boundaries](integrations.md) |
| `devbridge`, lunar debug views | Optional observations and gated disposable-world tools. [Diagnostics protocol](dev-bridge.md) |

Run `zto-tools/test-config.ps1` and `zto-tools/test-lunar-calendar.bat` for fast settings and lunar checks. Use `zto-tools/build-and-deploy.bat` for compilation, reobfuscation and packaging. Select the relevant silent Tekkit Dev probes from the [test map](testing.md).

Owner playtests still cover unrestricted Enderkin combat, powered RedPower entrances and live route selection. Dedicated multiplayer authority and pack variants need separate verification.
