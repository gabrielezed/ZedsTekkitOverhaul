# Lunar Cycle System implementation handoff

Status: implemented with partial single-player runtime verification. The [original lunar specification](lunar-cycle-system.md) supplies the requirements. This handoff records the legacy integration decisions and acceptance criteria. Read [AGENTS.md](../../AGENTS.md), [Project Vision §7 and §14](../../PROJECT_VISION.md), and the [MDK environment](environment.md) before editing code.

The sky should give players a recurring, readable signal for preparation. The milestone delivers that signal and a calendar that future directors can query. Encounter selection, timing balance beyond the eight-day baseline, sleep restrictions, spawn changes, new mobs, and ThreatLevel remain future work. Dedicated server wiring, packets, multiplayer commands, and multiplayer acceptance are deferred. Keep the authoritative service independent of the client so a later server adapter can own it.

## 1. Technical audit

| Area | Observed in the workspace | Implementation consequence |
| --- | --- | --- |
| Clock | `src/minecraft/net/minecraft/src/WorldInfo.java` persists one `long worldTime` as `Time`. `World.getWorldTime()` reads it, and the provider uses it for the celestial angle and moon. The inspected client has no separate total-time clock. `World.tick()` also uses this clock for scheduled updates and spawn cadence. | Sample the existing clock. Derive day and time of day separately. Do not repurpose or overwrite vanilla time for calendar controls. |
| Sleep and saves | `World.tick()` moves a fully sleeping local world to the next day boundary and then applies its ordinary increment. Autosave can run before that increment. `World.saveLevel()` writes world info before saving dirty map data. | Classify observed discontinuities and evaluate the destination directly. Test real beds and reloads at boundaries. Avoid claims of crash-atomic saves or exactly-once external gameplay effects. |
| Creation and dimensions | The local `World(ISaveHandler, String, WorldSettings, WorldProvider)` constructor sets `isNewWorld` from missing world info. `Minecraft.changeWorld` saves the outgoing world, installs the incoming world and can save a new world before the first tick. `Minecraft.usePortal` constructs another `World` first. The copy constructor shares the save handler, copies `WorldInfo`, and constructs a separate `MapStorage`. `WorldProvider.worldType` is the local dimension ID. | Use the active save as the ownership boundary. Preserve one calendar across portals. Avoid loading a second mutable lunar state from disk during construction of the incoming dimension. |
| Save API | `World.loadItemData` / `setItemData` delegate to `MapStorage`. The latter requires a public `WorldSavedData(String)` constructor, uses dirty flags, and stores map data through the save handler in the world's `data` directory. `MinecraftForge.registerSaveHandler(ISaveEventHandler)` exists locally. Forge's save notification runs after map data has already been written. | Use a namespaced `WorldSavedData` entry. Creation and save attachment must respect the actual call order. Confirm exact Forge declarations in matching artifacts before using the callback. |
| Tick and input | `Minecraft.runTick` calls `FMLClientHandler.onPreWorldTick/onPostWorldTick`. The latter dispatches `TickType.WORLD` while a world exists, including frames where the paused game can skip `World.tick()`. ZTO's `CreativeInventoryHook` uses `FMLCommonHandler.registerTickHandler`, `ModLoader.registerKey` and a bootstrap `keyboardEvent` delegate. The bootstrap's current in-game hook is also used by EMC and hopper diagnostics. | Use a dedicated legacy world tick-end adapter and verify scheduling against the packaged FML version. Keep existing EMC and GUI callbacks intact. Register the debug key through the established route. |
| Vanilla phase and renderer | `World.getMoonPhase(float)` calls `WorldProvider.getMoonPhase(long,float)`, which uses `(int)(time / 24000L) % 8`. `RenderGlobal.renderSky(float)` binds `/terrain/moon_phases.png` and uses a 4×2 UV grid. Local `jars/bin/minecraft.jar` contains the 128×64 atlas. The local Tekkit core decompilation `mods/coremods/l.java` also calls `xd.d(F)I` at the atlas selection. MCP mappings identify `l` as `RenderGlobal` and `xd` as `World`. | Reuse the native renderer and texture-pack lookup. Check the actual loaded renderer class and method bytecode before adding a phase-selection hook. |
| Time controls and existing changes | The inspected ZTO sources contain no writes to the world clock or lunar sky override. The local NEI `NEIUtils.setHourForward` computes the next day's requested hour, and its local `setTime` writes through `WorldInfo`, bypassing `World.setWorldTime`. No `WorldServer` or `CommandTime` source is present in this MDK tree. | Observing only the `World.setWorldTime` setter would miss sleep and NEI. Test the actual installed NEI time controls in Tekkit Dev. Do not assume modern single-player commands or an integrated server. |
| Remote time | `WorldClient.tick` increments local time and `NetClientHandler.handleUpdateTime` applies the single `long` in `Packet4UpdateTime`. `NetClientHandler` sets `isRemote` after constructing `WorldClient`. | Guard local ownership by checking the active world and rejecting `WorldClient` as well as remote worlds. Vanilla time packets do not convey a ZTO calendar offset or transition reason. |

