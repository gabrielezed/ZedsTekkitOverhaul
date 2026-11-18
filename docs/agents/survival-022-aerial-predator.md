# Survival 0.2.2 aerial predator and ecology

The working designation is **Aerial Predator**. The first prototype added the hooked bill, articulated teal wings, split tail and pixel-art atlas. The second iteration adds natural Overworld wildlife and coordinated hunts to that same creature. The design supports the project's intermittent danger: a pilot can take cover, descend, turn out of a dive or fight back while keeping the benefits of Tekkit flight.

## Ecology

The local Overworld session samples the Survival player once per second. The terrain estimate uses the cached vanilla precipitation surface at the player's column and eight surrounding columns within six blocks. The higher of the center and upper nearby surfaces serves as the reference. Water and tree canopies count as surfaces, and standing on a tall mountain or a constructed platform gives little or no clearance. Cover, water and missing chunks prevent admission. Nearby ridges can reduce exposure in valleys and along cliffs.

- After landing, open near-ground play must remain eligible for 30 seconds before an admission roll. On Normal with zero territorial activity, a check rolls 13 in 1,000 by day and 45 in 1,000 at night. Ground encounters admit a single creature. Some only cruise through. A committed ground hunt has one attack opportunity.
- Open flight builds a score from 0 to 100. The gain rises with height above the sampled terrain. An eligible low-air encounter needs score 32 and approximately seven blocks of clearance. Exposed flight enables flock rolls at score 60 and about 18 blocks. High sky starts at score 70 and about 36 blocks. Existing bands have lower exit thresholds to avoid flicker near terrain.
- Admission checks run at most every 15 seconds. New predators start behind the player's view with separated collision volumes and clear loaded approaches. An attempt tests up to eight formation sites, with one smaller formation as a fallback. The encounter director allows one group and up to five loaded predators at admission. It also accounts for local hostile pressure and the ordinary hostile cap, with a nine-slot ceiling above that cap to account for the native pass's existing overshoot. A congested area leaves less room for new predators.
- Blended Territorial Activity from the existing service contributes to admission and flock odds up to 24 points. Easy usually admits singles, Normal flocks contain two or three, and Hard can admit three to five. Larger flocks need extended clearance, and local hostiles still constrain the available slots.
- Each admitted encounter reserves a default 180-second quiet period. The timer counts down after the encounter ends during active, eligible Overworld play. It belongs to the save across reloads and territorial cells. Death, separation, cover, descending and the encounter time limit can end a group. No chunk remains loaded on behalf of a predator.

## Flight and combat

The same legacy movement, local steering, animations and collision-based dive apply to manual and natural predators. A hunting group approaches on parallel headings and circles in lanes with different radii and heights. Members can reserve one dive turn when lined up. The turn spans preparation and commitment. The next predator waits 64 ticks from its release, which includes the previous member's 50-tick climb and recovery. Each flock member can attempt one dive. Lost members release the turn, and surviving members can continue the encounter.

The warning raises the wings and opens the bill for 18 ticks with a high-pitched call. At commitment the predator leads the target once and follows a fixed trajectory with folded wings and extended talons. Damage requires actual swept contact and a clear path, and on Normal deals six health points before armor. The impulse is small and horizontal. Shelter, changes in direction and ranged attacks are useful counterplay.

## Placement, settings and saves

Use **Aerial Predator Spawn Egg** in Creative or NEI. Its configurable constructor ID is 22009 and its default stack ID is 22265. Use `naturalAerialEncounters` to control natural admission and `aerialEncounterCooldownSeconds` to tune the quiet period from 90 to 600 seconds. The Creative egg and manual predator behavior are available with either setting. [Configuration](configuration.md#survival)

Predators still save as `ZTO.AerialPredator`. Manual creatures retain their existing reload behavior. Naturally admitted creatures record their origin and a short retirement lifetime. They resume as passive flight on reload, with a fresh target state. Flock membership and the dive turn are local to the running encounter. The additional `zto_aerial_ecology` map data stores the cooldown. Older saves need no migration.

**Numpad 1 → Aerial Predator** shows the sampled clearance, exposure band, cooldown, last admission odds and territorial contribution, last rejection, active group and members, current dive owner, and the nearest predator's behavior. The screen is non-pausing.

Electric Jetpack weather faults, lunar aerial admission and additional species are later work.

## Verification

Run `zto-tools/test-zto-agent.ps1 -Pack` after producing the reobfuscated JAR. The marked disposable Tekkit worlds exercise egg placement and the original solo combat, terrain-relative exposure, day and night admission, passive wildlife, Normal and Hard flocks, attack-turn recovery after a member leaves, cooldown persistence, safe placement, and population limits. Fixture rolls are controlled to exercise rare outcomes reproducibly. They do not measure encounter frequency in graphical play.

### Owner playtest

1. In isolated Tekkit Dev on Normal, run a disposable Overworld save. Fly briefly over buildings and trees, then hold an exposed route for 30–90 seconds. Compare the Debug Hub readout over plains, along a mountainside and beneath a roof. Stop on a high ridge and confirm the score decays.
2. Compare sustained high-air travel on Normal and Hard, including a territory with established activity. Watch a group approach and circle, then dodge a dive after the wings fold. Confirm a distinct warning precedes each attack and there is time to aim or evade between dives. Descend into cover and read the encounter cooldown.
3. Spend several daylight and nighttime periods outdoors at ground level. Look for occasional silhouettes and verify a ground incident ends after one windup. For repeatable visual checks, use the Creative egg, switch to Survival, and watch the creature navigate a forest edge, a cliff and open sky.
4. Observe texture, wing motion and the call in the running client. Check both Creative catalogs, combat with an actual IC2 Electric Jetpack, and save/reload during an active flock. Automated graphical clients require a separate input session. [Host input isolation](testing.md#background-client-launch)
