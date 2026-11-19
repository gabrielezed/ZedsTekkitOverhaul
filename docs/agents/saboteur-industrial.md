# Saboteur industrial integration

## Survival 0.2.3-alpha.2 dismantling

On Normal and Hard, one in three eligible actor scans considers physical removal. The actor selects a visible block or multipart face, moves within 2.4 blocks and signals with an orange link and sparks for 35 ticks. A hit, obstruction, lost reach, replacement, unloaded neighbor or unsafe transport state cancels the attempt. The actor can carry one successfully removed component in its lifetime. It takes a short evasive route and tries a nearby clear recovery point after 100 ticks. Death uses the same release check. Cargo stays with the actor until a release succeeds, including across saves.

The natural selector keeps the five incidental Saboteur tickets and 106 industrial tickets within the existing roll. Exposed supported cables, pipes, tubes and wires qualify without an extractor. The selector bounds the loaded tile and sight checks. The actor validates the individual part and its buffers. This supports the Project Vision's controlled griefing and practical defenses while keeping the industrial targeting contract independent of lunar and future settlement ownership.

| Installed family | Eligible variants and states | Native removal and recoverable item | Focused fixture |
| --- | --- | --- | --- |
| BuildCraft 2.2.14 item pipes | Wood, cobblestone, stone, iron, gold, diamond and obsidian. Empty transport and restore queues, empty filters, and idle power provider where applicable. | Notified `BlockGenericPipe` removal, tile invalidation and persistent registry teardown. The registered `ItemPipe` with damage 0. | `verifyPipe` for every variant. The original item-line scenario checks committed items and chest delivery. |
| BuildCraft liquid pipes | Wood, cobblestone, stone, iron and gold. Empty center and six side buffers, including ready amounts and both travel queues. Wooden extraction credit and stored power must be idle. | Same native teardown and exact waterproof pipe item. Native placement restores pipe topology. | `verifyPipe` and `verifyPipeThroughput`. |
| BuildCraft conductive pipes | Wood, stone and gold. No current or next power transfers or requests and idle extractor power. | Same native teardown and exact conductive pipe item. | `verifyPipe` and `verifyPipeThroughput`. |
| Additional Pipes 2.1.3 | Advanced wooden, advanced insertion, distribution, item redstone and liquid redstone. Same transport guards, empty advanced wooden filters and default distribution weights. | The add-on registers these classes with `BlockGenericPipe.registerPipe`. Exact registered add-on `ItemPipe` through the same teardown. | `verifyPipe` for the five registrations and explicit rejection of all three teleport variants. |
| IC2 1.97 ordinary cables | Unpainted, unfoamed copper, gold, iron, glass fibre and tin across installed insulation variants. Damage 0–10. Live transfer is allowed. | Native cable drop lookup followed by notified removal. Tile invalidation removes the conductor from EnergyNet. Exact placeable `ItemCable` damage. | `verifyIC2`, including a live glass-fibre branch and parallel route. |
| IC2 detector and splitter | Unfoamed damage 11 and 12. Detector output and redstone-disabled splitter use their respective native tile types. | Same cable teardown. Neighbor notifications and native EnergyNet membership resolve current redstone state. | `verifyIC2` checks active detector output and the redstone-disabled EnergyNet state. |
| RedPower Machine 2.0pr5b2 tubes | Pneumatic, redstone, restriction and magnetic. Empty flow and no paint channel. | `TileTube.onHarvestPart` at index 29 preserves covers and produces the exact `ItemMicro`. | `verifyRedPower`, `verifyTubeTraffic` and `verifyCoveredParts`. |
| RedPower copper fluid pipe | Center part with zero level and delta. | `TilePipe.onHarvestPart` preserves covers and notifies neighbors. Copper pipe micro item. | `verifyRedPower`. |
| RedPower BluePower | Exposed blue alloy face wire or jacketed center. | `TileWiring.onHarvestPart` for one face or center. Exact wire or jacket material metadata. | `verifyRedPower` and `verifyJackets`. |
| RedPower red alloy, insulated and bundled signal wiring | Red alloy face wire or jacketed center, all 16 insulated colors, all 17 bundled face variants and jacketed bundled center. | Same face-specific native harvest. Exact color or jacket material metadata. | `verifyRedPower`, `verifyJackets`, `verifyWireColors`, `verifyCoveredParts` and red alloy `verifyWireSignals`. |
| RedPower Control ribbon | Exposed ribbon wire face. | Inherited native wiring harvest and exact ribbon micro item. | `verifyRedPower`. |