The audit used the local MCP tree, local atlas, SRG mappings, ZTO sources and reconstructed Tekkit overlay. Runtime class precedence, bytecode fingerprints and in-game behavior still require verification in Tekkit Dev. See the [version-pinned reference](../reference/tekkit-classic-3.1.2/README.md).

## 2. Gap analysis

| Specification | Available today | Milestone disposition |
| --- | --- | --- |
| LUNAR-01–03 | Vanilla supplies eight atlas cells and a mutable clock. | Add stable API phases, persistent day adjustment and deterministic queries. Use 24,000 ticks per day and eight days per cycle. |
| LUNAR-04–07, STATE-01/04 | Sleep, level saves, map data and dimension transitions exist. | Add a time observation policy, migration, replay protection and per-save ownership. Test new and legacy saves and portal travel. |
| RENDER-01–05 | Vanilla supplies the moon quad, UV lookup, texture packs and sky effects. | Adapt only the chosen atlas cell in supported local Overworlds. Verify rendering, pack behavior and unchanged lighting. |
| STATE-02/03 | The single-player world is locally simulated. Remote clients receive vanilla time. | Add a local authority adapter and a read-only snapshot boundary. Defer dedicated authority registration and network synchronization. |
| API-01–05 | There are legacy Forge/FML callbacks but no ZTO lunar service or listener API. | Add a narrow service and synchronous, session-scoped events. |
| SIEGE-API-01–05, PROG-01/02 | No lunar scheduling contract exists. | Add window/policy and cycle queries. Defer activity profiles, special calendars, threat calculation and directors. |
| DEBUG-01–04 | Legacy keybinding and GUI patterns exist. | Add an opt-in in-game panel, delegated calendar controls and a diagnostic listener. Administrative slash commands can wait. |
| PERF-01–03 | The native clock is cheap to read. | Constant-time calendar updates and skip classification, bounded diagnostics, no entity or chunk scanning. Isolate listener failures and surface storage or render-hook issues. |

## 3. Architecture decisions and semantics

### Time and migration

- Let `rawDay = floorDiv(worldTime, 24000)`, `timeOfDay = floorMod(worldTime, 24000)`, and `logicalDay = rawDay + dayAdjustment`. Derive `dayInCycle` with floor modulo and `cycleIndex` with floor division by 8. Expose completed calendar cycles as `max(0, cycleIndex)`. These values describe calendar position and can move after a correction. Consumers needing a monotonic progression counter must own it.
- A confirmed new save starts at logical day 0 with `NEW_MOON`. A legacy save with valid nonnegative vanilla time starts with adjustment +4, preserving its existing vanilla cell. The initial legacy cycle index is therefore a calendar position, including prior world age and alignment, rather than a history of observed cycle events. For unusual saved times or atlas results, validate the input and choose a documented fallback without rewriting the vanilla clock. Use bounded checked arithmetic for admin operations.
- Track the persisted initial adjustment separately from the active adjustment so reset restores the original alignment at the current vanilla time. Day-only changes use the lunar adjustment. A same-day hour edit has no effect on the phase. An external edit that changes the raw day changes the lunar day too, including NEI's next-day hour buttons.
- Define the scheduling night as `[13000, 23000)` in vanilla ticks, with the boundaries centralized in the calendar. This is the milestone's deterministic event window, independent of weather, brightness and bed eligibility. Countdown to the next phase is `24000 - timeOfDay`. There is no independent monotonic tick clock in this milestone.

