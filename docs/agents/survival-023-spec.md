# ZedsTekkitOverhaul — Survival 0.2.3
## Biome Identity & Living Ecosystems

**Document type:** Game Design & Functional Specification  
**Status:** Design Draft — Owner Review Required  
**Target:** Minecraft Java 1.2.5 / Tekkit Classic 3.1.2  
**Baseline:** Survival 0.2.2 — Aerial Ecology  
**Suggested repository path:** `docs/agents/survival-023-spec.md`

---

## 1. Executive Vision

Survival 0.2.3 introduces distinctive ecological identities to the original Minecraft Overworld.

Different biomes should feel like different environments not only because of their terrain, vegetation and color palettes, but because of the creatures that inhabit them and the behaviors the player can encounter.

This update expands upon existing ZedsTekkitOverhaul systems:

- Territorial Activity.
- Natural hostile spawning.
- Enderkin ecology.
- Fugl and Skelebird.
- Aerial encounter scheduling.
- Lunar events.
- Recoverable theft.
- Industrial survival and player-built defenses.

The objective is not to fill every biome with new monsters.

Instead, the world should exhibit **regional differences in encounter composition, wildlife behavior, ambient activity and environmental risk**.

A forest should not feel like a desert with different blocks.

A swamp should not feel like an ordinary forest with darker water.

Mountains should present different movement and encounter conditions from plains.

The player should gradually learn these distinctions through exploration.

### Core promise

**Every major biome family should have a recognizable ecological character, without making ordinary exploration continuously hostile.**

The world should remain familiar, mysterious and selectively dangerous.

---

## 2. Design Principles

### 2.1 Biome identity before content quantity

Do not create one new mob for every registered biome merely to complete a checklist.

Prioritize a small number of strongly differentiated species and behaviors.

Reuse existing creatures intelligently where appropriate.

### 2.2 Regional variety rather than rigid exclusivity

A species may prefer certain habitats without being absolutely restricted to them.

Biome selection should usually influence encounter probabilities rather than act as a hard prohibition.

Exceptions may exist for species with a strong ecological dependency.

### 2.3 Behavior matters more than statistics

Biome-specific encounters should differ through movement, targeting, camouflage, aggression, group behavior, sound and environmental interaction.

Avoid defining biomes exclusively through HP, damage or spawn multipliers.

### 2.4 Preserve ordinary Minecraft survival

The player must still be able to gather wood, mine, build, farm and explore.

New wildlife should produce interesting decisions rather than constant interruption.

### 2.5 Respect technological progression

Advanced Tekkit equipment must remain powerful.

A jetpack, energy armor, ranged weapon or automated defense should provide a meaningful advantage.

Do not systematically neutralize high-tier equipment to preserve enemy difficulty.

### 2.6 No mandatory biome checklist

Players should not be required to visit every biome to unlock basic survival or essential technology.

Unique drops may support optional equipment, alternate recipes or specialized capabilities, but should not automatically become mandatory progression gates.

---

## 3. Minecraft 1.2.5 Biome Baseline

The target Minecraft version predates many modern biome families.

The preliminary historical census identifies 23 registered biome IDs from 0 through 22, including Nether and End.

The relevant preliminary Overworld inventory is:

| ID | Legacy biome |
|---|---|
| 0 | Ocean |
| 1 | Plains |
| 2 | Desert |
| 3 | Extreme Hills |
| 4 | Forest |
| 5 | Taiga |
| 6 | Swampland |
| 7 | River |
| 10 | Frozen Ocean |
| 11 | Frozen River |
| 12 | Ice Plains |
| 13 | Ice Mountains |
| 14 | Mushroom Island |
| 15 | Mushroom Island Shore |
| 16 | Beach |
| 17 | Desert Hills |
| 18 | Forest Hills |
| 19 | Taiga Hills |
| 20 | Extreme Hills Edge |
| 21 | Jungle |
| 22 | Jungle Hills |

IDs 8 and 9 belong to Hell and Sky respectively and are outside the Overworld scope.

### Historical verification requirement

Before implementing biome-dependent gameplay, confirm this list against the actual pinned Minecraft 1.2.5 `BiomeGenBase` registry and world generator.

Distinguish:

- Registered biome IDs.
- Biomes naturally produced by the default Overworld generator.
- Rare transitions or hill variants.
- Legacy aliases and naming conventions.
- Any biome alterations introduced by the installed modpack.

