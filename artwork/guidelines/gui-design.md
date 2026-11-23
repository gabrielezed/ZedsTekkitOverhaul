# Classic machine GUI

Use the native Minecraft font, inventory slots and click handling. A classic IC2-style interface uses a warm grey shell, a light upper-left edge, a dark lower-right edge and 18-pixel inset slot frames around 16-pixel item areas. Keep an unbroken gap between captions, numbers and hit areas.

For Air Compressor the 176×169 panel sits at the origin of a 256×256 PNG. The container binds the tank at (16,26), inventory at (8,87) and hotbar at (8,145). The oxygen bar enters at (42,33), is 120×6 at full charge and reads its fill from (0,176) in the PNG. Keep oxygen capacity in ticks and the EU buffer in EU. Use an ordinary status line for empty, charging, full and missing supply. The muted blue-grey fill and small energy symbol group the two numerical readings.

Confirm font and hit alignment at each GUI scale with an inserted tank, a full tank, an empty slot and an unpowered machine.
