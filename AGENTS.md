# Instructions for coding agents

This is a **legacy Minecraft 1.2.5** project. Read this file before modifying code, then consult [docs/agents/README.md](docs/agents/README.md) for task-specific context. For IC2, EE2, BuildCraft, Forge and other external APIs, use the [version-pinned Tekkit Classic 3.1.2 reference](docs/reference/tekkit-classic-3.1.2/README.md) and verify uncertain signatures in matching artifacts.

## Language policy

- Write **all repository content in English**: documentation, code comments, JavaDoc, string literals/messages added to code, scripts, tests, commit messages and pull request text.
- The project owner communicates in **Italian**; explain changes and ask questions in Italian unless requested otherwise.
- Preserve code behavior during language-only edits. Do not use English-only changes as an excuse to refactor unrelated logic.

## Compatibility and source rules

- Target: Tekkit Classic **3.1.2**, Minecraft **1.2.5**, MCP **6.2**, Forge **3.4.9.171**, FML **2.2.106.176**, JDK **8**, Eclipse, FoxyRetroMDK **1.3.6**.
- Write code compatible with the legacy Java and Minecraft APIs. Do not use modern Minecraft systems (BlockPos, capabilities, DeferredRegister, JSON recipes, ForgeGradle).
- Original mod sources belong under `src/minecraft/zed/`. Do not commit or overwrite vanilla Minecraft, Forge, mod JARs, decompilations, reobfuscated Minecraft classes, saves or local config files.
- Avoid editing core game classes. If an unavoidable hook is needed, isolate and validate a patch/transformer; never silently replace NEI-patched `gb.class` or other shared classes.
- Keep `mod_ZedsTekkitOverhaul.java` a small bootstrap. Place feature logic in dedicated packages and preserve existing EE2/IC2 behavior unless the task explicitly changes it.
- Never assume release IC2/EE2 JARs work in the deobfuscated MCP runtime. Review [environment.md](docs/agents/environment.md) first.
- Treat numeric block IDs, recipe behavior and EMC values as world-compatibility concerns. Avoid silent migrations.

## Project Vision

Before designing or implementing any feature, read `PROJECT_VISION.md` in the repository root.
Treat it as the authoritative reference for the project's game design philosophy, intended player experience, atmosphere, balancing principles, and cross-system integration requirements.
Distinguish confirmed design directions from proposals and unresolved decisions. Do not assume that features described in the vision document are already implemented.
Feature-specific technical specifications govern implementation details, but must remain consistent with the broader project vision.
When completing a task, explain how the implementation supports the project vision and which integration points have been preserved for future development.

## Player-facing interactions

- Present machine status, controls, transfer results, and actionable failures in an in-game GUI. Include units, capacity, and guidance where needed.
- Reserve the console and logs for debugging, developer diagnostics, and failures that cannot be shown in-game. Do not use chat as a substitute for a machine menu.
- Preserve direct gameplay interactions such as bucket transfers and held-block placement. Keep GUI state separate from machine processing and use the appropriate client/server boundary.

## Workflow and verification

1. Confirm the current Git branch, repository state and relevant source files.
2. Make the smallest coherent change. Separate documentation, behavior changes and migrations when practical.
3. Review diffs and run available checks. On the Windows MDK environment, compile with `recompile.bat`; reobfuscate with `reobfuscate.bat` before packaging.
4. For integrated testing use `zto-tools/build-and-deploy.bat` and the isolated Prism `Tekkit Dev` instance, **not** the owner's ordinary play instance.
5. Report **what was actually tested**, where and how. Never claim in-game success based only on reading code, stubs or compilation.
6. Use focused English commit messages (`feat(crafting): ...`, `fix(legacy): ...`, `docs(agents): ...`). Do not commit unrelated generated output.

## Handbook

- [Architecture](docs/agents/architecture.md)
- [Environment and build](docs/agents/environment.md)
- [Legacy compatibility](docs/agents/compatibility.md)
- [Integration boundaries](docs/agents/integrations.md)
- [Validation and acceptance](docs/agents/testing.md)
- [Development workflow](docs/agents/workflow.md)
- [Roadmap and decisions](docs/agents/roadmap.md)

## Lunar calendar and local diagnostics

- Read the [Lunar Cycle handoff](docs/agents/lunar-cycle-implementation.md) before changing calendar semantics, world ownership, or the native atlas hook.
- Keep the calendar independent of Minecraft client classes. Gameplay consumers should use immutable snapshots, event causes, and scheduling windows.
- Read the [Dev Bridge guide](docs/agents/dev-bridge.md) before extending its protocol. Keep reads allowlisted and bounded, execute game reads on the owning thread, and keep discovery credentials out of logs and repository files.
- Use the isolated Tekkit Dev instance and disposable saves for acceptance. Record player-observed results separately from focused Java checks and MCP builds.

## Legacy external APIs

- [Tekkit Classic 3.1.2 API index](docs/reference/tekkit-classic-3.1.2/README.md)
- [Source quality and licensing](docs/reference/tekkit-classic-3.1.2/sources.md)
- Do not copy or redistribute mod SDKs, decompiled files or production JARs into Git. API summaries do not replace a version-matched MCP compilation.

## Experimental fractional distillation

- [Fractional distillation V1 specification](docs/engineering/fractional-distillation-v1.md)
- [Distillation agent handoff](docs/agents/distillation.md)
- [Rotatable layout and visual connector reference](docs/engineering/distillation-rendering-and-binding-v2.md)
- The distillation prototype includes 3D assets, an IC2 LV heater, BuildCraft 2.2.14 liquid ports, four placeable fractions and local machine panels. See the handoff for the remaining runtime checks.
- Confirm exact IC2 1.97 ABI and reobfuscated runtime linking before merging any direct `ic2.api` dependency.
