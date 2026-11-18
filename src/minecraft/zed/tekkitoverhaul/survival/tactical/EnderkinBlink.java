package zed.tekkitoverhaul.survival.tactical;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;

/** Shared movement and clearance. Target choice and cooldown remain with each role. */
final class EnderkinBlink {
    private static final double[] PLATFORM_ANGLES = {0, 0.62D, -0.62D};

    private EnderkinBlink() {}

    static boolean shift(TacticalHostile mob, double angle, double distance, int particles, float pitch) {
        double dx = Math.cos(angle) * distance, dz = Math.sin(angle) * distance;
        if (SaboteurBlink.check(mob, dx, dz) != SaboteurBlink.Result.CLEAR) return false;
        move(mob, dx, 0, dz, particles, pitch);
        return true;
    }

    static double[] elevatedLanding(TacticalHostile mob, EntityPlayer target, double separation, int maxRise) {
        if (!mob.onGround || !target.onGround || Math.abs(mob.motionY) > 0.1D) return null;
        double dy = Math.floor(target.boundingBox.minY + 0.01D) - mob.boundingBox.minY;
        if (dy < 1.95D || dy > maxRise + 0.01D) return null;
        double angle = Math.atan2(mob.posZ - target.posZ, mob.posX - target.posX);
        for (double offset : PLATFORM_ANGLES) {
            double dx = target.posX + Math.cos(angle + offset) * separation - mob.posX;
            double dz = target.posZ + Math.sin(angle + offset) * separation - mob.posZ;
            double distance = dx * dx + dz * dz;
            if (distance > 49 || distance < 1) continue;
            AxisAlignedBB body = mob.boundingBox;
            AxisAlignedBB raised = body.getOffsetBoundingBox(0, dy, 0);
            AxisAlignedBB landing = raised.getOffsetBoundingBox(dx, 0, dz);
            if (!loaded(mob.worldObj, body.addCoord(dx, dy, dz))) continue;
            if (!clear(mob, body.addCoord(0, dy, 0)) || !clear(mob, raised.addCoord(dx, 0, dz))) continue;
            if (floor(mob.worldObj, landing) && mob.worldObj.checkIfAABBIsClear(landing))
                return new double[] {dx, dy, dz};
        }
        return null;
    }

    static boolean loaded(World world, AxisAlignedBB area) {
        int halo = (int) Math.ceil(World.MAX_ENTITY_RADIUS) + 1;
        return area.minY >= 1 && area.maxY < world.getHeight()
                && world.checkChunksExist(MathHelper.floor_double(area.minX) - halo, 0,
                    MathHelper.floor_double(area.minZ) - halo, MathHelper.floor_double(area.maxX) + halo, 0,
                    MathHelper.floor_double(area.maxZ) + halo);
    }

    static boolean clear(TacticalHostile mob, AxisAlignedBB area) {
        if (!mob.worldObj.getCollidingBoundingBoxes(mob, area).isEmpty()) return false;
        for (int x = MathHelper.floor_double(area.minX); x <= MathHelper.floor_double(area.maxX); x++) {
            for (int z = MathHelper.floor_double(area.minZ); z <= MathHelper.floor_double(area.maxZ); z++) {
                for (int y = MathHelper.floor_double(area.minY + 0.01D); y <= MathHelper.floor_double(area.maxY); y++) {
                    Block block = Block.blocksList[mob.worldObj.getBlockId(x, y, z)];
                    if (block != null && (block.blockMaterial.isLiquid() || block == Block.fire
                            || block == Block.portal || block == Block.cactus)) return false;
                }
            }
        }
        return true;
    }

    static boolean floor(World world, AxisAlignedBB area) {
        int y = MathHelper.floor_double(area.minY + 0.01D) - 1;
        for (int x = MathHelper.floor_double(area.minX + 0.01D); x <= MathHelper.floor_double(area.maxX - 0.01D); x++) {
            for (int z = MathHelper.floor_double(area.minZ + 0.01D); z <= MathHelper.floor_double(area.maxZ - 0.01D); z++) {
                if (!world.isBlockNormalCube(x, y, z)) return false;
            }
        }
        return true;
    }

    static void move(TacticalHostile mob, double dx, double dy, double dz, int particles, float pitch) {
        burst(mob, particles);
        mob.setPosition(mob.posX + dx, mob.posY + dy, mob.posZ + dz);
        mob.prevPosX = mob.lastTickPosX = mob.posX;
        mob.prevPosY = mob.lastTickPosY = mob.posY;
        mob.prevPosZ = mob.lastTickPosZ = mob.posZ;
        mob.motionX = mob.motionY = mob.motionZ = 0;
        mob.fallDistance = 0;
        mob.getNavigator().clearPathEntity();
        burst(mob, particles);
        mob.worldObj.playSoundAtEntity(mob, "mob.endermen.portal", 0.65F, pitch);
    }

    private static void burst(TacticalHostile mob, int count) {
        for (int i = 0; i < count; i++) {
            mob.worldObj.spawnParticle("portal", mob.posX, mob.boundingBox.minY + mob.height * 0.45D, mob.posZ,
                    (mob.worldObj.rand.nextFloat() - 0.5F) * 0.6D, mob.worldObj.rand.nextFloat() * 0.3D,
                    (mob.worldObj.rand.nextFloat() - 0.5F) * 0.6D);
        }
    }
}
