# Survival 0.1.0-alpha.7 — Tactical hostile prototypes

Both entities are **prototypes under review**. Their save names are `ZTO.Skirmisher` and `ZTO.Breacher`. ModLoader allocates the numeric entity registry slots at startup, and vanilla NBT uses the names for loading. Their eggs are available through Creative. Both prototypes reject natural spawn checks and have no biome list, recipe or lunar allocation. Existing Zombie and Skeleton loadouts, replay protection, budgets and alpha.6 placement continue through the existing Survival pipeline.

## Experimental tuning

| | Skirmisher | Breacher |
| --- | --- | --- |
| Health | 16 HP | 22 HP |
| Collision width × height | 0.60 × 1.70 blocks | 0.85 × 1.90 blocks |
| Native AI move speed | 0.25 | 0.21 |
| Target range | 16 blocks | 16 blocks |
| Combat | 5–12 blocks, 12-tick aim, 50-tick cooldown after firing | Under 2 blocks, vertical overlap and visibility, 3 HP before mitigation, 30-tick cooldown |
| Projectile | Vanilla hostile arrow, launch speed 1.35, spread 10, damage coefficient 1.5. Impact damage depends on actual velocity and mitigation. | — |
| Replanning | One query every 20 ticks, four consecutive failed plans before backing off | One query every 24 ticks, 192 ticks without progress before backing off |
| Target loss | 100 unseen ticks | 216 unseen ticks |
| Reacquisition delay after giving up | 100 ticks | 100 ticks |

The Skirmisher approaches from outside firing range, sidesteps around a visible target between shots and tries to move outward when the target closes inside five blocks. It alternates the side of each movement attempt and checks for a path that reaches the intended endpoint. Cover and close pressure interrupt the aiming pose. The projectile starts inside the shooter's volume and uses vanilla collision.

The Breacher first requests a complete native route to the target. That route can take a nearby detour. When it cannot reach the target, it can follow the partial route toward the obstruction. Within 1.5 blocks of a relevant hit on a lower-body ray, it stops to strike up to three times, with a swing and a local impact sound. Closed vanilla doors and gates, fences and opaque cubes are eligible. TileEntity-bearing blocks are excluded from the pressure effect. Pressure leaves block IDs, metadata and tiles intact. If the route opens, the next planning cycle can resume pursuit. The runtime probe found that a closed door can stop collision even when the path starts inside the door's grid cell and returns a complete route. The Breacher checks for obstruction after two route intervals without progress even when the planner reports a complete path.

Both mobs use vanilla movement, collision, swimming, damage, death, despawn and Peaceful removal. Target, path, pose, counters and cooldowns are transient. Vanilla NBT carries position, health and normal living state. The path planner checks every chunk in the native 24-block cache radius before constructing it. Each visibility ray is bounded by the target range and checks the intervening chunk rectangle. The fixtures exercise local singleplayer authority. Dedicated-server, remote spawn transport and animation synchronization remain unverified.

## Visuals and access

The [Enderkin redesign](enderkin-visual-design.md) gives each prototype its own cuboid model, skin and attack poses. The Skirmisher's held bow is visually hidden while the existing arrow attack is retained. Industrial geometry keeps its [existing OBJ/TESR approach](codechicken-rendering-audit.md).

Open the Creative inventory and find **Skirmisher Spawn Egg** and **Breacher Spawn Egg** in **Items** or **ZedsTekkitOverhaul**. The compass search matches either creature name. Move each egg to the hotbar and right-click the top of a block with enough headroom. In the native **1.2.5** screen, scroll to the modded items or use NEI search. Set `creativeInventoryBackport=false` in the isolated instance's `config/mod_ZedsTekkitOverhaul.cfg` and restart to exercise the native catalog from startup. Save and Quit, then reopen the world to verify persistence.

The installed Forge catalog calls `Item.addCreativeItems` and separately enumerates `EntityList.entityEggs`. Native egg metadata is a numeric entity ID, which can vary with the mod environment and follows a spawn path without a solid-volume check. Two ordinary ZTO items reuse the native shell and spots render passes and resolve stable names on use. The classic container and NEI discover them as ordinary registered items. The backport imports the classic catalog and deduplicates matching stacks.

