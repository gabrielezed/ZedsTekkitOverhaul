# ZedsTekkitOverhaul — Survival Threats & Lunar Assaults

- **Status:** Design contract for the first implemented gameplay slice. See [acceptance evidence and limits](survival-assault-acceptance.md).
- **Target:** Minecraft Java 1.2.5, Tekkit Classic 3.1.2, singleplayer-first.
- **Release line:** `0.1.0-alpha.1`, following the [versioning policy](VERSIONING_POLICY.md).
- **Primary milestone:** Lunar `PLAYER_ASSAULT`.
- **Deferred systems:** Settlement inference, `SITE_SIEGE`, equipment backport, advanced enemy roles.
- **Authority:** `PROJECT_VISION.md` takes precedence on game-design intent; confirm implementation and ABI in the workspace.

---

## 1. Why this system exists

ZedsTekkitOverhaul should preserve the freedom and recognizable atmosphere of Tekkit Classic while making survival, homebuilding, technology, and defense matter. The world must create intermittent, understandable pressure. The player should *want* to construct stronger walls, routes, defenses, lighting, and machinery, rather than follow a mandatory quest or building template.

Lunar events introduce a recurring rhythm: quiet time, foreshadowing, preparation, danger, recovery, and renewed exploration. The first release is deliberately small: make a bounded lunar assault work end-to-end in real Tekkit singleplayer, measure the experience, and preserve clean interfaces for future threats.

**Do not confuse a player-targeted assault with a settlement siege.** Their target identity, persistence, targeting semantics, and later gameplay are fundamentally different.

### Non-negotiable game-design principles

- No mandatory quests, progression gates, marker blocks, claim flags, beacon structures, or standardized house shapes.
- No designated-base UI or registration mechanic, including a temporary "Defense Marker".
- No demand that bases contain doors, beds, machines, walls, roofs, or particular block arrangements.
- Buildings may be castles, bunkers, towers, outdoor factories, underground shelters, floating outposts, treehouses, or atypical structures.
- Technology gives players **options** and power, not a compulsory engineering checklist.
- Difficulty comes from tactical situations and consequences, not unbounded health/damage scaling or indiscriminate destruction.
- Calm periods are a feature. Ordinary nights should not become constant mini-sieges.
- A future `SITE_SIEGE` must be a real attack on a world site, **not** a `PLAYER_ASSAULT` renamed because it happened near a home.

---

## 2. Definitions and scope

### 2.1 Event kinds

**`PLAYER_ASSAULT` — implement now.** A bounded hostile event targeting the active player's *vicinity*, with no assumption about residence or buildings. The player may be at home, traveling, underground, or near industrial infrastructure. The director decides when the event is eligible; the spawn controller finds safe, eligible positions; vanilla AI handles ordinary combat for the first milestone. The event may pressure a fortification because the player is inside it, but this does not make it a site siege.

**`SITE_SIEGE` — design contract only, not implemented.** A hostile event aimed at a stable, persistent *place* that has been inferred to be a player-associated settlement or outpost. Its target remains spatially meaningful when the player moves. Such events may later involve attacks on entrances, routes, infrastructure, or defenses. They require a separate settlement-recognition capability, initially unavailable.

A small enum or equivalent type may identify both kinds. `SITE_SIEGE` must not appear as a selectable or triggerable runtime event in v0.1.

### 2.2 Milestone boundaries

**Implement in the first vertical slice:**

1. Reuse the existing lunar service and its scheduling windows.
2. A minimal `SiegeDirector` for singleplayer event eligibility and lifecycle.
3. A separate `SpawnController` for bounded, safe, vanilla-mob spawning.
4. Explicit `PLAYER_ASSAULT` event identity and immutable diagnostic snapshots.
5. Configurable conservative parameters, persistent/recoverable event state, and skip semantics.
6. Targeted runtime acceptance scenarios through the existing Dev Bridge; no independent QA infrastructure initiative.
7. Minimal in-world player feedback using a restrained, nonintrusive channel already supported in 1.2.5 if appropriate and technically safe; avoid obligatory HUD overlays.
8. Repository versioning migration and precise build provenance.

**Do not implement now:** settlement inference, site siege selection, claim/marker blocks, special Enderman, wall-breaking AI, intelligent pathfinding, mob gear backport, complex threat levels, player heatmaps, raids on offline bases, multiplayer packets, dedicated-server support, persistent siege HUD, graphical Dev Bridge test overlay, or a generalized event scripting engine.

