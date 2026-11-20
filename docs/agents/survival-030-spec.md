# ZedsTekkitOverhaul — Survival 0.3.0
## Settlement Recognition & World Provenance

**Document type:** Major Gameplay Systems & Technical Design Specification  
**Status:** Design Draft — Pending Owner Approval  
**Target:** Minecraft Java 1.2.5 / Tekkit Classic 3.1.2  
**Strategic milestone:** 0.3 — Settlement Recognition  
**Next strategic milestone:** 0.4 — SITE_SIEGE & Fortress Warfare  
**Suggested repository path:** `docs/agents/survival-030-settlement-recognition-spec.md`

---

# 1. Executive Vision

Survival 0.3.0 introduces a major foundational capability:

**ZedsTekkitOverhaul must learn to recognize the places players build, inhabit, modify, maintain, and use.**

Until now, ZTO has primarily understood the world through player position, local activity, environmental danger, hostile populations, and scheduled encounters.

These systems are sufficient for localized survival pressure.

They are not sufficient for strategic threats against player-built locations.

A player may construct a fortified home, an underground laboratory, a large industrial complex, a remote mining outpost, or a group of connected buildings.

Currently, the world has no authoritative understanding that these structures represent meaningful player settlements.

Survival 0.3.0 must establish that understanding.

The system should recognize settlements and outposts without requiring prescribed construction patterns, ownership markers, special blocks, settlement claims, or mandatory technology.

It should use a combination of available evidence:

- Player-authored modifications.
- Construction and removal history.
- Observed automation-related changes.
- Sustained or repeated activity.
- Spatial organization.
- Physical structures.
- Functional use.
- Relevant local environmental context.

No single signal should automatically define a settlement.

The result should be a persistent, bounded, inspectable representation of meaningful player-built sites.

This infrastructure will become a foundation for future strategic gameplay.

In particular, **Survival 0.4 will consume recognized settlement identities to implement SITE_SIEGE**.

Survival 0.3.0 must not implement SITE_SIEGE itself.

## Core design statement

**The world should understand where the player has established meaningful places, without requiring the player to formally declare what a base is.**

---

# 2. Why This Milestone Matters

ZTO's survival systems have already introduced:

- Territorial Activity.
- Local threat assessments.
- Enhanced ordinary hostile populations.
- Enderkin ecology.
- Lunar patrols and assaults.
- Aerial predators.
- Weather-dependent mobility constraints.
- Biome-dependent encounter composition.

These systems have increased the world's hostility and dynamism.

However, most encounters remain fundamentally player-centric.

The world reacts to the player's presence.

It does not yet reliably understand the player's constructions.

This creates an architectural limitation.

For example:

- An assault cannot independently target an established industrial outpost.
- The world cannot meaningfully distinguish a traveling player from an inhabited base.
- Hostile behavior cannot consistently reason about a site's perimeter.
- The strategic value of walls, access routes and protected interiors cannot be evaluated through a unified system.
- The future Saboteur cannot be assigned a site-specific tactical objective without another system first identifying the site.
- The game cannot distinguish an established settlement from a temporarily visited location through construction evidence alone.

Settlement Recognition is therefore not merely another feature.

It is an enabling system for the strategic evolution of ZTO.

It should be designed to support future mechanics without implementing them prematurely.

---

# 3. Alignment with PROJECT_VISION.md

This milestone implements the strategic role assigned to Survival 0.3 in the project charter.

The existing architectural distinction must be preserved.

### Territorial Activity

Measures activity in the world.

It does not decide whether an area is a settlement.

### Threat Assessment

Evaluates bounded local danger and activity.

It must not automatically treat wealth, industrial scale or settlement size as a reason to punish the player.

### Settlement Recognition

Interprets evidence of construction, modification, occupancy and spatial organization.

It identifies meaningful sites and describes their current properties.

### Protected Interior

Describes the protective characteristics of local spaces.

A protected interior is not necessarily a settlement.

A settlement is not necessarily enclosed.

### Encounter Director

Consumes independently established information to select and manage encounters.

It must not become the authority for recognizing settlements.

### SITE_SIEGE

A future encounter type targeting a recognized persistent site.

It belongs to Survival 0.4.

It must remain distinct from the existing PLAYER_ASSAULT.

---

# 4. Product Principles

## 4.1 No prescribed settlement shape

A settlement must not require a particular layout.

Valid candidates include:

- Wooden houses.
- Stone castles.
- Underground bunkers.
- Converted caves.
- Treehouses.
- Cliffside buildings.
- Open industrial compounds.
- Walled villages constructed by the player.
- Small farms.
- Remote mining outposts.
- Distributed industrial facilities.
- Connected clusters of buildings.
- Minimalist shelters with sustained genuine use.

The system must not assume that a base is a rectangular building with four walls, a roof and a door.

## 4.2 No mandatory declaration

The player must not need to place a special settlement marker.

Beds must not be mandatory.

Doors must not be mandatory.

Industrial machinery must not be mandatory.

The player should not need to construct a specific multiblock to establish site identity.

Optional diagnostic or administrative controls may exist for development, but they must not be required in ordinary survival.

## 4.3 Natural discovery

Settlement identity should emerge from player interaction with the world.

Building, modifying, using and revisiting an area should gradually provide evidence that the location is meaningful.

Recognition should not be instantaneous after placing one block.

Likewise, a genuine established home should not lose its identity merely because the player travels away temporarily.