### Authority, persistence and lifecycle

- Keep pure calendar math, transition logic and API types under `zed.tekkitoverhaul.lunar`. An MDK adapter supplies the clock, save ownership and active dimension. Only the owning game thread may update the service, dispatch events or modify save data. The local adapter runs on the single-player game thread even though it is bootstrapped from the client. Render and GUI code consume published immutable snapshots. No client classes in the core.
- Initialize a missing entry once using explicit creation evidence, never `Time == 0` as the new-world test. Verify an early local creation path using `isNewWorld` before the first vanilla save, for example the legacy Forge load callback. Guard out `WorldClient` explicitly at construction. Attach a fresh datum there only for confirmed creation. On ordinary load or dimension replacement, attach or rebind after the outgoing world has been saved and the incoming world is active. Key the in-memory session by actual save-handler identity within the local session, with explicit detach on unload or remote entry. Confirm lifecycle in the packaged build before relying on constructor ordering.
- Use one canonical datum per save, proposed key `zto_lunar`, with schema version, baseline and active day adjustments, and a natural-event replay watermark. The presence of validated versioned state represents initialization. Rebind the same datum to the active local world's storage across dimensions. Only the active save owns writes. Do not create independent dimension calendars or let two stale copies overwrite each other. Cleanly release world references and debug subscriptions on unload. Persist after meaningful changes via `markDirty()`; vanilla save cadence supplies the write.
- A readable older schema gets a deterministic migration. Unknown newer or invalid data must surface a diagnostic and be left intact. `MapStorage.loadData` can return null after a read failure, so distinguish a missing file from an existing unreadable file using the save handler before initializing. Treat the replay watermark conservatively if saved vanilla time is behind it. Normal save/reload is the guarantee; future directors must make their own durable actions idempotent.
- Continue the single calendar through Nether and End using the active local world's carried clock. Expose the active dimension in diagnostics but mark non-Overworld contexts ineligible for scheduling. Suppress natural event dispatch there. Rebaseline on portal handoff without replaying the interval or creating another cycle. Keep a replacement world or remote connection from displaying stale local snapshots.

### Transition and event rules

| Input | Observation and dispatch |
| --- | --- |
| Same tick or paused tick callback | No new events. GUI may still display the last snapshot. |
| Exact +1 raw tick in the active Overworld | Emit natural events only for crossed boundaries. At dawn emit day start, phase change, and cycle completion on a 7→0 wrap, in that order. Night start/end are separate boundary events. |
| Larger forward delta, including sleep and external NEI edits | Classify as `TIME_SKIP` unless an explicit service request supplied a cause. Publish one resync containing before/after snapshots and compact crossed day/cycle/night boundary information. Show the destination phase even when no callback is eligible. Never replay each skipped night. |
| External rewind | Publish one resync with cause `TIME_REWIND` and no ordinary boundary events. |
| Explicit phase set, advance, or reset | Record `ADMIN_COMMAND` or `CALENDAR_RESET`. Update the calendar and send one diagnostic resync; do not produce synthetic natural events. |
| Initialization, reload, portal handoff | Establish a baseline. A diagnostic resync may identify the lifecycle cause, with no ordinary transition callbacks. |

A sampled time delta cannot identify who changed the clock. A +1 edit indistinguishable from normal progression is treated as natural unless the adapter has reliable cause evidence. Avoid instrumenting every time writer just to guess provenance. Track the greatest raw tick through which ordinary boundaries were consumed or skipped. Persist watermark changes at natural boundaries, forward discontinuities and lifecycle rebaselines when relevant. A rewind retains the watermark, so a repeated boundary at or below it cannot refire. Check all events at the same boundary against the previous watermark before advancing it. Update state and watermark before invoking listeners.

### Rendering and synchronization

