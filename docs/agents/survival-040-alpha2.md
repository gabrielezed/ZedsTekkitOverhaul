# Survival 0.4.0-alpha.2 — Tactical Site Sieges

The local Debug Hub can call a real SITE_SIEGE in daylight. Manual and natural sieges share site observations, placement, movement, tactical objectives and resolution. Open entrances, exposed frontage and defended obstacles shape the deployment and combat of a mixed force. Reinforcements give the defender different targets over the course of one site-directed encounter. This advances the Project Vision's defensive architecture and counterplay. Settlement Recognition supplies stable site identity and the force plan reserves roles for the encounter director. [Project Vision](../../PROJECT_VISION.md) · [Approved scope](survival-040-spec.md#25-proposed-development-campaign)

## Admission and persistence

`GuiSiteSiege` is non-pausing. A click queues the selected profile, intensity and current player in the bound local world. The owner-thread world tick reads the queue after settlement observations and evaluates the request. The director arbitrates one major encounter. An outstanding request reserves its slot during planning and a second click reports the current reservation. Cancel accepts a pending or active manual request and releases its ownership on the next simulation tick.

The initial lookup uses the 24-block SiteQueries radius. The director prefers an eligible site associated with the player's position, then the nearest loaded recognized part, with site ID breaking a tie. It samples up to two parts. Manual planning stays on the selected site through any alternate lane search. PARTIAL coverage of a known compound can use a verified local route. An unresolved clipped footprint can use AUTO or PERIMETER with a verified local lane, four additional outward standing cells and both flanks of the insertion. Its spawn must be outside the full recorded envelope, like all other site insertions. Its specialist and natural admission require a resolved perimeter.

`NATURAL` follows the full-moon window, seed rarity and optional site-siege setting. Its temporary record, consumed boundary and two-cycle site cooldown keep the alpha.1 format. `MANUAL_DEBUG` uses session memory for admission and resolution. It bypasses the lunar requirements, cooldown, rarity, natural toggle and hostile light checks. It still requires a live, awake local Survival player, a non-Peaceful Overworld, suitable ground, open sky, legal collision clearance, local hostile headroom and an EXPOSED Protected Interior assessment. All insertions stay outside recorded site envelopes with a two-block margin and at least 12 blocks from the player.

An already reserved natural boundary takes priority over a queued click whose planning has yet to start. During manual planning or an active manual siege, the calendar advances and records any passing natural boundary through its normal watermark. The director skips that major admission and queues no replay. Natural patrol and aerial policies keep their own authority.

Siege Zombies and Skeletons retain native movement, combat and death drops. The manual variants suppress the sunlight check inside their native living update, including daytime calls. Normal fire can damage them. While assigned, Zombies leave closed-door changes to the one verified encounter objective. All siege attackers are transient and remaining living members are removed on resolution, cancellation, unload or world change. Thief loot remains in the ordinary custody and recovery system. A reopened world can retain an applied block change, native drops and a recoverable Thief trail. The natural interrupted-on-reopen record prevents replay.

## Force plan

Admission freezes the chosen site's ID and part, difficulty, territorial assessment at the part's center, primary objective, roster, timing and any structural budget. The assessment reads the existing regional activity blend and biome contributions. Missing assessment uses the Low profile and is shown as unavailable. The monitor offers independent intensity choices. NATURAL assesses the site. LOW, MODERATE, HIGH and MAXIMUM apply to the queued manual call.

| Difficulty | Low, below 16 | Moderate, 16–23 | High, 24–44 | Maximum, 45–50 |
| --- | --- | --- | --- | --- |
| Easy | 4 / 3 | 5 / 4 | 7 / 4 | 10 / 5 |
| Normal | 6 / 4 | 8 / 5 | 11 / 6 | 15 / 7 |
| Hard | 8 / 5 | 10 / 6 | 14 / 7 | 18 / 9 |

Values show total allocation and desired simultaneous ceiling. The active ceiling is reduced to the configured `assaultActiveCap` when necessary. The default is eight. Two waves are usual at Low and Normal Moderate. High and Maximum, Hard Moderate, and a structural objective have three. Each wave contains a fixed set of role slots. A wave can close with unused slots after geometry, local population or native spawn rules prevent deployment. Thief assignment is seeded and occasional. Industrial wealth adds no multiplier.

