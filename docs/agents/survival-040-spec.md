# ZedsTekkitOverhaul — Survival 0.4.0
## SITE_SIEGE & Fortress Warfare

**Document type:** Major Gameplay and Technical Design Specification  
**Status:** Strategic direction and alpha.1 boundary approved. Later phases require separate authorization. Gameplay values remain provisional until owner playtesting.

**Target:** Minecraft Java 1.2.5 / Tekkit Classic 3.1.2  
**Prerequisite milestone:** Survival 0.3 — Settlement Recognition  
**Strategic milestone:** Survival 0.4 — SITE_SIEGE & Fortress Warfare  

---

# 1. Executive Vision

Survival 0.4.0 introduces strategic hostile attacks against persistent player-established settlements and outposts.

The world already understands ordinary hostile spawning, Territorial Activity, lunar encounters, Enderkin roles, aerial predators, and local environmental threats.

Survival 0.3 established recognition of meaningful player-built locations through [0.3.0-alpha.4](survival-030-alpha4.md). Its implemented site and protection contract governs alpha.1 consumption.

Survival 0.4 must turn that recognition into a new form of gameplay.

**Enemies should be capable of identifying an established site, approaching it, pursuing meaningful tactical objectives, and responding to its actual defensive architecture.**

This is fundamentally different from simply spawning additional mobs around the player.

A fortified castle, underground bunker, industrial outpost, open farm, and treehouse should create different attacking opportunities and defensive problems.

The player should have meaningful reasons to build:

- Defensive walls.
- Protected entrances.
- Elevated firing positions.
- Layered barriers.
- Concealed or enclosed machinery.
- Protected logistical corridors.
- Accessible maintenance routes.
- Covered animal farms.
- Traps and tactical chokepoints.

Advanced Tekkit technology should provide additional defensive possibilities.

However, none of these structures or technologies should be mandatory settlement requirements.

## Core promise

**A siege should test how a place is defended, not simply how much damage the player can deal.**

Successful construction, preparation, engineering, and tactical decisions should materially influence the encounter.

---

# 2. The Strategic Role of Survival 0.4

The project roadmap establishes the following separation:

**0.2 — Territorial Activity & Threat**

The world understands localized activity and bounded hostile pressure.

**0.3 — Settlement Recognition**

The world identifies meaningful player-built locations, their approximate footprints, provenance, lifecycle, and protected spaces.

**0.4 — SITE_SIEGE & Fortress Warfare**

The world uses established site identities to conduct strategic encounters against persistent locations.

Survival 0.4 must consume the 0.3 recognition system rather than reinvent it.

It must not treat the player's current position as the sole definition of a site.

It must not establish a parallel settlement classifier inside the siege director.

The purpose is to complete the chain:

**World Activity → Settlement Recognition → Site Assessment → Strategic Encounter → Tactical Action → Resolution → Recovery.**

---

# 3. Non-Negotiable Design Principles

## 3.1 No compulsory base design

A player should not be required to build a standard castle.

Underground bases, mountain shelters, exposed factories, dispersed outposts, treehouses, and unconventional structures must remain legitimate.

An effective defense is a meaningful choice, not a prescribed checklist.

## 3.2 No claim markers

SITE_SIEGE must target naturally recognized sites.

Do not require flags, beacons, claim blocks, special beds, ownership machines, or registration commands.

## 3.3 Intermittent danger

Strategic sieges must be exceptional and consequential encounters.

They should not happen continually because the player spends time at a settlement.

Ordinary gameplay must retain substantial periods of calm.

## 3.4 Architecture matters

Walls, entrances, terrain, roofs, elevation, obstacles, defensive depth, and machinery should influence attacker behavior.

A perfectly secured base should not be bypassed through arbitrary enemy teleportation.

## 3.5 Technology remains powerful

Advanced equipment and automation should be genuinely useful.

Do not counter every defense automatically.

An advanced fortified player should sometimes dominate encounters that would have been dangerous earlier.

## 3.6 Controlled consequences

Enemy actions may cause disruption, limited recoverable damage, or tactical setbacks.

They must not indiscriminately destroy expensive machines or erase substantial player progress.

## 3.7 Tactical intelligence over numbers

Prefer recognizable roles and complementary actions.

Avoid relying primarily on inflated mob health, increased damage, or excessive enemy counts.

## 3.8 Meaningful recovery

An encounter must have a conclusion.

The player needs opportunities to repair, reorganize, reinforce, and resume normal play.

---

# 4. PLAYER_ASSAULT versus SITE_SIEGE

These encounter types must remain structurally distinct.

## PLAYER_ASSAULT

Targets the player or the player's immediate vicinity.

May occur while traveling, exploring, or defending a building.

Does not require settlement recognition.

Its location follows the active player under existing rules.

## SITE_SIEGE

Targets a stable recognized location.

Uses a persistent site identifier.

May choose approaches, entrances, exposed infrastructure, or defensible areas as tactical objectives.

The site's identity remains meaningful when the player moves.

The siege does not automatically retarget to wherever the player goes.

### Required technical distinction

A SITE_SIEGE must retain:

- Encounter kind.
- Stable site ID.
- Target dimension.
- Snapshot of site identity and relevant geometry.
- Encounter admission cause.
- Planned tactical objectives.
- Active attacking groups.
- Encounter budget.
- Lifecycle status.
- Resolution reason.

Do not implement SITE_SIEGE as a PLAYER_ASSAULT with a different display name.

---

# 5. Integration with Settlement Recognition

