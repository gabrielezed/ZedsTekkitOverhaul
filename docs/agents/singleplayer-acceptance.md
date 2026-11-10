# Milestone 1 singleplayer acceptance

## Provenance

- **Date:** 2026-11-10. Local `main` and `origin/main` both pointed to `73cdf9c1f462ff71c99075bdfc4738b7964447db`, the PR #17 merge. The acceptance candidate includes the local bridge changes over that baseline.
- **Candidate:** `b2929d8db30d655c2cd13d49e4763c071701b06a196218b1de52c1f419440163`. MCP client compilation and reobfuscation passed under Adoptium JDK 8.0.504.1, and the ZIP verifier read 219 entries. The staged and deployed file hashes matched the code-source hash returned by the running bridge. Build log at `dist-zto/milestone1-build.log`.
- **Redstone follow-up:** `56baf284f645c301d9643c7b1cb5d596e52dbfc472582961608fe1c90b374a30`. MCP client compilation, reobfuscation and streaming verification passed. Build log at `dist-zto/milestone1-redstone-build.log`. The live scenario results below identify the earlier build.
- **Distillation follow-up:** `e8b9b398d658f130cc5c73959139365c4ad8f15c975f4bd13c07f45641c29201`. The running bridge matched the deployed MCP-compiled and reobfuscated candidate. Build log at `dist-zto/milestone1-distillation-build.log`. Distillation and GUI observations below use this build.
- **Runtime:** isolated PrismLauncher 11.1.1 Tekkit Dev, Minecraft 1.2.5, FML 2.2.106.176, Forge 3.3.8.164, Java 8u51. Installed artifacts identify IC2 1.97, BuildCraft 2.2.14, RedPower 2 PR5B2, EE2 1.4.6.5, IronChests 3.8.0.40 and Inventory Tweaks 1.41b. The core overlay contains NEI 1.2.2.4 per the pinned local pack audit. MCP initialized Forge 3.4.9.171 during the headless crafting check. Tekkit Dev uses its own modpack overlay.

## Coverage matrix

Live results in this table refer to the candidate and runtime above. Owner observations refer to earlier builds. A portable PASS applies to the specific assertions named in the evidence column. All four gameplay subsystems are implemented. Scenario-specific gaps are listed below.

