package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityFlying;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IMob;
import net.minecraft.src.MathHelper;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;

/** A single predator with server-owned steering, commitment and contact. */
public final class EntityAerialPredator extends EntityFlying implements IMob {
    public enum Flight {
        CRUISE, GLIDE, CIRCLE, PREPARE, DIVE, RECOVER, DISENGAGE, FALL
    }

    public static final String TEXTURE = "/zed/textures/aerial_predator.png";
    private static final Flight[] FLIGHTS = Flight.values();
    private static final int FLIGHT_WATCHER = 20;
    private static final int PREPARE_TICKS = 18;
    private static final int RECOVER_TICKS = 50;
    private static final int DISENGAGE_TICKS = 120;
    private static final double AIRBORNE_HEIGHT = 8;
    private static final int[] AVOID_ANGLES = {0, -10, 10, -20, 20};

    private Flight flight = Flight.CRUISE;
    private EntityPlayer target;
    private int stateTicks, huntTicks, attempts, attemptLimit, scanDelay = 30, blockedTicks, hiddenTicks;
    private int cruiseTicks;
    private boolean aerialHunt;
    private float cruiseYaw;
    private double destinationX, destinationY, destinationZ;
    private double diveX, diveY, diveZ, diveRemaining, diveSpeed;
    private String lastFailure = "none", lastExit = "none";

    public EntityAerialPredator(World world) {
        super(world);
        setSize(0.95F, 0.62F);
        texture = TEXTURE;
        experienceValue = 4;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(FLIGHT_WATCHER, Byte.valueOf((byte) Flight.CRUISE.ordinal()));
    }

    @Override
    public int getMaxHealth() { return 16; }

    @Override
    public boolean getCanSpawnHere() {
        return worldObj.worldProvider.worldType == 0 && worldObj.difficultySetting > 0
                && AerialTerrain.clear(this, boundingBox)
                && worldObj.checkIfAABBIsClear(boundingBox);
    }

    public Flight flight() {
        return getHealth() <= 0 ? Flight.FALL : FLIGHTS[dataWatcher.getWatchableObjectByte(FLIGHT_WATCHER)];
    }

    @Override
    protected void updateEntityActionState() {
        if (worldObj.difficultySetting == 0) {
            setDead();
            return;
        }
        ++entityAge;
        despawnEntity();
        if (isDead) return;
        ++stateTicks;

        if (target != null) {
            ++huntTicks;
            String exit = invalidTarget();
            if (exit != null) disengage(exit);
        }

        switch (flight) {
            case CRUISE:
            case GLIDE:
                if (--scanDelay <= 0) {
                    scanDelay = 35 + rand.nextInt(21);
                    detect();
                }
                if (target == null) cruise();
                else circle();
                break;
            case CIRCLE:
                circle();
                break;
            case PREPARE:
                prepare();
                break;
            case DIVE:
                advanceDive();
                break;
            case RECOVER:
                climb();
                if (stateTicks >= RECOVER_TICKS) {
                    if (attempts >= attemptLimit) disengage("attack budget");
                    else enter(Flight.CIRCLE);
                }
                break;
            case DISENGAGE:
                steer(destinationX, destinationY, destinationZ, 0.30);
                if (stateTicks >= DISENGAGE_TICKS) {
                    cruiseTicks = 0;
                    scanDelay = 60;
                    enter(Flight.CRUISE);
                }
                break;
            default:
                break;
        }
    }

    private void detect() {
        if (worldObj.worldProvider.worldType != 0) return;
        EntityPlayer best = null;
        boolean bestAirborne = false;
        double bestDistance = 36 * 36;
        for (Object entry : worldObj.playerEntities) {
            EntityPlayer player = (EntityPlayer) entry;
            double distance = getDistanceSqToEntity(player);
            if (distance > 36 * 36 || !eligible(player)) continue;
            double altitude = AerialTerrain.skyHeight(player);
            if (altitude < 0 || Math.abs(centerY() - playerCenter(player)) > 30) continue;
            boolean airborne = !player.onGround && altitude >= AIRBORNE_HEIGHT;
            if (!airborne && (distance > 24 * 24 || bestAirborne)) continue;
            if (airborne == bestAirborne && distance >= bestDistance) continue;
            if (!visible(player)) continue;
            best = player;
            bestAirborne = airborne;
            bestDistance = distance;
        }
        if (best == null || (!bestAirborne && rand.nextInt(worldObj.isDaytime() ? 42 : 9) != 0)) return;
        target = best;
        aerialHunt = bestAirborne;
        attempts = huntTicks = hiddenTicks = 0;
        attemptLimit = aerialHunt ? 2 : 1;
        enter(Flight.CIRCLE);
    }

