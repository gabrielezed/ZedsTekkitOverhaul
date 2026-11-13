# Enderkin visual design

The two alpha.7 prototypes share obsidian skin, a cubic face with two violet eyes, and limbs whose proportions hint at a connection to the Enderman. The designs keep the origin unexplained and make the two tactical roles readable from a distance.

| | Skirmisher | Breacher |
| --- | --- | --- |
| Standing silhouette | About 2.7 blocks tall, with a narrow chest, pinched waist, long legs and a forward-set face | About 2.2 blocks tall, with a recessed head, broad shoulder mass, oversized forearms and planted feet |
| Palette | Blue-violet charcoal from `#111019` to `#4a3b61`, with lavender eyes | Purple charcoal from `#141019` to `#573b63`, with pink-violet eyes and angled forearm fissures |
| Motion | Long strides, lateral lean from actual movement relative to the gaze, offset reaching arms, release and recoil | Slow, opposed arm swings, a weighted torso, a two-arm lift and downward stroke with recovery |

Each model defines its own cuboids, pivots and poses. The shared code builds native parent/child parts in texel units and supplies interpolation helpers. Each atlas maps separate faces and limbs, using 64×64 for the Skirmisher and 128×64 for the Breacher. Eye texels use the native lightmap. They dim in deep shade and have no emissive pass. A future Juggernaut can reuse the cuboid builder with wider upper arms, a lower forward pivot and a new gait.

The Skirmisher's reach ramps in during the existing twelve-tick aiming window. The draw pass omits the held bow, and the existing arrow launch triggers recoil. The Breacher's lift, drop and recovery follow the existing swing event. Damage and door impact sound occur at the original event tick, early in the animation. Genuine pre-impact telegraphing would require a gameplay timing decision.

Collision remains 0.60×1.70 and 0.85×1.90 blocks. Legacy sight and targeting rays and the arrow origin still use the gameplay eye height. The taller mesh can extend above the selectable body in open terrain. A local, loaded-block collision query above the interpolated movement hull compresses the model vertically near low ceilings, about the feet. Abrupt overhead edges can produce visible compression. Limbs may overhang door jambs and narrow walls, especially during a swing. Navigation, melee reach and entity pushing continue to use the original hull. The stable save names, lunar placement and encounter loadouts are independent of the visual model. Aim and swing pose state is transient and exercised in local singleplayer.

## Reproduction

Deploy with `zto-tools/build-and-deploy.bat` using the workspace copy of Tekkit Dev. Run `test-alpha7-runtime.ps1` with `-Mode visual`, `-Mode combat` and `-Mode stress`. Each run uses a separate marked disposable save. The visual fixture positions a player, the two Enderkin, an Enderman, a Zombie and a second pair of Enderkin at a fixed camera distance. It captures daylight, torchlight, low light and a two-block ceiling. The combat fixture captures gameplay-driven poses and reloads the two entities through Save and Quit. The logs identify each capture in the isolated game's `screenshots` directory.