**Survival 0.3 is a hard architectural dependency.**

The siege system consumes immutable observations through `SiteQueries` on the owning world thread. `SiteSnapshot` carries a save-local ID, session, dimension, known parts, envelope, last-confirmed time, coverage and dormancy. Discovery can start near the player. The admitted save-local site ID stays fixed as the player moves. Siege logic leaves recognition, promotion, historical IDs and provenance evidence under settlement authority.

## 5.1 Site eligibility

A recognized settlement is not automatically a valid siege target.

The siege director must evaluate eligibility independently.

Candidate inputs include:

- Stable recognized site identity.
- Sufficient recognition confidence.
- Current site lifecycle state.
- Availability of relevant loaded terrain.
- Appropriate encounter timing.
- Local Territorial Activity and bounded threat.
- Recent siege history.
- Difficulty.
- Current hostile pressure.
- Suitable approach and objective opportunities.

## 5.2 Recognition uncertainty

Low-confidence or historically ambiguous sites should not automatically receive destructive tactical encounters.

Assess identity confidence separately from geometric confidence for each action. A recognized PARTIAL site can support a non-destructive siege when relevant loaded exterior geometry is independently verified. Partial coverage grants no permission to spawn in unknown space, traverse an unknown corridor or breach a structure. Historical geometry establishes neither live geometry nor physical access. Defer or reject unsafe actions when evidence is STALE, UNAVAILABLE or UNKNOWN.

Do not treat absence of provenance as evidence that the site is natural.

## 5.3 Site footprint

Use known parts and the approximate aggregate envelope from Settlement Recognition. Gaps between parts establish neither occupation, protection nor traversal. Alpha.1 excludes insertion inside the horizontal envelope including courtyards, with a conservative exterior margin.

Do not replace them with an arbitrary circular region centered on the player.

## 5.4 Protected interiors

`ProtectedInterior` reports PROTECTED, EXPOSED or UNKNOWN for local standing-height geometry. An exposed courtyard can still be part of the compound. Insertion also requires loaded terrain, valid ground, actor collision checks, verified route evidence and exterior context. Recognition shares its bounded read budget with protection. Defer UNKNOWN observations without forcing repeated synchronous scans.

Do not interpret an entire settlement as either universally protected or universally exposed.

## 5.5 Site changes

An active site may change during a siege.

Players may:

- Open doors.
- Rebuild walls.
- Destroy bridges.
- Place blocks.
- Remove machinery.
- Alter terrain.

The siege must validate relevant tactical information before acting.

Do not assume every structural snapshot remains correct throughout an encounter.

---

# 6. Siege Admission and Scheduling

## 6.1 Scheduling authority

The existing lunar system remains authoritative for lunar phases and event windows.

The siege director is responsible for encounter admission and lifecycle.

Do not introduce a second lunar calendar.

## 6.2 Initial scheduling policy

Evaluate the existing natural full-moon night event using the calendar’s eight-day cycle. A deterministic, reproducible rarity decision provisionally permits one in three opportunities, subject to site cooldown and feasibility. A disposable-world fixture may override rarity while preserving the admission boundary.

## 6.3 No constant escalation

Site age, industrial wealth, or total machine count must not directly cause endless siege escalation.

Territorial Activity may contribute to pressure, subject to saturation and existing limits.

## 6.4 Calm periods

Establish a meaningful site-level cooldown after a siege.

A recently defended site should not immediately receive another full strategic attack.

A committed siege starts a persistent site cooldown of at least two eight-day lunar cycles. Save/reload preserves the cooldown and the consumed boundary.

## 6.5 Encounter arbitration

A single major encounter decision evaluates eligibility, rarity and feasibility before allocating the existing event to SITE_SIEGE. If no siege commits, preserve established PLAYER_ASSAULT eligibility. A committed siege owns that boundary even if it later aborts. Patrols and ordinary ecology retain their existing authorities.

## 6.6 Event timing and replay

Preserve the established lunar calendar's exactly-once admission and replay protections.

Time jumps, Save/Quit, backward time manipulation, and reloads must not create duplicate siege activations.

## 6.7 Player absence

A site's identity persists when the player leaves.

However, **site persistence does not imply that the siege must continue physically simulating while the location is unloaded**.

When the player leaves the required simulation area or the relevant world context becomes invalid, stop insertion and terminate encounter authority with a recorded outcome. Preserve admission and cooldown. World unload and interrupted reload follow the same conservative policy. Never force-load, retarget or change structures offscreen. A later phase may consider suspension.

---

# 7. Siege Encounter Lifecycle

A true siege should have a distinct lifecycle.

## 7.1 Eligible

A recognized site satisfies the initial event rules.

No physical attack has begun.

## 7.2 Planning

The director evaluates bounded available information.

It chooses a tactical profile and candidate approaches.

No expensive full-site pathfinding should occur.

## 7.3 Warning

The encounter provides subtle, recognizable indications that an attack may occur.

Possible cues include:

- Distant hostile sounds.
- Unusual movement outside the perimeter.
- An approaching scouting group.
- Aerial silhouettes.
- A short-lived environmental disturbance.

Avoid mandatory boss bars and permanent warning overlays.

## 7.4 Approach

Attackers move toward valid exterior approach positions.

The encounter should visibly originate outside the defended area.

## 7.5 Engagement

Enemies attempt the selected tactical objectives.

The player can respond through combat, building, movement, traps, or technology.

## 7.6 Pressure and adaptation

