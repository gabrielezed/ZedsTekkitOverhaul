# Texture provenance

The distributable assets in [assets.json](../manifests/assets.json) are original ZTO pixels informed by the historical material and interface conventions in the local references. The exported PNG bytes can be traced to their authoring PNGs. The old Distillation items, meshes and material atlas are historical ZTO experiments available in Git history.

| Origin | Where to store it |
| --- | --- |
| ZTO original or reference-inspired artwork | `source/` plus an explicit runtime path in the manifest |
| Artwork derived under documented permission | `source/` plus the original source, permission and attribution in its manifest entry |
| Third-party texture, extracted sprite, original atlas or mixed comparison | Ignored local reference and preview directories |
| Experimental ZTO art awaiting replacement | Existing owned source or Git history, with its status documented |

Minecraft usage guidelines describe original mods and distribution of the mod itself. The reference importer uses the official 1.2.5 binary for local inspection. [Minecraft Usage Guidelines](https://www.minecraft.net/en-us/usage-guidelines)

The IC² team's historical terms address modified IC² files and original-pack redistribution. This workflow keeps the 1.97 sheets, GUI and UV references local. [IC² historical terms](https://forum.industrial-craft.net/user-post-list/18-alblaka/?pageNo=3)

BuildCraft lists the 2.2.5–2.2.14 modular releases under the Minecraft Mod Public License v1 with uncertain historical source coverage. Local archives supply the references. Confirm any proposed reuse against the applicable version and artwork permissions. [BuildCraft release licensing](https://modrinth.com/mod/buildcraft) · [Official 2.2.14 release index](https://mod-buildcraft.com/pages/buildinfo/BuildCraft/2.2.14.html)
