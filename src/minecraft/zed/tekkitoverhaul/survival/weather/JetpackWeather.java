package zed.tekkitoverhaul.survival.weather;

import java.lang.reflect.Method;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.BlockFluid;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.Material;
import net.minecraft.src.World;

/** Local IC2 1.97 weather state. IC2 owns activation, charge, steering and fall bookkeeping. */
public final class JetpackWeather {
    public enum Phase { DRY, WET, UNSTABLE, FAULT, RECOVERING, IMMERSED, RESTARTING }
    private static final String ELECTRIC = "ic2.common.ItemArmorJetpackElectric";
    private static final double PACK_HEIGHT = 1.35;
    private static final int RESTART_TICKS = 20;
    private static final int RESIDUAL_WETNESS = 48;
    private static final int RECOVERY_TICKS = 120;
    private static final int LANDING_CLEARANCE = 8;
    public static final JetpackWeather LOCAL = new JetpackWeather();
    private final ObjDoubleConsumer<Object> thrust = new ObjDoubleConsumer<Object>() {
        @Override public void accept(Object player, double previousY) {
            if (player instanceof EntityPlayer) onThrust((EntityPlayer) player, previousY);
        }
    };
    private final Predicate<Object> guard = new Predicate<Object>() {
        @Override public boolean test(Object player) {
            return Thread.currentThread() == owner && player instanceof EntityPlayer && player == localPlayer.get()
                    && blocksThrust((EntityPlayer) player);
        }
    };
    private Supplier<EntityPlayer> localPlayer;
    private Method activeHook;
    private Thread owner;
    private World world;
    private EntityPlayer pilot;
    private ItemStack pack;
    private Phase phase = Phase.DRY;
    private int wetness, remaining, untilSample, flightTicks;
    private boolean enabled, available, used, raining, storm, exposed, poweredRain, stormFault;
    private String condition = "No local player";

    private JetpackWeather() {}

