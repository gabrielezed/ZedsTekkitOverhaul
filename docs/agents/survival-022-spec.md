# ZedsTekkitOverhaul — Survival 0.2.2
## Aerial Ecology & Atmospheric Hazards

**Document type:** Game Design & Functional Specification  
**Status:** Design Draft — Pending Owner Approval and Gameplay Validation  
**Target:** Minecraft Java 1.2.5 / Tekkit Classic 3.1.2  
**Baseline:** Survival 0.2.1, merged into `main`  
**Primary scope:** Aerial wildlife, three-dimensional encounters, altitude-dependent ecology, weather-sensitive Electric Jetpack operation, aerial threat integration  
**Suggested repository path:** `docs/agents/survival-022-spec.md`

---

## 1. Vision

Survival 0.2.2 introduces the sky as a meaningful ecological and tactical environment.

Until now, the major survival threats in ZedsTekkitOverhaul have primarily occupied the ground, attacking players, installations, and defensive structures.

Tekkit's technological equipment allows players to escape many of these dangers by traveling above the terrain.

The objective of this update is to make aerial travel more engaging without removing the advantages of flight.

The sky should become a distinct environmental layer inhabited by original creatures, influenced by altitude, time, weather, territorial conditions, and specific encounter events.

Flying should remain an exceptional capability.

However, traveling through exposed airspace should require more judgment than simply activating a jetpack and ignoring the surrounding world.

**Core design principle: The sky offers freedom and speed, but exposure creates new risks.**

The release must preserve the broader ZTO philosophy:

- Technology provides meaningful power.
- Preparation and skill matter.
- Danger is intermittent rather than relentless.
- Threats have recognizable behavior and counterplay.
- Players retain freedom in how they travel and defend themselves.
- The world remains recognizable as Minecraft 1.2.5.

---

## 2. Release Objectives

The first implementation of 0.2.2 should deliver four interconnected systems.

### A. Original Flying Fauna

Introduce at least one fully functional, non-anthropomorphic flying creature with distinctive anatomy, movement, animation, combat behavior, and ecological spawning.

### B. Altitude-Dependent Encounters

Introduce aerial encounters influenced primarily by the player's exposure above the surrounding terrain.

### C. Weather-Sensitive Electric Jetpack

Rain and thunderstorms can temporarily interfere with the IndustrialCraft 2 Electric Jetpack through a readable, recoverable malfunction system.

### D. Flocks and Aerial Threat Integration

Support bounded group behavior, with more dangerous encounters under appropriate circumstances, including Hard difficulty, increased territorial activity, and potentially selected lunar events.

The result must feel like an interconnected aerial survival system rather than four unrelated additions.

---

## 3. Relationship to Existing Systems

Survival 0.2.2 builds on the established 0.2.1 implementation.

Relevant systems include:

- Territorial Activity and Threat Assessment.
- Ordinary hostile spawning pressure.
- Natural Enderkin ecology.
- Lunar event scheduling.
- Player-vicinity encounters.
- Survival and Territorial debug interfaces.
- Existing ZTOAgent compatibility infrastructure.

These systems should remain authoritative in their respective domains.

Aerial ecology should consume appropriate information from existing systems rather than duplicating territorial tracking or lunar scheduling.

The release must not alter the existing Enderkin bestiary or industrial sabotage behavior except where a focused compatibility adjustment is necessary.

---

## 4. Flying Fauna — Visual Identity

### 4.1 Creative direction

The initial flying creature should be an original predatory animal.

References include the Rakk and Skelerakk from the original Borderlands and its Zombie Island expansion.

These references inform:

- Animalistic flying anatomy.
- Recognizable wing silhouettes.
- Fast aerial maneuvers.
- Circling and diving behavior.
- Hunting in groups.
- An ominous presence in open sky.

Do not recreate Borderlands assets, models, sounds, or textures.

ZTO should develop its own visual identity within Minecraft's classic pixel-art vocabulary.

### 4.2 Non-anthropomorphic anatomy

The first creature must not simply be an Enderkin with wings.

It should look like an animal adapted to flight.

Potential anatomical elements include:

- Broad angular wings.
- A compact predatory torso.
- A distinctive head and beak or jaw.
- A tail or stabilizing appendage.
- Claws or other structures appropriate to its attacks.
- Distinctive wing joints visible during movement.

Its silhouette should be recognizable from the ground against the sky.

