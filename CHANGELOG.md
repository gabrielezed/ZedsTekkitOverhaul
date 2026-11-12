# ZedsTekkitOverhaul changelog

## 0.1.0-alpha.3 — Mob equipment candidate

- Allow explicit loadouts for vanilla Zombies and Skeletons in local play. Zombie swords affect melee damage, armor provides visible protection, and Skeletons retain their native ranged attack.
- Persist equipped items with the mob and release them once on death. A disposable viewing pen is available for owner testing. See the [compatibility and acceptance record](docs/agents/mob-equipment-acceptance.md).

## 0.1.0-alpha.2 — Lunar Assault gameplay

- Stage full-moon player assaults in three waves with bounded retries, active mob tracking and a separate nearby-hostile limit.
- Show wave timing, insertions, active mobs, skipped allocation and spawn constraints in Survival Debug.
- Add a repeatable [assault playtest world](docs/agents/survival-assault-alpha2.md#owner-playtest-world) with starter equipment and a natural full moon approaching dusk.

## 0.1.0-alpha.1 — Survival candidate

- Introduce the singleplayer full-moon player assault: bounded vanilla enemies, a short audible cue, and no automatic replay after a save is reopened.
- Adopt SemVer prereleases and record the Git SHA, dirty flag, UTC build time, and archive SHA-256 for the canonical legacy build. Tekkit Dev acceptance checks the running JAR.
- The old `0.7.0-distillation-prototype` identifier was a prototype label. This reset preserves registered block IDs, TileEntity names, and existing save schemas. The new encounter stores its own small record.
