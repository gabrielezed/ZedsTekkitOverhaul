# Survival 0.2.3-alpha.2 fieldwork and entrances

## Vein mining

Hold **Vein Mining (hold)** in Controls, bound to the grave key by default, and finish breaking an ore block with ordinary Survival attack input. Keep both inputs down to continue. Release either input before starting another gesture. The local Survival controller performs the starter and each additional harvest. Its ordinary permission checks, Forge harvest and drop paths, durability, and IC2 energy use remain the authority.

Each gesture follows matching face neighbors from successful harvests. It includes at most 24 blocks including the starter, within six Manhattan blocks of the first block. The player stays within five blocks of the starter. A tool swap, depleted or broken tool, insufficient food, movement out of range, a changed or unloaded frontier, a GUI, death, or a world change ends the attempt. The gesture has no saved state. Ordinary mining can resume after releasing the modifier.

Native block strength accumulates per tick and includes tool, hardness and environmental or potion modifiers. Manual picks add two ticks between extra blocks and 1.2 exhaustion per extra block. The food guard permits work at six food points or above. The two installed IC2 drills use their native timing and charge costs of 50 EU and 80 EU per block. Native exhaustion applies to all tools. The drill guard checks available item charge before each extra harvest.

| Registry | Ore variants |
| --- | --- |
| Vanilla | Coal, iron, gold, diamond, lapis and both redstone ore states. The lit redstone state follows the same seam identity and harvests through its own native block. |
| IC2 1.97 | Copper, tin and uranium from `Ic2Items`. The pinned profile uses block IDs 249, 248 and 247. |
| RedPower World 2.0pr5b2 | Ruby, green sapphire, sapphire, silver, tin, copper, tungsten and nikolite from the configured ore block, ID 140 in Tekkit Dev. The native metadata keeps these eight seams distinct. |

The tool allowlist includes the five vanilla pickaxes, the three native RedPower gem pickaxes and the two IC2 drills. EE2 area tools, Nether Ores and the storage-block registries retain their ordinary paths. The bounded harvest action gives conventional mining a convenience cost and preserves the Project Vision's advantage for rechargeable equipment.

### Six-block coal comparison

The headless Tekkit fixture holds player position, uses native ground mining, and drives the same connected layout for each tool. The ordinary test includes native inter-block delay and retargeting. The vein tests receive an ordinary starter and use the production gesture for the other five blocks.

| Method | Blocks | Ticks | Tool cost | Food | Saturation |
| --- | ---: | ---: | --- | --- | --- |
| Ordinary stone pickaxe | 6 | 169 | 6 durability | 20 → 20 | 0 → 0 |
| Vein stone pickaxe | 6 | 146 | 6 durability | 20 → 19 | 0 → 0 |
| Vein stone pickaxe with saturation | 6 | 145 | 6 durability | 20 → 20 | 2 → 1 |
| IC2 Mining Drill | 6 | 71 | 300 EU | 20 → 20 | 0 → 0 |
| IC2 Diamond Drill | 6 | 35 | 480 EU | 20 → 20 | 0 → 0 |

All six coal drops were counted once per sample. Diagonal and disconnected ore survived. The focused probe also covers releasing attack, replacing the tool, the low-food and low-charge cutoffs, an opened GUI, range, an unloaded frontier, radius and count caps and native ore identity.

## Furnace quick moves

The existing ZTOAgent now observes the pinned local base controller window-click entry point. It handles accepted fuel moving from the player's main inventory or hotbar and delegates other clicks through the native container. Matching item, metadata and tags merge up to the native slot limit. A full or incompatible fuel slot leaves the stack at its source. Normal ingredient and output clicks and native consumption retain their behavior.

| Native container | Fuel slot | Acceptance |
| --- | ---: | --- |
| Vanilla Furnace | 1 | Vanilla furnace predicate and Forge fallback |
| IC2 Iron Furnace | 1 | IC2 `getFuelValueFor` |
| RedPower Alloy Furnace | 9 | The vanilla burn-time predicate used by RedPower Core |
| Railcraft Blast Furnace | 1 | Native fuel slot predicate for charcoal and coal coke |
| EE2 Dark Matter Furnace | 0 | Native tile burn-time probe |
| EE2 Red Matter Furnace | 0 | Native tile burn-time probe on a copy to preserve a Klein Star's charge |

