# Survival 0.2.0-alpha.3

The optional territorial experiment now freezes the vanilla spawn order of each natural full-moon PLAYER_ASSAULT wave at admission. Territory remains the source of the local assessment. The director retains one immutable assessment and owns the encounter. The eight-day calendar, budgets, placement, retry limits, stats and loot retain their existing rules. Future site encounters can use an independent target and composition policy.

## Tactical design

Alpha.2 repeated zombie, zombie, skeleton on Easy and Normal in waves 1 and 2, then skeleton, zombie in wave 3. Hard used skeletons because the 1.2.5 zombie includes a wooden-door task that removes a door after a sustained attempt on difficulty 3. Lunar patrols retain their own phase, composition and placement rules.

Profile-guided equipment could also vary Hard combat, but it would shift the odds of a stone-sword hit or extra protection and would depend on the gear roll. The vanilla slot order makes the opening predictable at admission and gives the ranged and melee roles different weight. Hard keeps the same species safeguard and expresses territory through the alpha.2 wave allocation.

| Profile | Threat | Default waves | Easy / Normal vanilla slots |
| --- | --- | --- | --- |
| Disabled or assessment unavailable | Switch off or no usable assessment | 4 / 5 / 6 | `ZZSZ / ZZSZZ / SZSZSZ` |
| Quiet | Below 16 | 2 / 6 / 6 | `ZZ / ZZZSZZ / ZZZSZZ` |
| Ordinary | 16–23 | 4 / 5 / 6 | `ZZSZ / ZZSZZ / SZSZSZ` |
| Elevated | 24 or more | 6 / 5 / 4 | `SZSZSZ / ZZSZZ / SZSZ` |

`Z` is zombie and `S` is skeleton. Quiet repeats `ZZZS` in every wave. Elevated repeats `SZ` in wave 1 and uses the legacy order for waves 2 and 3. Each pattern is truncated to that wave's allocation for configured budgets 1 through 24. Hard uses `S` in all slots. Quiet retains the existing one-position reduction to a minimum of one. At low budgets a wave can have no slots, or profiles can share a prefix.

Quiet gives the player time to draw melee enemies away from the shelter or intercept them at a choke point. Elevated introduces arrows early while zombies approach, increasing the value of cover and target choice. Ordinary preserves the established encounter. The thresholds retain alpha.2 calibration. The experiment remains disabled by default.

## Selection and limits

An unsuccessful candidate leaves the same slot pending. A successful insertion advances the wave's cursor. A carry continues the receiving wave's pattern. Composition uses no RNG. Placement still samples the loaded outdoor annulus and checks sky exposure, ground, clearance, light and approaches. Placement and loadout assignment use separate random streams. Runtime fixtures can pin the placement sampler for repeatable candidate positions while vanilla combat and equipment still vary. The default active assault cap is eight and the nearby hostile cap is 28. A full cap defers spawning within the existing retry window.

Each inserted ordinary mob receives its existing loadout opportunity. The wave chances are 20%, 35% and 50%, subject to caps of 1 / 2 / 2 and five equipped mobs overall. Stone swords, leather chests and chain chests keep their existing damage, protection, rendering, NBT and no-extra-drop assignment. The mob type determines which equipment profile can be used.

Enderkin selection keeps its independent world-seed, logical-day, lunar-phase and wave roll. An eligible Skirmisher in wave 2 or Breacher in wave 3 can replace the first slot. Four rejected candidates exhaust the opportunity and leave the slot available. An inserted Enderkin spends one slot and skips ordinary equipment assignment.

Each wave in `assault.snapshot` exposes `composition` and ordered `vanillaSlots`. The first `planned` entries are the admission plan. An appended position represents an allocated carry. Existing `spawns` records actual insertions by wave and tick, including Enderkin and selected loadout. Wave rejections, caps, active counts and peaks describe where the realized sequence diverges. The new fields extend the existing read-only v1 query. Territory remains on WorldSavedData schema 1. The composition is session-local and needs no save migration.

## Verification

