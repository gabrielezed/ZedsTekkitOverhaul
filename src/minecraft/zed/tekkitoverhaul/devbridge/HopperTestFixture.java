package zed.tekkitoverhaul.devbridge;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.EntityItem;
import net.minecraft.src.IInventory;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import zed.tekkitoverhaul.hopper.HopperRegistry;
import zed.tekkitoverhaul.hopper.TileHopper;

/** One fixed arena, one owner, one replayable successful operation. Game thread only. */
final class HopperTestFixture {
    private static final int Y = 180;
    private static final int INPUT = 1;
    private static final int CHEST_FREE_SPACE = 2;
    private final String build;
    private final long sessionId = 1 + (new SecureRandom().nextLong() & ((1L << 52) - 1));
    private long worldId;
    private long revision;
    private int arenaX, arenaZ;
    private long runId;
    private TileHopper hopper;
    private TileEntityChest chest;
    private DistillationTestFixture distillation;
    private boolean capacityPrepared;
    private boolean powered;
    private boolean dirty;
    private BridgeProtocol.Query last;
    private String lastReply;

    HopperTestFixture() {
        build = runtimeJarHash();
    }

    void bind(long nextWorldId, World world) {
        if (worldId == nextWorldId) return;
        worldId = nextWorldId;
        if (world != null) {
            arenaX = (world.getWorldInfo().getSpawnX() & ~15) + 8;
            arenaZ = (world.getWorldInfo().getSpawnZ() & ~15) + 8;
        }
        revision = 0;
        runId = 0;
        hopper = null;
        chest = null;
        distillation = null;
        capacityPrepared = false;
        powered = false;
        dirty = false;
        last = null;
        lastReply = null;
    }

    String context(File gameDir, World world, int dimension, long ticks) {
        boolean allowed = marked(gameDir, world, dimension);
        String dropCount = "null";
        if (allowed) {
            try {
                dropCount = Integer.toString(drops(world));
            } catch (BridgeProtocol.Error unavailable) {
                // Context stays available when the arena chunk is unloaded.
            }
        }
        String result = identity() + ",\"drops\":" + dropCount + ",\"allowed\":" + allowed + ",\"dirty\":" + dirty
                + ",\"active\":" + (runId != 0) + ",\"simulationTicks\":" + ticks
                + ",\"modJarSha256\":" + build;
        return result + "}";
    }

    String command(File gameDir, World world, int dimension, long ticks, BridgeProtocol.Query query)
            throws BridgeProtocol.Error {
        String outcome = "INTERNAL_ERROR";
        try {
            if (worldId != query.worldId || sessionId != query.sessionId) {
                throw reject("STALE_WORLD", "The test session or world has changed");
            }
            if (!marked(gameDir, world, dimension)) {
                throw reject("TEST_WORLD_REQUIRED", "An explicitly marked disposable overworld is required");
            }
            if (dirty) throw reject("DIRTY_FIXTURE", "Use a fresh disposable save");
            if (last != null && last.id.equals(query.id) && last.afterSequence == query.afterSequence
                    && last.op.equals(query.op) && last.runId == query.runId && last.count == query.count) {
                outcome = "REPLAY";
                return lastReply;
            }
            if (revision != query.afterSequence) throw reject("STALE_REVISION", "Read the current test context");
            Chunk chunk = BridgeSnapshots.loaded(world, arenaX, arenaZ);
            String details = "";
            if ("distillation.create".equals(query.op)) {
                if (runId != 0) throw reject("FIXTURE_BUSY", "A test run already owns the arena");
                validateArena(world, chunk, false);
                DistillationTestFixture next = new DistillationTestFixture(arenaX, Y, arenaZ);
                dirty = true;
                next.place(world, chunk);
                distillation = next;
                runId = query.runId;
                dirty = false;
            } else if ("fixture.create".equals(query.op)) {
                if (runId != 0) throw reject("FIXTURE_BUSY", "A test run already owns the arena");
                if (HopperRegistry.block == null) throw reject("UNSUPPORTED", "Hopper is unavailable");
                validateArena(world, chunk, false);
                create(world, chunk);
                runId = query.runId;
            } else {
                if (runId != query.runId) throw reject("FOREIGN_FIXTURE", "The run does not own the fixture");
                if (distillation != null) {
                    distillation.validate(chunk);
                    if (drops(world) != 0) throw reject("ARENA_CHANGED", "Item drops are present in the arena");
                    if ("distillation.start".equals(query.op)) {
                        details = distillation.start();
                    } else if ("distillation.collect".equals(query.op)) {
                        details = distillation.collect();
                    } else if ("fixture.destroy".equals(query.op)) {
                        dirty = true;
                        distillation.remove(world);
                        validateArena(world, chunk, false);
                        distillation = null;
                        runId = 0;
                        dirty = false;
                    } else {
                        throw reject("UNSUPPORTED", "Operation does not apply to this fixture");
                    }
                } else {
                if (query.op.startsWith("distillation.")) throw reject("UNSUPPORTED", "Operation does not apply to this fixture");
                validateArena(world, chunk, true);
                if ("fixture.reset".equals(query.op)) {
                    clear(hopper);
                    clear(chest);
                    hopper.setTransferCooldown(0);
                    capacityPrepared = false;
                } else if ("hopper.prepare_capacity".equals(query.op)) {
                    if (!empty(hopper) || !empty(chest)) throw reject("INPUT_PRESENT", "Reset the fixture before filling");
                    int lastSlot = chest.getSizeInventory() - 1;
                    int limit = chest.getInventoryStackLimit();
                    for (int i = 0; i <= lastSlot; i++) {
                        chest.setInventorySlotContents(i, new ItemStack(INPUT, i == lastSlot ? limit - CHEST_FREE_SPACE : limit, 0));
                    }
                    capacityPrepared = true;
                } else if ("hopper.power_on".equals(query.op) || "hopper.power_off".equals(query.op)) {
                    setPower(world, chunk, "hopper.power_on".equals(query.op));
                } else if ("test.input".equals(query.op)) {
                    if (!empty(hopper) || (!capacityPrepared && !empty(chest))) {
                        throw reject("INPUT_PRESENT", "Reset the fixture before seeding");
                    }
                    hopper.setInventorySlotContents(0, new ItemStack(INPUT, query.count, 0));
                } else {
                    destroy(world);
                }
                }
            }
            revision++;
            last = query;
            lastReply = identity() + ",\"simulationTicks\":" + ticks + details + "}";
            outcome = "OK";
            return lastReply;
        } catch (BridgeProtocol.Error failure) {
            outcome = failure.code;
            throw failure;
        } catch (RuntimeException failure) {
            dirty = true;
            throw failure;
        } finally {
            ModLoader.getLogger().info("[ZTO] Test op=" + query.op + " world=" + worldId
                    + " run=" + query.runId + " simulationTicks=" + ticks + " result=" + outcome);
        }
    }