    public void configure(boolean enabled, Supplier<EntityPlayer> localPlayer) {
        this.enabled = enabled;
        this.localPlayer = localPlayer;
        owner = Thread.currentThread();
        bind(null, null);
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false, ClassLoader.getSystemClassLoader());
            agent.getMethod("registerJetpack", ObjDoubleConsumer.class, Predicate.class)
                    .invoke(null, enabled ? thrust : null, enabled ? guard : null);
            activeHook = agent.getMethod("jetpackActive", ClassLoader.class);
        } catch (ReflectiveOperationException unavailable) {
            activeHook = null;
        }
    }

    private void bind(World world, EntityPlayer pilot) {
        this.world = world;
        this.pilot = pilot;
        pack = null;
        phase = Phase.DRY;
        wetness = remaining = untilSample = flightTicks = 0;
        available = used = raining = storm = exposed = poweredRain = stormFault = false;
    }

    private void identify(ItemStack current) {
        pack = current;
        phase = Phase.DRY;
        wetness = remaining = untilSample = flightTicks = 0;
        used = raining = storm = exposed = poweredRain = stormFault = false;
        available = false;
        if (current != null && activeHook != null) {
            try {
                available = Boolean.TRUE.equals(activeHook.invoke(null, current.getItem().getClass().getClassLoader()));
            } catch (ReflectiveOperationException unavailable) { available = false; }
        }
    }

    private static ItemStack electric(EntityPlayer player) {
        ItemStack chest = player.inventory.armorInventory[2];
        return chest != null && chest.getItem() != null && ELECTRIC.equals(chest.getItem().getClass().getName())
                ? chest : null;
    }

    private boolean blocksThrust(EntityPlayer player) {
        World context = player.worldObj;
        // IC2 can tick equipment before the local end-of-tick observer, including on load.
        if (!enabled || context == null || context.isRemote || !eligible(player)) return false;
        if (context != world || player != pilot) bind(context, player);
        ItemStack current = electric(player);
        if (current != pack) identify(current);
        return pack != null && available && updateImmersion(false);
    }

    private boolean updateImmersion(boolean advance) {
        double chest = pilot.boundingBox.minY + PACK_HEIGHT;
        int x = MathHelper.floor_double(pilot.posX);
        int y = MathHelper.floor_double(chest);
        int z = MathHelper.floor_double(pilot.posZ);
        // Same water surface as the legacy entity submersion check, sampled at the pack.
        if (world.blockExists(x, y, z) && world.getBlockMaterial(x, y, z) == Material.water
                && chest < y + 1 - (BlockFluid.getFluidHeightPercent(world.getBlockMetadata(x, y, z)) - 0.11111111F)) {
            phase = Phase.IMMERSED;
            wetness = 100;
            remaining = flightTicks = 0;
            used = exposed = poweredRain = false;
            untilSample = 0;
            condition = "Pack immersed in water";
            return true;
        }
        if (phase == Phase.IMMERSED) {
            phase = Phase.RESTARTING;
            remaining = RESTART_TICKS;
            condition = "Pack above water. Restart delay.";
            return true;
        }
        if (phase != Phase.RESTARTING) return false;
        if (advance && --remaining == 0) {
            phase = Phase.WET;
            wetness = RESIDUAL_WETNESS;
            condition = "Propulsion restored. Pack still wet.";
        }
        return true;
    }

    private void onThrust(EntityPlayer player, double previousY) {
        if (Thread.currentThread() != owner || !enabled || player != pilot || world == null || player.worldObj != world
                || !eligible(player) || pack == null || player.inventory.armorInventory[2] != pack)
            return;
        used = true;
        if (phase == Phase.FAULT && !player.onGround) {
            double sink = landingWithinReach(player) ? 0.16 : stormFault ? 0.64 : 0.48;
            // Suppress ascent and bleed off speed. Only native thrust can slow an existing fall.
            double limit = Math.min(0, Math.max(-sink, previousY - 0.38));
            player.motionY = Math.min(player.motionY, limit);
        }
    }

    private boolean landingWithinReach(EntityPlayer player) {
        AxisAlignedBB area = player.boundingBox.addCoord(0, -LANDING_CLEARANCE, 0);
        return world.checkChunksExist(MathHelper.floor_double(area.minX - 1), 0,
                MathHelper.floor_double(area.minZ - 1), MathHelper.floor_double(area.maxX + 1), 0,
                MathHelper.floor_double(area.maxZ + 1)) && !world.getCollidingBoundingBoxes(player, area).isEmpty();
    }

    public void tick(World context, EntityPlayer player, boolean paused) {
        if (context != world || player != pilot) bind(context, player);
        if (!enabled) { condition = "Disabled in settings"; return; }
        if (context == null || context.isRemote || player == null || player.worldObj != context || !eligible(player)) {
            if (pack != null) identify(null);
            condition = "Requires a local Survival player";
            return;
        }
        ItemStack current = electric(player);
        if (current != pack) identify(current);
        if (pack == null) { condition = "Electric Jetpack not equipped"; return; }
        if (!available) { condition = "Pinned IC2 thrust hook unavailable"; return; }
        if (paused) return;
        if (updateImmersion(true)) return;
        boolean justPowered = used && !player.onGround;
        if (justPowered) ++flightTicks;
        used = false;

        if (--untilSample <= 0) {
            untilSample = 10;
            raining = world.isRaining();
            storm = raining && world.isThundering();
            int x = MathHelper.floor_double(player.posX);
            int y = MathHelper.floor_double(player.boundingBox.minY + PACK_HEIGHT);
            int z = MathHelper.floor_double(player.posZ);
            exposed = raining && world.blockExists(x, y, z) && world.canLightningStrikeAt(x, y, z);
            boolean powered = flightTicks >= 3 && !player.onGround;
            poweredRain = powered && exposed;
            flightTicks = 0;
            if (poweredRain) {
                if (phase != Phase.FAULT && phase != Phase.RECOVERING)
                    wetness = Math.min(100, wetness + (storm ? 4 : 2));
            } else if (phase != Phase.FAULT) {
                wetness = Math.max(0, wetness - (exposed ? 2 : 6));
            }
            condition = !exposed ? (raining ? "Sheltered or outside rain" : "Clear weather")
                    : powered ? "Powered flight in rain" : "Awaiting powered airborne flight";
        }

        if (remaining > 0) --remaining;
        switch (phase) {
            case DRY:
            case WET:
                if (wetness >= 54) transition(Phase.UNSTABLE, 56);
                else if (wetness >= 16) phase = Phase.WET;
                else if (wetness <= 9) phase = Phase.DRY;
                break;
            case UNSTABLE:
                if (wetness <= 36) transition(Phase.RECOVERING, 70);
                else if (remaining == 0 && wetness >= 78 && exposed && !player.onGround
                        && poweredRain && justPowered) {
                    stormFault = storm;
                    transition(Phase.FAULT, stormFault ? 116 : 88);
                }
                break;
            case FAULT:
                if (remaining == 0 || player.onGround) {
                    wetness = Math.min(stormFault ? 58 : 48, wetness);
                    transition(Phase.RECOVERING, RECOVERY_TICKS);
                }
                break;
            case RECOVERING:
                if (remaining == 0) phase = wetness >= 16 ? Phase.WET : Phase.DRY;
                break;
            default:
                break;
        }
    }

    private void transition(Phase next, int ticks) {
        phase = next;
        remaining = ticks;
        String cue = next == Phase.FAULT ? "fault" : next == Phase.UNSTABLE ? "warning" : "recovery";
        world.playSoundAtEntity(pilot, "zto.jetpack." + cue, 0.62F, 1.0F);
        if (next != Phase.RECOVERING) {
            double angle = Math.toRadians(pilot.rotationYaw);
            world.spawnParticle("crit", pilot.posX + Math.sin(angle) * 0.35,
                    pilot.boundingBox.minY + 1.25, pilot.posZ - Math.cos(angle) * 0.35, 0, 0.04, 0);
        }
    }

    private static boolean eligible(EntityPlayer player) {
        return player.isEntityAlive() && !player.isPlayerSleeping() && !player.capabilities.isCreativeMode;
    }

    public boolean protects(EntityPlayer player) {
        return player == pilot && pack != null && player.inventory.armorInventory[2] == pack
                && (phase == Phase.FAULT || phase == Phase.RECOVERING && remaining > RECOVERY_TICKS - 40);
    }

    public String status() { return condition; }
    public Phase phase() { return phase; }
    public int wetness() { return wetness; }
    public int remaining() { return remaining; }
    public boolean equipped() { return pack != null; }
    public boolean available() { return available; }
    public boolean exposed() { return exposed; }
}
