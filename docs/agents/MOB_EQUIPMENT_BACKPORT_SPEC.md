# ZedsTekkitOverhaul — Legacy Mob Equipment Backport

**Status:** Integrated in `main` for the Survival alpha.3 candidate. See the [runtime acceptance record](mob-equipment-acceptance.md) for the tested build and owner playtest status.

**Target:** `0.1.0-alpha.3` (within the Survival 0.1 release line).

**Platform:** Minecraft Java 1.2.5, Tekkit Classic 3.1.2, MCP 6.2 and Java 8. Runtime Forge 3.3.8.164 with FML 2.2.106.176. Build Forge 3.4.9.171.

**Authority:** `PROJECT_VISION.md`, `AGENTS.md`, and verified runtime APIs take precedence. This document defines desired behavior, **not** an assertion that the legacy API already provides it.

## 1. Product goal

Give selected hostile mobs practical equipment slots so future lunar assault roles can wield visible weapons and wear visible protective gear. Equipment must affect gameplay, survive normal save/reload, and have predictable drop semantics. This enables future tactical diversity without adding new mobs yet.

**This is a targeted backport, not a full modern entity inventory system.** Avoid a generic inventory GUI, arbitrary item pickup, data-driven loot framework, universal equipment on every entity, or multiplayer synchronization.

## 2. Initial supported entities and slots

- **Required:** vanilla Zombies and Skeletons in local singleplayer; investigate their exact MCP/render/AI hooks before designing concrete adapters.
- **Slots:** `MAIN_HAND`, `HEAD`, `CHEST`, `LEGS`, `FEET`. One `ItemStack` or empty per slot; defensive copies where ownership crosses APIs.
- **Required items:** selected vanilla swords for melee; existing Skeleton bow behavior; vanilla armor (leather, chainmail, iron, gold, diamond) if the installed 1.2.5 item/render ABI permits it. Record unsupported materials/variants explicitly.
- **Not required:** offhand, shields, armor trims, enchantment logic, generic pickup behavior, replacement of player inventories, mob-equipment crafting GUI, special Enderman equipment, all modded items, all entity types.
- **Compatibility safeguard:** preserve the existing skeleton bow and its original ranged attack; preserve vanilla pigman and other special-case held-item behavior without extending those mobs in this release.

## 3. Functional contract

### 3.1 Ownership and access

- Introduce the smallest practical equipment representation with five distinct slots and controlled assignment/removal.
- **No global singleton map keyed only by integer entity IDs.** A world-scoped runtime registry may be appropriate only if persistence and unload/reload semantics are proven. Prefer explicit entity-owned data via a safe verified legacy extension point when available.
- Preserve original vanilla entity AI and mobility unless equipment requires a specifically verified adaptation.
- A spawn-time or test-only assignment path must be available for deterministic validation without changing all natural mob spawns.
- Equipment must not create duplicate drops or mutate unrelated inventories.

### 3.2 Gameplay effects

- Correctly apply supported armor protection in actual incoming damage paths; do not assume modern `getArmorInventoryList()` or attribute systems exist.
- For a held melee weapon, verify that damage and attack behavior reflect the equipped item instead of merely changing the rendered model; avoid double-applying enchantments or modifying base damage twice.
- For the skeleton bow, maintain projectile spawning, aiming, attack rate and animation. A different supported held item must not silently corrupt ranged AI; unsupported combinations should be rejected or have an explicitly documented fallback.
- Equipment assignment must never silently grant excessive HP/damage or introduce global scaling.

### 3.3 Rendering

- Correct visible held items and armor layers in real Tekkit Dev, using legacy biped rendering and texture conventions verified in the installed pack.
- Validate orientation, layers, helmets, leggings, armor textures, animation, first-person/third-person compatibility where applicable, and correct fallback with empty slots.
- Avoid class replacement of vanilla or NEI-/OptiFine-patched render classes. If a transformer or renderer registration is necessary, isolate it behind a verified fingerprint, fail safely, and document precise compatibility risks.

### 3.4 Persistence and lifetime

- Use a versioned, namespaced data format that preserves existing vanilla entity NBT. Null/empty slots, world transitions, entity death and unload/reload must be safe.
- An equipped mob saved and reopened must retain the same item IDs, stack counts (where meaningful), metadata and supported tags. Do not assume that public WorldSavedData provides automatic per-entity NBT callbacks.
- No cross-world state leakage. No forced chunk loads. No growth of abandoned registry entries.
- Old saves, mob entities without equipment, and old assault records must continue to work.
- If persistence cannot be implemented without invasive core-class modification, stop, report alternatives and risks, and request approval before proceeding with a core patch.

### 3.5 Drops

- Separate ordinary vanilla drops from owned equipment drops; exactly-once semantics on death, not on chunk unload, despawn or save/reopen.
- Support a conservative, configurable or fixed initial equipment-drop policy; do not start with a full randomized loot generator.
- Define and test what happens for equipped entities killed by player, environment or another entity when the cause can be verified.
- Ensure no duplication from repeated death callbacks or retained entity state.

## 4. Architectural boundaries

`MobEquipment` (small slot/state contract) → verified legacy mob adapter → combat/render/NBT/drop integration.

`SiegeDirector`/`SpawnController` may later select a **loadout** at entity creation. They must not own inventory implementation, rendering, persistence or item-drop rules. `LunarService` must remain fully independent. Future `SITE_SIEGE` and player-vicinity assaults should be able to request a loadout without targeting-specific equipment logic.

Do not implement a generic `LoadoutManager` or rule engine until a second concrete consumer needs it. A very small named test preset is enough for alpha.3.

## 5. Required discovery gate (before implementation)

Provide a short source-and-runtime ABI audit of:

