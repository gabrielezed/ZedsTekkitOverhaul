# ZedsTekkitOverhaul — In-Game Development Bridge

**Document type:** Architecture and functional specification
**Status:** Proposed design; not an implementation report
**Target:** Minecraft Java Edition 1.2.5 / Tekkit Classic / ZedsTekkitOverhaul
**Audience:** Technical lead LLM, implementation agents, test agents
**Repository location** `docs/agents/DEV_BRIDGE_SPEC.md`
**Related document:** `PROJECT_VISION.md`

## 1. Mission

Build a **development-only local control and observability bridge** for the running Minecraft instance so coding agents can validate changes from a shell, scripted tests, and concise structured tool output instead of repeatedly using screenshot-driven computer interaction.

The bridge must support **observe → arrange test state → exercise runtime behavior → assert results → collect evidence**. It must be cheap in tokens, deterministic where practical, safe against accidental inventory/world corruption, and compatible with the legacy modded runtime.

The bridge is **not** a player-facing gameplay feature, public remote admin API, universal mod-control framework, or replacement for unit/integration tests. It must not change the modpack's design, economy, progression, or multiplayer behavior when disabled.

### Goals

1. Read runtime state: world/time, blocks, entities, inventories, machines, lunar state, diagnostics.
2. Set up restricted test fixtures: time, test position, selected blocks and inventories, and carefully scoped entity spawning, if implemented safely.
3. Observe behavior through structured event capture and bounded queries.
4. Run repeatable test scenarios with machine-readable pass/fail results.
5. Minimize screenshots and GUI automation; reserve them for visual validation and unexposed UI behavior.
6. Be *disabled by default*, easy to enable deliberately in local development, and **never exposed to the LAN by default**.
7. Remain extensible through domain adapters, not bespoke one-off command endpoints.

### Non-goals for v1

- Autonomous interaction with all Minecraft GUI controls.
- Generic arbitrary Java reflection or class execution.
- Execution of shell commands by Minecraft.
- Generic expression evaluation / scripting inside the game.
- Remote internet-accessible administration.
- A full headless Minecraft replacement; rendering and some input paths still need the real client.
- Global command permission escalation in multiplayer.
- Whole-world export or unrestricted scanning.

## 2. Mandatory repository/runtime audit

Before implementation, inspect the **actual workspace**, decompiled Minecraft/Forge/ModLoader/legacy Tekkit sources, existing ZedsTekkitOverhaul code, launching scripts, and build/test conventions. Do not assume modern Forge commands, events, capabilities, logical sides, registries, or threading semantics exist.

Identify:

- Actual singleplayer/integrated-server vs client-hosted/server-side architecture in 1.2.5 and the exact launch mode supported by the repo.
- Server tick and client tick entry points; thread ownership of world/entity/container mutations.
- World and player lifecycle callbacks; save/load and dimension switching.
- Existing commands, network channel, and event hooks.
- Source of truth for world time, inventory, tiles, loaded chunks, and mod machines.
- Available JSON/HTTP libraries compatible with the project's Java runtime and classpath.
- Whether headless/dedicated-server execution is actually supported by the modpack.
- Current lunar API or other future hooks (do not assume already implemented).

**Deliver an audit table**: existing primitive, file/class reference, side/thread, risk, planned adapter. Unverified compatibility claims are forbidden.

## 3. Recommended architecture

```
Agent shell / test script
        |
        v
zed-dev CLI (small Python stdlib or existing repo-compatible script)
        |
        v
127.0.0.1:<ephemeral-or-configured-port>  HTTP/JSON
        |
        v
DevBridge Transport (non-game, background I/O thread)
        |
        v
Command Validation / Auth / Rate & Size Limits
        |
        v
Main-Thread Job Queue  --->  Job status / bounded results
        |
        v
Runtime Adapters (core, world, entity, inventory, machine, lunar, events)
        |
        v
Authoritative game state in loaded world
```

**Design invariant:** HTTP handling threads MUST NEVER directly access or mutate mutable Minecraft world state. Enqueue game operations onto the appropriate authoritative game thread, then snapshot/copy and return limited, immutable result data. Never block the game thread waiting for HTTP processing.

Prefer a **minimal loopback-only JSON HTTP bridge** plus shell CLI wrapper. Plain command-line JSON output is ideal for agents; no dependency on browser orchestration or computer-use tooling.

If the legacy runtime cannot safely embed a listener, fallback design: **bounded command/result files** in a restricted local directory, polled on ticks, with atomic rename and explicit IDs. This is second choice because polling, file cleanup, and race handling are less ergonomic. Do not implement both transports in v1.

### Components

