# Survival 0.2.0-alpha.2

Alpha.2 continues [alpha.1](survival-02-alpha1.md) on top of PR #31. The experiment makes place and local history affect the opening of a natural full-moon encounter. It uses existing mobs and placement rules. Territory exposes an immutable assessment for future consumers. The assault director owns the wave decision and targets the current player's vicinity.

## Environmental identity

| Center class | Interior contribution | Legacy biomes and rationale |
| --- | --- | --- |
| Baseline | 12 | Default for plains, wooded, aquatic, snowy and other terrain. These biomes offer varied local cover and routes. Unclassified biome inputs also use this value. |
| Exposed | 16 | Desert and Desert Hills have sparse cover. Extreme Hills, Hills Edge and Ice Mountains introduce exposed slopes and constrained approaches. |
| Swampland | 18 | Swampland retains its alpha.1 value. Shallow water and broken terrain can complicate movement and retreat. |

The 1.2.5 Overworld uses `WorldChunkManager.getBiomeGenAt(x, z)`. The lookup's legacy biome cache holds biome-layer arrays for 16×16 positions. Misses fill those arrays directly from generation layers, independently of the chunk provider. The separate `World.getBiomeGenForCoords` can consult a loaded chunk's biome bytes. The territorial assessment uses the generator's identity consistently across loaded and unloaded positions and applies only in the local Overworld.

The score uses the center twice and four samples eight blocks away on the cardinal axes once each, rounded to the nearest integer. This softens abrupt seams with five lookups spanning at most three biome-cache tiles. The center still supplies the diagnostic category. A profile threshold can still fall on a seam. Admission freezes the result for the entire encounter.

## Activity, retention and decay

The grid remains 64×64 and the cap remains 512. An eligible Survival player is sampled every 100 simulation ticks. Each pair of consecutive samples in the same cell contributes one presence point, up to 24. A return requires at least 3,600 ticks since the last credited observation, surviving historical activity and four consecutive samples in the returning cell. It adds two return points, up to eight. The sampling phase can move a threshold by one interval. Returning after a player or world binding change starts a fresh qualification window.

| Pattern | Expected result at 20 TPS |
| --- | --- |
| One-sample passage, alternating samples across a cell boundary | Neither qualifies for a presence point. |
| 10 seconds of samples in a cell | One presence point. |
| Two minutes collecting or building in one cell | 12 presence points. |
| Four minutes of industrial work or stationary play in one cell | Presence saturates at 24. The model reads player presence and position. Building, inventories, EMC, gear and placed machines supply no inputs. |
| Spaced return to an outpost | After 20 seconds of samples on a qualified return, the outpost gains two return points. Distant outposts have independent records. |
| Once-visited region and recovered region | A single point expires after one minute away. A saturated presence score can decay to zero after 24 minutes away, and a saturated return score after 40 minutes away. Expired records may remain as eviction candidates. |

Decay is lazy. Presence loses one point per 1,200 active Overworld ticks since the last credited observation and returns lose one per 6,000. On a new credit, decayed values become the new stored values. Pause, time spent with the save closed and time in the Nether add zero ticks. NEI forward and backward edits contribute only actual simulation ticks. Vanilla saves persist the clock at activity changes and periodic dirty checkpoints at most 1,200 ticks apart while the dataset contains records. A save may reopen at the last persisted checkpoint.

At capacity, alpha.1 evicted the oldest observation in constant time. Alpha.2 evicts the smallest decayed score, with oldest observation as the tie-break. The scan stops at the first expired row and visits at most 512 records. Each qualifying new region can enter. An older strong record can survive a more recent weak one, and continued exploration eventually ages a strong record into the same pool. A single-player journey that qualifies hundreds of fresh cells leaves enough time for a prior presence score to expire naturally.

The WorldSavedData schema remains 1. Previous cell records, values and observation order load directly. Existing protections for future schema versions and corrupt or oversized files still apply. The lunar and assault save formats are unchanged.

## Full-moon admission

Total threat is the blended activity (0–32) plus the blended environment (12–18). It ranges from 12 to 50. Bilinear interpolation around the four surrounding cell centers smooths the activity contribution. The director chooses the profile once on natural full-moon admission.

| Decision | Criterion | Planned waves with configured budget 15 |
| --- | --- | --- |
| Quiet | Threat below 16 | **2 / 6 / 6**. Reduce the budget by one, to at least one. Move up to two opening positions to the second wave. |
| Ordinary | Threat 16–23 | **4 / 5 / 6**, the original distribution. |
| Elevated | Threat at least 24 | **6 / 5 / 4**. Move up to two positions from the last wave into the opening. |
| Disabled or assessment unavailable | Switch off or no usable assessment | **4 / 5 / 6**, the configured budget and original distribution. |

