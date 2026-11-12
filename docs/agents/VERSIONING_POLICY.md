# ZedsTekkitOverhaul — Versioning, Build Identity & Release Policy

**Status:** Adopted for Survival 0.1 after PR #19 merged.

**Initial release line:** `0.1.0-alpha.1` — first Survival gameplay vertical slice.

## 1. Why the version changes

The former ModLoader version `0.7.0-distillation-prototype` was a prototype identifier. The transition to `0.1.0-alpha.1` resets the product version line; it does not downgrade binaries or save data. See the [changelog](../../CHANGELOG.md). Do not change block IDs, TileEntity registration names, NBT field names or compatibility data merely due to reversioning.

## 2. Version scheme

Use **Semantic Versioning 2.0.0** with prerelease labels, and keep Git/build identity separate from the release version.

- `0.1.0-alpha.1`: first Survival prototype candidate line.
- `0.1.0-alpha.2`: a newly released/tested prototype of the same planned 0.1.0 line.
- `0.1.0-beta.1`: feature-complete trial with broader acceptance.
- `0.1.0`: stabilized first release of the survival prototype, not necessarily an externally public release.
- `0.1.1`: bug-fix release after 0.1.0.
- `0.2.0-alpha.1`: next planned gameplay feature line.
- `1.0.0`: meaningful product/API stability milestone; **not** required until scope and compatibility are mature.

While `0.x`, public API and save compatibility may evolve, **but explicit migrations must still be considered**. Do not treat SemVer major/minor numbers as a license to corrupt worlds.

`+build.metadata` MAY include the short Git commit (e.g. `0.1.0-alpha.1+g20a07f0`), but metadata does not increase semantic version ordering. Date is only a UTC provenance field; do not use Calendar Versioning as the user-visible main scheme.

## 3. Distinguish release identity from build identity

Each tested artifact must have:

```
productVersion: 0.1.0-alpha.1
commit: <full git sha>
sourceDirty: false | true
buildTimestampUtc: <ISO 8601 UTC>
compileForge: 3.4.9.171   # currently documented MDK profile
runtimeForge: 3.3.8.164   # observed Tekkit Dev runtime; read dynamically
minecraftVersion: 1.2.5
jarSha256: <SHA-256 of deployed/running JAR>
```

A clean release build should come from a fixed commit. Dirty builds are for tests only and must be labeled clearly. Do not bake a final JAR hash into the JAR itself (self-reference); compute the hash **after packaging** and compare it against the running code-source hash through Dev Bridge.

## 4. Single source of truth

`BuildVersion.VERSION` is canonical. ModLoader and the startup banner use it directly. The canonical `zto-tools/build-and-deploy.bat` checks `mcmod.info` against it before compiling. Update both together and add an entry to the changelog for a version change.

The build embeds `zto-build.properties` with the product version, full Git SHA, dirty state, UTC timestamp and compiler profile. The adjacent `ZedsTekkitOverhaul.jar.build.json` includes those values plus the SHA-256 of the finished archive. Keep the stable deployed JAR name. The Tekkit Dev lifecycle verifies the archive hash against the running code source and compares the running ModLoader version with the build record. Its report records pack Forge/FML from the current launch log and the Java version from the owned process.

## 5. Compatibility contract

- **Pack runtime:** the actual Forge/FML versions shipped with the supported Tekkit Classic instance, currently observed as Forge `3.3.8.164` / FML `2.2.106.176`.
- **Development profile:** the MDK compiles with Forge `3.4.9.171` and MCP 6.2.
- New or changed API calls need runtime verification against the distributed, obfuscated pack, not only compilation against the newer Forge.
- Never silently upgrade the user's pack Forge or claim that differences are exclusively bug fixes without inspecting the relevant APIs/behavior.
- The version manifest/report should show **both compile and runtime Forge versions**, not mask their difference.

## 6. Release process

1. Merge reviewed work; run baseline portable tests.
2. Choose the next version intentionally, update the central source and changelog.
3. Build/reobfuscate with the legacy toolchain and inspect packaged class/resource list.
4. Deploy in isolated Tekkit Dev; run actual singleplayer acceptance tests, including a negative test with Dev Bridge disabled for release candidates.
5. Record commit, dirty state, JAR SHA-256, effective Forge/FML/Java and test results.
6. Tag accepted milestones with an annotated Git tag `v0.1.0-alpha.1` (only after evidence and explicit release decision).
7. Never move an existing release tag or silently replace a published artifact. Rebuilds from the same source get distinct build hashes; material changes get a new prerelease version.
8. Report compatibility or data migration risks in release notes; preserve migration/backups for ID/NBT changes.

## 7. Branch/PR and CI conventions

- Feature branches may use the current next-release version; do not bump product version on every commit.
- Every PR states its intended release line, affected gameplay/save APIs, tests performed and known gaps.
- Require exact build evidence when claiming runtime PASS. `PASS` does not transfer automatically across different JAR hashes or different runtime versions.
- Keep dated human-friendly release notes, but do not use dates to replace product or build identity.
- Do not re-label older local worlds or legacy artifacts based on new product version.

## 8. Example release labels

- Display name: `ZedsTekkitOverhaul 0.1.0-alpha.1`
- Archive: `ZedsTekkitOverhaul-0.1.0-alpha.1.jar` (only if deployment script and pack loader tolerate the change; keep a stable deployed JAR alias if necessary).
- CI/run report: `version=0.1.0-alpha.1; git=20a07f0; sha256=...; compileForge=3.4.9.171; runtimeForge=3.3.8.164`
- Historical entry: `0.7.0-distillation-prototype` — pre-Survival prototype naming scheme.

## 9. Future Dev Bridge GUI note

Track an opt-in test status overlay as a **future developer-only feature** showing active scenario, measured simulation ticks, current assertion, PASS/FAIL and cleanup state. It must be disabled by default and absent from normal releases; never show tokens or sensitive file paths. It is **not** part of the 0.1.0-alpha.1 survival vertical slice.