---

## 3. System boundaries

```text
                         LunarService
                      (calendar + windows)
                              |
                              v
                     SiegeDirector (SP)
                eligibility / lifecycle / state
                              |
               +--------------+---------------+
               |                              |
               v                              v
        AssaultTarget                     SpawnController
        PLAYER_VICINITY             spawn site / caps / mobs
               |                              |
               +--------------+---------------+
                              |
                      Vanilla mob AI
                              |
                              v
                         Game world

           Observer/API -> Dev Bridge -> scenarios/reports

    Future only: SettlementInterestService -> SiteTarget -> SITE_SIEGE
```

Core gameplay logic must not depend on `devbridge`, GUI/render classes, HTTP, PowerShell, or test fixture types. The bridge is an observer and, in explicitly authorized disposable worlds, a controlled fixture driver.

### 3.1 Suggested contracts (adapt after source audit)

Names are illustrative; preserve verified existing conventions and do not create overengineered abstractions.

- `AssaultKind`: `PLAYER_ASSAULT`, `SITE_SIEGE` (reserved).
- `AssaultTarget`: immutable target descriptor with `kind`, dimension, initial location, and optional persistent site ID; player-vicinity target can query an authoritative active player through a narrow adapter.
- `AssaultSnapshot`: immutable ID, state, reason, calendar session/cycle/phase, start/end tick, target kind, spawned/active counters, caps, configured parameters, and last rejection/termination cause.
- `SiegeDirector`: observes lunar events and active local world, evaluates eligibility, transitions states, and owns event lifecycle.
- `SpawnController`: searches a bounded local candidate set, validates world/spawn constraints, requests normal entity insertion, reports outcomes.
- `AssaultEventListener`: bounded, registration-scoped event notifications (optional if the lunar event patterns make this trivial).
- `SettlementInterestService`: **future-only conceptual contract**; must not be instantiated, persisted, or called by the first milestone.

Avoid a framework of factories, generic rules engines, or plugin systems without a demonstrated second consumer.

### 3.2 Authority and lifecycle

- Singleplayer local world is authoritative for the first release; keep core logic independent of client rendering where practical.
- Reuse existing `LunarService` / snapshot / event cause semantics, including the actual legacy clock behavior and save ownership identified in `docs/agents/lunar-cycle-implementation.md`.
- Guard out `WorldClient` / remote multiplayer worlds; no multiplayer side effects.
- Distinguish simulation ticks from the mutable vanilla world clock. Do not assume linearity under sleep, NEI time edits, portal changes, or world reload.
- Subscribe/unsubscribe on world ownership changes. Never carry active encounter state into another save by accident.
- Only mutate world state on the owning game thread.
- If events are dispatched on a separate tick phase, avoid duplicate activation when several callbacks observe the same boundary.

---

## 4. Player assault gameplay — v0.1

### 4.1 Trigger

Baseline: an eligible full-moon night in the Overworld, using the existing lunar API's **real** `FULL_MOON` mapping and night window. The already implemented calendar controls the night classification. There is no second independent phase calculation.

Use one assault attempt at most per eligible night per world/save. A skipped night or external time jump must never produce retroactive waves.

**Intentional first-release choice:** ordinary bed skips/time resyncs suppress *new* assault activation at crossed boundaries, consistent with the lunar service's distinction between natural progression and `TIME_SKIP`/administrative resync. This is a safe technical baseline, **not a final anti-sleep design**. Record the game-design question as open. Never punish the player for skipping time through a hidden delayed encounter.

The exact full-moon eligibility, phase/cycle alignment, and event causes must come from the checked-out lunar code, not assumptions about vanilla moon texture indices.

### 4.2 Eligibility and safety

An assault can begin only if:

- The local authoritative Overworld and player are loaded, active, and in a supported state.
- The lunar snapshot identifies a naturally eligible event window.
- The world is not in a lifecycle transition or game pause that makes simulation invalid.
- No current event is active and no previous attempt in the same eligible night has already been consumed.
- The player is in a valid dimension and the candidate area can be safely searched without generating chunks.

Reject or defer an activation gracefully, with a reason visible to diagnostics, rather than forcing spawns or scanning a large area.

Do not spawn inside protected/occupied blocks, liquids where inappropriate, or inside the player's body. Respect actual 1.2.5 entity collision and placement rules verified from local sources. Check chunk-loaded status before any block/entity operations. Account for vanilla monster caps and existing ordinary mob populations so the event does not become a runaway entity source.

