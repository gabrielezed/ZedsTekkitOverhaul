# ZedsTekkitOverhaul — Agent-Driven Test Automation

**Status:** Proposed implementation specification  
**Target:** Minecraft 1.2.5 / Tekkit Classic 3.1.2 / singleplayer (`Tekkit Dev`)  
**Audience:** Architecture LLM and coding agents with access to the workspace  
**Priority:** Development infrastructure, ahead of general gameplay stabilization  
**Scope:** Opt-in local runtime observability, controlled test actions, repeatable scenarios and machine-readable evidence  
**Out of scope for this milestone:** Dedicated server, multiplayer synchronization, arbitrary code execution, public mod APIs, unattended changes to normal saves

## 1. Mission and definition of success

Turn the existing read-only **ZTO Dev Bridge** into a reliable automated verification environment for agent-assisted development. The goal is to minimize expensive GUI-based computer-use loops and human acceptance tests for deterministic behavior. A coding agent should be able to compile/deploy a candidate, start or reuse the isolated game process, run a named scenario against a disposable world, receive concise structured assertions, and decide whether the change passes.

Do **not** aim to remove human judgment from game feel, aesthetics, fun, visual composition, and emergent balance. Automated evidence must never be represented as proof of graphical quality when no image was inspected.

An example target workflow (illustrative, not currently available):

```powershell
.\zto-tools\zed-dev.ps1 capabilities -Json
.\zto-tools\zed-test.ps1 run lunar.full_moon_transition -Json
.\zto-tools\zed-test.ps1 run hopper.redstone_lock -Json
.\zto-tools\zed-test.ps1 run distillation.single_batch -Json
```

For each run, return `PASS` / `FAIL` / `SKIP` / `ERROR` with actual-vs-expected measurements, reproducible build/world identifiers, a compact trace and an artifact path for details. Failing assertions must cause a nonzero exit status. No result may claim real in-game verification merely because a unit test or mock passed.

## 2. Existing implementation — preserve and extend

The `main` branch already contains:

- `src/minecraft/zed/tekkitoverhaul/devbridge/{DevBridge,BridgeDispatcher,BridgeProtocol,BridgeServer,LunarObserver}.java`.
- `zto-tools/zed-dev.ps1`, `zto-tools/test-dev-bridge.ps1`, and Java transport tests.
- Authenticated loopback HTTP and discovery token gated by **both** `-Dzto.dev=true` and `-Dzto.devBridge.enabled=true`.
- Existing allowlisted reads: `world.time`, `world.block`, `lunar.snapshot`, and `lunar.events`.
- A bounded queue dispatched on the Minecraft client/game thread, no-world/paused/remote-world guards, loaded-chunk checks, response and request size limits, rate limits and lifecycle handling.
- Documentation in `docs/agents/dev-bridge.md` and `docs/agents/DEV_BRIDGE_SPEC.md`.

These are valuable invariants. **Do not replace the existing HTTP server, turn the bridge into an unauthenticated general command interpreter, or discard backward compatibility for existing queries.** Audit the real implementations and class ownership before editing. The currently shipped protocol parser accepts flat numeric query arguments; plan a deliberate versioned schema upgrade for named scenarios and structured assertions, rather than forcing nested JSON into a regex parser unsuited to that purpose. Preserve Java 8 runtime compatibility, the legacy source/target restrictions of the build, and minimal dependencies.

The project's documentation identifies test hooks and cases already available for Hopper, distillation, EMC, crafting, lunar behavior, OBJ and Foundry. **Inventory/Hopper acceptance scripts are useful precedents; they are not a substitute for a general bridge.**

## 3. Design principles

