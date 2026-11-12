# Tekkit Classic 3.1.2 — legacy API reference

**Scope:** Minecraft 1.2.5, Tekkit Classic pack Forge 3.3.8.164 and FML 2.2.106.176. The FoxyRetroMDK 1.3.6 / MCP 6.2 build uses Forge 3.4.9.171. This directory contains original project documentation **only**, not redistributed dependencies.

The documents are a version-pinned reference for ZedsTekkitOverhaul and its coding agents. The actual local MCP source, original mod release archives, and tested Tekkit Dev runtime remain the authorities when a signature or behavior is disputed.

## Reference map

| Integration | Reference | Verification status |
| --- | --- | --- |
| Installed mod versions | [versions.md](versions.md) | Technic official 3.1.2 list and existing workspace |
| IndustrialCraft 2 1.97 | [ic2-1.97.md](ic2-1.97.md) | Local IC2 1.97 API archive inspected; MCP remapping still required |
| Equivalent Exchange 2 1.4.6.5 | [ee2-1.4.6.5.md](ee2-1.4.6.5.md) | Signatures checked against locally provided decompilation; not a formal SDK |
| BuildCraft 2.2.14 | [buildcraft-2.2.14.md](buildcraft-2.2.14.md) | Official release modules verified; Java API signatures still unverified |
| Forge 1.2.5 | [forge-1.2.5.md](forge-1.2.5.md) | Local Forge legacy source and current ZTO usage checked |
| Railcraft 5.3.3 | [railcraft-5.3.3.md](railcraft-5.3.3.md) | Version-specific API archive identified, contents not yet audited |
| RedPower 2 PR5B2 | [redpower-2.md](redpower-2.md) | Pack version verified; stable API not verified |
| Provenance and rules | [sources.md](sources.md) | URLs, confidence and redistribution safeguards |

## Evidence and usage convention

- **Verified locally** = the indicated declaration or behavior was inspected in the user's matching-version archive or original ZTO source.
- **Verified externally** = the named release or archive was confirmed at an external source, without independently auditing its bytecode.
- **Unverified** = no compatible declaration has been inspected; never invent a method signature or assume that a modern guide applies.
- **Observed in Tekkit Dev** = directly tested in a real game by the project owner, with scope stated.

Always distinguish *source-declared signature* from *MCP-compatible signature*: archives from production Tekkit can contain obfuscated type names. Do not paste decompiled code into this repository. Before introducing a compile-time dependency, confirm package name, exact signature and mappings inside the real MCP environment; then test packaging and runtime.

For development policy see [AGENTS.md](../../../AGENTS.md), [agent handbook](../../agents/README.md) and [integration boundaries](../../agents/integrations.md).
