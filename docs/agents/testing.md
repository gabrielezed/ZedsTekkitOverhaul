# Validation and definition of done

## Evidence currently available

- **Observed in-game (owner):** Advanced Crafting Table opens with a correctly displayed 5×5 GUI; the Foundry recipe using smooth Stone is craftable.
- **Previously observed in real Tekkit (owner):** EE2 Alchemical Coal provides fuel to an IC2 Generator.
- **Observed in-game (owner):** Foundry right-click rotor toggle and independent NBT state across multiple placed blocks survive saving and reopening the world.
- **Observed in-game (owner):** the explicit full-bright OBJ lightmap fix resolves the previously dark rotor colors.
- **Implemented, not fully verified in this repository:** new block EMC mappings, multiplayer behavior, a production-compatible drag GUI hook and the proposed multiblock validator.
- **Observed in-game (owner):** OBJ model loads in Tekkit and the Foundry rotor animates. The initial dark colors were corrected by setting explicit Tessellator lightmap brightness.
- **Implemented on a feature branch, not yet tested in real Tekkit:** a temporary 3×3 Foundry Stone ring validator with isolated Java tests.
- **Planned only:** final Foundry multiblock architecture, EU energy/processing, production world-aware OBJ lighting, BuildCraft refinery, more complete EE2–IC2 conversion.

Update this list only with explicit evidence and indicate the game environment, date or build if known.

## Change validation

1. `git status --short` and `git diff --check` are clean of accidental files/whitespace errors.
2. The change does not import vanilla/Forge sources, mod JARs, local saves, production `gb.class` or generated compiled output.
3. If changing Java: run MCP/Eclipse compilation. Note failures and which versions of the API were used.
4. If shipping code: run MCP reobfuscation and inspect the packaging for `zed/tekkitoverhaul/mod_ZedsTekkitOverhaul.class` and `zed/gui/advanced_table.png`.
5. If changing runtime behavior: launch the **isolated Tekkit Dev** instance and test in a disposable/copy world.
6. If changing OBJ or renderer code: run `zto-tools/test-obj-loader.bat` and verify the packaged `zed/models/` and `zed/textures/` assets; confirm the visual outcome in the Prism instance.
7. If changing structure validation: run `zto-tools/test-multiblock.bat`, then test the 3×3 ring in Prism and check multiple controllers and world reload.
8. If touching an integration: verify it with the relevant installed mod and consider absence/failure paths.

## Regression checklist

- [ ] Mod starts without a crash in supported environments.
- [ ] Existing saved worlds still resolve configured block IDs.
- [ ] Advanced Crafting Table is crafted with four vanilla crafting tables.
- [ ] GUI opens and the 25 input slots, result and inventory slots work.
- [ ] Sixteen **Stone** along the perimeter, with a clear center, produce one Foundry.
- [ ] Crafting consumes exactly the required ingredients; closing returns unused items.
- [ ] Foundry can be placed and rendered; do not expect machine behavior yet.
- [ ] Existing EE2 fuels retain their expected IC2 Generator behavior.
- [ ] EMC values are checked explicitly in real EE2 before being marked verified.
- [ ] NEI works unchanged when testing any future GUI hook.

A documentation-only commit does not require claiming a successful Minecraft build. Never fabricate test output.

## Fractional distillation V1 prototype (feature branch)

The fluid state core and six OBJ models have portable Java tests. Before a merge, run `zto-tools/test-distillation-core.bat` and `zto-tools/test-distillation-models.bat` in the MDK workspace; compile using the actual MCP/Forge sources, then deploy to the isolated Prism instance. Check IC2 1.97 LV energy input, seven placement offsets, four thermometer setpoints, 1000 mB bucket transactions, NBT persistence and existing Foundry/GUI regressions. This feature is **not** runtime verified. See [engineering specification](../engineering/fractional-distillation-v1.md).

## Distillation binding and visual update

The owner confirmed recipe registration and NEI compatibility in Tekkit Dev, but reported controller binding failures, an incomplete structure and visually translucent saturated machine components. PR #5 now includes four-way layout resolution, exact missing-part diagnostics, opaque 64×32 atlas and 288-triangle connecting fittings. **No real MCP compile or Prism re-test has yet been reported for these changes.** Run `zto-tools/test-distillation-layout.bat` and `zto-tools/test-distillation-models.bat`, then the usual build/deploy and the [visual acceptance checklist](../engineering/distillation-rendering-and-binding-v2.md).
