package zed.tekkitoverhaul.crafting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import net.minecraft.src.Block;
import net.minecraft.src.CompressedStreamTools;
import net.minecraft.src.Container;
import net.minecraft.src.ContainerWorkbench;
import net.minecraft.src.CraftingManager;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryCrafting;
import net.minecraft.src.InventoryBasic;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.ShapedRecipes;
import net.minecraft.src.Slot;
import net.minecraft.src.SaveHandlerMP;
import net.minecraft.src.World;
import net.minecraft.src.WorldProvider;
import net.minecraft.src.WorldSettings;
import net.minecraft.src.WorldType;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.distillation.ItemDistillateBucket;
import zed.tekkitoverhaul.integration.inventory.DistributionPlanner;

/** Runs against MCP's real ItemStack, Slot, Container and recipe classes. */
public final class CraftingUxTest {
    public static void main(String[] args) throws IOException {
        batchAndReturn();
        holesAndOverflow();
        matchingAndFreshness();
        sharedVariants();
        storageAndBookmarks();
        dragDistribution();
        hotbarRecipeSource();
        containerCrafting();
        check(!DistillationRegistry.isBuildCraftOilBucket(new ItemStack(
                new ItemDistillateBucket(20000, DistillateType.HEAVY_GAS_OIL))),
                "Heavy Gas Oil is not a BuildCraft crude oil bucket");
        System.out.println("Crafting UX transaction checks passed.");
    }

    private static void containerCrafting() {
        World world = new World(new SaveHandlerMP(), "Crafting check", WorldProvider.getProviderForDimension(0),
                new WorldSettings(0L, 0, false, false, WorldType.DEFAULT));
        EntityPlayer player = new EntityPlayer(world) {
            @Override
            public void func_6420_o() {}
        };
        ContainerWorkbench workbench = new ContainerWorkbench(player.inventory, world, 0, 64, 0);
        Slot[] slots = new Slot[45];
        for (int i = 0; i < slots.length; i++) slots[i] = workbench.getSlot(i + 1);
        ItemStack[] sticks = new ItemStack[9];
        sticks[1] = new ItemStack(Block.planks, 1);
        sticks[4] = new ItemStack(Block.planks, 1);
        ItemStack output = new ItemStack(Item.stick, 4);
        CraftingFill fill = new CraftingFill(slots, 3);
        player.inventory.setInventorySlotContents(9, new ItemStack(Block.planks, 1));
        check(fill.prepare(sticks, output, 1, false).plan == null, "missing planks refuse a complete transfer");
        check(workbench.getSlot(0).getStack() == null && player.inventory.getStackInSlot(9).stackSize == 1,
                "missing recipe preserves source and output");
        player.inventory.setInventorySlotContents(9, new ItemStack(Block.planks, 2));
        check(fill.prepare(sticks, output, 1, false).plan.commit(workbench), "prepare sticks in real workbench slots");
        check(workbench.craftMatrix.getStackInSlot(1).stackSize == 1
                && workbench.craftMatrix.getStackInSlot(4).stackSize == 1
                && player.inventory.getStackInSlot(9) == null, "exact ingredient placement and debit");
        check(ItemStack.areItemStacksEqual(workbench.getSlot(0).getStack(), output), "real vanilla result slot matches");

        AdvancedCraftingManager.register(new ItemStack(Block.brick), new String[] {
            "S   S", "     ", "     ", "     ", "S   S"
        }, Character.valueOf('S'), Block.stone);
        ContainerAdvancedCraftingTable advanced = new ContainerAdvancedCraftingTable(player.inventory, world, 0, 64, 0);
        for (int i : new int[] {0, 4, 20}) advanced.craftMatrix.setInventorySlotContents(i, new ItemStack(Block.stone));
        advanced.craftMatrix.setInventorySlotContents(23, new ItemStack(Block.stone));
        check(advanced.getSlot(0).getStack() == null, "reject misplaced 5x5 ingredient");
        advanced.craftMatrix.setInventorySlotContents(23, null);
        advanced.craftMatrix.setInventorySlotContents(24, new ItemStack(Block.stone, 2));
        check(ItemStack.areItemStacksEqual(advanced.getSlot(0).getStack(), new ItemStack(Block.brick)),
                "real advanced result slot matches");
        Slot[] advancedSlots = new Slot[61];
        for (int i = 0; i < advancedSlots.length; i++) advancedSlots[i] = advanced.getSlot(i + 1);
        check(new CraftingFill(advancedSlots, 5).returnInputs().plan.commit(advanced), "return remaining input");
        check(advanced.getSlot(0).getStack() == null && advanced.craftMatrix.getStackInSlot(24) == null
                && ItemStack.areItemStacksEqual(player.inventory.getStackInSlot(9), new ItemStack(Block.stone, 5)),
                "recover all five stones and clear the advanced result");
    }

