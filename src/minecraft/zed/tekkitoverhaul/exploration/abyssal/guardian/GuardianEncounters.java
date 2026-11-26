package zed.tekkitoverhaul.exploration.abyssal.guardian;

import java.util.Random;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.AbyssalWorldgen;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites.Site;

/** Separates finite site slots from the occasional solitary open-water encounter. */
public final class GuardianEncounters {
    private static final int[][] ALTERNATIVES = {{0,0,0}, {1,0,0}, {-1,0,0}, {0,1,0}, {0,0,1}, {0,0,-1}};

    private GuardianEncounters() {}

    public static void register() {
        ModLoader.registerEntityID(EntityAbyssalGuardian.class, EntityAbyssalGuardian.NAME, ModLoader.getUniqueEntityId());
        ModLoader.addLocalization("entity." + EntityAbyssalGuardian.NAME + ".name", "Abyssal Guardian");
    }

    public static void tick(World world, EntityPlayer player) {
        if (!ObservatorySites.supported(world) || player == null || player.worldObj != world
                || !player.isEntityAlive() || player.capabilities.disableDamage || world.difficultySetting == 0
                || world.getWorldTime() % 20 != 0) return;
        int x = SwimRoute.floor(player.posX), z = SwimRoute.floor(player.posZ);
        Site site = ObservatorySites.nearby(world, x, z);
        if (site != null && site.excludesWild(x, z)) {
            if (site.ready(world)) materialize(world, player, site);
        } else if (world.getWorldTime() % 320 == 0) wild(world, player, x, z);
    }

    private static void materialize(World world, EntityPlayer player, Site site) {
        for (int slot = 0; slot < site.count(); slot++) {
            if (site.consumed(slot)) continue;
            int[] position = site.home(slot);
            double distance = player.getDistanceSq(position[0] + 0.5, position[1], position[2] + 0.5);
            if (distance < 8 * 8 || distance > 29 * 29) continue;
            EntityAbyssalGuardian actor = new EntityAbyssalGuardian(world);
            actor.home(position, site, slot);
            for (int[] offset : ALTERNATIVES) {
                actor.setPosition(position[0] + 0.5 + offset[0], position[1] + 0.08 + offset[1],
                        position[2] + 0.5 + offset[2]);
                if (!SwimRoute.canInsert(actor)) continue;
                if (site.reserve(slot) && !world.spawnEntityInWorld(actor)) site.finish(slot, false);
                return;
            }
        }
    }

    private static void wild(World world, EntityPlayer player, int x, int z) {
        if (player.posY > 59 || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)
                || !AbyssalWorldgen.isTrench(world.getBiomeGenForCoords(x, z).biomeID)) return;
        Random random = new Random(world.getSeed() ^ (world.getWorldTime() * 0x9E3779B97F4A7C15L)
                ^ ((long) x << 32) ^ (z & 0xffffffffL));
        if (random.nextInt(7) != 0) return;
        int wild = 0;
        for (Object entry : world.getEntitiesWithinAABB(EntityAbyssalGuardian.class, player.boundingBox.expand(48, 48, 48))) {
            EntityAbyssalGuardian actor = (EntityAbyssalGuardian) entry;
            if (actor.isEntityAlive() && !actor.siteOwned() && ++wild >= 2) return;
        }
        if (world.countEntities(net.minecraft.src.IMob.class) >= 70) return;
        EntityAbyssalGuardian actor = new EntityAbyssalGuardian(world);
        for (int attempt = 0; attempt < 6; attempt++) {
            int xx = x + random.nextInt(53) - 26, zz = z + random.nextInt(53) - 26;
            int yy = Math.min(42, SwimRoute.floor(player.posY) - 4 + random.nextInt(13) - 6);
            double distance = player.getDistanceSq(xx + 0.5, yy, zz + 0.5);
            if (distance < 16 * 16 || distance > 34 * 34 || !world.getChunkProvider().chunkExists(xx >> 4, zz >> 4)
                    || !AbyssalWorldgen.isTrench(world.getBiomeGenForCoords(xx, zz).biomeID)) continue;
            Site site = ObservatorySites.nearby(world, xx, zz);
            if (site != null && site.excludesWild(xx, zz)) continue;
            actor.setPosition(xx + 0.5, yy + 0.08, zz + 0.5);
            if (!SwimRoute.canInsert(actor)) continue;
            actor.home(new int[] {xx, yy, zz}, null, -1);
            world.spawnEntityInWorld(actor);
            return;
        }
    }

    static int engagements(EntityPlayer player) {
        int count = 0;
        for (Object entry : player.worldObj.getEntitiesWithinAABB(EntityAbyssalGuardian.class,
                player.boundingBox.expand(33, 33, 33))) {
            if (((EntityAbyssalGuardian) entry).engages(player)) count++;
        }
        return count;
    }
}