## 4.4 Construction quality is not wealth

A large factory must not automatically generate more hostility than a small home.

Settlement recognition must not become a mechanism for taxing technological progress.

The purpose is to understand locations, not punish prosperity.

## 4.5 Unknown is a valid state

Minecraft 1.2.5 does not provide perfect historical block provenance.

The system must explicitly support unknown provenance and incomplete observations.

An old construction may be recognizable through present-day evidence without possessing a complete construction history.

## 4.6 Conservative classification

False positive settlement recognition can become especially damaging once strategic sieges exist.

The system should favor understandable, confidence-bearing assessments rather than making unsupported binary assertions.

## 4.7 No universal safety guarantees

Recognizing a settlement does not automatically make its entire footprint protected.

Protected interiors, exposed courtyards, open factories and naturally occurring caves require separate interpretation.

## 4.8 Bounded performance

Every observation, classification, persistence operation and structural analysis must operate within documented limits.

Never load new chunks solely to recognize settlements.

---

# 5. Fundamental Domain Concepts

The implementation should distinguish several related but independent concepts.

## 5.1 World Modification Evidence

Evidence that a location has been changed by a player or an observable process.

Examples:

- A block placed by a player.
- A block removed by a player.
- A terrain modification attributable to an observed automation process.
- Repeated changes to a localized construction area.

This evidence has provenance and reliability.

It is not automatically settlement evidence.

## 5.2 Local Activity

Evidence from the existing Territorial Activity system.

Repeated visits and sustained presence may support settlement recognition.

However, activity alone is insufficient.

A player mining repeatedly in the same cave does not necessarily establish a settlement.

## 5.3 Structural Evidence

Evidence derived from the currently observable world.

Potential signals include:

- Persistent artificial block arrangements.
- Concentrated construction.
- Working spaces.
- Organized boundaries.
- Shelter-like geometry.
- Storage or utility placement.
- Concentrated modifications across multiple neighboring cells.

These signals require careful interpretation.

Do not classify every cave, village or dungeon as player-built.

## 5.4 Functional Evidence

Evidence that an area is used as a location rather than merely traversed.

Examples may include:

- Repeated interaction with storage.
- Crafting activity.
- Machine interaction.
- Sustained construction.
- Repeated visits to a consistent work area.

This is supporting evidence, not a universal requirement.

Do not require any particular machine, crafting block or storage container.

The current implementation uses the same native evidence path across the Tekkit block registry. Matching placement can establish unfamiliar construction. Loaded tiles, nonempty native inventory capacity and successful local activations can corroborate a work area. Optional mod knowledge can verify a specific relationship such as compatible IC2 wiring. Every equipment relationship stays local to the surveyed construction. The recognition result supplies neither shelter geometry nor a measure of industrial wealth. [Implemented evidence contract](settlement-generic-modded-evidence.md)

## 5.5 Settlement Candidate

A bounded spatial hypothesis that a particular area may represent a player-established site.

A candidate is not automatically a confirmed settlement.

It may have incomplete evidence and uncertain boundaries.

## 5.6 Settlement Identity

A durable identity assigned to a sufficiently supported settlement candidate.

It should remain stable across ordinary save/reload and modest structural changes.

## 5.7 Outpost

A smaller or less developed but persistent player-established location.

The distinction between settlement and outpost should describe character and evidence, not merely prestige or an arbitrary size threshold.

The exact classification rules require playtesting.

## 5.8 Protected Interior

A local spatial assessment of enclosure and protection.

It is independent of the settlement's identity.

## 5.9 Site Snapshot

A bounded, immutable representation of a recognized site's current state.

This is the intended future integration boundary for SITE_SIEGE.

---

# 6. World Provenance — Foundational Requirements

**This is one of the most technically significant parts of Survival 0.3.0.**

## 6.1 Objective

Introduce a reliable, bounded way of preserving evidence that parts of the world have been deliberately modified.

Minecraft 1.2.5 does not maintain universal historical ownership information for every block.

A new ZTO system is required.

## 6.2 Provenance categories

The system should conceptually distinguish:

**Observed manual placement**

A qualifying block placement associated with a real player action.

**Observed manual removal**

A qualifying removal associated with a real player action.

**Observed automation modification**

A change associated with supported automation, when the installed legacy integration can identify the cause reliably.

**Observed non-player modification**

A change caused by an identifiable non-player process.

**Unattributed modification**

A change was observed but its cause cannot reliably be established.

**Unknown historical state**

No reliable provenance information exists.

These are evidence categories, not necessarily a requirement to store a separate record for every block.

The implementation may use a compact representation.

## 6.3 Important negative rule

**An absent provenance record does not imply that a block is natural.**

This rule must hold throughout classification.

Old saves may contain extensive player structures constructed before installation of the new system.

Do not misclassify them as untouched natural terrain merely because their construction events were not observed.

## 6.4 Observation sources

Investigate the available legacy integration points for:

- Player block placement.
- Player block breaking.
- Tool-assisted changes.
- Explosions.
- Entity-caused modification.
- Piston operations.
- BuildCraft builders and quarry operations.
- RedPower machinery and automated construction.
- Relevant ZTO machine actions.

Not all sources need to be supported immediately.

Every adapter must be justified by actual installed runtime behavior.

Do not assume one vanilla block-change hook automatically captures all changes from third-party mods.

## 6.5 Attribution confidence

