# ZTO test automation bridge

The first scenario exercises the registered ZTO Hopper and vanilla chest in the authoritative Tekkit Dev singleplayer world. The bridge makes a narrow fixture and seeds four stone items. The actual Hopper performs all transfers through its usual inventory adapters.

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
| Query machine.inspect@1 | worldId, dimension, x, y, z | Hopper slots, facing (legacy side index), lock and output mode, or boiler formation, missing part count, fluid amounts and capacity in mB, fraction IDs and progress in ticks. |
| Query test.context@1 | Empty | Test gate state, world/session/revision, fixed arena, owner/run, dirty flag, observed simulation ticks, local arena item-entity count or null, and startup code-source JAR SHA-256 or null. With testing disabled it returns allowed=false. |
| Command fixture.create@1 | sessionId, worldId, runId, revision | Place one registered Hopper pointing down into one empty vanilla chest. Verify the reserved volume and record exact tile ownership. |
| Command fixture.reset@1 | Same | Empty the owned inventories and restore Hopper cooldown to zero. |
| Command test.input@1 | Same plus count (1–8) | Seed untagged stone (ID 1, damage 0) into the Hopper's first slot when both owned inventories are empty. |
| Command fixture.destroy@1 | Same | Empty owned inventories before block removal, remove the two owned blocks and check the arena. |

Send queries to POST /v1/query and commands to POST /v1/command. Successful commands return fixture identity, the new revision and simulation ticks at execution. Snapshot stacks are null or have itemId, damage, count and tagged. The tag-presence bit lets this scenario require untagged stone. It does not distinguish two tagged stacks with different payloads. Unknown tiles return UNSUPPORTED. Reads use all slots for supported tile types. There is no caller-supplied slot index. Hopper power inspection requires the relevant neighboring chunks to be loaded.

## Safety matrix

| Condition | Enforcement |
| --- | --- |
| Bridge disabled | Both existing read-bridge flags must be true before registration or listener startup. |
| Write flag missing | Command route is absent. Inventory and machine queries remain available. |
| Unauthenticated or oversized input | Existing bearer authentication, byte caps, worker count, queue depth and rate limit apply to both routes. |
| Remote, paused or unloaded world | Existing dispatcher refuses or cancels jobs. Execution rechecks test session and world identity. |
| Ordinary save, wrong seed or non-overworld | Commands require the test flag plus the exact canonical disposable folder and marker bytes. The bridge does not create markers. |
| Arbitrary coordinates, item, NBT, entity or action | The command schema contains none of those selectors. The single arena uses the center of the spawn chunk at Y=180 and checks a 5×7×5 volume. All command work stays in that loaded chunk and only modifies the Hopper and chest cells. |
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

The runner checks the runtime hash before creating the fixture. It captures empty pre-state and exact post-transfer quantities independently of command replies, tests an identical input retry, checks sampled drops, verifies reset, and checks both cells after cleanup. It allows up to 80 observed simulation ticks and a 10-second polling budget. Paused/stalled sessions fail within the transport or polling deadline. The terminal returns compact JSON, with bounded full snapshots under dist-zto/test-reports. Exit codes are 0 for PASS, 1 for assertion FAIL, 2 for ERROR or ABORTED and 3 for SKIP. Test output is evidence about state and inventory transfer. Graphics, GUI interaction, multiplayer and automated launch/restart require separate acceptance.

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

The Hopper tests exercised actual singleplayer ticks, including the reopened save. Computer Use opened and closed the world around the runs. Shell commands performed fixture setup, assertions and cleanup. The boiler snapshot compiled and was packaged, but was not exercised on a live boiler. No visual or multiplayer acceptance, missing-mod boot, deliberate partial-placement fault, performance benchmark or automatic client restart was run. A generic event stream and fingerprinting tagged stacks remain follow-ups. The single typed item input was sufficient for this scenario, so a redstone action was not added.

## Follow-up implementation prompts

- **Distillation batch.** Extend the existing boiler read adapter with the owned heater and receiver fields. Audit the pinned IC2 and BuildCraft contracts. Add one disposable layout and use the legitimate power and fluid entry points to seed exactly one documented batch. Assert crude and coolant usage, EU consumption, output identity and volume, progress and failure reasons using independent reads. Keep runtime, structure and rendering evidence separate.
- **Natural lunar boundary.** Prepare a disposable save at the desired pre-boundary position through the existing calendar service. Use lunar.snapshot and lunar.events, let ordinary ticks cross the boundary, and assert FULL_MOON and exactly one natural NIGHT_STARTED. Record cursor, dropped count and separate administrative transitions.
- **Build and restart.** Add an opt-in Tekkit Dev workflow that records Git SHA and worktree state, builds and deploys the candidate, checks its exact JAR hash, performs a controlled restart when necessary, and waits for a suitable disposable world. Preserve the current runner's build mismatch failure.
- **Typed events.** Add a bounded world/run-scoped stream for a specific domain transition when a scenario needs it. Reuse the lunar observer's cursor and overflow semantics. Use existing game-thread hooks and require explicit coverage for stale cursors and lifecycle reset.
