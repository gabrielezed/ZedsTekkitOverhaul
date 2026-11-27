package zed.tekkitoverhaul.exploration.abyssal.guardian;

import java.util.UUID;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IMob;
import net.minecraft.src.Item;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites.Site;

public final class EntityAbyssalCustodian extends EntityLiving implements IMob {
    public static final String NAME = "ZTO.AbyssalCustodian";
    public static final String TEXTURE = "/zed/textures/abyssal/custodian.png";
    static final String SOUND = "zto.custodian.";
    private static final int POSE = 20, TICK = 21, ARM = 22, REACH = 23, PITCH = 24, YAW = 25, HIGH = 26, SIZE = 27, GAZE = 28;
    public static final int DORMANT = 0, SWIM = 1, AWAKEN = 2, TRANSITION = 3, ATTACK = 4;
    private final CustodianCombat combat = new CustodianCombat(this);
    private final SwimRoute route = new SwimRoute();
    private CustodianArena arena;
    private UUID encounter;
    private long siteIdentity;
    private int homeX, homeZ;
    private boolean phaseTwo;
    private float scale = 1, previousScale = 1;
    private int lastPose, observedPoseTick, previousPoseTick;

    public EntityAbyssalCustodian(World world) {
        super(world);
        setSize(CustodianBalance.WIDTH, CustodianBalance.HEIGHT);
        experienceValue = 12;
        texture = TEXTURE;
    }

    @Override protected void entityInit() {
        super.entityInit();
        for (int i = POSE; i <= HIGH; i++) dataWatcher.addObject(i, Integer.valueOf(0));
        dataWatcher.addObject(SIZE, Integer.valueOf(Float.floatToIntBits(1)));
        dataWatcher.addObject(GAZE, Integer.valueOf(0));
    }

