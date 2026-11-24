# Motorboat

The Motorboat gives gasoline from Fractional Distillation a practical use in ocean exploration. A vanilla boat remains an option for short trips. The outboard uses the existing ZTO fuel bucket and can support trips to future exploration sites with the same controls and tank.

## Craft and operate

Craft an **Outboard Motor** in a crafting table. Use IC2 Refined Iron for `R`, BuildCraft Iron Gear for `G` and the BuildCraft Combustion Engine for `E`.

```text
RGR
RER
 G
```

Place a vanilla Boat above the Outboard Motor in the crafting grid to make an empty **Motorboat**.

1. Right-click water with the Motorboat item. Point in the direction you want the bow to face.
2. Sneak-right-click the hull and put a **ZTO Gasoline Bucket** in the Gasoline slot. Take the empty bucket from the return slot. The gauge shows up to **4,000 mB**, and each bucket adds **1,000 mB** when that much space is free. Both slots stay with the hull when the screen closes.
3. Right-click to board. Use your usual forward and back keys for throttle and reverse, and your strafe keys to steer while applying throttle. Right-click the hull again to dismount.
4. To recover the boat, get off and strike the hull until it drops. Pick up the Motorboat and any buckets left in its slots. The recovered item carries the remaining fuel and retains it on placement.

The engine burns **1 mB every two ticks of powered travel**. Releasing the throttle or running out of gasoline lets the boat coast. The hull stays afloat. The initial forward speed cap is **1 block per tick**. A vanilla boat moving along one axis caps at **0.4 blocks per tick**.

## Implementation and artwork

The Tekkit Classic 3.1.2 singleplayer implementation registers the IC2 1.97 `refinedIronIngot` item stack, BuildCraft 2.2.14 `ironGearItem` and `engineBlock` metadata **2**. The Motorboat inherits the vanilla boat's hull bounds, seating and mounting. It owns its speed, steering, buoyancy and fuel conversion. A separate local client adapter reads vanilla movement input and opens the native container. The hull's registered name, tank and queued buckets persist in entity NBT. The item and entity assignments are listed in the [configuration guide](../agents/configuration.md#registry-and-integration).

The renderer attaches the outboard on the model's positive X end, within the same transform as the stock hull and damage roll. The hood, clamp, shaft, gear case and propeller use the original pixel layouts in [the artwork suite](../../artwork/source/motorboat/). Minecraft supplies `/item/boat.png` and cell (8,8) of `/gui/items.png`. The client samples metal colors from BuildCraft's Combustion Engine and IC2's Compressor, then composites the inventory motor over that vanilla item cell. [Artwork coordinates and regeneration](../../artwork/README.md#motorboat).

## Verification

Run `zto-tools/test-zto-agent.ps1 -Focus motorboat -PackJava java` after the standard MCP build. The disposable headless pack fixture covers real registered recipes, placement, mounting, the fueling screen and its container, wrong fuels, bucket recovery, measured speed, steering, fuel exhaustion and disk save/reopen. It also checks the original hull model, stern geometry and composited texture pixels.

For the graphical check in an input-isolated Tekkit client, view the boat from behind and each side, then turn and strike the hull. Check that the bracket moves with the transom and the propeller sits below the waterline. Confirm the two sprites in the inventory and hotbar, the fueling panel at each GUI scale, and the wake and sound under throttle and during the transition to idle.
