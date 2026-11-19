package zed.tekkitoverhaul.settlement;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkPosition;
import net.minecraft.src.TileEntity;

/** Read-only adapter for the pinned IC2 1.97 client. Block IDs come from the live registry. */
final class Ic2SiteAdapter {
    static final int UNSUPPORTED = -1, CABLE = 9;
    static final int GENERATOR = 5, STORAGE = 6;
    private static final String PACKAGE = "ic2.common.";
    private static final String IC2_HASH = "3650c595ec9b1bd5b87bafac43640d2e76a0439ee70276fb9e9ccdf6337c0845";
    private static final String[] TILE_NAMES = {"ElecFurnace", "Macerator", "Extractor", "Compressor",
            "Generator", "ElectricBatBox", "ElectricMFE", "ElectricMFSU", "Cable"};
    private static final String[] CONTAINER_NAMES = {"ElectricMachine", "BaseGenerator", "ElectricBlock"};
    private static final String[] BLOCK_NAMES = {"Machine", "Generator", "Electric", "Cable", "Machine2"};
    private static Ic2SiteAdapter cached;

    private final byte[] families = new byte[Block.blocksList.length];
    private final Class<?>[] tiles = new Class<?>[TILE_NAMES.length + 1];
    private final Field[] containers = new Field[CONTAINER_NAMES.length];
    private Field cableType, color, foamed;
    private Method facing;
    private boolean available;

    static Ic2SiteAdapter get() {
        if (cached == null) cached = new Ic2SiteAdapter();
        return cached;
    }

    private Ic2SiteAdapter() {
        boolean detected = false;
        for (Block block : Block.blocksList) {
            if (block == null) continue;
            String name = block.getClass().getName();
            if (!name.startsWith(PACKAGE + "Block")) continue;
            for (int i = 0; i < BLOCK_NAMES.length; i++) {
                if ((PACKAGE + "Block" + BLOCK_NAMES[i]).equals(name)) {
                    families[block.blockID] = (byte) (i + 1);
                    detected = true;
                    break;
                }
            }
        }
        if (!detected) return;
        ClassLoader loader = Block.class.getClassLoader();
        try {
            if (!matchesRuntime(loader)) return;
            for (int i = 0; i < TILE_NAMES.length; i++)
                tiles[i + 1] = Class.forName(PACKAGE + "TileEntity" + TILE_NAMES[i], false, loader);
            for (int i = 0; i < CONTAINER_NAMES.length; i++)
                containers[i] = Class.forName(PACKAGE + "Container" + CONTAINER_NAMES[i], false, loader).getField("tileEntity");
            cableType = tiles[CABLE].getField("cableType");
            color = tiles[CABLE].getField("color");
            foamed = tiles[CABLE].getField("foamed");
            facing = tiles[STORAGE].getMethod("getFacing");
            available = true;
        } catch (Exception unavailable) {
            // Keep the family IDs so known industrial blocks stay outside the structural flood fill.
        } catch (LinkageError unavailable) {
            // Optional classes may be incomplete.
        }
    }

    private static boolean matchesRuntime(ClassLoader loader) throws Exception {
        InputStream stream = loader.getResourceAsStream("mod_IC2.class");
        if (stream == null) return false;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[4096];
            int count;
            while ((count = stream.read(bytes)) != -1) digest.update(bytes, 0, count);
            return IC2_HASH.equals(String.format("%064x", new java.math.BigInteger(1, digest.digest())));
        } finally {
            stream.close();
        }
    }

    boolean available() { return available; }

    boolean family(int id) { return id > 0 && id < families.length && families[id] != 0; }

    boolean inspect(int id) { return available && family(id) && families[id] != 5; }

    int type(int id, int meta, TileEntity tile) {
        int type;
        switch (families[id]) {
            case 1: type = meta >= 2 && meta <= 5 ? meta - 1 : 0; break;
            case 2: type = meta == 0 ? GENERATOR : 0; break;
            case 3: type = meta <= 2 ? STORAGE + meta : 0; break;
            case 4: type = meta <= 1 ? CABLE : 0; break;
            default: type = 0;
        }
        return type != 0 && tile != null && tiles[type] == tile.getClass() ? type : UNSUPPORTED;
    }

    int cableColor(int meta, TileEntity tile) {
        try {
            int value = color.getShort(tile);
            if (cableType.getShort(tile) == meta && foamed.getByte(tile) == 0 && value >= 0 && value <= 16)
                return value;
        } catch (IllegalAccessException ignored) {
        }
        return UNSUPPORTED;
    }

    int outputFace(int type, TileEntity tile) {
        if (type < STORAGE) return -1;
        try {
            int value = ((Number) facing.invoke(tile)).intValue();
            if (value >= 0 && value < 6) return value;
        } catch (ReflectiveOperationException ignored) {
        }
        return -1;
    }

    TileEntity openedTile(int id, Object container) {
        if (!inspect(id) || families[id] > 3 || container == null) return null;
        Field field = containers[families[id] - 1];
        if (field.getDeclaringClass() != container.getClass()) return null;
        try {
            TileEntity tile = (TileEntity) field.get(container);
            if (tile != null) {
                for (int type = 1; type < CABLE; type++)
                    if (tiles[type] == tile.getClass()) return tile;
            }
            return null;
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    /** The native world getter can create a missing tile. Inspect the loaded map directly. */
    static TileEntity loadedTile(Chunk chunk, int x, int y, int z) {
        TileEntity tile = (TileEntity) chunk.chunkTileEntityMap.get(new ChunkPosition(x & 15, y, z & 15));
        return tile != null && !tile.isInvalid() ? tile : null;
    }
}