    private static void hotbarRecipeSource() {
        check(ProtectedSlots.isRecipeOnlySource(27, 2999999, false), "hotbar row lock permits recipe sourcing");
        check(!ProtectedSlots.isRecipeOnlySource(27, 2999999, true), "frozen hotbar stays protected");
        check(!ProtectedSlots.isRecipeOnlySource(27, 4999999, false), "explicit slot lock stays protected");
        check(!ProtectedSlots.isRecipeOnlySource(26, 2999999, false), "other locked rows stay protected");
        Fixture fixture = new Fixture(2, 2, 64);
        fixture.slots[4].putStack(new ItemStack(Block.dirt, 64));
        fixture.slots[5].putStack(new ItemStack(Block.wood, 3, 2));
        CraftingFill fill = new CraftingFill(fixture.slots, 2, 5);
        ItemStack[] pattern = {new ItemStack(Block.wood, 1, 2), null, null, null};
        CraftingFill.Result loaded = fill.prepare(pattern, new ItemStack(Block.planks, 4, 2), 1, false);
        check(loaded.plan != null && loaded.plan.commit(fixture), "hotbar alone supplies a complete recipe");
        check(fixture.slots[5].getStack().stackSize == 2, "only one ingredient taken from hotbar");
        check(fill.returnInputs().plan.commit(fixture) && fixture.slots[0].getStack() != null,
                "return leaves ingredient in grid when only the locked row has room");
    }

    private static void dragDistribution() {
        int[] room = {64, 2, 64, 0};
        check(Arrays.equals(DistributionPlanner.drag(14, room, false), new int[] {4, 2, 4, 0}),
                "left drag uses equal shares and leaves excess on cursor");
        check(Arrays.equals(DistributionPlanner.drag(14, room, true), new int[] {1, 1, 1, 0}),
                "right drag gives one to each eligible slot");
        check(Arrays.equals(DistributionPlanner.drag(2, room, true), new int[] {1, 1, 0, 0}),
                "right drag stops when the cursor is exhausted");
        check(Arrays.equals(DistributionPlanner.drag(9, new int[] {0, 0}, false), new int[] {0, 0}),
                "full slots take nothing");
    }

