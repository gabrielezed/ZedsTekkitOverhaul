# Survival 0.2.0-alpha.5 — Enderkin Saboteur

The Saboteur tests one decision in the project vision. A player can interrupt an exposed attacker, defend the approach or ride out a short interruption of a useful machine. The prototype uses Creative placement in the local simulation. The lunar calendar, outdoor placement and the Skirmisher and Breacher selection rules retain their existing roles. Site ownership remains a separate future design.

## Entity and counterplay

`ZTO.Saboteur` is the native save name. ModLoader allocates its numeric entity slot after the existing tactical registrations. The ordinary **Saboteur Spawn Egg** uses constructor ID `22006`, stack ID `22262`, metadata 0. The unified config stores `item.saboteurEgg`. Startup checks for shared or occupied slots. Keep that item ID and the entity name stable in worlds that have saved them.

- The 0.45 × 1.1 block collision hull and 12 HP support a low, forward-leaning silhouette with wide hands, a large angular head and mint eyes. The original 64 × 64 atlas uses deep violet shading and a separate full-bright eye and hand mask. Its running posture uses exaggerated short strides. Both arms lift into a trembling forward reach during windup. Smoke and a recoil pose mark cancellation, and portal particles and lowered arms mark recovery.
- Navigation requests use the legacy walker at 0.34 versus the Skirmisher's 0.25. The Saboteur closes distance to a reachable Hopper and can defend itself with a one-damage melee attack. When a Hopper is unavailable it pursues the player and can telegraph a harmless diversion.
- An attempt can try at most three 2.75-block horizontal blinks, at least 48 ticks apart. Each candidate checks the loaded halo, horizontal world height, full-depth floor support and a conservative sweep of the entire body. The swept area includes both endpoints. Physical collision shapes reject closed door panels, stairs, glass and walls. Unloaded space, liquids, fire, portals and occupied landings also reject the candidate. A turn around a tight corner generally needs normal walking. Small particle bursts mark departure and arrival.
- Actor-staggered acquisition checks an eight-block radius, within two vertical blocks of the actor's feet, at most once every 24 ticks. The scan tests block IDs first and only looks up the exact tile of a matching loaded Hopper. Adjacent route requests are separated by 20 ticks. The actor gives up a Hopper approach after 100 ticks. Player loss releases targeting through the shared tactical lifecycle.
- The windup lasts 25 ticks. A short mint-colored particle link points to the Hopper. A landed player hit cancels the windup and a blocked sightline, lost contact or changed Hopper identity also interrupts it. Both outcomes hold a pose for 18 ticks and impose a 120-tick action cooldown. Blink attempts are suppressed while hurt.

## Hopper output lease

The Hopper already commits each move synchronously through `InventoryAccess.moveOne`. The lease gates only its own outgoing call, before the call starts. The intake and item collection branches still run, subject to the ordinary transfer cooldown, redstone lock and capacity. Passive access from other inventories still follows existing game rules. The receiver and already transferred items remain ordinary inventory state. The GUI reads **Output paused** while the lease applies.

The lease lives in memory on the exact loaded Hopper instance, is exclusive and lasts at most 80 Hopper ticks. The actor must still belong to the same authoritative world, have a valid player and be ticking in a loaded chunk. Actor death, Peaceful and loss of authority expire it. Target removal, replacement and chunk unload discard it. Native NBT contains the regular registry identity, position, health, inventory, output mode and ordinary transfer cooldown. A reopened save starts with no windups, player references or leases. The same rule applies after abrupt process termination. Inventory commits and block metadata use their original save paths throughout.

