# Abyssal Trench world generation

Survival 0.5.1-alpha.2 gives the Motorboat and diving equipment an expedition destination. The native ocean surface conceals deep shelves, fossil outcrops, sparse light and fragments of masonry. The model exposes stable regional identity, orientation, footprint and depth for future site selection.

## Biome and geography

`biome.abyssalTrench` defaults to 40. The valid byte-backed registry range is 0–254 and startup rejects occupied slots. The config loader appends the key to existing files. Keep the same ID for an existing world. The biome uses Ocean climate, water color, decoration and spawn policy, including squid, no passive land spawn list and native hostile spawn rules. ZTO treats the registered ID as Ocean for ecology. The sea floor relies on normal native spawn restrictions. Only the pinned default Overworld surface provider generates Trench terrain.

One seed-derived ticket per 768 × 768 block grid cell admits roughly one third of the cells. Negative coordinates use floor division. Centers sit at least 230 blocks inside their cell and the footprint stays inside its owning cell, so candidates cannot overlap. A 16-block lattice tests the full-resolution native Ocean mask across the curved footprint and a margin of 12% of its radius. All sampled points must be ordinary Ocean. Each affected column must independently pass the native Ocean mask and have a submerged geological floor. The locator and terrain reuse the same region model and ocean test. A weak world-keyed cache retains up to 128 suitability decisions per world.

The regional salt separates admission from the identity hash and feature fields. Seeded axes span 240–420 by 100–180 blocks before variation. A sine bend and 42-block width field vary the outline. The central floor targets Y12–20 with 29- and 8-block undulations. A smoothed ramp blends from the valley into the native floor, with irregular shelf variation across the Y25–40 band and a gradual margin whose width depends on the cross-section. Native seawater remains at Y62. Classification in saved chunks follows columns actually lowered by the terrain phase. Native GenLayer samples and cached manager-only queries continue to supply Ocean ancestry and climate. Queries through the native world and chunk biome lookup use the persisted Trench identity.

## Runtime order

The pinned 1.2.5 client provides `aly.b(II)Lack;`. Tekkit's overlay supplies the extended Chunk implementation. The ZTOAgent checks the SHA-256 of the effective provider and both instruction sites before installing the patch. Missing or unsupported hooks keep native generation active and emit a diagnostic. No base game archives are modified.

1. Native provider computes low-resolution biome-weighted noise and fills a legacy byte buffer. Native high-resolution biomes drive surface replacement and native bedrock placement.
2. ZTO blends eligible sea floors down and fills the removed geological volume with vanilla source water. The per-chunk plan records the shaped columns through the remaining provider calls.
3. Native caves and ravines run. Feature-enabled worlds run the mineshaft, village and stronghold preparation calls. The constructor converts the base byte buffer to sections and writes native biome bytes.
4. ZTO persists the Trench biome in shaped columns and coats eligible native substrates. The coating whitelists stone, dirt, grass, sand, sandstone and gravel. It skips interrupted surfaces and stops at the first different substrate. Formations and marine organisms only occupy present source water in the owned chunk. The extended section setters write the full integer block ID and metadata. The direct marine entry point reuses Part I's occupied-water metadata and requires opaque abyssal support, a fully submerged column and wet adjacent cells. No world setter is called on a pending chunk. Low block light is seeded locally and the native skylight pass builds height, sky light and dirty state.
5. Once neighboring chunks are available, native population writes structures, lakes and dungeons, decorates the biome, performs passive world-generation spawns and checks icing and snow. Then FML invokes its registered generators. The local catalog shows IC2 dispatching its ore and rubber tree generator, RedPower dispatching ore, marble, volcano and rubber tree generation, and BuildCraft dispatching `OilPopulate`. Their generation can consume remaining native stone and may intersect ZTO terrain. The shallow coating leaves most native stone volume below the new floor. ZTO does not perform repairs after population.

