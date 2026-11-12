import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.src.CompressedStreamTools;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagDouble;
import net.minecraft.src.NBTTagFloat;
import net.minecraft.src.NBTTagList;

/** Creates a fresh 1.2.5 Anvil save header. The Tekkit client generates terrain when opened. */
public final class AssaultPlaytestSave {
    private static final long SEED = -7184827518270584220L;
    private static final int ANVIL_VERSION = 19133;
    private static final int START_TICK = 10800;
    private static final int SPAWN_X = 34;
    private static final int SPAWN_Y = 65;
    private static final int SPAWN_Z = 215;

    public static void main(String[] args) throws Exception {
        Path target = Paths.get(args[0]);
        Files.createDirectory(target);
        Path data = Files.createDirectory(target.resolve("data"));
        Files.write(data.resolve("zto_lunar.dat"), CompressedStreamTools.compress(lunarState(Integer.parseInt(args[2]))));
        Files.write(target.resolve("level.dat"), CompressedStreamTools.compress(worldState(args[1])));
        System.out.println("Created " + args[1] + " at " + target);
    }

    private static NBTTagCompound worldState(String name) {
        NBTTagCompound world = new NBTTagCompound();
        world.setString("LevelName", name);
        world.setInteger("version", ANVIL_VERSION);
        world.setLong("RandomSeed", SEED);
        world.setString("generatorName", "default");
        world.setInteger("generatorVersion", 1);
        world.setInteger("GameType", 0);
        world.setBoolean("MapFeatures", true);
        world.setInteger("SpawnX", SPAWN_X);
        world.setInteger("SpawnY", SPAWN_Y);
        world.setInteger("SpawnZ", SPAWN_Z);
        world.setLong("Time", START_TICK);
        world.setLong("LastPlayed", System.currentTimeMillis());
        world.setCompoundTag("Player", playerState());

        NBTTagCompound root = new NBTTagCompound();
        root.setCompoundTag("Data", world);
        return root;
    }

    private static NBTTagCompound playerState() {
        NBTTagCompound player = new NBTTagCompound();
        // The legacy player stores Y above the feet. Leave clearance above the surface.
        player.setTag("Pos", vector(SPAWN_X + 0.5, SPAWN_Y + 1.72, SPAWN_Z + 0.5));
        player.setTag("Motion", vector(0, 0, 0));
        NBTTagList rotation = new NBTTagList();
        rotation.appendTag(new NBTTagFloat("", 0));
        rotation.appendTag(new NBTTagFloat("", 0));
        player.setTag("Rotation", rotation);
        player.setShort("Health", (short) 20);
        player.setShort("Air", (short) 300);
        player.setShort("Fire", (short) -20);
        player.setInteger("foodLevel", 20);
        player.setFloat("foodSaturationLevel", 5);
        player.setTag("Inventory", starterInventory());
        return player;
    }

    private static NBTTagCompound lunarState(int phase) {
        NBTTagCompound state = new NBTTagCompound();
        state.setInteger("Schema", 1);
        state.setLong("Baseline", 0);
        state.setLong("Adjustment", phase);
        state.setLong("Watermark", START_TICK);
        NBTTagCompound root = new NBTTagCompound();
        root.setCompoundTag("data", state);
        return root;
    }

    private static NBTTagList vector(double x, double y, double z) {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagDouble("", x));
        list.appendTag(new NBTTagDouble("", y));
        list.appendTag(new NBTTagDouble("", z));
        return list;
    }

    private static NBTTagList starterInventory() {
        NBTTagList inventory = new NBTTagList();
        item(inventory, 0, 272, 1);  // Stone sword
        item(inventory, 1, 261, 1);  // Bow
        item(inventory, 2, 262, 24); // Arrows
        item(inventory, 3, 297, 12); // Bread
        item(inventory, 4, 50, 32);  // Torches
        item(inventory, 5, 4, 64);   // Cobblestone
        item(inventory, 6, 274, 1);  // Stone pickaxe
        item(inventory, 7, 324, 1);  // Wooden door
        item(inventory, 100, 301, 1); // Leather boots
        item(inventory, 101, 300, 1); // Leather leggings
        item(inventory, 102, 299, 1); // Leather tunic
        item(inventory, 103, 298, 1); // Leather cap
        return inventory;
    }

    private static void item(NBTTagList inventory, int slot, int id, int count) {
        NBTTagCompound item = new NBTTagCompound();
        item.setByte("Slot", (byte) slot);
        item.setShort("id", (short) id);
        item.setByte("Count", (byte) count);
        item.setShort("Damage", (short) 0);
        inventory.appendTag(item);
    }
}