The probe exercises coal, charcoal, sticks, dual-use logs and lava. It covers empty, partial, full and incompatible fuel slots, ingredient input, an output quick move and the Blulectric control. Vanilla 1.2.5 consumes its lava bucket. The IC2 Iron Furnace and RedPower Alloy Furnace return the empty bucket through their native ticks. The existing mouse dispatcher still owns NEI passthrough, the craft and inventory drag policies and armor quick equip.

## Wooden entrances

On Normal and Hard, the Breacher prefers an open detour. A reachable vanilla wooden door or gate on the direct chase line permits narrow acquisition of a player within seven blocks on the far side. At up to 1.8 blocks from the panel it raises both arms for 36 ticks. Native door or gate opening changes the open bit, preserves orientation and leaves the structure in place. It replans immediately and continues using normal movement and attacks. Its 0.60-block collision width fits the centered native path beside the open door's three-sixteenths panel.

A landed hit, a changed or open panel, loss of contact, an invalid player or an unloaded approach cancels the transient action. Recovery lasts 12 ticks and the next attempt waits at least 60 ticks from interruption or impact. Iron doors, solid walls and modded entrances retain their baseline behavior. The Tank owns structural demolition. This local obstacle tactic uses the existing encounter rules and supports the Project Vision's fortification counterplay.

In the native tick probe, the Breacher acquired the player without a preassigned target, started its door windup on tick 44, opened on tick 80 and crossed on tick 114. Against a retreating player in Hard it opened on tick 84 and crossed on tick 118. It opened the gate on tick 84 and crossed on tick 108. The alternate route crossed with the door closed. Easy, iron, a solid wall, a landed hit and a replaced door prevented forced opening. An opening by the player cancelled the attack and admitted pursuit. The open and unchanged door halves, gate orientation and resumed ordinary melee were checked.

## Owner playtests

1. **Coal seam.** In a disposable Survival world, hold the vein control and mine the first coal block, then release attack before the next beat. Compare a full stone-pick gesture with zero saturation and the same seam using a charged Mining Drill. Watch food, wear and EU.
2. **Furnace.** Shift-click logs into a fuel-burning furnace with a partially filled log fuel slot. Check the remainder in the player inventory and place a log into the recipe input by hand. Repeat with coal and two different fuels.
3. **Wooden entrance.** Place a wooden door in a narrow passage and use a Breacher egg. Switch to Survival on Normal and wait across the closed door. Watch the two-handed warning, opening and pursuit. Try interrupting with a hit and repeat with an iron door.

Run the individual pack fixtures with `zto-tools/test-zto-agent.ps1 -Focus mining`, `-Focus furnace` and `-Focus breacher`. The full pack suite also exercises the industrial Saboteur, Tank, native Enderkin spawning, IC2 jetpack and the inventory hooks. For graphical use the [input-isolation rules](testing.md#background-client-launch).

## Combined candidate

Production source `f4120f33cc00f53ee0230c09b080259b432d6573`. The sidecars flag the worktree as dirty because it contains untracked owner notes. The tracked production sources matched the build commit.

| Staged artifact | SHA-256 |
| --- | --- |
| `ZedsTekkitOverhaul.jar` | `c9bd9e3ff1d1dc57f52d5d117d75fc90b94b06c744e491764d6d86182d724f7d` |
| `ZTOAgent.jar` | `e93eb776aca38a411e44b01ca2e80c1dab29b38667fdc56b6fa28a2c88675674` |

- **PASS, build.** MCP 6.2 with JDK 8 and development Forge 3.4.9.171 compiled and reobfuscated the candidate. Streaming validation read all 432 mod entries. A copied Prism manifest installed one managed agent with both verified hashes.
- **PASS, headless pack.** Tekkit Classic 3.1.2 on Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176 and Java 8u51. The pinned Survival controller and window-click hooks ran through the actual mining and furnace fixtures. The native Breacher and Tank, industrial removal, full and reduced pack linkage, direct industrial callbacks, natural Enderkin, biome ecology and IC2 weather probes passed.
- **PASS, portable.** Crafting transactions, inventory drag slot policy, lunar and repository documentation checks.
- **NOT RUN, graphical.** Physical key binding, NEI and Inventory Tweaks interactions, pose readability, and owner playtests still need a graphical session. The combined candidate is staged.
