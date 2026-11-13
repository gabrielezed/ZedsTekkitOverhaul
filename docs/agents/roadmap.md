# Roadmap and engineering decisions

This is a **directional design plan**, not a list of already implemented features. Scope can change after in-game prototypes.

## Priority order

1. **Foundation:** reproducible MCP build/deployment, documented dependencies and regression checks.
2. **Rendering prototype:** load an externally authored model in a practical format, with simple animation (e.g. a rotating rigid part). No Create-style physics engine.
3. **Machine architecture:** TileEntity state, NBT persistence, GUI synchronization and reusable recipe/energy boundaries.
4. **Foundry:** validate a multiblock structure, then processing recipes, IC2 EU usage and simple animated machinery. Exact dimensions/design should be specified before coding.
5. **EE2/IC2 interplay:** measured conversion between alchemical and electrical systems, with EMC balance and loop analysis.
6. **BuildCraft oil processing:** simple distillation first, larger modular refinery later; consider fuel/electric/alchemical heating adapters.
7. **Longer-term gameplay:** higher PvE pressure, progression-gated exploration, optional dangerous biomes and better legacy inventory/creative UI.

## Recorded decisions

| ID | Decision | Rationale |
| --- | --- | --- |
| D-001 | Keep original project sources under `src/minecraft/zed/`. | Works in the existing FoxyRetroMDK tree without bundling third-party source. |
| D-002 | Prefer extension points and isolated adapters over replacing vanilla/NEI classes. | Reduce runtime incompatibilities and fragility across iterations. |
| D-003 | Test integration in an isolated Prism Tekkit instance. | Release mod JAR mappings differ from deobfuscated MCP. |
| D-004 | Keep Java/bootstrap/feature modules separate. | Allow targeted changes and regressions. |
| D-005 | Do not build a Create-like kinetic physics simulator. | Visual animation and multiblocks are sufficient for current design goals. |
| D-006 | Treat EMC and block IDs as economic/save-compatibility inputs. | Avoid game-breaking exploits and damaged worlds. |
| D-007 | Preserve English repository content, Italian conversation. | Make code and agent handoffs consistent and broadly readable. |

## Verified foundations and current prototype

The Hopper backport, crafting and storage controls, fractional distillation prototype and local lunar calendar are implemented. Acceptance is tracked per workflow in the [singleplayer acceptance matrix](singleplayer-acceptance.md). Prioritize repeatable runtime coverage, save/reopen and pack integration before expanding progression or adding new gameplay systems.

- **Confirmed in Tekkit Dev by the owner:** OBJ mesh rendering, a rotating rotor, right-click pause/resume, NBT persistence for multiple Foundry blocks, and the corrected full-bright OBJ colors.
- **Under development on a feature branch:** a reusable relative-offset structure pattern with a provisional flat 3×3 Stone ring around the Foundry controller. This is **not** the final multiblock design and still requires a real-game check. See [multiblock validator](multiblock-validator.md).

## Proposals requiring tests before adoption

- Hardening the working OBJ prototype for production (world-aware lighting, asset error handling and performance tests).
- Full IC2/EE2 deobfuscated development libraries.
- Broader live compatibility and gesture coverage for the existing opt-in inventory drag agent.
- Multiplayer networking and container synchronization.
- Standardized machine energy/heat/fluid adapter interfaces.

Avoid prematurely implementing frameworks for untested requirements. Prove each shared abstraction through one machine before broad adoption.

## Survival gameplay

- **Implemented for 0.1:** [lunar player assault](survival-assault-acceptance.md) follows the active player's vicinity. Tune the warning, distance and wave cadence with a short Survival playtest.
- **Implemented in `0.1.0-alpha.3`:** opt-in hand and armor slots for vanilla Zombies and Skeletons with rendering, combat effects, entity NBT and death drops. [Specification](MOB_EQUIPMENT_BACKPORT_SPEC.md) · [Tekkit Dev evidence](mob-equipment-acceptance.md)
- **Alpha.5 candidate:** [encounter loadouts, respawn bed anchors and armor Shift+Click](survival-alpha5.md). Owner feedback will guide later tactical enemy roles and defenses.
- **Alpha.6 candidate:** [outdoor lunar pressure and local fortification](survival-alpha6.md). Evaluate the placement model across representative shelters and Tekkit compounds before expanding tactical roles.
- **Later release lines:** territorial activity and threat in 0.2, settlement recognition in 0.3, and `SITE_SIEGE` in 0.4. A site siege targets a persistent settlement inferred from long-term construction and activity. Doors, beds and machines are optional evidence. The plan supports caves, treehouses, castles, outdoor industry and multiple outposts without claims or marker blocks.
- **Optional developer feature, deferred:** an opt-in in-game Dev Bridge panel may show scenario, step, simulation tick and PASS/FAIL. Keep it off by default and independent of gameplay.

## Fractional distillation prototype

The seven-placement prototype has original OBJ meshes, a thermometer selector, cooling-water tank and an IC2 1.97 low-voltage heater. Gasoline, Kerosene, Diesel and Heavy Gas Oil have placeable world blocks, buckets and BuildCraft liquid identities. See the [engineering specification](../engineering/fractional-distillation-v1.md) for setpoints, batch rules, IDs and structure.

**Immediate next iteration:** verify the component panels, bucket feedback, pipe and tank conservation, and save/reopen behavior for each fraction. Engine fuel registration for the ZTO fractions is a separate progression decision. See the [distillation handoff](distillation.md).
