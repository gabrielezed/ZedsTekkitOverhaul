# Abyssal ecology

The open ocean supports rare encounters with familiar undead. The deep Trench favors predators. An Observatory discovered under layout revision two has a finite enderic population. Diving gear, Tekkit armor and player-made refuges give different ways to explore these habitats.

| Display name | Native registration | Habitat and role |
| --- | --- | --- |
| Abyssal Fangfish | `ZTO.AbyssalGuardian` | Deep ocean predator, with revision-one Observatory compatibility |
| Diver Zombie | `ZTO.DiverZombie` | Aquatic melee undead in oceans and abyssal margins |
| Diver Skeleton | `ZTO.DiverSkeleton` | Aquatic archer in the same habitat |
| Abyssal Enderkin | `ZTO.AbyssalEnderkin` | Finite revision-two Observatory patrol |

FML assigns available runtime IDs. Each variant saves under its own native name. The diver arrow is registered as `ZTO.DiverArrow` for rendering and native entity identity. Its short flight is transient. The art and animation of the original Fangfish stay available under their original resource names. It has 20 HP, an 18-block detection radius and a 4 HP Normal lunge. Its original wind-up and recovery give a diver time to move or take cover. [Fangfish implementation](abyssal-guardian.md)

The undead inherit the classic Zombie and Skeleton classes. The attached mask, lenses, mouthpiece, tube, harness, back cylinder and fins are cosmetic model parts drawn with the current limb transforms. Their original mob textures, sounds, drops, health and armor rules remain in use. Their gaits use alternating fin kicks. The Zombie uses 3 HP Normal melee and closes slowly through loaded water. The Skeleton swims to a 3.25–6.5-block firing position, draws its visible bow for 10 ticks and uses a 46-tick cooldown. Its arrows retain the native 0.8 water drag and damage calculation. A 2.15-block launch speed and close firing range give useful underwater reach. The dedicated arrow guard limits flight to loaded chunks and at most 30 ticks.

The shared swimmer derives width and height from each creature's current collision box. It first sweeps its clearance through water, tries short local alternatives, and uses a cached path of at most 18 steps and 192 expanded nodes. Native collision resolves actual movement. A full-height diver requires more clearance than a Fangfish or Enderkin. Each species keeps its own targeting, attack and recovery behavior. Land Zombies, Skeletons and player arrows keep their native handling.

A combined ecological pass runs every 320 ticks. Its one-in-seven Trench ticket or one-in-ten ordinary ocean ticket can produce a solitary predator or one to two divers. Fangfish favor Trench water at or below Y42 and have a local cap of two. Divers favor Y28–56, with Zombies weighted twice as often as Skeletons per selection. The combined local cap is four within a 48-block scan, and native hostile pressure can reject an encounter. No ecological insertion occurs in an admitted Observatory and its 12-block buffer.

Use `-Dzto.dev=true` and **Numpad 1 → Biome Ecology → Observatory** for the layout revision, ledger, nearest aquatic actor and local block, metadata and block light. A save marked `.zto-disposable-test` also exposes a four-species selector and fixture spawn button. Manual actors require clear submerged positions, are capped locally, and leave no persisted actor or Observatory slot.
