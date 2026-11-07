# Foundry multiblock validator prototype

This is a **Phase 0 structure-validation test** on Minecraft 1.2.5, not the final Foundry construction recipe or a processing machine. The project owner has already verified the OBJ renderer, persistent NBT rotor state and explicit OBJ lightmap brightness in real Tekkit.

## Temporary in-world structure

Place one **Foundry** controller at the center and **eight vanilla smooth Stone blocks** immediately around it, at the **same Y level**:

```text
Top view (all blocks on the same layer)

S S S
S F S
S S S

S = vanilla smooth Stone (Block.stone, metadata 0)
F = Foundry controller
```

This is a **flat 3×3 footprint**, not a 3×3×3 hollow cube. It deliberately leaves the animated rotor visible. It uses eight Stone in the built structure; the already-existing Advanced Crafting Table recipe for crafting the Foundry still uses sixteen Stone and remains unchanged.

The new structure is purely a validator test. It does not consume EU, alter recipe outputs, transform blocks, provide inventory or affect rotor speed. The controller retains its original right-click rotor pause/resume behavior.

## What happens in game

- Right-click a Foundry: its rotor still pauses/resumes, and an English chat message reports either `FORMED (8/8 Stone)` or `INCOMPLETE (N Stone missing)`.
- Replace/remove any of the eight Stone blocks and right-click again to inspect the updated result.
- Each Foundry evaluates the eight positions **relative to its own controller coordinates**.
- Adjacent block changes invalidate the current cached result, and a **20-tick periodic recheck** catches diagonal changes that do not directly notify the controller.
- Formation status is **derived from actual world blocks** after loading, not read from persistent NBT.
- The existing rotor NBT keys and block IDs are preserved.

Only a singleplayer-oriented in-game diagnostic chat is provided. Multiblock networking and client synchronization are not yet designed.

## Architecture

- `StructurePattern`: Minecraft-free structure requirements, offsets and validation results. A reusable builder supports arbitrary 3D coordinates, optional exact metadata and a wildcard metadata sentinel (`ANY_METADATA = -1`).
- `FoundryStructureValidator`: constructs this prototype's eight-block Stone ring.
- `WorldBlockView`: small adapter from Minecraft `World.getBlockId` and `World.getBlockMetadata` to the generic validator.
- `TileEntityFoundryVisual`: holds a **transient** formed/missing cache and refreshes it at most once per 20 ticks during ordinary ticking, or immediately upon player interaction/neighbor invalidation.
- `BlockFoundry`: connects vanilla neighbor notifications and right-click feedback to the TileEntity.

The validator does not edit or reserve any required blocks. This ensures that future machine logic can decide independently what it means for a structure to be formed.

## Validation

From the MDK root, run:

```powershell
.\zto-tools\test-multiblock.bat
.\zto-tools\build-and-deploy.bat
```

The portable, pure Java test uses **13 assertions**: empty/complete patterns, missing diagonal, incorrect metadata, controller exclusion, multiple independent structures, extra blocks, duplicate offsets and metadata wildcards. The feature was also compiled with temporary legacy-shaped Minecraft API stubs and passed **17 TileEntity/integration assertions** (cooldown, NBT, interaction and independent controller states). Those stub results are not a substitute for compiling against the owner's actual MCP.

### In-game acceptance checklist

1. Keep an existing Foundry with its rotor state saved; verify this continues to work.
2. Right-click an isolated Foundry: report should say **INCOMPLETE (8 Stone missing)**.
3. Build the eight-Stone ring on the Foundry's **same Y level** and right-click: report should say **FORMED**.
4. Remove a diagonal Stone and right-click: report should say **INCOMPLETE (1 Stone missing)**.
5. Restore that Stone and verify formation again.
6. Place two Foundries far enough apart; only build one ring and verify independent results.
7. Save, exit, reenter the world, and verify that the actual world structure is reevaluated. Existing rotor pause state should still persist.
8. Confirm the Advanced Crafting Table, 5×5 Foundry recipe, OBJ colors and rotating parts are unchanged.

**Test status:** offline validation passed, real MCP compile and Prism runtime verification pending. Keep the feature in a draft PR until the owner confirms it works.

## Future structure designs

For a final industrial Foundry, specify controller position, orientation, accepted block families, air/cavity requirements, dimensions and whether rotations/reflections are supported. Replace the temporary `FoundryStructureValidator` pattern without altering `StructurePattern`. Later phases can add machine inventories, EU usage, processing recipes and in-game visualization of the complete structure.
