package zed.tekkitoverhaul.assault;

import java.util.Random;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Chunk;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
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
    static final int LOCAL_CAP = 28;
    static final int INNER_RADIUS = 12;
    private static final double BLOCK_CENTER_ERROR = Math.sqrt(0.5);
    private final int outerRadius;
    private final Random random = new Random();
    String rejection;

    SpawnController(int outerRadius) {
        this.outerRadius = outerRadius;
    }

    int localHostiles(World world, EntityPlayer player) {
        int alive = 0;
        for (Object entity : world.getEntitiesWithinAABB(IMob.class, player.boundingBox.expand(32, 24, 32))) {
            if (((Entity) entity).isEntityAlive()) alive++;
        }
        return alive;
    }

    EntityMob trySpawn(World world, EntityPlayer player, boolean skeleton) {
        // Inset the continuous annulus so rounding to a block center cannot cross either edge.
        double inner = INNER_RADIUS + BLOCK_CENTER_ERROR;
        double outer = outerRadius - BLOCK_CENTER_ERROR;
        double radius = Math.sqrt(inner * inner + random.nextDouble() * (outer * outer - inner * inner));
        double angle = random.nextDouble() * 2 * Math.PI;
        int x = MathHelper.floor_double(player.posX + radius * Math.cos(angle));
        int z = MathHelper.floor_double(player.posZ + radius * Math.sin(angle));
        if (x < -29999998 || x >= 29999998 || z < -29999998 || z >= 29999998
                || !world.checkChunksExist(x - 2, 0, z - 2, x + 2, 255, z + 2)) {
            return reject("UNLOADED_CHUNK");
        }
        Chunk chunk = world.getChunkFromBlockCoords(x, z);
        int y = world.getHeightValue(x, z);
        if (y < 1 || y > 253 || Math.abs(y - player.boundingBox.minY) > 8) return reject("SURFACE_OUT_OF_REACH");
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
        AxisAlignedBB box = mob.boundingBox;
        if (!world.checkIfAABBIsClear(box) || !world.getEntitiesWithinAABB(EntityLiving.class, box).isEmpty()) {
            return reject("OCCUPIED");
        }
        if (!world.getCollidingBoundingBoxes(mob, box).isEmpty() || world.isAnyLiquid(box)) {
            return reject("COLLISION_OR_LIQUID");
        }
        if (!mob.getCanSpawnHere()) return reject("VANILLA_LIGHT");
        if (!world.spawnEntityInWorld(mob)) return reject("INSERTION_REJECTED");
        return mob;
    }

    private EntityMob reject(String reason) {
        rejection = reason;
        return null;
    }
}