- `DevBridgeBootstrap`: initializes only with deliberate local configuration.
- `DevBridgeServer`: loopback socket, auth, parsing, schema validation and response limits.
- `DevBridgeDispatcher`: thread-safe bounded request queue, deadline and job tracking.
- `DevBridgeRegistry`: named commands/adapters with metadata, capabilities and versioning.
- `DevBridgeContext`: world/player/dimension selectors and access checks.
- `DevBridgeEvents`: bounded ring buffer for diagnostic events and sequence-based retrieval.
- `DevBridgeFixtureService`: optional controlled state setup and cleanup.
- `DevBridgeScenarioRunner`: later scripted test orchestration on the service layer.
- `zed-dev` CLI: reliable human-readable and JSON output, exit codes, timeouts.

Names are conceptual; reuse existing project architecture.

## 4. Activation, safety, and isolation

### Configuration

- Disabled by default; no listener bound and no command registration when disabled.
- Explicit local `devBridge.enabled=true` plus explicit development/environment gate. Prefer a dev launch profile or JVM flag rather than enabling from an untrusted in-game menu alone.
- Bind to **127.0.0.1 only**, never `0.0.0.0`, no UPnP, no automatic port forwarding.
- Port configurable; allow OS-assigned ephemeral port and write its effective value to a local runtime discovery file.
- Per-process random high-entropy bearer token generated at startup, stored in a local file with restrictive filesystem permissions when possible; do not print in general logs or embed in screenshots.
- Strongly recommend refusing to start the bridge on a remote/dedicated production server or any nonlocal multiplayer session. If external LAN clients can connect to the game, default to **read-only mode or refuse mutating methods**.
- Separate capability levels: `read`, `test_write`, `destructive_fixture`. Initial default after opt-in is `read`; explicit second opt-in is required for mutations.
- Fail closed if auth/config is ambiguous. Non-dev builds should not package the server listener when build separation is feasible.

### Request protection

- Authenticate every request except (optionally) an extremely limited unauthenticated `/health` endpoint with no world details.
- Hard limits on body size, response size, batch size, requested area, number of entities, number of jobs and outstanding requests.
- Max per-request execution budget; queue backpressure and `429/503` behavior.
- Reject commands against unloaded chunks by default. Never force-load arbitrary chunks.
- Reject remote paths, URLs, arbitrary class/method identifiers, process commands, reflection, and file writes by API request.
- Never expose credentials, player private data, full local paths, or external account information in responses.
- Audit mutations with request ID, command, target, outcome and game tick; no secret logging.
- Dev-use warning when enabled; disabling shuts down listener and drops pending mutation jobs safely.

**Threat model:** local programs can generally access a loopback listener; bearer authentication and opt-in gates still matter. Loopback binding alone is not authentication. This tool is meant for a trusted developer workstation, not untrusted code or shared remote hosting.

## 5. Protocol and response contract

Protocol: versioned HTTP/JSON, ASCII-safe command identifiers and bounded structured responses. Do **not** expose arbitrary Java object serialization.

Example endpoints:

| Endpoint | Verb | Purpose | Access |
|---|---|---|---|
| `/v1/health` | GET | Process/bridge ready (no sensitive world state) | minimal |
| `/v1/capabilities` | GET | Supported adapters, commands, versions | read |
| `/v1/status` | GET | Session, world loaded, logical side, tick, dimensions | read |
| `/v1/query` | POST | Typed read-only query | read |
| `/v1/command` | POST | Controlled mutation or dev action | test_write |
| `/v1/jobs/{id}` | GET | Pending/completed/failed job | read |
| `/v1/events?after=<seq>&limit=<n>` | GET | Read bounded event stream | read |
| `/v1/scenarios/run` | POST | Run registered test scenario (later milestone) | test_write |

Illustrative request:

```json
{
  "requestId": "test-0042",
  "op": "world.getBlock",
  "args": {"dimension": 0, "x": 100, "y": 64, "z": 120}
}
```

Illustrative response:

```json
{
  "apiVersion": "1.0",
  "requestId": "test-0042",
  "ok": true,
  "tick": 120845,
  "data": {
    "loaded": true,
    "blockId": 54,
    "metadata": 0,
    "name": "minecraft:chest"
  }
}
```

Namespaced IDs are illustrative: 1.2.5 uses legacy IDs and metadata, and human-readable names may need an adapter or may be omitted if not reliably available.

All responses must contain stable `apiVersion`, request correlation ID, success state, tick if relevant, and either bounded `data` or typed error. Errors: `NOT_READY`, `UNAUTHORIZED`, `DISABLED`, `UNSUPPORTED`, `INVALID_ARGUMENT`, `OUT_OF_BOUNDS`, `CHUNK_NOT_LOADED`, `TIMEOUT`, `CONFLICT`, `CAPABILITY_DENIED`, `RATE_LIMITED`, `INTERNAL_ERROR`.

