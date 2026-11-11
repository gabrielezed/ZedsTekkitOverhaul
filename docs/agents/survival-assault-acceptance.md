# Survival 0.1: Lunar Player Assault

## Playable rules

- The existing lunar service schedules the encounter. Natural `NIGHT_STARTED` at tick 13,000 on a full moon is the only trigger. Newly aligned worlds reach their first full moon on lunar day 4. The active player must be alive, awake, in Survival and in the local Overworld on a non-Peaceful difficulty.
- The cue is one cave sound at dusk. Up to three pulses, two mobs per pulse, begin 100 simulation ticks later. Default spacing is 160 ticks; the director stops after the final pulse even if it could not find a valid site. The total budget is six. Easy and Normal use a two-zombie, one-skeleton pattern; Hard uses only skeletons because vanilla Hard zombies can break wooden doors.
- Each pulse samples the player's current vicinity: 10–15 blocks horizontally, with at most 18 blocks of vertical difference and 24 attempts. It checks loaded chunks, a clear outdoor column, suitable support, player distance, vanilla light and collision, and a local hostile count below 16. A roof, including a transparent roof, rules out spawning below it. Deep caves, the open sea, well-lit land and crowded areas can yield fewer or no spawns. Mobs use their ordinary targeting, despawn and persistence rules. Resolution ends spawning; it does not remove living mobs.
- `data/zto_player_assault.dat` records the consumed raw night boundary and whether the event was open at save time. Normal reload never restarts that night. An open record reopens as `ABORTED / INTERRUPTED_ON_REOPEN`. A portal detaches the director; returning does not resume pulses. Clock resync, sleeping, death and difficulty changes stop active pulses. The lunar replay watermark and the raw consumed boundary both prevent duplicate starts. Ordinary save cadence governs durability; hard-crash recovery between saves is not guaranteed.
- `playerAssault` enables the feature; `assaultSpawnBudget` (1–6) and `assaultPulseInterval` (80–400) tune it in the ModLoader config. Defaults are enabled, six, and 160. Ordinary vanilla mobs and the separate lunar calendar remain active when the feature is disabled.

## Verification commands

```powershell
.\zto-tools\test-lunar-calendar.bat
.\zto-tools\test-dev-bridge.ps1
.\zto-tools\build-and-deploy.bat
.\zto-tools\test-tekkit-dev.ps1 -DisposableSave <marked-daytime-save> -Persistence Assault -LunarWaitSeconds 650
```

The lifecycle copies the marked save, checks the candidate and running archive SHA-256, then uses the existing lunar phase authority to align the upcoming night. Vanilla time advances normally. It runs the unchanged five-scenario `singleplayer-core` suite, checks the assault, closes and reopens the same copy, and verifies that the event does not start again. Use `-SkipCore -InterruptAssault` to exercise Save/Quit after the first successful pulse and check the interruption reason. The source must be in Survival with available outdoor terrain, with the clock at least 100 ticks before dusk and no prior attempt for that boundary. The fixture can only run in an explicitly marked disposable Overworld, with the existing test flags and session/revision gates.

## Evidence

Candidate tested on 2026-11-11: **0.1.0-alpha.1**, Git `399d7b0001d5cb92ae79c978a2198397bbed968e` with a **dirty** source tree, built at `2026-11-11T10:51:38.8688184Z`. Archive and running code-source SHA-256: `140687c93cb6f2dce671438707f290275074c45a05c95cc9fbbf17e95b2b1728`. This is a test candidate; no release tag was created.

- **Compile profile:** MCP 6.2, Forge 3.4.9.171, JDK 8. Client recompile, reobfuscation and streaming verification of 236 archive entries passed. The MDK has no server sources.
- **Live pack:** isolated Prism Tekkit Dev, Tekkit Classic 3.1.2, Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176, Java 8u51. Installed artifacts include IC2 1.97, BuildCraft 2.2.14, EE2 1.4.6.5, RedPower PR5B2 and Inventory Tweaks 1.41b. `javap` checked the new legacy references against the installed overlay before the vanilla client on the classpath. Overlay SHA-256: `0827ee55325fedbc37f2a6a890f19db11f75fab5e9a5c02eafd1bd50d2b59446`.
- **World:** source `ZTO-Test-4e2e8523bd1c44aaa76e1c32fb6c5516`, seed `-7184827518270584220`, Survival on Hard. The source stayed closed; the runner tested copy `ZTO-Test-dd978bafe3bb4254b8504f5d313f7bea`. The fixture aligned the calendar in daytime and left vanilla time untouched. The observer reported zero starts before dusk.

| Evidence | Result | Observation |
| --- | --- | --- |
| Portable lunar API, assault schedule and Dev Bridge | PASS | Ordinary and full-moon boundaries, rewind, skip, alignment, reload/dimension filtering and bridge transport/dispatch. |
| Portable moon transformer | PASS | Tested against MCP, vanilla and the installed pack overlay. |
| Live natural full moon and placement | PASS | One start at raw tick 181,000. Three skeletons inserted in three pulses and 66 attempts over 420 simulation ticks. All three columns were loaded, supported and clear at feet/head, with measured horizontal distances of approximately 11.9–14.4 blocks. Ended with `PULSES_COMPLETE`. |
| Live resolved Save/Quit/Reload | PASS | A different Java process and bridge session reopened the same night with boundary 181,000, zero new starts and zero new spawns. Both shutdowns were graceful. |
| Live `singleplayer-core` | PASS | Natural lunar boundary, distillation gasoline batch, Hopper transfer, capacity and redstone. Crafting 3×3, 5×5 and hotbar lock also passed; the current Inventory Tweaks profile blocked the separate hard-protection check. |
| New candidate with bridge disabled, live bed/NEI/portal and ordinary-night assault denial | BLOCKED | No live run for these specific cases on this JAR. The portable gate covers their corresponding event causes. A one-tick external time write is indistinguishable from natural progress to the existing calendar sampler. |
| Human Survival trial | BLOCKED | Owner assessment pending. Unattended tests did not judge audio, combat, doors, walls, pathfinding or the mixed Normal encounter. |

Local reports: `dist-zto/test-reports/autonomy-ZTO-Test-dd978bafe3bb4254b8504f5d313f7bea.json` and `609813737575974.json`. The lifecycle links all five core reports. Build log: `dist-zto/assault-build.log`.

## Owner playtest

Use a disposable Survival save in Normal difficulty. Compare the encounter in open terrain and behind a simple covered, lit shelter. Use a door and a clear retreat route of your choice. The lunar debug menu can align to full moon during daylight, then let dusk arrive naturally. Check whether the cue gives time to react, whether the first pulse engages from a fair distance, and whether leaving or defending feels viable. Tune the cue, radius or pulse spacing based on that comparison. Leave the longer-term effect of sleep and the criteria for a future `SITE_SIEGE` open.
