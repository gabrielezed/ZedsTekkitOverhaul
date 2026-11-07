# Fractional distillation agent handoff

**Implementation:** [engineering specification](../engineering/fractional-distillation-v1.md) and [rotated binding / visual pass](../engineering/distillation-rendering-and-binding-v2.md).

This is the first ZTO refinery machine and exercises the existing OBJ model parser, lightmap renderer, generic structure validator and TileEntity NBT patterns.

## Non-negotiable constraints

- Preserve stable block IDs and fluid IDs; do not silently reorder `DistillateType`.
- Keep pure fluid logic independent from Minecraft and IC2.
- Do not copy modern Forge `FluidRegistry`, `Capability`, `ForgeDirection` or BuildCraft 7+ adapters.
- Only `TileEntityDistillationHeater` implements the IC2 1.97 `IEnergySink` interface.
- Remember **IC2 1.97 injectEnergy returns EU not accepted**.
- Respect singleplayer-only GUI limitations until a separate networking milestone.
- Preserve the Foundry and 5×5 crafting functionality and EMC mappings.
- Do not commit production IC2 or BuildCraft binaries, decompiled sources or local configurations.

## Evidence

The model and gameplay source were authored for the legacy target; check the version-specific [IC2 API reference](../reference/tekkit-classic-3.1.2/ic2-1.97.md) and [BuildCraft 2.2.14 reference](../reference/tekkit-classic-3.1.2/buildcraft-2.2.14.md) before modifying adapters.

A source-level test or Java stub compile is **not** equivalent to a successful real Tekkit deployment. Record exact Eclipse errors, Visuals, UI response, water/oil consumption, actual EU transfer, chunk unload/reload and NBT state before promoting the first iteration from draft.

## Current debugging status

The project owner confirmed recipes, NEI discovery, structure formation, LV EU intake, thermometer readouts and distillate output in Tekkit Dev before the visual fixes. The revised atlas, thermometer controls and connector fit still require a Tekkit Dev pass. See the [visual and binding acceptance plan](../engineering/distillation-rendering-and-binding-v2.md).

## Next iteration: placeable petroleum fluids

Create a dedicated legacy fluid module for Gasoline, Kerosene, Diesel and Heavy Gas Oil. Give each fluid its own configurable block IDs, flowing and still behavior, world textures and a placeable bucket variant drawn in the style of the original Minecraft bucket. Keep the existing NBT fluid IDs and plan compatibility for existing sample buckets before changing their behavior. Verify the BuildCraft 2.2.14 liquid interfaces in matching artifacts before wiring pipe transport, then reuse the module for storage tanks and fuel consumers. The current iteration still uses internal tanks and sealed sample buckets.

## Placement and wrench

Place components like a furnace, with the preferred front facing the player. Use the IC2 wrench on a horizontal face to turn a standalone part. A linked auxiliary follows the boiler so the ports line up. The boiler's preferred front breaks ties between equally complete layouts, while a better physical match wins. Wrench removal is disabled for these components.

Use an empty hand for diagnostics and the thermometer. The supported bucket interactions still take priority, while held construction blocks pass through to normal placement.

## Short test command

For an existing `config/ZedsTekkitOverhaul-Distillation.cfg` that fails with
`unknown character _`, close Minecraft and rename `heavy_gas_oilBucket`
to `heavygasoilBucket`. Keep the
numeric value and the rest of the file so existing item and block IDs are preserved.

```powershell
.\zto-tools\test-distillation-core.bat
.\zto-tools\build-and-deploy.bat
```

Do not apply NEI or vanilla class overrides. All comments, docs, code identifiers, logs, commit messages and PR text are in English; chat with the owner may remain Italian.
