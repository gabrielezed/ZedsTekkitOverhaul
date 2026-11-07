# Tekkit mod integration boundaries

**Version-specific references:** [Tekkit Classic 3.1.2 API handbook](../reference/tekkit-classic-3.1.2/README.md), with [IC2 1.97](../reference/tekkit-classic-3.1.2/ic2-1.97.md), [EE2 1.4.6.5](../reference/tekkit-classic-3.1.2/ee2-1.4.6.5.md), [BuildCraft 2.2.14](../reference/tekkit-classic-3.1.2/buildcraft-2.2.14.md), and [Forge 1.2.5](../reference/tekkit-classic-3.1.2/forge-1.2.5.md). Signature evidence and licensing are documented separately from these general integration boundaries.

## Existing integrations

### Equivalent Exchange 2 (EE2 1.4.6.5)

- `EE2ItemResolver` resolves three alchemical fuel items using reflection.
- `EE2FuelIntegration` supplies ModLoader furnace burn times for Alchemical Coal, Mobius Fuel and Aeternalis Fuel.
- `AdvancedCraftingEmcIntegration` attempts to register EMC **128** (Advanced Crafting Table) and **16** (Foundry) after EE2 external integration initialization, only if a value is missing.
- Missing EE2 must not cause an unconditional startup crash in the vanilla MCP dev client.

### IndustrialCraft 2 (IC2 1.97)

- The original IC2 Generator uses furnace fuel burn time through the legacy callback path. The EE2 fuel integration was tested in a real Tekkit instance.
- `IC2EmcIntegration` contains experimental mappings and is **disabled** by the entry-point flag.
- Future EU-consuming machines should isolate IC2-facing code from generic machine logic; test the exact IC2 API/classpath and energy rules before selecting an adapter.

### BuildCraft

BuildCraft is a future integration target for oil processing, fluid logistics and related machines. This repository does **not** currently contain a BuildCraft adapter or a working refinery. Do not pretend it does.

## Economic integrity

An EE2→EU converter or alternative thermal power source can create exploitative EMC/EU cycles. Before adding recipes or conversions, document input/output units, throughput, losses and whether any closed-loop path generates resources. These are design constraints, not currently enforced by a balance engine.

## Dependencies

Avoid introducing direct references to optional mod classes into the core bootstrap. Reflection is currently used where appropriate. If compile-time dependencies are required for new APIs, document how to obtain/remap them for MCP and validate startup both with and without the optional mod, as applicable.

## Experimental fractional distillation (feature branch)

The initial distillation heater proposes a direct IC2 1.97 `IEnergySink` implementation and uses original, compile-only ABI placeholders in the ignored local MCP tree. The release JAR must contain only `zed/` project classes/resources. Verify actual EnergyNet linking and EU remainder semantics in real Tekkit; do not confuse the test-double EnergyNet with a functional client integration. BuildCraft 2.2.14 pipes/liquid transport are deferred; only internal mB storage and bucket-based exchange exist in V1.