The RedPower recovery path takes ownership only of the single matching item emitted at the harvested part during the native call. BuildCraft and IC2 use one native component stack without invoking a second harvest. Reinstallation follows each mod's ordinary item placement. BuildCraft orientation and other transient settings follow their native placement rules. The legacy cargo and one-theft NBT keys still load. If an optional item registry is unavailable, the actor retains the original stack data.

### Installed cases outside physical removal

- Additional Pipes item, liquid and power teleport endpoints maintain channel, owner and remote routing state that their item does not carry. Teleport Tethers own chunk-loading state.
- IC2 construction foam covers the cable and has a special block teardown drop. Painted IC2 cable and painted RedPower tube carry network colors only in the tile. Native drops cannot reproduce those channels. Stocked filters, custom distribution weights and live or queued transport states listed above are ineligible.
- BuildCraft pre-2.2.5 legacy pipe IDs convert through their native legacy tile tick. The resulting registered generic pipe can qualify. Docking Stations hold inventory and fluid.
- RedPower covered parts require the selected part to be the first native ray hit. Accelerators, pumps, grates, relay and inventory machines, moving frames and bundled I/O peripherals are combined or stateful machine blocks. The Relay retains its existing interruption path.
- TubeStuff supplies combined inventory and automation blocks. PowerConverters and CompactSolars supply endpoints and sources. Wireless Redstone supplies linked transmitter and receiver tiles. Railcraft supplies tracks and rail machines. These packs register no additional ordinary pipe, tube or cable segment in this installation.

### Owner playtest

1. In a disposable Survival world on Normal or Hard, run an item or EU line with one accessible segment. Use a Saboteur egg and switch to Survival. Keep a clear approach, watch for the orange link and strike during it. A fresh Saboteur lets you test a successful dismantle afterward.
2. Repeat with the same line inside a wall and roof, or with a RedPower cover facing the approach. Leave a nearby exposed comparison segment so you can see which part the actor selects.
3. Let the carrier flee, or defeat it before it escapes. Pick up one component and reinstall it. Compare the signal or downstream delivery before and after the repair.