### 4.3 Visual style

Use original low-resolution textures consistent with Minecraft 1.2.5.

Avoid unnecessarily detailed textures or excessively smooth modern animation.

Movement should communicate weight and aerodynamics while remaining visually compatible with the existing ZTO creatures.

### 4.4 Animation states

At minimum, support:

- Sustained flight.
- Wing flapping.
- Gliding or cruising.
- Banking or turning.
- Diving preparation.
- Attack dive.
- Recovery and climbing.
- Death or falling behavior.

The creature should not resemble a ground mob translated through the air without appropriate animation.

### 4.5 Initial creature designation

**Working name: Duskwing.**

This name is provisional.

The final creature name, anatomy, palette, and thematic relationship to Ender phenomena remain creative decisions.

A skeletal or Ender-corrupted version may be introduced later.

---

## 5. Flight AI and Combat

### 5.1 Fundamental behavior

The creature should operate primarily in three dimensions.

Its AI must be capable of maintaining altitude, selecting positions in open air, pursuing suitable targets, and executing diving attacks.

Avoid depending entirely on ordinary ground navigation.

A lightweight dedicated aerial movement controller is appropriate if required by the actual Minecraft 1.2.5 entity implementation.

### 5.2 Core state sequence

The first creature should support a recognizable behavior cycle:

**Cruise → Detect → Observe/Circle → Commit → Dive → Recover → Disengage**

Each stage must have a gameplay purpose.

The creature should not continuously accelerate directly into the player.

### 5.3 Detection

A flying creature may detect exposed players within a bounded range.

Detection should depend on relevant factors such as:

- Distance.
- Line of sight.
- Player altitude.
- Creature altitude.
- Player exposure.
- Current encounter state.

Avoid unrealistic perfect awareness through terrain or enclosed structures.

### 5.4 Circling and preparation

Before attacking, the creature should spend time repositioning or circling.

This period creates anticipation and an opportunity for the player to react.

A distinct animation and sound may indicate an imminent dive.

The player should be able to identify the threat even when focusing on travel.

### 5.5 Diving attacks

Diving is the primary combat identity.

The creature accelerates toward a predicted attack position, commits to an attack trajectory, and then attempts to climb away.

The attack should not track every last-second player movement perfectly.

A player who recognizes the dive should have a meaningful opportunity to evade.

Successful contact may cause damage and moderate knockback.

Avoid severe unavoidable knockback that throws jetpack users into catastrophic falls.

### 5.6 Recovery

Following a dive, the creature must recover.

Recovery should temporarily limit its ability to launch another immediate attack.

This creates opportunities for ranged attacks, counterattacks, evasive movement, or escape.

### 5.7 Disengagement

Aerial predators should not pursue the player indefinitely.

They may disengage after:

- A completed attack sequence.
- Excessive pursuit distance.
- Loss of target.
- Entry into unsuitable terrain.
- A significant altitude change.
- A failed or abandoned encounter.

Disengagement must be a meaningful part of the system.

The player should be capable of escaping a dangerous flight encounter.

---

## 6. Altitude-Dependent Aerial Ecology

### 6.1 Objective

Aerial wildlife should become more relevant when the player travels through exposed airspace.

Simply walking on the ground must not trigger the same encounter intensity as sustained high-altitude flight.

### 6.2 Primary altitude metric

Use **height above local terrain**, or an equivalent grounded exposure metric, as the primary signal.

The player's absolute world Y-coordinate alone is insufficient.

A player above a high mountain should not automatically receive the same encounter classification as a player at the same Y-coordinate flying far above a low plain.

### 6.3 Terrain sampling

Investigate a bounded method of estimating the distance between the player and the relevant terrain below.

Use only already loaded world data.

Possible inputs include:

- Height of the supporting terrain beneath the player.
- Nearby surface samples.
- Absolute Y-coordinate as a secondary modifier.
- Whether the player is genuinely exposed to open sky.
- Whether the surrounding area contains extensive open air.

Evaluate edge cases such as:

- Mountains.
- Cliffs.
- Ravines.
- Deep valleys.
- Oceans.
- Tall player-built structures.
- Large forests and tree canopies.
- Overhangs.
- Players descending or ascending rapidly.

Do not perform expensive terrain analysis each tick.

### 6.4 Aerial exposure

Define a lightweight concept of sustained aerial exposure.