Long-running mutations should return `202` and a job ID, with polling. Simple read-only operations may use synchronous bounded waits on a queued tick job. Ensure bounded timeout if paused or world not ticking.

**Idempotency:** mutation requests must accept an `idempotencyKey`; retry must not execute the same destructive action twice during the retention window. Maintain a bounded deduplication cache. Distinguish accepted from completed.

**Response compactness:** return only requested fields; support `fields`, `limit`, `cursor` where appropriate; summarize large NBT structures rather than dumping everything. Provide `--json` and concise text formats in CLI. Add `--quiet` for pass/fail test automation.

## 6. Read-only observability API (MVP)

### CORE

- `core.status`: build/version, mod loaded, bridge version, world readiness, side, tick, queue depth (no secrets).
- `core.capabilities`: registered operations and schema IDs.
- `core.recentErrors`: bounded recent bridge/game errors; no arbitrary log file reads.

### WORLD

- `world.time`: total and day time, logical day, weather and dimension where supported.
- `world.block`: block ID, metadata, tile presence, selected tile summary at coordinate, loaded-state validation.
- `world.region`: **small bounded** rectangular area, optional compact palette encoding, never full-world export.
- `world.players`: local test player(s), dimension, coordinates, health, selected slot, with privacy minimized.
- `world.gamerules` or closest supported legacy equivalent, if any; audit before exposing.

### ENTITIES

- `entity.nearby`: bounded nearby entities with class/type, position, health, AI state only if reliable.
- `entity.inspect`: targeted entity ID in loaded world; bounded properties.
- `entity.count`: counts by type in radius/loaded area, no world-wide chunk loading.

### INVENTORY

- `inventory.player`: slot index, legacy item ID, metadata, count, selected relevant NBT fields.
- `inventory.container`: permitted loaded tile/container by position, logical slot groups, contents and capacities.
- `inventory.diff`: optional compare snapshots in a bounded test session.

### MACHINES / MOD ADAPTERS

- `machine.inspect`: normalized diagnostics, with adapter-specific fields: input, output, working state, progress, power type, stored energy, voltage/input constraints, fluid information.
- `machine.recipe`: recipe/operation identity if resolvable.
- `machine.adapters`: supported machine families and precise capability matrix.

Machine inspection must not read private mutable mod structures off-thread. For unknown machine types return typed `UNSUPPORTED` or a limited tile summary, rather than guessing energy units or modifying state.

### LUNAR / EVENT ADAPTERS

- `lunar.snapshot`: current phase, logical day/cycle, next transition, only **if** LunarService exists.
- `events.read`: recent events, sequence numbers, tick and compact typed payload.
- `events.filter`: bounded typed filters by subsystem, e.g. `lunar.*`, `machine.*`, `spawn.*`.

Adapters for planned but absent features must be **not registered**, rather than returning fictional values.

## 7. Controlled commands (post-MVP, capability-gated)

Introduce only after read-only bridge is stable. Each command has explicit schema, bounds, dimension/world validation, permission gate, mutation audit, and cleanup behavior.

### WORLD / FIXTURE

- `test.setTime` or `test.advanceTime`: local test-world time control. Explicit semantics for phase changes and sleep bypass; do not silently change other worlds.
- `test.teleportPlayer`: selected local test player, destination within safe loaded terrain or validated target.
- `test.setBlock`: only inside a configured **test arena** or explicitly marked fixture area; fixed allowed block IDs or whitelisted registry; no unrestricted mass replacement.
- `test.placeFixture`: named, versioned fixture that assembles a known small test environment.
- `test.restoreFixture`: restore snapshot or remove fixture-created objects, with clear ownership.
- `test.setInventorySlot`: explicitly allowed test player/fixture container; exact item ID/metadata/count and optional validated NBT schema; never generic arbitrary NBT injection.
- `test.spawnEntity`: whitelist, bounded count, loaded/validated positions, tag spawned entities for cleanup. No implicit malicious/unbounded AI spawning.
- `test.clearSpawnedEntities`: remove only tagged test-owned entities.

Do **not** implement destructive commands in the base MVP by default. Implement only as required by a concrete feature test.

### TICK AND WAITING

- `test.waitTicks`: asynchronous job waiting for game ticks; must time out when paused, unloaded or not ticking.
- `test.waitUntil`: *registered predicates only* (e.g., machine output count, phase, event occurrence), bounded by max ticks and wall-clock timeout.
- `test.getTickRate`: diagnostic only; do not assume timing remains exactly 20 TPS.

