# Survival 0.5.1-alpha.1 — Underwater equipment

This candidate turns existing Tekkit resources and EU into expedition time. A player can prepare at a coastal outpost, explore ordinary water and return to recharge. The underwater biome, structures and enemies are later milestones. Equipment reads the native water conditions, so later regions can build on the same equipment without coupling it to terrain generation.

## Equipment and recipes

The initial balance is provisional. The equipment grants no combat armor points and uses vanilla helmet, chest and boots slots. The Oxygen Tank competes with an IC2 Jetpack or chest armor. Damaged chestplates and Rubber Boots do not satisfy these exact metadata-zero recipes.

| Output | Rows, top to bottom | Ingredients |
| --- | --- | --- |
| Underwater Mask | ` G ` / `R R` / ` L ` | `G` Glass Pane, `R` IC2 Rubber, `L` Leather |
| Oxygen Tank | `ACA` / `ALA` / `   ` | `A` Advanced Alloy, `C` Empty Cell, `L` Leather Chestplate |
| Flippers | `CBC` / `C C` / `   ` | `C` Carbon Plate, `B` Rubber Boots |
| Air Compressor | `P P` / `CMC` / `C C` | `P` Diamond Transport Pipe, `C` Empty Cell, `M` IC2 Compressor |

IC2 1.97's `ic2.api.Items.getItem(String)` returns stacks from `Ic2Items`. The keys are `rubber`, `cell`, `advancedAlloy`, `rubberBoots` and `carbonPlate` (metadata 0), and `compressor` (metadata 5, established by `BlockMachine`). BuildCraft Transport 2.2.14 registers `BuildCraftTransport.pipeItemsDiamond` as a `PipeItemsDiamond` item with metadata 0. Its ID comes from the pack configuration. Vanilla Glass Pane uses `Block.thinGlass` (ID 102). FML orders ZTO after IC2 and BuildCraft Transport so their ingredients are available to the recipe registry.

| Config key | Default | Saved ID |
| --- | ---: | ---: |
| `block.airCompressor` | 3031 | 3031 |
| `item.underwaterMask` | 22012 | 22268 |
| `item.oxygenTank` | 22013 | 22269 |
| `item.flippers` | 22014 | 22270 |

Existing canonical configurations receive missing keys. Current values, comments and unrelated entries are preserved. Startup checks the block and item tables before registration and reports conflicts. Keep assigned IDs stable once a world contains the new equipment.

## Breathing, movement and visibility

In Minecraft 1.2.5, `EntityLiving.onEntityUpdate` decrements native air only for a living entity whose eyes are in water and who lacks native breathing or the Water Breathing potion. The single ZTOAgent preserves the decrement call and presents its result to the equipment callback before native drowning is evaluated. An equipped charged tank on the owning simulation keeps the current air (or restores it to zero if already in the drowning interval) and spends one tick of oxygen. Creative and invulnerable players keep their tank charge. Existing potion, Respiration, surfacing and ordinary drowning behavior remain in the native method. There is no player-side oxygen timer.

A tank is a single-item stack with integer NBT `ZTO_OxygenTicks`; absent or mistyped values read as zero, and values are bounded to 0–6000. Crafting produces an empty tank. The tooltip shows remaining ticks and the 20 TPS conversion. A full tank provides five minutes of supplemental air at 20 TPS.

For flippers, the agent adjusts the input thrust to the water branch of `EntityLiving.moveEntityWithHeading` by 1.25. The vanilla collision, drag, gravity and vertical input remain in place. On the pinned OptiFine renderer, the mask divides the water fog density by 3.50 for the local equipped view entity. The old 1.30 divisor only reduced native density by 23%. Disassembly confirmed that it already targeted the correct site. The pinned matcher now identifies the native water-material branch and the Clear Water join feeding `GL_FOG_DENSITY`. Native exponential density 0.1 becomes approximately 0.02857, and Clear Water density 0.02 becomes approximately 0.00571. The branch uses the native projected camera material, so it applies in occupied Kelp water. Native fog mode, tint, potion adjustments and land settings are kept. Fog reaches farther through the water, but cannot reveal geometry that the native lightmap renders nearly black. OptiFine's pinned `lr.e()V` uploads a 16 × 16 RGB lightmap through either its custom colorizer or the native sky/block brightness and gamma calculation. The agent tracks both native uploads and samples the same projected camera block in `lr.a(IF)V` that selects water fog. When the local view wears the mask in native water material, the mod lifts each uploaded color channel by 36% of its remaining range. It uses a separate buffer and preserves texture binding. A native lightmap refresh reapplies the lift on the next applicable fog pass. Air, lava, another camera and helmet removal restore native colors on that pass. World light, gamma, block textures and spawning are unchanged. The agent accepts the exact pack SHA-256 fingerprints for `acq` and `lr`. An absent callback or rejected binary keeps the corresponding native effect. Each item selects its original worn layer through Forge's `IArmorTextureProvider`; flippers use biped boot geometry.

## Air Compressor

The block accepts 32 EU or smaller packets on all six faces through IC2 1.97's `IEnergySink`. It buffers up to 4,000 EU and returns the remainder of each offer. A loaded machine spends 1 EU per active tick for up to 4 oxygen ticks, including a capped final cycle. An empty tank takes 1,500 EU and 75 seconds at 20 TPS. Full or absent tanks stop charging. The machine can still buffer available input until the EU capacity is reached.