- The explicit API order is `NEW_MOON`, `WAXING_CRESCENT`, `FIRST_QUARTER`, `WAXING_GIBBOUS`, `FULL_MOON`, `WANING_GIBBOUS`, `LAST_QUARTER`, `WANING_CRESCENT`. Corresponding vanilla atlas cells are `4, 5, 6, 7, 0, 1, 2, 3`. Keep this mapping in the client adapter and verify all cells and orientation visually.
- Inspect verified legacy hooks first. The inspected `WorldProvider` and `RenderGlobal` have no per-world sky renderer registration path. If necessary, isolate a version-guarded transformer that substitutes only the phase result in the existing moon-quad path. The existing `zto-tools/drag-agent/MouseAgent.java` demonstrates an optional Java 8 agent, fingerprint check and exact-match guard. It targets unrelated GUI code and must keep working. Verify one matched call site in both supported environments, account for OptiFine and core overlay class precedence, and report missing or unsupported hooks. On mismatch, absent state, wrong world or remote connection, return the original phase. An unrecognized renderer is a diagnostic fallback, not a pass for visual acceptance.
- Reject a replacement sky renderer, replacing core class files, global provider substitution and rotating the texture atlas. Those approaches affect unrelated sky behavior, world-provider behavior or texture packs. Do not change angle, geometry, UV orientation, lighting, stars, or non-Overworld sky.
- The local view reads snapshots in process. Preserve the boundary for a future authoritative snapshot transport. When ported, the server owns the same core, the client holds a view keyed by session and dimension, and revisioned baselines or deltas reconcile login, reconnect, dimension changes and corrections. Network receive must never drive authoritative listeners. This describes the future adapter boundary; there is no packet or multiplayer test dependency now.

## 4. Internal API and debug menu

Names below are proposed ZTO types. Existing MCP symbols are listed in the audit.

| Contract | Minimal shape |
| --- | --- |
| `LunarService` | Read-only snapshot per valid world context, convenience phase and full/new predicates, and days until a phase (0 if already in that phase). A separate authority entry point observes ticks and accepts validated local debug requests. Unknown or detached contexts return unavailable. |
| `LunarPhase`, `LunarSnapshot` | Stable explicit ID and order with display labels. Immutable snapshot contains save-session identity, dimension, raw time, time of day, logical day, day in cycle, cycle index, completed cycles, phase, night, ticks until next phase and scheduling eligibility. No mutable world references in snapshots. |
| `LunarWorldState` | Persistence adapter for versioned settings and replay watermark. Keep derived phase and counts out of the saved schema. |
| `LunarEvent`, listener registry | One event value with kind, cause, before/after snapshots and relevant boundary/delta information. Kinds cover day start, phase change, night start/end, cycle completion and calendar resync. Register/unregister idempotently for a session. Isolate and report listener exceptions and prevent reentrant mutation. Read queries are safe during callbacks. |
| `LunarEventWindow`, `LunarEventPolicy` | A pure predicate over a snapshot with a phase filter, dimension eligibility and half-open time bounds. Support windows crossing midnight, reject equal endpoints, and evaluate the phase of the sampled snapshot. Optional ID/priority can identify candidate windows without making the calendar arbitrate them. |

A future director can subscribe to natural `NIGHT_STARTED`, inspect the snapshot for `FULL_MOON` and an eligible Overworld window, and then apply its own scheduling and world rules. The debug listener exercises that same path. Matching a window at the destination of a skip is visible in the GUI but does not imply a natural night-start callback.

### Lunar Debug GUI

- Provide a configurable, opt-in keybinding and a dedicated legacy `GuiScreen` that opens only during local play with no other screen active. Use the standard keyboard callback and existing config conventions. Keep the screen non-pausing so the owner can observe natural boundaries. Handle world change, screen close, unsupported renderer and unavailable state explicitly. Present labels and status in English.
- Display phase, API ID and atlas cell; logical day and cycle; raw clock, time of day and ticks to the next phase; dimension, night status, active test window and scheduling eligibility; local authority and render-hook status; save/migration status; and the latest transition and cause.
- Display a small bounded history of observed transitions with before/after state, reason and eligibility outcome. The diagnostic full-moon listener should show whether it received a natural night start and whether its test window matched. Label this as a diagnostic trigger and show why a skip, rewind or off-dimension observation was suppressed. Opening or closing the GUI must never register duplicate listeners. Do not use chat for routine feedback.
- Provide **Next phase**, **Set phase**, **Advance cycle**, **Reset alignment**, and **Simulate time jump**. Set phase selects the current or next occurrence of the requested phase, advance cycle adds eight days, and reset restores the original alignment at the current vanilla time. The first four actions route through the local authority and report their applied or rejected result in the panel. Reset requires confirmation. Time-jump simulation runs the transition policy on a detached copy and labels its preview, without changing save data, vanilla time or live events. Test real sleep and NEI edits separately.
- Enable verbose logging only by configuration for transitions, migration, save or listener diagnostics. The panel is the primary inspection surface.