Use the [test map](testing.md) with the reobfuscated JAR in an isolated Tekkit Dev staging copy. The pressure fixture leaves every hostile alive unless ordinary simulation removes it. It records active population, waves with simultaneous live mobs, the first observed player health loss and gaps between losses. Health loss records damage and can miss attacks that hit cover or deal zero damage. The passive leather fixture counts safety heals and recentering when knockback moves the player toward the edge of the arena. The fixture accelerates the legacy timer and reports simulation ticks.

The tested production commit is `51b34d27f22f38532e18152ecc617b95ef30eaad` on baseline `564b60d0cc781edcfcee2f61a8e811848254e810`. The archive is version `0.2.0-alpha.3` with SHA-256 `c0ca2c62381e8e25897a27cb7cf11bbf86846bb1d69ec70239d2df4ddff49356`. The build sidecar records a dirty checkout containing runtime-test changes and existing untracked local notes. The packaged source and resources match the production commit. MCP 6.2 compiled and reobfuscated the client with the Forge 3.4.9.171 development sources and JDK 8.0.504.1. Streaming verification passed for 306 archive entries. Tekkit Dev reported Forge 3.3.8.164, FML 2.2.106.176 and Java 1.8.0_51.

Portable tests passed across all profiles, difficulties and budgets 1 through 24. They cover legacy and unavailable fallback, rejected candidates, seeded substitution and equipment caps. Configuration migration, lunar and patrol scheduling, outdoor approach rules, the 10,000-seed Enderkin sweep and Dev Bridge transport passed. The portable Hopper, crafting, distillation, EMC, conversion and Foundry checks also passed.

### Concurrent pressure in Tekkit Dev

Each run used world seed `-7184827518270584220`, a placement sampler seeded to 6001 and natural dusk at raw tick 205000. The lit arena sat in baseline terrain. The configured budget was 15. Bridge v1 matched the game-thread frozen threat, allocation, slot order, insertion order and terminal state. A receiving wave may gain a slot when the previous wave carries it over. The clients started with music and sound at zero and asserted an uninitialized SoundSystem. They saved and exited before the next run.

| Fixture | Frozen threat (environment + activity) | Planned counts | Actually inserted by wave | Peak active / overlapping waves | First damage / median gap (ticks) | Safety heals |
| --- | --- | --- | --- | --- | --- | --- |
| Disabled, Normal | — | 4 / 5 / 6 | `ZZSZ / ZZSZ / S` | 8 / 3 | 206 / 21 | 35 |
| Quiet, Normal | 12 (12 + 0) | 2 / 6 / 6 | `ZZ / ZZZSZZ / —` | 8 / 2 | 206 / 20 | 42 |
| Ordinary, Normal | 18 (12 + 6) | 4 / 5 / 6 | `ZZSZ / ZZSZ / —` | 8 / 2 | 206 / 20 | 28 |
| Elevated, Normal | 25 (12 + 13) | 6 / 5 / 4 | `SZSZSZ / ZZS / —` | 8 / 2 | 219 / 20 | 40 |
| Elevated + Enderkin, Normal | 25 (12 + 13) | 6 / 5 / 4 | `SZSZSZ / KZSZ / B` | 8 / 3 | 131 / 21 | 36 |
| Elevated, Hard shelter | 25 (12 + 13) | 6 / 5 / 4 | `SSSSSS / SS / —` | 8 / 2 | — | 0 |
| Elevated, Normal with IC2 QuantumSuit | 25 (12 + 13) | 6 / 5 / 4 | `SZSZSZ / ZZS / SZ` | 8 / 3 | — | 0 |

The first four runs reached `ENCOUNTER_TIMEOUT` at elapsed tick 3080. The active cap blocked 23, 14, 24 and 25 retries respectively. Sky-light rejections were 11, 5, 5 and 12. One candidate in each of the disabled, Quiet and Ordinary runs failed sky exposure or approach checks. Successful loadout allocations by wave were 1 / 1 / 1, 0 / 1 / 0, 0 / 2 / 0 and 1 / 2 / 0. The Quiet plan stayed frozen as the live score rose to 25. Its melee mix landed more hits on the passive leather player than the other three runs. Actual openings differed in mob order, and first damage timing varied with pathing and line of sight.

In the mixed run, `K` is the Skirmisher and `B` is the Breacher. Each replaced its first vanilla slot. The mixed fixture reseeded the placement sampler to 3 and recentered before wave 2 to exercise an eligible candidate after the first attempt at this fixture missed the Skirmisher. The role roll, population rules and combat followed the production paths. The active cap blocked 23 retries and the loadout counts were 1 / 2 / 0.

