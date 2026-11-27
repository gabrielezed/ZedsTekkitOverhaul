# Abyssal Custodian

Survival 0.5.1-alpha.4 places an aquatic Enderman relative in the Sunken Observatory's deep chamber. Players can clear the galleries before entering, use masonry as cover, fight with ordinary or advanced equipment, and retreat for oxygen or supplies. Learning the creature's silhouette and cues creates an exploration advantage. The encounter uses the existing Observatory identity, terrain and finite population, leaving future regional progression independent of this local reward.

## Entity, model and resources

`ZTO.AbyssalCustodian` is a registered legacy living entity with the native hostile marker, no natural spawn table or egg, and a stable 1.20 × 2.55 block body box. It breathes in water and retains native health, armor and damage attribution. It uses the existing loaded-water swim sweeps and bounded route search. Idle movement thrust is 0.09 blocks/tick, cruising 0.13, and phase-two repositioning 0.17 before drag. The controller bounds swimming by the chamber, permits pursuit at the entrances, and returns toward the center on retreat. The original construction remains mutable.

The pelvis anchors four tentacles of three linked cuboids each. The torso, neck, head and two side gills articulate above it. Both shoulders carry upper arms, forearms and hands, with separate axial deformation per segment. The pose varies from about 2 blocks high contracted to about 3.9 blocks at full vertical extension. Neutral height is about 2.8 blocks, head width 0.875 blocks and torso width 0.75 blocks. The server checks clearance for the high pose and sends its result along with the combat pose, age, strike side, orientation and wall-limited reach. The eyes have a separate additive pass with native body illumination.

The 128 × 64 atlases use these cuboid UV origins. Coordinates are in pixels. The body atlas and eye atlas share the head UVs. Mono PCM cues cover idle movement, damage, death, awakening, both parts of each attack, Riftstep arrival and the phase transition.

| Part | U, V |
| --- | --- |
| Head and gills | 0,0 · 56,0 · 70,0 |
| Neck, torso and pelvis | 84,0 · 0,24 · 42,25 |
| Upper arm, forearm and hand | 80,18 · 96,18 · 110,18 |
| Tentacle root, joint and tip | 0,47 · 18,47 · 32,47 |

- Editable atlases in `artwork/source/abyssal/custodian*.png`. Runtime copies in `src/minecraft/zed/textures/abyssal/`.
- Sound family in `src/minecraft/zed/sounds/custodian/`. Regenerate these original assets with `python artwork/tools/custodian_assets.py`, then export and validate with the artwork tools.
- After MCP compilation, run `python artwork/tools/custodian_previews.py` for `artwork/previews/custodian/poses.png` and `scale.png`. The recorder runs the actual model, its native cuboids and UVs without opening a graphical client. The scale sheet uses the local MCP native models and textures.

## Combat

The creature starts at 80 HP. A vulnerable diver within 17 blocks who enters the chamber territory can trigger a 24-tick awakening. Phase two starts once at 40 HP or below after the active attack cycle. It has a 28-tick transformation, shorter Riftstep cooldown and more frequent repositioning. Damage values and recovery windows carry across both phases.

| Attack | Wind-up / active / recovery | Effect |
| --- | --- | --- |
| Elongated Strike | 22 / 6 / 34 ticks | Alternates arms. Locks aim six ticks before release. A 0.17-block half-width sweep from the selected shoulder stops at native block collision and attempts one hit on the nearest intersected diver. Maximum visible reach is 4.85 blocks and Normal damage is 5 HP. |
| Abyssal Repulsion | 34 / 1 / 42 ticks | A single water shock within 6 blocks applies 2 HP on Normal and a distance-scaled radial impulse up to 0.46 blocks/tick. Solid cover interrupts the effect. Independent cooldown is 108 ticks after recovery. |
| Riftstep | 18 / 1 / 22 ticks | Samples up to 16 chamber destinations 4–9 blocks away with vertical variation and a 3-block target margin. Rechecks water, loaded chunks, body and animation clearance, and occupancy before moving the same entity. Cooldown is 132 ticks in phase one and 92 in phase two after recovery. |
| Enderic Resonance | 26 / 1 / 32 ticks | Signals at most two living, loaded, same-site Enderkin for 48 ticks. Eligible actors approach using their existing swimming, pursuit and casting constraints. Independent cooldown is 220 ticks after recovery. Revision-one fish cannot respond. |

