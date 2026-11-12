# ZTO test automation bridge

The [Survival gameplay pass](survival-assault-alpha2.md) documents the `assault.natural_full_moon@1` scenario and its Save/Quit/Reload options in the same lifecycle runner. The fixture changes only lunar alignment in a marked disposable Overworld. A real natural boundary must follow.

The first scenario exercises the registered ZTO Hopper and vanilla chest in the authoritative Tekkit Dev singleplayer world. The bridge makes a narrow fixture and seeds four stone items. The actual Hopper performs all transfers through its usual inventory adapters.

## Singleplayer core suite

| Scenario | Setup and assertions |
| --- | --- |
| `hopper.basic_transfer@1` | Transfers four stones downward to an empty chest. Checks identities, counts, replay, sampled drops, reset and cleanup. |
| `hopper.destination_capacity@1` | Seeds an owned chest with two units of free capacity, then supplies four stones to the Hopper. After at least 80 simulation ticks the chest must be full and the Hopper must retain two stones. Checks conservation, sampled drops, reset and cleanup. |
| `hopper.redstone_lock@1` | Places an owned vanilla redstone torch on a stone support beside the Hopper. Checks that four stones stay in the source for at least 40 simulation ticks, then removes power and checks normal transfer and cleanup. |
| `lunar.natural_boundary@1` | Read-only. Start within 160 ticks before 13000, 23000 or dawn, or pass `-LunarWaitSeconds` to wait for the next boundary. The runner checks the exact event sequence, natural cause, tick, phase, night state and absence of cursor overflow. It observes several ticks after the boundary to detect repeated events. |
| `distillation.gasoline_batch@1` | Places the seven registered components in the reserved arena. Fills the BuildCraft liquid ports, injects IC2 LV packets, waits for real ticks, then checks consumed inputs and drains the output via the receiver. Physical pipes and generators are outside this scenario. If placement fails partway, the arena is flagged dirty and the disposable copy must be replaced. |

Run a single scenario with `run <scenario>` or all currently registered scenarios with `suite singleplayer-core`, using the flags in the setup below. The suite starts with the time-sensitive lunar check. It runs each scenario serially, includes each report path in its JSON summary and returns nonzero if any member fails or skips. The suite covers the registered workflows only. See the [acceptance matrix](singleplayer-acceptance.md) for remaining cases and current evidence.

`-LunarWaitSeconds` accepts up to 720 seconds. The lifecycle runner below keeps the simulation ticking without window focus. When running `zed-test.ps1` directly, leave the marked save open and unpaused during the wait.

## Unattended Tekkit Dev lifecycle

Prepare one marked disposable Overworld as described below. Keep its player near spawn, its fixture arena empty and the source save closed. Deploy the candidate with `zto-tools/build-and-deploy.bat`. In **Tekkit Dev** only, enable the three bridge flags from the setup below and configure a Prism account and Java installation that can already launch the pack. Close any existing Tekkit Dev client.

From the MDK root, run this command with the folder name of the authorized save.

```powershell
.\zto-tools\test-tekkit-dev.ps1 -DisposableSave 'ZTO-Test-0123456789abcdef0123456789abcdef'
```

The script uses the standard Windows Prism data directory and launches `Tekkit Dev` by its instance ID. Use `-PrismExe` if Prism is installed outside the per-user program directory. It copies the marked save to a new marked folder, verifies the deployed candidate, launches and identifies the game process, then checks the running JAR SHA-256 through the authenticated bridge. It runs `singleplayer-core` and the live crafting transactions, stages a loaded Hopper, stops and restarts the client, and verifies that the same inventory, facing and mode survived in a new bridge session. It stops the second client when assertions finish. The natural lunar transition can take several minutes. `-StartupSeconds` bounds each launch and `-LunarWaitSeconds` bounds the lunar wait.

- `-SkipCore -Persistence Distillation` runs the crafting checks and the partial-batch persistence case on a separate copy. The fixture supplies only enough EU for half a batch. After the buffer runs out, the restart must preserve progress, fluids and fraction selection, and the parts must bind again.
- `-SkipCore -PhysicalDistillation` runs the crafting checks and one physical gasoline batch on a separate copy. A powered wooden waterproof pipe draws crude from a BuildCraft tank into the boiler, a coal-fueled IC2 Generator supplies the adjacent heater, and a second powered wooden waterproof pipe draws the gasoline into a BuildCraft tank. Cooling water is seeded directly into the condenser. The report records measured liquid and EU observations.
- `-SkipCore` alone runs crafting and Hopper restart without repeating the already-covered transfer scenarios. The 3×3 and 5×5 transaction check uses the installed recipes, the production fill planner and real result-slot and deposit clicks. It checks missing ingredients, conservation and the active Inventory Tweaks protections. Protection assertions that require a rule absent from the active profile are reported `BLOCKED`. NEI and mouse input are covered separately by actual GUI checks.

