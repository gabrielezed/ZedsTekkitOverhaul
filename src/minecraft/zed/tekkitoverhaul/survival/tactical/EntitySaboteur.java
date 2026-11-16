package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.PathEntity;
import net.minecraft.src.TileEntity;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import zed.tekkitoverhaul.distillation.DistillationPartType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.distillation.TileEntityDistillationReceiver;
import zed.tekkitoverhaul.hopper.HopperRegistry;
import zed.tekkitoverhaul.hopper.TileHopper;
import zed.tekkitoverhaul.integration.LoadedTile;

/** Small Enderkin available through its spawn egg. */
public final class EntitySaboteur extends TacticalHostile {
    public static final byte IDLE = 0, APPROACH = 1, WINDUP = 2, RECOVER = 3, CANCELLED = 4,
            EVADE = 5, DECOY = 6;
    private static final int POSE = 20;
    private static final float SPEED = 0.34F;
    private static final int ROUTE_INTERVAL = 20;
    private static final int APPROACH_BLINK_COOLDOWN = 32;
    private static final int EVASIVE_BLINK_COOLDOWN = 38;
    private static final int BLINK_RETRY = 8;
    private static final int SCAN_INTERVAL = 24;
    private static final int WINDUP_TICKS = 25;
    private static final int RECOVERY_TICKS = 18;
    private static final int ACTION_COOLDOWN = 120;
    private static final double CONTACT_SQ = 5.76D;
    private static final double[] BLINK_ANGLES = {0, Math.PI / 5, -Math.PI / 5};
    private static final double[] ESCAPE_ANGLES = {
        0, Math.PI / 3, -Math.PI / 3, Math.PI / 2, -Math.PI / 2, 0
    };
    private static final int[] SIDE_X = {1, -1, 0, 0};
    private static final int[] SIDE_Z = {0, 0, 1, -1};
    private int nextRoute, nextBlink, retryBlink, nextMelee, evadeEnd, hitEvadeEnd, closeTicks;
    private int evasiveAttempts, evasiveBlinks, failedBlinks, rejectedDestinations;
    private enum BlinkCause { NONE, APPROACH, HIT, CLOSE, INTERRUPTED }
    private BlinkCause blinkCause = BlinkCause.NONE, pendingEvade = BlinkCause.NONE;
    private int nextScan, windupEnd, recoverEnd, nextAction, approachEnd, nextSide;
    private int stalledRoutes;
    private double progressX, progressZ;
    private int pathPlans, blinkAttempts, blinkChecks, blinks;
    private int scans, scannedBlocks, windups, cancelled, disruptions, diversions;
    private TileEntity machine;
    private TileEntity connection;
    private enum TargetType { NONE, HOPPER, BUILDCRAFT_RECEIVER }
    private TargetType targetType = TargetType.NONE;
    private int machineX, machineY, machineZ;
    private long heartbeat = Long.MIN_VALUE;
    private enum Outcome { NONE, NO_SUPPORTED_TARGET, UNSUPPORTED_CONNECTION, OUT_OF_REACH, NO_SIGHT, TARGET_CHANGED, ALREADY_PAUSED,
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
    public int blinkBackoff() { return Math.max(0, retryBlink - ticksExisted); }
    public int windupRemaining() { return windingUp() ? Math.max(0, windupEnd - ticksExisted) : 0; }
    public TileEntity disruptionTarget() { return machine; }
    public TileEntity disruptionConnection() { return connection; }
    public String targetType() { return targetType.name(); }
    public String lastOutcome() { return outcome.name(); }
    public String lastBlinkResult() { return blinkResult.name(); }
    public String lastBlinkCause() { return blinkCause.name(); }
    public int evasiveAttempts() { return evasiveAttempts; }
    public int evasiveBlinks() { return evasiveBlinks; }
    public int failedBlinks() { return failedBlinks; }
    public int rejectedDestinations() { return rejectedDestinations; }
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
        if (windingUp()) {
            continueWindup(visible);
            return;
        }
        if (ticksExisted < evadeEnd) return;
        closeTicks = visible && distanceSq <= 6.25D ? Math.min(8, closeTicks + 1) : 0;
        BlinkCause escape = ticksExisted < hitEvadeEnd ? pendingEvade
                : closeTicks >= 8 && ticksExisted >= recoverEnd ? BlinkCause.CLOSE : BlinkCause.NONE;
        if (escape != BlinkCause.NONE && (escape != BlinkCause.INTERRUPTED || ticksExisted >= recoverEnd - 12)
                && blink(target.posX, target.posZ, escape)) {
            hitEvadeEnd = closeTicks = 0;
            evadeEnd = ticksExisted + 6;
            pose(EVADE);
            return;
        }
        if (ticksExisted < recoverEnd) return;
        pose(APPROACH);

