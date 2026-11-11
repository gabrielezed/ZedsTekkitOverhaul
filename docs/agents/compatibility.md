# Legacy compatibility and failure modes

## API and naming

- This is **Minecraft 1.2.5**. Look up signatures in the actual MCP source, not modern Forge tutorials.
- Verify reobfuscated signatures against the Tekkit Classic 3.1.2 overlay and vanilla client in that order. The runtime is Forge **3.3.8.164** / FML **2.2.106.176**; Forge **3.4.9.171** is the MCP compile profile.
- `ContainerAdvancedCraftingTable` must override `canInteractWith(EntityPlayer)`, **not** `isUsableByPlayer(EntityPlayer)`. The latter caused an actual Eclipse compile error and was corrected.
- No modern registries, capabilities, `BlockPos`, GUI handler assumptions from newer Forge or JSON recipes.
- The current advanced crafting GUI is a **singleplayer-oriented** implementation using `ModLoader.openGUI`. Multiplayer synchronization has **not** been implemented or tested.

## Obfuscation and packaging

- Source in MCP uses deobfuscated class names. Production Tekkit uses mapped/obfuscated classes.
- Build with MCP's `recompile.bat` and `reobfuscate.bat`; package only our own `zed/` classes/assets.
- Binary patches to vanilla classes are **not** part of normal releases. They can overwrite NEI modifications and should not be inserted into `modpack.jar` or `minecraft.jar` during routine work.

## Inventory drag

The production drag path uses the opt-in Java 8 [mouse dispatcher agent](../engineering/inventory-drag.md). It recognizes the verified 1.2.5 `vp.class` fingerprint and inserts a cancellable event check in memory. NEI's `gb.class` remains the installed class. The old local MCP source hooks are available for development and are excluded from the packaged mod.

## Safe numeric data changes

- Default block IDs: `advancedCraftingTable = 3000`, `foundry = 3001`; consult the generated config and active Tekkit block registry for collisions.
- Changing numeric IDs can damage worlds. Record existing ID mappings and test on copied saves.
- The Foundry recipe intentionally uses **smooth Stone**, not Cobblestone.
- Intended EMC: Advanced Crafting Table **128**, Foundry **16**. The integration uses reflection and only adds absent mappings; confirm actual runtime values before calling this fully validated.

## Rendering direction

Custom models, TileEntity renderers, and simple visual animation are planned. No Create-style physics, moving-block contraptions, or kinetic network are required. Keep rendering compatibility scoped to the legacy OpenGL/TileEntity renderer APIs.