Distinguish directly observed events from inferred ones.

For example:

- A player placing a block may provide strong authorship evidence.
- An unknown block that appears inside an already-established building provides weaker contextual evidence.
- A naturally generated structure provides no direct evidence of player authorship.

Avoid turning inference into false certainty.

## 6.6 Removal and replacement

A provenance system must handle changes over time.

Consider:

- Placing and removing a block.
- Replacing stone with machinery.
- Removing a previously observed machine.
- Destroying and rebuilding part of a wall.
- Repeatedly replacing the same block.
- Automating placement and removal.
- Explosions affecting observed constructions.

Avoid permanently increasing a construction score when the same block is repeatedly placed and removed.

## 6.7 Compact representation

Do not store an unlimited historical event log.

Investigate efficient approaches such as:

- Aggregated spatial modification counters.
- Sparse modified-block evidence.
- Region or section summaries.
- Compact confidence-bearing records.
- Hybrid approaches combining coarse activity with sparse precise observations.

Compare alternatives using real memory, storage and runtime measurements.

Select the smallest representation that supports actual settlement recognition.

## 6.8 Persistence

Provenance must survive save/reload where meaningful.

Use a versioned, bounded format compatible with the actual legacy save architecture.

Do not overwrite the existing Territorial Activity data.

## 6.9 Failure behavior

When provenance storage is unavailable or corrupted:

- Avoid crashes.
- Preserve the original world.
- Report the failure through diagnostics.
- Avoid fabricating confident site classifications.
- Degrade gracefully using other supported evidence where appropriate.

---

# 7. Spatial Settlement Detection

## 7.1 Objective

Identify meaningful localized concentrations of construction and use.

The system should detect spatial relationships rather than simply counting placed blocks globally.

## 7.2 Candidate generation

Potential candidate evidence includes:

- Concentrated sustained modifications.
- Repeated use in a localized area.
- Observed structural development.
- Clusters of functional activity.
- Multiple nearby construction signals.

A settlement candidate should emerge only when enough independent evidence is present.

Do not identify every isolated bridge, staircase, torch or mining shaft as a settlement.

## 7.3 Spatial scale

Minecraft constructions vary substantially in size.

A single fixed-size cell cannot fully represent:

- A tiny shelter.
- A large industrial compound.
- A sprawling castle.
- Multiple adjacent farms.
- Underground rooms.
- A remote outpost.

Investigate multiscale or clustered spatial representations.

Potential approaches include:

- Fixed coarse observation cells.
- Sparse fine-grained structural evidence.
- Connected candidate clusters.
- Localized site footprints.

Avoid assuming every structure must fit inside one 64-block territorial cell.

The existing Territorial Activity grid can provide supporting signals, but does not define site boundaries.

## 7.4 Three-dimensional structure

Settlement recognition must account for Y-coordinate.

Two structures at the same X/Z location but different heights should not automatically become one site.

Examples include:

- A surface house above a deep mine.
- A multi-level bunker.
- A mountain settlement.
- A sky platform.
- A factory built beneath an ordinary building.

The system should evaluate meaningful vertical relationships.

Do not create a prohibitively expensive full voxel map.

## 7.5 Connected and disconnected buildings

A settlement may contain several structures separated by courtyards, roads, fields or short distances.

Do not require every building to touch another.

However, do not merge independent outposts merely because they happen to lie within the same broad geographic cell.

Use multiple signals to decide whether nearby construction clusters belong to one site.

## 7.6 Natural versus artificial terrain

A naturally occurring cave may be converted into a base.

Its original rock geometry must not prevent recognition.

Likewise, a generated village or dungeon must not automatically be assigned player-built ownership.

Observed modifications and functional activity should influence interpretation.

## 7.7 Conservative boundaries

A site's boundaries need not exactly match every wall.

An approximate footprint is acceptable if it is:

- Consistent.
- Stable.
- Useful.
- Inspectable.
- Supported by observed evidence.

Avoid creating huge circular site boundaries around a single storage chest.

## 7.8 Confidence

A candidate should carry a confidence assessment.

Confidence should reflect supporting evidence and uncertainty.

It must not be a disguised arbitrary difficulty rating.

---

# 8. Site Identity and Lifecycle

**Recognition is a continuous process, not a one-time classification.**

## 8.1 Candidate formation

A site begins as a candidate when construction and activity evidence become sufficiently concentrated.

Candidate formation should be gradual and resistant to trivial single-action triggers.

## 8.2 Promotion

A candidate may become a recognized outpost or settlement after sufficient evidence accumulates.

Do not require a universal mandatory waiting time if physical and functional evidence already provides strong support.

## 8.3 Stable identity

Recognized sites should receive a durable identity.

The identity must remain stable across:

- Save/reload.
- Short player absences.
- Ordinary building modifications.
- Moderate boundary changes.
- Local activity decay.

Do not generate a new site identity every time the boundary estimate changes.

## 8.4 Expansion

If the player adds new rooms, buildings, farms or machinery near a recognized site, the system should be capable of expanding the represented footprint.

Expansion must be bounded and based on evidence.

## 8.5 Contraction

If significant parts of a site are removed, the system should eventually reflect the change.

Do not immediately delete an entire settlement because a wall is demolished.

## 8.6 Abandonment

Distinguish temporary inactivity from abandonment.

A player traveling for many Minecraft days should not cause a long-established home to instantly disappear from the settlement registry.

