package zed.tekkitoverhaul.territory;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.World;

/** Budgets the vanilla hostile pass for the active single-player world. */
public final class NaturalSpawnPressure {
    private static final int SATURATION_ACTIVITY = 20;
    private final TerritorialActivityService territory;
    private final Thread owner = Thread.currentThread();
    private final int maxIncreasePercent;
    private final BiConsumer<Object, Boolean> start = new BiConsumer<Object, Boolean>() {
        public void accept(Object context, Boolean enabled) { begin(context, enabled); }
    };
    private final BiFunction<Object, int[], Boolean> cap = new BiFunction<Object, int[], Boolean>() {
        public Boolean apply(Object context, int[] population) { return aboveCap(context, population); }
    };
    private final Predicate<Object> candidate = new Predicate<Object>() {
        public boolean test(Object context) { return hasRoom(context); }
    };
    private final Consumer<Object> inserted = new Consumer<Object>() {
        public void accept(Object context) { recordSpawn(context); }
    };
    private Method agentActive;
    private World world;
    private String status = "Waiting for hostile pass";
    private int count, vanillaCap, effectiveCap, increasePercent, passActivity;
    private int spawnedThisPass;
    private long successfulSpawns, limitedPasses;

    public NaturalSpawnPressure(TerritorialActivityService territory, int maxIncreasePercent) {
        this.territory = territory;
        this.maxIncreasePercent = maxIncreasePercent;
    }

    public void register() {
        try {
            Class<?> agent = Class.forName("zed.tekkitoverhaul.agent.ZTOAgent", false,
                    ClassLoader.getSystemClassLoader());
            agent.getMethod("registerNaturalSpawning", BiConsumer.class, BiFunction.class,
                    Predicate.class, Consumer.class).invoke(null, start, cap, candidate, inserted);
            agentActive = agent.getMethod("naturalSpawningActive", ClassLoader.class);
        } catch (ReflectiveOperationException unavailable) {
            agentActive = null;
        } catch (LinkageError unavailable) {
            agentActive = null;
        }
    }

    public void bind(World next) {
        if (next == world) return;
        world = next;
        successfulSpawns = limitedPasses = 0;
        begin(next, true);
    }

    private boolean owns(Object context) {
        return Thread.currentThread() == owner && context != null && context == world && !world.isRemote;
    }

    private void begin(Object context, boolean hostileEnabled) {
        if (context != world || Thread.currentThread() != owner) return;
        if (hostileEnabled) status = "Waiting for hostile pass";
        else if (world != null && world.difficultySetting == 0) status = "Peaceful difficulty";
        else status = "Hostile spawning disabled by world";
        count = vanillaCap = effectiveCap = passActivity = -1;
        spawnedThisPass = increasePercent = 0;
    }

    private boolean aboveCap(Object context, int[] population) {
        if (!owns(context)) return population[0] > population[1];
        count = population[0];
        vanillaCap = population[1];
        effectiveCap = vanillaCap;
        status = "Vanilla spawning";
        if (world.difficultySetting == 0) {
            status = "Peaceful difficulty";
        } else if (maxIncreasePercent == 0) {
            status = "Territorial boost disabled";
        } else if (world.worldProvider.worldType != 0 || world.playerEntities.size() != 1) {
            status = "Requires a single player in the Overworld";
        } else {
            ThreatAssessment assessment = territory.assess(world, (EntityPlayer) world.playerEntities.get(0));
            if (assessment == null) {
                status = "Survival activity unavailable";
            } else {
                passActivity = assessment.activity;
                increasePercent = Math.min(passActivity, SATURATION_ACTIVITY) * maxIncreasePercent / SATURATION_ACTIVITY;
                effectiveCap += vanillaCap * increasePercent / 100;
                status = boosted() ? "Territorial boost active" : "Territorial activity too low";
            }
        }
        boolean capped = boosted() ? count >= effectiveCap : count > vanillaCap;
        if (capped) limitPass();
        return capped;
    }

    private boolean boosted() { return effectiveCap > vanillaCap; }

    private boolean hasRoom(Object context) {
        return !owns(context) || !boosted() || count + spawnedThisPass < effectiveCap;
    }

    private void recordSpawn(Object context) {
        if (!owns(context)) return;
        successfulSpawns++;
        spawnedThisPass++;
        if (boosted() && count + spawnedThisPass == effectiveCap) limitPass();
    }

    private void limitPass() {
        limitedPasses++;
        status = boosted() ? "Territorial cap reached" : "Vanilla cap exceeded";
    }

    private boolean hookActive() {
        if (agentActive == null) return false;
        try {
            return (Boolean) agentActive.invoke(null, World.class.getClassLoader());
        } catch (ReflectiveOperationException failure) {
            return false;
        }
    }

    public Snapshot snapshot(World context) {
        return owns(context) ? new Snapshot(this) : null;
    }

    public static final class Snapshot {
        public final boolean hookActive;
        public final String status;
        public final int count, vanillaCap, effectiveCap, increasePercent, maxIncreasePercent, passActivity;
        public final long successfulSpawns, limitedPasses;

        private Snapshot(NaturalSpawnPressure pressure) {
            hookActive = pressure.hookActive();
            status = hookActive ? pressure.status : "Tekkit spawning hook unavailable";
            count = pressure.count;
            vanillaCap = pressure.vanillaCap;
            effectiveCap = pressure.effectiveCap;
            increasePercent = pressure.increasePercent;
            maxIncreasePercent = pressure.maxIncreasePercent;
            passActivity = pressure.passActivity;
            successfulSpawns = pressure.successfulSpawns;
            limitedPasses = pressure.limitedPasses;
        }
    }
}
