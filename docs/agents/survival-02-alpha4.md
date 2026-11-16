# Survival 0.2.0-alpha.4

Alpha.3 could make the opening more varied on Normal but put only skeletons in ordinary Hard assault slots. It also allowed living early-wave mobs to consume all active places before a selected Enderkin could attempt placement. Alpha.4 restores native zombie pressure on Hard and gives selected opt-in Enderkin a bounded opportunity within the existing assault population. This makes doors, firing lanes and retreat routes part of the player's engineering choices. The director still owns only a player-vicinity encounter. Site recognition and asset ownership belong to later systems.

## Native Hard pressure

Hard plans repeat two skeleton slots and one zombie slot. Quiet opens with `ZSS`, Elevated opens with `SZS`, and other openings and later waves repeat `SSZ`. Each sequence is truncated to the planned allocation, including when a receiving wave uses a carried slot. At the default configured budget of 15 the Elevated allocation is `SZSSZS / SSZSS / SSZS`. Even on Hard a short wave can contain only one species. Changes to difficulty abort the active encounter, and a subsequent admission plans fresh slots. First-quarter and waxing-gibbous patrol composition retains its own existing policy.

| Profile | Player-facing pressure |
| --- | --- |
| Quiet, threat below 16 | Smaller opening allows time to intercept or withdraw. Easy and Normal are melee-led. The first Hard opening slot may immediately put a door-breaking zombie on the approach. |
| Ordinary, threat 16–23, or adaptation disabled | Established wave allocation. Hard opens with ranged pressure and introduces zombies periodically. |
| Elevated, threat 24 or higher | Larger mixed opening raises the value of cover, early target selection and a retreat path. Hard inserts a zombie between the first two skeleton slots. |

The installed Minecraft 1.2.5 classes `is` and `ue` match MCP's door interaction and break tasks. A vanilla zombie enables break-door navigation and registers the break task at priority 1. The task requires horizontal collision, an unfinished eligible navigator path and a closed `Block.doorWood` on a nearby path point or under the zombie's upper body. The path point check uses a horizontal distance of at most 1.5 blocks and a continuation condition within two blocks of the target. A sustained attempt consumes 240 task ticks, about 12 seconds at 20 TPS. Destruction requires live difficulty 3 at expiration. Easy and Normal can exhibit the attempt without destruction. The check is for the vanilla wooden-door ID, so iron doors and modded doors on separate IDs are outside this task. A mod reusing or patching the vanilla ID would need its own verification.

The production change creates the existing vanilla classes. Native combat, placement safety and the Breacher's separate non-destructive pressure remain the authorities for their behavior. ZTO adds no machinery or inventory mutation to the assault. A door open under redstone control can still admit hostiles. A closed iron entrance offers protection under the tested rules.

## Opt-in Enderkin presence

Set `experimentalEnderkinEncounters=true` in `minecraft/config/ZedsTekkitOverhaul.cfg` and restart the client. Territorial threat is independent and both settings default to false. The seeded rolls still select a waxing-gibbous patrol Skirmisher at 25%, a wave-2 Skirmisher at 50% and a wave-3 Breacher at 40%. The assault rolls require a planned allocation in the relevant wave. The portable sweep across 10,000 seed/cycle pairs counted 2,491 patrol Skirmishers, 5,010 wave Skirmishers, 3,996 Breachers and 2,957 full moons with neither role selected. These are selection counts, before placement.

Each already-selected future assault role reserves one place from the active assault ceiling for ordinary insertions in earlier waves, with a minimum ordinary ceiling of one. Its actual insertion still spends the first budgeted slot of its wave. Its four placement attempts, ordinary fallback, retry windows, existing allocations, actual active cap and local hostile cap all apply. A failed role releases its priority immediately. The lowest cap can leave only one place available to two later selections. Previous living mobs and the separate local cap can also defer a role; existing mobs are never cleared to make space.

Read `assault.snapshot` for admission difficulty, frozen threat inputs, wave plan, outcomes, and `enderkin.eligible`, `selected`, `capDeferrals`, `attempts`, `inserted` and `rejections`. An earlier ordinary retry blocked by this policy reports `ENDERKIN_HEADROOM`. A selected role with zero attempts and cap deferrals had a population problem. A selected role with rejected candidates exposes its placement problem. Placement still requires a loaded, unobstructed outdoor surface, sufficient space, open local approaches, allowed light and, for the Breacher, absence of a nearby Breacher. The protected-building rejection rules apply to both experiments.