The script prints a compact JSON result and saves a report under `dist-zto/test-reports/`. Exit codes are 0 for `PASS`, 1 for an assertion `FAIL`, and 2 for `BLOCKED`. Individual checks can be blocked by the active Inventory Tweaks profile even when the lifecycle passes. Reports include the running build hash, runtime log provenance, child process and bridge session IDs, per-scenario evidence and bounded failure diagnostics. Fixture copies are retained for inspection and each invocation starts from a fresh copy of the named source. The script never adopts an old fixture. Remove retained copies through the usual disposable-save cleanup when their evidence is no longer needed.

The game bootstrap consumes a local request once at the title screen and requires the test flags, a matching candidate hash, an existing canonical save and a marker that matches the saved seed. It uses the vanilla singleplayer loading path and a non-pausing screen during the run. A second local request with the same launch nonce can ask that specific world session to shut down using the normal Minecraft save path. There is no world selection or process administration route on the bridge. A crash or forced stop blocks the persistence result. The runner refuses to start while Tekkit Dev is already running. On startup failure before the nonce announcement, it withdraws its pending request and can stop a single new Prism Java process whose native-library path matches the exact Tekkit Dev instance. Ambiguous matches require inspecting the launcher log before retrying.

### GUI-assisted runtime checks

Use the marked Tekkit Dev copy and record the running candidate hash from `test.context`. Set NEI to Survival before either test. From Creative, the first mode click selects Creative Plus and the second selects Survival. Verify hearts and hunger in the HUD. Seeded materials make these integration checks.

- **Bed sleep.** Place a vanilla bed at a safe Overworld position and capture `lunar.snapshot` during the night. Enter the bed, wait to wake and capture a second snapshot. Query `lunar.events` with the initial world ID and sequence. Store the three response data objects as `before`, `after` and `events` with `buildSha256` in the report.
- **5×5 crafting.** Stock at least 16 smooth stones, place an Advanced Crafting Table and capture `inventory.player` with an empty cursor. Open NEI uses for stone, choose Advanced Crafting and click **+** on the Foundry recipe. Verify the 16 perimeter cells, collect the Foundry and place it in inventory. Confirm the grid and cursor are empty. Capture `inventory.player` again and store both response data objects as `before` and `after` with `buildSha256`. The observation verifier uses the default Foundry ID 3001 in Tekkit Dev.

~~~powershell
.\zto-tools\test-singleplayer-observation.ps1 -Scenario bed-sleep -Evidence .\dist-zto\test-reports\milestone1-bed-sleep.json
.\zto-tools\test-singleplayer-observation.ps1 -Scenario crafting-5x5 -Evidence .\dist-zto\test-reports\milestone1-crafting-5x5.json
~~~

The verifier recomputes the assertions from the bridge snapshots and exits 1 if an assertion fails. The bed interaction, recipe placement and output collection are performed in the actual GUI.

## Audit and implementation map

| Capability | Evidence and owner | Delivery |
| --- | --- | --- |
| Loopback transport, auth, bounded JSON, queue deadlines | BridgeServer, BridgeProtocol and BridgeDispatcher. The owner-thread reader only receives parsed values. | Existing HTTP and discovery limits retained. Separate POST command route behind the testing flag. |
| Local world and pause detection | DevBridge checks the FML render-end callback, the current world object, Minecraft multiplayer state and GUI pause state. | Existing readiness and queue cancellation retained. Commands additionally check session, world and revision at execution. |
| Simulated ticks | Legacy Minecraft.runTick calls World.updateEntities and World.tick when local and unpaused. FML onPostWorldTick follows both calls. | DevBridge counts qualifying post-world callbacks separately from the existing bridge counter and legacy clock. |
| Loaded-only block and tile access | ChunkProvider.chunkExists and the legacy chunk tile map. World.getBlockTileEntity can create tiles. | BridgeSnapshots shares the existing provider guard and reads the map directly. |
| Player, Hopper and chest inventory | InventoryPlayer holds the selected hotbar index and cursor stack. TileHopper and TileEntityChest expose IInventory slots. | Read adapters copy bounded slot arrays. Chest halves are separate physical inventories. |
| Distillation | TileEntityDistillationBoiler exposes FractionationProcess values. Pinned reference covers BuildCraft 2.2.14 and IC2 1.97. The GUI status getter looks up the heater through the world. | Read-only boiler process quantities and formation state. No new external API dependency or heater lookup. |
| Lunar events | LunarObserver already provides sequence cursors, wrap/loss information and per-world rebinding. | Existing operations retained. Hopper assertions use sampled state. |
| Hopper test writes | Existing HopperAcceptance is a separate diagnostics hook. TileHopper, HopperRegistry and legacy block APIs suffice for the new fixture. | HopperTestFixture owns the arena, placement, reset, input, destruction and audit. Gameplay modules do not depend on it. |
| Shell and package provenance | zed-dev.ps1 pins loopback and validates discovery. The build deploys the reobfuscated mod archive. | zed-test.ps1 adds the single scenario and compares the runtime startup code-source JAR hash against the supplied candidate. Portable transport coverage extends the existing harness. |

