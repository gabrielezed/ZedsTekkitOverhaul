package zed.tekkitoverhaul.exploration.underwater;

import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EnumArmorMaterial;
import net.minecraft.src.ItemArmor;
import net.minecraft.src.ItemStack;
import net.minecraft.src.forge.ArmorProperties;
import net.minecraft.src.forge.IArmorTextureProvider;
import net.minecraft.src.forge.ISpecialArmor;

class DivingArmor extends ItemArmor implements IArmorTextureProvider, ISpecialArmor {
    private final String texture;

    DivingArmor(int id, int armorType, String name) {
        super(id, EnumArmorMaterial.CLOTH, 0, armorType);
        setItemName("zto." + name);
        setMaxDamage(0);
        texture = UnderwaterRegistry.TEXTURES + name + "_armor.png";
        setIconIndex(UnderwaterRegistry.sprite("/gui/items.png", name));
    }

    @Override
    public int getItemEnchantability() { return 0; }

    @Override
    public String getArmorTextureFile(ItemStack stack) { return texture; }

    @Override
    public ArmorProperties getProperties(EntityLiving wearer, ItemStack armor, DamageSource source,
                                         double damage, int slot) {
        return new ArmorProperties(0, 0.0D, 0);
    }

    @Override
    public int getArmorDisplay(EntityPlayer wearer, ItemStack armor, int slot) { return 0; }

    @Override
    public void damageArmor(EntityLiving wearer, ItemStack armor, DamageSource source, int damage, int slot) {}
}