A player briefly jumping from a cliff should not immediately attract a hostile flock.

Exposure should accumulate through qualifying movement or time spent in open air.

The system should respond progressively rather than switching instantly at one exact altitude.

### 6.5 Ecological bands

The following qualitative bands are proposed:

| Ecological layer | Intended behavior |
|---|---|
| Ground and near-ground | Ordinary survival remains dominant; aerial encounters are uncommon |
| Low open air | Occasional individual aerial creatures |
| Exposed flight altitude | More frequent aerial encounters |
| High open sky | Potentially more specialized or coordinated aerial threats |

Exact numeric altitude thresholds require implementation investigation and playtesting.

Avoid enforcing rigid boundaries that generate creatures the moment the player crosses a specific Y-level.

### 6.6 Flight does not require a jetpack

Aerial ecology should respond to the actual world situation.

The system must not simply detect an Electric Jetpack and spawn enemies as punishment for equipping it.

Other forms of flight, unusual traversal, or exposed elevated positions may qualify where appropriate.

The environmental state matters more than the specific equipment.

---

## 7. Ground-Level Encounter Protection

**Critical balancing requirement.**

Aerial predators must not make ordinary ground-level survival intolerable.

### 7.1 Ground players

A player walking through forests, villages, bases, or ordinary open terrain should not experience constant attacks from the sky.

Aerial predators may occasionally descend near the ground, particularly at night, but these events must remain limited.

### 7.2 Attacks at night

Nocturnal diving attacks are a desirable atmospheric feature.

They should be memorable precisely because they do not happen continuously.

The player may hear a distant call, glimpse a silhouette, and then see a creature descend through the darkness.

A rare nighttime encounter should create tension without becoming an unavoidable daily nuisance.

### 7.3 Roof and shelter protection

Properly enclosed structures should provide meaningful protection.

Aerial mobs must not damage players through solid roofs.

If an airborne creature cannot establish a valid attack trajectory, it should reposition or disengage.

### 7.4 Ground pursuit limits

Most ordinary aerial predators should not continuously circle above a grounded player and repeatedly dive until the player dies.

A typical incident may involve one or two attack opportunities followed by disengagement.

Extended pressure should be reserved for deliberately more dangerous encounters.

### 7.5 Natural counterplay

Useful responses should include:

- Entering cover.
- Moving beneath suitable terrain.
- Descending from exposed altitude.
- Changing flight direction.
- Using ranged weapons.
- Reaching a safe landing location.
- Defeating or evading the predator.

No single response should be mandatory in every encounter.

---

## 8. Aerial Spawning and Population Control

### 8.1 Encounter admission

Aerial spawning must respect existing world authority and population-management principles.

Do not implement an independent uncontrolled spawning loop.

Use a bounded aerial encounter controller or equivalent integration with the existing hostile ecology.

### 8.2 Valid spawn locations

Spawn candidates must satisfy appropriate conditions.

These include:

- Loaded terrain.
- Sufficient free air volume.
- Safe entity collision.
- Meaningful separation from the player.
- Reasonable approach geometry.
- Valid dimension and difficulty.
- Species-specific environmental restrictions.

Do not spawn creatures directly inside the player's collision space.

Avoid visible appearance immediately in front of the camera where possible.

### 8.3 Encounter budgets

Aerial encounters should have their own bounded admission logic while remaining compatible with ordinary hostile population constraints.

The total number of active aerial predators must be limited.

Avoid creating a population system that grows indefinitely because the player remains at altitude.

### 8.4 Cooldowns

Following a significant aerial encounter, enforce an appropriate cooldown or recovery period before another comparable encounter can begin.

This is especially important during long-distance jetpack travel.

The intended experience is intermittent danger, not uninterrupted combat.

### 8.5 Despawning

Unengaged aerial wildlife should follow appropriate lifecycle rules.

Avoid accumulating distant flying entities.

Do not keep chunks loaded merely to maintain an aerial flock.

---

## 9. Flocks and Coordinated Behavior

### 9.1 Creative objective

Flocks should make some aerial encounters more impressive and tactically complex.

A flock is not simply several identical mobs attacking independently at the same moment.

Its behavior should be coordinated enough to create a recognizable group encounter.

### 9.2 Initial flock mechanics

The first implementation may support small groups with lightweight coordination.

Possible behaviors include:

