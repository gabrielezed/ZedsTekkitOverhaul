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

Small magpies can appear in dry Overworld daylight on exposed grass in Forest and ForestHills, including suitable clearings at their borders. One check tries six loaded positions 18–32 blocks from the player. Successful placements are spaced at least 30 seconds apart during normal 20 TPS play, with an admission cap of four loaded magpies. They can walk, hop, observe, flap through short flights, land and briefly perch when they land on leaves. Approaching closely without sneaking or damaging a bird can cause it to flee. Magpies use ordinary distance despawning and an age limit of five minutes of entity ticks that survives save/load.

The playtest follow-up gives ground travel 14–27 tick bouts with 8–18 tick pauses. The trot advances by actual level-ground travel, so a pause or blocked stride settles the feet. A reachable full-block rise gets an 11-tick hop. A clear two-block rise gets a 16-tick wing-assisted climb. Takeoff, crossing and landing are bounded to loaded terrain and checked against collision boxes before departure and during movement. A tall wall, low canopy or blocked landing redirects the bird along a clear local heading.

The head can yaw and pitch independently toward a visible stationary or sneaking player within six blocks. A fast approach within four blocks can still startle the bird, as can damage. Seeds and feathers draw brief attention, including a stationary peck. The existing mesh and atlas use a slightly darker tint. Ground travel, hopping, flight, a brief landing pose and leaf perching have separate animation cues.

An empty-beaked bird that pecks at a reachable vanilla seed or feather makes one 1-in-5 pickup roll after six close peck ticks. The bird carries exactly one item, including its stack data, and leaves any remainder on the ground. Carriers check nearby low leaf perches with the existing hop or wing-assisted path. Otherwise they trot roughly 2.4 blocks along a supported path and release the item on landing or stopping. If no route is clear, the bird tries a release from its current spot. The actual item is attached to the animated head at the beak.

Player-dropped items return on death or before ordinary retirement. A rejected item spawn leaves the one-item stack with the bird for retry. The existing `ZTO.Magpie` save ID gains an optional beak stack and gold recovery flags. Save/load keeps the item and age, then starts fresh movement. Older saves load with an empty beak.

After successful natural insertion, one roll out of 100 gives a new magpie one vanilla gold ingot. Startling a gold carrier starts a short escape and subsequent release, and killing it also returns one ingot regardless of Looting. Untouched gold can expire on ordinary retirement. The independent **Magpie Spawn Egg** uses `item.magpieEgg` (constructor ID 22011, stack ID 22267) and places an empty-beaked bird in suitable loaded Overworld space, including outside the natural habitat.

## Electric Jetpack immersion

Water reaching 1.35 blocks above the player's collision-box floor shuts down the equipped Electric Jetpack through the pinned IC2 1.97 guard. Shallow water below that sampled point permits normal operation. After 20 consecutive ticks above the waterline, propulsion returns at 48/100 wetness. The original rain and thunder behavior, inventory status, and Debug Hub stay in the existing weather service. See [water protection and owner checks](survival-022-aerial-predator.md#electric-jetpack-weather).

## Fugl predation

A solitary Fugl in a passing daytime ground encounter may select an exposed chicken after its arrival. It uses the existing Fugl approach, warning, actual dive, collision and recovery. The prey search is intermittent and limited to an 18-block box with an additional 22-block distance cap. A Fugl gets one chicken target and at most one committed dive. Roof cover disqualifies the chicken throughout pursuit and at contact. Native ray and swept collision checks provide wall protection. Aerial flocks, player hunts, lunar predators and Skelebird retain their prior behavior. Chickens are the only prey in this prototype.

## Settings and diagnostics

The existing naturalEnderkinSpawning setting controls the ordinary substitutions. The new forestMagpies and fuglChickenHunting settings both default to true and apply after restart. Disabling magpie admission leaves saved birds readable. Existing profiles get the new defaults from the settings loader. No world migration is required.

Numpad 1 opens the Debug Hub. Biome Ecology displays the player's loaded biome ID, family and blended relative weights. It also shows hostile cap and pass activity, the latest ecological selection and latest ecological rejection, the magpie admission status and loaded count at the last check, and any active Fugl prey target. A recorded replacement decision precedes vanilla insertion. The monitor does not require Dev Bridge.

## Owner playtest

