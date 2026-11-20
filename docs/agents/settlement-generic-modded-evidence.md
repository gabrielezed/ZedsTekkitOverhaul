# Generic settlement evidence

Settlement Recognition combines construction, bounded local function, successful player use and regional activity. The original classifier paired a vanilla material and utility list with a pinned IC2 adapter. That architecture required registration knowledge before industrial evidence could enter the decision. The survey now applies the same model to the loaded block registry.

## Observations in Tekkit Classic 3.1.2

- A positive, matching placement record supplies verified authorship. Solid blocks that render as normal blocks can supply construction without registration in a ZTO list. Vanilla construction and utilities retain their established treatment. Device placements are also visible as provenance even when their shape or role keeps them outside the structural component.
- The cached legacy block flags identify tile-capable blocks, including metadata-dependent Forge overrides. The survey reads the registered variant and existing tile map only when relevant. A usable tile has the sampled world, coordinates and a current lifecycle. A tile with an inventory is stronger when its native slot count is positive. Contents and transfers stay untouched. BuildCraft item pipes can expose the inventory interface with zero slots.
- The existing pinned local Survival activation hook checks the success result and a newly opened container or screen. Either can supply one observation of the clicked block. The `SiteUse` listener checks player, world, reach and current block/tile identity. Modded uses are distinct by coordinate, cooled down across 20 eligible ticks and retained for 1,200 ticks in memory. A new tile instance at the same coordinate invalidates its earlier use. A non-tile GUI is linked by current block ID. Vanilla utility behavior retains its historical use thresholds.
- A full registered device or positive-size inventory can supply endpoint evidence when supported by selected construction. A device can rest on the floor, touch walls or share one horizontal hop of up to two blocks with a directly supported endpoint inside the construction envelope. Thin or ambiguous tiles add supporting evidence alongside an endpoint. The resulting proximity describes local organization. Verified item, fluid, kinetic or electrical paths require their own native evidence.
- The IC2 adapter retains its exact role, container and wiring checks. Supported unfoamed copper groups are tested within the completed window, with a 12-tile limit and compatible source/sink faces. The generic path also observes the same IC2 equipment, including variants outside that profile. Exact paths do not prove current flow or machine output.

## Qualification and persistence

The same local construction and movement requirements apply to cabins and industrial sites. An authored open workshop can qualify with the existing local activity and at least three associated devices, two directly supported endpoint types and local grouping. Repeated devices of one class cannot satisfy the diversity gate by volume. Device use can satisfy the ordinary cover-or-use gate. The exact IC2 wiring rule continues to apply.

Old structures using familiar construction can qualify after distinct modded device openings and local movement. For a wholly unfamiliar material, a bounded and mostly filled single-material floor of at least 16 blocks may supply contextual construction. The floor must extend at least three blocks in both dimensions, differ from the substrate beneath it, and fit within the sampled window. This inference also requires local activity, distinct device use and diverse associated equipment before confirming a site. Unknown authorship stays unknown. An isolated device, a partial sample and unattended generated blocks are insufficient for promotion.

Construction defines the footprint. Installed equipment and long runs never supply territorial extent by proximity. Earlier schema 2 registries and their save-local IDs remain readable, with existing compound overlap, retention and contraction rules. Previously observed shapes inside stored bounds are taken into account during contraction checks. Use evidence starts fresh on reopen. SiteQueries, Protected Interior and SITE_SIEGE continue to use the established read-only identity and geometry contracts. [Site siege compatibility](survival-040-alpha1.md)

## Work limits

A survey samples a `13 × 9 × 13` window on the simulation thread. Each update spends up to 169 observations on block IDs, selective metadata, loaded chunk retrievals, tile lookup and slot counts. Loaded-chunk presence checks run before each resumed slice. An absent chunk ends the survey with incomplete evidence. Registry flags are cached and spatial work runs on sampled arrays. There are no world-wide tile scans. Dense slices can resume on an additional tick, and the usual survey starts are 100 eligible ticks apart. Generic and IC2-only observations have separate counters. Protection can still use its independent budget on an idle survey tick.

## Acceptance evidence

Build and gameplay source was `90aa3ccc1cd308370dabee544d045add4bca1f4d`. The packaged mod SHA-256 was `ec10ce2186e2020506dd5016e4cf16aa700ba5f442d52413182c1bfae96df91b` and the agent SHA-256 was `f77f681c88418f48569bce47d2f695717fb1e242033cc7f2785c6d467253b7b4`. Native fixtures used the Tekkit Classic 3.1.2 client overlay, Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176 and Java 8u51. They register real mod classes directly and bypass the final graphical handoff in disposable worlds.