Context and cooldowns affect attack choice. The previous selection receives lower weight. The player can break line of sight, move out of the locked strike, use vertical space or retreat. The Custodian applies no oxygen cost, forced player displacement through blocks, arena lock, healing, replacement minions or boss bar.

## Admission and persistence

Both existing Observatory layout revisions are eligible once their persisted footprint is complete, loaded, stamped and populated. Admission checks hostile difficulty, the nearby diver and native water clearance. The full-body bounds come from that layout's chamber. Modifications are never repaired. Failed geometry can defer initial admission. This policy uses the existing Anvil provider and site stamp hooks.

A versioned file adjacent to the site record binds its site identity to a generated encounter UUID. A synced atomic replacement reserves it before native insertion. Native NBT owns actor position, health and phase. On load, site ownership and UUID must match the record. The lowest native entity ID among nearby copies of the same authorized actor is canonical. Peaceful suspends the fight and preserves the actor. Retreat preserves health and resets the current attack. Terminal defeat consumes the local reward before a normal Ender Pearl item enters the native drop list. [Reward and crash semantics](ender-pearl-scarcity.md)

The entity is written by the ordinary chunk saver. I/O failures fail closed. Loss of the actor after a durable reservation, including interrupted insertion or an unsaved chunk, can leave the encounter reserved without a living Custodian. It cannot be recreated automatically with fresh health or a fresh reward.

## Owner walkthrough

Build and install the matching local-dev pair. Generate a prepared save with `zto-tools/test-zto-agent.ps1 -Focus custodian -CustodianOwnerSave -PackJava java`. Extract the resulting zip's **ZTO Custodian playtest** folder into Tekkit Dev's `minecraft/saves`. The prepared seed-32021 save spawns at a surface platform with diving equipment and four charged tanks. It contains the real Observatory and a bound full-health Custodian. The entrance anchor is X −1170, floor Y 22, Z 765. The chamber fixture admitted the boss at X −1171.5, Y 24.08, Z 810.5.

1. Equip the mask, tank and flippers from the platform chest. Enter the galleries on Normal. Observe surviving Enderkin and locate the central chamber.
2. Look for the dormant silhouette, gills, four articulated tentacles and awakening. Watch a strike from either side, sidestep a committed reach and use the masonry against repulsion. Look and listen for Riftstep preparation and arrival.
3. Fight past half health and inspect the transformation. Retreat and return, then save and reopen during the fight to compare HP. Finish the fight, collect one pearl, and save and reopen to verify completion.
4. With `-Dzto.dev=true`, open **Numpad 1 → Biome Ecology**. **Observatory** shows site and reward state plus the nearest actor's health and pose when in range. In a marked local save, select **Abyssal Custodian** in the species selector and use **Spawn in fixture** while facing clear submerged space. The manual creature is temporary and provides no progression reward.

Additional generated entrances verified by the native runner are X 3368, Y 31, Z −4268 and X −1802, Y 24, Z 1884 on seed −32021. New native verification uses `-Focus custodian` and the existing full Observatory regression uses `-Focus observatory`. The probe exercises native structures, attacks, reload, revision-one admission, duplicate reconciliation, Peaceful and reward behavior. Graphical and audio acceptance remain owner playtest items, including low-light texture readability, attack cue timing, geometry in irregular chambers and alpha.3's outstanding ceiling, lighting and route readability refinements.