The director may select a limited follow-up tactical action if conditions justify it.

Avoid infinitely adapting until every defense is defeated.

## 7.7 Resolution

The encounter ends through success, repulsion, exhaustion, disengagement, player loss, or environmental invalidation.

## 7.8 Recovery

The system records the outcome and enters a meaningful cooldown.

Remaining owned enemies are cleaned up or released according to documented rules.

Temporary sabotage must expire safely.

---

# 8. Strategic Objectives

**This is the defining gameplay feature of SITE_SIEGE.**

A siege should have objectives other than simply killing the player.

Objectives must be physically meaningful and observable.

The initial implementation should support a small set of complete behaviors.

## 8.1 Perimeter Pressure

Attackers attempt to approach the settlement through exterior routes.

They may pressure doors, gates, corridors, or other valid access points.

The objective is to challenge the defensive perimeter.

Do not allow attackers to teleport directly into an enclosed interior simply because a route is blocked.

## 8.2 Localized Breach

A qualified attacker may attempt a limited breach against an eligible obstacle.

Reuse existing Tank or Breacher capabilities where appropriate.

Breach actions must remain telegraphed, bounded, interruptible, and restricted to appropriate blocks.

Avoid indiscriminate destruction.

## 8.3 Industrial Disruption

A Saboteur may attempt to reach a supported exposed industrial endpoint.

The tactical objective is temporary disruption or a narrowly permitted recoverable intervention.

Do not treat all pipes, cables and machines as equivalent.

Use the established verified industrial adapter contracts.

Do not break native transport serialization or energy network membership.

## 8.4 Exposed Area Control

A group may temporarily pressure an open courtyard, approach lane, or outdoor work area.

This can make exposed settlement architecture tactically relevant.

Avoid adding abstract ownership zones or capture meters to ordinary gameplay.

The effect should emerge through actual enemy positioning and combat behavior.

## 8.5 Aerial Pressure

Fugl or Skelebird may occasionally participate where the site has legitimate exposed airspace.

Aerial groups should pressure open courtyards, exposed elevated approaches, or an airborne player.

Roofs and shelter should remain meaningful.

Respect existing aerial encounter limits.

## 8.6 Tactical Diversion

One group may draw the player's attention while another pursues an accessible objective.

This should remain readable and avoid guaranteed unavoidable damage.

Prefer a small number of meaningful coordinated actions rather than a complex strategic AI simulation.

---

# 9. Tactical Profile Selection

The encounter director should select a bounded tactical profile.

Candidate initial profiles:

### Assault

Focused exterior pressure using ordinary hostiles and appropriate Enderkin.

### Breach

A limited attempt to open an accessible structural obstruction.

### Infiltration

A Saboteur-centered encounter directed toward a verified industrial endpoint.

### Aerial Harassment

A limited aerial component supporting exterior pressure.

### Mixed Pressure

A small combination of two complementary tactics.

These are proposed profiles, not mandatory rigid encounter templates.

Each profile must have:

- Explicit admission conditions.
- Suitable target evidence.
- Supported attacking roles.
- A bounded budget.
- Observable tactical progress.
- A termination condition.
- Relevant player counterplay.

Do not admit an infiltration profile when no supported industrial objective exists.

Do not admit a breach profile without a legal, reachable obstruction.

Do not require every siege to contain every tactical role.

---

# 10. Site-Aware Approach Planning

## 10.1 Objective

Enemies should attack a location intelligently enough that its construction matters.

This does not require an elaborate real-time strategy AI.

## 10.2 Loaded-terrain observations

Use only loaded chunks.

Potential approach information includes:

- Approximate site footprint.
- Exterior approach surfaces.
- Accessible passages.
- Terrain elevation.
- Water or cliffs.
- Obstructions.
- Known local protected spaces.
- Valid target endpoints.

## 10.3 Entry directions

Evaluate a small number of candidate attack directions.

Avoid always approaching from precisely the same cardinal direction.

However, do not select directions that have no plausible access route.

## 10.4 Walls

A solid wall should prevent ordinary attackers from walking through it.

Specialized breaching behavior may target limited eligible obstacles.

A blocked route should not authorize arbitrary teleportation.

## 10.5 Elevation

High walls, towers, cliffs and underground entrances should influence approach behavior.

Do not spawn enemies directly on top of a defended tower simply because ordinary pathfinding cannot reach it.

## 10.6 Open industrial sites

A factory without walls may still present meaningful logistical and positional vulnerabilities.

Do not require a conventional perimeter to admit a site siege.

## 10.7 Underground settlements

Consider valid cave entrances, tunnels and exposed exterior approaches.

Do not spawn hostile groups inside the deepest protected room.

## 10.8 Unreachable sites

If no valid approach or objective exists, the siege should reject, defer, or resolve without destructive side effects.

**A successful defense may genuinely prevent an attack.**

Do not invent an unavoidable countermeasure merely to ensure every siege damages the player.

---

# 11. Attacker Roles

Reuse existing ZTO mobs wherever they provide meaningful tactical functions.

## 11.1 Ordinary vanilla hostiles

Provide general pressure and occupy existing hostile budgets.

They should remain useful components of siege composition.

## 11.2 Skirmisher

Supports ranged pressure and repositioning.

May threaten exposed defenders on walls or open approaches.

Must respect cover and attack visibility.

## 11.3 Breacher

Supports close-range perimeter pressure and obstacles.

Its role should differ from the Tank's heavier structural intervention.

## 11.4 Tank

