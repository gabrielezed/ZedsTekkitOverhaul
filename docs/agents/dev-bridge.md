# Dev Bridge MVP

This page documents the original read-only slice and its verification. See [test automation](test-automation.md) for the additive inventory, machine and disposable fixture contracts.

## Runtime audit

The MCP paths below refer to the installed MDK sources. They are excluded from Git.

| Evidence | Owner | Decision and risk |
| --- | --- | --- |
| `src/minecraft/zed/tekkitoverhaul/mod_ZedsTekkitOverhaul.java`, `modsLoaded` and the existing ModLoader callbacks | Client bootstrap | Check both JVM flags before starting transport or registering a bridge handler. The existing in-game hook is not available at the title screen. |
| `src/minecraft/net/minecraft/client/Minecraft.java`, `run`, `runTick`, `changeWorld`, `usePortal`, `isMultiplayerWorld` | Minecraft client loop | Local play ticks the authoritative `World` here. Remote client state is a replica. A portal can replace the active world object. The checkout has no MCP server source tree, so dedicated-server wiring is outside this delivery. |
| `src/minecraft/cpw/mods/fml/client/FMLClientHandler.java`, `onPostWorldTick` and `onRenderTickEnd`, plus `common/FMLCommonHandler.java` and `common/ITickHandler.java` | Same client thread | The world callback counts observed ticks. The render-end callback runs at the title screen and in the pause menu, and reads a fresh world identity and pause state before servicing jobs. Registration follows `creative/CreativeInventoryHook.java`. |
| `src/minecraft/net/minecraft/src/World.java`, `tick`, `getWorldTime`, `getChunkProvider`, `WorldInfo.java` and `WorldProvider.worldType` | Active local world | The single legacy clock can jump with sleep and tooling. Report its raw value and derived day fields separately from the callback counter. |
| `src/minecraft/net/minecraft/src/ChunkProvider.java`, `chunkExists` and `provideChunk`, and `Chunk.java`, `chunkTileEntityMap` and `getChunkBlockTileEntity` | Active local world | Check the actual provider's loaded map before obtaining a chunk on the same thread. Read the existing tile map directly because the normal tile getter can create a tile. Refuse unknown provider implementations. |
| `src/minecraft/net/minecraft/src/EntityPlayer.java`, `InventoryPlayer.java`, `Container.java`, `Slot.java` and ZTO `hopper/InventoryAccess.java` | Game-thread inventories and containers | Future snapshots must account for slot ownership and the held cursor stack. Some discovery or accessor paths can load chunks or change state. |
| `src/minecraft/zed/tekkitoverhaul/hopper/dev/HopperAcceptance.java`, ModLoader and FML callbacks, and the [lunar audit](lunar-cycle-implementation.md) | Client-side diagnostics and legacy hooks | Existing hopper acceptance is feature-specific and can change a test world. The bridge has its own read allowlist. No tracked lunar service or dedicated command handler was found on the branch baseline. Local lunar-agent artifacts are not a repository API. |
| `recompile.bat`, `reobfuscate.bat`, `zto-tools/build-and-deploy.bat` and local `lib/argo-2.25.jar` | MCP 6.2 build and Prism | The compiler uses Java 6 source/target with JDK 8. Use only Java 8 runtime classes for the bridge so packaging needs no JSON or HTTP dependency, including Argo. Deploy only reobfuscated `zed/` output and existing assets. |

## Architecture decision

`devbridge/DevBridge.java` owns the optional FML hook and local-world reads. `BridgeDispatcher.java` accepts bounded jobs containing only parsed values, publishes a scalar status snapshot, and completes jobs on the client thread. Each observed world instance receives a new process-local ID. A world change, pause, unload, or remote world invalidates pending work before any reader runs. Socket workers consume only replies and copied status. The transport stays alive at the menu so the same CLI session can diagnose and observe reloads. JVM shutdown closes sockets, cancels jobs and removes discovery.