    private static void storageAndBookmarks() throws IOException {
        Fixture fixture = new Fixture(2, 4, 64);
        Slot[] slots = {fixture.slots[4], fixture.slots[5], fixture.slots[6], fixture.slots[7]};
        slots[0].putStack(new ItemStack(Block.cloth, 63, 2));
        slots[1].putStack(new ItemStack(Block.dirt, 64));
        slots[2].putStack(new ItemStack(Block.cloth, 3, 2));
        slots[3].putStack(new ItemStack(Block.cloth, 2, 3));
        InventoryPlan deposit = new InventoryPlan(slots);
        check(CraftingDestination.planStorage(deposit, 2, 0) == 1 && deposit.commit(fixture), "matching deposit");
        check(slots[2].getStack().stackSize == 2 && slots[3].getStack().stackSize == 2,
                "overflow and different metadata stay at source");
        InventoryPlan take = new InventoryPlan(slots);
        check(CraftingDestination.planStorage(take, 2, 2) == 62 && take.commit(fixture), "take fits only matching room");
        check(slots[0].getStack().stackSize == 2 && slots[1].getStack().stackSize == 64, "take preserves overflow");
        check(CraftingDestination.exactCount(fixture, slots[0].getStack(), 4, 8) == 66,
                "totals separate metadata and count both sections");

        File base = Files.createTempDirectory(new File("temp").toPath(), "crafting-bookmarks-").toFile();
        File config = new File(base, "config");
        check(config.mkdir(), "bookmark test directory");
        try {
            ItemStack item = new ItemStack(Block.cloth, 1, 7);
            item.setTagCompound(new NBTTagCompound());
            item.getTagCompound().setString("tagged", "value");
            RecipeBookmarks.Entry entry = new RecipeBookmarks.Entry(item, true, "Shaped Crafting", 2);
            RecipeBookmarks bookmarks = new RecipeBookmarks(base);
            check(bookmarks.toggle(entry).equals("Recipe bookmarked"), "bookmark saved");
            File file = new File(config, "ZTORecipeBookmarks.dat");
            NBTTagCompound saved = CompressedStreamTools.read(file);
            NBTTagList entries = saved.getTagList("recipes");
            NBTTagCompound invalid = new NBTTagCompound();
            NBTTagCompound invalidItem = new NBTTagCompound();
            invalidItem.setShort("id", (short) -1);
            invalid.setCompoundTag("item", invalidItem);
            NBTTagList mixed = new NBTTagList();
            mixed.appendTag(invalid);
            mixed.appendTag(entries.tagAt(0));
            saved.setTag("recipes", mixed);
            CompressedStreamTools.write(saved, file);
            bookmarks = new RecipeBookmarks(base);
            check(bookmarks.advance().same(entry), "invalid bookmark does not hide a valid entry");
            check(bookmarks.toggle(entry).equals("Bookmark removed") && new RecipeBookmarks(base).advance() == null,
                    "bookmark removal persists");
        } finally {
            Files.deleteIfExists(new File(config, "ZTORecipeBookmarks.dat").toPath());
            Files.deleteIfExists(config.toPath());
            Files.deleteIfExists(base.toPath());
        }
    }

    @SuppressWarnings("unchecked")
    private static void sharedVariants() {
        Fixture fixture = new Fixture(2, 2, 64);
        ItemStack oak = new ItemStack(Block.wood, 1, 0);
        ItemStack birch = new ItemStack(Block.wood, 1, 2);
        ItemStack output = new ItemStack(Item.snowball);
        CraftingManager.getInstance().getRecipeList().add(0, new ShapedRecipes(2, 1,
                new ItemStack[] {new ItemStack(Block.wood, 1, -1), oak}, output));
        ItemStack[] shown = {oak, oak, null, null};
        fixture.slots[4].putStack(oak);
        fixture.slots[5].putStack(birch);
        LegacyNei.Selection selection = new LegacyNei.Selection(null, null, 0, shown, output, false, false,
                new boolean[] {true, false, false, false});
        CraftingFill.Result exact = fixture.fill.prepare(shown, output, 1, false);
        check(exact.plan == null, "pinned variant refuses substitution");
        CraftingFill.Result alternative = new VariantFill(fixture.fill, selection).prepare(exact, false);
        check(alternative.alternatives && alternative.plan.commit(fixture), "flexible slot leaves oak for exact slot");
        check(fixture.slots[0].getStack().getItemDamage() == 2 && fixture.slots[1].getStack().getItemDamage() == 0,
                "owned variant and exact metadata are both preserved");
    }