The defaults in `config/ZedsTekkitOverhaul-Tactical.cfg` are constructor IDs 22004 and 22005, respectively, giving stack IDs **22260** and **22261**, metadata 0. Registration checks both slots after the normal mod load phase. A conflicting ID fails with the relevant config path. Keep configured IDs stable once stacks are saved. The Skirmisher shell and spots use `#17171C` and `#535170`. The Breacher uses `#1C171B` and `#785469`. The item checks edit permissions, height, loaded placement, collision and Peaceful, and consumes one egg on successful non-Creative use. It allows manual placement in the active local dimension.

The game-thread runtime probe uses marked disposable saves in the isolated Tekkit Dev copy. Build with `zto-tools/build-and-deploy.bat`, point `ZTO_PRISM_MODS` at the copy's `minecraft/mods` directory, and run:

```powershell
.\zto-tools\test-alpha7-runtime.ps1
.\zto-tools\test-alpha7-runtime.ps1 -Mode stress
.\zto-tools\test-alpha7-runtime.ps1 -Mode visual
.\zto-tools\test-alpha7-runtime.ps1 -Mode creative
.\zto-tools\test-alpha7-runtime.ps1 -Mode creative-native
```

The probe uses the same ignored `dist-zto/survival-hud-runtime/launch.json`, pack runtime and natives as the alpha.6 checks. Each invocation makes a new disposable save. Reports and screenshots remain in ignored local paths.

## Polish acceptance

- PR #28 started at `aa494013a3ac0075bdcc7884fe1d18f52235b179`. The accepted runtime archive was built at `567ea62b5c693a90d3fd844626cfe4eb21a7d370` with probe changes and the two pre-existing untracked documents in the workspace. Its SHA-256 is `64f46fb46b0e0b47e04de794f06d400e6680469bc69774e4deacf80abc0b8b7f`. MCP 6.2 client compile, Forge 3.4.9.171 reobfuscation and the 283-entry streaming archive check passed. The local test archive includes the instance's pre-existing optional Hopper and Creative assets.
- The isolated Tekkit Classic 3.1.2 client ran Minecraft 1.2.5, Forge 3.3.8.164 and FML 2.2.106.176 on Java 8u504. The local launcher profile used workspace LWJGL and JInput jars with the isolated pack classpath. The bridge was disabled. The known resource download failures and the pre-existing EMC warning for item 169 still appeared.
- Both `creative` and `creative-native` passed. Each run checked one entry per variant, NEI's general and filtered lists, transfer into hotbar slots and two uses per egg. The backport run also exercised its mod category and keyboard search. The native run scrolled the original catalog with the backport disabled at startup. The probes checked correct class, health, position, renderer, unchanged Creative stacks, solid-block and Peaceful rejection, no pre-existing Enderkin, and all four entities after Save and Quit. Dimension transfer and remote server use remain unverified.
- Combat passed with the original timing and spacing for ranged attacks, cover cancellation, exactly three pressure events, intact door, reopened traversal, melee damage and persistence. The first pressure sequence checked that the sound dispatch occurred on the fourth tick. Visual and stress passed with the original light comparisons and twelve entities active across 140 ticks. See the [game frames](enderkin-visual-design.md#polish-in-tekkit-dev).
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
| Legacy complexity | Bounded waypoint probes and native projectiles | Distinguishes complete and partial paths and checks actual ray hits |
| Rendering | Native living renderer with narrow articulated cuboids | Native living renderer with broad articulated cuboids |
| Lunar suitability | Candidate for a rare composition slot after playtest and safe placement verification | Requires more terrain and alternate-route evidence before allocating a slot |
| Future site sieges | Potential ranged support | Potential approach pressure using a future site objective |

The prototypes test the project vision's emphasis on positioning, meaningful construction and readable difficulty. Existing player-vicinity targeting, outdoor placement, loadouts and future site recognition remain independent integration points. Creative provides direct access for the owner's evaluation of counterplay and a bounded placement policy.
