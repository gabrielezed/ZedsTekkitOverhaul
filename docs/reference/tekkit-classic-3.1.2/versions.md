# Version lock — Tekkit Classic 3.1.2

Use these versions rather than similarly named mods from later Minecraft releases.

| Component | Pack/development version | Evidence |
| --- | --- | --- |
| Minecraft | 1.2.5 | Tekkit Classic 3.1.2; local MDK |
| Tekkit Classic | 3.1.2 | [Technic official pack listing](https://www.technicpack.net/modpack/tekkit.552560/mods) |
| IndustrialCraft 2 | **1.97** | Technic pack listing; user's `industrialcraft-2-client_1.97` archive |
| Equivalent Exchange 2 | **1.4.6.5** | Technic pack listing; user's EE2 1.4.6.5 decompilation |
| BuildCraft | **2.2.14** | Technic pack listing; [official 2.2.14 module archive](https://mod-buildcraft.com/releases/BuildCraft/2.2.14/) |
| Railcraft | **5.3.3** | Technic pack listing; [5.3.3 API archive listing](https://mcarchive.net/mods/railcraft?gvsn=1.2.5) |
| RedPower 2 | **2.0.5b2 (PR5B2)** | Technic pack listing |
| ComputerCraft | **1.33** | Technic pack listing; future integration |
| MinecraftForge | **3.4.9.171** | Local FoxyRetroMDK / project runtime logs (environment-specific) |
| FML | **2.2.106.176** | Local FoxyRetroMDK / project runtime logs (environment-specific) |
| MCP | **6.2** | Local FoxyRetroMDK / project setup |
| Java | **JDK 8** for building/running | Working legacy development setup |

**Pack vs. toolchain:** the official pack listing specifies mod versions, not the exact patch-level Forge build installed in the user's FoxyRetroMDK. Do not attribute the local Forge/FML versions to Technic's listing without separate verification.

## BuildCraft module names

The 2.2.14 client is distributed as five modules: `A-core`, `B-builders`, `B-energy`, `B-factory` and `B-transport`. Match **all** integration tests to the modules actually installed in Prism Tekkit Dev; a single combined BuildCraft JAR from a later release is not equivalent.

## Protect this version boundary

- IC2 1.97 is **not** IC2 Experimental 2.x.
- BuildCraft 2.2.14 is **not** BuildCraftAPI 7.2+, which has different classes and packages.
- EE2 1.4.6.5 is **not** ProjectE or newer EE3.
- Forge 3.x for Minecraft 1.2.5 is **not** Forge 6.x+; sided inventory APIs, GUI APIs and naming may differ.
- Bukkit/MCPC server variants may differ from the user's client; server testing is a separate milestone.

When an agent uses an external page, record the version mentioned there before copying any design assumptions into code.