The first wave establishes exterior pressure with infantry and a Skirmisher from Moderate on Normal or Hard. The main assault applies a Breacher to an eligible door or gate. Hard Maximum can send a late Tank and a second Skirmisher. The last available active slot in a wave gives a pending Breacher or Tank priority over infantry. A Tank without an assigned structural target can sweep exposed defenders from its wide exterior staging area. Saboteur allocation and industrial infiltration remain alpha.3 scope. Aerial roles remain later scope.

## Objectives and tactics

| Objective | Actor and outcome |
| --- | --- |
| `APPROACH_ENTRANCE` | Infantry can physically cross a valid open threshold and reach its next standing cell. |
| `PRESSURE_PERIMETER` | Infantry move to verified frontage and resume their local native combat. Open workshops and sealed walls can both have useful exterior frontage. |
| `PRESSURE_ENTRANCE` | A Breacher approaches the outside of one closed vanilla wooden door or fence gate on Normal or Hard. A cue marks a 36-tick windup. One native opening transition can let the group continue through the threshold. |
| `LIMITED_BREACH` | A Tank on Hard with structural breaching enabled approaches one verified structural column. Its 24-tick windup can remove up to two eligible cells. A narrow opening can admit infantry while the Tank holds outside. |

AUTO tries open access, eligible wooden entrance pressure, enabled structural pressure and exterior frontage in that order within a candidate part. PERIMETER restricts the plan to open access and frontage. BREACH requires one of the two specialist objectives. Structural edits require `siteSiegeStructuralBreaching=true` even in BREACH mode. The default is false. [Configuration](configuration.md)

The selected site and primary approach remain fixed. Infantry takes the primary lane or a verified alternate offset. Skirmishers stage outside the chokepoint and use their native aim and energy pulse when a defender is exposed. Tanks require a wide lane and a separate clearance check. A Thief must close physically on a visible defender before its normal windup and inventory checks. Assigned specialists retain the staging reference during combat. Future-wave insertion starts remain outside the site even when a threshold opens.

An obstructed route can remove an individual attacker. A defender can close an opening, stop the windup with damage, or kill the specialist. A wooden entrance opened by the defender can allow advance with zero forced transitions. No alternate barrier is assigned. Passage cells are rechecked before further movement. A second wall layer can leave the primary objective at `PASSAGE_BLOCKED` while reserves continue to contest the exterior.

Structural eligibility reuses the Tank whitelist and native drops for planks, logs, cobblestone, glass and brick. The planner requires a matching physical face and revalidates the loaded target's block ID and metadata, side, range, footing and nearby attachments before impact. The structural path checks both intended cells and then revalidates the lower cell after any upper edit and neighbor notification. Functional or uncertain neighbors disqualify the edit. Machinery, storage, TileEntities, non-vanilla doors and unknown blocks fall outside the eligible target set. Native glass recovery follows the vanilla drop rule. The monitor counts successful block removals separately from entrance transitions.

## Budgets and limits

- The local hostile cap is 28. The director makes at most one deployment attempt every eight ticks while an active slot is available. A role slot gets up to nine placement failures. Light rolls, protection deferrals and cap delays consume time in the deployment window. Failed slots remain accounted for by role and reason.
- Up to 24 candidate lanes in 40 planning ticks. Each sample caps standing positions at 28 and spawn sky reads at 255. Wide positions check support on the 3 × 3 footprint and up to four body cells per supporting column. Protected Interior shares the existing recognition interval and its 169-read cap.
- Waves become eligible at combat ticks 0, 320 and, for three-wave plans, 690. Final deployment closes at tick 750 or 1,130. A member has up to 560 engaged ticks after insertion. A stalled route withdraws its member after 140 ticks without waypoint progress. Natural admission has a bounded darkness wait.
- Reaching frontage or clearing the first wave leaves the reserves eligible. Elimination, expired waves, encounter deadline, invalidated site, player absence, difficulty change, world lifecycle and cancellation have finite resolution. One interruptible windup uses only the assigned barrier and damage budget.

