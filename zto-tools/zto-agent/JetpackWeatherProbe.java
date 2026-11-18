import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;

import ic2.common.ElectricItem;
import ic2.common.Ic2Items;
import ic2.common.ItemArmorJetpack;
import ic2.common.ItemArmorJetpackElectric;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import zed.tekkitoverhaul.agent.ZTOAgent;
import zed.tekkitoverhaul.survival.aerial.AerialEcology;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator;
import zed.tekkitoverhaul.survival.aerial.EntityAerialPredator.Flight;
import zed.tekkitoverhaul.territory.NaturalSpawnPressure;
import zed.tekkitoverhaul.territory.TerritorialActivityService;
import zed.tekkitoverhaul.survival.weather.JetpackWeather;
import zed.tekkitoverhaul.survival.weather.JetpackWeather.Phase;

/** Executes the real IC2 useJetpack method with charged equipment and a windowless client fixture. */
public final class JetpackWeatherProbe {
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        Fixture world = new Fixture(root, "jetpack-weather");
        Files.createFile(new File(root, "jetpack-weather/.zto-disposable").toPath());
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        Minecraft game = new Minecraft(null, null, null, 320, 240, false) {
            @Override public void a(ja failure) { throw new AssertionError(failure); }
        };
        // OptiFine probes the display in the settings constructor. Only IC2 forward input is read here.
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafe = unsafeClass.getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        game.A = (hu) unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe.get(null), hu.class);
        game.A.n = new afu("Forward", 17);
        game.f = world;
        Field client = FMLClientHandler.class.getDeclaredField("client");
        client.setAccessible(true);
        client.set(FMLClientHandler.instance(), game);

        ItemArmorJetpack fuel = new ItemArmorJetpack(22100, 0, 0);
        Ic2Items.jetpack = new aan(fuel, 1, 1);
        ItemArmorJetpackElectric electric = new ItemArmorJetpackElectric(22101, 0, 0);
        aan pack = new aan(electric);
        ElectricItem.charge(pack, 30000, 1, true, false);
        yw player = NaturalSpawnProbe.join(world);
        player.ap.b[2] = pack;
        JetpackWeather weather = JetpackWeather.LOCAL;
        weather.configure(true);
        position(player, 24, false);
        weather.tick(world, player, false);
        check(weather.available() && ZTOAgent.jetpackActive(electric.getClass().getClassLoader()), "native IC2 hook is active");
        int charge = electric.getCharge(pack);
        powered(world, player, electric, 20);
        check(weather.phase() == Phase.DRY && player.s > 0.13 && electric.getCharge(pack) == charge - 140,
                "clear air preserves native thrust and EU use");
        world.rain = true;
        position(player, 4, true);
        powered(world, player, electric, 240);
        check(weather.wetness() == 0, "equipped and grounded is dry even during native use");

        position(player, 24, false);
        powered(world, player, electric, 65);
        check(weather.phase() != Phase.FAULT && weather.wetness() > 0, "a short powered flight does not fault");
        int rainTime = 65;
        while (weather.wetness() < 70 && rainTime < 400) {
            powered(world, player, electric, 1);
            ++rainTime;
        }
        player.s = 0;
        EntityAerialPredator attacker = new EntityAerialPredator(world);
        attacker.c(299, 34, 288.5, 0, 0);
        check(world.a(attacker), "loaded predator near the storm pilot");
        for (int i = 0; i < 210 && attacker.flight() != Flight.PREPARE; ++i) world.a(attacker, true);
        check(attacker.flight() == Flight.PREPARE, "predator has warned during instability: "
                + attacker.flight() + " " + attacker.diveStatus() + " " + attacker.lastDiveRejection());
        rainTime += untilFault(world, player, electric);
        world.a(attacker, true);
        check(attacker.flight() == Flight.RECOVER && attacker.diveCommitments() == 0,
                "a fault cancels a telegraph before commitment");
        attacker.A();
        check(rainTime >= 320 && world.cues.contains("zto.jetpack.warning")
                && world.cues.indexOf("zto.jetpack.warning") < world.cues.indexOf("zto.jetpack.fault"),
                "ordinary rain is progressive and warns before fault: " + rainTime);

        for (int altitude : new int[] {12, 60, 105}) {
            position(player, altitude, false);
            double x = player.o;
            player.r = 0.24;
            player.b(player.r, 0, 0);
            check(player.o > x, "moving pilot retains lateral travel");
            charge = electric.getCharge(pack);
            player.s = 0.14;
            check(electric.useJetpack(player, false) && player.s < 0.08 && electric.getCharge(pack) == charge - 7,
                    "fault changes actual powered ascent and spends native EU at Y=" + altitude);
            player.M = 12;
            player.s = -0.42;
            check(electric.useJetpack(player, false) && player.s > -0.42 && player.s <= 0.02 && player.M == 0,
                    "native powered descent braking and fall bookkeeping remain available at Y=" + altitude);
            check(weather.protects(player), "active fault gates new dives");
            weather.tick(world, player, false);
        }

        TerritorialActivityService territory = new TerritorialActivityService();
        territory.bind(world);
        AerialEcology air = new AerialEcology(true, true, 90, territory, new NaturalSpawnPressure(territory, 70));
        air.bind(world);
        check(air.admitLunar(world, player, 15, 8, 4) == 0 && air.lunarStatus().contains("jetpack"),
                "a moving pilot with an active fault blocks new lunar air pressure");

        position(player, 24, false);
        world.g((int) player.o, 32, (int) player.q, 20);
        powered(world, player, electric, 160);
        check(!weather.exposed() && weather.phase() == Phase.DRY && player.s > 0.13
                && world.cues.contains("zto.jetpack.recovery"), "glass shelter dries equipment and native lift returns");
        world.g((int) player.o, 32, (int) player.q, 0);
        world.storm = true;
        int stormTime = untilFault(world, player, electric);
        check(stormTime < rainTime - 100, "thunder accelerates the warning and fault: " + stormTime + " / " + rainTime);
        int faults = count(world.cues, "zto.jetpack.fault");
        powered(world, player, electric, 120);
        check(count(world.cues, "zto.jetpack.fault") == faults && weather.phase() == Phase.RECOVERING,
                "fault cannot chain through recovery");

        player.ap.b[2] = Ic2Items.jetpack;
        weather.tick(world, player, false);
        check(!weather.equipped() && !weather.protects(player), "a replacement chestplate clears the pilot state");
        player.s = 0;
        check(fuel.useJetpack(player, false) && player.s > 0.19, "fuel jetpack retains its distinct native thrust");
        player.ap.b[2] = pack;
        weather.tick(world, player, false);
        check(weather.phase() == Phase.DRY && weather.wetness() == 0, "re-equipped electric pack starts a new exposure session");

        untilFault(world, player, electric);
        player.aT.d = true;
        weather.tick(world, player, false);
        check(!weather.protects(player) && !weather.equipped(), "Creative clears temporary state");
        player.aT.d = false;
        weather.tick(world, player, false);
        powered(world, player, electric, 80);
        ady itemNbt = new ady();
        pack.b(itemNbt);
        int savedCharge = electric.getCharge(pack);
        weather.tick(null, null, false);
        aan fromSave = aan.a(itemNbt);
        player.ap.b[2] = fromSave;
        weather.tick(world, player, false);
        check(weather.phase() == Phase.DRY && electric.getCharge(fromSave) == savedCharge,
                "reload preserves native equipment NBT and clears transient instability");
        player.ap.b[2] = new aan(electric);
        weather.tick(world, player, false);
        player.s = -0.35;
        check(!electric.useJetpack(player, false) && player.s == -0.35, "empty equipment provides no emergency energy");
        player.l(0);
        weather.tick(world, player, false);
        check(!weather.equipped() && !weather.protects(player), "death clears the local state");
        player.l(20);
        weather.configure(false);
        player.ap.b[2] = pack;
        powered(world, player, electric, 35);
        check(player.s > 0.13 && weather.phase() == Phase.DRY, "disabled settings preserve native operation in storms");
        System.out.println("Native powered ticks to fault: rain " + rainTime + ", thunder " + stormTime);
        System.out.println("PASS native IC2 thrust, EU, weather, shelter, descent braking at 3 heights, equipment and save lifecycle");
    }

    private static int count(List<String> values, String value) {
        return java.util.Collections.frequency(values, value);
    }

    private static int untilFault(Fixture world, yw player, ItemArmorJetpack item) {
        for (int i = 1; i <= 580; ++i) {
            powered(world, player, item, 1);
            if (JetpackWeather.LOCAL.phase() == Phase.FAULT) return i;
        }
        throw new AssertionError("No fault: " + JetpackWeather.LOCAL.status() + ", wet=" + JetpackWeather.LOCAL.wetness());
    }

    private static void powered(Fixture world, yw player, ItemArmorJetpack item, int ticks) {
        for (int i = 0; i < ticks; ++i) {
            player.s = 0;
            check(item.useJetpack(player, false), "powered native tick");
            JetpackWeather.LOCAL.tick(world, player, false);
        }
    }

    private static void position(yw player, int height, boolean grounded) {
        player.c(288.5, height, 288.5, 0, 0);
        player.z = grounded;
        player.r = player.s = player.t = 0;
    }

    private static final class Fixture extends xd {
        boolean rain, storm;
        final List<String> cues = new ArrayList<String>();
        Fixture(File root, String name) {
            super(new aia(root, name, false), name, NaturalSpawnProbe.SETTINGS);
            q = 2;
            for (int x = 14; x <= 22; x++) for (int z = 14; z <= 22; z++) d(x, z);
        }
        @Override public boolean F() { return storm; }
        @Override public boolean G() { return rain; }
        @Override public void a(nn entity, String sound, float volume, float pitch) { cues.add(sound); }
    }

    private static void check(boolean expected, String description) {
        if (!expected) throw new AssertionError(description);
    }
}