1. **Singleplayer-first:** only the isolated local authoritative world. Refuse remote/multiplayer sessions for mutation. No networking milestone.
2. **Safe by construction:** disabled by default, loopback only, bearer auth, dedicated test-mode flag, disposable-save marker, allowlisted operations, bounded workloads, audit trail.
3. **Observe before acting:** make tests verifiable from actual state; never assume a command succeeded because HTTP returned 200.
4. **Deterministic where feasible:** fixed inputs, known initial state, stable fixture coordinates, bounded world ticks, clear timeouts, no uncontrolled randomness in assertions.
5. **Idempotent and recoverable:** repeat runs without stack duplication, resource leaks, corrupted world state or stale handlers.
6. **Separation of concerns:** mod runtime exposes state and safe atomic actions; host-side scenario runner owns scheduling, assertions, evidence and exit codes.
7. **No production dependencies:** normal gameplay objects must not depend on test-bridge types. Add adapters to the bridge, not bridge calls to production machinery.
8. **Truthful evidence:** distinguish pure logic/unit tests, mocked MCP harness, live in-game runtime, and human visual acceptance.
9. **Token economy:** small concise responses by default, expandable diagnostic payloads on failure. Avoid polling every tick or dumping massive state.
10. **Feature extensibility:** each new gameplay module should expose a small diagnostic adapter and at least one deterministic scenario, without implementing a new HTTP server.

## 4. Threat model and activation

Keep the current gate flags. Add an **independent write gate** that can be enabled only if the read bridge is on, e.g. `-Dzto.devBridge.testing=true`. In addition, require **a per-save marker created explicitly in a disposable test world**, and verify that the active world is local, not remote, and that a unique test-world identity matches the marker. Never mutate ordinary saves, even when testing flag is enabled.

Recommended semantics:

- Read routes available under existing flags, with existing auth.
- Mutation/fixture/scenario-internal operations additionally need test flag + valid disposable world marker + authorization + game-thread ownership.
- By default bind only `127.0.0.1`. Do not expose to LAN, accept dynamic shell commands, arbitrary Java reflection, file paths, class names, NBT scripts, arbitrary coordinates/world edits, or plugins running untrusted code.
- Explicitly cap per-request coordinates, fixture footprints, block/item counts, entity counts, response sizes, active runs, queue length and duration.
- On a world switch, title screen, pause (unless deliberately handled), process stop, or test-world identity mismatch, pending work must be invalidated.
- Each write has an audit record with operation, fixture/run ID, world identity, game tick, resulting status and reason. Never log the bearer secret.
- Refuse to run test actions in the player's ordinary save or on a server. No bypass via unusual dimension transitions.
- `cleanup` must remove only resources tagged/owned by the current fixture, not nearby player structures. Snapshot/restore of arbitrary real-world areas is **not** an acceptable safety mechanism for ordinary saves.

Security note: bearer authentication alone is insufficient protection against accidental world destruction. The disposable-world policy and narrow mutation allowlist are hard requirements.

## 5. Runtime architecture

### 5.1 Components

**BridgeServer / Transport** — Keep current HTTP listener/discovery, auth, bounds and rate limits. Introduce only necessary versioned routes and response capacity. Treat socket threads as untrusted request parsing/serialization workers; no world reads or changes there.

**BridgeDispatcher / GameThreadGate** — All Minecraft state reads and writes execute on the owner's client/game thread. World-instance ID, world lifecycle, paused status, active dimension and stale jobs are revalidated at execution time, not only enqueue time. Operations that cannot finish in one tick become a bounded state machine resumed on future game ticks; never block the render/game thread waiting for ticks.

**Read Adapter Registry** — Typed providers for inventory, TileEntity/machines, loaded entities, calendar, events and active GUI. Providers must inspect only loaded chunks and known compatible classes. Unknown adapters return `UNSUPPORTED`, not guesses.

**FixtureManager** — Owns test fixture lifecycle and namespace. Constructs only named recipes/placements in a dedicated arena/test world, records ownership and cleanup obligations, and provides resettable initial conditions. Prefer fixture-specific factory methods over generic block/world edit APIs.

**ActionRegistry** — Allowlisted, schema-validated actions such as fixture.create, fixture.reset, test.setTime, test.provideInput, test.toggleRedstone. Mutations are checked against fixture ownership, allowed block types, quantity bounds and expected preconditions. No generic `executeCommand`, `eval`, method invocation, or unrestricted spawn API.

**EventCollector** — Bounded event ring with sequence numbers, world/run identity, drop counts and typed event payloads. Subscribe through existing domain-level event APIs when possible; otherwise add minimal, test-only observers on legitimate state transitions. Avoid scanning the world to infer everything.

