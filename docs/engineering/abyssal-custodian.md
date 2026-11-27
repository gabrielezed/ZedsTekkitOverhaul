# Abyssal Custodian

Survival 0.5.1-alpha.4 places an aquatic Enderman relative in the Sunken Observatory's deep chamber. Players can clear the galleries before entering, use masonry as cover, fight with ordinary or advanced equipment, and retreat for oxygen or supplies. Learning the creature's silhouette and cues creates an exploration advantage. The encounter uses the existing Observatory identity, terrain and finite population, leaving future regional progression independent of this local reward.

## Entity, model and resources

`ZTO.AbyssalCustodian` is a registered legacy living entity with the native hostile marker, no natural spawn table or egg, and a stable 1.55 × 4.45 block body box. It breathes in water and retains native health, armor and damage attribution. It uses the existing loaded-water swim sweeps and bounded route search. Idle movement thrust is 0.09 blocks/tick, cruising 0.13, and phase-two repositioning 0.17 before drag. The controller bounds swimming by the chamber, permits pursuit at the entrances, and returns toward the center on retreat. The original construction remains mutable.

The redesigned body carries four individually phased four-part tentacles. Front and rear roots are separated in depth and width. Tapered upper arms and separate forearms deform around the shoulder and elbow. The narrow neck allows independent head tracking. The body turns at bounded rates while the renderer interpolates across the shortest yaw arc. The strike commits its own direction and shares shoulder coordinates between Java hit geometry and model.

The body and eye atlases are 128 × 128. Their faces remap original Thief and Saboteur textures at nearest-neighbor resolution. The gills and ribs add small original accents. The renderer observes the authoritative strike length, elevation clearance and body scale. Mono cues include Compression contraction and re-expansion.

Run `python artwork/tools/custodian_assets.py` then export the textures. After MCP compilation, run `python artwork/tools/custodian_previews.py` for poses, scale and the enderic family sheet under `artwork/previews/custodian`. Set `ZTO_REFINEMENT_BASELINE` to the old local mod JAR to include the previous Enderkin atlas. The preview recorder uses the authored Java model and native UV quads.

Fresh native damage animates a directional head and body recoil, followed by a delayed tentacle curl. The model keeps the committed striking arm aligned with the authoritative attack geometry. The same recoil applies at compressed scale. The headless model capture writes `hurt-reactions.png` beside the combat poses.

## Combat

The creature starts at 80 HP. A vulnerable diver within 17 blocks who enters the chamber territory can trigger a 24-tick awakening. Phase two starts once at 40 HP or below after the active attack cycle. It has a 28-tick transformation, shorter Riftstep cooldown and more frequent repositioning. Damage values and recovery windows carry across both phases.

| Attack | Wind-up / active / recovery | Effect |
| --- | --- | --- |
| Elongated Strike | 22 / 6 / 34 ticks | Alternates arms. Locks aim six ticks before release. A 0.17-block half-width sweep from the selected shoulder stops at native block collision and attempts one hit on the nearest intersected diver. Maximum visible reach is 4.85 blocks and Normal damage is 5 HP. |
| Abyssal Repulsion | 34 / 1 / 42 ticks | A single water shock within 6 blocks applies 2 HP on Normal and a distance-scaled radial impulse up to 0.46 blocks/tick. Solid cover interrupts the effect. Independent cooldown is 108 ticks after recovery. |
| Riftstep | 18 / 1 / 22 ticks | Samples up to 16 chamber destinations 4–9 blocks away with vertical variation and a 3-block target margin. Rechecks water, loaded chunks, body and animation clearance, and occupancy before moving the same entity. Cooldown is 132 ticks in phase one and 92 in phase two after recovery. |
| Compression | 26 / 48 / 48 ticks minimum | Shrinks the entire rendered body and native collision to 38%, swims physically at up to 0.23 blocks/tick, and re-expands only after checking loaded water, arena bounds, masonry and actors. It does no added damage. |
| Enderic Resonance | 26 / 1 / 32 ticks | Signals at most two living, loaded, same-site Enderkin for 48 ticks. Eligible actors approach using their existing swimming, pursuit and casting constraints. Independent cooldown is 220 ticks after recovery. Revision-one fish cannot respond. |

Context and cooldowns affect attack choice. The previous selection receives lower weight. The player can break line of sight, move out of the locked strike, use vertical space or retreat. The Custodian applies no oxygen cost, forced player displacement through blocks, arena lock, healing, replacement minions or boss bar.

Compression preserves the feet anchor, native identity, health, damage eligibility and encounter state. Expansion takes at least 24 ticks, followed by recovery. If geometry blocks expansion, the Custodian remains smaller and looks for clearance. The native scale is saved in entity NBT. Original alpha.4 actors from before the resize field reopen at 55% and use the same validated expansion path. Compression cooldown is 280 ticks in phase one and 205 in phase two.

## Admission and persistence

All three Observatory layout revisions are eligible once their persisted footprint is complete, loaded, stamped and populated. Admission checks hostile difficulty, the nearby diver and native water clearance. The full-body bounds come from that layout's chamber. Modifications are never repaired. Failed geometry can defer initial admission. This policy uses the existing Anvil provider and site stamp hooks.

A versioned file adjacent to the site record binds its site identity to a generated encounter UUID. A synced atomic replacement reserves it before native insertion. Native NBT owns actor position, health and phase. On load, site ownership and UUID must match the record. The lowest native entity ID among nearby copies of the same authorized actor is canonical. Peaceful suspends the fight and preserves the actor. Retreat preserves health and resets the current attack. Terminal defeat consumes the local reward before a normal Ender Pearl item enters the native drop list. [Reward and crash semantics](ender-pearl-scarcity.md)

The entity is written by the ordinary chunk saver. I/O failures fail closed. Loss of the actor after a durable reservation, including interrupted insertion or an unsaved chunk, can leave the encounter reserved without a living Custodian. It cannot be recreated automatically with fresh health or a fresh reward.

## Owner walkthrough

Build and install the matching local-dev pair. `zto-tools/test-zto-agent.ps1 -Focus custodian -CustodianOwnerSave -PackJava java` exports the marked **ZTO Alpha4 Refinement** Creative save. It starts on a platform directly above a verified buried entrance, with test architecture and diving gear.

1. Open Creative inventory and select **Spawn Eggs**. Use the five aquatic eggs against a submerged face or aim into open water. The Custodian needs a clear volume roughly five blocks tall. Spawn an Abyssal Enderkin near the manual Custodian to exercise Resonance.
2. Switch the disposable world to Survival using the pack's NEI game mode control. Observe strike, pressure, teleport, resonance and Compression. Use cover to evade the strike and pressure. Watch for shorter teleport cooldowns after half health.
3. Follow the seabed under the platform to the eroded entrance. Evaluate the density of patrols, the staircase, lamp surrounds and the alternate route. The chamber contains a legitimate actor. Retreat and return to confirm health, then defeat it and check the single pearl after saving and reopening.
4. The existing **Numpad 1 → Biome Ecology → Observatory** developer view remains available with `-Dzto.dev=true`. Ordinary Creative eggs need no developer flag.

The focused native fixture verifies three new plans, both historical revisions, temporary egg insertion, native attack geometry, physical Compression, collision-blocked expansion, scale NBT and reward regressions. The observational graphics, underwater partial-block seams, sound timing and live Creative interaction require owner review.
