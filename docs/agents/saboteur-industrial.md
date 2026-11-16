# Saboteur industrial integration

Tekkit Classic 3.1.2 singleplayer, Minecraft 1.2.5, Forge 3.3.8.164 and FML 2.2.106.176. This revision keeps the Creative Saboteur in 0.2.0-alpha.5 and uses the same encounter and spawn-egg entry points.

## Supported operations

| Installed mod | Target configuration | What the disruption withholds |
| --- | --- | --- |
| BuildCraft 2.2.14 | `TileGenericPipe` with `PipeItemsWood` facing a stocked vanilla chest, powered by a directly adjacent redstone engine | New extraction at `doWork`. Traveling items still update, route and enter their destination. |
| BuildCraft 2.2.14 | `TileGenericPipe` with `PipeLiquidsWood` facing a stocked BuildCraft `TileTank`, powered by a directly adjacent redstone engine | New extraction work and draws from the source tank. The native transport and its side and center buffers still update and deliver. |
| BuildCraft 2.2.14 | `TileEngine` with `EngineWood` facing a wooden extractor and no other adjacent tiles | Redstone-engine generation and piston strokes. The stroke and stored power remain in the engine until it resumes. |
| IC2 1.97 | Charged BatBox, MFE or MFSU with an adjacent output connection and an output-enabled redstone mode | The storage unit withholds its ordinary EU offer. The receiver, station slots, input side and EnergyNet registration retain native behavior. |
| CompactSolars 2.3.2.10 | Daylit LV, MV or HV array with an adjacent EU connection | The array withholds its EU offer to EnergyNet. Sunlight tracking and local item charging continue. |
| RedPower 2.0pr5b2 | Stocked, unpowered `TileRelay` with an adjacent output tile | The relay stops extracting new stacks from its inventory. The inherited tube buffer can still drain and the relay still schedules ticks. |
| EE2 1.4.6.5 | `TileCollector`, `TileCollector2` or `TileCollector3` with usable light | The collector skips its local production, conversion and outgoing packet work. Its existing EMC, items and inbound packet handlers retain native ownership. |

The actor scans the same loaded eight-block radius on a 24-tick interval. It prefers a working extraction or machine endpoint over a drive engine and checks that it can see and reach the endpoint. The purple link identifies the target in the 25-tick windup. A Survival hit or obstructed approach cancels the attempt. The disruption gives off portal particles at the affected block and lasts at most 80 game ticks, about four seconds at 20 TPS. Defeating the actor, switching to Peaceful, unloading the endpoint or replacing its tile releases the lease. Save/Quit and restart start with the mods' ordinary serialized state. The lease has no NBT representation.

The legacy client lacks public cancellable callbacks at these boundaries. The companion agent instruments a small list of fingerprinted native classes when Java loads them. ZTO owns the decision and transient lease, and the pack continues to own the simulation. This supports fortification and tactical defense without coupling industrial targets to the lunar scheduler or assigning them settlement ownership.

## Scope limits

- BuildCraft routing pipes, junctions, pumps, stone and combustion engines, and other extraction or source configurations have no direct target. An extractor pause leaves its driver and ordinary BuildCraft power perdition running. Engine admission excludes adjacent machinery that could feed a stalled engine. The existing ZTO Distillation Receiver retains its own source gate.
- IC2 generators, machines, cables and splitters have no direct target. The native splitter changes EnergyNet registration with its redstone state. The storage integration leaves graph membership and cached paths under EnergyNet control.
- RedPower transposers and retrievers couple extraction to native redstone, scheduling or machine power state. Timers, bundled wiring and BluePower conductors remain outside the relay boundary.
- EE2 relays and condensers can receive packets outside their tick. Pausing their tick alone would leave those operations active. The tested intervention is on the collector's own work.
- Railcraft 5.3.3 is installed. Its Item Loader combines transfer success with progress counters and the signal that releases carts. Suppressing a transfer alone can change whether a cart is retained or dispatched. No Railcraft device is enabled. Forestry is absent from the installed mods directory.

## Build and run

Build with `zto-tools/build-and-deploy.bat` under JDK 8. Add this JVM argument to the isolated Prism Tekkit Dev instance. Use the full path to the companion JAR outside the mods directory.

```text
-javaagent:<MDK root>/dist-zto/ZTOIndustrialAgent.jar
```

Quote the entire JVM argument if its path contains spaces. The original Hopper and Distillation Receiver remain available with the base mod JAR. Native endpoints are available only when their hooks install successfully. A fingerprint mismatch logs the inactive hook while the device continues its normal behavior.

