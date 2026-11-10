# Fractional distillation agent handoff

The [singleplayer acceptance matrix](singleplayer-acceptance.md) tracks current portable coverage, earlier owner observations and open live tests. The Dev Bridge can read boiler process values and heater EU and EnergyNet membership. Live batch conservation and BuildCraft transfer still need runtime scenarios.

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

The project owner confirmed recipes, NEI discovery, structure formation, LV EU intake, thermometer readouts and distillate output in Tekkit Dev on an earlier build. The latest component panels compiled, reobfuscated and deployed to Tekkit Dev. Their visual and interaction checks, including bucket feedback, remain open. See the [visual and binding acceptance plan](../engineering/distillation-rendering-and-binding-v2.md).

## Placeable petroleum fluids

All four fractions have a flowing block, a consecutive still block and a reusable bucket. The still block ID is also the BuildCraft liquid ID. Keep both block IDs stable once a world contains that fraction. Existing bucket IDs and boiler NBT product IDs are unchanged. Older receiver tiles upgrade in place on their first tick.

| Fraction | Flowing config key | Default flowing / still IDs | Ticks per flow update | Horizontal velocity retained |
| --- | --- | --- | --- | --- |
| Gasoline | `gasolineFlowing` | 3016 / 3017 | 5 | 95% |
| Kerosene | `keroseneFlowing` | 3018 / 3019 | 9 | 88% |
| Diesel | `dieselFlowing` | 3020 / 3021 | 14 | 76% |
| Heavy Gas Oil | `heavygasoilFlowing` | 3022 / 3023 | 22 | 60% |

The four fluids use distinct materials and a shared world renderer. Each spreads at most four horizontal steps from a source. A drop carries the distance already traveled. On a flat open floor a single source covers 41 cells, compared with up to 113 for vanilla water. The drag percentages describe a horizontal velocity multiplier per player tick, rather than a measured movement speed. At a boundary between liquids the strongest drag applies. Heavy Gas Oil also uses a brief vanilla Slowness effect to reduce ground acceleration and preserves any Slowness already active on the player.

For the new intake test, route oil from a BuildCraft pump or tank into the boiler and water into the condenser. Each inlet accepts only its designated liquid and clamps to its remaining 8,000 mB capacity. Try swapping the pipes and verify that the wrong liquids do not enter either tank. Existing bucket inputs share the same tanks. Test a condenser saved before the adapter was installed so its port upgrades in place.

Pour each fuel into a basin, check the source and flowing surfaces, and collect the source again. Send each fraction through a powered wooden waterproof pipe into an empty BuildCraft tank and collect it with a bucket. Save and reopen to verify world liquid, pipe contents and tank quantities. Test walking in gasoline, water and Heavy Gas Oil on the same floor.

Align the receiver flange to the pipe geometry in all four rotations. BuildCraft 2.2.14 extraction is unsided, so the endpoint is accessible from any face. Changing generic Fuel production and engine support is a later compatibility decision for existing worlds.

## Placement and wrench

Place components like a furnace, with the preferred front facing the player. Use the IC2 wrench on a horizontal face to turn a standalone part. A linked auxiliary follows the boiler so the ports line up. The boiler's preferred front breaks ties between equally complete layouts, while a better physical match wins. Wrench removal is disabled for these components.

Right-click any of the six components with an empty hand to open its live panel. The boiler shows its tanks and missing parts, the heater shows EU, the column shows batch progress, the condenser and receiver show their respective fluid, and the thermometer provides four setpoints. Supported bucket interactions still transfer directly and show their result in the panel. Held construction blocks pass through to normal placement. Keep developer diagnostics in the logs and gameplay feedback in the GUI.

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