| Feature | Scenario | Portable | Live | Pack integration | Visual | Owner | Evidence and limits |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Hopper | HOPPER-01 downward transfer | PASS | PASS | NOT_APPLICABLE | NOT_APPLICABLE | NOT_RUN | `954256515276138.json`. Four stones transferred in 26 simulation ticks, zero sampled drops, reset and cleanup verified. Baseline JAR also passed in `344264174459423.json`. |
| Hopper | HOPPER-02 redstone lock and resume | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_APPLICABLE | NOT_RUN | Scenario implemented with an owned vanilla torch and stone support. Command schema checks passed. Runtime assertions remain open. |
| Hopper | HOPPER-03 partial then full destination | PASS | PASS | NOT_APPLICABLE | NOT_APPLICABLE | NOT_RUN | `909610751355446.json`. Chest 1726 → 1728, two of four stones retained in the Hopper after 82 ticks, zero sampled drops, cleanup complete. |
| Hopper | HOPPER-04 loaded inventory, facing and mode across reload | PASS | NOT_RUN | NOT_APPLICABLE | NOT_APPLICABLE | NOT_RUN | Portable NBT round trip. Reopening a save with an already cleaned arena only checks that the next fixture can run. |
| Hopper | HOPPER-05 furnace, IC2 Generator, IronChest and BuildCraft wooden item pipe | PASS | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_RUN | Portable furnace and chest rules only. Earlier owner observations reported IC2/EE2 interoperability and an IronChest failure before the adapter follow-up. |
| Hopper | HOPPER-06 placement, pickup, GUI, breaking, model and collision | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Manual checklist and prior reported issues in the [Hopper handoff](../engineering/hopper-backport.md). |
| Distillation | DIST-01 formation, orientation, independent layout | PASS | NOT_RUN | NOT_RUN | NOT_APPLICABLE | MANUAL_ACCEPTED | 44 layout assertions. Prior owner observed formation. Live negative/revalidation cases remain open. |
| Distillation | DIST-02 gasoline batch | PASS | PASS | PASS | NOT_APPLICABLE | MANUAL_ACCEPTED | `160717546507312.json`. Seven real components formed. In 162 observed ticks, 250 mB oil, 40 mB coolant and 1280 EU were consumed. The receiver delivered 250 mB of gasoline (liquid ID 3017). Cleanup complete. Integration coverage is limited to direct BuildCraft liquid and IC2 LV endpoint calls. |
| Distillation | DIST-03–04 starvation and other fractions | PASS | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_RUN | Covered by the portable process harness. Live cases remain open. |
| Distillation | DIST-05 BuildCraft fluid transport | NOT_RUN | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_RUN | Requires physical oil/water pipe input and receiver extraction. |
| Distillation | DIST-06 partial batch across reload | PASS | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_RUN | Portable state restoration. Real NBT and multiblock rebind remain open. |
| Distillation | DIST-07 panels and inspector parity | PASS | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | 31770 model assertions apply to assets. Heater inspector is built and deployed. Live machine snapshot and panel comparison remain open. |
| Crafting | CRAFT-01 3×3 result collection | PASS | PASS | PASS | PASS | MANUAL_ACCEPTED | In the disposable Creative save, NEI supplied 64 vanilla workbenches. Four were placed in the 3×3 grid and consumed to collect one Advanced Crafting Table. The bridge then reported 60 workbenches and one advanced table in `milestone1-before-reload.json`. Integration here covers NEI item search and provisioning. |
| Crafting | CRAFT-02–03 recipe transfer, missing ingredients and batches | PASS | NOT_RUN | NOT_RUN | NOT_RUN | MANUAL_ACCEPTED | MCP workbench slots and result preview, missing ingredient refusal and planner quantities. |
| Crafting | CRAFT-04 advanced 5×5 | PASS | NOT_RUN | PASS | PASS | MANUAL_ACCEPTED | The live table opened and NEI showed the Foundry recipe. The prepare attempts covered Creative and Creative Plus. NEI requires a second mode-button click to enter Survival from Creative. Live Survival output collection and Inventory Tweaks compatibility remain open. |
| Inventory | CRAFT-05–06 slot protection, storage and overflow | PASS | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Protection rules and storage planner checks. Live Inventory Tweaks locks and full NBT-preserving transfers remain open. |
| Inventory | CRAFT-07 drag distribution | PASS | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Planner and slot policy checks plus drag-agent bytecode probe against Tekkit Dev archives. Actual mouse gesture coverage remains open. |
| Inventory | CRAFT-08 IC2, RedPower and other modded containers | PASS | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Portable target policy only. Live clicks and per-container compatibility remain open. |
| Lunar | LUNAR-01–03 phase order, fresh and legacy alignment | PASS | NOT_RUN | NOT_APPLICABLE | MANUAL_ACCEPTED | MANUAL_ACCEPTED | Calendar harness. Earlier Tekkit Dev showed new-save data and legacy alignment. Owner verified all visible phases. |
| Lunar | LUNAR-04 and LUNAR-09 natural boundary and listener | PASS | PASS | NOT_APPLICABLE | NOT_APPLICABLE | MANUAL_ACCEPTED | `474224864946997.json` verified one `NIGHT_STARTED` with cause `NATURAL` at raw tick 157000 after reopening the save, with zero dropped events. `milestone1-lunar-observed.json` covers the earlier night end and dawn phase change. Natural full-moon arrival and disposable listener registration remain open. Owner observed the natural full-moon diagnostic. |
| Lunar | LUNAR-05 clock edits and suppression | PASS | NOT_RUN | NOT_RUN | NOT_APPLICABLE | MANUAL_ACCEPTED | Portable time manipulation. Earlier logs recorded admin and external time changes. |
| Lunar | LUNAR-06 real bed sleep | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_APPLICABLE | NOT_RUN | Bed-based advancement still open. |
| Lunar | LUNAR-07 persistence and world switching | PASS | PASS | NOT_APPLICABLE | NOT_APPLICABLE | MANUAL_ACCEPTED | After Save and Quit, automated clicks on Play Selected World left the client at Select World. Double-clicking the marked save row successfully reopened it. The bridge preserved logical day 5 and phase 5, assigned world/session 2, and rejected the old cursor with `STALE_CURSOR`. See `milestone1-lunar-reopen.json`. |
| Lunar | LUNAR-08 native atlas | PASS | NOT_RUN | NOT_APPLICABLE | MANUAL_ACCEPTED | MANUAL_ACCEPTED | Transformer probe against actual overlay. Owner visual pass on earlier build. |
| Cross-system | Disabled bridge, unmarked save, wrong build and ordinary gameplay | PASS | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_RUN | Transport, command schema and dispatch checks passed. PR #17 records earlier disabled/unmarked evidence. |
| Cross-system | NEI, Inventory Tweaks, Hopper restrictions, EE2/IC2, distillation and sleep | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Pack startup reached a disposable Creative save. Combined gameplay workflows remain open. |
| Survival | Unassisted smoke path | NOT_APPLICABLE | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | The seeded Hopper scenario counts as a runtime fixture. |