- Compare repeated ordinary nights in Forest, Desert and Swampland at similar light and activity. Check whether the regional roles are recognizable and whether a desert factory still presents Saboteur opportunities.
- In a disposable save in dry daylight, find flat grass at a forest edge with one- and two-block dirt ledges nearby. Leave one clear landing and put leaves over a second approach. Watch a magpie trot and pause, hop, climb, land and turn away under the canopy. Use the Creative egg to repeat the route and return to Survival for the approach checks.
- Stand still to one side within a few blocks, then approach from the other side while sneaking. Check that the head tracks independently of the body and that a quick close approach still causes evasion. Drop a seed or feather during a pause. Review the darker plumage in open light and forest shade and the feel of the gait and wing transitions.
- Search for **Magpie Spawn Egg** in Creative and NEI. Place a bird near flat grass and a low reachable perch. Drop several seeds or feathers and let it investigate several times. When it takes one, follow the beak during a head turn and relocation, then pick up the released item. Compare the ground route with the perch route. A gold carrier is a rare natural sighting during dry forest daylight.
- Put a few chickens in an exposed pen and others behind walls and a roof. Observe passing daytime Fugl across several encounters. Review visibility of the warning, hunt frequency and whether shelter is effective in a survival build.

The weights, habitat and hunt cadence are reversible prototype defaults. Player-inventory theft, specialized calls, alternate prey and EE2 crafting remain open.

## Ordinary aerial encounters

The local Survival tick samples terrain once a second. Ordinary checks require 30 seconds of open ground exposure or a qualified open flight band. Rolls are at least 15 seconds apart. A successful admission reserves the existing shared saved cooldown. Both source switches, lunar allocation and the existing encounter and hostile caps retain their authority.

Quiet Normal ground now rolls 18/1,000 by day and 65/1,000 by night, up from 13 and 45. Ground groups remain solitary. A ground admission hunts on 46% of daytime rolls and 67% at night. Air admission odds rise by 20% in low air and 25% in exposed or high flight. Low air stays solitary. Exposed Normal flocks draw either two or three Fugls, with three favored at higher clearance and activity. Hard gets more multi-bird rolls and retains formations of up to five. Easy remains mostly solitary. A failed full placement can retry a formation one member smaller within the existing 16-site ceiling.

Nocturnal Skelebird ground selection starts at Territorial Activity 12 with 8% on Normal and 16% on Hard. Quiet Normal ground remains Fugl. Air selection at zero activity is 8%, 28% and 39% on Easy, Normal and Hard, with the existing difficulty-based activity bonuses and Skelebird group limits.

Open **Numpad 1 → Aerial Predators** for the exposure requirement, next possible ordinary check, latest ordinary attempt and the actual birds inserted out of the requested formation. The same screen labels active ordinary and lunar encounters independently.

For an egg-free check, use a disposable Survival save outside full moon, with ordinary encounters enabled and lunar aerial encounters disabled. Spend at least 30 seconds away from roofs, water and canopy on a plain. Watch for solo birds after dusk and read the last check and shared cooldown in the Debug Hub. On another run, hold open flight more than 36 blocks above surrounding terrain until the High Sky band appears. Compare quiet Normal with established Hard, then land under a roof after a warning to check retreat.

### Attack follow-through

Ordinary hunters begin target acquisition during arrival. An exposed player can remain a staging target behind a local obstacle, with a clear swept launch corridor and sight still required at warning and commitment. An obstructed ordinary hunter can search five alternate bearings per re-stage and make at most four searches within the existing hunt deadline. A candidate can approach directly or climb over local terrain before settling into its launch lane. Skelebird keeps its wider staging radius, shallow crossing, locked warning aim and forward runout. Fugl closes from above and retains its raised-wing warning and steep dive.

Ordinary birds can recover and re-stage after up to two cancelled warnings before a third cancellation ends their hunt. A cancellation releases the turn without spending the committed pass. The 64-tick flock gap, one ordinary committed pass per hunter, swept contact, damage, descent, shelter and jetpack protection remain in the original handlers. Lunar and manual birds keep their original acquisition and staging rules. Passing Fugl and its bounded chicken opportunity remain in their original path.

Compare the silhouettes on an ordinary night. For Fugl, look for the raised wings above the target and the steeper descent. For Skelebird, use established territory or exposed night flight, listen for the rattle and sidestep after the warning to see the locked shallow crossing and climb. Watch from a route with an adjacent roof or ridge to see an aborted warning or an alternate approach.

