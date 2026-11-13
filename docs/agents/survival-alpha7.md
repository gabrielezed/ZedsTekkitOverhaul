# Survival 0.1.0-alpha.7 — Tactical hostile prototypes

Both entities are **prototypes under review**. Their save names are `ZTO.Skirmisher` and `ZTO.Breacher`. ModLoader allocates the numeric registry slots at startup, and vanilla NBT uses the names for loading. Keep the names stable while disposable saves still contain the entities. Neither class registers a spawn list, egg, recipe or lunar allocation. Existing Zombie and Skeleton loadouts, replay protection, budgets and alpha.6 placement continue through the existing Survival pipeline.

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

The Breacher first requests a complete native route to the target. That route can take a nearby detour. When it cannot reach the target, it can follow the partial route toward the obstruction. Within 1.5 blocks of a relevant hit on a lower-body ray, it stops to strike up to three times, with a swing and a local impact sound. Closed vanilla doors and gates, fences and opaque cubes are eligible. TileEntity-bearing blocks are excluded from the pressure effect. Pressure leaves block IDs, metadata and tiles intact. If the route opens, the next planning cycle can resume pursuit.

Both mobs use vanilla movement, collision, swimming, damage, death, despawn and Peaceful removal. Target, path, pose, counters and cooldowns are transient. Vanilla NBT carries position, health and normal living state. The path planner checks every chunk in the native 24-block cache radius before constructing it. Each visibility ray is bounded by the target range and checks the intervening chunk rectangle. The fixtures exercise local singleplayer authority. Dedicated-server, remote spawn transport and animation synchronization remain unverified.

## Visuals and access

The original temporary 64×32 skins use the legacy biped atlas. The green, narrow Skirmisher carries a vanilla bow and raises it during the windup. The rust-colored Breacher uses a broader biped silhouette and a swing for both melee and pressure. Native walking, orientation, lightmap and model-view state come from the pack entity renderer. Industrial geometry keeps its [existing OBJ/TESR approach](codechicken-rendering-audit.md).

The game-thread runtime probe is the developer spawn mechanism. It uses a marked generated disposable world in the isolated Tekkit Dev copy and resolves each entity through the production registry. It stays outside the production JAR. Build with `zto-tools/build-and-deploy.bat`, pointing `ZTO_PRISM_MODS` at the copy's `minecraft/mods` directory, and run:

```powershell
.\zto-tools\test-alpha7-runtime.ps1
.\zto-tools\test-alpha7-runtime.ps1 -Mode stress
```

The probe uses the same ignored `dist-zto/survival-hud-runtime/launch.json`, pack runtime and natives as the alpha.6 checks. Each invocation makes a new disposable save. Reports and screenshots remain in ignored local paths.

## Evaluation

| Area | Skirmisher | Breacher |
| --- | --- | --- |
| Tactical identity | Ranged spacing with lateral movement and retreat | Melee approach that explicitly reacts to a blocked route |
| Readability | Bow windup and projectile give the player cues | A swing and localized impact indicate the obstruction |
| Counterplay | Cover, pushing inside its firing range, ranged return fire and retreat | Closed barriers, alternate access control, terrain and direct combat |
| Legacy complexity | Bounded waypoint probes and native projectiles | Distinguishes complete and partial paths and checks actual ray hits |
| Render complexity | One biped and native held-item pass | One widened biped |
| Lunar suitability | Candidate for a rare composition slot after playtest and safe placement verification | Requires more terrain and alternate-route evidence before allocating a slot |
| Future site sieges | Potential ranged support | Potential approach pressure using a future site objective |

The prototypes test the project vision's emphasis on positioning, meaningful construction and readable difficulty. Existing player-vicinity targeting, outdoor placement, loadouts and future site recognition remain independent integration points. Keep both developer-only until the owner evaluates counterplay and a bounded placement policy. Prioritize testing pressure readability and path selection across real builds before redesigning either mob.
