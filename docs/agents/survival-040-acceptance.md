# Survival 0.4 completion acceptance

The [fortress warfare specification](survival-040-spec.md#26-acceptance-test-matrix) owns the acceptance scenarios and definition of done. This record maps those scenarios to the pinned Minecraft 1.2.5, Tekkit Classic 3.1.2 singleplayer probes. The graphical and owner results remain **NOT RUN**. The candidate retains `0.4.0-alpha.4` while the missing acceptance evidence is collected.

## Engineering gates

| Gate | Implementation | Automated evidence and remaining work |
| --- | --- | --- |
| A — Tactical correctness | `SiteSiege`, `SiteApproach`, `TerrainApproachSearch`, `PerimeterApproachTask` | A lost route can trigger at most eight staggered samples for the same site. Infantry can bypass locally, join a nearby verified exterior lane, or relinquish tactical control. A Breacher can switch once to a nearby verified wooden entrance. Tanks retain their original collision and damage limits. Skirmishers can hold ranged frontage. Unsupported industry still falls back to infantry within the frozen budget. Owner checks cover long detours, live movement and readable roles. |
| B — Aerial correctness | `AerialEcology`, `AerialEncounter`, `EntityAerialPredator` | A high or maximum force can reserve one reinforcement slot for a Skelebird. Its insertion uses an exterior site column, loaded collision checks, defender exposure and the shared aerial cap and encounter exclusion. Save/Quit, native chunk eviction and the former wild timeout are exercised by `TacticalSiteSiegeProbe`. `LunarAerialProbe` covers glass, population cap and an active wildlife group. Verify restored combat and site airspace visually across the remaining layouts. |
| C — Natural play | `ZtoConfig`, `SiteSiegePolicy`, `SiegeDirector`, `AssaultWorldState` | The existing key defaults to enabled when absent. Explicit values survive configuration migration. The calendar still selects one repeatable full-moon opportunity every three cycles. Cooldown, local loaded pressure, geometry, the major boundary watermark and ordinary assault fallback still apply. The nominal opportunity is every 24 game days. The successful encounter interval across representative Survival worlds remains unmeasured. |
| D — Release readiness | `SiegeCensus`, `GuiSiteSiege`, pack and focused probes | Recent census records survive reload. Headless probes cover separate sites, post-release mobs, repeated native saves, malformed optional census data, limited read budgets and native compatibility. Graphical gameplay, memory trends and a sustained natural-history workload remain open. |

The census retains the newest 256 successful actor insertions in the Overworld. Each record has a ZTO-generated identity, mobilization identity, site and manual/natural origin. Native entity NBT carries the optional actor identity. Restoration reconciles on its first native update and adds zero insertions. Loaded counts require the observed native entity in its loaded chunk list. An observed actor in a missing chunk is last-known unloaded. A missing or stale observation is unknown. Only observed health depletion establishes death. Retention eviction affects history and leaves native actors intact. Older actors with only `ZTOSiegeSite` remain persistent and contribute to the existing loaded population caps. Optional census data does not gate natural admission or change the lunar save record.

The Skelebird keeps its native type, health, position and movement in the chunk save. Restoring begins with safe cruise steering and clears transient target and flock state. Tactical release preserves the siege origin, suppresses ordinary wildlife expiration and distance despawn, and returns the bird to its local hostile behavior. Native death, bone drops and Peaceful behavior use the established species code. Ordinary Fugl and Skelebird lifecycle and player-assault allocations keep their existing rules. The Fugl continues its wildlife and chicken-hunting role. Structural damage remains opt-in.

The natural warning uses the existing cave ambience at a verified exterior staging column. The warning window precedes the first deployment and later successful waves have a single arrival cue. The role-specific wood, Tank, Saboteur and flight sounds still come from their existing implementations. The monitor distinguishes the live site threat sample from the admission assessment and reports reinforcement exhaustion separately from tactical control and physical casualties. Audible clarity awaits the owner pass.

## Native evidence and build identity

Clean source build `01c4d8fccb4f6401f2e13a9c8d36d04da97df94e` used MCP 6.2 and compile Forge 3.4.9.171. The mod JAR SHA-256 is `98fd4263da1dbb18a6d828940731404c52ee2200585b4353adcaa5f0cfd79640`. The ZTOAgent hash is `f23e71c3c30f0c6b3c7e43bbc9daa152387de3b2d4fcb5289371b94e02df2ffc`. MCP client compilation, reobfuscation and streaming verification of all 492 entries passed. The isolated headless runtime used Java 8u51 and the pinned pack overlay with Forge 3.3.8.164 and FML 2.2.106.176.

Portable checks passed for configuration and migration, calendar and major scheduling, outdoor approaches, Territorial Activity, force selection, Dev Bridge transport, documentation links, distillation and alchemical conversion. These results exercise the typed settings, including fresh defaults and explicit legacy choices. Native pack fixtures test admission and fallback separately from the settings parser.

| Native campaign | Result on the recorded artifact |
| --- | --- |
| Full `-Pack` | **PASS on retry.** The first run passed the siege, settlement and industrial probes, then failed the existing PLAYER_ASSAULT wetland floor. The two swamps yielded 2 + 2 insertions against a minimum of 5. The identical JAR yielded 2 + 4 and the full suite completed on retry. The fixture seeds the director, and native per-entity light rolls still vary. Both runs retain the original threshold. |
| Site siege, escalation, terrain, industrial, persistence and completion cases | **PASS** within `-Pack`, including the restored bird's slashing contact and a bridge disconnected after staging. |
| Settlement, provenance, partial footprints and protected interiors | **PASS** within `-Pack`, including the independent workshops and clipped compound. |
| Existing Enderkin, Thief cargo, aerial ecology, PLAYER_ASSAULT arbitration and recovery | **PASS** within the completed pack run, subject to the wetland repeatability issue above. |
| Resource conservation and third-party compatibility | **PASS** for the tested IC2, BuildCraft, RedPower and ZTO transport, energy, inventory, cargo and fluid paths. The pack ran the installed and reduced compatibility overlays. |
| `-Focus modded-profile` | **PASS.** The native mixed equipment probe associated all 19 fixtures at a peak of 169 detail reads with the IC2 adapter unavailable. |
| Graphical and owner acceptance | **NOT RUN.** Host automation is prohibited by the repository policy. The separate graphical campaign remains open. |

The history fixture ran four finite manual mobilizations across two recognized workshops. It retained 16 native infantry through two Save/Quit cycles, checked the 579-byte compressed census, admitted a natural siege near the residual population and reopened it without replay. Its first full native save and flush took 250 ms. The Skelebird fixture checked tactical release, native motion beyond 1,100 ticks, native chunk eviction, a single restoration at the saved health, a successful slashing contact after reopening, malformed optional actor metadata and one confirmed death. The entrance fixture replaced a closed door during windup and observed the same Breacher walk to and open the second door. The waterfront fixture severed a bridge after deployment and observed a living vanguard with further insertions blocked.

### Performance samples

Times are microseconds from the headless simulation on the same workstation. The fixture tick covers native entity motion, territorial and settlement ticks, and director work. The snapshot includes the bounded census lookup. Samples exclude graphical rendering and sound playback. Dashes mark distributions with fewer than 100 samples for p99.

| Workload | Samples | p50 | p95 | p99 | Max |
| --- | ---: | ---: | ---: | ---: | ---: |
| Two-site history fixture before mobilization | 256 | 0.50 | 4.90 | 9.80 | 57.40 |
| Same fixture after four mobilizations | 256 | 88.40 | 155.70 | 593.00 | 622.20 |
| Same fixture after first reopen | 256 | 48.70 | 99.30 | 305.20 | 361.90 |
| Same fixture after second reopen | 256 | 49.90 | 83.50 | 302.00 | 1834.10 |
| History snapshot after four mobilizations | 256 | 6.30 | 10.00 | 19.70 | 32.90 |
| History snapshot after first reopen | 256 | 1.60 | 2.40 | 6.40 | 26.90 |
| Hard/MODERATE director ticks | 1690 | 1.60 | 5.30 | 15.70 | 388.50 |
| Hard/MAXIMUM director ticks | 1690 | 1.00 | 7.90 | 85.50 | 777.80 |
| Hard/MAXIMUM planner samples | 284 | 1.90 | 95.20 | 176.20 | 773.00 |
| Combined force native hostile ticks, peak seven actors | 96 | 194.50 | 275.90 | — | 760.70 |
| Skelebird native update within the combined fixture | 96 | 27.60 | 53.00 | — | 572.00 |
| Unavailable bridge terrain planner samples | 55 | 17.40 | 200.20 | — | 588.20 |
| Mixed equipment recognition, generic-only idle | 3240 | 0.10 | 0.20 | 0.30 | 23.40 |
| Mixed equipment recognition, generic-only sampled | 360 | 7.20 | 53.70 | 78.50 | 96.40 |

The narrow waterfront fixture used the same shape and pinned runtime on the baseline and candidate. Across 1,690 active director ticks in each run, baseline → candidate p50 was 2.30 → 1.80 µs, p95 was 11.80 → 9.30 µs, and p99 was 127.10 → 94.40 µs. Native hostile p95 was 151.70 → 101.10 µs and p99 was 473.60 → 210.30 µs, with a peak of eight loaded actors in each run. Native paths took 9.66 ms across 123 calls and 3.89 ms across 106 calls. These single-run distributions describe the matched fixtures. Long-run memory growth and wall-clock frequency in representative player worlds remain unmeasured.

### Milestone definition of done

| Specification items | Status |
| --- | --- |
| 1–5. Distinct event, fixed site identity, bounded calendar and safe approach | Implemented. Verified in native cabin, compound, covered, waterfront, partial and fallback probes. |
| 6–8. Objectives, responsive defenses and mob roles | Implemented. Native entrance changes, specialist combat, ranged roles and one limited aerial slot verified. Graphical playtest and additional terrain layouts pending. |
| 9–10. Reversible disruption and bounded structural impact | Implemented. Native cable, pipe, energy, fluid, unsupported tiles, drop recovery and damage-budget probes exercise conservation. |
| 11–12. Save lifecycle and independent PLAYER_ASSAULT | Implemented. Native release, Save/Quit, chunk eviction, persistence, cooldown, replay and ordinary assault probes exercised. |
| 13–14. Population, performance, persistence and debugging | Implemented with bounded queries and retention. Native observations and distributions above. Long-duration memory measurements and graphical monitor review pending. |
| 15–16. Player counterplay and owner gameplay | Pending owner playtest. The pack fixtures cover underlying movement, collision, advanced equipment and machinery. |

## Specification scenarios

| Spec | Implementation and native probe | Evidence to collect in the graphical client |
| --- | --- | --- |
| A. Unfortified cabin | `SiteSiegeProbe` verifies recognition, site admission, frontage, movement and native combat. | Approach remains readable while the defender fights and retreats. |
| B. Walled castle | `TacticalSiteSiegeProbe` exercises doors, gates, alternative doors and physical Breacher action. `SiteSiegeEscalationProbe` checks a narrow Tank route. | Test a larger two-gate perimeter with both gates changing during combat. |
| C. Underground bunker | `TacticalSiteSiegeProbe` checks no legal surface insertion. `LunarAerialProbe` checks aerial exclusion under cover. | Watch a sealed bunker during a natural opportunity and an exposed exit during a debug call. |
| D. Open industrial facility | `ModdedSiteSiegeProbe` uses a recognized pack-native workshop and supported equipment. | Verify the physical Saboteur approach and pressure from exposed working areas. |
| E. Enclosed factory | `ModdedSiteSiegeProbe` checks sealed access, lease interruption and resource conservation. | Verify that a covered work area blocks Skelebird attack and ordinary machine use remains available. |
| F. Elevated treehouse | The pinned probes cover elevated construction and loaded sky/ground distinctions. An actual treehouse combat fixture remains pending. | Verify eligible ground access, open-air exposure and no internal insertion. |
| G. Mountain fortress | `TerrainSiteSiegeProbe` covers supported uneven routes and turns. A mountain fortress combat fixture remains pending. | Check cliffs, stairs and a high parapet during a natural or debug siege. |
| H. Dispersed compound | `SiteSiegeProbe`, `ManualPartialSiteSiegeProbe` and `ModdedSettlementProbe` cover multipart identity and partial exterior placement. | Verify site assignment when moving between buildings. |
| I. Two nearby outposts | `SiteSiegeProbe` checks separate identities. `TacticalSiteSiegeProbe` checks independent retained histories across two workshops. | Verify monitor selection, independent cooldown and no transfer of objectives. |
| J. Player leaves | `TacticalSiteSiegeProbe` checks absence, cancellation and retained actors. | Retreat, return and confirm actual survivors explain residual pressure. |
| K. Save during siege | Tactical and industrial probes check world reopen, interrupted leases and one-time cargo recovery. The completion probe tests the aerial survivor through chunk unload and Save/Quit. | Save mid-encounter and check returning native actors, combat and quiet recovery. |
| L. Full-moon collision | `SiteSiegeProbe`, `TacticalSiteSiegeProbe` and `LunarAerialProbe` check one major event, fallback and replay. | Observe natural admission without pressing the manual call button. |
| M. Strong late-game player | `JetpackWeatherProbe` checks pinned IC2 capabilities and resource use. The late-game fortress fight remains pending. | Play the same layout with advanced equipment and verify progression still matters. |
| N. Unsupported machines | `ModdedSiteSiegeProbe` checks protected inventory, native equipment, unknown tiles, IC2 energy and BuildCraft fluid. | Try a qualified disruption with an unsupported machine beside the approach and inspect both inventories. |
| O. No valid route | Ground and terrain probes cover unavailable terrain, obstruction, missing bridges and safe rejection. | Destroy a supported bridge mid-approach and verify that surviving actors remain and no route appears through water. |

## Owner playtest

Use a disposable Tekkit Dev copy, Minecraft 1.2.5 and Tekkit Classic 3.1.2. Start in Overworld Survival on Normal or Hard. Open **Numpad 1 → SITE_SIEGE** for the recognized site, natural forecast, force and diagnostic pages. Use **Call Site Siege** and **Cancel Test Siege** when a manual probe is specified. Keep production worlds separate from the test copy.

| Test | Steps | Expected observation and failure condition |
| --- | --- | --- |
| 1. Natural admission | Set `playerAssault=true` and `experimentalSiteSieges=true` in `minecraft/config/ZedsTekkitOverhaul.cfg` while the game is closed. Restart and occupy a recognized house with a loaded exterior approach. Remain nearby through the monitor's next full-moon opportunity. | The dusk warning and site approach start without a manual call on a qualifying boundary. Record any rejection. Duplicate mobilization after reopening or unsafe placement fails. |
| 2. Changing entrances | Build two wooden entrances in a walled compound. Choose **AUTO** and call a siege on Hard. Close the first entrance during approach and leave the second usable. Repeat by replacing the first closed door during Breacher windup. | Infantry tests a verified alternate or releases its assignment. An eligible Breacher may take the other entrance if it is locally reachable. Repeated pressure on an invalid target with active expensive navigation fails. |
| 3. Aerial pressure | Give a roofed factory an uncovered courtyard and clear high airspace. Select **MAXIMUM**, call a siege, and stay near the site in an exposed position through the second wave. Try the same fight under a solid or glass roof. | A qualified slot can produce one Skelebird. Cover blocks the attack. A rejected air slot can remain unused. Internal insertion or contact through a roof fails. |
| 4. Aerial survivor | Let an inserted bird survive. Cancel or wait through the end of tactical control. Save, quit and reopen near its last position. Leave and return far enough to unload its chunk in a second pass. | The same species returns as a local hostile, keeps its health and can fly. A vanished living actor, duplicate, or stale controller fails. |
| 5. Ground survivors | Leave some infantry and specialists alive. Let the final window close, then save and reload. | The census shows released actors separately from confirmed deaths. Surviving ground actors remain in the world and new waves do not replay. |
| 6. Multiple sieges | Call several encounters at two recognized workshops, leaving survivors in both. Save, quit and reopen twice. Check the census for each site when selected. | Loaded counts track observed actors. Unloaded or unverified actors remain separate. Duplicated actors, increasing insertion counts on reload, or new waves from stale state fail. |
| 7. Waterfront | Connect an island factory to shore with a supported bridge. Try a narrow bridge and a wider one. Break a segment during combat and watch the monitor and the remaining actors. | Ground attackers use actual support. A Tank needs its full footing. An exposed shoreline may admit a bird. Movement through unsupported water or loss of living actors due to reassessment fails. |
| 8. Industrial safety | Equip a workshop with a supported IC2 or BuildCraft target and an unsupported nearby machine. Call **AUTO / MAXIMUM**, let the Saboteur start, interrupt it and check the machinery and recoverable items. | The supported effect is bounded and reversible. Loss, duplication or persistent disruption of the unsupported machine fails. |
| 9. Advanced defender | Repeat an earlier fight with advanced Tekkit gear such as QuantumSuit and an Electric Jetpack. | The same pressure is easier to manage through gear and mobility. Arbitrary immunity or damage changes that erase progression fail. |
| 10. Recovery | Listen from the site during warning and through the first reinforcement. After the last wave, wait nearby and compare what you hear and see with the force page. | New arrivals cease. Remaining danger corresponds to actual hostile creatures. Timer-driven disappearance or an incorrect cleared-state display fails. |

## Release boundaries

Balance remains provisional for frequency, aerial selection, exposure, relative role distribution and timing. Dedicated multiplayer certification, Survival 0.5 exploration, End progression and additional machines remain deferred by the milestone scope. The remaining beta gates include the exact open layout and combat cases in the matrix, repeatability of the wetland acceptance threshold, sustained heap measurements, natural frequency in representative worlds and owner acceptance.
