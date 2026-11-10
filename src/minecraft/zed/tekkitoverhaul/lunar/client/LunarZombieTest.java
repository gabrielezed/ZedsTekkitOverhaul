package zed.tekkitoverhaul.lunar.client;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityZombie;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;
import zed.tekkitoverhaul.lunar.LunarEvent;
import zed.tekkitoverhaul.lunar.LunarService;

/** Temporary disposable-save fixture for manual calendar changes. */
final class LunarZombieTest implements LunarService.Listener {
    static final String MARKER = "zto-lunar-zombie-test.enabled";
    private static final int BURST_SIZE = 120;
    private final Minecraft game;

    LunarZombieTest(Minecraft game) { this.game = game; }

    static boolean enabled(World world) {
        if (!Boolean.getBoolean("zto.dev")) return false;
        File datum = world.getSaveHandler().getMapFileFromName(LunarWorldState.KEY);
        return datum != null && new File(datum.getParentFile(), MARKER).isFile();
    }

    @Override
    public void onLunarEvent(LunarEvent event) {
        if (event.cause != LunarEvent.Cause.ADMIN_COMMAND || event.before == null
                || event.before.isFullMoon() || !event.after.isFullMoon() || !event.after.schedulingEligible) return;

        World world = game.theWorld;
        EntityPlayer player = game.thePlayer;
        if (world == null || world.isRemote || world.difficultySetting == 0 || player == null || !enabled(world)) return;

        int originX = MathHelper.floor_double(player.posX);
        int originZ = MathHelper.floor_double(player.posZ);
        int spawned = 0;
        for (int attempt = 0; attempt < BURST_SIZE * 5 && spawned < BURST_SIZE; attempt++) {
            int dx = world.rand.nextInt(25) - 12;
            int dz = world.rand.nextInt(25) - 12;
            int distance = dx * dx + dz * dz;
            if (distance < 25 || distance > 144) continue;
            int x = originX + dx;
            int z = originZ + dz;
            if (!world.getChunkProvider().chunkExists(x >> 4, z >> 4)) continue;
            int y = world.getHeightValue(x, z);
            if (y < 1 || y >= 254 || !world.getBlockMaterial(x, y - 1, z).isSolid()) continue;

            EntityZombie zombie = new EntityZombie(world);
            zombie.setPositionAndRotation(x + 0.5, y, z + 0.5, world.rand.nextFloat() * 360, 0);
            if (!world.getCollidingBoundingBoxes(zombie, zombie.boundingBox).isEmpty()
                    || !world.checkIfAABBIsClear(zombie.boundingBox)) continue;
            if (world.spawnEntityInWorld(zombie)) spawned++;
        }
        ModLoader.getLogger().info("[ZTO lunar test] Full-moon admin transition spawned " + spawned + " zombies.");
    }
}
