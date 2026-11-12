# Lunar Pressure candidate (0.1.0-alpha.4)

| Lunar phase | Additional pressure |
| --- | --- |
| New moon, waxing crescent | Ordinary nights |
| First quarter | One patrol of up to two mobs |
| Waxing gibbous | One patrol of up to three mobs |
| Full moon | Existing three-wave player assault |
| Waning gibbous, last quarter, waning crescent | Ordinary nights. A residual waning-gibbous patrol is deferred. |

Patrols are enabled by default. Set `lunarPatrols=false` in `config/mod_ZedsTekkitOverhaul.cfg` while the client is closed to turn them off independently of `playerAssault`. New worlds still start at new moon (day index 0), with the full moon on day five (index 4).

Natural dusk at tick 13,000 schedules one patrol in the local Overworld. An awake Survival player must remain available on the same non-Peaceful difficulty for the 240-tick lead-in. A paused simulation tick during that lead-in cancels the patrol. The patrol then samples at most 24 sites 12–16 blocks from the player, using the assault's loaded-chunk, surface, roof, collision, light and local hostile checks. It can finish with fewer mobs or none. Zombies and skeletons use their normal equipment and AI. Hard uses skeletons to avoid vanilla wooden-door breaking by zombies. Existing hostiles stay in the world under vanilla rules.

`SiegeDirector` receives the lunar event and ticks the patrol on the same simulation thread as the assault. The lunar service already persists its replay watermark, including the dusk boundary. Save and Quit abandons a queued patrol. Reopening, rewinding, skipping time or changing phase cannot replay that boundary. The patrol adds no save record. A phase selected in daylight can still encounter the next natural dusk. Both encounters target the player's vicinity, which keeps site targeting and territorial systems available as separate future work.

Survival Debug shows the current phase, patrol-night eligibility, encounter state, successful insertions and the outcome. The latest patrol's count remains visible until the next attempt or world change. The Forge log and optional Survival Debug history report one outcome per attempt, with aggregated spawn rejections.

## Reproduce in Tekkit Dev

1. Run `zto-tools/test-lunar-calendar.bat` and `zto-tools/build-and-deploy.bat`.
2. For an interactive trial, run `zto-tools/new-assault-playtest.ps1 -Name 'First Quarter Playtest' -LunarPhase FIRST_QUARTER`. Open the generated world in Normal, stay on the open surface through dusk and inspect F10 with `survivalDebugHud=true`.
3. For an automated run, [enable the existing test flags and mark a disposable save](test-automation.md#enable-and-prepare-a-save). Generate it with the same playtest script using `-Name "Test-$id"` where `$id = [Guid]::NewGuid().ToString('N')`. The folder is `ZTO-Test-$id` and the generated seed is `-7184827518270584220`. Run `zto-tools/test-tekkit-dev.ps1 -DisposableSave "ZTO-Test-$id" -Persistence Patrol -SkipCore`. Use `FIRST_QUARTER` or `WAXING_GIBBOUS` for the positive and reload checks, and `NEW_MOON` for the quiet-night check. Use `FULL_MOON` with `-Persistence Assault -SkipCore` for the existing assault regression.
4. Restore the Tekkit Dev flags and launch once with the bridge, Lunar Debug and Survival Debug off.

## Candidate evidence

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

Next playtest: repeat the waxing-gibbous scenario on open terrain and compare the rejection counts with a lit or covered shelter. If daylight at the current spawn tick still dominates, try a later spawn tick while retaining the vanilla light rule. Verify the HUD in open play and sleep through an eligible night using a bed.
