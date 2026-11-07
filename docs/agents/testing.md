# Validation and definition of done

## Evidence currently available

- **Observed in-game (owner):** Advanced Crafting Table opens with a correctly displayed 5×5 GUI; the Foundry recipe using smooth Stone is craftable.
- **Previously observed in real Tekkit (owner):** EE2 Alchemical Coal provides fuel to an IC2 Generator.
- **Implemented, not fully verified in this repository:** new block EMC mappings, multiplayer behavior, deploy script on the owner's Windows machine, and a production-compatible drag GUI hook.
- **Experimental on a feature branch:** OBJ loader and simple Foundry visual renderer (offline parser tests passed; no in-game confirmation yet).
- **Planned only:** Foundry multiblock/energy/processing, finalized external 3D model pipeline, BuildCraft refinery, more complete EE2–IC2 conversion.

Update this list only with explicit evidence and indicate the game environment, date or build if known.

## Change validation

1. `git status --short` and `git diff --check` are clean of accidental files/whitespace errors.
2. The change does not import vanilla/Forge sources, mod JARs, local saves, production `gb.class` or generated compiled output.
3. If changing Java: run MCP/Eclipse compilation. Note failures and which versions of the API were used.
4. If shipping code: run MCP reobfuscation and inspect the packaging for `zed/tekkitoverhaul/mod_ZedsTekkitOverhaul.class` and `zed/gui/advanced_table.png`.
5. If changing runtime behavior: launch the **isolated Tekkit Dev** instance and test in a disposable/copy world.
6. If changing OBJ or renderer code: run `zto-tools/test-obj-loader.bat` and verify the packaged `zed/models/` and `zed/textures/` assets; confirm the visual outcome in the Prism instance.
7. If touching an integration: verify it with the relevant installed mod and consider absence/failure paths.

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