1. Zombie/Skeleton class hierarchy, AI/attack methods, and supported item/armor hooks.
2. Existing legacy held-item behavior of Skeletons and pigmen.
3. Relevant biped renderers, armor layer selection and held-item render hooks; competing installed mod patches.
4. Entity NBT read/write and death/drop paths available without direct base-class replacement.
5. MCP names versus reobfuscated Tekkit pack signatures, including OptiFine/NEI impact.

Choose the **least invasive demonstrably working** technique. Do not assume later-Minecraft APIs exist. If safe coverage cannot be obtained for both zombie and skeleton within scope, implement one complete vertical slice and document the missing integration instead of shipping visual-only equipment as complete.

## 6. Tests and acceptance

Run focused portable tests where meaningful, then verify the deployed, reobfuscated JAR in the actual Tekkit Dev runtime and a disposable world.

| Case | Required observation |
| --- | --- |
| Zombie with sword | Correct visible item and measured melee damage behavior |
| Zombie with armor | All supported slots visibly render and incoming damage demonstrates protection |
| Skeleton with bow | Existing shooting, animation and projectile behavior unchanged |
| Skeleton with armor | Visible armor without breaking bow or ordinary AI |
| Save/Quit/Reload | Same equipment and tags return; no orphan state |
| Entity death | Exactly-once equipment drops under declared policy; no duplication |
| Despawn/chunk unload | No unintended equipment drops, no unbounded registry retention |
| Unmodified vanilla entities | Normal rendering, AI, drops and spawn rules remain intact |
| Assault regression | Current `PLAYER_ASSAULT` still works with unchanged default balance |
| No Dev Bridge | Normal local startup without debug flags and no mandatory diagnostic dependency |

At least one screenshot of equipped Zombie and Skeleton, a concise damage comparison and provenance (commit SHA, dirty flag, JAR SHA-256, actual runtime version) are required. Report any blocked test honestly.

## 7. Scope limits

**Do not implement in alpha.3:** new custom mobs, procedural gear tiers, endless progression, territorial threat, settlement recognition, SITE_SIEGE, entity equipment pickup from the ground, generalized mob inventory GUI, broad modded-item compatibility, rewritten render engine, networking, unrelated infrastructure or refactoring.

**Future milestones:** alpha.4 tactical mob archetypes; subsequent 0.1 alpha combat composition and optional visual loadout variants; 0.2 territorial activity/threat; 0.3 automatic settlement recognition; 0.4 true SITE_SIEGE; later exploration, cross-mod progression, End anomalies and dragon overhaul.

## 8. Definition of done

The project owner can inspect an equipped Zombie and Skeleton in live Tekkit Dev, see their items and armor, observe coherent combat behavior, save/reload equipment, and kill entities without duplicate or accidental drops. A future assault can assign a loadout through a narrow contract. No regression in vanilla behavior or existing ZTO saves is observed in representative tests.

Stop after this complete vertical slice. Any unresolved core-patch requirement must be explicitly surfaced for a design decision rather than hidden behind additional agent iterations.

## 9. Implemented contract

- `MobEquipment.assign`, `get` and `remove` are game-thread calls on exact vanilla Zombies and Skeletons in an authoritative local world. Assign returns false for invalid combinations or an unsupported saved format. Assignment replaces the slot using a copy. Reads and removals return detached stacks with deep copies of nested item tags.
- Zombies accept stone and iron swords. Skeletons always have their native functional bow. An explicitly assigned bow occupies the owned slot and can be recovered on death. Its metadata is preserved, and the Skeleton still uses its native bow model and ranged task. All four pieces from each of the five stock armor materials are accepted in their matching slots.
- Zombie sword damage adds the difference between the stock sword attack value and the stock Zombie's 4 damage at the Forge hurt hook. This adds 1 HP for stone and 2 HP for iron to a normal accepted melee hit. Vanilla potions and difficulty continue to operate on the native attack path. The sword bonus is applied after the target's difficulty and hurt-resistance checks.
- Armor subtracts the integer part of incoming damage multiplied by total equipment armor points and divided by 25. The resulting hit goes through vanilla's native armor and potion calculations. Armor-bypassing sources skip equipment protection. Small hits may receive no equipment reduction after rounding. Equipment retains its assigned durability. Enchantment tags are stored and returned with the stack, and gear grants only the damage or protection described here.
- Owned slots live under `ForgeData["zed.tekkitoverhaul.equipment"]` with integer `Version = 1`. Each present slot uses standard 1.2.5 item-stack NBT. Removing the last valid slot removes the equipment compound. The native bow is implicit and never creates an owned drop.
- At the Forge drops callback, every owned slot is cleared and appended once to the normal drop list. This rule applies to player, mob and environmental deaths that reach the callback. Vanilla loot is generated independently. Despawn, unload and Save and Quit have no drop callback.
- Future encounters can assign individual slots on a newly constructed mob before normal insertion. Natural spawn lists, assault composition, lunar logic and HUD wiring have no equipment consumer.

### Disposable viewing pen

In isolated Tekkit Dev, use a disposable Overworld save named `ZTO-Test-<32 lowercase hexadecimal digits>`. Add the existing `.zto-disposable-test` marker in the save directory containing its exact folder name, one space, its signed seed and a single LF. Add `-Dzto.mobEquipmentFixture=<exact folder name>` to the Tekkit Dev JVM arguments. Open the marked save on Easy, Normal or Hard. The fixture creates a small roofed viewing pen above the loaded player chunk and moves the player to its viewing side. From left to right the cells contain an ordinary Zombie, a sword-and-armor Zombie, an armored Skeleton and an ordinary Skeleton. Remove the front glass to observe combat. Remove the JVM argument before restarting into the same save to keep a single set of specimens. Dev Bridge and the HUD are optional.