    private boolean eligible(EntityPlayer player) {
        return player.worldObj == worldObj && !player.isDead && player.getHealth() > 0
                && !player.capabilities.disableDamage && !player.isPlayerSleeping();
    }

    private String invalidTarget() {
        if (!eligible(target)) return "target unavailable";
        if (huntTicks > 400 || getDistanceSqToEntity(target) > 48 * 48) return "pursuit limit";
        // Native block collision protects all intervening terrain. A roof also ends pursuit.
        double altitude = AerialTerrain.skyHeight(target);
        if (altitude < 0) return "target under cover or unloaded";
        if (aerialHunt && altitude < 3 && flight != Flight.DIVE) return "target descended";
        if (huntTicks % 5 == 0) hiddenTicks = visible(target) ? 0 : hiddenTicks + 5;
        if (hiddenTicks >= 30 && flight != Flight.DIVE) return "target out of sight";
        return null;
    }

    private boolean visible(EntityPlayer player) {
        return AerialTerrain.loaded(worldObj, posX, posZ, player.posX, player.posZ) && canEntityBeSeen(player);
    }

    private void cruise() {
        if (--cruiseTicks <= 0 || horizontalRange(destinationX, destinationZ) < 3) {
            cruiseTicks = 55 + rand.nextInt(41);
            cruiseYaw = rotationYaw + 20 + rand.nextInt(56);
            float angle = cruiseYaw * (float) Math.PI / 180;
            destinationX = posX - MathHelper.sin(angle) * 18;
            destinationZ = posZ + MathHelper.cos(angle) * 18;
            double desiredY = centerY() + 4;
            if (AerialTerrain.loaded(worldObj, destinationX, destinationZ, destinationX, destinationZ)) {
                int surface = worldObj.getHeightValue(MathHelper.floor_double(destinationX),
                        MathHelper.floor_double(destinationZ));
                desiredY = surface + 12 + rand.nextInt(7);
            }
            destinationY = limitAltitude(desiredY);
        }
        Flight pose = cruiseTicks < 29 && Math.abs(destinationY - centerY()) < 3 ? Flight.GLIDE : Flight.CRUISE;
        if (flight != pose) enter(pose);
        steer(destinationX, destinationY, destinationZ, pose == Flight.GLIDE ? 0.27 : 0.32);
    }

    private void circle() {
        if (hiddenTicks > 0) {
            steer(destinationX, destinationY, destinationZ, 0.26);
            return;
        }
        double angle = Math.atan2(posZ - target.posZ, posX - target.posX) + 0.82;
        double altitude = limitAltitude(playerCenter(target) + 8.5);
        steer(target.posX + Math.cos(angle) * 10, altitude, target.posZ + Math.sin(angle) * 10, 0.37);
        double height = centerY() - playerCenter(target);
        double range = horizontalRange(target.posX, target.posZ);
        if (stateTicks >= 42 && height >= 4 && height <= 15 && range >= 5 && range <= 16 && visible(target)) {
            enter(Flight.PREPARE);
            worldObj.playSoundAtEntity(this, "mob.ghast.moan", 0.65F, 1.7F);
        } else if (stateTicks >= 185) {
            disengage("no dive approach");
        }
    }

    private void prepare() {
        if (hiddenTicks == 0) {
            steer(target.posX, Math.max(centerY(), playerCenter(target) + 5), target.posZ, 0.15);
        } else {
            steer(destinationX, destinationY, destinationZ, 0.15);
        }
        if (stateTicks < PREPARE_TICKS) return;
        ++attempts;
        double endX = target.posX + clamp(target.motionX * 10, -3, 3);
        double endY = playerCenter(target) + (aerialHunt ? clamp(target.motionY * 8, -2, 2) : 0);
        double endZ = target.posZ + clamp(target.motionZ * 10, -3, 3);
        double dx = endX - posX, dy = endY - centerY(), dz = endZ - posZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double headingError = Math.abs(wrap(yaw(dx, dz) - rotationYaw));
        if (dy > -2.5 || length > 23 || headingError > 42 || !visible(target)
                || !AerialTerrain.loaded(worldObj, posX, posZ, endX, endZ)
                || !unobstructed(posX, centerY(), posZ, endX, endY, endZ)) {
            lastFailure = "commit path rejected";
            recover();
            return;
        }
        diveX = dx / length;
        diveY = dy / length;
        diveZ = dz / length;
        diveRemaining = length + 1.4;
        diveSpeed = 0.40;
        destinationX = endX;
        destinationY = endY;
        destinationZ = endZ;
        enter(Flight.DIVE);
        advanceDive();
    }

