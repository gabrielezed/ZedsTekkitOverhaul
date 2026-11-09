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

This workstation keeps versioned extracted classes and decompiled sources in the ignored root directory 	ekkit-mods-decompiles/. Start with its local catalog.csv. Use decompilations as research leads and verify ABI details with javap against the matching binaries. Keep the catalog and all upstream files out of Git.
