# Survival 0.1.0-alpha.6 candidate

## Placement contract

First-quarter and waxing-gibbous patrols and full-moon player assault waves share `SpawnController`. The lunar calendar, replay watermark, assault boundary, wave and retry windows, species order, and post-insertion loadouts retain the alpha.5 rules. Patrol limits are 2 and 3. The full-moon default is 15 slots planned as 4/5/6, with existing configuration overrides, carry, active and nearby population caps.

With `lunarOutdoorLightPressure=true`, a candidate still needs the existing loaded annulus, surface height within eight blocks of the player, an unobstructed column above the surface, vanilla spawn support, player separation, collision and entity clearance, and a dry mob volume. These checks run on the local Overworld simulation thread. The candidate then needs two perpendicular surface routes directed away from the target's block. If aligned with the target on an axis, either direction on that axis can supply the route.

Each route walks 24 cardinal columns. Each column needs a loaded neighborhood, a solid top face under the heightmap, room for feet and head, and at most a one-block height change. Steps need additional headroom on the lower side. Liquids and blocking blocks stop the route. The block movement contract provides the local check for gates and doors. Native candidate collision remains authoritative for the actual mob. A failed route rejects the candidate regardless of light.

Saved block light above 7 would always fail the installed Zombie and Skeleton local-light roll. In that case, the lunar controller keeps the native comparison of saved skylight against a uniform roll from 0 through 31. It skips the combined light roll and the separate brightness-derived path-weight check. The installed `yy` bytecode implements both as brightness tests and neither stock species overrides the spawn gate. EntityLiving's clearance, collision and liquid checks already run in the controller. The sky roll passes 17/32 of the time at saved skylight 15. At block light 7 or below the controller calls vanilla `getCanSpawnHere()`. With `lunarOutdoorLightPressure=false` in `config/ZedsTekkitOverhaul.cfg` and a client restart, lunar candidates use alpha.5 cover and light rules. Vanilla natural spawning still calls the original methods.

## Local limits and cost

- A roof can exclude an interior, including a transparent roof ignored by the heightmap. A roof's top can qualify if it meets the support, clearance, height and approach rules. Covered caves and bunkers keep the alpha.5 restriction for lunar placements. Ordinary cave spawns follow vanilla rules.
- Two routes keep the tested courtyard interior protected even when one route faces a gap. A wall between the player and a valid exterior candidate can prevent the mob from reaching the player. Native AI decides what the mob does next.
- Very large yards can contain both routes. Low barriers the model can step over and layouts with two straight outward openings can also qualify. Broken, narrow or winding exterior terrain, some modded surfaces, and unloaded neighborhoods can skip slots conservatively. This is a local surface check. Territorial threat, persistent settlement recognition and site targeting remain in the later 0.2, 0.3 and 0.4 lines.
- The original scan checks at most 255 vertical blocks. The new test checks at most 72 route columns per candidate, including the possible opposite route on one aligned axis. Per route column it checks at most four chunk coordinates, reads a height and a supporting block, and makes at most three clearance checks. It uses constant scratch space with no path queue or cross-tick cache. Patrol and assault retries still sample at most 4 and 8 candidates respectively. The adapter checks chunk availability before each route read. Portable tests cover unknown columns. Live chunk-generation instrumentation was outside this probe.

Survival Debug and the optional read-only Dev Bridge expose successful outdoor and vanilla light counts, plus candidate rejections. `NO_OPEN_APPROACHES` means the required routes could not be established. It covers both blocking terrain and unknown route data.

## Reproduce