`BridgeServer.java` uses a small JDK socket listener with fixed concurrency and an absolute HTTP read deadline. Only one request per connection is supported. This gives an explicit header cap and deadline even for slowly sent headers. The bounded file mailbox alternative would add polling and file coordination. Dedicated endpoints for writes, expression evaluation, scans, persistent jobs and remote administration are outside this milestone. The JAR contains the bridge classes under `zed/` because this MDK packages one mod artifact. Two JVM flags gate registration and startup.

The bridge supports the project vision by making legacy systems easier to validate from their public game-thread boundaries. The mod bootstrap is its only caller. Crafting, energy, EMC, rendering and future lunar or siege logic need no dependency on the bridge.

## Enable and call

1. Build and deploy with `zto-tools/build-and-deploy.bat` using JDK 8 and the isolated **Tekkit Dev** instance.
2. Add `-Dzto.dev=true -Dzto.devBridge.enabled=true` to that instance's JVM arguments before launch. Remove both arguments and restart to disable it. Prism can filter `JAVA_TOOL_OPTIONS`, so use the instance Java settings. Optional `-Dzto.devBridge.port=PORT` selects a fixed port. The default `0` lets the OS pick.
3. Copy only the discovery **path** from the line beginning `[ZTO] Read-only Dev Bridge enabled` in the Forge log or Prism console. Use that path for `-Discovery` or set `ZTO_DEV_BRIDGE_DISCOVERY`. The discovery file is owner-restricted and contains the bearer secret. A force-killed process can leave a stale discovery file whose token becomes invalid with the process.

```powershell
$env:ZTO_DEV_BRIDGE_DISCOVERY = '<discovery path from this launch>'
.\zto-tools\zed-dev.ps1 health -Json
.\zto-tools\zed-dev.ps1 capabilities -Json
.\zto-tools\zed-dev.ps1 status -Json
.\zto-tools\zed-dev.ps1 query world.time -Json
.\zto-tools\zed-dev.ps1 query world.block -Dimension 0 -X -4 -Y 72 -Z 240 -Json
.\zto-tools\zed-dev.ps1 query lunar.snapshot -Json
.\zto-tools\zed-dev.ps1 query lunar.events -WorldId 1 -AfterSequence 0 -Json
```

The block coordinates are examples. Use F3 for a loaded position in a disposable world. The CLI prints text by default, compact JSON with `-Json`, and suppresses normal output with `-Quiet`. Exit codes are 0 for success, 1 for a bridge error and 2 for discovery, connection or local validation errors. After `NOT_READY`, inspect status and retry when ready. Wait at least one second before retrying `RATE_LIMITED`. Retry a transient `BUSY` or `TIMEOUT` with bounded backoff.

## Wire contract

All routes require the discovery bearer token. The CLI pins IPv4 loopback, disables redirects and bypasses proxies.

| Route or query | Args | Reply data |
| --- | --- | --- |
| GET `/v1/health` | Empty body | `alive` |
| GET `/v1/capabilities` | Empty body | Access level, runtime mode, allowlisted operation/schema IDs |
| GET `/v1/status` | Empty body | Mod version, side, readiness state, active dimension/world ID or null, observed world callback counter and queued job count |
| POST `/v1/query` with `world.time` | Empty object | Dimension, raw legacy world time in ticks, zero-based day, time within the 24,000-tick day and rain/thunder flags |
| POST `/v1/query` with `world.block` | `dimension`, `x`, `y`, `z` as signed 32-bit integers | Coordinates and dimension, loaded flag, numeric block ID, metadata and presence of an existing tile entry |
| POST `/v1/query` with `lunar.snapshot` | Empty object | Active bridge world ID, calendar session and dimension, authoritative phase and cycle, raw clock, night and scheduling flags, and latest observer sequence |
| POST `/v1/query` with `assault.snapshot` | Empty object | Player assault state and reason, consumed raw night boundary, world/session, pulse and attempt counts, spawn budget, last rejection and at most six insertion positions |
| POST `/v1/query` with `lunar.events` | Positive `worldId` and nonnegative `afterSequence` as signed 64-bit integers | Up to eight events after the cursor in ascending order, latest sequence, and count of events dropped since the cursor |

