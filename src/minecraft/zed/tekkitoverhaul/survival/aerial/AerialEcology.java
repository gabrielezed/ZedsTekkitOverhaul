package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EnumCreatureType;
import net.minecraft.src.IMob;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;
import zed.tekkitoverhaul.survival.weather.JetpackWeather;
import zed.tekkitoverhaul.territory.NaturalSpawnPressure;
import zed.tekkitoverhaul.territory.TerritorialActivityService;
import zed.tekkitoverhaul.territory.ThreatAssessment;

/** Sparse encounter admission for one local Overworld player. Predators count toward the ordinary hostile cap. */
public final class AerialEcology {
    private static final int SAMPLE_TICKS = 20;
    private static final int CHECK_INTERVAL = 15;
    private static final int PREDATOR_CAP = 5;
    private static final int LOCAL_HOSTILE_CAP = 14;
    // Preserve room for occasional aerial encounters when the vanilla pass overshoots its cap.
    private static final int HOSTILE_CAP_SLACK = 9;
    private static final int PLACEMENT_TRIES = 8;
    private final boolean enabled, lunarEnabled;
    private final int cooldownSeconds;
    private final TerritorialActivityService territory;
    private final NaturalSpawnPressure naturalSpawning;
    private World world;
    private EntityPlayer observer;
    private AerialEcologyState state;
    private AerialEncounter encounter;
    private boolean lunar;
    private double lunarX, lunarZ;
    private String lunarStatus = "No lunar request";
    private AerialExposure exposure = new AerialExposure();
    private int ticks, nextCheck, activity, chance, loadedPredators, localHostiles, placementTries;
    private int admissions;
    private String status = "No local Overworld", lastAttempt = "None", lastEnd = "None";

    public AerialEcology(boolean enabled, int cooldownSeconds, TerritorialActivityService territory,
            NaturalSpawnPressure naturalSpawning) {
        this(enabled, false, cooldownSeconds, territory, naturalSpawning);
    }

    public AerialEcology(boolean enabled, boolean lunarEnabled, int cooldownSeconds, TerritorialActivityService territory,
            NaturalSpawnPressure naturalSpawning) {
        this.enabled = enabled;
        this.lunarEnabled = lunarEnabled;
        this.cooldownSeconds = Math.max(90, Math.min(600, cooldownSeconds));
        this.territory = territory;
        this.naturalSpawning = naturalSpawning;
    }

    public void bind(World active) {
        if (active == world) return;
        if (encounter != null) finish("World changed");
        world = active;
        state = (enabled || lunarEnabled) && active != null && !active.isRemote && active.worldProvider.worldType == 0
                ? AerialEcologyState.load(active) : null;
        observer = null;
        exposure = new AerialExposure();
        ticks = nextCheck = activity = chance = loadedPredators = localHostiles = placementTries = admissions = 0;
        status = !enabled ? "Disabled in settings" : state == null ? "Outside local Overworld" : "Waiting for sample";
        lastAttempt = lastEnd = "None";
        lunarStatus = "No lunar request";
    }

    public void tick(EntityPlayer player, boolean paused) {
        if (paused || state == null) return;
        if (encounter != null) encounter.tick();
        if (++ticks < SAMPLE_TICKS) return;
        ticks = 0;
        if (observer != player) {
            observer = player;
            exposure = new AerialExposure();
        }
        if (!eligible(player)) {
            if (encounter != null) finish("Player unavailable or Peaceful");
            exposure = new AerialExposure();
            status = world.difficultySetting == 0 ? "Peaceful difficulty" : "Requires a local Survival player";
            return;
        }
        exposure.sample(player);
        if (encounter != null) {
            double dx = player.posX - lunarX, dz = player.posZ - lunarZ;
            String end = lunar && dx * dx + dz * dz > 48 * 48 ? "Left lunar airspace"
                    : encounter.sample(world, player, exposure);
            if (end.isEmpty()) status = encounter.hunting ? "Active hunt" : "Wildlife passing through";
            else finish(end);
            return;
        }
        state.elapseSecond();
        if (nextCheck > 0) --nextCheck;
        if (!enabled) status = "Natural admission disabled";
        else if (JetpackWeather.LOCAL.protects(player)) status = "Jetpack fault or recovery";
        else if (state.cooldownSeconds > 0) status = "Encounter cooldown";
        else if (!exposure.ready()) status = exposure.restriction;
        else if (nextCheck > 0) status = "Waiting for admission check";
        else {
            nextCheck = CHECK_INTERVAL;
            admit(player);
        }
    }

    private boolean eligible(EntityPlayer player) {
        return world.difficultySetting > 0 && player != null && player.worldObj == world && player.isEntityAlive()
                && !player.capabilities.disableDamage && !player.capabilities.isCreativeMode && !player.isPlayerSleeping()
                && world.playerEntities.size() == 1 && world.playerEntities.get(0) == player;
    }

