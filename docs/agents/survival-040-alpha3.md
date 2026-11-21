# Survival 0.4.0-alpha.3 — Industrial infiltration

Natural and Debug Hub site sieges use the same force planner. The selected part supplies up to four exterior sectors. The planner samples twelve straight lanes and gives an accessible closed wooden door or gate priority. Each side also searches up to twelve blocks either side of its midpoint for a supported entrance. The existing bounded terrain search supplies a connected route when straight approaches fail. Each insertion rechecks support, clearance, local population and protection.

The director reserves the Breacher in the first wave and keeps it on the verified interaction lane. A Hard HIGH or MAXIMUM force reserves one Saboteur in the second wave. A missing target costs that slot. At the approach endpoint the planner checks loaded devices within the part against the existing sabotage and transport contracts. It permits up to four short native path checks. The entity enters play outside and has to reach its target and complete the native windup. A completed or interrupted assignment cannot start a second operation. Native cargo recovery and transient industrial leases retain responsibility for resources.

The 560-tick allowance now belongs to the final encounter phase. A stalled waypoint records a diagnostic and the actor retains its place under the cap. Owned actors suppress native distance and random despawn. Near the encounter deadline, actors still standing at their route endpoint can walk back along it. The deadline and explicit cancellation release remaining actors. Details show sector and insertion counts, industrial outcome, native despawns and per-cause removals including liveness.

## Verification

Run the pinned headless suite with `zto-tools/test-zto-agent.ps1 -Focus site-siege`. The industry subset is `-Focus site-siege-industry`. Its fixtures cover a stocked hopper behind a door, interruption during windup and a sealed workshop. They check the nine fuel items after reopen. The mixed-device fixture separately compares inventory, EU and fluid NBT. The waterfront fixture uses a real bridge, including a turn, and removes support to check route failure.

The Hard MAXIMUM natural fixture deployed 17 of 18 planned actors in three waves, with eight active at peak and a longest observed lifetime of 1,654 ticks. The absent industrial target accounted for one undeployed reserve. The director took 4.9 microseconds at p95 across 1,690 active ticks in this development sample. The industrial fixture also saved while a siege Saboteur carried a removed BuildCraft pipe. After reopening, killing the carrier recovered exactly one pipe and the original cell remained empty.

## Owner playtest

Use Survival on Hard in a disposable Tekkit Dev world. Confirm Settlement Recognition reports a site. Leave four accessible exterior sides, a closed wooden door and a stocked ZTO Hopper just inside with clear standing space. Open Numpad 1, select Site Siege, AUTO and MAXIMUM, then Call Site Siege. Watch the Breacher at the door and the second wave at the hopper. Compare the sectors, active population, removal reasons and industrial result in Details. Repeat with the entrance sealed and with the Saboteur interrupted during windup. Save and reopen to count stock.
