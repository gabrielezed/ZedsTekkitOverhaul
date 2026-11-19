# Survival 0.2.3-alpha.2 fieldwork and entrances

## Vein mining suspended

This candidate leaves attack and harvesting with the pinned local Survival controller. The previous Vein Mining control is no longer registered. An old binding in local options has no effect. Ordinary mining uses the equipped tool's normal wear, EU and drop rules. There is no queued work or saved gesture state to migrate.

The earlier headless timing measurements exercised controller methods and synthetic key state. They did not validate a physical gesture. The pinned `aes.b(IIII)Z` hook called ZTO before `onBlockStartBreak` and again on a successful return, after native removal, tool cost and harvesting. The patch also gated both attack entry points and that harvest method at entry. The queue called the harvest method directly instead of going through the client ray target, click callback and native damage animation. Its five-block player-to-origin check plus six-block seam radius could reach well past the four-block native controller reach. An empty or cancelled queue cleared the action while leaving the latch active until an input was released or focus was lost. Native hit effects and swings remained tied to the cursor despite mining a different block. Holding attack while using an item could also feed the queue outside the normal click path. The one native wear and drop path seen in the old fixture is compatible with these input failures.

A future opt-in gesture needs to establish native targeting, permissions, cancellation and controller progress with physical input before it can return. This candidate removes the registration, queue and agent interception together. Test the restored baseline in a disposable Survival world by repeatedly mining and releasing attack with a stone pickaxe and charged IC2 drills, including while the former grave binding is held. Check that only the targeted block breaks, the very next target responds normally, and drops, durability and EU match ordinary mining. Repeat after opening inventory and switching tools.

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

1. **Ordinary mining.** Use the restored baseline checklist above.
2. **Furnace.** Shift-click logs into a fuel-burning furnace with a partially filled log fuel slot. Check the remainder in the player inventory and place a log into the recipe input by hand. Repeat with coal and two different fuels.
3. **Wooden entrance.** Place a wooden door in a narrow passage and use a Breacher egg. Switch to Survival on Normal and wait across the closed door. Watch the two-handed warning, opening and pursuit. Try interrupting with a hit and repeat with an iron door.

Run the individual pack fixtures with `zto-tools/test-zto-agent.ps1 -Focus mining`, `-Focus furnace` and `-Focus breacher`. The full pack suite also exercises the industrial Saboteur, Tank, native Enderkin spawning, IC2 jetpack and the inventory hooks. For graphical use the [input-isolation rules](testing.md#background-client-launch).

## Corrected alpha.2 candidate

| Evidence | Result |
| --- | --- |
| **PASS** — MCP build and packaging | MCP 6.2, JDK 8 and development Forge 3.4.9.171 compiled and reobfuscated. All 424 mod entries passed streaming validation. |
| **PASS** — native Survival mining | Release interrupted a partial hit. Holding attack on the exhausted target left adjacent ore intact. Manually targeting all six coal blocks yielded six drops with each tool and left adjacent stone and isolated ore. Stone pickaxe spent 6 durability, Mining Drill 300 EU and Diamond Drill 480 EU. |
| **PASS** — pack regressions | `zto-tools/test-zto-agent.ps1 -Pack` passed on Tekkit Classic 3.1.2, Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176 and pack Java 8u51. The pinned probes covered all six fuel-burning furnace families and native bucket return, Breacher wooden door and gate, industrial Saboteur, native spawning and territory, aerial ecology, lunar authority and the existing agent hooks. See [ecology and theft measurements](survival-023-implementation.md#alpha2-correction-verification). |
| **PASS** — portable checks | Configuration, lunar schedules and roles, crafting transactions, inventory drag slot policy and documentation links. |
| **PASS** — Tekkit Dev deploy | The installed mod and the existing Prism-managed agent match the candidate SHA-256 values below. |
| **NOT RUN** — graphical and owner playtest | Physical input, NEI and Inventory Tweaks on screen, rendered poses, wildlife feel and fairness. Vein-gesture cases are suspended with the gesture. |

| Artifact | Identity |
| --- | --- |
| Tested source | `7467ef414a22712bdffdcdcfc558a52104da4dd0` |
| Mod SHA-256 | `1deafe34633b83acb0820f9faccbe77c7af2198b390d8cff4b86223da4e51d52` |
| Agent SHA-256 | `f5216604ce3eb89bdc0d5719d0a7dd3bbab2604ea7fdec011aebb842f85b0890` |

The sidecars report the existing untracked owner files. Tracked production sources matched the tested commit.

## Previous combined candidate

Production source `f4120f33cc00f53ee0230c09b080259b432d6573`. The sidecars flag the worktree as dirty because it contains untracked owner notes. The tracked production sources matched the build commit.

| Staged artifact | SHA-256 |
| --- | --- |
| `ZedsTekkitOverhaul.jar` | `c9bd9e3ff1d1dc57f52d5d117d75fc90b94b06c744e491764d6d86182d724f7d` |
| `ZTOAgent.jar` | `e93eb776aca38a411e44b01ca2e80c1dab29b38667fdc56b6fa28a2c88675674` |

- **PASS, build.** MCP 6.2 with JDK 8 and development Forge 3.4.9.171 compiled and reobfuscated the candidate. Streaming validation read all 432 mod entries. A copied Prism manifest installed one managed agent with both verified hashes.
- **PASS, headless pack.** Tekkit Classic 3.1.2 on Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176 and Java 8u51. The pinned Survival controller and window-click hooks ran through the actual mining and furnace fixtures. The native Breacher and Tank, industrial removal, full and reduced pack linkage, direct industrial callbacks, natural Enderkin, biome ecology and IC2 weather probes passed.
- **PASS, portable.** Crafting transactions, inventory drag slot policy, lunar and repository documentation checks.
- **NOT RUN, graphical.** Physical key binding, NEI and Inventory Tweaks interactions, pose readability, and owner playtests still need a graphical session. The combined candidate is staged.
