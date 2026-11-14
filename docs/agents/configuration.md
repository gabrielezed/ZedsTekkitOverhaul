# ZedsTekkitOverhaul configuration

Edit `minecraft/config/ZedsTekkitOverhaul.cfg` inside the instance while Minecraft is closed. ZTO reads it once per process before feature registration. The file uses Java Properties syntax with `key=value`, `#` comments and ISO-8859-1 encoding. Use Java Unicode escapes for characters outside that encoding. Keep `configVersion=1` in a migrated file.

## Survival

| Key | Default | Supported values and effect |
| --- | --- | --- |
| `playerAssault` | true | Full-moon player assaults |
| `lunarPatrols` | true | First-quarter and waxing-gibbous patrols |
| `lunarOutdoorLightPressure` | true | Limited lunar outdoor artificial-light exception. Set false for alpha.5 placement rules. |
| `experimentalEnderkinEncounters` | false | Set true to allow opt-in substitutions. Set false for ordinary lunar composition. Creative spawn eggs work either way. |
| `disableNightSkipping` | true | Use eligible local Overworld beds as respawn anchors without sleep or time advancement. Set false for native bed behavior. |
| `assaultSpawnBudget` | 15 | 1–24 hostiles per assault |
| `assaultWaveInterval` | 900 | 400–1200 simulation ticks between waves while enemies remain |
| `assaultActiveCap` | 8 | 2–12 active tracked assault hostiles |
| `assaultOuterRadius` | 16 | 14–28 blocks horizontally |

## Interface and diagnostics

| Key | Default | Effect |
| --- | --- | --- |
| `creativeInventoryBackport` | true | Use the backport in local Creative when its optional assets are installed. False uses native Creative with NEI. |
| `partialRecipeFill` | false | Alt-click the recipe prepare button to place available ingredients. |
| `craftingUxShortcut` | 0 | Shortcut for the hovered crafting UX button. |
| `lunarDebugKey` | 0 | Open Lunar Debug. F9 is 67. |
| `lunarVerboseLogging` | false | Log lunar transitions, initialization and listener failures. |
| `survivalDebugHud` | false | Enable the local Survival Debug HUD and history. |
| `survivalDebugHudKey` | 68 | Open Survival Debug. 68 is F10. |
| `survivalDebugLogCapacity` | 250 | 20–300 entries per save session when the HUD is enabled. |

Booleans accept either capitalization. Key codes use LWJGL values 0–255. Zero disables the shortcut. Controls can persist registered key bindings in Minecraft's own `options.txt`.

## Registry and integration

| Keys | Defaults |
| --- | --- |
| `block.advancedCraftingTable`, `block.foundry` | 3000, 3001 |
| `block.hopper` | 3030 with Hopper assets. An existing entry requires the assets. |
| `block.alchemicalConverter` | 3024 |
| `block.boiler`, `block.heater`, `block.column`, `block.condenser`, `block.receiver`, `block.thermometer` | 3010–3015 in this order |
| `block.gasolineFlowing`, `block.keroseneFlowing`, `block.dieselFlowing`, `block.heavygasoilFlowing` | 3016, 3018, 3020, 3022. The still block uses the next ID. |
| `item.gasolineBucket`, `item.keroseneBucket`, `item.dieselBucket`, `item.heavygasoilBucket` | 22000–22003 in this order |
| `item.skirmisherEgg`, `item.breacherEgg` | 22004, 22005 |
| `integration.buildcraftOilBucketItemId` | -1 for autodetection, or the registered runtime item ID from 0–31999 |
| `emc.profile` | Empty for compatibility values. Use escaped newlines between directives. See the [EMC profile guide](../engineering/emc-profile.md). |

Keep assigned IDs stable for existing worlds. Block IDs use 1–4095, flowing IDs use 1–4094, and item constructor IDs use 0–31743. Inventory item IDs are constructor IDs plus 256. Registry collisions still fail with a diagnostic. Restore the original assignment before opening saves after an ID error.

## Legacy ownership inventory

These files were present in the installed Tekkit Dev profile. The source audit also checked the staged runtime, user-facing documentation and developer runners. All settings above are user-editable and restart-required. The registry and startup snapshot consume values during mod initialization. EMC rules apply once per process on the first eligible world tick after EE2 initialization.