Transfers preserve all originally nonempty waves at low budgets. The practical difference is an opening that gives the player more time to respond in quiet areas, and more immediate pressure with a smaller finish in elevated areas. Placement can reduce the observed difference when clearance or population limits reject candidates. The Enderkin switch still controls substitution independently and the seed, phase, day and wave number determine eligibility. Patrolling, hostile caps, retries, outdoor placement, protections, replay, entity stats and drop rules retain their existing contracts.

`territory.snapshot` exposes the local inputs. `assault.snapshot` reports `admissionProfile` together with the frozen assessment, configured and selected budget, planned wave allocations, insertion evidence and rejection counters. Before admission the profile is null. `DISABLED` and `ASSESSMENT_UNAVAILABLE` identify the two legacy-plan decisions. Both queries use the existing authenticated read path. The runtime checker uses only that path and requires a fresh marked test save. Checkpoint acknowledgments are files used by the external test harness.

## Verification

Run the [test map](testing.md) with the reobfuscated JAR deployed to the staging mods directory. For a local manual snapshot, use the path printed by the read-only Bridge in the isolated client. The following commands use the existing PowerShell client.

```powershell
.\zto-tools\test-territory.ps1
.\zto-tools\test-alpha6-runtime.ps1 -Modes territory-threat,territory-elevated,territory-legacy,territory-both
.\zto-tools\zed-dev.ps1 query territory.snapshot -Discovery $discovery -Json
.\zto-tools\zed-dev.ps1 query assault.snapshot -Discovery $discovery -Json
```

The automated runtime probes accelerate ordinary simulation and use marked saves. The controlled placement probe removes hostiles between attempts. It measures insertion and lifecycle completion under that fixture. Its peak active count is specific to the fixture. Owner combat and defensibility require play.

The candidate artifact used production sources at `b199590fef497dd63a0ac6f8b493704cda11956d` and SHA-256 `6a2e27c558faf78069c1f7ba4057bb1adb8ebbd46d9ba0137b6b5f84dda400b0`. Its build sidecar reports a dirty checkout with test, documentation and local untracked files. MCP 6.2 client compilation, Forge 3.4.9.171 reobfuscation and validation of 305 archive entries passed.

The portable checks passed for signed cell boundaries and interpolation, qualified sampling and returns, saturation, decay, retention, v1 save rows, malformed NBT, threat thresholds and profiles for budgets 1–24. The 512-cell eviction case used synthetic activity traces. Configuration migration, lunar and patrol schedules, outdoor approach rules, the 10,000-seed Enderkin selection sweep and Bridge transport passed. The Hopper, distillation core, layout and model checks, crafting transactions, EMC, alchemical conversion, Foundry rotor and multiblock validation also passed.

### Tekkit Dev observations

On 2026-11-16, the staged Tekkit Classic 3.1.2 client reported Forge 3.3.8.164, FML 2.2.106.176 and Java 1.8.0_51. Both read-only Bridge probes passed with zero music and effects volume and an uninitialized SoundSystem. Authenticated HTTP agreed with in-process snapshots through sustained presence, admission and resolution, the Nether, Overworld return and native Save and Quit/reopen. The world IDs were 1, 2, 3 and 4 across those world bindings. The discovery file disappeared after each graceful exit.

| Admission | Frozen threat | Planned and inserted | Composition and active counts | Completion and placement feedback |
| --- | --- | --- | --- | --- |
| Quiet | 12, ordinary terrain with zero blended activity | 2 / 6 / 6, all 14 | 14 skeletons on Hard, peak 1 of cap 8 | `WAVES_COMPLETE`. 14 sky-light roll rejections. |
| Elevated | 25, ordinary terrain with 13 blended activity | 6 / 5 / 4, all 15 | 15 skeletons on Hard, peak 1 of cap 8 | `WAVES_COMPLETE`. 18 sky-light roll and 3 sky-exposure rejections. |

Both runs used the same disposable arena seed. The quiet run admitted at a different cell and used the original arena for combat, so the exact rejection counts also reflect the runs' timing and target positions. The quiet profile remained frozen while the current threat rose to 23. A live lookup in fresh cells on the same seed returned 16 for exposed terrain at (-1024, -1024) and 18 for swamp at (256, -1024). While the player stayed away for 3,600 Overworld ticks, home presence fell from 21 to 18. The next qualified return yielded two return points and preserved the same environmental class. Independent fresh-world and adjacent-cell movement checks also passed. A matched same-tick check equipped all four pieces of the installed IC2 QuantumSuit and observed the same activity, environment and total threat of 19.