Do not import modern biome categories or IDs into legacy code.

Do not assume Jungle Edge, modern Dark Forest, Savanna, Badlands, or other later biomes exist in the target version.

The confirmed runtime registry, not a modern wiki list, must become the implementation authority.

---

## 4. Ecological Biome Families

The 21 preliminary Overworld entries should be organized into manageable ecological families.

A family may share species while individual biomes modify encounter probabilities, terrain behavior or ambience.

### 4.1 Temperate Forests

Includes Forest and Forest Hills.

**Identity:** Dense vegetation, intermittent visibility, tree cover and small opportunistic wildlife.

Potential features:

- Magpie-like scavengers or thieves.
- More frequent evasive Enderkin.
- Wildlife moving between tree canopies.
- Occasional aerial predators visible above forest openings.
- Small passive or neutral forest animals.

These biomes should reward awareness without turning every tree into an ambush.

### 4.2 Plains and Open Grassland

Includes Plains.

**Identity:** Long sightlines, exposed travel, open-air wildlife and visible distant threats.

Potential features:

- Greater visibility of Fugl hunting overhead.
- Small ground animals.
- Rare opportunistic daytime aerial attacks.
- Enderkin that exploit open spaces rather than dense cover.
- Distinctive distant creature calls.

Open terrain should offer tactical freedom but less natural overhead protection.

### 4.3 Swamps and Wetlands

Includes Swampland and potentially related river transitions.

**Identity:** Poor visibility, waterlogged terrain, unfamiliar calls and creatures that exploit difficult movement.

Potential features:

- Evasive or ambush-oriented Enderkin preferences.
- Small marsh scavengers.
- Rare nocturnal wildlife concentrated near water.
- Atmospheric sounds.
- Water-adjacent hunting behavior.

Do not make swamp traversal continuously punishing.

The biome should feel uneasy and mysterious rather than uniformly lethal.

### 4.4 Deserts and Arid Hills

Includes Desert and Desert Hills.

**Identity:** Exposure, sparse cover, long travel distances and creatures adapted to open ground.

Potential features:

- Scarce but conspicuous predators.
- Scavengers around exposed terrain.
- Small desert-specific animals.
- Distinct day/night behavioral differences.
- Enderkin roles suited to ranged pressure or rapid repositioning.

Avoid introducing unrealistic infinite daytime monster pressure.

### 4.5 Mountains and Rocky Terrain

Includes Extreme Hills and Extreme Hills Edge.

**Identity:** Verticality, cliffs, high exposed airspace and terrain-controlled pursuit.

Potential features:

- Elevated Fugl encounters.
- More frequent aerial hunting in genuine exposed flight.
- Rare Skelebird sightings under appropriate nighttime conditions.
- Creatures that exploit ledges and steep slopes.
- Greater importance of line of sight and flight navigation.

Mountain danger must come from meaningful terrain and ecology, not simply a higher biome difficulty multiplier.

### 4.6 Taiga and Cold Forests

Includes Taiga and Taiga Hills.

**Identity:** Sparse cold woodland, lower vegetation, atmospheric isolation and reserved wildlife activity.

Potential features:

- Different ambient bird calls.
- Cold-adapted small animals.
- Occasional predators hunting along forest edges.
- Distinct Enderkin encounter composition.

Maintain a contrast with temperate forests.

### 4.7 Snow and Ice

Includes Ice Plains, Ice Mountains, Frozen River and Frozen Ocean.

**Identity:** Open cold environments, visibility, environmental exposure and sparse but memorable encounters.

Potential features:

- Rare specialized flying wildlife.
- Skelebird-associated nighttime pressure where appropriate.
- Animals with cold-adapted silhouettes and behaviors.
- Terrain and visibility influencing aerial approaches.

Avoid mandatory cold-survival meters or new armor requirements in this milestone.

### 4.8 Jungles

Includes Jungle and Jungle Hills.

**Identity:** Dense canopy, vertical vegetation, restricted sightlines and active wildlife.

Potential features:

- More varied small animal activity.
- Canopy-associated birds.
- Opportunistic theft.
- Evasive Enderkin.
- Aerial predators that patrol openings above the trees.

The dense canopy should meaningfully protect players from some aerial attacks while creating other local risks.

### 4.9 Shores, Rivers and Oceans

Includes Ocean, River and Beach.

**Identity:** Transitional spaces, exposed shorelines, open water and aquatic-adjacent wildlife.

