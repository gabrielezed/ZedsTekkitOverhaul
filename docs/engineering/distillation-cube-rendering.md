# Distillation cube rendering in Survival 0.5.1-alpha.1

This records the earlier cube pass. See [the current industrial presentation](distillation-industrial-presentation.md) for the shaped vessels, operating cues and owner walkthrough.

The legacy render type is 0. Forge binds `zed/textures/distillation_blocks.png` for the world block. The corresponding native ItemBlock uses the same sheet for inventory, hotbar, held stacks and drops. Inventory metadata zero puts the front on the vanilla furnace's visible face. World faces use the stored placement metadata. The upper column front uses two vertical neighbor lookups. The adjoining coil rails and lateral flanges are painted into the sheet.

Six block IDs, five TileEntity identifiers and the seven-place layout keep their existing meanings. Column and Thermometer share `ZTODistillationPart`. Native NBT loading and old condenser/receiver upgrade code still run. Wrench permissions and linked facing use the existing structure resolver. The render path reads the saved block front even when the physical structure resolves to another orientation. Existing non-opaque and non-normal solidity/light behavior is retained. The boiler process and IC2/BuildCraft adapters own the same data and controls. No data migration is needed.

| Part | Front and surface treatment |
| --- | --- |
| Boiler | Pale steel hatch with a copper latch and a warm inspection strip. |
| LV Heater | Dark reinforced housing and a low-orange serpentine coil. |
| Column | Vertical steel chambers and continuous joining rings with an upper instrument. |
| Condenser | Blue-grey coolant recess, four fins and flanged pipe faces. |
| Receiver | Warm liquid sight glass and an outlet collar. |
| Thermometer | Recessed round dial with a red pointer. |

The six meshes, connection mesh, material atlas, flat item atlas and TileEntity special renderer are retired. Foundry retains `RenderFoundryVisual`, the shared OBJ loader and its rotor assets. The Distillation configuration and recipes remain provisional, and the unchanged adapters leave room for later IC2 and BuildCraft integration decisions. The visual pass follows the project's approved classic Tekkit identity.

## Automated checks

```powershell
.\zto-tools\test-distillation-layout.bat
.\zto-tools\test-distillation-core.bat
.\zto-tools\test-distillation-models.bat
.\zto-tools\test-zto-agent.ps1 -Focus distillation -CandidateJar dist-zto/ZedsTekkitOverhaul.jar
```

The historical model-test entry point now verifies cube face indices, six unique fronts and the filled atlas cells. The native probe exercises registered block and item mappings, recipes, all four orientations, persisted tile identities, fluid inlets, bucket transfer, the LV batch process, save/reopen and the wooden liquid pipe. The probe runs without a display. See [artwork previews](../../artwork/README.md#alpha1-review) for contact sheets.

## Owner walkthrough

1. Use the matching mod and agent in a marked disposable Tekkit Classic 3.1.2 profile. Compare all six blocks beside an IC2 machine and a BuildCraft pump. At daylight and with only torch light, confirm six distinguishable fronts, the two-section column and the assembled port alignment. Place the apparatus facing each horizontal direction and include one incomplete structure.
2. Compare the six stack appearances in Survival, Creative, NEI, the hotbar, first and third person and as drops. Check texture packs at default scale first. If faces are missing in a third-party pack, repeat with the default pack.
3. Use a save with the earlier six block types present. Open each panel, check the same IDs and prior contents, use the wrench on a standalone block, and reload. Confirm the thermometer controls, oil and water bucket transfers, LV heater, batch completion and extraction through a powered wooden waterproof pipe.