### 4.3 Wave shape

The first milestone is **a single modest encounter**, not a full dynamic horde system. The initial encounter uses up to six vanilla enemies in three pulses. Easy and Normal mix zombies and skeletons; Hard uses skeletons because vanilla 1.2.5 Hard zombies can break wooden doors. Counts remain provisional pending owner playtesting.

V0.1 exposes the enable switch, total spawn budget and pulse interval. Keep the other bounds internal until playtesting gives a reason to tune them separately:

- Activation permission (default enabled for the actual game build if testing confirms fairness, independently disableable).
- Spawn budget and total spawned cap.
- Maximum simultaneously active assault entities.
- Pulse interval (simulation ticks).
- Allowed horizontal spawn annulus, player exclusion radius, and search attempt budget.
- Event duration / timeout and post-event cooldown.
- World/dimension gating and diagnostic verbosity.

Do not raise all mob HP/damage based on calendar age. Avoid a fixed mandatory technology requirement. The player must be able to survive with ordinary early-game materials and good preparation.

### 4.4 Spawn strategy

Choose candidate positions in a limited annulus around the player at the time of each pulse, accounting for local terrain and loaded chunks. The spawn algorithm must:

- Apply a bounded attempt budget per pulse; no full chunk scans, no unconditional chunk loading.
- Test ground support, clearance, collision, player proximity, dimension/world identity, vanilla difficulty/monster settings, and practical line-of-sight fairness as appropriate.
- Avoid spawning *inside* an enclosed base or directly beside/behind the player simply to bypass walls.
- Avoid spawning inside machinery or altering blocks.
- Use ordinary vanilla entity lifecycle and AI as much as possible.
- Report spawn denials and causes (`UNLOADED_CHUNK`, `NO_VALID_POSITION`, `ENTITY_CAP`, `PEACEFUL`, etc.) in bounded diagnostics.
- Use deterministic/seedable candidate selection in isolated tests when feasible without altering ordinary game randomness globally.

A spawn failure must not be compensated with infinite retries or larger search regions. The event may safely produce fewer enemies than the target budget.

### 4.5 Event states and completion

A minimal state machine suffices:

`IDLE -> ARMED/ELIGIBLE -> ACTIVE -> RESOLVED / ABORTED -> IDLE`

`ARMED` is optional if a direct eligibility check is cleaner. Each transition is idempotent within the same world and night identity.

An event ends when its duration expires and there are no further waves, or when another documented completion condition is satisfied. Existing spawned mobs should follow ordinary Minecraft lifecycles unless a specific cleanup policy is justified; do not teleport/remove unrelated mobs or sweep the world indiscriminately. If a player travels, changes dimension, reloads, dies, or changes the game difficulty, use bounded explicit termination rules. Do not pursue a player indefinitely across chunks with hidden force loading.

Prefer predictable, explainable reasons: `NIGHT_ENDED`, `PLAYER_UNAVAILABLE`, `DIMENSION_CHANGED`, `WORLD_UNLOADED`, `TIME_RESYNC`, `DIFFICULTY_PEACEFUL`, `TIMEOUT`, `SPAWN_BUDGET_EXHAUSTED`, `COMPLETED`.

### 4.6 Persistence and time manipulation

Persist only the minimal event identity and night-attempt record required to avoid duplicate starts and accidental replay across Save/Quit/Reload. Do not persist client object references, live entity objects, or speculative threat state.

Decide and document restart semantics explicitly. **Recommended v0.1:** an interrupted event is marked closed/aborted on reload; the same full-moon night is not automatically restarted, and old enemies remain subject to normal world behavior. This avoids rebuilding per-mob ownership for the initial milestone. Verify at least once by live save/reload. Later siege versions may choose event resumption with dedicated ownership tags and stable IDs.

- Natural transition: may activate once when eligible.
- Ordinary skip / bed: never replay each crossed boundary.
- Administrative and NEI time changes: calendar resync, no retrospective activation.
- Backward clock movement: no second event for already-consumed same-night identity; bounded persistence required.
- World transition: detach old listeners and cancel queued work.

Never overwrite the vanilla world clock to make an event happen.

### 4.7 Presentation and atmosphere

The environment, full moon, and the player's surroundings should communicate approaching risk. Keep the first milestone understated and consistent with Minecraft 1.2.5. Do not add a permanent flashy HUD or modal interruption. A small optional sound or concise in-world indication may be evaluated after gameplay works; avoid hardcoded chat spam or a required warning UI.