Get the current `worldId` from `lunar.snapshot`. Poll with `afterSequence=0` to read all retained events, or start from the snapshot's `latestSequence` to receive only later events. Each event has its kind, cause, before and after positions, and signed boundary deltas. Retained events cover observations after subscription. Use the snapshot as the lifecycle baseline. A reload or dimension replacement starts a new observer stream, even when the lunar save session stays the same. A mismatched world ID or a sequence beyond the active stream returns `STALE_CURSOR`. Use a fresh snapshot after that error or after restarting the client. A missing or invalid lunar datum returns `LUNAR_UNAVAILABLE`.

Queries accept `Content-Type: application/json` and an object with exactly `requestId`, `op` and `args`. Identifiers use one to 64 characters from `[A-Za-z0-9_.-]` without escapes. Field order is immaterial. Responses include `apiVersion`, `requestId` (null for uncorrelated replies), `ok`, and `data` or `error`. Successful game reads also include the callback counter as `tick`. Its value can include callbacks while simulation is paused, and is distinct from the legacy world clock. A stalled client reports `stalled` and can retain the last observed dimension and ID in status until the next frame.

The listener binds `127.0.0.1` and uses four workers, an eight-connection requested OS backlog and zero buffered worker tasks. A connection beyond worker capacity may close without a reply. Header and body limits are 4096 and 1024 bytes. The read deadline is one second. Authenticated requests are limited to 32 per fixed one-second window. The two-slot game queue rejects overflow with `BUSY`; queued queries expire after 750 ms. No user text beyond the validated request ID is reflected. The fixed response schemas bound the reply size. The trusted-workstation threat model assumes local software running as the same user can read the discovery file.

## Verification

Run the isolated transport and dispatch harness from the repository root:

```powershell
.\zto-tools\test-dev-bridge.ps1 -JdkBin 'C:/Program Files/Eclipse Adoptium/jdk-8.0.504.1-hotspot/bin'
```

The harness starts the real listener with a test reader and verifies auth, allowlisting, malformed and oversized input, thread ownership, backpressure, world-ID invalidation, paused state, deadline expiry, rate limiting, discovery cleanup, same-port restart and old-token rejection. Its sample reply is:

```json
{"apiVersion":"1.0","requestId":"check","ok":true,"tick":7,"data":{"worldTime":42}}
```

MCP client compilation, client reobfuscation and packaging via `zto-tools/build-and-deploy.bat` succeeded. The deployed archive passed the 196-entry streaming ZIP verification. The installed Tekkit Dev client reported Minecraft 1.2.5, FML 2.2.106.176 and Forge 3.3.8.164. The declared Forge 3.4.9.171 runtime remains unverified. The first launch without effective flags reached the title menu without a bridge discovery file or a TCP listener in the game process. The enabled launch listened on `127.0.0.1:55118` as observed with `netstat -ano -p tcp`. The discovery ACL had one explicit allow entry.

The CLI returned health, capabilities and menu status. At the menu, `world.time` returned HTTP 503 `NOT_READY`. In a fresh disposable world, two time reads advanced from 330 to 337. The block query returned:

```json
{"apiVersion":"1.0","requestId":"1a1db19a47b6449eb677663b87601147","ok":true,"tick":338,"data":{"dimension":0,"x":-4,"y":72,"z":240,"loaded":true,"blockId":2,"metadata":0,"tilePresent":false}}
```

The same CLI returned `CHUNK_NOT_LOADED` for `(1000000,64,1000000)`, `OUT_OF_BOUNDS` for Y=256, and `INVALID_ARGUMENT` for the wrong dimension. Direct PowerShell HTTP checks against the enabled client returned 401 for missing and wrong tokens, 400 for malformed JSON, 413 for a 1025-byte body and 404 for `POST /v1/command`. Windows PowerShell 5.1 also returned a successful `health`.

For remaining client acceptance, open the Escape menu and confirm `paused` and `NOT_READY`, use Save and Quit and confirm `no_world`, then reopen the same save and compare world IDs. For a dimension transition, repeat the block query with the old and new dimension selectors. Use Quit Game, check that discovery disappears and that the original port stops listening, then restart with the same configured port. Only test multiplayer against a local disposable server and check that status reports `multiplayer` and world reads fail. These checks are separate from the simulated dispatcher transitions and do not cover rendering.

