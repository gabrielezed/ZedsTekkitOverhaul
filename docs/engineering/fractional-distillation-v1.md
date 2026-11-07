# Fractional Distillation — Iteration 1 design and engineering specification

**Branch:** `feature/fractional-distillation-v1`  
**Status:** implemented prototype awaiting full MCP 6.2 compilation, IC2 1.97 EnergyNet testing and real Prism Tekkit Dev gameplay verification.  
**Target:** Tekkit Classic 3.1.2 / Minecraft 1.2.5. Singleplayer first.

## 1. Design principle

Build a readable, modular **chemical-mechanical distillation apparatus** with separate physical components, visually consistent with Minecraft/Tekkit. Avoid a Create-style simulation, physics engine or global game patches.

This first machine provides a working structure check, four setpoints, internal fluid quantities, cooling-water consumption and a low-voltage IC2 heater prototype. BuildCraft fluid-pipe I/O and automatic water pumping are future adapters, **not** claimed here.

### Real-world terminology

- **Boiler / reboiler:** metal heated vessel containing crude petroleum, with **borosilicate-glass inspection windows**. A full glass high-temperature industrial boiler is not the intended model.
- **Fractionating column:** two stacked, narrow tray segments with reinforcing bands.
- **Water-cooled condenser:** metal shell with a visible serpentine and tinted coolant jacket.
- **Distillate receiver:** small collection tank with a sight-level window and a future pipe outlet.
- **Thermometer / controller:** temperature selector and process readout.
- **Auxiliary heater:** independent IC2 EU consumer, intended to support non-EU alternatives later.

**Future material:** a craftable `Borosilicate Sight Glass` is under consideration. Current model windows visually depict it but no new chemistry recipe has been frozen. Vanilla glass in temporary component recipes does **not** imply it is chemically identical.

## 2. Fixed initial layout (no rotation support yet)

All components lie in the same Z plane. Positive X is the side containing the condenser, receiver and thermometer. The main boiler is the reference position `(0,0,0)`.

```text
Side elevation, facing toward +Z

 Y+2    [COLUMN]       [CONDENSER]
 Y+1    [COLUMN]       [RECEIVER]
 Y 0    [BOILER]       [THERMOMETER]
 Y-1    [LV HEATER]    [empty]

        X=0            X=+1
```

The two column segments are the same block type; there are **six unique blocks** and **seven physical placements**. The heater sits **below** the boiler and should have an exposed cable-access face. The condenser's bucket input is a test substitute for a water pipe. The receiver's empty-bucket interaction is the test substitute for a BuildCraft liquid output pipe.

The initial +X-only layout has been extended to four horizontal rotations. See [binding and visual pass](distillation-rendering-and-binding-v2.md) for the exact orientation rules, missing-part messages, original restyled textures and connector geometry.

This layout is deliberately small for prototyping. Final industrial-scale dimensions, rotational symmetry, transparent mechanics and any multi-block transform into a single rendered unit remain design decisions.

### Model dimensions

Each OBJ is authored around the **center** of a corresponding Minecraft block in X/Z, using approximately one model unit per block:
- Boiler: circular steel body, copper flanges, borosilicate sight glass, top vapor outlet.
- Heater: steel housing, heating-coil indicator and insulated electrical terminal.
- Column (two copies): plated vertical shell, bands, sight window and service pipe.
- Condenser: rectangular coolant jacket with visible copper serpentine.
- Receiver: metal vessel, fluid-level window, brass bands, side output nozzle.
- Thermometer: metal panel, circular gauge, visible pointer and control-light slots.

The six original OBJ meshes are under `src/minecraft/zed/models/distillation_*.obj`, sharing `src/minecraft/zed/textures/distillation_atlas.png` (64×32 pixels). Four 16×16 pixel bucket icons are also provided. The renderer uses the existing `ObjLoader` / `ObjTessellatorRenderer`. It currently uses diagnostic packed-lightmap full-brightness; environmentally correct lighting is a later pass.

Materials and textures are original project resources; the glass alpha and UVs should be checked visually before treating the models as final.

## 3. Four named distillation products

