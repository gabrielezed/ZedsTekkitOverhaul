# Survival 0.3.0-alpha.2 implementation

The prototype runs in local singleplayer Overworlds. It recognizes a maintained place without requiring a claim block. The [owner specification](survival-030-spec.md) defines the full milestone and [Project Vision](../../PROJECT_VISION.md#9-attività-territoriale-provenienza-dei-blocchi-e-insediamenti) keeps territorial pressure, recognition, and shelter assessment under their own authorities.

## Provenance and use

| Evidence | Pinned source | Interpretation |
| --- | --- | --- |
| Manual placement | `vd` after the native ItemBlock placement callback | Latest successful placement by the active local Survival player. The survey counts a solid block when its current ID matches the sampled record. Matching IDs cannot establish continuous authorship through missed mutations. |
| Manual removal | `ki` after native `removeBlockByPlayer` success | Latest confirmed removal at the coordinate. |
| Quarry excavation | BuildCraft 2.2.14 `TileQuarry.positionReached` after a successful removal | Latest confirmed automation removal. Quarry frame and blueprint construction are outside this hook. |
| Utility opening | `aes` after a successful `Block.blockActivated` that replaces the player's open container | The active local Survival player opened a vanilla crafting table, chest or furnace. Blocked chest clicks leave the container unchanged. This measures interaction, with actual crafting, item transfer and production left unmeasured. |

The same ZTOAgent applies removal and the existing furnace quick-move transformation to the exact controller fingerprint. The native calls keep their original return values. Unavailable hooks and storage appear in diagnostics. Unobserved operations include explosions, pistons, BuildCraft builders, RedPower frames and deployers, IC2 Miners, dedicated item placements and ZTO mob changes. Missing records, unloaded coordinates and changes from those paths carry unknown authorship.

The alpha.1 format remains in `data/zto_world_provenance.dat`. Schema 1 stores the latest observation per coordinate in up to 1,024 sections of 8×8×8, with write-recency eviction. The compressed NBT envelope carries the schema and packed section bytes. With all 512 positions full in every section, the packed payload is about 2 MiB before compression. Observations dirty memory. Native world saves write changed evidence via a temporary file and atomic replacement.

## Recognition

The following thresholds are reversible prototype defaults:

- The active Survival player supplies one local survey anchor. Every 100 unpaused ticks at most one survey starts. Nine successive updates sample a single 13×13 slice around the player's foot level, from two below to six above, shifted inside the world at the height limits. Each slice first checks the loaded status of at most four chunks. Missing terrain cancels the survey.
- Face-connected solid construction inside the window forms a component. A matching manual placement or a small palette of manufactured materials can seed it. The inferred palette includes planks, cobblestone, slabs, brick, stone brick, glass and the supported utility blocks. The selected component must be near the player's feet. Its measured bounds have to span at least three blocks on each horizontal axis. Components reaching a survey edge can form candidates while their limits remain uncertain.
- A candidate needs at least six matching placements or twelve inferred blocks. It also needs shelter-like cover or a local utility opening, and it needs supporting activity from a regional score of at least 1 or at least two utility openings. Cover counts solid blocks above at least four of nine nearby columns. The result describes supporting geometry for recognition. Protected Interior remains an alpha.4 assessment.
- Promotion accepts eight matching placements, or sixteen inferred blocks accompanied by two native utility openings. Both paths also require the player to have crossed four distinct columns in the component and to have a regional score of at least 2 or at least two local openings. A valid Territorial Activity observation clock supplies the timestamp. A furniture-free cabin can qualify through construction, cover, walking and regional activity. An open workshop can qualify through construction and use. An old inhabited structure can qualify through inferred geometry and verified interaction, with unknown authorship.
- The session retains the last 16 distinct locally walked columns and a ring of 16 successful utility activations. Activations are spaced by at least 20 ticks, expire after 1,200 ticks, and must still be inside or one block from the sampled component with a current supported utility. A utility present in the component is reported separately. Territorial Activity supplies its existing regional score and clock via read-only queries.

An immutable diagnostic snapshot exposes the latest local reasons and counts, placement consistency, inferred blocks, cover, equipped utilities, verified openings, occupied columns, activity, the registry status, and the last observation tick. The Debug Hub reads cached observations. Ordinary world ticks perform surveys with the screen closed.

## Site identity and storage

The separate `data/zto_settlement_sites.dat` binary registry has a `ZTOS` magic prefix, version 1, and a bounded count. Each 37-byte row holds a sequential world-local ID, six inclusive bounds, last meaningful Territorial Activity tick and whether qualifying manual construction was corroborated. The 10-byte header and 256-row limit cap the file at 9,482 bytes. The filesystem replacement and decoder preserve malformed or unsupported data for inspection. A write failure leaves pending records in memory and retries at the next save. Existing Territorial Activity and provenance data use their own files and schemas.

After promotion, the registry matches new component bounds to existing three-dimensional bounds. A shared volume of at least one third of the smaller box is required for this match. A player already inside a site's stored bounds can also re-observe it. Re-observation requires spread, at least four matching placements or eight inferred blocks, and two visited columns or one recent opening. Unavailable terrain, eviction and low activity retain the known ID and last corroborated information. The registry is append-only with no ID reuse. Bounds are fixed after recognition in this prototype. Only the current local candidate is retained in memory.

The per-tick maximum is 169 block reads. A complete survey visits 1,521 positions. The final update performs at most one 1,521-position component walk, six neighbor checks per visited block, and bounded site lookup. At most one survey is queued. The Debug Hub does not request chunks. There is no consumer of the registry in threat, spawning, encounter budgets or lunar scheduling.

The current scope is a small contiguous construction. Large compounds, several floors, adjoining sites, atypical material palettes, or very narrow rooms may remain candidates or require the player to occupy a representative part of the site. Merge, split, footprint evolution, dormancy policy, broader automation support and the final site query contract are later work.

## Native verification procedure

Build and stage with `zto-tools/build-and-deploy.bat` and an isolated `ZTO_PRISM_MODS` target. Run `zto-tools/test-zto-agent.ps1 -Focus settlement`, `-Focus provenance`, and `-Pack` using the same candidate pair. The headless settlement harness drives natural placement and collision-aware movement, uses vanilla utility activations, ticks the ordinary services, and saves through the Forge callback in marked disposable Anvil worlds. The pinned local chunk provider's unload poll is a no-op. The fixture replaces its chunk cache with a new native world instance using the same save handler to exercise unavailable terrain and return. Save/reopen also exercises a new handler. The generated-village control uses the native flat-world generator with structure generation enabled.

The [verification record](#verification-record) identifies the artifacts and the measured results.

## Owner playtests

Use a disposable Tekkit Dev save and the matching mod and agent. Open **Numpad 1 → Settlement Recognition** while standing at the location under test.

1. Build a wood cabin around a walkable interior, leave a normal entrance, and cover it with a roof. Walk around inside and use it during ordinary play. Check the candidate or site, construction count, cover, and regional activity. Open the world again after Save & Quit and compare the ID from the same location.
2. Build a small open workshop with a connected floor. Use an ordinary workbench, chest or furnace, and move between work positions. Check the recognized site's activity and utility openings while cover stays low.
3. Repeatedly return to an excavation without building a work area. Check for removal history and regional activity with no recognized site. A temporary isolated wall should also remain unrecognized.

## Verification record

The clean builds used MCP 6.2, JDK 8u511 and development Forge 3.4.9.171. Headless probes used the same Tekkit Classic 3.1.2 overlay SHA-256 `9808277d362d4f67ed5031b9d47afbe90f696125e06cc333342917719f0c3ad5`, Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176 and Java 8u51.

| Source | Mod SHA-256 | Agent SHA-256 | Furnace | Provenance |
| --- | --- | --- | --- | --- |
| Merged baseline `6c350f0266b21231edbfb7c59d63ffd737405f41` | `c79480f68f1dc770241e9620054bca8a91dfc9b2d045227616e032b91351247a` | `6532152c92c697317628f57d6210776af841704eb005683263ff3c25cb790cf8` | PASS | Feature absent in package |
| Initial PR head `315904d73702b37f209bcba1a5a0297964fa04d6` | `97aab6672d8bdf45a80a89557be4157e210b40ae69758cd559bace2ac9b934ef` | `5b1292da7b94b4543ada2c52d7dbdd835d098d24fc38d6be66b88cf8a4f422ce` | FAIL at the first vanilla empty fuel slot | PASS |
| Alpha.2 `ec5a50ec78e9d25e5f40724f6b7d9e484e3c413c` | `91eb56fdf269b94f374d7c9d9afd00b63dc6cfef0f9765831d04056e47a12074` | `4bd665a75c97e46570c0690c526e742604b39c8eb4be55a1ec855bc147477817` | PASS | PASS |

Alpha.1 returned the removal transformation for `ki` before reaching the existing furnace transformation for the same class. Commit `6b4cacb` composes the two. The alpha.2 full headless pack run passed the six fuel-burning containers, exact transfer limits, container returns and the powered furnace control.

- **PASS build and portable checks.** MCP compilation, reobfuscation, 441-entry staged JAR validation, territorial activity and legacy NBT, configuration, lunar calendar and documentation.
- **PASS focused and full headless pack.** `-Focus settlement`, `-Focus provenance`, `-Focus breacher` and `-Pack`. Native site IDs 1–4 corresponded to the furniture-free cabin, cave workspace, open elevated workshop and old inhabited structure. Wall edits, a stale recorded placement, unavailable terrain, return, re-open and provenance eviction preserved the known ID. Four registry rows occupied 158 bytes. The blocked chest produced no utility-opening evidence. The untouched revisits, wall, replacement cycles, 256 Quarry removals and native generated village produced zero sites.
- **Measured in the dispersed fixture.** 1,088 native placements reached the 1,024-section cap. The compressed provenance file occupied 2,154 bytes. Across 3,200 service ticks, 288 sampled a slice and 256 had one unfinished survey. Peak 169 block reads per tick. Sampled updates took 0.035 ms on average and 0.355 ms at the observed maximum. Timing includes the evaluator and final component pass. Setup and saves ran outside the timed loop.
- **Pending graphical evidence.** The owner playtests, rendered Debug Hub and physical NEI/Inventory Tweaks interaction require an isolated client with an independent input session. The headless fixtures used no graphical client.
