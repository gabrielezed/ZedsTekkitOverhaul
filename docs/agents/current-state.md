# Current implementation

**Survival 0.2.2 — Aerial Ecology & Atmospheric Hazards** is complete on `main`. Its final stabilization adds Skelebird bone drops and strengthens Electric Jetpack faults while retaining powered descent braking. The supported target is Tekkit Classic 3.1.2 singleplayer. [Jetpack verification](survival-022-aerial-predator.md#verification) · [Skelebird](survival-022-skelebird.md)

The 0.2.3 development branch identifies itself as `0.2.3-alpha.2`. It adds physical industrial sabotage, timed vein mining, furnace fuel priority and a wooden-entrance tactic for the Breacher. The alpha.1 prototype introduced biome-weighted ordinary Enderkin, forest magpies and occasional Fugl predation on exposed chickens. [Industrial Saboteur](saboteur-industrial.md) · [Mining, furnaces and entrances](survival-023-alpha2-fieldwork.md) · [Biome ecology prototype](survival-023-implementation.md)

| Owner | Current responsibility |
| --- | --- |
| `mod_ZedsTekkitOverhaul`, `config` | Bootstrap and typed startup settings. Edit `minecraft/config/ZedsTekkitOverhaul.cfg`. [Configuration](configuration.md) |
| `lunar`, `lunar.client` | Eight-day calendar, natural events, alignment, replay watermark, save and dimension lifecycle. [Lunar contract](lunar-cycle-implementation.md) |
| `assault` | Full-moon PLAYER_ASSAULT with frozen regional wave and vanilla slot plans, first-quarter and waxing-gibbous patrols, bounded outdoor placement. [Lunar placement](survival-021-implementation.md#lunar-placement-on-irregular-terrain) |
| `territory` | Qualified activity sampling, bounded Overworld save data, lazy decay and biome-aware local threat. Ordinary hostile caps can rise with blended activity. `assault` can use one frozen assessment at natural admission. [0.2.1 implementation](survival-021-implementation.md) |
| `survival`, `equipment` | Local bed anchors, equipped mobs and five Enderkin roles. Ordinary Enderkin substitutions default to on with the matching agent. Lunar substitutions are opt-in. All five Creative eggs remain available. [Enderkin ecology](survival-021-implementation.md#ordinary-enderkin-ecology) |
| `survival.aerial` | Fugl wildlife and staggered dives, Skelebird nocturnal crossing passes, bounded full-moon air slots and Electric Jetpack weather faults. [Fugl and jetpack](survival-022-aerial-predator.md) · [Skelebird](survival-022-skelebird.md) |
| `creative`, `crafting`, `integration.inventory` | Asset-backed Creative UI, crafting assistance and legacy mouse dispatch. [Creative guide](../engineering/creative-inventory-backport.md) |
| `hopper`, `render`, `power`, `distillation`, `fluids`, `emc` | Experimental machines, Foundry visuals and pack adapters. Preserve IDs, NBT and economic behavior. [Integration boundaries](integrations.md) |
| `devbridge`, lunar debug views | Numpad 1 opens the local Debug Hub. Dev Bridge provides optional observations and gated disposable-world tools. [Diagnostics protocol](dev-bridge.md) |

Run `zto-tools/test-config.ps1` and `zto-tools/test-lunar-calendar.bat` for fast settings and lunar checks. Use `zto-tools/build-and-deploy.bat` for compilation, reobfuscation and packaging. Select the relevant silent Tekkit Dev probes from the [test map](testing.md).

See the [compatibility evidence](compatibility-acceptance.md) for live behavior and the [owner checklist](survival-021-integration.md#owner-verification) for graphical follow-up. Dedicated multiplayer authority and pack variants need separate verification.
