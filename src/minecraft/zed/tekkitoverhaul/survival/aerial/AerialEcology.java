package zed.tekkitoverhaul.survival.aerial;

import java.util.Random;
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
    private boolean siteSiege;
    private double lunarX, lunarZ;
    private String lunarStatus = "No lunar request";
    private AerialExposure exposure = new AerialExposure();
    private int ticks, nextCheck, activity, chance, loadedPredators, localHostiles, placementTries;
    private int admissions, ordinaryAttempts, ordinaryAdmissions, requestedMembers, insertedMembers;
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
        ordinaryAttempts = ordinaryAdmissions = requestedMembers = insertedMembers = 0;
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
        Random moon = new Random(world.getSeed() ^ (logicalDay * 341873128712L));
        int roll = moon.nextInt(100);
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
        boolean airborne = !player.onGround && exposure.height >= 8;
        boolean skeletal = moon.nextInt(100) < (world.difficultySetting == 3 ? 72 : 50);
        for (int size = desired; size >= 1; --size) {
            if (!place(player, size, airborne, true, skeletal, false)) continue;
            lunar = true;
            lunarX = player.posX;
            lunarZ = player.posZ;
            state.reserve(cooldownSeconds);
            ++admissions;
            lunarStatus = "Admitted " + encounter.size() + (skeletal ? " lunar Skelebirds" : " lunar Fugls");
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

    public boolean siteSiegeAvailable() { return state != null; }

    /** Attempts one already-reserved slot, from a verified exterior column of the selected site. */
    public EntitySkelebird admitSiteSiege(World context, EntityPlayer player, int site,
            int x, int y, int z, int goalX, int goalZ) {
        if (context != world || state == null || encounter != null || state.cooldownSeconds > 0
                || !eligible(player) || JetpackWeather.LOCAL.protects(player)) return null;
        AerialExposure sample = new AerialExposure();
        sample.sample(player);
        if (sample.band == AerialExposure.Band.COVERED) return null;
        NaturalSpawnPressure.Snapshot nativePass = naturalSpawning.snapshot(world);
        if (nativePass != null && nativePass.hookActive && !nativePass.hostileEnabled) return null;
        if (populationRoom(player, ordinaryCap(nativePass)) == 0) return null;

        double dx = goalX - x, dz = goalZ - z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 5) return null;
        double sx = x + 0.5, sz = z + 0.5;
        if (!AerialTerrain.loaded(world, sx, sz, goalX + 0.5, goalZ + 0.5)) return null;
        double altitude = Math.max(y + 9, world.getPrecipitationHeight(x, z) + 5);
        EntitySkelebird bird = new EntitySkelebird(world);
        bird.setLocationAndAngles(sx, altitude, sz, (float) Math.toDegrees(Math.atan2(-dx, dz)), 0);
        double dy = altitude - player.posY;
        if (Math.abs(dy) > 22 || bird.getDistanceSqToEntity(player) < 12 * 12
                || bird.getDistanceSqToEntity(player) > 36 * 36 || !siteClear(bird, -dx / length, -dz / length))
            return null;
        bird.markSiegeOrigin(site);
        if (!world.spawnEntityInWorld(bird)) return null;
        encounter = new AerialEncounter(player, !player.onGround && sample.height >= 8, true, false,
                goalX + 0.5, goalZ + 0.5);
        encounter.add(bird, 0);
        siteSiege = true;
        state.reserve(cooldownSeconds);
        ++admissions;
        return bird;
    }

    public boolean controlsSiteBird(EntitySkelebird bird) {
        return siteSiege && encounter != null && bird.belongsTo(encounter);
    }

    public void endSiteSiege(EntitySkelebird bird, String reason) {
        if (controlsSiteBird(bird)) finish(reason);
    }

    private int ordinaryCap(NaturalSpawnPressure.Snapshot nativePass) {
        return nativePass != null && nativePass.hookActive && nativePass.effectiveCap > 0
                ? nativePass.effectiveCap : EnumCreatureType.monster.getMaxNumberOfCreature() * 289 / 256;
    }

    private void admit(EntityPlayer player) {
        NaturalSpawnPressure.Snapshot nativePass = naturalSpawning.snapshot(world);
        ++ordinaryAttempts;
        placementTries = requestedMembers = insertedMembers = chance = 0;
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
            chance = (day ? 14 : 57) + difficulty * (day ? 2 : 4) + activity / (day ? 8 : 3);
        } else {
            int base = 115 + (int) (exposure.exposure * 2) + difficulty * 24 + activity * 4;
            chance = exposure.band == AerialExposure.Band.LOW_AIR ? (base * 6 + 2) / 5 : (base * 5 + 2) / 4;
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

        boolean skeletal = selectSkelebird(ground, difficulty);
        int groupSize = ground ? 1 : flockSize(difficulty);
        if (skeletal) groupSize = Math.min(groupSize, difficulty == 3 && activity >= 12 ? 3 : 1);
        int desired = Math.min(room, groupSize);
        requestedMembers = desired;
        boolean hunting = desired > 1 || world.rand.nextInt(1000) < (ground ? (day ? 460 : 670) : 730);
        for (int size = desired; size >= (desired > 1 ? desired - 1 : 1); --size) {
            if (place(player, size, !ground, hunting, skeletal, true)) {
                state.reserve(cooldownSeconds);
                ++admissions;
                ++ordinaryAdmissions;
                insertedMembers = encounter.size();
                lastAttempt = "Admitted " + insertedMembers + (skeletal ? " Skelebird" : " Fugl")
                        + (hunting ? " (hunting)" : " (passing)");
                status = hunting ? "Active hunt" : "Wildlife passing through";
                return;
            }
        }
        reject("No clear loaded approach");
    }

    private void reject(String reason) { lastAttempt = status = reason; }

    private boolean selectSkelebird(boolean ground, int difficulty) {
        if (!EntitySkelebird.isNight(world)) return false;
        if (ground && (difficulty < 2 || activity < 12)) return false;
        int share = ground ? (difficulty == 3 ? 16 : 8)
                : difficulty == 1 ? 8 + activity / 4 : (difficulty == 3 ? 39 : 28) + activity / 2;
        return world.rand.nextInt(100) < share;
    }

    private int flockSize(int difficulty) {
        boolean high = exposure.band == AerialExposure.Band.HIGH_SKY;
        if (!high && exposure.band != AerialExposure.Band.EXPOSED) return 1;
        if (difficulty == 1) return high && world.rand.nextInt(1000) < (420 + activity * 7) / 3 ? 2 : 1;
        int groupChance = (high ? 420 : 270) + difficulty * 90 + activity * 8;
        if (world.rand.nextInt(1000) >= groupChance) return 1;
        if (difficulty == 2) return world.rand.nextInt(1000) < (high ? 640 : 520) + activity * 8 ? 3 : 2;
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

    private boolean place(EntityPlayer player, int size, boolean airborne, boolean hunting, boolean skeletal,
                          boolean ordinary) {
        EntityAerialPredator[] candidates = new EntityAerialPredator[size];
        for (int lane = 0; lane < size; ++lane)
            candidates[lane] = skeletal ? new EntitySkelebird(world) : new EntityAerialPredator(world);
        for (int attempt = 0; attempt < PLACEMENT_TRIES; ++attempt) {
            if (ordinary) ++placementTries;
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

            AerialEncounter incoming = new AerialEncounter(player, airborne, hunting, ordinary,
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
        siteSiege = false;
        status = "Encounter cooldown";
        lastEnd = reason;
    }

    public Snapshot snapshot() { return new Snapshot(this); }

    private String nextOrdinaryCheck() {
        if (!enabled) return "Ordinary admission disabled";
        if (state == null || !eligible(observer)) return status;
        if (encounter != null) return "Ordinary checks resume after encounter and cooldown";
        if (JetpackWeather.LOCAL.protects(observer)) return "Ordinary checks wait for jetpack recovery";
        int wait = Math.max(nextCheck, state.cooldownSeconds);
        if (exposure.band == AerialExposure.Band.GROUND) wait = Math.max(wait, 30 - exposure.groundSeconds);
        else if (!exposure.ready()) return "Ordinary checks wait for open exposure";
        return "Next ordinary check " + (wait == 0 ? "on next sample" : "in " + wait + "s");
    }

    public static final class Snapshot {
        public final double height, exposure;
        public final String band, status, lastAttempt, lastEnd, membership, lunarStatus, preyStatus;
        public final int cooldownSeconds, checkSeconds, admissions, activity, chance, loadedPredators, localHostiles;
        public final int placementTries, activeMembers, attackerId, gapTicks;
        public final int ordinaryAttempts, ordinaryAdmissions, requestedMembers, insertedMembers;
        public final String exposureRequirement, ordinarySchedule, encounterSource;

        private Snapshot(AerialEcology ecology) {
            AerialEncounter active = ecology.encounter;
            height = ecology.exposure.height;
            exposure = ecology.exposure.exposure;
            band = ecology.exposure.band.name();
            status = ecology.status;
            exposureRequirement = ecology.exposure.requirement();
            ordinarySchedule = ecology.nextOrdinaryCheck();
            encounterSource = active == null ? "No active encounter" : active.ordinary ? "Ordinary" : "Lunar";
            lunarStatus = ecology.lunarStatus;
            lastAttempt = ecology.lastAttempt;
            lastEnd = ecology.lastEnd;
            cooldownSeconds = ecology.state == null ? 0 : ecology.state.cooldownSeconds;
            checkSeconds = ecology.nextCheck;
            admissions = ecology.admissions;
            ordinaryAttempts = ecology.ordinaryAttempts;
            ordinaryAdmissions = ecology.ordinaryAdmissions;
            requestedMembers = ecology.requestedMembers;
            insertedMembers = ecology.insertedMembers;
            activity = ecology.activity;
            chance = ecology.chance;
            loadedPredators = ecology.loadedPredators;
            localHostiles = ecology.localHostiles;
            placementTries = ecology.placementTries;
            activeMembers = active == null ? 0 : active.size();
            membership = active == null ? "" : active.membership();
            preyStatus = active == null ? "No active Fugl prey target" : active.preyStatus();
            attackerId = active == null ? -1 : active.attackerId();
            gapTicks = active == null ? 0 : active.gapTicks();
        }
    }
}
