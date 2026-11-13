# Survival 0.1.0-alpha.7 — Tactical hostile prototypes

This is the alpha.7 baseline. See [alpha.8](survival-alpha8.md) for opt-in lunar placement.

Both entities are **prototypes under review**. Their save names are `ZTO.Skirmisher` and `ZTO.Breacher`. ModLoader allocates the numeric entity registry slots at startup, and vanilla NBT uses the names for loading. Their eggs are available through Creative. Both prototypes reject natural spawn checks and have no biome list, recipe or lunar allocation. Existing Zombie and Skeleton loadouts, replay protection, budgets and alpha.6 placement continue through the existing Survival pipeline.

## Experimental tuning

| | Skirmisher | Breacher |
| --- | --- | --- |
| Health | 16 HP | 22 HP |
| Collision width × height | 0.60 × 1.70 blocks | 0.85 × 1.90 blocks |
| Native AI move speed | 0.25 | 0.21 |
| Target range | 16 blocks | 16 blocks |
| Combat | 5–12 blocks, 12-tick aim, 50-tick cooldown after firing | Under 2 blocks, vertical overlap and visibility, 3 HP before mitigation, 30-tick cooldown |
| Projectile | Ender Pulse, 0.82 blocks/tick, 2 HP base damage through the native thrown-projectile source, up to 18 movement steps | — |
| Replanning | One query every 20 ticks, four consecutive failed plans before backing off | One query every 24 ticks, 192 ticks without progress before backing off |
| Target loss | 100 unseen ticks | 216 unseen ticks |
| Reacquisition delay after giving up | 100 ticks | 100 ticks |

The Skirmisher approaches from outside firing range, sidesteps around a visible target between shots and tries to move outward when the target closes inside five blocks. It alternates the side of each movement attempt and checks for a path that reaches the intended endpoint. Cover and close pressure interrupt the cast. The twelve-tick windup and fifty-tick post-shot cooldown remain in effect.

### Ender Pulse

The cast starts about one block forward at hand height. A native world ray between the caster and the launch point stops the discharge at any intervening panel. Each movement step traces terrain and checks nearby living collision boxes swept along the same segment. The closest contact wins, with terrain winning ties. The visual arcs extend outside the centerline terrain ray. Living hit boxes get a 0.10-block margin. The caster is excluded even if the pulse starts inside its hull.

The trajectory is set once at release and covers at most 14.76 blocks. The previous arrow used the native ceiling of impact speed times 1.5, giving around 2–3 HP near its starting speed of 1.35 blocks/tick before mitigation. The pulse uses 2 HP base damage. Legacy difficulty scaling and equipment mitigation still apply. The attack has a single direct hit, four impact particles and a quiet native fizz at release.

ModLoader assigns a free numeric registry ID to `ZTO.EnderPulse`. Rendering uses the existing entity renderer callback. Forge's chunk update hook allows just this entity to tick when the wider vanilla update neighborhood is incomplete. Its own check retires it before it enters an unloaded chunk. It expires on tick 19 and is dropped in Peaceful. A fired pulse retains its owner reference through the end of flight, even if the owner dies. It has no dependency on the original target once launched. In-flight pulses opt out of chunk and save serialization. They disappear on unload or when the world is closed. Remote entity spawning and pose transport remain outside this local singleplayer implementation.

The Breacher first requests a complete native route to the target. That route can take a nearby detour. When it cannot reach the target, it can follow the partial route toward the obstruction. Within 1.5 blocks of a relevant hit on a lower-body ray, it stops to strike up to three times, with a swing and a local impact sound. Closed vanilla doors and gates, fences and opaque cubes are eligible. TileEntity-bearing blocks are excluded from the pressure effect. Pressure leaves block IDs, metadata and tiles intact. If the route opens, the next planning cycle can resume pursuit. The runtime probe found that a closed door can stop collision even when the path starts inside the door's grid cell and returns a complete route. The Breacher checks for obstruction after two route intervals without progress even when the planner reports a complete path.