**Scenario Runner (host-side)** — PowerShell (or a small portable Python script without new game dependencies) discovers the bridge, validates capabilities, creates fixture, executes explicit actions, polls relevant state/events at bounded intervals, evaluates assertions, collects evidence, then performs cleanup. Owns scenario definitions and reproducible output. A scenario should be readable code or a small declarative format, not a general scripting language embedded in Minecraft.

### 5.2 Ownership and lifecycle

Define precisely who owns fixtures, active runs, subscriptions and ephemeral event buffers. All owned handles include `worldId`, `testSessionId`, `fixtureId`, `runId` and schema version where relevant. Every operation must reject a stale or foreign handle. A run interrupted by save/quit or dimension change must report `ABORTED` and must not execute pending writes after entering another save.

### 5.3 Controlled simulation time

Do not change vanilla world time as a substitute for executing machine ticks. `worldTime` may jump under NEI/bed controls and is not identical to elapsed simulation ticks. Add a game-tick counter sampled from the actual world update, distinguishing it from the pre-existing bridge callback count. Support `waitUntil` with a maximum tick budget while the game is running. Avoid busy-spin or sleeping on the game thread.

If accelerated ticking is desired later, prototype it in a separate milestone with explicit world-update and side-effect audits. The first useful MVP can let the real world tick normally and run short scenarios. Where simulation must be accelerated, prefer testing pure domain logic in headless unit harnesses rather than indiscriminately driving Minecraft `World.tick()` recursively.

A paused game must be reported as `PAUSED`/`NOT_READY` with timeouts suspended or failed according to a documented policy. No hidden unpause or global time advancement without a distinct approved test action.

## 6. Read-only observability API (Phase A)

**`inventory.player@1`** — Hotbar, main inventory, armor, held cursor stack where accessible, selected slot. Each stack: item ID, damage/metadata, count, a bounded safe NBT fingerprint if possible, and optional readable name. Strict bounded output. Do not serialize arbitrary NBT or personal account information.

**`inventory.container@1`** — Named fixture/container ID, slots with flags (input/output/fuel), stack identity and count, supported sided exposure when explicitly known. No automatic chunk loads, no mutable accessors from off-thread. Safe distinction between accessible GUI and world TileEntity.

**`machine.inspect@1`** — Typed adapters for ZTO Distillation (boiler/heater/condenser/receiver), Hopper, Alchemical Converter and Foundry, then selected IC2/BuildCraft machines. Every adapter reports `type`, coordinates or fixture ID, loaded state, machine-specific state and units (`EU`, `mB`, items, ticks), current process, progress, failure reason and meaningful inventory/fluids. Unknown machines return `UNSUPPORTED`. Match units to the real code and machine GUI.

**`entity.list@1`** — Bounded entities within a small radius of a named fixture; ID/type, position and meaningful combat state only. No global world enumeration. Required later for siege tests.

**`world.region@1`** — Small, bounded loaded-only volume around fixture; block IDs/metadata, optionally tile presence. Hard caps per query and no chunk generation.

**`events.poll@1`** — Typed bounded ring of domain events with `worldId`, `runId`, sequence, tick, kind and payload. Return dropped-event count, detect stale cursors. Reuse existing lunar observer behavior and avoid overlapping event streams without need.

**`gui.inspect@1` (optional Phase C)** — Read-only active GUI class/identifier, slot layout and visible machine state (if allowed). Never include arbitrary text fields, login data or chat contents. This provides structural UX regression evidence but does not replace a visual screenshot.

Preserve `world.time`, `world.block`, `lunar.snapshot`, and `lunar.events` exactly unless a versioned migration is explicitly documented.

## 7. Controlled test actions (Phase B)

All commands operate only inside a named disposable fixture and are **explicitly not general gameplay cheats**.

Initial allowlist (expand only after end-to-end acceptance):