Potential features:

- Shore scavengers.
- Flying creatures traveling along coastlines.
- Different encounter conditions over water.
- Rare environmental wildlife interactions.

A future aquatic ecology deserves its own specification.

Do not attempt to implement a complete marine ecosystem here.

### 4.10 Mushroom Islands

Includes Mushroom Island and Mushroom Island Shore.

**Identity:** Unusual isolated ecosystems and a distinctive break from ordinary hostile survival.

Preserve their exceptional character.

Avoid casually introducing ordinary hostile wildlife or Enderkin populations that invalidate the biome's established identity.

Possible additions should emphasize mystery, harmless unusual fauna, or very rare explicitly justified phenomena.

---

## 5. Biome Preferences for Existing Enderkin

**First major gameplay objective.**

Existing Enderkin should gain meaningful regional encounter preferences.

Do not create new Enderkin species as part of this initial change.

### 5.1 Skirmisher

Suitable for environments that support maneuvering, ranged pressure and repositioning.

Candidate preferences:

- Plains.
- Deserts.
- Open mountainous approaches.

Dense terrain may still allow occasional appearances, but its ranged identity should remain meaningful.

### 5.2 Breacher

Suitable for terrain where close-range pressure and obstacle navigation create interesting encounters.

Candidate preferences:

- Forests.
- Jungles.
- Hills.
- Areas with meaningful cover and narrow approaches.

Avoid restricting the Breacher to player-made structures or forcing it to spawn near bases.

### 5.3 Saboteur

Its industrial infrastructure signal remains the primary contextual factor.

Biome preferences should be secondary.

A nearby supported industrial target must remain relevant regardless of whether the player built the installation in a forest, desert or snowy biome.

Do not punish the player for choosing an unusual location for industrial construction.

### 5.4 Tank

The Tank should remain rare.

Candidate ecological preferences include:

- Rugged mountain environments.
- Dense forests.
- Selected dangerous territorial regions.

Do not make every mountainous region routinely spawn near-miniboss Tanks.

Its rarity and physical presence are essential to its identity.

### 5.5 Thief

Candidate preferences:

- Forests.
- Jungles.
- Swamps.
- Environments supporting evasive movement.

The Thief's actual escape and recovery gameplay must remain unchanged.

Biome distribution should not interfere with saved stolen-item ownership or compass tracking.

### 5.6 Population and spawn constraints

The existing Enderkin ecology replaces selected validated natural hostile candidates.

Retain that architecture unless investigation identifies a compelling reason to change it.

Biome preferences should primarily redistribute eligible Enderkin species within the existing bounded encounter opportunities.

Do not accidentally multiply overall Enderkin density.

Preserve:

- Vanilla light restrictions.
- Entity collision and clearance.
- Existing local Enderkin limits.
- The native hostile population budget.
- Territorial Activity integration.
- Lunar encounter scheduling authority.
- Industrial contextual selection.

### 5.7 Avoid hard spawn borders

Crossing a biome boundary should not instantly switch the world from one creature population to another.

Consider bounded neighboring-biome sampling or a similarly inexpensive weighting mechanism if useful.

Do not create visible spawn cliffs, particularly between hill variants and their parent biomes.

---

## 6. First New Regional Species — Magpie-Like Scavenger

**Second major gameplay candidate.**

Introduce a small opportunistic bird inspired by real magpies.

This creature should be fundamentally different from the Enderkin Thief.

### 6.1 Role

A minor environmental nuisance and an example of non-hostile wildlife with meaningful behavior.

It may occasionally take a small eligible possession from the player.

The experience should be amusing, surprising or mildly inconvenient rather than punishing.

### 6.2 Movement

The creature should be able to:

- Walk and hop on the ground.
- Turn its head toward objects.
- Fly short distances.
- Move between nearby perches.
- Flee from danger.
- Return to normal ambient behavior.

Its movement should not reuse Fugl's combat flight unchanged.

### 6.3 Theft behavior

A magpie may occasionally steal a suitable item under clearly limited conditions.

Possible target categories include:

- Low-value ordinary items.
- Small dropped items.
- Exposed possessions under carefully defined rules.

The exact eligibility rules require owner approval.

**It must not casually steal expensive equipment, equipped armor or irreplaceable progression items.**

### 6.4 Recovery

The stolen object must remain recoverable.

Prefer short-distance escape or an accessible temporary cache.

Do not require a cross-biome pursuit.

