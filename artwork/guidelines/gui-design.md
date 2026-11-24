# Classic machine GUI

Use Minecraft Java 1.2.5 and IC2 1.97 as the design and palette references for ZTO machine GUIs. The vanilla Furnace and IC2 Generator use the same neutral `#C6C6C6` panel, `#000000` outline, white upper-left highlight and `#555555` lower-right bevel. Inset slots and gauges use `#8B8B8B` wells with a `#373737` upper-left edge and white lower-right edge. Set ordinary labels in the native Minecraft font at `#404040`. Reserve color for the gauge fill, an instrument symbol or a status that needs attention.

Use one flat panel surface with a centered machine name. Inventory screens use native slots and 18-pixel frames around 16-pixel item areas. Status screens use native buttons and short inset gauges. Keep an unbroken gap between captions, numbers and hit areas.

The pinned local references are `artwork/references/vanilla/gui/furnace.png` and `ic2/sprites/GUIGenerator.png` in the IC2 1.97 archive under `tekkit-mods-decompiles/`. Generate the original ZTO panels with `python artwork/tools/machine_panels.py` and export using the artwork manifest.

For Air Compressor the 176×169 panel sits at the origin of a 256×256 PNG. The container binds the tank at (16,26), inventory at (8,87) and hotbar at (8,145). The oxygen bar enters at (42,33), is 120×6 at full charge and reads its fill from (0,176) in the PNG. Keep oxygen capacity in ticks and the EU buffer in EU. Use an ordinary status line for empty, charging, full and missing supply. The muted blue-grey fill and small energy symbol group the two numerical readings.

Confirm font and hit alignment at each GUI scale with an inserted tank, a full tank, an empty slot and an unpowered machine.

The 246×206 Distillation panel uses inset fluid and progress troughs and native fraction buttons across the six component screens. See [the interface and asset notes](../../docs/engineering/distillation-industrial-presentation.md#panels-and-assets).
