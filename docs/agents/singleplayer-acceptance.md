# Milestone 1 singleplayer acceptance

The [Survival 0.1 assault record](survival-assault-acceptance.md) tracks the later candidate separately. The historical results below apply only to their original build hashes.

## Provenance

- **Date:** 2026-11-10. Local `main` and `origin/main` both pointed to `73cdf9c1f462ff71c99075bdfc4738b7964447db`, the PR #17 merge. The acceptance candidate includes the local bridge changes over that baseline.
- **Candidate:** `b2929d8db30d655c2cd13d49e4763c071701b06a196218b1de52c1f419440163`. MCP client compilation and reobfuscation passed under Adoptium JDK 8.0.504.1, and the ZIP verifier read 219 entries. The staged and deployed file hashes matched the code-source hash returned by the running bridge. Build log at `dist-zto/milestone1-build.log`.
- **Redstone follow-up:** `56baf284f645c301d9643c7b1cb5d596e52dbfc472582961608fe1c90b374a30`. MCP client compilation, reobfuscation and streaming verification passed. Build log at `dist-zto/milestone1-redstone-build.log`. The live scenario results below identify the earlier build.
- **Distillation follow-up:** `e8b9b398d658f130cc5c73959139365c4ad8f15c975f4bd13c07f45641c29201`. The running bridge matched the deployed MCP-compiled and reobfuscated candidate. Build log at `dist-zto/milestone1-distillation-build.log`. Distillation and GUI observations below use this build.
- **Runtime:** isolated PrismLauncher 11.1.1 Tekkit Dev, Minecraft 1.2.5, FML 2.2.106.176, Forge 3.3.8.164, Java 8u51. Installed artifacts identify IC2 1.97, BuildCraft 2.2.14, RedPower 2 PR5B2, EE2 1.4.6.5, IronChests 3.8.0.40 and Inventory Tweaks 1.41b. The core overlay contains NEI 1.2.2.4 per the pinned local pack audit. MCP initialized Forge 3.4.9.171 during the headless crafting check. Tekkit Dev uses its own modpack overlay.

## Coverage matrix

Live results in this table refer to the 2026-11-10 candidates and runtime above. The unattended evidence section records later runs separately. Owner observations refer to earlier builds and remain unpinned where the source report omits a hash. A portable PASS applies to the specific assertions named in the evidence column.

| Evidence level | Meaning |
| --- | --- |
| Portable | Shell or Java assertions outside a running Tekkit client, including tests using MCP classes. |
| Live runtime | Assertions observed in an actual Tekkit Dev world. Setup may be assisted. |
| GUI-assisted | A real GUI action was performed before or during runtime assertions. The NEI recipe transfer and bed-sleep observations are in this category. |
| Fully unattended | Shell invocation, world opening, scenario execution and shutdown complete without GUI input. Persistence additionally requires a new client session opening the same disposable copy. |
| Owner-accepted | The owner observed and accepted the behavior. Applies only to the scope and build recorded for that observation. |