## API v1

Requests keep the existing envelope with ASCII requestId, op and a flat args object. Schema IDs end in @1. Send the bare operation name in the op field. Positive identity fields are integers. The world, run and session fields identify the single active fixture. The session ID changes on client restart, and the world ID changes on world replacement.

| Route and schema | Arguments | Result or effect |
| --- | --- | --- |
| Query inventory.player@1 | Empty | World ID, selected hotbar slot, up to 40 slots in vanilla order, and the cursor stack. Slots 0–8 are the hotbar, 9–35 are storage, and 36–39 are armor. |
| Query inventory.container@1 | worldId, dimension, x, y, z | Loaded Hopper or vanilla chest tile, slot array and stack limit. A chest position returns the physical half. |
| Query machine.inspect@1 | worldId, dimension, x, y, z | Hopper slots, facing (legacy side index), lock and output mode. Boiler formation, missing part count, fluid amounts and capacity in mB, fraction IDs and progress in ticks. Heater stored EU and IC2 EnergyNet membership. |
| Query test.context@1 | Empty | Test gate state, world/session/revision, fixed arena, owner/run, dirty flag, observed simulation ticks, local arena item-entity count or null, and startup code-source JAR SHA-256 or null. With testing disabled it returns allowed=false. |
| Command fixture.create@1 | sessionId, worldId, runId, revision | Place one registered Hopper pointing down into one empty vanilla chest. Verify the reserved volume and record exact tile ownership. |
| Command fixture.reset@1 | Same | Empty the owned inventories and restore Hopper cooldown to zero. |
| Command hopper.prepare_capacity@1 | Same as fixture.create | Fill the empty owned chest with untagged stone, leaving two units of room in the final slot. Requires both inventories to be empty. |
| Commands hopper.power_on@1 and hopper.power_off@1 | Same as fixture.create | Place or remove the vanilla redstone torch and its stone support at the two reserved cells immediately east of the fixture. |
| Command test.input@1 | Same as fixture.create plus count (1–8) | Seed untagged stone (ID 1, damage 0) into the empty Hopper. The chest must be empty unless the capacity operation prepared it. |
| Command fixture.destroy@1 | Same | Empty Hopper fixture inventories before block removal, or remove the owned endpoint-test distillation parts, then check the arena. |
| Command hopper.stage_reload@1 | Same as fixture.create | Place a north-facing Hopper in Input mode with four stones. The fresh fixture stays in the disposable copy for restart assertions. |
| Commands distillation.create@1, distillation.start@1 and distillation.collect@1 | Same as fixture.create | Place and own the seven-part layout, feed one full batch through the machine entry points, and collect the gasoline from the receiver. |
| Command distillation.start_partial@1 | Same as fixture.create | Seed the same fluids and supply half the batch's EU. Returns the number of process ticks the heater can power. |
| Command distillation.transport@1 | Same as fixture.create | Place and start the fixed physical BuildCraft/IC2 layout. Leave the fixture in its disposable copy. `machine.inspect` reads liquid amount and identity from the installed BuildCraft tank tile. |
| Command crafting.transactions@1 | Same as fixture.create | In an idle marked world, use the real containers and a temporary inventory on the game thread. Restore the live inventory before returning. Report both recipe assertions and active protection checks. |

