# Creative inventory backport

The prototype targets single-player Tekkit Classic 3.1.2. Open the inventory normally in Creative mode. Use the compass to search by localized name or numeric `ID:metadata`, the chest to access armor and inventory, and the **1.2.5** tab to open the classic screen. Press **F6** to restore the backport without restarting. Rebind **Restore ZTO Creative Inventory** in Options → Controls. The shortcut restores an open classic screen when the cursor is empty, or the next Creative inventory opening if you press it while playing. Tab toggles text focus in the search view so NEI shortcuts can be used there. Set `creativeInventoryBackport=false` in `minecraft/config/ZedsTekkitOverhaul.cfg` and restart for a persistent rollback. Builds without the four optional textures leave the legacy screen active.

## Reference client and local build

Run the preparation script and deploy with Tekkit Dev closed. Keep the default Prism target and remove any deployment override from the current shell.

```powershell
$env:ZTO_CREATIVE_ASSETS = & .\zto-tools\prepare-creative-assets.ps1
Remove-Item Env:ZTO_PRISM_MODS -ErrorAction SilentlyContinue
cmd /c "zto-tools\build-and-deploy.bat < nul"
```

The script resolves 1.4.7 through the [Mojang launcher manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json), verifies the [version metadata](https://piston-meta.mojang.com/v1/packages/7aa8e9aeacf4e1076bfd81c096f78de9b883ebe6/1.4.7.json), and uses the metadata's [client download](https://launcher.mojang.com/v1/objects/53ed4b9d5c358ecfff2d8b846b4427b888287028/client.jar). The verified client has 5,005,735 bytes and SHA-1 `53ed4b9d5c358ecfff2d8b846b4427b888287028`. The JAR, metadata, staged PNGs and local disassembly belong in the research directory under `%TEMP%\zto-creative-1.4.7`. The build copies the four unchanged PNGs to `zed/creative-1.4.7/` inside the local test JAR.

Use `javap -classpath client.jar -p -c avs avt tj` from the research directory to reproduce the reference audit. The bundled Fernflower can also decompile these classes locally. `avs` binds the tab atlas and the background selected by `tj`. `avt` defines the slot grid. All four PNGs are 256 × 256 and UVs below are measured in pixels from the upper-left.

| Client resource | Region and purpose |
| --- | --- |
| `gui/allitems.png` | Tab cells are 28 × 32, with U equal to 28 × column. V is 0 or 32 for the inactive or selected top tab, and 64 or 96 for the inactive or selected bottom tab. Scroll thumbs are 12 × 15 at (232, 0) when active and (244, 0) when disabled. |
| `gui/creative_inv/list_items.png` | The 195 × 136 panel starts at (0, 0). Nine columns by five rows start at (9, 18) with pitch 18. The nine hotbar slots start at (9, 112). The thumb starts at (175, 18) and travels 95 pixels. |
| `gui/creative_inv/search.png` | Same panel and slots. The search box is baked into the background. `avs` places a transparent text field at (82, 6) with width 89. |
| `gui/creative_inv/survival_inv.png` | Same panel bounds. Armor positions are (9, 6), (9, 33), (63, 6), (63, 33). The inventory and hotbar start at (9, 54) and (9, 112). The trash slot is at (173, 112) and the player render is anchored at (43, 45). |

Tab icons are rendered item stacks. The prototype uses the local 1.2.5 item textures, tooltip renderer and text input. The reference Creative GUI uses the tab and scrollbar states above. Its inherited achievement and stats actions are outside this backport. ZTO adds a small rollback control in a reserved bottom tab cell.

For a distributable build, leave `ZTO_CREATIVE_ASSETS` unset. Keep Mojang textures and decompilation output out of source control and shared builds. For distribution of an enabled version, either retain local extraction by the end user or use independently authored textures with an appropriate license.

## Tekkit Dev bytecode audit

The test instance loads Minecraft 1.2.5, Forge **3.3.8.164**, FML **2.2.106.176**, NEI **1.2.2.4** and OptiFine **1.2.5 HD S C6**. The MDK development target uses Forge **3.4.9.171**. The runtime versions came from the title screen, FML startup log and the `mod_NotEnoughItems` package annotation. The Prism `mmc-pack.json` selects a single modpack jar-mod. The jar-mod and `minecraft/bin/modpack.jar` had matching SHA-256 `0827EE55325FEDBC37F2A6A890F19DB11F75FAB5E9A5C02EAFD1BD50D2B59446`.

Set `$jarMod` to that instance's `minecraft/bin/modpack.jar` and `$baseClient` to Prism's `libraries/com/mojang/minecraft/1.2.5/minecraft-1.2.5-client.jar`. Inspect the effective classpath in overlay order:

```powershell
javap -classpath "$jarMod;$baseClient" -p -c net.minecraft.client.Minecraft vp ain sr os gb ki
javap -classpath "$jarMod;$baseClient" -p -c ModLoader BaseMod forge.MinecraftForge forge.ForgeHooksClient
javap -classpath "$jarMod;$baseClient" -p -c cpw.mods.fml.client.FMLClientHandler cpw.mods.fml.common.FMLCommonHandler cpw.mods.fml.common.ITickHandler
javap -classpath "$jarMod;$baseClient" -p -c codechicken.nei.NEIController codechicken.nei.NEIUtils codechicken.nei.LayoutManager
javap -classpath "$jarMod;$baseClient" -v mod_NotEnoughItems
```

Use `conf/client.srg` and `conf/methods.csv` to map the MCP names. The inventory key branch in `Minecraft.runTick()` constructs `GuiInventory(EntityPlayer)` (`ain(yw)`). `GuiInventory.initGui()` (`ain.c()V`) queries the controller's `isInCreativeMode()` (`ki.h()Z`) and calls `displayGuiScreen(GuiScreen)` with `new GuiContainerCreative(EntityPlayer)` (`sr(yw)`). That constructor creates `ContainerCreative(EntityPlayer)` (`os(yw)`), sets the player's active container and installs catalog slots and the real hotbar. `displayGuiScreen` closes the previous screen, assigns the current screen and calls `setWorldAndResolution`, which invokes `initGui`. NEI's `gb` override initializes a `NEIController(gb)` after this call when the initialized screen is still current. In Tekkit Dev the jar-mod supplies the Minecraft loop, `os` and the NEI-patched `gb`. The base client supplies `ain` and `sr`.

| Option | Verified hook, ordering and input impact | NEI and rollback |
| --- | --- | --- |
| Forge GUI API | `IGuiHandler` and `PacketOpenGUI` implement mod GUI routing. The effective inventory key and display path have no cancellable GUI-open event. | No entry point for replacing this inventory. |
| ModLoader GUI callback | `setInGUIHook(BaseMod, boolean, boolean)` reaches `onTickInGUI(float, Minecraft, GuiScreen)`. FML `GUILOAD` is a one-shot notification from render-end. The normal render-end callback runs after the legacy frame and may show it once. | Existing NEI initialization has already run. Removing the callback restores the classic path. |
| FML render-start | `FMLCommonHandler.registerTickHandler(ITickHandler)` schedules `tickStart(EnumSet<TickType>, Object...)` for `RENDER`. The effective Minecraft loop calls `FMLClientHandler.onRenderTickStart(float)` before its renderer. ZTO recognizes the exact current legacy instance and uses normal screen dispatch to close it and initialize the replacement. Input processed before the hook belongs to the legacy screen. A screen with a held cursor stack is skipped. | NEI initializes for the legacy instance and again for the ZTO instance. The old screen is not drawn on the verified call path. The **1.2.5** tab selects the classic path. The configurable FML key binding clears that session choice and lets the same render-start hook restore the backport. The config disables both. |
| Subclass the legacy Creative screen | `sr(yw)` is public and its drawing and click methods are overridable. Its scroll and backing inventory fields are private, and the effective `os` list and scroll rules are tied to the old 8-column layout. Calling the constructor again also changes the active container and repeats the open-inventory statistic. | NEI `LayoutManager` uses `instanceof sr` to configure its delete control. A subclass inherits that Creative-specific UI but still needs the slot and scroll rewrite. Stop selecting the subclass to roll back. |
| Explicit opening | A separately registered ModLoader key and `displayGuiScreen` could open the ZTO screen directly. This adds a key binding and requires its own checks for world, mode, current screen and cursor ownership. | The ZTO screen can still inherit `gb` hooks. The successful render-start path keeps the normal inventory key. |

## Catalog and input boundaries

The backport extends `GuiContainer` and keeps NEI's inherited overlay and tooltip path. `gb.clickSlot(yu,int,int,boolean)` dispatches virtually to the ZTO click method. The optional NEI bridge reads the already populated, unfiltered `NEIController.items` field on 1.2.2.4. The legacy Forge list supplies its existing variants, and registered ZTO machines are included even when the default Forge block hook contributes no stack. Entries retain ID, damage and NBT and are deduplicated by those values. The catalog is built on opening. Search filters cached names and the visible slot copies change only on tab, query or row changes.

Extensions call `CreativeCategories.register(key, title, icon, filter)` during mod loading. Up to six extra categories appear in key order. The built-in Building Blocks and Items tabs group by the registered item's block wrapper. Some pack blocks use a regular item as their placeable form and appear under Items. Search spans all categories.

The catalog supports left and right click, Shift-click for a full cursor stack and number keys for a hotbar stack under the pointer. In the player tab the number keys swap actual stacks and Shift-click uses the normal player inventory container for main inventory and hotbar moves. The NEI search and the ZTO search are separate. NEI receives keyboard shortcuts outside a focused ZTO search field. NEI's Creative-specific controls can differ because the replacement is a plain `gb` subclass. Multiplayer uses the existing screen.

## Acceptance checks in Tekkit Dev

Create a disposable Creative world. Open with the inventory key and test the three catalog tabs, search for `wool` and a modded item, then move Orange Wool (35:1) to the hotbar. Scroll with the wheel and drag the thumb. Open the player inventory tab and move a stack between the main inventory and hotbar. Close, wait, and reopen. Repeat at two GUI scales. With NEI enabled, hover a known EMC item, type in NEI's separate search, open a recipe and usage view with its configured shortcuts, then return to the backport. Use NEI's game-mode control to switch to Survival and confirm the regular inventory. Click **1.2.5** in Creative, reopen with the inventory key and confirm that the classic layout persists. Press **F6** with an empty cursor to restore the backport. Rebind the shortcut in Controls and repeat with the new key.

### Observed in Tekkit Dev

- In the disposable `zto creative` world, the normal inventory key opened the backport at effective Auto GUI scales of 2× and 4×. Escape left it closed and the next opening worked normally. No legacy frame was noticed during normal interaction. Frame-level ordering comes from bytecode inspection.
- The catalog returned wool variants, the IC2 Macerator and the ZTO machines and buckets. Orange Wool (35:1) moved to the hotbar and then to the player's main inventory. The `1` shortcut put 64 Advanced Crafting Tables into the first hotbar slot.
- With NEI active, its overlay and separate search worked. The tooltips showed EMC for Stone Bricks (98:1), Orange Wool (35:1) and the Advanced Crafting Table (3000). `R` and `U` on Stone Bricks opened the respective views and Escape returned to the same catalog position.
- The NEI mode control switched to the normal Survival inventory and back to the backport. The **1.2.5** tab opened Item Selection and the next press of the inventory key still opened Item Selection. Restarting restored the backport.
- An intermediate build hit a `StackOverflowError` during wheel and recipe testing. The trace alternated between `dd.a(IIZLyw;)` and `dd.b(IIZLyw;)` because the default transfer method returned an unchanged stack. The replacement container now rejects direct transfers from NEI and protects the temporary slots. Wheel scrolling and both recipe shortcuts worked after redeployment.
- The final build handled a rapid scrollbar drag and showed later Railcraft and other pack blocks. The Advanced Crafting Table was placed from the hotbar and its 5 × 5 GUI opened with NEI visible. Closing it and pressing the inventory key opened the backport again.
- The inactive **1.2.5** tab is drawn beneath the panel, so the panel covers its upper four pixels like the adjacent inventory tab. In Tekkit Dev, a quick **F6** tap restored the open classic screen. After rebinding to **F10** in Controls, the new key restored it and the tooltip showed the updated binding. The render-start hook checks the last consumed keyboard event to cover taps that finish between FML's game-tick key polls.

MCP compiled and reobfuscated the client. The deployed JAR matched the local SHA-256, and its class entries were all under `zed/`. Multiplayer, the full machine GUI suite, armor equip, and the persistent config toggle were outside this runtime pass.