## 5. Implementation sequence

| Task and dependencies | Components and change | Risks and acceptance |
| --- | --- | --- |
| A. Core, no dependencies | Add pure lunar phase, snapshot, math, transition and window contracts. Add focused Java tests under the established `zto-tools` pattern. | Signed arithmetic and replay semantics. Test new day 0, full day 4, wrap at day 8, migration mapping, bounded jumps, rewind replay suppression and midnight windows. |
| B. Local authority and state, after A | Add the world adapter and versioned save datum. Own initial changes to `mod_ZedsTekkitOverhaul.java` and preserve existing callbacks. Bind local tick and load lifecycle. | First save, pause, remote construction and portal order. Demonstrate new/legacy classification, persistence, no lifecycle replay and independent saves. Compile with MCP. |
| C. Events and debug screen, after B | Wire the session listener registry, debug listener, GUI and key delegate. Add storage, render and trigger status fields with unavailable handling. | Key collisions, duplicate callbacks and accidental writes from render. Verify live natural and suppressed paths, all buttons, detached simulation and valid vanilla clock behavior. Compile with MCP. |
| D. Moon rendering, after B | Add the client atlas adapter and the smallest verified hook. If needed, own its tools, packaging and isolated launch instructions. | Core overlay, mappings, classloading and texture packs. Check supported fingerprints, untouched fallback and all eight visible phases in Tekkit Dev. Compile and reobfuscate. |
| E. Integrated acceptance, after C and D | Reconcile debug hook status, run the full disposable-world matrix and record build and runtime results. | Prioritize world safety and single-player evidence. Report remaining failures with reproduction steps. |

Run the tasks sequentially by default. If assigned separately, C and D may work independently after B. C owns the debug menu and its bootstrap delegate. D owns the render bridge and tooling, with B's adapter owner or the integrator handling any shared bootstrap change. Agree on the read-only snapshot contract before either task modifies a shared interface.

## 6. Single-player verification

The focused calendar, moon transformer, and Dev Bridge tests passed. The MDK client compiled, reobfuscated and packaged successfully. The transformer test checked the MCP renderer, vanilla archive, and installed Tekkit Dev overlay. The isolated runtime reports Minecraft 1.2.5, FML 2.2.106.176 and Forge 3.3.8.164. The declared Forge 3.4.9.171 runtime still needs a separate check.

Observed in Tekkit Dev:

- A new disposable save wrote `data/zto_lunar.dat`. A legacy save reported legacy alignment and exposed its full-moon snapshot through the [Dev Bridge](dev-bridge.md). At the title menu, the bridge rejected the lunar query with `NOT_READY`.
- The project owner confirmed that the Lunar Debug panel worked. Runtime logs showed a phase set as `ADMIN_COMMAND` and an external time edit as `TIME_SKIP`. The owner also confirmed the temporary full-moon zombie fixture and the log recorded 120 spawned zombies from an administrative transition. The fixture was removed after the test.

Still open in the client: natural full-moon night scheduling and delivery through the bridge, boundary reload and dimension round trips, real sleep, malformed data handling, the full set of panel controls, and visual comparison of the eight atlas cells with a compatible texture pack. The temporary zombie fixture exercised administrative event delivery.

| Check | Expected evidence |
| --- | --- |
| New and legacy saves | Fresh save shows new moon at logical day 0, full moon at day 4 and wrap at day 8. Include create/exit/reopen. A copied legacy save keeps vanilla clock and visible phase on first load, then retains its alignment on subsequent loads. |
| Natural event path | Observe dawn and a natural full-moon night with the panel open. Confirm event order, one notification per boundary and one eligible diagnostic trigger. Also test with the listener removed, the GUI closed and a failing test listener. |
| Sleep and time edits | Sleep in a bed, exercise installed NEI hour buttons, and cover same-day, multi-day and backward adjustments through tests or the simulation. Phase matches the destination. Skipped or repeated boundaries do not replay ordinary callbacks. |
| Persistence and travel | Check autosave, Save and Quit, reopen around a boundary, unload/reload, opening another save, Nether and End round trips and reopening a save from another dimension. Check malformed or newer lunar data on disposable copies. Verify one datum per save and no stale view or subscriber. |
| Rendering and GUI | Compare all eight cells with default and compatible legacy texture packs. Verify apparent motion, blending, ordinary brightness and the Nether/End sky. Try Fast/Fancy, multiple GUI scales, key conflicts, unknown-hook fallback and all control result messages. |
| Packaging and pack regressions | Run focused tests, `recompile.bat`, `reobfuscate.bat` and `zto-tools/build-and-deploy.bat`. Verify the packaged classes and any separately required hook. Use the isolated **Tekkit Dev** client. Smoke-test NEI inventory/time, ZTO crafting and GUI startup, and ordinary spawning and sleep. Record MDK versus real Tekkit evidence separately. |

