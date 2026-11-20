# Recognition with partial footprints

A strongly corroborated local site can receive a save-local ID while its perimeter is still unknown. This supports the Project Vision's recognition by construction and occupation across cabins, open workshops and larger factories. SiteQueries remains the identity interface. Protected Interior owns local shelter and the encounter director owns admission. [Project Vision](../../PROJECT_VISION.md)

## Reported base

The reproduction builds a 236 × 12 multi-room factory using native Survival placement and eight actual pack devices. The survey at (-121, 63, 256) observes (-127, 61, 250) to (-115, 66, 261), with 90 matching placements, 121 inferred blocks, activity 24, 11 walked columns, one workbench, two furnaces and zero openings.

The previous PR #45 build returned Candidate and “Partial footprint. Sampling closer to the clipped edge.” The local component hits both east and west survey faces. Recenter can shift vertically for the clipped lower face, but it cannot fit the horizontal extent. The old classifier deferred on recenter and then disqualified any clipped component before checking positive evidence. Its ordinary open-air rules also required explicit use or enough industrial corroboration. Promotion now evaluates a strong partial path independently of the cover threshold used for ordinary sites.

Both builds measure 0/9 overhead at the survey anchor and 3/9 at the best sampled walked position in the fixture. A small connected glass roof covers the latter and leaves the anchor open. The survey checks a 3 × 3 area starting two blocks above the feet, selecting cover at the surveyed anchor and at eligible positions remembered during walking. These values describe local overhead. The exact roof and standing clearance of the owner's original building remain unverified.

## Acceptance evidence

Tested source `c55e0384d1fc5e9a355558f9a5a13fe215aea580` ran against the pinned Tekkit Classic 3.1.2 client overlay, Forge 3.3.8.164, FML 2.2.106.176 and Java 8u51. The verified 471-entry mod archive SHA-256 is `61819037e7e23c14d378af54cb3c4f2bda88ddb72ea3e1aa9eb924663d57aab2`. The pre-fix PR #45 archive SHA-256 is `ec10ce2186e2020506dd5016e4cf16aa700ba5f442d52413182c1bfae96df91b`.

| Check | Result | Evidence |
| --- | --- | --- |
| Build and full pack regression | PASS | MCP compile and reobfuscation, streaming JAR verification and the complete headless `-Pack` run. Portable settings, lunar and documentation checks also passed. |
| Owner-scale baseline and corrected decision | PASS | The same fixture changes from Candidate with no ID to Recognized with high confidence, site #1 and PARTIAL coverage. The initial stored local part matches the reported bounds. There are zero openings in both runs. |
| Multi-room factory larger than any survey | PASS | A continuous one-block-per-five-ticks visit extends site #1 to 44 parts. The observed envelope reaches from (-127, 61, 250) to (100, 66, 261), a span of 228 blocks including both endpoints. Survey limits and unresolved coverage persist during growth. |
| Native save and reopen | PASS | The 2,887-byte registry restores #1, all 44 parts and the same PARTIAL envelope. Schema 1 migration and compact schema 2 compound fixtures also run in the headless pack suite. |
| Partial false-positive control | PASS | A clipped L-shaped temporary wall stays unrecognized despite manually observed construction and local activity. The pack suite covers natural caves, a generated village, excavation, disconnected cabling, unrelated devices, open workshops and independent nearby compounds. |
| SiteQueries and the encounter director | PASS | SiteQueries discovers site #1 with PARTIAL coverage. Natural dusk produces `SITE_FOOTPRINT_UNRESOLVED`, zero siege insertions and the ordinary assault fallback. The recognized ID survives the rejection. |
| Original Survival save and graphical check | NOT RUN | All automated world mutations used marked disposable fixtures. The owner's roof and movement path need in-game confirmation on an isolated copy. |

The stationary owner-scale benchmark used 3,000 warm-up updates and 3,600 measured updates. Active update median / p95 were 5.1 / 28.4 µs before and 5.0 / 54.4 µs after. Sampled updates fell from 720 to 360 as the stable partial site stopped repeating the recenter pass. Idle medians were 0.1 µs. The peak remained 169 observations per update, and the chunk audit saw zero absent-chunk retrievals. Times are host elapsed measurements of the recognition service.

## Evidence and continuation

- New partial recognition requires at least 48 matching placements and 80 connected construction blocks across at least 6 × 6 horizontal bounds, eight walked columns supported by that component, Territorial Activity of at least six, and local equipment, verified use or overhead corroboration. Two overlapping observations separated by at least 50 ticks on the same continuous visit confirm the decision. Clipped walls, pure terrain and equipment without construction cannot satisfy those requirements.
- Each stored partial part includes its local approximate bounds and a bit mask of the actual observed construction. Later surveys must find at least eight shared construction cells in the current face-connected component before associating a recorded partial part. Legacy complete parts can supply a local anchor through currently observed construction inside their established bounds. A new non-overlapping strip requires at least 12 connected construction cells, four matching placements and observed supported walking. Competing site evidence blocks attachment.
- Continuous walking across a recognized partial footprint can bring the next survey forward after a five-block horizontal displacement and at least 12 ticks since the previous assessment. This keeps adjacent samples overlapping at ordinary walking speed. The 13 × 13 × 9 window and 169 observations per update still apply. The recorded extent is the union of local parts with no interpolated bridge through unsurveyed areas. Disconnected wings and segments reached by teleport need a surveyed construction path to demonstrate a shared identity.
- The registry allows up to 64 parts in a fragmented site and a bounded 512 × 64 × 512 envelope. A survey stores at most one new part. Sparse or unobserved extensions remain outside the recorded footprint. Ordinary compact compounds retain their existing limits.

## Save and encounter boundaries

Schema 1 and 2 registries remain readable and preserve IDs. A save writes schema 3 only when a partial construction mask is present. The new row payload is at most 191 mask bytes per local part. This stores the actual overlap evidence across reloads and distinguishes an unresolved perimeter from temporarily stale coverage of a previously complete footprint. Older binaries require the prior registry from the backup when rolling back a save that has written schema 3.

An unresolved site is returned by SiteQueries with PARTIAL coverage when its evidence is current. It is excluded from SITE_SIEGE approach planning until a compact site has a complete corroborated survey. A newly observed clipped boundary conveys no exterior guarantee. Existing PARTIAL coverage of a historically verified footprint still uses the native approach, open-sky, collision and insertion checks. Rejection leaves settlement identity intact, with ordinary PLAYER_ASSAULT governed by its existing scheduler. [Siege acceptance](survival-040-alpha1.md)

## Owner walkthrough

1. Copy the Survival save into an isolated Tekkit Dev profile for the updated build. Leave the existing construction and utility layout in place.
2. Return to the reported location and walk within the existing factory. After two local assessments, open Numpad 1 → Settlement Recognition. Check the site ID, high confidence and partial footprint. Use the survey page to record the local bounds and clipped faces.
3. Walk through the connected aisles at ordinary speed. Compare the number of observed parts and bounds at intervals, keeping the original site ID as the reference. Save and reopen to verify that the same observed parts remain attached.
