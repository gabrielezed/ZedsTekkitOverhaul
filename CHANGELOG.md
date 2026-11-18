# ZedsTekkitOverhaul changelog

## 0.2.2-alpha.1 — Aerial predator prototype

- Add the Aerial Predator Spawn Egg for Creative testing. The predator flies in three dimensions, favors exposed airborne players, and uses a telegraphed dive with a fixed trajectory.
- Add its articulated wings, hooked bill, split tail, and original pixel-art atlas. Flight poses follow its hunt and movement.
- Add an Aerial Predator view to the Debug Hub for the nearest creature's target, destination, phase and recent failures.

[Implementation and playtest](docs/agents/survival-022-aerial-predator.md)

## 0.2.1 — Enderkin ecology and territorial survival

- Sustained territorial activity increases ordinary hostile pressure. Activity persists across saves, decays during absence, and can shape lunar assault admission when enabled.
- The Numpad 1 Debug Hub opens the Lunar Cycle, Survival & Assault, and Territorial Threat views. Read ordinary spawn counts separately from the threat recorded at lunar admission.
- All five Enderkin have Creative spawn eggs and can appear in ordinary Overworld survival with the companion agent. Each role has distinct mobility, and exposed supported industry increases the chance of meeting a Saboteur.
- The Tank uses telegraphed heavy strikes and can breach eligible walls on Hard when navigation stalls. Lunar placement can follow bounded routes around obstacles and beneath trees while retaining spawn clearance, cover, and population checks.
- The Thief steals recoverable inventory items. Sneaking with a vanilla compass shows a temporary Ender Trail. The Saboteur can steal empty supported BuildCraft item pipes and retains its existing temporary disruption behavior.
- The build stops before updating the published JARs if MCP reports a compile failure.

[Gameplay and settings](docs/agents/survival-021-implementation.md) · [Integration evidence](docs/agents/survival-021-integration.md)

## 0.2.0-alpha.6 — Enderkin bestiary (visual prototype)

- Add Tank and Thief Spawn Eggs for Creative inspection. The Tank has a broad, recessed silhouette and a weighted stride. The Thief has an asymmetric crouch, an alert idle pose and a quick reach.
- Bring Skirmisher and Breacher skin and eye colors closer to the original Enderman and Saboteur palettes.
- Tank combat, Thief theft and recovery, physical Saboteur component theft, encounter integration and balancing remain in development.

## 0.2.0-alpha.5 — Enderkin Saboteur

- Add a Creative-spawned Saboteur with a crouched model, luminous eyes, rapid pursuit and short blinks across clear, level ground.
- A visible, interruptible windup can pause new output from a ZTO Hopper for up to 80 ticks. Input, collection and stored stacks remain available. The pause expires automatically and is transient across saves.
- Support dedicated BuildCraft wooden extraction lines and RedPower Transposers drawing from a ZTO Hopper, BuildCraft liquid extraction from a Distillation Receiver, and the EE2-to-IC2 Alchemical Converter link. [Supported layouts](docs/agents/saboteur-industrial.md)
- Consolidate inventory gestures, moon rendering and the restored direct industrial hooks into one Prism-managed ZTOAgent. [Compatibility acceptance](docs/agents/compatibility-acceptance.md)
- Correct the Alchemical Converter's north and south EE2 packet source offsets. Existing converter saves use the same buffer format.
- Add local read-only diagnostics and a silent Tekkit Dev fixture for counterplay, transfers and recovery. [Rules and evidence](docs/agents/survival-02-alpha5-saboteur.md)

## 0.2.0-alpha.4 — Hard assault pressure

- Restore a bounded mix of skeletons and zombies in Hard full-moon assaults. A closed wooden entrance is vulnerable to native zombie behavior.
- Reserve existing active-population space for selected later-wave Enderkin when the experiment is enabled. The read-only assault snapshot identifies selected roles, cap deferrals, and the frozen admission difficulty. [Balance and verification](docs/agents/survival-02-alpha4.md)

## 0.2.0-alpha.3 — Contextual assault composition

- The opt-in territorial experiment freezes melee-led Quiet waves and a mixed melee/ranged Elevated opening on Easy and Normal. Ordinary keeps the legacy order. Hard keeps vanilla zombies out of assault waves to protect wooden doors.
- The read-only assault snapshot exposes ordered vanilla slots alongside the insertion records. [Design and verification](docs/agents/survival-02-alpha3.md)

## 0.2.0-alpha.2 — Regional threat and contextual assaults

- Require sustained local presence before recording territorial activity and favor still-active regions when the record cap is reached.
- Distinguish exposed terrain and swamps from ordinary Overworld environments with bounded biome-edge blending.
- Use one frozen admission profile to redistribute the existing full-moon assault waves when territorial adaptation is enabled. Show the decision and allocation in Dev Bridge. [Territorial contract](docs/agents/survival-02-alpha2.md)

## 0.2.0-alpha.1 — Territorial activity and threat

- Record bounded, decaying activity in local Overworld regions and persist it with the save.
- Combine activity with an independent biome signal and smooth activity across cell boundaries.
- Offer opt-in territorial tuning at natural full-moon assault admission. Quiet regions can reduce the configured budget by one slot.
- Expose the current local assessment and the budget selected at admission in Dev Bridge. [Territorial contract](docs/agents/survival-02-alpha1.md)

## 0.1.0-alpha.8 — Experimental Enderkin encounters

- Add opt-in Enderkin substitutions for waxing-gibbous patrols and later full-moon assault waves. Existing hostile budgets and population caps still apply.
- Check outdoor approaches and model clearance before placement, with a bounded fallback to ordinary composition.
- Consolidate startup settings, registry IDs and EMC rules in the unified configuration. Import existing values and keep recovery copies. [Settings and migration](docs/agents/configuration.md)
- Record special selection, placement failures and successful substitutions in Survival Debug and the read-only assault snapshot. [Configuration and playtest](docs/agents/survival-alpha8.md)

## 0.1.0-alpha.7 — Tactical hostile prototypes

- Add Skirmisher and Breacher prototypes with stable entity save names and Creative spawn eggs.
- Darken both Enderkin skins, keep their eyes visible in low light and refine walking, ranged release and barrier impact poses.
- Give the Skirmisher a hands-cast Ender Pulse with a moving faceted core, rotating violet contours and single-target collision.
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