The Hard shelter kept both halves of its closed wooden door and the original RedPower machine TileEntity through timeout. The player stayed at 20 health. Placement admitted only skeletons, one of which received chain armor. The active cap blocked 26 retries and ten other candidates failed sky-light rolls.

The installed IC2 QuantumSuit used 11,400 EU while the player stayed at 20 health. The active cap blocked 23 retries. The remaining rejections comprised 12 sky-light rolls and one failed approach check. One skeleton received chain armor. Both shelter and QuantumSuit runs resolved by timeout at tick 3080, as did the mixed run.

The per-wave equipment profiles, rejection codes and spawn times are in the local `dist-zto/test-reports/alpha3-http-ZTO-Test-*.json` files. The corresponding `alpha6-composition-*.txt` logs retain damage sample counts, gap ranges, health and recentering totals.

### Pack regressions

The natural recurrence probe passed repeated full-moon admissions, interruption by save or dimension switch, same-night replay suppression, time-skip behavior and Peaceful and Creative eligibility. The outdoor, multi-wave, quiet-night and disabled-placement probes passed, including roof, water, courtyard and entrance checks. The four switch combinations passed across the pressure probes and `ender-waves`, which also exercised the installed Breacher and protected barrier matrix. The fallback probe passed after four special candidates were rejected. The hostile cap probe passed when the local population saturated. Tactical combat passed, including persistence after reopening and removal after death.

Both Creative catalogs passed. All five configuration startup cases passed, including fresh defaults, legacy import, restart, canonical precedence and a blocked config write. Music and sound stayed at zero and the SoundSystem remained uninitialized after explicit playback calls.

All four territory modes passed. The legacy probe first reached its assertions and timed out during shutdown. It passed with graceful exit on retry. The combined territory probe passed after fixing the fixture placement seed. Its initial unseeded run exhausted the Breacher's four attempts on sky-light rolls. Authenticated HTTP matched Quiet and Elevated admission, controlled insertion, Overworld return and native reopen. The Elevated probe confirmed decay, repeat-visit credit, an exposed score of 16 and a swamp score of 18 on the fixture seed.

The isolated Prism lifecycle runner checked the candidate hash at startup. Hopper passed all nine restart checks and Distillation passed all twelve. Both lifecycles passed 3×3 and 5×5 crafting and hotbar locking. The hard-protection check was **BLOCKED** by the installed Inventory Tweaks profile. All four clients exited gracefully. The original isolated JAR and JVM arguments were restored byte for byte, and the lifecycle runner restored the instance audio options. The local reports are `autonomy-ZTO-Test-dfd39bc7551a41248526ed6cb65c9458.json` and `autonomy-ZTO-Test-378fed3a4b99435f8f45680f8cc53a2d.json`.

## Owner playtest

Create independent copies with the [assault playtest generator](../../zto-tools/new-assault-playtest.ps1) in the isolated instance. Compare opt-in and default configuration at natural dusk. Use the read-only Bridge to confirm the frozen profile. For Quiet, enter a fresh Plains cell away from accumulated activity shortly before dusk. For Elevated, dwell near the center of the same Plains cell for several minutes before the full moon. For Ordinary in exposed terrain, try fresh Desert or Extreme Hills where the blended environment is at least 16. The fixture seed has an exposed sample near x = -1024, z = -1024.

Try a lit 7×7 shelter with a closed wooden door, then use only terrain for cover. For the working workshop trial, place a Hopper feeding a chest inside the shelter and leave open ground 12–16 blocks out. Copy the save before dusk and verify the machine and inventory after each trial. Repeat in a charged IC2 QuantumSuit. Enable Enderkin in a separate copy and repeat across full moons so the seeded opportunities can vary.

Combat in naturally exposed terrain, combat at a working workshop and human playtesting are **NOT RUN**. Judge whether the opening is readable without diagnostics, whether cover and retreat stay practical, and how the same encounter feels in basic and late-game armor. Alpha.4 should calibrate the opening and assess restrained in-world feedback. Patrol adaptation needs separate gameplay evidence.
