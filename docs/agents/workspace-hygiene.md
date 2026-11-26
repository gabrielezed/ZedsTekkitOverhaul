# Workspace hygiene

Run these commands from the MDK root.

```powershell
.\zto-tools\clean-workspace.ps1
.\zto-tools\clean-workspace.ps1 -Clean
.\zto-tools\test-workspace-cleanup.ps1
```

The audit reports logical file lengths and skips reparse points. Cleanup is limited to regular GUID-named direct children of `dist-zto/survival-hud-runtime/runs`. A marked run needs a completed manifest and matching saved evidence. A legacy run needs the exact fixture layout, a marker that identifies each save and matching external PASS logs. Legacy screenshots are copied to `dist-zto/test-reports/legacy-runtime-frames/<run-id>` before the game copy is removed. Runs with unverified ownership, incomplete evidence, explicit retention, links or open files stay in place. The command reports each decision and actual before/after bytes. Run cleanup with runtime clients and build processes closed.

The five direct runners create a run manifest before copying fixture inputs. They reuse the existing runtime classpath and natives, and exclude generated EMC audit exports and snapshots from the writable configuration copy. Each fixture owns its saves, configuration, harness and child process. Cleanup waits for the owned process to exit and runs after success, failure, timeout or setup errors that unwind through PowerShell. A hard kill of the runner can prevent cleanup. In that case use the manifest to inspect the owner process, the child and the evidence before touching the recorded directory. Use `-KeepGame` for explicit debugging retention.

Direct probe evidence goes in `dist-zto/test-reports/runtime/<run-id>`. Keep the two most recent successful console bundles per family, with a 64 KiB tail per log plus a JSON result and bounded observations. Preserve compact JSON results, captured screenshots, and crash and failure evidence for review. Preserve current acceptance evidence separately before pruning older reports. The Prism lifecycle uses its existing JSON report and cleans only the copy it created after verifying that Tekkit Dev has exited.

Keep the current deployable build and build provenance together. Preserve personal worlds, normal instances, the MCP toolchain, rare dependencies and `tekkit-mods-decompiles/`. Audit disk use when a new test fixture makes large artifacts. Review any superseded deployment backups individually with their source and provenance.

Before investigating a packaging or artwork regression, copy the installed mod and agent, published pair and sidecars, prior and replaced JARs, relevant deployment backups and private artwork evidence to a separate local directory. Record their SHA-256 hashes. Keep confirmed owner-approved candidates available for comparison. The build preserves replaced artifact pairs and the installer retains replaced Prism files. Review those copies before cleaning up.