In an eight-day cycle, the experiment offers one eligible patrol night and one eligible full-moon night, subject to natural scheduling and player eligibility. Even after selection and insertion, a Skirmisher must find an opportunity to telegraph and fire, and a Breacher must find a suitable barrier to show obstruction pressure. The existing poses, Pulse damage, pressure hits, entity names, eggs, and NBT are retained. Player recognition needs a human trial.

## Validation

Baseline is integrated `main` at `81d828382de7ab82276e8929d66372cac0248233` (PR #32). The alpha.4 production source is `e5e09e240c2b4d4ebb714dde3fd54d941f100c60`. The tested archive SHA-256 is `286362becc67225dbbb9a1c86194823990ff8ac44c3b09eb3dc00abefe7424c1`. The sidecar records a dirty workspace with local notes and test work. Packaged production files correspond to the production commit. MCP 6.2, JDK 8.0.504.1 and development Forge 3.4.9.171 completed client compilation, reobfuscation and streaming validation of 306 archive entries. Tekkit Classic 3.1.2 reported Minecraft 1.2.5, runtime Forge 3.3.8.164, FML 2.2.106.176 and Java 1.8.0_51.

Portable configuration, lunar recurrence policy, scheduling, outdoor approach, seeded Enderkin, territorial persistence, profile/cap coverage for budgets 1–24, Dev Bridge, Hopper, crafting, EMC, alchemical conversion and distillation core/layout tests passed. The runtime pressure probe uses a fixed arena and placement RNG seed, natural dusk, preserved hostiles, and zero music/effects with an uninitialized sound backend verified in each disposable client. Health drops are a damage proxy. Safety heals and recentering keep observation alive and do not establish player survivability. Each row is one sample. Ambient combat, placement timing, knockback and equipment can change outcomes between otherwise comparable runs.

### Native entrance observations

The installed native task removed both halves of a wooden door after exactly 240 task updates in a controlled path/collision test on Hard. The same task selected the wooden door on Normal and left it intact. It refused the closed iron, open iron and open wooden cases. The untouched wooden door in the passive Hard shelter remained intact for the natural assault. The controlled task result establishes the real destruction rule, while the natural shelter result reflects the limited acquisition opportunities of that geometry. The natural closed iron and torch-powered open iron doors retained both halves. The open iron shelter took damage after tick 222. RedPower machine and stocked chest TileEntity identity and serialized NBT survived all three Hard shelter trials. A powered RedPower entrance and a running industrial assembly need their own trial.

### Persistent pressure

All rows below passed the game-thread and authenticated HTTP assertions. Waves began at elapsed ticks 100, 1000 and 1900. Each reached `ENCOUNTER_TIMEOUT` at tick 3080. Each used the same Plains arena with its respective activity sample and left hostiles alive. Time at cap counts distinct sampled simulation ticks and can undercount if a render skips a game tick. Z, S, K and B denote Zombie, Skeleton, Skirmisher and Breacher. An empty wave has zero insertions.

| Scenario | Planned | Inserted by wave | Peak / overlapping waves | Cap samples / cap deferrals | Deaths / active at timeout | First damage / safety heals |
| --- | --- | --- | --- | --- | --- | --- |
| Elevated Hard, wood | 6/5/4 | SZSSZS / SS / SSZ | 8 / 3 | 1752 / 23 | 0 / 7 | — / 0 |
| Elevated Hard, closed iron | 6/5/4 | SZSSZS / SS / — | 8 / 2 | 1183 / 26 | 0 / 7 | — / 0 |
| Elevated Hard, powered open iron | 6/5/4 | SZSSZS / SS / SSZ | 8 / 3 | 1797 / 23 | 3 / 8 | 222 / 5 |
| Quiet Normal + Enderkin | 2/6/6 | ZZ / KZZSZ / B | 8 / 3 | 910 / 13 | 1 / 7 | 206 / 34 |
| Ordinary Normal + Enderkin | 4/5/6 | ZZSZ / KZS / BZ | 8 / 3 | 1131 / 12 | 2 / 7 | 206 / 37 |
| Elevated Normal + Enderkin | 6/5/4 | SZSZSZ / ZZ / BZ | 8 / 3 | 1147 / 12 | 2 / 8 | 219 / 42 |
| Elevated Normal, QuantumSuit | 6/5/4 | SZSZSZ / ZZS / SZ | 8 / 3 | 1109 / 23 | 5 / 6 | — / 0 |
| Quiet Normal, Enderkin off | 2/6/6 | ZZ / ZZZSZZ / — | 8 / 2 | 1915 / 14 | 0 / 8 | 206 / 47 |
| Elevated Normal, Enderkin off | 6/5/4 | SZSZSZ / ZZSZ / — | 8 / 2 | 1380 / 24 | 3 / 7 | 219 / 31 |
| Normal, both switches off | 4/5/6 | ZZSZ / ZZSZ / — | 8 / 2 | 1173 / 24 | 1 / 7 | 207 / 42 |

The installed QuantumSuit consumed 14,040 EU while sustaining 20 health. Among open leather samples with Enderkin, the Elevated profile delivered a later first health loss than Quiet and a similar median damage gap (20 ticks). Alpha.3 controls also first damaged the passive Quiet player at tick 206 and Elevated at tick 219. Melee closing distance, ranged line of sight and recentering all affect this comparison. The result supports tracking profile rhythm, target choice and exposed approach separately from raw damage.

### Hard open-arena comparison

The same passive leather fixture compared the actual alpha.3 archive (production source `51b34d27f22f38532e18152ecc617b95ef30eaad`, SHA-256 `c0ca2c62381e8e25897a27cb7cf11bbf86846bb1d69ec70239d2df4ddff49356`), alpha.4 production and an alternating Hard slot order set only by the test harness. Each used a 6/5/4 plan and reached a peak of eight with three overlapping waves.

| Hard slot policy | Insertion by wave / zombies total | First projectile / first damage | Damage samples / safety heals | Cap samples / deferrals | Deaths / remaining / timeout |
| --- | --- | --- | --- | --- | --- |
| Alpha.3 skeletons | 6/5/4 / 0 | Unmeasured / 154 | 77 / 30 | 353 / 11 | 11 / 4 / 2920 |
| Alpha.4 bounded mix | 6/5/4 / 4 | 126 / 131 | 71 / 31 | 781 / 9 | 10 / 5 / 2860 |
| Fixture alternating mix | 6/4/3 / 6 | 126 / 132 | 112 / 54 | 1656 / 21 | 5 / 8 / 3080 |

The bounded mix delivered the first damage sooner than the all-skeleton sample and sustained a full population for more sampled ticks while requiring a comparable number of fixture heals. The alternating mix stayed at cap for substantially longer and needed more interventions in this sample. Keep the smaller share to expose a real breach threat and leave more room for responses. Open samples used recentering, so the record supports a conservative initial policy and a follow-up owner comparison across cover and retreat routes.

In a basic compound fixture, the script supplied normal movement input along a walled approach and used a native lever to close the iron door after crossing. First player acquisition, projectile and health loss were observed at ticks 101, 126 and 131. The player reached shelter and closed the door at tick 270. It took two health-loss samples, ended with a minimum of 13 health and used zero heals and zero recenterings. The 6/5/4 plan inserted 6/2/1 mobs, peaked at eight with three overlapping waves, deferred 25 retries at cap, and timed out at tick 3080 with four active, five removed from tracking and no observed deaths. The machine, stocked chest and door survived. The route demonstrates an available defense in this layout. Human navigation, combat and other approach shapes remain open.

### Selected versus inserted

One seed selected both roles in each of the three representative Normal profiles. All six eligible opportunities reached the placement code with zero special cap deferrals. Earlier ordinary retries recorded 9, 11 and 12 headroom deferrals respectively. Those counts are deferred retries and can include repeat retries for the same allocation.

| Profile | Wave 2 Skirmisher | Wave 3 Breacher |
| --- | --- | --- |
| Quiet | Inserted at 1000 on attempt 1. Aiming observed at 1020. Present in 1,794 sampled ticks. | Inserted at 1900 on attempt 1. Present in 1,136 sampled ticks. |
| Ordinary | Inserted at 1000 on attempt 1. Aiming observed at 1114. Present in 959 sampled ticks. | Inserted at 1900 on attempt 3 after two sky-light rolls. Present in 1,149 sampled ticks. |
| Elevated | Four rejections, comprising three sky-light rolls and one roof check. Ordinary fallback took the slot. | Inserted at 1900 on attempt 2 after an approach rejection. Present in 1,167 sampled ticks. |

Matched alpha.3 scenarios also selected all six roles. They inserted two Skirmishers and one Breacher. In Quiet, the earlier waves filled all eight places and the selected Breacher had zero placement attempts over 14 cap-blocked retries. In Ordinary it attempted placement four times and failed three sky-light checks and one approach check. The Elevated Skirmisher failed four sky-light checks. The alpha.4 reservation directly addresses the Quiet starvation case. The Ordinary placement outcome can also vary with the sampler sequence and player movement. These samples give a paired result of 3/6 versus 5/6 for the chosen seed, with three fewer ordinary insertions across the matched samples and the same peak of eight.

These fixtures demonstrate insertion and two ranged telegraphs. The open arena did not elicit Breacher obstruction swings. The separate `ender-waves` ability probe observed a cast Ender Pulse, a Breacher pressure swing at a closed wooden door, and a successful reopen without replay. It uses a directed doorway and clears mobs between samples. The Pulse capture shows the narrow silhouette and bright eyes at range. The doorway capture also illustrates that the barrier can occlude the Breacher's silhouette. Presence samples count a role while it stays within the arena observation volume. Outdoor visibility, clearance and line-of-sight opportunities remain distinct from the selection roll. The three same-seed samples do not estimate the natural rate across worlds. Human recognition and audio feedback remain open. The logs and bounded HTTP records under `dist-zto/test-reports` retain wave attempts, rejections, insertion ticks and outcomes.

### Pack regressions

- **PASS:** Ten persistent pressure cases, three additional alpha.4 Hard comparator/retreat cases and four alpha.3 comparators. Every pressure peak was within the configured cap.
- **PASS:** `ender-waves`, general outdoor placement, multi-wave, quiet and disabled-mode probes. With local population saturated, the selected patrol Skirmisher received zero attempts. A separate fallback probe exhausted four special attempts at an out-of-reach surface before completing the ordinary patrol.
- **PASS on retry:** Natural recurrence, replay suppression, save/reopen, NEI time-skip, phase alignment, Peaceful and Creative eligibility, difficulty-change abort and dimension return. The initial augmented fixture crossed the generated portal again before its new difficulty assertion. The corrected fixture checks Hard admission and its abort before changing dimensions.
- **PASS:** All four territory scenarios with persistence and authenticated Bridge checks in Quiet and Elevated. Tactical combat passed save/reopen, death cleanup, Breacher contact at a closed door and melee through the reopened entrance. The Pulse regression verified cover, dodging, one-hit behavior, armor, expiry and cleanup.
- **PASS:** The backported and native Creative catalogs and all five configuration starts passed, including legacy import, reopen, canonical precedence and blocked-write handling. The configuration probes asserted the absent sound backend even after explicit playback calls.
- **PASS:** Tekkit Dev lifecycle checks passed for Hopper (9 restart assertions), Distillation (12), 3×3 and 5×5 crafting and hotbar locking. All four lifecycle launches verified the candidate hash and exited gracefully. The isolated JAR, JVM settings and audio options were restored.
- **BLOCKED:** The installed Inventory Tweaks profile supplied no hard-protected crafting slot to the lifecycle fixture. The crafting result for that specific assertion was `BLOCKED`.
- **NOT RUN:** Human combat, naturally generated varied terrain, a powered RedPower entrance, and an assault on a running industrial assembly. The vanilla torch and lever cases cover real redstone behavior on the installed iron door.

### Owner playtest

Generate separate disposable copies with `zto-tools/new-assault-playtest.ps1`. Use an unchanged copy as a control. Start with Hard and compare wooden, closed iron and switch-controlled entrances. Make the approach visible to hostiles around a corner, leave an interior retreat lane, and compare a bare shelter with a workshop or defensible compound. Check stocked machines and inventory after the encounter. Compare a stationary target with movement, target prioritization and retreat. Try leather and a charged QuantumSuit.

For role recognition, enable Enderkin in a separate copy, leave territorial adaptation independent, and play across natural dusk on several cycles. Use the read-only Bridge after play to see which roles were selected, deferred, inserted or rejected. Break line of sight during the Skirmisher windup and try baiting the Breacher against a protected entrance. Note whether silhouettes and behavior communicate the roles in darkness when ordinary mobs overlap. Compare a fresh quiet Plains cell with a settled high-activity cell or exposed Ordinary terrain. Use observed opening pressure, viable retreats and whether cover helps to judge profiles. The passive melee-heavy damage rate alone cannot establish which profile is more tactically demanding.

Next, calibrate live cover and disengagement across real terrain, then tune role feedback only if recognition remains weak. Evaluate individual [future role prototypes](enderkin-future-roles.md) separately once this baseline has owner playtest evidence.