Send queries to POST /v1/query and commands to POST /v1/command. Successful commands return fixture identity, the new revision and simulation ticks at execution. Snapshot stacks are null or have itemId, damage, count and tagged. The tag-presence bit lets this scenario require untagged stone. It does not distinguish two tagged stacks with different payloads. Unknown tiles return UNSUPPORTED. Reads use all slots for supported tile types. There is no caller-supplied slot index. Hopper power inspection requires the relevant neighboring chunks to be loaded.

## Safety matrix

| Condition | Enforcement |
| --- | --- |
| Bridge disabled | Both existing read-bridge flags must be true before registration or listener startup. |
| Write flag missing | Command route is absent. Inventory and machine queries remain available. |
| Unauthenticated or oversized input | Existing bearer authentication, byte caps, worker count, queue depth and rate limit apply to both routes. |
| Remote, paused or unloaded world | Existing dispatcher refuses or cancels jobs. Execution rechecks test session and world identity. |
| Ordinary save, wrong seed or non-overworld | Commands require the test flag plus the exact canonical disposable folder and marker bytes. The bridge does not create markers. |
| Arbitrary coordinates, item, NBT, entity or action | The command schema contains none of those selectors. The single arena uses the center of the spawn chunk at Y=180 and checks a 5×7×5 volume. Block writes stay in that loaded chunk and target the cells owned by the selected fixture. |
| Foreign, stale or repeated write | Require current session, world and revision. Non-create operations require the current run ID and the exact live tiles. The most recent successful request can be replayed with the same request ID and arguments. Older revisions fail. |
| Arena changed or partial placement/removal | Refuse changed blocks, replaced tiles, tagged or unexpected inventory contents and item entities in the arena. Attempt bounded rollback of recognized, empty setup tiles. If cleanup is uncertain, require a fresh disposable copy. |
| Audit | Each dispatched command logs operation, world/run, observed simulation tick and outcome. Discovery credentials stay out of evidence. |

This is a trusted local workstation feature. Software running as the same OS user can access that user's discovery file and can opt a disposable save in. Do not interact with the fixture or change spawn, dimension, game time or mods during a run. Fixture ownership is in memory. If the client exits with an active fixture, the next session cannot adopt it. An occupied arena will be refused. Start with another disposable copy in that case.

## Enable and prepare a save

1. Build from the MDK root with zto-tools/build-and-deploy.bat using JDK 8. Use the isolated Prism Tekkit Dev instance. Close the old client before deploying and restart after deployment.
2. In the instance Java arguments, use -Dzto.dev=true -Dzto.devBridge.enabled=true -Dzto.devBridge.testing=true. Remove the testing flag and restart to retain only reads. Remove the two original flags and restart to disable the bridge.
3. Create a disposable world in Tekkit Dev. A creative world with a known numeric seed and an empty sky above spawn is convenient. Save and quit. Make a **new copy** of that disposable save under the same instance's saves directory named ZTO-Test- followed by 32 lowercase hexadecimal digits. Keep the new folder distinct from the display name.
4. In the copy only, create .zto-disposable-test containing the exact folder name, one ASCII space, the world's saved signed decimal seed, and a single LF. For example, after setting $folder to the new copy and $seed to its actual numeric seed:

~~~powershell
$name = Split-Path $folder -Leaf
[IO.File]::WriteAllText(
    (Join-Path $folder '.zto-disposable-test'),
    "$name $seed" + [char]10,
    [Text.Encoding]::ASCII
)
~~~

5. Open the copy in singleplayer and remain near world spawn, with the game unpaused. Set ZTO_DEV_BRIDGE_DISCOVERY to the discovery **path** printed in the current launch's Forge log. Keep the file contents private.

~~~powershell
.\zto-tools\zed-test.ps1 list
$hash = (Get-FileHash .\dist-zto\ZedsTekkitOverhaul.jar -Algorithm SHA256).Hash
.\zto-tools\zed-test.ps1 run hopper.basic_transfer@1 -DisposableWorld -ExpectedJarSha256 $hash
~~~

The runner checks the runtime hash before creating the fixture. It captures empty pre-state and exact post-transfer quantities independently of command replies, tests an identical input retry, checks sampled drops, verifies reset, and checks both cells after cleanup. It allows up to 80 observed simulation ticks and a 10-second polling budget. Reads can retry a short stall within a bounded window. Paused or unloaded worlds and failed writes are surfaced to the caller. The terminal returns compact JSON, with full snapshots under `dist-zto/test-reports`. Exit codes are 0 for PASS, 1 for assertion FAIL, 2 for ERROR or ABORTED and 3 for SKIP. The unattended lifecycle runner wraps these existing scenario reports.