A site may become dormant while preserving identity.

## 8.7 Reoccupation

If a dormant site is used again, recognition should resume without arbitrary loss of historical context.

## 8.8 Site merging

Nearby candidates may eventually form one larger settlement.

Handle merging deliberately.

Avoid duplicate identities and conflicting ownership records.

Preserve an understandable history or stable identity policy.

## 8.9 Site splitting

A large site may become two separate locations after significant structural changes.

Splitting should be rare, evidence-driven and stable.

Do not implement fragile automatic splitting that oscillates whenever a connecting bridge changes.

## 8.10 Site retirement

A site may be retired when evidence convincingly indicates that it no longer exists or has been dismantled.

Retirement must not corrupt or remove ordinary world blocks.

Recognition is descriptive, not an ownership claim system.

---

# 9. Settlement, Outpost, and Functional Character

## 9.1 Avoid mandatory building categories

The first system does not need to determine a perfect semantic label for every building.

Its primary responsibility is recognizing meaningful persistent sites.

## 9.2 Useful descriptive properties

Future systems may benefit from knowing whether a site contains evidence of:

- Dwelling.
- Storage.
- Industry.
- Farming.
- Underground construction.
- Defensive structures.
- Open-air facilities.
- Multiple connected buildings.

These are optional descriptive signals.

They should not be treated as mandatory settlement prerequisites.

## 9.3 Industrial sites

An automated factory may represent a meaningful outpost even without beds or a conventional house.

The classifier should support this.

However, the number or value of machines must not automatically determine settlement importance or threat.

## 9.4 Minimalist settlements

A small, frequently used cabin may deserve recognition.

Avoid a classifier that requires enormous block counts or technological equipment.

## 9.5 Temporary work sites

A quarry, excavation or temporary construction zone may have substantial modification activity.

That does not automatically make it a settlement.

The system should distinguish excavation activity from habitation or persistent functional use as far as the available evidence allows.

## 9.6 Descriptive classification versus tactical value

A recognized settlement is not automatically an eligible siege target.

The future SITE_SIEGE system will need its own event eligibility, tactical budget and safety rules.

Do not encode those decisions into Settlement Recognition.

---

# 10. Protected Interior — Separate Spatial Assessment

## 10.1 Objective

Develop the foundation for recognizing locally protected spaces.

This is related to but distinct from identifying settlements.

## 10.2 Core distinction

A shelter may exist outside a settlement.

A recognized settlement may contain exposed areas.

Examples:

- A cave can provide cover but contain no player-built settlement.
- A walled farm may be a settlement component while remaining open to aerial threats.
- A bunker may contain protected rooms connected to exposed entrances.
- A house with its roof removed is still part of a settlement, but no longer offers the same overhead protection.

## 10.3 Signals

Potential protection evidence includes:

- Solid overhead cover.
- Enclosure.
- Openings.
- Passable entrances.
- Accessible exterior paths.
- Vertical access.
- Local collision geometry.
- Player modifications supporting a structure.

Do not assume torch lighting alone makes a space protected.

## 10.4 Underground environments

Do not treat every natural cave as a player-built protected interior.

However, a converted cave should remain a legitimate settlement.

The protection assessment should answer a spatial question, not attempt to infer all social meaning.

## 10.5 Intended initial scope

Survival 0.3 should establish and validate a practical local protection assessment if technically feasible.

It should be independent and queryable.

Do not build a universal computationally expensive room-detection engine before proving a bounded implementation.

## 10.6 No automatic spawn immunity

Recognizing a protected interior must not instantly rewrite all vanilla and ZTO spawning rules.

Any gameplay integration should be explicitly approved and tested.

The first responsibility is accurate observation.

---

# 11. Relationship to Existing Territorial Activity

The existing Territorial Activity system is already implemented.

It records qualified presence and revisits in bounded regional cells.

Its data should remain authoritative for that purpose.

## 11.1 Reuse

Settlement Recognition may consume Territorial Activity observations or snapshots.

Do not duplicate its history.

## 11.2 Independence

A site must not be created solely from high activity.

A frequently traversed road is not automatically a settlement.

## 11.3 Decay

Activity may decay faster than persistent physical construction evidence.

These signals should not share identical lifecycle rules automatically.

## 11.4 Threat

Settlement recognition must not directly multiply hostile difficulty.

Its output should describe sites.

The existing Threat Assessment remains responsible for its own bounded danger calculation.

## 11.5 No circular authority

Avoid a design where:

- Territorial Activity calls Settlement Recognition.
- Settlement Recognition calls Threat Assessment.
- Threat Assessment calls Territorial Activity through another path.

Define explicit, one-directional dependencies.

---

# 12. Relationship to World Generation and Biome Ecology

Minecraft contains natural terrain, generated structures and biome-specific features.

These must not automatically become player settlements.

## 12.1 Generated structures

Generated villages, dungeons and other structures are not inherently player-built sites.

They may become meaningful settlements after genuine player occupation and modification.

## 12.2 Biomes

Biome identity may provide contextual information.

It must not be a hard settlement restriction.

A desert base and a forest base are equally legitimate.

## 12.3 Existing worlds

Previously built structures may lack recorded provenance.

The system should support conservative recognition using available current structural and activity evidence.

## 12.4 No world generation changes

Survival 0.3.0 does not require adding or modifying biomes.

It must not rewrite world generation.

---