- Traveling together.
- Circling the same general region.
- Staggered diving attacks.
- Temporary separation.
- Regrouping after an attack.
- Collective disengagement.

Avoid developing an unnecessarily sophisticated simulation of real-world bird flocking.

The gameplay effect is more important than biological realism.

### 9.3 Attack staggering

This is a central balancing requirement.

A flock should not perform simultaneous unavoidable collisions against the player.

Members should take turns attacking or maintain sufficient separation to create readable threats.

The player must have opportunities to dodge and respond between attacks.

### 9.4 Difficulty scaling

On Easy and Normal, encounters should generally remain smaller and more forgiving.

Hard may introduce:

- Larger groups.
- More coordinated attack sequencing.
- Slightly more persistent aerial pursuit.
- Increased opportunities for specialized flock encounters.

Do not rely only on higher health and damage values.

### 9.5 Territorial activity

Existing Territorial Activity should influence the probability or intensity of qualifying flock encounters.

High activity may increase the likelihood of coordinated aerial threats.

However, territorial activity should not imply that every high-activity region is permanently covered in flying enemies.

The system must respect encounter cooldowns and population constraints.

### 9.6 Future specialized flocks

Skeletal or Ender-influenced flying predators may later become particularly associated with:

- High territorial activity.
- Hard difficulty.
- Specific lunar phases.
- Organized hostile events.

The first 0.2.2 implementation should establish the foundation without requiring all these variants immediately.

---

## 10. Lunar Events and Aerial Pressure

### 10.1 Design objective

Aerial wildlife may eventually participate in existing lunar encounter systems.

The sky becomes another source of tactical pressure during selected events.

### 10.2 Existing authority

The lunar calendar remains the scheduling authority.

Do not introduce a second independent lunar scheduler.

Aerial encounters should consume event signals and appropriate admission budgets from existing systems.

### 10.3 Encounter composition

During appropriate lunar events, a small aerial component may supplement the ground encounter.

Potential examples:

- An individual predator circling above a patrol.
- A limited coordinated flock accompanying a difficult assault.
- A more threatening aerial encounter during a full moon on Hard.

Do not automatically add numerous flying enemies on top of an already saturated ground assault.

### 10.4 Defensive implications

Aerial pressure should encourage players to consider overhead exposure when building defenses.

Roofs, covered walkways, ranged weapons, and elevated defensive positions become more interesting.

However, aerial predators must not simply invalidate all previously developed fortifications.

### 10.5 Initial scope

Basic aerial integration with selected lunar events is desirable for 0.2.2.

Complex combined ground-air sieges and dedicated Skelerakk-like assault variants may be deferred.

---

## 11. Atmospheric Hazards — Electric Jetpack

**Critical gameplay objective.**

### 11.1 Concept

Rain and thunderstorms should create a new environmental consideration for the IndustrialCraft 2 Electric Jetpack.

Prolonged exposure to rain may cause temporary electrical instability.

The Electric Jetpack can experience short interruptions or reduced reliability.

This must be communicated clearly to the player.

### 11.2 Scope

The mechanic applies specifically to the supported Electric Jetpack implementation from the installed IndustrialCraft 2 version.

Do not automatically apply the same malfunction to ordinary fuel-powered jetpacks.

Do not change other electrical equipment without a separate design decision.

Investigate the exact IndustrialCraft 2 version and its actual jetpack runtime behavior before choosing an integration method.

Preserve native energy consumption, equipment identity, NBT, charging, and existing movement behavior outside the new malfunction state.

### 11.3 Exposure model

A malfunction should not occur immediately when the first rain particle touches the player.

Use a bounded exposure or wetness model.

Qualifying exposure may depend on:

- Active rain.
- Player exposure to precipitation.
- Duration of exposure.
- Current flight use.
- Thunderstorm severity.
- Recovery after reaching shelter.

Standing under a roof should meaningfully reduce or prevent further exposure.

Do not infer rain exposure solely from the world's global weather flag.

### 11.4 Progressive instability

The proposed sequence is:

**Dry → Wet → Unstable → Temporary Fault → Recovery**

These are functional states, not necessarily visible labels.

At first, rain exposure produces only atmospheric feedback.

With sustained exposure, the jetpack may become unstable.

A fault should be temporary and recoverable.

The player should receive warnings before severe interruption.

### 11.5 Fault behavior

A temporary electrical fault may:

