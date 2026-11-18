package zed.tekkitoverhaul.survival.tactical;

import java.util.List;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkProvider;
import net.minecraft.src.EntityCreeper;
import net.minecraft.src.EntityEnderman;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.EntityZombie;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.MathHelper;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.TileEntity;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;

/** Gives some validated ordinary hostile slots to Enderkin. No independent spawn loop. */
public final class EnderkinEcology {
    private static final int INDUSTRY_RADIUS = 12;
    private static final int TILES_PER_CHUNK = 32;

    private EnderkinEcology() {}

    public static EntityLiving select(EntityLiving original, int activity) {
        Class<?> type = original.getClass();
        if (type != EntityZombie.class && type != EntitySkeleton.class && type != EntityCreeper.class
                && type != EntityEnderman.class) return original;
        World world = original.worldObj;
        int roll = world.rand.nextInt(1000);
        int residentBonus = Math.min(20, activity);
        if (roll >= 145 + residentBonus) return original;

        // Cheap independent habitat rejection before looking up the nearby hostile population.
        int skirmisher = 15 + residentBonus / 2;
        int breacher = skirmisher + 11 + residentBonus / 2;
        int thief = breacher + 6;
        int tank = thief + 2;
        int saboteur = tank + 5;
        if (roll >= saboteur && !nearIndustry(original)) return original;

        List<?> nearby = world.getEntitiesWithinAABB(TacticalHostile.class, original.boundingBox.expand(32, 16, 32));
        int alive = 0;
        for (Object entity : nearby) {
            TacticalHostile mob = (TacticalHostile) entity;
            if (!mob.isEntityAlive()) continue;
            if (++alive >= 5 || (roll >= thief && roll < tank && mob instanceof EntityTank)
                    || (roll >= tank && mob instanceof EntitySaboteur)) return original;
        }
        TacticalHostile replacement;
        if (roll < skirmisher) replacement = new EntitySkirmisher(world);
        else if (roll < breacher) replacement = new EntityBreacher(world);
        else if (roll < thief) replacement = new EntityThief(world);
        else if (roll < tank) replacement = new EntityTank(world);
        else replacement = new EntitySaboteur(world);
        replacement.setLocationAndAngles(original.posX, original.boundingBox.minY, original.posZ, original.rotationYaw, 0);
        // The vanilla candidate already passed the light roll at these feet coordinates.
        if (!EnderkinBlink.loaded(world, replacement.boundingBox)
                || !EnderkinBlink.floor(world, replacement.boundingBox) || !EnderkinBlink.clear(replacement, replacement.boundingBox)
                || !world.checkIfAABBIsClear(replacement.boundingBox)) return original;
        return replacement;
    }

    private static boolean nearIndustry(EntityLiving candidate) {
        World world = candidate.worldObj;
        IChunkProvider provider = world.getChunkProvider();
        if (provider.getClass() != ChunkProvider.class) return false;
        int cx = MathHelper.floor_double(candidate.posX), cz = MathHelper.floor_double(candidate.posZ);
        for (int x = (cx - INDUSTRY_RADIUS) >> 4; x <= (cx + INDUSTRY_RADIUS) >> 4; x++) {
            for (int z = (cz - INDUSTRY_RADIUS) >> 4; z <= (cz + INDUSTRY_RADIUS) >> 4; z++) {
                if (!provider.chunkExists(x, z)) continue;
                Chunk chunk = provider.provideChunk(x, z);
                int tested = 0;
                for (Object value : chunk.chunkTileEntityMap.values()) {
                    if (++tested > TILES_PER_CHUNK) break;
                    TileEntity tile = (TileEntity) value;
                    if (tile.isInvalid() || tile.worldObj != world || Math.abs(tile.yCoord - candidate.posY) > 4) continue;
                    int dx = tile.xCoord - cx, dz = tile.zCoord - cz;
                    if (dx * dx + dz * dz > INDUSTRY_RADIUS * INDUSTRY_RADIUS) continue;
                    if (!(tile instanceof SabotageTarget) && !IndustrialSabotage.transportSignal(tile)) continue;
                    if (exposed(candidate, tile, cx, cz)) return true;
                }
            }
        }
        return false;
    }

    private static boolean exposed(EntityLiving candidate, TileEntity tile, int x, int z) {
        World world = candidate.worldObj;
        if (!world.checkChunksExist(Math.min(x, tile.xCoord), 0, Math.min(z, tile.zCoord),
                Math.max(x, tile.xCoord), 0, Math.max(z, tile.zCoord))) return false;
        MovingObjectPosition hit = world.rayTraceBlocks(Vec3D.createVector(candidate.posX, candidate.posY + 0.8D, candidate.posZ),
                Vec3D.createVector(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D));
        return hit == null || (hit.blockX == tile.xCoord && hit.blockY == tile.yCoord && hit.blockZ == tile.zCoord);
    }
}
