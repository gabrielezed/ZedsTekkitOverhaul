package zed.tekkitoverhaul.exploration.abyssal.guardian;

import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EnumSkyBlock;
import net.minecraft.src.World;

public final class AquaticDevelopment {
    private static final Map<EntityLiving, Boolean> MANUAL = new WeakHashMap<EntityLiving, Boolean>();

    public enum Species {
        FANGFISH("Fangfish"), ZOMBIE("Diver Zombie"), SKELETON("Diver Skeleton"), ENDERKIN("Abyssal Enderkin"),
        CUSTODIAN("Abyssal Custodian");

        public final String label;

        Species(String label) { this.label = label; }

        public Species next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private AquaticDevelopment() {}

    public static boolean disposable(World world) {
        if (!Boolean.getBoolean("zto.dev") || !local(world)) return false;
        File data = world.getSaveHandler().getMapFileFromName("ZTOObservatories");
        return data != null && new File(data.getParentFile().getParentFile(), ".zto-disposable-test").isFile();
    }

    public static String spawn(World world, EntityPlayer player, Species species) {
        if (!disposable(world) || player == null || player.worldObj != world || world.difficultySetting == 0)
            return "Spawn requires a marked local fixture and hostile difficulty";
        int count = 0;
        for (Object entry : world.getEntitiesWithinAABB(EntityLiving.class, player.boundingBox.expand(16, 16, 16)))
            if (manual((EntityLiving) entry) && ++count >= 6) return "Six manual actors are already nearby";

        EntityLiving actor;
        switch (species) {
            case FANGFISH: actor = new EntityAbyssalGuardian(world); break;
            case ZOMBIE: actor = new EntityDiverZombie(world); break;
            case SKELETON: actor = new EntityDiverSkeleton(world); break;
            case ENDERKIN: actor = new EntityAbyssalEnderkin(world); break;
            case CUSTODIAN: actor = new EntityAbyssalCustodian(world); break;
            default: throw new AssertionError(species);
        }
        double yaw = Math.toRadians(player.rotationYaw);
        for (int i = 0; i < 8; i++) {
            int range = 5 + (i % 4);
            double x = player.posX - Math.sin(yaw) * range;
            double z = player.posZ + Math.cos(yaw) * range;
            double y = player.boundingBox.minY - i / 4;
            actor.setPosition(x, y, z);
            if (insertManual(actor)) return "Spawned manual " + species.label;
        }
        return "No clear submerged position in front of player";
    }

    /** Ordinary Creative eggs and the debug fixture share temporary, independently owned actors. */
    public static boolean insertManual(EntityLiving actor) {
        if (actor.worldObj.isRemote || !SwimRoute.canInsert(actor)) return false;
        int x = SwimRoute.floor(actor.posX), y = SwimRoute.floor(actor.boundingBox.minY), z = SwimRoute.floor(actor.posZ);
        if (actor instanceof EntityAbyssalGuardian)
            ((EntityAbyssalGuardian) actor).home(new int[] {x, y, z}, null, -1);
        if (actor instanceof EntityAbyssalEnderkin) ((EntityAbyssalEnderkin) actor).manualHome(x, y, z);
        if (actor instanceof EntityAbyssalCustodian) ((EntityAbyssalCustodian) actor).manualHome();
        MANUAL.put(actor, Boolean.TRUE);
        if (actor.worldObj.spawnEntityInWorld(actor)) return true;
        MANUAL.remove(actor);
        return false;
    }

    static boolean manual(EntityLiving actor) { return MANUAL.containsKey(actor); }

    public static String inspect(World world, EntityPlayer player) {
        if (!Boolean.getBoolean("zto.dev") || !local(world)
                || player == null || player.worldObj != world) return "";
        EntityLiving nearest = null;
        double distance = 16 * 16;
        for (Object object : world.getEntitiesWithinAABB(EntityLiving.class, player.boundingBox.expand(16, 16, 16))) {
            EntityLiving actor = (EntityLiving) object;
            if (!(actor instanceof EntityAbyssalEnderkin || actor instanceof EntityAbyssalGuardian
                    || actor instanceof EntityDiverZombie || actor instanceof EntityDiverSkeleton
                    || actor instanceof EntityAbyssalCustodian)) continue;
            double range = actor.getDistanceSqToEntity(player);
            if (actor.isEntityAlive() && range < distance) {
                distance = range;
                nearest = actor;
            }
        }
        int x = SwimRoute.floor(player.posX), y = SwimRoute.floor(player.posY), z = SwimRoute.floor(player.posZ);
        String cell = world.blockExists(x, y, z) ? world.getBlockId(x, y, z) + ":" + world.getBlockMetadata(x, y, z)
                + " | block light " + world.getSavedLightValue(EnumSkyBlock.Block, x, y, z) : "Unloaded cell";
        return nearest == null ? cell : cell + " | " + label(nearest) + " " + Math.round(Math.sqrt(distance))
                + "m " + provenance(nearest);
    }

    private static String label(EntityLiving actor) {
        if (actor instanceof EntityAbyssalCustodian) return Species.CUSTODIAN.label;
        if (actor instanceof EntityAbyssalEnderkin) return Species.ENDERKIN.label;
        if (actor instanceof EntityDiverSkeleton) return Species.SKELETON.label;
        if (actor instanceof EntityDiverZombie) return Species.ZOMBIE.label;
        return Species.FANGFISH.label;
    }

    private static String provenance(EntityLiving actor) {
        if (actor instanceof EntityAbyssalCustodian) {
            EntityAbyssalCustodian boss = (EntityAbyssalCustodian) actor;
            int pose = boss.pose();
            String state = pose >= EntityAbyssalCustodian.ATTACK
                    ? CustodianBalance.Attack.values()[pose - EntityAbyssalCustodian.ATTACK].name()
                    : new String[] {"dormant", "swim", "awaken", "transition"}[pose];
            return (manual(actor) ? "manual" : Long.toHexString(boss.siteIdentity())) + " | "
                    + boss.getHealth() + " HP | phase " + (boss.secondPhase() ? 2 : 1) + " | " + state + " " + boss.poseTick();
        }
        if (manual(actor)) return "manual";
        if (actor instanceof EntityAbyssalEnderkin) {
            EntityAbyssalEnderkin caster = (EntityAbyssalEnderkin) actor;
            return caster.casting() > 0 ? "site aiming" : "site";
        }
        if (actor instanceof EntityAbyssalGuardian) {
            EntityAbyssalGuardian fish = (EntityAbyssalGuardian) actor;
            return (fish.siteOwned() ? "legacy site " : "wild ") + fish.state();
        }
        return actor instanceof EntityDiverSkeleton && ((EntityDiverSkeleton) actor).aiming() ? "wild aiming" : "wild";
    }

    private static boolean local(World world) {
        return world != null && !world.isRemote && world.worldProvider.worldType == 0;
    }
}
