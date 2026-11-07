# Distillation integration and visual pass — 0.7.1 draft

**Branch:** `feature/fractional-distillation-v1`. This is an unmerged test iteration for Minecraft 1.2.5 / Tekkit Classic 3.1.2.

## Symptoms reproduced by the project owner

The temporary recipes and NEI integration work. Several clicked apparatus parts returned `No matching distillation boiler` and the controller reported incomplete formation. The components rendered, but their saturated translucent colors did not match the established IC2/BuildCraft-era look. Visually, the independent models failed to connect into one apparatus.

**Source diagnosis:** the previous `BlockDistillationPart.getBoiler` used fixed offsets and `DistillationStructure` validated only the `+X` orientation. A rotated assembly could therefore be geometrically plausible while no matching controller was found. This is not a confirmed bug in the IC2 EnergyNet API.

## Geometry contract

The seven placements remain unchanged in local controller coordinates, but can now face **EAST (+X), SOUTH (+Z), WEST (-X), NORTH (-Z)**.

```text
Controller-relative Y levels:

+2  [column] [condenser]
+1  [column] [receiver]
 0  [boiler] [thermometer]
-1  [heater] [empty]

The right-hand column follows the chosen orientation; Y never rotates.
```

`DistillationLayout` is Minecraft-free. It enumerates actual offsets, checks for missing components and resolves a clicked component to a boiler. The resolver **does not require full formation**: water intake and temperature selection are possible while the player assembles the apparatus. When several boilers could claim the same part at the same score, the resolver avoids arbitrarily choosing a different controller.

`DistillationStructure` adapts the layout to Minecraft `World.getBlockId`. The Boiler prints orientation and full missing-part coordinates. Inspecting a rotated layout picks the orientation with the most matching parts. `TileEntityDistillationPart` caches its visual orientation at 20-tick intervals; the TESR rotates both the original model and its fittings as one unit.

### Tests

```powershell
.\zto-tools\test-distillation-layout.bat
.\zto-tools\test-distillation-models.bat
.\zto-tools\test-distillation-core.bat
.\zto-tools\build-and-deploy.bat
```

The portable layout self-test covers all four facings, incomplete structures, independent boilers, controller selection and named missing parts. The tests are authored but **not yet executed against the owner's real MCP or Prism runtime**. The existing source-level model tests now load the connector OBJ and require all seven logical connector groups.

## Original texture restyle

The new `src/minecraft/zed/textures/distillation_atlas.png` retains the **64×32** dimensions and material slot order of the original V1 model UVs. It uses **fully opaque RGBA** pixels (alpha 255), with muted, shaded 8×8 tiles:

- gray steel and charcoal iron
- weathered copper and subdued brass
- blue-gray inspection panels
- dark insulation, water and petroleum
- restrained rust-red heating indicators and light ceramic

This is independently produced artwork inspired by the low-resolution **aesthetic** of IC2/BuildCraft, not a direct asset rip. The user-provided IC2 texture files have not been included in the current development workspace. Any later derivative work must record exact source filenames, licenses and credit in `CREDITS.md`.

The renderer now **disables blending for the opaque atlas** and enables alpha testing rather than blending everything. It still uses diagnostic full-bright packed lightmap coordinates to ensure correct colors; world-aware lighting is a separate future task.

## Model cohesion and soft edges

`src/minecraft/zed/models/distillation_connections.obj` adds **288 triangles** split across seven named groups:

- `boiler`: heater collar, vapor riser and thermometer pressure outlet
- `heater`: top mount and beveled housing trim
- `column_lower`: two joining sleeves and lateral branch
- `column_upper`: two joining sleeves and steam outlet to condenser
- `condenser`: steam inlet, water-cooled housing trim and receiver outlet
- `receiver`: condensate inlet and future pipe-output nozzle
- `thermometer`: boiler pressure probe and beveled display trim

These fittings extend slightly beyond their own block bounds to meet the neighboring component. The actual physical interaction blocks, recipes and collision behavior are unchanged. Their additional OBJ groups and small chamfers reduce the appearance of disconnected cubes without requiring a custom physics or Create-style renderer.

## Operational diagnostics

The boiler's right-click chat now reports **why the process is stalled** in addition to structure status and tank quantities. Reasons include: incomplete structure, insufficient crude, missing cooling water, incompatible or full receiver, missing LV heater and insufficient stored EU. In the owner's screenshot, the boiler already displayed **1,000 mB crude but 0 mB coolant**; even a fully formed assembly could not process a batch until water was provided.

This diagnostic distinguishes the **controller-binding problem** from the **independent energy/coolant prerequisites**. It does not claim that the IC2 EnergyNet connection itself has passed an in-game test.

## In-game acceptance conditions

1. Build the seven-part structure with the receiver/thermometer/condenser side facing each of the **four** horizontal directions. Right-click the boiler; all orientations should report `FORMED`.
2. Right-click the **thermometer** on a complete and incomplete structure; it should find the correct boiler and open the existing GUI.
3. Remove one upper column: the controller should list the exact missing-part coordinate; restore it and confirm the status returns to `FORMED`.
4. Verify two separate boilers do not share state.
5. Check colors in daylight and at night, especially sight windows, pipe fittings and adjacent glass/metal. No ghost-like translucency should remain.
6. Check actual pipe ends touch the upper column, condenser, receiver and boiler/heater without conspicuous gaps.
7. Retest the Boiler's oil intake, condenser's water intake, IC2 LV heater, receiver buckets, and save/reload.
8. Verify `mcmod.info` is present in the **root of the built JAR**, then check whether FML 2.2.106.176 displays it in the mod list. Metadata support is not yet runtime-verified.

**Keep PR #5 in draft** until real MCP and Tekkit Dev checks pass. A visual fix alone does not establish that the IC2 1.97 energy adapter is operating correctly.
