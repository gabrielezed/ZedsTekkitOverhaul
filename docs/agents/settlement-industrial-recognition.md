# Industrial settlement recognition

This Survival 0.3 follow-up applies the Project Vision's support for different kinds of bases. An indoor or open industrial workshop can supply evidence through its own equipment and use. The existing SiteQueries contract lets future encounters discover eligible sites using the same boundaries and coverage as other settlements.

## Root cause

Alpha.4 inferred construction from selected vanilla materials, selected one face-connected component near the player, and associated vanilla utility blocks through membership or the floor directly below. Its successful activation observer could see an IC2 GUI handoff but the recognition service accepted only workbench, furnace and chest IDs. Native IC2 machines without tracked placement neither supplied inferred construction nor corroborated a workshop through use. Even a recognized house could show zero industrial activity. Independent machinery and cable runs had no modeled relationship to the selected floor. The registry and Siege Director were downstream consumers of that missing evidence.

The existing ItemBlock and removal hooks remain the source of manual observations. IC2's ItemCable uses its own placement routine. Older installations and other unobserved mutation paths have incomplete authorship information. IC2 appearance, age, GUI use and local wiring supply independent evidence.

## Pinned adapter

The [generic settlement evidence contract](settlement-generic-modded-evidence.md) now governs equipment observation and qualification across mods. This adapter supplies IC2 endpoint roles, container identity and compatible short copper paths. Generic evidence remains available when an IC2 variant falls outside its exact subset.

The adapter discovers current numeric IDs by exact IC2 block class in the loaded registry and checks the pinned client mod entry point's SHA-256. It caches reflection for IC2-specific fields and methods and uses metadata plus exact native tile class to verify a block. The lookup reads existing entries in the loaded chunk tile map. The normal world tile getter can create missing tiles and is unsuitable for an observation.

| Native family | Registration default | Supported metadata and exact tile suffix |
| --- | ---: | --- |
| BlockMachine | 250 | 2 ElecFurnace, 3 Macerator, 4 Extractor, 5 Compressor |
| BlockGenerator | 246 | 0 Generator |
| BlockElectric | 227 | 0 ElectricBatBox, 1 ElectricMFE, 2 ElectricMFSU |
| BlockCable | 228 | 0 insulated copper, 1 bare copper, both TileEntityCable |

Supported cable tiles need matching cable type, valid color and no foam. BlockMachine2, machine casings, transformers, other generator variants, foam, other conductor types, detector and splitter cables are outside this subset. The verified wiring calculation requires matching loaded tiles. The generic classifier can still count supported native capabilities on these blocks.

## Evidence and thresholds

- **Construction and relationship.** The existing face-connected component defines the surveyed bounds and clipping. A supported machine within one vertical block of the observer's survey level associates through the selected floor directly below it. IC2 machines and cables stay outside the construction flood fill. Natural terrain or another structure under the equipment cannot supply membership in the selected component. A found machine with a matching provenance record is tracked separately from inferred construction.
- **Use.** The existing pinned PlayerControllerSP activation observation requires success and a changed open container. IC2 BlockMultiID routes to Platform.launchGui and FML. The same agent hook now shares the new container with the observer, which checks the exact native IC2 container, tile identity, world and coordinates. The observer requires a nearby Survival player on the world thread and the existing 20-tick cooldown. Use is recent for 1,200 eligible ticks and must match the sampled tile and its floor. Opening a GUI establishes interaction only. EU, production, inventories and EnergyNet are outside this observation.
- **Wiring.** Six-face traversal uses only the bounded sample. Two cable colors agree when they match or either is unpainted, following IC2 1.97. A conventional Generator emits, each supported processor accepts, and storage emits toward its native facing and accepts on the other faces. A corroborating group has 1–12 cable tiles, stays within the sampled window and connects two distinct associated endpoints in compatible directions. Endpoints terminate traversal. Debug pair counts describe local compatibility, without current, tier, load or EnergyNet telemetry.
- **Promotion and re-observation.** Existing construction, coverage, movement, activity and use thresholds still apply. Old structures can qualify with at least 16 inferred construction blocks, four walked columns and two qualifying openings. An open area with eight matching construction blocks, at least two types of associated machines, one qualifying local network and Territorial Activity of at least two can meet the cover-or-use gate through wiring. Machine counts saturate at a two-type condition for these gates. Re-observation uses the existing construction thresholds and walking or local openings.
- **Growth and compounds.** Inferred growth requires at least 16 inferred construction blocks, four walked columns, two equipment types, and either two IC2 openings or one plus a qualifying network. At least one of the IC2 openings must occur in the addition outside the previously stored part. The geometry must still meet the existing two-thirds overlap, complete sample, envelope and conflict checks. Detached additions use the existing observed-construction, source and destination openings, continuous route and gap rules. Cable evidence alone never associates detached parts. The registry remains schema 2 with up to eight parts, a 64×24×64 envelope and 256 save-local IDs.

