# Survival 0.4.0-alpha.2 — Tactical Site Sieges

The local Debug Hub can call a real SITE_SIEGE in daylight. Manual and natural sieges share site observations, placement, movement, tactical objectives and resolution. Open entrances, exposed frontage and defended obstacles now give a small group different physical goals. This advances the Project Vision's defensive architecture and counterplay, with site identity kept in Settlement Recognition and all future tactical roles assigned through the encounter director. [Project Vision](../../PROJECT_VISION.md) · [Approved scope](survival-040-spec.md#25-proposed-development-campaign)

## Admission and persistence

`GuiSiteSiege` is non-pausing. A click queues the selected profile and current player in the bound local world. The owner-thread world tick reads the queue after settlement observations and evaluates the request. The director arbitrates one major encounter. An outstanding request reserves its slot during planning and a second click reports the current reservation. Cancel accepts a pending or active manual request and releases its ownership on the next simulation tick.

The initial lookup uses the 24-block SiteQueries radius. The director prefers an eligible site associated with the player's position, then the nearest loaded recognized part, with site ID breaking a tie. It samples up to two parts. Manual planning stays on the selected site through any alternate lane search. PARTIAL coverage of a known compound can use a verified local route. An unresolved clipped footprint can use AUTO or PERIMETER with a verified local lane, four additional outward standing cells and both flanks of the insertion. Its spawn must be outside the full recorded envelope, like all other site insertions. Its specialist and natural admission require a resolved perimeter.

`NATURAL` follows the full-moon window, seed rarity and optional site-siege setting. Its temporary record, consumed boundary and two-cycle site cooldown keep the alpha.1 format. `MANUAL_DEBUG` uses session memory for admission and resolution. It bypasses the lunar requirements, cooldown, rarity, natural toggle and hostile light checks. It still requires a live, awake local Survival player, a non-Peaceful Overworld, suitable ground, open sky, legal collision clearance, local hostile headroom and an EXPOSED Protected Interior assessment. All insertions stay outside recorded site envelopes with a two-block margin and at least 12 blocks from the player.

An already reserved natural boundary takes priority over a queued click whose planning has yet to start. During manual planning or an active manual siege, the calendar advances and records any passing natural boundary through its normal watermark. The director skips that major admission and queues no replay. Natural patrol and aerial policies keep their own authority.

Manual skeletons have native movement and combat. Their transient subclass suppresses the sunlight brightness test during the native living update. Ordinary fire sources can still damage them. Debug attackers have no save entry and living members are removed on resolution, cancellation, unload or world change. Native combat deaths retain normal drops. Natural skeletons return to their ordinary lifecycle after task release. Assigned specialists are likewise transient in natural sieges so later autonomous tactics cannot extend a siege's damage allocation. Save and reopen can retain an already applied world edit and its native drops. Encounter tasks and queued commands are session-only, and the natural interrupted-on-reopen record applies to natural sieges.

## Objectives and tactics

| Objective | Actor and outcome |
| --- | --- |
| `APPROACH_ENTRANCE` | Skeletons can physically cross a valid open threshold and reach its next standing cell. |
| `PRESSURE_PERIMETER` | Skeletons move to verified frontage and resume their local native combat. Open workshops and sealed walls can both have useful exterior frontage. |
| `PRESSURE_ENTRANCE` | A Breacher approaches the outside of one closed vanilla wooden door or fence gate on Normal or Hard. A cue marks a 36-tick windup. One native opening transition can let the group continue through the threshold. |
| `LIMITED_BREACH` | A Tank on Hard with structural breaching enabled approaches one verified structural column. Its 24-tick windup can remove up to two eligible cells. A narrow opening can admit the skeletons while the Tank holds outside. |

AUTO tries open access, eligible wooden entrance pressure, enabled structural pressure and exterior frontage in that order within a candidate part. PERIMETER restricts the plan to open access and frontage. BREACH requires one of the two specialist objectives. Structural edits require `siteSiegeStructuralBreaching=true` even in BREACH mode. The default is false. [Configuration](configuration.md)

The group owns one fixed site, sampled lane and destination. An assigned specialist shares that lane and receives the exact barrier position and face. The movement task issues native waypoints and validates the Tank's wider and taller collision envelope separately. A defender can close an opening, block the lane, stop the attack with damage, or kill the specialist. A wooden entrance opened by the defender can allow advance with zero forced transitions. After action, passage cells are rechecked before issuing further movement. A second wall layer can stop the party with `PASSAGE_BLOCKED`.

