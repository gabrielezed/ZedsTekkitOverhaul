# Phase 3 · Configuring ZTO EMC

The EMC module uses the legacy client game tick after EE2 reports external initialization. It has no hard EE2 dependency. If the EE2 classes are absent, the module skips initialization. The adapter is scoped to the inspected EE2 1.4.6.5 client and single-player use. Dedicated server behavior needs separate lifecycle verification.

Place a UTF-8 file named `ZedsTekkitOverhaul-emc.txt` in the instance's `config` directory. Omitting the file preserves the compatibility defaults. The following one-line file enables baseline audit.

```text
zto-emc 1 AUDIT
```

Open an isolated world. Audit exports `ZedsTekkitOverhaul-emc-baseline.tsv` and `ZedsTekkitOverhaul-emc-energy.txt` under `config`. Each run replaces the previous reports. Copy these files away when comparing different pack revisions.

An APPLY file starts with `zto-emc 1 APPLY`. Blank lines and whole-line `#` comments are accepted. Directives have the form below.

```text
zto-emc 1 APPLY
INHERIT name:tile.stone 0
SET name:tile.ztoFoundry 0 16
DISABLE name:item.ingotIron *
```

- `name:` uses an exact untranslated item or metadata-specific internal name. If a name is shared, missing, or contains whitespace, use an explicit `id:<runtime-number>` after verifying the catalog. Names and IDs are resolved against the live item registry. Metadata uses a decimal number from 0 to 32767. The maintainer must choose meaningful metadata for each item.
- EE2 base and add-ons load first. ZTO fills missing or nonpositive metadata-0 mappings for the 5×5 table and Foundry with 128 and 16. `INHERIT` retains the resulting value. `SET` overwrites the selected mapping. `DISABLE` removes it. `DISABLE ... *` removes the entire item's mappings. Disabling one nonzero metadata requires that the final base cannot supply fallback. The collector fuel registry blocks `DISABLE` on its entries. Fuel pricing changes require the ladder checks in the economy review.
- Duplicate resolved keys, whole-item overlaps, unregistered or ambiguous identities, malformed files and invalid ranges fail the entire profile with diagnostics in the Forge Mod Loader log. The adapter stages edits in a detached map and publishes it once after validation. Positive explicit values are capped at 419,430 to leave room for a normal stack of 64 in EE2's 80× integer accounting. Inherited values and EE2's pre-existing accumulator limits are outside that validation.
- Restart the client to change or roll back a profile. Remove the profile or switch to AUDIT and restart to recover compatibility values. The module does not migrate IDs, inventories, learned identities, or stored EMC. Existing EMC retains its numeric balance and will buy at the current prices. An old learned identity may stop being offered under the current mapping and become eligible again after rollback. Restore a world backup if you need to undo transactions performed under a different profile.

## Verification matrix

| Check | Status |
| --- | --- |
| MCP client compilation, reobfuscation, ZTO-only packaging and deployment | Verified with `zto-tools/build-and-deploy.bat` in Tekkit Dev. MCP server sources and md5s are absent in this workspace. |
| Parsing, resolution, precedence, metadata fallback rejection and economic fixtures | Run `zto-tools/test-emc.bat` after recompiling. It uses the MCP client registry and the real ZTO batch model. |
| Startup, new world, baseline generation, save and reopen | Observed in Tekkit Dev, Forge 3.3.8.164, using a disposable creative world. |
| APPLY and NEI | A profile that tried to disable Oil Bucket was rejected and the stone tooltip remained at 1. A valid 12-rule profile then loaded and NEI showed stone at 2. Stone at 2 was a disposable test override. Tekkit Dev's profile file was restored to AUDIT. |
| Interactive acceptance pending | Exercise a new tablet and one that knows a disabled item, an already targeted condenser, and a collector under APPLY and rollback. Craft the table from four workbenches and the Foundry from the 16-cell perimeter. Run IC2 with demand and cooling and transport fixtures for each petroleum fraction. These GUI and machine fixtures are separate from the automated tests and audit probes. |
