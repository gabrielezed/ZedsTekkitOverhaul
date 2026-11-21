# Survival 0.4.0-alpha.4 — Persistent siege forces

## Battlefield lifecycle

Natural admission uses the existing full-moon boundary. One cycle residue in three can admit a siege, a nominal opportunity every 24 Minecraft days. Recognition, supported exterior access, a 16-day site cooldown and local hostile pressure still gate admission. Rejected site opportunities can fall back to PLAYER_ASSAULT. An enabled site siege always assesses threat at the recognized site. The experimental territorial switch still controls player-assault adaptation. An accepted siege consumes the boundary and starts the cooldown. Each successful insertion spends one wave slot. The finite post-deployment tactics window releases the encounter's controller when it ends. A physical withdrawal follows existing waypoints and releases tactical control when it reaches staging. Cancellation, player absence, region loss, and world changes also release control.

Siege infantry have dedicated native entity registry names, ZTO.SiegeZombie and ZTO.SiegeSkeleton. Their class marks them as siege infantry. Infantry and siege Enderkin write the recognized site ID as a positive integer under ZTOSiegeSite. Enderkin without a valid ID retain ordinary despawn behavior and their existing cargo exception. Native NBT preserves the concrete entity, health, position, and existing cargo or custody token. Native Peaceful handling still applies. The registry names and metadata affect new saves from this candidate. Older saves remain readable.

Survivors use their usual hostile combat behavior on release or reload. Their individual routes, leases, and live controller references are transient. The daylight exemption for manual infantry applies during its test approach task. Released or restored infantry are exposed to native daylight damage. Natural admission consumption is saved separately and interrupted forces do not replay. Loaded insertion caps count survivors along with ordinary mobs. Encounter diagnostics distinguish confirmed deaths from tactical releases. The director cannot count survivors in unloaded chunks.

## Ground and industrial behavior

Infantry may try a short exterior bypass when their next waypoint is blocked. The director tests at most two sides once per actor with the existing terrain sampler. Failed routes release the mob to local combat. The site ID, force budget, and structural permissions stay fixed. Saboteur slots fall back to infantry if the supported objective is unavailable. A site-controlled disruption lease checks the living actor's contact with the loaded target. Existing binding checks and lease time limits still apply.

Aerial siege allocation remains disabled pending a species-specific persistence test. Existing aerial groups use a transient encounter. Leaving it clamps the remaining wild lifetime to 240 ticks, and natural flight can then call setDead. A siege integration needs verified save/reload and survival after separation from that controller.

## Native evidence

The candidate was built from `110dc1367348becb918d631eea6a6c44b22b933a`. Mod JAR SHA-256 is `1fccf2d20d5c5637eaacea634b4c644ffe0f01ccdd7e1bca680f399cb46e99f6`. MCP 6.2 compiled and reobfuscated the client. Streaming verification read 487 entries. The headless fixtures use the Tekkit Classic 3.1.2 overlay with Forge 3.3.8.164 and Java 8.

| Result | Check |
| --- | --- |
| PASS | Portable configuration, lunar calendar, assault and patrol scheduling, and the handbook link check |
| PASS | Full pinned pack regression, Site Siege, escalation, industrial and terrain focus suites, plus native settlement and protection checks |
| PASS | Older than 560 ticks and alive after the three deployment windows, physical death, manual cancellation, retreat, native infantry NBT, native chunk eviction and reload, Save/Quit reconstruction, Peaceful after reload |
| PASS | Saved Thief custody and Saboteur pipe cargo recovered once after native death. Moving the defender out of the machine room preserves the valid site lease. |
| NOT RUN | Graphical gameplay and automatic chunk eviction in the live client, and aerial siege persistence. The headless fixture moves beyond the site and uses the native provider to queue and save the actor chunks, then reloads them and checks the exact infantry count. |

The historical alpha.3 Hard MAXIMUM result was 4.9 µs at p95 for active director ticks. In the alpha.4 escalation focus, the matched scenario shape had 1,690 active samples, 8.6 µs at p95 for the director, 87.5 µs at p95 for planner samples, and 114.6 µs at p95 for aggregate native hostile ticks. It peaked at nine loaded mobs in the fixture. These are development samples from separate runs. Dedicated idle, post-release, post-reopen and multi-site resource profiling remains outstanding.

Diagnostics show inserted actors, controller ownership, confirmed deaths, releases and objective outcomes. A release is an observation at detachment. A persistent population census, per-entity serialization tracing, broader group replanning and specialist route recovery remain outstanding.

## Owner walkthrough

1. In a disposable Tekkit Dev save, build a recognized industrial site and set Hard. Use Numpad 1, Site Siege, Call Site Siege with MAXIMUM intensity. Test a natural Hard siege separately.
2. Leave several attackers alive as the final wave ends. Compare the inserted, confirmed death and tactical release counts in Site Siege Details. Confirm survivors remain dangerous after the director releases them.
3. Repeat with Cancel Test Siege after insertion. Confirm reinforcement stops, then save and reopen in the same area. Compare surviving types and health. Walk far enough away for chunks to unload and return. Test the daylight and Peaceful rules separately.
4. Block an exterior approach during the siege. Verify local movement or a living fallback. Compare an accessible supported industrial installation with a sealed one. During disruption, move away from the machine while staying near the site. Recover stolen cargo once through ordinary combat.
