# Abyssal Trench world generation

Survival 0.5.1-alpha.2 gives the Motorboat and diving equipment an expedition destination. The native ocean surface conceals deep shelves, fossil outcrops, sparse light and fragments of masonry. A site can use a stable local anchor and sample the continuous terrain for future placement.

## Biome and geography

`biome.abyssalTrench` defaults to 40. The valid byte-backed registry range is 0–254 and startup rejects occupied slots. The config loader appends the key to existing files. Keep the same ID for an existing world. The biome uses Ocean climate, water color, decoration and spawn policy, including squid, no passive land spawn list and native hostile spawn rules. ZTO treats the registered ID as Ocean for ecology. The sea floor relies on normal native spawn restrictions. Only the pinned default Overworld surface provider generates Trench terrain.

Two independent 184- and 157-block value fields displace the sample coordinates by up to 62 blocks per axis. The warped geography blends 576-, 229- and 112-block fields at weights 0.67, 0.23 and 0.10. Influence ramps smoothly between 0.64 and 0.78. This world-space field can fork and join across any chunk boundary. Seeded value noise uses smooth interpolation between hashed lattice samples and floor-based coordinates.

Each affected column must pass the full-resolution native Ocean mask and have a submerged geological floor with water at Y62 and air immediately above. Shallow ocean floors are eligible when those checks pass. A 20 × 20 quarter-resolution native biome sample finds nearby non-Ocean columns and attenuates influence over the 8–28 block distance band. The native biome manager samples terrain-free GenLayers. Land, river, beach and mushroom-shore columns keep their original blocks and biome.

The depth calculation blends the native floor toward a separate relief field. A folded 138-block field creates channels, a 91-block field adds ridges, and 364-, 43- and 13-block fields vary basin depth, shelves and rock detail. Relief can target Y12–46 and can only lower the native floor. The perimeter preserves most of the native relief. Several corridors, ridges and low points can occupy the same connected region. Native seawater remains at Y62. Saved Trench membership follows actually lowered columns, and the vanilla biome layers continue to supply Ocean climate and ancestry.

## Runtime order

The pinned 1.2.5 client provides `aly.b(II)Lack;`. Tekkit's overlay supplies the extended Chunk implementation. The ZTOAgent checks the SHA-256 of the effective provider and both instruction sites before installing the patch. Missing or unsupported hooks keep native generation active and emit a diagnostic. No base game archives are modified.

1. Native provider computes low-resolution biome-weighted noise and fills a legacy byte buffer. Native high-resolution biomes drive surface replacement and native bedrock placement.
2. ZTO samples raw native density for the owned chunk and a 16-block halo with an independent native provider. It never calls a chunk loading or population entry point. A bounded 64-entry, weak world-keyed cache retains only raw arrays. The plan computes continuous influence and relief, validates geological cuts and applies a two-pass height envelope before writing vanilla source water. A preserved floor attenuates nearby cuts at four blocks of rise per cardinal step or six per diagonal step. The halo exceeds the reach of any constraint between Y12 and Y62. The envelope can only reduce excavation. Existing native cliffs are preserved.
3. Native caves and ravines run. Feature-enabled worlds run the mineshaft, village and stronghold preparation calls. The constructor converts the base byte buffer to sections and writes native biome bytes.
4. ZTO persists the Trench biome in shaped columns and coats eligible native substrates. The coating whitelists stone, dirt, grass, sand, sandstone and gravel. It skips interrupted surfaces and stops at the first different substrate. Exposed underwater cliff columns bordering a cut receive a shallow Stone or Shellstone face treatment. Sediment remains a shelf and basin material. Formations and marine organisms only occupy present source water in the owned chunk. The extended section setters write the full integer block ID and metadata. The direct marine entry point reuses Part I's occupied-water metadata and requires opaque abyssal support, a fully submerged column and wet adjacent cells. No world setter is called on a pending chunk. Low block light is seeded locally and the native skylight pass builds height, sky light and dirty state.
5. Once neighboring chunks are available, native population writes structures, lakes and dungeons, decorates the biome, performs passive world-generation spawns and checks icing and snow. Then FML invokes its registered generators. The local catalog shows IC2 dispatching its ore and rubber tree generator, RedPower dispatching ore, marble, volcano and rubber tree generation, and BuildCraft dispatching `OilPopulate`. Their generation can consume remaining native stone and may intersect ZTO terrain. The shallow coating leaves most native stone volume below the new floor. ZTO does not perform repairs after population.

Disk loading bypasses both callbacks, including for older unpopulated chunks. A preexisting chunk may still receive ordinary native or mod population work. Old terrain can meet new deep terrain at explored boundaries. Changes to normal world generation or the ZTO generator can also introduce seams across existing boundaries. Marine cells retain Part I's exposed-water-edge rendering limitation, so initial plants are inset one cell from chunk edges and validated against surrounding water. Test a visual transition in a fresh save.

## Habitat and density

