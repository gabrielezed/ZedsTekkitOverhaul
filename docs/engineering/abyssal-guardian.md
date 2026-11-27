# Abyssal Fangfish

Fangfish are ambush predators native to deep ocean water, especially the Trench. They patrol different elevations, pause before lunging, and give the player time to break line of sight, seal an opening or change depth. Revision-one Observatories retain their original fish populations. [Revised ecology](abyssal-ecology.md)

## Runtime and presentation

The persistent native name is `ZTO.AbyssalGuardian`. FML assigns a free numeric registry ID during startup. The entity extends the 1.2.5 `EntityMob` class for native hostile difficulty scaling, armor, damage hooks and XP. Its own controller handles breathing, motion and targeting in water. It has no ordinary mob-list entry or configured spawn egg.

The renderer uses cuboids with articulated head, jaw, peduncle, tail and fins. The 64×64 source at `artwork/source/abyssal/guardian.png` exports to `/zed/textures/abyssal/guardian.png` in both profiles. The watcher drives pursuit, wind-up, lunge and recovery poses. Five original mono PCM clips under `/zed/sounds/guardian/` run through the legacy Forge positional sound pool. `artwork/tools/guardian_assets.py` authors the UV sheet, sound waveforms and enlarged UV preview.

## Movement and combat

Guardians accelerate in three axes and apply drag before their next move. Swept water checks and the native entity collision resolver constrain movement. The direct path is preferred in open water. If it is obstructed, four local feelers and a cached search over suitable loaded water cells handle corners, passages and changes in depth. Each search expands at most 192 nodes, keeps paths within an 18-step horizon, and retries at staggered intervals. A continuously blocked pursuit releases after 46 unsuccessful ticks.

| Parameter | Candidate value |
| --- | --- |
| Health | 20 HP |
| Unarmored attack damage | 3 / 4 / 6 HP on Easy / Normal / Hard |
| Detection | 18 blocks with line of sight |
| Patrol | Up to 10 blocks per horizontal axis and four vertically from the home cell |
| Pursuit | 24-block acquisition territory, 28-block target leash and 30-block actor leash |
| Alert, wind-up, lunge, recovery | 12, 10, up to 8, and 32 ticks |
| Cruise, pursuit, burst target speeds | 0.14, 0.23, 0.38 blocks per tick |
| Hit reach | 1.45 blocks, with clear water and line of sight |

The owner-thread state machine is Patrol → Alert → Pursue → Wind-up → Lunge → Recover. A dry refuge, target loss, range or prolonged obstruction sends the creature home. An attack commits to its wind-up direction and gets one damage attempt. Native armor and damage hooks handle the result. Guards leave blocks, inventories and oxygen items under ordinary game rules.

## Site and wild populations

Each revision-one site retains 16–19 deterministic fish slots across exterior patrols, the atrium, galleries, annexes, hall and arena approaches. Once the matching generated slices are loaded and populated, one candidate can materialize per 20 ticks, 8–29 blocks from the player. The intended cell and five nearby alternatives must be submerged and clear. Revision-two sites use [Abyssal Enderkin](abyssal-enderkin.md).

The site reserves each slot on disk before inserting its actor. Native NBT stores position, health, home, site identity and slot. The site identity and slot together form the stable actor identity. Native death records the terminal bit. Peaceful retires loaded site actors without replacing them. Eviction does not change the record. Loaded duplicates lose eligibility and are removed without a drop. When a reservation survives a crash but an entity save does not, the reservation remains consumed.

The shared wild pass considers Fangfish in ordinary deep ocean and saved Trench water at or below Y42. At most two wild Fangfish share the combined four-creature budget. Admitted site bounds and a 12-block margin exclude wild encounters. Wild Fangfish use native distance despawn and award ordinary XP. [Spawn parameters](abyssal-ecology.md)

## Owner walkthrough

Dive with a mask and a charged tank. Approach the exterior and verify the silhouette, ambient positioning and attack cue. Break pursuit around a hall column, swim over a Guardian and try a constructed dry refuge. Kill a site actor, Save/Quit and revisit. Check the finite population counters in the developer panel. Verify that the main entrance supports retreat and that the two arena entrances remain usable. Sound, model appearance and combat feel require in-game acceptance.
