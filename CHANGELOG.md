# ZedsTekkitOverhaul changelog

## Unreleased — Survival alpha.2 gameplay

- Stage full-moon player assaults in three waves with bounded retries, active mob tracking and a separate nearby-hostile limit.
- Show wave timing, insertions, active mobs, skipped allocation and spawn constraints in Survival Debug.
- Add a repeatable [assault playtest world](docs/agents/survival-assault-alpha2.md#owner-playtest-world) with starter equipment and a natural full moon approaching dusk.

## 0.1.0-alpha.1 — Survival candidate

- Introduce the singleplayer full-moon player assault: bounded vanilla enemies, a short audible cue, and no automatic replay after a save is reopened.
- Adopt SemVer prereleases and record the Git SHA, dirty flag, UTC build time, and archive SHA-256 for the canonical legacy build. Tekkit Dev acceptance checks the running JAR.
- The old `0.7.0-distillation-prototype` identifier was a prototype label. This reset preserves registered block IDs, TileEntity names, and existing save schemas. The new encounter stores its own small record.