| Check | Result | Observed behavior |
| --- | --- | --- |
| Portable settings, calendar and documentation | PASS | Configuration migration, calendar, patrol/assault scheduling, outdoor policy and current documentation checks. |
| Client build and packaging | PASS | MCP 6.2 compile and reobfuscation, 471-entry streaming verification, isolated staging, and archive checks for fixture and vanilla class exclusion. |
| A. Real mixed equipment | PASS | Eight associated devices of eight native tile types. IC2 Macerator and Iron Furnace, BuildCraft tank, RedPower deployer, Railcraft rolling machine, EE2 collector, ComputerCraft computer and ZTO hopper supplied generic evidence. Six had positive native inventory capacity. The Iron Furnace was outside the exact IC2 profile. The open factory promoted with no vanilla utilities or verified network. The enclosed factory kept the cabin's ID and bounds. |
| B and D. Unfamiliar construction and equipment | PASS | Authored masonry included a metadata-zero construction variant of a fixture block that only creates tiles at other metadata values. On the same normal entry points, a fixture inventory and non-inventory controller supplied associated evidence through their native tile and activation behavior. The test-only block and tile classes are absent from the packaged mod. A native Macerator also remained visible in the generic-only run with IC2 enrichment unavailable. |
| C. Unlogged installations and use | PASS | An unlogged unfamiliar floor on a stone substrate qualified after two distinct device activations. An unlogged mixed workshop promoted after real ZTO Hopper and IC2 Iron Furnace container openings. The real ComputerCraft screen also supplied a use event without a container change. Repeated openings of one unchanged modded device counted once. |
| E. Infrastructure and negative controls | PASS | Two independent sites remained distinct across a long native BuildCraft pipe run and retained both envelopes after reload. An isolated pipe exposed zero inventory slots and did not join the nearby device. A nearby unrelated spawner stayed unlinked. Unused unknown blocks, modded ore and oil, generated IC2 rubber trees, a vanilla spawner, temporary construction, untouched caves, generated villages and unrelated excavation failed to promote. Unavailable terrain produced no chunk retrievals. |
| F. Survival, registry and protection | PASS | Both ordinary utility-equipped and utility-free cabins qualified. Existing and reopened site IDs, migration, multiple compounds and independent outposts passed. Above the mixed factory aisle, the roof remained visible to Protected Interior and nonstandard collision correctly left enclosure uncertain. |
| G. Mixed site siege and ordinary assault | PASS | The ordinary SiteQueries path discovered the mixed workshop. Natural lunar admission inserted an attacker via a separately verified exterior approach. Cooldown, replay, partial coverage, invalid approaches and ordinary PLAYER_ASSAULT fallback passed in the pinned suite. |
| Owner graphical test, TreeCapitator interaction and dedicated multiplayer | NOT RUN | The checks above exercised the headless client simulation. Graphical readability and input, a dedicated TreeCapitator playthrough and server authority remain separate verification. |

## Performance evidence

Each workload warmed for 3,000 updates and measured 3,600. Times are median / p95 in microseconds of recognition service CPU per sampled update, unless noted. The baseline was rebuilt from `be74bc0ff29c33e378069c10ff8787b2b443ebd2` and had mod SHA-256 `19c81bed0fdd5ccbe22a3f9f3249cca8684f2e2482dae0e9d910ce7721b6c368`. Both sides used the same runtime probe and agent.

| Workload | Main | Candidate | Sampled updates per side |
| --- | ---: | ---: | ---: |
| 19 hoppers and tanks, generic path only | — | 8.9 / 45.9 | 360 candidate |
| Two IC2 endpoints | 8.1 / 57.9 | 9.9 / 83.2 | 360 |
| Six IC2 machines | 7.2 / 35.1 | 7.4 / 43.0 | 360 |
| Twenty IC2 machines | 7.2 / 27.7 | 7.5 / 30.2 | 360 |
| Cable-heavy IC2 factory | 7.8 / 26.2 | 13.5 / 72.0 | 396 |
| Long IC2 cable | 6.8 / 26.2 | 7.7 / 53.1 | 432 |
| Unavailable-window check | 0.7 / 3.2 | 0.8 / 4.3 | 36 |

The generic-only workload spent 50 extra observations per completed survey, including its slot count checks, and zero IC2-specific reads. The eight-device mixed factory spent 23 generic and two extra IC2 observations. All runs stayed at or below 169 per update and recorded zero absent-chunk retrievals. Idle medians were at most 0.1 µs. Cable-heavy workloads had higher sampled p95 with the new classifier and the same sampled-update count. The 32-observer replay reported p95 final-frame times of 0.657 ms with overlap and 1.011 ms with dispersed sites. The replay runs individual service instances on one simulation thread as a scaling measurement.

## Owner walkthrough

1. Copy your save to an isolated Tekkit Dev profile and install the matching ZTO and ZTOAgent artifacts. Enter the Overworld in Survival and note any existing site ID and footprint.
2. Use a compact work area. Put a Macerator, BuildCraft tank and a second mod's machine near one another on the floor. Leave room to walk at least four columns. For an unlogged workshop, open two distinct local devices, leaving at least one second of unpaused play between openings.
3. Open Numpad 1 → Settlement Recognition → **Industry**. Check the current decision, coverage, equipment found and associated, inventory endpoints and successful openings. **IC2 Wiring** shows verified copper paths separately. Wait for a completed survey if the window is pending.
4. Save and reopen. Check the same site ID and boundaries. Repeat a local device opening to rebuild use observations. For a control, extend a pipe away from the floor and check that the site ends at the construction.

## Limits

Custom item placement may bypass ItemBlock, as confirmed with the BuildCraft pipe fixture. Those blocks can still supply native functional and spatial evidence. A successful activation without an observable local screen or container change is outside the use hook. Tileless nonstandard equipment may still contribute via placement or a successful opening. Old irregular structures made entirely of unfamiliar materials may require existing authored or inferred construction elsewhere in the work area. Singleplayer is the verified runtime. Dedicated server ownership and real multiplayer load require their own validation.
