# Survival 0.3.0-alpha.4 implementation and 0.4 handoff

The [industrial recognition follow-up](settlement-industrial-recognition.md) extends the utility and local survey observations described here. This record describes the original alpha.4 implementation and its historical acceptance.

This iteration implements the owner-approved compound, protection and read-only query scope. The [milestone specification](survival-030-spec.md) still contains proposals for later work. The [alpha.3 record](survival-030-implementation.md) describes the earlier single-part registry and the local survey it introduced. This implementation supports the [Project Vision](../../PROJECT_VISION.md) by recognizing a player's ordinary work areas without claims and giving future encounters observations of actual construction. The [0.4 director](survival-040-spec.md#5-integration-with-settlement-recognition) owns eligibility and tactical decisions.

## Compound recognition

The 13×13×9 local survey and optional single recentered window still recognize individual structures. A site can contain up to eight known local parts, each with its own bounds, confirmation and evidence. The total envelope is at most 64×24×64. Parts use all three dimensions. Air in a courtyard or between floors confers no occupancy or protection. A wide connected construction that clips a local survey can still have partial coverage.

An unregistered part first needs the ordinary promotion evidence and at least eight matching native placements to attach. Both parts need two recent native utility openings associated with their local components. The source must be a recognized part with observed construction, and a complete source survey must have confirmed its openings. The same observer then travels continuously to the destination, according to consecutive tick positions, with a traversed distance of 3–24 columns/steps and no jump of more than one block per axis between ticks. The parts can have up to eight combined empty horizontal blocks between them, with at most two blocks of floor-height difference. The activity evidence expires after 400 ticks. A destination awaiting its second opening can remain a candidate; proximity, regional activity and travel provide no attachment on their own. A workshop that already has its own ID remains a separate site. When evidence connects multiple established IDs, the decision reports ambiguity without merging or splitting them.

A corroborated overlapping extension can enlarge a part while preserving its ID. Smaller corroborated bounds require three agreeing complete observations that encompass the old part; a detached annex can be removed after three complete observations find no construction in its old extent. The final part remains as history. Temporary interruption preserves earlier evidence, and contraction counters are session-local. The Territorial Activity clock treats a site as dormant after 24,000 ticks without a meaningful observation. Returning and corroborating use refreshes the existing ID.

## Registry migration

`data/zto_settlement_sites.dat` keeps the existing `ZTOS` magic. Schema 2 stores at most 256 world-local IDs. Each site header is 13 bytes and each known part is 39 bytes. A 10-byte file header gives an 83,210-byte maximum. Readers verify coordinates, dimensions, counts, sequences, evidence bounds, timestamps and trailing data before accepting the file. A schema 1 row becomes one known part with the original ID, bounds, confirmation and observed-construction flag. Detailed counts remain unknown until sampled. The next ordinary save writes schema 2 through a buffered temporary file and atomic replacement. Unsupported future and malformed files remain untouched. Provenance storage keeps its existing schema.

## Protected Interior

Protection assesses the queried standing-height point independently of site recognition and light. It reports `PROTECTED`, `EXPOSED` or `UNKNOWN`, a reason, and separate overhead, enclosure and exterior-route evidence. The search observes up to four blocks horizontally from the queried point and traverses floors within two blocks vertically. Overhead checks the three cell levels from two through four above each standing level. A clear horizontal exit into an uncovered position is explicit route evidence. Directly uncovered ground is exposed with its lateral route left unknown. Fully covered reachable standing cells with a closed perimeter provide local enclosure.

The pinned 1.2.5 model uses the two door halves and open bit, blocks crossing toward the remaining open panel along its collision axis, and handles a fence gate's open bit. It checks support, two-cell clearance, bounded steps and ladder access. Normal full blocks, glass and legacy slabs can supply overhead cover. Unsupported collision shapes, boundary-reaching covered routes, missing chunks and exhausted work return uncertainty. The result describes a local standing-height model. Door handling and candidate paths therefore require native collision revalidation for any future actor-specific use. The assessment is observational. Vanilla and ZTO spawns, threat and encounter scheduling use their existing authorities.

## Read-only contract

| Query | Result |
| --- | --- |
| `SiteQueries.resolve(world, id)` | Immutable recognized snapshot by persisted ID, independent of observer location. Includes world-session and dimension context, envelope, known parts, evidence, coverage and last meaningful observation. Unavailable terrain preserves the confirmed footprint. |
| `nearby(world, x, y, z, radius)` | Up to 16 recognized snapshots in ID order in a 3D cubical range of radius 0–24. At least one intersecting known part must be loaded. At most 256 site headers and eight parts per site are considered. |
| `locate(world, x, y, z)` | Approximate recognized membership by known part, a recent local candidate, ambiguity or unknown. A part outside loaded terrain still has historical membership and unavailable coverage. |
| `protection(world, x, y, z)` | Current local geometry using the idle service tick's allowance. A second point returns unknown until another idle tick. Repeating a point in the same tick reuses its immutable observation. |

Call on the owning world thread after the local service tick. Wrong world or thread produces empty, null or unknown as appropriate. Results can be retained. Their contents are observations from the query time. IDs are save-local, and the session token changes on world binding. A fresh local observation can give a part `COMPLETE`; historical loaded data, clipped observations and mixed coverage remain `PARTIAL`. Long inactivity produces `STALE`, and unavailable terrain is explicit. An aggregate is complete only while all parts have recent complete observations. A query cannot create, merge or promote a site, advance territorial evidence or request a chunk load.

