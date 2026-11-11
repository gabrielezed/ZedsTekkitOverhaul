# ZedsTekkitOverhaul changelog

## 0.1.0-alpha.1 — Survival candidate

- Introduce the singleplayer full-moon player assault: bounded vanilla enemies, a short audible cue, and no automatic replay after a save is reopened.
- Adopt SemVer prereleases and record the Git SHA, dirty flag, UTC build time, and archive SHA-256 for the canonical legacy build. Tekkit Dev acceptance checks the running JAR.
- The old `0.7.0-distillation-prototype` identifier was a prototype label. This reset preserves registered block IDs, TileEntity names, and existing save schemas. The new encounter stores its own small record.
