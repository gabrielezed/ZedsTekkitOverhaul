# Alchemical Converter V1

Place one or more lit EE2 Energy Collectors against any face of the Alchemical Converter. Leave their fuel-upgrade slots idle to make their normal output available. Attach IC2 cables or a compatible receiver to any free face, including the top. Right-click with an empty hand to see the highest connected collector tier, output limit, EMC buffer and connection instructions. The prototype uses vanilla textures with a glowstone top and requires both EE2 and IC2 to register.

Craft one converter in a vanilla crafting table. Use three Glowstone blocks on the top row, a Basic Machine Block, an Advanced Circuit and a Basic Machine Block across the middle row, and one insulated Copper Cable in the bottom center. The recipe resolves IC2 item identities at post-initialization. The default block ID is 3024 under `block.alchemicalConverter` in `config/ZedsTekkitOverhaul.cfg`. Keep the assigned ID stable once a world uses it.

## Conversion and progression

| Collector | Maximum accepted EMC per face per tick | Maximum single EU offer | Full-light average before cable losses |
| --- | ---: | ---: | ---: |
| MK I | 0.2 | 1 EU | 0.8 EU/t |
| MK II | 0.6 | 3 EU | 2.4 EU/t |
| MK III | 2 | 8 EU | 8 EU/t |

The converter exchanges 1 EMC for 4 EU and stores up to 8 EMC. EE2 represents 1 EMC as 80 packet units. The converter keeps that integer representation for exact fractional accounting and save compatibility. A standalone IC2 Generator produces 10 EU per active tick in Tekkit Dev. MK I supplements an early workshop, MK II supports a modest continuous load, and MK III can feed an 8 EU/t distillation heater under ideal conditions and without cable loss.

The advanced circuit and two machine blocks require an industrial investment before any collector can feed a cable. The highest adjacent collector tier sets the shared EU packet cap. Extra collectors can refill the buffer faster and sustain that cap more consistently. Larger peak output requires additional converters.

## Integration contract

- EE2 1.4.6.5 collectors push into adjacent `ee.IEEPowerNet` tiles. The converter resolves the sending face from `receiveEnergy(int, byte, boolean)` and checks the adjacent tile against the three collector classes. It accepts whole packets that fit the per-face tick limit and shared buffer. EE2 simulates first and then commits a share among available neighbors. The converter never offers a relay bonus.
- The converter credits committed packets once, retains fractional conversion units and offers at most one packet per world tick to IC2 1.97. It spends energy only when the network consumes an offer. `EnergyNet.emitEnergyFrom(IEnergySource, int)` returns the unused remainder, as confirmed by the installed IC2 generator's caller. Network losses are accounted for by IC2. A full or disconnected receiver stops draining the buffer.
- Input and output are one-way, with no direct intake from relays, items, Klein Stars or stored EMC. The converter does not write to the EMC map. There is no EU-to-EMC path in this block. Energy remains attributable to EE2 collector production, and charging the converter competes with other destinations for the same collector output.
- The buffer is saved in tile NBT and discarded when the block is broken. The tile joins the IC2 network on its first world tick and unregisters on invalidation or chunk unload. Replacing the collector changes the output cap. Removing the collector pauses output until a supported collector is reattached.
- Both mod APIs are compile-only boundaries in MCP. The packaging script includes only ZTO code and resources.

## Verification

Run `zto-tools/test-alchemical-power.bat` for accounting, face mapping, tier limits, fractional carry, simulated inputs, partial network acceptance and NBT range checks. In isolated Tekkit Dev, place an idle lit collector against the converter and run a short IC2 cable to a drained BatBox. Test a horizontal input, a vertical input, and output through the top. Connect two collectors of different tiers and confirm the shared output cap. Disconnect the cable and verify the bounded buffer, then save and reopen to verify buffer and network recovery. Also test a relay, a full BatBox, and block removal. Record observed transfer separately from theoretical throughput and the compile-only checks.

The project owner confirmed the original single-input converter worked in Tekkit Dev. Tekkit Dev loaded 44 mods and logged converter registration at ID 3024 on Forge 3.3.8.164. The latest six-face build compiled, reobfuscated and deployed successfully. The self-test verifies the six source offsets, per-face accounting and the shared output cap. Side input, top output, the EMC panel and persistence still need a live-world pass with this build.
