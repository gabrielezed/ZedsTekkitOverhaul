# Territorial activity and threat · 0.2.0-alpha.1

## Runtime contract

| Owner | Responsibility |
| --- | --- |
| `territory/TerritorialActivity` | Grid, bounded observations, lazy decay and interpolation. Pure Java. |
| `territory/TerritorialActivityService` | Bind the active local Overworld, sample the eligible Survival player and adapt the legacy biome input. |
| `territory/TerritorialWorldState` | Validate the world's versioned NBT and register it with MapStorage for saves. |
| `territory/ThreatAssessment` | Immutable activity and environment contributions, cell, observation status and assault budget decision. |
| `assault/SiegeDirector` | Use the assessment once at eligible natural dusk when configured, then freeze the budget and plan the regular three waves. |

A 64×64-block cell spans 4×4 chunks. Signed coordinates use floor division. The deterministic comparison below uses five simulated seconds per sample, a 21-block straight walking stride, a small work loop straddling x=64 and outposts 96 blocks apart. It counts distinct cells before eviction.

| Cell width | 1,000 walking samples | 720 work samples | 960 outpost samples |
| --- | ---: | ---: | ---: |
| 16 | 1,000 | 2 | 2 |
| 64 | 328 | 2 | 2 |
| 128 | 164 | 1 | 1 |

64 blocks trades some precision for fewer retained records while distinguishing both test outposts. One cell is observed per sample. Assessment uses four lookups and bilinear weights between cell centers. At a corner, each surrounding cell can contribute about a quarter of the activity. Fast crossing produces no extra observations. Fixed costs and a 512-record budget keep the dataset small during long exploration. Order is oldest observation first, including ties loaded in file order. At capacity a new observation evicts the oldest cell. Revisiting an evicted cell starts with one presence point.

## Evidence and elapsed time

- One sample per 100 unpaused local Overworld simulation ticks with an eligible awake, living Survival player. Position is only needed when a sample is due or to serve an explicit assessment. A brief passage contributes zero or a few points depending on sampling alignment.
- Presence gains one point per observation up to 24, equivalent to two minutes of successive samples. A return adds two points up to eight when the cell still has evidence, was last observed at least 3,600 ticks ago and receives three consecutive samples after reentry. Each uninterrupted visit can earn the return contribution once.
- On read or update, subtract one presence point per 1,200 ticks since the cell's last observation and one return point per 6,000 ticks. Clamp each at zero. An observation applies this decay before storing its new observation tick and values. Fractional decay is discarded when a cell is observed again. Sustained activity plateaus at 32 total. Repeated short crossings add presence at the ordinary sampling rate and do not qualify for return points.
- The independent counter advances by one per unpaused authoritative local Overworld tick, including active time without an eligible player, and saturates at the integer limit. Pause, title screen, offline time, remote replicas, Nether and End leave it stationary. NEI time edits, sleep clocks and lunar alignment contribute only ordinary elapsed ticks. Reopening continues from the saved clock. A save after a checkpoint serializes the latest counter and records together. Otherwise up to 1,199 uncheckpointed ticks can be lost across reopen.

## Save ownership and recovery

`<save>/data/zto_territorial_activity.dat` is a separate WorldSavedData entry. A local Overworld without the entry starts empty. Legacy World construction gives a portal destination a fresh MapStorage and reuses the ISaveHandler. The service keeps the Overworld state for that handler, suspends collection in other dimensions and reattaches it to the returning Overworld's MapStorage. A different handler opens its own dataset. No territory writes are issued by the service. Vanilla autosave and Save and Quit handle serialization.

The `data` compound contains integer `Schema=1`, `Grid=1`, `Dimension=0`, a nonnegative long `Clock` and an ordered `Regions` list. Each row stores signed integer `X`, `Z`, integer `Presence`, `Returns` and a long `Seen`. Unknown schemas, bad types, invalid coordinates, duplicates, out-of-order or out-of-range values and lists larger than 512 disable the dataset for the session and preserve the existing file. The bounded input reader checks the compressed file size, tag types, depth, field counts, names and list length before allocating records. MapStorage writes the usual compressed NBT format and retains the existing save cadence.

Score changes mark the dataset dirty at sample time. With history present, a checkpoint marks it at most every 1,200 ticks between score changes. Clock and timestamps can advance in memory between checkpoints. An untouched empty save needs no new file. The activity record holds only bounded counters, its last observation tick and a packed key. No blocks, machines, entities or raw trajectories are retained.

## Local threat and encounters

Threat is the sum of rounded interpolated activity (0–32) and environment (12 baseline, 18 swampland), for a maximum of 50. The vanilla 1.2.5 WorldChunkManager uses its biome cache and generation layers for this lookup. Swampland is a provisional traversal-risk distinction that can apply even in an unvisited cell. Values move by at most one activity point across adjacent blocks. A change of biome can contribute six more points independently of a grid boundary. Progression inputs await a reliable signal that preserves the benefit of advanced equipment.

