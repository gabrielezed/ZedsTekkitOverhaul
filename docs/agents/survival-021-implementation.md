# Survival 0.2.1 implementation

The [full design specification](survival-021-spec.md) governs the milestone. The draft includes ordinary hostile pressure, live diagnostics, Tank combat and localized breaching, role-specific teleportation, ordinary Enderkin ecology and the Thief encounter for local single-player Tekkit Classic 3.1.2.

## Debug Hub

Press **Numpad 1** from a local world to open the ZTO Debug Hub. Choose **Lunar Cycle**, **Survival & Assault**, or **Territorial Threat Monitor**. Escape or the Debug Hub button returns from a child screen. These screens keep the simulation running. The Hub and histories work with Dev Bridge off. Existing direct shortcuts remain configurable, with Numpad 1 reserved for the Hub by default. The Survival overlay is still opt-in.

In the monitor, **Current territory** shows the current 64-block cell, its decayed presence and return contributions, the blended activity from adjacent cells, biome contribution, combined threat, last observation age and storage status. At 20 ticks per second, absence removes one point of presence per minute and one return point per five minutes of Overworld simulation. Two valid 100-tick observations qualify for a point of presence. A spaced return with activity remaining requires four samples for the two-point visit contribution. The ledger retains its existing save key and schema.

**Ordinary hostile spawning** reports the most recent native pass, including sampled blended activity, the loaded hostile count at the start of the pass, the vanilla and effective caps, and the current restriction. Direct native hostile insertions and cap-limited passes are cumulative for the current world session. The loaded count includes qualifying hostiles from any source, including caves and surviving lunar mobs. The insertion counter measures the ordinary primary insertion site. It excludes secondary vanilla effects such as spider jockey riders.

**Lunar assault** shows values captured at the last recorded admission, including its cell, combined threat, profile and budget. `experimentalTerritorialThreat` independently controls whether the director uses that assessment at natural full-moon admission.

## Ordinary hostile pressure

In the Overworld, the existing single-player territorial service supplies the same blended activity used by the threat assessment. The cap bonus rises linearly from zero at activity 0 to the configured maximum at activity 20, with integer rounding. The environmental contribution remains available to the assault director. Fresh terrain has the vanilla cap in any biome, and biome-specific ordinary species and weights still come from the native spawn lists.

On a standard single-player native pass, the base cap is 79. The default `territorialHostileCapBonusPercent=70` raises the effective cap to 134 at saturation. Edit the unified configuration and restart to set the maximum from 0 to 100 percent. Zero uses the native comparison and loop. The increased cap allows the ordinary pass to continue replenishing its population after it would normally stop. The native attempt frequency, chunk radius, and block, light, distance, biome and entity validation remain in place. Bonus passes stop admitting primary hostile insertions when their budget fills. Native secondary spawns, other spawn sources, and a cap that decreases while mobs remain alive can leave the loaded population above the effective cap.

The single ZTOAgent checks the fingerprint of the pack's `vf.class` and adjusts only the ordinary hostile path. Territorial cap management uses the game's existing count and insertion result. Remote play, other dimensions, unavailable activity, Peaceful, and missing or unsupported agent hooks receive no cap bonus. The monitor reports when the hook is unavailable. Keep the mod and companion agent from the same build. [Prism installation](../deploy-prism.md)

## Tank

The Tank keeps its 1.85×3.15 block collision box and has 64 health points. Native damage still applies, with 40% of the ordinary melee knockback impulse. A frontal strike deals 9 base damage and pushes the target on a successful hit. The Tank locks its facing while winding up for 24 ticks. At the contact keyframe it checks the frontal arc, distance and a strike lane through cover. A miss still incurs 26 ticks of recovery.

On Hard, the Tank first tries normal navigation. After roughly 2.4 seconds without meaningful progress it can commit that same animation to a nearby obstruction between it and its target. One strike can remove up to four blocks in one exposed column. It can complete at most three breach strikes over its lifetime. Planks, logs, cobblestone, glass and bricks are eligible. Reinforced blocks, doors, complex blocks and TileEntities stay protected. Destruction uses ordinary block removal and drops. Leaving the attack arc, changing the obstruction, or landing a hit of at least five incoming damage during the windup can foil a breach. Lower difficulties keep the heavy combat and suppress block removal. Existing Tanks keep the health from their saves. Only the breach count is persisted, and existing tags without the counter get a fresh budget.

## Enderkin mobility