# 13. Automation and Modded Construction

Tekkit Classic includes powerful world-changing systems.

A provenance implementation that only observes direct manual placement will have important blind spots.

## 13.1 Initial audit

Investigate actual mutation mechanisms in the installed pack.

Prioritize systems relevant to existing player construction workflows.

Candidate integrations include:

- BuildCraft automated builders and quarries.
- RedPower block-moving or construction machinery.
- IC2 operations that modify blocks.
- Existing ZTO machine placement or transformation behavior.

## 13.2 Controlled attribution

Do not assume all automation changes are directly attributable to the player.

An automated quarry may be operating far from the player's current position.

Record what is genuinely known.

## 13.3 Modification versus excavation

Mining out a large volume should not automatically create a major settlement.

Differentiate destruction or excavation from deliberate constructed arrangements where feasible.

## 13.4 Integration priorities

Not every legacy modded mutation pathway needs to be intercepted in the first release.

Prioritize representative, verifiable integrations.

Record unsupported pathways explicitly.

## 13.5 Compatibility

Do not interfere with native transport, energy, block updates or TileEntity lifecycle merely to observe construction.

Observation should not change the behavior of the installed mods.

## 13.6 Industrial workshop evidence

The IC2 1.97 follow-up recognizes equipment associated with surveyed construction. Successful machine GUI openings supply functional evidence. Short, locally compatible source-to-sink cable paths can corroborate a workshop with diverse equipment. Construction and established compound rules govern footprint and identity. Matching manual provenance and inferred current infrastructure remain distinct. [Implemented subset, thresholds and acceptance](settlement-industrial-recognition.md)

---

# 14. Evidence Aggregation and Classification

This section defines the conceptual recognition process.

Exact thresholds and weights remain prototype decisions.

## 14.1 Evidence categories

Evaluate a candidate using several independent dimensions:

**Authorship evidence**

Observed player-attributable construction.

**Modification evidence**

Meaningful persistent terrain changes.

**Structural evidence**

Localized built organization.

**Functional evidence**

Repeated meaningful use.

**Activity evidence**

Presence and returns over time.

**Spatial coherence**

Whether observations describe a connected or consistently associated place.

## 14.2 Avoid single-signal recognition

No single machine, bed, chest, torch or block type should universally certify a settlement.

Similarly, prolonged standing still should not automatically create one.

## 14.3 Evidence diversity

Several independent moderate signals may provide stronger evidence than one extreme signal.

A small cabin containing ordinary functional activity may be more clearly a settlement than a massive excavation.

## 14.4 Confidence and explanations

Recognition output should retain understandable reasons.

Examples:

- Repeated construction activity.
- Persistent modified structure.
- Functional use.
- Sustained revisits.
- Uncertain historical provenance.
- Insufficient spatial coherence.

These explanations are important for testing.

## 14.5 Hysteresis

Use distinct conditions for recognition and loss of recognition.

Avoid sites appearing and disappearing repeatedly near a threshold.

## 14.6 Data uncertainty

Confidence must reflect the limitations of the available observations.

Do not label old-world inferred structures as directly observed player-built constructions.

---

# 15. Persistence and Storage

## 15.1 Separate authority

Do not modify the existing Territorial Activity save schema unnecessarily.

Create a separate versioned persistence responsibility for provenance and recognized sites.

## 15.2 Bounded storage

Define explicit limits for:

- Observed spatial records.
- Candidate sites.
- Recognized sites.
- Evidence per site.
- Stored historical metadata.
- Serialization size.

The limits should be informed by measured realistic worlds.

## 15.3 Efficient updates

Avoid writing persistent world data on every observed block mutation.

Use bounded updates, dirty tracking and controlled checkpoints.

## 15.4 World ownership

Data must belong to the correct save and dimension.

Initial recognition may focus on the local singleplayer Overworld, matching the project's supported runtime.

Preserve the ability to extend the design deliberately later.

## 15.5 Migration

Previously existing saves should load safely.

Unrecognized new tags should not invalidate unrelated data.

Schema upgrades should be explicit and idempotent.

## 15.6 Corruption handling

Malformed or unsupported data must not crash the world or cause destructive behavior.

Report a clear diagnostic status.

Do not silently replace unreadable site history with fabricated certainty.

## 15.7 Stable identifiers

Persistent site identities must survive ordinary save/reload cycles.

IDs must not collide after creation, deletion, merging or restoration.

---

# 16. Runtime Architecture

The exact class architecture should emerge from repository investigation.

However, the following responsibilities should remain conceptually distinct.

## World Mutation Observation

Receives supported block-change evidence.

Does not classify settlements directly.

## Provenance Store

Maintains compact, persistent evidence of relevant world modifications.

Does not schedule encounters.

## Structural Evidence Sampler

Performs bounded observation of local loaded-world geometry.

Does not force-load chunks.

## Settlement Candidate Evaluator

Combines relevant evidence and proposes site candidates.

## Settlement Registry

Owns stable recognized identities and lifecycle.

## Protected Space Evaluator

Assesses local shelter and enclosure independently of site identity.

## Settlement Query Interface

Provides immutable, bounded observations to diagnostics and future gameplay systems.

## Debug and Testing Adapters

Read authoritative snapshots without becoming gameplay owners.

Avoid creating unnecessary abstractions for systems that have no real consumer.

Each major component must be justified through a working use case.

---

# 17. Performance Requirements