The planner uses straight, level, cardinal lanes. Stairs, turns, steep terrain and unsupported shapes can force a short exterior hold or prevent deployment. A visually identical replacement between observations presents the same native ID and metadata. The controller compares native state and keeps the original target.

## Owner walkthrough

1. Install the candidate mod and matching agent into the isolated **Tekkit Dev** instance with Prism closed. Launch it and open a copy of the Survival save. Use Normal or Hard and stay in Survival. The daytime test uses the world's current clock. [Prism installation](../deploy-prism.md)
2. Stand inside a recognized part or within 24 blocks of a recognized part. Confirm its positive site ID under **Numpad 1 → Settlement Recognition**. Leave a level exterior lane with about 14 blocks of clear approach and stand at least 12 blocks from its insertion end.
3. Open **Numpad 1 → Site Siege**. On Hard, select **MAXIMUM** and **AUTO**, then click **Call Site Siege**. For a frontage-only control run, select **PERIMETER**. Check for `REQUESTED → PLANNING → ADMITTED → ACTIVE`, origin `MANUAL_DEBUG`, the selected site and coverage, an exterior insertion and its destination. `ADMITTED` reserves the encounter. `ACTIVE` confirms a native insertion.
4. Return from the Hub and watch the group move. With a closed entrance, look for `PRESSURE_PERIMETER` at exterior frontage. Call again with the entrance open and look for `APPROACH_ENTRANCE` and an arrival on the inside. Use **Cancel Test Siege** to end the preceding attempt when needed. Expect `CANCELLED`, `DEBUG_CANCELLED` and zero active attackers.
5. Close a wooden door or fence gate. Select **BREACH** with the structural setting still off. On Normal or Hard a verified lane gives `APPROACHING → PRESSURING → ENGAGING`, `ENTRANCE_OPENED` and one entrance change in Diagnostics. Repeat and hit or kill the specialist during the windup. Expect `WINDUP_INTERRUPTED` or `SPECIALIST_LOST` with the entrance still closed.
6. In a disposable copy, set `siteSiegeStructuralBreaching=true` in `minecraft/config/ZedsTekkitOverhaul.cfg`, restart the instance and choose Hard. Select **BREACH** at a sealed cobblestone column with wide exterior clearance. Expect `APPROACHING → BREACHING → ENGAGING` and `STRUCTURE_BREACHED` if the Tank completes its action. Diagnostics records up to two removals. A second layer can resolve the approach at `PASSAGE_BLOCKED`. Try killing the Tank during windup and compare the unchanged wall. Structural tests with the setting off report `STRUCTURAL_BREACH_DISABLED` when a structural candidate is the only eligible tactic.
7. Open Diagnostics and compare the natural cooldown and forecast with the values before the manual call. Save and quit during a separate manual attempt, then reopen. Expect request `READY`, no resumed group and the same recognized site. A naturally eligible future dusk follows its own schedule.
8. Check Small, Normal, Large and Auto GUI scales. At each scale verify both selectors, the overview, Force and Diagnostics pages, and **Back to Debug Hub**. Compare actual approach and counterplay with the monitor's outcome.

| Monitor result | What to inspect |
| --- | --- |
| `NO_RECOGNIZED_SITE` | Recognition, loaded coverage and initial query range. |
| `APPROACH_UNLOADED`, `SURFACE_OUT_OF_REACH`, `ROUTE_OBSTRUCTED`, `TANK_CLEARANCE` | Loaded ground, elevation, continuous clearance and specialist dimensions. |
| `INSIDE_SITE_EXCLUSION`, `PROTECTED_INSERTION`, `UNCERTAIN_PROTECTION` | Recorded compound and local placement safety. |
| `OCCUPIED`, `POPULATION_CAP`, `SPECIALIST_POPULATION`, `PLAYER_TOO_CLOSE` | Existing local hostiles or a blocked insertion. |
| `NO_ELIGIBLE_BREACH`, `STRUCTURAL_BREACH_DISABLED`, `SITE_FOOTPRINT_UNRESOLVED` | Requested profile, target material and configuration. |
| `PLAYER_ASSAULT_ACTIVE`, `SITE_SIEGE_ACTIVE`, `REQUEST_PENDING` | Major encounter reservation. |
| `PEACEFUL`, `PLAYER_UNAVAILABLE`, `WORLD_UNAVAILABLE` | Difficulty, awake Survival player and bound Overworld. |