A rare heavy attacker capable of forcing the player to respond to a concentrated threat.

Reuse existing telegraphed frontal strikes and bounded structural breach mechanics.

Do not automatically introduce unlimited building destruction.

## 11.5 Thief

May create tactical distraction or limited recoverable equipment loss.

Preserve existing theft protections, item ownership, recovery, compass and trail behavior.

Do not guarantee theft simply because a siege is active.

## 11.6 Saboteur

A primary strategic specialist.

May be assigned an explicitly supported infrastructure objective.

The surrounding encounter may give the player a reason to intercept it.

Do not broaden the Saboteur's industrial compatibility without a separate verified adapter implementation.

## 11.7 Fugl

May provide limited open-air pressure.

Avoid constant aerial attacks on a player defending from within protected cover.

## 11.8 Skelebird

May serve as a rarer nighttime or full-moon aerial threat.

Preserve its distinct crossing attack and attack-turn constraints.

## 11.9 New siege-exclusive mobs

Do not create a new creature merely because the system is called SITE_SIEGE.

First demonstrate that existing roles can produce a complete, interesting strategic encounter.

Additional species require their own gameplay justification.

---

# 12. Group Coordination

## 12.1 Bounded coordination

A siege group should share sufficient tactical context to avoid acting as entirely unrelated mobs.

Relevant shared information may include:

- Encounter identity.
- Site target.
- Current tactical objective.
- Group role.
- Approach region.
- Objective status.
- Limited coordination timing.

## 12.2 No omniscience

Attackers must not possess perfect knowledge of all hidden rooms, machines, or inventories.

Known objectives should derive from supported site evidence and local observations.

## 12.3 Objective ownership

An assigned Saboteur should not cause multiple independent mobs to attempt identical exclusive disruptions against the same endpoint.

## 12.4 Attack sequencing

Preserve existing telegraph and recovery behavior.

Avoid simultaneous unavoidable player damage from several specialized enemies.

## 12.5 Lost members

The siege must recover safely when an attacker dies, unloads, becomes stuck, or loses its target.

Do not leave an encounter permanently active because one objective owner disappeared.

---

# 13. Controlled Griefing and Structural Consequences

**This is a critical safety and gameplay boundary.**

## 13.1 Purpose

Limited structural damage can make defenses meaningful.

It should never become arbitrary destruction for its own sake.

## 13.2 Default philosophy

Start with recoverable and narrowly bounded consequences.

Prefer:

- Temporary industrial interruptions.
- Limited eligible barrier damage.
- Recoverable theft.
- Tactical loss of access to an exposed area.
- Short-lived disruption that expires automatically.

## 13.3 Protected objects

Expensive machines, TileEntities, inventory contents, fluid buffers, energy storage and complex modded infrastructure require strong protection.

Do not allow generic mob block-breaking logic to destroy unknown industrial objects.

## 13.4 Breach budget

Define a finite, explicit structural damage budget for each applicable encounter.

Count actual successfully modified blocks.

Do not grant unlimited retries or damage simply because the player rebuilds the same wall.

## 13.5 Block eligibility

Use narrowly defined, tested material rules.

Do not treat every non-bedrock block as destructible.

Never remove unknown TileEntities through generic breach behavior.

## 13.6 Drops and recovery

If a supported block is removed, follow a documented, safe drop and recovery policy.

Avoid duplication or item loss caused by repeated siege cancellation.

## 13.7 Sabotage leases

Temporary disruption must expire automatically.

Unloading, death, Save/Quit, and target replacement must not leave equipment permanently disabled.

## 13.8 No guaranteed irreversible losses

Do not introduce random machine destruction, inventory deletion, or major permanent structural damage as an unavoidable outcome.

Any more consequential future damage model requires explicit owner approval.

---

# 14. Defensive Architecture and Counterplay

The player should be able to influence a siege through construction.

## 14.1 Walls

Walls obstruct ordinary approaches.

Their effectiveness depends on material, layout, access points, and specialized enemy capabilities.

## 14.2 Entrances

Doors, gates, bridges and passages may serve as meaningful chokepoints.

Enemies should not automatically bypass every defended entrance.

## 14.3 Elevation

Elevated positions should help defenders observe and attack.

They may also create limited exposure to ranged or aerial threats.

## 14.4 Trenches and water

Terrain manipulation should influence approach routes.

Do not invalidate player engineering through unconditional pathing exceptions.

## 14.5 Covered facilities

Enclosed industrial lines should reduce Saboteur access when a physical approach is required.

## 14.6 Traps

Existing vanilla and Tekkit traps should remain effective where their real mechanics permit.

## 14.7 Advanced equipment

Powered armor, ranged weapons, flight and automation should provide genuine advantages.

The encounter may create tactical complications without nullifying equipment.

## 14.8 Retreat

A player must retain the option to withdraw.

The consequences of abandoning an active siege must be bounded and explicitly defined.

The first implementation should not automatically destroy an unattended base.

---

# 15. Defensive Technology — Candidate Extensions

Survival 0.4 may reveal useful opportunities for additional defensive machinery.

However, new technology is not automatically required.

Potential future features include:

- Compact alarms.
- Motion or entity sensors.
- Redstone-controlled warning systems.
- Electric barriers.
- Automated defensive turrets.
- Short-range anti-teleport measures.
- Powered gates.
- Defensive lighting.
- Alarm outputs compatible with RedPower.
- Industrial monitoring devices.

These are design candidates.

The first implementation should determine whether existing Minecraft and Tekkit technology already provides sufficient defensive options.

