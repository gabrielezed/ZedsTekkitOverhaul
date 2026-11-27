# Sunken Observatory

The Observatory gives the Abyssal Trench a destination for a diving expedition. Follow the weathered columns to the entrance, descend through the flooded halls, and return by a side gallery or through the main approach. Oxygen, lights, temporary refuges, mining and advanced Tekkit equipment remain ordinary sandbox tools.

## Revision four

New sites use the same candidate grid, footprint, native terrain survey and buried room envelope. The survey measures the approach floor and rock shoulders. A stored sill and profile bind the portal to those measurements across chunk writes and save/reload. The shelf and recessed compositions share a three- or five-block opening, stepped stone crown, pillar jambs and an asymmetrically damaged outer support. The threshold opens into a descending flooded vestibule on the existing gallery axis.

The revision-four record adds two validated bytes after the base floor. The layout, slot ownership, stamps and encounter state in revisions one through three retain their original interpretation. Stored chunks and player alterations remain protected by the existing slice admission rules. [Native evidence and coordinates](../agents/survival-051-beta1.md).

## Revision-three owner refinement

New structures now occupy rock beneath their natural seabed. The survey measures cover over the actual chamber roofs before carving. Most of the upper shell is never visible in the seabed projection. The shaft mouth opens between eroded columns and leads through a staggered stair gallery to the observation hall. The return corridor can reach the tall Custodian chamber if the main inner passage is blocked. [Architecture, atlas and recipes](abyssal-architecture.md)

The native survey sampled 1,444 cells across four seeds. Six accepted the geometry and terrain gates, about 0.016 candidates per million blocks in that sample. The probe loaded and checked the following three native ruins.

| Seed | Entrance approach X, Y, Z | Natural cover over principal plan |
| --- | --- | --- |
| −32021 | 3371, 27, −4268 | 1,367 / 1,396 columns |
| −32021 | −3371, 38, −2914 | 1,353 / 1,382 columns |
| 32022 | 4846, 28, −3289 | 1,387 / 1,416 columns |

Revision-three layouts deterministically allocate 8–12 patrol slots. A site can admit one actor per 160 world ticks, outside the diver's immediate range and view. A revision-three room with a terminal slot stops admitting further actors there. Historical populations retain their 16–19 slot masks, actors and terminal outcomes.

## Placement and architecture

The Default Overworld is divided into 512-block candidate cells. A salted seed hash provides one possible anchor per cell, kept at least 136 blocks from its edges. Five of eight hash tickets can advance to the geography filter. The anchor requires strong Trench influence and deep target relief. The whole footprint then samples the same bounded trench envelope as the native provider without requesting chunks. Every surveyed column must be shaped ocean within the depth and influence limits. The foundation floor uses the mean seabed at Y17–32, with at most seven blocks of rise from the lowest seabed and eight blocks of burial beneath the highest.

Revision 1 uses an 11–14-block atrium including walls, a seven-block-wide principal gallery including walls, a 15×16 hall, and a 19×19 chamber with eight clear blocks above its central floor. The exterior adds columns and a submerged entry cut. The overall width is 35–39 blocks and its length is 59–66 blocks including the approach. Four orientations, mirrored side galleries, varying gallery and atrium lengths, a conditional annex, coherent wall erosion and roof damage vary the silhouette. One bypass rejoins the central hall. The lower bypass links the hall to the future arena separately from its front arch.

Revision-two sites use the following original layout. An atrium, offset hall, future arena and two branches are joined by independently composed longitudinal and cross passages. Seeded choices shift and mirror rooms, add annexes, change elevations and open selected roofs. Recessed lamps and paired hall columns guide movement. Passage water and interior clearance take priority over neighboring shells and foundations. The finished plan is checked in three dimensions for two-block-high continuity to the hall and arena, including a route with the main arena gate closed.

Foundations follow the sampled seabed. The generator cuts flooded volumes, writes integer block IDs into native sections, seeds low lamp light and leaves native height and skylight initialization to the provider. Geological coatings remain in the Trench. Fossil formations and marine planting skip construction columns. Native population and third-party generators retain their normal order and can intersect the ruins.

The inaugural candidate includes salvage from the existing masonry, lamps and fossil materials. Optional chest caches are omitted. The finished chamber includes a focal platform, masonry recess and two entrances for later integration. The [Custodian encounter](abyssal-custodian.md) uses the existing site identity, chamber bounds and separate persistent encounter record in both revisions.

## Ownership and persistence

The existing provider hook handles new chunks. Before taking the first slice, ZTO rejects any footprint containing a loaded chunk or a nonempty Anvil or McRegion header entry. Only the pinned local Anvil provider is supported. Each slice owns an independent intersection of the frozen plan. It touches no neighboring chunk and aborts the site if the owned slice cannot safely replace its actual terrain.

Each admitted cell has a fixed-size record under the world's `data/zto-observatories/` directory. It stores a schema magic, layout revision, seed-derived 64-bit site identity, anchor and floor, slice reservation bits, consumed, dead and retired population bits, and a failure flag. World seed and cell location validate the record. The stored revision and identity reconstruct the original plan and stations. Revision-one sites keep their geometry and their original Fangfish policy. Revision-two sites use Abyssal Enderkin. Neither revisits nor upgrades reset slots or repair altered blocks.

A flushed file write and atomic replacement reserve admission and each slice before block writes. A second pinned agent callback stores a site stamp in the native chunk root before Anvil queues it for asynchronous serialization. Forge loads the stamp with the chunk. Encounters begin after the complete footprint is loaded and populated with matching stamps. An interrupted build can therefore leave a partial Observatory or an unavailable encounter. Previously saved slices and player modifications are preserved. A record and the native chunk saves are independent files.

## Historical revision-two walkthrough

In a marked disposable Default world with the matching agent, `-Dzto.dev=true` enables **Numpad 1 → Biome Ecology → Observatory**. The locator checks at most 169 cells and refines at most 12 promising anchors. It reports predicted or admitted coordinates, distance, dimensions and identity. Near a site, the panel includes reserved slices and population counters. The lookup leaves chunks unloaded.

| Seed | Entrance X, floor Y, Z | Width × length |
| --- | --- | --- |
| 32021 | −1170, 22, 765 | 37 × 60 |
| −32021 | 3368, 31, −4268 | 35 × 66 |
| −32021 | −1802, 24, 1884 | 39 × 61 |

The native revision-two survey tested 1,444 cells over four seeds. Twenty-five passed the cheap geography filter and seven passed the whole-footprint terrain survey, about 0.0185 suitable sites per million square blocks in that sample. The table gives sites exercised through the native provider in fresh saves. The fixture writes floor plans, centerline and arena sections, and exterior projections to `dist-zto/observatory-refinement-previews/`.

Run `zto-tools/test-zto-agent.ps1 -Focus observatory` for the pinned headless fixture. The test covers native candidate surveys, loaded and stored footprint exclusion, chunk stamps, submerged routes, preservation of a player edit, and finite Guardian persistence. Evaluate the exterior, navigational landmarks, damaged roof silhouettes and lamp visibility in the owner playtest. A previously explored Trench may have no Observatory.