After `REJECTED`, correct the reported condition and call again. Terminal results retain attacker counts, the last approach and the tactical outcome for inspection.

## Verification

Use `zto-tools/test-zto-agent.ps1 -Focus site-siege` for the native local command queue, tactical fixtures, mixed equipment and natural regressions. Use `-Focus partial` for the owner-scale clipped case. The full `-Pack` run includes both. All automated native fixtures use marked disposable saves, pinned Tekkit Classic 3.1.2, Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176 and Java 8u51. The MCP build uses Forge 3.4.9.171 with MCP 6.2 and JDK 8.

### Three-attacker baseline

Clean build `e5934af1a683403e4ba11afa4eec867d4a6a19b3` compiled and reobfuscated through MCP. The pack test used the verified 478-entry mod archive with SHA-256 `270c3c4c3036453056b1a6eeffe72bdcc303f5dac9ee6829f1f93fe5b5b05d0c` and agent SHA-256 `7e3cbb494a77110a70490d08bf0af59ba43885160e7e0ffa6d4a0941dacbee06`.

The complete headless `-Pack` run passed, including the installed and reduced classpaths, the natural assault and patrol baseline, settlement and provenance, ordinary Enderkin tactics and the pinned compatibility suite. Portable configuration, lunar calendar, territory, wave policy and documentation checks passed.

- The manual New Moon fixture inserted three daylight attackers and observed 15 blocks of movement to the open threshold with a natural cooldown active and natural site sieges disabled. Native save/reopen kept the natural record and site ID. Pending and active duplicate requests, Peaceful, absent recognition, population pressure, and mutual exclusion with PLAYER_ASSAULT produced explicit results.
- Natural AUTO and manual BREACH both opened an eligible wooden door. A closed fence gate also opened and the attackers moved through it. Opt-in structural pressure removed exactly two cobblestone cells. A second wall layer held. Combat, specialist death, a replaced block, an unloaded target and save/reopen during windup prevented impact.
- The mixed workshop compared persisted state for an actual IC2 cable and energy buffer, BuildCraft pipe and fluid buffer, ZTO Hopper, chest and unknown functional tile. The Breacher opened one door and all sampled device NBT matched, including after reopen.
- The clipped factory retained site #1, 44 parts and PARTIAL coverage across save/reopen. The perimeter test placed three actors outside the recorded envelope and observed 7.50 blocks of progress. Elevated and underground fixtures rejected unsupported routes. The audited siege fixtures recorded zero missing-chunk retrievals by the director.

Elapsed time was sampled on the owning thread. Native hostile times include all fixture attackers during one tick. The planner column counts include active revalidation. These measurements cover the sampled legacy runtime on the development host.

| Scenario | Planner p95 µs | Active director p95 µs | Native hostiles p95 µs | Peak block and metadata calls per director tick |
| --- | ---: | ---: | ---: | ---: |
| Daytime approach | 2,179.3 | 41.1 | 218.0 | 63 / 24 |
| Closed wooden door | 78.8 | 22.2 | 100.6 | 59 / 32 |
| Closed fence gate | 95.7 | 27.5 | 64.5 | 67 / 25 |
| One cobblestone layer | 150.7 | 32.8 | 97.2 | 698 / 142 |
| Two cobblestone layers | 107.5 | 36.5 | 93.3 | 700 / 144 |

The daytime sample took 9.1 ms of total planner work including discovery, with an initial peak of 7.2 ms. The clipped factory's 12 candidate lanes took 7.6 ms total. Measured geometry samples stayed within 28 standing positions and 255 sky reads, peak active enemies reached three, and recognition's peak remained 169 reads.

Graphical controls, all GUI scales and the owner's Survival save await manual acceptance. The native probes simulate the same queue entry used by the buttons and measure world behavior without graphical input. The Windows graphical runner is restricted by the repository's input isolation rule. [Test tiers](testing.md#background-client-launch)