| Legacy file | Owner and previous reader/writer | Keys and defaults | Migration |
| --- | --- | --- | --- |
| `mod_ZedsTekkitOverhaul.cfg` | ZTO settings generated and read by FML's ModLoader adapter | Survival, Interface and diagnostics keys above | Import both the active name and FML's `.cfg.bak` when present. |
| `ZedsTekkitOverhaul.cfg` in Forge syntax | ZTO crafting and Hopper via Forge Configuration load/save | Table 3000, Foundry 3001, Hopper 3030 with assets | Keep an exact `.legacy` copy before replacing. |
| `ZedsTekkitOverhaul-Distillation.cfg` | ZTO distillation via Forge Configuration load/save | Distillation blocks, fluids, buckets and oil override above | Import category-qualified keys. Old underscore variants of the heavy gas oil keys are accepted. |
| `ZedsTekkitOverhaul-AlchemicalPower.cfg` | ZTO converter via Forge Configuration load/save when EE2 and IC2 integration is available | Converter 3024 | Import the block ID. |
| `ZedsTekkitOverhaul-Tactical.cfg` | ZTO spawn eggs via Forge Configuration load/save | Egg constructor IDs 22004 and 22005 | Import both item IDs. |
| `ZedsTekkitOverhaul-emc.txt` | ZTO EMC integration reads this optional owner-authored UTF-8 profile | Header and rules, absent by default | Import the full text into `emc.profile`. |

No other mod reader for these ZTO files was identified. IC2, BuildCraft, RedPower, EE2, NEI, Forge, Inventory Tweaks and other pack settings belong to their respective mods. The pack can maintain its own files on startup.

## Precedence and recovery

1. On first start, an existing Properties file wins for every key it contains. For absent keys, ZTO checks the verified legacy sources. The old shared Forge file or its recovery copy takes precedence over dedicated Forge files if they share a qualified key. The dedicated files load in Alchemical Power, Tactical, Distillation order. The active MLProp filename takes precedence over its FML backup when both are still present at ZTO load time.
2. The installed FML adapter configures ModLoader mods in pre-initialization, before instantiation and `load()`. Without public `@MLProp` fields it attempts to rename the existing file to `.cfg.bak` and does not generate a new one. ZTO reads that backup during migration. The dedicated legacy files and EMC input are retained for recovery.
3. A missing or empty canonical file is initialized. A partial unversioned Properties file retains its text and receives the migration marker plus missing values. After migration, `configVersion=1` makes the file authoritative. Deleting individual keys then selects built-in defaults. The old files remain available for manual recovery. Preserve the canonical file and backups when rolling releases back.
4. Invalid booleans or unparsable numbers are logged with the key and effective default. Out-of-range Survival numbers clamp to their existing limits. An invalid canonical setting falls back to its built-in value even if a legacy source supplies the key. Valid canonical edits, comments and unknown keys are preserved byte-for-byte on normal startup. Unmapped legacy text remains in the original files or backups.
5. Writes stage a complete file beside the target and then move it into place. A write failure logs a warning and uses the loaded settings for that session. A conflicting Forge backup is preserved along with the original. Restore the matching backup and retry with Minecraft closed. A settings file that cannot be read or parsed as a document fails startup with a recovery diagnostic to protect existing registry assignments.

## Extending the contract

Declare the key and default in `ZtoConfig`. Validate it before passing the typed value to the existing consumer. Add a legacy alias only for an established on-disk key, add a startup test, and document the new option. Older versioned files use the new default until the user adds the key. When revising the file format, implement an explicit upgrade that preserves comments and unknown keys. Keep diagnostics opt-in.

World saves own lunar phase/alignment, replay markers, entity and machine NBT, and future territory or settlement data. The active world session owns encounter scheduling, entity tracking and transient debug history. Recipe bookmarks are user interaction data. Audit TSVs, logs, captures, test markers and build metadata are outputs. JVM `-Dzto.*` opt-ins, Dev Bridge discovery/token files and disposable-world requests retain their existing launch and runtime ownership.

The lunar service still supplies immutable timing snapshots. The director decides encounter admission and composition. SpawnController owns placement and clearance. Tactical entities own their movement and combat. HUD and Dev Bridge observe those boundaries. A future local threat assessment can inform admission without making the lunar calendar or global configuration own territorial records. See the [territorial design contract](territorial-threat-design.md).