Disk loading bypasses both callbacks, including for older unpopulated chunks. A preexisting chunk may still receive ordinary native or mod population work. Old terrain can meet new deep terrain at explored boundaries. Changes to normal world generation or the ZTO generator can also introduce seams across existing boundaries. Marine cells retain Part I's exposed-water-edge rendering limitation, so initial plants are inset one cell from chunk edges and validated against surrounding water. Test a visual transition in a fresh save.

## Habitat and density

- Abyssal Stone coats two to four shallow layers. Locally coherent Shellstone increases around rocky material clusters. On gentler slopes, a 24-block sediment field groups Silt across shelves and depressions.
- A 19-block forest field favors Kelp on Y23–41 shelves. Selected eligible columns have a 30% planting ticket and a 20/35/30/15% distribution for one through four segments. A separate 9-block colony field groups Ribbon Algae, Fan Coral and Tube Sponge. Fan orientation uses a seed bit.
- One deterministic formation ticket per chunk gives eligible interiors a 17/128 chance of an irregular Petrified Coral mound or pillar and a 2/128 chance of low weathered masonry. Anchors use 5 × 3 or smaller owned footprints and grow only from locally coated surfaces. There is no loot or persistent structure state.
- Deep water stays largely open. Eligible Y20-or-deeper anchors have a 2/8 ticket for one Ghost Anemone. A smaller subset of the remaining tickets can convert an exposed Stone cell to an Abyssal Lamp. Both use native light levels, 3 and 5 respectively.

## Developer locator and walkthrough

Launch the isolated Tekkit Dev instance manually with the managed agent and `-Dzto.dev=true`. Use **Numpad 1 → Biome Ecology → Locate Trench**. Each click searches at most 81 grid cells near the player and reports a planned center, not a confirmation that old chunks regenerated. It does not load or generate chunks and does not move the player. The regular Biome Ecology panel shows the current saved biome.

1. Create a disposable Default Overworld with seed `32021`. Sail toward **X -1948, Z 269**. The tested footprint is approximately 262 × 142 blocks. Approach from the east to follow the ordinary ocean into the margin.
2. Equip the Underwater Mask, charged Oxygen Tank and Flippers. Descend through the outer rock and look for a Kelp shelf, Fan Coral, Tube Sponge and fossil formations. Continue toward the dark central valley. Inspect how darkness separates individual luminous organisms.
3. Check that seams, open air and bedrock are absent across the new ocean surface. Harvest and replant one organism, return to the Motorboat and save and reopen the world. Record underwater graphical acceptance separately.

## Evidence

Run `zto-tools/test-zto-agent.ps1 -Focus trench` for the pinned headless provider and disk fixture. Inspect `dist-zto/trench-previews/depth.png` and `habitats.png`. These are top-down maps of native generated chunks with north at the top. Depth shades lighter for shallower floors. Habitat hues distinguish exposed material or marine occupants. Neither image simulates underwater fog or gameplay lighting.

A three-seed sample examined 121 cells per seed. Seeds `32021`, `32022` and `-32021` admitted 41, 51 and 46 tickets and yielded 3, 20 and 16 eligible candidates respectively. Ocean coverage drives the variation. The seed 32021 preview includes 25,482 shaped columns, with 4,756 seabed columns at Y20 or lower. The sample generated both brick variants, all major geological formations and all five marine types. No Abyssal Lamp happened to occur in that footprint. The direct provider probe compared 729 chunks in forward and reverse order, matched representative land and ordinary ocean chunks against native output, and reloaded a neighboring old chunk with a marker block and a chest inventory. Approximate headless provider means were 1.8 ms per affected chunk and 1.5 ms for surrounding chunks in one run, including native work and warm-up. These values are comparative observations of one fixture.

A separate population sample exercised the native client provider and registered IC2, RedPower and BuildCraft generators. Copper and RedPower ores remained present below the coated floor. A controlled BuildCraft medium-deposit ticket exercises oil intersecting the generated terrain. Native structures were enabled in the provider fixture, but intersections with individual structure types have not been exhaustively sampled.

Graphical owner acceptance is pending. The submerged observatory, its creatures, the Custodian and Ender Pearl progression belong to later revisions.