- Abyssal Stone coats two to four shallow layers. Shellstone favors 12- and 97-block fossil fields. Silt favors low or gently sloped terrain and combines 24- and 68-block sediment fields.
- A 34- and 118-block woodland field favors Kelp on shelf floors at Y23–43. A separate 9-block colony field favors Fan Coral and Tube Sponge over rock and mostly Ribbon Algae over sediment. Fan orientation uses a seed bit.
- Fossil outcrops use world-space anchors jittered on a 23-block grid. A 109-block habitat field varies their frequency. Individual anchors vary in orientation, footprint, relief and branch shape. Each chunk evaluates the portion within its own footprint and uses its own rooted seabed and water cells. Small masonry traces fit within one owned chunk.
- The deepest areas favor open water with sparse Ghost Anemones and Abyssal Lamps. The lighting writes stay inside the owning chunk. The marine inset remains one block because the pending chunk cannot safely inspect a neighboring chunk's water or support.

## Developer locator and walkthrough

Launch the isolated Tekkit Dev instance manually with the managed agent and `-Dzto.dev=true`. Use **Numpad 1 → Biome Ecology → Locate Trench**. Each click searches a 97 × 97 lattice of potential local anchors at 64-block spacing, up to roughly 3 km per axis. A qualifying location has strong influence, low target relief and an eligible ocean mask. The displayed position is a planned dive site. Its seed-and-coordinate hash identifies the anchor without enumerating the connected biome. It does not load or generate chunks and does not move the player. The regular Biome Ecology panel shows the current saved biome.

1. Create a disposable Default Overworld with seed `32021`. Sail toward **the planned dive site listed in the evidence table**. Approach through the ordinary Ocean visible on the membership map to observe the transition.
2. Equip the Underwater Mask, charged Oxygen Tank and Flippers. Descend through the outer rock and look for a Kelp shelf, Fan Coral, Tube Sponge and fossil formations. Continue toward the dark central valley. Inspect how darkness separates individual luminous organisms.
3. Check that seams, open air and bedrock are absent across the new ocean surface. Harvest and replant one organism, return to the Motorboat and save and reopen the world. Record underwater graphical acceptance separately.

## Evidence

Run `zto-tools/test-zto-agent.ps1 -Focus trench -TrenchSeed 32021` for the pinned native provider and disk fixture. The comparison maps are under `dist-zto/trench-refinement/before/<seed>` and `after/<seed>`. Each sample spans 784 × 784 blocks around that model's planned site. It includes saved biome membership, seabed height, exposed habitats and two orthogonal native sections through the anchor. These maps show generated blocks, outside the visual client's fog and lighting.

| Seed | Previous dive X, Z | Refined dive X, Z | Trench share of sampled ocean before / after | Mean affected chunk before / after |
| --- | --- | --- | --- | --- |
| `32021` | -1948, 269 | -952, 840 | 5.95% / 56.01% | 2.48 / 2.26 ms |
| `32022` | 1285, -1113 | 520, 648 | 4.88% / 43.90% | 2.31 / 2.09 ms |
| `-32021` | -2791, 1294 | -1016, 1736 | 9.62% / 31.88% | 2.26 / 2.30 ms |

Coverage is local to locator-selected windows. It is not a global frequency estimate. The native sample includes 2,401 chunks per window and checks 729 in reverse order. Background chunk means ranged from 1.90 to 2.12 ms across both models. These timings share the same fixture and include native generation and warm-up. The refined samples contain 5,673–22,613 seabed columns at Y25 or lower. They generated all five marine organisms across the three seeds, both brick variants and Petrified Coral. Naturally generated lamps were not observed in these windows.

### Continuity acceptance after owner report

Fresh reproduction confirmed that the previous Y58 cutoff could keep a Y59 column intact alongside a deep cut at Y19–22. The jumps were already present in the shaped byte buffer before native caves and ravines. No mixed revision was required to reproduce them. The screenshot has no embedded coordinates or revision history. The nearby Trench 01 player save was inspected read-only. Previously saved terrain keeps its original geometry, including any seam between revisions.

The continuity probe compares 784 × 784 block windows on seeds 32021, 32022 and -32021. It counts edges with a resulting step of at least 12 and an excavation-induced increase of at least eight blocks, and isolated residual columns at least 12 blocks above all four neighbors. Old → revised edge totals were 406 → 0, 1231 → 0 and 620 → 0. The largest added step changed from 37/39/35 to four. The native shape before the cut, applied cut, and final surface are the RGB channels of `height-stages.png`, each multiplied by four. The field plan uses RGB for geographical influence, coast weight and unconstrained target Y multiplied by four. Red/orange in `eligibility.png` denotes rejected overhead or shallow columns, magenta marks isolated residuals and blue marks shaped seabed. In `height-deltas.png`, cyan intensity is the cut depth multiplied by six and red intensity marks changes after shaping.

Recreate seed `32021` in a new Default world and inspect the old defect area near X -965, Z 796 at Y27, facing south or toward the adjacent wall. The planned dive anchor remains X -952, Z 840. For a fixed view of a second defect, use seed `32022` near X 461, Z 456 at Y25. Compare the same views with the previous and current artifacts in separate disposable worlds.

A separate population sample exercised the native client provider and registered IC2, RedPower and BuildCraft generators. Copper and RedPower ores remained present below the coated floor. A controlled BuildCraft medium-deposit ticket exercises oil intersecting the generated terrain. Native structures were enabled in the provider fixture, but intersections with individual structure types have not been exhaustively sampled.

Graphical owner acceptance is pending. The submerged observatory, its creatures, the Custodian and Ender Pearl progression belong to later revisions.
