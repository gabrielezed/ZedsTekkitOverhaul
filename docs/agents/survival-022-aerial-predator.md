# Survival 0.2.2 aerial predator and ecology

The working designation is **Aerial Predator**. Its revised silhouette has a narrow ribbed trunk, articulated teal wings, fangs, thin talons and a forked tail. The design supports the project's intermittent danger. A pilot can take cover, descend, turn out of a dive or fight back while keeping the benefits of Tekkit flight.

## Ecology

The local Overworld session samples the Survival player once per second. The terrain estimate uses the cached vanilla precipitation surface at the player's column and eight surrounding columns within six blocks. The higher of the center and upper nearby surfaces serves as the reference. Water and tree canopies count as surfaces, and standing on a tall mountain or a constructed platform gives little or no clearance. Cover, water and missing chunks prevent admission. Nearby ridges can reduce exposure in valleys and along cliffs.

- After landing, open near-ground play must remain eligible for 30 seconds before an admission roll. On Normal with zero territorial activity, a check rolls 13 in 1,000 by day and 45 in 1,000 at night. Ground encounters admit a single creature. Some only cruise through. A committed ground hunt has one attack opportunity.
- Open flight builds a score from 0 to 100. The gain rises with height above the sampled terrain. An eligible low-air encounter needs score 32 and approximately seven blocks of clearance. Exposed flight enables flock rolls at score 60 and about 18 blocks. High sky starts at score 70 and about 36 blocks. Existing bands have lower exit thresholds to avoid flicker near terrain.
- Admission checks run at most every 15 seconds. New predators start behind the player's view with separated collision volumes and clear loaded approaches. An attempt tests up to eight formation sites, with one smaller formation as a fallback. The encounter director allows one group and up to five loaded predators at admission. It also accounts for local hostile pressure and the ordinary hostile cap, with a nine-slot ceiling above that cap to account for the native pass's existing overshoot. A congested area leaves less room for new predators.
- Blended Territorial Activity from the existing service contributes to admission and flock odds up to 24 points. Easy usually admits singles, Normal flocks contain two or three, and Hard can admit three to five. Larger flocks need extended clearance, and local hostiles still constrain the available slots.
- Each admitted encounter reserves a default 180-second quiet period. The timer counts down after the encounter ends during active, eligible Overworld play. It belongs to the save across reloads and territorial cells. Death, separation, cover, descending and the encounter time limit can end a group. No chunk remains loaded on behalf of a predator.

## Flight and combat

A hunting group approaches on parallel headings and circles in lanes with different radii and heights. Once a dive turn opens, a member first finds a launch position above the target, brakes, and aligns with a predicted intercept. It checks line of sight and short sections of the swept collision box before reserving the turn. A blocked lane prompts a short repositioning pass.

The 18-tick warning raises the wings and opens the jaw. If the route remains clear, the predator commits to a fixed trajectory with folded wings and extended talons. Damage still requires swept contact and a clear path. Normal difficulty deals six health points before armor, with a small horizontal impulse. A route blocked during the warning cancels commitment and returns the predator to staging after recovery. Two rejected warnings end that member's hunt. The existing hunt deadline also bounds repositioning.

The next member waits 64 ticks from the previous turn's release, including the 50-tick recovery. A ground hunter or flock member spends its attack opportunity when it commits. The group releases the turn for a cancelled warning or a missing member. Manual airborne hunters can commit twice.

## Sounds

Two irregular ambient calls play during cruise and glide. Warning, committed dive, hurt and death each have a separate synthetic clip. The packed sounds are mono PCM WAVs at 22,050 Hz. The client registers their JAR URLs with the Forge sound pool on initial load and on replacement of the sound manager. The included Node script `zto-tools/synthesize-aerial-sounds.cjs` regenerates all six clips.

## Placement, settings and saves