Run focused scenarios with `zto-tools/test-saboteur-runtime.ps1 -Mode industrial-item`. Other modes are in the runner's parameter list. The runner checks the packaged mod, starts an isolated muted client on a private desktop, and writes its measurements under `dist-zto/test-reports`. `-DiscardGame` removes that run's disposable copy after it finishes.

## Tekkit Dev measurements

The native lines ran before a real Saboteur applied the lease. The BuildCraft fixtures sampled a warmed native redstone engine and counted its strokes independently of the extraction rate. Recovery ticks measure when new output reached the receiver. The lease itself is bounded to 80 ticks.

| Result | Fixture | Before | During | After |
| --- | --- | --- | --- | --- |
| PASS | BuildCraft chest → wooden item extractor → chest | Four extracted in 114 ticks. One reached the chest. | Three native strokes and zero new items withdrawn. All three committed items reached the chest. 59 items stayed at the source. | The sink reached six items at tick 180 after application. All 64 items reconciled. |
| PASS | BuildCraft tank → wooden liquid extractor → tank | 1,920 mB drawn in 110 ticks. 1,260 mB reached the tank. | Three native strokes and zero new liquid drawn. The 640 mB in flight reached the receiver. The source retained 11,540 mB. | The sink reached 2,480 mB at tick 113. All 14,000 mB and the fluid type reconciled. |
| PASS | BuildCraft redstone engine powering wood extraction from a furnace | Four items extracted in 115 ticks. One reached the chest. | Zero strokes. Piston progress and stored engine power held. Three committed items arrived. | The sink reached six items at tick 182. All 64 items reconciled. |
| PASS | Native IC2 BatBox, MFE and MFSU, each connected directly to a matching unit | 32, 128 and 512 EU per tick, respectively. | Zero EU sent. No loss from the 24,000, 96,000 or 384,000 EU totals. | Normal packets resumed at tick 80. |
| PASS | CompactSolars LV, MV and HV arrays feeding matching IC2 receivers | 8, 64 and 512 EU per generating tick, respectively. | Zero EU sent. The receivers held 224, 1,792 and 14,336 EU, respectively. | Normal packets resumed at tick 80. |
| PASS | RedPower Relay → chest | One eight-item stack arrived in the first 11 ticks. | Zero new withdrawals, 24 items held in the Relay. | Another stack reached the chest at tick 82. All 72 items reconciled. |
| PASS | EE2 Collector Mk1, Mk2 and Mk3 | 16, 48 and 160 collector-counter units per tick in daylight, respectively. | Reserve held at 448, 1,344 and 4,480 counter units, respectively. | Each collector resumed at its original rate at tick 80. |

The IC2 lifecycle fixtures interrupted one attempt with a landed Survival hit. A later successful effect released one tick after actor death, Peaceful, native tile replacement or native chunk unload and reload. Save/Quit during the lease and forced process exit after a native save both reopened with 24,000 EU conserved and the next 32 EU packet delivered. A working EE2 Collector behind a sealed glass wall kept producing while the actor approached outside. A reduced mods directory with BuildCraft Core, IC2 and ZTO started with the industrial agent and passed the existing Saboteur exercise.

Lifecycle coverage used the shared lease with a native IC2 BatBox. Separate restart fixtures for the other devices, owner playtesting and dedicated multiplayer are **NOT RUN**.

### Provenance

MCP 6.2 client compilation, reobfuscation and streaming verification of 321 archive entries passed on JDK 8. The runtime reported Forge 3.3.8.164 and Java 8u51.

| Scope | Source commit | Mod JAR SHA-256 | Agent JAR SHA-256 |
| --- | --- | --- | --- |
| BuildCraft, IC2 storage, RedPower and EE2 throughput fixtures | `e485536f8258f7bf705a77da7a66941b0aac8dc4` | `eeb705ee0bf105abc59e7409264fca8ac9fd7946a11b05a0495bf88c310c0c2e` | `fa0d9fed18b8cad9583de8ab982fe20bd30c1d65beaf53a4113da0548a5402ed` |
| Final package with CompactSolars and IC2 update check | `c2fa378ce6fef21d90e268781725990409f9bd00` | `3bdf7bc8f858b9510f5a3cd1e23188126181110cbe8cf7ae360ec36f089d5dc0` | `1389314854ca71379abfdda1e72c2cbed7c8048c8a68903b3ada2c6863af4a81` |

The final agent emits byte-for-byte identical patches for the eight native classes from the first package. The build sidecars flag the working tree because it contained documentation edits and pre-existing untracked files. Packaged production source matches the commits listed above. The earlier geometry, removal and reduced-pack runs used mod JAR SHA-256 `d53331f46020d19b5a868a1bf8646bb5fdfbb2167e0aff8fce596f5cd5755ef5`.