Controlled single-step or accelerated tick simulation is **not MVP**. Changing the game loop under Minecraft 1.2.5 and modded environments may break determinism; test feasibility separately.

### PLAYER AND GUI

- Player inventory and selected hotbar item can be inspected in MVP.
- Simulated player clicks and full container UI transaction automation are **not MVP**. They require faithful server-container semantics and actual client behavior validation.
- Optional later `ui.inspectOpenContainer` can expose slot layout, enabled buttons, text fields and client-side values if a client adapter exists.
- An agent may still need brief computer use for visual alignment, mouse interaction, rendering regressions and unautomatable GUI events.

## 8. Events and targeted instrumentation

Create a small **bounded ring buffer**, not a noisy global logging layer.

Example event envelope:

```json
{
  "seq": 291,
  "tick": 120900,
  "type": "lunar.phaseChanged",
  "source": "LunarService",
  "data": {"from": "WAXING_GIBBOUS", "to": "FULL_MOON", "cycle": 2}
}
```

Features can register development event adapters without depending on the bridge. Expose only stable event payloads, not raw object references. Allow `afterSeq` polling and return dropped-event counts if the buffer wraps. Do not inject dev-only dependencies into normal gameplay modules; prefer optional registration of feature events or a thin observer interface.

Useful future event types: `machine.workStarted`, `machine.workCompleted`, `machine.outputBlocked`, `inventory.changed` (scoped), `entity.spawned` (scoped), `entity.died` (scoped), `siege.started`, `siege.waveStarted`, `siege.completed`, `lunar.phaseChanged`, `lunar.nightStarted`.

## 9. Scenario runner: repeatable agent-driven tests

Second milestone after read-only and controlled fixtures. Define a **whitelisted test scenario registry**, each with parameters, setup, execution, observations, assertions, cleanup. Avoid generic code execution.

Scenario lifecycle:

1. Validate bridge and feature capabilities.
2. Require development test-world/arena and write permission.
3. Set up tagged fixture through approved commands.
4. Record starting tick and event sequence.
5. Trigger approved test action or wait for a registered condition.
6. Read target state and assert expected values.
7. Return structured result and compact evidence.
8. Clean up owned entities/blocks, even on failure when possible.

Result example:

```json
{
  "scenario": "lunar.full_moon_transition",
  "ok": true,
  "elapsedTicks": 4,
  "assertions": [
    {"name": "phase_equals_full_moon", "pass": true},
    {"name": "event_emitted_exactly_once", "pass": true}
  ],
  "evidence": {"eventSeqStart": 41, "eventSeqEnd": 42}
}
```

### Priority scenarios

- Lunar: eight phase order, full moon, time skip, restart, multiplayer consistency.
- Crafting: recipe transfer and output conservation, 5×5 grid, NBT-preserving transactions.
- Industrial: input/output and energy transfer, halted output full, EMC/EU conversion conservation.
- Mob/assault later: bounded spawn counts, phase-based policies, cleanup, no unintended block destruction.

Rendering pixels and UX layout cannot be asserted reliably via server snapshots alone. Use a separate small number of deliberate visual tests.

## 10. Shell/CLI developer experience

Use a lightweight CLI wrapper compatible with the agent's shell environment. Python standard library (`argparse`, `json`, `urllib.request`) is reasonable **outside** the game process; Java-side dependencies must be vetted against legacy runtime. The CLI should discover the session/port/token from explicitly configured local runtime files and avoid printing secrets.

Proposed UX (illustrative, not implemented):

```bash
zed-dev status --json
zed-dev query world.time --json
zed-dev query world.block --x 10 --y 64 --z 10 --json
zed-dev query machine.inspect --x 20 --y 65 --z 12 --json
zed-dev events --after 120 --type lunar.phaseChanged --limit 10 --json
zed-dev command test.advanceTime --days 4 --json
zed-dev scenario lunar.full_moon_transition --json
```

- Commands exit `0` on success, nonzero on error or assertion failure.
- Errors contain a short message, stable machine-readable code and remediation hint if useful.
- No entire log streams, giant JSON dumps or screenshot attachments by default.
- Optional `--select`/`--fields` to output only required values; avoid making the model parse megabytes.
- Provide documented retry/backoff for temporary `NOT_READY` and `TIMEOUT` without blindly retrying non-idempotent mutations.
- Provide shell-friendly examples in repository docs. Do not require network access beyond localhost.

## 11. World lifecycle and robust execution