A new machine should only be approved if it creates a meaningful capability missing from the installed pack.

Do not turn fortress construction into a mandatory industrial crafting ladder.

---

# 16. Difficulty and Threat Assessment

## 16.1 Separate site identity from difficulty

Settlement size does not equal danger.

A large factory should not automatically receive a larger siege budget than a small cabin.

## 16.2 Relevant inputs

Potential encounter inputs include:

- Existing bounded Territorial Activity.
- Environmental danger.
- Lunar event window.
- Difficulty.
- Current hostile population.
- Current site availability.
- Previous siege cooldown.
- Selected tactical profile.

## 16.3 No infinite scaling

Enemy numbers, health, damage and destructive capabilities must remain bounded.

## 16.4 Composition

Hard difficulty may introduce more complex combinations of roles.

Prefer coordination and tactical diversity over simply adding more mobs.

## 16.5 Early survival

A newly recognized modest base must not automatically become subject to high-intensity strategic attacks.

The first siege experience should remain survivable with reasonable preparation.

## 16.6 Late-game survival

Advanced defenders should be able to defeat ordinary threats convincingly.

More sophisticated encounters may challenge positioning or infrastructure without nullifying technological power.

---

# 17. Siege Budgets and Population Limits

Every siege must have explicit resource budgets.

Candidate budget categories:

- Total admitted attackers.
- Maximum active attackers.
- Number of reinforcement phases.
- Number of tactical objectives.
- Number of specialized roles.
- Maximum active aerial predators.
- Structural breach allowance.
- Temporary sabotage allowance.
- Spawn candidate attempts.
- Encounter duration.
- Recovery cooldown.

The exact values must be calibrated through tests.

Do not invent a large number of configuration settings without demonstrated need.

Respect ordinary hostile populations and existing lunar/event ownership.

A siege must not accidentally produce unbounded mob accumulation.

---

# 18. Victory, Failure, and Encounter Resolution

A strategic siege requires understandable ending conditions.

## 18.1 Defensive victory

Possible conditions:

- Attacker budget exhausted.
- Tactical objectives defeated.
- Key specialist attackers eliminated.
- Encounter deadline reached with the site still defended.
- Attackers voluntarily disengage after unsuccessful approaches.

## 18.2 Attacker tactical success

An attacker may succeed at a limited objective.

Examples:

- A temporary industrial disruption.
- A supported barrier breach.
- Short-lived control of an exposed approach.
- A recoverable theft.

This does not necessarily mean the entire settlement has been destroyed or captured.

## 18.3 Player death

Define a safe, consistent resolution policy.

Do not continue causing escalating offscreen damage after the player dies.

## 18.4 Player retreat

A site should remain the conceptual target.

However, absence or unload rules must prevent unattended destructive simulation.

## 18.5 No available objective

An encounter without meaningful legal objectives should terminate or retreat.

Do not treat the inability to penetrate a strong fortification as an AI failure that must be corrected by cheating.

## 18.6 Recovery

After resolution, temporary state must clean up.

Preserve a documented cooldown and historical outcome.

---

# 19. Siege Outcomes and Persistent History

A strategic encounter should leave limited useful historical information.

Potential retained fields:

- Site ID.
- Encounter identity.
- Start and end cause.
- Tactical profile.
- Actual attackers inserted.
- Actual objectives attempted.
- Objectives completed.
- Structural changes caused by the siege.
- Final outcome.
- Cooldown.

Avoid unbounded historical logs.

The system should preserve enough evidence for diagnostics and future balancing.

Do not introduce site-level permanent damage or ownership states without explicit product approval.

---

# 20. Saved Worlds and Lifecycle

## 20.1 Persistence

Save authoritative encounter identity, target site, admission key, consumed event state and necessary bounded progress.

## 20.2 Reload

Do not duplicate a consumed lunar event or resurrect an already resolved siege.

Define whether a paused siege resumes or terminates after reopening.

Prefer a conservative policy for the first release.

## 20.3 Chunk unloading

Do not force-load a site to keep an attack active.

Unload must not cause further destructive actions.

## 20.4 Player dimension changes

A siege must not continue producing unobserved destruction in an unavailable dimension.

## 20.5 Peaceful difficulty

Terminate or suppress hostile siege behavior safely.

## 20.6 Missing site

If the recognized site becomes unavailable, uncertain, retired or invalid, handle the event without crashes or arbitrary retargeting.

## 20.7 Site expansion

Changes in settlement boundaries should not instantly redirect attackers to unrelated locations.

Use stable target snapshots with appropriately bounded revalidation.

## 20.8 Existing saves

Older worlds without siege records must remain compatible.

No migration should overwrite existing Territorial Activity, lunar or settlement data.

---

# 21. Runtime Architecture

The implementation should respect the existing authority boundaries.

### LunarService

Owns lunar timing and event windows.

Does not recognize settlements.

### Settlement Recognition

Owns site identities and read-only site snapshots.

Does not schedule attacks.

### Territorial Activity and Threat Assessment

Provide bounded environmental and activity context.

Do not dictate tactical objectives.

### Encounter / Siege Director

Owns event admission, budgets, target selection, lifecycle and resolution.

### Tactical Planner

Selects bounded objectives and candidate approaches from an authoritative site snapshot.

Does not perform unrestricted world mutation.

### Spawn Controller

Validates loaded-world placement, collision, terrain, light exceptions, population limits and owned encounter budgets.

### Entity AI

