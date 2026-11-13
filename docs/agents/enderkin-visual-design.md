# Enderkin visual design

The two alpha.7 prototypes share obsidian skin, a cubic face with two violet eyes, and limbs whose proportions hint at a connection to the Enderman. The designs keep the origin unexplained and make the two tactical roles readable from a distance.

| | Skirmisher | Breacher |
| --- | --- | --- |
| Standing silhouette | About 2.7 blocks tall, with a narrow chest, pinched waist, long legs and a forward-set face | About 2.2 blocks tall, with a recessed head, broad shoulder mass, oversized forearms and planted feet |
| Palette | Blue-violet charcoal from `#111019` to `#4a3b61`, with lavender eyes | Purple charcoal from `#141019` to `#573b63`, with pink-violet eyes and angled forearm fissures |
| Motion | Long strides, lateral lean from actual movement relative to the gaze, offset reaching arms, release and recoil | Slow, opposed arm swings, a weighted torso, a two-arm lift and downward stroke with recovery |

Each model defines its own cuboids, pivots and poses. The shared code builds native parent/child parts in texel units and supplies interpolation helpers. Each atlas maps separate faces and limbs, using 64×64 for the Skirmisher and 128×64 for the Breacher. Eye texels use the native lightmap. They dim in deep shade and have no emissive pass. A future Juggernaut can reuse the cuboid builder with wider upper arms, a lower forward pivot and a new gait.

The Skirmisher's reach ramps in during the existing twelve-tick aiming window. The draw pass omits the held bow, and the existing arrow launch triggers recoil. The Breacher's lift, drop and recovery follow the existing swing event. Damage and door impact sound occur at the original event tick, early in the animation. Genuine pre-impact telegraphing would require a gameplay timing decision.

Collision remains 0.60×1.70 and 0.85×1.90 blocks. Legacy sight and targeting rays use the gameplay eye height, and the arrow still starts 0.1 blocks below it. The taller mesh can extend above the selectable body in open terrain and vanilla frustum culling still uses the collision box. A local, loaded-block collision query above the interpolated movement hull compresses the model vertically near low ceilings, about the feet. Abrupt overhead edges can produce visible compression. Limbs may overhang door jambs and narrow walls, especially during a swing. Navigation, melee reach and entity pushing continue to use the original hull. The stable save names, lunar placement and encounter loadouts are independent of the visual model. Aim and swing pose state is transient and exercised in local singleplayer.

## Reproduction

Deploy with `zto-tools/build-and-deploy.bat` using the workspace copy of Tekkit Dev. Run `test-alpha7-runtime.ps1` with `-Mode visual`, `-Mode combat` and `-Mode stress`. Each run uses a separate marked disposable save. The visual fixture positions a player, the two Enderkin, an Enderman, a Zombie and a second pair of Enderkin at a fixed camera distance. The fixture hides the reference Zombie's fire overlay for a clear daylight comparison. It captures daylight, torchlight, low light and a two-block ceiling. The combat fixture captures gameplay-driven poses and reloads the two entities through Save and Quit. The logs identify each capture in the isolated game's `screenshots` directory.

## Regression evidence

- The visual and stress fixtures passed on commit `5df8c12c3a7708c79164511c832ba28ab62596f0`, JAR SHA-256 `da44191327ada8326ca5b0589c08908941b985958e306521630f56c3753392b7`. Two unrelated untracked documents account for the build's dirty flag. The stress fixture kept twelve active mobs for 140 game ticks. Both fixtures checked native renderer lookup, texture upload and stable texture IDs. Frame captures also checked for OpenGL errors.
- The combat fixture passed on the same JAR after fixing the harness to wait for both doorway traversal and melee damage. It verified Skirmisher spacing, retreat, aim and cover cancellation, Breacher pressure and melee, both health values after Save and Quit, then death and removal.
- The isolated Tekkit Classic 3.1.2 client ran Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176 and Java 1.8.0_51. MCP 6.2 compiled and reobfuscated the client with development Forge 3.4.9.171. The archive passed the 280-entry streaming ZIP check and contains the models and both atlases.
- All six alpha.6 runtime modes passed on the first candidate archive, SHA-256 `c6f263ff33272017d90f3704838b2a1d2dcf75141a22a814450de17386faa659`. Its sidecar recorded base `9e1d2bdaffbe9a912ea8219ee1d9d6ac0f32bd6f` with the redesign in the working tree. Every non-directory JAR entry other than the provenance file matches the subsequent commit build byte-for-byte. Portable calendar, assault and patrol schedules, outdoor approaches, OBJ loader and legacy lightmap checks also passed.
- This environment still logged obsolete resource URL failures and the pre-existing EMC initialization warning for item 169. Owner playtesting and remote multiplayer animation synchronization remain open.

## In-game comparisons

Left to right in the daylight lineup are the reference player, Skirmisher, Breacher, vanilla Enderman, Zombie and a second Enderkin pair. The Skirmisher reaches nearly to the Enderman's head with a narrower chest and a more forward pose. The Breacher reads as lower and wider even when its eye color is hard to see.

![Daylight lineup](images/enderkin-lineup-daylight.png)

[Torchlight](images/enderkin-lineup-torchlight.png) · [Low light](images/enderkin-lineup-low-light.png) · [Two-block ceiling](images/enderkin-two-block-clearance.png)

The two-block capture shows the compressed pair under the roof and a second pair at full standing height in the open. The reference Enderman flashes red because the fixture holds it underneath the same roof.

- Skirmisher [windup](images/enderkin-skirmisher-windup.png), [release](images/enderkin-skirmisher-release.png) and [repositioning](images/enderkin-skirmisher-strafe.png).
- Breacher [pursuit](images/enderkin-breacher-pursuit.png), [lift at the closed door](images/enderkin-breacher-pressure-windup.png), [downstroke](images/enderkin-breacher-pressure.png) and [melee](images/enderkin-breacher-melee.png). The door partially occludes both pressure poses.
- [Both prototypes after reopening the save](images/enderkin-reopened.png) and the [twelve-entity stress fixture](images/enderkin-stress.png).