Performance is a release-blocking consideration.

## 17.1 Bounded work

Every observation process must have a controlled maximum amount of work per simulation interval.

## 17.2 No forced chunk loading

Settlement Recognition must not load arbitrary chunks to scan structures.

## 17.3 No full-world rescans

Do not periodically scan all existing chunks, blocks or TileEntities in the save.

## 17.4 Event-driven observation

Use reliable world modification events where possible.

Combine them with periodic low-frequency validation when necessary.

## 17.5 Bounded structural sampling

Sample only relevant, already-loaded regions.

Use cached evidence where appropriate.

## 17.6 Large installations

Test factories, castles, farms, mines and dispersed construction.

The system must not become dramatically more expensive simply because the player builds a larger base.

## 17.7 Stress testing

Measure tick cost, memory usage, record growth and serialization overhead in representative worlds.

Do not accept purely theoretical performance claims.

---

# 18. Debug Hub — Settlement Recognition

A major new infrastructure system requires strong observability.

Add a dedicated **Settlement Recognition** screen to the existing ZTO Debug Hub.

The interface is for development and diagnostics.

Ordinary gameplay should remain free from intrusive settlement markers or constant notifications.

## 18.1 Current location

Display:

- Current dimension.
- Current position.
- Current Territorial Activity region.
- Current settlement candidate, if any.
- Recognized site identity, if applicable.
- Relevant provenance confidence.

## 18.2 Settlement state

Display:

- Site ID.
- Classification or status.
- Confidence.
- Approximate footprint.
- Relevant vertical extent.
- Activity contribution.
- Structural evidence.
- Provenance evidence.
- Functional evidence.
- Last meaningful observation.
- Lifecycle status.

## 18.3 Classification explanations

Provide bounded reasons for:

- Candidate creation.
- Promotion.
- Rejection.
- Expansion.
- Contraction.
- Merge.
- Dormancy.
- Retirement.

## 18.4 Protected interior

If implemented, display the local protection assessment independently.

Do not report an entire settlement as universally protected.

## 18.5 Spatial visualization

A development-only visualization of recognized boundaries would be valuable.

Investigate a lightweight implementation using appropriate legacy rendering capabilities.

Keep it optional and disabled by default.

Do not make world mutation or gameplay authority dependent on the renderer.

## 18.6 Read-only by default

Normal diagnostics should not create, delete or alter settlements.

Disposable-world test controls may exist behind the established development safeguards.

---

# 19. Future 0.4 Integration Contract

**This is the central architectural deliverable of the milestone.**

At the end of 0.3, the future SITE_SIEGE system should be able to ask:

- Which recognized sites exist in the relevant loaded world context?
- Is this site currently recognized, dormant or retired?
- What is its stable identity?
- Where is its approximate footprint?
- What vertical area does it occupy?
- What evidence supports its recognition?
- Which relevant areas appear exposed or protected?
- Is the available information sufficiently reliable for a future encounter decision?
- When was the site last meaningfully observed?

The result should be bounded and immutable.

The future siege director must not need to recalculate all provenance and structural evidence itself.

### Site ownership versus siege eligibility

A recognized site is not automatically eligible for siege.

The 0.4 system will determine when strategic pressure is appropriate.

Recognition should not encode arbitrary offensive targeting decisions.

### No settlement-specific attack spawning in 0.3

Do not introduce SITE_SIEGE scheduling, enemy waves, siege targeting, destruction budgets or enemy tactical objectives during this milestone.

### No premature tactical map

Do not build an extensive siege pathfinding or fortification analysis engine before the intended consumers exist.

Represent only information reasonably justified by current settlement recognition and a minimal future-facing contract.

---

# 20. Failure Modes and Anti-Exploitation

The implementation must handle deliberate and accidental edge cases.

## 20.1 Single-block farming

Repeatedly placing and removing the same block should not create an established settlement.

## 20.2 Repetitive activity

Standing in one place indefinitely must not automatically create a settlement without supporting evidence.

## 20.3 Mining

A large excavation must not automatically be classified as an inhabited base.

## 20.4 Natural villages

Generated structures must not automatically receive player-established settlement identity.

## 20.5 Artificial platforms

Skybases must remain recognizable where genuine construction and use evidence exists.

## 20.6 Underground construction

A converted cave must remain eligible even without a traditional freestanding building.

## 20.7 Large factories

Machine count must not directly become a hostile pressure multiplier.

## 20.8 Disconnected outposts

Independent nearby sites should not be incorrectly merged solely through a coarse territorial cell.

## 20.9 Temporary absence

An established site should not disappear immediately when the player travels far away.

## 20.10 Old saves

Historical construction without provenance must not be automatically classified as natural.

## 20.11 Missing chunks

Unloaded terrain must not be interpreted as empty terrain.

## 20.12 Automation

Supported modded block modifications must not silently bypass the evidence model where reliable observation is available.

## 20.13 Unexpected interruptions

Save/Quit, crash recovery, dimension changes and partial persistence must not produce duplicate identities or corrupt site data.

---

# 21. Proposed Development Campaign

The milestone should be implemented through several focused iterations rather than one enormous PR.

## 0.3.0-alpha.1 — Provenance Foundation

Primary deliverables:

- Audit real Minecraft and modded world-mutation paths.
- Select a bounded provenance representation.
- Implement reliable observation of core manual placement and removal.
- Establish versioned persistence.
- Add initial provenance diagnostics.
- Test old-world behavior and resource bounds.

