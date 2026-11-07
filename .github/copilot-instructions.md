# Repository instructions for GitHub Copilot

Use the repository-root [AGENTS.md](../AGENTS.md) as the canonical instructions and [docs/agents/README.md](../docs/agents/README.md) as the project handbook.

This is a Minecraft **1.2.5 / MCP 6.2 / legacy Forge and ModLoader** codebase, not a modern ForgeGradle project. Keep all repository content, Java comments, scripts, logs, commit messages and PR descriptions **in English**. Discussions with the project owner can be in Italian.

Only version original `src/minecraft/zed/` code, project documentation and project tooling. Never add third-party JARs, Minecraft/Forge sources, saves, decompiled mods, or binary overlays of vanilla/NEI classes.

Before changing behavior, check API signatures in the actual MCP environment and read the relevant handbook page. State clearly whether checks were run in MCP, in real Tekkit, or not run.