1. Run `zto-tools/test-lunar-calendar.bat`. Compile `zto-tools/assault-tests/*.java` against `bin/minecraft` and run `LunarPatrolSelfTest`, `AssaultDiagnosticsSelfTest`, and `LoadoutPolicySelfTest` with the MCP client classpath.
2. Point `ZTO_PRISM_MODS` at the isolated copy in `dist-zto/survival-hud-runtime/Tekkit Dev/minecraft/mods` and run `zto-tools/build-and-deploy.bat`. Follow the [alpha.5 copy setup](survival-alpha5.md#reproduction) for `launch.json` and staged natives.
3. Close the isolated client and run `zto-tools/test-alpha6-runtime.ps1`. It checks the deployed archive hash, generates a new marked save per mode, launches the assembled pack with diagnostics disabled, and uses a private config in a per-invocation instance copy. It leaves reports in `dist-zto/test-reports/`. Fixed-coordinate checks seed the controller RNG and measure the packaged placement call on the game thread. The lifecycle checks use natural ticks across dusk. The wave-clear mode removes spawned hostiles to exercise subsequent waves.

## Verification

The remote `main` baseline was `90fa6f1d6c2eeebfc4484bf5249d74e99d52fef9`. The tested archive was built on `codex/survival-alpha6-outdoor-pressure` from production commit `80a05d6f2bb53709e224d817dc5bace422731109`, with `sourceDirty=true` for documentation edits and two pre-existing untracked local notes. Tracked production sources matched the commit. Build UTC was `2026-11-13T07:46:09.8767555Z`. Archive SHA-256 was `c16554d58feda475bc484cdd55bf7fab3861112478efa99f9138de7e723003aa`.

MCP 6.2 compiled and reobfuscated the client with JDK 8u504 and Forge 3.4.9.171. The streaming ZIP verifier read all 267 entries. Archive inspection found only `zed/` classes. The isolated Prism Tekkit Dev copy launched the assembled Minecraft 1.2.5 / Tekkit Classic 3.1.2 client with Forge 3.3.8.164, FML 2.2.106.176, and Java 8u51. The pack reported 44 mods. The copied profile logged the existing EMC warning for item ID 169.

The portable calendar, assault and patrol schedule, and geometry suites passed. The MCP-side patrol pause and Peaceful cancellation checks, diagnostics history, and alpha.5 loadout weights and caps passed, including 10,000 fixed-seed assaults. The standalone Dev Bridge transport, deadline, world transition and restart checks passed.

| Fixed candidate in Tekkit Dev | Inserted out of 24 | Other outcomes | Mean / max per call |
| --- | ---: | --- | ---: |
| Torch-lit field | 13 | 11 sky-light rolls | 92 / 1,008 µs |
| Same field with the flag off | 0 | 24 vanilla-light rejections | 24 / 243 µs |
| Sealed glass-roofed house | 0 | 24 covered columns | 1 / 9 µs |
| Covered cell below a raised stone surface | 0 | 24 height rejections | <1 / 5 µs |
| Water | 0 | 24 unsuitable ground | 4 / 9 µs |
| Inside closed courtyard | 0 | 24 missing approaches | 14 / 59 µs |
| Outside closed courtyard | 11 | 13 sky-light rolls | 26 / 84 µs |
| Inside courtyard facing an open entrance | 0 | 24 missing approaches | 15 / 33 µs |
| Outside courtyard with open entrance | 8 | 16 sky-light rolls | 17 / 36 µs |

The installed stock Zombie rejected the lit field coordinate. The times include reflection and entity creation and insertion where applicable. Cleanup time is excluded. This small single-client sample measures the placement call and does not represent a whole-tick performance benchmark.

The first-quarter patrol paused in the native menu with zero attempts, resumed and inserted two mobs through the light exception in three attempts. One candidate failed the sky-light roll. Waxing gibbous inserted three in three attempts, including one equipped mob. After Save and Quit, both reopened saves had no encounter replay through tick 13,900.

The reopened waxing-gibbous save contained one Skeleton with a chain chestplate (item 303, count 1) and its encounter `NoDrop` flag in Anvil entity NBT. The interrupted full-moon run inserted one mob outside the wall in five attempts, with three approach rejections and one sky-light roll. Save and Quit reopened as `INTERRUPTED_ON_REOPEN` and reached tick 13,200 with zero new starts or patrol attempts.

| Full-moon wave-clear trial | Outdoor insertions | Equipped | Attempts | Rejections |
| --- | ---: | ---: | ---: | --- |
| Wave 1 | 4 | 0 | 49 | 31 approach, 10 sky roll, 4 cover |
| Wave 2 | 5 | 2 | 36 | 26 approach, 3 sky roll, 2 cover |
| Wave 3 | 6 | 2 | 36 | 28 approach, 2 sky roll |

Every assault insertion in that trial was outside the wall and within the default 12–16 block horizontal range. The probe removed each spawn to free active slots. Save and Quit reopened as `NIGHT_CONSUMED` through tick 14,260 with zero new starts or patrol attempts. The outcome measures placement through the three native wave windows with clearance, with player combat and survival pacing reserved for owner acceptance.

New moon produced no patrol or assault across dusk and on reopen through tick 14,145. With the flag disabled, the first-quarter patrol expired with zero insertions in 48 attempts. The recorded rejections were 47 vanilla-light failures and one obstructed sky column.

The local report filenames in `dist-zto/test-reports/` contain save IDs beginning with `4dd57b26` (first quarter and fixed coordinates), `b5bb083d` (waxing gibbous), `5d4c25e1` (interrupted assault), `b29b9540` (three waves), `a9dd9dce` (new moon) and `24d67b0a` (disabled).

## Owner acceptance

Compare an evenly lit field, a roofed shelter, a walled open yard, and an adapted cave in Normal Survival. At full moon, listen for the warning and check that enemies can gather outside while the interior stays usable. Try a moat, a RedPower-controlled entrance or a Tekkit compound with raised walkways. Compare open and closed entrances and watch where the mobs can actually move. IC2 armor, existing weapons and automated access remain ways to prepare for the same encounters. Record how readable the boundary feels on real terrain and whether the protected space still offers time for nighttime industrial work. Broader natural cave, irregular terrain and modded barrier coverage remains for playtesting.
