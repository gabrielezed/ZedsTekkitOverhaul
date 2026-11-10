# Coding agent handbook

This directory is the **project-specific technical reference** for coding agents and future contributors. The canonical executable instructions are the repository-root [AGENTS.md](../../AGENTS.md). These pages provide context; they do not override the actual source code.

## Read by task

| Task | Read first |
| --- | --- |
| Starting a new work session | [Architecture](architecture.md), [Environment](environment.md) |
| Updating blocks, recipes or GUI | [Architecture](architecture.md), [Compatibility](compatibility.md), [Testing](testing.md) |
| Crafting and inventory UX, NEI or Inventory Tweaks | [Implementation checklist and agent prompts](crafting-inventory-ux.md) |
| IC2, EE2, BuildCraft or EMC changes | [Integrations](integrations.md), [Versioned API reference](../reference/tekkit-classic-3.1.2/README.md), [Compatibility](compatibility.md) |
| Fractional distillation or fluid models | [Distillation handoff](distillation.md), [Engineering spec](../engineering/fractional-distillation-v1.md), [IC2 API](../reference/tekkit-classic-3.1.2/ic2-1.97.md) |
| Building, deploying or fixing a crash | [Environment](environment.md), [Testing](testing.md) |
| Planning multiblocks or custom rendering | [Roadmap](roadmap.md), [Architecture](architecture.md), [OBJ prototype](obj-renderer.md), [Legacy lightmap](lightmap-diagnostics.md), [Multiblock validator](multiblock-validator.md) |
| Implementing the lunar calendar, moon rendering or debug menu | [Lunar implementation handoff](lunar-cycle-implementation.md), [Original lunar specification](lunar-cycle-system.md) |
| Using or extending the Dev Bridge | [Read-only diagnostics](dev-bridge.md), [Opt-in test automation](test-automation.md), [Singleplayer acceptance](singleplayer-acceptance.md) |
| Making a Git commit or pull request | [Workflow](workflow.md), [Testing](testing.md) |

## Evidence levels

Separate these four states in notes and PRs:

1. **Implemented** — code is present in the repository.
2. **Compiled** — a build was run successfully in the named environment.
3. **Runtime-tested** — behavior was directly observed in an actual game.
4. **Planned** — design intention only; do not represent it as implemented.

Project documentation may become outdated. When documentation conflicts with source, **inspect the source** and update the documentation after the discrepancy is understood.

## Language

Everything committed to this repository is in **English**. Chat with the owner may remain in Italian. Do not translate API identifiers, class names, item registry keys, or on-disk configuration names merely for style consistency.

## Version-pinned external references

Read [Tekkit Classic 3.1.2](../reference/tekkit-classic-3.1.2/README.md) before creating external-mod adapters. Pages distinguish inspected API declarations from unverified, version-specific research leads; use the [source policy](../reference/tekkit-classic-3.1.2/sources.md) before committing dependency material.
