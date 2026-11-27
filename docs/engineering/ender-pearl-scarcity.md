# Ender Pearl scarcity

Survival 0.5.1-alpha.4 makes the first legitimate defeat of an authorized Custodian yield one vanilla Ender Pearl per eligible Observatory. The provisional rule operates independently per site. The world-wide budget, selection of rewarding regions or sites and recovery of lost pearls remain open design decisions.

## Defeat and duplicates

The native death path reaches the Custodian's ordinary drop callback. Only the canonical actor with the reserved encounter UUID can atomically commit the terminal site record. ZTO then submits one Ender Pearl to the native drop list, independent of Looting. A death callback replay or another actor with that UUID sees the terminal record. Peaceful, unload, administrative removal and manual fixture creatures cannot claim a reward.

A durable terminal record means the opportunity has been consumed. The actual item is subject to normal collection, storage, destruction, despawn and native save timing. A crash after the record commits and before the item persists can lose the item. A corrupt record disables the encounter. A crash between admission and native entity persistence can leave a reservation with no actor. Neither case grants an automatic replacement.

## Sources and EE2

In the pinned Tekkit overlay, the native Enderman (`jg`) rolls its normal pearl loot with Looting, then hands its captured drops to Forge 3.3.8.164. ZTO removes only vanilla pearl item stacks from the exact native Enderman's drop list. Ordinary ZTO terrestrial and aquatic Enderkin have no pearl loot path. Their attacks, routine drops and spawning are independent of the restriction.

ZTO removes the whole runtime item identities for Ender Pearl and Eye of Ender from EE2's EMC map before the first world. It reapplies the exclusion after EE2 external integration and when publishing a configured APPLY profile. An explicit SET for either identity rejects that optional profile with a diagnostic. Compatibility defaults, AUDIT and invalid profiles all retain the mandatory exclusion. Positive EMC for unrelated identities and stored EMC are preserved.

| Path | Pinned implementation evidence |
| --- | --- |
| Energy Condenser | Target price is read through `EEMaps.getEMC`. A zero price prevents the production loop even with a pre-existing target and stored energy. The physical target stack remains. |
| Transmutation Tablet and portable tablet | Both use `TransTabletData`. Target and learned output generation require positive EMC. Ordinary learning requires an exchangeable stack. Previously learned entries and Tome knowledge cannot create positive EMC. |
| Collectors and relays | The three Collectors convert through EE2's fuel upgrade registry. The protected identities are absent from that registry. Relays supply EMC to production machines. |
| Other EE2 production | The shipped default EMC entries priced pearls at 1,024 EMC and derived Eye of Ender price from its ingredients. The inspected special recipes and Philosopher's Stone conversions contain no direct renewable recipe for either item. |
| Native crafting and exploration | The Eye of Ender recipe still consumes a real pearl and Blaze Powder. A thrown eye can return as an item or break normally. Native Stronghold chest corridors can contain one pearl per loot roll. Existing generated loot, existing physical items, Stronghold behavior, and portal activation are preserved. |

Native validation exercises both protected item lookups, a funded Condenser with an existing target, Tablet output selection and native Enderman deaths alongside the legitimate reward. GUI clicking, complete pack-wide third-party loot enumeration, and automated Forge or EE2 compatibility on other pack versions require separate acceptance.
