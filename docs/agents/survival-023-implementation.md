# Survival 0.2.3 alpha.1 implementation

The [design draft](survival-023-spec.md) records the proposed milestone. This page describes the playable prototype and the decisions still open for owner review. Regional encounters give players a reason to read the terrain and adapt their builds and tactics. The prototype consumes ordinary spawning and aerial encounters without taking ownership of territorial history, lunar scheduling, or technology progression.

## Legacy biome registry

Verified with MCP 6.2 mappings and the Tekkit Classic 3.1.2 client classpath, with the installed Forge overlay before the Minecraft 1.2.5 client. The registry is BiomeGenBase (abn). The default generator uses WorldType (vx), GenLayerBiome (ahy), GenLayerHills (ao), GenLayerShore (aae) and GenLayerRiverMix (akl). A headless probe sampled all 21 Overworld IDs from the default generator and checked the registry names and both native mushroom hostile lists.

| Family | Registered IDs and runtime names | Default Overworld generation |
| --- | --- | --- |
| Ocean | 0 Ocean, 10 FrozenOcean | Base water and the cold ocean variant emitted by AddIsland after biome selection. FrozenOcean can be narrow and easy to miss. |
| Plains | 1 Plains | Base land. Hills can create pockets of Forest. |
| Desert | 2 Desert, 17 DesertHills | Base land and hills variant. |
| Mountains | 3 Extreme Hills, 20 Extreme Hills Edge | Base land and transition produced at the boundary by Shore. |
| Temperate Forest | 4 Forest, 18 ForestHills | Base land and hills variant. |
| Taiga | 5 Taiga, 19 TaigaHills | Base land and snowy hills variant. |
| Swamp | 6 Swampland | Base land with intermittent River cells. |
| Rivers and Coasts | 7 River, 16 Beach | RiverMix and coastal Shore outputs. SwampRivers also places River cells in swamps and jungles. |
| Snow and Ice | 11 FrozenRiver, 12 Ice Plains, 13 Ice Mountains | FrozenRiver is the result of a river crossing Ice Plains. Ice Mountains is the Ice Plains hills variant. |
| Mushroom Island | 14 MushroomIsland, 15 MushroomIslandShore | Rare mushroom islands and their shore. RiverMix also uses the mushroom shore for river cells crossing them. |
| Outside the system | 8 Hell, 9 Sky | Nether and End provider biomes. |

This inventory applies to the default 1.2.5 generator. Flat worlds and worlds retaining the 1.1 generator use their native biome output. Existing chunk biome bytes are read as saved. Unclassified IDs use the fallback weights.

## Ordinary Enderkin

The existing ZTOAgent callback still substitutes one validated Zombie, Skeleton, Creeper or Enderman slot. Vanilla light admission, standard spawn fallback, replacement collision and floor checks, and Territorial Activity's hostile population cap remain in the same path. Each 1,000 eligible slots give the four ordinary roles 34 combined tickets, plus two per pair of activity points up to 54. Activity is the existing natural-pass assessment. Each role draw takes place within this budget. A Tank still occupies at most one local Tank slot. The overall local Enderkin limit remains five.

| Family | Skirmisher | Breacher | Thief | Tank |
| --- | ---: | ---: | ---: | ---: |
| Temperate Forest | 19 | 42 | 35 | 4 |
| Plains | 62 | 22 | 13 | 3 |
| Desert | 72 | 15 | 10 | 3 |
| Swamp | 12 | 36 | 50 | 2 |
| Mountains | 49 | 29 | 12 | 10 |
| Taiga | 20 | 52 | 22 | 6 |
| Snow and Ice | 48 | 35 | 13 | 4 |
| Jungle | 10 | 48 | 38 | 4 |
| Rivers and Coasts | 41 | 32 | 22 | 5 |
| Ocean | 42 | 32 | 20 | 6 |
| Unclassified | 44 | 32 | 18 | 6 |

The candidate's biome contributes four parts. Each available cardinal sample 16 blocks away contributes one part. Sampling checks chunk availability first. Protected families neither lend weights to a neighbor nor admit replacements at their own center. Snow and mountains retain the same ordinary substitution budget, with Tank weighted at 10% of the four roles in the mountains before local role caps. The existing Saboteur lane has five incidental tickets and up to 106 additional tickets near exposed supported infrastructure per 1,000. It shares the same local cap and retains its one-Saboteur limit. Industrial eligibility is the same across ordinary biomes. Lunar role plans retain their separate selection and budgets.

## Forest wildlife