The Skirmisher blinks sideways for a new firing angle or away from close pressure. The Breacher closes through a short clear lane when route-finding stalls and can reach low platforms. The Saboteur keeps its existing short approach and evasive blinks during industrial disruption. The Thief favors a diagonal retreat after contact or injury. After a longer stall, the Tank telegraphs a rare relocation onto a visible open platform up to four blocks higher. A nearby eligible Hard-mode breach takes priority. Grounded blinks check the swept body and floor. Elevated moves require a grounded target, clear vertical and horizontal volumes, and solid footing for the whole body. All checks stay within loaded terrain.

## Ordinary Enderkin ecology

With `naturalEnderkinSpawning=true`, the pinned hook can substitute a Skirmisher, Breacher, Thief, Tank or Saboteur after a vanilla Zombie, Skeleton, Creeper or Enderman passes its ordinary spawn checks. The replacement must also fit its full body with solid footing. Failed substitutions keep the native entity in the same slot. The ambient roll is at most 3.9% of eligible candidates with zero activity and 5.9% at saturation, before space and population checks. Tank accounts for 0.2% of eligible rolls. The already loaded local population gate permits at most five Enderkin within the candidate's 32-block horizontal and 16-block vertical box, with one nearby Tank and one nearby Saboteur.

An exposed BuildCraft wooden item or liquid extractor, or a ZTO sabotage-capable tile, raises the nominal Saboteur roll from 0.5% to 11.1%. Candidates check within 12 horizontal and four vertical blocks, up to 32 tiles per already loaded chunk, and require sight to the signal. The habitat check reads component identity and leaves the machine operating normally. The existing hostile cap and lunar encounter selection remain separate controls. Disable the ecology setting to compare against ordinary vanilla species under the same territorial cap.

## Thief encounter

A grounded Thief in the local Overworld can wind up a grab within 2.2 horizontal blocks of a visible, vulnerable player. The reach animation lasts 16 ticks. A landed hit, loss of sight or reach, or displacement interrupts it. The transfer occurs at contact. It removes one item from a nonempty ordinary inventory or hotbar slot. The hotbar is preferred one third of the time when both categories have candidates. Armor, held cursor items, active-use stacks and foreign container inventories are outside the grab. Inventory Tweaks sorting rules still apply to its own actions. The Thief's gameplay selection uses the actual 36 player slots independently of sorting priorities.

An ordinary grab plays a low pickup sound. A hotbar grab plays a sharper portal sound. A brief burst of portal particles and a chat line identify the theft and the item. The Thief tucks one arm against its torso while carrying the loot. During the chase, short portal blinks and intermittent particles mark its movement.

Carrying loot switches the Thief from combat to escape. It uses successive native paths to seek ground away from the pursuer, favors routes that break sight, and can make a short diagonal blink when cornered or stalled. It runs for at least 130 ticks, can continue while pursued, and settles once it has opened a substantial gap. It flees again if approached. It can cross chunk and biome boundaries through ordinary movement. Routes and blinks stay within the loaded terrain.

The local Overworld save stores one outstanding stack in `data/zto_thief_trail.dat`. The Thief entity stores only its matching custody token. Native stack splitting and save tags preserve count, metadata, durability and nested NBT. Ordinary despawn remains available before theft and is suppressed while carrying. The position is updated during the chase and survives chunk unloading and normal save and reopen. There is no migration for old Thieves without a custody token. Player inventory, chunk data and the trail use the normal legacy save cycle, which writes separate files.

Sneaking with a vanilla compass once shows the last recorded location. Death or Peaceful removal releases the stack once as a normal dropped item on nearby loaded, safe footing, after which normal item-drop risks apply. If there is no safe surface near the Thief, the stack stays in a cache at the original theft site. The same compass gesture within six blocks of the cache releases the item onto safe footing. It can also claim an orphaned trail at the last location after the nearby chunks have loaded and no matching carrier is present. Go back to the Overworld to inspect a trail after taking a portal. The one-loot limit applies until the outstanding stack is dropped or recovered.

## Saboteur pipe theft

On Normal or Hard in the local Overworld, a Saboteur can prioritize an exposed BuildCraft 2.2.14 stone or cobblestone **item** transport pipe during its normal industrial scan. The dismantling pose lasts 35 ticks. A player hit, lost reach or visibility, replacement of the block or tile, or cargo entering the pipe interrupts the attempt. The mob can steal once over its lifetime. Temporary disruption still works when it chooses another supported target.