    private void advanceDive() {
        diveSpeed = Math.min(1.05, diveSpeed + 0.085);
        motionX = diveX * diveSpeed;
        motionY = diveY * diveSpeed;
        motionZ = diveZ * diveSpeed;
        faceMotion();
    }

    private void recover() {
        enter(Flight.RECOVER);
        destinationX = posX;
        destinationY = limitAltitude(centerY() + 8);
        destinationZ = posZ;
    }

    private void climb() {
        steer(posX - MathHelper.sin(rotationYaw * (float) Math.PI / 180) * 9,
                destinationY, posZ + MathHelper.cos(rotationYaw * (float) Math.PI / 180) * 9, 0.31);
    }

    private void disengage(String reason) {
        lastExit = reason;
        double dx = target == null ? 0 : posX - target.posX;
        double dz = target == null ? 0 : posZ - target.posZ;
        double range = Math.sqrt(dx * dx + dz * dz);
        float angle = rotationYaw * (float) Math.PI / 180;
        destinationX = posX + (range < 1 ? -MathHelper.sin(angle) : dx / range) * 23;
        destinationZ = posZ + (range < 1 ? MathHelper.cos(angle) : dz / range) * 23;
        destinationY = limitAltitude(centerY() + 7);
        target = null;
        enter(Flight.DISENGAGE);
    }

    private void steer(double x, double y, double z, double speed) {
        destinationX = x;
        destinationY = limitAltitude(y);
        destinationZ = z;
        double dy = clamp((destinationY - centerY()) * 0.095, -0.25, 0.31);
        float desired = horizontalRange(x, z) < 1 ? rotationYaw : yaw(x - posX, z - posZ);
        // Check the current reachable headings first. A climb or slow pivot is the local fallback.
        double turn = clamp(wrap(desired - rotationYaw), -10, 10);
        int previous = Integer.MIN_VALUE;
        for (int offset : AVOID_ANGLES) {
            int step = (int) Math.round(clamp(turn + offset, -10, 10));
            if (step == previous) continue;
            previous = step;
            float bearing = rotationYaw + step;
            float angle = bearing * (float) Math.PI / 180;
            double vx = -MathHelper.sin(angle) * speed;
            double vz = MathHelper.cos(angle) * speed;
            if (!clearance(vx, dy, vz, 7)) continue;
            rotationYaw = bearing;
            blend(vx, dy, vz);
            return;
        }
        if (clearance(0, 0.24, 0, 5)) {
            lastFailure = "obstacle: climbing";
            rotationYaw += 9;
            blend(0, 0.24, 0);
        } else {
            lastFailure = "obstacle: turning";
            rotationYaw += 11;
            // Pivot until a reachable corridor opens. Repeated contacts request a fresh cruise waypoint.
            blend(0, clearance(0, -0.14, 0, 4) ? -0.14 : 0, 0);
            if (blockedTicks >= 6 && target == null) cruiseTicks = 0;
        }
    }

    private boolean clearance(double x, double y, double z, int ticks) {
        return AerialTerrain.clear(this, boundingBox.addCoord(x * ticks, y * ticks, z * ticks));
    }

    private void blend(double x, double y, double z) {
        motionX += (x - motionX) * 0.25;
        motionY += (y - motionY) * 0.25;
        motionZ += (z - motionZ) * 0.25;
        rotationPitch += ((float) -Math.toDegrees(Math.atan2(motionY, Math.max(0.12,
                Math.sqrt(motionX * motionX + motionZ * motionZ)))) - rotationPitch) * 0.28F;
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (worldObj.isRemote || isDead) return;
        if (getHealth() <= 0) {
            motionX *= 0.91;
            motionY = Math.max(-0.85, motionY - 0.085);
            motionZ *= 0.91;
        }
        boolean diving = flight == Flight.DIVE && getHealth() > 0;
        double fromX = posX, fromY = centerY(), fromZ = posZ;
        if (!AerialTerrain.loaded(worldObj, boundingBox.addCoord(motionX, motionY, motionZ))) {
            lastFailure = "unloaded boundary";
            motionX = motionY = motionZ = 0;
            rotationYaw += 11;
            cruiseTicks = 0;
            if (target != null) disengage("unloaded boundary");
            return;
        }
        moveEntity(motionX, motionY, motionZ);
        if (isCollided) {
            ++blockedTicks;
            lastFailure = "terrain contact";
        } else {
            blockedTicks = 0;
        }
        if (!diving) return;
        if (hitAlongMovement(fromX, fromY, fromZ)) {
            recover();
        } else if (isCollided || (diveRemaining -= diveSpeed) <= 0) {
            lastFailure = isCollided ? "dive blocked by terrain" : "dive missed";
            recover();
        }
    }

