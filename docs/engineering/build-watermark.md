# Build watermark

The canonical product version comes from `BuildVersion.VERSION`. The build captures Git commit, source-dirty state, timestamp and compiler profile in `zto-build.properties`. The archive hash in the sidecar covers the finished archive.

`BuildIdentity` uses the class code source that provided `BuildVersion`. It hashes a file-based archive once and reads the corresponding embedded metadata. The first eight hexadecimal characters of SHA-256 follow `b`. Loose MCP classes can use the embedded Git commit prefix with `g`. If provenance is unavailable, the product version is shown on its own. An asterisk marks dirty source metadata. A build through `build-and-deploy.bat` emits metadata into `bin/minecraft` for the MCP classpath as well as the JAR.

The pinned Tekkit `xt` title screen renders FML branding up from the bottom-left edge. FML adds its own and Forge's versions, the OptiFine label on supported installs and the loaded-mod count. Copyright occupies the lower-right edge. The next free adjacent line is above the native left stack. At small scaled heights the ZTO label moves to the upper-left corner to clear the button rectangle.

The post-render FML `TickType.RENDER` callback runs after the legacy renderer draws the current GUI or world HUD. One client handler selects title or gameplay. The ModLoader GUI callback can also be invoked around screen lifecycle events and is reserved for its existing features. The in-game label is shown with the ordinary HUD, yields to F1, F3 and open screens, and leaves the optional Survival Debug HUD room beneath it. The calendar and single ZTOAgent retain their existing responsibilities.

Run `.\zto-tools\test-build-identity.ps1 -CandidateJar dist-zto/ZedsTekkitOverhaul.jar` to check loose classes, dirty metadata, two archives declaring the same version and the final JAR hash against its sidecar. The candidate check uses the pinned local pack classpath to exercise title placement at five scaled sizes.

## Owner walkthrough

1. Compare the first main menu with the prior build. Resize the window and set GUI Scale to Small, Normal, Large and Auto. Repeat with the installed OptiFine overlay. Confirm all native labels, buttons, copyright and splash stay legible.
2. Hash the actual JAR in the disposable profile using `Get-FileHash -Algorithm SHA256`. Match the first eight lowercase hexadecimal digits to the `b` prefix. Repeat with another JAR declaring the same product version. Compare dirty metadata against its sidecar.
3. Load a world and check the label in the upper-left corner. Toggle the Survival Debug HUD, F1 and F3. Open pause, inventory and a machine GUI. Close them, save and reload. Confirm that the watermark returns once and clears all other visible controls.
4. For loose MCP classes, prepare metadata after recompiling if needed with `.\zto-tools\build-provenance.ps1 -Metadata bin/minecraft/zto-build.properties`. The label shows the prefix `g` and the embedded Git commit. Repeat with a build whose metadata has `sourceDirty=true`.
