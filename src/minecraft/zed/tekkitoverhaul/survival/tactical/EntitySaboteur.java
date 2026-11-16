package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.PathEntity;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import zed.tekkitoverhaul.hopper.HopperRegistry;
import zed.tekkitoverhaul.hopper.TileHopper;

/** Small Enderkin available through its spawn egg. */
public final class EntitySaboteur extends TacticalHostile {
    public static final byte IDLE = 0, APPROACH = 1, WINDUP = 2, RECOVER = 3, CANCELLED = 4;
    private static final int POSE = 20;
    private static final float SPEED = 0.34F;
    private static final int ROUTE_INTERVAL = 20;
    private static final int BLINK_INTERVAL = 48;
    private static final int SCAN_INTERVAL = 24;
    private static final int WINDUP_TICKS = 25;
    private static final int RECOVERY_TICKS = 18;
    private static final int ACTION_COOLDOWN = 120;
    private static final double CONTACT_SQ = 5.76D;
    private static final double[] BLINK_ANGLES = {0, Math.PI / 5, -Math.PI / 5};
    private static final int[] SIDE_X = {1, -1, 0, 0};
    private static final int[] SIDE_Z = {0, 0, 1, -1};
    private int nextRoute, nextBlink, nextMelee;
    private int nextScan, windupEnd, recoverEnd, nextAction, approachEnd, nextSide;
    private int stalledRoutes;
    private double progressX, progressZ;
    private int pathPlans, blinkAttempts, blinkChecks, blinks;
    private int scans, scannedBlocks, windups, cancelled, disruptions, diversions;
    private TileHopper hopper;
    private int hopperX, hopperY, hopperZ;
    private long heartbeat = Long.MIN_VALUE;
    private enum Outcome { NONE, NO_HOPPER, OUT_OF_REACH, NO_SIGHT, TARGET_CHANGED, ALREADY_PAUSED,
        HIT, PLAYER_LOST, APPLIED, DECOY }
    private Outcome outcome = Outcome.NONE;
    private SaboteurBlink.Result blinkResult = SaboteurBlink.Result.NOT_GROUNDED;

    public EntitySaboteur(World world) {
        super(world, 0.45F, 1.1F, "saboteur");
        attackStrength = 1;
        nextScan = 1 + rand.nextInt(SCAN_INTERVAL);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(POSE, Byte.valueOf(IDLE));
    }

    public byte pose() { return dataWatcher.getWatchableObjectByte(POSE); }

    private void pose(byte value) { dataWatcher.updateObject(POSE, Byte.valueOf(value)); }

    public int blinkCooldown() { return Math.max(0, nextBlink - ticksExisted); }
    public int actionCooldown() { return Math.max(0, nextAction - ticksExisted); }
    public int windupRemaining() { return pose() == WINDUP ? Math.max(0, windupEnd - ticksExisted) : 0; }
    public TileHopper disruptionTarget() { return hopper; }
    public String lastOutcome() { return outcome.name(); }
    public String lastBlinkResult() { return blinkResult.name(); }
    public int scans() { return scans; }
    public int scannedBlocks() { return scannedBlocks; }
    public int pathPlans() { return pathPlans; }
    public int blinkAttempts() { return blinkAttempts; }
    public int blinkChecks() { return blinkChecks; }
    public int blinks() { return blinks; }
    public int windups() { return windups; }
    public int cancelled() { return cancelled; }
    public int disruptions() { return disruptions; }
    public int diversions() { return diversions; }

    @Override
    public int getMaxHealth() { return 12; }

    @Override
    protected int unseenLimit() { return 60; }

