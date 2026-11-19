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
- A candidate needs at least six matching placements or twelve inferred blocks, plus shelter-like cover or a local utility opening, and regional activity of at least one or two utility openings. Cover counts solid blocks above at least four of nine nearby columns. The result describes supporting geometry for recognition. Protected Interior remains an alpha.4 assessment.
- Promotion requires eight matching placements or sixteen inferred blocks and two native utility openings. Both paths also require the player to have crossed four distinct columns in the component and either regional activity of at least two or two local openings. A valid Territorial Activity observation clock supplies the timestamp. A furniture-free cabin can qualify through construction, cover, walking and regional activity. An open workshop can qualify through construction and use. An old inhabited structure can qualify through inferred geometry and verified interaction, with unknown authorship.
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

Final build evidence is recorded after packaging and runtime checks.
