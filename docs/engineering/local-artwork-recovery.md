# PR #50 local artwork recovery

The approved candidate was source `3ae9b2bf24db47370d62237b6c20c1584e2090c2`, mod SHA-256 `58c184005d5f605e7d262fe93daffa6d998e3c98bb2c107e6b5be475f66baf34`. The local development branch retained the commit. Identical copies of the approved JAR were recovered from the refinery follow-up staging directory and a Tekkit Dev deployment backup.

The Motorboat branch inherited PR #50's public artwork when it started from the merge into `main`. The build chose the tracked PNGs and preserved only the older optional Hopper and Creative groups. The four approved private machine PNGs therefore reverted on deployment even though the public Git comparison showed no texture changes relative to the merge.

## Package audit

| Compared with the installed Motorboat build | Observed result |
| --- | --- |
| Distillation block atlas and Compressor cap, side and grille | Four changed PNG entries. Each approved byte hash matches the recovered JAR, the local Git source and the deterministic regeneration from the pinned IC2 and BuildCraft donors. |
| Refinery geometry, rotations, operating indicators, fluid levels, depth offset, sounds, classic panels and Thermometer | Existing Distillation classes, GUI, indicator and sound entries match byte for byte. |
| Air Compressor operation, facing and legacy metadata, diving recipes and function | Underwater classes and all other art entries match byte for byte. This includes all three item icons, their worn layers and the Compressor panel. |
| Build watermark, native branding, agent hooks, Hopper and Creative | All corresponding mod entries match, except the expected build provenance record. The 74 file entries in the old and new agent JARs match by content. The whole JAR hashes differ. |
| Motorboat | The package adds the Motorboat classes and resources. The five changed existing class entries belong to configuration, mod bootstrap or sound registration. The config diff adds the new registry keys and retains all existing IDs. |

There were zero removed entries. The additional deployment failure was the artifact-directory default evaluated in the script parameter block. The build now supplies the absolute artifact path and the installer resolves its own default in the script body. A Prism override installs both artifacts via the transactional installer. A loose staging directory is identified explicitly in the build output.

The [profile manifest](../../artwork/manifests/local-dev.json) records full PNG and RGBA hashes, runtime paths and face maps. The [donor and edit records](../../artwork/manifests/machine-derivations.json) preserve per-face provenance. Owner builds apply the verified overlay automatically. This keeps the approved classic Tekkit machinery available while allowing the public baseline and future art changes to evolve deliberately.

## Owner playtest

1. In Prism, start `Tekkit Dev` and check the current build watermark. Open a disposable world prepared for testing.
2. Inspect the Compressor and the refinery from four sides. Check the Compressor grille, recessed refinery vessels, the Column rails and collars, and the Condenser and Receiver. Open both machine panels. Run a refinery batch and watch the fluid levels and indicators, then listen for the operating cues. Try the Thermometer controls.
3. Look at the Mask, Oxygen Tank and Flippers in inventory and while equipped. Recharge a tank and check the Compressor's oxygen and EU readouts.
4. Place and refuel the Motorboat, check the empty bucket return, then watch the hull and propeller while moving forward, reversing and coasting. Save and reopen to verify the held fuel.

The archive audit and headless tests establish resource and behavior continuity. The visual and audible checks above require a graphical client. [Deployment guide](../deploy-prism.md)