- `fixture.create@1`: Create a predefined testing fixture within a small fixed test arena (e.g., Hopper facing a chest, distillation assembly, furnace). Returns ownership handles and coordinates.
- `fixture.reset@1`: Restore fixture-owned contents and machine state to a known baseline; idempotent and bounded.
- `fixture.destroy@1`: Remove only owned blocks/entities/temporary state; if full cleanup cannot be proven, mark fixture dirty and require fresh disposable world.
- `test.input@1`: Provide an allowlisted, bounded item/fluid/energy input to a specific test-owned machine using its legitimate entry points where possible.
- `test.redstone@1`: Change only a fixture-owned control lever/signal.
- `test.lunar@1`: Advance or set the **logical lunar state** through the existing authorized lunar service; distinguish an admin transition from a natural one. Where a scenario needs a natural transition, set up the pre-boundary state then let actual world ticks cross it.
- `test.player@1`: Optional controlled position/orientation inside the fixture arena, with collision/safety checks; do not implement until needed for a specific acceptance scenario.

For each action specify schema, preconditions, expected postconditions, permitted fixture types, whether effects are reversible, audit record and maximum work/tick. Use `expectedVersion`/state precondition or idempotency key for retry safety. Never retry an ambiguous non-idempotent action blindly after a timeout.

**Not in Phase B:** arbitrary item give, arbitrary entity spawn, unrestricted block placement, arbitrary command execution, filesystem reading, code evaluation, client input injection, running system commands from the game.

## 8. Scenario definitions and runner (Phase C)

Start with named, versioned, allowlisted scenarios in `zto-tools/scenarios/`. Every scenario defines:

- `id`, `version`, `requires` capabilities, tested gameplay contract and maximum runtime.
- A fixed fixture type and setup inputs.
- Required world/run isolation and seed assumptions, if any.
- Actions or legitimate in-game conditions to create.
- Assertions based on *independently observed post-state* (not merely action acknowledgments).
- Expected event types and sequence/unique-count rules.
- Cleanup and failure reporting.
- Evidence level and limits of the test (logic-only vs in-game state vs visual).

Use one test run at a time initially. Runner CLI should support `list`, `check`, `run <scenario>`, and `report <runId>` (actual verbs can reflect existing PowerShell CLI). Require explicit opt-in for destructive/disposable-world operations. Cache no authorization in generated repository artifacts.

**Result shape (illustrative):**

```json
{
  "schema":"zto.test-result@1",
  "runId":"run-123",
  "scenario":"hopper.redstone_lock@1",
  "status":"PASS",
  "evidenceLevel":"live-singleplayer",
  "build":{"commit":"<actual sha>","modJarSha256":"<actual hash>"},
  "environment":{"minecraft":"1.2.5","pack":"Tekkit Classic 3.1.2","worldId":7},
  "assertions":[
    {"id":"unpowered_moves","pass":true,"actual":4,"expected":4},
    {"id":"powered_blocks","pass":true,"actual":0,"expected":0}
  ],
  "events":{"dropped":0},
  "cleanup":{"status":"complete"},
  "artifacts":{"report":"<actual local path>"}
}
```

Include statuses `PASS`, `FAIL`, `ERROR`, `SKIP`, `ABORTED`, `INCONCLUSIVE`; never report `PASS` on missing events, unsupported capabilities, a paused world, incomplete cleanup when that affects subsequent runs, or an assertion not actually evaluated. `SKIP`/`INCONCLUSIVE` must be conspicuous. A run's provenance should include code/build identity and whether it was executed against the actual Tekkit Dev runtime.

### Suggested exit codes

`0` = complete PASS; `1` = assertion FAIL; `2` = infrastructure ERROR/ABORTED; `3` = prerequisites SKIP/INCONCLUSIVE. Document actual chosen codes and make scripts stable for agents/CI.

### Core assertions

Implement small typed predicates: equality, ranges/tolerances, delta, eventually-within-N-actual-ticks, remains-true-for-N-ticks, event count, no unexpected drops, inventory/energy/fluid conservation. Assertions must include observed values and scope. Avoid opaque text-search assertions over logs when typed state is available.

## 9. First five end-to-end scenarios

Implement them in the order below, adding only the necessary adapters/actions for each.

**S1 `lunar.full_moon_transition@1`** — Create fresh lunar fixture context / use a disposable save at the boundary; observe pre-state; cross the scheduled night boundary through legitimate game ticks; assert phase FULL_MOON, one natural `NIGHT_STARTED` and no duplicate when polling. Distinguish set/reset administrative transitions from natural events. Include save/reload as a separate scenario later.