        if (ticksExisted >= nextAction) {
            Outcome rejection = approachRejection();
            if (rejection != Outcome.NONE) {
                outcome = rejection;
                machine = connection = null;
                nextScan = Math.max(nextScan, ticksExisted + SCAN_INTERVAL);
                getNavigator().clearPathEntity();
            }
            if (machine == null && ticksExisted >= nextScan) scan();
            if (machine != null) {
                approachMachine();
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
        if (machine == null) return Outcome.NONE;
        if (!sameMachine()) return Outcome.TARGET_CHANGED;
        if (((SabotageTarget) machine).isDisrupted()) return Outcome.ALREADY_PAUSED;
        return ticksExisted >= approachEnd ? Outcome.OUT_OF_REACH : Outcome.NONE;
    }

    private void scan() {
        nextScan = ticksExisted + SCAN_INTERVAL;
        scans++;
        outcome = Outcome.NO_SUPPORTED_TARGET;
        targetType = TargetType.NONE;
        int baseX = MathHelper.floor_double(posX);
        int baseY = MathHelper.floor_double(boundingBox.minY);
        int baseZ = MathHelper.floor_double(posZ);
        int hopperId = HopperRegistry.block == null ? -1 : HopperRegistry.block.blockID;
        int receiverId = DistillationRegistry.blockId(DistillationPartType.RECEIVER);
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
                    if (distance > best) continue;
                    int id = worldObj.getBlockId(x, y, z);
                    if (id != hopperId && id != receiverId) continue;
                    TileEntity candidate = LoadedTile.at(worldObj, x, y, z);
                    if (!(candidate instanceof TileHopper) && !(candidate instanceof TileEntityDistillationReceiver)) {
                        outcome = Outcome.TARGET_CHANGED;
                    } else if (((SabotageTarget) candidate).isDisrupted()) {
                        outcome = Outcome.ALREADY_PAUSED;
                    } else if (!seesMachine(x, y, z)) {
                        outcome = Outcome.NO_SIGHT;
                    } else {
                        TileEntity pipe = candidate instanceof TileEntityDistillationReceiver
                                ? ((TileEntityDistillationReceiver) candidate).activeExtractionPipe() : null;
                        if (id == receiverId && pipe == null) {
                            outcome = Outcome.UNSUPPORTED_CONNECTION;
                            continue;
                        }
                        best = distance;
                        machine = candidate;
                        connection = pipe;
                        targetType = pipe == null ? TargetType.HOPPER : TargetType.BUILDCRAFT_RECEIVER;
                        machineX = x;
                        machineY = y;
                        machineZ = z;
                    }
                }
            }
        }
        if (machine == null) return;
        outcome = Outcome.NONE;
        approachEnd = ticksExisted + 100;
        double dx = posX - machineX - 0.5D;
        double dz = posZ - machineZ - 0.5D;
        nextSide = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? 0 : 1) : (dz > 0 ? 2 : 3);
        getNavigator().clearPathEntity();
    }

    private void approachMachine() {
        lookAtMachine();
        if (inContact() && seesMachine(machineX, machineY, machineZ)) {
            startWindup();
            return;
        }
        if (blink(machineX + 0.5D, machineZ + 0.5D, BlinkCause.APPROACH)) return;
        if (ticksExisted < nextRoute || !onGround) return;
        nextRoute = ticksExisted + ROUTE_INTERVAL;
        double x = machineX + 0.5D + SIDE_X[nextSide];
        double z = machineZ + 0.5D + SIDE_Z[nextSide];
        nextSide = (nextSide + 1) % SIDE_X.length;
        pathPlans++;
        PathEntity path = plan(x, machineY, z);
        if (reaches(path, x, machineY, z)) getNavigator().setPath(path, SPEED);
    }

    private boolean inContact() {
        return onGround && Math.abs(boundingBox.minY - machineY) <= 1.25D
                && getDistanceSq(machineX + 0.5D, machineY + 0.5D, machineZ + 0.5D) <= CONTACT_SQ;
    }

    private boolean sameMachine() {
        return machine != null && machine.xCoord == machineX && machine.yCoord == machineY && machine.zCoord == machineZ
                && LoadedTile.at(worldObj, machineX, machineY, machineZ) == machine
                && (connection == null || ((TileEntityDistillationReceiver) machine).activeExtractionPipe() == connection);
    }

    private boolean seesMachine(int x, int y, int z) {
        if (!worldObj.checkChunksExist(MathHelper.floor_double(Math.min(posX, x)) - 1, 0,
                MathHelper.floor_double(Math.min(posZ, z)) - 1,
                MathHelper.floor_double(Math.max(posX, x)) + 1, 0,
                MathHelper.floor_double(Math.max(posZ, z)) + 1)) return false;
        MovingObjectPosition hit = worldObj.rayTraceBlocks(eyes(), Vec3D.createVector(x + 0.5D, y + 0.65D, z + 0.5D));
        return hit == null || hit.blockX == x && hit.blockY == y && hit.blockZ == z;
    }

    private void lookAtMachine() {
        getLookHelper().setLookPosition(machineX + 0.5D, machineY + 0.65D, machineZ + 0.5D, 30.0F, 30.0F);
    }

    private void startWindup() {
        pose(machine == null ? DECOY : WINDUP);
        windupEnd = ticksExisted + WINDUP_TICKS;
        windups++;
        getNavigator().clearPathEntity();
        motionX = motionZ = 0;
        link();
    }

    private boolean windingUp() { return pose() == WINDUP || pose() == DECOY; }

    private void continueWindup(boolean visible) {
        if (machine == null) {
            if (!visible) finish(Outcome.NO_SIGHT, true);
            else if (ticksExisted >= windupEnd) {
                diversions++;
                finish(Outcome.DECOY, false);
            } else if ((windupEnd - ticksExisted) % 5 == 0) link();
            return;
        }

        if (!sameMachine()) finish(Outcome.TARGET_CHANGED, true);
        else if (!inContact()) finish(Outcome.OUT_OF_REACH, true);
        else if (!seesMachine(machineX, machineY, machineZ)) finish(Outcome.NO_SIGHT, true);
        else if (((SabotageTarget) machine).isDisrupted()) finish(Outcome.ALREADY_PAUSED, true);
        else {
            lookAtMachine();
            if (ticksExisted >= windupEnd) {
                if (((SabotageTarget) machine).beginDisruption(this)) {
                    disruptions++;
                    if (connection == null) burst(machineX + 0.5D, machineY + 0.85D, machineZ + 0.5D, "portal");
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
                    machine == null ? x : x + (machineX + 0.5D - x) * t,
                    machine == null ? y + t * 0.3D : y + (machineY + 0.7D - y) * t,
                    machine == null ? z : z + (machineZ + 0.5D - z) * t,
                    machine == null ? 0.44D : 0.82D, machine == null ? 0.12D : 0.42D, 0.9D);
        }
    }

    private void finish(Outcome result, boolean interrupted) {
        machine = connection = null;
        outcome = result;
        nextAction = ticksExisted + ACTION_COOLDOWN;
        recoverEnd = ticksExisted + RECOVERY_TICKS;
        getNavigator().clearPathEntity();
        pose(interrupted ? CANCELLED : RECOVER);
        if (interrupted) cancelled++;
        if (interrupted || result == Outcome.DECOY) burst(posX, boundingBox.minY + 0.7D, posZ, "smoke");
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, int damage) {
        boolean hit = super.attackEntityFrom(source, damage);
        if (hit && damage > 0 && !worldObj.isRemote && source.getEntity() instanceof EntityPlayer) {
            boolean interrupted = windingUp();
            if (interrupted) finish(Outcome.HIT, true);
            if (isEntityAlive() && (interrupted || damage >= 2 && rand.nextInt(3) != 0)) {
                pendingEvade = interrupted ? BlinkCause.INTERRUPTED : BlinkCause.HIT;
                hitEvadeEnd = ticksExisted + 22;
            }
        }
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

    public boolean canStartDisruption(TileEntity tile) {
        return pose() == WINDUP && machine == tile && ticksExisted >= windupEnd;
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
        if (allowBlink && blink(x, z, BlinkCause.APPROACH)) return;
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

    private boolean blink(double x, double z, BlinkCause cause) {
        double dx = x - posX;
        double dz = z - posZ;
        double distanceSq = dx * dx + dz * dz;
        boolean evasive = cause != BlinkCause.APPROACH;
        if (ticksExisted < nextBlink || ticksExisted < retryBlink || !onGround
                || !evasive && (hurtTime > 0 || distanceSq < 12.25D || distanceSq > 64))
            return false;
        blinkCause = cause;
        blinkAttempts++;
        if (evasive) evasiveAttempts++;
        double direction = Math.atan2(dz, dx) + (evasive ? Math.PI : 0);
        double[] angles = evasive ? ESCAPE_ANGLES : BLINK_ANGLES;
        int flank = (entityId & 1) == 0 ? 1 : -1;
        for (int i = 0; i < angles.length; i++) {
            double angle = direction + flank * angles[i];
            double range = !evasive ? 2.75D : i == angles.length - 1 ? 2.125D : 3.25D;
            double shiftX = Math.cos(angle) * range;
            double shiftZ = Math.sin(angle) * range;
            double separation = (dx - shiftX) * (dx - shiftX) + (dz - shiftZ) * (dz - shiftZ);
            if (evasive && (separation < 9 || separation <= distanceSq)) continue;
            blinkChecks++;
            blinkResult = SaboteurBlink.check(this, shiftX, shiftZ);
            if (blinkResult != SaboteurBlink.Result.CLEAR) {
                rejectedDestinations++;
                if (blinkResult == SaboteurBlink.Result.NOT_GROUNDED) break;
                continue;
            }
            burst(posX, posY + 0.45D, posZ, "portal");
            setPosition(posX + shiftX, posY, posZ + shiftZ);
            prevPosX = lastTickPosX = posX;
            prevPosZ = lastTickPosZ = posZ;
            motionX = motionZ = 0;
            getNavigator().clearPathEntity();
            burst(posX, posY + 0.45D, posZ, "portal");
            blinks++;
            if (evasive) evasiveBlinks++;
            nextBlink = ticksExisted + (evasive ? EVASIVE_BLINK_COOLDOWN : APPROACH_BLINK_COOLDOWN);
            nextRoute = ticksExisted + (evasive ? 10 : 2);
            stalledRoutes = 0;
            return true;
        }
        failedBlinks++;
        retryBlink = ticksExisted + BLINK_RETRY;
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
        closeTicks = hitEvadeEnd = evadeEnd = 0;
        machine = connection = null;
        if (windingUp()) finish(Outcome.PLAYER_LOST, true);
        else if (ticksExisted >= recoverEnd) pose(IDLE);
    }

    @Override
    protected void updateAttackPose() {
        if (target == null && ticksExisted >= recoverEnd && pose() != IDLE) pose(IDLE);
    }
}
