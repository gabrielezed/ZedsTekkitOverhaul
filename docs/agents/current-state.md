# Current implementation

The `0.5.1-alpha.2` candidate generates Abyssal Trench terrain in new eligible Default Overworld ocean chunks. Continuous world-space fields shape connected channels and basins. Geological coatings and marine colonies use native extended block storage. The shaping plan constrains cuts across a native terrain halo. The mask reduces native water fog density and lifts the underwater lightmap in the pinned renderer. Owner graphical acceptance is pending. [World generation and owner walkthrough](../engineering/abyssal-worldgen.md) · [Abyssal decoration](../engineering/abyssal-decoration.md).

The `0.5.1-alpha.1` candidate adds underwater equipment and an IC2 Air Compressor for ordinary water. It adds [coupled Distillation vessels and operating cues](../engineering/distillation-industrial-presentation.md), a [build watermark](../engineering/build-watermark.md) and a shared [artwork workspace](../../artwork/README.md). The default build restores the approved private machine textures through the [local-dev artwork profile](../engineering/local-artwork-recovery.md). [Implementation and owner walkthrough](survival-051-alpha1.md).

**Survival 0.2.2 — Aerial Ecology & Atmospheric Hazards** added Skelebird bone drops and strengthened Electric Jetpack faults while retaining powered descent braking. The supported target is Tekkit Classic 3.1.2 singleplayer. [Jetpack verification](survival-022-aerial-predator.md#verification) · [Skelebird](survival-022-skelebird.md)

Survival `0.4.0-alpha.4` made ground siege actors persist across tactical release and native saves. [Battlefield lifecycle and owner walkthrough](survival-040-alpha4.md). [Industrial infiltration and siege lifecycle](survival-040-alpha3.md). Natural site sieges and the local Debug Hub use the same site-based tactics. A force scales with threat at the selected site and admission difficulty. Infantry and Enderkin deploy in bounded waves around one verified site objective. Lit and covered exterior routes, supported bridge turns and one-block steps can connect the force to a recognized waterfront site. The monitor can call and cancel a test siege in daylight with a separate intensity override. [Architecture and owner walkthrough](survival-040-alpha2.md)

The completion candidate adds bounded route reassessment, limited persistent Skelebird support, a recent battlefield census and default natural admission for configurations without an explicit setting. The version remains alpha pending the [completion acceptance matrix and owner campaign](survival-040-acceptance.md).

Survival 0.3.0-alpha.4 established the recognition contract. Settlement Recognition can link corroborated parts across short courtyards and retains stable IDs from earlier saves. Protected Interior reports local geometry separately. [Implementation, acceptance and 0.4 handoff](survival-030-alpha4.md)

Settlement surveys recognize mixed-mod equipment and successful local device use through native block, TileEntity and inventory capabilities. Matching placement records can establish unfamiliar construction. The pinned IC2 1.97 adapter adds precise local copper wiring evidence. Construction still defines site boundaries. [Generic evidence and owner walkthrough](settlement-generic-modded-evidence.md) · [IC2 wiring](settlement-industrial-recognition.md)

Strong clipped structures can receive an ID with partial coverage. Continuous visits can extend that identity through directly overlapping construction observations, including large factories. [Partial footprint contract and reproduction](settlement-partial-footprints.md)

Survival 0.2.3 added physical industrial sabotage, furnace fuel priority and a wooden-entrance tactic for the Breacher. The fieldwork correction suspends vein mining and restores native extraction. Biome ecology favors ordinary Enderkin underground and gives surface magpies occasional one-item theft. The alpha.1 Fugl chicken hunt remains available. [Industrial Saboteur](saboteur-industrial.md) · [Mining, furnaces and entrances](survival-023-alpha2-fieldwork.md) · [Biome ecology](survival-023-implementation.md)

| Owner | Current responsibility |
| --- | --- |
| `mod_ZedsTekkitOverhaul`, `config` | Bootstrap and typed startup settings. Edit `minecraft/config/ZedsTekkitOverhaul.cfg`. [Configuration](configuration.md) |
| `lunar`, `lunar.client` | Eight-day calendar, natural events, alignment, replay watermark, save and dimension lifecycle. [Lunar contract](lunar-cycle-implementation.md) |
| `assault` | Shared major admission, site-bound tactics and local test siege commands, full-moon PLAYER_ASSAULT with frozen regional wave and vanilla slot plans, first-quarter and waxing-gibbous patrols, bounded outdoor placement. [Site tactics](survival-040-alpha2.md) · [Lunar placement](survival-021-implementation.md#lunar-placement-on-irregular-terrain) |
| `territory` | Qualified activity sampling, bounded Overworld save data, lazy decay and biome-aware local threat. Ordinary hostile caps can rise with blended activity. `assault` can use one frozen assessment at natural admission. [0.2.1 implementation](survival-021-implementation.md) |
| `settlement` | Manual and Quarry provenance, loaded construction surveys, site registry and independent local protection. Recognition reads Territorial Activity. [0.3 implementation](survival-030-implementation.md) |
| `survival`, `equipment` | Local bed anchors, equipped mobs and five Enderkin roles. Ordinary Enderkin substitutions default to on with the matching agent. Lunar substitutions are opt-in. All five Creative eggs remain available. [Enderkin ecology](survival-021-implementation.md#ordinary-enderkin-ecology) |
| `survival.aerial` | Fugl wildlife and staggered dives, Skelebird nocturnal crossing passes, bounded full-moon air slots and Electric Jetpack weather faults. [Fugl and jetpack](survival-022-aerial-predator.md) · [Skelebird](survival-022-skelebird.md) |
| `creative`, `crafting`, `integration.inventory` | Asset-backed Creative UI, crafting assistance and legacy mouse dispatch. [Creative guide](../engineering/creative-inventory-backport.md) |
| `hopper`, `render`, `power`, `distillation`, `fluids`, `emc` | Experimental machines, Foundry visuals and pack adapters. Preserve IDs, NBT and economic behavior. [Integration boundaries](integrations.md) |
| `exploration.underwater` | Diving equipment, persistent item oxygen, water-specific hooks and the LV Air Compressor. [Underwater equipment](survival-051-alpha1.md) |
| `exploration.motorboat` | Gasoline-powered travel, recoverable fuel tank, native boat hull and fueling container. [Motorboat](../engineering/motorboat.md) |
| `devbridge`, lunar debug views | Numpad 1 opens the local Debug Hub. Dev Bridge provides optional observations and gated disposable-world tools. [Diagnostics protocol](dev-bridge.md) |

Run `zto-tools/test-config.ps1` and `zto-tools/test-lunar-calendar.bat` for fast settings and lunar checks. Use `zto-tools/build-and-deploy.bat` for compilation, reobfuscation and packaging. Select the relevant silent Tekkit Dev probes from the [test map](testing.md).

See the [compatibility evidence](compatibility-acceptance.md) for live behavior and the [owner checklist](survival-021-integration.md#owner-verification) for graphical follow-up. Dedicated multiplayer authority and pack variants need separate verification.
