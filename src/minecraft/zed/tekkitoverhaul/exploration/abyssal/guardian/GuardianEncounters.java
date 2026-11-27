package zed.tekkitoverhaul.exploration.abyssal.guardian;

import java.util.Random;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.EntityLiving;
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
        register(EntityAbyssalGuardian.class, EntityAbyssalGuardian.NAME, "Abyssal Fangfish");
        register(EntityDiverZombie.class, EntityDiverZombie.NAME, "Diver Zombie");
        register(EntityDiverSkeleton.class, EntityDiverSkeleton.NAME, "Diver Skeleton");
        register(EntityAbyssalEnderkin.class, EntityAbyssalEnderkin.NAME, "Abyssal Enderkin");
        ModLoader.registerEntityID(DiverArrow.class, "ZTO.DiverArrow", ModLoader.getUniqueEntityId());
    }

    private static void register(Class<? extends EntityLiving> type, String id, String name) {
        ModLoader.registerEntityID(type, id, ModLoader.getUniqueEntityId());
        ModLoader.addLocalization("entity." + id + ".name", name);
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
            EntityLiving actor;
            if (site.revision() == 1) {
                EntityAbyssalGuardian legacy = new EntityAbyssalGuardian(world);
                legacy.home(position, site, slot);
                actor = legacy;
            } else {
                EntityAbyssalEnderkin minion = new EntityAbyssalEnderkin(world);
                minion.home(position, site, slot);
                actor = minion;
            }
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
        if (player.posY > 66 || !ocean(world, x, z) || world.countEntities(net.minecraft.src.IMob.class) >= 70) return;
        boolean trench = AbyssalWorldgen.isTrench(world.getBiomeGenForCoords(x, z).biomeID);
        Random random = new Random(world.getSeed() ^ (world.getWorldTime() * 0x9E3779B97F4A7C15L)
                ^ ((long) x << 32) ^ (z & 0xffffffffL));
        if (random.nextInt(trench ? 7 : 10) != 0) return;
        int wild = 0, fish = 0;
        for (Object entry : world.getEntitiesWithinAABB(EntityLiving.class, player.boundingBox.expand(48, 48, 48))) {
            EntityLiving actor = (EntityLiving) entry;
            if (!actor.isEntityAlive()) continue;
            if (actor instanceof EntityAbyssalGuardian && !((EntityAbyssalGuardian) actor).siteOwned()) {
                fish++;
                wild++;
            } else if (actor instanceof EntityDiverZombie || actor instanceof EntityDiverSkeleton) wild++;
        }
        if (wild >= 4) return;
        boolean predator = fish < 2 && random.nextInt(trench ? 4 : 10) < (trench ? 3 : 1);
        int pack = !predator && wild < 3 && random.nextInt(5) == 0 ? 2 : 1;
        for (int i = 0; i < pack; i++) {
            EntityLiving actor = predator ? new EntityAbyssalGuardian(world)
                    : random.nextInt(3) == 0 ? new EntityDiverSkeleton(world) : new EntityDiverZombie(world);
            for (int attempt = 0; attempt < 6; attempt++) {
                int xx = x + random.nextInt(53) - 26, zz = z + random.nextInt(53) - 26;
                int yy = Math.min(predator ? 42 : 56, SwimRoute.floor(player.posY) - 4 + random.nextInt(13) - 6);
                if (!predator && yy < 28) continue;
                double distance = player.getDistanceSq(xx + 0.5, yy, zz + 0.5);
                if (distance < 16 * 16 || distance > 34 * 34 || !ocean(world, xx, zz)) continue;
                Site site = ObservatorySites.nearby(world, xx, zz);
                if (site != null && site.excludesWild(xx, zz)) continue;
                actor.setPosition(xx + 0.5, yy + 0.08, zz + 0.5);
                if (!SwimRoute.canInsert(actor)) continue;
                if (predator) ((EntityAbyssalGuardian) actor).home(new int[] {xx, yy, zz}, null, -1);
                if (world.spawnEntityInWorld(actor)) break;
            }
        }
    }

    private static boolean ocean(World world, int x, int z) {
        if (!world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return false;
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        return biome == BiomeGenBase.ocean || AbyssalWorldgen.isTrench(biome.biomeID);
    }

    static int casters(EntityPlayer player, boolean casting) {
        int count = 0;
        for (Object entry : player.worldObj.getEntitiesWithinAABB(EntityAbyssalEnderkin.class,
                player.boundingBox.expand(32, 32, 32))) {
            EntityAbyssalEnderkin actor = (EntityAbyssalEnderkin) entry;
            if (actor.engages(player) && (!casting || actor.casting() > 0)) count++;
        }
        return count;
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
