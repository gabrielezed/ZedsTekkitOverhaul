# Development environment and deployment

## Target

| Component | Version / role |
| --- | --- |
| Game | Minecraft **1.2.5**, Tekkit Classic **3.1.2** |
| Development kit | FoxyRetroMDK **1.3.6**, MCP **6.2** |
| Forge / FML | MinecraftForge **3.4.9.171**, FML **2.2.106.176** |
| Java | **JDK 8** for the local toolchain; use legacy-compatible source constructs |
| IDE | Eclipse, Client project |
| Development source | `MDK-1.2.5/src/minecraft/zed/` |
| Packaging output | `MDK-1.2.5/dist-zto/ZedsTekkitOverhaul.jar` |

The Git repository is initialized **inside the existing `MDK-1.2.5` directory** and uses an allowlist `.gitignore`. Most of the MCP tree intentionally does not exist on GitHub. A standalone checkout is **not** a complete runnable MDK.

## Two test environments

The MCP client identifies its Forge sources as **3.4.9.171**. The isolated Prism Tekkit Dev manifest installs a legacy modpack JAR overlay, and the running title screen reports **3.3.8.164**. Both use Minecraft 1.2.5 and the tested Prism runtime reports FML 2.2.106.176. Keep the compile and runtime versions separate in reports. See the [singleplayer acceptance record](singleplayer-acceptance.md) for the effective pack versions.

**MCP/Eclipse**: deobfuscated Minecraft with original project code. Use it for rapid development and API checks. It is not equivalent to the distributed Tekkit classpath.

**PrismLauncher / Tekkit Dev**: isolated complete Tekkit Classic instance with IC2, EE2 and BuildCraft. Use it for integration testing after reobfuscation; never deploy over an ordinary gameplay instance.

The default Prism mod directory is:

```text
%APPDATA%\PrismLauncher\instances\Tekkit Dev\minecraft\mods
```

## Build and deploy

Close the game and, from the MDK root, run:

```powershell
.\zto-tools\build-and-deploy.bat
```

The script calls `recompile.bat`, removes only stale `reobf/minecraft/zed/` output, calls `reobfuscate.bat`, packages the reobfuscated `zed/` classes and `zed/gui/` texture, and deploys the finished JAR. It aborts if required tools or assets are absent and if competing old `ZedsTekkitOverhaul*.jar` names are detected.

For a different Prism instance, set `ZTO_PRISM_MODS` before calling it. See [../deploy-prism.md](../deploy-prism.md) for the exact command and limitations.

## Important classpath issue

**Do not put the distributed IC2 1.97 or EE2 1.4.6.5 client JARs directly into the deobfuscated MCP runtime's `jars/mods` directory.** They may fail during classloading; a recorded error is `NoClassDefFoundError: forge/IConnectionHandler` because production and MCP namespaces/mappings differ. Decompilation alone does not solve this. If source-level debugging of those mods becomes essential, create a separate, tested remapping procedure; do not make it a hidden prerequisite for simple project changes.

## Version control boundaries

Never commit `bin/`, `reobf/`, `jars/`, `forge/`, `runtime/`, `eclipse/`, local `config/`, saves, copied mod archives, or proprietary decompiled sources. Only original code/resources under `src/minecraft/zed/`, project tools and documentation are intended for Git.

## Local Tekkit research catalog

The ignored root directory `tekkit-mods-decompiles/` is a workstation reference for Tekkit Dev. It contains two collections:

- **Mod archives and directories.** The root `catalog.csv` lists 42 decompiled entries and three resource-only directories. Archive directories under `mods/` preserve their installed names and contain `extracted/` files, `decompiled/` sources, and Fernflower logs. Examples include IC2 1.97, EE2 1.4.6.5, BuildCraft 2.2.14, IronChests 3.8.0.40, and the separate NEI plugins for Railcraft and RedPower. For Inventory Tweaks 1.41b, start in `mods/InvTweaks-1.41b-1.2.4.zip/decompiled/` with `InvTweaksContainerManager.java`, `InvTweaksHandlerSorting.java`, and `InvTweaksHandlerShortcuts.java`.
- **Core client references.** The separately added `mods/coremods/` tree contains 401 Java files and some resources. The older CSV and local README describe the original mod archive pass, so the CSV does not inventory this addition. For NEI 1.2.2.4, start with `mod_NotEnoughItems.java`, `codechicken/nei/NEIController.java`, `codechicken/nei/LayoutManager.java`, and `gb.java`, the NEI-patched inventory screen. Forge and FML references are under `forge/` and `cpw/mods/fml/`. The observed tree does not establish complete coverage of the runtime overlay.

The local `tools/` directory contains the Fernflower launcher, engine and install metadata. Use reconstructed sources to trace behavior, then confirm signatures and class ownership with `javap` against the exact Tekkit Dev JARs in runtime order. See the [Creative inventory audit](../engineering/creative-inventory-backport.md) for a worked example. Keep the local catalog, extracted files and decompiled sources outside Git.