Do not use the Ender Trail.

Do not use the Thief's compass tracking system.

### 6.5 Frequency

Theft should be rare compared with ordinary passive behavior.

Most magpie encounters should involve walking, hopping, observing or flying away.

A biome populated by magpies must not become a continuous inventory-management problem.

### 6.6 Habitat

Temperate forests and appropriate forest edges are initial candidates.

Other biomes may eventually host related scavenger species.

Do not assume every biome requires its own stealing bird.

---

## 7. Predation on Other Mobs

**Third major gameplay direction.**

Some predators should be capable of hunting entities other than the player.

This includes existing Fugl and potentially selected future wildlife.

### 7.1 Potential prey

Candidate prey classes include:

- Chickens.
- Other small passive animals.
- Selected hostile mobs.
- Future small wildlife species.

The actual target matrix must be deliberate.

### 7.2 Ecological value

Predation creates observable interactions even when the player is not the direct target.

It also introduces reasons to:

- Protect exposed farms.
- Construct covered animal enclosures.
- Observe wildlife behavior.
- Choose safer farm locations.
- Defend valuable livestock.

### 7.3 Farm protection

Properly enclosed animal pens must provide meaningful protection.

Predators should not attack livestock through solid roofs or walls.

Do not add arbitrary block destruction to ordinary aerial wildlife.

### 7.4 Limits

Predation must be intermittent.

Avoid:

- Mass slaughter of farm animals.
- Endless predator-versus-monster combat.
- Infinite target switching.
- Overly aggressive hunting near every farm.
- Permanent accumulation of hunting entities.

Existing wildlife behavior should remain a source of environmental variety rather than a new compulsory base-defense chore.

### 7.5 Fugl and Skelebird differentiation

Fugl is a plausible occasional hunter of small animals.

Skelebird may remain more strongly oriented toward hostile nighttime encounters and player threats.

Whether Skelebird should attack passive animals is an open design decision.

Do not assume identical prey behavior for both species.

---

## 8. Ambient Ecology and Atmosphere

Not every regional feature must be hostile or mechanically consequential.

Biome identity can also emerge through:

- Distant animal calls.
- Distinctive wingbeats.
- Occasional wildlife silhouettes.
- Short flock movements.
- Perching.
- Ground scavenging.
- Flight between trees.
- Creatures reacting to one another.
- Time-of-day differences.

Avoid excessive ambient sound playback.

Avoid adding permanent populations of decorative entities that burden performance.

Use restrained, recognizable cues.

A player should learn the soundscape of a biome over time.

---

## 9. Day and Night Identity

The world should not become completely safe during daylight.

However, daytime encounters must remain manageable.

### Day

Favor:

- Ordinary wildlife.
- Rare opportunistic nuisances.
- Occasional natural predators.
- Contextual Enderkin encounters where vanilla spawning permits.

### Night

Favor:

- More threatening encounter composition.
- Increased relevance of existing hostile wildlife.
- Skelebird activity.
- Stronger regional atmosphere.
- Territorial and lunar pressure where eligible.

Do not bypass vanilla light rules indiscriminately to make Enderkin appear during the day.

Daytime tension may emerge naturally through wildlife, terrain, weather, existing enemies and rare ecological incidents.

---

## 10. Territorial Activity and Ecological Pressure

Territorial Activity already reflects sustained player presence and return visits.

The new biome ecology may consume its assessment.

However, biome and territorial activity must remain conceptually distinct.

**Biome identity answers:** What kinds of creatures and encounters belong here?

**Territorial Activity answers:** How much pressure is currently appropriate around this area?

A forest should remain recognizably a forest at low or high territorial activity.

A dangerous region should not simply become populated by every available creature.

Use territorial scores to modify appropriate encounter intensity and species weighting within reasonable caps.

Do not introduce biome-specific territorial ledgers without a demonstrated need.

---

## 11. Interaction with Aerial Ecology

The existing aerial system uses terrain exposure, difficulty, Territorial Activity and cooldowns.

Preserve these mechanisms.

Biome identity may influence:

- Relative Fugl and Skelebird prevalence.
- Ambient wildlife appearances.
- Typical group composition.
- Appropriate cruising altitude.
- Encounter atmosphere.

Biome preferences must not override the existing safety constraints for aerial spawning.

Do not create a special biome that ignores active encounter limits.

Preserve sheltered-player protection and jetpack fault safety.

---