- Interrupt thrust for a short period.
- Reduce thrust consistency.
- Prevent immediate reactivation during a brief recovery window.
- Produce distinctive electrical sound and particle effects.

Avoid creating a permanent equipment failure.

Do not destroy the jetpack or erase its energy storage.

### 11.6 Critical safety requirement

**A fault must not automatically cause unavoidable fatal fall damage.**

A player at significant altitude must retain a reasonable opportunity to respond.

Consider one or more safeguards:

- An advance warning interval.
- Brief intermittent thrust before complete interruption.
- A limited emergency descent-control capability.
- Automatic recovery after a short fault.
- A cap on the severity of repeated faults during one exposure period.

The exact mechanism requires technical validation against the native jetpack's movement implementation.

The malfunction should create danger and demand attention, not function as a random death sentence.

### 11.7 Rain and thunderstorms

Ordinary rain should be a manageable hazard.

Thunderstorms may increase instability or make faults more likely.

However, avoid making all storm flight prohibitively dangerous.

Weather must influence risk without completely forbidding aerial travel.

### 11.8 Recoverability

The jetpack should return to normal behavior after a suitable recovery period.

Shelter and cessation of rain should help recovery.

The player should not need to perform an arbitrary repair ritual or consume a new item after every fault.

### 11.9 Electricity and authenticity

Use effects and consequences consistent with the fictional technology of Tekkit.

Do not claim to simulate realistic electronic engineering.

The design goal is readable environmental gameplay rather than strict physical realism.

---

## 12. Jetpack Malfunction Feedback

### 12.1 Audio

Introduce restrained electrical feedback.

Possible sounds include:

- Intermittent buzzing.
- A short electrical crackle.
- A recognizable fault cue.
- A recovery sound.

Avoid excessively loud or repetitive effects.

### 12.2 Particles

Small electrical sparks may accompany instability.

Use modest effects suitable for the original game.

Do not flood the player's camera with particles during flight.

### 12.3 Inventory feedback

Display an understandable indication of jetpack instability or temporary malfunction.

The preferred approach is a small status indicator associated with the equipped Electric Jetpack.

Potential locations include:

- The equipment slot tooltip.
- A restrained inventory status line.
- A compact icon or label in the existing inventory interface.

Do not assume a modern Minecraft status-effect or inventory API exists in Minecraft 1.2.5.

Choose the least intrusive approach compatible with the project's existing UI architecture.

### 12.4 Immediate flight feedback

Because the player may be unable to open the inventory safely while flying, inventory feedback alone is insufficient.

The world must also communicate the malfunction through sound, movement and visible effects.

The player should understand whether the jetpack is:

- Operating normally.
- Becoming unstable.
- Temporarily unable to provide normal thrust.
- Recovering.

---

## 13. Interaction Between Weather and Aerial Predators

### 13.1 Combined pressure

Weather hazards and aerial combat may overlap.

This can create interesting situations.

However, the systems must not accidentally create unavoidable failure sequences.

### 13.2 Risk stacking

A storm-induced jetpack fault combined with a flock attack could be disproportionately dangerous.

Implement safeguards to prevent repeated overlapping disruptions from eliminating player agency.

Potential approaches include:

- Reduced aerial encounter admission during severe malfunction.
- Attack staggering.
- A short recovery interval before another major aerial threat.
- Reduced aggressive pursuit of a player already descending uncontrollably.

The chosen solution should feel systemic rather than artificially scripted.

### 13.3 Environmental strategy

The player should be rewarded for noticing changing weather.

Possible decisions include:

- Flying at lower altitude.
- Waiting for the storm to pass.
- Traveling through valleys.
- Planning shorter flights.
- Carrying backup equipment.
- Landing temporarily.
- Using terrain or structures for protection.

Do not make any one preparation method mandatory.

---

## 14. Additional Gameplay Opportunities

The following features are promising but require evaluation before becoming mandatory implementation targets.

### 14.1 Roosting and distant wildlife

Flying animals could occasionally be seen circling distant terrain or resting at suitable locations.

This would help the world feel inhabited even when no encounter is active.

Avoid developing elaborate persistent nest systems in the first implementation.

### 14.2 Non-hostile aerial behavior

Not every flying creature must immediately attack the player.

Some individuals might cruise without engaging, while others become aggressive when approached.

This would make aerial encounters less predictable and more ecological.

### 14.3 Dive interruption

