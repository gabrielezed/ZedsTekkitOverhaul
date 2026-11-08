# Phase 1 · Tekkit Classic EMC integration audit

Baseline is main at `6bf9f1e`. The isolated Prism Tekkit Dev client reported Minecraft 1.2.5, FML 2.2.106.176, Forge **3.3.8.164**, and 44 loaded mods. FoxyRetroMDK targets Forge 3.4.9.171. The historical design notes were absent from the workspace and its enclosing MDK tree. The source already registers the distillation structure, four placeable liquids, and BuildCraft transport identities.

## Installed bytecode

| Surface | Verified signature or behavior |
| --- | --- |
| EE2 startup | `mod_EE.load()` initializes items and blocks and calls `EEMaps.InitAlchemicalValues()`, which also calls `InitModBlockValues()`. On a game tick, EE2 runs external add-on initializers and then sets public static boolean `EEBase.externalModsInitialized`. The installed IC2 add-on writes its own values independently of ZTO's disabled IC2 experiment. |
| Exact lookup | `public static int EEMaps.getEMC(int, int)` reads `alchemicalValues[id][metadata]`, returning 0 for missing keys. `getEMC(int)` looks up metadata 0. |
| Stack lookup | `public static int EEMaps.getEMC(ItemStack)` accepts an exact nonzero value. On zero or absence it uses a positive metadata-0 value. A damageable stack uses the remaining durability ratio via float multiplication and truncation to int. |
| Writes | `public static void EEMaps.addEMC(int, int, int)` overwrites the exact key and reports zero. The public static `HashMap alchemicalValues` is a nested integer map. Removing a variant permits fallback. Removing an entire ID suppresses lookup and fallback. Negative entries are unsafe because some consumers test nonzero. |
| Tablet | `TransTabletData` retains learned item identities and current and latent EMC. `ContainerTransmutation` and slot logic query EMC during input and selection. Fuel lock and Klein Star special cases remain separate. A learned entry can persist even when it is filtered from available outputs. |
| Condenser | `TileCondenser` resolves target and input costs during operation. It has its own exact-then-base target path, special items, and integer energy accounting including a scale of 80. |
| Collector | `TileCollector.getFuelLevel(ItemStack)` reads EE2 live. `getNextFuel` has an explicit progression and checks `fuelItemRegistry`. Its cost is the difference between live output and input values. The registry and collector ladder require separate verification when changing fuel EMC. |
| NEI | EE2's `ee.client.nei.EETooltipHandler` queries `getEMC(ItemStack)` on tooltip display, with separate item-specific and formatting conditions. |
| IC2 Generator | `TileEntityGenerator.gainFuel()` uses `TileEntityIronFurnace.getFuelValueFor(ItemStack)` and divides by 4 with integer truncation. `TileEntityBaseGenerator` produces configured EU per active burn tick while there is storage capacity. |
| BuildCraft | `API.ironEngineFuel` maps liquid block IDs to `IronEngineFuel(int,int,int)` entries with `fuelId`, `powerPerCycle` and `totalBurningTime`. `EngineIron.fill` checks registry membership. `burn` takes 1 mB and uses integer `totalBurningTime / 1000` for that amount. Generated energy and energy delivered to a receiver differ due to buffering, engine state and cooling. |

ZTO resolves EE2 fuels by reflective item fields and exposes furnace ticks through ModLoader. The 5×5 recipe manager matches exact IDs and metadata and is separate from ordinary crafting. The Foundry recipe consumes 16 smooth stone. The 2×2 vanilla recipe consumes four workbenches for the 5×5 table. The disabled ZTO IC2 EMC experiment is disconnected from the entry point. The optional EE2 module keeps one writer for ZTO defaults and profile rules.

## Reproducing evidence

- Run JDK 8 `javap -p -c -cp <EE2 JAR>` on `mod_EE`, `ee.EEMaps`, `ee.EEBase`, `ee.TransTabletData`, `ee.ContainerTransmutation`, `ee.TileCondenser`, `ee.TileCollector`, and `ee.client.nei.EETooltipHandler`. The production ItemStack name is `aan`. In the IC2 JAR inspect `ic2.common.TileEntityGenerator`, `ic2.common.TileEntityIronFurnace` and `ic2.common.TileEntityBaseGenerator`. With BuildCraft core and energy on the classpath inspect `buildcraft.api.API` and `buildcraft.energy.EngineIron`.
- Enable [audit mode](emc-profile.md) and open a disposable single-player world. The TSV records the effective and direct values separately. Advanced Crafting Table ID 3000 and Foundry ID 3001 resolved to 128 and 16. Save and Quit and reopening the same world worked. The two baseline exports had identical SHA-256 hashes.
- To reproduce the limitation of a metadata-only zero, compare a positive `durability` row with its metadata-0 row. The worn stack has no direct mapping and receives a positive value. The bytecode takes that same fallback branch for a stored zero. In APPLY mode, `DISABLE` on that worn metadata is rejected while its positive base exists. Whole-ID removal is available without a code hook.
- Oil Bucket is classified as a fuel by the installed EE2 add-on. A live APPLY trial with `DISABLE name:item.bucketOil *` after a stone override rejected the entire profile. NEI still displayed the original stone EMC. The collector can use an explicit fuel target, so changing the map alone would leave a route outside normal exchange rules.
- The energy report uses the installed furnace resolver and an isolated in-memory IC2 Generator with a single fuel stack. All three fuels were accepted. The furnace reported 25,600 / 102,400 / 409,600 ticks and IC2 assigned 6,400 / 25,600 / 102,400 burn ticks. The installed IC2 config sets 10 EU per active tick. This probe checks ingestion and assigned burn ticks. Machine operation over time and cable delivery require world fixtures.
- The live BuildCraft registry contained lava (11), oil (163) and generic Fuel (4064). Gasoline (3017), kerosene (3019), diesel (3021) and heavy gas oil (3023) were absent from the combustion registry. ZTO registers the fractions in the separate liquid transport registry.

| Live engine entry | Power per cycle | Registry burn ticks per bucket | Nominal generated energy per 1,000 mB |
| --- | ---: | ---: | ---: |
| Lava | 1 | 20,000 | 20,000 |
| Oil | 2 | 10,000 | 20,000 |
| Generic Fuel | 5 | 50,000 | 250,000 |

Nominal figures use the inspected per-mB counter and count every configured generation tick. They do not measure energy received through pipes or used by a machine.

`BuildCraftEnergy` registers a `RefineryRecipe(int,int,int,int,int,int,int,int)` of 1 mB still oil to 1 mB generic Fuel with no second input, 10 BuildCraft energy per operation and a delay of 1. This is 10,000 energy charged for 1,000 mB before pipe and delivery losses. Oil and Fuel can power refining, but oil is depleted and no reverse recipe is evidenced here.

Archive SHA-256 for the inspected EE2, IC2 and BuildCraft energy binaries respectively

```
0FBD1DE0A62FC8AB3CA7C80802B9184CBB548FBA77DD2D4920244D2C1CCD31D3
87DC6DAA69B0594512DB09BB736E8EAF7A6D9C0DF1D264C97BCA6BA01180A98A
412F36EC44F66FDDAEECC78F8B342D209A8F5723B3DA84E3A6D833F2B4FB1A98
```