### Ordinary aerial balance sample

Run `zto-tools/test-zto-agent.ps1 -AerialSample -PackJava java` after staging. Supply `-CandidateJar <baseline jar>` with the archived `80b158e` mod to replay the same seeded scenarios. The harness runs ordinary admission and native bird ticks with lunar admission off. Trials use the same loaded terrain, time, difficulty, activity and RNG seed per scenario. It resets the check delay and saved cooldown between independent observations and lets each hunt run until first commitment, exit, or the deadline. These are probabilities conditional on eligibility, rather than an estimate of encounters per real play hour.

Each paired cell reads baseline → candidate. Groups list actual inserted sizes from one through five. Hunt stages list acquisition/warning/commitment out of the admitted hunts. Passing encounters are admissions minus hunts.

| Scenario | Check trials | Admissions | Inserted Fugl / Skelebird | Groups by size | Hunt stages / hunts |
| --- | ---: | ---: | --- | --- | --- |
| Normal day ground, activity 0 | 2,400 | 29 → 35 | 29/0 → 35/0 | 29,0,0,0,0 → 35,0,0,0,0 | 14/14/14 of 14 → 19/19/19 of 19 |
| Normal night ground, activity 0 | 2,400 | 99 → 155 | 99/0 → 155/0 | 99,0,0,0,0 → 155,0,0,0,0 | 65/65/65 of 65 → 101/101/101 of 103 |
| Normal night ground, activity 24 | 2,400 | 116 → 167 | 116/0 → 153/14 | 116,0,0,0,0 → 167,0,0,0,0 | 74/74/74 of 74 → 110/110/110 of 113 |
| Hard night ground, activity 24 | 2,400 | 128 → 171 | 120/8 → 142/29 | 128,0,0,0,0 → 171,0,0,0,0 | 81/81/81 of 84 → 110/110/110 of 114 |
| Hard night ground with ridge, activity 24 | 1,600 | 85 → 114 | 79/6 → 93/21 | 85,0,0,0,0 → 114,0,0,0,0 | 42/36/36 of 55 → 73/73/73 of 76 |
| Hard night under glass, activity 24 | 400, zero eligible checks | 0 → 0 | 0/0 → 0/0 | 0,0,0,0,0 → 0,0,0,0,0 | 0 → 0 |
| Normal low air, day, activity 0 | 400 | 133 → 166 | 133/0 → 166/0 | 133,0,0,0,0 → 166,0,0,0,0 | 98/98/98 of 100 → 122/122/122 of 127 |
| Normal exposed flight, day, activity 0 | 400 | 137 → 176 | 185/0 → 298/0 | 89,48,0,0,0 → 96,38,42,0,0 | 109/109/109 of 112 → 154/154/154 of 155 |
| Normal high sky, night, activity 0 | 400 | 136 → 174 | 242/30 → 254/52 | 68,0,68,0,0 → 93,30,51,0,0 | 114/114/114 of 119 → 146/146/146 of 150 |
| Hard high sky, night, activity 0 | 400 | 146 → 187 | 289/46 → 339/73 | 83,0,0,63,0 → 112,0,0,75,0 | 120/120/120 of 125 → 154/154/154 of 158 |
| Hard high sky, night, activity 24 | 400 | 197 → 254 | 493/212 → 568/342 | 37,0,66,0,94 → 36,0,108,0,110 | 187/187/187 of 187 → 244/244/244 of 245 |

The rolls accounted for every eligible miss in the repeated sample. In the ridge fixture, the large remaining losses were before acquisition and launch. With tuning alone, 55 of 76 hunts committed there. Alternate lanes increased that to 73 of 76 while preserving zero admissions under glass. In the open night-air sample, established Hard had 126 Skelebird groups on 254 admissions, up from 80 on 197, and five-bird Fugl formations increased from 94 to 110. Established ground observed 14 of 167 Normal and 29 of 171 Hard Skelebird encounters. A separate controlled formation fixture blocked all eight three-bird attempts, then observed two actual insertions on the ninth site. Controlled fixtures also exercise cap rejection, unavailable space, warning cancellation, a later flock member's pass, and protected jetpack behavior.

## Verification