Use `experimentalTerritorialThreat=true` in the canonical `ZedsTekkitOverhaul.cfg` to opt in. With the experiment enabled, threat below 18 selects `max(1, configured budget - 1)`. All other admissions, including unavailable assessments, use the configured budget. A default budget of 15 produces 14 or 15. Ordinary wave allocation divides 14 as 4/4/6 and 15 as 4/5/6. With very small configured budgets, ordinary rounding can leave a wave without a special slot. The lunar calendar owns eligibility and replay. Patrols, Enderkin role rolls, placement, caps, loot and enemy stats have their existing rules. Observation operates with either setting.

The read-only Dev Bridge query `territory.snapshot` returns the current cell and scores on the game thread. `assault.snapshot` includes the configured and selected budgets and the admission threat. Current location and admission location can diverge as the player moves. The persisted-observation flag describes the current cell's history in the loaded dataset, including evidence collected since the last save. A neighboring observed cell can still contribute to interpolation.

## Verification

Run `zto-tools/test-territory.ps1`, `zto-tools/test-config.ps1`, `zto-tools/test-lunar-calendar.bat` and `zto-tools/test-dev-bridge.ps1`. Run `zto-tools/build-and-deploy.bat` with the staging mods directory, then run `zto-tools/test-alpha6-runtime.ps1 -Modes territory-legacy,territory-threat,territory-both`. The probes use marked saves, native Save and Quit, runtime audio assertions and the pinned reobfuscated archive.

Representative world-data envelopes compressed to 124/323/833/2,969 bytes for 1/32/128/512 records. Four Java 8 heap-difference trials holding 128 populated datasets estimated 29–41 KiB per full dataset after GC. A 12,000-tick stationary trace issued 32 dirty marks. Portable batches of 100,000 operations measured 11–12 ns for position conversion, 56–120 ns for a record update, 21–93 ns for one lookup, 67–149 ns for the four-cell blend and 62–114 ns for capped insertion and eviction. These timings isolate the pure operations on this machine and exclude the biome manager, world tick and save I/O.

The runtime probe measured 231 ns, 9,761 ns and 294 ns per full assessment and diagnostic snapshot in three 512-call batches at the same position in a single tick. These measurements include the cached biome lookup and show the spread observed on this machine.

The tested production source was `ffa7871f694acaf8b71ba1947cd265bde2d2868a`. The candidate JAR SHA-256 was `0fd6bd61d86e51f2c817cc18254983e219d2bed8aecc81e71fb70bcf88f9f201`. The provenance marks the working tree dirty because two unrelated untracked documents were present. The tracked production source matched the commit. The client ran Java 8u51, Minecraft 1.2.5, Forge 3.3.8.164 and FML 2.2.106.176. MCP 6.2 used Forge 3.4.9.171 and packaging verified 304 archive entries.

The three territorial modes passed at that hash with audio volumes at zero and the sound backend absent. The legacy-format fixture began with no territorial file, recorded two cells, retained them across the Nether round trip and reopened a 145–146 byte dataset. The fresh-world probe recorded its first observation at simulation tick 100 and crossed into another cell via the player's collision-aware movement method. Pause and forward and backward world-clock edits preserved the territorial elapsed-time rules. At natural full moon the legacy mode selected 15 and the two experimental modes selected 14 with local threat 12. Movement after admission held the same selected budget. The combined fixture inserted the Skirmisher and Breacher within the three-wave budget.

The same archive passed all six alpha.6 placement modes, all six alpha.8 Enderkin modes, the alpha.7 combat, Pulse and both Creative catalog probes, and all five configuration startup cases. The configuration probe confirmed zero music and effects volumes and an absent sound backend after attempting playback. The isolated Prism lifecycle passed nine Hopper restart assertions and twelve distillation restart assertions. Crafting passed the 3×3, 5×5 and hotbar-lock checks in both runs. The hard-protection fixture was unavailable under the installed Inventory Tweaks profile. All four Prism launches stopped gracefully and the runner restored the original isolated profile archive.

The multi-cycle regression completed three successive full-moon encounters, preserved replay protection across a rewind and reopen, classified the installed NEI forward jump, and admitted later natural dusks. It also checked lunar alignment, Peaceful and Creative rejection, interrupted saves and the Nether round trip. The fixture waits for its clock writes to reach the authoritative snapshot before advancing assertions.

## Follow-ups

For alpha.2, compare pacing in ordinary Survival play, including established infrastructure, settlement-free travel and different natural environments. Investigate regional environment signals, feedback and how composition would interact with advanced gear once the intensity envelope is understood. Meaningful progression requires a bounded independent signal. In 0.3, construction changes, provenance, automation-modified blocks, older worlds and protected interiors need separate investigation before activity can contribute to any settlement recognition. In 0.4, SITE_SIEGE would consume an independently recognized place. PLAYER_ASSAULT continues to follow the player.
