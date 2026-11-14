# Lunar patrols

PR #25 introduced patrols in `0.1.0-alpha.4` and merged into `main` as `6e61d3e`. The scheduling rules below still apply. The acceptance results at the end record the original alpha.4 builds.

| Lunar phase | Additional pressure |
| --- | --- |
| New moon, waxing crescent | Ordinary nights |
| First quarter | One patrol of up to two mobs |
| Waxing gibbous | One patrol of up to three mobs |
| Full moon | Existing three-wave player assault |
| Waning gibbous, last quarter, waning crescent | Ordinary nights. A residual waning-gibbous patrol is deferred. |

Patrols are enabled by default. Set `lunarPatrols=false` in `config/ZedsTekkitOverhaul.cfg` while the client is closed to turn them off independently of `playerAssault`. New worlds still start at new moon (day index 0), with the full moon on day five (index 4).

Natural dusk at tick 13,000 schedules one patrol in the local Overworld. The patrol is pending for 800 simulation ticks. An ordinary pause freezes its timer and suppresses placement until play resumes. Player loss, Peaceful, a difficulty change, a world or dimension change, night end and calendar resync discard pending work.

Placement begins around tick 13,800. The patrol checks up to four candidates every 20 simulation ticks for a 240-tick window, with at most 12 rounds and 48 candidates. Filling the allocation or reaching the local hostile cap closes the window early. Any unfilled slots are skipped. Each round samples 12–16 blocks from the player's current position. Alpha.6 checks local surface approaches and allows eligible torch-lit exterior placements. Set `lunarOutdoorLightPressure=false` and restart to restore the earlier placement rules. Eligible mobs can receive the alpha.5 encounter loadouts. Hard uses Skeletons to avoid vanilla wooden-door breaking by Zombies. Spawned hostiles use native AI and persistence. [Outdoor placement rules](survival-alpha6.md) · [Loadout rules](survival-alpha5.md)

`SiegeDirector` receives the lunar event and ticks the patrol on the same simulation thread as the assault. The lunar service already persists its replay watermark, including the dusk boundary. Save and Quit abandons a queued patrol. Reopening, rewinding, skipping time or changing phase cannot replay that boundary. The patrol adds no save record. A phase selected in daylight can still encounter the next natural dusk. Both encounters target the player's vicinity, which keeps site targeting and territorial systems available as separate future work.

Survival Debug shows the current phase, patrol-night eligibility, pending or spawning state, pause, elapsed time, candidate attempts, successful insertions, remaining or skipped allocation and grouped rejection reasons. The latest outcome remains visible until the next patrol or world change. The Forge log and optional Survival Debug history report a bounded terminal summary.

## Reproduce in Tekkit Dev