Runtime artifacts are under the ignored `dist-zto/test-reports/` directory. Portable commands were `test-dev-bridge.ps1`, `test-hopper.ps1`, `test-crafting-ux.bat`, `test-drag-agent.ps1`, `test-distillation-core.bat`, `test-distillation-layout.bat`, `test-distillation-models.bat`, `test-lunar-calendar.bat` and `test-lunar-agent.bat`, all under `zto-tools/`.

The first `singleplayer-core` invocation exited 2 at world selection before creating any fixture. After reopening the marked save, `suite singleplayer-core -LunarWaitSeconds 120` passed all five registered scenarios and exited 0 on the distillation candidate. See `milestone1-lunar-suite-reopened.json` for the per-scenario results and report paths.

## Survival smoke path

1. Start a fresh disposable Survival save in Tekkit Dev with test writes disabled. Record seed, build hash and runtime versions.
2. Gather wood, stone, coal and iron in Survival. Use NEI to find ordinary recipes, prepare a workbench recipe from gathered materials and collect it. Make the Advanced Crafting Table when resources allow and check one Foundry preview.
3. Craft a Hopper and chest. Transfer a counted stack into the chest, lock the Hopper with redstone and verify the count after unlocking. Leave a counted stack in the Hopper with the output locked.
4. Build and fuel a basic IC2 Generator using obtained materials. Power a supported IC2 machine, process one item and check the result. Treat a later distillation build as an optional extension to the smoke path.
5. Observe a natural lunar transition. Record the phase and use a bed on a later night to check advancement.
6. Save and reopen. Check the Hopper stack, facing and mode, machine state and lunar phase. Record any stuck interactions and any item count differences.

## Open risks and readiness

- Live gasoline conservation passed through the real machine endpoints. Physical pipes, an external EU source and partial-batch persistence remain release risks.
- Hopper capacity evidence covers the vanilla chest. Redstone, persistence of a loaded tile and per-mod slot restrictions still need live evidence.
- Portable crafting checks do not initialize the installed FML mods. Live output collection, Inventory Tweaks protections and drag gestures remain separate acceptance work.
- Lunar sleep and cross-save listener behavior still need live checks. The Forge 3.4.9.171 compile profile has no equivalent full Tekkit runtime run in this record.
- The candidate is suitable for an exploratory Survival smoke run in Tekkit Dev. Functional milestone acceptance and subjective Survival acceptance remain open.
