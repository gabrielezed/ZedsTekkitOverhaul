# Codebase architecture

## Repository layout

```text
src/minecraft/zed/
├── gui/advanced_table.png
├── models/foundry_rotor.obj
├── textures/foundry_rotor.png
└── tekkitoverhaul/
    ├── mod_ZedsTekkitOverhaul.java
    ├── crafting/
    │   ├── AdvancedCraftingManager.java
    │   ├── AdvancedCraftingRegistry.java
    │   ├── BlockAdvancedCraftingTable.java
    │   ├── BlockFoundry.java
    │   ├── ContainerAdvancedCraftingTable.java
    │   └── GuiAdvancedCraftingTable.java
    ├── machine/FoundryRotorState.java
    ├── render/
    │   ├── TileEntityFoundryVisual.java
    │   ├── model/ (OBJ loader and mesh model)
    │   └── client/ (legacy Tessellator and TESR)
    ├── multiblock/ (pattern, Foundry ring, World adapter)
    ├── emc/ (catalog, profile validation, rules, EE2 adapter)
    └── integration/
        ├── ee2/
        │   ├── EE2FuelIntegration.java
        │   ├── EE2ItemResolver.java
        │   └── IC2EmcIntegration.java (inactive experiment)
        └── inventory/
            ├── DistributionPlanner.java
            └── DragDistributionIntegration.java
```

## Bootstrapping

`mod_ZedsTekkitOverhaul` extends the legacy `BaseMod`. Its `load()` registers advanced crafting and distillation and enables a game-tick hook. `modsLoaded()` resolves EE2 fuels and `addFuel()` forwards burn-time queries. The tick callback delegates to the EMC module, which applies compatibility defaults or the selected profile. The experimental IC2 EMC mapping has no bootstrap caller.

## Advanced crafting

- `AdvancedCraftingRegistry`: Forge configuration, numeric block IDs (defaults **3000/3001**), block registration, vanilla 2×2 recipe and one 5×5 recipe.
- `AdvancedCraftingManager`: recipes in a **fixed 5×5 grid**, with exact item ID and metadata matching; blank pattern cells must remain empty. This is not vanilla CraftingManager or an ore dictionary implementation.
- `ContainerAdvancedCraftingTable`: 25 inputs, one result, player inventory and vanilla `SlotCrafting` ingredient consumption.
- `GuiAdvancedCraftingTable`: a **212×236** GUI backed by `/zed/gui/advanced_table.png` in a 256×256 texture.
- `BlockFoundry`: placeable prototype with vanilla furnace textures and a small, animated OBJ rotor. The rotor has persistent NBT state and right-click pause/resume; **there is no machine inventory, energy use or processing yet**. On the multiblock feature branch, a temporary eight-Stone 3×3 ring validator is used for formation testing. See [multiblock validator](multiblock-validator.md).

The Foundry test recipe uses **16 smooth Stone** on the perimeter of the 5×5 grid, with the nine interior cells empty. The Advanced Crafting Table uses four vanilla crafting tables in a 2×2.

## Separation boundaries

- Keep world logic, recipe matching, inventory handling and rendering in separate classes.
- Keep dependency-specific access behind integrations or adapters.
- Avoid growing the `BaseMod` entry point into a feature implementation.
- The generic `StructurePattern` and its Minecraft `WorldBlockView` adapter provide the first testable structure validator. Its temporary eight-Stone ring is not the final Foundry design. Machines still need dedicated controllers, processing, energy and synchronization layers.
