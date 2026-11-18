package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.EnumCreatureAttribute;
import net.minecraft.src.World;

/** A nocturnal hunter with a shallow, pre-aimed crossing pass. */
public final class EntitySkelebird extends EntityAerialPredator {
    public static final String TEXTURE = "/zed/textures/skelebird.png";

    public EntitySkelebird(World world) {
        super(world);
        setSize(0.86F, 0.65F);
        texture = TEXTURE;
        experienceValue = 4;
    }

    @Override
    public int getMaxHealth() { return 14; }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() { return EnumCreatureAttribute.UNDEAD; }

    @Override
    public String speciesName() { return "Skelebird"; }

    @Override
    boolean slashingPass() { return true; }

    static boolean isNight(World world) {
        long time = Math.floorMod(world.getWorldTime(), 24000L);
        return time >= 12000 && time < 23000;
    }
}
