# ZTO artwork

Use the [classic style](guidelines/classic-minecraft-style.md), [industrial materials](guidelines/industrial-machinery.md), [inventory icons](guidelines/inventory-icons.md), [GUI](guidelines/gui-design.md) and [provenance](guidelines/texture-provenance.md) for texture work.

- `source/` contains the editable ZTO PNGs. [assets.json](manifests/assets.json) maps each source, dimensions, alpha, role and ownership to its runtime destination.
- `references/` contains local third-party images and per-import `index.json` records. The records include archive identity, SHA-256 when an archive is available and resource SHA-256 by original path. The [reference manifest](manifests/reference-sources.json) pins the sources and filters.
- `local-dev/<profile manifest SHA-256>/` contains validated local derivatives at their final archive paths. It is ignored by Git and independent of ordinary source exports.
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
& $artPython $textureTool validate --jar dist-zto/public/ZedsTekkitOverhaul.jar
```

Use individual `image.png@column,row` arguments for compact comparison sheets. Use `--scale 1` for native size and `--scale 8` for pixel inspection. The contact sheet preserves opaque RGB values and uses a checkerboard under transparent pixels.

For a new atlas, provide a JSON file with `{"cell":16,"size":[256,256],"sprites":[["boiler_front.png",0,0]]}`. Locate the relative PNG paths beside the layout and run `textures.py build-atlas layout.json output.png`. Keep the approved output under `source/`, document the cell map and validate the populated cells in the asset manifest. Keep authoring inputs and generated sheets under the appropriate paths for their ownership.

## Build profiles

The normal `.\zto-tools\build-and-deploy.bat` uses **local-dev** and installs it in Prism `Tekkit Dev`. The build checks the tracked baseline exports, verifies the approved overlay and generates missing local files automatically. It verifies the packaged PNG bytes, then records the profile, the combined artwork fingerprint and the SHA-256 of each manifest PNG in both build sidecars. The mod also embeds these fields in `zto-build.properties`.

The first local build in a fresh workspace needs Python with [the pinned requirements](requirements.txt) and the IC2 1.97 and BuildCraft Core 2.2.14 archives. The build finds Python in `artwork/.venv`, via `py`, or via `python.exe`. Set `ZTO_ART_PYTHON` to use a specific executable. The default donor location is `Tekkit Dev/minecraft/mods`. Set `ZTO_ART_REFERENCE_MODS` to supply the same archives from another local installation. Verified donor images already present under `references/` can also supply the inputs. A missing or incompatible donor stops the build before publishing or installing the new archives.

Set `ZTO_ARTWORK_PROFILE=public` explicitly to build the tracked redistribution baseline into `dist-zto/public`. This also excludes the private Hopper and Creative texture groups and skips Prism installation. Check either packaged profile directly with `zto-tools/check-artwork.ps1 -Profile <profile> -Archive <jar>`. [Deployment and staging](../docs/deploy-prism.md)

## Alpha.1 review

The local comparison script builds the six native-style item projections, an assembled elevation, and before/after sheets for the underwater icons, worn UVs, compressor and GUI. To supply the historical images, extract the earlier ZTO PNGs into `previews/before/` using their existing filenames, then run `artwork/tools/alpha1_previews.py` with the same Python environment. See the [renderer walkthrough](../docs/engineering/distillation-cube-rendering.md) for in-game checks.

### Local original-sprite machine study

The [refinery visual pass](../docs/engineering/distillation-industrial-presentation.md) uses the original face map for both shaped vessels and cube components. Run `python artwork/tools/distillation_previews.py` for layout studies, operating states and GUI mockups. See [the three layout proposals](../docs/design/distillation-layout-alternatives.md).

The normal build prepares the Air Compressor and Distillation variants. For artwork review, run the same derivation directly. It writes only the approved, ignored profile overlay and the requested previews.

```powershell
$modReferences = "$env:APPDATA/PrismLauncher/instances/Tekkit Dev/minecraft/mods"
& $artPython artwork/tools/derive_machines.py build --mods-dir $modReferences --preview
& $artPython artwork/tools/derive_machines.py check --mods-dir $modReferences
```

The [derivation manifest](manifests/machine-derivations.json) pins each original atlas and sprite. The [local profile manifest](manifests/local-dev.json) pins each full image and its face map, including the exact PNG recovered from the approved playtest. Sprite SHA-256 values use 16×16 RGBA bytes in row-major order. Atlas coordinates are zero-based from the upper left. Edited bounds are measured within the face and exclude the right and bottom endpoints. The comparison sheets use orange for edited pixels. The caps, sides and rears retain complete original faces in some cases. Revise the face map, recipe and pins together when approving a new machine variant.

`previews/industrial-source-sprites.png` identifies the source block faces, pipe cells and refinery UVs. Compare `compressor-original-vs-zto.png`, `distillation-original-vs-zto.png`, `distillation-complete-apparatus.png` and `industrial-machines-comparison.png`. Use the sheets ending in `-native.png` for a one-pixel-per-texel inspection. The inventory, hotbar, held item and placed-block checks in Minecraft still use the owner walkthrough.

## Abyssal environments

Original block and item atlases are in `source/abyssal/` and exported to both profiles. Run `py artwork/tools/abyssal_previews.py` to regenerate the local review sheets and underwater scene. See the [underwater style guide](guidelines/underwater-environments.md) and [atlas layout](../docs/engineering/abyssal-decoration.md#atlas-and-model).

## Motorboat

The authored 64×32 engine UV sheet and two 16×16 sprite layers are in [`source/motorboat/`](source/motorboat/). The [asset manifest](manifests/assets.json) maps these PNGs and the fueling panel to their packaged paths. The installed client composes the vanilla boat cell (8,8) under the motor overlay and uses the stock boat hull texture directly.

The engine and sprites sample the installed textures at runtime. Coordinates are zero-based and all sampled pixels are opaque.

| Installed reference | Samples |
| --- | --- |
| BuildCraft 2.2.14 Core, `net/minecraft/src/buildcraft/energy/gui/base_iron.png` | Bright iron (20,4), bevel (0,17), dark frame (21,12) |
| IC2 1.97, `ic2/sprites/block_machine.png` | Compressor casing (94,56), shaded casing (95,62), mechanical panel (82,58) |

Run `python artwork/tools/motorboat_assets.py` to regenerate the editable motor PNGs and the original synthesized running cue. Run `python artwork/tools/machine_panels.py` for the GUI, followed by `python artwork/tools/textures.py export` and `python artwork/tools/textures.py validate`. Keep any composed comparison sheet under ignored `artwork/previews/`.
