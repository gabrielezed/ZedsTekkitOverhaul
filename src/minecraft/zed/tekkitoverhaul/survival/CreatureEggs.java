package zed.tekkitoverhaul.survival;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityList;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;
import zed.tekkitoverhaul.creative.CreativeCategories;

/** Explicit metadata is part of the saved ItemStack contract. */
public final class CreatureEggs extends ItemSurvivalSpawnEgg {
    private static final Variant[] VARIANTS = {
        new Variant("ZTO.AbyssalGuardian", "Abyssal Fangfish", 0x263B3D, 0x46534E, true),
        new Variant("ZTO.DiverZombie", "Diver Zombie", 0x497533, 0x273332, true),
        new Variant("ZTO.DiverSkeleton", "Diver Skeleton", 0xAAAAAA, 0x404E4E, true),
        new Variant("ZTO.AbyssalEnderkin", "Abyssal Enderkin", 0x2A2934, 0xE079FA, true),
        new Variant("ZTO.AbyssalCustodian", "Abyssal Custodian", 0x101015, 0xCC00FA, true),
        new Variant("Giant", "Giant", 0x4E7144, 0x405C77, false),
        new Variant("SnowMan", "Snow Golem", 0xECF3F4, 0xE58B36, false),
        new Variant("VillagerGolem", "Iron Golem", 0xCDCAC2, 0x676E45, false),
        new Variant("EnderDragon", "Ender Dragon", 0x202027, 0xBF7EE2, false),
        new Variant("ZTO.SiegeZombie", "Siege Zombie", 0x425838, 0x846850, false),
        new Variant("ZTO.SiegeSkeleton", "Siege Skeleton", 0xAAA396, 0x5F4439, false)
    };
    private static CreatureEggs item;

    private CreatureEggs(int id) {
        super(id, null, "ztoCreatureEgg", 0, 0);
        setHasSubtypes(true);
    }

    public static void register(int id) {
        if (Item.itemsList[id + 256] != null) throw new IllegalStateException("Creature egg item ID is occupied: " + id);
        item = new CreatureEggs(id);
        CreativeCategories.register("zto.spawn_eggs", "Spawn Eggs", new ItemStack(Item.monsterPlacer, 1, 90),
                new CreativeCategories.Filter() {
                    @Override public boolean includes(ItemStack stack) { return isEgg(stack); }
                });
    }

    public static boolean isEgg(ItemStack stack) {
        int meta = stack.getItemDamage();
        Item kind = stack.getItem();
        return kind == Item.monsterPlacer ? EntityList.entityEggs.containsKey(meta)
                : kind instanceof ItemSurvivalSpawnEgg && ((ItemSurvivalSpawnEgg) kind).validVariant(meta);
    }

    public static List<ItemStack> entries() {
        List<ItemStack> eggs = new ArrayList<ItemStack>();
        for (Object id : EntityList.entityEggs.keySet()) eggs.add(new ItemStack(Item.monsterPlacer, 1, (Integer) id));
        for (Item candidate : Item.itemsList) if (candidate instanceof ItemSurvivalSpawnEgg && candidate != item)
            eggs.add(new ItemStack(candidate));
        if (item != null) for (int meta = 0; meta < VARIANTS.length; meta++)
            if (item.validVariant(meta)) eggs.add(new ItemStack(item, 1, meta));
        return eggs;
    }

    public static void audit() {
        TreeSet<String> supported = new TreeSet<String>();
        for (ItemStack egg : entries()) {
            String name = egg.getItem() == Item.monsterPlacer ? EntityList.getStringFromID(egg.getItemDamage())
                    : ((ItemSurvivalSpawnEgg) egg.getItem()).entityName(egg.getItemDamage());
            if (name != null) supported.add(name);
        }
        TreeSet<String> unsupported = new TreeSet<String>();
        for (Map.Entry<String, Class<? extends Entity>> entry : EntityList.getEntityToClassMapping().entrySet()) {
            Class<?> type = entry.getValue();
            if (type != EntityLiving.class && EntityLiving.class.isAssignableFrom(type)
                    && !Modifier.isAbstract(type.getModifiers()) && !supported.contains(entry.getKey()))
                unsupported.add(entry.getKey() + "=" + type.getName());
        }
        ModLoader.getLogger().info("[ZTO] Spawn eggs cover " + supported + "; unaudited constructors: " + unsupported);
    }

    @Override public String getItemDisplayName(ItemStack stack) {
        Variant variant = variant(stack.getItemDamage());
        return variant == null ? "Unknown Spawn Egg" : "Spawn " + variant.label;
    }

    @Override protected String entityName(int damage) {
        Variant variant = variant(damage);
        return variant != null && EntityList.getEntityToClassMapping().containsKey(variant.name) ? variant.name : null;
    }

    @Override protected boolean aquatic(int damage) {
        Variant variant = variant(damage);
        return variant != null && variant.water;
    }

    @Override public int getColorFromDamage(int damage, int pass) {
        Variant variant = variant(damage);
        return variant == null ? 0xffffff : pass == 0 ? variant.shell : variant.spots;
    }

    @Override protected boolean placementAllowed(ItemStack stack, EntityPlayer player, World world) {
        if (!player.capabilities.isCreativeMode) return false;
        if (stack.getItemDamage() != 8 || world.worldProvider.worldType == 1) return true;
        player.addChatMessage("The Ender Dragon can only be spawned in the End.");
        return false;
    }

    private static Variant variant(int meta) { return meta < 0 || meta >= VARIANTS.length ? null : VARIANTS[meta]; }

    private static final class Variant {
        final String name, label;
        final int shell, spots;
        final boolean water;
        Variant(String name, String label, int shell, int spots, boolean water) {
            this.name = name;
            this.label = label;
            this.shell = shell;
            this.spots = spots;
            this.water = water;
        }
    }
}
