# Survival 0.2.2-alpha.5 — Skelebird

Skelebird is a nocturnal, undead aerial hunter for the local Overworld in Tekkit Classic 3.1.2. Its open ribs, narrow skull and hinged jaw sit on a long spine with a segmented tail. Jointed arms and swept spars form the wings. The original 64×64 atlas draws on the 1.2.5 Skeleton's `#989898` bone gray, `#888888` shadows, `#CACACA` highlights and `#494949` cavities. The skull and ribs retain visible gaps. The renderer uses flight pitch and turn for banking. The model cycles jointed wingbeats, a nearly still glide, an asymmetric circle, raised wing spars and an open jaw during the warning, a swept crossing pose, unfolding recovery and a collapsing fall.

## Combat and counterplay

Fugl aims through its warning and stops on contact. Skelebird lines up from farther out and closer to the pilot's altitude. It locks its aim at the start of a 24-tick dry rattle and checks the corridor again before accelerating. The pass travels at a shallow downward angle. It crosses the original target position and runs out ahead even after a hit, then climbs for 62 ticks. The first swept contact can deal five health points before armor. Skelebird has 14 health, uses ordinary damage rules including projectile damage, and counts as undead.

A lateral change while the warning is playing can defeat the pass. Descent breaks an airborne hunt, and overhead cover, including glass, interrupts acquisition and pursuit. Cover and a blocked corridor can cancel a warning. Jetpack fault protection uses the existing aerial rules. Manual birds can spend two passes against an exposed pilot. A ground hunt or an encounter member spends one. Manual ground hunts require Hard, night and a rare detection roll. Range, sight, missed approaches and time all bound a hunt.

Skelebird drops zero to two vanilla bones with the standard living-entity loot roll. The same path applies native Looting and Forge drop handling.

## Shared encounter rules

The existing Overworld admission pulse first checks exposure, difficulty, Territorial Activity, cooldown and room under the hostile caps. On a successful ordinary admission between world times 12000 and 22999, species selection uses the following shares. Sampled activity is capped at 24 for these calculations.

| Exposure | Easy | Normal | Hard |
| --- | --- | --- | --- |
| Flight | 6% plus one point per 4 activity | 20% plus one point per 2 activity | 30% plus one point per 2 activity |
| Ground | Fugl | Fugl | 8% at activity 12 or above |

Easy and Normal Skelebirds arrive individually. Ordinary Hard Skelebirds can form groups of up to three at activity 12 or above, subject to the existing flight band and flock roll. A group contains one species and shares an attack turn with a 64-tick gap after release. Alternating lanes circle in opposite directions. Group members take elevated positions, make separate passes and leave together if the encounter ends.

A selected natural full-moon **PLAYER_ASSAULT** uses a stable species roll for its existing aerial slots. Skelebird takes 50% of selected Normal air allocations and 72% on Hard. Normal can allocate one air slot. Hard can allocate up to two at elevated activity, subject to the original budget and caps. The director still returns unspent air slots to the ground plan. Both species count toward the existing limit of five loaded predators at natural admission and share the ordinary hostile cap checks and saved cooldown. Lunar scheduling, Territorial Activity and future settlement ownership stay with their existing authorities.

## Items, saves and diagnostics

Find **Skelebird Spawn Egg** in Creative search or NEI. Its configurable `item.skelebirdEgg` constructor ID is 22010 and its default stack ID is 22266. The registered save ID is `ZTO.Skelebird`. Fugl uses `ZTO.AerialPredator` and its existing egg. Old worlds require no migration. On reload birds keep identity, position and health. Admitted birds keep a short retirement lifetime. Warning, commitment, target and group are session state.

`naturalAerialEncounters`, `lunarAerialEncounters` and `aerialEncounterCooldownSeconds` apply to both species. Egg placement uses the existing Overworld and difficulty checks. Open **Numpad 1 → Aerial Predators** for altitude, target, current state, phase and recovery time, attack turn, group roster and recent launch rejection.

The six original sound clips are two ambient rattles, a warning, the air and bone rush, a hurt crack and a death rattle. Filtered noise excites short hollow resonances with irregular impacts. Regenerate the positional mono 22.05 kHz PCM WAVs with `node zto-tools/synthesize-aerial-sounds.cjs --skelebird`.

## Verification and owner playtest

Run `zto-tools/test-config.ps1`, build with `zto-tools/build-and-deploy.bat`, then run `zto-tools/test-zto-agent.ps1 -Pack`. The pack probes use pinned Minecraft, Forge and IC2 classes and disposable worlds. They cover both registry names and egg placement, atlas and WAV loading, shallow movement, warning and impact, dodge and shelter, recovery, ground hunting, difficulty, Territorial Activity, flock turns, the shared cap, native NBT and full-moon allocation.

### Development evidence

- Configuration and lunar calendar, assault, patrol, outdoor approach and Enderkin selection checks passed.
- MCP 6.2 compiled and reobfuscated feature source `e074b0015d5c45c301a66fc286c62241d2bc3bc0` with JDK 8u504. The streaming ZIP checker validated 402 entries. The provenance sidecar records the three pre-existing untracked design files. The mod SHA-256 was `00136770f664647cf696e1d6ef4779c49245d752eff89595e9ba43998847f3cb` and the agent SHA-256 was `f364d81f2c170289db700669ce9bb5033adb6dc443b679ca30ad0affd5a259fb`.
- The full headless Tekkit Classic 3.1.2 run passed with Forge 3.3.8.164 and IC2 1.97. The Skelebird probe observed a five-health hit and forward runout, a warning-time miss, blocked pursuit under glass, arrow damage and a fall on death. Hard ordinary flocks had three members and at least two consecutive commitments with a single attack turn. An elevated Hard full moon allocated two Skelebirds from the original budget. Regression probes covered Fugl, the Electric Jetpack, ordinary spawning, Enderkin and ground waves. An initial run stopped at the existing ordinary-spawn density assertion. A fresh full run measured 80 quiet and 134 occupied hostiles and passed.
- A focused follow-up on the same packaged candidate also checked default item enumeration through the legacy Creative API.

### Owner playtest

Balance, distant silhouette, NEI browsing, joint motion in the game camera and audible character still need owner play. In isolated Tekkit Dev with a disposable save:

1. Search for both eggs in Creative and NEI. Place a Skelebird outdoors at night, switch to Survival and fly high enough to expose the pilot. Compare its silhouette from below and from the side with Fugl.
2. Listen for the rattle and watch for the raised wings. Stay in path for one crossing. On the next warning, strafe across the line or descend into a roofed landing. Fire an arrow during the climb.
3. Compare an ordinary night on Easy and Normal with a high-activity Hard area. Observe spacing and staggered warnings. Save during a committed hunt, reopen, then inspect the cooldown and absence of an active pass.
4. Cross an exposed full-moon dusk on Hard. Compare with a roofed entrance and verify that the ground waves continue. Check the weather-sensitive Electric Jetpack with both species present in separate encounters.