IC2 cables, BuildCraft item and fluid pipes, and RedPower networks require individually verified adapters and are currently unsupported. The [installed-pack audit](enderkin-future-roles.md#adapter-boundary-and-installed-pack-audit) records possible sender-side integration paths and conservation requirements.

## Owner playtest

1. In a disposable world, place an output chest and aim a ZTO Hopper into it by sneak-right-clicking the side of that chest with the Hopper held. Place a second chest above the Hopper and load it with several stacks of cobblestone. Check that the output chest is gaining items.
2. Build a small torchlit alcove for the machine. Leave an open, level approach about eight blocks long. Add a wall or a closed door beside the open approach so you can compare an open route with a sealed one.
3. Place a **Saboteur Spawn Egg** four to eight blocks from the Hopper. Set the difficulty to Normal and use the NEI game-mode control to return to Survival. Verify hearts and hunger in the HUD. Stand within sight and a few blocks to the side. Watch for the short blink, hands-up pose and mint link as the Saboteur reaches the Hopper.
4. Strike it during the link to cancel. Let a later attempt finish, then check the Hopper GUI and receiver. Items can still enter the Hopper during the pause. Watch the receiver gain items again and reconcile the three inventories. Repeat with the approach sealed or the Hopper positioned on a raised platform.
5. Compare recognition during torchlight and an ordinary fight. Assess whether the hands and link read from several blocks away, whether 25 ticks is enough reaction time and whether the blink, four-second pause and six-second action cooldown feel fair.

## Diagnostics and reproducibility

The authenticated `saboteur.nearby` query takes empty arguments and returns at most four live records near the local player. Pose codes are 0 idle, 1 approach, 2 windup, 3 recovery and 4 cancelled. Records carry the pending target coordinates, last outcome and blink result, cooldowns and cumulative counts. Use `machine.inspect` at the Hopper for `disruptionTicks` and its actual slots. Zero remaining ticks and subsequent receiver transfers confirm recovery. The probe uses marked disposable saves and does not add a mutation command.

```powershell
.\zto-tools\test-saboteur-runtime.ps1
.\zto-tools\test-saboteur-runtime.ps1 -Mode restart
.\zto-tools\test-saboteur-runtime.ps1 -Mode stress
.\zto-tools\test-saboteur-runtime.ps1 -Mode dimension
.\zto-tools\test-saboteur-runtime.ps1 -Bridge
.\zto-tools\test-saboteur-runtime.ps1 -WithoutOptionalMods
```

The default run places a real input chest, Hopper and receiver on a flat pad with a torchlit alcove and obstruction lane. It exercises the installed collision queries, actual movement, Survival controller attacks, native transfers and reopening the save. The restart probe flushes the world while the lease is active, halts the test process without shutdown hooks and reopens the same save in a separate process. The stress probe uses eight Saboteurs, captures a rendered frame and records aggregate client timing and bounded operation counts. The dimension probe calls the installed portal travel code in both directions during a lease. The reduced profile keeps the pinned BuildCraft Core and IC2 archives because existing distillation tiles link their APIs on startup. The test entry point is separate from the gameplay selector.

## Evidence and limitations

Baseline `3ddc67b8036bb8cb9472ceae9e6e7a7d503807a7` is the integrated merge of PR #33 on `main`. The archive was built from `554bb935c21331680bdd528bf64598a29fedef1d` with SHA-256 `a102764e60faa52dd7ce32ffb4fc29e502a50f3d4152efcf99d1a730255401d3`. The sidecar reports a dirty worktree because two pre-existing, untracked owner documents were present. The tracked production source and assets matched the build commit. Later fixture and evidence commits leave those files unchanged.

| Status | Evidence |
| --- | --- |
| PASS | JDK 8, MCP 6.2 and development Forge 3.4.9.171 compiled and reobfuscated the client. Streaming validation checked 313 archive entries, including the two Saboteur textures and renderer model. |
| PASS | Tekkit Classic 3.1.2 ran Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176 and Java 1.8.0_51. The probes verified zero music and effects and an uninitialized audio backend on a private desktop. |
| PASS | The Survival probe observed normal movement, a valid blink, a landed player hit, target-replacement cancellation, lease exclusivity and output recovery after timeout, death, Save and Quit, native chunk unload and Peaceful. All 320 cobblestone remained accounted for across the input chest, Hopper and receiver. The input chest drained during the pause and the receiver stayed constant. The block ID, facing metadata and live tile stayed intact during transfer checks. |
| PASS | With the Bridge disabled, the exercise observed one successful blink in two attempts and checked four candidates. An obstructed attempt failed. Direct production geometry checks also rejected closed wood, stone, glass, stairs, half steps, gaps, unsafe height and unloaded space. The trial averaged about 13 µs over 96 open sweeps, including harness reflection. |
| PASS | A separate process reopened the same save after an abrupt halt during the lease. The fixture waited for native chunk writes before halting. The persisted Saboteur had a clean target and the Hopper resumed operating from the durable checkpoint, conserving all 320 items. A native trip to the Nether and back also cleared a live lease and preserved all 320 items. |
| PASS | The Dev Bridge observed a visible target's windup, cancellation, a positive lease remainder and expiration over authenticated HTTP. Actual daylight and torchlight captures showed the crouched model, luminous eyes and particle link. |
| PASS | Eight Saboteurs remained live for 120 stress ticks. The sample recorded 7 scans, 33 path plans, 9 blink checks and 7 diversions. The nearby unsupported furnace retained its block, facing and five items. The client used about 7.4 s of game-thread CPU over 7.5 s wall time. Used heap sampled 126 MB at setup and 121 MB at the end. These figures cover the whole client and include rendering and garbage collection. Per-actor tick cost and long-duration heap retention were not isolated. |
| PASS | Both Creative catalogs and NEI listed one Saboteur egg. Placement, collision rejection, repeated Creative use and the native save names for all three roles passed. Skirmisher movement and Pulse, Breacher obstruction, lunar recurrence and replay, territorial threat and encounter cap probes passed. The configuration runtime passed five startup and migration cases. |
| PASS | Portable config, bridge, Hopper with IC2 and BuildCraft absent, territory, assault policy, lunar, crafting, and distillation core/layout checks passed. |
| PASS | The reduced client completed the real movement, sabotage, transfer and recovery scenario with only BuildCraft Core, IC2 and ZTO in the mods folder. The fixture did not load EE2, RedPower or BuildCraft transport. BuildCraft Core reported its missing optional power framework during startup and gameplay proceeded. |
| BLOCKED | The mod by itself cannot start without BuildCraft Core. The existing distillation registry links `buildcraft.api.ILiquidContainer` during startup, before a fixture can run. The portable Hopper test also covers its inventory and NBT path without IC2 or BuildCraft. |
| NOT RUN | Owner combat balance, unrestricted play across naturally generated terrain, dedicated or remote-server behavior, and standalone in-pack distillation and crafting transactions for this candidate. |

Reports are under `dist-zto/test-reports/` and `dist-zto/saboteur-*.log`. The generated captures and disposable saves remain local.

The prototype targets pinned local singleplayer and conservatively rejects obstructed or uneven blinks. Its sampler is intended for hand-placed test encounters and visible attack lanes. The current prototype can target an exposed, empty Hopper of the supported type. A natural encounter needs owner approval, one shared special-role allocation, role-aware safe placement, validation across terrain and player defenses, and a deliberate policy for nearby machinery before selecting this role.

The Tank iteration should test one heavy, slow prototype with a committed frontal telegraph, room for turning, an exposed flank and native collision across narrow entrances. Use one existing active allocation in a controlled fixture. Verify player dodging, gaps, gear and save/reopen before general encounter composition. [Role proposal](enderkin-future-roles.md#tank-prototype)