A predator hit during its attack approach might miss, recoil, or abandon the dive.

This would reward accurate ranged attacks and player reaction time.

### 14.4 Terrain-guided evasion

Flying close to a mountainside or entering a narrow valley could help break a predator's pursuit.

This would give meaningful value to altitude management and route planning.

### 14.5 Distinctive aerial sounds

Distant calls could warn of wildlife before it becomes visible.

Audio can reinforce the impression of a populated sky without requiring excessive entity density.

These possibilities should be evaluated for their impact on gameplay and performance.

---

## 15. Performance and Compatibility

Aerial ecology introduces additional simulation costs.

The implementation must remain compatible with Minecraft 1.2.5 and the existing Tekkit Classic environment.

### 15.1 Bounded AI

Limit navigation and target-selection work.

Avoid expensive full three-dimensional pathfinding when simpler steering and local collision avoidance are sufficient.

### 15.2 Loaded terrain

Do not load chunks merely to maintain aerial creatures or compute long-distance routes.

### 15.3 Population

Bound the number of active aerial entities and flock members.

### 15.4 Rendering

Keep models, animations, and particle effects lightweight.

A flock should not cause severe frame-rate degradation.

### 15.5 Equipment compatibility

Verify the Electric Jetpack against the actual installed IndustrialCraft 2 runtime.

Do not modify protected third-party binaries or overwrite other mod functionality without a justified and version-pinned integration approach.

Preserve existing inventory, equipment, movement and energy behavior when weather effects are inactive.

### 15.6 Existing gameplay

Preserve:

- Enderkin behavior and spawning.
- Territorial Activity.
- Lunar events.
- Inventory controls.
- Existing industrial features.
- Saved worlds.
- Entity registrations and item IDs.

---

## 16. Debugging and Diagnostics

The existing ZTO Debug Hub should gain appropriate aerial diagnostics.

A dedicated Aerial Ecology monitor or a clearly separated diagnostic section is recommended.

Useful values include:

- Player Y-coordinate.
- Estimated height above local terrain.
- Current aerial exposure.
- Active aerial encounter state.
- Nearby aerial creature population.
- Flock size and admission status.
- Encounter cooldown.
- Territorial contribution.
- Weather conditions.
- Electric Jetpack wetness or instability.
- Active fault and recovery timers.
- Relevant spawn rejection reasons.

All displayed values should originate from actual runtime owners.

Do not generate expensive diagnostics merely because a menu is open.

The diagnostic system must distinguish between conditions preventing an encounter and a feature being disabled.

---

## 17. Proposed Implementation Phases

### Phase A — Aerial Movement Prototype

Implement the first creature's model, animations, and foundational flight AI.

Verify sustained flight, collision avoidance, target detection, diving attacks, recovery and disengagement.

The creature should be inspectable through a Creative spawn egg before being introduced into natural survival.

### Phase B — Aerial Ecology

Implement exposure-based encounter admission, valid aerial spawning, population limits and encounter cooldowns.

Verify that ordinary ground players remain comparatively safe.

### Phase C — Electric Jetpack Weather System

Investigate the actual IC2 implementation.

Add rain exposure, instability, temporary fault behavior, recovery, and feedback.

Verify that the mechanic cannot create unavoidable fatal falls.

### Phase D — Flocks and Integration

Add bounded flock behavior.

Introduce difficulty and Territorial Activity influences.

Evaluate limited integration with existing lunar encounters.

### Phase E — Integration and Acceptance

Run comprehensive tests of:

- Aerial combat.
- Natural spawning.
- Ground-level balance.
- Storm behavior.
- Jetpack compatibility.
- Population caps.
- Save/reload.
- Lunar event coexistence.
- Performance.

Perform owner gameplay acceptance before declaring the release complete.

---

## 18. Balance Targets and Acceptance Scenarios

The following scenarios should guide initial tuning.

### Scenario A — Ordinary ground survival

The player spends a night outdoors at ground level.

Aerial encounters may occasionally occur, but should not dominate the night.

The player must be able to pursue normal survival activities.

### Scenario B — Short jetpack flight

The player briefly flies over an obstacle and lands.

The system should not automatically generate a hostile encounter.

### Scenario C — Extended exposed flight

The player travels across the Overworld at substantial height above terrain.

An aerial encounter becomes possible.

The predator is visible, its behavior is understandable, and the player can react.

