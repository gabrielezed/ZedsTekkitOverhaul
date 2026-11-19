import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;

import ic2.common.ElectricItem;
import ic2.common.Ic2Items;
import ic2.common.ItemArmorJetpack;
import ic2.common.ItemArmorJetpackElectric;
import ic2.platform.Keyboard;
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
import zed.tekkitoverhaul.survival.weather.client.JetpackWeatherStatus;

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
        Field modeSwitch = Keyboard.class.getDeclaredField("modeSwitchKey");
        modeSwitch.setAccessible(true);
        modeSwitch.set(null, new afu("Mode switch", 50));

        ItemArmorJetpack fuel = new ItemArmorJetpack(22100, 0, 0);
        Ic2Items.jetpack = new aan(fuel, 1, 1);
        ItemArmorJetpackElectric electric = new ItemArmorJetpackElectric(22101, 0, 0);
        aan pack = new aan(electric);
        ElectricItem.charge(pack, 30000, 1, true, false);
        yw player = NaturalSpawnProbe.join(world);
        player.ap.b[2] = pack;
        JetpackWeather weather = JetpackWeather.LOCAL;
        weather.configure(true, () -> player);
        position(player, 24, false);
        weather.tick(world, player, false);
        check(weather.available() && ZTOAgent.jetpackActive(electric.getClass().getClassLoader()), "native IC2 hook is active");
        int charge = electric.getCharge(pack);
        powered(world, player, electric, 20);
        check(weather.phase() == Phase.DRY && player.s > 0.13 && electric.getCharge(pack) == charge - 140,
                "clear air preserves native thrust and EU use");
        double normalFlight = continuousFlight("clear", world, player, electric, pack, false, false);
        double normalHover = continuousFlight("clear", world, player, electric, pack, true, false);
        world.rain = true;
        position(player, 4, true);
        powered(world, player, electric, 240);
        check(weather.wetness() == 0, "equipped and grounded is dry even during native use");

        position(player, 24, false);
        double wetFlight = continuousFlight("short rain", world, player, electric, pack, false, false);
        check(Math.abs(wetFlight - normalFlight) < 0.001, "short exposure retains the clear-air flight envelope");
        position(player, 24, false);
        powered(world, player, electric, 25);
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
        check(weather.remaining() == 88, "ordinary rain fault lasts 88 ticks");
        verifyInventoryStatus(game, player, "fault");

        for (int altitude : new int[] {12, 60, 105}) {
            position(player, altitude, false);
            double x = player.o;
            player.r = 0.24;
            player.b(player.r, 0, 0);
            check(player.o > x, "moving pilot retains lateral travel");
            charge = electric.getCharge(pack);
            player.s = 0.14;
            check(electric.useJetpack(player, false) && player.s <= 0 && electric.getCharge(pack) == charge - 7,
                    "fault changes actual powered ascent and spends native EU at Y=" + altitude);
            player.M = 12;
            player.s = -0.82;
            check(electric.useJetpack(player, false) && player.s > -0.82 && player.s <= 0 && player.M == 0,
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
        double faultFlight = continuousFlight("rain fault", world, player, electric, pack, false, true);
        check(normalFlight > 10 && faultFlight < -16 && faultFlight > -21,
                "rain fault more than doubles the historical 40-tick altitude loss: " + normalFlight + " / " + faultFlight);
        finishFault(world, player, electric, pack, false);
        double recoveryFlight = continuousFlight("rain recovery", world, player, electric, pack, false, false);
        check(weather.phase() == Phase.RECOVERING && Math.abs(recoveryFlight - normalFlight) < 0.001,
                "native climb returns during a guaranteed recovery interval");
        verifyInventoryStatus(game, player, "recovering");
        int rainFaults = count(world.cues, "zto.jetpack.fault");
        powered(world, player, electric, 79);
        check(count(world.cues, "zto.jetpack.fault") == rainFaults && weather.phase() == Phase.RECOVERING,
                "persistent rain respects the recovery window");
        int repeatRain = untilFault(world, player, electric);
        check(repeatRain >= 56 && repeatRain < rainTime - 140,
                "persistent rain builds to another warned fault: " + repeatRain);

        position(player, 24, false);
        world.g((int) player.o, 32, (int) player.q, 20);
        powered(world, player, electric, 220);
        check(!weather.exposed() && weather.phase() == Phase.DRY && player.s > 0.13
                && world.cues.contains("zto.jetpack.recovery"), "glass shelter dries equipment and native lift returns");
        world.g((int) player.o, 32, (int) player.q, 0);
        world.storm = true;
        int stormTime = untilFault(world, player, electric);
        check(stormTime < rainTime - 100, "thunder accelerates the warning and fault: " + stormTime + " / " + rainTime);
        check(weather.remaining() == 116, "a thunder fault has a larger interruption window");
        double stormFlight = continuousFlight("thunder fault", world, player, electric, pack, false, true);
        check(stormFlight < faultFlight - 5 && stormFlight > -28, "thunder loses more altitude under the same input");
        finishFault(world, player, electric, pack, false);

        int faults = count(world.cues, "zto.jetpack.fault");
        powered(world, player, electric, 119);
        check(count(world.cues, "zto.jetpack.fault") == faults && weather.phase() == Phase.RECOVERING,
                "fault cannot chain through recovery");
        int repeatTicks = untilFault(world, player, electric);
        check(repeatTicks >= 56 && repeatTicks < stormTime - 75,
                "retained moisture makes a second fault arrive sooner after recovery: " + repeatTicks);
        double faultHover = continuousFlight("thunder fault", world, player, electric, pack, true, true);
        check(faultHover < -22 && faultHover > -28 && normalHover > faultHover + 15,
                "jump held in hover cannot sustain altitude");
        position(player, 68, false);
        player.s = -0.72;
        input(player, pack, false);
        mod_IC2.setIsJumpingOfEntityLiving(player, false);
        charge = electric.getCharge(pack);
        check(!electric.onTick(player, pack), "releasing jump in normal mode releases the brake");
        player.a_(0, 0);
        check(player.s < -0.72 && electric.getCharge(pack) == charge, "release permits unpowered fall with no EU use");

        for (int platform : new int[] {3, 40, 80}) verifyLanding(world, player, electric, pack, platform);

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
        immersion(game, world, player, electric, fuel, pack);
        weather.configure(false, () -> player);
        player.ap.b[2] = pack;
        double disabledFlight = continuousFlight("thunder disabled", world, player, electric, pack, false, false);
        check(weather.phase() == Phase.DRY && Math.abs(disabledFlight - normalFlight) < 0.001,
                "disabled settings preserve native movement in storms");
        System.out.println("Native powered ticks to fault: rain " + rainTime + ", thunder " + stormTime
                + ", repeats after 119 recovery ticks: rain " + repeatRain + ", thunder " + repeatTicks);
        System.out.println("PASS native IC2 thrust, EU, weather, shelter, descent braking at 3 heights, equipment and save lifecycle");
    }

    private static void immersion(Minecraft game, Fixture world, yw player, ItemArmorJetpackElectric item,
                                  ItemArmorJetpack fuel, aan pack) {
        JetpackWeather weather = JetpackWeather.LOCAL;
        world.rain = world.storm = false;
        weather.configure(true, () -> player);
        player.ap.b[2] = pack;
        for (int y = 20; y <= 23; ++y) world.g(288, y, 288, 9);
        world.b(288, 24, 288, 8, 4); // Legacy submersion surface Y=24+5/9.
        position(player, 24, true);
        input(player, pack, false);
        int charge = item.getCharge(pack);
        check(item.onTick(player, pack) && item.getCharge(pack) == charge - 7 && weather.phase() == Phase.DRY,
                "shallow wading leaves the chest dry and thrust available");
        player.c(288.5, 23.3, 288.5, 0, 0); // Pack at 24.65, above the flowing water surface.
        check(item.onTick(player, pack) && weather.phase() == Phase.DRY, "air above flowing water allows thrust");
        for (boolean hover : new boolean[] {false, true}) {
            position(player, 23, false); // Pack at 24.35, below the flowing water surface.
            input(player, pack, hover);
            charge = item.getCharge(pack);
            player.s = -0.45;
            player.M = 12;
            check(!item.onTick(player, pack) && player.s == -0.45 && player.M == 12
                    && item.getCharge(pack) == charge && weather.phase() == Phase.IMMERSED
                    && weather.wetness() == 100, "first immersed armor tick blocks lift and powered fall reset");
            verifyInventoryStatus(game, player, "wet, water protection");
            if (hover) {
                mod_IC2.setIsJumpingOfEntityLiving(player, false);
                check(!item.onTick(player, pack) && player.s == -0.45 && item.getCharge(pack) == charge,
                        "immersion blocks automatic hover braking even with jump released");
            }
            double bare = swim(world, player, item, null, hover);
            double equipped = swim(world, player, item, pack, hover);
            check(Math.abs(bare - equipped) < 1.0e-9 && item.getCharge(pack) == charge,
                    "immersed armor tick matches unpowered swimming");
            System.out.printf("Immersed %s: %.3f blocks swimming with or without equipment, zero EU%n",
                    hover ? "hover" : "normal", equipped);
        }

        player.ap.b[2] = null;
        weather.tick(world, player, false);
        player.ap.b[2] = pack;
        position(player, 23, false);
        input(player, pack, true);
        check(!item.onTick(player, pack) && weather.phase() == Phase.IMMERSED,
                "re-equipping underwater blocks propulsion before the weather tick");
        ady saved = new ady();
        pack.b(saved);
        weather.tick(null, null, true);
        aan restored = aan.a(saved);
        player.ap.b[2] = restored;
        charge = item.getCharge(restored);
        check(!item.onTick(player, restored) && weather.phase() == Phase.IMMERSED
                && item.getCharge(restored) == charge, "loaded equipment is protected before the first weather tick");

        position(player, 26, false);
        check(!item.onTick(player, restored) && weather.remaining() == 20, "emergence begins a 20-tick restart");
        verifyInventoryStatus(game, player, "wet, restarting 1.0s");
        for (int i = 0; i < 5; ++i) weather.tick(world, player, false);
        position(player, 23, false);
        check(!item.onTick(player, restored) && weather.phase() == Phase.IMMERSED, "re-entry interrupts restart");
        position(player, 26, false);
        check(!item.onTick(player, restored) && weather.remaining() == 20, "second emergence starts a full restart");
        for (int i = 0; i < 20; ++i) {
            check(!item.onTick(player, restored) && player.s == 0 && item.getCharge(restored) == charge,
                    "restart denies propulsion for the full interval");
            weather.tick(world, player, false);
        }
        check(weather.phase() == Phase.WET && item.onTick(player, restored) && player.s > 0
                && item.getCharge(restored) == charge - 4, "native hover resumes while the pack is still wet");
        aan dryFuel = new aan(fuel, 1, 1);
        aan wetFuel = new aan(fuel, 1, 1);
        position(player, 26, false);
        player.ap.b[2] = dryFuel;
        input(player, dryFuel, false);
        check(fuel.onTick(player, dryFuel) && player.s > 0.19, "dry fuel control thrusts");
        double fuelMotion = player.s;
        position(player, 23, false);
        player.ap.b[2] = wetFuel;
        input(player, wetFuel, false);
        charge = fuel.getCharge(wetFuel);
        check(fuel.onTick(player, wetFuel) && player.s == fuelMotion
                && fuel.getCharge(wetFuel) == fuel.getCharge(dryFuel) && fuel.getCharge(wetFuel) < charge,
                "immersed fuel jetpack matches its dry control");
        System.out.println("PASS chest-height flowing-water protection, first activation, full restart, and fuel control");
        for (int y = 20; y <= 24; ++y) world.g(288, y, 288, 0);
        player.B(); // Refresh vanilla's cached water contact after draining the fixture.
        world.rain = world.storm = true;
    }

    private static double swim(Fixture world, yw player, ItemArmorJetpackElectric item, aan pack, boolean hover) {
        position(player, 21, false);
        double start = player.p;
        player.ap.b[2] = pack;
        for (int i = 0; i < 5; ++i) {
            player.B(); // Native water contact and current.
            check(player.H(), "swimming fixture is in water");
            if (pack != null) {
                input(player, pack, hover);
                check(!item.onTick(player, pack), "immersed swimming has no propulsion");
            }
            player.s += 0.04; // Native EntityLiving jump impulse in water.
            player.a_(0, 0);
            JetpackWeather.LOCAL.tick(world, player, false);
        }
        return player.p - start;
    }

    private static int count(List<String> values, String value) {
        return java.util.Collections.frequency(values, value);
    }

    private static double continuousFlight(String label, Fixture world, yw player, ItemArmorJetpackElectric item,
                                           aan pack, boolean hover, boolean fault) {
        position(player, 68, false);
        player.s = 0.32;
        input(player, pack, hover);
        double start = player.p;
        int charge = item.getCharge(pack);
        for (int tick = 0; tick < 40; ++tick) {
            check((JetpackWeather.LOCAL.phase() == Phase.FAULT) == fault, "expected propulsion phase");
            check(item.onTick(player, pack), "held jump activates native IC2 armor tick");
            if (fault) check(player.s <= 0, "fault cannot add powered ascent");
            player.a_(0, 0);
            JetpackWeather.LOCAL.tick(world, player, false);
        }
        check(item.getCharge(pack) == charge - (hover ? 4 : 7) * 40, "40 ticks use native EU");
        check(!fault || (player.s < -0.50 && player.s > -0.73), "fault enforces a bounded downward velocity");
        double displacement = player.p - start;
        System.out.printf("Pinned IC2, %s %s, 40 ticks: %+.2f blocks / %+.3f blocks per tick%n",
                label, hover ? "hover" : "normal", displacement, player.s);
        return displacement;
    }

    private static void input(yw player, aan pack, boolean hover) {
        mod_IC2.setIsJumpingOfEntityLiving(player, true);
        ic2.common.StackUtil.getOrCreateNbtData(pack).a("hoverMode", hover);
    }

    private static void verifyInventoryStatus(Minecraft game, yw player, String state) {
        List<String> labels = new ArrayList<String>();
        ain inventory = new ain(player) {
            @Override public void a(nl font, String text, int x, int y, int color) {
                check(x == 160 && y == 208, "status stays centered below the 176 x 166 Survival panel");
                labels.add(text);
            }
        };
        inventory.q = 320;
        inventory.r = 240;
        JetpackWeatherStatus.drawInventory(game, inventory);
        check(labels.size() == 1 && labels.get(0).endsWith(state), "Survival inventory reports " + state);
    }

    private static void finishFault(Fixture world, yw player, ItemArmorJetpackElectric item, aan pack, boolean hover) {
        int ticks = JetpackWeather.LOCAL.remaining();
        input(player, pack, hover);
        for (int i = 0; i < ticks && JetpackWeather.LOCAL.phase() == Phase.FAULT; ++i) {
            check(item.onTick(player, pack) && player.s <= 0, "continued activation through fault");
            player.a_(0, 0);
            JetpackWeather.LOCAL.tick(world, player, false);
        }
        check(JetpackWeather.LOCAL.phase() == Phase.RECOVERING, "fault ends within its interruption window");
    }

    private static void verifyLanding(Fixture world, yw player, ItemArmorJetpackElectric item, aan pack, int platform) {
        JetpackWeather weather = JetpackWeather.LOCAL;
        weather.configure(true, () -> player);
        world.g(288, platform, 288, 1);
        position(player, platform + 13, false);
        untilFault(world, player, item);
        input(player, pack, false);
        player.l(20);
        boolean fast = false, braking = false;
        int ticks = 0;
        while (weather.phase() == Phase.FAULT && ticks++ < 116) {
            check(item.onTick(player, pack) && player.s <= 0, "landing retains native descent control");
            fast |= player.s < -0.45;
            if (fast && player.s >= -0.18) braking = true;
            player.a_(0, 0);
            weather.tick(world, player, false);
        }
        check(fast && braking && player.z && player.bb() == 20 && weather.phase() == Phase.RECOVERING,
                "a charged pilot can brake and land safely on a platform at Y=" + platform);
        System.out.printf("Controlled thunder landing on Y=%d in %d ticks%n", platform, ticks);
        if (platform > 3) world.g(288, platform, 288, 0);
        position(player, 24, false);
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

    private static void position(yw player, double height, boolean grounded) {
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