1. Run `zto-tools/test-lunar-calendar.bat` and `zto-tools/build-and-deploy.bat`.
2. For an interactive trial, run `zto-tools/new-assault-playtest.ps1 -Name 'First Quarter Playtest' -LunarPhase FIRST_QUARTER`. Open the generated world in Normal, stay on the open surface through dusk and inspect F10 with `survivalDebugHud=true`.
3. For an automated run, [enable the existing test flags and mark a disposable save](test-automation.md#enable-and-prepare-a-save). Generate it with the same playtest script using `-Name "Test-$id"` where `$id = [Guid]::NewGuid().ToString('N')`. The folder is `ZTO-Test-$id` and the generated seed is `-7184827518270584220`. Run `zto-tools/test-tekkit-dev.ps1 -DisposableSave "ZTO-Test-$id" -Persistence Patrol -SkipCore`. Use `FIRST_QUARTER` or `WAXING_GIBBOUS` for the positive and reload checks, and `NEW_MOON` for the quiet-night check. Use `-InterruptPatrol` for an early Save and Quit, or `-ProtectedPatrol` with a glass roof spanning the entire spawn annulus. Use `FULL_MOON` with `-Persistence Assault -SkipCore` for the existing assault regression.
4. Restore the Tekkit Dev flags and launch once with the bridge, Lunar Debug and Survival Debug off.

## Original single-burst evidence

Portable checks passed for the new-world phase alignment, all eight natural night gates, non-Overworld rejection, reload and rewind suppression, a sleep-like time jump, and administrative phase alignment. `AssaultDiagnosticsSelfTest` also passed. The time jumps were exercised in Java, with physical bed use left for an in-game playtest.

MCP 6.2 compiled and reobfuscated the client with JDK 8 and Forge 3.4.9.171. The deploy script verified 255 archive entries. Unattended Tekkit Dev runs used Minecraft 1.2.5 / Tekkit Classic 3.1.2 with Forge 3.3.8.164, FML 2.2.106.176 and Java 8u51. The running JAR was built from `ed870e9b615c8f40ab985c48abcb6313e6329b87` and matched SHA-256 `48becd24f687b09bcd68f82dcefa458222c2d59349830e11fc080b7126adf89b`. The build was marked dirty because two pre-existing local notes were untracked.

| Tekkit Dev check | Observation |
| --- | --- |
| First quarter | Natural dusk at tick 13,000. One mob inserted out of two at tick 13,240 in 24 attempts. Eighteen light checks and five covered-column checks rejected candidates. Zero assault starts. The same night reopened with zero new patrols. |
| Waxing gibbous | Natural dusk scheduled the three-mob patrol. One run aborted when the client reported a paused tick at 13,009. The retry reached its spawn window at 13,240 and inserted 0/3 in 24 attempts (15 light checks, nine covered columns). The positive insertion assertion failed. |
| New moon | One natural dusk at tick 13,000. Zero patrol outcomes and zero assault starts. |
| Full moon, unattended fight | One `PLAYER_ASSAULT` started at tick 13,000. Three waves inserted 1, 6 and 6 mobs. The Survival player became unavailable and the encounter ended with `ABORTED / PLAYER_UNAVAILABLE`. This run failed the runner's expected `RESOLVED` state. |
| Full moon, interrupted reload | The existing assault scenario passed with `-InterruptAssault`. One zombie inserted in wave one, with zero patrols. Save and Quit preserved boundary 13,000. Reopen reported `INTERRUPTED_ON_REOPEN` with zero new assault starts or insertions. |
| Diagnostics disabled | The isolated instance launched with the Dev Bridge and both debug interfaces disabled. FML reported startup complete with 44 mods loaded. |

The original two placement runs reported 33 vanilla-light rejections, 14 covered-column rejections and one insertion across 48 candidates. These counts motivated moving placement further into the night and distributing candidates over several rounds. The comparison does not isolate timing from random candidate selection.

## Follow-up verification

The portable lunar suite passed the eight-phase gate, replay prevention, paused clock and retry-bound checks. The MCP-side `LunarPatrolSelfTest` passed with the real patrol tick entry point. It checks pause in the lead-in, resume, no placement on a paused tick at the threshold, and Peaceful termination. `AssaultDiagnosticsSelfTest` passed.

```powershell
.\zto-tools\test-lunar-calendar.bat
javac -cp bin/minecraft -d dist-zto/lunar-tests zto-tools/assault-tests/*.java
java -cp 'dist-zto/lunar-tests;bin/minecraft;jars/bin/minecraft.jar;jars/bin/lwjgl.jar;jars/bin/lwjgl_util.jar;jars/bin/jinput.jar' zed.tekkitoverhaul.assault.LunarPatrolSelfTest
java -cp 'dist-zto/lunar-tests;bin/minecraft' zed.tekkitoverhaul.assault.AssaultDiagnosticsSelfTest
```

MCP 6.2 compiled and reobfuscated the client with JDK 8 and Forge 3.4.9.171. The build verified 255 archive entries. The Tekkit Dev runs used Minecraft 1.2.5, Tekkit Classic 3.1.2, Forge 3.3.8.164, FML 2.2.106.176 and Java 8u51. The tested JAR SHA-256 was `697eb1924a756b2243edeae6adc6bc174b4eaf0a0762bf419dc41fe54773d2d7`. It was built from a working tree at `4e4ba8a` with the production Java changes subsequently committed as `19f2b49`.

| Tekkit Dev check | Observation |
| --- | --- |
| First quarter, generated terrain | One natural dusk. Inserted 2/2 by tick 13,840 in 12 attempts, with seven vanilla-light and three covered-column rejections. No assault and no patrol on same-night reload. |
| Waxing gibbous, generated terrain | One natural dusk. Inserted 3/3 by tick 13,940 in 29 attempts, with 18 vanilla-light and eight covered-column rejections. No assault and no patrol on same-night reload. |
| Waxing gibbous, illuminated glass-roofed annulus | Timed out at tick 14,040 with 0/3 inserted, 48 covered-column rejections and three skipped slots. Same-night reload produced no patrol. |
| First quarter, vanilla pause menu | A GUI-driven probe in a separate Tekkit Dev copy held pending elapsed time at nine ticks with zero attempts for two seconds. It observed the paused flag in the patrol, resumed at ten ticks and resolved at elapsed tick 920 with 2/2 insertions in 25 attempts. |
| First quarter, pending Save and Quit | The vanilla menu logged `ABORTED / WORLD_UNLOADED` at elapsed tick nine with zero attempts. The reopened world kept the patrol idle through tick 14,076. Inventory Tweaks threw in `cloneHotbar` at the GUI probe's final shutdown after the checks passed. The separate-process lifecycle reopened the same night, produced no patrol through tick 14,156, and exited cleanly on both launches. |
| New moon | One natural dusk. Zero patrol outcomes and zero assault starts. |
| Full moon, interrupted reload | Only `PLAYER_ASSAULT` started. Wave one inserted one zombie before Save and Quit. Reopen retained boundary 13,000 and reported `ABORTED / INTERRUPTED_ON_REOPEN` with zero new starts or insertions. |
| Full moon, three waves in the covered arena | The assault scenario passed with `RESOLVED / WAVES_COMPLETE` at elapsed tick 1,280. Each wave rejected 112 covered candidates, and there was no patrol. Inventory Tweaks threw in `cloneHotbar` during automated shutdown, so this run could not verify reload after resolution. |
| Equipment backport | The existing live equipment probe passed for native Skeleton arrows, Zombie sword damage, armor protection, persisted loadouts, one-time drops and ordinary comparison mobs. |
| Diagnostics disabled | The separate copy reached the title screen with 44 mods loaded, Lunar Debug and Survival Debug off, and the Dev Bridge disabled. |

The lifecycle report filenames end with `7832ce7df99d4915a1d97ab277748249` (first quarter), `69a370e39d8b4282ba4ee628e37ebef4` (waxing gibbous), `72a60089a705494896fd0d30d15ccc63` (shelter), `df5c0f8bd3524d5eb3cde346faa299bd` (interrupted patrol), `c4eda3316fb94c63a5183a91de8be8be` (new moon), `3bed987288db4f65bf438ffcc9e6e4d3` (interrupted assault) and `f4e116d07416485c8f9212aa817dab14` (covered assault) under `dist-zto/test-reports/`. The GUI checks used the vanilla menu on the game thread. Owner playtesting and physical bed use remain separate.

## Follow-up milestones

Alpha.4 used vanilla hostile light validation and required an open column. Alpha.5 introduced encounter equipment, and alpha.6 added the lit exterior rule and the local approach checks. Territorial threat, settlement recognition and persistent `SITE_SIEGE` targets remain future milestones. [Project Vision](../../PROJECT_VISION.md) · [Alpha.6 acceptance](survival-alpha6.md)