Do not attempt full settlement classification before mutation evidence works.

## 0.3.0-alpha.2 — Settlement Candidates and Identity

Primary deliverables:

- Combine provenance and Territorial Activity.
- Implement structural and functional evidence sampling.
- Create settlement candidates.
- Establish conservative recognition criteria.
- Assign stable persistent site IDs.
- Support a minimal lifecycle.
- Add classification explanations.

Test diverse building types.

## 0.3.0-alpha.3 — Spatial Refinement and Lifecycle

Primary deliverables:

- Improve site footprints.
- Support multi-building sites.
- Handle vertical construction.
- Refine expansion and contraction.
- Implement conservative dormancy and reoccupation.
- Investigate safe merging and splitting.
- Add targeted legacy automation provenance where reliable.

Avoid premature complexity unsupported by actual test cases.

## 0.3.0-alpha.4 — Protected Spaces and Site Query Contract

Primary deliverables:

- Establish a bounded local Protected Interior assessment.
- Keep protected space separate from settlement identity.
- Implement stable read-only site snapshots.
- Provide the minimum site data needed for future 0.4.
- Improve debugging and optional visualization.
- Verify no gameplay systems accidentally use settlement status as automatic spawn immunity.

## 0.3.0 — Integration and Acceptance

A dedicated campaign covering:

- Diverse constructions.
- Old saves.
- New saves.
- Modded automation.
- World lifecycle.
- Chunk availability.
- Data size.
- Tick performance.
- Recognition stability.
- False positive and false negative rates.
- Interoperability with existing survival systems.

Keep implementation evidence separate from owner gameplay observations.

---

# 22. Acceptance Test Matrix

## Scenario A — Tiny survival cabin

The player builds and uses a modest wooden shelter.

Expected:

The system gathers relevant evidence and can eventually recognize a meaningful small site.

No mandatory bed or machine is required.

## Scenario B — Underground bunker

The player converts part of a cave into a maintained base.

Expected:

The system recognizes evidence of deliberate modification and use without demanding a freestanding building.

## Scenario C — Open industrial compound

The player constructs machines and automation outdoors.

Expected:

An open-air industrial site can qualify without a conventional roof.

It is not automatically classified as a protected interior.

## Scenario D — Remote mining operation

The player excavates a large area and then leaves.

Expected:

Excavation alone should not automatically produce a high-confidence settlement.

## Scenario E — Repeated temporary visits

The player repeatedly visits an untouched location.

Expected:

Territorial Activity may rise, but settlement identity should not automatically appear.

## Scenario F — Isolated wall

The player builds a wall in an otherwise untouched region.

Expected:

Insufficient evidence for a significant persistent settlement.

## Scenario G — Multi-building base

The player builds several separate structures around a courtyard.

Expected:

The system can recognize a coherent site without requiring direct block connectivity.

## Scenario H — Separate outposts

The player constructs two independent nearby locations.

Expected:

The system avoids merging them without sufficient evidence.

## Scenario I — Skybase

The player builds an elevated platform and living area.

Expected:

Recognition does not depend on ground-level construction.

## Scenario J — Converted natural village

The player inhabits and modifies a generated village.

Expected:

The system distinguishes natural structure evidence from observed player activity.

## Scenario K — Extensive factory

The player builds a large automated facility.

Expected:

Recognition works without excessive scans or automatic threat scaling.

## Scenario L — Old existing world

Load an old save containing construction created before 0.3.

Expected:

No data corruption.

Unknown provenance remains unknown.

Conservative structural and activity evidence can still contribute to recognition.

## Scenario M — Expansion and abandonment

Expand a recognized settlement, leave for an extended period and return.

Expected:

Identity remains stable while lifecycle properties evolve appropriately.

## Scenario N — Two-dimensional overlap

Construct separate spaces at substantially different Y-levels.

Expected:

The system does not blindly conflate them because X/Z coordinates match.

## Scenario O — Block replacement exploit

Repeatedly place and remove the same blocks.

Expected:

No artificial unbounded growth in settlement confidence.

## Scenario P — Automation

Use a representative supported BuildCraft or RedPower world-modifying mechanism.

Expected:

Any claimed provenance integration is directly verified against the native runtime.

Unsupported sources remain explicitly unobserved.

## Scenario Q — Save corruption

Present malformed or unsupported ZTO settlement data in a disposable world.

Expected:

No destructive fallback or fabricated confident site state.

---

# 23. Performance and Reliability Acceptance

The complete milestone must verify:

1. Bounded observation cost.
2. Bounded storage growth.
3. Bounded site-candidate growth.
4. No forced chunk loading.
5. No accidental world generation changes.
6. Stable persistent site identity.
7. Correct dimension isolation.
8. Safe behavior in old worlds.
9. Safe behavior when provenance is unavailable.
10. No regressions in Territorial Activity.
11. No regressions in natural hostile spawning.
12. No changes to lunar event authority.
13. No accidental SITE_SIEGE activation.
14. No loss of existing player construction data.
15. Acceptable runtime cost in representative Tekkit worlds.

Automated tests should measure actual behavior rather than only verify that classes instantiate.

---

# 24. Owner Gameplay Acceptance

Automated tests cannot determine whether settlement recognition makes conceptual sense to a player.

The owner should evaluate a set of manually constructed worlds and existing saves.

The principal questions are:

