# Hopper backport: Minecraft 1.2.5 / Tekkit Classic 3.1.2

## Scope and evidence

Original implementation under `src/minecraft/zed/tekkitoverhaul/hopper/`; no vanilla or third-party patches, agents or transformers. The bootstrap only registers the feature, dispatches rendering and invokes the opt-in development fixture. Single-player client prototype; multiplayer GUI/network synchronization and dedicated-server packaging are not supported. Remote worlds do not execute transfers or open the local GUI.

Baseline: clean `main`, fetched and equal to `origin/main`, then branch `codex/hopper-backport`. No commit, push or merge was requested. Development uses FoxyRetroMDK 1.3.6, MCP 6.2, Forge 3.4.9.171, JDK tools 8u504 and Java 8u511. Actual Tekkit Dev overlay is Forge **3.3.8.164**, FML **2.2.106.176**, NEI **1.2.2.4**, OptiFine **1.2.5 HD S C6**, IC2 **1.97** and BuildCraft **2.2.14**. The Forge mismatch is intentional existing environment state, not an upgrade made by this feature. Versions were checked in the effective modpack bytecode, FML startup log and installed archives.

Evidence must distinguish the reference audit, MCP tests, tests running inside Tekkit Dev, and visually observed interactions. See the results section below; unfinished manual checks are not implied by successful compilation.

## Verified official reference and asset preparation

Both behavior and visuals use **Minecraft 1.5.2**, with no later model assets. Resolve through the [official manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json), [version metadata](https://piston-meta.mojang.com/v1/packages/924a2dcd8bdc31f8e9d36229811c298b3537bbc7/1.5.2.json), and its `downloads.client` [artifact](https://launcher.mojang.com/v1/objects/465378c9dc2f779ae1d6e8046ebc46fb53a57968/client.jar). Metadata SHA-1: `924a2dcd8bdc31f8e9d36229811c298b3537bbc7`; client SHA-1: `465378c9dc2f779ae1d6e8046ebc46fb53a57968`; size: **5,564,661 bytes**. The preparation script verifies both hashes and the client size, then extracts exactly five resources outside the repository and Prism instances.

With Tekkit Dev closed:

```powershell
$env:ZTO_HOPPER_ASSETS = & .\zto-tools\prepare-hopper-assets.ps1
# Preserve the independently prepared Creative prototype when desired:
$env:ZTO_CREATIVE_ASSETS = & .\zto-tools\prepare-creative-assets.ps1
Remove-Item Env:ZTO_PRISM_MODS -ErrorAction SilentlyContinue
cmd /c "zto-tools\build-and-deploy.bat < nul"
```

Default research location: `%TEMP%/zto-hopper-1.5.2`. Client, extracted PNGs, disassembly and decompilation remain there. `stage/zed/hopper-1.5.2` contains only the resources needed by the local prototype. No Mojang images or decompiled source belong in Git. A shared source build can omit the assets (Hopper registration is disabled on a fresh installation; an already configured Hopper installation fails startup explicitly to protect existing worlds); **do not open Hopper worlds with an asset-free build**. For an enabled redistributable binary, obtain an appropriate license for independently authored replacement textures, or require each end user to run the local extraction step. The local prototype JAR containing Mojang assets must not be distributed as a repository release.

| Official resource | Local resource | Use |
| --- | --- | --- |
| `textures/blocks/hopper.png` | `outer.png` | 16x16 wall, body and spout sprite; allocated terrain atlas tile |
| `textures/blocks/hopper_top.png` | `top.png` | 16x16 top rim with transparent opening; allocated terrain atlas tile |
| `textures/blocks/hopper_inside.png` | `inside.png` | 16x16 interior floor; allocated terrain atlas tile |
| `textures/items/hopper.png` | `item.png` | 16x16 flat held/dropped/inventory item; allocated item atlas tile |
| `gui/hopper.png` | `gui.png` | 256x256 texture, panel rectangle (0,0)-(176,133) |

Reference classes: `amw` (block), `aqi` (tile), `bgf` (block renderer), `azf` (GUI), `ty` (container). Reproduce the audit with JDK 8 `javap -classpath client.jar -p -c amw aqi bgf azf ty`. Local Fernflower inspection was also used. These are version-specific obfuscated identifiers, not dependencies. Resource names were located in class constant pools before inspecting the methods.

The 1.5.2 renderer generates geometry; there is no block model JSON to extract. World bounds in block units:

- Upper shell: (0, 0.625, 0)-(1, 1, 1). Interior wall planes at X/Z 0.125 and 0.875; interior floor at Y 0.625. Top sprite supplies the opening mask.
- Body: (0.25, 0.25, 0.25)-(0.75, 0.623, 0.75); the 0.002 gap follows the reference.
- Down spout: (0.375, 0, 0.375)-(0.625, 0.25, 0.625).
- Horizontal spouts: Y 0.25-0.5, transverse coordinate 0.375-0.625; output coordinate 0-0.25 or 0.75-1 according to the face.
- UVs use the reference's block-face projection: a coordinate range [a,b] selects pixels [16a,16b] of its 16x16 sprite (vertical face V uses the renderer's inverted Y projection). The local ModLoader terrain/item allocation replaces 1.5.2's stitched texture bindings; atlas tile indices are dynamic, not fixed constants.
- Metadata faces: 0 down, 2 north (-Z), 3 south (+Z), 4 west (-X), 5 east (+X). Placement points opposite the clicked face, replacing upward with downward. The upper collision bowl has a full base through Y 0.625 and four 0.125-thick walls, matching the inspected reference collision bounds.