Executes actual attacks and movement.

Does not recalculate settlement identity.

### Industrial Adapters

Own narrow, tested interference contracts.

Preserve mod-specific inventory, fluid and energy behavior.

### Local debug admission (alpha.2)

The non-pausing Site Siege monitor queues a world-bound request for the owning simulation tick. `MANUAL_DEBUG` uses the same site query, approach validation, allocation and execution as `NATURAL`. It bypasses the natural toggle, dusk, moon phase, rarity, cooldown and hostile light checks. Debug attackers are transient and removed by encounter ownership on termination. Peaceful, loaded geometry, local population and Protected Interior remain admission constraints. A natural boundary already awaiting admission has priority over a queued debug request. A boundary arriving while a debug encounter is planning or active passes without a major encounter and is never queued for later admission. Debug admission and resolution write no assault or site cooldown records.

### Debug and Dev Bridge

Observe the authoritative simulation.

Do not become a second gameplay director.

Avoid speculative frameworks.

Build only abstractions supported by a working gameplay scenario.

---

# 22. Diagnostics — SITE_SIEGE Monitor

Extend the ZTO Debug Hub.

Provide a dedicated siege monitor.

Alpha.2 adds Call Site Siege, Cancel Test Siege and an AUTO, PERIMETER or BREACH selector. The overview displays request and tactical states, origin, site ID and coverage, objective, approach and both rejection and resolution. A details view shows planning work and actual changes. The screen remains non-pausing at the legacy GUI scales.

Useful information includes:

- Event kind.
- Event eligibility.
- Target site ID.
- Target site status and confidence.
- Lunar admission cause.
- Territorial assessment.
- Tactical profile.
- Chosen approach.
- Spawn candidates and rejection reasons.
- Active attackers.
- Planned and consumed budgets.
- Assigned tactical objectives.
- Objective progress.
- Actual structural changes.
- Active sabotage leases.
- Remaining encounter time.
- Completion or abort reason.
- Site cooldown.

Keep normal gameplay feedback separate from debug instrumentation.

The player should not need a debug overlay to understand that an attack is occurring.

However, development tests must be able to explain every significant admission and rejection.

---

# 23. Player-Facing Atmosphere

SITE_SIEGE should feel different from an ordinary hostile night.

Potential atmospheric elements:

- Approaching enemy activity outside a settlement.
- Distant Skelebird calls.
- Brief unusual hostile reconnaissance.
- A visible group assembling outside a defended approach.
- A Saboteur examining exposed machinery.
- A Tank moving toward a vulnerable wall.
- A pause before the first major attack.

These elements should arise through actual encounter behavior.

Avoid obligatory announcements such as:

"Your base is under siege!"

A restrained notification may be evaluated if necessary for readability, but must not be the primary source of atmosphere.

Use sounds, movements, silhouettes and world interaction wherever possible.

---

# 24. Abuse and Edge Cases

Test and prevent:

- Repeatedly triggering sieges by walking between sites.
- Infinite hostile spawning after reaching population limits.
- Duplicate event admission after Save/Quit.
- Multiple siege directors targeting the same site.
- Automatic destruction when the site is unloaded.
- Attacks originating inside a protected room.
- Unreachable attackers permanently blocking resolution.
- Fake route completion through unloaded chunks.
- Unsupported machine removal.
- Inventory or fluid duplication.
- Permanent industrial interruption.
- Repeated breach damage beyond its budget.
- Uncontrolled overlaps with lunar PLAYER_ASSAULT.
- Wrong site selection after a boundary change.
- Abuse of low-confidence settlement records.
- Missing or corrupted settlement data.
- Excessive performance cost in large factories.

---

# 25. Proposed Development Campaign

Implement the milestone in several substantial iterations.

## 0.4.0-alpha.1 — First True Site Siege

### 0.4.0-alpha.1 — Approved Initial Implementation Decisions

The sole objective is `PERIMETER_APPROACH`. A small group of existing non-destructive hostiles inserts into verified loaded terrain outside the envelope and physically walks toward reachable site frontage. Solid obstacles and entrances affect route feasibility. The frontage remains site-anchored when the player moves. Ordinary nearby combat may take over after observable approach progress. Missing routes trigger bounded failure, retreat or resolution.

Keep a finite group allocation, deadline and retry allowance. Resolve on elimination, exhausted allocation, loss of route, deadline, player absence, world or dimension invalidation, Peaceful, unavailable site identity or geometry, interrupted reload, or lunar cancellation and resynchronization as applicable. Release ordinary mobs to their native lifecycle on resolution, stop reinforcement and drop encounter references. Reopen interrupted saves without restarting the encounter.

Alpha.1 adds no structural damage, block breaking, machinery sabotage, resource mutation, siege-exclusive creatures, compulsory defenses, or ownership mechanics. Later alpha plans and open owner decisions remain proposals. Range, rarity and encounter pressure require owner playtesting.

## 0.4.0-alpha.2 — Approved Tactical Objectives and Manual Testing

One site-bound group owns one objective. A level exterior lane can lead to an open entrance, accessible frontage, one closed vanilla wooden door or fence gate, or one verified structural obstruction. Prefer open access in AUTO, followed by eligible entrance pressure and opt-in structural pressure, then frontage. PERIMETER limits selection to open access and frontage. BREACH requires a verified specialist target and respects the same structural opt-in as natural admission.

