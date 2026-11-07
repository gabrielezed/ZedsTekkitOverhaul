# Codebase architecture

## Repository layout

```text
src/minecraft/zed/
├── gui/advanced_table.png
└── tekkitoverhaul/
    ├── mod_ZedsTekkitOverhaul.java
    ├── crafting/
    │   ├── AdvancedCraftingManager.java
    │   ├── AdvancedCraftingRegistry.java
    │   ├── BlockAdvancedCraftingTable.java
    │   ├── BlockFoundry.java
    │   ├── ContainerAdvancedCraftingTable.java
    │   └── GuiAdvancedCraftingTable.java
    └── integration/
        ├── ee2/
        │   ├── EE2FuelIntegration.java
        │   ├── EE2ItemResolver.java
        │   ├── IC2EmcIntegration.java
        │   └── AdvancedCraftingEmcIntegration.java
        └── inventory/
            ├── DistributionPlanner.java
            └── DragDistributionIntegration.java
```

## Bootstrapping

`mod_ZedsTekkitOverhaul` extends the legacy `BaseMod`. Its `load()` registers advanced crafting and enables a game-tick hook. `modsLoaded()` resolves EE2 fuels; `addFuel()` forwards fuel burn-time queries. Its tick callback invokes advanced crafting EMC registration and optionally the experimental IC2 EMC mapping.

The `ENABLE_IC2_EMC_INTEGRATION` flag is currently **false**. Do not enable it incidentally.

## Advanced crafting

- `AdvancedCraftingRegistry`: Forge configuration, numeric block IDs (defaults **3000/3001**), block registration, vanilla 2×2 recipe and one 5×5 recipe.
- `AdvancedCraftingManager`: recipes in a **fixed 5×5 grid**, with exact item ID and metadata matching; blank pattern cells must remain empty. This is not vanilla CraftingManager or an ore dictionary implementation.
- `ContainerAdvancedCraftingTable`: 25 inputs, one result, player inventory and vanilla `SlotCrafting` ingredient consumption.
- `GuiAdvancedCraftingTable`: a **212×236** GUI backed by `/zed/gui/advanced_table.png` in a 256×256 texture.
- `BlockFoundry`: placeable, **inert** block with vanilla furnace textures; no TileEntity, inventory, multiblock detection or processing logic yet.

The Foundry test recipe uses **16 smooth Stone** on the perimeter of the 5×5 grid, with the nine interior cells empty. The Advanced Crafting Table uses four vanilla crafting tables in a 2×2.

## Separation boundaries

- Keep world logic, recipe matching, inventory handling and rendering in separate classes.
- Keep dependency-specific access behind integrations or adapters.
- Avoid growing the `BaseMod` entry point into a feature implementation.
- Any future multiblock should have a controller, structure validator, persistence and rendering layers; these are **design intentions**, not existing classes.
