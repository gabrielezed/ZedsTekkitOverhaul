# Credits and asset provenance

## ZedsTekkitOverhaul

Project concept, design direction and testing: **Gabriele Zed**.

Programming assistance, technical analysis and initial procedural model/texture drafts: **ChatGPT (OpenAI)**. All submitted code and original artwork are part of the ZTO project.

## Third-party projects

- **Minecraft**: Mojang Studios. Original Minecraft resources and game assets remain the property of their respective owners.
- **IndustrialCraft 2**: the IC2 team. This mod uses version-pinned integration references; the distillation texture restyle is original artwork **inspired by the general low-resolution industrial palette**, not copied IC2 pixels.
- **BuildCraft**: the BuildCraft team. Fluid and machinery design references use the historical BuildCraft 2.2.14 ecosystem; BuildCraft artwork is not included in the ZTO material atlas.
  Distillate bucket icons use the fuel bucket tile from the locally installed
  `net/minecraft/src/buildcraft/core/gui/item_textures.png` at runtime. ZTO changes
  the fuel color and clears the atlas padding. The original sprite and generated
  variants are sourced at runtime from the player's BuildCraft installation.
- **Equivalent Exchange 2**, **Railcraft**, **RedPower 2** and other Tekkit components: all trademarks and assets belong to their respective authors.

### Future derivative textures

If IC2/BuildCraft texture pixels are copied or adapted in a later commit, record the **exact original asset filename, mod version, license/permission and attribution** here before distribution. The current `distillation_atlas.png` restyle is an independently authored 64×32 pixel atlas with fully opaque pixels. Visual references were IC2 1.97's `ic2/sprites/block_machine.png` and BuildCraft 2.2.14's `net/minecraft/src/buildcraft/core/gui/block_textures.png`. The atlas uses original pixel patterns and palettes.