Keep the alpha.1 allocation of up to three attackers and seven insertion attempts. Use at most one Breacher or Tank in that allocation. Use the existing entrance transition and Tank material/drop rules. Allow one bounded, interruptible windup at the designated obstruction. A successful impact can open one entrance or remove up to two eligible vanilla structural blocks. Furniture, modded blocks, TileEntities, inventory and machinery are outside the structural target set. Use loaded, dimension-appropriate collision checks and preserve native movement. A surviving wall may end the objective at the exterior.

A clipped footprint can admit a manual perimeter test when the insertion is outside the recorded envelope and the approach has additional verified, loaded clearance outside the insertion. Native specialist targets require a resolved perimeter. Natural admission continues to defer clipped perimeters.

The monitor displays Call Site Siege, Cancel Test Siege, profile selection, origin, request state, site ID and coverage, approach, objective phase, attacker accounting and reasons. Manual tests retain the same recognized site identity across attempts. Disabling the experimental natural toggle still permits manual admission. The owner tests through the normal Debug Hub without changing world time. Keep the release Draft pending graphical and gameplay acceptance.

## 0.4.0-alpha.3 — Industrial Infiltration

**Goal:** Make the Saboteur a meaningful site-siege specialist.

Deliver:

- Selection of supported industrial targets.
- Saboteur assignment.
- Limited coordinated diversion.
- Verified reversible industrial disruption.
- Protection through enclosing infrastructure.
- Conservation and save/reload tests.

Do not expand third-party support beyond verified safe integrations without a separate compatibility implementation.

## 0.4.0-alpha.4 — Combined Arms and Fortress Warfare

**Goal:** Produce mature, readable siege encounters.

Deliver:

- Bounded combinations of existing mob roles.
- Optional limited aerial pressure.
- Better approach selection.
- Tactical progression.
- Retreat and resolution.
- Improved atmospheric signaling.
- Balanced Hard difficulty.

Avoid excessive attack counts or permanent harassment.

## 0.4.0 — Integration and Acceptance

Run a dedicated comprehensive campaign.

Evaluate varied settlements, technologies, attacker compositions, lifecycle behavior, performance and player experience.

Do not declare completion solely because the automated fixtures pass.

---

# 26. Acceptance Test Matrix

## Scenario A — Unfortified Cabin

An ordinary small home is recognized.

Expected:

A limited siege can approach externally without immediately overwhelming an early-game player.

## Scenario B — Walled Castle

The player builds substantial walls and a defended entrance.

Expected:

Ordinary attackers respect barriers.

Specialized attackers may attempt bounded legal objectives.

The walls provide meaningful benefit.

## Scenario C — Underground Bunker

The site is mostly underground.

Expected:

The siege uses eligible exterior approaches rather than spawning inside the protected interior.

## Scenario D — Open Industrial Facility

The site has exposed supported machinery.

Expected:

A Saboteur may be assigned a real industrial objective.

The operation remains reversible.

## Scenario E — Enclosed Factory

The same industrial equipment is protected behind solid barriers.

Expected:

Physical protection prevents invalid immediate sabotage access.

## Scenario F — Elevated Treehouse

The site has unusual vertical geometry.

Expected:

Attacker placement and movement remain physically valid.

No arbitrary teleportation into the site.

## Scenario G — Mountain Fortress

The site lies on irregular elevated terrain.

Expected:

Approach selection respects cliffs and loaded-world geometry.

## Scenario H — Dispersed Multi-Building Settlement

Several buildings share one recognized site identity.

Expected:

The siege targets the site coherently without treating each room as an independent settlement.

## Scenario I — Two Nearby Outposts

Two sites exist close to one another.

Expected:

The director selects one stable target.

No accidental dual siege or retargeting.

## Scenario J — Player Leaves

The player retreats or travels elsewhere.

Expected:

The site's identity remains unchanged.

Encounter lifecycle follows the defined absence policy.

No arbitrary offscreen destruction.

## Scenario K — Save During Siege

Save and reopen during an active encounter.

Expected:

No duplicate activation, stale sabotage lease or inconsistent population budget.

## Scenario L — Full Moon Collision

A site siege becomes eligible when a PLAYER_ASSAULT might also occur.

Expected:

Explicit arbitration prevents uncontrolled overlapping major encounters.

## Scenario M — Strong Late-Game Player

A player uses advanced Tekkit equipment and effective fortifications.

Expected:

Technology provides genuine dominance over appropriate encounters.

The siege does not artificially neutralize every advantage.

## Scenario N — Unsupported Machines

A Saboteur reaches an unverified industrial component.

Expected:

No destructive fallback.

Unsupported targets remain safe.

## Scenario O — No Valid Attack Route

A site is exceptionally well protected.

Expected:

The event can fail to achieve its tactical objective without cheating or endless retries.

---

# 27. Performance and Compatibility Requirements

The milestone must verify:

1. Bounded tactical planning cost.
2. No forced chunk loading.
3. No unbounded pathfinding.
4. Controlled spawn attempts.
5. Controlled active populations.
6. Stable site identity consumption.
7. Correct event replay protection.
8. Safe Save/Quit behavior.
9. Safe dimension and difficulty transitions.
10. Industrial resource conservation.
11. No unintended TileEntity destruction.
12. No permanent stale sabotage.
13. Correct coexistence with Territorial Activity.
14. Correct coexistence with Fugl and Skelebird.
15. Correct coexistence with natural Enderkin spawning.
16. Correct coexistence with lunar event authority.
17. Stable performance in large modded factories.