Both mobs use vanilla movement, collision, swimming, damage, death, despawn and Peaceful removal. Target, path, pose, counters and cooldowns are transient. Vanilla NBT carries position, health and normal living state. The path planner checks every chunk in the native 24-block cache radius before constructing it. Each visibility ray is bounded by the target range and checks the intervening chunk rectangle. The fixtures exercise local singleplayer authority. Dedicated-server, remote spawn transport and animation synchronization remain unverified.

## Visuals and access

The [Enderkin redesign](enderkin-visual-design.md) gives each prototype its own cuboid model, skin and attack poses. The Skirmisher gathers and releases the pulse with both hands. Industrial geometry keeps its [existing OBJ/TESR approach](codechicken-rendering-audit.md).

Open the Creative inventory and find **Skirmisher Spawn Egg** and **Breacher Spawn Egg** in **Items** or **ZedsTekkitOverhaul**. The compass search matches either creature name. Move each egg to the hotbar and right-click the top of a block with enough headroom. Switch to Survival in the disposable test world to provoke the Skirmisher, whose existing targeting requires a damageable player. In the native **1.2.5** screen, scroll to the modded items or use NEI search. Set `creativeInventoryBackport=false` in the isolated instance's `config/mod_ZedsTekkitOverhaul.cfg` and restart to exercise the native catalog from startup. Save and Quit, then reopen the world to verify persistence.

The installed Forge catalog calls `Item.addCreativeItems` and separately enumerates `EntityList.entityEggs`. Native egg metadata is a numeric entity ID, which can vary with the mod environment and follows a spawn path without a solid-volume check. Two ordinary ZTO items reuse the native shell and spots render passes and resolve stable names on use. The classic container and NEI discover them as ordinary registered items. The backport imports the classic catalog and deduplicates matching stacks.

The defaults in `config/ZedsTekkitOverhaul-Tactical.cfg` are constructor IDs 22004 and 22005, respectively, giving stack IDs **22260** and **22261**, metadata 0. Registration checks both slots after the normal mod load phase. A conflicting ID fails with the relevant config path. Keep configured IDs stable once stacks are saved. The Skirmisher shell and spots use `#17171C` and `#535170`. The Breacher uses `#1C171B` and `#785469`. The item checks edit permissions, height, loaded placement, collision and Peaceful, and consumes one egg on successful non-Creative use. It allows manual placement in the active local dimension.

The game-thread runtime probe uses marked disposable saves in the isolated Tekkit Dev copy. Build with `zto-tools/build-and-deploy.bat`, point `ZTO_PRISM_MODS` at the copy's `minecraft/mods` directory, and run:

```powershell
.\zto-tools\test-alpha7-runtime.ps1
.\zto-tools\test-alpha7-runtime.ps1 -Mode pulse
.\zto-tools\test-alpha7-runtime.ps1 -Mode stress
.\zto-tools\test-alpha7-runtime.ps1 -Mode visual
.\zto-tools\test-alpha7-runtime.ps1 -Mode creative
.\zto-tools\test-alpha7-runtime.ps1 -Mode creative-native
```

The probe uses the same ignored `dist-zto/survival-hud-runtime/launch.json`, pack runtime and natives as the alpha.6 checks. Each invocation makes a new disposable save. Reports and screenshots remain in ignored local paths.

## Ender Pulse acceptance

