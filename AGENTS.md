# Instructions for coding agents

Start with the [current implementation map](docs/agents/current-state.md), then open only the relevant [handbook entries](docs/agents/README.md). Read [PROJECT_VISION.md](PROJECT_VISION.md) before designing or implementing a feature. Distinguish approved design, proposals and observed implementation. Explain how a feature supports the vision and preserves future integration points.

## Compatibility and ownership

- Runtime is Tekkit Classic **3.1.2**, Minecraft **1.2.5**, Forge **3.3.8.164** and FML **2.2.106.176**. Development uses MCP **6.2**, Forge **3.4.9.171**, JDK **8**, Eclipse and FoxyRetroMDK **1.3.6**. Use legacy APIs and verify uncertain calls in the [matching pack artifacts](docs/reference/tekkit-classic-3.1.2/README.md).
- Original mod sources live under `src/minecraft/zed/`. Keep the mod entry point as bootstrap. Never commit or overwrite vanilla or Forge sources, external mod JARs, decompilations, reobfuscated game classes, saves or local configs. Isolate and verify unavoidable core hooks. Protect NEI's patched `gb.class`.
- Read the [environment guide](docs/agents/environment.md) before using production mod libraries in MCP. Preserve EE2/IC2 behavior, numeric IDs, recipes, EMC and NBT unless the task changes them. Document migrations.
- Present machine controls, status, units, capacity and actionable failures in the GUI. Preserve bucket transfers and held-block placement. Keep processing separate from presentation and respect the client/server boundary. Reserve logs for diagnostics.
- Keep lunar authority independent of client classes. Gameplay consumes snapshots and event causes. Keep scheduling, placement and diagnostics separate. PLAYER_ASSAULT has no settlement ownership.
- Read the [Dev Bridge guide](docs/agents/dev-bridge.md) before extending the protocol. Keep reads bounded and allowlisted, run them on the owning thread and keep credentials out of Git and logs.

## Workflow

1. Check the branch, working tree and relevant source. Keep edits focused and separate migrations from behavior changes where practical.
2. Use the [test tiers](docs/agents/testing.md). Run MCP compilation and reobfuscation before packaging with `zto-tools/build-and-deploy.bat`. Use isolated Tekkit Dev and marked disposable saves for runtime evidence.
3. **Automated Minecraft/Tekkit Dev clients must be launched silently by default. Never modify host-wide audio settings. Scope audio suppression to isolated test instances and verify it before running audible gameplay fixtures.** Follow the [silent launch procedure](docs/agents/testing.md#silent-client-launch).
4. Review the diff and report what actually ran. Keep portable, build, runtime and owner observations distinct. Add a focused runtime test for new gameplay where the isolated client can exercise it.
5. Write all repository content and commit/PR text in English. Communicate with the owner in Italian unless requested otherwise. Use focused commits and keep local/generated work out of Git.