At contact the implementation checks the installed block, loaded tile, pipe implementation, registered item and both native item transport queues again. It removes the block using Minecraft's normal notified removal path. BuildCraft tears down the tile and updates the neighboring network, and the mob stores one stack with the stock pipe's exact item ID and metadata zero. Fluid, power, extractor and inventory-bearing pipes are ineligible, as are item pipes with cargo in transit or awaiting restoration after chunk load. The Saboteur uses fast short routes and its existing evasive blinks to get away.

The component and theft flag persist in the Saboteur's entity NBT. The carrier is exempt from ordinary despawn. Death or Peaceful removal tries to release the item once on nearby safe, loaded footing, with the loaded theft site as a fallback. If neither is suitable, the entity retains the cargo and retries rather than clearing it. Ordinary dropped-item rules apply once the pipe is released. Existing Saboteurs have no stored cargo or theft flag.

## Ender Trail

Sneak with a vanilla compass to see the existing chat report and request a short violet trail toward the last recorded Thief location or cache. A moving brighter marker shows direction. The trail lasts about four seconds, fades near the end and can be refreshed with another sneak press after roughly one second. It remains visible after releasing sneak. The compass can still retrieve a nearby cache.

The route uses up to three native land-path queries through already loaded terrain, toward waypoints within eight blocks. It shows up to 12 actual path nodes, including partial paths that make measurable progress toward the saved location. If there is no usable route, chat retains the coordinates and asks the player to try another approach. The trail represents the last saved position at request time.

## Verification and pending work

Run `zto-tools/test-zto-agent.ps1 -Pack` after building. The headless comparison exercises the reobfuscated mod against the pinned Forge 3.3.8.164 world, real terrain and native spawner. It compares saturated nighttime populations with identical terrain and player position, then checks daylight restrictions for light-sensitive mobs, saved activity reload, return qualification, decay-driven cap reduction and the off setting. The fixture retains mobs and runs spawn passes directly to measure density independently of combat and despawning.

In a disposable Tekkit Dev guest, play comparable nights in quiet and sustained-activity cells. Compare the monitor's pass activity, effective cap and insertions while observing ordinary hostile density outside a lit shelter. Repeat across a natural full moon to compare the current territorial score with the captured assault admission. Physical key navigation, perceived balance and full client regression still require a graphical Tekkit Dev run.

The same command checks Tank timing, dodge and recovery, Hard-only damage to a plank barrier, protected blocks, role movement and the persisted breach budget. It checks actual native Enderkin insertions and compares the same ecology roll beside an exposed BuildCraft pipe and behind a wall. The combat fixture runs on the pinned legacy world and silences the graphical client's audio hook. Owner playtesting still needs to judge combat feel, pathing across varied terrain, graphical pose readability and ordinary encounter frequency.

The Thief probe checks a canceled grab, the contact tick, distinct sound dispatch, a sustained pursuit with native navigation, ordinary despawn resistance, Anvil save and reopen, and release of a tagged item exactly once. For a graphical playtest, carry inexpensive items in both inventory categories, then intercept one grab and allow another to finish. Follow the Thief through mixed terrain, lose sight and use a compass, and test recovery after reopening and after a hazardous death. Combat feel, audio balance and cross-biome chase behavior remain owner playtest targets.

The focused pack probe exercises actual pipe removal, the resulting BuildCraft disconnection, interrupted dismantling, cargo recovery and temporary guidance around a wall. In an isolated playtest, place a draining item line with a stone or cobblestone segment at foot level. Interrupt one dismantle, let another finish, pursue the Saboteur and reinstall the recovered pipe. Try refreshing the Ender Trail beside a wall or hillside and around a Thief cache.

Equipped armor theft, cooperative theft and broader integration acceptance remain pending. Aerial fauna and settlement recognition belong to later development. Ecology consumes the existing assessment and the native hostile budget, leaving future settlement data and lunar encounter ownership with their respective systems.

### Owner observations for acceptance testing

- A Hard-mode Tank reportedly failed to breach the wall between it and the player. Reproduce with the wall material, collision dimensions, line of sight and pursuit progress recorded separately.
- A fresh Survival world in an outdoor swamp produced one lunar assault insertion from 113 attempts. Another ordinary Survival world showed similarly low rates. Use the existing Survival & Assault diagnostics to compare rejection reasons, terrain, approach lanes and eligibility. These observations have no confirmed cause yet.
