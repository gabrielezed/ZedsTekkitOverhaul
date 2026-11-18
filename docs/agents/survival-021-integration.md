# Survival 0.2.1 integration campaign

The baseline was PR #36 at `93748e7a7f9a13295e0224ec29dcb4dd122900b1`, mod SHA-256 `0dbe7f5a064822a6a3bc089e8dbf8e5e9878f35e035a88537d33a683eb35ea12`. The [implementation guide](survival-021-implementation.md) describes the shipped gameplay. The coverage below records which checks ran and which still require graphical evidence.

## Reproduced defects

- **Tank acquisition.** A Tank placed behind an occluding cobblestone wall stayed idle because autonomous acquisition required a sight ray. The earlier narrow breach probe assigned the encounter target explicitly and bypassed that gate. On Hard, an eligible first obstruction now allows acquisition at short range. The probe also checks a two-block elevation difference and a short wall with an ordinary route around it. A target acquired before retreat could already trigger a breach in the long-wall fixture.
- **Lunar placement.** Both generated swamp sites and the forest site had exposed player positions and produced zero insertions on the baseline. Two straight, 24-block routes rejected every candidate that made it past the earlier checks in the sampled vanilla slots. In the forest, the approach adapter also treated the canopy heightmap as the only possible floor. Two bounded corridors now permit local turns and can follow walkable ground under vanilla leaves. Spawn columns still require open sky, valid ground, appropriate light and clearance.
- **Packaging.** MCP 6.2 emitted a compile error but the old batch workflow continued and published a partial archive. A deliberately malformed temporary Java source now stops the build before changing either published JAR or the staged copy. The guard requires the MCP completion marker and checks for compiler errors.

## Runtime coverage

`zto-tools/test-zto-agent.ps1 -Pack` runs repository probes on the pinned 1.2.5 overlay with Forge 3.3.8.164 and Java 8. The integration probe uses native entity updates for Tank, Skirmisher and Breacher. The assault fixtures advance the real calendar through a natural full-moon boundary and keep the actual spawned mobs in the world. They hold combat while measuring insertion and population caps. All natural terrain samples use world seed `32021` and the same placement RNG seed. Native entity light rolls can vary between runs.

| Natural full-moon fixture | Baseline insertions / candidates | Stabilized insertions / candidates |
| --- | ---: | ---: |
| Plains | 3 / 326 | 8 / 150 |
| Swamp A | 0 / 336 | 3 / 325 |
| Forest | 0 / 336 | 8 / 237 |
| Hills | 7 / 311 | 8 / 131 |
| Swamp B | 0 / 336 | 6 / 310 |
| Outdoors beside a closed base | 8 / 121 | 8 / 142 |
| Fully glass-covered annulus | 0 / 336 | 0 / 336 |
| Plains after territorial natural spawning | 8 / 231 | 8 / 171 |
| 28 live hostiles within local cap range | 0 / 0 | 0 / 0 |

These are paired representative sessions. Other stabilized trials at the same swamp sites reached eight and seven insertions respectively. The sampled terrain still rejects many columns because of roofs or invalid ground. The fully covered annulus rejected all 336 candidates for roof cover and the crowded control recorded 42 local-cap deferrals. The occupied plains had 103 ordinary hostiles before the first lunar wave, with enough local headroom for eight lunar insertions. The focused save check reopens an interrupted full-moon encounter and verifies that the same boundary is not replayed.

