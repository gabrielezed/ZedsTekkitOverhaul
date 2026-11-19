import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import ee.ContainerDMFurnace;
import ee.ContainerRMFurnace;
import ee.EEItem;
import ee.TileDMFurnace;
import ee.TileRMFurnace;
import eloraam.base.ContainerAlloyFurnace;
import eloraam.base.TileAlloyFurnace;
import eloraam.machine.ContainerBlueFurnace;
import eloraam.machine.TileBlueFurnace;
import ic2.common.ContainerIronFurnace;
import ic2.common.TileEntityIronFurnace;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import railcraft.common.utility.ContainerBlastFurnace;
import railcraft.common.utility.TileBlastFurnace;
import zed.tekkitoverhaul.integration.inventory.FurnaceFuelTransfer;

/** Runs normal window clicks with the production callback and the pack's actual containers. */
public final class FurnaceFuelProbe {
    public static void main(String[] args) throws Exception {
        try {
            run(new File(args[0]));
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private static void run(File root) throws Exception {
        FurnaceFuelTransfer.register();
        xd world = NaturalSpawnProbe.open(root, "furnace-transfer", 1000);
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        Field registeredMods = Loader.class.getDeclaredField("mods");
        registeredMods.setAccessible(true);
        registeredMods.set(Loader.instance(), Collections.emptyList());
        yw player = NaturalSpawnProbe.join(world);
        aes controller = new aes(null);
        ahg vanilla = new ahg();
        dd furnace = new ug(player.ap, vanilla);
        family(controller, player, furnace, 1, 263, 0);
        family(controller, player, furnace, 1, 263, 1);
        family(controller, player, furnace, 1, 280, 0);
        family(controller, player, furnace, 1, 17, 0); // Log is also a charcoal recipe.
        family(controller, player, new ContainerIronFurnace(player, new TileEntityIronFurnace()), 1, 263, 0);
        family(controller, player, new ContainerAlloyFurnace(player.ap, new TileAlloyFurnace()), 9, 17, 0);
        TileBlastFurnace blast = new TileBlastFurnace();
        blast.setMaster(true);
        family(controller, player, new ContainerBlastFurnace(player.ap, blast), 1, 263, 1);
        // This headless test skips EE2 registration. Seed the references that its fuel predicate reads.
        EEItem.kleinStar1 = EEItem.kleinStar2 = EEItem.kleinStar3 = EEItem.kleinStar4 =
                EEItem.kleinStar5 = EEItem.kleinStar6 = yr.e[266];
        family(controller, player, new ContainerDMFurnace(player.ap, new TileDMFurnace()), 0, 263, 0);
        family(controller, player, new ContainerRMFurnace(player.ap, new TileRMFurnace()), 0, 263, 0);

        player.ar = furnace;
        int input = playerSlot(player, furnace, false);
        slot(furnace, input).d(new aan(15, 3, 0));
        controller.a(furnace.f, input, 0, true, player);
        check(count(slot(furnace, 0), 15, 0) == 3 && slot(furnace, input).b() == null, "vanilla ingredient moved");
        slot(furnace, 2).d(new aan(265, 1, 0));
        controller.a(furnace.f, 2, 0, true, player);
        check(slot(furnace, 2).b() == null, "native output quick move");

        slot(furnace, 1).d((aan) null);
        slot(furnace, input).d(new aan(327, 1, 0));
        controller.a(furnace.f, input, 0, true, player);
        check(count(slot(furnace, 1), 327, 0) == 1, "lava bucket enters fuel slot");
        world.g(288, 4, 288, 61);
        world.a(288, 4, 288, vanilla);
        vanilla.n_();
        check(vanilla.a > 0 && slot(furnace, 1).b() == null, "this vintage vanilla burn consumes its lava bucket");

        dd blue = new ContainerBlueFurnace(player.ap, new TileBlueFurnace());
        player.ar = blue;
        int playerSlot = playerSlot(player, blue, false);
        slot(blue, playerSlot).d(new aan(263, 1, 0));
        controller.a(blue.f, playerSlot, 0, true, player);
        check(count(slot(blue, 0), 263, 0) == 1, "powered furnace keeps its normal material routing");
        System.out.println("PASS six fuel-burning furnace containers, exact limits, dual-use logs, native lava consumption, output and BluePower control");
    }

    private static void family(aes controller, yw player, dd container, int index, int id, int damage) {
        player.ar = container;
        int source = playerSlot(player, container, false);
        int hotbar = playerSlot(player, container, true);
        yu fuel = slot(container, index);
        fuel.d((aan) null);
        slot(container, source).d(new aan(id, 7, damage));
        controller.a(container.f, source, 0, true, player);
        check(count(fuel, id, damage) == 7 && slot(container, source).b() == null, container.getClass() + " empty");
        fuel.d(new aan(id, 62, damage));
        slot(container, hotbar).d(new aan(id, 5, damage));
        controller.a(container.f, hotbar, 0, true, player);
        check(count(fuel, id, damage) == 64 && count(slot(container, hotbar), id, damage) == 3, container.getClass() + " partial");
        controller.a(container.f, hotbar, 0, true, player);
        check(count(fuel, id, damage) == 64 && count(slot(container, hotbar), id, damage) == 3, container.getClass() + " full");
        fuel.d(new aan(4, 1, 0));
        controller.a(container.f, hotbar, 0, true, player);
        check(count(fuel, 4, 0) == 1 && count(slot(container, hotbar), id, damage) == 3, container.getClass() + " incompatible");
        slot(container, hotbar).d((aan) null);
    }

    private static int playerSlot(yw player, dd container, boolean hotbar) {
        int first = 0;
        while (slot(container, first).b != player.ap) first++;
        return hotbar ? first + 27 : first;
    }

    private static yu slot(dd container, int index) { return (yu) container.e.get(index); }
    private static int count(yu slot, int id, int damage) {
        aan stack = slot.b();
        return stack == null || stack.c != id || stack.i() != damage ? 0 : stack.a;
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
