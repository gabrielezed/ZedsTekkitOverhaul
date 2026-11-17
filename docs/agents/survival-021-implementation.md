# Survival 0.2.1 implementation

The [full design specification](survival-021-spec.md) governs the milestone. The draft includes ordinary hostile pressure, live diagnostics, Tank combat and localized breaching, role-specific teleportation and ordinary Enderkin ecology for local single-player Tekkit Classic 3.1.2.

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

## Verification and pending work

Run `zto-tools/test-zto-agent.ps1 -Pack` after building. The headless comparison exercises the reobfuscated mod against the pinned Forge 3.3.8.164 world, real terrain and native spawner. It compares saturated nighttime populations with identical terrain and player position, then checks daylight restrictions for light-sensitive mobs, saved activity reload, return qualification, decay-driven cap reduction and the off setting. The fixture retains mobs and runs spawn passes directly to measure density independently of combat and despawning.

In a disposable Tekkit Dev guest, play comparable nights in quiet and sustained-activity cells. Compare the monitor's pass activity, effective cap and insertions while observing ordinary hostile density outside a lit shelter. Repeat across a natural full moon to compare the current territorial score with the captured assault admission. Physical key navigation, perceived balance and full client regression still require a graphical Tekkit Dev run.

The same command checks Tank timing, dodge and recovery, Hard-only damage to a plank barrier, protected blocks, role movement and the persisted breach budget. It checks actual native Enderkin insertions and compares the same ecology roll beside an exposed BuildCraft pipe and behind a wall. The combat fixture runs on the pinned legacy world and silences the graphical client's audio hook. Owner playtesting still needs to judge combat feel, pathing across varied terrain, graphical pose readability and ordinary encounter frequency.

Thief theft, extended escape and loot recovery, and Saboteur physical component theft remain pending. Aerial fauna and settlement recognition belong to later development. Ecology consumes the existing assessment and the native hostile budget, leaving future settlement data and lunar encounter ownership with their respective systems.
