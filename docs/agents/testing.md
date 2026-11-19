# Validation and definition of done

Select tiers by the change. An integration candidate should cover release regressions in the pinned pack. Report PASS, FAIL and NOT RUN, with the tested source commit and JAR SHA-256. Record developer fixtures and owner observations separately.

| Tier | Scope and commands |
| --- | --- |
| 1 — Fast checks | `zto-tools/test-config.ps1` for migration and settings. `zto-tools/test-lunar-calendar.bat` for continuous cycles, event classification, scheduling, placement policy and role selection. Use `zto-tools/test-agent-docs.ps1` for current links, commands and provenance references. Use the crafting, EMC, Hopper and relevant asset or machine self-tests for those areas. |
| 2 — Build and packaging | `zto-tools/build-and-deploy.bat` runs MCP compilation, reobfuscation and streaming JAR validation. Use `ZTO_PRISM_MODS` to deploy to the isolated staging copy. Check the [provenance sidecar](VERSIONING_POLICY.md). |
| 3 — Targeted pack runtime | `zto-tools/test-config-runtime.ps1` checks actual migration and silent startup. `zto-tools/test-alpha6-runtime.ps1 -Modes assault-recurrence` checks recurring dusk and replay. The alpha.5 probe owns native bed/respawn and armor-transfer coverage. |
| 4 — Release regression | Run the alpha.6 and alpha.8 encounter modes and alpha.7 combat, Pulse and both Creative catalogs. Use the [Dev Bridge lifecycle](test-automation.md) for representative Hopper, crafting and distillation transactions and restart persistence. [Release commands](survival-alpha8.md#configuration-regression) |
| 5 — Stress and visuals | Alpha.7 `stress` and `visual` are targeted runtime probes. Use the [tactical guide](survival-alpha7.md) and [distillation rendering checklist](../engineering/distillation-rendering-and-binding-v2.md) for owner-facing visual review. |

Keep marked saves disposable. Keep output in ignored staging and report paths. Run graphical fixtures serially when timing matters. Verify that the process exits and report unresolved shutdown issues. Compare measurements for the same fixture and artifact when claiming a performance change.

The Saboteur runtime suite covers `exercise`, `restart` and `stress` modes. Both alpha.7 Creative modes check the Saboteur egg alongside the existing eggs. The [Saboteur guide](survival-02-alpha5-saboteur.md) describes the fixture and historical evidence. The Windows launch restriction below applies to all graphical runners.

Use `zto-tools/test-zto-agent.ps1 -Pack` to verify the pinned overlay and direct industrial hooks without a graphical process. It also compares ordinary hostile populations in matched quiet and occupied Tekkit worlds. The [compatibility manifest](compatibility-acceptance.md) tracks live gesture, rendering and throughput checks separately.

For the 0.2.3 prototype, the same headless pack runner checks the default biome generator, matched natural Enderkin candidate distributions, magpie behavior and Fugl chicken hunts. See the [biome ecology implementation and playtest](survival-023-implementation.md).

Use `zto-tools/test-zto-agent.ps1 -AerialSample -PackJava java` for the repeated ordinary-only aerial sample. `-CandidateJar` selects an archived mod for that sample. The controlled pack suite separately checks failed formations, predator warnings and recovery, lunar allocation, and the magpie scavenging and egg lifecycle.

For territory, run `zto-tools/test-territory.ps1` after MCP compilation to use the legacy NBT classes. Run `zto-tools/test-alpha6-runtime.ps1 -Modes territory-legacy,territory-threat,territory-elevated,territory-both` with the reobfuscated JAR in the isolated staging copy. The quiet and elevated modes use authenticated HTTP to compare game-thread observations with Bridge replies. [Territorial evidence](survival-02-alpha2.md#verification)

For concurrent assault pressure, run `zto-tools/test-alpha6-runtime.ps1 -Modes composition-disabled,composition-quiet,composition-ordinary,composition-elevated,composition-quantum`. These modes retain hostiles and compare the planned slots and actual insertions over authenticated HTTP. For Hard entrances use `composition-hard,composition-hard-iron,composition-hard-powered,composition-hard-retreat`. Use `composition-hard-open,composition-hard-balanced` to compare production and a fixture-only 50:50 mix. Use `-Modes composition-hard-baseline-open -CandidateJar <alpha.3 JAR>` for the historical all-skeleton comparator in a disposable instance. For selected-versus-inserted roles use `composition-quiet-enderkin,composition-ordinary-enderkin,composition-elevated-enderkin`. The staged role fixture remains available as `composition-both`. [Balance and evidence](survival-02-alpha4.md)

## Background client launch

The shared Windows launcher returns `INPUT_ISOLATION_UNVERIFIED` before any client, copy or live profile mutation. The retired Win32 desktop launcher only verified window placement. The legacy client can grab and recenter the mouse, and the installed LWJGL build has native cursor clipping and an AWT-parent exception to its foreground check.

Use the portable test tiers on the host. For a graphical pack test, use a separate VM with its own OS input session, an isolated Tekkit Dev profile, and host input integration disengaged. The host runners remain blocked and require a guest-specific launch integration before unattended graphical use. Run an interactive host test only at the owner's request.

The read-only [Windows input sampler](../../zto-tools/runtime-tests/BackgroundMinecraft.cs) records the input and thread desktops, foreground, focus and capture PIDs, cursor position and clip rectangle from the interactive session. Use it alongside process identity and exit checks when validating a replacement, including startup, normal exit, crash, timeout and force-stop cases. Keep test processes away from the host session while gathering evidence. See [workspace hygiene](workspace-hygiene.md) for fixture lifetime and evidence.

## Silent client launch

The installed Minecraft 1.2.5 client reads `music:0.0` and `sound:0.0` from its own `options.txt` before initializing SoundManager. The shared [preparation helper](../../zto-tools/runtime-tests/SilentMinecraft.ps1) writes both values before launch and can create a missing options file. It is used by all tracked automated graphical launch paths.

- The alpha.5, alpha.6, alpha.7 and config runners only mute per-run copies. Their clients assert both effective volumes are zero and the SoundSystem backend is absent. The configuration probe also calls music and sound playback before checking the backend again.
- The Prism lifecycle runner mutes only the isolated Tekkit Dev instance while it owns the launch. It restores the exact original options after exit. If forced termination prevents cleanup, close the client and use `zto-options-before-automation.bak` in the Tekkit Dev game directory to restore the original `options.txt`. Never run the lifecycle with an ordinary profile.
- Use a manual launch in a separate disposable profile for owner-requested sound design checks. Leave host-wide audio controls alone.

For historical release evidence use the [handbook index](README.md#historical-evidence-and-reference-material). For the current source and ownership map see [current implementation](current-state.md).
