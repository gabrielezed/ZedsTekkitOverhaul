# Crafting and inventory UX implementation checklist

Baseline at `main` / `f8ee800`. See [execution record](#execution-record) for implementation, build evidence and outstanding acceptance checks.

## Workspace and evidence

Start at [AGENTS.md](../../AGENTS.md), then read [Environment](environment.md), [Architecture](architecture.md), [Compatibility](compatibility.md), [Testing](testing.md) and the [Tekkit Classic 3.1.2 reference](../reference/tekkit-classic-3.1.2/README.md). Target Minecraft 1.2.5, MCP 6.2, Forge 3.4.9.171 and FML 2.2.106.176 in the FoxyRetroMDK 1.3.6 / JDK 8 development environment. Verify the effective pack runtime separately. The [creative inventory audit](../engineering/creative-inventory-backport.md) previously observed Forge 3.3.8.164 in Tekkit Dev.

| Area | Observed in this workspace | Consequence |
| --- | --- | --- |
| NEI 1.2.2.4 | Under local `tekkit-mods-decompiles/mods/coremods/`, `codechicken/nei/GuiRecipe` builds two contextual `?` overlay buttons. Its action returns to the original GUI and installs a renderer. `DefaultOverlayRenderer` draws positioned ingredient stacks. `GuiCraftingRecipe` registers 2×2, 3×3, smelting, fuel and brewing overlays. | Preserve the ghost overlay. This path provides no ingredient movement or Shift-transfer. Reproduce both inputs in the exact pack before choosing a UI hook. |
| NEI extension surface | `API` exposes recipe/usage handler registration, positioned overlays, tooltip handlers, settings and keybinds. `NEIConfig` discovers `IConfigureNEI` plugins. The inspected `API.registerGuiOverlay` overloads register renderers via positioners. | Prototype an additive handler/control with the actual GUI lifecycle. Verify a supported action extension or a separate contextual entry point. Keep any unavoidable patch isolated and reversible. |
| CodeChickenCore 0.5.3 | `mod_CodeChickenCore` declares the version, loads Forge bridges and forwards custom packets to `codechicken.core.PacketCustom`. The same tree also contains `codechicken.nei` and patched `gb.java`. | Treat CCC as pack infrastructure and a reference for loading and packet dispatch. NEI owns the recipe UI. Verify class ownership in the actual jar order before adding any dependency. Evaluate CCC packets only if a server action needs transport and both sides have a matching ABI. |
| Inventory Tweaks 1.41b | Local `mods/InvTweaks-1.41b-1.2.4.zip/decompiled/` contains `InvTweaksContainerManager`, sorting, shortcuts and auto-refill handlers. Ruleset source exposes distinct `locked` and `frozen` rules. | Reuse features after checking coverage and semantics. Preserve the pack bindings and inventory section rules. Verify how existing locks apply to new actions. |
| Vanilla | MCP `GuiContainer`, `Container`, `SlotCrafting` and `PlayerControllerMP.windowClick` are starting points. The latter predicts a click and sends `Packet102WindowClick`. Legacy Shift-click can retry while an output still matches. | Keep normal slot callbacks and result consumption. A sequence of clicks is individually synchronized and can partially succeed. Establish a separate atomic operation where required. |
| ZTO 5×5 | `crafting/AdvancedCraftingManager` owns fixed 25-cell patterns matched by item ID and damage, including required empty cells. `ContainerAdvancedCraftingTable` has output 0, inputs 1–25, player main inventory 26–52 and hotbar 53–61. It uses `SlotCrafting`; Shift-click returns null. `BlockAdvancedCraftingTable` opens a local GUI via ModLoader. Closing drops remaining inputs. | Expose a read-only recipe view from the existing registry. Preserve matching and normal close behavior. The current 5×5 matcher has no shapeless or Ore Dictionary support. Server-backed opening and Shift-click are separate prerequisites for 5×5 multiplayer support. |
| Other ZTO code | `integration/inventory/DragDistributionIntegration` uses ordinary clicks and has no caller under ZTO. `tests/` contains drag/planner harnesses. `creative/CreativeInventoryHook` is a separate SSP feature. | Use the drag code as a reference for click discipline. Keep it separate from recipe transactions. Preserve the creative catalog and NEI overlay interaction. |
| Machines | IC2 1.97 `ic2/neiIntegration11x/SubModule` registers advanced recipe handlers and machine recipe/usage handlers and overlays, including Macerator, Extractor and Compressor. ZTO distillation has fluid/energy processing and local panels. `hopper/InventoryAccess` has explicit automation policy. | Start with the existing Macerator handler and verify its actual input slot. ZTO fluid recipes can be read-only. Manual GUI access and sided automation require separate checks. BuildCraft, RedPower and EE2 coverage remains to be measured per GUI. |

Paths under `crafting/`, `creative/`, `integration/`, `distillation/` and `hopper/` above are relative to `src/minecraft/zed/tekkitoverhaul/`. Keep decompilations and artifacts local. Inspect effective classes with JDK 8 `javap -p -c` in Prism jar order and map obfuscated descriptors with `conf/client.srg` and `conf/methods.csv`. The local coremods dump mixes sources from multiple components and does not prove runtime ownership. See the worked example in the creative audit.

## Feature matrix

Statuses describe the source-level disposition. **Audit** means exact pack coverage is open. **Reuse** means a baseline exists and needs acceptance tests. **Extend** means the baseline needs a focused addition. **Build** means plan an original feature. **Defer** means evaluate after earlier releases.

| IDs | Priority | Disposition and expected result | Work |
| --- | --- | --- | --- |
| CRAFT-01, CRAFT-02 | P0 | Extend NEI with a contextual `+` for one full recipe and Shift for the maximum complete sets that fit. Show available, partial, missing and valid-alternative states with counts and a non-color cue. Preserve the `?` overlay and explain disabled actions. | 1–2 |
| CRAFT-03, CRAFT-05 | P1 | Build opt-in partial placement and explicit reload of the last successful recipe in a compatible grid. Replan on every request. | 5 |
| CRAFT-04, CRAFT-06, CRAFT-08 | P0 | Reuse vanilla output semantics. Extend 5×5 with Shift-click and recipe visibility. Build Return Ingredients and balanced batch loading. Leave inputs that do not fit on return. | 2–3 |
| CRAFT-07, BATCH-05 | P1 | Extend allocation with displayed-variant preference, compatible alternatives already owned and manual pinning. Prefer a deterministic order and evidence-based substitutions. | 5 |
| CRAFT-09 | P2 | Defer optional adjacent storage. Require loaded, reachable, explicitly supported inventories and server permission checks. Show source distinctions. | 6 |
| NEI-01 | P1 | Audit the existing search parser, subsets and localized names. Extend only uncovered name/mod/ID, multiple terms, exclusion and feasible tooltip search. Preserve existing syntax. | 5 |
| NEI-02, NEI-03 | P1 | Audit existing back navigation and favorites. Extend to bounded Back/Forward preserving query mode, category, variant and page. Add compact persistent item/recipe bookmarks if absent. | 5 |
| NEI-04, NEI-05 | P0 | Reuse recipe/usage navigation and machine handlers. Build read-only handlers for ZTO processes where applicable, with amounts, secondary results, fluid volumes, energy and duration when known. | 1, 4 |
| NEI-06, NEI-07, NEI-08 | P1 | Extend existing machine category views with provenance, alternative processes and short factual tooltips where gaps remain. | 5 |
| NEI-09 | P2 | Defer narrative discoveries. Keep ordinary and industrial recipes visible. | 6 |
| NEI-10, GUI-01, GUI-04, GUI-05, GUI-06 | P0 | Extend legacy layout with contextual controls, consistent tooltips, configurable shortcuts and placement based on live GUI bounds and occupancy. | 1–4 |
| INV-01 | P0 | Reuse sorting and validate one handler per gesture. Verify player/chest support and isolation from specialized slots. | 1 |
| INV-02, INV-03, INV-04, INV-05 | P1 | Audit existing quick moves. Extend supported storage with explicit Deposit Matching, Deposit All and Take All where missing. Default to hotbar protection and preserve overflow in the source. | 5 |
| INV-06, INV-07, INV-08, INV-12 | P1 | Reuse locks, refill and consolidation where present. Extend only verified gaps with protected-slot and valuable-stack policies. Avoid a second sorter/refill listener. | 5 |
| INV-09, INV-10, INV-11 | P2 | Audit NEI inventory search highlighting first. Extend container search, equivalent-item highlighting and totals only for uncovered cases. Define equivalence per action. | 6 |
| MACH-01 | P0 | Extend one proven machine handler with manual input transfer. Verify a safe destination before broadening coverage. | 4 |
| MACH-02, MACH-03, MACH-10 | P1 | Extend capacity-limited input, normal output Shift-click and contextual failure feedback. Supply basic failure feedback with P0 input. | 4–5 |
| MACH-04, MACH-05, MACH-06 | P1 | Extend machine tooltips with distinct energy units, actionable inactivity and slot roles. Use authoritative values where available. | 5 |
| MACH-07 | P1, P0 baseline | The feature entry is P1 and the release outline calls for machine navigation at P0. Verify existing handler navigation in P0; add missing machine-local shortcuts in P1. | 1, 4–5 |
| MACH-08 | P1 | Audit Hopper and external automation regression tests. Respect each container's manual and sided rules. | 4–5 |
| MACH-09 | P2 | Defer presets until a ZTO machine has multiple useful configurations. | 6 |
| BATCH-01 | P1 | Build an optional craftable-count preview using the shared allocator. Distinguish total complete sets available from sets loadable in one fill. | 5 |
| BATCH-02, BATCH-04, BATCH-06 | P2 | Defer direct material quantities, personal material lists and explicit repeated intermediate crafting. Use output yield and require player initiation. | 6 |
| BATCH-03 | P3 | Defer a read-only dependency planner with cycle detection, alternatives, yield, non-consumed tools and optional stock accounting. | 6 |
| GUI-02, GUI-03, GUI-07 | P1 | Audit existing provenance and technical tooltips. Extend with optional advanced info and a small settings panel. Preserve normal tooltip density. | 5 |

## Transfer and integration design

- Keep NEI recipe display and input gestures client-side. Resolve the original destination GUI and its current container separately from the recipe screen. A recipe view uses virtual slots. Use explicit adapters for supported player, workbench, ZTO 5×5 and machine layouts. Each adapter owns recipe resolution, slot roles, limits and side-effect rules. Unsupported targets give a contextual reason and retain their normal controls.
- Use logical ingredient positions and the actual matcher. Preserve 5×5 empty cells, shaped orientation/offset rules and shapeless placement for recipes that support it. A displayed rotating stack is a hint. Derive permitted alternatives from the matching recipe. Use the matcher again on the proposed final grid before committing a complete fill. Keep strict stack merging by item ID, damage and NBT separate from recipe equivalence. Preserve durability, charged items, container returns and reusable tools.
- Plan on copied snapshots of allowed player inputs and destination slots. Account for usable items already in the grid. Reserve room to return incompatible stacks before overwriting any cell. Assign shared alternatives across all requirements so a flexible ingredient cannot exhaust the only valid stock for another slot. Bound the search by stack/slot limits and cache previews by recipe, variant and relevant inventory change. For Shift-transfer use the greatest feasible number of complete sets. Default to no movement on shortage. Partial mode is an explicit later policy with the same atomic commit requirements.
- Keep planning and application separate. For an atomic bulk action in SMP, use a matching server implementation that resolves a bounded recipe/action identifier against its own registry and the player's currently open window. Check access, window/session identity, recipe, cursor, slot roles, item identity, quantities and limits against current state. Revalidate the affected snapshot immediately before commit on the server game thread. Apply all approved deltas through the verified slot/container lifecycle and synchronize affected slots and cursor. Reject stale or duplicate requests and unsafe adapters before mutation. Never trust a client-supplied stack, destination index or computed quantity as authority. In SSP use the same validation in the local authoritative context.
- Ordinary synchronized clicks are acceptable for existing behavior and explicitly sequential operations. Validate the next move and stop on rejection or GUI changes. Do not present that path as atomic. Where a server capability is absent, keep ghost preview and existing clicks available and disable new bulk actions. Do not fake a multiplayer 5×5 by opening a client container.
- Recipe transfer prepares inputs only. Normal output pickup owns consumption, container items, crafting hooks and achievements. When implementing repeated output Shift-click, verify the effective legacy retry path and require progress per iteration. Check room for the complete output before consuming a recipe. Use a conservative policy for handlers with irreversible or undocumented side effects.
- Put original code under `src/minecraft/zed/` and keep client integration classes separate from server-loadable code. Keep the bootstrap small. Verify NEI plugin discovery and reobfuscated linking without committing upstream JARs or sources. Never package a replacement `gb.class`. Test absence/disable paths for optional adapters and preserve the existing EE2/EMC, IC2 and creative behavior.

## Milestones and acceptance

### 1. Establish the pack baseline

- [x] Record branch, worktree state, artifact versions, effective client class ownership and exact server distribution if available. Capture descriptors for NEI plugin discovery, GUI lifecycle and packet/container paths. Record exact tools and mapping sources.
- [ ] In Tekkit Dev measure the `?` overlay, regular and Shift-click behavior, recipes/usages and back navigation, shaped/shapeless/Ore variants, search modes, machine handlers and keybinds. Record actual button effects, supported GUIs and which artifact owns them. Verify existing Inventory Tweaks sorting, locks, refill, consolidation and shortcut coverage.
- [x] Inventory the player 2×2, workbench 3×3, 5×5, chest, IC2 Macerator and representative BuildCraft, RedPower and EE2 interfaces. Record recipe category, editable input/output roles, rules, existing quick moves and possible automation conflicts. Record unknowns explicitly.
- [x] Prove a small client integration with the effective NEI ABI and dynamic layout. If the existing API cannot host an action, evaluate a scoped contextual control before a transformer. Record the minimal hook, rollback and absent-NEI behavior.

### 2. Ordinary crafting vertical slice

- [ ] Implement/test planning for one and maximum complete sets, missing-count preview and Return Ingredients. Begin with the 3×3 workbench, then 2×2. Include occupied-grid and full-inventory cases.
- [ ] Wire one click to prepare one recipe and Shift-click to balanced maximum loading. Keep the cursor stable and let the player choose when to take the result. Return only what fits and show the remainder. Preserve vanilla recipes, actions and callbacks.
- [ ] Prove the authoritative path in SSP and a named matched server for any advertised SMP support. Disable unavailable server actions cleanly. This is the gate for reuse by 5×5 and machine adapters.

### 3. Native 5×5

- [ ] Expose a defensive recipe view and a NEI recipe/usage handler using the existing registry. Fit all 25 cells, output and contextual controls at low resolution. Test the Foundry perimeter recipe and verify all nine interior cells remain empty.
- [ ] Add destination mapping, shortages, batch filling and return through the shared path. Add progress-safe output Shift-click through the container and verify normal result callbacks and close behavior. Treat new matcher semantics as a separate feature.
- [ ] For SMP, establish real server GUI/container registration and validate open/close and result synchronization before enabling transfers. Record SSP-only status until a matched server test passes.

### 4. Industrial P0

- [ ] Reuse the IC2 Macerator recipe handler and validate one explicit item input adapter. Exercise valid input, invalid input, occupied/full slot and NEI disabled. Confirm battery, upgrades and output are untouched. Confirm processing speed, energy use and automation stay consistent.
- [ ] Expose real ZTO process data in NEI where applicable. Keep fluid and virtual destinations read-only until a valid transfer path exists. Confirm ingredient/usage navigation and contextual failure feedback in the original GUI.

### 5. P1 release

- [ ] Deliver storage actions, protection and refill gaps in independent changes. First accept Deposit Matching on a plain chest, using a snapshot of identities already stored and preserving hotbar/locked slots. Then add explicit Deposit All and Take All. Test full inventory, locked/frozen rules, specialized slots and one gesture per operation.
- [ ] Deliver recipe history/bookmarks, search gaps and tooltips as client changes independent of inventory mutations. In separate changes add partial transfer, recipe reload, pinned variants and batch counts through the same planner. Keep partial transfer off by default.
- [ ] Extend machine information, capacity moves and normal output handling only for audited adapters. Verify GUI diagnostics, quantities and units against machine state.

### 6. Optional P2/P3

- [ ] Evaluate each item separately against the matrix and existing pack behavior. Keep adjacent storage opt-in and test permission, chunk and concurrent-source changes. Preserve the role of BuildCraft and RedPower logistics. Keep recursive planning advisory.

## Verification matrix

| Gate | Required checks |
| --- | --- |
| Planner harness | Shaped and shapeless placement, 5×5 holes, repeated/shared ingredients, overlapping alternative pools, metadata, durability, NBT, non-stackables, nonstandard limits, incompatible grid contents, locked sources, no space for displacement, complete-only refusal, bounded maximum load and conservation of stacks. Add regressions for cases the existing drag/planner tests do not cover. |
| Container and transaction checks | Rejected or stale action leaves all affected state unchanged. Repeat packets, change the cursor/window, close during a request and race two users against shared storage. Verify normal slot/result hooks, output yield, remainders, reusable tools and full-output destination handling. Run on the actual matched server for SMP claims. |
| Tekkit Dev UX | Survival 2×2/3×3 and 5×5, full and missing materials, repeated crafting of 32 components, Return Ingredients with insufficient space, Macerator, NEI recipe/usage and return navigation, inventory sorting/refill, and chest deposit with hotbar protection once P1 ships. Use disposable worlds. Test multiple GUI scales and small windows with NEI open, hidden and absent where applicable. Check the creative backport and existing machine/Hopper behavior. |
| Build and packaging | Review diffs, run `recompile.bat` and `reobfuscate.bat`, or the [build/deploy workflow](../deploy-prism.md) with `zto-tools/build-and-deploy.bat` targeting isolated Prism **Tekkit Dev**. Check the packaged class list, resources, startup logs and reobfuscated linking. Report planned, implemented, compiled and runtime-tested separately. |

## Operational prompts

Copy the common brief and one assignment into the next agent's task.

### Common brief

> You are working in the Windows FoxyRetroMDK 1.3.6 / MDK-1.2.5 workspace for ZedsTekkitOverhaul. Read AGENTS.md and this checklist. Confirm branch and worktree, then use the exact Tekkit Classic 3.1.2 artifacts and MCP 6.2 names. Write repository content in English and report to the owner in Italian. Keep changes scoped to your assignment and put original mod code under src/minecraft/zed/. Preserve save compatibility and NEI, Inventory Tweaks, EE2 and IC2 behavior. Use local decompilations for inspection and verify runtime ABI with javap. Report files changed, checks run, runtime evidence, open blockers and the next unchecked items. Use the isolated Tekkit Dev instance for client integration and identify the matched server separately.

### Agent A — pack audit and hook proof

> Complete milestone 1 before changing gameplay. Start at GuiRecipe, DefaultOverlayRenderer, API, NEIConfig, mod_CodeChickenCore and the Inventory Tweaks handlers named in this checklist. Trace the actual overlay and key/shift handling in Tekkit Dev and map the effective classes. Audit vanilla, ZTO 5×5 and the representative mod GUIs. Deliver a checked baseline with existing, correctable, extendable, missing and avoid-duplication cases. Prove one additive client integration with a read-only action. Leave uncertain hooks and unsupported servers explicitly open.

### Agent B — transfer core and vanilla slice

> Use Agent A's verified hook and slot maps. Implement milestone 2 starting with a deterministic snapshot planner and focused regression cases. Wire CRAFT-01/02/04/06 into the 3×3 workbench and then 2×2. Preserve the legacy ghost overlay. Prove commit validation and ordinary output consumption through the effective container. If server capability is unavailable, ship a clearly gated SSP or preview slice and record the SMP dependency. Keep new machine and storage actions outside this change.

### Agent C — 5×5 and industrial slices

> After the workbench vertical slice, take milestone 3 as its own change and milestone 4 as a separate change. Expose the existing 5×5 recipes to NEI and reuse transfer planning with an explicit 25-cell map. Verify the Foundry recipe, output Shift-click progress and grid close behavior. Preserve the current matcher and gate SMP on a real server container. For milestone 4, reuse the existing IC2 handler, verify the Macerator destination and add only the supported manual input action. Present ZTO fluid processes through read-only recipe data where applicable.

### Agent D — P1 increments and optional evaluation

> Start after the relevant P0 acceptance gates. Take one milestone 5 group at a time. Compare each proposed feature with measured NEI and Inventory Tweaks coverage before adding controls. Reuse the planner for batch counts and alternative selection, and verify protection and capacity rules before enabling bulk storage actions. Treat milestone 6 as a separate design review and implement only selected features. Update the matrix and checklist with evidence for each completed group.

## Execution record

### 1. Installed pack and hook

- Started on `main` at `f8ee800`, tracking `origin/main`. The owner's existing edits to `AGENTS.md`, `docs/agents/README.md`, and this new checklist were present at entry.
- Prism **Tekkit Dev** has one modpack jar overlay. The configured jar-mod and `minecraft/bin/modpack.jar` both hash to SHA-256 `0827EE55325FEDBC37F2A6A890F19DB11F75FAB5E9A5C02EAFD1BD50D2B59446`. The overlay owns `gb`, NEI and CCC. FML's effective merged Minecraft archive loads the core entries. Startup confirms Minecraft 1.2.5, Forge 3.3.8.164 and FML 2.2.106.176. JDK 8 `javap -p -c/-v` confirmed NEI 1.2.2.4 and CCC 0.5.3 annotation constants. Inventory Tweaks 1.41b is loaded from its own installed ZIP. No matched dedicated server was identified or used. MCP client names come from `conf/client.srg`, `conf/fields.csv` and `conf/methods.csv`.
- `IConfigureNEI.loadConfig()` is discovered by the legacy `NEI*Config.class` plugin scan. The bridge instead uses the existing FML ModLoader GUI callback and public NEI methods after mod loading. `API.registerRecipeHandler(ICraftingHandler)`, `registerUsageHandler(IUsageHandler)`, and `registerGuiOverlay(Class,String,int,int)` are present. `GuiRecipe.firstGui/prevGui` point to the real destination and previous view. `getIngredientStacks(int)` returns positioned virtual stacks and `getResultStack(int)` returns the virtual result. `gb.clickSlot(yu,int,int,boolean)` routes ordinary slots through the player controller. The ordinary MP click path predicts locally and sends `Packet102WindowClick` using a container transaction ID. It cannot commit a multi-slot fill atomically. CCC's packet facilities do not establish a ZTO server endpoint. The new code has no direct CCC dependency.
- The additive buttons use the existing control list (`GuiScreen.controlList` / `vp.s`) and the live dimensions and question-button positions. A missing NEI class leaves basic return and storage controls available. A missing bridge signature disables integration, with a diagnostic in the log. Rollback is removal of the ZTO GUI callback or replacing the ZTO JAR with the previous build. The packaged JAR contains only ZTO classes and assets.
- Tekkit Dev Survival world **New World**, GUI at an 856×480 client area: observed localized NEI name search and recipe navigation from the item panel. Observed `?` creating a ghost with an empty real grid and no result. `+` with no stock stayed in the recipe GUI, with a missing-count tooltip. The existing installed `D LOCKED` rule protected the whole hotbar. Moving the stack manually into the main inventory made the same request succeed. Checked `getConfig()`, `getLockPriorities()`, and `getFrozenSlots()` against the installed Inventory Tweaks classes. Exact sorting gestures, refill, shortcuts, shaped/shapeless/Ore visual coverage and all resolution cases remain separate runtime checks.

| Container | Verified slot/category evidence | Boundary |
| --- | --- | --- |
| Vanilla 3×3 / 2×2 | Result 0, editable inputs 1–9 / 1–4. Player main/hotbar start at 10 / 9. Player armor is excluded. NEI shaped/shapeless positioners are existing behavior. | Vanilla output Shift-click still uses its own merge and retry behavior. No new full-yield guard is installed for vanilla output. |
| Native 5×5 | Result 0, fixed pattern 1–25, player inventory 26–61. Existing Stone perimeter and normal SlotCrafting callbacks. | Local only. Server opening, window ID and output synchronization are prerequisites for multiplayer. |
| Vanilla chest | Plain 27-slot chest and vanilla double chest wrapper, followed by 36 player slots. Existing Shift-click and Inventory Tweaks sorting are independent. | ZTO storage actions only inspect known slot layouts. Specialized storage is excluded. |
| IC2 Macerator | Installed `ContainerElectricMachine.getInput()` and constructor show slot 0 input, 1 discharge, 2 result, 3–6 upgrades. `Ic2Recipes.getMaceratorOutputFor(aan,boolean)` confirmed against IC2 1.97. The existing handler owns recipe and usage navigation. | Manual local input only. Native output handling and automation paths remain IC2's. |
| BuildCraft Auto Workbench | BuildCraft 2.2.14 local archive inspection shows output 0 as a specialized auto-crafting slot and inputs 1–9 bound to the tile. | No generic transfer adapter. Automated crafting and close semantics differ. |
| RedPower Alloy Furnace | PR5B2 local archive inspection shows 0–8 material, 9 fuel and specialized result 10. RedPower NEI plugin has a distinct alloy handler. | No transfer adapter. Recipe assignment and sided automation still need a live test. |
| EE2 Energy Condenser | 1.4.6.5 local archive inspection shows slot 0 target, 1–91 EMC conversion slots and 36 player slots. | No transfer adapter. Conversion target and consuming inventory need separate policies. |

### 2. Ordinary crafting

- **CRAFT-01/02/04/06, GUI-01/04/05/06, NEI-10.** Added `+` for one set, Shift for a balanced fill, and Return. The existing matcher checks the planned result. The plan works on slot copies and validates current container, cursor, freshness, destination validity, metadata, NBT and stack limits before writing. Requests expire when the screen/window or displayed recipe changes. Single-use plans reject repeated commits. Bulk actions are disabled outside authoritative local Survival. No packet sender or server-side component is enabled.
- **Observed in Tekkit Dev.** In both 2×2 and 3×3, loaded one birch log, showed a four-plank result, and returned a log to its original stack. Took output through the normal 2×2 slot and observed the consumed input. Also crafted a vanilla workbench and observed its normal achievement. Shortage preview and protected-hotbar refusal were observed. The first build's tooltip could be obscured by the NEI panel and the reload control could collide with Inventory Tweaks. The current build moves those controls and draws tooltip feedback after the NEI layer. The runtime matrix still includes minimum-size/other scale checks, Shift gesture, stale GUI request, and mod absence.
- **MCP harness.** `zto-tools/test-crafting-ux.bat` uses real MCP classes. It covers 32 balanced sets, reused grid stock, shared shortage, partial opt-in, full-inventory displacement including room freed by a larger fill, return overflow, 5×5 holes, defensive recipe copies, metadata, NBT, nonstandard slot limits, stale snapshots and duplicate commit rejection. The final stock matcher is exercised with a constrained wildcard choice.

### 3. Native 5×5

- **CRAFT-06/08, NEI-04.** Defensive registry view, a full-size NEI category per registered fixed pattern, recipe/usage navigation, ghost overlay and shared fill/return. Native result Shift-click preflights the whole output and uses SlotCrafting's decrement and pickup callbacks. Bounded retry stops when ingredient count stops decreasing. The table opens only in a local world. Existing block IDs, recipes, matcher and close return/drop semantics are preserved.
- The portable MCP harness checks the perimeter pattern and nine empty interior cells. The runtime gate still includes the Foundry output, Shift-click and closing with occupied inputs. Multiplayer requires a real matched GUI/container endpoint and dedicated server checks.

### 4. Industrial P0

- **MACH-01/02/03/07/10, NEI-04/05.** The Macerator adapter checks the installed handler, GUI, container and exact tile class, validates the live IC2 recipe, and moves only to slot 0. Batch input is rounded to complete machine recipe units within the input capacity. Existing output Shift-click remains IC2's. The ZTO Oil Fractionation NEI view uses registered fractions and real mB, ticks, temperature, EU/t and EU per batch. It is read-only because tanks require different interaction and validation rules.
- **MACH-04/05/06/08, NEI-06/07/08.** The Macerator info control identifies slot roles and shows local EU storage, rate, duration and actionable inactivity. Fractionation lists each available setpoint and process values. Existing generic NEI machine categories remain available. Other machine-local diagnostics and shortcuts need verified slot semantics and authoritative values. Runtime Macerator insertion, output, energy, automation and NEI-disabled behavior remain acceptance checks.

### 5. P1 increments and remaining gaps

| IDs | Disposition and evidence |
| --- | --- |
| CRAFT-03, CRAFT-05 | Implemented opt-in Alt-click partial placement and explicit reload in the same grid or a compatible vanilla grid. Partial is off by default and preserves incompatible occupied cells. Reload replans against current stock. Partial planner is tested in MCP. Full controls and reload need in-game acceptance. |
| CRAFT-07, BATCH-05 | Implemented displayed-variant preference, Ctrl pin for the current action and bounded allocation among owned vanilla metadata wildcard candidates. Tagged stock is never substituted. The shared-stock wildcard test passes. IC2/Forge Ore alternatives remain open. In this NEI ABI positioned stacks expose one displayed stack, and IC2's handler resolves Ore Dictionary entries to a representative. A reliable mapping from the selected NEI row to the authoritative Ore recipe is required before extending substitutions. |
| BATCH-01 | Tooltip distinguishes stock sets for the shown variants from loadable sets, using the same allocation as transfer. The stock estimate is for shown variants and eligible slots. It does not count alternate-only theoretical stock. The wildcard search has a bounded node budget. |
| NEI-01 | Reuse localized names, ID suffixes when shown, `@` subsets, wildcards and stock item tooltip lines. The installed search strips periods, expands `*` and rewrites `?` before Java regex evaluation. There is no public filter extension. Unordered term/exclusion grammar, reliable mod ownership and search through third-party injected tooltips remain open pending a compatible search entry point that preserves visibility, subset and permission rules. |
| NEI-02/03 | Added recipe Back/Forward (16 forward entries in the open recipe chain) and 12 persistent recipe/usage bookmarks. Navigation reuses live recipe screens to retain category and page. Bookmarks store item ID, metadata/NBT, mode, category and page. They do not pin the current animated ingredient frame. Runtime navigation and persistence acceptance are still required. |
| INV-01/06/07/08/12 | Inventory Tweaks retains sorting, locks/frozen rules, consolidation, refill and existing shortcuts. New actions respect the active lock/frozen arrays. No new sort/refill listener. Broader valuable-item classification and complete sorting/refill gesture tests remain open. |
| INV-02/03/04/05 | Added plain-chest Deposit Matching, Deposit All and Take All. Matching uses a pre-operation snapshot of strict stack identities. Hotbar and protected player slots are skipped, overflow stays in the source, specialized destinations are excluded. Runtime acceptance still includes full chest, protected-slot behavior, competition with sorting controls and single gesture handling. |
| GUI-02/03/07 | Contextual recipe, slot and process information plus configurable key code and partial-fill switch through existing ModLoader configuration. A dedicated graphical settings panel and broad mod item provenance are pending a supported UI/ownership surface. |
| MACH-07/08 | Reuse native NEI navigation and output handling on the existing Macerator view. New mutations have no sided Hopper, BuildCraft or RedPower entry point. Portable Hopper tests are separate from live automation acceptance. |

### 6. P2/P3 decisions

| IDs | Disposition and prerequisite |
| --- | --- |
| CRAFT-09 | Deferred. Adjacent inventories require a matched server transaction across explicitly allowed inventories, loaded-chunk and reachability checks, protection policy, concurrent-source tests and source cues. This preserves the role of pipe and tube logistics. |
| NEI-09 | Deferred. Narrative discoveries need an approved discovery model and save schema. Ordinary and industrial handlers remain visible. |
| INV-09 | Reuse NEI's installed double-click inventory search highlight. Dedicated container-only search is deferred until verified coverage and a separate query focus rule exist. |
| INV-10 | Deferred. Equivalent-item highlighting needs a per-action equivalence policy and a verified Ore/IC2 recipe identity bridge. Strict stack equivalence already protects storage merges. |
| INV-11 | Implemented optional exact-stack totals in plain chests with the `#` control. The tooltip lists chest, main inventory and hotbar counts separately, includes protected slots, and groups only identical ID, metadata and NBT. The MCP count test passes. Other container categories and live UX acceptance remain open. |
| MACH-09 | Deferred. No audited ZTO machine has multiple useful saved inventory-routing profiles. Fraction selection remains the existing thermometer interaction. |
| BATCH-02 | Deferred. Material quantity requests need a requested-output control, recipe yield and a way to represent alternative requirements without selecting inputs automatically. |
| BATCH-04 | Deferred. Personal material lists need a list UI, item/recipe identity and a persistence lifecycle distinct from recipe bookmarks. |
| BATCH-06 | Deferred. Repeated intermediate crafting needs explicit per-step authorization and a verified recipe graph including reusable tools, byproducts and container returns. |
| BATCH-03 | Deferred. An advisory recursive planner requires stable recipe identities, cycle detection, yields, alternatives, tools and optional stock accounting. This is separate from local grid preparation. |

### Build and test evidence

- Windows FoxyRetroMDK 1.3.6, MCP 6.2 and JDK 8 `javac 1.8.0_504`. Client `recompile.bat` and `reobfuscate.bat` succeeded. This MDK reports missing server sources and server MD5s. No server build or server tests were run.
- `zto-tools/build-and-deploy.bat` packaged and deployed to isolated **Tekkit Dev**. Optional Hopper and creative textures were preserved from the prior local build. JAR inspection found the ZTO entry point and UX classes and found no packaged `gb.class`, NEI, CCC or compile-time mod stubs. The first deployed build started in Prism and the 2×2/3×3 observations above were made in the newly created Survival world only.

### Follow-up: compact controls and manual drag

The compact controls now use the full four-corner button border from the installed GUI atlas, a gap between the prepare and question controls, and a separate action row in the 2x2 panel. The 5x5 recipe preview uses the installed crafting slot, arrow and result textures. A separate Java 8 launch agent activates the manual drag gesture in local Survival. See the [inventory drag engineering notes](../engineering/inventory-drag.md) for activation, supported layouts, and verification.

### Follow-up: protected hotbar and release verification

Recipe preparation can now use the stock Inventory Tweaks hotbar row lock as an ingredient source after the main inventory. Frozen and individually locked slots are still excluded. Returns and storage operations retain their previous protection rules. The MCP harness verifies sourcing from the row and prevents returns into it.

The final client build was compiled, reobfuscated, and deployed to Tekkit Dev. Java 8u51 accepted all 176 entries through the streaming ZIP reader. The archive that previously crashed NEI on world entry failed the same check. Tekkit Dev reached a Survival world with NEI available, and a subsequent launch registered the drag agent with the corrected absolute path. The owner accepted the feature set. Individual physical drag gestures were not independently recorded in the final build.
