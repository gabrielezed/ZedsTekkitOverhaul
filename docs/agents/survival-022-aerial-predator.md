# Survival 0.2.2-alpha.1 aerial predator

This prototype implements Phase A of the local Survival 0.2.2 design draft. The working designation is Aerial Predator. A compact rust-colored head, hooked amber bill, stepped teal wings and forked tail distinguish it in silhouette. The visual design and in-game tuning await owner review.

## Behavior

The predator is a legacy flying hostile entity with standard health, collision, damage and despawn behavior. It hunts in the Overworld on Easy, Normal and Hard. Wildlife alternates between powered flight and glides above a local terrain estimate. Steering changes heading gradually and tests short corridors against loaded collision geometry and liquids. When a corridor closes, the predator brakes, climbs or turns to look for another route.

- Exposed airborne players at least eight blocks above the local height map are favored within 36 blocks. Exposed near-ground players use a smaller radius and a rarer acquisition roll, especially in daylight. Creative invulnerability, sleeping players and overhead cover prevent acquisition.
- The predator circles before an 18-tick windup with raised wings, an open bill and a high-pitched call. On commitment it leads the observed target by a limited amount and accelerates down a fixed vector. The wings fold and talons extend.
- Damage requires contact with the target's expanded hitbox along the actual displacement and a clear path at impact. A lateral dodge or terrain can break the dive. Damage on Normal is six health points before protection. Contact adds a small horizontal impulse and preserves vertical speed.
- Recovery climbs for up to 50 ticks. A ground encounter allows one attempt and an airborne encounter allows two. Shelter, sustained loss of sight, excessive distance or the hunt time limit end the pursuit. Disengagement moves the predator away before further target scans.

## Placement, saves and integration

Use **Aerial Predator Spawn Egg** in Creative or NEI. Its configurable constructor ID is 22009 and its default stack ID is 22265. The egg uses the shared survival egg placement path with species clearance checks. The five existing Enderkin eggs keep their names, IDs and placement rules. A predator saves under `ZTO.AerialPredator`. Reload restores its normal entity data and returns it to cruise with a short acquisition delay. Older worlds need no migration.

Use **Numpad 1 → Aerial Predator** for the nearest creature within 48 blocks. The non-pausing view reads its current phase, target, terrain clearance, steering destination, timer and last failure or disengagement reason. It refreshes twice a second.

Ambient admission remains pending the flight and collision playtest. The entity owns movement and combat independently of how it is admitted. Its placement checks and height-above-terrain query can support a future encounter director with its own budgets. Sustained exposure, flocks, lunar admission and IC2 weather faults remain later work. Giving the player a visible window to evade, descend, use cover or fight preserves the charter's intermittent danger and freedom of travel.

## Verification

- Run `zto-tools/test-zto-agent.ps1 -Pack` after building. The aerial probe uses the reobfuscated JAR and installed Tekkit Forge classes in a marked disposable world. It checks native entity registration and egg use, renderer construction and atlas decoding, launch, headless flight, timing, damage, dodge, climb, shelter, cliff and canopy avoidance, falling on death, and named NBT reload. The same command runs the existing compatibility and Survival 0.2.1 headless checks.
- Use the approved separate OS input session or an owner-operated isolated Tekkit Dev instance for the graphical checklist. The Windows host blocks automated graphical clients. [Test tiers and input isolation](testing.md#background-client-launch)

### Owner playtest

1. In a disposable Overworld save on Normal, search for the egg in Creative. Spawn one from the top of a block in an open field. Watch it leave the ground, turn, flap, and glide. Repeat by a tree canopy and a cliff. Check that wings and textures are readable from the ground.
2. Switch to Survival and use an Electric Jetpack to fly roughly 10–20 blocks above the field. Remain within about 25 blocks of the predator. Watch for circling, the audible windup, and the tucked-wing dive. Hold a steady course on one pass and turn sideways after commitment on another. Compare damage, knockback and recovery. Test bow and melee damage when a pass brings the predator within reach.
3. Descend under a solid roof during a hunt and use the Debug Hub to confirm disengagement. Repeat at ground level in daylight and at night. Verify that a ground hunt spends only one attempt. Save during a hunt and reopen the world, then check the creature returns to cruise.
4. Repeat Creative catalog lookup with the inventory backport disabled. Observe death in the air and report any visible clipping, unreadable silhouettes, abrupt animation transitions or repeated impacts with terrain.
