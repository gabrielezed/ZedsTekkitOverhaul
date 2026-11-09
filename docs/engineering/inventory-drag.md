# Inventory drag and NEI controls

## Enable inventory drag in Tekkit Dev

1. Build the mod with `zto-tools/build-and-deploy.bat` after exiting Tekkit Dev.
2. Run `zto-tools/build-drag-agent.bat` with JDK 8. It creates `dist-zto/ZTOInventoryDragAgent.jar`.
3. From the MDK root, run the PowerShell command below. Paste its output into the Java 8 JVM arguments of the isolated Prism **Tekkit Dev** instance. Keep the agent JAR outside the mods directory.

   ```powershell
   '"-javaagent:{0}"' -f (Resolve-Path -LiteralPath '.\dist-zto\ZTOInventoryDragAgent.jar' -ErrorAction Stop).Path
   ```

4. Start the game. The log line `[ZTO] Inventory drag connected to the legacy mouse dispatcher` confirms registration. Removing the JVM argument disables the production hook.

The main mod JAR contains the NEI rendering changes and the drag logic. The launch agent enables mouse event interception. The agent uses JDK 8's bundled ASM and contains only original ZTO classes. It matches the SHA-256 fingerprint of the installed Minecraft 1.2.5 `vp.class` (GuiScreen), from the client JAR under the Tekkit overlay. It inserts a callback in `vp.f()V` before the ordinary LWJGL dispatcher. This preserves NEI's installed `gb.class` subclass, its original click/release methods and its scroll dispatch. An unrecognized class body leaves drag disabled.

## Behavior and boundaries

Pick up a stack normally, then press and drag from a compatible slot. The preview shows the amount to be added to each selected slot. Left drag gives each visited eligible slot the same integer share, limited by its free capacity, with leftovers on the cursor. Right drag places one item per visited eligible slot. Revisiting a slot does not add a second entry. A press and release with a single eligible slot replays the ordinary NEI click.

Supported in local Survival on the vanilla inventory, workbench, plain single/double chests and native 5x5 table. The gesture selects ordinary slots with an empty or strictly matching ID, damage and NBT. Armor, crafting outputs, recipe screens and machine-specific destinations retain their existing actions. Shift/Ctrl/Alt, the drop shortcut, NEI widget presses, infinite held stacks and delete mode are outside the gesture. Inventory Tweaks bulk-action rules still apply to ZTO's automated moves. Drag is an explicit manual slot selection.

The session checks the live screen, window, player, cursor, slot ownership, capacities and the original target stack snapshots. The commit uses ordinary window clicks on the game thread and stops if any result differs from the planned change. This sequence is local only. A verified server-side drag transaction would be required for multiplayer activation.

## NEI styling

The compact actions use the same atlas and complete four-corner border as legacy NEI. The 2x2 controls sit in the blank area below the grid, separate from the title and Inventory Tweaks button. The prepare action has a gap beside NEI's question button. The 5x5 preview uses the installed crafting texture's recessed slots, arrow and result frame, including all nine empty interior cells. Tooltip wrapping is constrained to the active screen width.

## Verification

- `zto-tools/test-crafting-ux.bat` runs against real MCP classes and checks both split policies, a capped slot, remainders and empty/full targets, alongside the crafting transaction regressions.
- `zto-tools/test-drag-agent.ps1` verifies the injected dispatch under `-Xverify:all` with Tekkit Dev's actual modpack, Minecraft and LWJGL artifacts. It checks that the callback receives the correct screen once and that the NEI subclass still owns its mouse override.
- Live acceptance still covers left and right drags in each supported GUI, normal clicks and NEI overlays, closing mid-gesture, rapid input, display scales, and the four reported layout cases. Use an expendable world in **Tekkit Dev**.

MCP client compilation, reobfuscation, packaging, and the crafting transaction harness passed. The production probe also passed with Prism's assembled Tekkit Dev `minecraft.jar`, its Java 8u51 runtime, and the reobfuscated ZTO JAR. It observed successful registration, one injected callback, and pass-through for a non-container screen. The package was staged while a Java GUI was running, so the visual fixes and physical drag gestures have not been checked in-game on this build.
