package zed.tekkitoverhaul.survival.mining;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.src.BaseMod;
import net.minecraft.src.Block;
import net.minecraft.src.ChunkCoordinates;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.ModLoader;
import net.minecraft.src.PlayerControllerSP;
import net.minecraft.src.World;
import org.lwjgl.input.Keyboard;

/** Client input and transient work for one authoritative local Survival harvest gesture. */
public final class VeinMining implements ITickHandler {
    private static final int LIMIT = 24;
    private static final int RADIUS = 6;
    private static final double ANCHOR_DISTANCE_SQ = 25.0D;
    private static final int FOOD_CUTOFF = 6;
    private static final float EXTRA_EXHAUSTION = 1.2F;
    private static final int[][] FACES = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};

    private final KeyBinding modifier = new KeyBinding("Vein Mining (hold)", Keyboard.KEY_GRAVE);
    private final OreRegistry ores = new OreRegistry();
    private final BiConsumer<Object, int[]> harvestCallback = new BiConsumer<Object, int[]>() {
        @Override public void accept(Object controller, int[] event) { observeHarvest(controller, event); }
    };
    private final Predicate<Object> inputCallback = new Predicate<Object>() {
        @Override public boolean test(Object controller) { return ownsInput(controller); }
    };
    private Action starter;
    private Action action;
    private boolean latched, harvesting;

    public void register(BaseMod mod) {
        if (!attachAgent()) return;
        ModLoader.registerKey(mod, modifier, false);
        FMLCommonHandler.instance().registerTickHandler(this);
    }

    private boolean attachAgent() {
        try {
            Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false, ClassLoader.getSystemClassLoader())
                    .getMethod("registerVeinMining", BiConsumer.class, Predicate.class)
                    .invoke(null, harvestCallback, inputCallback);
            return true;
        } catch (ReflectiveOperationException unavailable) {
            return false;
        } catch (LinkageError unavailable) {
            return false;
        }
    }

    @Override public EnumSet<TickType> ticks() { return EnumSet.of(TickType.GAME); }
    @Override public String getLabel() { return "ZTO vein mining"; }
    @Override public void tickStart(EnumSet<TickType> types, Object... data) {}

    @Override public void tickEnd(EnumSet<TickType> types, Object... data) {
        tick(ModLoader.getMinecraftInstance());
    }

    private boolean held(Minecraft game) {
        return game != null && game.theWorld != null && !game.theWorld.isRemote && game.thePlayer != null
                && game.thePlayer.isEntityAlive() && !game.thePlayer.capabilities.isCreativeMode
                && game.currentScreen == null && game.inGameHasFocus && game.gameSettings != null
                && modifier.pressed && game.gameSettings.keyBindAttack.pressed;
    }

    private boolean ownsInput(Object controller) {
        if (!latched || harvesting) return false;
        Minecraft game = ModLoader.getMinecraftInstance();
        if (!held(game)) reset();
        return latched && game.playerController == controller;
    }

    private void observeHarvest(Object controller, int[] event) {
        if (harvesting || latched) return;
        Minecraft game = ModLoader.getMinecraftInstance();
        if (event[3] == 0) {
            starter = null;
            if (!held(game) || game.playerController != controller || !(controller instanceof PlayerControllerSP)) return;
            World world = game.theWorld;
            EntityPlayer player = game.thePlayer;
            Point point = new Point(event[0], event[1], event[2], 0);
            if (!world.blockExists(point.x, point.y, point.z) || !player.canPlayerEdit(point.x, point.y, point.z)) return;
            int ore = ores.key(world, point);
            Tool tool = Tool.from(player.getCurrentEquippedItem());
            if (ore == 0 || tool == null || !tool.canWork(player)) return;
            Block block = Block.blocksList[world.getBlockId(point.x, point.y, point.z)];
            if (!block.canHarvestBlock(player, world.getBlockMetadata(point.x, point.y, point.z))) return;
            starter = new Action(world, player, (PlayerControllerSP) controller, point, ore, tool);
        } else {
            Action prepared = starter;
            starter = null;
            if (prepared == null || !held(game) || !prepared.valid(game) || !prepared.origin.at(event)
                    || prepared.world.getBlockId(event[0], event[1], event[2]) != 0) return;
            action = prepared;
            latched = true;
            action.expand(action.origin, ores);
        }
    }

    private void tick(Minecraft game) {
        starter = null;
        if (!held(game)) {
            reset();
            return;
        }
        if (action == null) return;
        Action work = action;
        if (!work.valid(game) || !work.tool.canWork(work.player)) {
            action = null;
            return;
        }
        Point point = work.pending.peek();
        if (point == null || !work.world.blockExists(point.x, point.y, point.z)
                || ores.key(work.world, point) != work.ore || !work.player.canPlayerEdit(point.x, point.y, point.z)) {
            action = null;
            return;
        }
        Block block = Block.blocksList[work.world.getBlockId(point.x, point.y, point.z)];
        if (!block.canHarvestBlock(work.player, work.world.getBlockMetadata(point.x, point.y, point.z))) {
            action = null;
            return;
        }
        if (work.rest > 0) {
            work.rest--;
            return;
        }
        float strength = block.blockStrength(work.world, work.player, point.x, point.y, point.z);
        if (!(strength > 0)) {
            action = null;
            return;
        }
        work.damage += strength;
        if (work.damage < 1.0F) return;

        harvesting = true;
        boolean removed;
        try {
            removed = work.controller.onPlayerDestroyBlock(point.x, point.y, point.z, point.face);
        } finally {
            harvesting = false;
        }
        if (!removed || work.world.getBlockId(point.x, point.y, point.z) != 0) {
            action = null;
            return;
        }
        work.player.swingItem();
        if (!work.tool.electric()) work.player.addExhaustion(EXTRA_EXHAUSTION);
        work.pending.remove();
        work.damage = 0;
        work.rest = work.tool.electric() ? 0 : 2;
        if (++work.harvested >= LIMIT) action = null;
        else work.expand(point, ores);
    }

    private void reset() {
        starter = action = null;
        latched = false;
    }

    private static final class Action {
        final World world;
        final EntityPlayer player;
        final PlayerControllerSP controller;
        final Point origin;
        final int ore, heldSlot;
        final Tool tool;
        final Queue<Point> pending = new ArrayDeque<Point>();
        final Set<ChunkCoordinates> seen = new HashSet<ChunkCoordinates>();
        int harvested = 1, rest;
        float damage;

        Action(World world, EntityPlayer player, PlayerControllerSP controller, Point origin, int ore, Tool tool) {
            this.world = world;
            this.player = player;
            this.controller = controller;
            this.origin = origin;
            this.ore = ore;
            this.tool = tool;
            this.heldSlot = player.inventory.currentItem;
            seen.add(origin.coords());
        }

        boolean valid(Minecraft game) {
            return game.theWorld == world && game.thePlayer == player && game.playerController == controller
                    && player.inventory.currentItem == heldSlot && player.getCurrentEquippedItem() == tool.stack
                    && player.getDistanceSq(origin.x + 0.5D, origin.y + 0.5D, origin.z + 0.5D) <= ANCHOR_DISTANCE_SQ;
        }

        void expand(Point point, OreRegistry ores) {
            for (int side = 0; side < FACES.length; side++) {
                int[] face = FACES[side];
                Point next = new Point(point.x + face[0], point.y + face[1], point.z + face[2], side ^ 1);
                if (Math.abs(next.x - origin.x) + Math.abs(next.y - origin.y) + Math.abs(next.z - origin.z) > RADIUS
                        || !seen.add(next.coords()) || !world.blockExists(next.x, next.y, next.z)) continue;
                if (ores.key(world, next) == ore) pending.add(next);
            }
        }
    }

    static final class Point {
        final int x, y, z, face;
        Point(int x, int y, int z, int face) { this.x = x; this.y = y; this.z = z; this.face = face; }
        boolean at(int[] event) { return x == event[0] && y == event[1] && z == event[2]; }
        ChunkCoordinates coords() { return new ChunkCoordinates(x, y, z); }
    }

    private static final class Tool {
        final ItemStack stack;
        final Method canUse;
        final int eu;
        Tool(ItemStack stack, Method canUse, int eu) { this.stack = stack; this.canUse = canUse; this.eu = eu; }

        static Tool from(ItemStack stack) {
            if (stack == null || stack.getItem() == null) return null;
            Item item = stack.getItem();
            if (item == Item.pickaxeWood || item == Item.pickaxeStone || item == Item.pickaxeSteel
                    || item == Item.pickaxeGold || item == Item.pickaxeDiamond) return new Tool(stack, null, 0);
            String type = item.getClass().getName();
            if ("eloraam.world.ItemCustomPickaxe".equals(type) && OreRegistry.redPowerPick(item)) return new Tool(stack, null, 0);
            if (!"ic2.common.ItemElectricToolDrill".equals(type) && !"ic2.common.ItemElectricToolDDrill".equals(type))
                return null;
            try {
                Method hasCharge = Class.forName("ic2.common.ElectricItem", false, item.getClass().getClassLoader())
                        .getMethod("canUse", ItemStack.class, Integer.TYPE);
                int cost = item.getClass().getField("operationEnergyCost").getInt(item);
                return cost > 0 ? new Tool(stack, hasCharge, cost) : null;
            } catch (ReflectiveOperationException unavailable) {
                return null;
            } catch (LinkageError unavailable) {
                return null;
            }
        }

        boolean electric() { return canUse != null; }
        boolean canWork(EntityPlayer player) {
            if (stack.stackSize <= 0) return false;
            if (!electric()) return player.getFoodStats().getFoodLevel() >= FOOD_CUTOFF;
            try {
                return Boolean.TRUE.equals(canUse.invoke(null, stack, eu));
            } catch (ReflectiveOperationException unavailable) {
                return false;
            } catch (LinkageError unavailable) {
                return false;
            }
        }
    }
}
