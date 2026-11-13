# Survival 0.1.0-alpha.5 candidate

Alpha.5 builds on [PR #25](https://github.com/gabrielezed/ZedsTekkitOverhaul/pull/25), merged as `6e61d3e`. [PR #26](https://github.com/gabrielezed/ZedsTekkitOverhaul/pull/26) merged the alpha.5 changes as `90fa6f1`. This record describes the alpha.5 build. See [alpha.6 placement](survival-alpha6.md) for current light and perimeter rules.

## Encounter composition

`LunarService` still gates natural dusk and replay. `SiegeDirector` and `LunarPatrol` request a loadout from `EncounterLoadouts` after `SpawnController` returns a successfully inserted mob. `LoadoutPolicy` makes one roll per insertion. A failed placement spends neither a loadout roll nor an equipment slot. There is no equipment assignment on entity load. Existing timing, pause, species order, light, placement, population limits and the default 4/5/6 assault wave allocation remain the encounter inputs.

| Profile | Equipment |
| --- | --- |
| Vanilla | Stock Zombie or Skeleton behavior |
| Light Zombie | Stone sword |
| Protected Zombie | Leather chestplate |
| Protected Skeleton | Chain chestplate with its native bow |

The initial weights are percentages per successfully inserted mob before the caps. Rolls outside the listed weights produce vanilla mobs.

| Encounter | Zombie light / protected | Skeleton protected | Maximum equipped |
| --- | ---: | ---: | ---: |
| First-quarter patrol (2 mobs) | 15% / 0% | 0% | 1 |
| Waxing-gibbous patrol (3 mobs) | 20% / 10% | 30% | 1 |
| Full moon, wave 1 | 20% / 0% | 20% | 1 |
| Full moon, wave 2 | 25% / 10% | 35% | 2 |
| Full moon, wave 3 | 30% / 20% | 50% | 2 |

The assault cap is five across all three waves. Hard continues to use Skeletons for both encounters to avoid wooden-door breaking. First quarter on Hard therefore uses vanilla loadouts. Quiet phases and ordinary spawns follow their existing rules. Use the existing config value of 15 for baseline assault trials. Installations with older explicit values keep them.

`MobEquipment` owns rendering, damage effects and Forge entity NBT. The new optional `NoDrop` flag is stored per gear stack. Death consumes the backport slots and emits no encounter gear. Ordinary loot and explicitly equipped fixture drops retain their existing behavior. The Survival Debug screen reports profile counts, failures and caps. The existing read-only `assault.snapshot` observation adds bounded assault and patrol equipment summaries and per-wave cap information.

## Bed anchors

`disableNightSkipping=true` is an MLProp in `config/mod_ZedsTekkitOverhaul.cfg`. On restart with the setting enabled, clicking either half of a valid local Overworld bed sets the native player spawn coordinates during day or night. The hook keeps the legacy reach and nearby-monster checks and requires both matching bed halves and an available native bed exit. A successful click confirms the anchor and leaves the player awake. A rejected anchor attempt preserves the previous spawn.

The installed 1.2.5 bed resolves the head and handles occupancy and non-respawn dimensions before calling `EntityPlayer.sleepInBedAt`. Forge 3.3.8.164 invokes `ISleepHandler` at the start of that method. `BedRespawn` returns a handled non-sleep result, which prevents the sleep GUI, bed occupancy and the fully-asleep world tick that advances time. Player `SpawnX/Y/Z` NBT and the native respawn verifier handle persistence and destroyed or obstructed bed fallback. The handler acts only in local worlds. Set the option to `false` and restart the client to use legacy sleep. The anchor implementation supports vanilla Overworld beds and leaves the pre-hook dimension behavior to the installed game.

## Shift+Click

The existing fingerprint-checked [inventory mouse agent](../engineering/inventory-drag.md) is required. `ArmorQuickEquip` handles plain left Shift+Click in the standard local Survival player inventory when the cursor is empty. It checks `ItemArmor.armorType` and the real slot's validity and moves one item through the original slots with its damage and NBT. A filled armor slot, crafting slot, cursor stack, NEI widget or other container passes through to the existing input path. The supported baseline is vanilla armor. A modded `ItemArmor` subclass can qualify through the same slot contract. Multiplayer is outside the integration.

## Verification

| Provenance | Value |
| --- | --- |
| Source | `ce6c51003d8672df4b0fe25d5e6b0b7f9358b902` |
| Branch | `codex/survival-alpha5-composition` |
| Build dirty flag | `true`; tracked source was clean and two pre-existing untracked local notes were present |
| Build UTC | `2026-11-12T22:35:41.9246020Z` |
| JAR SHA-256 | `6d83c0efa1806cc7718eab10adc1bdd8caea5448f4f2e4b6aca01f7b42fa5eb0` |
| Build | JDK 8u504, FoxyRetroMDK 1.3.6, MCP 6.2, Forge 3.4.9.171 |
| Runtime | Isolated Tekkit Dev, Minecraft 1.2.5, Tekkit Classic 3.1.2, Forge 3.3.8.164, FML 2.2.106.176, Java 8u51 |

- Portable and MCP-side checks passed for the calendar, assault schedule, patrol pause and Peaceful cancellation, diagnostics, every profile roll and caps over 10,000 fixed-seed assaults.
- Client compilation and reobfuscation passed. The streaming ZIP verifier checked 264 entries. Archive inspection found only `zed/` classes.

| Runtime case | Observed in Tekkit Dev |
| --- | --- |
| First quarter | Natural dusk produced 2/2 vanilla mobs in 5 attempts. The same night reopened without another patrol. |
| Waxing gibbous | Natural dusk produced 3/3 in 24 attempts, including one leather-chestplate Zombie and two vanilla mobs. Inventory Tweaks failed during the lifecycle runner's shutdown after the placement checks. The save at tick 14,104 contained one chestplate with its no-drop flag. A separate client reopened that save copy and observed one equipped Zombie, zero new patrol attempts and zero assault starts through tick 14,129. |
| Full moon, interrupted | One assault started with no patrol. The first wave inserted one vanilla mob before Save and Quit. Reopen preserved the consumed boundary, reported `INTERRUPTED_ON_REOPEN`, and produced zero new starts and insertions. |
| New moon | One natural dusk, zero patrols and zero assaults. |
| Full moon, open annulus | All three wave windows opened at 100, 1,000 and 1,900 ticks. The first run inserted 4/4/0, with three equipped Zombies. It resolved at tick 3,080 and hit the Dev Bridge rate limit during terrain assertions. With bounded retry on `RATE_LIMITED`, the repeat passed placement, cap, and restart checks. The 15-slot plan inserted 3/5/1, including one chain-chestplate Skeleton in wave 1 and one in wave 3. Reopen preserved boundary 205,000 and reported `NIGHT_CONSUMED` with zero new starts or insertions. |
| Alpha.5 loadouts | The live client accepted all three profiles. A stone sword increased Zombie melee damage and a leather chestplate reduced an incoming hit. The chain chestplate survived native entity NBT, kept the Skeleton's bow and produced zero gear drops across repeated death callbacks. |
| Equipment backport regression | With the same candidate and the explicit fixture, armored and ordinary Skeletons fired native arrows. Save and Quit restored four original mobs and their equipment, including damage and a custom tag. Explicit drops appeared once on death, direct removal produced none, and comparison mobs still produced ordinary loot. |
| Beds | Day and night activation called through the installed bed class. The player stayed awake through 110 ordinary night ticks. A blocked exit and a nearby hostile each preserved the previous anchor. Save and Quit retained the valid anchor. Native death and respawn selected the bed exit. A second death after destroying the bed used world spawn. A separate launch with the setting off slept to dawn and saved the bed anchor. |
| Inventory | Synthetic LWJGL Shift+Click events ran through the installed NEI mouse dispatcher with Inventory Tweaks installed. All four armor types moved once from the main inventory or hotbar and survived Save and Quit with the expected counts, damage and tags. An occupied armor slot sent a spare helmet to the hotbar, a four-item stone stack followed the ordinary path, and a vanilla chest transferred a helmet into chest storage. |
| Diagnostics disabled | The focused bed, gear and inventory probes opened the world and passed with the HUD and Dev Bridge disabled. |

The focused probe initially needed corrections to the legacy slot accessors, sleep-light timing, player survival on the test platform and hotbar capacity. The final results above use the corrected probe against the same production JAR. The copied profile still logs the existing EMC warning for item ID 169. Physical mouse input, Inventory Tweaks sorting and comparisons across modded containers remain owner checks.

The local lifecycle report filenames end with `b9670ec29ccb49d0b80cfb50ed982138`, `37f53247b12a4d4081719e17ffcdcfdd`, `4125cf230bf44689a32fe83182d81878`, `317e2c1e57854044baecfc168ffc5887` and `4e00c6529f204a44ba84db83b0c09fe1` in the order of the five lunar rows. The first open-annulus report is `dist-zto/test-reports/980046501656309.json`. The separate waxing-gibbous reopen is in `dist-zto/test-reports/alpha5-patrol-reopen.txt`. The final focused anchor and vanilla reports end with `3a6a382344da4c8092e84d970c8d81e6` and `7c031ea1949c49079b99fb054ae59b63`. The backport regression log is `dist-zto/survival-hud-runtime/equipment-alpha5-regression-stdout.txt`.

### Reproduction

Run `zto-tools/test-lunar-calendar.bat` and compile `zto-tools/assault-tests/*.java` against `bin/minecraft`. Run `LoadoutPolicySelfTest` alongside the existing patrol and diagnostics checks. For natural dusk and reopen, use the marked-save [patrol runner](lunar-pressure.md#reproduce-in-tekkit-dev) and the existing `-Persistence Assault` path. Both check the new read-only counts and caps.

The focused `zto-tools/test-alpha5-runtime.ps1 -TemplateSave <marked-save>` probe runs in a separate Tekkit Dev copy under `dist-zto/survival-hud-runtime/Tekkit Dev`. Its local `launch.json` supplies the Java 8 runtime path, effective Prism classpath with the assembled client, and existing inventory and lunar agent arguments as `java`, `classpath` and `agents`. The runner uses the staged `natives` directory, compiles the probe against the obfuscated classes, checks the deployed JAR hash, clones the template per mode and restores the config after the two launches. Logs are written under `dist-zto/test-reports/`.

### Owner acceptance in Tekkit Dev

1. Start marked first-quarter, waxing-gibbous, full-moon and new-moon saves just before natural dusk in Normal Survival. Check that first quarter and waxing gibbous place at most two and three enemies. Use F10 to inspect the profiles. Stay alive through the three full-moon waves and compare melee pressure with ranged pressure. Try Hard separately for the Skeleton-only distribution.
2. Leave one equipped encounter mob alive, Save and Quit, and reopen during the same night. Look for the same gear on the existing mob and no additional encounter. Defeat the mob and check for ordinary loot with no added sword or armor. Confirm that the chain chestplate Skeleton still fires its bow.
3. Click the foot and head of a bed by day and at night. Confirm the short spawn message, normal clock progression and absence of a sleep screen. Die after Save and Quit and check the bed exit. Repeat after obstructing the exits and after destroying the bed. Try with a nearby hostile and an existing anchor. Restart with `disableNightSkipping=false` and confirm legacy sleep to dawn.
4. With the inventory agent loaded, Shift+Click a damaged or tagged item of each vanilla armor type from both the main inventory and hotbar. Repeat with occupied armor slots, other items, NEI controls, Inventory Tweaks sorting, a chest and a machine container. Reopen the save and inspect item counts, damage and tags. Repeat with the agent absent to confirm legacy transfer.

Owner feedback is needed on whether light protection is readable, whether wave three feels distinct, and whether safe nighttime work fits the intended rhythm. The current changes preserve player-vicinity targeting. Territorial threat, settlement recognition and persistent `SITE_SIEGE` remain future release lines.