## 12. Environmental Storytelling

Different biomes should suggest different ecological relationships.

Examples:

**Forest:** A small bird investigates a dropped object while a distant predator circles above an opening in the canopy.

**Swamp:** Strange calls come from the trees at night, but the source is not immediately visible.

**Mountain:** A Fugl glides along a cliff and redirects toward a player flying above the ridge.

**Jungle:** Small birds move through the canopy while an evasive Enderkin appears near a clearing.

**Snowy plain:** The environment is quiet for long periods, making a rare skeletal silhouette especially noticeable.

These are illustrative design scenarios, not mandatory scripted events.

Prefer emergent encounters over fixed cinematic sequences.

---

## 13. Biome Ecology Architecture

### 13.1 Version-specific registry

Use the actual Minecraft 1.2.5 biome identity available in the pinned runtime.

Do not build around modern biome resource-location APIs.

### 13.2 Data-driven preferences

Prefer a compact registry of ecological family and species weights over scattered biome-specific conditions throughout multiple entity classes.

Keep it understandable and maintainable.

### 13.3 Existing authority

Ordinary Enderkin selection should remain under the established natural spawning integration.

Aerial wildlife should retain its bounded encounter controller.

Do not introduce a third general-purpose hostile spawning authority that competes with both.

### 13.4 Performance

Biome sampling should be inexpensive.

Avoid large per-tick scans of chunks, animals or blocks.

Do not load new terrain for population planning.

### 13.5 Save compatibility

Biome preference changes should not require rewriting existing world data.

Preserve entity registrations, numeric IDs, inventories and persistent theft systems.

### 13.6 Diagnostics

Extend the Debug Hub with ecological diagnostics where useful.

Potential values:

- Current biome ID and name.
- Ecological family.
- Species weights.
- Current regional activity.
- Relevant natural spawn restrictions.
- Recent species-selection decisions.
- Nearby active wildlife.
- Encounter cooldowns.

Display authoritative data rather than approximate predictions.

---

## 14. Balance and Progression

### 14.1 No automatic biome difficulty ladder

Avoid making each successive biome objectively harder than the previous one.

Different biomes should favor different forms of preparation and tactical behavior.

### 14.2 Advanced equipment remains valuable

Jetpacks, strong weapons, armor and industrial defenses must retain their advantages.

Regional creatures should sometimes change the tactical problem without making advanced gear obsolete.

### 14.3 New drops

Creature drops should primarily use existing Minecraft or Tekkit materials where appropriate.

New item drops require a clear gameplay purpose.

Do not introduce large families of otherwise useless crafting ingredients.

### 14.4 Optional progression

Specialized drops may eventually unlock alternative crafting paths, decorative items or optional technology.

Do not introduce mandatory mob farming as the default path to the Electric Jetpack.

### 14.5 Existing EE2 economy

Avoid casually modifying EE2 progression or EMC values as part of a biome ecology implementation.

A dedicated economy and crafting pass should address those topics separately.

---

## 15. Proposed Development Phases

### Phase A — Biome Audit and Ecological Weighting

Confirm the legacy biome registry.

Define ecological families.

Implement species preference weights for the existing Enderkin without increasing overall spawn budgets.

Add diagnostics and verify differences in representative biomes.

### Phase B — Magpie Prototype

Create one original small bird with ground hopping, short flight, perching, evasion and ambient behavior.

Introduce carefully bounded opportunistic theft only after the movement and recovery design is proven.

### Phase C — Predator–Prey Interactions

Allow selected existing aerial predators to occasionally hunt appropriate passive or hostile mobs.

Verify safe farm protection and population impact.

### Phase D — Regional Atmosphere

Add restrained biome-specific sound and wildlife behavior differences.

Avoid creating an excessive ambient entity population.

### Phase E — Comprehensive Acceptance

Test habitat preferences, rare-biome behavior, daylight, nighttime, territorial intensity, world persistence, ecology interactions and compatibility.

Conduct owner playtesting before considering the feature complete.

---

## 16. Verification Scenarios

### Forest

Compare ordinary Enderkin species distribution with other biomes under similar lighting and territorial activity.

Observe wildlife without requiring continuous combat.

### Plains

Verify that open terrain supports appropriate visibility and occasional aerial activity.

### Swamp

Verify distinct encounter preferences without degrading ordinary movement into constant harassment.

### Desert

Check sparse but meaningful wildlife behavior and the absence of excessive daylight hostile generation.