Structural eligibility reuses the Tank whitelist and native drops for planks, logs, cobblestone, glass and brick. The planner requires a matching physical face and revalidates the loaded target's block ID and metadata, side, range, footing and nearby attachments before impact. The structural path checks both intended cells and then revalidates the lower cell after any upper edit and neighbor notification. Functional or uncertain neighbors disqualify the edit. Machinery, storage, TileEntities, non-vanilla doors and unknown blocks fall outside the eligible target set. Native glass recovery follows the vanilla drop rule. The monitor counts successful block removals separately from entrance transitions.

## Budgets and limits

- One shared group, up to three allocated attackers and one specialist. The existing local hostile cap is 28. Reinforcements use at most seven insertion attempts at 20-tick spacing during approach.
- Up to 24 candidate lanes in 40 planning ticks. Each sample caps standing positions at 28 and spawn sky reads at 255. Wide positions check support on the 3 × 3 footprint and up to four body cells per supporting column. Protected Interior shares the existing recognition interval and its 169-read cap.
- A committed group owns one route. Local observations revalidate upcoming waypoints every five ticks, with one validated extension through an opened or breached threshold. A stalled approach ends after 120 ticks without waypoint progress.
- One interruptible windup against the chosen obstruction, with zero replacement targets. The encounter ends on elapsed budget, lost route or site, absent player, Peaceful, missing attackers or cancellation. A group can engage for a minimum window before normal resolved arrival. The active deadline is 600 ticks.

The current planner uses straight, level, cardinal lanes and samples three offsets per face. Stairs, turns, steep terrain, unsupported shapes and ambiguous frontage may yield an exterior hold or rejection. A visually identical block replacement between observations presents the same native ID and metadata. The controller compares native state and never retargets on replacement. Industrial Infiltration and Combined Arms remain later scope.

## Owner walkthrough

1. Launch the isolated **Tekkit Dev** instance and open a copy of the Survival save. Use Normal or Hard and stay in Survival. The daytime test uses the world's current clock.
2. Stand inside a recognized part or within 24 blocks of a recognized part. Confirm its positive site ID under **Numpad 1 → Settlement Recognition**. Leave a level exterior lane with about 14 blocks of clear approach and stand at least 12 blocks from its insertion end.
3. Open **Numpad 1 → Site Siege**. Click the profile selector to choose **PERIMETER** and click **Call Site Siege**. Check for `REQUESTED → PLANNING → ADMITTED → ACTIVE`, origin `MANUAL_DEBUG`, the selected site and coverage, an exterior insertion and its destination. `ADMITTED` reserves the encounter. `ACTIVE` confirms a native insertion.
4. Return from the Hub and watch the group move. With a closed entrance, look for `PRESSURE_PERIMETER` at exterior frontage. Call again with the entrance open and look for `APPROACH_ENTRANCE` and an arrival on the inside. Use **Cancel Test Siege** to end the preceding attempt when needed. Expect `CANCELLED`, `DEBUG_CANCELLED` and zero active attackers.
5. Close a wooden door or fence gate. Select **BREACH** with the structural setting still off. On Normal or Hard a verified lane gives `APPROACHING → PRESSURING → ENGAGING`, `ENTRANCE_OPENED` and one entrance change in Details. Repeat and hit or kill the specialist during the windup. Expect `WINDUP_INTERRUPTED` or `SPECIALIST_LOST` with the entrance still closed.
6. In a disposable copy, set `siteSiegeStructuralBreaching=true` in `minecraft/config/ZedsTekkitOverhaul.cfg`, restart the instance and choose Hard. Select **BREACH** at a sealed cobblestone column with wide exterior clearance. Expect `APPROACHING → BREACHING → ENGAGING` and `STRUCTURE_BREACHED` if the Tank completes its action. Details records up to two removals. A second layer can resolve the approach at `PASSAGE_BLOCKED`. Try killing the Tank during windup and compare the unchanged wall. Structural tests with the setting off report `STRUCTURAL_BREACH_DISABLED` when a structural candidate is the only eligible tactic.
7. Reopen Details and compare the natural cooldown and forecast with the values before the manual call. Save and quit during a separate manual attempt, then reopen. Expect request `READY`, no resumed group and the same recognized site. A naturally eligible future dusk follows its own schedule.
8. Check Small, Normal, Large and Auto GUI scales. At each scale verify all three controls, the overview and error rows, Details, and **Back to Debug Hub**. Compare actual approach and counterplay with the monitor's outcome.

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

Graphical controls, all GUI scales and the owner's Survival save await manual acceptance. The native probes simulate the same queue entry used by the buttons and measure world behavior without graphical input. The Windows graphical runner is restricted by the repository's input isolation rule. [Test tiers](testing.md#background-client-launch)
