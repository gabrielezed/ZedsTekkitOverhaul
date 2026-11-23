import buildcraft.api.Orientations;
import buildcraft.api.PowerFramework;
import buildcraft.core.DefaultProps;
import buildcraft.energy.BlockOilFlowing;
import buildcraft.energy.BlockOilStill;
import buildcraft.energy.ItemBucketOil;
import buildcraft.energy.PneumaticPowerFramework;
import buildcraft.transport.BlockGenericPipe;
import buildcraft.transport.PipeTransportLiquids;
import buildcraft.transport.TileGenericPipe;
import buildcraft.transport.pipes.PipeLiquidsWood;
import ic2.api.Direction;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.DistillationLayout;
import zed.tekkitoverhaul.distillation.DistillationPartType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.distillation.FractionationProcess;
import zed.tekkitoverhaul.distillation.TileEntityDistillationBoiler;
import zed.tekkitoverhaul.distillation.TileEntityDistillationCondenser;
import zed.tekkitoverhaul.distillation.TileEntityDistillationHeater;
import zed.tekkitoverhaul.distillation.TileEntityDistillationReceiver;

/** Type-zero rendering contract and gameplay checks in the pinned headless pack. */
final class DistillationVisualProbe {
    private static final int X = 294, Y = 12, Z = 294;
    private static final String SHEET = "/zed/textures/distillation_blocks.png";
    private static final String[] TILE_IDS = {
            "ZTODistillationBoiler", "ZTODistillationLVHeater", "ZTODistillationPart",
            "ZTODistillationCondenser", "ZTODistillationReceiver", "ZTODistillationPart"
    };

    static void run(File root, xd world, yw player) throws Exception {
        BufferedImage atlas = ImageIO.read(DistillationVisualProbe.class.getResource(SHEET));
        for (DistillationPartType part : DistillationPartType.values()) {
            int id = id(part);
            pb block = pb.m[id];
            check(id == 3010 + part.index, "stable block ID " + part);
            check(block.d() == 0 && vl.a(block.d()), "native cube item renderer " + part);
            check(yr.e[id] instanceof vd && SHEET.equals(yr.e[id].getTextureFile()), "held item sheet " + part);
            check(!block.isDefaultTexture && SHEET.equals(block.getTextureFile()), "Forge chunk sheet " + part);
            check(block.a(3, 0) == part.index, "inventory front cell " + part);
            for (int side = 0; side < 6; side++) {
                int cell = block.a(side, 0);
                check(cell % 16 == part.index && (atlas.getRGB((cell % 16) * 16 + 7,
                        (cell / 16) * 16 + 7) >>> 24) == 255, "occupied item face " + part + " side " + side);
            }
        }
        verifyRecipes();
        DistillationLayout.Facing[] facings = DistillationLayout.Facing.values();
        for (int i = 0; i < facings.length; i++) {
            verifyLayout(world, X + i * 8, facings[i]);
        }
        System.out.println("PASS native type-zero and ItemBlock mappings, recipes and all four apparatus facings");
        verifyProcessing(root, world, player);
    }

    private static void verifyLayout(xd world, int x, DistillationLayout.Facing facing) {
        place(world, x, Y, Z, DistillationPartType.BOILER, facing.frontSide);
        TileEntityDistillationBoiler boiler = (TileEntityDistillationBoiler) world.b(x, Y, Z);
        boiler.refreshStructure();
        check(!boiler.isFormed() && boiler.getMissingParts() == 6, "incomplete structure");
        for (int i = 0; i < 6; i++) {
            DistillationLayout.Placement p = DistillationLayout.placement(x, Y, Z, facing, i);
            place(world, p.x, p.y, p.z, p.part, facing.frontSide);
        }
        boiler.refreshStructure();
        check(boiler.isFormed() && boiler.getVisualFacing() == facing, "formed " + facing);
        pb column = pb.m[id(DistillationPartType.COLUMN)];
        check(column.d((ali) world, x, Y + 1, Z, facing.frontSide) == 2, "lower column world cell");
        check(column.d((ali) world, x, Y + 2, Z, facing.frontSide) == 82, "upper column world cell");
    }

    private static void place(xd world, int x, int y, int z, DistillationPartType part, int front) {
        int id = id(part);
        check(world.b(x, y, z, id, front), "placed " + part);
        int frontCell = part.index;
        if (part == DistillationPartType.COLUMN && y == Y + 2) frontCell += 5 * 16;
        check(pb.m[id].d((ali) world, x, y, z, front) == frontCell, "directional world front " + part);
        kw tile = world.b(x, y, z);
        ady nbt = new ady();
        tile.b(nbt);
        check(TILE_IDS[part.index].equals(nbt.j("id")), "stable tile identifier " + part + " " + nbt);
        kw decoded = kw.c(nbt);
        check(decoded != null && decoded.getClass() == tile.getClass(), "native NBT tile factory " + part);
    }

    private static void verifyRecipes() {
        String[] patterns = {"IGIIFIIII", "IRIRFRIII", "IGIIGIIGI", "IIIGWGIII", "GGGG GIII", "IGIRGRIII"};
        int[] ingredientIds = {265, 20, 61, 331, 326};
        for (DistillationPartType part : DistillationPartType.values()) {
            ade grid = new ade(new dd() {
                @Override public boolean b(yw player) { return true; }
            }, 3, 3);
            String pattern = patterns[part.index];
            for (int i = 0; i < pattern.length(); i++) {
                char mark = pattern.charAt(i);
                if (mark != ' ') grid.a(i, new aan(ingredientIds["IGFRW".indexOf(mark)], 1, 0));
            }
            aan result = fr.a().a(grid);
            check(result != null && result.c == id(part) && result.a == (part == DistillationPartType.COLUMN ? 2 : 1),
                    "native crafting output " + part);
        }
    }