Use **Aerial Predator Spawn Egg** in Creative or NEI. Its configurable constructor ID is 22009 and its default stack ID is 22265. Use `naturalAerialEncounters` to control natural admission and `aerialEncounterCooldownSeconds` to tune the quiet period from 90 to 600 seconds. The Creative egg and manual predator behavior are available with either setting. [Configuration](configuration.md#survival)

Predators still save as `ZTO.AerialPredator`. Manual creatures retain their existing reload behavior. Naturally admitted creatures record their origin and a short retirement lifetime. They resume as passive flight on reload, with a fresh target state. Flock membership and the dive turn are local to the running encounter. The additional `zto_aerial_ecology` map data stores the cooldown. Older saves need no migration.

**Numpad 1 → Aerial Predator** shows the sampled clearance, exposure band, cooldown, last admission odds and territorial contribution, last rejection, active group and members, current dive owner, and the nearest predator's behavior, launch eligibility, most recent rejection and dive counts. The screen is non-pausing.

## Electric Jetpack weather

`electricJetpackWeather` enables the hazard by default in local Tekkit Classic 3.1.2. The existing ZTOAgent pins the installed IC2 1.97 `ItemArmorJetpack.useJetpack` method. After each successful native thrust call, ZTO checks that the caller is the exact equipped Electric Jetpack. IC2 owns activation, hover mode, the stored charge, EU use, horizontal motion and native fall bookkeeping. Unsupported bytecode leaves native operation available and the Debug Hub reports the missing hook.

Every ten ticks, the equipped pack checks the loaded chest-height column with the native rain and precipitation-height checks. The pack gains wetness only when it has supplied sustained airborne thrust. Clear air and shelter dry it faster than idle outdoor air in rain. Thunder doubles the wetness gain. The warning begins at 54/100 wetness. A fault needs at least 56 warning ticks, a score of at least 78 and current powered flight in exposed rain.

A fault lasts up to 22 ticks. Successful thrust sheds up to 0.22 blocks/tick of upward speed per call while leaving negative vertical velocity and native braking intact. Hold the normal jetpack control to brake a descent. The jetpack spends its normal EU for that braking and needs enough charge to operate. Landing ends the fault. The 120-tick post-fault recovery blocks repeats and reduces wetness. Equipment swaps, death, Creative, loading another world and reload clear the session state. Save data and item charge remain native to IC2.

Three original PCM cues mark the warning, short circuit and recovery. Warning and fault emit a small spark behind the player. The Survival inventory shows the equipped pack state beneath the panel. **Numpad 1 → Electric Jetpack Weather** shows the current weather, exposure, hook status, wetness, timer and propulsion restriction. The sound script regenerates the electrical clips with `node zto-tools/synthesize-aerial-sounds.cjs --jetpack`.

## Full-moon aerial participation

`lunarAerialEncounters` enables a bounded allocation at selected natural full-moon PLAYER_ASSAULT admissions. Easy retains ground forces. Normal can spend one slot. Hard can spend two with at least 12 blended Territorial Activity and enough room in the original total and active budgets. Selection uses a stable world seed and lunar day roll. Loaded airspace, overhead cover, ordinary hostile pressure, aerial population and the shared cooldown determine whether the selected slots can actually be spent. Slots that fail admission remain in the ground plan.

A lunar group reserves the existing aerial cooldown and shares the same encounter controller. An active wildlife encounter, a cooldown or a protected jetpack pilot blocks the lunar request. The group disengages if the player leaves its 48-block horizontal area, takes cover, or the parent assault ends or is interrupted. Reopening a consumed night follows the existing assault replay rules and preserves the shared cooldown.

During a fault and the first 40 ticks of post-fault recovery, the controller defers new wildlife and lunar admissions. Predators that are circling wait, and a predator still telegraphing a dive backs off into recovery. A committed dive follows its fixed trajectory, with the pilot retaining steering and powered braking.

## Verification

Run `zto-tools/test-zto-agent.ps1 -Pack` after producing the reobfuscated JAR. The marked disposable Tekkit worlds exercise egg placement and the original solo combat, terrain-relative exposure, day and night admission, passive wildlife, Normal and Hard flocks, attack-turn recovery after a member leaves, cooldown persistence, safe placement, and population limits. The combat fixtures also exercise eight approach bearings, a cancelled launch and retry, and actual commitments in Normal and Hard flocks. The audio probe uses the pinned Forge pool and WAV decoder and observes the world events for all five predator cue types. The new probes execute IC2 thrust and charge code at several heights, test weather transitions and a cancelled predator warning, and advance the native lunar calendar through admission, interruption and reload. Fixture rolls are controlled to exercise rare outcomes reproducibly. They do not measure encounter frequency in graphical play.

### Owner playtest

1. In isolated Tekkit Dev on Normal, run a disposable Overworld save. Fly briefly over buildings and trees, then hold an exposed route for 30–90 seconds. Compare the Debug Hub readout over plains, along a mountainside and beneath a roof. Stop on a high ridge and confirm the score decays.
2. Compare sustained high-air travel on Normal and Hard, including a territory with established activity. Watch a group approach and circle, then dodge a dive after the wings fold. Confirm a distinct warning precedes each attack and there is time to aim or evade between dives. Descend into cover and read the encounter cooldown.
3. Spend several daylight and nighttime periods outdoors at ground level. Look for occasional silhouettes and verify a ground incident ends after one windup. For repeatable visual checks, use the Creative egg, switch to Survival, and watch the creature navigate a forest edge, a cliff and open sky.
4. Watch the predator from directly beneath and at mid-distance in daylight and at night. Check the wing joints, gliding, the raised-wing warning, folded dive and recovery. With sound enabled, distinguish the occasional ambient call from the warning, the dive rush, and the hurt and death calls. Check actual IC2 Electric Jetpack evasion and save/reload during a flock hunt. Automated graphical clients require a separate input session. [Host input isolation](testing.md#background-client-launch)

5. With a fully charged Electric Jetpack, compare lift in clear air, a short rain flight, and prolonged exposed flight in rain and thunder. Hold jump through a fault at low and medium height and confirm that descent can be braked. Fly under a roof to dry the pack. Check the Survival inventory, the Debug Hub and all three sounds. Repeat after re-equipping and reloading. Try the fuel jetpack as the native control.
6. In an isolated save, cross a natural full-moon dusk outdoors and compare the aerial admission readout with an overhead roof and an existing flock. Look for one attacker at a time, check that ground waves continue and reopen an interrupted assault during the same night.