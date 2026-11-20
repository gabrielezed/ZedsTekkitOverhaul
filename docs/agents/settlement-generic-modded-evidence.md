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

## Owner walkthrough

1. Copy your save to an isolated Tekkit Dev profile and install the matching ZTO and ZTOAgent artifacts. Enter the Overworld in Survival and note any existing site ID and footprint.
2. Use a compact work area. Put a Macerator, BuildCraft tank and a second mod's machine near one another on the floor. Leave room to walk at least four columns. For an unlogged workshop, open two distinct local devices, leaving at least one second of unpaused play between openings.
3. Open Numpad 1 → Settlement Recognition → **Industry**. Check the current decision, coverage, equipment found and associated, inventory endpoints and successful openings. **IC2 Wiring** shows verified copper paths separately. Wait for a completed survey if the window is pending.
4. Save and reopen. Check the same site ID and boundaries. Repeat a local device opening to rebuild use observations. For a control, extend a pipe away from the floor and check that the site ends at the construction.

## Limits

Custom item placement may bypass ItemBlock, as confirmed with the BuildCraft pipe fixture. Those blocks can still supply native functional and spatial evidence. A successful activation without an observable local screen or container change is outside the use hook. Tileless nonstandard equipment may still contribute via placement or a successful opening. Old irregular structures made entirely of unfamiliar materials may require existing authored or inferred construction elsewhere in the work area. Singleplayer is the verified runtime. Dedicated server ownership and real multiplayer load require their own validation.