The machine owns one stack and its buffer. Tile entity `ZTOAirCompressor` stores `ZTO_CompressorTank` as the native stack compound and `ZTO_CompressorEU` as an integer. The EnergyNet connection is rebuilt after loading and removed on invalidation or chunk unload. The singleplayer container shares the native tile with the GUI. The GUI displays a charge bar, numeric oxygen and EU values, status and the player inventory. Shift transfer moves one stack to a free slot without merging. The Forge sided inventory exposes zero automation slots because legacy item pipes cannot query a tank-only filter. There is no IC2 wrench integration.

Original PNGs are packaged under `zed/textures/underwater/` (mask, oxygen tank, flippers, three worn layers, and compressor cap, side and grille) and `zed/gui/air_compressor.png`. New placements store the player-facing grille in horizontal block metadata using the vanilla Furnace convention (yaw quadrants map to sides 2, 5, 3 and 4). Existing metadata-zero machines keep their original pair of grilles and their saved contents. The item view uses one grille. The build checks each resource inside the archive.

The alpha.1 visual pass gives the compressor an iron bezel, louvered grille, pressure gauge and the light neutral palette of the vanilla Furnace and IC2 Generator. Equipment uses a glass-and-rubber mask with a leather strap, a steel reservoir with a harness and brass valve, and charcoal flippers with carbon blades. The worn layers use 64×32 biped UVs. The slot coordinates, charging and equipment gameplay use their existing implementations. See the [GUI design guide](../../artwork/guidelines/gui-design.md), [Distillation rendering](../engineering/distillation-industrial-presentation.md) and [build watermark](../engineering/build-watermark.md).

## Validation and owner walkthrough

Run `zto-tools/test-zto-agent.ps1 -Focus underwater -CandidateJar dist-zto/ZedsTekkitOverhaul.jar` from the workspace root. The disposable headless fixture uses the pinned game overlay, IC2 1.97 and BuildCraft 2.2.14 artifacts. It checks native shaped recipes and metadata, hook linkage, local fog density selection and worn resource paths. A fixture copy of LWJGL and the headless RenderEngine record actual lightmap uploads and `lr.a(IF)V` fog calls for native water, Clear Water, Kelp, helmet removal, another camera, lava and air without creating an OpenGL context. The player tests cover finite breathing, native drowning, shallow water, Water Breathing, malformed charge data, the charged death drop and matched 20-tick swims with a wall collision check. The compressor tests cover real generator-to-copper-cable EU delivery, capacity remainders, normal and Shift-click slot transfers, disk chunk reload, breaking and placing a replacement. The test client substitutes texture loading and runs without a window. The headless BuildCraft path calls the actual diamond item registration without constructing OpenGL renderers.

1. In a marked disposable Tekkit Classic 3.1.2 save, launch with the matching mod and agent pair. Craft the mask, tank, flippers and Air Compressor in a vanilla crafting table. Check the recipes in NEI.
2. Place the compressor while facing each horizontal direction and check that the grille faces you. Connect an IC2 Generator through insulated copper cable. Open the GUI with an empty hand and an ordinary held item. Confirm the tank slot rejects unrelated items and that a Shift-click inserts one tank.
3. Charge the tank, retrieve it and equip all three items. In third person inspect the mask, harness, and boots. Compare the same submerged view with and without the mask, then surface and confirm ordinary fog.
4. Compare a short horizontal swim with and without flippers. Enter and leave the water. Check remaining oxygen in the tank tooltip and verify a nearly empty tank returns to ordinary drowning behavior when exhausted.
5. Recharge a partially depleted tank. Save/Quit with a tank inserted and reload. Verify oxygen and EU, confirm charging resumes, and break the compressor with a tank inside. Recover one tank with the saved charge.
6. Sample the existing inventory gestures, NEI and Inventory Tweaks against the [compatibility acceptance matrix](compatibility-acceptance.md).
7. Check the Compressor panel, slot frames and oxygen bar at GUI scales Small, Normal, Large and Auto. Review the status with an empty slot, a partially charged tank, a full tank and no EU. Inspect the three equipment sprites next to the IC2 Rubber Boots and compare the worn layers in daylight and torch light.

Graphical rendering, live input, full launcher startup and the owner walkthrough need an isolated guest or a manual session. The implementation targets classic singleplayer. Dedicated multiplayer GUI synchronization and off-profile OptiFine binaries require separate validation.


### Fixed camera mask comparison

In a disposable Creative Default Overworld, find a straight stretch of ordinary ocean at least 12 blocks deep. Clear Water should be off for the first comparison. Use F3 to record the coordinates and choose a fixed view looking due east at Y54. Place distinguishable Stone markers at X +5, +10, +20 and +30 relative to the camera, at the same eye level, and place a Glowstone block near the +10 and +20 markers. At each marker build a short stone post from the seabed. Plant Kelp or Ribbon Algae toward the foreground over abyssal material, and leave an unlit overhang or a deeper stone alcove in the same view.

Use the same player coordinates, yaw, pitch and lighting for both captures. Toggle the mask in the helmet slot and repeat with Clear Water on. At the surface, compare a view with the camera above the water. Repeat below the surface in third person and inside occupied Kelp. The lit markers and nearby stone textures should be readable with the mask. The unlit alcove should remain visibly darker than the lit surfaces. Repeat by diving at X -965, Z 796 around Y27 in a fresh Default world with seed 32021. Owner visual acceptance is pending.