The internal `DistillateType` enum assigns **stable IDs**; never reorder or renumber silently because the IDs are stored in NBT.

| ID | Fluid (English registry name) | Setpoint | Batch duration | Cooling water per 250 mB batch |
| --- | --- | ---: | ---: | ---: |
| 0 | Gasoline | 125 °C | 160 ticks | 40 mB |
| 1 | Kerosene | 205 °C | 200 ticks | 50 mB |
| 2 | Diesel | 300 °C | 260 ticks | 60 mB |
| 3 | Heavy Gas Oil (HGO) | 380 °C | 320 ticks | 80 mB |

Heavy Gas Oil is a heavy petroleum distillate, whereas **Marine Diesel Oil (MDO)** is a commercial maritime fuel grade, often blended. The prototype therefore uses HGO. Heavy Fuel Oil / bunker fuel can be added as a later blending/residue product.

These setpoints are **gameplay abstractions**, not laboratory boiling points or a physical temperature simulation. Real refinery cuts overlap across boiling ranges. We intentionally defer fractional yield curves and product purification.

External technical background: [US DOE refinery terminology](https://www.energy.gov/sites/default/files/2022-12/ref2000-Assuring_Adequacy_and_Affordability-Cleaner_Fuels.pdf) and [NREL refinery fractionation](https://www.nrel.gov/docs/fy02osti/30611.pdf).

## 4. First operational rules

- Internal tank capacity: **8,000 mB** each for crude input, condenser water and the selected distillate.
- Prototype batch: **250 mB crude → 250 mB selected output**; no material balance fractions yet. This is strictly for testing state, not economic balance.
- Selected temperature changes the product, coolant per batch and processing duration. Selecting a different cut resets partial progress.
- Provisional electric demand: **8 EU/t while a batch advances**; total 1,280 / 1,600 / 2,080 / 2,560 EU per batch, respectively.
- IC2 LV heater accepts packets up to **32 EU**, buffers up to **4,000 EU**, returns unaccepted EU to the network and rejects higher packets in this prototype.
- A batch advances only with all six auxiliary part placements, enough crude and water, available EU and an output tank not already containing a **different** product.
- A completed batch consumes crude/water, writes 250 mB to output, and resets progress. No energy is consumed while stalled.
- Receiver supports a sealed bucket when at least **1,000 mB** of one product has accumulated.
- Capacities and product identity are NBT-persistent. Structure formation is checked against the world and not saved as authoritative state.

**Important limitations:**
1. Four internal fluid identifiers and sealed bucket items are implemented; **BuildCraft 2.2.14-compatible pipe/liquid registration is not implemented**. These are not yet world-flowing liquids.
2. Oil intake uses a cautious BuildCraft oil-bucket item-name detector with a **configurable numeric override**; do not claim automatic compatibility with every BC installation.
3. Water must currently be added with vanilla water buckets; BuildCraft pumping from a 4×4 water pool is a later integration.
4. All GUI and controller interactions are **singleplayer-oriented**. Dedicated client/server synchronization is not implemented.
5. Temperature is an **immediate setpoint**, not a simulation with heat-up/cool-down or thermal inertia.
6. Current recipes use vanilla materials only and are **temporary**. Borosilicate crafting, metallurgy and proper EU/EMC balancing are future work.
7. Receiver produces sealed sample buckets; they do not yet pour into the world or connect to BuildCraft fluid systems.

## 5. Technical architecture

```text
zed.tekkitoverhaul.distillation/
├── DistillateType.java              stable fluid IDs and setpoints
├── FractionationProcess.java        pure batch logic, 3 tanks, progress
├── LvHeaterBuffer.java              pure EU buffer and packet rules
├── DistillationPartType.java        six stable block types
├── DistillationStructure.java       relative-offset multiblock pattern
├── DistillationRegistry.java        configurable IDs, blocks, bucket items and temporary recipes
├── ItemDistillateBucket.java        four distinct sample items
├── BlockDistillationBoiler.java     crude-oil intake/controller
├── BlockDistillationPart.java       thermometer, water input and receiver output
├── TileEntityDistillationBoiler.java persistent process state and structure check
├── TileEntityDistillationPart.java  visual-only auxiliary anchor
├── TileEntityDistillationHeater.java IC2 1.97 IEnergySink adapter
├── GuiDistillationThermometer.java  four singleplayer setpoint buttons
└── client/
    └── RenderDistillationComponent.java  six OBJ meshes via legacy TESR
```

`TileEntityDistillationHeater` alone should know the `ic2.api` classes. Future alternative heaters should implement a shared energy-source/heat-provider boundary, rather than copying IC2 logic into the boiler.

## 6. IC2 1.97 compilation strategy

The original IC2 1.97 API is an external dependency. The new `zto-tools/prepare-ic2-dev-stubs.bat` prepares **original, minimal compile-time ABI test doubles** under the ignored `src/minecraft/ic2/api/` directory **only when no local API source exists**.

- These are not redistributed official IC2 API sources.
- The **five** compile-time placeholders are under `zto-tools/ic2-dev-stubs/`.
- The test doubles provide **no real EnergyNet** inside Eclipse. **Prism Tekkit Dev** must load the **original IC2 1.97 JAR** at runtime.
- The deployment JAR packages only `reobf/minecraft/zed/`, not `ic2/api/`.
- API method descriptors referring to Minecraft classes must be verified **after MCP reobfuscation**, as production IC2 uses obfuscated Minecraft class names. Test cable input, removal and reloading.
- No API source should be copied from a decompiled third-party library into Git.

See [IC2 1.97 reference](../reference/tekkit-classic-3.1.2/ic2-1.97.md).

## 7. Build, test and first in-game acceptance

From the MDK root:

```powershell
git fetch origin
git switch -c feature/fractional-distillation-v1 --track origin/feature/fractional-distillation-v1

.\zto-tools\test-distillation-core.bat
.\zto-tools\build-and-deploy.bat
```

**Before deploy:** close Minecraft, move old competing ZTO JARs out of the Prism mods directory and make a backup of your test world. Check `config/ZedsTekkitOverhaul-Distillation.cfg` for numeric ID collisions; defaults are **3010–3015** for six blocks, **22000–22003** as *item constructor IDs* for four sample buckets (their runtime item IDs are 256 higher).

**In Tekkit Dev:**
1. Obtain one Boiler, one LV Heater, two Columns, one Condenser, one Receiver and one Thermometer from crafting/NEI.
2. Construct the exact vertical pattern shown above. Right-click the boiler; confirm `FORMED`.
3. Hold a BuildCraft Oil Bucket and right-click the boiler. If the item is not recognized, set `integration.buildcraftOilBucketItemId` to its exact **runtime item ID** in the configuration and restart.
4. Fill the condenser with vanilla Water Buckets.
5. Connect an IC2 **LV** EU source/cable to an exposed side of the heater (up to 32 EU per packet).
6. Right-click the thermometer and choose Gasoline. Observe the progress/amounts and wait for at least four batches.
7. Hold an empty bucket and right-click the Receiver: it should produce one **Gasoline Bucket** from 1,000 mB of product.
8. Clear the output tank before selecting Kerosene, Diesel or HGO; confirm the four setpoints create four distinct bucket items.
9. Remove a required column or other part; processing should stop. Rebuild and confirm it resumes.
10. Save/reopen the world and verify fluid quantities, selection, heater EU and progress.
11. Retest the Foundry rotor, NBT persistence, Stone ring and Advanced Crafting Table. Check for lightmap/NEI rendering conflicts.

## 8. Follow-up milestones

- **V2 in draft:** four-way orientation/controller lookup, better structure diagnostics, opaque IC2/BuildCraft-inspired palette, interlocking connection meshes, and `mcmod.info` JAR metadata. The actual IC2 1.97 EnergyNet lifecycle and new visuals still require in-game validation.
- **V3:** BuildCraft 2.2.14-specific liquid adapter, water input via pipe, receiver pipe output and pumps.
- **V4:** actual multi-output fractional yields, residue, energy/EMC balancing and alternative heat adapters.
- **V5:** production world lighting, refined glass rendering, more detailed models and server synchronization.

Do not merge into `main` until the user's real MCP and Tekkit Dev tests pass.
