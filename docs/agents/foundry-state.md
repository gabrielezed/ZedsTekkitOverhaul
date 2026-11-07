# Foundry state persistence prototype

This is a small **Minecraft 1.2.5 TileEntity NBT test**, intentionally separate from the future Foundry machine. It follows the working OBJ renderer prototype.

## Behavior

- Place a Foundry in a disposable **singleplayer** test world.
- Right-click the block once to **pause** the rotor.
- Right-click again to **resume**, preserving its previous rotation angle.
- Right-click once to pause, save and exit the world, reopen the same world: the rotor should **still be paused**.
- Right-click again after reopening: the rotor should resume smoothly.
- The renderer currently disables OpenGL lighting **for the OBJ only** to reveal the actual gray and bronze PNG colors. This is a **temporary unlit preview**, not a final world-lighting solution.

The vanilla furnace block texture, 5×5 Foundry recipe, item ID, EMC target and IC2/EE2 integrations remain unchanged. The Foundry has **no processing, energy or multiblock behavior** yet.

## Implementation

- `FoundryRotorState` is a dependency-free model of the visual state, using a reference world tick, frozen angle and enable flag.
- `TileEntityFoundryVisual` serializes this state with legacy `readFromNBT` / `writeToNBT` and marks its chunk dirty **only when toggled** by calling `onInventoryChanged()`. There is no per-tick disk update.
- `BlockFoundry.blockActivated` toggles state only on the authoritative world side, for the current singleplayer-oriented test.
- `RenderFoundryVisual` queries `getRotorAngle(partialTicks)`. The rotation is visual, not a physics simulation.

NBT keys under the existing TileEntity registration `ZTOFoundryVisual`:

| Key | Type | Purpose |
| --- | --- | --- |
| `ZTO_RotorRunning` | Boolean | Whether rotation is active |
| `ZTO_RotorAngle` | Float | Last angle at pause/resume |
| `ZTO_RotorReferenceTick` | Long | World tick of last transition |

Older TileEntities without these keys default to a running rotor. Do **not** rename the TileEntity registration or keys without a migration plan. The new state is not yet synchronized for remote multiplayer clients.

## Verification

From the MDK root:

```powershell
.\zto-tools\test-foundry-state.bat
.\zto-tools\build-and-deploy.bat
```

The standalone test runs **15 checks** for running, stopping, resuming, wraparound, time changes and the fields required for NBT round-tripping. Offline legacy-shaped API stubs were additionally used to compile the TileEntity/block/renderer integration and run **9 integration checks**, but this does **not** replace a real MCP 6.2 compile or Prism runtime test.

In the Prism Tekkit Dev instance verify the toggle, save/reload, other existing craftings, and whether fullbright reveals the intended gray/bronze palette. Report screenshots and any Minecraft errors. Leave the PR in draft until the runtime checks pass.
