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

The project owner confirmed the component recipes and NEI discovery in-game. Reports of missing controller binding and unfinished geometry prompted a four-way layout resolver, precise missing-part diagnostics, an opaque industrial atlas and interlocking connector geometry. These changes have **not yet been validated in Tekkit Dev**; see the [visual and binding acceptance plan](../engineering/distillation-rendering-and-binding-v2.md).

## Short test command

```powershell
.\zto-tools\test-distillation-core.bat
.\zto-tools\build-and-deploy.bat
```

Do not apply NEI or vanilla class overrides. All comments, docs, code identifiers, logs, commit messages and PR text are in English; chat with the owner may remain Italian.
