# Abyssal decoration

Survival 0.5.1-alpha.2 Part I is a work in progress based on the Motorboat and local artwork branch from PR #51 at `1effc47c42727a38653dc7a2d706733612d96b88`. These materials support the project's environmental storytelling through recoverable ocean geology, recognizable marine silhouettes and submerged masonry. The content registers independently of terrain generation. Use Creative or NEI to obtain the initial building materials and propagules for a disposable Survival fixture.

| Block | Config key after `block.` | Default ID | Normal harvest | Block light |
| --- | --- | ---: | --- | ---: |
| Abyssal Stone | abyssalStone | 3032 | Pickaxe, 1 stone | 0 |
| Abyssal Silt | abyssalSilt | 3033 | Shovel effective, 1 silt | 0 |
| Shellstone | shellstone | 3034 | Pickaxe, 1 Shellstone | 0 |
| Petrified Coral | petrifiedCoral | 3035 | Pickaxe, 1 coral block | 0 |
| Abyssal Bricks | abyssalBricks | 3036 | Pickaxe, 1 brick block | 0 |
| Eroded Abyssal Bricks | erodedAbyssalBricks | 3037 | Pickaxe, 1 eroded brick block | 0 |
| Abyssal Lamp | abyssalLamp | 3038 | Pickaxe, 1 lamp | 5 |
| Abyssal Kelp | abyssalKelp | 3039 | 1 Kelp Frond per segment | 0 |
| Ribbon Algae | ribbonAlgae | 3040 | 1 Ribbon Algae Cutting | 0 |
| Deepwater Fan Coral | deepwaterFanCoral | 3041 | 1 Fan Coral Fragment | 0 |
| Tube Sponge | tubeSponge | 3042 | 1 Sponge Cutting | 0 |
| Ghost Anemone | ghostAnemone | 3043 | 1 Luminous Polyp | 3 |

| Item | Config key after `item.` | Constructor ID | Stack ID |
| --- | --- | ---: | ---: |
| Kelp Frond | kelpFrond | 22017 | 22273 |
| Ribbon Algae Cutting | ribbonAlgaeCutting | 22018 | 22274 |
| Fan Coral Fragment | fanCoralFragment | 22019 | 22275 |
| Sponge Cutting | spongeCutting | 22020 | 22276 |
| Luminous Polyp | luminousPolyp | 22021 | 22277 |
| Abyssal Shell | abyssalShell | 22022 | 22278 |

ItemBlocks use their block IDs. Migration appends absent keys and preserves explicit assignments. Startup checks both native registries for occupied slots and detects duplicate effective stack IDs before creating the new entries. Voluntary ID changes belong in fresh instances because saved blocks and inventory refer to their numeric assignments.

## Crafting

A 2×2 square of Abyssal Stone yields four Abyssal Bricks. Shapeless recipes yield one result each. Bricks plus Sand gives Eroded Abyssal Bricks. Stone plus Luminous Polyp gives Abyssal Lamp. Shellstone gives an Abyssal Shell. Stone plus a Shell reconstructs Shellstone and consumes both inputs. The recipes use the native manager and introduce no EMC overrides.

## Water occupancy

A marine block replaces a vanilla water source below another water or marine cell. It uses the native water material and full-depth fluid metadata to preserve eye submersion, swimming and the water light opacity. The native bucket source check requires metadata zero. A marine cell persists as data value 8, with value 9 reserved for the perpendicular Fan Coral orientation. The upper Kelp sprite is derived from its neighbor. A column can have one to four segments over a natively solid upper face. The other organisms root directly on a natively solid upper face. Both item entry points use the same placement method. Neither organism growth nor terrain seeding is scheduled.

Player removal and support-loss removal restore a vanilla source when an adjacent or overhead native source is still present. With no such source the cell becomes air and normal fluid updates determine refill. Vanilla direct writes and explosions use native neighbor updates for refill. The block-removal callback runs inside the pending chunk write, so replacing the cell reentrantly would disrupt the original write. Marine cell flow physics use the legacy falling-fluid metadata convention. Clear-water borders, isolated water columns and third-party fluid tools need graphical or mod-specific checks. The model supplies organism geometry. Native adjacent water renders against the shared water material, and exposed edges of occupied cells have no dedicated water surface mesh.

## Atlas and model

Original source PNGs live in `artwork/source/abyssal` and export to `zed/textures/abyssal` in both artwork profiles. The 256×256 atlases use 16×16 cells. The first seven cells of materials row zero match the table order. Cells 7–8 are Kelp stem and tip, 9 Ribbon Algae, 10 Fan Coral, 11–12 sponge tube sides and openings, 13 Ghost Anemone and 14 sponge inventory silhouette. The items atlas uses row zero cells 0–5 in item table order.

The renderer batches two crossed cutouts for Kelp, Algae and Anemone, an axis-aligned double-sided fan for Coral, and three shaded square-section tubes for Sponge. Marine ItemBlocks use cutout inventory sprites. Solid ItemBlocks use the normal cube path.

## Natural generation

The Abyssal Trench reuses the registered geological materials and Part I's marine water occupancy rules. The pinned terrain stage places the palette in newly generated chunks. See the [generation contract and owner walkthrough](abyssal-worldgen.md).

Beta.1 separates shelf kelp, deep anemone-and-sponge colonies and sparse algae or fan coral. Rooted fossil spurs, shellstone bands and taller stone stacks share deterministic terrain anchors. Architectural lamps are reserved for placed structures. The generation-only marine placement continues to validate the local support, water above the organism and lateral source-water cells.

## Owner playtest

1. Assemble a source-water basin in a marked disposable world. Search for the 18 names in NEI and check both Creative catalog modes. Craft the five recipes and harvest the solid blocks with the appropriate tools.
2. Plant each organism over the basin floor. Try air, a current, an unsupported cell and a fifth Kelp segment. Swim through the organisms while checking air, then repeat with a charged Oxygen Tank and Flippers. Break a support and count drops. Test the pool edge and Save/Quit.
3. Place both Fan Coral orientations. Inspect the Kelp join, sponge openings, Anemone tentacles, brick erosion, water borders and the two light sources with normal and deep underwater lighting. Check inventory, held and dropped items.
4. Run `py artwork/tools/abyssal_previews.py` for native faces, cube projections, marine geometry, item sprites, enlarged tiles and an assembled elevation under `artwork/previews/abyssal-*.png`. These are static previews. In-game graphical acceptance remains pending.
