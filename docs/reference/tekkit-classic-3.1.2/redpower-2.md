# RedPower 2 reference — PR5B2 / Minecraft 1.2.5

**Pack version:** RedPower 2 **PR5B2** (also displayed as `2.0.5b2`) in Tekkit Classic 3.1.2. Source: [Technic's pack listing](https://www.technicpack.net/modpack/tekkit.552560/mods).

**API status:** no standalone, version-verified Java SDK or trustworthy complete signature reference for PR5B2 has been audited for this project. RedPower's behavior should be investigated **only** when a concrete interop feature requires it.

## Prospective interoperability

- RedPower tubes inserting/extracting items from ZTO machine inventories.
- Face-specific inventory access and potentially special transport rules.
- Future block movement/frame interactions with custom TileEntities, only if explicitly in scope.

Do not conflate this with modern Minecraft automation capabilities or assume that implementing a later sided-inventory interface is sufficient.

## Proposed investigation path

1. Test the simplest possible ZTO inventory under a RedPower tube and a BuildCraft pipe **in real Tekkit Dev**.
2. Establish whether the exact Forge 1.2.5 `ISidedInventory` path is used, whether additional interfaces are required, and whether the machine must expose a vanilla `IInventory`.
3. Verify extracted and inserted slot indices, handling of full inventories and partially transferred stacks.
4. If necessary, inspect the original PR5B2 library **locally**, documenting only the minimum interoperability contract and the evidence used.
5. Introduce a RedPower-specific adapter only if a verified interface is essential.

Never add RedPower decompiled sources or a copy of its JARs to GitHub.

Related: [Forge 1.2.5 sided inventories](forge-1.2.5.md), [BuildCraft](buildcraft-2.2.14.md), [sources](sources.md).