| Feature | Scenario | Portable | Live | Pack integration | Visual | Owner | Evidence and limits |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Hopper | HOPPER-01 downward transfer | PASS | PASS | NOT_APPLICABLE | NOT_APPLICABLE | NOT_RUN | `954256515276138.json`. Four stones transferred in 26 simulation ticks, zero sampled drops, reset and cleanup verified. Baseline JAR also passed in `344264174459423.json`. |
| Hopper | HOPPER-02 redstone lock and resume | NOT_RUN | PASS | NOT_APPLICABLE | NOT_APPLICABLE | NOT_RUN | `303938030476879.json`. The reopened suite verified lock, resume, conservation and cleanup on the distillation candidate. |
| Hopper | HOPPER-03 partial then full destination | PASS | PASS | NOT_APPLICABLE | NOT_APPLICABLE | NOT_RUN | `909610751355446.json`. Chest 1726 → 1728, two of four stones retained in the Hopper after 82 ticks, zero sampled drops, cleanup complete. |
| Hopper | HOPPER-04 loaded inventory, facing and mode across reload | PASS | NOT_RUN | NOT_APPLICABLE | NOT_APPLICABLE | NOT_RUN | Portable NBT round trip. The earlier reopen used a cleaned arena. See the unattended lifecycle evidence for the loaded fixture. |
| Hopper | HOPPER-05 furnace, IC2 Generator, IronChest and BuildCraft wooden item pipe | PASS | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_RUN | Portable furnace and chest rules only. Earlier owner observations reported IC2/EE2 interoperability and an IronChest failure before the adapter follow-up. |
| Hopper | HOPPER-06 placement, pickup, GUI, breaking, model and collision | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Manual checklist and prior reported issues in the [Hopper handoff](../engineering/hopper-backport.md). |
| Distillation | DIST-01 formation, orientation, independent layout | PASS | PASS | NOT_RUN | NOT_APPLICABLE | MANUAL_ACCEPTED | 44 layout assertions. `160717546507312.json` verifies the formed east-facing layout. Live negative/revalidation cases and independent layouts remain open. |
| Distillation | DIST-02 gasoline batch | PASS | PASS | PASS | NOT_APPLICABLE | MANUAL_ACCEPTED | `160717546507312.json`. Seven real components formed. In 162 observed ticks, 250 mB oil, 40 mB coolant and 1280 EU were consumed. The receiver delivered 250 mB of gasoline (liquid ID 3017). Cleanup complete. Integration coverage is limited to direct BuildCraft liquid and IC2 LV endpoint calls. |
| Distillation | DIST-03–04 starvation and other fractions | PASS | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_RUN | Covered by the portable process harness. Live cases remain open. |
| Distillation | DIST-05 BuildCraft fluid transport | NOT_RUN | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_RUN | See the unattended evidence for physical oil input and receiver extraction. A physical coolant inlet remains open. |
| Distillation | DIST-06 partial batch across reload | PASS | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_RUN | Portable state restoration. See the unattended evidence for real NBT and multiblock rebind. |
| Distillation | DIST-07 panels and inspector parity | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | The batch scenario exercised live boiler and heater snapshots. Comparison against GUI panels remains open. |
| Crafting | CRAFT-01 3×3 result collection | PASS | PASS | PASS | PASS | MANUAL_ACCEPTED | In the disposable Creative save, NEI supplied 64 vanilla workbenches. Four were placed in the 3×3 grid and consumed to collect one Advanced Crafting Table. The bridge then reported 60 workbenches and one advanced table in `milestone1-before-reload.json`. Integration here covers NEI item search and provisioning. |
| Crafting | CRAFT-02–03 recipe transfer, missing ingredients and batches | PASS | NOT_RUN | NOT_RUN | NOT_RUN | MANUAL_ACCEPTED | MCP workbench slots and result preview, missing ingredient refusal and planner quantities. |
| Crafting | CRAFT-04 advanced 5×5 | PASS | PASS | PASS | PASS | MANUAL_ACCEPTED | In Survival, NEI prepared 16 smooth stones around the grid perimeter. Collecting one Foundry emptied the grid. Bridge snapshots showed 65 → 49 stones, 0 → 1 Foundry and an empty cursor after deposit. Inventory Tweaks 1.41b was loaded. Its sorting and slot protection require separate verification. See `milestone1-crafting-5x5.json`. |
| Inventory | CRAFT-05–06 slot protection, storage and overflow | PASS | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Protection rules and storage planner checks. See the unattended evidence for the hotbar row rule. Full NBT-preserving transfers remain open. |
| Inventory | CRAFT-07 drag distribution | PASS | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Planner and slot policy checks plus drag-agent bytecode probe against Tekkit Dev archives. Actual mouse gesture coverage remains open. |
| Inventory | CRAFT-08 IC2, RedPower and other modded containers | PASS | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Portable target policy only. Live clicks and per-container compatibility remain open. |
| Lunar | LUNAR-01–03 phase order, fresh and legacy alignment | PASS | NOT_RUN | NOT_APPLICABLE | MANUAL_ACCEPTED | MANUAL_ACCEPTED | Calendar harness. Earlier Tekkit Dev showed new-save data and legacy alignment. Owner verified all visible phases. |
| Lunar | LUNAR-04 and LUNAR-09 natural boundary and listener | PASS | PASS | NOT_APPLICABLE | NOT_APPLICABLE | MANUAL_ACCEPTED | `474224864946997.json` verified one `NIGHT_STARTED` with cause `NATURAL` at raw tick 157000 after reopening the save, with zero dropped events. `milestone1-lunar-observed.json` covers the earlier night end and dawn phase change. Natural full-moon arrival and disposable listener registration remain open. Owner observed the natural full-moon diagnostic. |
| Lunar | LUNAR-05 clock edits and suppression | PASS | NOT_RUN | NOT_RUN | NOT_APPLICABLE | MANUAL_ACCEPTED | Portable time manipulation. Earlier logs recorded admin and external time changes. |
| Lunar | LUNAR-06 real bed sleep | NOT_RUN | PASS | NOT_APPLICABLE | PASS | NOT_RUN | In Survival the player entered a real bed at night and woke at dawn. The bridge reported one `RESYNC/TIME_SKIP` at raw tick 168001, day and phase 6 → 7, with no dropped or duplicate events. See `milestone1-bed-sleep.json`. |
| Lunar | LUNAR-07 persistence and world switching | PASS | PASS | NOT_APPLICABLE | NOT_APPLICABLE | MANUAL_ACCEPTED | After Save and Quit, automated clicks on Play Selected World left the client at Select World. Double-clicking the marked save row successfully reopened it. The bridge preserved logical day 5 and phase 5, assigned world/session 2, and rejected the old cursor with `STALE_CURSOR`. See `milestone1-lunar-reopen.json`. |
| Lunar | LUNAR-08 native atlas | PASS | NOT_RUN | NOT_APPLICABLE | MANUAL_ACCEPTED | MANUAL_ACCEPTED | Transformer probe against actual overlay. Owner visual pass on earlier build. |
| Cross-system | Disabled bridge, unmarked save, wrong build and ordinary gameplay | PASS | NOT_RUN | NOT_RUN | NOT_APPLICABLE | NOT_RUN | Transport, command schema and dispatch checks passed. See the PR #19 final review verification for current live release gates. |
| Cross-system | NEI, Inventory Tweaks, Hopper restrictions, EE2/IC2, distillation and sleep | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Pack startup reached a disposable Creative save. Combined gameplay workflows remain open. |
| Survival | Unassisted smoke path | NOT_APPLICABLE | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | The seeded Hopper scenario counts as a runtime fixture. |