- Does the system recognize an ordinary home?
- Does it recognize a small, unconventional shelter?
- Does it distinguish construction from mining?
- Does it correctly identify a remote outpost?
- Can it understand a large factory without interpreting every machine as a separate settlement?
- Are recognized boundaries plausible?
- Does identity remain stable during normal building?
- Does the debug interface explain its decisions?
- Does the system avoid creating invisible mandatory settlement rules?
- Is the performance cost acceptable during actual survival?

The acceptance campaign should include representative old-world construction.

Do not use only clean synthetic fixtures with idealized rectangular houses.

---

# 25. Explicitly Deferred Features

Survival 0.3.0 must not implement:

- SITE_SIEGE events.
- Settlement-targeted enemy waves.
- Strategic building destruction.
- Autonomous siege objectives.
- Settlement-specific loot rewards.
- Mandatory claim blocks.
- Formal territory ownership.
- Player factions.
- Village management.
- NPC settlement populations.
- Mandatory settlement upgrades.
- Automatic factory taxation.
- New defensive turrets.
- Complete AI navigation through fortress layouts.
- Large-scale world scanning.
- Unrelated new mobs or biomes.
- Major EE2 or crafting rebalances.
- Industrial Saboteur target expansion.

Some of these features belong to Survival 0.4 or later.

The absence of immediate combat content is intentional.

This milestone establishes the foundation that makes future strategic gameplay reliable.

---

# 26. Open Design Decisions

The following require investigation or explicit owner review.

### Provenance

- What spatial resolution provides the best practical evidence?
- Which block mutations can be attributed reliably?
- How should unsupported automation be represented?
- How much historical information is worth retaining?
- When should provenance records expire, if ever?

### Recognition

- What minimum combination of signals should create a candidate?
- When should a candidate become a recognized site?
- Should a tiny frequently used shelter be classified as an outpost?
- What evidence should distinguish excavation from construction?
- How should old-world structures be treated when authorship is unknown?

### Spatial identity

- How should buildings be grouped?
- What degree of separation should be tolerated?
- How should vertical structures be represented?
- When should sites merge?
- When should sites split?
- What constitutes abandonment?

### Protected interiors

- How should natural caves differ from adapted caves?
- What local enclosure evidence is sufficiently reliable?
- How should open entrances and irregular roofs be evaluated?
- How can the analysis remain performant?

### Future integration

- What is the smallest stable site snapshot required by SITE_SIEGE?
- How should future sieges represent targets spanning several buildings?
- Which site properties are genuinely necessary for siege planning?
- What information belongs to the future siege director rather than Settlement Recognition?

Agents may prototype conservative reversible defaults, but should not silently turn unresolved product decisions into permanent rules.

---

# 27. Milestone Definition of Done

Survival 0.3.0 is complete when ZTO can:

1. Observe relevant player modifications reliably.
2. Preserve bounded provenance evidence.
3. Distinguish observed, inferred and unknown evidence.
4. Identify plausible settlement candidates.
5. Recognize meaningful settlements and outposts without prescribed structures.
6. Assign durable site identities.
7. Maintain approximate spatial boundaries.
8. Handle genuine building changes over time.
9. Distinguish settlement identity from Protected Interior.
10. Consume existing Territorial Activity without replacing it.
11. Expose authoritative snapshots through a stable query boundary.
12. Remain safe across old saves, chunk unloading and world lifecycle changes.
13. Provide meaningful diagnostics.
14. Pass targeted and comprehensive pinned Tekkit runtime tests.
15. Demonstrate convincing recognition in owner-built real gameplay scenarios.

**Survival 0.3 does not need to spawn a single new hostile mob to meet these criteria.**

Its success is measured by the quality, stability, and usefulness of its understanding of player-established locations.

---

# 28. Handoff to Survival 0.4

At the conclusion of this milestone, prepare a dedicated technical handoff describing:

- Settlement identity semantics.
- The provenance model.
- Confidence and uncertainty.
- Site boundaries.
- Lifecycle and persistence.
- Protected interior queries.
- Known limitations.
- Performance budgets.
- Available read-only snapshots.
- Verified scenarios.
- Unresolved issues affecting strategic siege planning.

This handoff will inform the next design document:

**Survival 0.4 — SITE_SIEGE & Fortress Warfare**

Survival 0.4 may then introduce:

- Strategic site targeting.
- Encounters directed at persistent locations.
- Tactical attacking roles.
- Protected access routes.
- Industrial infiltration.
- Controlled structural breaching.
- Defenses and countermeasures.
- Site-specific preparation and warning.
- Encounter resolution and recovery.

Those mechanics will have their own authority, budgets, and acceptance criteria.

They must not be smuggled into the Settlement Recognition implementation.

---

# 29. Final Creative Direction

ZedsTekkitOverhaul is evolving from a game that recognizes nearby enemies and player activity into a world that understands meaningful places.

A small cabin can become a home.

A converted cave can become a bunker.

A cluster of machines can become an industrial outpost.

A distant fortified structure can remain meaningful even when its owner is exploring elsewhere.

The world should recognize these places without requiring the player to declare them.

The system must remain conservative, explainable, persistent and computationally bounded.

Most importantly, it must preserve the freedom to build anything, anywhere, using the materials and technologies the player prefers.

**Survival 0.3 teaches the world what the player has built. Survival 0.4 will teach the world how to respond strategically to those constructions.**
