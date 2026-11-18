package zed.tekkitoverhaul.survival.weather;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

/** Local IC2 1.97 weather state. IC2 owns activation, charge, steering and fall bookkeeping. */
public final class JetpackWeather {
    public enum Phase { DRY, WET, UNSTABLE, FAULT, RECOVERING }
    private static final String ELECTRIC = "ic2.common.ItemArmorJetpackElectric";
    public static final JetpackWeather LOCAL = new JetpackWeather();
    private final BiConsumer<Object, Object> thrust = new BiConsumer<Object, Object>() {
        @Override public void accept(Object item, Object player) {
            if (player instanceof EntityPlayer) onThrust(item, (EntityPlayer) player);
        }
    };
    private Method activeHook;
    private Thread owner;
    private World world;
    private EntityPlayer pilot;
    private ItemStack pack;
    private Phase phase = Phase.DRY;
    private int wetness, remaining, untilSample, flightTicks;
    private boolean enabled, available, used, raining, storm, exposed, poweredRain;
    private String condition = "No local player";

    private JetpackWeather() {}

    public void configure(boolean enabled) {
        this.enabled = enabled;
        owner = Thread.currentThread();
        bind(null, null);
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false, ClassLoader.getSystemClassLoader());
            agent.getMethod("registerJetpack", BiConsumer.class).invoke(null, enabled ? thrust : null);
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
        available = used = raining = storm = exposed = poweredRain = false;
    }

    private void identify(ItemStack current) {
        pack = current;
        phase = Phase.DRY;
        wetness = remaining = untilSample = flightTicks = 0;
        used = raining = storm = exposed = poweredRain = false;
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

    private void onThrust(Object item, EntityPlayer player) {
        if (Thread.currentThread() != owner || !enabled || player != pilot || world == null || player.worldObj != world
                || !eligible(player) || pack == null || player.inventory.armorInventory[2] != pack || pack.getItem() != item)
            return;
        used = true;
        if (phase == Phase.FAULT && !player.onGround) {
            // Preserve every bit of powered descent braking. Shed upward speed gradually.
            if (player.motionY > 0.02) player.motionY = Math.max(0.02, player.motionY - 0.22);
        }
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
        boolean justPowered = used && !player.onGround;
        if (justPowered) ++flightTicks;
        used = false;

        if (--untilSample <= 0) {
            untilSample = 10;
            raining = world.isRaining();
            storm = raining && world.isThundering();
            int x = MathHelper.floor_double(player.posX);
            int y = MathHelper.floor_double(player.boundingBox.minY + 1.35);
            int z = MathHelper.floor_double(player.posZ);
            exposed = raining && world.blockExists(x, y, z) && world.canLightningStrikeAt(x, y, z);
            boolean powered = flightTicks >= 3 && !player.onGround;
            poweredRain = powered && exposed;
            flightTicks = 0;
            if (powered && exposed && phase != Phase.FAULT && phase != Phase.RECOVERING) {
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
                        && poweredRain && justPowered) transition(Phase.FAULT, 22);
                break;
            case FAULT:
                if (remaining == 0 || player.onGround) {
                    wetness = Math.min(42, wetness);
                    transition(Phase.RECOVERING, 120);
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
                && (phase == Phase.FAULT || phase == Phase.RECOVERING && remaining > 80);
    }

    public String status() { return condition; }
    public Phase phase() { return phase; }
    public int wetness() { return wetness; }
    public int remaining() { return remaining; }
    public boolean equipped() { return pack != null; }
    public boolean available() { return available; }
    public boolean exposed() { return exposed; }
}
