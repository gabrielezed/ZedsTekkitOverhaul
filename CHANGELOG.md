# ZedsTekkitOverhaul changelog

## 0.1.0-alpha.7 — Tactical hostile prototypes

- Add Skirmisher and Breacher prototypes with stable entity save names and Creative spawn eggs.
- Darken both Enderkin skins, keep their eyes visible in low light and refine walking, ranged release and barrier impact poses.
- The Skirmisher repositions and telegraphs ranged attacks. The Breacher follows access routes and applies bounded, non-destructive pressure at nearby barriers.
- Retain the existing industrial OBJ pipeline after inspecting CodeChickenCore 0.5.3. See the [rendering audit](docs/agents/codechicken-rendering-audit.md).

## 0.1.0-alpha.6 — Outdoor pressure and fortification candidate

- Lunar patrols and full-moon player assaults can place Zombies and Skeletons on lit outdoor ground with open approaches. Ordinary spawning still uses vanilla lighting.
- Special encounters can leave slots empty when roofs, barriers, or terrain block placement. Survival Debug reports outdoor-light insertions and placement constraints.
- Set `lunarOutdoorLightPressure=false` and restart to restore alpha.5 placement. See the [alpha.6 acceptance record](docs/agents/survival-alpha6.md).

## 0.1.0-alpha.5 — Encounter composition and nocturnal survival candidate

- Give selected lunar patrol and assault enemies a single piece of gear, with phase and wave limits. Encounter gear carries no added loot.
- Use eligible Overworld beds to set a respawn point by day or night. Set `disableNightSkipping=false` to restore legacy sleep.
- With the existing inventory launch agent, Shift+Click armor into an empty matching slot in the local Survival inventory.

## 0.1.0-alpha.4 — Lunar pressure candidate

- Add small vanilla-mob patrols at natural first-quarter and waxing-gibbous dusks, with an independent `lunarPatrols` switch and counts in Survival Debug. Full-moon assaults retain their own schedule and settings.
- Pause and resume patrols with the singleplayer simulation. Retry placements in a short window later at night while retaining vanilla light and the existing terrain and population checks.

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