    private String identity() {
        return "{\"worldId\":" + worldId + ",\"sessionId\":" + sessionId + ",\"revision\":" + revision
                + ",\"runId\":" + runId + ",\"fixture\":\"" + (distillation == null ? "hopper_chest" : "distillation") + "\",\"dimension\":0"
                + ",\"x\":" + arenaX + ",\"y\":" + Y + ",\"z\":" + arenaZ + ",\"chestY\":" + (Y - 1)
                + ",\"inputItemId\":" + INPUT + ",\"powerX\":" + (arenaX + 1);
    }

    private void validateArena(World world, Chunk chunk, boolean owned) throws BridgeProtocol.Error {
        for (int x = arenaX - 2; x <= arenaX + 2; x++) {
            for (int z = arenaZ - 2; z <= arenaZ + 2; z++) {
                for (int y = Y - 3; y <= Y + 3; y++) {
                    int expected = 0;
                    if (owned && x == arenaX && z == arenaZ) {
                        if (y == Y) expected = HopperRegistry.block.blockID;
                        if (y == Y - 1) expected = Block.chest.blockID;
                    }
                    if (owned && powered && x == arenaX + 1 && z == arenaZ) {
                        if (y == Y) expected = Block.torchRedstoneActive.blockID;
                        if (y == Y - 1) expected = Block.stone.blockID;
                    }
                    if (chunk.getBlockID(x & 15, y, z & 15) != expected
                            || (expected == Block.torchRedstoneActive.blockID
                                && chunk.getBlockMetadata(x & 15, y, z & 15) != 5)
                            || ((expected == 0 || expected == Block.stone.blockID
                                 || expected == Block.torchRedstoneActive.blockID)
                                && BridgeSnapshots.tile(chunk, x, y, z) != null)) {
                        throw reject("ARENA_CHANGED", "The arena is occupied or its blocks have changed");
                    }
                }
            }
        }
        if (owned && (BridgeSnapshots.tile(chunk, arenaX, Y, arenaZ) != hopper
                || BridgeSnapshots.tile(chunk, arenaX, Y - 1, arenaZ) != chest
                || chunk.getBlockMetadata(arenaX & 15, Y, arenaZ & 15) != 0 || !"Auto".equals(hopper.outputModeName())
                || !testContents(hopper, 8) || !testContents(chest, capacityPrepared ? 64 : 8))) {
            throw reject("ARENA_CHANGED", "Fixture ownership, mode or contents changed");
        }
        if (drops(world) != 0) throw reject("ARENA_CHANGED", "Item drops are present in the arena");
    }

    private int drops(World world) throws BridgeProtocol.Error {
        BridgeSnapshots.loaded(world, arenaX, arenaZ);
        return world.getEntitiesWithinAABB(EntityItem.class,
                AxisAlignedBB.getBoundingBoxFromPool(arenaX - 2, Y - 3, arenaZ - 2, arenaX + 3, Y + 4, arenaZ + 3)).size();
    }

