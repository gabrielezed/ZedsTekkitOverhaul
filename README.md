# ZedsTekkitOverhaul

A Minecraft **1.2.5 / Tekkit Classic 3.1.2** overhaul that connects industrial and alchemical progression and uses the moon as a recurring signal for future world events. The development target is FoxyRetroMDK 1.3.6, MCP 6.2, JDK 8, Forge 3.4.9.171 and the legacy ModLoader API.

## Current functionality

- **Lunar Cycle:** a persistent eight-phase single-player calendar, immutable snapshots, transition events and scheduling windows. New saves start at new moon. Legacy saves retain their initial vanilla atlas cell. Native moon rendering uses a separately enabled launch agent. An opt-in Lunar Debug panel exposes the calendar, transitions and detached simulations. [Lunar handoff and acceptance](docs/agents/lunar-cycle-implementation.md)
- **Dev Bridge:** opt-in authenticated loopback access to read-only local single-player diagnostics. The PowerShell CLI can inspect the world clock, loaded blocks, the lunar snapshot and recent lunar transitions. [Dev Bridge setup and protocol](docs/agents/dev-bridge.md)
- **Creative inventory backport:** tabbed catalog, search and a player inventory view with locally prepared textures. The **1.2.5** tab opens the classic screen. Press **F6** to restore the backport, or rebind **Restore ZTO Creative Inventory** in Options → Controls. [Creative inventory backport](docs/engineering/creative-inventory-backport.md)
- **Hopper prototype:** five persistent slots, directional item transfer, loose-item pickup, redstone locking and a dedicated GUI, based on the official 1.5.2 client. Requires locally prepared assets. Includes a narrow IC2 1.97 adapter and inventory-facing BuildCraft integration. See the [engineering and acceptance note](docs/engineering/hopper-backport.md) for preparation, compatibility and the exact verification status.
- **Advanced Crafting Table:** a placeable 5×5 crafting station with its own recipe manager and GUI.
- **Foundry:** a placeable block crafted from 16 smooth Stone around the 5×5 grid, with a small animated OBJ rotor and persistent NBT pause/resume state. A provisional 3×3 in-world structure validator is being developed separately.
- **EE2 fuels for IC2 generators:** legacy fuel integration through the ModLoader furnace-fuel callback.
- **Configurable EMC:** compatibility defaults of 128 EMC for the Advanced Crafting Table and 16 for the Foundry, plus external audit and override profiles. The example profile covers selected IC2, BuildCraft, RedPower, EE2 and ZTO products. [Profile guide](docs/engineering/emc-profile.md) · [Coverage audit](docs/engineering/emc-coverage-audit.md)
- **Alchemical Converter:** accepts EMC from adjacent EE2 collectors and supplies the IC2 network on any free face. The highest connected collector tier sets the shared output limit. Right-click with an empty hand for the live EMC buffer and connection status. [Converter specification](docs/engineering/alchemical-power-v1.md)
- **Fractional distillation:** six distinct components form a seven-position apparatus. An IC2 LV heater powers oil processing, with BuildCraft oil and water intake, fraction extraction, and four placeable petroleum liquids. Empty-hand right-click opens each component's live status panel. [Distillation handoff](docs/agents/distillation.md)
- **Crafting and inventory tools:** recipe preparation and inventory drag support for compatible modded containers. Drag requires the separate inventory agent and a supported GUI fingerprint. [Crafting and inventory handbook](docs/agents/crafting-inventory-ux.md)

The project owner verified the 5×5 interface, Foundry crafting and rotor persistence, and operation of the original single-input Alchemical Converter. The latest six-face converter and machine panels were compiled, reobfuscated and deployed to Tekkit Dev. Their visual and interaction checks are pending. The Foundry remains a visual prototype.

## Project structure

- `src/minecraft/zed/tekkitoverhaul/` — original Java source, including entry point, crafting and integrations.
- `src/minecraft/zed/gui/` — original GUI textures bundled into the release JAR.
- `zto-tools/build-and-deploy.bat` — compile, reobfuscate, package and copy into the dedicated PrismLauncher development instance.
- `docs/deploy-prism.md` — Windows build/deploy instructions.
- `docs/agents/` — technical handbook and factual project state for coding agents.
- `AGENTS.md` — canonical repository-wide instructions for AI coding agents.

Minecraft, Forge, IC2, EE2, BuildCraft and MCP source/binaries are **not included** in this repository.

## Development

1. Prepare a working FoxyRetroMDK 1.3.6 / MCP 6.2 environment using JDK 8.
2. Work in the MDK root with this repository's files under `src/minecraft/zed/`. The IDE is Eclipse.
3. Compile in Eclipse or use the existing MCP scripts.
4. Close Minecraft, then run the deployment script from the MDK root:

```powershell
.\zto-tools\build-and-deploy.bat
```

The script expects the isolated PrismLauncher instance `Tekkit Dev` under `%APPDATA%\PrismLauncher\instances\Tekkit Dev\minecraft\mods`. See [deployment documentation](docs/deploy-prism.md) for overrides, prerequisites and safeguards.

### Optional development features

- **Moon rendering:** run `.\zto-tools\build-lunar-agent.bat` and add `-javaagent:<absolute-path-to-ZTOLunarAgent.jar>` to Tekkit Dev's Java 8 arguments. The agent can run alongside the inventory agent. Unsupported renderer fingerprints keep the native phase selection.
- **Lunar Debug:** set `lunarDebugKey=67` in `config/mod_ZedsTekkitOverhaul.cfg` for F9. The default `0` disables the shortcut. Optionally set `lunarVerboseLogging=true` for transition logs. Restart after changing these settings.
- **Dev Bridge:** add `-Dzto.dev=true -Dzto.devBridge.enabled=true` to the isolated instance's JVM arguments. Use the discovery path printed in the log with `zto-tools/zed-dev.ps1`. The discovery file contains the authentication token. [CLI examples and wire contract](docs/agents/dev-bridge.md)

**Important:** the original production Tekkit mod JARs are obfuscated. Placing IC2/EE2 release JARs directly into the deobfuscated MCP runtime can cause class-loading failures. Test full mod integration in the separate PrismLauncher instance; see [environment notes](docs/agents/environment.md).

## Legacy mod API documentation

The [Tekkit Classic 3.1.2 reference](docs/reference/tekkit-classic-3.1.2/README.md) documents selected historical IC2, EE2, BuildCraft, Railcraft, RedPower and Forge integration surfaces, including sources, version caveats and unverified contracts. No third-party mod binaries or source archives are included.

## Visual assets and metadata

The distillation prototype includes an [industrial-style atlas and connected pipe geometry](docs/engineering/distillation-rendering-and-binding-v2.md), a root-of-JAR `mcmod.info`, and [asset credits](CREDITS.md). The project uses original artwork for the custom distillation textures.

## Contribution and agent guidance

Read [AGENTS.md](AGENTS.md) before changing any code, and use the [agent handbook](docs/agents/README.md) for architecture, compatibility constraints, validation and planned work.

All repository documentation, comments, code identifiers, commit messages and user-facing project text must be written in **English**. Conversations with the project owner may be in Italian. Keep changes scoped and do not commit third-party code or game assets.
