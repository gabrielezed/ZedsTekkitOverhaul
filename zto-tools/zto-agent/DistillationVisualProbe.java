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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.DistillationLayout;
import zed.tekkitoverhaul.distillation.DistillationPartType;
import zed.tekkitoverhaul.distillation.DistillationRegistry;
import zed.tekkitoverhaul.distillation.DistillationState;
import zed.tekkitoverhaul.distillation.FractionationProcess;
import zed.tekkitoverhaul.distillation.TileEntityDistillationBoiler;
import zed.tekkitoverhaul.distillation.TileEntityDistillationCondenser;
import zed.tekkitoverhaul.distillation.TileEntityDistillationHeater;
import zed.tekkitoverhaul.distillation.TileEntityDistillationReceiver;
import zed.tekkitoverhaul.distillation.client.DistillationSounds;
import zed.tekkitoverhaul.survival.aerial.client.AerialSoundRegistration;

/** Native block renderer dispatch and gameplay checks in the pinned headless pack. */
final class DistillationVisualProbe {
    private static final int X = 294, Y = 12, Z = 294;
    private static final String SHEET = "/zed/textures/distillation_blocks.png";
    private static final String[] TILE_IDS = {
            "ZTODistillationBoiler", "ZTODistillationLVHeater", "ZTODistillationPart",
            "ZTODistillationCondenser", "ZTODistillationReceiver", "ZTODistillationPart"
    };