**S2 `hopper.basic_transfer@1`** — Place a Hopper and chest via a named fixture; seed known item stack; let actual ticks run; assert movement, total-count conservation, absence of extra drops, proper output facing. Reset and repeat without duplication.

**S3 `hopper.redstone_lock@1`** — Toggle fixture-owned redstone; assert no transfer while powered and correct transfer after unpowering. Verify both actual game-tick intervals, not just GUI state.

**S4 `distillation.single_batch@1`** — Assemble a valid machine fixture, insert exactly one legal batch of crude/water and measured EU via supported interfaces; run to completion; assert output type/amount, input consumption, EU decrement, no invalid byproduct and machine status. Choose setpoint and precise quantities from implementation/spec; do not invent a generic recipe. Test incomplete structure and insufficient power as separate follow-ups.

**S5 `crafting.advanced_table@1`** — Prepare a supported 5×5 recipe through the real registered crafting/container mechanisms, verify accepted result and exact ingredient consumption. A bridge-only synthetic recipe result is insufficient. Since GUI/slot callbacks may be necessary, audit how to exercise the actual logic without UI and separately mark the visual UX as unverified.

After these: Alchemical Converter transfer, EE2/IC2 EMC-EU accounting, Hopper↔IC2/BuildCraft, persistence/reload, and eventually lunar siege/spawn behavior. Emphasize cross-system tests: a passing machine unit test alone does not prove integration.

## 10. Build/deploy/restart orchestration (Phase D)

The bridge cannot hot-reload arbitrary Java bytecode from a rebuilt mod jar. If code changes, the agent must compile/reobfuscate, deploy, and restart the game **when required**, using the established scripts and isolated Prism `Tekkit Dev`. Reuse an already-running process only for data-level tests against the same build; never report a new build validated against an old process.

Add a host-side orchestrator that:

1. Records Git SHA/diff state and expected mod JAR hash.
2. Invokes `zto-tools/build-and-deploy.bat` in the existing environment and checks failure status, artifact identity and packaging invariants.
3. Determines whether the running client is on the expected build. If not, requires safe shutdown/restart. Automate launch/shutdown only through known controlled Prism/client mechanisms, avoiding force-kill except in disposable processes with explicit policy.
4. Waits for an authenticated bridge health/readiness state and a suitable test world, with timeout; does not fake world selection by injecting arbitrary input.
5. Runs named scenarios and archives reports.
6. Reports a build mismatch as ERROR, never PASS.

Do not integrate full game-launch automation before a working read/write+scenario MVP; host process orchestration can be a later isolated task. If Prism has no reliable shell-level save selection, use a dedicated disposable test profile with a known fixture save and document the initial one-time bootstrap. No opaque automated clicks as the main path.

## 11. Evidence management and token efficiency

Report artifacts should live outside the tracked repository or in ignored local directories, such as `dist-zto/test-reports/`, with a short on-screen summary. Include build/version, scenario, fixture ID, world identity, actual ticks, assertions, timing, and error classification.

- Default successful response: one compact JSON object plus report path; no verbose logs.
- On failure: targeted diff of state, short recent event tail, validation error and diagnostic hint.
- Provide `--verbose` or `report` for details; hard-cap event/state samples.
- Store screenshots only for narrowly defined visual acceptance; do not poll full-screen images continuously.
- Prefer event-driven `waitUntil` over repeated unbounded queries.
- Track test reliability by repeated clean runs; do not silently retry a genuine failing assertion into PASS.
- Redact discovery tokens and any sensitive environment identifiers from stored output.

## 12. Testing the test infrastructure

### Transport and safety

Retain all existing `test-dev-bridge.ps1` checks. Extend with: missing/invalid write flag, missing world marker, remote world rejection, malformed schema, oversized request, forged handle, stale world ID, duplicate idempotency key, partial fixture failure, unknown fixture/action, unauthorized write, paused world, timeout, cancellation, restart and cleanup.

### Thread ownership

