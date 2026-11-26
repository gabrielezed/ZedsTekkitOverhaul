# Automatic build and deployment to PrismLauncher (Windows)

This workflow uses the **existing FoxyRetroMDK / MCP 6.2 workspace**. It does not include Minecraft, Forge or Tekkit binaries.

## Prerequisites

- JDK 8 with `jar.exe` available on `PATH` (the same environment used by MCP).
- A working `MDK-1.2.5` directory containing `recompile.bat` and `reobfuscate.bat`.
- Original mod source under `src/minecraft/zed/` and GUI texture at `src/minecraft/zed/gui/advanced_table.png`.
- The isolated PrismLauncher instance named **Tekkit Dev**, with Prism and Minecraft closed before deployment.
- A verified local artwork overlay, or Python with the [artwork requirements](../artwork/requirements.txt) and access to the original IC2 1.97 and BuildCraft 2.2.14 Core archives.

## One command

From PowerShell in the `MDK-1.2.5` root:

```powershell
.\zto-tools\build-and-deploy.bat
```

The script:

1. Runs MCP compilation.
2. Removes **only** the old `reobf/minecraft/zed/` output, then runs reobfuscation.
3. Packages `local-dev` by default. The build verifies or regenerates the four approved machine PNGs and adds complete local Hopper and Creative sets from the configured directories, installed JAR, published JAR or backups.
4. Verifies the actual archive entries, publishes the matching mod, agent and sidecars in `dist-zto` and installs both in Tekkit Dev. The installer backs up replaced instance files and verifies both installed hashes. Replaced build pairs and their sidecars are retained under `dist-zto/artifact-backups`.

### Managed compatibility

To stage a candidate with no Prism changes, point `ZTO_PRISM_MODS` to a loose directory outside a Prism instance. The build stages the verified mod there and prints the location of the matching mod, agent and sidecars. Once the candidate meets the [compatibility gates](agents/compatibility-acceptance.md), close Prism and install both matching artifacts in the intended instance.

```powershell
.\zto-tools\install-zto-prism.ps1 -Instance "$env:APPDATA\PrismLauncher\instances\Tekkit Dev"
```

The installer writes a local `+agents` component and its library. Prism 11.1.1 resolves the library and supplies one `-javaagent` argument on launch. Repeated installs keep a single ZTO component and remove recognized old ZTO JVM arguments while preserving other arguments. A pack prepared in this layout carries the local library, component and mod in its instance directory. [Prism component parsing](https://github.com/PrismLauncher/PrismLauncher/blob/11.1.1/launcher/minecraft/OneSixVersionFormat.cpp#L188)

The plain mod install keeps the native gameplay gates. Inventory gestures and Shift armor equip, the custom rendered moon phase and direct third-party gates require the JVM startup agent. FML 2.2.106.176 discovers legacy mods after the JVM starts. This installation uses its existing mod discovery and does not rely on it to invoke Java instrumentation. The agent shades ASM 9.9.1 into its own namespace and uses Java 8.

### Default destination and staging

```text
%APPDATA%\PrismLauncher\instances\Tekkit Dev\minecraft\mods
```

Pointing `ZTO_PRISM_MODS` at another existing Prism instance's `minecraft/mods` or `.minecraft/mods` directory installs the mod and managed agent together in that instance. Clear the variable to return to the default Tekkit Dev destination.

```powershell
$env:ZTO_PRISM_MODS = 'D:\PrismLauncher\instances\Tekkit Dev\minecraft\mods'
.\zto-tools\build-and-deploy.bat
Remove-Item Env:ZTO_PRISM_MODS
```

### Public artifacts

```powershell
$env:ZTO_ARTWORK_PROFILE = 'public'
.\zto-tools\build-and-deploy.bat
Remove-Item Env:ZTO_ARTWORK_PROFILE
```

The public profile writes a separate pair to `dist-zto/public` and performs no installation. Local machine derivatives, Hopper and Creative texture groups are excluded. Use the default `local-dev` command for owner playtests. The profile name and PNG fingerprints appear in the sidecars. The installed mod carries the same fields in its `zto-build.properties` entry.

## Safeguards and limitations

- The destination mods directory must **already exist**. The script does not create new Prism instances.
- If `ZedsTekkitOverhaul-0.6.0.jar` or another competing `ZedsTekkitOverhaul*.jar` is detected, the script stops. Move the old file out of `mods` before retrying.
- The script does **not** modify `minecraft.jar`, `modpack.jar`, IC2, EE2 or BuildCraft.
- The compatibility JAR lives in the instance library directory. The standard mod JAR lives in `mods`.
- Failed compilation, reobfuscation or packaging stops before the new JAR is copied.
- A local-dev build with missing or mismatched approved artwork stops before publishing or installing. The diagnostic identifies the artwork or donor archive to restore.
- A configured Hopper requires all five local textures. Deployment stops if the textures cannot be recovered. For first-time setup, use `ZTO_HOPPER_ASSETS` and `ZTO_CREATIVE_ASSETS` with the directories produced by the local asset preparation scripts.
- The JDK `jar` tool adds the recovered textures. The candidate must pass a full Java `ZipInputStream` read before publication because NEI streams mod archives on the first world tick.
- Check block ID mappings and the mod configuration before opening an existing world.
- A complete Windows MCP build and Prism launch must still be verified on the owner's development machine.
