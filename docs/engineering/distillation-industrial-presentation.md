# Distillation industrial presentation

This alpha.1 pass keeps the refinery a provisional technology prototype under the [Project Vision](../../PROJECT_VISION.md). It gives players cues they can learn beside the machine and keeps EU, BuildCraft fluids and the Saboteur's extraction pause at the existing endpoints. The [layout study](../design/distillation-layout-alternatives.md) compares three directions for an owner review.

## World and item models

The BuildCraft 2.2.14 Tank uses a 2/16 inset on each horizontal side. Its block renderer selects side cell 96 or 97 when stacked and cap cell 98 from the BuildCraft atlas. The transparent tank shell stays in the block renderer. Its tile renderer draws the liquid separately at one of 100 cached heights. The refinery adapts the inset and stacked joint with an opaque metal model using ZTO's existing front, side and cap cells.

| Part | Changes |
| --- | --- |
| Column | 10×10-pixel chamber, four recessed edge rails and two 14×14-pixel band ends. The lower and upper bands meet as one coupling. Upper and lower fronts retain their separate cells. |
| Condenser | 12×12-pixel body, cold-side front, shallow collar and a matching lateral vapor port. |
| Receiver | 10×10-pixel body, low platform, rear outlet and neck when the Condenser occupies the cell above. |
| Boiler, Heater, Thermometer | Native furnace-oriented cubes with their existing faces. |

One native ModLoader render type handles the three shaped items and world blocks. The chunk path uses legacy face methods, pixel-aligned bounds, adjacent-face culling at cell edges and native mixed light with vanilla directional shade. The nominal connected shape emits 31 quads for the lower Column, 36 for the upper Column, 21 for the Condenser and 26 for the Receiver. The other three parts use their normal cube geometry. Ordinary face textures use the placement metadata. The collar ends use the physical neighbor direction, including across a chunk boundary. Selection, collision and placement still occupy a full cell. The view is opaque with recessed surfaces, so a gap beside a vessel is part of the occupied cell.

The item path uses the same shapes and face atlas with capped standalone forms and native item lighting. Native block ID, recipe, TileEntity and ItemBlock tests run without OpenGL. Creative, NEI, held and dropped presentation is part of the owner walkthrough.

## Operating cues

The Boiler projects the existing process result after each authoritative tick. An actual 8 EU debit marks an advancing tick. The typed state also distinguishes incomplete, ready, crude or cooling-water shortage, insufficient LV energy, full output and mixed output. A completed batch starts a separate ten-tick transfer cue. Output quantity comes from the actual Receiver volume in eight one-bucket bands. The Thermometer's small strip shows batch activity and warnings. Its GUI labels temperature as a target.

The static contours stay in the chunk mesh. One small tile renderer on the Boiler draws at most six quads. It reads the cached facing and existing process state, checks the auxiliary chunk and skips hidden faces. A four-frame heat, vapor and coolant cycle advances at one frame per four world ticks. The upper sight glass is two frames ahead of the lower one. Pausing freezes the world clock. Missing parts suppress presentation when removed, including shared auxiliaries. The existing periodic structure check still controls processing. Unload and NBT load reset the transient cue state. The next genuine step reestablishes activity. The effects have no network payload or new NBT and use the supported local singleplayer world.

The 256×256 indicator sheet consists of 16×16 cells. Rows 0 to 2 are the four heat, vapor and coolant frames. Row 3 is empty, eight fill bands, blockage and completion. Row 4 contains five progress or transfer marks and a stall mark. The source is `artwork/source/distillation/distillation_indicators.png`.

## Sound

The existing Forge sound handler registers original mono PCM WAVs from `zed/sounds/distillation/` and reinstalls them when the pool changes. The client listens only to events for its current local world. Positional playback is centered on the Boiler and follows the native 16-block attenuation. One-shots finish within 0.26 seconds, so there is no looping source or retained playback handle after shutdown.

| Clip | Trigger | Duration | Volume |
| --- | --- | --- | --- |
| `start.wav` | First successful step after an inactive tick | 0.15 s | 0.27 |
| `running.wav` | Successful step on a coordinate-staggered 79-tick cadence. Local playback admits at most one periodic pulse every 10 world ticks across refineries. | 0.23 s | 0.19 |
| `complete.wav` | Actual batch transfer into the Receiver | 0.26 s | 0.32 |

The cadence keeps the common operating cue quiet in a shared workshop. Startup and transfer cues are discrete per machine. Sound and effects keep no active machine list. The audio event usually performs only the local world check and cadence calculation. The synchronous renderer and audio callback leave all EU and tank writes with the Boiler and the existing adapters.

## Panels and assets

The six status screens use a 246×206 classic metal panel at the origin of `artwork/source/interfaces/distillation.png`. They use native text and buttons, with separate colors and explicit units for crude, coolant, product, EU and progress. The controls retain their local authority, live updates, placement fall-through and direct bucket transfers. The Heater shows demand and the last tick's EU draw. The Thermometer retains its four selection buttons. The Receiver includes blocked output, capacity and BuildCraft extraction disruption. The Air Compressor's inventory layout and the Alchemical Converter's panel remain on their existing implementations.

```powershell
python artwork/tools/distillation_assets.py
python artwork/tools/textures.py export
python artwork/tools/textures.py validate
python artwork/tools/distillation_previews.py
```

The asset script regenerates both original PNG inputs and the WAVs from fixed envelopes, damped tones and seeded noise. The manifest validates the PNG exports and the build checks the WAV entries. The local direct-sprite pass remains available under its existing [provenance policy](../../artwork/guidelines/texture-provenance.md).

## Verification and owner walkthrough

Run the layout, core and model tests, validate the artwork and build with MCP 6.2 / JDK 8. Use the pinned headless `test-zto-agent.ps1` Distillation, Underwater and Pack checks. The focused native probe submits world geometry into the actual game's tessellator without drawing, checks collision, tests registrations and calls the local GUI controls and audio event paths. It exercises actual EU/bucket/pipe transactions, stalls and region reload.

**Graphical and listening acceptance requires a disposable graphical client.** Follow the [VM policy](../agents/testing.md#background-client-launch) or use an owner-run playtest. The generated elevations and panel renders are static mockups.

1. Use the earlier build and the new artifact in the same marked test save. Capture the front, side, higher view and close-up of the Column seam with matching lighting. Check sunlight and torches beside IC2 and BuildCraft blocks. Rotate the layout four ways, remove an upper part and inspect all six items in Creative, NEI, the hand and on the ground.
2. Feed crude and water, connect a low-voltage line and follow the Heater, two Column sections and Condenser. Remove power, refill coolant, return to a compatible fraction, and block the output. Confirm that the active markers disappear, the Receiver warns on both kinds of blockage, and a genuine transfer produces one brief cue. Walk away and back, pause, cross a chunk boundary, then save and reopen a partial batch.
3. Listen for the relay, periodic pump and valve cues beside one and several installations and after changing sound settings. Check that the remaining short one-shot tail ends and no new operating cue plays after shutdown.
4. Open each screen at Small, Normal, Large and Auto. Check the text, button hits, units, transfer feedback, four setpoints, receiver disruption and close-on-distance or removal. Verify the Alchemical Converter screen and normal Air Compressor slot transfer.
