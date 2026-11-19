package zed.tekkitoverhaul.survival.mining;

import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;

/** Explicit registry references keep independent ore variants and volatile Nether ores apart. */
final class OreRegistry {
    private final Block redPower = (Block) registry("RedPowerWorld", "blockOres");
    private final Block copper = ic2Ore("copperOre");
    private final Block tin = ic2Ore("tinOre");
    private final Block uranium = ic2Ore("uraniumOre");

    int key(World world, VeinMining.Point point) {
        Block block = Block.blocksList[world.getBlockId(point.x, point.y, point.z)];
        if (block == null) return 0;
        int metadata = world.getBlockMetadata(point.x, point.y, point.z);
        if (block == Block.oreRedstoneGlowing) block = Block.oreRedstone;
        if (block == Block.oreGold || block == Block.oreIron || block == Block.oreCoal || block == Block.oreLapis
                || block == Block.oreDiamond || block == Block.oreRedstone || block == copper || block == tin
                || block == uranium || block == redPower && metadata < 8) return block.blockID << 4 | metadata;
        return 0;
    }

    static boolean redPowerPick(Item item) {
        return item == registry("RedPowerWorld", "itemPickaxeRuby")
                || item == registry("RedPowerWorld", "itemPickaxeEmerald")
                || item == registry("RedPowerWorld", "itemPickaxeSapphire");
    }

    private static Block ic2Ore(String name) {
        Object value = registry("ic2.common.Ic2Items", name);
        if (!(value instanceof ItemStack)) return null;
        int id = ((ItemStack) value).itemID;
        return id > 0 && id < Block.blocksList.length ? Block.blocksList[id] : null;
    }

    private static Object registry(String owner, String field) {
        try {
            return Class.forName(owner, false, OreRegistry.class.getClassLoader()).getField(field).get(null);
        } catch (ReflectiveOperationException unavailable) {
            return null;
        } catch (LinkageError unavailable) {
            return null;
        }
    }
}
