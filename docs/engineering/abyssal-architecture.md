# Abyssal architecture

Revision three places the Observatory beneath the existing Trench seabed. The survey samples the pinned native terrain provider before claiming chunks. It chooses the highest common floor that fits under at least three blocks of natural cover over the principal rooms. A short entry shaft opens between eroded supports and leads to a stepped gallery, recessed observation hall, side return and tall Custodian chamber. The site leaves the surrounding seabed profile in place. The deepest chamber floor stays above the native lava-bearing cave zone.

The old layout hashes, physical revision-one and revision-two plans, finite slot masks and Custodian records keep their original interpretation. A newly reserved site never writes into a previously loaded or stored footprint. A native slice containing foreign blocks is vetoed rather than carving through them. See [Sunken Observatory](sunken-observatory.md) for the site contract.

## Building palette

All IDs are the configurable default block IDs in the local pack.

| ID | Block | Recipe |
| ---: | --- | --- |
| 3044 | Abyssal Brick Stairs | Six Abyssal Bricks in a stair pattern yield four. |
| 3045 | Eroded Abyssal Brick Stairs | Six Eroded Abyssal Bricks in a stair pattern yield four. |
| 3046 | Abyssal Brick Slab | Three Abyssal Bricks in a row yield six. |
| 3047 | Abyssal Pillar | Three Abyssal Bricks vertically yield three. |
| 3048 | Carved Abyssal Bricks | Shapeless Abyssal Brick and Shellstone yield one. |
| 3049 | Abyssal Frieze | Three carved bricks in a row yield three. |
| 3050 | Abyssal Grate | Four eroded bricks around an empty center yield three. |
| 3051 | Abyssal Pedestal | A pillar between two slabs yields one. |

Stairs use the native stair renderer and stepped collision. Generated stairs carry the longitudinal orientation in native metadata. Slabs are low half-block ledges. The grate is a low pierced-texture plate with physical plate bounds, and the pedestal has inset bounds. Interior circulation checks use blocks fully occupied by source water. The matched agent masks OptiFine water quads against submerged stairs, slabs, grates and pedestals. It checks local water continuity before suppressing a contact face. The correction also applies in existing saves because generation, block identities, fluid metadata and collision stay native. The two owner screenshots showed source water at the stair-gallery slabs and around the pillar bases. Native fixture coverage includes each direction, an exposed pool, open water and lava. The visual playtest remains separate.

The lamp retains block ID 3038 and emission level five. Its front uses a narrow mineral aperture set directly into the Abyssal Brick face. Caps and rear faces use masonry borders. Revision-three lamps face the open passage, in paired supports or recessed chamber positions. Historical metadata-zero lamps retain a lit aperture on the four lateral faces.

`python artwork/tools/abyssal_architecture_assets.py` derives the new atlas cells directly from the owned masonry pixels and produces an architectural texture sheet. `python artwork/tools/textures.py export` updates the runtime atlas. The pinned Observatory probe writes native seabed projections, flooded interior plans and cross-sections into `dist-zto/observatory-refinement-previews`.