Runtime artifacts are under the ignored `dist-zto/test-reports/` directory. Portable commands were `test-dev-bridge.ps1`, `test-hopper.ps1`, `test-crafting-ux.bat`, `test-drag-agent.ps1`, `test-distillation-core.bat`, `test-distillation-layout.bat`, `test-distillation-models.bat`, `test-lunar-calendar.bat` and `test-lunar-agent.bat`, all under `zto-tools/`.

The first `singleplayer-core` invocation exited 2 at world selection before creating any fixture. After reopening the marked save, `suite singleplayer-core -LunarWaitSeconds 120` passed all five registered scenarios and exited 0 on the distillation candidate. See `milestone1-lunar-suite-reopened.json` for the per-scenario results and report paths.

## Unattended evidence on 2026-11-11

The lifecycle work started from `5f44018` (merge of PR #18). Live runs used the isolated Prism 11.1.1 Tekkit Dev, Minecraft 1.2.5, FML 2.2.106.176, Forge 3.3.8.164 and Java 8u51. BuildCraft 2.2.14, IC2 1.97 and Inventory Tweaks 1.41b were present. The MCP compilation profile and portable crafting tests used Forge 3.4.9.171 under Adoptium JDK 8.0.504.1. The report files named below are under `dist-zto/test-reports/`.

- The first unattended core attempt used `b5fa41d985b76902718c7562bccca49a9a9403902701435cadd4fc8d83d282c8`. `787400144027926.json` passed a natural night boundary. The bridge returned `NOT_READY` during the following distillation batch and the remaining scenarios did not start. The client was shut down by the runner. See `autonomy-ZTO-Test-788459eeb3f14a6d8034d818b51b53da.json`.
- The first distillation restart used `5746139b0ca5a09a6afc4997ba29c68127cea535d72167a3c828561c108e33c6`. It verified two separate client sessions and reached 80 ticks of progress before shutdown. The boiler was missing after restart. The legacy shutdown path tried to unload with the main loop already stopped. The bootstrap now saves and unloads before shutdown and waits for pending chunk I/O. See `autonomy-ZTO-Test-e10a8c17600a449ea0a0c53b860d7682.json`.

The verified candidate SHA-256 is `c3aa20585057ac20eae04517a898ba6d30d5e5fbe848ed476cb45be713d8195b`. MCP client compilation, reobfuscation and streaming verification of 224 archive entries passed. See `dist-zto/autonomy-final-build.log`.

| Case | Fully unattended | Evidence |
| --- | --- | --- |
| Complete `singleplayer-core` lifecycle | PASS | `autonomy-ZTO-Test-f7f85e0e8cdb4a7998eaad100d960d32.json`. The runner opened the authorized copy, verified the JAR, passed all five core scenarios, ran crafting, restarted for Hopper persistence and shut down both clients with exit 0. Core reports: `348504318111747.json` (lunar), `76295289328409.json` (distillation), `18193651766776.json` (Hopper transfer), `303807655731504.json` (capacity), `229521781509103.json` (redstone). |
| HOPPER-04 loaded inventory, facing and mode across client restart | PASS | The same lifecycle report verified four stones in slot 0, north-facing metadata 2 and Input mode before and after reopening the same copy in a new Java process and bridge session. |
| DIST-06 partial batch across client restart | PASS | `autonomy-ZTO-Test-55ec07a1710646098b08f26da780130d.json`. Separate Java PIDs and bridge sessions opened the same copy. The apparatus re-formed with 80 progress ticks, 250 mB of crude, 40 mB of coolant, the same selected fraction and no product. Both clients exited cleanly. |
| DIST-05 physical BuildCraft transport and external IC2 energy | PASS | `autonomy-ZTO-Test-a5b4d185ac7f40f1b777dac5ac83c21e.json`. Two redstone engines powered two wooden waterproof pipes. The input tank emptied and the output tank received 250 mB of gasoline (ID 3017) after 315 observed simulation ticks. The adjacent coal-fueled IC2 Generator delivered energy through EnergyNet, with up to 1870 EU observed in the heater. The coolant was seeded through the condenser inlet. |
| CRAFT-01 and CRAFT-04 transaction paths and missing ingredients | PASS | The same two reports cover installed 3×3 and 5×5 recipes, insufficient ingredient refusal, real result-slot pickup, inventory deposit and exact conservation. |
| CRAFT-05 active Inventory Tweaks hotbar row rule | PASS | The same reports verify the active rule allowed recipe sourcing from the hotbar and kept returned ingredients out of that row. |
| CRAFT-05 explicit slot lock or frozen slot in Inventory Tweaks | BLOCKED | The active ruleset had no such slot. The portable protection check passed. |

Portable checks also passed for bridge transport and dispatch, the 21 Hopper inventory and NBT assertions, crafting transactions and drag policy, and the 197 distillation core assertions. The unattended runs add no owner acceptance.

## PR #19 final review verification

The final review ran the existing `singleplayer-core` suite once on the same `c3aa20585057ac20eae04517a898ba6d30d5e5fbe848ed476cb45be713d8195b` JAR in Tekkit Dev. All five scenarios passed. The runner also verified Hopper persistence across two Java processes and bridge sessions. Both clients exited normally. See `autonomy-ZTO-Test-e342fcb52a694abca6b19dda42f62ce0.json` and its scenario reports `127692015345927.json`, `168001427109018.json`, `62922365354059.json`, `839151396963949.json` and `391332132417160.json`. The existing portable bridge harness passed again.

The live release-gate audit used the same JAR, Java 8u51 and the same Prism-assembled Minecraft and Forge runtime. The temporary audit launch omitted JVM flags without editing the instance configuration. Evidence is in `pr19-release-gates.json`.

| Gate | Result | Evidence |
| --- | --- | --- |
| All development flags absent | PASS | Client reached the title screen. The valid launch request was ignored and no bridge discovery was published. The orchestrator hit the expected startup timeout, removed its request and closed the identified Java process. |
| Read bridge enabled, testing flag absent | PASS | The request was ignored, bridge status stayed `no_world` and the authenticated fixture command returned `UNSUPPORTED`. The expected startup timeout left neither the request nor the Java process. |
| Unmarked disposable copy with testing flags enabled | PASS | The bootstrap returned `TEST_WORLD_REQUIRED` and consumed the request without changing any file in the copy. After opening that copy through the normal singleplayer menu, `test.context` returned `allowed=false`, `fixture.create` returned `TEST_WORLD_REQUIRED`, the arena block stayed empty and the stop request was ignored. The operator closed this audit through the GUI. |
| Client failure before the launch announcement | PASS | During audit setup the Minecraft main thread failed before bridge startup because the temporary launch lacked its native libraries. The runner identified and closed the remaining Java process and removed the request. See `autonomy-ZTO-Test-3dcf864c8d90495995b146a790bd693b.json`. Final inspection found no Tekkit Dev Java process and no pending autoload request. |

## Survival smoke path

1. Start a fresh disposable Survival save in Tekkit Dev with test writes disabled. Record seed, build hash and runtime versions.
2. Gather wood, stone, coal and iron in Survival. Use NEI to find ordinary recipes, prepare a workbench recipe from gathered materials and collect it. Make the Advanced Crafting Table when resources allow and check one Foundry preview.
3. Craft a Hopper and chest. Transfer a counted stack into the chest, lock the Hopper with redstone and verify the count after unlocking. Leave a counted stack in the Hopper with the output locked.
4. Build and fuel a basic IC2 Generator using obtained materials. Power a supported IC2 machine, process one item and check the result. Treat a later distillation build as an optional extension to the smoke path.
5. Observe a natural lunar transition. Record the phase and use a bed on a later night to check advancement.
6. Save and reopen. Check the Hopper stack, facing and mode, machine state and lunar phase. Record any stuck interactions and any item count differences.

## Deferred coverage and next milestone

- Deferred industrial cases include a physical coolant inlet, IC2 cable loss, alternate fractions and Hopper restrictions across additional modded inventories.
- Deferred GUI and inventory cases include NEI recipe transfer and missing-ingredient display on the final candidate, explicit Inventory Tweaks locks, sorting and refill, mouse drag gestures, machine panels and real bed use on the final candidate. Earlier GUI-assisted evidence covers the 5×5 NEI transfer and bed interaction.
- Deferred environment cases include cross-save lunar listener behavior and a full Tekkit runtime run on the Forge 3.4.9.171 compile profile.
- Development now moves to the Survival Gameplay Vertical Slice, starting with the first lunar siege prototype under [Project Vision](../../PROJECT_VISION.md). Use the Survival smoke path above for gameplay acceptance. Extend the Dev Bridge only when a concrete gameplay feature requires an additional diagnostic or assertion.