The reference GUI has Hopper slots (44+18i,20), player rows (8+18i,51+18j), hotbar (8+18i,109). The backport adds an `Enabled`/`Locked` indicator and a header tooltip showing capacity, nominal transfer rate, output direction and redstone/destination guidance. Enabled describes redstone state, not a guarantee of destination capacity. Normal legacy slot clicks and NBT-aware shift transfers are supported; no global inventory hook is installed. The flat item sprite follows the original item, independently of the world model. Sneaking while holding a block bypasses the Hopper menu for placement. Sneak-right-click a chest or another block with a Hopper to place before block activation. Click the top for downward output. Side clicks still point toward the clicked face.

## Transfer contract and deliberate differences

`aqi` decrements cooldown, then pushes one item before pulling one item; both actions run even if pushing succeeds. Any successful transfer sets 8 ticks, including insertion into another Hopper. Nominal continuous throughput is **2.5 items/second per direction at 20 TPS**. Loose entity pickup inserts as much of one eligible entity stack as fits. The search starts at the bowl floor (Y + 0.625) and skips entities that cannot insert. Inventory lookup above still takes priority. Redstone stops the Hopper's own transfers, not other machines pulling from or inserting into its inventory. The prototype uses an authoritative-world power query rather than a cached metadata lock bit.

Insertion first checks acceptance/capacity without changing the source. Single-item transfers commit to the destination before decrementing the source, on the single authoritative tick thread. This relies on the verified `IInventory` setter contract; arbitrary third-party inventories that lie about setters are not supported. Entity insertion subtracts only the accepted count. Stack compatibility compares item ID, exact damage and deep NBT equality. Inventory and item stack limits are both respected. NBT is copied, not reconstructed. Failed transfers retain the original source. Breaking drains each slot once into an intact EntityItem before removing the tile.

No forced chunk loads: inventory lookups check `blockExists` before retrieving tiles, including the second half of a double chest. Chest slot ordering follows the vanilla negative/positive neighbor ordering. Inventory minecarts, named Hoppers and comparator output from 1.5.2 are outside this first iteration (comparators do not exist in 1.2.5). Partial entity pickup deliberately counts as success for cooldown purposes, preventing repeated partial insertion attempts in consecutive ticks.

## Actual legacy ABI and integration matrix

MCP `IInventory` has no `isItemValidForSlot`, `canInsertItem` or `canExtractItem`. Forge `ISidedInventory` extends it with only `getStartInventorySide(int)` and `getSizeInventorySide(int)`. Side numbering is 0=-Y, 1=+Y, 2=-Z, 3=+Z, 4=-X, 5=+X. Production names are `io` (IInventory), `aan` (ItemStack), `kw` (TileEntity), and `forge.ISidedInventory`; MCP reobfuscation handles these types. No modern sided-inventory API is assumed.