The survey can resume within a layer. Each update spends at most 169 reads on block IDs, selective metadata, explicit loaded-chunk retrievals and existing tile lookups. Block presence checks repeat before each slice. The graph walks only sampled entries on the owner thread. The nine-layer vanilla scan keeps its usual cadence. Dense equipment can add updates before classification. Protection retains its independent native shape and standing clearance assessment during an idle survey slot.

## Debug Hub

Open Numpad 1 → Settlement Recognition, then cycle to **IC2 Wiring**. Compare machines found, machines linked to the structural component, recent openings, types and matching placements. The page shows unlinked equipment, blocks outside the IC2 profile, local source-to-sink pairs and clipped or oversized cable groups. The **Industry** page shows the cross-mod evidence. The common survey header reports missing terrain and clipped construction. A local wiring decision is explicitly marked when wiring supplies the required corroboration.

## Native evidence and performance

| Check | Result | Evidence |
| --- | --- | --- |
| Configuration, lunar calendar and schedules | PASS | Portable configuration and calendar suites. |
| Client MCP compilation, reobfuscation and streaming JAR verification | PASS | MCP 6.2, development Forge 3.4.9.171, 456 entries, isolated packaging and staging. |
| IC2 installations and successful GUI use | PASS | Native placement and opening in an already recognized six-machine house. Old open workshop with unlogged infrastructure, repeated real openings, failed sneak activation, cooldown and identity invalidation after replacement. |
| Wiring and geometry | PASS | Both copper variants, color separation, distinct endpoints, a long clipped line, loaded terrain checks and an open observed workshop corroborated by wiring. Independent protection checks found an exposed open workshop and genuine overhead in the house. |
| Footprints, compounds and world lifecycle | PASS | Inferred addition expands under the same ID after local use. Isolated machine is excluded. Independently used outpost gets a different ID. Save/reopen preserves bounds and IDs. Existing migration, abandonment, mutation, SiteQueries and compound fixtures pass. |
| Full pack and existing ecology | PASS | Tekkit Classic 3.1.2, Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176, Java 8u51. Includes the existing provenance, spawn pressure, assault, aerial, native mining and industrial item, liquid and EU fixtures. |
| PR #43 runtime compatibility smoke | PASS | Ran PR head `a1bde07` fixtures using its production-equivalent `f9c5958` JAR with the candidate recognition classes and agent. Native admission, collision, compound/independent identity, coverage, fallback and lifecycle still passed. |
| Owner Survival save, graphical layout and rebased PR #43 acceptance | NOT RUN | Isolated headless checks completed. Owner playtest and formal integration follow the foundation review. |

The pinned industrial probe registers real IC2 block and tile classes and supplies native texture pixels for headless placement. It exercises the actual controller, IC2 GUI/container construction and FML handoff. The fixture substitutes the final graphical handoff to avoid opening a window. The pinned pack runner also executes installed and reduced pack linkage checks.