See [Testing](testing.md) and [Environment and build](environment.md). Dedicated server, reconnect and packet synchronization are outside this acceptance matrix.

## 7. Agent prompts

### Implement the full milestone

> Implement the first single-player Lunar Cycle milestone for ZedsTekkitOverhaul. Read AGENTS.md, PROJECT_VISION.md, docs/agents/lunar-cycle-system.md and docs/agents/lunar-cycle-implementation.md. Follow tasks A through E in the handoff and verify the actual Minecraft 1.2.5 MCP and Tekkit Dev symbols before editing. Deliver the persistent eight-phase calendar, native moon phase integration, read-only snapshots and event/window API, plus the opt-in Lunar Debug GUI and diagnostic full-moon listener. Keep local authority, persistence, rendering and UI separate so a future dedicated-server adapter can reuse the calendar. Multiplayer implementation and tests are deferred. Leave vanilla time, sleep, lighting and spawn rules intact. Keep changes focused and avoid unrelated refactors. Run focused tests, MCP compilation and reobfuscation, then verify behavior in the isolated Tekkit Dev client on disposable saves. Report actual results, remaining limitations and the preserved extension points. Write repository content in English and communicate with the project owner in Italian.

### Focused assignments

Use the full prompt's workspace, scope and reporting rules for every assignment.

- **A and B, core and persistence.** Implement the calendar, time classification and one save-scoped datum using the verified `World`, `WorldInfo`, `WorldSavedData` and FML lifecycle paths in §§1–3. Own the first bootstrap edits. Run focused tests and MCP compilation. Show how first-save initialization, legacy migration, portal handoff and replay protection were verified.
- **C, event diagnostics and GUI.** Use A/B's APIs and the existing `CreativeInventoryHook` keybinding pattern to implement the event and GUI requirements in §4. Keep the calendar controls inside the authoritative service. Run focused event tests and MCP compilation. In Tekkit Dev, show a natural full-moon trigger and the reason a skip was suppressed, and verify all controls and the simulated jump.
- **D, renderer integration.** Inspect `RenderGlobal.renderSky`, the runtime overlay's `l` and the matching atlas and SRG files. Implement the smallest verified substitution from §3. Run compile, reobfuscation and hook checks. Verify all eight phases in Tekkit Dev with a legacy texture pack and vanilla fallback on unsupported inputs. Coordinate bootstrap work with the integrator.
- **E, integration.** Integrate reviewed A–D changes, run §6 and document exact artifacts, commands, results and unverified items. Keep changes scoped to failures found in lunar integration. Record any dedicated runtime limitations as deferred work.

## 8. Acceptance checklist

- [ ] New and legacy worlds initialize correctly and preserve vanilla time across saves and portal travel.
- [ ] The eight visible phases match snapshots and native atlas orientation. Legacy texture-pack lookup, sky motion, lighting and other dimensions retain their native behavior.
- [ ] Natural boundaries fire once. Sleep, large jumps, rewinds, lifecycle changes and admin actions have bounded and distinguishable results.
- [ ] Pure snapshot and window queries expose the phase, time, cycle and context needed by future directors.
- [ ] The owner can inspect API data, trigger decisions, suppression causes and action results from the in-game menu.
- [ ] The debug listener demonstrates scheduling eligibility without touching spawn or threat systems.
- [ ] Core code is independent of client classes. Multiplayer transport and dedicated lifecycle remain separable future work.
- [ ] Focused checks, MCP compile/reobfuscation and isolated single-player Tekkit Dev results are recorded with their actual evidence level.
