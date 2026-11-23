# ZTO artwork

Use the [classic style](guidelines/classic-minecraft-style.md), [industrial materials](guidelines/industrial-machinery.md), [inventory icons](guidelines/inventory-icons.md), [GUI](guidelines/gui-design.md) and [provenance](guidelines/texture-provenance.md) for texture work.

- `source/` contains the editable ZTO PNGs. [assets.json](manifests/assets.json) maps each source, dimensions, alpha, role and ownership to its runtime destination.
- `references/` contains local third-party images and per-import `index.json` records. The records include archive identity, SHA-256 when an archive is available and resource SHA-256 by original path. The [reference manifest](manifests/reference-sources.json) pins the sources and filters.
- `palettes/` contains sampled reference families and the original ZTO ramps.
- `previews/` contains local contact sheets and comparison work. Both this directory and the reference cache are ignored by Git.
- `src/minecraft/zed/textures/` and `src/minecraft/zed/gui/` contain exported runtime assets. Edit the source PNG first and run export. Existing artwork unrelated to this pass can migrate incrementally.

## Windows quick start

Run these commands from the repository root. The tools were tested with Python 3.15 and the pinned Pillow release.

```powershell
python -m venv artwork/.venv
$artPython = 'artwork/.venv/Scripts/python.exe'
& $artPython -m pip install -r artwork/requirements.txt
& $artPython artwork/tools/textures.py vanilla
& $artPython artwork/tools/textures.py mods
& $artPython -m unittest discover -s artwork/tools -v
```

The vanilla command resolves 1.2.5 through Mojang's version manifest and verifies the published SHA-1 before using the cached download. When offline, add `--archive 'C:/path/to/minecraft-1.2.5-client.jar'` with the same unmodified client. For mods, use the local extracted catalog or add `--mods-dir 'C:/path/to/Tekkit Dev/minecraft/mods'`. The BuildCraft Core ZIP contains the shared Core, Transport, Factory and Energy art. The three other modules still receive provenance records.

## Inspect, compare and export

```powershell
$textureTool = 'artwork/tools/textures.py'
& $artPython $textureTool inspect artwork/references/vanilla/terrain.png
& $artPython $textureTool extract artwork/references/vanilla/terrain.png 12 2 artwork/previews/furnace-front.png
& $artPython $textureTool preview artwork/previews/vanilla-terrain.png artwork/references/vanilla/terrain.png --atlas --columns 16
& $artPython $textureTool preview artwork/previews/vanilla-items.png artwork/references/vanilla/gui/items.png --atlas --columns 16
& $artPython $textureTool preview artwork/previews/ic2-machines.png artwork/references/ic2/ic2/sprites/block_machine.png --atlas --columns 16
& $artPython $textureTool preview artwork/previews/buildcraft-machines.png artwork/references/buildcraft-core/net/minecraft/src/buildcraft/core/gui/block_textures.png --atlas --columns 16
& $artPython $textureTool palette artwork/previews/sample-colors.json 'artwork/references/ic2/ic2/sprites/block_machine.png@5,3'
& $artPython $textureTool export
& $artPython $textureTool validate
& $artPython $textureTool validate --jar dist-zto/ZedsTekkitOverhaul.jar
```

Use individual `image.png@column,row` arguments for compact comparison sheets. Use `--scale 1` for native size and `--scale 8` for pixel inspection. The contact sheet preserves opaque RGB values and uses a checkerboard under transparent pixels.

For a new atlas, provide a JSON file with `{"cell":16,"size":[256,256],"sprites":[["boiler_front.png",0,0]]}`. Locate the relative PNG paths beside the layout and run `textures.py build-atlas layout.json output.png`. Keep the approved output under `source/`, document the cell map and validate the populated cells in the asset manifest. Keep authoring inputs and generated sheets under the appropriate paths for their ownership.

The normal MCP build checks source/export byte equality and packaged entry names directly from the manifest with PowerShell. Reference downloads and Python are used during artwork development.

## Alpha.1 review

The local comparison script builds the six native-style item projections, an assembled elevation, and before/after sheets for the underwater icons, worn UVs, compressor and GUI. To supply the historical images, extract the earlier ZTO PNGs into `previews/before/` using their existing filenames, then run `artwork/tools/alpha1_previews.py` with the same Python environment. See the [renderer walkthrough](../docs/engineering/distillation-cube-rendering.md) for in-game checks.

### Local original-sprite machine study

Build the Air Compressor and Distillation texture variants from the pinned Tekkit Classic archives. This writes four editable PNGs to `source/`, marks their provenance as local, and writes comparison images to the ignored `previews/` directory. Export the PNGs for a local candidate. Keep the resulting PNGs, mixed comparisons and candidate JAR local until redistribution permission is documented.

```powershell
& $artPython $textureTool mods
& $artPython artwork/tools/derive_machines.py references
& $artPython artwork/tools/derive_machines.py build
& $artPython artwork/tools/derive_machines.py check
& $artPython $textureTool export
& $artPython $textureTool validate
```

The [derivation manifest](manifests/machine-derivations.json) pins each original atlas and sprite. Sprite SHA-256 values use 16×16 RGBA bytes in row-major order. Atlas coordinates are zero-based from the upper left. Edited bounds are measured within the face and exclude the right and bottom endpoints. The comparison sheets show exact face colors, orange for edited pixels, and separately labeled static cube projections. The caps, sides and rears intentionally retain complete original faces in some cases.

`previews/industrial-source-sprites.png` identifies the source block faces, pipe cells and refinery UVs. Compare `compressor-original-vs-zto.png`, `distillation-original-vs-zto.png`, `distillation-complete-apparatus.png` and `industrial-machines-comparison.png`. Use the sheets ending in `-native.png` for a one-pixel-per-texel inspection. The inventory, hotbar, held item and placed-block checks in Minecraft still use the owner walkthrough.
