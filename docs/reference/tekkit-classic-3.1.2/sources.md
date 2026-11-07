# Sources, evidence quality and redistribution policy

## References verified for this documentation

| Scope | Source | What it supports | Limitations |
| --- | --- | --- | --- |
| Exact pack versions | [Technic: Tekkit Classic 3.1.2 mod list](https://www.technicpack.net/modpack/tekkit.552560/mods) | IC2 1.97, EE2 1.4.6.5, BuildCraft 2.2.14, Railcraft 5.3.3, RedPower PR5B2 | Not an API method reference |
| IC2 1.97 and official API availability | [Official IC2 1.97 release thread](https://forum.industrial-craft.net/thread/6710-mc-1-2-5-ic%C2%B2-v1-97-ssp-smp/) | Official API source offered to addon developers; legal notices | Historical linked download may be unavailable |
| IC2 archive identification | [MCArchive IC2 1.2.5](https://mcarchive.net/mods/ic2?gvsn=1.2.5) | Specific client release archive/version | Does not establish MCP-remapped class names |
| EE2 1.4.6.5 archive identification | [MCArchive EE2 1.2.5](https://mcarchive.net/mods/ee2?gvsn=1.2.5) | Specific EE2 release archive/version | Does not establish an official public EE2 API |
| BuildCraft 2.2.14 modules | [BuildCraft official release archive](https://mod-buildcraft.com/releases/BuildCraft/2.2.14/) | Exact historical client/server ZIP module names | Does not provide audited Java method signatures |
| BuildCraft API version warning | [BuildCraftAPI GitHub repository](https://github.com/BuildCraft/BuildCraftAPI) | Public API repository targets 7.2+ | Must **not** be used as authority for BuildCraft 2.2.14 |
| Railcraft 5.3.3 API | [MCArchive Railcraft 1.2.5](https://mcarchive.net/mods/railcraft?gvsn=1.2.5) | `Railcraft_API_5.3.3.zip` and recorded hash | Contents and license not yet inspected |
| Legacy Forge orientation | [Forge 1.2.3 JavaDoc archive](https://legacymcmodding.github.io/Forge-JavaDoc/generated/1.2.3-1.4.1.64/client/index-all.html) | Historical conventions and API index | **Older** than the project's actual Forge version |

## Private/local user-provided reference material inspected

The following were supplied for analysis in the development conversation; **they are not committed**:

- `api.zip`: 29 source files under `api/`, with IC2-related classes.
- `industrialcraft-2-client_1.97.zip`: user-supplied decompilation containing `ic2/api/` and `ic2/common/`. Exact IC2 API declarations were inspected here.
- `EE2ClientV1.4.6.5.jar_Decompiler.com.zip`: user-supplied decompilation with `ee/EEMaps.java`, `ee/EEBase.java`, and `ee/EEItem.java`. EE2 EMC method signatures and initialization flag were inspected here.
- `forge.zip`: user-supplied historical source containing `Configuration`, `ISidedInventory` and `oredict/OreDictionary`, with package declarations under `net.minecraft.src.forge`.
- Existing ZTO source on GitHub: `src/minecraft/zed/tekkitoverhaul/integration/ee2/`, `crafting/AdvancedCraftingRegistry.java`, the ModLoader entrypoint and other original ZTO modules.

**Important:** these are source/binary provenance references, not promises that an agent can access the same local files in another session. When the files are unavailable, rely on this original summary and request/locate the matching archives before changing uncertain signatures.

## License and redistribution boundaries

This repository may contain our own commentary, compatibility tables, references and original adapter code, but not entire upstream API archives or decompiled mod sources.

- The official IC2 1.97 forum post explicitly provides a development API but separately restricts redistribution of IC2 files. Do not upload source or binaries without separately confirming permission.
- EE2's provided decompilation is for local analysis; no independently redistributable EE2 SDK has been established.
- BuildCraft 2.2.14 release files can be found in its official archive, but any redistribution must follow the specific artifact's license; linking to the official archive does not authorize repackaging its code.
- Railcraft offers a version-specific API ZIP, but its **2012 licensing contents** have not yet been checked. Do not assume current licenses retroactively cover the old archive.
- Do not publish RedPower binaries/decompilations or vanilla Minecraft/Forge source just to make a standalone repository compile.

## Agent source-validation protocol

Before adding a dependency or citing an API:

1. Identify the exact target game/mod version and original file/archive.
2. Record whether the claim is **verified in local source**, **externally listed**, **verified in MCP compilation**, or **runtime-tested**.
3. Confirm package and parameter types; account for obfuscated production names.
4. Build a minimal proof-of-interaction and run it in the isolated Tekkit Dev instance.
5. Record any newly verified signatures in the relevant versioned document.
6. Never convert a guess from a modern mod API into a verified historical claim.

When an external link goes dead, preserve its original URL for provenance and add a separately verified mirror or archive description; do not silently switch to a newer API release.
