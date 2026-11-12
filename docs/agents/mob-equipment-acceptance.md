# Mob equipment compatibility and acceptance

Target candidate `0.1.0-alpha.3` for Minecraft 1.2.5 and Tekkit Classic 3.1.2. The implementation contract and fixture are in the [backport specification](MOB_EQUIPMENT_BACKPORT_SPEC.md).

## Compatibility decision

Audited the actual MCP 6.2 client sources and `conf/client.srg`, then inspected the effective Prism Tekkit Dev classpath using the selected jar-mod ahead of the 1.2.5 base client. The selected jar-mod and `minecraft/bin/modpack.jar` both hashed to SHA-256 `0827ee55325fedbc37f2a6a890f19db11f75fab5e9a5c02eafd1bd50d2b59446`. The development source profile is Forge 3.4.9.171. The runtime log identifies Forge 3.3.8.164, FML 2.2.106.176 and OptiFine 1.2.5 HD S C6. The overlay includes NEI and its patched inventory classes.

| Area | MCP source and effective runtime ABI |
| --- | --- |
| Vanilla combat | Zombie `ajg` and Skeleton `xr` extend Mob `yy`, Creature `aaa`, Living `acq` and Entity `nn`. The base client provides both concrete mob classes and their combat tasks. Zombie's collide task `ax` calls `attackEntityAsMob(Entity)`, runtime `yy.c(Lnn;)Z`, with native attack strength 4 and potion modifiers. Skeleton's ranged task `sf` creates an arrow directly with its target and a 60-tick interval. |
| Special held items | `getHeldItem()` is `acq.ae()Laan;`. Skeleton `xr` and pigman `alt` override it with stock bow and golden sword respectively. Pigman extends Zombie, so equipment eligibility checks the exact class. The normal biped renderer still draws those native stacks. |
| Persistence | Overlay Entity `nn` exposes `getEntityData()Lady;`. Its `writeToNBT` and `readFromNBT` are `nn.d(Lady;)V` and `nn.e(Lady;)V` and write/read `ForgeData` alongside the vanilla state before dispatching to `b(Lady;)V` and `a(Lady;)V`. The existing `EntityList` mapping still loads the original entity IDs and classes. |
| Damage and loot | OptiFine's overlay Living `acq` calls `forge.ForgeHooks` through `Reflector`. `damageEntity` at `acq.c(Lmd;I)V` calls `onEntityLivingHurt(Lacq;Lmd;I)I` before `applyArmorCalculations` at `acq.d(Lmd;I)I`. The latter reads the native armor value from `au()I` and respects bypass sources. `onDeath` at `acq.a(Lmd;)V` collects stock loot through `a(ZI)V` and dispatches `onEntityLivingDrops(Lacq;Lmd;Ljava/util/ArrayList;IZI)V` before insertion. Skeleton's override calls the Living death method. The same registration and handler descriptors exist in both inspected Forge profiles. |
| Rendering | Overlay `RenderManager` (`ahu`) initializes both stock mob entries with overlay `RenderBiped` (`tg`). Its held-item path is `tg.b(Lacq;F)V`. Base `RenderLiving` (`fe`) supplies armor-pass selection `b(Lacq;IF)I`. Overlay `RenderPlayer` (`we`) establishes the stock armor paths, pass order and thinner leggings model. FML passes its renderer map to `BaseMod.addRenderer(Map)`, then initializes registered renderers. NEI's `gb.class` belongs to the installed overlay. |

Register one legacy Forge handler and wrap only the two stock biped renderer entries through FML. Use the original renderer directly for ordinary mobs and subclasses. If either renderer entry already belongs to a different renderer type, skip that registration and log the conflict. The equipment feature ships no class replacements, transformers, entity registrations or resource packs.

## Runtime evidence

### Development run

GitHub `main` and the starting checkout both resolved to `8e7679181015b54b1e7fb4c581f7feb8318189f6`. The development JAR carried product version `0.1.0-alpha.2`, that Git SHA, `sourceDirty=true`, UTC build time `2026-11-12T10:17:42.8418421Z`, and SHA-256 `25d3a6ba5386b728c6bb40f6ee9b3d75470ee11cffc6b3ec64df7c43309eb2bd`. MCP client compilation and reobfuscation passed and the packaging script streamed all 251 entries. Every packaged class was under `zed/`.

The in-game probe ran on a separate copy of the isolated Tekkit Dev client with the same merged pack, local Java 8 runtime and existing inventory and lunar agents. The equipment test opened the marked save `ZTO-Test-c735f2f6fa0b4106b0d659abfd603f93` with Dev Bridge and the Survival Debug HUD off. The probe confirmed the running ZTO code source was the deployed mod archive and exercised the native game thread, not an MCP stub.

| Case | Status | Observed |
| --- | --- | --- |
| Visible Zombie and Skeleton gear | PASS | Screenshots show the iron sword, native bow, four armor layers on each mob and unmodified comparison mobs. |
| Melee damage | PASS | The ordinary and iron-sword Zombies dealt 4 and 6 HP respectively to fresh unarmored vanilla living targets and to the unarmored player on Normal. |
| Incoming protection | PASS | An 8 HP player hit removed 7 HP from the bare Zombie and 2 HP from the equipped Zombie. The bare and armored Skeleton lost 8 and 4 HP. |
| Native ranged behavior | PASS | The armored and ordinary Skeleton each spawned real arrows attributed to the correct shooter through the existing AI. Both rendered the native bow pose. |
| Save and Quit and reopen | PASS | The same four original entities loaded after the world was reopened from the title screen. All eight armor pieces and the iron sword's ID, count, damage value 7 and custom tag returned. There were zero owned gear drops before save and after reopen. |
| Equipment drops | PASS | Player kill and lethal fall produced nine owned stacks total. Replaying both death callbacks and advancing world ticks added no owned drops. |
| Despawn | PASS | The legacy direct removal path for a stone-sword Zombie emitted no gear. The save/reopen case also exercised world unload. |
| Ordinary mobs | PASS | The comparison mobs kept their usual empty hand or bow, base damage/protection and stock renderer. The bounded probe did not statistically sample rare loot. |
| Startup without diagnostics | PASS | The pack completed startup and opened the test world with Dev Bridge and Survival Debug HUD disabled. |
| Lunar assault regression | PASS | In a separate Hard Survival arena, natural dusk at raw tick 205,000 started the assault. Wave 1 opened at 205,100 and placed 4/4 vanilla Skeletons with 12 nearby hostiles and peak active count 4. The HUD reflected placement and light rejections. Pause froze elapsed time and Save and Quit reopened as `ABORTED / INTERRUPTED_ON_REOPEN` with zero new starts or insertions. |

| Equipped Zombie | Armored Skeleton with its native bow |
| --- | --- |
| ![Equipped Zombie next to ordinary Zombie](images/mob-equipment-zombie.png) | ![Armored Skeleton next to ordinary Skeleton](images/mob-equipment-skeleton.png) |
