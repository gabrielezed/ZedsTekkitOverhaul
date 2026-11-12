package zed.tekkitoverhaul.survival;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.BlockBed;
import net.minecraft.src.ChunkCoordinates;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EnumStatus;
import net.minecraft.src.World;
import net.minecraft.src.forge.ISleepHandler;

/** Consumes local bed use before the player enters the legacy sleep state. */
public final class BedRespawn implements ISleepHandler {
    @Override
    public EnumStatus sleepInBedAt(EntityPlayer player, int x, int y, int z) {
        World world = player.worldObj;
        if (world.isRemote) return null;
        if (player.isPlayerSleeping() || !player.isEntityAlive()) return EnumStatus.OTHER_PROBLEM;
        if (Math.abs(player.posX - x) > 3 || Math.abs(player.posY - y) > 2 || Math.abs(player.posZ - z) > 3) {
            return EnumStatus.TOO_FAR_AWAY;
        }
        // The vanilla player save stores bed coordinates without a dimension.
        if (world.worldProvider.worldType != 0 || !intactBed(world, x, y, z)
                || BlockBed.getNearestEmptyChunkCoordinates(world, x, y, z, 0) == null) {
            player.addChatMessage("tile.bed.notValid");
            return EnumStatus.OTHER_PROBLEM;
        }
        AxisAlignedBB danger = AxisAlignedBB.getBoundingBoxFromPool(x - 8, y - 5, z - 8, x + 8, y + 5, z + 8);
        if (!world.getEntitiesWithinAABB(EntityMob.class, danger).isEmpty()) return EnumStatus.NOT_SAFE;
        player.setSpawnChunk(new ChunkCoordinates(x, y, z));
        player.addChatMessage("Respawn point set.");
        // OK would cause BlockBed to mark the bed occupied.
        return EnumStatus.OTHER_PROBLEM;
    }

    private static boolean intactBed(World world, int x, int y, int z) {
        if (world.getBlockId(x, y, z) != Block.bed.blockID) return false;
        int head = world.getBlockMetadata(x, y, z);
        if (!BlockBed.isBlockFootOfBed(head)) return false; // The MCP predicate tests the head bit.
        int direction = BlockBed.getDirection(head);
        int[] offset = BlockBed.headBlockToFootBlockMap[direction];
        int footX = x - offset[0];
        int footZ = z - offset[1];
        if (world.getBlockId(footX, y, footZ) != Block.bed.blockID) return false;
        int foot = world.getBlockMetadata(footX, y, footZ);
        return !BlockBed.isBlockFootOfBed(foot) && BlockBed.getDirection(foot) == direction;
    }
}
