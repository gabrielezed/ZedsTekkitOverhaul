package zed.tekkitoverhaul.multiblock;

import net.minecraft.src.World;

/** Legacy MCP 6.2 adapter, keeping StructurePattern free from Minecraft APIs. */
public final class WorldBlockView implements StructurePattern.BlockView {
    private final World world;

    public WorldBlockView(World world) {
        if (world == null) throw new IllegalArgumentException("World cannot be null");
        this.world = world;
    }

    public int getBlockId(int x, int y, int z) {
        return world.getBlockId(x, y, z);
    }

    public int getBlockMetadata(int x, int y, int z) {
        return world.getBlockMetadata(x, y, z);
    }
}