- **Tank scene results.** The Hard Tank acquired through the hidden cobblestone and raised barriers and first struck at tick 120. The retreat-behind-planks scene struck at tick 136. All three showed a 24-tick windup. The level walls opened in two strikes, leaving two adjacent columns clear, and the Tank then damaged the stationary target. The raised case spent three strikes and did not damage the elevated player. The short wall was bypassed and the obsidian wall remained intact.
- **Territory.** The native spawner suite compares quiet and sustained-activity caps, daylight restrictions, the disabled setting, saved ledger reload, absence decay and qualifying returns. The generated plains trial also runs ordinary spawning before admission and checks that the director freezes the actual assessment at dusk. A separate local control has 28 live hostiles within range and verifies a cap deferral before any placement attempts.
- **Enderkin.** Real ticks exercised a Skirmisher aiming, firing and changing position, plus a Breacher striking at a closed wooden door while leaving both halves intact. The focused combat probe checks damage timing, recovery, cancellation, protected blocks, the persisted breach limit and role-specific relocation. It also observes ordinary Enderkin insertions and the contextual Saboteur placement preference.
- **Thief and compass.** The suite checks actual one-item transfer, interruption, separate hotbar feedback, native pursuit on terrain with small obstructions, Anvil reopen with the debited inventory and the carrier, tagged-item recovery exactly once, the saved location at a distance and cache recovery. The trail tests exercise navigation around a wall, expiration, repeat activation and refusal to route into unloaded terrain.
- **Industry.** A pinned BuildCraft stone pipe line delivered all five payload items into a chest after incoming cargo cancelled a dismantle. The drained segment could be stolen and disconnected normally, survived Anvil reopen as Saboteur cargo and dropped once on death. The probe also checks rejection of a fluid pipe and the reload queue. The existing Hopper lease paused output without removing the block and allowed output after expiry.
- **Compatibility.** Full and reduced pack agent linkage retain the inventory dispatcher and the pinned industrial hooks. The crafting and drag-slot policy harness exercises the relevant rules. The installed Tekkit Dev profile was checked in read-only mode against the baseline JAR hashes and its one managed agent entry. The candidate is staged separately.

## Saboteur compatibility scope

| Target | Physical theft | Existing disruption or future work |
| --- | --- | --- |
| BuildCraft 2.2.14 stone and cobblestone item pipes | Empty stock pipe only. Active transport and queued reload cargo prevent teardown. | Network neighbors update through normal removal. The item can be reinstalled. |
| BuildCraft wooden extractors, liquid and power lines, engines, other pipes | Outside the theft scope. | Existing verified wooden item and fluid extraction and redstone engine gates still apply. Extending teardown requires separate transport and network checks. |
| ZTO Hopper, distillation and alchemical endpoints | Outside the theft scope. | Supported transfer leases remain available. |
| IC2 BatBox, MFE, MFSU and CompactSolars | Outside the theft scope. | Existing supported offer gates pause output. IC2 cables and physical disconnection require their own energy lifecycle rules. |
| RedPower Relay, tubes, wiring and machines | Outside the theft scope. | Relay and supported ZTO-bound extraction gates remain. Tubes and wiring need dedicated in-flight transport and topology checks before physical disconnection. |
| EE2 collectors and other infrastructure | Outside the theft scope. | Supported collector layouts retain the existing pause. Wider coverage needs a dedicated specification. |

See the [industrial compatibility record](saboteur-industrial.md) for the exact existing gate classes and historical live throughput checks.

## Owner verification

Automated graphical launch on the Windows host returned `INPUT_ISOLATION_UNVERIFIED`. The runtime campaign therefore leaves visual timing, real input, audio clarity and encounter feel for a separate graphical session in a disposable Tekkit Dev world.

1. On Hard in Survival, try a long four-block-high cobblestone wall with the Tank placed before it ever sees the player. Repeat by retreating behind planks and by climbing two blocks. Compare with a short wall it can go around and an obsidian wall. Strike the Tank during windup and check animation, impact and recovery. Repeat on Normal and compare the wall.
2. In a disposable world with seed `32021`, check swamps near `(-376, 63, -130)` and `(760, 63, -776)`, then forest near `(-255, 63, -392)`. Enter a natural full-moon dusk. Compare actual insertions and rejection reasons via **Numpad 1 → Survival & Assault**. Try open ground near a base and a covered courtyard. The owner's original one-in-113 swamp result did not include a rejection breakdown, so compare these controls with the original world if it is still available.
3. Compare quiet and occupied nights with the **Territorial Threat Monitor**. Save and return to a previously occupied cell, then repeat after a long absence. Observe local pressure, natural count, cap and lunar admission together.
4. Pursue a Thief through mixed terrain and use a held compass with a sneak press after losing sight. Save and reopen before recovery. Check a tagged Tekkit item and try repeated trail activation around trees or a wall.
5. Let a two-segment stone pipe line drain into a stocked machine area. Interrupt one Saboteur during its windup, then allow a theft, chase it and reinstall the recovered pipe. Verify that cargo keeps a flowing pipe safe. Check the separate temporary pause at a Hopper or supported extractor.
6. Open single and double chests and the crafting screen. Check Inventory Tweaks buttons and sorting, NEI clicks, drag previews and Shift armor equip. Check all five egg placements and Enderkin models, the moon and the Debug Hub in the same run.