## Tekkit Dev run report

Verified on 2026-11-10 in the isolated Prism 11.1.1 Tekkit Dev instance. Its menu reported Minecraft 1.2.5, FML 2.2.106.176 and Forge 3.3.8.164. Forge 3.4.9.171 is the declared project target and was not exercised here. The worktree used main at e226b02ce5bc308c2f75785ca8c6c9d552c35786 with the uncommitted implementation. The SHA-256 of the packaged and running code-source JAR was 93061dc91591a2da35be2b90bef164dead6d0272b988daa480485501474c45d3.

| Command or check | Observed result | Local artifact |
| --- | --- | --- |
| zto-tools/test-dev-bridge.ps1 with Adoptium JDK 8.0.504.1 | PASS. Existing coverage plus command route/auth, schema bounds, remote and paused dispatcher states, queued write cancellation and dispatch on the owner thread. | Console |
| zto-tools/build-and-deploy.bat | Client MCP compilation and reobfuscation succeeded. The packaging check streamed 219 ZIP entries. The MDK has no server sources or server MD5s. | dist-zto/test-automation-build.log |
| zed-test.ps1 run hopper.basic_transfer@1 -DisposableWorld -ExpectedJarSha256 with the candidate hash | Four PASS results with exit 0, all with four items in the chest, zero left in the Hopper, zero sampled drops, successful replay and verified reset and cleanup. Three consecutive runs used world ID 1. After Save and Quit and reopening the same disposable save, the fourth used world ID 2 and the old ID was refused. Each run observed completion 26 simulation ticks after seeding. | dist-zto/test-reports/{777410462102190,234336539156216,296758256061978,586256690779207}.json |
| Same runner with an all-zero expected hash | ERROR, exit 2, BUILD_MISMATCH before fixture creation. | dist-zto/test-reports |
| Windows PowerShell 5.1 with a local copy of the runner expecting five items in the chest | FAIL, exit 1, actual 4 versus expected 5, with complete cleanup. The tracked runner continued to expect four. | dist-zto/test-automation-probe/dist-zto/test-reports/115009583710957.json |
| zed-dev.ps1 legacy queries and inventory.player on the candidate | world.time, world.block, lunar.snapshot and lunar.events all succeeded. Player query returned 40 slots. A stale world and an empty loaded tile returned STALE_WORLD and UNSUPPORTED. | Console |
| Direct fixture calls on the candidate | Wrong session returned STALE_WORLD, foreign owner returned FOREIGN_FIXTURE, old revision returned STALE_REVISION, and the paused client returned NOT_READY. | dist-zto/test-automation-live-audit.log and console |
| Preliminary JAR with fixed-origin arena | A disposable copy with no marker returned TEST_WORLD_REQUIRED. A distant tile read returned CHUNK_NOT_LOADED. This run used the earlier JAR and established the need for a spawn-chunk anchor. | Console |
| Candidate restarted with zto.devBridge.enabled=false | The client reached the title screen with 44 mods loaded, no discovery announcement and zero listening TCP sockets in its process. Original Prism JVM arguments were restored after testing. | Forge log and netstat |

The 2026-11-10 Hopper runs exercised actual singleplayer ticks. Computer Use opened and closed the world around these historical runs, and shell commands performed fixture setup, assertions and cleanup. Subsequent runtime evidence covers redstone and the live gasoline batch. See the [singleplayer acceptance matrix](singleplayer-acceptance.md) for build-specific results and remaining gaps.

## Follow-up implementation prompts

- **Distillation batch.** Extend the existing boiler read adapter with the owned heater and receiver fields. Audit the pinned IC2 and BuildCraft contracts. Add one disposable layout and use the legitimate power and fluid entry points to seed exactly one documented batch. Assert crude and coolant usage, EU consumption, output identity and volume, progress and failure reasons using independent reads. Keep runtime, structure and rendering evidence separate.
- **Natural lunar boundary.** Prepare a disposable save at the desired pre-boundary position through the existing calendar service. Use lunar.snapshot and lunar.events, let ordinary ticks cross the boundary, and assert FULL_MOON and exactly one natural NIGHT_STARTED. Record cursor, dropped count and separate administrative transitions.
- **Typed events.** Add a bounded world/run-scoped stream for a specific domain transition when a scenario needs it. Reuse the lunar observer's cursor and overflow semantics. Use existing game-thread hooks and require explicit coverage for stale cursors and lifecycle reset.