No base damage or block breaking in the first encounter. The experience should still reward walls, entrances, elevation, obstacles, and the player's positioning naturally. Player feedback on whether the first assault actually creates the desire to fortify is a release acceptance criterion, separate from automated correctness.

---

## 5. True site sieges and automatic settlement recognition — future

**Hard requirement:** no manually declared sites or compulsory blocks. The game should ultimately infer important player settlements from observable usage, not predefined architecture.

### 5.1 What counts as a settlement?

A *candidate player-associated site* is a world area that shows sustained, repeated activity and investment. Potential evidence (none required individually):

- Player dwell time and repeat visits over multiple days.
- Resting/sleeping locations, storage interactions, crafting and machine usage.
- Industrial machinery, transport networks, power generation, and sustained processing.
- Player-mediated placement and modification events, including later building history.
- Doors, beds, storage and lighting as supporting hints, never mandatory conditions.
- Clustering of several adjacent, meaningfully used chunks.

Do **not** infer ownership merely from block density, naturally generated villages, naturally generated structures, or one visit to a cave or chest. A mine, farm or outpost may eventually be a valid separate site, but it must not always replace the primary home.

### 5.2 Proposed future model

A future `SettlementInterestService` can maintain sparse, save-scoped spatial evidence (e.g., chunk-based cells) and time-decayed activity. Nearby high-interest cells may form candidate site regions. Hysteresis and minimum observation periods prevent oscillation when the player briefly travels or builds somewhere else.

In a later prototype, developers should quantify false positives and negatives on diverse build styles before using detected sites for hostile events. Expose confidence and evidence in Dev Bridge diagnostics; do not invent a numerical score today.

**Important separation:** settlement confidence answers *which places matter*, not *how hard the game should punish their owner*. Threat level is an independent future system; high industrial activity must not silently create infinitely strong mobs.

### 5.3 Future `SITE_SIEGE` behavior

True site sieges retain a place target independent of the player's current position and must define what happens when the player is away, the site is unloaded, or a second site is present. They may later use specialized mob roles and infrastructure targeting, with controlled, recoverable griefing and countermeasures.

None of this belongs in the first implementation. Do not build a temporary settlement marker as a shortcut; it would normalize the wrong gameplay contract.

---

## 6. Mob equipment backport — required later, not now

Minecraft 1.2.5 contains special-case held items on selected mobs (e.g., skeleton bow, zombie pigman weapon), but predates the generalized 1.4-era equipment system used for varied zombie/skeleton armor and weapons. Agents must verify exact class members and rendering hooks in the local 1.2.5 MCP and overlay before implementing a backport.

**Confirmed future design direction:** backport a carefully scoped mob equipment capability supporting held weapon and armor slots, equipment-dependent stats, rendering, damage reduction and appropriate drops for chosen hostile entities.

Recommended future development sequence:

1. Audit vanilla/entity renderer, combat, ItemStack, NBT and held-item behavior in installed 1.2.5.
2. Define a small equipment-slot state model (hand, head, chest, legs, feet), not necessarily a full general inventory.
3. Implement NBT persistence and rendering without breaking existing skeleton/pigman behavior.
4. Add equipment generation profiles and drop chances only after slot behavior is correct.
5. Attach equipped roles to lunar assault archetypes and later site sieges.
6. Add Dev Bridge equipment snapshots and combat assertions when this work starts.

**Do not implement the backport as part of `0.1.0-alpha.1`.** Current assault mobs remain vanilla; variation initially comes from timing, numbers, positions and mixed mob roles.

---

## 7. Forge, Tekkit and legacy API compatibility

The source repository historically declares Forge `3.4.9.171` as its development/build target. The actual isolated Tekkit Dev runtime reports Forge `3.3.8.164`, with Minecraft `1.2.5` and FML `2.2.106.176` in recent acceptance reports.

**Policy:** the Forge/FML and mod versions shipped with the supported **Tekkit Classic 3.1.2 runtime** are authoritative for runtime compatibility. The newer Forge may remain in MCP as the compilation environment as long as the resulting mod demonstrably runs on the pack's actual installed ABI.