    private static void verifyProcessing(File root, xd world, yw player) {
        BuildCraftEnergy.oilMoving = new BlockOilFlowing(DefaultProps.OIL_MOVING_ID, acn.g);
        BuildCraftEnergy.oilStill = new BlockOilStill(DefaultProps.OIL_STILL_ID, acn.g);
        BuildCraftEnergy.bucketOil = new ItemBucketOil(DefaultProps.BUCKET_OIL_ID);

        TileEntityDistillationBoiler boiler = (TileEntityDistillationBoiler) world.b(X, Y, Z);
        TileEntityDistillationHeater heater = (TileEntityDistillationHeater) world.b(X, Y - 1, Z);
        TileEntityDistillationCondenser condenser = (TileEntityDistillationCondenser) world.b(X + 1, Y + 2, Z);
        TileEntityDistillationReceiver receiver = (TileEntityDistillationReceiver) world.b(X + 1, Y + 1, Z);
        int selected = DistillateType.KEROSENE.id;
        boiler.select(selected);
        check(boiler.fill(Orientations.XPos, 500, 9, true) == 0
                && condenser.fill(Orientations.XPos, 500, DefaultProps.OIL_STILL_ID, true) == 0,
                "crossed liquids rejected");
        check(boiler.fill(Orientations.XPos, 500, DefaultProps.OIL_STILL_ID, true) == 500
                && condenser.fill(Orientations.XPos, 500, 9, true) == 500, "native BuildCraft oil and water inlets");
        player.ap.a[0] = new aan(BuildCraftEnergy.bucketOil);
        player.ap.c = 0;
        check(pb.m[id(DistillationPartType.BOILER)].b(world, X, Y, Z, player) && player.ap.a[0].c == 325,
                "native oil bucket exchanges for empty");
        player.ap.a[0] = new aan(326, 1, 0);
        check(pb.m[id(DistillationPartType.CONDENSER)].b(world, X + 1, Y + 2, Z, player) && player.ap.a[0].c == 325,
                "native water bucket exchanges for empty");
        player.ap.a[0] = new aan(1, 1, 0);
        check(!pb.m[id(DistillationPartType.BOILER)].b(world, X, Y, Z, player),
                "holding a block passes through to native placement");
        heater.n_();
        check(heater.isAddedToEnergyNet() && heater.injectEnergy(Direction.XN, 33) == 33,
                "IC2 heater joins EnergyNet and rejects overvoltage");
        int ticks = DistillateType.KEROSENE.durationTicks;
        for (int i = 0; i < ticks * 5 + 4; i++) {
            check(heater.injectEnergy(Direction.XN, FractionationProcess.EU_PER_TICK) == 0, "LV input");
            boiler.n_();
        }
        check(heater.getStoredEnergy() == 0 && boiler.getProcess().getDistillateMB() == 1250
                && boiler.getProcess().getCrudeMB() == 250 && boiler.getProcess().getCoolantMB() == 1250
                && boiler.getProcess().getProgressTicks() == 4, "five native batches and partial sixth with exact EU accounting");

        player.ap.a[0] = new aan(325, 1, 0);
        check(pb.m[id(DistillationPartType.RECEIVER)].b(world, X + 1, Y + 1, Z, player)
                && player.ap.a[0].a() == DistillationRegistry.bucketFor(selected), "native bucket collection");
        world.a(true, (rw) null);
        ce.a.a();
        agp.a();
        heater.onChunkUnload();
        xd reopened = new UnderwaterEquipmentProbe.Fixture(root, "distillation");
        TileEntityDistillationBoiler restored = (TileEntityDistillationBoiler) reopened.b(X, Y, Z);
        restored.refreshStructure();
        check(restored.isFormed() && restored.getProcess().getSelectedTypeId() == selected
                && restored.getProcess().getProgressTicks() == 4 && restored.getProcess().getDistillateMB() == 250,
                "region save and reopen keep selected fraction, progress, output and layout");
        verifyWoodenPipe(reopened);
        System.out.println("PASS native oil/water inlets and bucket exchange, IC2 LV batches, bucket collection, "
                + "disk reload and powered BuildCraft extraction");
    }

    private static void verifyWoodenPipe(xd world) {
        PowerFramework.currentFramework = new PneumaticPowerFramework();
        // At time zero the legacy liquid transport has not initialized its split vector.
        world.a(1L);
        int id = 166;
        BuildCraftTransport.genericPipeBlock = new BlockGenericPipe(id);
        int item = BlockGenericPipe.registerPipe(4057, PipeLiquidsWood.class).bQ;
        int x = X + 2;
        PipeLiquidsWood pipe = (PipeLiquidsWood) BlockGenericPipe.createPipe(world, x, Y + 1, Z, item);
        check(world.b(x, Y + 1, Z, id, Orientations.XNeg.ordinal()), "place wooden liquid pipe beside receiver");
        TileGenericPipe tile = (TileGenericPipe) world.b(x, Y + 1, Z);
        tile.pipe = pipe;
        pipe.setTile(tile);
        pipe.setWorld(world);
        pipe.initialize();
        pipe.getPowerProvider().receiveEnergy(1);
        pipe.doWork();
        pipe.updateEntity();
        TileEntityDistillationReceiver receiver = (TileEntityDistillationReceiver) world.b(X + 1, Y + 1, Z);
        PipeTransportLiquids transport = (PipeTransportLiquids) pipe.transport;
        int quantity = transport.getLiquidQuantity();
        check(quantity > 0 && receiver.getLiquidQuantity() == 250 - quantity, "wooden pipe conserves extracted liquid");
    }

    private static int id(DistillationPartType part) { return DistillationRegistry.blockId(part); }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
