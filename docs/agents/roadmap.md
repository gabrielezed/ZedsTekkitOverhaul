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

## Proposals requiring tests before adoption

- An external model format/loader (OBJ is one candidate, **not yet a settled implementation**).
- Full IC2/EE2 deobfuscated development libraries.
- A production-safe bytecode transformer for inventory drag.
- Multiplayer networking and container synchronization.
- Standardized machine energy/heat/fluid adapter interfaces.

Avoid prematurely implementing frameworks for untested requirements. Prove each shared abstraction through one machine before broad adoption.