### Scenario D — Hard difficulty flock

The player travels through exposed airspace in a high-activity territory.

A coordinated small flock may attack.

The encounter should be dangerous but readable.

Attacks should not land simultaneously without counterplay.

### Scenario E — Ground retreat

A player under aerial attack descends and seeks cover.

The predators should not continuously force combat after the player reaches genuine shelter.

### Scenario F — Rain exposure

The player uses an Electric Jetpack in ordinary rain.

Prolonged exposure can produce recognizable instability and an occasional recoverable interruption.

### Scenario G — Thunderstorm

The player travels during severe weather.

Electrical instability becomes more significant.

The player receives sufficient warning and retains meaningful options.

### Scenario H — Combined storm and flock

The player encounters aerial predators while weather is affecting the jetpack.

The encounter must not create an unavoidable chain of stun, fall and death.

### Scenario I — Lunar event

A qualifying lunar encounter includes an appropriate limited aerial component.

Ground encounter constraints and existing event authority remain functional.

---

## 19. Open Design Decisions

The following details require owner approval or prototype evaluation.

**Flying creature identity**

What should the first species look like and what should it be called?

Should its aesthetic be primarily natural, corrupted, skeletal, or subtly Ender-influenced?

**Altitude thresholds**

At what height above ground should encounters become plausible, and how quickly should exposure accumulate?

**Flock size**

What is an appropriate maximum group size in Normal and Hard?

**Ground attacks**

How frequently should ordinary ground-level players encounter diving predators?

**Jetpack fault behavior**

Should temporary malfunctions reduce thrust, interrupt it intermittently, or block thrust briefly while preserving emergency descent control?

**Weather status display**

Should instability appear primarily in the inventory, HUD, equipment tooltip, or a combination?

**Lunar integration**

Should aerial groups appear in the first 0.2.2 implementation of full-moon assaults or initially remain restricted to ordinary ecology?

**Future Skelerakk-like creatures**

Should skeletal flying predators be a separate species, a rare variant, or part of a dedicated future encounter family?

Agents should not silently resolve major creative decisions merely because one implementation is easier.

---

## 20. Explicitly Deferred Scope

The first 0.2.2 release should not require:

- A large flying bestiary.
- Fully developed Skelerakk-like variants.
- Complex persistent nests.
- Global migration patterns.
- A new formal sky biome.
- Full ecological simulation of airborne food chains.
- New jetpack crafting tiers.
- Permanent jetpack damage.
- New mandatory storm-protection machines.
- Complete aerial siege systems.
- Settlement recognition.
- Expansion of Saboteur industrial targets.

These features may become relevant in later releases.

The immediate goal is one coherent, playable aerial ecology system.

---

## 21. Release Acceptance Criteria

Survival 0.2.2 should be considered functionally successful when:

1. At least one original non-anthropomorphic flying predator is fully implemented.
2. It can cruise, detect, circle, dive, recover, and disengage.
3. Aerial encounters respond meaningfully to exposure above terrain.
4. Ground-level players do not experience excessive aerial harassment.
5. Appropriate shelter provides meaningful protection.
6. Flocks can operate with bounded, coordinated behavior.
7. Difficulty and territorial activity influence aerial pressure.
8. The Electric Jetpack can experience temporary rain-related instability.
9. Malfunctions are observable, recoverable, and avoid unavoidable fatal falls.
10. Jetpack operation remains consistent with its existing behavior outside the hazard.
11. Aerial ecology coexists safely with existing hostile spawning and lunar events.
12. Performance remains acceptable on the target modpack.
13. Appropriate debug information supports gameplay verification.
14. Compilation, packaging, runtime testing, and owner playtests confirm the essential experience.

---

## Final Creative Objective

The world beneath the player is already becoming more dangerous through territorial hostility and the expanded Enderkin ecosystem.

Survival 0.2.2 should make the air above that world feel equally alive.

A player flying across a forest at night might notice distant silhouettes circling above the treetops.

A predator may descend, miss, climb, and prepare another attack.

A storm might force the player to reconsider an extended flight.

A high-altitude journey through a dangerous region might attract a coordinated flock.

But the player should always retain meaningful choices.

They may fight, evade, descend, take shelter, change route, or rely on advanced technology.

**The sky should become worth exploring, worth respecting, and occasionally worth fearing — without taking away the freedom that makes Tekkit flight enjoyable.**