    private void create(World world, Chunk chunk) throws BridgeProtocol.Error {
        dirty = true;
        boolean placedChest = world.setBlockWithNotify(arenaX, Y - 1, arenaZ, Block.chest.blockID);
        boolean placedHopper = placedChest && world.setBlockAndMetadataWithNotify(arenaX, Y, arenaZ, HopperRegistry.block.blockID, 0);
        TileEntity lower = BridgeSnapshots.tile(chunk, arenaX, Y - 1, arenaZ);
        TileEntity upper = BridgeSnapshots.tile(chunk, arenaX, Y, arenaZ);
        if (!placedHopper || !(upper instanceof TileHopper) || !(lower instanceof TileEntityChest)) {
            if (placedHopper && upper instanceof TileHopper && empty((TileHopper) upper)) {
                world.setBlockWithNotify(arenaX, Y, arenaZ, 0);
            }
            if (placedChest && lower instanceof TileEntityChest && empty((TileEntityChest) lower)) {
                world.setBlockWithNotify(arenaX, Y - 1, arenaZ, 0);
            }
            validateArena(world, chunk, false);
            dirty = false;
            throw reject("FIXTURE_SETUP_FAILED", "The arena did not produce the expected tiles");
        }
        hopper = (TileHopper) upper;
        chest = (TileEntityChest) lower;
        dirty = false;
    }

    private void destroy(World world) throws BridgeProtocol.Error {
        Chunk chunk = BridgeSnapshots.loaded(world, arenaX, arenaZ);
        setPower(world, chunk, false);
        dirty = true;
        clear(hopper);
        clear(chest);
        if (!world.setBlockWithNotify(arenaX, Y, arenaZ, 0) || !world.setBlockWithNotify(arenaX, Y - 1, arenaZ, 0)) {
            throw reject("DIRTY_FIXTURE", "Fixture removal did not complete");
        }
        validateArena(world, chunk, false);
        hopper = null;
        chest = null;
        capacityPrepared = false;
        runId = 0;
        dirty = false;
    }

    private void setPower(World world, Chunk chunk, boolean enabled) throws BridgeProtocol.Error {
        if (powered == enabled) return;
        dirty = true;
        int x = arenaX + 1;
        if (enabled) {
            boolean support = world.setBlockWithNotify(x, Y - 1, arenaZ, Block.stone.blockID);
            if (!support || !world.setBlockAndMetadataWithNotify(x, Y, arenaZ, Block.torchRedstoneActive.blockID, 5)) {
                if (support && chunk.getBlockID(x & 15, Y, arenaZ & 15) == 0) {
                    world.setBlockWithNotify(x, Y - 1, arenaZ, 0);
                }
                validateArena(world, chunk, true);
                dirty = false;
                throw reject("FIXTURE_SETUP_FAILED", "Could not place the fixture power source");
            }
        } else if (!world.setBlockWithNotify(x, Y, arenaZ, 0) || !world.setBlockWithNotify(x, Y - 1, arenaZ, 0)) {
            throw reject("DIRTY_FIXTURE", "Fixture power source removal did not complete");
        }
        powered = enabled;
        validateArena(world, chunk, true);
        dirty = false;
    }

    private static boolean empty(IInventory inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            if (inventory.getStackInSlot(i) != null) return false;
        }
        return true;
    }

    private static boolean testContents(IInventory inventory, int maxCount) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && (stack.itemID != INPUT || stack.getItemDamage() != 0
                    || stack.hasTagCompound() || stack.stackSize < 1 || stack.stackSize > maxCount)) return false;
        }
        return true;
    }

    private static void clear(IInventory inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            if (inventory.getStackInSlot(i) != null) inventory.setInventorySlotContents(i, null);
        }
    }

    private static boolean marked(File gameDir, World world, int dimension) {
        if (dimension != 0 || world.isRemote || gameDir == null) return false;
        String name = world.getSaveHandler().getSaveDirectoryName();
        if (name == null || !name.matches("ZTO-Test-[a-f0-9]{32}")) return false;
        try {
            Path saves = new File(gameDir, "saves").toPath().toRealPath();
            Path dir = saves.resolve(name);
            if (!dir.equals(dir.toRealPath())) return false;
            Path marker = dir.resolve(".zto-disposable-test");
            if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)) return false;
            byte[] expected = (name + " " + world.getWorldInfo().getSeed() + "\n").getBytes(StandardCharsets.US_ASCII);
            InputStream in = Files.newInputStream(marker, LinkOption.NOFOLLOW_LINKS);
            try {
                for (byte value : expected) {
                    if (in.read() != value) return false;
                }
                return in.read() == -1;
            } finally {
                in.close();
            }
        } catch (IOException failure) {
            return false;
        }
    }

    private static String runtimeJarHash() {
        try {
            File jar = new File(HopperTestFixture.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jar.isFile()) return "null";
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            InputStream in = Files.newInputStream(jar.toPath());
            try {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) hash.update(buffer, 0, count);
            } finally {
                in.close();
            }
            StringBuilder hex = new StringBuilder("\"");
            for (byte value : hash.digest()) hex.append(Integer.toHexString((value & 255) | 256).substring(1));
            return hex.append('"').toString();
        } catch (Exception failure) {
            return "null";
        }
    }

    private static BridgeProtocol.Error reject(String code, String message) {
        return new BridgeProtocol.Error(409, code, message);
    }
}