- Start the listener only when explicitly enabled. Distinguish `process alive` from `world ready`.
- On world load, assign session/world identifiers. On unload, cancel or fail pending jobs scoped to that world.
- On dimension changes, validate request targets and avoid keeping stale world/player references.
- On process shutdown, close listener, release port, invalidate session token, and clean discovery files when possible.
- If game paused, queries that require ticking may time out with clear `NOT_READY` / `WORLD_PAUSED`-equivalent error.
- World may be reloaded without restarting process; the bridge should not leak listeners, sockets, snapshots or queued jobs.
- Prevent results from one world/session being applied to another (session and world IDs in job scope).
- Multiplayer/client distinction: run authoritative world mutations on server side, even when testing from a client instance.
- If client can render but server cannot expose a feature, show it in capabilities as unavailable rather than claiming unsupported commands succeeded.

## 12. Testing and acceptance

### Security / isolation

- Disabled: port closed; no running bridge thread; no runtime feature changes.
- Enabled with no token: all privileged endpoints refused.
- Listener only binds loopback; attempts to configure external address are rejected.
- Read capability cannot mutate state.
- Test-write capability blocked outside configured test world/arena.
- Oversized requests, unknown ops, malformed JSON, invalid dimension and unloaded chunks produce typed errors.
- Repeated mutating request with same idempotency key produces no duplicate effect.
- Sensitive secrets are absent from logs.

### Runtime / correctness

- `status` works when runtime ready and reports clear not-ready when no world loaded.
- `world.time`, `world.block`, `inventory.player` snapshots match in-game state.
- Queries/mutations never touch game state directly on HTTP thread.
- Server restart, world unload/reload and dimension switch do not hang or leak.
- Bounded queue and deadlines prevent deadlock when ticking stops.
- Multiplayer cannot be mutated without explicit trusted local test configuration.
- Registered event read returns stable sequence and dropped-event count.
- Performance impact when bridge disabled is effectively zero; when enabled and idle negligible.

### Developer outcomes

- Agent can run a lunar state validation from shell without opening GUI.
- Agent can check a machine's input/output and error state from JSON when its adapter exists.
- The same test can be repeated without stale state from prior fixtures.
- Logs/results are compact enough to include in PR summaries and agent feedback.
- Visual checks remain possible via selective traditional client use.

## 13. Delivery plan

**Phase 0 — Audit and ADR.** Confirm actual client/server model, tick hooks, JSON and HTTP feasibility, security gates, and narrow MVP; record decisions.

**Phase 1 — Read-only bridge.** Dev opt-in, localhost HTTP, token, bounded queue, `health`, `capabilities`, `status`, `world.time`, selected world/player/block queries, CLI, typed errors. Test lifecycle, disabled state and auth.

**Phase 2 — Observability adapters.** Inventory, entities, machine-specific adapters, event ring buffer, optional LunarService adapter. Only registered/implemented capabilities appear.

**Phase 3 — Controlled test operations.** Local test-world restrictions, time changes, safe fixtures, whitelisted state changes, async jobs, ownership and cleanup. Begin with the minimal commands needed for an actual recurring test.

**Phase 4 — Scenario runner.** Named, versioned scenarios, assertions, evidence, cleanup and CI/dev scripts. Lunar feature test as first vertical example if implemented.

**Phase 5 — Optional client GUI introspection.** Only if actual UI tests justify it. Keep graphics validation separate from authoritative gameplay tests.

Do not deliver all phases in one giant patch. Each phase must be individually testable and should not regress old worlds or normal gameplay.

## 14. Implementation agent deliverables

1. Repo-specific audit and architecture decision record, including exact hooks/paths.
2. Interface contracts and command registry schema.
3. Working local dev bridge MVP with strict opt-in and auth.
4. Shell CLI and documentation.
5. Automated tests appropriate to the available legacy runtime; manual test procedure for client-only cases.
6. Test evidence: build output, sample JSON, lifecycle checks, and negative/security tests.
7. Explicit list of deferred work and unsupported adapters.
8. Short note explaining consistency with `PROJECT_VISION.md`: dev bridge exists for development efficiency and does not alter player experience.

## 15. Non-negotiable design rules

- **No bridge effect when disabled.**
- **No open network listener.** Local authenticated access only.
- **No arbitrary code execution or reflection API.**
- **No world mutation off the authoritative game thread.**
- **No unbounded queries, result dumps, or entity generation.**
- **No fictitious capability claims.**
- **No hard dependency from player-facing features back to the dev bridge.**
- **No assumption that client graphics can be completely replaced by server queries.**
- **No monolithic endpoint specialized exclusively for one feature.**

The desired outcome is a small, maintainable **test instrument**, not an alternate gameplay engine.