- Do not infer API compatibility merely because both builds target Minecraft 1.2.5 or because version differences appear to be bugfix-only.
- Audit and test any legacy Forge event/entity/spawn signature in the matching runtime JAR or source.
- Do not silently upgrade the player's Tekkit installation.
- Do not use compile-only APIs absent at runtime.
- Do not bundle Forge, FML, mod JARs, vanilla classes, or decompiled sources in ZTO artifacts.
- Update `AGENTS.md`, `docs/agents/environment.md`, and the compatibility matrix to clearly distinguish **runtime target** from **development compiler**.
- Minecraft behavior, local MCP mappings, bytecode transformations and existing hook ownership must be checked in the actual installed overlay.

Singleplayer is required; multiplayer implementations, sync packets and dedicated-server behavior are deferred. Keep core rules sufficiently isolated to make eventual server ownership possible without undertaking server work now.

---

## 8. Release versioning and provenance

### 8.1 New version line

Adopt **SemVer-like pre-1.0 development versions**. Starting survival release: `0.1.0-alpha.1`.

Existing `0.7.0-distillation-prototype` is a **legacy prototype label**, not a reason to keep escalating the previous numbering. Document the one-time reset explicitly in the changelog; no saved data, block IDs, tile registrations or network contract may change simply because the display version changes.

Use:

- Release line: `MAJOR.MINOR.PATCH` with prerelease suffix (e.g. `0.1.0-alpha.1`, `0.1.0-alpha.2`, `0.1.0-beta.1`, `0.1.0`).
- Git revision and build hash: provenance metadata, e.g. `0.1.0-alpha.1+g<short-sha>`, **not** a new semantic release for every build.
- Build artifact and QA report: full Git SHA, dirty/clean state, UTC build timestamp, archive SHA-256, Minecraft/Tekkit/Forge/FML runtime versions, compiler environment.
- Tags: `v0.1.0-alpha.1` and subsequent release tags point to exact clean commits. Never retag published artifacts silently.
- Changelog/release notes: describe gameplay, bug fixes, compatibility and breaking changes.
- Do not mix a version bump with a save-format migration without an explicit migration plan.

**Version semantics:** 0.1.x is experimental; adjust the minor version for a new playable feature milestone, patch for compatible fixes, increment prerelease number for internal release candidates. A non-release CI/development build can display its base version plus commit metadata.

### 8.2 Single source of truth

Implement one canonical version constant or generated build metadata source, in whichever form works safely with FoxyRetroMDK/MCP and `mcmod.info`. The same version must appear in `getVersion()`, the mod metadata, logs, artifacts and test reports. Do not create two divergent hardcoded versions. If generated metadata is unsafe in this legacy toolchain, keep an explicit checked-in version file and a build-time consistency check.

Preserve current build layout and jar naming where changing it would break the deployment scripts. Add uniquely named retained build archives or a manifest as needed without committing generated binary outputs.

Document the concrete policy in `docs/development/VERSIONING_POLICY.md` or another single canonical location and link from `AGENTS.md`/README; avoid contradictory duplicate documents.

---

## 9. Dev Bridge role and deferred on-screen QA overlay

The existing authenticated, opt-in, loopback Dev Bridge and unattended test runner remain the project's test infrastructure. Do not initiate another general infrastructure milestone for this feature.

Add only the minimum diagnostics required to test assaults:

- `assault.snapshot` (read-only): current state, kind, target dimension, event/night key, wave/pulse number, spawn budget, active owned mob count if reliable, elapsed simulation ticks, finish reason, last rejected spawn reason.
- The bounded snapshot and existing `lunar.events` suffice for the initial scenario. A separate assault event stream can wait for a consumer.
- One narrow, disposable-world-only scenario may set a known lunar condition or prepare a test night *through the existing authoritative lunar controls*; must not rewrite vanilla time or permit arbitrary world mutation.
- Automated assertions should verify exactly-once activation, bounded spawning, no invalid positions, persistence/reload policy, suppression after time skip, clean shutdown and ordinary-world denial.

Prefer natural-event observation or safe deterministic fixture setups over long wall-clock waits. No runtime entity scans of whole dimensions.

### Future Dev Bridge graphical overlay (non-priority)

The owner would like optional on-screen developer information while Dev Bridge tests run. Record a future `DevTestOverlay`/developer status panel that could show: connected bridge, active scenario, current step, simulation ticks, PASS/FAIL counters, event names and failure reasons. It must be disabled by default, visible only in development mode, compact, noninvasive and not change game logic. **No implementation required in v0.1.**

---

## 10. Test matrix and acceptance

