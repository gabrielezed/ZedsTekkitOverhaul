# MinecraftForge legacy reference — Minecraft 1.2.5

**Workspace-specific version:** Forge **3.4.9.171**, FML **2.2.106.176**, MCP **6.2**. Source declarations below were checked in the user's existing `forge.zip` reference and the ZedsTekkitOverhaul code. They should still be verified against the **actual working MDK source** before a new integration is committed.

## Legacy package names

In the deobfuscated MCP environment used by ZTO, Forge integration sources use packages such as:

- `net.minecraft.src.forge.Configuration`
- `net.minecraft.src.forge.ISidedInventory`
- `net.minecraft.src.forge.oredict.OreDictionary`

Do **not** blindly use later `net.minecraftforge.common` APIs or modern Forge capabilities.

## Sided inventories

The user's historical `ISidedInventory` source declares:

```java
// Signature summary only (not copied implementation):
int getStartInventorySide(int side);
int getSizeInventorySide(int side);
```

It extends `IInventory`. The numeric side convention documented in this source is:

| Side | Face |
| --- | --- |
| 0 | Bottom (`-Y`) |
| 1 | Top (`+Y`) |
| 2 | North (`-Z`) |
| 3 | South (`+Z`) |
| 4 | West (`-X`) |
| 5 | East (`+X`) |

**Version warning:** Forge 1.4.4+ and later Minecraft versions use different packages and may use `ForgeDirection` rather than the numeric `int` seen in this 1.2.5 source. Avoid mixing documentation for different mappings.

For automation work, validate which slots each side exposes, and test with both BuildCraft pipes and RedPower tubes if those integrations are required. Do not assume the sided interface alone guarantees compatibility with either.

## Configuration and Ore Dictionary

- `Configuration(File)`, `load()`, `save()`, and `getOrCreateBlockIdProperty(String, int)` were used before the [alpha.8 migration](../../agents/configuration.md).
- Numeric block IDs must stay stable for existing worlds. The current defaults are **3000** for Advanced Crafting Table and **3001** for Foundry; the active world config takes precedence.
- In the locally supplied `OreDictionary` source, `getOreID(String)`, `getOreName(int)`, and `getOres(String)` exist. Confirm registration APIs in the installed MDK before implementing an ore-dictionary recipe handler.

Do not assume the Advanced Crafting Table supports Ore Dictionary entries: its current recipe manager compares item ID and metadata exactly. Supporting alternative ingots/dusts is a future feature, not a current capability.

## GUI, persistence and network boundaries

- Legacy `Container` requires `canInteractWith(EntityPlayer)`. Using `isUsableByPlayer` for this subclass caused a real compile error in ZTO.
- ZTO already uses `TileEntity.readFromNBT` / `writeToNBT` for Foundry rotor state and a legacy TESR for OBJ animation.
- Minecraft 1.2.5's `GuiContainer` may be modified by NEI. Never ship a replacement deobfuscated `GuiContainer` / production `gb.class` over NEI.
- Singleplayer behavior is not proof of SMP synchronization; plan separate GUI, TileEntity update and network testing before adding multiplayer.

## Reference sources

- Local, exact-user supplied `forge.zip` for declarations; matching FoxyRetroMDK 1.3.6 tree remains authoritative.
- [Legacy Forge 1.2.3 JavaDoc](https://legacymcmodding.github.io/Forge-JavaDoc/generated/1.2.3-1.4.1.64/client/index-all.html) is **older** than the target build: useful as a historic pointer, **not** authoritative for Forge 3.4.9.171.
- Modern Forge API guides are not compatible references for this target.
