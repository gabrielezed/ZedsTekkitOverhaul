# ZedsTekkitOverhaul — Approved Release Direction (2026-11-12)

**Status:** Owner-approved strategic direction; proposed milestones and numbering, subject to validation and explicit release decisions.  
**Authority:** `PROJECT_VISION.md` governs gameplay intent. This document defines development priorities rather than implementation claims.

## Versioning convention

Keep semantic versioning. Use **minor versions for major playable system families**, not for every new entity or feature. Use successive `0.1.0-alpha.N` prereleases for focused expansions within the first Survival line. A beta means the chosen scope is feature-complete and under stabilization; an accepted final `0.1.0` closes that line. Do not increment the product version for every commit or PR, and do not create tags without owner approval.

## Survival 0.1 — playable hostile encounters

| Candidate | Intended scope | Acceptance signal |
| --- | --- | --- |
| `0.1.0-alpha.1` | Initial lunar PLAYER_ASSAULT | Already implemented in earlier PR; refer to existing runtime evidence |
| `0.1.0-alpha.2` | Bounded, meaningful waves and debug diagnostics | Implemented; further owner balancing feedback is useful |
| **`0.1.0-alpha.3`** | **Backport of mob held-item and armor equipment slots** | Zombie/Skeleton equipment visibly renders, affects combat, persists and drops correctly |
| `0.1.0-alpha.4` | First specialized tactical mob | A readable, counterable behavior beyond increased HP/damage |
| `0.1.0-alpha.5` | Additional encounter roles/loadout composition | Distinct tactical roles work together in controlled encounters |
| `0.1.0-alpha.6` | First practical defensive technology | One useful, optional fortification tool integrated with a real threat |
| `0.1.0-alpha.7` | Atmosphere, warning and first rare End anomaly | Recognizable anticipation and environmental storytelling |
| `0.1.0-alpha.8` | Combined survival gameplay pass | Combat, building, gear, defenses and recovery produce a repeatable fun loop |
| `0.1.0-beta.1` → `0.1.0` | Stabilization and acceptance | Representative normal play, save compatibility, pack runtime, sensible tuning |

Candidate numbers are planning labels, not promises to ship eight alphas; combine or defer items when evidence warrants it. The equipment backport was deliberately moved **ahead of** new mobs, rather than introducing it as a separate minor system.

## Later major systems

| Line | Major system | Essential scope |
| --- | --- | --- |
| `0.2.x` | Territorial Activity & Threat | Bounded local activity record, decay/saturation, separate threat assessment; no mandatory base markers |
| `0.3.x` | Settlement Recognition | Infer persistent player-associated sites from multiple optional signals; distinguish homes, mines and brief visits |
| `0.4.x` | SITE_SIEGE & Fortress Warfare | Real site targets, tactical objectives, controlled structure damage, meaningful fortification responses |
| `0.5.x` | Dangerous Exploration | Hostile regions, unusual sites and reasons to explore beyond base-building |
| `0.6.x` | Industrial/Economic Progression | Selected machines, cross-mod interoperability, EMC/EU balance, defense utility |
| `0.7.x` | The End Awakens | Enderman behavioral anomalies and environmental mystery deepen |
| `0.8.x` | Endgame & Dragon | End access, dragon encounter and meaningful advanced equipment preparation |
| `0.9.x` | Integration & Balance | Cross-system balancing, saves, reliability, performance, complete progression |
| `1.0.0` | Full Survival-Industrial Overhaul | Coherent experience from early survival through Endgame |

These lines mark the **primary architectural focus**, not strict bans on cross-cutting fixes or prototypes. Some End anomalies and industrial defenses can arrive during 0.1, while broader implementation comes later.

## Non-negotiable design rules

- Technology must answer real world threats but never be a mandatory factory checklist.
- Difficulty should change problems and behavior, not endlessly inflate stats as days or wealth increase.
- PLAYER_ASSAULT targets player vicinity; SITE_SIEGE targets a persistent inferred settlement. Never relabel the former as the latter.
- Settlement detection must not require claims, blocks, beds, standard doors or specific layouts.
- Respect Minecraft 1.2.5 / Tekkit Classic 3.1.2 ABI and save compatibility.
- No indefinite generic Dev Bridge/QA milestones; use focused, reproducible runtime tests.
- Prefer small complete PRs with explicit non-goals, evidence and acceptance decisions.

## Immediate authorized next step

Implement `0.1.0-alpha.3` **mob equipment backport** following `MOB_EQUIPMENT_BACKPORT_SPEC.md`, then proceed to first custom tactical enemy only after functional gear, armor rendering, combat effects and save/drop behavior have been demonstrated. Do not start territorial threat, settlement recognition or site sieges yet.

## Proposed repository integration

1. Add this file under `docs/agents/` and link it from `docs/agents/roadmap.md`.
2. Add `MOB_EQUIPMENT_BACKPORT_SPEC.md` under `docs/agents/` and link it in the agent handbook.
3. Supply `prompts/ALPHA_3_MOB_EQUIPMENT_AGENT_PROMPT.md` to the implementation agent (this prompt need not be committed if task instructions live outside the repository).
4. Record milestone outcomes in the changelog as features are actually accepted; do not mark proposed features as implemented.
