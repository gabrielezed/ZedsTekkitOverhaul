# BuildCraft reference — version 2.2.14 / Minecraft 1.2.5

**Verification status:** the exact version and its five client modules are confirmed from the official [BuildCraft 2.2.14 release archive](https://mod-buildcraft.com/releases/BuildCraft/2.2.14/) and the [Technic pack list](https://www.technicpack.net/modpack/tekkit.552560/mods). We do **not yet have a locally audited 2.2.14 Java API archive or signature map**. This document therefore records safe integration boundaries and the work needed to verify them, rather than guessing signatures.

## Components

| 2.2.14 module | Relevance to ZTO |
| --- | --- |
| `buildcraft-client-A-core-2.2.14.zip` | Shared BuildCraft definitions and integration infrastructure |
| `buildcraft-client-B-builders-2.2.14.zip` | Builder and blueprint functionality; not a current priority |
| `buildcraft-client-B-energy-2.2.14.zip` | BuildCraft engine/energy integration |
| `buildcraft-client-B-factory-2.2.14.zip` | Tanks, pumps, refinery-type machine concepts |
| `buildcraft-client-B-transport-2.2.14.zip` | Pipes and transport automation |

The similarly named **BuildCraftAPI 7.2+** GitHub project documents a **much newer API**. Do not use its classes or names as evidence for 2.2.14.

## Planned integration surfaces

ZTO's longer-term petroleum project needs to answer these questions against the actual BuildCraft 2.2.14 distribution:

1. **Fluid storage:** what container or liquid interface lets tanks and waterproof pipes interact with an external TileEntity?
2. **Pipe endpoints:** does automation use a vanilla `IInventory`, the legacy Forge `ISidedInventory`, or a BuildCraft-specific transport hook for the intended machine?
3. **Power:** which version-specific interface/engine protocol is present, and what are its units, capacity, transmission rules and simulation semantics?
4. **World life cycle:** how are connected pipes/machines notified of adjacent changes and chunk unloads?
5. **Item handling:** what happens when an attached inventory rejects some or all inputs?

A later Forge/BuildCraft tutorial mentioning `IPowerReceptor`, `FluidStack`, `ForgeDirection` or `buildcraft.api.*` is **not proof** those types and package names exist in BC 2.2.14.

## Concrete verification plan before writing adapter code

- Obtain the exact modules from the **user's isolated Prism Tekkit Dev** instance or the linked official archive and keep these **outside Git**.
- Inspect the packaged public classes and method signatures. Record exact package names, return types and version-specific power/liquid units.
- Check whether the production classes use obfuscated MCP/Forge names. Determine a reversible mapping/development classpath strategy.
- Write a small compatibility adapter in a `buildcraft` integration package, with an absence check when appropriate.
- Test a tank or pipe next to a simple ZTO inventory/fluid prototype **in real Tekkit** before designing the large refinery.
- Confirm that no later BuildCraft classes were accidentally imported.

## Licensing and provenance

The project maintains an official historical archive; [version 2.2.14 has been associated with the Minecraft Mod Public License v1](https://modrinth.com/mod/buildcraft), but redistribution obligations must be checked against the actual archived source/binary license before shipping dependencies. This repository includes links and our original notes only, never repackaged BuildCraft classes or binaries.

See [Forge 1.2.5](forge-1.2.5.md), [versions](versions.md), [sources](sources.md).