Run `zto-tools/test-zto-agent.ps1 -Focus industrial-profile -IndustrialProfileBaseline <baseline-jar>`. The comparison used the same Java 8u51 process settings and disposable geometry on each side. The baseline JAR at `f9c5958` has the same settlement service sources as `ec91f00`. Each workload warms for 3,000 updates and measures 3,600. Times are median / p95 in microseconds, sampled ticks unless noted.

| Workload | Before | After | Sampled updates before / after |
| --- | ---: | ---: | ---: |
| Small workshop, two endpoints | 9.3 / 41.0 | 8.7 / 57.1 | 324 / 360 |
| Six-machine workshop | 5.2 / 23.9 | 7.0 / 34.0 | 324 / 360 |
| Mixed factory, 20 machines | 5.0 / 9.2 | 6.9 / 16.5 | 324 / 360 |
| Cable-heavy factory | 5.1 / 9.3 | 7.3 / 25.1 | 324 / 396 |
| Long cable with distant endpoints | 4.9 / 8.1 | 6.7 / 26.7 | 324 / 432 |
| Missing chunk check | 0.8 / 1.0 | 0.8 / 4.9 | 36 / 36 |

All after-run industrial idle medians were 0.1 µs. Peak work stayed at 169 counted reads per update, with zero retrievals of missing chunks. The higher active p95 and additional sampled updates reflect the selective probes and graph evaluation. The survey starts on the existing 100-tick cadence. The timings measure service CPU time. The vanilla cabin center profile compared with the historical alpha.3 series at 0.1 µs idle median on both, 4.6–4.7 µs versus 5.6 µs on initial slices, and 69.5–72.2 µs versus 70.9 µs on final ticks. The unchanged dense provenance codec measured a 227 ms median dirty save in the candidate profile, versus 228–252 ms historically. The combined native compound fixture wrote the same 153-byte registry. The latency numbers are host observations and vary with JVM warm-up and scheduling.

## Owner acceptance

1. Copy the Survival save into an isolated Tekkit Dev instance. Install the matching candidate mod and ZTOAgent. Start in Survival in the Overworld and record the current ID, bounds and assessment from Settlement Recognition.
2. Use a connected plank, cobblestone or stone-brick floor. A 9×7 floor gives a compact enclosed test. Use its interior row z=2 for six machines at x=1–6, with a parallel copper cable row at z=1. Measure x and z from one floor corner. Use a Generator, Electric Furnace, Macerator, Extractor, Compressor and BatBox. Keep the six cables unpainted and free of foam. Leave a walking aisle behind the equipment. Provide no vanilla utility for the workshop test.
3. Open the Macerator. Wait at least one second in active play and open the Extractor. Walk a small square in the aisle with at least four distinct columns. Stand near the floor center until the completed survey appears, allowing about 5–10 seconds per cycle.
4. Confirm that Industry reports six found and linked machines and at least one local cluster. Inspect recent openings, coverage and bounds. An established site should retain its ID. An unlogged old structure may use the two openings and movement to qualify. Repeat the procedure on a copy where the equipment predates ZTO provenance and check that machine placement evidence is still absent.
5. Save and reopen. Check the same ID and footprint after the fresh survey, then reopen the machine interfaces to rebuild session-local use evidence. The headless fixture checks this same ID through SiteQueries.locate and resolve. Live Debug Hub displays the site's remembered ID and current coverage.
6. Build a small contiguous work area with its own floor and a newly associated machine, use its interface, and survey both areas from a central point. Compare the footprint after the area satisfies the growth rules. For a control, extend a copper cable well beyond the building and inspect the bounds again. An independent distant workshop can earn its own ID.

## Survival 0.4 handoff

SITE_SIEGE resolves current parts through SiteQueries and samples terrain collision independently for its exterior approach. Industrial observations stay in recognition and its debug snapshot. Recognized workshops enter ordinary site discovery on the same terms as a house. Natural full-moon scheduling, the opt-in setting, cooldown, player presence, darkness and a valid exterior approach govern admission. [Mixed-mod runtime coverage](settlement-generic-modded-evidence.md)
