# Industrial machinery

When extending an existing IC2 or BuildCraft machine family, prefer direct derivation from the matching pack sprite. Crop the original 16×16 face into an editable copy and preserve its casing, bevel and material shading. Keep edits focused on the new machine's functional parts. Inspect the front, side and cap together at native size and review a pixel difference overlay. Record the source versions, coordinates, sprite hashes and operations in the [machine derivation manifest](../manifests/machine-derivations.json). Follow the [provenance policy](texture-provenance.md) before distributing the result.

Use the Macerator's framed opening, the Compressor's dark bezel and BuildCraft's centered fittings where they fit the machine's function. For new artwork, see the original ZTO ramps and sampled colors in [industrial.json](../palettes/industrial.json).

| Surface | Treatment |
| --- | --- |
| Iron and steel | Pale neutral cap, cooler side, narrow bevel and four recessed fasteners. |
| Reinforced frame | Use dark steel on a structural edge or powered unit. Keep the working surface brighter than its recess. |
| Brass and copper | Desaturated brown and gold in a port collar, cable, valve or heat coil. The chassis stays neutral. |
| Rubber and insulation | Charcoal, warm near-black edges and a few wider grey planes. |
| Glass and fluid | A dark recess, one bright glass edge and a short fill stripe. Use blue-grey for water or air, warm brown for petroleum. |
| Pipe fittings | Center a compact flange with two or three steps in the outline. Match the flange elevation across adjacent machines. |
| Indicators | A single red, amber or grey-green pixel beside its instrument. Give state and numeric values in the GUI. |

### Distillation sheet

The 256×256 Forge sheet is a 16-column grid of 16×16 cells. Columns 0–5 are Boiler, Heater, Column, Condenser, Receiver and Thermometer. Rows 0–4 are front, lateral side, top, bottom and rear. Cell (2,5) is the upper column front. Align the chamber and collar edges through the stacked column and put lateral fittings on the same centerline across adjacent machines. Facing metadata selects the front. The grid coordinates are part of the Java texture contract.
