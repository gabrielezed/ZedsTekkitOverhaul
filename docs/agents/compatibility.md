# Legacy compatibility and failure modes

## API and naming

- This is **Minecraft 1.2.5**. Look up signatures in the actual MCP source, not modern Forge tutorials.
- `ContainerAdvancedCraftingTable` must override `canInteractWith(EntityPlayer)`, **not** `isUsableByPlayer(EntityPlayer)`. The latter caused an actual Eclipse compile error and was corrected.
- No modern registries, capabilities, `BlockPos`, GUI handler assumptions from newer Forge or JSON recipes.
- The current advanced crafting GUI is a **singleplayer-oriented** implementation using `ModLoader.openGUI`. Multiplayer synchronization has **not** been implemented or tested.

## Obfuscation and packaging

- Source in MCP uses deobfuscated class names. Production Tekkit uses mapped/obfuscated classes.
- Build with MCP's `recompile.bat` and `reobfuscate.bat`; package only our own `zed/` classes/assets.
- Binary patches to vanilla classes are **not** part of normal releases. They can overwrite NEI modifications and should not be inserted into `modpack.jar` or `minecraft.jar` during routine work.

## Inventory drag prototype

`DragDistributionIntegration` is an implementation module, not an automatic ModLoader feature. It was exercised with a source-level `GuiContainer` hook during Eclipse testing. In production, Tekkit's existing obfuscated `gb.class` contains **Not Enough Items (NEI)** code. A naive replacement can remove NEI behavior; do not ship one.

The old hook/ASM agent experiments are separate prototypes, not proof that the repository's current JAR enables drag on its own. A runtime hook must explicitly preserve NEI and be validated in the exact loaded game configuration.

## Safe numeric data changes

- Default block IDs: `advancedCraftingTable = 3000`, `foundry = 3001`; consult the generated config and active Tekkit block registry for collisions.
- Changing numeric IDs can damage worlds. Record existing ID mappings and test on copied saves.
- The Foundry recipe intentionally uses **smooth Stone**, not Cobblestone.
- Intended EMC: Advanced Crafting Table **128**, Foundry **16**. The integration uses reflection and only adds absent mappings; confirm actual runtime values before calling this fully validated.

## Rendering direction

Custom models, TileEntity renderers, and simple visual animation are planned. No Create-style physics, moving-block contraptions, or kinetic network are required. Keep rendering compatibility scoped to the legacy OpenGL/TileEntity renderer APIs.