### Mountains

Compare ground-level players, players standing on high terrain and players flying above exposed ridges.

### Jungle

Verify canopy-aware behavior and meaningful aerial cover.

### Cold Biomes

Check snow, ice and cold-forest family assignments.

### Mushroom Island

Verify preservation of the biome's special hostile ecology.

### Biome Boundaries

Confirm that small movements across boundaries do not create abrupt uncontrolled changes.

### High Territorial Activity

Verify that encounter pressure rises within population limits without erasing regional identities.

### Saved Worlds

Verify that existing Enderkin, aerial entities, stolen items, equipment and player data remain compatible.

---

## 17. Immediate Pre-Implementation Corrections

The following issues belong to the preceding Aerial Update and should not be silently considered part of the new biome ecology implementation.

### Skelebird Bone Drops

Skelebird should have a modest, natural bone drop consistent with its skeletal identity.

Use existing vanilla bone items.

Verify the actual legacy drop semantics and ensure no duplication or excessive farming yield.

### Electric Jetpack Weather Severity

The owner reports that weather malfunctions still permit near-normal powered flight.

The desired behavior is substantially more consequential.

During a severe fault, continued activation should not allow the player to maintain ordinary flight indefinitely.

The player should visibly lose altitude and need to adapt their travel plans.

Repeated storm exposure should sometimes make walking a more attractive choice than continuing to fly.

Preserve warning, recovery, meaningful emergency descent control and native IC2 item integrity.

Measure actual flight trajectories, not only fault timers.

These corrections should be implemented and tested as a focused continuation of 0.2.2 before broad biome ecology changes begin.

---

## 18. Open Design Questions

The following decisions remain subject to owner review.

1. Which precise Enderkin species should receive the strongest preference in each ecological family?
2. Should biome distribution affect only natural encounters or also selected lunar encounters?
3. Which forests should host magpie-like scavengers?
4. Should magpies primarily steal dropped items, inventory items, or both?
5. Should stolen magpie items be cached locally or dropped during flight?
6. Which passive animals may Fugl hunt?
7. Should Skelebird attack animals or primarily hostile entities and players?
8. Which biome families need completely new wildlife species?
9. Should ecological ambience rely more heavily on real creatures or occasional sound-only activity?
10. Should a later release introduce specialized biome-related crafting drops?

Do not silently convert these questions into irreversible implementation decisions.

---

## 19. Deferred Scope

This milestone does not require:

- An entirely new biome generation system.
- Dozens of new creatures.
- Global wildlife migration.
- Persistent nest colonies.
- Complex ecological food chains.
- Mandatory regional quests.
- A universal temperature-survival system.
- A complete marine ecosystem.
- Redesign of Enderkin theft.
- Full Saboteur compatibility expansion.
- Major EE2 economy changes.
- Mandatory new jetpack crafting ingredients.

These features should receive separate evaluation if they become relevant.

---

## 20. Release Acceptance Criteria

Survival 0.2.3 should be considered successful when:

1. The actual Minecraft 1.2.5 Overworld biome registry has been verified.
2. Major ecological families have documented identities.
3. Existing Enderkin have meaningful and bounded biome preferences.
4. Regional weighting does not uncontrollably increase total hostile spawning.
5. At least one new small wildlife species demonstrates a distinct non-combat behavior.
6. Opportunistic wildlife theft, if implemented, is limited and recoverable.
7. Selected predators can interact meaningfully with other mobs.
8. Protected farms remain useful.
9. Daytime wildlife adds interest without making ordinary survival exhausting.
10. Nighttime regional differences are noticeable.
11. Existing Territorial Activity and aerial ecology remain functional.
12. Biome boundaries, rare environments and save compatibility behave correctly.
13. Diagnostic information supports balancing.
14. Runtime and owner testing confirm that regions feel different in actual survival.

---

## Final Creative Objective

The player should gradually learn the ecology of the world.

A familiar forest might contain opportunistic scavenging birds.

An exposed plain might reveal distant aerial predators.

A swamp might be characterized by uneasy sounds and evasive creatures.

A mountain ridge might make aerial travel more dangerous than the valley beneath it.

These differences should emerge from real world behavior rather than quest markers or arbitrary numerical difficulty zones.

The result should be a Minecraft Overworld that is more varied, more alive and more worth exploring.

**Biome identity should create recognizable experiences, not a mandatory catalog of monsters.**