    /** Called once at a consumed, natural full-moon assault admission. Returns spent assault slots. */
    public int admitLunar(World context, EntityPlayer player, int budget, int activeCap,
            long logicalDay) {
        if (!lunarEnabled) return lunarRejected("Disabled in settings");
        if (context != world || state == null || !eligible(player) || world.difficultySetting < 2)
            return lunarRejected("Requires a Normal or Hard Overworld assault");
        if (encounter != null || state.cooldownSeconds > 0 || JetpackWeather.LOCAL.protects(player))
            return lunarRejected("Aerial encounter, cooldown or jetpack protection");
        if (budget < 6 || activeCap < 3) return lunarRejected("Ground allocation too small");

        ThreatAssessment threat = territory.assess(world, player);
        int pressure = threat == null ? 0 : threat.activity;
        // Stable selection avoids modifying the ground spawner's random sequence on ineligible moons.
        int chance = world.difficultySetting == 3 ? 58 : 34;
        long roll = new java.util.Random(world.getSeed() ^ (logicalDay * 341873128712L)).nextInt(100);
        if (roll >= chance + Math.min(20, pressure)) return lunarRejected("Full moon not selected");

        exposure.sample(player);
        if (exposure.band == AerialExposure.Band.COVERED) return lunarRejected(exposure.restriction);
        NaturalSpawnPressure.Snapshot nativePass = naturalSpawning.snapshot(world);
        if (nativePass != null && nativePass.hookActive && !nativePass.hostileEnabled)
            return lunarRejected("Hostile spawning disabled by world");
        int capacity = populationRoom(player, ordinaryCap(nativePass));
        int desired = world.difficultySetting == 3 && pressure >= 12 && budget >= 10 && activeCap >= 5 ? 2 : 1;
        desired = Math.min(desired, capacity);
        if (desired == 0) return lunarRejected("Aerial or ordinary hostile cap");
        placementTries = 0;
        boolean airborne = !player.onGround && exposure.height >= 8;
        for (int size = desired; size >= 1; --size) {
            if (!place(player, size, airborne, true)) continue;
            lunar = true;
            lunarX = player.posX;
            lunarZ = player.posZ;
            state.reserve(cooldownSeconds);
            ++admissions;
            lunarStatus = "Admitted " + encounter.size() + " lunar predators";
            return encounter.size();
        }
        return lunarRejected("No clear lunar airspace");
    }

    private int lunarRejected(String reason) {
        lunarStatus = reason;
        return 0;
    }

    public int lunarMembers() { return lunar && encounter != null ? encounter.size() : 0; }

    public void endLunar(String reason) {
        if (lunar && encounter != null) finish(reason);
    }

    public String lunarStatus() { return lunarStatus; }

    private int ordinaryCap(NaturalSpawnPressure.Snapshot nativePass) {
        return nativePass != null && nativePass.hookActive && nativePass.effectiveCap > 0
                ? nativePass.effectiveCap : EnumCreatureType.monster.getMaxNumberOfCreature() * 289 / 256;
    }

    private void admit(EntityPlayer player) {
        NaturalSpawnPressure.Snapshot nativePass = naturalSpawning.snapshot(world);
        placementTries = 0;
        if (nativePass != null && nativePass.hookActive && !nativePass.hostileEnabled) {
            reject("Hostile spawning disabled by world");
            return;
        }

        ThreatAssessment threat = territory.assess(world, player);
        activity = threat == null ? 0 : Math.min(24, threat.activity);
        boolean ground = exposure.band == AerialExposure.Band.GROUND;
        boolean day = world.isDaytime();
        int difficulty = world.difficultySetting;
        if (ground) {
            chance = (day ? 9 : 37) + difficulty * (day ? 2 : 4) + activity / (day ? 8 : 3);
        } else {
            chance = 115 + (int) (exposure.exposure * 2) + difficulty * 24 + activity * 4;
        }
        if (world.rand.nextInt(1000) >= chance) {
            reject("Admission roll missed");
            return;
        }
        int room = populationRoom(player, ordinaryCap(nativePass));
        if (room == 0) {
            reject("Aerial or ordinary hostile cap");
            return;
        }

        int desired = Math.min(room, ground ? 1 : flockSize(difficulty));
        boolean hunting = desired > 1 || world.rand.nextInt(1000) < (ground ? (day ? 460 : 670) : 730);
        for (int size = desired; size >= (desired > 1 ? desired - 1 : 1); --size) {
            if (place(player, size, !ground, hunting)) {
                state.reserve(cooldownSeconds);
                ++admissions;
                lastAttempt = "Admitted " + encounter.size() + (hunting ? " hunting" : " cruising");
                status = hunting ? "Active hunt" : "Wildlife passing through";
                return;
            }
        }
        reject("No clear loaded approach");
    }

    private void reject(String reason) { lastAttempt = status = reason; }

    private int flockSize(int difficulty) {
        boolean high = exposure.band == AerialExposure.Band.HIGH_SKY;
        if (!high && exposure.band != AerialExposure.Band.EXPOSED) return 1;
        int groupChance = (high ? 340 : 180) + difficulty * 80 + activity * 7;
        if (difficulty == 1) groupChance /= 3;
        if (world.rand.nextInt(1000) >= groupChance) return 1;
        if (difficulty == 1) return high ? 2 : 1;
        if (difficulty == 2) return high || activity >= 16 ? 3 : 2;
        if (!high) return activity >= 16 ? 4 : 3;
        return activity >= 16 ? 5 : 4;
    }

