# Survival 0.4.0-alpha.4 — Persistent siege forces

## Battlefield lifecycle

Admission consumes the existing natural lunar boundary and starts the site's cooldown. Each successful insertion spends one wave slot. The finite post-deployment tactics window releases the encounter's controller when it ends. A physical withdrawal follows existing waypoints and releases tactical control when it reaches staging. Cancellation, player absence, region loss, and world changes also release control.

Siege infantry have dedicated native entity registry names, ZTO.SiegeZombie and ZTO.SiegeSkeleton. Their class marks them as siege infantry. Infantry and siege Enderkin write the recognized site ID as a positive integer under ZTOSiegeSite. Enderkin without a valid ID retain ordinary despawn behavior and their existing cargo exception. Native NBT preserves the concrete entity, health, position, and existing cargo or custody token. Native Peaceful handling still applies. The registry names and metadata affect new saves from this candidate. Older saves remain readable.

Survivors use their usual hostile combat behavior on release or reload. Their individual routes, leases, and live controller references are transient. The daylight exemption for manual infantry applies during its test approach task. Released or restored infantry are exposed to native daylight damage. Natural admission consumption is saved separately and interrupted forces do not replay. Loaded insertion caps count survivors along with ordinary mobs. Encounter diagnostics distinguish confirmed deaths from tactical releases. The director cannot count survivors in unloaded chunks.

## Ground and industrial behavior

Infantry may try a short exterior bypass when their next waypoint is blocked. The director tests at most two sides once per actor with the existing terrain sampler. Failed routes release the mob to local combat. The site ID, force budget, and structural permissions stay fixed. Saboteur slots fall back to infantry if the supported objective is unavailable. A site-controlled disruption lease checks the living actor's contact with the loaded target. Existing binding checks and lease time limits still apply.

Aerial siege allocation remains disabled pending a species-specific persistence test. Existing aerial groups use a transient encounter and pass departure control to the bird's local behavior. A siege integration needs verified save/reload and survival after separation from that controller.

## Owner walkthrough

1. In a disposable Tekkit Dev save, build a recognized industrial site and set Hard. Use Numpad 1, Site Siege, Call Site Siege with MAXIMUM intensity. Test a natural Hard siege separately.
2. Leave several attackers alive as the final wave ends. Compare the inserted, confirmed death and tactical release counts in Site Siege Details. Confirm survivors remain dangerous after the director releases them.
3. Repeat with Cancel Test Siege after insertion. Confirm reinforcement stops, then save and reopen in the same area. Compare surviving types and health. Walk far enough away for chunks to unload and return. Test the daylight and Peaceful rules separately.
4. Block an exterior approach during the siege. Verify local movement or a living fallback. Compare an accessible supported industrial installation with a sealed one. During disruption, move away from the machine while staying near the site. Recover stolen cargo once through ordinary combat.