    private static void batchAndReturn() {
        Fixture fixture = new Fixture(3, 3, 64);
        ItemStack[] pattern = new ItemStack[9];
        pattern[0] = new ItemStack(Block.planks);
        pattern[3] = new ItemStack(Block.planks);
        ItemStack sticks = new ItemStack(Item.stick, 4);
        fixture.slots[0].putStack(new ItemStack(Block.planks, 3));
        fixture.slots[9].putStack(new ItemStack(Block.planks, 61));
        CraftingFill.Result result = fixture.fill.maximum(pattern, sticks);
        check(result.sets == 32 && result.plan.commit(fixture), "32 balanced sets");
        check(fixture.slots[0].getStack().stackSize == 32 && fixture.slots[3].getStack().stackSize == 32,
                "existing grid stock participates");
        check(!result.plan.commit(fixture), "a plan is single-use");
        CraftingFill.Result returned = fixture.fill.returnInputs();
        check(returned.plan.commit(fixture) && fixture.slots[9].getStack().stackSize == 64,
                "return merges without losing materials");

        fixture.slots[9].putStack(new ItemStack(Block.planks, 1));
        CraftingFill.Result shortage = fixture.fill.prepare(pattern, sticks, 1, false);
        check(shortage.plan == null && shortage.available[0] == 1 && shortage.available[3] == 0,
                "shared stock is counted once");
        check(fixture.slots[9].getStack().stackSize == 1, "shortage leaves inventory alone");
        CraftingFill.Result partial = fixture.fill.prepare(pattern, sticks, 1, true);
        check(partial.plan.commit(fixture) && fixture.slots[3].getStack() == null, "explicit partial action");
    }

    private static void holesAndOverflow() {
        ItemStack output = new ItemStack(Block.stoneOvenIdle);
        AdvancedCraftingManager.register(output, new String[] {"SSSSS", "S   S", "S   S", "S   S", "SSSSS"},
                'S', Block.stone);
        ItemStack[] pattern = AdvancedCraftingManager.getRecipes().get(0).getInputs();
        AdvancedCraftingManager.getRecipes().get(0).getInputs()[0].stackSize = 3;
        check(AdvancedCraftingManager.getRecipes().get(0).getInputs()[0].stackSize == 1, "defensive recipe view");
        Fixture fixture = new Fixture(5, 1, 64);
        fixture.slots[6].putStack(new ItemStack(Block.dirt));
        fixture.slots[25].putStack(new ItemStack(Block.stone, 32));
        check(fixture.fill.prepare(pattern, output, 1, false).plan == null, "single fill cannot displace into full bag");
        CraftingFill.Result result = fixture.fill.maximum(pattern, output);
        check(result.sets == 2 && result.plan.commit(fixture), "batch frees room for displaced stack");
        check(fixture.slots[6].getStack() == null && fixture.slots[25].getStack().itemID == Block.dirt.blockID,
                "Foundry-style perimeter preserves the nine holes");
        check(fixture.fill.returnInputs().status.equals("32 items still in grid"), "full bag retains the grid remainder");
    }

    private static void matchingAndFreshness() {
        Fixture fixture = new Fixture(2, 2, 1);
        ItemStack tagged = new ItemStack(Block.cloth, 1, 2);
        tagged.setTagCompound(new NBTTagCompound());
        tagged.getTagCompound().setString("key", "first");
        ItemStack other = tagged.copy();
        other.getTagCompound().setString("key", "second");
        check(!InventoryPlan.sameType(tagged, other), "different NBT cannot merge");
        check(!InventoryPlan.sameType(tagged, new ItemStack(Block.cloth, 1, 3)), "metadata is exact");
        fixture.slots[4].putStack(tagged);
        InventoryPlan plan = new InventoryPlan(fixture.slots);
        check(plan.insert(new ItemStack(Block.cloth, 3, 2), 4, 6) == 2, "custom slot limit and NBT");
        fixture.slots[4].putStack(other);
        check(!plan.commit(fixture) && fixture.slots[5].getStack() == null, "stale snapshot rejects all writes");
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }

    private static final class Fixture extends Container {
        final Slot[] slots;
        final CraftingFill fill;

        Fixture(int width, int stock, final int limit) {
            int cells = width * width;
            slots = new Slot[cells + stock];
            InventoryCrafting grid = new InventoryCrafting(this, width, width);
            InventoryBasic bag = new InventoryBasic("Stock", stock);
            for (int i = 0; i < slots.length; i++) {
                slots[i] = i < cells ? new Slot(grid, i, 0, 0) : new Slot(bag, i - cells, 0, 0) {
                    @Override
                    public int getSlotStackLimit() { return limit; }
                };
                addSlot(slots[i]);
            }
            fill = new CraftingFill(slots, width);
        }

        @Override
        public boolean canInteractWith(EntityPlayer player) { return true; }
    }
}
