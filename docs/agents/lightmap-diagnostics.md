# Minecraft 1.2.5 lightmap diagnosis (Foundry OBJ)

## Observed issue

The external `foundry_rotor.png` is an opaque **32×16** atlas containing mid-gray and bronze pixels. The first in-game screenshots show nearly black/brown geometry despite daylight, and disabling `GL_LIGHTING` in the `TileEntitySpecialRenderer` did not change the outcome.

This suggests **missing legacy lightmap coordinates**, rather than a dark or missing source texture. Minecraft's legacy fixed-function renderer multiplies the base texture by a second texture unit (lightmap), which is not disabled by `GL11.glDisable(GL11.GL_LIGHTING)`. This cause is currently a **diagnostic hypothesis to be validated in game**.

## Isolated fix in this PR

The reusable `ObjTessellatorRenderer.render(part, packedBrightness)` method now calls:

```java
tessellator.startDrawing(GL11.GL_TRIANGLES);
tessellator.setBrightness(packedBrightness);
```

The preview Foundry TESR currently passes `0x00F000F0` to **both** independently drawn OBJ parts: the maximum lightmap coordinates (240/240). That should reveal the PNG's intended gray and bronze colors irrespective of environmental light. This is a **diagnostic full-bright preview**, not final production lighting.

No changes are made to PNG pixels, OBJ UVs, block texture, machine state, crafting or NBT.

## Tests

Run from the FoxyRetroMDK root:

```powershell
.\zto-tools\test-obj-lightmap.bat
.\zto-tools\build-and-deploy.bat
```

The first script compiles the exact OBJ parser and renderer with **test-only** stub classes (stored under `zto-tools/render-tests/stubs/`, not packaged in the mod) and verifies that per-part packed brightness is set **before** Tessellator vertices. It does **not** test actual OpenGL output. The local Java harness passed **8 assertions**.

In the isolated Prism `Tekkit Dev` instance:
1. Place at least two Foundry blocks in daylight and compare the OBJ rotor with the older screenshot.
2. Verify the static base looks gray, the rotor looks bronze, and both parts remain animated/visible.
3. Pause/resume each rotor, save/reopen the world, and confirm NBT state still persists.
4. Check that adjacent terrain, GUI and vanilla furnace block faces are not visually modified.

**If the OBJ is still almost black**, do not blindly brighten the texture or add global OpenGL state changes. Capture a screenshot and investigate secondary texture state, texture binding, UV selection and the exact MCP Tessellator implementation.

## Next step after confirmation

Switch from diagnostic full-bright to **world-aware lightmap brightness** (for example, sampling the packed light value at the model's location), while keeping explicit lightmap coordinates on every draw. Verify day, night, indoor and shaded conditions, and ensure all GL state is restored.

No global lightmap or Minecraft class patches should be required.
