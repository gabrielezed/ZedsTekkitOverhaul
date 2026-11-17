# Inventory drag and NEI controls

## Enable inventory drag in Tekkit Dev

Use the [managed Prism deployment](../deploy-prism.md) with Java 8. Prism loads the unified ZTOAgent at startup. The log line `[ZTO] Inventory drag connected to the legacy mouse dispatcher` confirms registration.

The mod JAR contains the NEI presentation and drag session. The inventory hook matches the pinned SHA-256 of Minecraft 1.2.5 `vp.class` and inserts one callback in `vp.f()V` before the ordinary LWJGL dispatcher. NEI's installed `gb.class` keeps its override, click/release methods and scroll dispatch. An unsupported dispatcher keeps its original behavior.

## Behavior and boundaries

Pick up a stack normally, then press and drag from a compatible slot. The preview shows the amount to be added to each selected slot. Left drag gives each visited eligible slot the same integer share, limited by its free capacity, with leftovers on the cursor. Right drag places one item per visited eligible slot. Revisiting a slot does not add a second entry. A press and release with a single eligible slot replays the ordinary NEI click.

In local Survival, drag selects ordinary slots with an empty or strictly matching ID, damage and NBT. Ordinary player inventory slots are available when the GUI and container follow the standard click path or an audited equivalent. Non-player slots use the following layout rules.

| Container | Eligible non-player slots |
| --- | --- |
| Vanilla and ZTO | Crafting inputs in 2x2, 3x3 and 5x5, vanilla chests, dispensers, furnace input and fuel, and Hopper storage |
| IronChest and EnderStorage | Chest storage, with the slot count checked against the backing inventory |
| EE2 | Alchemical Chest storage, Energy Condenser storage slots 1–91, and Portable Crafting Table inputs |
| IC2 and BuildCraft | Standard electric machine material input and Auto Workbench inputs |
| RedPower PR5B2 | Project Table grid slots 0–8 and storage slots 10–27, Buffer Chest storage, Deployer and Ejector inventory, and Alloy Furnace, Blulectric Furnace and Blulectric Alloy Furnace material inputs |

Armor, crafting outputs, NEI recipe screens, the Condenser target, RedPower sorting and filtering templates, and unaudited machine slots retain their existing actions. The Project Table refill result uses its original click path. EE2 Transmutation uses custom click handling and is outside this policy. Shift/Ctrl/Alt, the drop shortcut, NEI widget presses, infinite held stacks and delete mode are outside the gesture. Inventory Tweaks bulk-action rules still apply to ZTO's automated moves. Drag is an explicit manual slot selection.

The session checks the live screen, window, player, cursor, slot ownership, capacities and the original target stack snapshots. The commit uses ordinary window clicks on the game thread and stops if any result differs from the planned change. This sequence is local only. A verified server-side drag transaction would be required for multiplayer activation.

## NEI styling

The compact actions use the same atlas and complete four-corner border as legacy NEI. The 2x2 controls sit in the blank area below the grid, separate from the title and Inventory Tweaks button. The prepare action has a gap beside NEI's question button. The 5x5 preview uses the installed crafting texture's recessed slots, arrow and result frame, including all nine empty interior cells. Tooltip wrapping is constrained to the active screen width.

## Verification

- Run `zto-tools/test-crafting-ux.bat` for the distribution and slot policy.
- Run `zto-tools/test-zto-agent.ps1 -Pack` for headless identification of the real transformed dispatcher, production callback and NEI override.
- In an input-isolated guest, pick up ten matching items, drag left over three empty slots and check for a preview of three per slot with one on the cursor. Right-drag three slots and check for one per slot with seven on the cursor. Revisit a slot, close mid-gesture and repeat with Shift/Ctrl/Alt and unsupported outputs. Verify a single normal click, Shift armor quick equip, Inventory Tweaks bulk actions and NEI buttons and recipe overlays. Exercise the player inventory, a chest, 2x2, 3x3 and 5x5 crafting, and an audited modded inventory. Reconcile all stacks and cursor contents.

See the [feature acceptance matrix](../agents/compatibility-acceptance.md) for evidence levels and outstanding results. Windows host automation remains disabled until input isolation is verified.
