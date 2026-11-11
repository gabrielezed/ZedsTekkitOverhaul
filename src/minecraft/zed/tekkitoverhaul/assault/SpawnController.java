package zed.tekkitoverhaul.assault;

import java.util.Random;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Chunk;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntitySkeleton;
import net.minecraft.src.EntityZombie;
import net.minecraft.src.EnumCreatureType;
import net.minecraft.src.IMob;
import net.minecraft.src.MathHelper;
import net.minecraft.src.SpawnerAnimals;
import net.minecraft.src.World;

/** Samples outdoor, loaded sites and inserts vanilla mobs using their ordinary spawn checks. */
final class SpawnController {
    static final int LOCAL_CAP = 16;
    static final int INNER_RADIUS = 10;
    static final int OUTER_RADIUS = 15;
    static final int ATTEMPTS = 24;
    private final Random random = new Random();
    String rejection;

    boolean belowLocalCap(World world, EntityPlayer player) {
        return world.getEntitiesWithinAABB(IMob.class, player.boundingBox.expand(32, 24, 32)).size() < LOCAL_CAP;
    }

    EntityMob trySpawn(World world, EntityPlayer player, boolean skeleton) {
        int x = MathHelper.floor_double(player.posX) + offset();
        int z = MathHelper.floor_double(player.posZ) + offset();
        double dx = x + 0.5 - player.posX;
        double dz = z + 0.5 - player.posZ;
        double horizontalSquared = dx * dx + dz * dz;
        if (horizontalSquared < INNER_RADIUS * INNER_RADIUS || horizontalSquared > OUTER_RADIUS * OUTER_RADIUS) {
            return reject("OUTSIDE_ANNULUS");
        }
        if (x < -29999998 || x >= 29999998 || z < -29999998 || z >= 29999998
                || !world.checkChunksExist(x - 2, 0, z - 2, x + 2, 255, z + 2)) {
            return reject("UNLOADED_CHUNK");
        }
        Chunk chunk = world.getChunkFromBlockCoords(x, z);
        int y = world.getHeightValue(x, z);
        if (y < 1 || y > 253 || Math.abs(y - player.boundingBox.minY) > 18) return reject("SURFACE_OUT_OF_REACH");
        // Heightmap ignores transparent roofs. Reject every occupied block above the surface too.
        for (int above = y; above < 256; above++) {
            if (chunk.getBlockID(x & 15, above, z & 15) != 0) return reject("NOT_OPEN_TO_SKY");
        }
        if (!SpawnerAnimals.canCreatureTypeSpawnAtLocation(EnumCreatureType.monster, world, x, y, z)) {
            return reject("UNSUITABLE_GROUND");
        }
        if (world.getClosestPlayer(x + 0.5, y, z + 0.5, INNER_RADIUS) != null) return reject("PLAYER_TOO_CLOSE");
        EntityMob mob = skeleton ? new EntitySkeleton(world) : new EntityZombie(world);
        mob.setLocationAndAngles(x + 0.5, y, z + 0.5, random.nextFloat() * 360, 0);
        if (!mob.getCanSpawnHere()) return reject("LIGHT_OR_COLLISION");
        AxisAlignedBB box = mob.boundingBox;
        if (!world.getEntitiesWithinAABB(EntityMob.class, box).isEmpty()) return reject("OCCUPIED");
        if (!world.spawnEntityInWorld(mob)) return reject("INSERTION_REJECTED");
        return mob;
    }

    private int offset() { return random.nextInt(OUTER_RADIUS * 2 + 1) - OUTER_RADIUS; }

    private EntityMob reject(String reason) {
        rejection = reason;
        return null;
    }
}