The headless suite is `zto-tools/test-zto-agent.ps1 -Pack`. The graphical `theft-cable` fixture exercises the cue, cover cancellation and escape recovery in a fully initialized client. Run it with `-WithAgent` under the [guest input-isolation rules](testing.md#background-client-launch).

## Temporary interruption

In Tekkit Classic 3.1.2 the mod JAR provides the actor and native transfer gates. The managed Prism deployment adds the single ZTOAgent for direct industrial gates, using the same startup component as mouse gestures and moon rendering. [Installation](../deploy-prism.md)

A nearby Saboteur must reach and see the ZTO endpoint. Its 25-tick visible windup gives a Survival player time to interrupt it. A successful windup applies an exclusive lease for at most 80 simulation ticks. This gives the player a reason to shield vulnerable parts of a line and time a response, as described in the project's controlled-griefing direction. Endpoints remain local to the machine and actor. Future site encounters can use them through the same targeting contract.

## Supported layouts

- **BuildCraft items:** Set a native wooden item pipe to pull from a stocked ZTO Hopper. The admitted pipe has one adjacent chest and can have adjacent redstone engines. Junctions, other adjacent tile machinery and unavailable neighboring chunks make it ineligible. The Hopper recognizes a recent sided-slot query on the intake face. On that one face it temporarily reports zero accessible slots, which BuildCraft checks before withdrawing an item. Native power, the pipe's existing payloads and the chest remain under BuildCraft control. The dedicated layout limits the shared sided ABI to the extractor's input face.
- **BuildCraft liquids:** Set one wooden liquid pipe to pull from a stocked ZTO Distillation Receiver and run it until the receiver has a recent drain. The receiver declines new drain offers in its normal liquid API. Native power, delivery of buffered fluid, receiver intake and direct player bucket transfers continue. The legacy liquid API has no side argument, so the gate applies to automation through that receiver's drain API. Multiple wooden extractors make the receiver ineligible.
- **RedPower items:** Put a native Transposer with its input against a stocked ZTO Hopper and pulse the Transposer with ordinary redstone. The same sided-slot gate applies to the bound extraction face after a recent access. Pulses, native scheduling, tube routing and committed items remain under RedPower control. The Transposer reads the Hopper's inventory before withdrawing an item. The native Relay extracts from its own slots and has no such upstream withdrawal boundary.
- **EE2 and IC2:** Connect an idle lit EE2 Collector to a ZTO Alchemical Converter and an IC2 receiver to a free converter face. Recent accepted EU delivery qualifies the converter for a lease. While disrupted, it declines EE2 packet probes and commits at its receiver interface and withholds new EU offers at its source interface. The converter retains all stored units. A Collector with no other accepting neighbor may bank production in its own reserve, and other accepting paths remain under native EE2 control. The converter keeps its ordinary EnergyNet membership. Its panel displays the remaining interruption in ticks. The original packet-facing table had the north and south sources swapped; both offsets now follow the installed EE2 packet ordering.

The Hopper's own push gate still applies. Hopper intake, player inventory access and unbound faces retain their ordinary rules. The item lease releases when its exact extraction peer or supported layout changes. Player redstone and native machine settings stay under their original owners. There is no saved settings snapshot to restore. All three lease strategies use transient fields on their loaded ZTO tile, with ordinary invalidation, unload and reopening behavior. Registry identities, recipes and NBT formats require no migration.

## Direct hook ownership

The compatibility hooks restore the pinned pre-intervention boundaries listed below. An actor's 120-tick action cooldown outlasts each 80-tick lease. The extractor and its engine share one line claim. A ZTO-source pipe uses the native gate, including during a source or orientation change. A Collector adjacent to a ZTO converter leaves its link to the converter. A native storage unit's own downstream output remains distinct from EU arriving from a converter.

| Installed target | Gate | Focused runner mode |
| --- | --- | --- |
| BuildCraft 2.2.14 wooden item pipe, chest source | New `doWork` withdrawals | `direct-item` |
| BuildCraft 2.2.14 wooden liquid pipe, tank source | New credit and source drain, after native transport update | `direct-liquid` |
| BuildCraft 2.2.14 redstone engine with a distinct native extractor output | Engine work after base tile update | `direct-engine` |
| IC2 1.97 BatBox, MFE and MFSU | Withhold an EU offer and return its full remainder | `direct-eu`, `direct-eu-mfe`, `direct-eu-mfsu` |
| CompactSolars 2.3.2.10 LV, MV, HV | Withhold new EU offers | `direct-solar`, `direct-solar-mv`, `direct-solar-hv` |
| RedPower 2.0pr5b2 Relay | Pause new extraction before a buffer mutation | `direct-redpower` |
| EE2 1.4.6.5 Collector Mk1, Mk2 and Mk3 | Pause Collector tick work for the standalone layout | `direct-ee2`, `direct-ee2-mk2`, `direct-ee2-mk3` |

## Focused verification

The Saboteur probe uses a marked disposable save and checks the loaded candidate. Direct modes require the unified agent. Add `-WithAgent` to a native mode to verify coexistence. Host Windows graphical launches remain blocked until input isolation is verified. Run live fixtures in a separate guest with a verified launcher. [Runtime restrictions](testing.md#background-client-launch)

~~~powershell
.\zto-tools\test-saboteur-runtime.ps1 -Mode industrial-item
.\zto-tools\test-saboteur-runtime.ps1 -Mode industrial-redpower
.\zto-tools\test-saboteur-runtime.ps1 -Mode industrial-redpower-quit
.\zto-tools\test-saboteur-runtime.ps1 -Mode industrial-power
.\zto-tools\test-saboteur-runtime.ps1 -Mode industrial-power-quit
.\zto-tools\test-saboteur-runtime.ps1 -Mode transport
.\zto-tools\test-saboteur-runtime.ps1 -Mode transport-restart
.\zto-tools\test-saboteur-runtime.ps1 -Mode smoke -WithoutOptionalMods
~~~

## Historical native baseline and provenance

Baseline `bb48cad6155a681de1ac08f90f49ebfede9a4970`. Production source `bea34fba0854380fc596408c6d0fd54fa6478f35`. The candidate mod JAR SHA-256 is `461dd37d200b85ac9dacd1cb350adcdd2dccad2736088480a356ec3d2208cf48`. The sidecar flags unrelated working-tree entries. The production sources and build scripts match the production commit.

- **PASS — build and package.** Temurin JDK 8u504, FoxyRetroMDK 1.3.6, MCP 6.2 and development Forge 3.4.9.171 compiled and reobfuscated the client. Streaming validation checked 317 archive entries. The isolated staging mods directory received one JAR, with no agent classes or premain manifest entry.
- **PASS — portable checks.** The Hopper passed 21 MCP inventory and NBT assertions with IC2 and BuildCraft absent. The Alchemical Converter face and accounting checks passed.

Runtime used Tekkit Classic 3.1.2, Minecraft 1.2.5, Forge 3.3.8.164, FML 2.2.106.176 and Java 8u51. The full profile loaded 44 mods, including BuildCraft 2.2.14, IC2 1.97, CompactSolars 2.3.2.10, RedPower 2.0pr5b2 and EE2 1.4.6.5. JVM options were `-Xmx1G`, `-Dzto.dev=false`, `-Dzto.devBridge.enabled=false`, and paths for `java.io.tmpdir` and `java.library.path`. The fixture checked the running JAR path and spawned the Saboteur by its registered native identity. All runtime clients ran muted on private desktops.

| Result | Fixture | Before | During | Recovery and conservation |
| --- | --- | --- | --- | --- |
| PASS | BuildCraft wooden item pipe with redstone engine | Chest reached one item in 114 ticks and held two when the lease began. | Hopper held 59 items. Three committed items reached the chest and the engine completed three strokes. | Chest reached six items at tick 180 after application. All 64 items reconciled at each sample. |
| PASS | BuildCraft wooden liquid pipe with Distillation Receiver | Native tank received 580 mB and another 420 mB was in transit at application. | Receiver held 5,000 mB and declined 38 offers. The tank reached 1,000 mB, then held for 56 ticks. The standalone native tank line kept delivering. | Fresh delivery resumed 33 ticks after expiry. Both lines reconciled 6,000 mB of gasoline (liquid ID 3017). |
| PASS | Distillation Receiver process restart | Native liquid delivery and a live lease. | Saved after the in-flight buffer had drained to zero and the receiver had denied an offer. | A new process reopened with a clear lease and delivered fresh liquid after 35 ticks. Both 6,000 mB lines reconciled. |
| PASS | RedPower Transposer | Chest reached two items before application. | Hopper held 62 items. The chest gained zero across three redstone rising edges. | Chest reached three at tick 92. All 64 items reconciled at each sample. |
| PASS | RedPower Save and Quit | Lease active at checkpoint. | Saved 62 items in the Hopper and two in the chest. | Reopened with the lease cleared. A new redstone pulse moved the next item and all 64 reconciled. |
| PASS | EE2 Mk1 → converter → IC2 BatBox | BatBox reached 3 EU in four ticks and held 24 EU at application. | BatBox held 24 EU. Converter held 16 packet units. The isolated Collector banked 16 units per tick during the measured interruption. | BatBox reached 25 EU at tick 80. |
| PASS | Converter Save and Quit | Lease active at checkpoint. | Saved 23 EU in the BatBox, four converter packet units and 528 Collector units. | Reopened with the same values and delivered the next EU with the lease cleared. |

**PASS — reduced cold start.** The six-mod profile used the existing BuildCraft Core and IC2 prerequisites. It omitted BuildCraft Transport, RedPower, EE2 and CompactSolars, loaded the same candidate and spawned the registered Saboteur.

Owner balance playtest, dedicated multiplayer, and separate Mk2 and Mk3 throughput fixtures are **NOT RUN**.