| Inventory/transport | Insertion | Extraction | Boundary |
| --- | --- | --- | --- |
| Single/double chest, dispenser, Hopper | All available slots, capacity and identity checked | Available slots | Generic vanilla inventory contract |
| Forge sided inventory | Exposed contiguous slot range | Exposed contiguous slot range | Legacy generic contract; arbitrary additional machine restrictions cannot be inferred |
| Vanilla furnace | Top slot 0 only for smeltable items; bottom slot 1 only for fuel; lateral output slot rejects insertion | Slot 2 on horizontal faces; bottom fuel is protected | Explicit furnace policy respects 1.2.5 faces; direct Hopper below cannot collect furnace output |
| IC2 Macerator / Extractor / Compressor / Electric Furnace | Top slot 0, only if `getResultFor(ItemStack, false)` returns a recipe output | Product slot 2 from bottom for Hopper; also exposes lateral product to the transfer policy | Narrow reflection adapter; battery slot 1 and upgrade slots 3+ excluded |
| Other IC2 inventories | Denied | Denied | Fail closed until their contracts are audited |
| Unknown unsided machine | Denied | Denied | No universal slot-acceptance contract in this release |
| BuildCraft item pipe into Hopper | All five Hopper slots through vanilla inventory contract | N/A | No BC dependency in Hopper classes |
| Powered wooden item pipe from Hopper | N/A | All five slots through Forge sided contract | Pipe timing and transport remain BuildCraft-owned |
| Hopper actively facing item pipe | Not implemented | N/A | Pipe is not treated as ordinary writable inventory |

IC2 `TileEntityElectricMachine` implements the legacy sided interface: top slot 0, bottom slot 1 (discharge/battery), every horizontal side slot 2 (output), range size 1. The machine container confirms upgrade slots 3-6. Its `getResultFor(ItemStack, boolean)` is public; Macerator delegates to `Ic2Recipes.getMaceratorOutputFor`. **Owner-authorized difference:** the Hopper's IC2 adapter reads product slot 2 from below instead of the battery slot exposed by IC2. This changes only ZTO's transfer policy; IC2 and pipe side mappings are untouched. Other automation continues to see IC2's original sides. Reflection keeps optional IC2 classes out of the Hopper's linkage surface.

BuildCraft audit: `TileGenericPipe` implements `ISpecialInventory` and `IPipeEntry`. `addItem(ItemStack, boolean, Orientations)` delegates to `PipeLogic`; the base implementation always returns false and has zero inventory slots. `PipeItemsWood.checkExtract` first checks `ISpecialInventory`, then the Forge sided range, then the ordinary inventory path. The pipe wraps adjacent chests using `Utils.getInventory`. `IPipeEntry.entityEntering(EntityPassiveItem, Orientations)` returns void. `PipeTransportItems.acceptItems()` always returns true, so it is not a downstream capacity/backpressure test. Active injection could hand ownership to the transport, but cannot promise final insertion or retain the source on a blocked route. This prototype deliberately delivers only inventory-facing item-pipe integration; liquid and power transport APIs are not imported into the feature.

To repeat installed-ABI inspection, build a `javap` classpath with Tekkit Dev's `minecraft/bin/modpack.jar`, Prism's base 1.2.5 client, `industrialcraft-2-client_1.97.jar`, and the BuildCraft A-core/B-transport 2.2.14 ZIPs. Inspect `ic2.common.TileEntityElectricMachine`, `ic2.common.ContainerElectricMachine`, the concrete machine classes, `buildcraft.transport.TileGenericPipe`, `PipeLogic`, `PipeTransportItems`, `pipes.PipeItemsWood`, `ItemPipe`, and `buildcraft.api.IPipeEntry`. Keep the resulting disassembly outside Git.

## Registration, recipe and saves

Default ID **3030**, stored as `block.hopper` in `config/ZedsTekkitOverhaul.cfg`. Existing Tekkit Dev config uses 3000/3001, 3010-3023 and 3024 for other ZTO features; 3020 was rejected because it is dieselFlowing. Registration verifies both block and item tables and fails on collision, without automatically choosing a different ID. Verify the startup line `[ZTO] Hopper ID=...` for a different installation.

Vanilla 3x3 recipe: `I I / ICI /  I `, five iron ingots and one chest, produces one Hopper. No automatic EMC assignment was added. Tile identifier `ZTOHopper`, schema `ZTOHopperVersion=1`; `Items` list uses byte `Slot` and complete vanilla ItemStack NBT; `TransferCooldown` is an integer. Facing is block metadata. IDs must stay stable once saved. Removing this mod, disabling registration by omitting assets, or reassigning its block ID can destroy blocks and contents when loading a world. Back up worlds before changing installations; no silent migration is provided.

## Repeatable validation