    @Override
    protected void fight(double distanceSq, boolean visible) {
        heartbeat = worldObj.getWorldTime();
        if (pose() == WINDUP) {
            continueWindup(visible);
            return;
        }
        if (ticksExisted < recoverEnd) return;
        pose(APPROACH);

        if (ticksExisted >= nextAction) {
            Outcome rejection = approachRejection();
            if (rejection != Outcome.NONE) {
                outcome = rejection;
                hopper = null;
                nextScan = Math.max(nextScan, ticksExisted + SCAN_INTERVAL);
                getNavigator().clearPathEntity();
            }
            if (hopper == null && ticksExisted >= nextScan) scan();
            if (hopper != null) {
                approachHopper();
                return;
            }
            if (scans > 0 && visible && distanceSq > 2.25D && distanceSq <= 12.25D && onGround) {
                startWindup();
                return;
            }
        }

        if (defend(distanceSq, visible)) return;
        pursue(target.posX, target.boundingBox.minY, target.posZ, visible);
    }

    private Outcome approachRejection() {
        if (hopper == null) return Outcome.NONE;
        if (!sameHopper()) return Outcome.TARGET_CHANGED;
        if (hopper.isDisrupted()) return Outcome.ALREADY_PAUSED;
        return ticksExisted >= approachEnd ? Outcome.OUT_OF_REACH : Outcome.NONE;
    }

