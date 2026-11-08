# Tekkit mod integration boundaries

**Version-specific references:** [Tekkit Classic 3.1.2 API handbook](../reference/tekkit-classic-3.1.2/README.md), with [IC2 1.97](../reference/tekkit-classic-3.1.2/ic2-1.97.md), [EE2 1.4.6.5](../reference/tekkit-classic-3.1.2/ee2-1.4.6.5.md), [BuildCraft 2.2.14](../reference/tekkit-classic-3.1.2/buildcraft-2.2.14.md), and [Forge 1.2.5](../reference/tekkit-classic-3.1.2/forge-1.2.5.md). Signature evidence and licensing are documented separately from these general integration boundaries.

## Existing integrations

### Equivalent Exchange 2 (EE2 1.4.6.5)

- `EE2ItemResolver` resolves three alchemical fuel items using reflection.
- `EE2FuelIntegration` supplies ModLoader furnace burn times for Alchemical Coal, Mobius Fuel and Aeternalis Fuel.
- The [EMC module](../engineering/emc-profile.md) waits for EE2 external integration and fills missing compatibility values of **128** for the Advanced Crafting Table and **16** for the Foundry. An explicit pack profile can override these defaults.
- Missing EE2 must not cause an unconditional startup crash in the vanilla MCP dev client.

### IndustrialCraft 2 (IC2 1.97)

- The original IC2 Generator uses furnace fuel burn time through the legacy callback path. The EE2 fuel integration was tested in a real Tekkit instance.
- `IC2EmcIntegration` contains experimental mappings and has no bootstrap caller. EE2's own IC2 add-on supplies the baseline.
- Future EU-consuming machines should isolate IC2-facing code from generic machine logic; test the exact IC2 API/classpath and energy rules before selecting an adapter.

### BuildCraft

The distillation boiler and receiver integrate with BuildCraft 2.2.14 liquids. The four ZTO fractions have transport identities. Their combustion registration is a separate concern covered by the [EMC audit](../engineering/emc-audit.md).

## Economic integrity

The [Alchemical Converter](../engineering/alchemical-power-v1.md) uses adjacent EE2 packets and the IC2 EnergyNet source API. It keeps conversion accounting separate from item EMC and applies per-collector throughput limits.

An EE2→EU converter or alternative thermal power source can create exploitative EMC/EU cycles. Before adding recipes or conversions, document input/output units, throughput, losses and whether any closed-loop path generates resources. These are design constraints, not currently enforced by a balance engine.

## Dependencies

Avoid introducing direct references to optional mod classes into the core bootstrap. Reflection is currently used where appropriate. If compile-time dependencies are required for new APIs, document how to obtain/remap them for MCP and validate startup both with and without the optional mod, as applicable.

## Fractional distillation

The distillation heater implements the IC2 1.97 `IEnergySink` interface. The boiler, condenser and receiver use the BuildCraft 2.2.14 liquid ABI. The converter implements EE2's adjacent power receiver and IC2's energy source. MCP uses compile-only placeholders for these APIs. Package only original ZTO classes and resources and verify network and transport behavior in Tekkit Dev.