- Portable checks passed for configuration, lunar calendar, assault and patrol schedules, outdoor approaches, and current documentation.
- MCP 6.2 with JDK 8 compiled and reobfuscated the client. The staging build passed streaming validation of all 412 entries. The gameplay source is commit 3c730e695535e9d4197421ed6e66d963a281250b. The build sidecar reports a dirty workspace because documentation and test changes and unrelated local files were present at build time. Mod SHA-256 is 4c8472ffae33e2473ea9944503da396aebcdd8cd1fa7bf85f9ef41eeca1902d2. Agent SHA-256 is 35e8fc1ec25c552afd401890d4d444e63227d9ca1abfd1ffb5ddebf026613060.
- The pinned Tekkit client classpath passed the headless pack suite with Forge 3.3.8.164 and Java 8u511. A fixed-seed selection fixture used 20,000 candidates per biome. Each of the 11 ordinary test biomes yielded 756 replacements. Forest yielded 274 Breachers and 223 Thieves, Desert yielded 459 Skirmishers, and Swampland yielded 309 Thieves. Extreme Hills yielded 55 Tanks. Both mushroom profiles yielded zero substitutions. The fixture also checked boundary blending and unloaded sampling.
- The pack fixture verified daytime forest admission, the magpie admission cap, walking, hopping, pecking, short flight, landing, a leaf perch, evasion, distance despawn, native entity NBT and the packaged atlas. It observed a Fugl damage an exposed chicken while leaving the roofed and subsequent chicken unharmed. Existing pack probes passed for ordinary hostile pressure, Enderkin abilities and theft, Skelebird, native IC2 weather behavior, lunar aerial authority and replay, and installed compatibility hooks.
- Graphical playtesting and full milestone acceptance remain pending. This campaign used headless disposable worlds and staged the build separately from the interactive Tekkit Dev profile.

### Immersion and movement follow-up

- Ran `zto-tools/test-config.ps1`, `zto-tools/test-lunar-calendar.bat`, and `zto-tools/test-agent-docs.ps1`. Built via `zto-tools/build-and-deploy.bat` with `ZTO_PRISM_MODS` pointing to `dist-zto/biome-ecology-staging/mods`. Used JDK 8u504 and MCP 6.2 with development Forge 3.4.9.171. Pack checks use `zto-tools/test-zto-agent.ps1 -Pack -PackJava java` under Java 8u511 and the pinned Minecraft 1.2.5 / Tekkit Classic 3.1.2 overlay.
- The headless magpie fixture landed at Y=5 on a one-block rise and Y=6 on a two-block rise from Y=4, peaking 0.18 blocks above each landing. It redirected on the floor within 16 ticks from the start of each blocked fixture. The flat route exercised both halves of the alternating stride and a pause with no gait advance. The probe also preserved seed and feather stacks, observed attention and evasion, and reloaded the saved bird.
- The Electric Jetpack fixture measured the same 0.298-block rise over five swimming ticks with and without the submerged pack. The propulsion cost was zero in normal and hover modes. The probe also exercised dry flight, rain and thunder, underwater re-equip and reload, restart interruption, resumed thrust and native fuel controls. [Jetpack results](survival-022-aerial-predator.md#verification)
- The first combined staged candidate used source `99d2db5eb8a8fec1bd944725d674c7ef02014309` plus the working-tree magpie changes and passed the full headless pack suite on rerun. Mod SHA-256 `e09a655e2d8ac8e1bba18bd0f2bf4d5ef1b2f25c33b19029a7f6d391a00ed9a8`. Agent SHA-256 `b03d1022845eae83cc9a2b5fbd9d30c914efa544a19b4486c1dcba5cc2394635`. The first suite attempt stopped at the existing natural hostile-density assertion. Gait readability, the plumage in forest shade, and the feel of takeoff, landing and perching transitions remain owner playtest checks.

### Scavenging and egg verification

The pinned pack probe exercises single and split native stacks with identifying NBT, disappearing items, ground and leaf releases, carrying across save/reload, rejected release and death retry, and both distance and age retirement. Forced natural matches and misses check that only successful insertions roll, that escape and Looting III death each return one ingot, and that untouched gold can expire. The egg fixture checks the registry collision diagnostic, localization, the native Creative catalog used by the NEI creative scan, safe and blocked use, consumption and scavenging after hatch. The in-game beak alignment, readability in flight, and NEI search panel remain owner checks.
