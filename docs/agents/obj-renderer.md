# Foundry OBJ rendering prototype (experimental)

This branch tests a minimal **Wavefront OBJ → named parts → Minecraft 1.2.5 Tessellator → TileEntitySpecialRenderer** pipeline. It is a visual prototype, not the Foundry machine implementation.

## In-game behavior

- The Foundry remains craftable with **16 smooth Stone** in the 5×5 outer ring. Its numeric ID and EMC registration are unchanged.
- The block still uses vanilla furnace textures for its cube and inventory item.
- A **small rotor appears on top** of a placed Foundry. The `base` group stays still; the `rotor` group rotates about local Y using world time and partial ticks.
- No electricity, recipes, heat, inventory, collision changes or kinetic simulation are implemented.
- A lightweight TileEntity enables the legacy special renderer. Previously placed Foundry blocks may need breaking/replacing for the TileEntity to be initialized.

## Source and assets

| Path | Role |
| --- | --- |
| `src/minecraft/zed/tekkitoverhaul/render/model/ObjLoader.java` | Independent OBJ parser |
| `src/minecraft/zed/tekkitoverhaul/render/model/ObjModel.java` | Immutable named mesh parts |
| `src/minecraft/zed/tekkitoverhaul/render/client/ObjTessellatorRenderer.java` | Triangulated mesh drawing |
| `src/minecraft/zed/tekkitoverhaul/render/client/RenderFoundryVisual.java` | TileEntity special renderer with rigid animation |
| `src/minecraft/zed/tekkitoverhaul/render/TileEntityFoundryVisual.java` | Visual-only TileEntity |
| `src/minecraft/zed/models/foundry_rotor.obj` | Example model asset |
| `src/minecraft/zed/textures/foundry_rotor.png` | Example texture asset |

## Supported OBJ subset

- `v`, `vt`, `vn`, `g`, `o` and `f` records.
- `v`, `v/vt`, `v//vn` and `v/vt/vn` face references; negative relative indices.
- Triangle and quad faces, with quad triangulation on import.
- Computed flat normals when normals are absent; flipped OBJ V texture coordinate.
- Named rigid parts; each part is a separate draw call.

Not currently supported: material libraries (`.mtl` / `usemtl`), smoothing groups, polygons larger than quads, vertex colors, skeletal animation and per-material texture switching. The renderer loads and caches its model once.

## Blender export guidance

Export as **Wavefront OBJ**, triangulate faces or use quads, create UVs mapped to a single PNG, and give moving components their own group names. Treat **one Blender unit as one Minecraft block** for this prototype. The rotor's pivot is at its local origin; the TESR positions that origin at the top-center of the Foundry block.

This prototype uses a 32×16 texture atlas and two OBJ groups: `base` and `rotor`.

## Validation before merging

From the MDK root:

```powershell
.\zto-tools\test-obj-loader.bat
```

Then compile in Eclipse / MCP, close the game and run:

```powershell
.\zto-tools\build-and-deploy.bat
```

In the isolated Prism **Tekkit Dev** instance, create a disposable test world, craft a Foundry with 16 smooth Stone, place it, and confirm a visible spinning rotor above its upper face. Check inventory texture, vanilla furnace cube faces and the Advanced Crafting Table GUI. Reopen the world to test TileEntity creation/persistence.

**Test status:** offline OBJ parser tests and compilation against minimal legacy-shaped API stubs passed; the real MCP 6.2 / Tekkit runtime renderer is not yet verified. Keep the pull request in draft until confirmed.