### Portable/unit tests

- Moon phase/window mapping through existing `LunarService` snapshots.
- Eligibility gate, exactly-once night key, one active event maximum.
- Bed skip, admin jump, backward time changes and lifecycle invalidation.
- Event state transitions and persistence marker reconciliation.
- Deterministic spawn-site validator using stubbed loaded-block access; bounded attempt count and entity cap.
- Configuration bounds, disabled mode and peaceful difficulty.
- No dependency from core gameplay logic back into Dev Bridge/client-only rendering.

### Live Tekkit Dev scenarios

1. **Natural full-moon assault:** start from an authorized disposable world, observe a natural eligible boundary, assert `PLAYER_ASSAULT` becomes active once and spawns no more than its configured budget.
2. **Ordinary night:** no assault starts in a noneligible phase.
3. **Safety/placement:** entities do not appear inside the player's bounding box, blocked locations or unloaded chunks; zero valid sites terminates cleanly.
4. **Time skip:** crossed boundaries do not trigger retroactive waves; resync reason recorded.
5. **Save/reload:** re-entering the world does not duplicate an already consumed night or resurrect a terminated event.
6. **World/dimension switch:** subscriptions and world-scoped diagnostics do not leak; no remote/multiplayer mutations.
7. **Dev Bridge disabled:** no listener or test mutation endpoint; ordinary gameplay continues.
8. **Regression:** existing `singleplayer-core` suite and relevant lunar tests remain passing.

### Human gameplay acceptance

A short real Survival session should determine:

- Is the warning/anticipation understandable without constant UI?
- Are first-cycle enemy numbers survivable for a player with basic equipment?
- Does building walls/entrances materially help when the player is nearby?
- Are spawn positions fair, rather than surprising from inside secure structures?
- Can the player choose to flee, regroup, or defend without oppressive penalties?
- Does the event create a desire to improve one's base rather than only grind weapon damage?

Automated PASS validates code behavior, not subjective fun. Record both separately.

---

## 11. Work packages and milestones

**S0 — Handoff and version baseline.** Confirm PR #19 status; inspect checkout, running runtime and lunar APIs; establish version metadata policy and compatibility wording. No runtime changes until source signatures are checked.

**S1 — Minimal assault model.** Define `PLAYER_ASSAULT`, snapshot, eligibility and persistent night-consumption marker with portable tests. Reserve `SITE_SIEGE` as a type only.

**S2 — Spawn controller.** Bounded site validation and conservative vanilla zombie/skeleton pulses; no chunk generation or block destruction; portable placement tests.

**S3 — Runtime integration.** Local world lifecycle, lunar listener, event start/finish, config, idempotency, logs; real Tekkit build and first end-to-end observation.

**S4 — Focused Dev Bridge diagnostics.** Minimal read APIs and one reproducible automated scenario; regressions against `singleplayer-core`.

**S5 — Gameplay tuning.** One owner-facing Survival test with very small iterations in spawn distance, caps and cadence; release candidate metadata and clear known limitations.

Do not let a single failed corner case consume the whole milestone: after two focused debugging attempts, record a reproducible blocker and continue independent packages. Do not declare acceptance when critical tests are blocked.

---

## 12. Definition of Done — Survival 0.1

- `PLAYER_ASSAULT` reliably occurs on a configured natural full-moon night and never masquerades as a base-targeted siege.
- Vanilla mobs spawn safely, in bounded quantities, under normal game ownership and without creating/loading distant chunks.
- Assault progression, termination, world transitions, time edits, and Save/Quit/Reload have documented deterministic behavior.
- Ordinary nights and peaceful mode remain unaffected; normal worlds cannot invoke development fixture mutations.
- No Defense Marker, player site claim, structure template, settlement inference, player gear backport or multiplayer work is included.
- Core logic is testable and does not depend on Dev Bridge or renderer classes.
- The real Tekkit Dev runtime passes a focused assault scenario and relevant existing regression tests, with build hashes and runtime provenance recorded.
- Version metadata is coherent, with a documented reset from the old prototype label.
- Owner can play a short Survival trial and evaluate tension, fairness and freedom of building.
- Future `SITE_SIEGE`, settlement recognition, equipment backport and optional in-game Dev Bridge status overlay remain explicitly recorded, not accidentally implemented.

**Design test:** if the first implementation forces the player to declare a base, builds a generic settlement detector, or treats a mob wave following the player as a true site siege, it has deviated from this specification.
