# CodeChickenCore 0.5.3 rendering audit

## Artifact and source identity

The local Tekkit Dev copy's runtime classpath uses `bin/minecraft.jar` (SHA-256 `9808277d362d4f67ed5031b9d47afbe90f696125e06cc333342917719f0c3ad5`). The corresponding `bin/modpack.jar` overlay also carries CodeChickenCore (SHA-256 `0827ee55325fedbc37f2a6a890f19db11f75fab5e9a5c02eafd1bd50d2b59446`). No standalone CodeChickenCore archive was present in the copy's mods or coremods directories. The FML launch log discovers `mod_CodeChickenCore` in `minecraft.jar`. Its binary `Packager` annotation and decompiled entry point both report **0.5.3**. Its ModLoader priority is `before:*`. The inspected merged archive contains `codechicken/core/` and NEI's `codechicken/nei/`, with no `codechicken/lib/` entries.

Local references were found under the ignored `tekkit-mods-decompiles/mods/coremods/` catalog. Inspection used those reconstructed sources for behavior and Java 8 `jar tf` and `javap -c -v` on the merged runtime archive for signatures, bytecode and provenance. The local decompilation contains incomplete obfuscated type names. No third-party implementation or archive is included in ZTO.

## Verified rendering facilities

- `codechicken.core.CoreRender.addVecWithUV(Vector3, double, double)` forwards position and UV to `adz.a(DDDDD)V`. `addVec(Vector3)` forwards position to `adz.a(DDD)V`. Both use the vanilla Tessellator singleton `adz.a`. The caller owns draw mode, batching, normals, packed brightness, texture and GL state.
- `Vector3` contains mutable XYZ doubles and vector math. `Quat` provides axis-angle construction, quaternion composition, normalization and in-place vector rotation. These are math primitives with no model hierarchy, transform stack or animation player.
- `ItemCustomTexture` implements the pack's `forge.ITextureProvider` for an item texture filename and icon index. It provides no general mesh material system.
- `RenderCustomEndPortal` is a concrete layered portal effect using GL texture generation, blending and separate Tessellator draws. It is tailored to its effect and render environment.
- `ForgeManagerClient.load()` and `ForgeManager.load()` are empty in the client artifact. ModLoader entity renderer registration and Forge/TESR hooks remain the applicable integration points.
- RedPower Core 2.0pr5b2 supplies its own `eloraam.core.RenderModel`, `RenderContext`, `RenderLib` and math utilities. That is a separate mod-owned pipeline. The inspected CodeChickenCore inventory contains no general OBJ loader, mesh model, light model, render-state abstraction, entity animation system or batching scheduler.

## Comparison

| Area | ZTO implementation | Legacy facility | Direction |
| --- | --- | --- | --- |
| Geometry | `ObjLoader` triangulates faces into immutable, named `ObjModel` parts. `ModelBiped` uses native cuboids. | `CoreRender` submits one vertex at a time. | Keep native entity models and the existing OBJ parser for machines. |
| Texturing | OBJ UVs and PNG atlases through TESR, native entity and held-item textures for mobs. | UV forwarding and a Forge item texture provider. | Keep each caller's binding path. |
| Transformations | GL matrix scopes for rotor, facing and entity scaling. | `Vector3` and `Quat` can transform individual vectors. | Keep current transforms. Revisit quaternion use when an actual model needs composition. |
| Animation | Foundry interpolates a rigid rotor. Tactical mobs use native walking, look, swing and bow pose. | No general animation consumer. | Keep rigid parts and native biped animation. |
| Lighting | Explicit per-vertex normals and packed lightmap for OBJ. Distillation samples world light. Foundry is a full-bright preview. | The forwarding methods leave lighting to the caller. | Keep explicit brightness. Validate world lighting when revisiting the Foundry preview. |
| Performance | Cached parsed meshes. One Tessellator draw per OBJ part, including attachments. Native biped display lists. | Same vertex submission and caller-owned draw count. | An adapter leaves batch count unchanged and may require vector conversion. No comparative frame-time benefit was measured. |
| Entities | ModLoader renderer map, `RenderLiving` transforms and state, `RenderBiped` held item rendering. | Portal effect and generic math have no living renderer contract. | Use the pack's native biped path for these prototypes. |
| Tile entities | Foundry and distillation use TESR. Distillation scopes GL and client arrays around machine geometry. | One specialized portal renderer. RedPower uses separate classes. | Keep the existing TESR and isolate each effect. |
| Compatibility | Pack-obfuscated vanilla and Forge hooks. Lunar launch agent patches the RenderGlobal moon atlas binding. Inventory agent serves inventory input. | Core loads early from the overlay. Direct linkage adds a pack-specific compile and runtime dependency. | Keep the agents and third-party overlay boundaries. |
| Maintenance | Original source and assets with focused OBJ and lightmap tests. | Thin wrapper over an already used singleton. | Retain ZTO's pipeline and defer adapters until a concrete benefit can be measured. |

The tactical mobs exercise native animated entity rendering. This milestone adopts separate entity and machine rendering approaches. It adds no CodeChicken dependency. A future industrial performance pass should first count draws and vertices per assembled machine and compare frame time on the same scene. Machine GPU timing, shader interactions and a complete inventory of rendering utilities across every installed mod remain unverified.
