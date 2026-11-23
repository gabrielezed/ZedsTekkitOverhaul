package zed.tekkitoverhaul.assault;

import net.minecraft.src.DamageSource;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagInt;
import net.minecraft.src.World;

/** An encounter-owned skeleton with an optional daytime test exemption. Its visible brightness and native combat remain intact. */
public final class DebugSiegeSkeleton extends EntitySkeleton implements SiegeCensus.Participant {
    private final SiegeCensus.Actor siegeActor = new SiegeCensus.Actor();

    @Override public final SiegeCensus.Actor siegeActor() { return siegeActor; }

    @Override public void onUpdate() {
        super.onUpdate();
        if (siegeSite > 0) siegeActor.observe(this, siegeSite);
    }

    @Override public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (dead && siegeSite > 0) siegeActor.died(this, siegeSite);
    }
    private final boolean daylightTest;
    private boolean checkingSun;
    private int siegeSite;
    PerimeterApproachTask approach;

    public DebugSiegeSkeleton(World world) {
        this(world, false, 0);
    }

    DebugSiegeSkeleton(World world, boolean daylightTest, int siegeSite) {
        super(world);
        this.daylightTest = daylightTest;
        this.siegeSite = siegeSite;
    }

    @Override protected void despawnEntity() {
        boolean owned = approach != null && !isDead;
        super.despawnEntity();
        if (owned && isDead) approach.nativeDespawn = true;
    }

    @Override protected boolean canDespawn() { return false; }

    @Override protected void updateAITasks() {
        if (approach == null || !approach.updateWithoutCombat()) super.updateAITasks();
    }

    @Override public void onLivingUpdate() {
        checkingSun = daylightTest && approach != null;
        try {
            super.onLivingUpdate();
        } finally {
            checkingSun = false;
        }
    }

    @Override public float getBrightness(float partialTick) {
        return checkingSun ? 0.0F : super.getBrightness(partialTick);
    }

    @Override public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (siegeSite > 0) tag.setInteger("ZTOSiegeSite", siegeSite);
        siegeActor.write(tag);
    }

    @Override public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        siegeSite = tag.getTag("ZTOSiegeSite") instanceof NBTTagInt
                ? Math.max(0, tag.getInteger("ZTOSiegeSite")) : 0;
        siegeActor.read(tag, siegeSite);
    }
}