## Follow-up handoffs

Each quoted prompt is standalone for a later implementation task. Use the existing transport and game-thread boundary in `src/minecraft/zed/tekkitoverhaul/devbridge/`. Register capabilities only after the named implementation exists.

### Inventory and containers

> Implement read-only `inventory.player` and loaded-tile `inventory.container` under `src/minecraft/zed/tekkitoverhaul/devbridge/`. Audit `InventoryPlayer`, `Container`, `Slot`, `ItemStack` and `hopper/InventoryAccess.java` against the local MCP tree. Copy bounded slot index, ID, metadata and count on the game thread. Inspect cursor ownership and avoid accessors that create tiles or load chunks. Verify empty, filled, cursor-held and sided inventory cases in Tekkit Dev. Accept when the snapshots match the GUI and a query across an unloaded chunk is refused.

### Machines

> Add a typed machine adapter registry under `devbridge/`. Start with `distillation/TileEntityDistillationBoiler.java`, `TileEntityDistillationHeater.java` and `TileEntityDistillationReceiver.java`. Read the pinned API index under `docs/reference/tekkit-classic-3.1.2/` before extending to IC2, BuildCraft, RedPower or EE2. Confirm units and getter side effects in version-matched artifacts. Publish per-adapter schema and report unknown tiles as unsupported. Test idle, processing, missing power and full output in Tekkit Dev, including absent optional dependencies. Accept when values match the machine panel and no new hard dependency enters the bridge bootstrap.

### Lunar snapshot and observer

> Once a tracked `LunarService` snapshot and transition API exist, use `docs/agents/lunar-cycle-implementation.md` to locate the actual service under `src/minecraft/zed/tekkitoverhaul/lunar/` and add a consumer in `devbridge/`. Use service phase and cycle values. Subscribe and unsubscribe at the service's world ownership boundary. Test phase transitions, time jumps, reloads and portal travel. Accept when the bridge matches the service and each accepted transition produces one observer event.

### Events

> Add a bounded typed ring buffer under `devbridge/` using the world ID from `DevBridge.java`. Implement capped polling by sequence, with wrap detection and dropped-count reporting. Define whether a cursor survives a world change and enforce that contract. Copy immutable values from existing observers on the owner thread. Test empty, wrapped and stale-cursor reads. Accept when polling is ordered, bounded, and cannot return an event scoped to the wrong world.

### Controlled fixtures

> After read-only acceptance, implement one named fixture operation under `devbridge/` for a concrete recurring test. Depend on a separately enabled local disposable-world marker, arena limits, a schema-validated allowlist and existing game-thread dispatch. Add idempotency, audit output, ownership and cleanup. Test denied access on normal worlds, retries, cancellation on unload, and rollback after a partially applied fixture. Accept when only fixture-owned state is modified and repeated requests have no additional effect.

### Scenario runner

> After fixture and event support, add one versioned allowlisted scenario script under `zto-tools/` using `zed-dev.ps1` protocol calls. Start with a lunar boundary if the service is available, or a small machine-cycle test. Capture bounded tick and event evidence, structured assertions and nonzero failure status. Test timeout, pause and reload with cleanup. Accept when consecutive runs start from the same owned fixture state and produce reproducible assertions.

### Optional GUI introspection

> If a repeatable GUI test needs it, audit `Minecraft.currentScreen`, `GuiContainer` and `creative/CreativeInventoryHook.java`, then add a read-only screen snapshot in `devbridge/`. Use the game thread and allowlisted field IDs. Keep rendered pixels and gameplay state separate, and exclude login, chat and arbitrary text fields. Test a known container alongside NEI and a non-container screen. Accept when introspection reports the active screen without changing focus or input.

The [test automation slice](test-automation.md) covers the first inventory and machine adapters, the Hopper fixture and one shell scenario. A general event registry, GUI snapshots, alternate chunk providers, multiplayer and dedicated servers remain outside that slice.
