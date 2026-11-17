# Survival 0.2.1 first iteration

The [full design specification](survival-021-spec.md) governs the milestone. This iteration adds an experimental ordinary hostile pressure policy and live diagnostics for local single-player Tekkit Classic 3.1.2.

## Debug Hub

Press **Numpad 1** from a local world to open the ZTO Debug Hub. Choose **Lunar Cycle**, **Survival & Assault**, or **Territorial Threat Monitor**. Escape or the Debug Hub button returns from a child screen. These screens keep the simulation running. The Hub and histories work with Dev Bridge off. Existing direct shortcuts remain configurable, with Numpad 1 reserved for the Hub by default. The Survival overlay is still opt-in.

In the monitor, **Current territory** shows the current 64-block cell, its decayed presence and return contributions, the blended activity from adjacent cells, biome contribution, combined threat, last observation age and storage status. At 20 ticks per second, absence removes one point of presence per minute and one return point per five minutes of Overworld simulation. Two valid 100-tick observations qualify for a point of presence. A spaced return with activity remaining requires four samples for the two-point visit contribution. The ledger retains its existing save key and schema.

**Ordinary hostile spawning** reports the most recent native pass, including sampled blended activity, the loaded hostile count at the start of the pass, the vanilla and effective caps, and the current restriction. Direct native hostile insertions and cap-limited passes are cumulative for the current world session. The loaded count includes qualifying hostiles from any source, including caves and surviving lunar mobs. The insertion counter measures the ordinary primary insertion site. It excludes secondary vanilla effects such as spider jockey riders.

**Lunar assault** shows values captured at the last recorded admission, including its cell, combined threat, profile and budget. `experimentalTerritorialThreat` independently controls whether the director uses that assessment at natural full-moon admission.

## Ordinary hostile pressure

In the Overworld, the existing single-player territorial service supplies the same blended activity used by the threat assessment. The cap bonus rises linearly from zero at activity 0 to the configured maximum at activity 20, with integer rounding. The environmental contribution remains available to the assault director. Fresh terrain has the vanilla cap in any biome, and biome-specific ordinary species and weights still come from the native spawn lists.

On a standard single-player native pass, the base cap is 79. The default `territorialHostileCapBonusPercent=70` raises the effective cap to 134 at saturation. Edit the unified configuration and restart to set the maximum from 0 to 100 percent. Zero uses the native comparison and loop. The increased cap allows the ordinary pass to continue replenishing its population after it would normally stop. The native attempt frequency, chunk radius, and block, light, distance, biome and entity validation remain in place. Bonus passes stop admitting primary hostile insertions when their budget fills. Native secondary spawns, other spawn sources, and a cap that decreases while mobs remain alive can leave the loaded population above the effective cap.

The single ZTOAgent checks the fingerprint of the pack's `vf.class` and adjusts only the ordinary hostile path. The patch uses the game's existing count and insertion result and adds no chunk search, entity scan or spawn loop. Remote play, other dimensions, unavailable activity, Peaceful, and missing or unsupported agent hooks receive no bonus. The monitor reports when the hook is unavailable. Keep the mod and companion agent from the same build. [Prism installation](../deploy-prism.md)

## Verification and pending work

Run `zto-tools/test-zto-agent.ps1 -Pack` after building. The headless comparison exercises the reobfuscated mod against the pinned Forge 3.3.8.164 world, real terrain and native spawner. It compares saturated nighttime populations with identical terrain and player position, then checks daylight restrictions for light-sensitive mobs, saved activity reload, return qualification, decay-driven cap reduction and the off setting. The fixture retains mobs and runs spawn passes directly to measure density independently of combat and despawning.

In a disposable Tekkit Dev guest, play comparable nights in quiet and sustained-activity cells. Compare the monitor's pass activity, effective cap and insertions while observing ordinary hostile density outside a lit shelter. Repeat across a natural full moon to compare the current territorial score with the captured assault admission. Physical key navigation, perceived balance and full client regression still require a graphical Tekkit Dev run.

Advanced Tank combat and structural interaction, Thief theft and recovery, Saboteur physical component theft, differentiated teleportation, and ambient and contextual Enderkin spawning remain pending. Aerial fauna and settlement recognition belong to later development. This iteration uses local activity through the existing assessment interface, leaving future settlement data and encounter ownership with their respective systems.
