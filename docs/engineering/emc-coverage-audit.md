# EMC coverage audit

The target is a positive base material value for every obtainable product in the supported pack, with bedrock explicitly excluded. World representations, such as a burning furnace, the lower half of a placed door or a flowing petroleum block, must resolve to their canonical product or liquid before pricing. Items with charge, stored contents, knowledge, fuel progress or configuration require explicit state accounting. A plain numeric EMC value by item ID and metadata can only express the material portion of that policy.

## Inventory and evidence

Run `powershell -ExecutionPolicy Bypass -File zto-tools/audit-zero-emc.ps1` from the MDK root. The script reads the Tekkit Dev baseline TSV and writes an itemized Markdown report under `dist-zto`. Every zero row is represented as an ID and metadata or as an inclusive state range. Preserve the original catalog and its hash for comparison.

The installed baseline hash is `5AE1FA07391BA96389B66C7345183A83891EFCCD17985BF60EB12AF1F1FA7E21`. Its 114,676 zero rows comprise 762 base or creative candidates across 563 IDs and 113,914 durability/state rows across 255 IDs. The primary candidates include 235 IC2 entries, 97 from the RedPower Core archive, 65 from the merged Minecraft JAR, 58 from EE2, 36 from Railcraft and 18 from ZTO. Archive origin describes the implementing class. Counts include technical or unnamed IDs and candidates whose obtainability is still unverified.

The baseline predates the Alchemical Converter. Tekkit Dev logged its registration at ID 3024. The ZTO source supplies its recipe and has no explicit EMC registration for it. Treat it as an additional candidate pending a fresh runtime value lookup. The installed candidate profile only changes Water Bucket from one positive value to another, so that edit does not change the zero set in the captured catalog. No audit or profile changes were applied to the live game.

The catalog combines registered items, creative entries, direct EE2 keys and enumerated damage ranges. It can miss variants supplied solely by NEI plugins, recipe registries, configuration or NBT. A single NetherOres catalog row, for example, does not establish that its shared ID has only one obtainable metadata. Reconcile those registries before declaring coverage complete. This audit also found Ruby Ore at ID 140 metadata 0 with zero EMC and positive siblings on the same ID. Use resolved ID and metadata in any change.

## Valuation batches

| Batch | Examples already in the zero audit | What must be established before assigning a value |
| --- | --- | --- |
| Ordinary products | Glass Pane, Snowball, Cauldron, Brewing Stand | Resolve the obtainable form and its cheapest repeatable recipe, count outputs and containers, and check that crafting and exchange cannot amplify EMC. Start here. |
| Ores, dusts and refined materials | Iron, Gold, Diamond, Copper, Tin and Uranium ore, Ruby Ore variants | Compare ore drops, Fortune, Silk Touch, direct smelting, maceration and NetherOres preprocessing. For example, one iron ore yields one 256-EMC ingot by smelting or two by macerating and smelting. Pricing the ore at 256 would give a repeatable 256 EMC surplus when the extra processing is powered renewably. A floor of 512 for this specific two-ingot route makes direct smelting a deliberate value loss. Additional routes still need checking. |
| Machines and infrastructure | BatBox, Rotary Macerator, RedPower Battery Box, BuildCraft pipes, six ZTO distillation parts, Alchemical Converter candidate | Value the empty chassis and compare alternate recipes, upgrades, wrench recovery, block drops and packaging into vehicles. Stored EU, inventory and progress must not be copied from a condenser template. |
| Fluids and containers | Generic Fuel Bucket and four ZTO fraction buckets | Separate the 768 EMC reusable bucket from the content at 1,000 mB. Compare refinery, distiller, pickup, placement, tank extraction and combustion. Audit world liquid IDs as representations of the same content. Account for returned containers and keep EU and BuildCraft energy separate from EMC. |
| Alchemical equipment | Watch of Flowing Time, portable Transmutation Tablet, EE2 tools and armor | Check repair, charge, target cloning, collector behavior and access gates in the actual consumers. Price an empty or canonical item before deciding how EE2 may reproduce it. |
| Stateful industrial products | Uranium Cell, Near-depleted Uranium Cell, Re-Enriched Uranium Cell, batteries, IC2 tools | Separate chassis or fuel material from charge, life and reactor byproducts. Compare depleting, re-enriching and recovering containers. Check the condenser's actual stack and metadata handling. |
| Special acquisition and identity | Monster Spawner, spawn eggs, potions, locked chest and unnamed internal IDs | Establish what is obtainable under the pack's rules and which identity is valid for exchange. Spawn eggs and potions need per-variant enumeration. Bedrock stays excluded. |

For each ordinary item, track its exact runtime identity, acquisition evidence, recipe input and output counts, alternate routes, proposed EMC, and the verification result for tablet and condenser. For each stateful item, also record whether exchange produces an empty or canonical instance, and test a charged or filled template before allowing reproduction. EE2 has separate consumer rules, so positive lookup and actual exchange eligibility need distinct checks. All pricing remains pending in this audit.

## Tekkit Dev example

The [opt-in Tekkit Dev example](emc-tekkit-dev-example.txt) contains 81 recipe-derived mappings for IC2, BuildCraft, RedPower and EE2, plus 15 follow-up IC2 and ZTO mappings. The first pass inspected 698 declarations and resolved 536. Source line references in the profile refer to the local Fernflower output. The profile preserves the water-bucket parity rule and prices all four ZTO fraction buckets at 768 EMC for the reusable bucket plus 2,048 EMC for the liquid. Verify the configured IDs before reusing the profile in another instance. The follow-up was verified on disk and still requires an in-game tooltip and exchange check.
