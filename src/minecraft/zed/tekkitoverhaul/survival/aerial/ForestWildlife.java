package zed.tekkitoverhaul.survival.aerial;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;
import zed.tekkitoverhaul.survival.tactical.BiomeEcology;

/** Bounded ambient placement for the active local Overworld. */
public final class ForestWildlife {
    public static final int LOADED_CAP = 4;
    private final boolean enabled;
    private World world;
    private int untilCheck;
    private int loaded;
    private String status;

    public ForestWildlife(boolean enabled) {
        this.enabled = enabled;
        bind(null);
    }

    public void bind(World next) {
        world = next;
        untilCheck = 100;
        loaded = 0;
        status = enabled ? "Waiting for daylight habitat check" : "Disabled in settings";
    }

    public void tick(EntityPlayer player, boolean paused) {
        if (!enabled || paused || world == null || world.isRemote || world.worldProvider.worldType != 0
                || player == null || player.worldObj != world || !player.isEntityAlive()) return;
        if (--untilCheck > 0) return;
        untilCheck = 200;
        loaded = world.countEntities(EntityMagpie.class);
        if (loaded >= LOADED_CAP) status = "Loaded magpie cap reached";
        else if (!world.isDaytime() || world.isRaining()) status = "Requires dry daylight";
        else if (place(player)) {
            ++loaded;
            untilCheck = 600;
            status = "Placed one magpie";
        } else status = "No loaded forest clearing nearby";
    }

    private boolean place(EntityPlayer player) {
        for (int tries = 0; tries < 6; ++tries) {
            double angle = world.rand.nextDouble() * Math.PI * 2;
            int radius = 18 + world.rand.nextInt(15);
            int x = MathHelper.floor_double(player.posX + Math.cos(angle) * radius);
            int z = MathHelper.floor_double(player.posZ + Math.sin(angle) * radius);
            if (!world.blockExists(x, 0, z) || BiomeEcology.family(world.getBiomeGenForCoords(x, z).biomeID)
                    != BiomeEcology.FOREST) continue;
            int y = world.getHeightValue(x, z);
            if (y <= 0 || Math.abs(y - player.boundingBox.minY) > 10
                    || world.getBlockId(x, y - 1, z) != Block.grass.blockID || !world.canBlockSeeTheSky(x, y, z)) continue;
            EntityMagpie bird = new EntityMagpie(world);
            bird.setLocationAndAngles(x + 0.5, y, z + 0.5, world.rand.nextFloat() * 360, 0);
            if (AerialTerrain.clear(bird, bird.boundingBox) && world.checkIfAABBIsClear(bird.boundingBox)
                    && world.spawnEntityInWorld(bird)) return true;
        }
        return false;
    }

    public int loaded() { return loaded; }

    public String status() { return status; }
}