- The production source matches `be52b0877933b4a0d8005d1820d747e829ce0463`. The test archive was built from the changes against `90d532998e3dbb5d52d63e01fcd731a67ef6e211`. Its SHA-256 is `69a70e138082b12d3145d66d665e8d3950b91f784b24fdca746810796d4a0d78`. MCP client compilation, reobfuscation, the 286-entry streaming check and the class namespace audit passed. The local build retains the instance's optional Creative and Hopper assets.
- The isolated Tekkit Dev client ran Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176 and Java 8u504 with diagnostics disabled. The dedicated pulse mode observed a 2 HP Normal hit without armor and 0 HP immediate loss on the first hit with full diamond armor. The pulse survived its caster's death, ignored a dead target, stopped at stone and a closed door, missed after a sidestep, and retired on expiry, on entering Peaceful or before crossing into an unloaded chunk. The boundary probe drove the normal entity update against the installed pack without loading the destination chunk. Save and Quit restored no in-flight shots.
- Combat observed two natural pulses with the expected spacing, no Skirmisher arrows, retreat and cancelled casting. The Breacher still applied three pressure events, traversed the reopened door and landed melee. Visual and 12-entity stress passed, including simultaneous casts and the existing Creative modes with NEI, hotbar transfers, placement and Save/Quit. [In-game captures](enderkin-visual-design.md#ender-pulse-casting) show the charge, release, recovery, daylight, torchlight and low light.
- The same archive passed all six alpha.6 modes (`first`, `gibbous`, `full`, `waves`, `quiet` and `disabled`) and the portable calendar, schedule and outdoor approach checks. The completed assault inserted 4/5/6 outdoor mobs. The diagnostics-disabled runtime retained replay protection and the disabled lighting exception.
- Switching directly to a second marked disposable world with an active pulse left the new world clear of projectiles. The verified scope is local singleplayer in the isolated pack. Direct dimension transfer, remote spawning and other modded armor sets remain unverified. The native release fizz was confirmed in the installed sound resources. Perceived projectile fairness and sound mix remain owner playtest questions.

## Polish acceptance

- PR #28 started at `aa494013a3ac0075bdcc7884fe1d18f52235b179`. The accepted runtime archive was built at `567ea62b5c693a90d3fd844626cfe4eb21a7d370` with probe changes and the two pre-existing untracked documents in the workspace. Its SHA-256 is `64f46fb46b0e0b47e04de794f06d400e6680469bc69774e4deacf80abc0b8b7f`. MCP 6.2 client compile, Forge 3.4.9.171 reobfuscation and the 283-entry streaming archive check passed. The local test archive includes the instance's pre-existing optional Hopper and Creative assets.
- The isolated Tekkit Classic 3.1.2 client ran Minecraft 1.2.5, Forge 3.3.8.164 and FML 2.2.106.176 on Java 8u504. The local launcher profile used workspace LWJGL and JInput jars with the isolated pack classpath. The bridge was disabled. The known resource download failures and the pre-existing EMC warning for item 169 still appeared.
- Both `creative` and `creative-native` passed. Each run checked one entry per variant, NEI's general and filtered lists, transfer into hotbar slots and two uses per egg. The backport run also exercised its mod category and keyboard search. The native run scrolled the original catalog with the backport disabled at startup. The probes checked correct class, health, position, renderer, unchanged Creative stacks, solid-block and Peaceful rejection, no pre-existing Enderkin, and all four entities after Save and Quit. Dimension transfer and remote server use remain unverified.
- Combat passed with the original timing and spacing for ranged attacks, cover cancellation, exactly three pressure events, intact door, reopened traversal, melee damage and persistence. The first pressure sequence checked that the sound dispatch occurred on the fourth tick. Visual and stress passed with the original light comparisons and twelve entities active across 140 ticks. See the [game frames](enderkin-visual-design.md#polish-in-tekkit-dev).
- All six alpha.6 runtime modes passed on the same archive with the HUD, debug key, verbose lunar logging and bridge disabled. First-quarter and waxing-gibbous patrols inserted two and three mobs. The completed assault inserted 4/5/6 outdoor mobs across its three waves. The suite also covered interrupted and completed Save and Quit, replay protection, quiet new moon and the disabled outdoor-light exception. Portable calendar, assault and patrol schedule, and outdoor approach checks passed.
- A separate archive without the optional local texture groups passed the 274-entry streaming check. Its 240 payload files match the test archive byte for byte, and all 218 classes are under `zed/`. SHA-256 `52c7a294b6e565a6babe6a7a9f158dde8e9cb8c11cabc726d2b6a1e6f4c39b08`.

## Original biped prototype evidence

- Local and remote `main` resolved to `92abefd1b4ef8aaf4c67416be2d5ef4ada2475d0`, the merge of PR #27 after #25 and #26. Baseline product version was alpha.6.
- Candidate source commit `c594a46663f4bc132484c75aaf8419541b9642fd` on `codex/survival-alpha7-tactical-mobs`. The build sidecar reports `sourceDirty=true` because the workspace had two pre-existing untracked documentation files. Packaged SHA-256 `9691843f47d8a84f13eaa046bc86865b42ab398f138c1defc7ef3fd340985647`. MCP 6.2 client compile, reobfuscation and the 277-entry streaming archive check passed. Compiler profile Forge 3.4.9.171 with Java 8. The candidate was deployed into the workspace copy of Tekkit Dev.
- Portable lunar calendar, assault and patrol schedules, outdoor approaches, OBJ parser (13 checks) and lightmap renderer (8 checks) passed. Loadout weights and caps (including 10,000 fixed-seed assaults) and assault diagnostics checks passed against MCP output.
- Actual pack client Java `1.8.0_51`, Minecraft `1.2.5`, Forge `3.3.8.164`, FML `2.2.106.176`, CodeChickenCore `0.5.3`. Dev Bridge and test flags were disabled. The isolated pack also logged old resource URL failures and an EMC initialization warning for unregistered item 169. The tactical fixtures continued.
- Combat save `ZTO-Test-8e34f9472cae4c4da24d153cd2cf2e56` passed. The Skirmisher acquired the player, moved laterally, fired two shots at least 62 ticks apart, retreated, and cancelled its prepared shot after a stone wall was added. The Breacher approached, struck a closed wooden door three times with its metadata intact, passed through the reopened doorway and dealt melee damage. Save and Quit returned to the title and reopening restored exactly two entities with 9 and 13 HP. Normal lethal damage then removed both.
- Stress save `ZTO-Test-bb1f81da64e743028e035b34fffb1932` passed. Six Skirmishers and six Breachers remained active after 140 game ticks. The interval measured 7.466 s wall clock across 139 sampled transitions (approximately 18.6 observed ticks/s), including the whole client and rendering. This is a throughput observation on one host, with no idle baseline or per-operation CPU attribution.
- The probe checked custom renderer lookup, packaged PNGs, upload IDs and GL error status. Captured game frames show the Skirmisher in motion and the Breacher swinging with the original skins loaded. Reopen exercised renderer lookup and texture upload with both restored entities. Full GL state comparison, controlled light levels, and render-distance transitions remain open.
- All six alpha.6 runtime modes passed against the same archive. First-quarter and waxing-gibbous patrols inserted two and three outdoor mobs, respectively. The full-moon runs covered interrupted Save and Quit and a completed assault with 4/5/6 outdoor insertions and 0/2/2 equipped mobs. The suite checked pause/resume, same-night replay, protected placement, quiet new moon and the disabled outdoor-light exception.

The runtime evidence covers the solid cover and vanilla wooden-door fixtures. The longer obstacle matrix remains open, including alternate doors and gates, detours, trenches, elevation, unloaded chunk edges, RedPower barriers and TileEntity preservation checks. Explicit chunk unload/reload, distance despawn, world switching and dimension transitions also remain open. Owner playtesting is needed to assess attack readability, audio, player equipment and encounter balance.

## Evaluation

| Area | Skirmisher | Breacher |
| --- | --- | --- |
| Tactical identity | Ranged spacing with lateral movement and retreat | Melee approach that explicitly reacts to a blocked route |
| Readability | Asymmetric reach and projectile give the player cues | A two-arm strike and localized impact indicate the obstruction |
| Counterplay | Cover, pushing inside its firing range, ranged return fire and retreat | Closed barriers, alternate access control, terrain and direct combat |
| Legacy complexity | Bounded waypoint probes and a finite swept pulse | Distinguishes complete and partial paths and checks actual ray hits |
| Rendering | Native living renderer with narrow articulated cuboids | Native living renderer with broad articulated cuboids |
| Lunar suitability | Candidate for a rare composition slot after playtest and safe placement verification | Requires more terrain and alternate-route evidence before allocating a slot |
| Future site sieges | Potential ranged support | Potential approach pressure using a future site objective |

The prototypes test the project vision's emphasis on positioning, meaningful construction and readable difficulty. Existing player-vicinity targeting, outdoor placement, loadouts and future site recognition remain independent integration points. Creative provides direct access for the owner's evaluation of counterplay and a bounded placement policy.