Keep the existing recognition and protection cap of 169 block/metadata reads per service interval. Alpha.1 uses a separate measurable planner budget for bounded candidates, route observations and retries, tracks observation age and revalidates before action. Report work and rejection reasons. Access live world data only on the owning simulation thread. Limit simultaneous groups and avoid per-tick global site scans.

Use the pinned Minecraft 1.2.5 / Tekkit Classic 3.1.2 runtime and relevant legacy build tooling.

Distinguish portable tests, compilation, headless runtime observations and owner gameplay acceptance.

Respect established graphical input-isolation restrictions.

---

# 28. Owner Gameplay Acceptance

A true fortress warfare update cannot be validated through code assertions alone.

The project owner should evaluate whether:

- A siege feels different from an ordinary hostile encounter.
- Attackers visibly recognize and approach the settlement.
- Defensive architecture changes encounter outcomes.
- Walls and entrances genuinely help.
- Saboteurs have meaningful but recoverable objectives.
- Tanks feel dangerous without destroying everything.
- Aerial creatures improve the encounter rather than create excessive pressure.
- Different base designs produce different tactical situations.
- The player can retreat, repair and recover.
- Advanced technology remains satisfying.
- Calm periods remain significant.
- The encounter creates reasons to improve the base rather than merely increase damage output.

The system must be evaluated in actual survival worlds, not just idealized automated test structures.

---

# 29. Explicitly Deferred Features

Survival 0.4 does not automatically include:

- Full autonomous faction warfare.
- Persistent hostile armies.
- NPC kingdoms.
- Permanent enemy conquest of settlements.
- Mandatory settlement-management interfaces.
- Unlimited industrial sabotage.
- Fully destructible bases.
- Universal block-breaking AI.
- Complete RTS-style pathfinding.
- Offline siege simulation of unloaded bases.
- Mandatory defensive technology.
- A large catalog of new siege-exclusive mobs.
- A new economy or siege currency.
- Global settlement rankings.
- Major biome generation changes.
- Rebalanced EE2 progression.
- New underwater or End dimensions.

These require separate design decisions.

---

# 30. Open Design Decisions

## Siege timing

- Should SITE_SIEGE initially occur only during selected full moons?
- Should later versions allow rarer non-lunar strategic attacks?
- What minimum settlement maturity should be required?
- How long should a site remain safe after a siege?

## Player absence

- Should a siege stop, suspend or withdraw when the player leaves the loaded area?
- Should the player receive any lasting consequence for retreat?
- Can a siege remain pending for a future visit without becoming unfair?

## Tactical objectives

- Which objective profiles should be implemented first?
- How much coordination is genuinely useful?
- Should multiple objectives occur sequentially or simultaneously?
- How should attackers react to newly built defensive obstacles?

## Structural consequences

- Which blocks may be damaged?
- How much damage is acceptable per siege?
- How should dropped blocks be recovered?
- Should stronger building materials receive special protection?
- What constitutes an attacker tactical victory?

## Industrial interference

- Which supported endpoints are valuable enough to attract Saboteurs?
- Should target selection consider production activity?
- How should exposed wiring and pipes be represented?
- What is the appropriate maximum disruption duration?

## Player feedback

- How should a siege be foreshadowed without obvious quest-like announcements?
- What sounds or behaviors communicate enemy intent?
- Should any optional defensive alarm devices exist?

## Progression

- Should certain advanced tactical compositions require demonstrated regional danger?
- How should Easy, Normal and Hard differ beyond numbers?
- Should late-game players receive different kinds of challenges rather than stronger enemies?

These decisions require playtesting and owner approval.

---

# 31. Milestone Definition of Done

Survival 0.4.0 is complete when:

1. SITE_SIEGE exists as a distinct encounter type.
2. It targets a persistent recognized settlement identity.
3. It does not simply follow the player's position.
4. Admission uses existing scheduling and bounded threat principles.
5. Encounter targets and approaches are physically valid.
6. At least several meaningful tactical objectives function in gameplay.
7. Defenses and terrain visibly affect the encounter.
8. Existing specialized mobs contribute their established roles.
9. Industrial interference remains bounded and reversible.
10. Structural damage is limited and recoverable.
11. Site and encounter lifecycle are safe across world changes.
12. PLAYER_ASSAULT remains independently functional.
13. Population, performance and persistence budgets are respected.
14. Debugging explains target, objective and encounter decisions.
15. The player can respond through combat, engineering, equipment or retreat.
16. Actual owner gameplay confirms that sieges are atmospheric, understandable and tactically meaningful.

---

# 32. Final Creative Direction

Survival 0.3 teaches the world to recognize the places the player has created.

Survival 0.4 teaches hostile forces to interact with those places.

A wooden cabin may face a limited attack from the surrounding wilderness.

A castle may force enemies toward defended entrances.

An underground bunker may protect its inhabitants while leaving the exterior vulnerable.

An exposed industrial facility may attract a Saboteur seeking an accessible transport or energy endpoint.

A high mountain fortress may gain protection from terrain while facing occasional aerial pressure.

The same hostile event should not play identically against all of these places.

Player engineering should matter.

Preparation should matter.

Technology should matter.

A well-defended site should sometimes successfully repel an attack before attackers achieve anything significant.

**The purpose of fortress warfare is not to destroy the player's constructions. It is to give those constructions strategic meaning.**

A successful siege system should make the player want to build a better fortress, not regret having built one.

**Survival 0.4.0 — SITE_SIEGE & Fortress Warfare: The world recognizes what you built, and hostile forces finally have a reason to care.**