Small magpies can appear in dry Overworld daylight on exposed grass in Forest and ForestHills, including suitable clearings at their borders. One check tries six loaded positions 18–32 blocks from the player. Successful placements are spaced at least 30 seconds apart during normal 20 TPS play, with an admission cap of four loaded magpies. They can walk, hop, observe, flap through short flights, land and briefly perch when they land on leaves. Approaching closely without sneaking or damaging a bird can cause it to flee. Magpies occasionally approach and peck at dropped vanilla seeds or feathers. The item entity and its stack remain available to pick up. The prototype adds no drops or item IDs. Magpies use ordinary distance despawning and an age limit of five minutes of entity ticks that survives save/load. Their transient flight and curiosity reset on reload.

## Fugl predation

A solitary Fugl in a passing daytime ground encounter may select an exposed chicken after its arrival. It uses the existing Fugl approach, warning, actual dive, collision and recovery. The prey search is intermittent and limited to an 18-block box with an additional 22-block distance cap. A Fugl gets one chicken target and at most one committed dive. Roof cover disqualifies the chicken throughout pursuit and at contact. Native ray and swept collision checks provide wall protection. Aerial flocks, player hunts, lunar predators and Skelebird retain their prior behavior. Chickens are the only prey in this prototype.

## Settings and diagnostics

The existing naturalEnderkinSpawning setting controls the ordinary substitutions. The new forestMagpies and fuglChickenHunting settings both default to true and apply after restart. Disabling magpie admission leaves saved birds readable. Existing profiles get the new defaults from the settings loader. No world migration is required.

Numpad 1 opens the Debug Hub. Biome Ecology displays the player's loaded biome ID, family and blended relative weights. It also shows hostile cap and pass activity, the latest ecological selection and latest ecological rejection, the magpie admission status and loaded count at the last check, and any active Fugl prey target. A recorded replacement decision precedes vanilla insertion. The monitor does not require Dev Bridge.

## Owner playtest

- Compare repeated ordinary nights in Forest, Desert and Swampland at similar light and activity. Check whether the regional roles are recognizable and whether a desert factory still presents Saboteur opportunities.
- Visit a forest edge during dry daylight. Watch from a distance, drop seeds or feathers, then compare a sneaking approach with a normal approach. Review the bird's proportions, gait, wing animation, landing and how frequently the behaviors are visible in regular play.
- Put a few chickens in an exposed pen and others behind walls and a roof. Observe passing daytime Fugl across several encounters. Review visibility of the warning, hunt frequency and whether shelter is effective in a survival build.

The weights, habitat and hunt cadence are reversible prototype defaults. Dropped-item carrying, player-inventory theft, active perch selection, specialized calls, regional changes to aerial species prevalence, and alternate prey remain open. Electric Jetpack and EE2 crafting changes belong to a separate design pass.

## Verification

- Portable checks passed for configuration, lunar calendar, assault and patrol schedules, outdoor approaches, and current documentation.
- MCP 6.2 with JDK 8 compiled and reobfuscated the client. The staging build passed streaming validation of all 412 entries. The gameplay source is commit 3c730e695535e9d4197421ed6e66d963a281250b. The build sidecar reports a dirty workspace because documentation and test changes and unrelated local files were present at build time. Mod SHA-256 is 4c8472ffae33e2473ea9944503da396aebcdd8cd1fa7bf85f9ef41eeca1902d2. Agent SHA-256 is 35e8fc1ec25c552afd401890d4d444e63227d9ca1abfd1ffb5ddebf026613060.
- The pinned Tekkit client classpath passed the headless pack suite with Forge 3.3.8.164 and Java 8u511. A fixed-seed selection fixture used 20,000 candidates per biome. Each of the 11 ordinary test biomes yielded 756 replacements. Forest yielded 274 Breachers and 223 Thieves, Desert yielded 459 Skirmishers, and Swampland yielded 309 Thieves. Extreme Hills yielded 55 Tanks. Both mushroom profiles yielded zero substitutions. The fixture also checked boundary blending and unloaded sampling.
- The pack fixture verified daytime forest admission, the magpie admission cap, walking, hopping, pecking, short flight, landing, a leaf perch, evasion, distance despawn, native entity NBT and the packaged atlas. It observed a Fugl damage an exposed chicken while leaving the roofed and subsequent chicken unharmed. Existing pack probes passed for ordinary hostile pressure, Enderkin abilities and theft, Skelebird, native IC2 weather behavior, lunar aerial authority and replay, and installed compatibility hooks.
- Graphical playtesting and full milestone acceptance remain pending. This campaign used headless disposable worlds and staged the build separately from the interactive Tekkit Dev profile.