    static void run(File root, UnderwaterEquipmentProbe.Fixture world, Minecraft client) throws Exception {
        vq player = new vq(client, world, new fc("DistillationFixture", ""), 0);
        client.h = player;
        client.i = player;
        BufferedImage atlas = ImageIO.read(DistillationVisualProbe.class.getResource(SHEET));
        for (DistillationPartType part : DistillationPartType.values()) {
            int id = id(part);
            pb block = pb.m[id];
            check(id == 3010 + part.index, "stable block ID " + part);
            boolean vessel = part == DistillationPartType.COLUMN || part == DistillationPartType.CONDENSER
                    || part == DistillationPartType.RECEIVER;
            check(block.d() == (vessel ? DistillationRegistry.vesselModel() : 0) && vl.a(block.d()),
                    "registered three-dimensional item renderer " + part);
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
        System.out.println("PASS native world model dispatch and ItemBlock mappings, recipes and all four facings");
        verifyOperatingState(world, X + 8, facings[1]);
        verifySounds(world, client, X + 16);
        verifyPanels(world, client, X + 24, facings[3]);
        verifyProcessing(root, world, player);
    }

    private static void verifyLayout(xd world, int x, DistillationLayout.Facing facing) throws Exception {
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
        int lower = vertices(world, x, Y + 1, Z, column);
        int upper = vertices(world, x, Y + 2, Z, column);
        check(lower == 124 && upper == 144, "linked collar culling and vapor port in " + facing);
        int ax = x + facing.dx, az = Z + facing.dz;
        check(vertices(world, ax, Y + 2, az, pb.m[id(DistillationPartType.CONDENSER)]) == 84,
                "condenser inlet in " + facing);
        check(vertices(world, ax, Y + 1, az, pb.m[id(DistillationPartType.RECEIVER)]) == 104,
                "receiver neck and outlet in " + facing);
    }

    private static int vertices(xd world, int x, int y, int z, pb block) throws Exception {
        vl renderer = new vl((ali) world);
        adz tess = adz.a;
        Field index = adz.class.getDeclaredField("r");
        Field buffer = adz.class.getDeclaredField("h");
        index.setAccessible(true);
        buffer.setAccessible(true);
        tess.b();
        try {
            check(renderer.b(block, x, y, z), "legacy dispatch emitted world geometry");
            int used = index.getInt(tess);
            int[] data = (int[]) buffer.get(tess);
            for (int offset = 0; offset < used; offset += 8) {
                float px = Float.intBitsToFloat(data[offset]);
                float py = Float.intBitsToFloat(data[offset + 1]);
                float pz = Float.intBitsToFloat(data[offset + 2]);
                check(px >= x && px <= x + 1 && py >= y && py <= y + 1 && pz >= z && pz <= z + 1,
                        "vessel vertices stay within the occupied cell");
            }
            wu collision = block.c(world, x, y, z);
            check(collision.a == x && collision.b == y && collision.c == z
                    && collision.d == x + 1 && collision.e == y + 1 && collision.f == z + 1,
                    "full collision bounds restored after world rendering");
            return used / 8;
        } finally {
            // Discard CPU-side vertices. A native draw requires an OpenGL context.
            tess.z = false;
        }
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
        check(restored.visualState() == DistillationState.IDLE && restored.completionTicks() == 0,
                "disk reload clears ephemeral activity and completion");
        verifyWoodenPipe(reopened);
        System.out.println("PASS native oil/water inlets and bucket exchange, IC2 LV batches, bucket collection, "
                + "disk reload and powered BuildCraft extraction");
    }

    private static void verifyOperatingState(xd world, int x, DistillationLayout.Facing facing) {
        TileEntityDistillationBoiler boiler = (TileEntityDistillationBoiler)world.b(x, Y, Z);
        TileEntityDistillationHeater heater = (TileEntityDistillationHeater)world.b(x, Y - 1, Z);
        FractionationProcess batch = boiler.getProcess();
        DistillateType gas = DistillateType.GASOLINE;
        int water = gas.coolantPerBatchMB;
        int[] events = new int[2];
        TileEntityDistillationBoiler.setStepListener((source, started, completed) -> {
            check(source == boiler, "controller owns refinery events");
            if (started) events[0]++;
            if (completed) events[1]++;
        });
        try {
            check(boiler.visualState() == DistillationState.IDLE, "formed before its first step");
            assertStep(boiler, DistillationState.CRUDE_REQUIRED);
            boiler.addCrude(1000);
            assertStep(boiler, DistillationState.COOLANT_REQUIRED);
            boiler.addCoolingWater(water * 3);
            assertStep(boiler, DistillationState.POWER_REQUIRED);
            heater.n_();
            check(heater.injectEnergy(Direction.XN, 8) == 0, "one actual processing tick of EU");
            assertStep(boiler, DistillationState.ACTIVE);
            check(batch.getProgressTicks() == 1 && heater.getStoredEnergy() == 0 && events[0] == 1,
                    "activity follows successful EU debit");
            assertStep(boiler, DistillationState.POWER_REQUIRED);
            check(batch.getProgressTicks() == 1, "power interruption preserves progress");

            check(heater.injectEnergy(Direction.XN, 32) == 0, "LV reserve for remaining transitions");
            batch.restore(1000, 0, 0, -1, gas.id, 1);
            assertStep(boiler, DistillationState.COOLANT_REQUIRED);
            batch.restore(0, water, 0, -1, gas.id, 1);
            assertStep(boiler, DistillationState.CRUDE_REQUIRED);
            batch.restore(1000, water, 8000, gas.id, gas.id, 1);
            assertStep(boiler, DistillationState.OUTPUT_FULL);
            batch.restore(1000, water, 250, DistillateType.DIESEL.id, gas.id, 1);
            assertStep(boiler, DistillationState.OUTPUT_MISMATCH);
            check(batch.getProgressTicks() == 1 && heater.getStoredEnergy() == 32 && events[1] == 0,
                    "stalls consume no EU and never emit a completion");

            batch.restore(1000, water * 3, 0, -1, gas.id, gas.durationTicks - 1);
            assertStep(boiler, DistillationState.ACTIVE);
            check(batch.getDistillateMB() == 250 && boiler.completionTicks() == 10 && events[1] == 1,
                    "one real transfer and completion event");
            assertStep(boiler, DistillationState.ACTIVE);
            check(batch.getProgressTicks() == 1 && events[0] == 2 && events[1] == 1,
                    "next batch continues without replaying the startup or completion");
            int ax = x + facing.dx, az = Z + facing.dz;
            check(world.b(ax, Y + 2, az, 0, 0), "removed condenser");
            check(!boiler.presentationAvailable() && boiler.completionTicks() == 0
                    && boiler.visualState() == DistillationState.INCOMPLETE, "removal stops effects immediately");
            check(vertices(world, x, Y + 2, Z, pb.m[id(DistillationPartType.COLUMN)]) == 124,
                    "removed condenser leaves no phantom connector");
            boiler.refreshStructure();
            assertStep(boiler, DistillationState.INCOMPLETE);
            boiler.onChunkUnload();
            check(!boiler.presentationAvailable() && boiler.completionTicks() == 0, "unload ends effects");
        } catch (Exception failure) {
            throw new AssertionError("Failed native operating state fixture", failure);
        } finally {
            TileEntityDistillationBoiler.setStepListener(null);
        }
        System.out.println("PASS authoritative idle, start, power/crude/coolant stalls, both output blocks, completion and removal");
    }

    private static void assertStep(TileEntityDistillationBoiler boiler, DistillationState expected) {
        boiler.n_();
        check(boiler.visualState() == expected,
                "operating state " + boiler.visualState() + ", expected " + expected);
    }

    private static void verifySounds(UnderwaterEquipmentProbe.Fixture world, Minecraft client, int x) throws Exception {
        sd manager = new sd();
        // Invoke the production Forge callback without initializing LWJGL Display on the host.
        Field handlerField = AerialSoundRegistration.class.getDeclaredField("HANDLER");
        handlerField.setAccessible(true);
        AerialSoundRegistration handler = (AerialSoundRegistration)handlerField.get(null);
        handler.onLoadSoundSettings(manager);
        handler.onLoadSoundSettings(manager);
        check(manager.getSoundsPool().a == 18, "one registration per clip, including after reload");
        sd replacement = new sd();
        handler.onLoadSoundSettings(replacement);
        for (String clip : new String[] {"start", "running", "complete"}) {
            ub entry = replacement.getSoundsPool().a("zto.distillation." + clip);
            check(entry != null, "Forge sound pool lookup for " + clip);
            try (AudioInputStream audio = AudioSystem.getAudioInputStream(entry.b)) {
                check(audio.getFormat().getChannels() == 1 && audio.getFormat().getSampleRate() == 22050
                        && audio.getFrameLength() <= 6000, "short positional mono clip " + clip);
            }
        }
        TileEntityDistillationBoiler boiler = (TileEntityDistillationBoiler)world.b(x, Y, Z);
        TileEntityDistillationHeater heater = (TileEntityDistillationHeater)world.b(x, Y - 1, Z);
        DistillateType gas = DistillateType.GASOLINE;
        boiler.getProcess().restore(1000, 3 * gas.coolantPerBatchMB, 0, -1, gas.id, gas.durationTicks - 2);
        heater.n_();
        DistillationSounds.register(client);
        client.h.c(x + 2, Y, Z, 0, 0);
        try {
            for (int tick = 0; tick < 160; tick++) {
                world.a((long)tick);
                heater.injectEnergy(Direction.XN, 8);
                assertStep(boiler, DistillationState.ACTIVE);
            }
            assertStep(boiler, DistillationState.POWER_REQUIRED);
            client.h.c(x + 25, Y, Z, 0, 0);
            heater.injectEnergy(Direction.XN, 8);
            assertStep(boiler, DistillationState.ACTIVE);
            check(java.util.Collections.frequency(world.distillationSounds, "zto.distillation.start") == 1,
                    "only the nearby startup reaches positional playback");
            check(java.util.Collections.frequency(world.distillationSounds, "zto.distillation.complete") == 1,
                    "one positional completion cue");
            int pulses = java.util.Collections.frequency(world.distillationSounds, "zto.distillation.running");
            check(pulses >= 1 && pulses <= 3, "running pulse is rate limited");
        } finally {
            TileEntityDistillationBoiler.setStepListener(null);
            world.a(0L);
        }
        System.out.println("PASS native sound pool reload, WAV resources and bounded positional event dispatch");
    }

    private static void verifyPanels(xd world, Minecraft client, int x, DistillationLayout.Facing facing)
            throws Exception {
        TileEntityDistillationBoiler boiler = (TileEntityDistillationBoiler)world.b(x, Y, Z);
        Field controls = vp.class.getDeclaredField("s");
        Field buttonWidth = abp.class.getDeclaredField("a");
        Field buttonHeight = abp.class.getDeclaredField("b");
        Method click = vp.class.getDeclaredMethod("a", int.class, int.class, int.class);
        controls.setAccessible(true);
        buttonWidth.setAccessible(true);
        buttonHeight.setAccessible(true);
        click.setAccessible(true);
        client.h.ap.a[0] = null;
        client.h.c(x + 1, Y, Z, 0, 0);
        for (int index = -1; index < 6; index++) {
            int bx = x, by = Y, bz = Z;
            DistillationPartType part = DistillationPartType.BOILER;
            if (index >= 0) {
                DistillationLayout.Placement p = DistillationLayout.placement(x, Y, Z, facing, index);
                bx = p.x;
                by = p.y;
                bz = p.z;
                part = p.part;
            }
            check(pb.m[id(part)].b(world, bx, by, bz, client.h) && client.s != null, "opens panel " + part);
            vp panel = client.s;
            check(!panel.b(), "machine panel allows processing " + part);
            for (int width : new int[] {320, 427, 640, 1280}) {
                panel.a(client, width, width * 3 / 4);
                int left = (panel.q - 246) / 2, top = (panel.r - 206) / 2;
                List<?> buttons = (List<?>)controls.get(panel);
                check(buttons.size() == (part == DistillationPartType.THERMOMETER ? 5 : 1),
                        "controls rebuilt once at scale " + width);
                for (Object control : buttons) {
                    abp button = (abp)control;
                    check(button.c >= left && button.d >= top && button.c + buttonWidth.getInt(button) <= left + 246
                            && button.d + buttonHeight.getInt(button) <= top + 206, "control within panel");
                    if (button.f == DistillateType.DIESEL.id && button.h) {
                        click.invoke(panel, button.c + 1, button.d + 1, 0);
                        check(boiler.getProcess().selectedType() == DistillateType.DIESEL && !button.h,
                                "native thermometer click changes and highlights selected fraction");
                    }
                }
            }
            abp close = (abp)((List<?>)controls.get(panel)).get(0);
            click.invoke(panel, close.c + 1, close.d + 1, 0);
            check(client.s == null, "native close click " + part);
        }
        check(pb.m[id(DistillationPartType.BOILER)].b(world, x, Y, Z, client.h), "reopen boiler");
        client.s.a(client, 320, 240);
        client.h.c(x + 20, Y, Z, 0, 0);
        client.s.a();
        check(client.s == null, "moving away closes a machine panel");
        System.out.println("PASS six native panels, four viewport sizes, thermometer click, close and distance invalidation");
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
