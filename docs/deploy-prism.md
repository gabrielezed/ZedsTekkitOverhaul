# Automatic build and deployment to PrismLauncher (Windows)

This workflow uses the **existing FoxyRetroMDK / MCP 6.2 workspace**. It does not include Minecraft, Forge or Tekkit binaries.

## Prerequisites

- JDK 8 with `jar.exe` available on `PATH` (the same environment used by MCP).
- A working `MDK-1.2.5` directory containing `recompile.bat` and `reobfuscate.bat`.
- Original mod source under `src/minecraft/zed/` and GUI texture at `src/minecraft/zed/gui/advanced_table.png`.
- The isolated PrismLauncher instance named **Tekkit Dev**, with Minecraft closed before deployment.

## One command

From PowerShell in the `MDK-1.2.5` root:

```powershell
.\zto-tools\build-and-deploy.bat
```

The script:

1. Runs MCP compilation.
2. Removes **only** the old `reobf/minecraft/zed/` output, then runs reobfuscation.
3. Builds a candidate JAR and restores complete local Hopper and creative texture sets from the configured asset directories, installed JAR, last published JAR, or backup, in that order.
4. Publishes `dist-zto/ZedsTekkitOverhaul.jar` and `dist-zto/ZTOAgent.jar` with matching build sidecars. The default Tekkit Dev deploy installs the managed component and backs up the files it replaces.

### Managed compatibility

Build into an isolated staging directory with `ZTO_PRISM_MODS` set to its `mods` folder. Once the candidate meets the [compatibility gates](agents/compatibility-acceptance.md), close Prism and install both matching artifacts in the intended instance.

```powershell
.\zto-tools\install-zto-prism.ps1 -Instance "$env:APPDATA\PrismLauncher\instances\Tekkit Dev"
```

The installer writes a local `+agents` component and its library. Prism 11.1.1 resolves the library and supplies one `-javaagent` argument on launch. Repeated installs keep a single ZTO component and remove recognized old ZTO JVM arguments while preserving other arguments. A pack prepared in this layout carries the local library, component and mod in its instance directory. [Prism component parsing](https://github.com/PrismLauncher/PrismLauncher/blob/11.1.1/launcher/minecraft/OneSixVersionFormat.cpp#L188)

The plain mod install keeps the native gameplay gates. Inventory gestures and Shift armor equip, the custom rendered moon phase and direct third-party gates require the JVM startup agent. FML 2.2.106.176 discovers legacy mods after the JVM starts. This installation uses its existing mod discovery and does not rely on it to invoke Java instrumentation. The agent shades ASM 9.9.1 into its own namespace and uses Java 8.

### Default destination and staging

```text
%APPDATA%\PrismLauncher\instances\Tekkit Dev\minecraft\mods
```

To stage the mod without editing a Prism profile, point the build at an existing staging mods directory. The agent and both sidecars are written to `dist-zto`.

```powershell
$env:ZTO_PRISM_MODS = 'D:\PrismLauncher\instances\Tekkit Dev\minecraft\mods'
.\zto-tools\build-and-deploy.bat
```

## Safeguards and limitations

- The destination mods directory must **already exist**. The script does not create new Prism instances.
- If `ZedsTekkitOverhaul-0.6.0.jar` or another competing `ZedsTekkitOverhaul*.jar` is detected, the script stops. Move the old file out of `mods` before retrying.
- The script does **not** modify `minecraft.jar`, `modpack.jar`, IC2, EE2 or BuildCraft.
- The compatibility JAR lives in the instance library directory. The standard mod JAR lives in `mods`.
- Failed compilation, reobfuscation or packaging stops before the new JAR is copied.
- A configured Hopper requires all five local textures. Deployment stops if the textures cannot be recovered. For first-time setup, use `ZTO_HOPPER_ASSETS` and `ZTO_CREATIVE_ASSETS` with the directories produced by the local asset preparation scripts.
- The JDK `jar` tool adds the recovered textures. The candidate must pass a full Java `ZipInputStream` read before publication because NEI streams mod archives on the first world tick.
- Check block ID mappings and the mod configuration before opening an existing world.
- A complete Windows MCP build and Prism launch must still be verified on the owner's development machine.