    @Override public int getMaxHealth() { return CustodianBalance.HEALTH; }
    @Override public float getEyeHeight() { return height * 0.87F; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected boolean canDespawn() { return false; }
    @Override protected String getLivingSound() { return SOUND + "ambient"; }
    @Override protected String getHurtSound() { return SOUND + "hurt"; }
    @Override protected String getDeathSound() { return SOUND + "death"; }
    @Override public int getTalkInterval() { return 220; }

    static void admit(World world, EntityPlayer player, Site site) {
        if (!site.custodian().available() || !player.isInWater()) return;
        CustodianArena arena = new CustodianArena(site);
        if (player.getDistanceSq(arena.x, arena.y, arena.z) > 26 * 26) return;
        EntityAbyssalCustodian actor = new EntityAbyssalCustodian(world);
        int[][] offsets = {{0,0,0}, {0,1,0}, {2,0,0}, {-2,0,0}, {0,0,2}, {0,0,-2}};
        for (int[] p : offsets) {
            double x = arena.x + p[0], y = arena.y + p[1], z = arena.z + p[2];
            if (!arena.landing(actor, x, y, z)) continue;
            actor.encounter = site.custodian().reserve();
            if (actor.encounter == null) return;
            actor.arena = arena;
            actor.siteIdentity = site.identity();
            actor.homeX = SwimRoute.floor(arena.x);
            actor.homeZ = SwimRoute.floor(arena.z);
            actor.setPosition(x, y, z);
            if (!world.spawnEntityInWorld(actor))
                System.err.println("[ZTO] Custodian insertion failed at " + Long.toHexString(site.identity()));
            return;
        }
    }

    void manualHome() { arena = new CustodianArena(posX, boundingBox.minY, posZ); }

    private Site site() {
        Site site = encounter == null ? null : ObservatorySites.nearby(worldObj, homeX, homeZ);
        return site != null && site.identity() == siteIdentity && site.complete() ? site : null;
    }

    private boolean canonical(Site site) {
        if (site == null || !site.custodian().owns(encounter)) return false;
        if (arena == null) arena = new CustodianArena(site);
        AxisAlignedBB area = arena.bounds.expand(8, 8, 8);
        for (Object object : worldObj.getEntitiesWithinAABB(EntityAbyssalCustodian.class, area)) {
            EntityAbyssalCustodian other = (EntityAbyssalCustodian) object;
            if (!other.isDead && other.entityId < entityId && encounter.equals(other.encounter)) return false;
        }
        return true;
    }

    @Override public void onUpdate() {
        if (!worldObj.isRemote && isEntityAlive() && !AquaticDevelopment.manual(this)
                && (arena == null || (ticksExisted & 31) == 0)) {
            Site site = site();
            if (!canonical(site)) {
                if (site != null && site.custodian().owns(encounter))
                    System.err.println("[ZTO] Discarded duplicate Custodian at " + Long.toHexString(siteIdentity));
                setDead();
                return;
            }
        }
        previousScale = scale;
        if (worldObj.isRemote) applyBodyScale(bodyScale());
        super.onUpdate();
        previousPoseTick = lastPose == pose() ? observedPoseTick : poseTick();
        lastPose = pose();
        observedPoseTick = poseTick();
        particles();
    }

    @Override public boolean attackEntityFrom(DamageSource damage, int amount) {
        return worldObj.difficultySetting != 0 && super.attackEntityFrom(damage, amount);
    }

    @Override protected void dropFewItems(boolean recentlyHit, int looting) {
        if (worldObj.difficultySetting == 0) return;
        Site site = site();
        if (canonical(site) && site.custodian().defeat(encounter)) dropItem(Item.enderPearl.shiftedIndex, 1);
    }

    @Override protected void updateEntityActionState() {
        if (worldObj.isRemote || arena == null) return;
        if (worldObj.difficultySetting == 0) {
            combat.disengage();
            halt();
            return;
        }
        if (scale < 1 && !combat.compressing()) {
            if (!resize(Math.min(1, scale + 0.035F))) steer(arena.x, arena.y, arena.z, CustodianBalance.CRUISE);
            return;
        }
        combat.tick();
    }

    @Override public void moveEntityWithHeading(float strafe, float forward) {
        if (worldObj.isRemote || arena == null) return;
        if (worldObj.difficultySetting == 0) {
            halt();
        } else if (isInWater()) {
            boolean inside = arena.contains(posX, boundingBox.minY, posZ, width, height);
            if (!inside || arena.contains(posX + motionX, boundingBox.minY + motionY, posZ + motionZ,
                    width, height)) {
                SwimRoute.move(this, 0.85);
            } else halt();
        } else super.moveEntityWithHeading(0, 0);
    }

    void steer(double x, double y, double z, double speed) {
        float oldYaw = rotationYaw;
        route.steer(this, arena.xWithin(x, width), arena.yWithin(y, height), arena.zWithin(z, width), speed);
        rotationYaw = oldYaw + Math.max(-11, Math.min(11, wrap(rotationYaw - oldYaw)));
    }

    public static float wrap(float angle) { return angle - 360 * (float) Math.floor((angle + 180) / 360); }

    void lookAt(EntityPlayer player) {
        float offset = 0;
        if (player != null) {
            float yaw = (float) Math.toDegrees(Math.atan2(posX - player.posX, player.posZ - posZ));
            offset = Math.max(-30, Math.min(30, wrap(yaw - rotationYaw)));
        }
        float previous = gaze();
        float next = previous + Math.max(-6, Math.min(6, offset - previous));
        dataWatcher.updateObject(GAZE, Integer.valueOf(Float.floatToIntBits(next)));
    }

    public float gaze() { return Float.intBitsToFloat(dataWatcher.getWatchableObjectInt(GAZE)); }

    public float bodyScale() { return Float.intBitsToFloat(dataWatcher.getWatchableObjectInt(SIZE)); }

    public float animatedScale(float partial) { return previousScale == 0 ? bodyScale() : previousScale + (bodyScale() - previousScale) * partial; }

    boolean resize(float next) {
        if (next > scale && !arena.free(this, posX, boundingBox.minY, posZ,
                CustodianBalance.WIDTH * next, CustodianBalance.HEIGHT * next)) return false;
        applyBodyScale(next);
        dataWatcher.updateObject(SIZE, Integer.valueOf(Float.floatToIntBits(next)));
        return true;
    }

    private void applyBodyScale(float next) {
        scale = next;
        setSize(CustodianBalance.WIDTH * next, CustodianBalance.HEIGHT * next);
        setPosition(posX, posY, posZ);
    }

    void halt() { motionX = motionY = motionZ = 0; }

    void blink(Vec3D to) {
        halt();
        route.reset();
        riftParticles(posX, boundingBox.minY, posZ, 12);
        setPosition(to.xCoord, to.yCoord, to.zCoord);
        prevPosX = lastTickPosX = posX;
        prevPosY = lastTickPosY = posY;
        prevPosZ = lastTickPosZ = posZ;
        riftParticles(posX, boundingBox.minY, posZ, 12);
    }

    void riftParticles(double x, double y, double z, int count) {
        // Keep visual variation independent of combat and destination randomness.
        for (int i = 0; i < count; i++) {
            double angle = (i + ticksExisted * 0.375) * 2.39996323;
            worldObj.spawnParticle("portal", x + Math.cos(angle) * 0.52, y + (i % 6) * 0.62 * bodyScale(),
                    z + Math.sin(angle) * 0.52, Math.cos(angle) * 0.25, 0.02, Math.sin(angle) * 0.25);
        }
    }

    int combatPressure(EntityPlayer player) { return isEntityAlive() ? combat.pressureOn(player) : 0; }

    boolean secondPhase() { return phaseTwo; }
    void enterSecondPhase() { phaseTwo = true; }
    CustodianArena arena() { return arena; }
    long siteIdentity() { return siteIdentity; }
    boolean siteOwned() { return encounter != null; }
    boolean fullHeight() {
        return arena.contains(posX, boundingBox.minY, posZ, CustodianBalance.EXTENDED_HEIGHT)
                && SwimRoute.clear(this, posX, boundingBox.minY + CustodianBalance.EXTENDED_HEIGHT - height, posZ);
    }

    public int pose() { return dataWatcher.getWatchableObjectInt(POSE); }
    public int poseTick() { return dataWatcher.getWatchableObjectInt(TICK); }
    public float animationTick(float partial) {
        int tick = poseTick();
        return Math.max(0, tick - (tick > 0 && tick != previousPoseTick ? 1 - partial : 0));
    }
    public int strikingArm() { return dataWatcher.getWatchableObjectInt(ARM); }
    public float strikeReach() { return Float.intBitsToFloat(dataWatcher.getWatchableObjectInt(REACH)); }
    public float strikePitch() { return Float.intBitsToFloat(dataWatcher.getWatchableObjectInt(PITCH)); }
    public float strikeYaw() { return Float.intBitsToFloat(dataWatcher.getWatchableObjectInt(YAW)); }
    public boolean highPose() { return dataWatcher.getWatchableObjectInt(HIGH) != 0; }

    void pose(int pose, int tick) {
        dataWatcher.updateObject(POSE, Integer.valueOf(pose));
        dataWatcher.updateObject(TICK, Integer.valueOf(tick));
        dataWatcher.updateObject(HIGH, Integer.valueOf((pose == AWAKEN || pose == TRANSITION
                || pose == ATTACK + CustodianBalance.Attack.BLINK.ordinal()
                || pose == ATTACK + CustodianBalance.Attack.RESONANCE.ordinal()) && fullHeight() ? 1 : 0));
    }

    void strikePose(int side, double reach, float yaw, float pitch) {
        dataWatcher.updateObject(ARM, Integer.valueOf(side));
        dataWatcher.updateObject(REACH, Integer.valueOf(Float.floatToIntBits((float) reach)));
        dataWatcher.updateObject(PITCH, Integer.valueOf(Float.floatToIntBits(pitch)));
        dataWatcher.updateObject(YAW, Integer.valueOf(Float.floatToIntBits(yaw)));
    }

    private void particles() {
        int tick = poseTick();
        int pose = pose();
        int count = deathTime > 0 ? 2 : pose >= AWAKEN && (ticksExisted % 3 == 0) ? 3 : ticksExisted % 14 == 0 ? 1 : 0;
        for (int i = 0; i < count; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            boolean pressure = pose == ATTACK + CustodianBalance.Attack.PRESSURE.ordinal()
                    && tick >= CustodianBalance.Attack.PRESSURE.windup
                    && tick < CustodianBalance.Attack.PRESSURE.windup + 10;
            boolean signal = pose == ATTACK + CustodianBalance.Attack.RESONANCE.ordinal()
                    && tick >= CustodianBalance.Attack.RESONANCE.windup
                    && tick < CustodianBalance.Attack.RESONANCE.windup + 16;
            double radius = pressure ? 0.6 * (tick - CustodianBalance.Attack.PRESSURE.windup + 1)
                    : signal ? 0.22 * (tick - CustodianBalance.Attack.RESONANCE.windup + 1) : 0.45;
            worldObj.spawnParticle(pressure ? "bubble" : "portal", posX + radius * Math.cos(angle),
                    boundingBox.minY + (0.75 + rand.nextDouble() * 2.8) * bodyScale(), posZ + radius * Math.sin(angle),
                    Math.cos(angle) * 0.2, 0.03, Math.sin(angle) * 0.2);
        }
    }

    @Override public boolean addEntityID(NBTTagCompound tag) {
        return encounter != null && super.addEntityID(tag);
    }

    @Override public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (encounter == null) return;
        tag.setLong("ZtoObservatory", siteIdentity);
        tag.setLong("ZtoCustodianMost", encounter.getMostSignificantBits());
        tag.setLong("ZtoCustodianLeast", encounter.getLeastSignificantBits());
        tag.setInteger("ZtoHomeX", homeX);
        tag.setInteger("ZtoHomeZ", homeZ);
        tag.setBoolean("ZtoCustodianPhaseTwo", phaseTwo);
        tag.setFloat("ZtoCustodianScale", bodyScale());
    }

    @Override public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (!tag.hasKey("ZtoCustodianMost") || !tag.hasKey("ZtoCustodianLeast")) return;
        encounter = new UUID(tag.getLong("ZtoCustodianMost"), tag.getLong("ZtoCustodianLeast"));
        siteIdentity = tag.getLong("ZtoObservatory");
        homeX = tag.getInteger("ZtoHomeX");
        homeZ = tag.getInteger("ZtoHomeZ");
        phaseTwo = tag.getBoolean("ZtoCustodianPhaseTwo");
        // The original alpha.4 body may have saved inside a space too small for the redesigned actor.
        float saved = tag.hasKey("ZtoCustodianScale") ? tag.getFloat("ZtoCustodianScale") : 0.55F;
        if (Float.isNaN(saved) || saved < CustodianBalance.COMPRESSED_SCALE || saved > 1) saved = 1;
        applyBodyScale(saved);
        previousScale = saved;
        dataWatcher.updateObject(SIZE, Integer.valueOf(Float.floatToIntBits(saved)));
    }
}