    private void scan() {
        nextScan = ticksExisted + SCAN_INTERVAL;
        scans++;
        outcome = Outcome.NO_HOPPER;
        if (HopperRegistry.block == null) return;
        int baseX = MathHelper.floor_double(posX);
        int baseY = MathHelper.floor_double(boundingBox.minY);
        int baseZ = MathHelper.floor_double(posZ);
        int id = HopperRegistry.block.blockID;
        double best = 64.0D;
        for (int x = baseX - 8; x <= baseX + 8; x++) {
            double dx = x + 0.5D - posX;
            for (int z = baseZ - 8; z <= baseZ + 8; z++) {
                double dz = z + 0.5D - posZ;
                double horizontal = dx * dx + dz * dz;
                if (horizontal > best || !worldObj.blockExists(x, baseY, z)) continue;
                for (int y = Math.max(0, baseY - 2); y <= Math.min(worldObj.getHeight() - 1, baseY + 2); y++) {
                    scannedBlocks++;
                    double distance = horizontal + (y + 0.5D - posY) * (y + 0.5D - posY);
                    if (distance > best || worldObj.getBlockId(x, y, z) != id) continue;
                    TileHopper candidate = TileHopper.loadedAt(worldObj, x, y, z);
                    if (candidate == null) {
                        outcome = Outcome.TARGET_CHANGED;
                    } else if (candidate.isDisrupted()) {
                        outcome = Outcome.ALREADY_PAUSED;
                    } else if (!seesHopper(x, y, z)) {
                        outcome = Outcome.NO_SIGHT;
                    } else {
                        best = distance;
                        hopper = candidate;
                        hopperX = x;
                        hopperY = y;
                        hopperZ = z;
                    }
                }
            }
        }
        if (hopper == null) return;
        outcome = Outcome.NONE;
        approachEnd = ticksExisted + 100;
        double dx = posX - hopperX - 0.5D;
        double dz = posZ - hopperZ - 0.5D;
        nextSide = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? 0 : 1) : (dz > 0 ? 2 : 3);
        getNavigator().clearPathEntity();
    }

    private void approachHopper() {
        lookAtHopper();
        if (inContact() && seesHopper(hopperX, hopperY, hopperZ)) {
            startWindup();
            return;
        }
        if (tryBlink(hopperX + 0.5D, hopperZ + 0.5D)) return;
        if (ticksExisted < nextRoute || !onGround) return;
        nextRoute = ticksExisted + ROUTE_INTERVAL;
        double x = hopperX + 0.5D + SIDE_X[nextSide];
        double z = hopperZ + 0.5D + SIDE_Z[nextSide];
        nextSide = (nextSide + 1) % SIDE_X.length;
        pathPlans++;
        PathEntity path = plan(x, hopperY, z);
        if (reaches(path, x, hopperY, z)) getNavigator().setPath(path, SPEED);
    }

    private boolean inContact() {
        return onGround && Math.abs(boundingBox.minY - hopperY) <= 1.25D
                && getDistanceSq(hopperX + 0.5D, hopperY + 0.5D, hopperZ + 0.5D) <= CONTACT_SQ;
    }

    private boolean sameHopper() {
        return hopper != null && hopper.xCoord == hopperX && hopper.yCoord == hopperY && hopper.zCoord == hopperZ
                && TileHopper.loadedAt(worldObj, hopperX, hopperY, hopperZ) == hopper;
    }

    private boolean seesHopper(int x, int y, int z) {
        if (!worldObj.checkChunksExist(MathHelper.floor_double(Math.min(posX, x)) - 1, 0,
                MathHelper.floor_double(Math.min(posZ, z)) - 1,
                MathHelper.floor_double(Math.max(posX, x)) + 1, 0,
                MathHelper.floor_double(Math.max(posZ, z)) + 1)) return false;
        MovingObjectPosition hit = worldObj.rayTraceBlocks(eyes(), Vec3D.createVector(x + 0.5D, y + 0.65D, z + 0.5D));
        return hit == null || hit.blockX == x && hit.blockY == y && hit.blockZ == z;
    }

    private void lookAtHopper() {
        getLookHelper().setLookPosition(hopperX + 0.5D, hopperY + 0.65D, hopperZ + 0.5D, 30.0F, 30.0F);
    }

    private void startWindup() {
        pose(WINDUP);
        windupEnd = ticksExisted + WINDUP_TICKS;
        windups++;
        getNavigator().clearPathEntity();
        motionX = motionZ = 0;
        link();
    }

    private void continueWindup(boolean visible) {
        if (hopper == null) {
            if (!visible) finish(Outcome.NO_SIGHT, true);
            else if (ticksExisted >= windupEnd) {
                diversions++;
                finish(Outcome.DECOY, false);
            } else if ((windupEnd - ticksExisted) % 5 == 0) link();
            return;
        }

        if (!sameHopper()) finish(Outcome.TARGET_CHANGED, true);
        else if (!inContact()) finish(Outcome.OUT_OF_REACH, true);
        else if (!seesHopper(hopperX, hopperY, hopperZ)) finish(Outcome.NO_SIGHT, true);
        else if (hopper.isDisrupted()) finish(Outcome.ALREADY_PAUSED, true);
        else {
            lookAtHopper();
            if (ticksExisted >= windupEnd) {
                if (hopper.beginDisruption(this)) {
                    disruptions++;
                    burst(hopperX + 0.5D, hopperY + 0.85D, hopperZ + 0.5D, "portal");
                    finish(Outcome.APPLIED, false);
                } else finish(Outcome.TARGET_CHANGED, true);
            } else if ((windupEnd - ticksExisted) % 5 == 0) link();
        }
    }

    private void link() {
        double x = posX;
        double y = boundingBox.minY + 0.7D;
        double z = posZ;
        for (int i = 1; i <= 3; i++) {
            double t = i / 3.0D;
            worldObj.spawnParticle("reddust",
                    hopper == null ? x : x + (hopperX + 0.5D - x) * t,
                    hopper == null ? y + t * 0.3D : y + (hopperY + 0.7D - y) * t,
                    hopper == null ? z : z + (hopperZ + 0.5D - z) * t, 0.15D, 0.75D, 0.63D);
        }
    }

    private void finish(Outcome result, boolean interrupted) {
        hopper = null;
        outcome = result;
        nextAction = ticksExisted + ACTION_COOLDOWN;
        recoverEnd = ticksExisted + RECOVERY_TICKS;
        getNavigator().clearPathEntity();
        pose(interrupted ? CANCELLED : RECOVER);
        if (interrupted) cancelled++;
        burst(posX, boundingBox.minY + 0.7D, posZ, interrupted ? "smoke" : "portal");
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, int damage) {
        boolean hit = super.attackEntityFrom(source, damage);
        if (hit && damage > 0 && !worldObj.isRemote && pose() == WINDUP && source.getEntity() instanceof EntityPlayer)
            finish(Outcome.HIT, true);
        return hit;
    }

    /** The lease expires if either world object stops owning or ticking this encounter. */
    public boolean canMaintainDisruption(World world) {
        if (world == null || world != worldObj || world.isRemote || world.difficultySetting == 0
                || !addedToChunk || !isEntityAlive() || target == null || target.worldObj != world
                || !target.isEntityAlive() || target.capabilities.disableDamage || getDistanceSqToEntity(target) > 256)
            return false;
        long age = world.getWorldTime() - heartbeat;
        return age >= 0 && age <= 2 && world.getChunkProvider().chunkExists(chunkCoordX, chunkCoordZ);
    }

    public boolean canStartDisruption(TileHopper tile) {
        return pose() == WINDUP && hopper == tile && ticksExisted >= windupEnd;
    }

    private boolean defend(double distanceSq, boolean visible) {
        if (!visible || distanceSq > 1.69D || target.boundingBox.minY >= boundingBox.maxY
                || target.boundingBox.maxY <= boundingBox.minY) return false;
        getNavigator().clearPathEntity();
        stalledRoutes = 0;
        if (ticksExisted >= nextMelee) {
            nextMelee = ticksExisted + 34;
            swing();
            attackEntityAsMob(target);
        }
        return true;
    }

    private void pursue(double x, double y, double z, boolean allowBlink) {
        if (allowBlink && tryBlink(x, z)) return;
        if (ticksExisted < nextRoute || !onGround) return;
        nextRoute = ticksExisted + ROUTE_INTERVAL;
        double dx = posX - progressX;
        double dz = posZ - progressZ;
        progressX = posX;
        progressZ = posZ;
        stalledRoutes = dx * dx + dz * dz < 0.16D ? stalledRoutes + 1 : 0;
        if (stalledRoutes >= 5) {
            disengage();
            return;
        }
        pathPlans++;
        PathEntity path = plan(x, y, z);
        if (reaches(path, x, y, z)) getNavigator().setPath(path, SPEED);
    }

    private boolean tryBlink(double x, double z) {
        double dx = x - posX;
        double dz = z - posZ;
        double distanceSq = dx * dx + dz * dz;
        if (ticksExisted < nextBlink || !onGround || hurtTime > 0 || distanceSq < 16 || distanceSq > 64)
            return false;
        nextBlink = ticksExisted + BLINK_INTERVAL;
        blinkAttempts++;
        double direction = Math.atan2(dz, dx);
        for (double angle : BLINK_ANGLES) {
            double shiftX = Math.cos(direction + angle) * 2.75D;
            double shiftZ = Math.sin(direction + angle) * 2.75D;
            blinkChecks++;
            blinkResult = SaboteurBlink.check(this, shiftX, shiftZ);
            if (blinkResult != SaboteurBlink.Result.CLEAR) continue;
            burst(posX, posY + 0.45D, posZ, "portal");
            setPosition(posX + shiftX, posY, posZ + shiftZ);
            prevPosX = lastTickPosX = posX;
            prevPosZ = lastTickPosZ = posZ;
            motionX = motionZ = 0;
            getNavigator().clearPathEntity();
            burst(posX, posY + 0.45D, posZ, "portal");
            blinks++;
            return true;
        }
        return false;
    }

    private void burst(double x, double y, double z, String particle) {
        for (int i = 0; i < 4; i++) {
            worldObj.spawnParticle(particle, x, y, z,
                    (rand.nextFloat() - 0.5F) * 0.6D, rand.nextFloat() * 0.3D,
                    (rand.nextFloat() - 0.5F) * 0.6D);
        }
    }

    @Override
    protected void resetTactics() {
        stalledRoutes = 0;
        hopper = null;
        if (pose() == WINDUP) finish(Outcome.PLAYER_LOST, true);
        else if (ticksExisted >= recoverEnd) pose(IDLE);
    }

    @Override
    protected void updateAttackPose() {
        if (target == null && ticksExisted >= recoverEnd && pose() != IDLE) pose(IDLE);
    }
}
