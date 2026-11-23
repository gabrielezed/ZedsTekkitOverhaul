# Classic Minecraft style

Work against the Minecraft 1.2.5 terrain and item sheets, IC2 1.97's machine and equipment sheets, and the BuildCraft 2.2.14 Core art. The contact sheets in the local cache make the comparison repeatable.

- Build block faces and inventory items at 16×16. At native resolution, reserve two or three adjacent pixels for a distinguishing detail. Use one-pixel accents for rivets, bevels and small indicators.
- Use hard pixel edges and deliberate color runs. The reference furnace and dispenser keep the aperture dark and place most surface variation near the edges. Open a reference sprite at 1× and 8× nearest-neighbor to check proportions.
- Keep metal highlights on the top and left. Vanilla block lighting supplies the large face-to-face value changes. Leave enough contrast in the art for caves and rainy daylight.
- Use an opaque face for painted glass, a recessed gauge or a black interior. Use binary alpha for equipment silhouettes, worn layers and unused atlas cells. Check the silhouettes against light, stone-grey and dark backgrounds.
- Use legacy 64×32 player armor UVs for these accessories. Confirm helmet, body and boot faces individually, including mirrored legs. Texture pixels follow the native biped mesh.

See [industrial surfaces](industrial-machinery.md), [items](inventory-icons.md) and [GUI design](gui-design.md).