Assert that every world read/write and fixture mutation executes on the owner game thread. Test that socket threads cannot access world objects. Ensure non-blocking long-running scenarios.

### State/conservation

Verify fixture reset/destroy is safe under retry, full inventory, invalid input, chunk unload, world unload and save/reopen. Prove no unexplained gain/loss of stacks, fluids or EU in deterministic cases.

### Repeatability

Run the same scenario at least three times from a clean fixture and once after restarting the disposable world. Compare observations, not only boolean flags. Document nondeterministic tolerances and rare-event exclusions.

### Performance

Check frame/tick responsiveness under idle bridge, event collection and repeated diagnostics. No global scans, unbounded buffers or per-tick serializing large NBT objects. Benchmark on actual Tekkit Dev.

### Backward compatibility

Existing read operations and scripts work unchanged. With bridge disabled, gameplay has no extra socket listener, test mutations, visible UI or intrusive tick costs. Test launch without flags.

### Visual boundary

Use occasional manual or screenshot-based acceptance for textures, animation, GUI interaction, spatial presentation and "fun". `gui.inspect` only proves GUI structure and selected state; it cannot certify correct colors, scale, clipping, readability or atmosphere.

## 13. Milestones and acceptance gates

**A — Observability:** `inventory.player`, `inventory.container`, initial machine adapters, robust event stream. Gate: can inspect an actual live Hopper and distillation apparatus, with matching values and no side effects. Existing bridge remains stable.

**B — Controlled fixtures:** isolated-world marker, single fixture for Hopper, input/reset/destroy and audit. Gate: write denial in normal worlds and three safe repeat cycles in the disposable save.

**C — Executable scenarios:** host runner, typed assertions, lunar and Hopper scenarios, failure classification, evidence artifact. Gate: an agent can run a scenario from a shell, see a meaningful nonzero exit code on forced failure and obtain a verifiable PASS on the real client.

**D — Industrial integration:** distillation/IC2/BC/EMC scenarios and conservation accounting. Gate: first complete multiblock process verified without Computer Use, including state before/after and energy/fluid quantities.

**E — Agent workflow:** reliable build identification, deploy/restart/ready orchestration and regression suite. Gate: agent-driven code/test loop with no GUI for covered logic scenarios. No need to solve all graphical testing first.

**F — Later:** combat/siege spatial fixtures, mob AI/event probes, optional GUI structural introspection, limited visual capture, deterministic replay and multiplayer adaptation. None are prerequisites for A–E.

## 14. Definition of done for the first usable release

- [ ] Read-only APIs report real game state for at least one Hopper and one ZTO machine.
- [ ] A mutation endpoint exists only behind dual flags, authenticated loopback and disposable-save marker.
- [ ] One named fixture can be created, reset and destroyed without duplication or affecting unrelated blocks.
- [ ] One actual in-game scenario runs start-to-finish from shell with independently measured assertions and machine-readable exit code.
- [ ] Event stream has bounded sequence cursors, loss indication and lifecycle invalidation.
- [ ] Scenarios cannot succeed against a stale client/mod build without an explicit provenance warning or failure.
- [ ] The old read-only bridge functions without changes and disabled mode stays inert.
- [ ] At least three repeated disposable-world runs succeed with documented runtime evidence.
- [ ] Human testing is explicitly reserved for visual/experiential cases, not required for covered logic scenarios.
- [ ] Documentation describes extending the bridge for the next feature in a small adapter + scenario, with no transport changes.

## 15. Architecture non-goals and cautions

Avoid an agent-driven "god console", generic NBT patcher, unrestricted custom command executor, arbitrary state graph reflection, dynamic dependency injection, generic world editor and parallel scenario execution in the first release. These increase attack surface and nondeterminism without proving immediate value.

Do not conflate a local game integration test with proof of multiplayer correctness. Do not conflate a headless unit test with a test inside Tekkit Dev. Do not conflate an HTTP action result with verified game effects. Do not conflate an unverified visual snapshot with a correct rendering.

**North star:** A new feature should ship together with a compact state adapter and a repeatable disposable-world scenario. The agent should be able to prove its core behavior from a terminal, rather than repeatedly driving Minecraft by screenshots.