The future siege director needs to resolve the site again and revalidate world/session, coverage, freshness, chunk residency, actor-specific collision and relevant route geometry before acting. It chooses eligibility, scheduling, hostile placement and tactical objectives. Area queries perform bounded chunk-presence checks and no block-ID reads.

## Work budget and verification

Recognition and protection share a ceiling of 169 block/metadata reads between service ticks. Recognition has priority and performs one 169-block slice. Protection gets one synchronous result on an idle tick and keeps no queued jobs. It checks the chunk before each uncached cell read. The same-tick cache expires on the next service tick, so doors and other metadata changes need no provenance callback to appear in a fresh assessment. With a continuously retrying observer, a recentered survey can delay protection for up to 18 sampled ticks before an idle slot. All work and registry writes remain on the owning thread.

The native headless fixtures exercise the owner's `Recognized` with `Partial footprint` case, interrupted surveys, a furnished compound and independent used outpost, a connecting observation with competing IDs, an extension, contraction, dormancy and return. They reopen both new and migrated saves and retain IDs on different floors. The protection consumer compares an interior, closed and open entrance, courtyard, open-sided shelter and natural cave. It also exercises solid, glass and slab roofs, roof removal, a ladder opening, a gate, unavailable terrain and a budget-exhausted roof. A matched ordinary-spawn selection sample and the full pack encounter probes cover the existing gameplay authorities.

### Measured workloads

On Windows 11 / Ryzen 7 5800X, the compound workload warmed 600 frames and measured 1,800 frames per run while rotating among the house interior, doorway and courtyard. Each run assessed 1,638 requests and deferred 162 against recognition slices. The longest deferral was nine ticks. The combined peak was 169 reads, with zero queued protection jobs and zero requests for absent chunks in the audit. The dense covered-area control used all 169 reads on each assessment and returned UNKNOWN in that same tick.

| Pinned workload | Samples per run | Median µs | p95 µs | p99 µs | Maximum µs |
| --- | ---: | ---: | ---: | ---: | ---: |
| Resolve + protection, `-Focus compound` | 1,800 | 1.5 | 6.5 | 10.9 | 80.7 |
| Resolve + protection, `-Focus query` | 1,800 | 1.0 | 3.6 | 8.0 | 78.0 |
| Dense covered area, `-Focus protection` | 144 | 11.8 | 44.6 | 60.6 | 106.6 |
| Dirty registry save, `-Focus compound` | 24 | 1,100.2 | 1,357.5 | — | 1,572.6 |
| Dirty registry save, `-Focus query` | 24 | 1,002.5 | 1,085.5 | — | 1,122.6 |

The dirty-save timings cover the two-site, three-part, 153-byte registry using its production writer and file replacement. The narrowed footprint occupied 114 bytes. The dense protection control uses a wide low roof and reports bounded computation time separately from the wait for a budget slot. These timings are descriptive; deterministic bounds and outcomes are the gates.

### Staged artifact identity

Clean source `241ece1107ad16028db6356c964863716134b120` compiled with MCP 6.2, JDK 8u504 and development Forge 3.4.9.171. Client reobfuscation and streaming validation of 455 mod entries passed. The headless runtime was the Minecraft 1.2.5 / Tekkit Classic 3.1.2 overlay `9808277d362d4f67ed5031b9d47afbe90f696125e06cc333342917719f0c3ad5`, Forge 3.3.8.164, FML 2.2.106.176 and Java 8u51.

| Artifact in local `dist-zto/settlement-alpha4/stage` | SHA-256 |
| --- | --- |
| `mods/ZedsTekkitOverhaul.jar` | `dab3fedd3b643772d9f3b92bcba7a7383c1f815f28178c2b95d887b7e907bdae` |
| `ZTOAgent.jar` | `bea15285c92439d2606c3d8e4cd33de54167b21811b8818e0f31aa6c21e30578` |

The staged pair passed `-Focus cabin`, `-Focus mutations`, `-Focus compound`, `-Focus protection`, `-Focus query`, `-Focus provenance`, `-Focus settlement`, `-Focus furnace` and the full pinned headless `-Pack`. The final ecology fixture collected 500 cave and 5,252 surface candidates in 1,719 passes. Portable territory, legacy NBT and documentation checks also passed. The owner Tekkit Dev instance received the same pair, with matching installed hashes and one managed Prism agent component.

## Owner walkthrough

Use a disposable Survival save with a furnished house, a short open courtyard, an open workshop and a nearby independent outpost. Stand inside the recognized house, open two linked utilities at least one second apart and wait one survey cycle. Open the door, walk across the courtyard and open two workshop utilities, again one second apart. Walk four columns in the workshop and wait for its survey. Check that it shares the house ID and that the outpost, used independently, receives another ID.

At the house, doorway, courtyard, workshop and outpost open **Numpad 1 → Settlement Recognition** and cycle **Assessment → Survey & Storage → Protection**. Compare site coverage with physical protection. A closed room can report PROTECTED, an open entrance can report a passable route, and a courtyard can lie inside the envelope while outside known parts. Save & Quit and reopen to compare the IDs. Repeat page navigation and readability at Small, Normal, Large and Auto.

On 19 November 2026 the owner confirmed their manual verification for merge. This is owner-reported evidence. The development runs recorded above were headless, and graphical details by scale were not recorded in the developer logs.