1. Close Tekkit Dev, prepare assets and run the build/deploy command above. It runs `recompile.bat` and `reobfuscate.bat`, packages only `zed/` classes, and deploys to the isolated instance. Inspect `jar tf dist-zto/ZedsTekkitOverhaul.jar`: every `.class` must start with `zed/`. Reobfuscation may generate unrelated vanilla/stub output in `reobf`; that output is not packaged.
2. Run `.\zto-tools\test-hopper.ps1` after compilation: 17 assertions execute against actual MCP inventory/NBT classes without installed IC2/BC, not replacement stubs. This is not a game startup test.
3. For the opt-in live fixture, create an empty file `minecraft/zto-hopper-acceptance.enable` in **Tekkit Dev only** and create a disposable Creative world named exactly **ZTO Hopper Acceptance**. The fixture builds a stone platform at Y=99 near the player, seeds distinct inventory scenarios, and writes `minecraft/zto-hopper-acceptance.log`. It does nothing in other world names or without the marker file. Let the unpaused world run for at least 50 seconds (1000 ticks). Look for `COMPLETE`, not merely initial `PASS` lines. IC2 processing is supplied energy through its verified energy field; the wooden pipe receives 8 units through its power provider. This tests inventory/transport integration, not energy cable/engine integration.
4. The fixture marker below the platform preserves existing fixtures on reload. To repeat from scratch, use a fresh disposable save with that exact display name. Remove the enable file after testing. No fixture should ever be enabled in an ordinary play instance.
5. Manually inspect down and four horizontal spouts, interior and held sprite. Open the demonstration Hopper with orange wool and an enchanted damaged pickaxe, test normal clicks and shift-clicks, then save/quit/reload and count again. Test two GUI scales. Place a new Hopper against each face and test a redstone lever. Break a loaded Hopper and count the drops. Inspect NEI and existing Creative inventory for regressions.
6. For BuildCraft, use a powered wooden item pipe extracting from a Hopper into cobblestone item pipe and another Hopper, then fill the destination. Count inventory contents, traveling items and dropped entities together: BC's own blocked-path behavior is not transactional Hopper backpressure. Test a Hopper facing an item pipe separately; it must retain its contents. Do not substitute waterproof or power pipes.

## Results (2026-11-09)

- **Reference/ABI inspected:** verified official 1.5.2 client and local renderer/tile/container audit; actual Tekkit Dev IC2 and BC bytecode; local Forge/MCP inventory declarations.
- **Compiled:** MCP 6.2 client compilation and reobfuscation succeeded; server sources/md5s are absent, so no server result is claimed.
- **Packaged/deployed:** local prototype deployed through the standard script to Tekkit Dev; SHA-256 `0bad89b5b25e968d6a9973b8be88fff4282d351b11b883bdf5639da14feedbed`; no vanilla/third-party `.class` entries in the JAR.
- **MCP assertions:** 17 passed, covering item count, full destinations, metadata/NBT separation, simulation, partial insertion, NBT persistence, legacy furnace slots and double-chest slots. IC2 and BuildCraft were absent from this test classpath; this does not prove optional-free game startup.
- **Tekkit Dev fixture:** pending completion and review of the live log.
- **Owner observations on the initial build:** IC2 and EE2 interoperability reported working. Screenshots show items stranded in the bowl, a lateral outlet after placement against a support, and gaps above chest lids. The owner also reported IronChest transfer failure and chest activation while sneaking.

Do not call the feature fully accepted until the runtime results and manual checklist above have been recorded.


## Follow-up controls and integration

The output button cycles Auto, Input, and Fuel. The selection is saved as OutputMode and old saves default to Auto. Vanilla furnace Auto follows the legacy face range. Input selects the smelting slot and Fuel selects the fuel slot regardless of face, subject to item validation. The IC2 coal Generator uses slot 1 in Auto and Fuel and accepts ordinary furnace fuels meeting its burn threshold, excluding lava buckets. Input disables output to that generator. Other destinations retain their existing policies. The button changes routing only. The spout still follows the placement face.

IronChests 3.8.0.40 implements the production inventory interface io without Forge sided ranges. The optional adapter recognizes its TileEntityIronChest base class and handles all variants through the normal stack checks. The downward spout extends one pixel over the recessed chest lid. Neither adapter links optional classes directly.

Retest sneak placement on a chest top and side, confirm Creative stack count is unchanged, then leave a stack resting in the bowl and verify pickup. Test IronChest in both directions, a Generator with coal and an invalid item, and a vanilla furnace in all three modes. Reopen the world to verify the mode persists. The follow-up requires a fresh Tekkit Dev runtime pass.
