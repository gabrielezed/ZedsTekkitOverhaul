# Railcraft reference — version 5.3.3 / Minecraft 1.2.5

**Verification status:** a version-specific developer API archive exists. [MCArchive lists `Railcraft_API_5.3.3.zip`](https://mcarchive.net/mods/railcraft?gvsn=1.2.5), with recorded SHA-256:

```text
98b9ea6d8db25a8e53efb966578432006d61e07099e767aa73eac95c39af9ff0
```

The archive's **contents, signatures and license have not yet been audited for ZTO**. Do not guess Railcraft method names or import a newer Railcraft API. The archive listing notes compatibility with BuildCraft 2.2.14 and IC2 1.95+, but that is not proof our future custom integration works.

## Why this matters

Railcraft 5.3.3 supplies examples of multi-block industrial machines in the Tekkit era. For ZTO it could become useful for:

- Matching materials via the Forge Ore Dictionary, if the relevant item tags are present in the target version.
- Integrating with rail or boiler-oriented systems, after discovering the actual API surface.
- Avoiding recipe/progression conflicts when designing advanced smelting, furnaces and steel production.

These are **prospective use cases**, not installed ZTO adapters.

## Verification checklist

1. Read the archive's package index, original notices and version metadata **locally**.
2. Compare SHA-256 with the archived checksum above before trusting the download.
3. Audit each API class needed by a concrete ZTO feature, including package, method signature and dependency mapping.
4. Check whether an API class may be bundled in an addon under its applicable license; do not assume a modern Railcraft MIT notice covers this specific 2012 artifact.
5. Compile against MCP 6.2 and test with Railcraft 5.3.3 in Prism Tekkit Dev.

Until a feature needs Railcraft, documentation-only tracking is sufficient. No Railcraft JAR, ZIP or API source is committed to the ZTO repository.
