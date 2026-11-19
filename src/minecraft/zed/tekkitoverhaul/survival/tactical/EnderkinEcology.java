package zed.tekkitoverhaul.survival.tactical;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.Block;
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
    private static final int SABOTEUR_TICKETS = 111;
    private static final int UNDERGROUND_TICKETS = 85;

    private EnderkinEcology() {}

    public static EntityLiving select(EntityLiving original, int activity) {
        return select(original, activity, null);
    }

    public static EntityLiving select(EntityLiving original, int activity, BiConsumer<Boolean, String> observer) {
        Class<?> type = original.getClass();
        if (type != EntityZombie.class && type != EntitySkeleton.class && type != EntityCreeper.class
                && type != EntityEnderman.class) return original;
        World world = original.worldObj;
        int roll = world.rand.nextInt(1000);
        int ordinaryBudget = 34 + 2 * (Math.min(20, Math.max(0, activity)) / 2);
        if (roll >= ordinaryBudget + SABOTEUR_TICKETS + UNDERGROUND_TICKETS) return original;

        int x = MathHelper.floor_double(original.posX), z = MathHelper.floor_double(original.posZ);
        BiomeGenBase biome = BiomeEcology.loadedBiome(world, x, z);
        if (biome == null) return rejected(original, observer, "Candidate chunk unavailable");
        String location = biome.biomeName + " (" + biome.biomeID + ")";
        BiomeEcology family = BiomeEcology.family(biome.biomeID);
        if (world.worldProvider.worldType != 0 || family == BiomeEcology.MUSHROOM || family == BiomeEcology.OUTSIDE_OVERWORLD)
            return rejected(original, observer, location + ": protected habitat");

        int y = MathHelper.floor_double(original.boundingBox.minY);
        boolean underground = subterranean(world, x, y, z);
        location += underground ? ", underground" : ", surface or built";
        boolean bonus = roll >= ordinaryBudget + SABOTEUR_TICKETS;
        if (bonus && !underground) return rejected(original, observer, location + ": outside underground bonus");
        boolean saboteur = !bonus && roll >= ordinaryBudget;
        // Keep the previous five incidental Saboteur tickets, plus 106 conditional industrial tickets.
        if (saboteur && roll >= ordinaryBudget + 5 && !nearIndustry(original))
            return rejected(original, observer, location + ": no exposed industry");
        int role = saboteur ? 4 : new BiomeEcology.Profile(world, x, z, biome).choose(world.rand);

        List<?> nearby = world.getEntitiesWithinAABB(TacticalHostile.class, original.boundingBox.expand(32, 16, 32));
        int alive = 0;
        for (Object entity : nearby) {
            TacticalHostile mob = (TacticalHostile) entity;
            if (!mob.isEntityAlive()) continue;
            if (++alive >= 5 || (role == 3 && mob instanceof EntityTank)
                    || (role == 4 && mob instanceof EntitySaboteur))
                return rejected(original, observer, location + ": local Enderkin cap");
        }
        TacticalHostile replacement;
        switch (role) {
            case 0: replacement = new EntitySkirmisher(world); break;
            case 1: replacement = new EntityBreacher(world); break;
            case 2: replacement = new EntityThief(world); break;
            case 3: replacement = new EntityTank(world); break;
            default: replacement = new EntitySaboteur(world); break;
        }
        replacement.setLocationAndAngles(original.posX, original.boundingBox.minY, original.posZ, original.rotationYaw, 0);
        // The vanilla candidate already passed the light roll at these feet coordinates.
        if (!EnderkinBlink.loaded(world, replacement.boundingBox)
                || !EnderkinBlink.floor(world, replacement.boundingBox) || !EnderkinBlink.clear(replacement, replacement.boundingBox)
                || !world.checkIfAABBIsClear(replacement.boundingBox))
            return rejected(original, observer, location + ": replacement clearance");
        if (observer != null) observer.accept(true, location + ": " + replacement.getClass().getSimpleName().substring(6));
        return replacement;
    }

    /** The native spawn hook has already validated lighting and population for these feet coordinates. */
    private static boolean subterranean(World world, int x, int y, int z) {
        if (world.getHeightValue(x, z) - y < 6 || !naturalGround(world.getBlockId(x, y - 1, z))) return false;
        int cover = 0;
        for (int roof = y + 2; roof <= y + 12 && roof < world.getHeight(); ++roof) {
            int block = world.getBlockId(x, roof, z);
            if (naturalGround(block)) {
                if (++cover == 3) return true;
            } else {
                cover = 0;
                if (world.getBlockMaterial(x, roof, z).blocksMovement()) return false;
            }
        }
        return false;
    }

    private static boolean naturalGround(int id) {
        return id == Block.stone.blockID || id == Block.dirt.blockID || id == Block.grass.blockID
                || id == Block.gravel.blockID || id == Block.sand.blockID || id == Block.sandStone.blockID
                || id == Block.oreCoal.blockID || id == Block.oreIron.blockID || id == Block.oreGold.blockID
                || id == Block.oreDiamond.blockID || id == Block.oreLapis.blockID
                || id == Block.oreRedstone.blockID || id == Block.oreRedstoneGlowing.blockID;
    }

    private static EntityLiving rejected(EntityLiving original, BiConsumer<Boolean, String> observer, String reason) {
        if (observer != null) observer.accept(false, reason);
        return original;
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
                    if ((tile instanceof SabotageTarget || IndustrialSabotage.transportSignal(tile))
                            && exposed(candidate, tile, cx, cz)) return true;
                    if (TransportTheft.exposedSignal(tile,
                            Vec3D.createVector(candidate.posX, candidate.posY + 0.8D, candidate.posZ))) return true;
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
