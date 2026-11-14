# Coding agent handbook

Start with the [root instructions](../../AGENTS.md) and [current implementation map](current-state.md), then pick the relevant row. The [Project Vision](../../PROJECT_VISION.md) governs design. Current feature contracts and runbooks govern implementation and verification. Resolve discrepancies against the active branch and approved design before changing behavior.

| Task | Current references |
| --- | --- |
| Settings, build and deploy | [Configuration](configuration.md), [environment](environment.md), [build provenance](VERSIONING_POLICY.md) |
| Selecting checks and launching a client | [Test tiers and silent launch](testing.md), [Dev Bridge lifecycle](test-automation.md) |
| Calendar, world ownership or rendering | [Lunar implementation](lunar-cycle-implementation.md), [lunar specification](lunar-cycle-system.md) |
| Natural encounters and replay | [Survival contract](SURVIVAL_SIEGE_SPEC.md), [alpha.8](survival-alpha8.md), [HUD](survival-debug-hud.md) |
| Tactical mobs and mob equipment | [Tactical guide](survival-alpha7.md), [equipment contract](MOB_EQUIPMENT_BACKPORT_SPEC.md) |
| Inventory, crafting and Creative | [Crafting UX](crafting-inventory-ux.md), [Creative backport](../engineering/creative-inventory-backport.md) |
| Machines, rendering and external APIs | [Architecture](architecture.md), [integrations](integrations.md), [distillation](distillation.md), [pack ABI](../reference/tekkit-classic-3.1.2/README.md) |
| Diagnostics protocol | [Dev Bridge](dev-bridge.md) |
| Future territorial design | [0.2 design contract](territorial-threat-design.md), [roadmap](roadmap.md) |

## Historical evidence and reference material

[Alpha.1 assault](survival-assault-acceptance.md), [alpha.2 playtest](survival-assault-alpha2.md), [equipment acceptance](mob-equipment-acceptance.md) and [singleplayer acceptance](singleplayer-acceptance.md) record earlier observations. The alpha.5 [beds and composition](survival-alpha5.md), alpha.6 [outdoor placement](survival-alpha6.md) and alpha.7 tactical guide preserve feature-specific procedures alongside historical results. Open earlier notes when their artifacts, compatibility findings or fixtures matter. Screenshots, past proposals and the pinned API catalog are supporting references. Keep local saves, copied binaries and reports in ignored staging paths.

Report the tested source and artifact, and distinguish portable, build, actual pack and owner evidence.