    private int populationRoom(EntityPlayer player, int hostileCap) {
        int hostiles = 0;
        loadedPredators = localHostiles = 0;
        for (Object entry : world.loadedEntityList) {
            Entity mob = (Entity) entry;
            if (mob.isDead || !(mob instanceof IMob)) continue;
            ++hostiles;
            if (mob instanceof EntityAerialPredator) ++loadedPredators;
            if (mob.getDistanceSqToEntity(player) < 36 * 36) ++localHostiles;
        }
        return Math.max(0, Math.min(PREDATOR_CAP - loadedPredators,
                Math.min(hostileCap + HOSTILE_CAP_SLACK - hostiles, LOCAL_HOSTILE_CAP - localHostiles)));
    }

    private boolean place(EntityPlayer player, int size, boolean airborne, boolean hunting) {
        EntityAerialPredator[] candidates = new EntityAerialPredator[size];
        for (int lane = 0; lane < size; ++lane) candidates[lane] = new EntityAerialPredator(world);
        for (int attempt = 0; attempt < PLACEMENT_TRIES; ++attempt) {
            ++placementTries;
            double angle = Math.toRadians(player.rotationYaw + 180 + world.rand.nextInt(121) - 60);
            double xDir = -Math.sin(angle), zDir = Math.cos(angle);
            double distance = 25 + world.rand.nextInt(9);
            double baseY = Math.min(world.getHeight() - 12, player.boundingBox.minY + 9 + world.rand.nextInt(6));
            boolean clear = true;
            for (int lane = 0; lane < size && clear; ++lane) {
                double lateral = (lane - (size - 1) * 0.5) * 3.5;
                double x = player.posX + xDir * distance + zDir * lateral;
                double z = player.posZ + zDir * distance - xDir * lateral;
                double y = baseY + lane % 3 * 1.8;
                EntityAerialPredator mob = candidates[lane];
                mob.setLocationAndAngles(x, y, z, (float) Math.toDegrees(angle + Math.PI), 0);
                clear = siteClear(mob, xDir, zDir);
            }
            if (!clear) continue;

            AerialEncounter incoming = new AerialEncounter(player, airborne, hunting,
                    player.posX + xDir * 12, player.posZ + zDir * 12);
            for (int lane = 0; lane < size; ++lane) {
                EntityAerialPredator mob = candidates[lane];
                if (world.spawnEntityInWorld(mob)) incoming.add(mob, lane);
            }
            if (incoming.size() == 0) continue;
            encounter = incoming;
            loadedPredators += incoming.size();
            return true;
        }
        return false;
    }

    private boolean siteClear(EntityAerialPredator mob, double xDir, double zDir) {
        AxisAlignedBB room = mob.boundingBox.expand(0.35, 0.45, 0.35);
        int x = MathHelper.floor_double(mob.posX), y = MathHelper.floor_double(mob.posY);
        int z = MathHelper.floor_double(mob.posZ);
        return AerialTerrain.clear(mob, room.addCoord(-xDir * 5, 0, -zDir * 5))
                && world.getPrecipitationHeight(x, z) <= y - 4
                && world.canBlockSeeTheSky(x, y, z)
                && world.getEntitiesWithinAABB(EntityLiving.class, room).isEmpty();
    }

    private void finish(String reason) {
        encounter.finish(reason);
        encounter = null;
        if (lunar) lunarStatus = "Ended: " + reason;
        lunar = false;
        status = "Encounter cooldown";
        lastEnd = reason;
    }

    public Snapshot snapshot() { return new Snapshot(this); }

    public static final class Snapshot {
        public final double height, exposure;
        public final String band, status, lastAttempt, lastEnd, membership, lunarStatus;
        public final int cooldownSeconds, checkSeconds, admissions, activity, chance, loadedPredators, localHostiles;
        public final int placementTries, activeMembers, attackerId, gapTicks;

        private Snapshot(AerialEcology ecology) {
            AerialEncounter active = ecology.encounter;
            height = ecology.exposure.height;
            exposure = ecology.exposure.exposure;
            band = ecology.exposure.band.name();
            status = ecology.status;
            lunarStatus = ecology.lunarStatus;
            lastAttempt = ecology.lastAttempt;
            lastEnd = ecology.lastEnd;
            cooldownSeconds = ecology.state == null ? 0 : ecology.state.cooldownSeconds;
            checkSeconds = ecology.nextCheck;
            admissions = ecology.admissions;
            activity = ecology.activity;
            chance = ecology.chance;
            loadedPredators = ecology.loadedPredators;
            localHostiles = ecology.localHostiles;
            placementTries = ecology.placementTries;
            activeMembers = active == null ? 0 : active.size();
            membership = active == null ? "" : active.membership();
            attackerId = active == null ? -1 : active.attackerId();
            gapTicks = active == null ? 0 : active.gapTicks();
        }
    }
}