    private boolean hitAlongMovement(double x, double y, double z) {
        if (target == null) return false;
        AxisAlignedBB contact = target.boundingBox.expand(width * 0.43, height * 0.43, width * 0.43);
        Vec3D start = Vec3D.createVector(x, y, z);
        Vec3D end = Vec3D.createVector(posX, centerY(), posZ);
        if (!contact.isVecInside(start) && !contact.isVecInside(end)
                && contact.calculateIntercept(start, end) == null) return false;
        if (!visible(target) || !unobstructed(posX, centerY(), posZ, target.posX, playerCenter(target), target.posZ)) return false;
        double vx = target.motionX, vy = target.motionY, vz = target.motionZ;
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), 4 + worldObj.difficultySetting);
        if (hit) {
            target.motionX = vx + clamp(target.motionX - vx, -0.30, 0.30) * 0.35;
            target.motionY = vy;
            target.motionZ = vz + clamp(target.motionZ - vz, -0.30, 0.30) * 0.35;
            worldObj.playSoundAtEntity(this, "random.bowhit", 0.8F, 0.72F);
        }
        lastFailure = hit ? "none" : "contact damage resisted";
        return true;
    }

    private boolean unobstructed(double x, double y, double z, double endX, double endY, double endZ) {
        return worldObj.rayTraceBlocks(Vec3D.createVector(x, y, z),
                Vec3D.createVector(endX, endY, endZ)) == null;
    }

    private void enter(Flight state) {
        stateTicks = 0;
        flight = state;
        dataWatcher.updateObject(FLIGHT_WATCHER, Byte.valueOf((byte) state.ordinal()));
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        int delay = target != null ? DISENGAGE_TICKS : scanDelay;
        if (flight == Flight.DISENGAGE) delay = Math.max(delay, DISENGAGE_TICKS - stateTicks + 60);
        tag.setInteger("ZtoAerialScanDelay", delay);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        target = null;
        enter(Flight.CRUISE);
        cruiseTicks = 0;
        scanDelay = (int) clamp(tag.getInteger("ZtoAerialScanDelay"), 30, 180);
        motionX = clamp(motionX, -0.32, 0.32);
        motionY = clamp(motionY, -0.25, 0.31);
        motionZ = clamp(motionZ, -0.32, 0.32);
    }

    public String targetName() { return target == null ? "-" : target.username; }
    public double terrainClearance() { return AerialTerrain.skyHeight(this); }
    public double destinationX() { return destinationX; }
    public double destinationY() { return destinationY; }
    public double destinationZ() { return destinationZ; }
    public int phaseTicks() { return stateTicks; }
    public int remainingPhaseTicks() {
        switch (flight) {
            case PREPARE: return Math.max(0, PREPARE_TICKS - stateTicks);
            case RECOVER: return Math.max(0, RECOVER_TICKS - stateTicks);
            case DISENGAGE: return Math.max(0, DISENGAGE_TICKS - stateTicks);
            default: return 0;
        }
    }
    public String lastFailure() { return lastFailure; }
    public String lastExit() { return lastExit; }

    private double centerY() { return boundingBox.minY + height * 0.5; }
    private static double playerCenter(EntityPlayer player) {
        return (player.boundingBox.minY + player.boundingBox.maxY) * 0.5;
    }
    private double horizontalRange(double x, double z) {
        return Math.sqrt((x - posX) * (x - posX) + (z - posZ) * (z - posZ));
    }
    private double limitAltitude(double y) { return clamp(y, 2, worldObj.getHeight() - 2.0); }
    private void faceMotion() {
        rotationYaw = yaw(motionX, motionZ);
        rotationPitch = (float) -Math.toDegrees(Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)));
    }
    private static float yaw(double x, double z) { return (float) Math.toDegrees(Math.atan2(-x, z)); }
    private static double wrap(double degrees) {
        degrees %= 360;
        if (degrees >= 180) degrees -= 360;
        if (degrees < -180) degrees += 360;
        return degrees;
    }
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