The four combinations of territorial adaptation and Enderkin passed in `territory-legacy`, the two Bridge modes, `ender-waves` and `territory-both`. The mixed quiet encounter inserted both experimental roles within its budget of 14. With territory off, the original 4 / 5 / 6 plan also inserted both roles. The Bridge-disabled runs checked for an absent listener and discovery announcement. The recurrence probe passed multiple natural full-moon admissions, time-skip suppression, same-night replay protection, peaceful and Creative rejection, and native save and dimension interruption.

The full-moon, multi-wave, first-quarter patrol, waxing-gibbous patrol, disabled outdoor pressure, Enderkin fallback and hostile-cap probes passed. The mixed-assault fixture verified that the Breacher left the closed wooden door intact. The placement fixtures exercised protected interiors and existing machinery checks. All five configuration startup scenarios passed, covering fresh defaults, legacy import, restart, canonical precedence and a blocked write. Music and effects remained at zero and no audio backend initialized after playback calls.

The isolated Prism lifecycles verified the running archive hash. Hopper passed all nine restart assertions, distillation passed all twelve, and both runs passed 3×3, 5×5 and hotbar-lock crafting checks. The Inventory Tweaks hard-protection fixture was BLOCKED under the installed profile. All four launches exited gracefully. The original isolated JAR and audio options were restored byte for byte. The local lifecycle reports are `autonomy-ZTO-Test-bafcded69b8d43b1842640a99d0653f6.json` and `autonomy-ZTO-Test-6105aac8ce234b08b001359fb83932a0.json` under `dist-zto/test-reports`.

Local HTTP evidence is in `dist-zto/test-reports/alpha2-http-ZTO-Test-ffecef3e66c4473c8e68b1304c182a77.json` and `dist-zto/test-reports/alpha2-http-ZTO-Test-318fead25e0e45bea255cd1943b586cb.json`. These are ignored build outputs. Owner combat, powered entrances and perceived difficulty have status NOT RUN.

### Cost measurements

| Check | Local observation |
| --- | --- |
| Pure Java, three warmed batches of 100,000 operations | 77–79 ns per qualified update, 22–24 ns per cached-record lookup and 71–80 ns per four-cell blend. Two observations constitute one qualified update. |
| Capped insertion | 92–190 ns with expired records first. A synthetic full dataset with positive scores in every row cost 11.2–12.1 μs per insertion, including the 512-row scan. Alpha.1 removed the oldest row in constant time and had no equivalent scan. |
| Packed data and cadence | 126 / 328 / 846 / 3,058 compressed bytes for 1 / 32 / 128 / 512 records. 30 dirty marks in the 12,000-tick stationary trace, down from 32 in alpha.1. |
| Warm biome lookup and snapshot | 331–543 ns combined in the quiet-mode Tekkit Dev probe. This includes all five biome-cache hits, activity interpolation and direct snapshot construction. Alpha.1 recorded 231, 9,761 and 294 ns in the earlier one-lookup fixture. |

Four retained-heap trials holding 128 populated ledgers estimated 28–40 KiB per 512-record ledger, comparable to the alpha.1 estimate of 29–41 KiB. Cold biome-layer performance remains unmeasured. Additional HTTP serialization happens on requests and adds one scalar field to the existing bounded assault response.

## Owner playtest

1. Use separate copies of a Survival save with the experiment enabled. At natural dusk, compare a fresh ordinary area, exposed terrain or swamp, and an established location in otherwise similar terrain. Observe the start and third wave from a safe position or inspect the wave plan through the Bridge. Repeat on a fixed seed with the experiment disabled.
2. Spend a few minutes mining or building a small shelter, then operating a powered workshop. Compare similar dwell times. Check whether the earlier pressure remains manageable and whether closed, protected interiors and interiors around powered machinery still provide a safe retreat.
3. Travel among two distant outposts and several biomes. Revisit an outpost after three minutes of Overworld simulation and compare the two observations. Leave a mature region longer and see whether its pressure changes on the next natural full moon.
4. Try the same encounter in basic armor and in advanced Tekkit equipment. Judge whether the opening is noticeable, whether the close feels too light, and whether stronger equipment still gives satisfying advantages.

Alpha.3 can calibrate the thresholds and the two-position transfer against this feedback. Measure which terrain boundaries feel meaningful before applying regional decisions to other existing encounters. Settlement recognition, construction provenance and SITE_SIEGE remain later work.
