# Industrial machinery

Start with an IC2-style pale steel shell and a single identifiable mechanical feature. The IC2 Macerator has a framed opening, the Compressor has a dark grille, and the BuildCraft tank and pump use simple centered mechanical fittings. See the sampled colors and original ZTO ramps in [industrial.json](../palettes/industrial.json).

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

The 256×256 Forge sheet is a 16-column grid of 16×16 cells. Columns 0–5 are Boiler, Heater, Column, Condenser, Receiver and Thermometer. Rows 0–4 are front, lateral side, top, bottom and rear. Cell (2,5) is the upper column front. The tank inspection strips and brass pipe bands read vertically in the seven-place stack. Facing metadata selects the front. The lateral pipe band recurs on both sides to support any of the four layouts. The grid coordinates are part of the Java texture contract.
