# Lunar Pressure candidate (0.1.0-alpha.4)

| Lunar phase | Additional pressure |
| --- | --- |
| New moon, waxing crescent | Ordinary nights |
| First quarter | One patrol of up to two mobs |
| Waxing gibbous | One patrol of up to three mobs |
| Full moon | Existing three-wave player assault |
| Waning gibbous, last quarter, waning crescent | Ordinary nights. A residual waning-gibbous patrol is deferred. |

Patrols are enabled by default. Set `lunarPatrols=false` in `config/mod_ZedsTekkitOverhaul.cfg` while the client is closed to turn them off independently of `playerAssault`. New worlds still start at new moon (day index 0), with the full moon on day five (index 4).

Natural dusk at tick 13,000 schedules one patrol in the local Overworld. An awake Survival player must remain available on the same non-Peaceful difficulty for the 240-tick lead-in. The patrol then samples at most 24 sites 12–16 blocks from the player, using the assault's loaded-chunk, surface, roof, collision, light and local hostile checks. It can finish with fewer mobs or none. Zombies and skeletons use their normal equipment and AI. Hard uses skeletons to avoid vanilla wooden-door breaking by zombies. Existing hostiles stay in the world under vanilla rules.

`SiegeDirector` receives the lunar event and ticks the patrol on the same simulation thread as the assault. The lunar service already persists its replay watermark, including the dusk boundary. Save and Quit abandons a queued patrol. Reopening, rewinding, skipping time or changing phase cannot replay that boundary. The patrol adds no save record. A phase selected in daylight can still encounter the next natural dusk. Both encounters target the player's vicinity, which keeps site targeting and territorial systems available as separate future work.

Survival Debug shows the current phase, patrol-night eligibility, encounter state, successful insertions and the outcome. The latest patrol's count remains visible until the next attempt or world change. The Forge log and optional Survival Debug history report one outcome per attempt, with aggregated spawn rejections.

## Reproduce in Tekkit Dev

1. Run `zto-tools/test-lunar-calendar.bat` and `zto-tools/build-and-deploy.bat`.
2. For an interactive trial, run `zto-tools/new-assault-playtest.ps1 -Name 'First Quarter Playtest' -LunarPhase FIRST_QUARTER`. Open the generated world in Normal, stay on the open surface through dusk and inspect F10 with `survivalDebugHud=true`.
3. For an automated run, [enable the existing test flags and mark a disposable save](test-automation.md#enable-and-prepare-a-save). Generate it with the same playtest script using `-Name "Test-$id"` where `$id = [Guid]::NewGuid().ToString('N')`. The folder is `ZTO-Test-$id` and the generated seed is `-7184827518270584220`. Run `zto-tools/test-tekkit-dev.ps1 -DisposableSave "ZTO-Test-$id" -Persistence Patrol -SkipCore`. Use `FIRST_QUARTER` or `WAXING_GIBBOUS` for the positive and reload checks, and `NEW_MOON` for the quiet-night check. Use `FULL_MOON` with `-Persistence Assault -SkipCore` for the existing assault regression.
4. Restore the Tekkit Dev flags and launch once with the bridge, Lunar Debug and Survival Debug off.
