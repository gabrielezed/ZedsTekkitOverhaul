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
3. Creates `dist-zto/ZedsTekkitOverhaul.jar` with the `zed/` classes and GUI texture.
4. If present, backs up the previously deployed JAR as `dist-zto/ZedsTekkitOverhaul.previous.jar`, then copies the new JAR to PrismLauncher.

### Default destination

```text
%APPDATA%\PrismLauncher\instances\Tekkit Dev\minecraft\mods
```

To change the target directory for the current PowerShell session:

```powershell
$env:ZTO_PRISM_MODS = 'D:\PrismLauncher\instances\Tekkit Dev\minecraft\mods'
.\zto-tools\build-and-deploy.bat
```

## Safeguards and limitations

- The destination mods directory must **already exist**. The script does not create new Prism instances.
- If `ZedsTekkitOverhaul-0.6.0.jar` or another competing `ZedsTekkitOverhaul*.jar` is detected, the script stops. Move the old file out of `mods` before retrying.
- The script does **not** modify `minecraft.jar`, `modpack.jar`, IC2, EE2 or BuildCraft.
- Runtime Java agents or binary hooks needed by optional experimental GUI features are **not** part of this deploy.
- Failed compilation, reobfuscation or packaging stops before the new JAR is copied.
- Check block ID mappings and the mod configuration before opening an existing world.
- A complete Windows MCP build and Prism launch must still be verified on the owner's development machine.
