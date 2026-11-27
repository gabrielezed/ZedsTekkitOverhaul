import ee.EEBase;
import ee.EEMaps;
import ee.TileCondenser;
import ee.TransTabletData;
import java.io.File;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import zed.tekkitoverhaul.config.ZtoConfig;
import zed.tekkitoverhaul.emc.EmcIntegration;
import zed.tekkitoverhaul.exploration.abyssal.guardian.CustodianBalance;
import zed.tekkitoverhaul.exploration.abyssal.guardian.CustodianBalance.Attack;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalCustodian;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalEnderkin;
import zed.tekkitoverhaul.exploration.abyssal.guardian.GuardianEncounters;
import zed.tekkitoverhaul.exploration.abyssal.worldgen.ObservatorySites.Site;

/** Exercises the reobfuscated Custodian in the existing disposable native Observatory fixture. */
final class CustodianProbe {
    private static final int PEARL = 368, EYE = 381;

    static void ownerSave(File root, xd world, Site site, ZtoConfig config, int x, int z) throws Exception {
        yw diver = fixturePlayer(world);
        admit(world, site, diver);
        EntityAbyssalCustodian boss = actor(world);
        check(boss.bb() == 80, "owner fixture contains a legitimate full-health Custodian");
        world.b(diver);

        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
            world.g(x + dx, 64, z + dz, 5);
        world.g(x + 1, 65, z, 54);
        hb chest = (hb) world.b(x + 1, 65, z);
        chest.a(0, new aan(config.underwaterMask + 256, 1, 0));
        chest.a(1, new aan(config.flippers + 256, 1, 0));
        for (int slot = 2; slot < 6; slot++) {
            aan tank = new aan(config.oxygenTank + 256, 1, 0);
            tank.d = new ady();
            tank.d.a("ZTO_OxygenTicks", 6000);
            chest.a(slot, tank);
        }
        chest.a(6, new aan(267, 1, 0));
        chest.a(7, new aan(308, 1, 0));
        chest.a(8, new aan(20, 32, 0));
        chest.a(9, new aan(297, 8, 0));
        for (int i = 0; i < 8; i++) chest.a(10 + i, new aan(3044 + i, 8, 0));
        chest.a(18, new aan(config.abyssalId("block.abyssalLamp"), 8, 0));
        world.g(x - 2, 65, z, 3047);
        world.g(x - 2, 66, z, 3038);
        world.g(x - 2, 67, z, 3049);
        world.a(new uh(x, 65, z));
        world.a(true, null);
        world.n();
        ce.a.a();
        agp.a();
        Files.createFile(new File(root, "ZTO Beauty of the Abyss/.zto-disposable-test").toPath());
        check(new File(root, "ZTO Beauty of the Abyss/level.dat").isFile(), "owner fixture saved level.dat");
        System.out.println("PASS marked owner save on the surface above the buried entrance");
    }

    static void verifyCreativeCatalog(ZtoConfig config) throws Exception {
        // The tab only needs its normal block icon. Do not initialize the graphical Foundry renderer.
        zed.tekkitoverhaul.crafting.AdvancedCraftingRegistry.advancedTable = pb.M;
        Class<?> type = Class.forName("zed.tekkitoverhaul.creative.CreativeCatalog");
        java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor(java.util.List.class);
        constructor.setAccessible(true);
        aan pig = new aan(383, 1, 90), chickenEgg = new aan(344, 1, 0);
        int creatureEgg = config.abyssalId("item.creatureEgg") + 256;
        aan siegeZombie = new aan(creatureEgg, 1, 9), siegeSkeleton = new aan(creatureEgg, 1, 10);
        Object catalog = constructor.newInstance(java.util.Arrays.asList(pig, pig.m(), chickenEgg, siegeZombie, siegeSkeleton));
        Object[] tabs = (Object[]) field(catalog, "tabs");
        int eggTab = -1;
        for (int i = 0; i < tabs.length; i++) if (tabs[i] != null && "Spawn Eggs".equals(field(tabs[i], "title"))) eggTab = i;
        check(eggTab >= 0, "Spawn Eggs category is present");
        java.util.List<?> entries = (java.util.List<?>) method(catalog, "entries", int.class, String.class).invoke(catalog, eggTab, "");
        check(entries.size() == zed.tekkitoverhaul.survival.CreatureEggs.entries().size(), "native plus ZTO entries deduplicate");
        for (Object entry : entries) {
            aan stack = (aan) field(entry, "stack");
            check(zed.tekkitoverhaul.survival.CreatureEggs.isEgg(stack) && stack.c != 344, "egg tab contains only usable eggs");
        }
        java.util.List<?> search = (java.util.List<?>) method(catalog, "entries", int.class, String.class)
                .invoke(catalog, 5, "custodian");
        check(search.size() == 1, "ordinary Creative search finds the Custodian egg");
        for (int tab : new int[] {1, 2}) {
            java.util.List<?> generalEntries = (java.util.List<?>) method(catalog, "entries", int.class, String.class)
                    .invoke(catalog, tab, "");
            for (Object entry : generalEntries)
                check(!zed.tekkitoverhaul.survival.CreatureEggs.isEgg((aan) field(entry, "stack")),
                        "general categories omit spawn eggs");
        }
        check(((java.util.List<?>) method(catalog, "entries", int.class, String.class)
                .invoke(catalog, 5, "siege")).isEmpty(), "legacy siege metadata stays out of Creative search");
        System.out.println("PASS Creative Spawn Eggs category, filtering, search and deduplication");
    }

    static void verifyEggs(xd world, Site site, ZtoConfig config) {
        int id = config.abyssalId("item.creatureEgg") + 256;
        yr egg = yr.e[id];
        int[] p = site.arenaCenter();
        yw player = fixturePlayer(world);
        place(player, p[0] + 0.5, p[1] + 1.5, p[2] + 0.5);
        player.aT.d = true;
        for (int metadata : new int[] {9, 10}) {
            aan retired = new aan(id, 1, metadata);
            check(!egg.a(retired, player, world, p[0], p[1], p[2], 1) && retired.a == 1,
                    "saved siege egg cannot spawn or consume its stack");
        }
        // Use the actual item entry point, with the viewer aimed into open chamber water.
        int consumed = 0;
        for (int slot = 0; slot < site.count(); slot++) if (site.consumed(slot)) consumed++;
        String status = site.custodian().status();
        aan stack = new aan(id, 1, 4);
        check(egg instanceof zed.tekkitoverhaul.survival.CreatureEggs
                && zed.tekkitoverhaul.survival.CreatureEggs.isEgg(stack), "real Custodian egg stack");
        egg.a(stack, world, player);
        EntityAbyssalCustodian test = null;
        for (Object entity : world.b)
            if (entity instanceof EntityAbyssalCustodian && !((EntityAbyssalCustodian) entity).c(new ady()))
                test = (EntityAbyssalCustodian) entity;
        check(test != null && stack.a == 1, "open-water egg inserts a temporary Custodian without consuming Creative stack");
        test.a(md.a(player),100);
        check(pearls(world) == 0 && status.equals(site.custodian().status()), "manual boss has no progression ownership");
        world.b(test);
        int still = 0;
        for (int slot = 0; slot < site.count(); slot++) if (site.consumed(slot)) still++;
        check(consumed == still, "manual creature cannot touch the finite patrol ledger");
        world.b(player);
        System.out.println("PASS egg catalog entries and independent manual ownership");
    }

    static void legacy(xd world, Site site, yw player) {
        admit(world, site, player);
        check(world.b(EntityAbyssalCustodian.class) == 1, "revision-one Custodian admission");
    }

    static void exercise(xd world, Site site) throws Exception {
        yw player = fixturePlayer(world);
        admit(world, site, player);
        EntityAbyssalCustodian boss = actor(world);
        check(boss.bb() == 80 && site.custodian().status().equals("reserved"), "native admission and health");
        System.out.printf("Custodian r%d site=%x position=%.2f,%.2f,%.2f%n", site.revision(), site.identity(),
                boss.o, boss.p, boss.q);
        double x = boss.o, y = boss.p, z = boss.q;
        // A controlled lane inside the actual loaded arena. All edits are confined to this disposable save.
        for (int bx = (int) Math.floor(x) - 1; bx <= Math.floor(x) + 1; bx++)
            for (int bz = (int) Math.floor(z) - 1; bz <= Math.floor(z) + 6; bz++)
                for (int by = (int) Math.floor(y); by <= Math.floor(y) + 3; by++)
                    world.g(bx, by, bz, 9);
        place(player, x, y + 1.25, z + 4);
        Object combat = field(boss, "combat");
        set(combat, "target", player);
        Method begin = method(combat, "begin", Attack.class);
        Method advance = method(combat, "advance");
        int health = player.bb();
        begin.invoke(combat, Attack.STRIKE);
        advance(advance, combat, Attack.STRIKE.windup);
        check(player.bb() == health, "strike wind-up causes no damage");
        advance(advance, combat, Attack.STRIKE.active);
        check(player.bb() == health - 5 && boss.strikeReach() > 4, "visible limb deals one native 5 HP strike: HP=" + player.bb() + " reach=" + boss.strikeReach() + " wet=" + player.H());
        advance(advance, combat, Attack.STRIKE.recover);
        check(player.bb() == health - 5, "recovery cannot deal a second strike hit");

        place(player, x, y + 1.25, z + 4);
        int wall = (int) Math.floor(z) + 2;
        for (int bx = (int) Math.floor(x) - 1; bx <= Math.floor(x) + 1; bx++)
            for (int by = (int) Math.floor(y); by <= Math.floor(y) + 3; by++) world.g(bx, by, wall, 1);
        begin.invoke(combat, Attack.STRIKE);
        advance(advance, combat, Attack.STRIKE.windup + Attack.STRIKE.active);
        check(player.bb() == health - 5 && boss.strikeReach() < 2, "wall clips strike and physical reach");
        begin.invoke(combat, Attack.PRESSURE);
        advance(advance, combat, Attack.PRESSURE.windup + 1);
        check(player.bb() == health - 5, "wall blocks pressure");
        for (int bx = (int) Math.floor(x) - 1; bx <= Math.floor(x) + 1; bx++)
            for (int by = (int) Math.floor(y); by <= Math.floor(y) + 3; by++) world.g(bx, by, wall, 9);
        player.l(20);
        clearHurt(player);
        begin.invoke(combat, Attack.PRESSURE);
        advance(advance, combat, Attack.PRESSURE.windup + 1);
        check(player.bb() == 18 && player.t > 0 && player.t <= 0.8, "open-water pressure damage and bounded displacement");

        Method landing = method(combat, "landing");
        bo destination = (bo) landing.invoke(combat);
        check(destination != null, "bounded search finds a native submerged Riftstep candidate");
        int bx = (int) Math.floor(destination.a), by = (int) Math.floor(destination.b), bz = (int) Math.floor(destination.c);
        int replaced = world.a(bx, by, bz);
        world.g(bx, by, bz, 1);
        set(combat, "destination", destination);
        begin.invoke(combat, Attack.BLINK);
        advance(advance, combat, Attack.BLINK.windup + 1);
        check(boss.o == x && boss.p == y && boss.q == z, "changed Riftstep destination cancels before movement");
        world.g(bx, by, bz, replaced);

        destination = (bo) landing.invoke(combat);
        check(destination != null, "clear Riftstep destination");
        set(combat, "destination", destination);
        begin.invoke(combat, Attack.BLINK);
        advance(advance, combat, Attack.BLINK.windup + 1);
        double span = Math.sqrt(square(boss.o - x) + square(boss.p - y) + square(boss.q - z));
        check(span >= CustodianBalance.BLINK_MIN && span <= CustodianBalance.BLINK_MAX && boss.bb() == 80,
                "Riftstep changes native position while preserving health");
        advance(advance, combat, Attack.BLINK.recover);
        place(boss, x, y, z);
        place(player, x, y + 1.25, z + 4);
        verifyResonance(world, boss, begin, advance, combat);
        verifyCompression(world, boss, combat, begin, advance);
        boss.l(40);
        boss.B();
        method(combat, "tick").invoke(combat);
        method(combat, "tick").invoke(combat);
        check(boss.pose() == EntityAbyssalCustodian.TRANSITION && boss.bb() == 40,
                "phase two transforms without healing");

        place(player, x, y + 25, z);
        player.B();
        method(combat, "tick").invoke(combat);
        check(boss.pose() == EntityAbyssalCustodian.DORMANT && boss.bb() == 40, "retreat stops the attack without healing");
        world.q = 0;
        boss.J_();
        check(!boss.G && boss.bb() == 40 && pearls(world) == 0, "Peaceful suspends boss without a reward or health reset");
        world.q = 2;

        ady tag = new ady();
        check(boss.c(tag), "site-owned boss writes native NBT");
        EntityAbyssalCustodian duplicate = new EntityAbyssalCustodian(world);
        duplicate.e(tag);
        check(world.a(duplicate), "insert duplicate for reconciliation");
        duplicate.J_();
        check(duplicate.G && pearls(world) == 0 && boss.M(), "duplicate removed without taking the canonical reward");
        player.A();
        System.out.println("PASS native strike, masonry, pressure, Riftstep, phase, retreat, Peaceful and duplicate reconciliation");
    }

    static void reopened(xd world, Site site) throws Exception {
        EntityAbyssalCustodian boss = actor(world);
        check(boss.bb() == 40 && (Boolean) field(boss, "phaseTwo"), "native reload preserves HP and phase");
        yw player = fixturePlayer(world);
        place(player, boss.o, boss.p + 1.2, boss.q + 3);
        int pearls = pearls(world);
        aan looting = new aan(267, 1, 0);
        looting.a(jt.n, 3);
        player.ap.a[player.ap.c] = looting;
        boss.a(md.a(player), 100);
        check(pearls(world) == pearls + 1 && site.custodian().status().contains("reward consumed"), "one real pearl on native defeat");
        boss.a(md.a(player));
        check(pearls(world) == pearls + 1, "duplicate death callback does not repeat reward");

        for (int i = 0; i < 12; i++) {
            jg enderman = new jg(world);
            enderman.c(boss.o, boss.p, boss.q, 0, 0);
            check(world.a(enderman), "place native Enderman");
            if (i < 6) enderman.a(md.a(player), 100);
            else enderman.a(md.k, 100);
        }
        check(pearls(world) == pearls + 1, "ordinary Enderman death cannot issue pearls");
        check(!site.custodian().available(), "terminal site cannot admit a replacement");
        EntityAbyssalCustodian unowned = new EntityAbyssalCustodian(world);
        unowned.c(boss.o, boss.p, boss.q, 0, 0);
        check(world.a(unowned) && !unowned.c(new ady()), "unowned preview actor cannot save");
        unowned.a(md.a(player), 100);
        check(pearls(world) == pearls + 1, "unowned preview actor cannot issue a pearl");
        verifyEe2(player);
        player.A();
        System.out.println("PASS native Custodian reload, exactly one legitimate pearl, and ordinary Enderman suppression");
    }

    static void terminal(xd world, Site site) {
        yw player = fixturePlayer(world);
        admit(world, site, player);
        check(site.custodian().status().contains("reward consumed"), "terminal record survived process-local reload");
        for (Object object : new ArrayList<>(world.b)) if (object instanceof EntityAbyssalCustodian) {
            EntityAbyssalCustodian corpse = (EntityAbyssalCustodian) object;
            corpse.J_();
            check(!corpse.M(), "terminal record rejects a stale native living snapshot on its first update");
            for (int tick = 1; tick <= 20 && !corpse.G; tick++) corpse.J_();
            check(corpse.G, "saved corpse expires or stale actor is reconciled");
        }
        check(pearls(world) <= 1, "reopen does not create another drop");
        world.b(player);
        System.out.println("PASS completed Observatory after native reload");
    }

    private static void verifyEe2(yw player) {
        EEMaps.addEMC(PEARL, 1024);
        EEMaps.addEMC(EYE, 1552);
        EEMaps.addEMC(1, 1);
        TileCondenser condenser = new TileCondenser();
        condenser.scaledEnergy = 500000;
        TransTabletData tablet = new TransTabletData("custodian-scarcity");
        tablet.player = player;
        tablet.latentEnergy = 5000;
        for (int id : new int[] {PEARL, EYE}) {
            EEBase.addPlayerKnowledge(player, id, 0);
            check(EEBase.playerKnows(player, id, 0), "loaded pre-existing EE2 knowledge");
        }
        condenser.a(0, new aan(PEARL, 1, 0));
        check(condenser.canCondense(), "EE2 positive-value condenser control");
        EmcIntegration integration = new EmcIntegration();
        integration.protectBeforeFirstWorld();
        for (int id : new int[] {PEARL, EYE}) {
            aan stack = new aan(id, 1, 0);
            condenser.a(0, stack);
            tablet.items[8] = stack;
            tablet.displayResults(5000);
            check(EEMaps.getEMC(id) == 0 && !condenser.canCondense() && condenser.target() == stack,
                    "preloaded EE2 condenser target retained and gated for " + id);
            for (int slot = 9; slot < tablet.items.length; slot++)
                check(tablet.items[slot] == null || tablet.items[slot].c != id, "tablet rejects protected output " + id);
        }
        check(EEMaps.getEMC(1) == 1, "unrelated EMC preserved");
        for (int id : new int[] {PEARL, EYE})
            check(EEBase.playerKnows(player, id, 0), "learnt identity itself retained");
        System.out.println("PASS pinned EE2 EMC, native condenser and tablet output gates for both identities");
    }

    private static void verifyCompression(xd world, EntityAbyssalCustodian boss, Object combat,
                                          Method begin, Method advance) throws Exception {
        double feet = boss.y.b;
        double full = boss.y.e - boss.y.b;
        begin.invoke(combat, Attack.COMPRESSION);
        advance(advance, combat, Attack.COMPRESSION.windup);
        check(Math.abs(boss.bodyScale() - 0.38) < 0.01 && boss.y.e - boss.y.b < full * 0.4
                && Math.abs(boss.y.b - feet) < 0.001, "native Compression AABB shrinks about its feet");
        ady tag = new ady();
        check(boss.c(tag), "save compressed legitimate actor");
        EntityAbyssalCustodian copy = new EntityAbyssalCustodian(world);
        copy.e(tag);
        check(Math.abs(copy.y.e - copy.y.b - (boss.y.e - boss.y.b)) < 0.001 && copy.bb() == boss.bb(),
                "compressed body dimensions and health round-trip through native NBT");
        int x = (int) Math.floor(boss.o), y = (int) Math.floor(feet) + 2, z = (int) Math.floor(boss.q);
        int previous = world.a(x,y,z);
        world.g(x,y,z,1);
        advance(advance, combat, Attack.COMPRESSION.active + Attack.COMPRESSION.recover);
        check(boss.bodyScale() < 0.65, "solid masonry blocks re-expansion");
        world.g(x,y,z,previous);
        advance(advance, combat, 54);
        check(boss.bodyScale() == 1 && Math.abs(boss.y.e - boss.y.b - full) < 0.001, "clear water permits re-expansion");
        System.out.println("PASS native Compression size, feet anchoring, blocked expansion and NBT");
    }

    private static void verifyResonance(xd world, EntityAbyssalCustodian boss, Method begin, Method advance, Object combat)
            throws Exception {
        yw player = (yw) field(combat, "target");
        for (int attempt = 0; attempt < 20; attempt++) {
            world.a(attempt * 20L);
            GuardianEncounters.tick(world, player);
        }
        int existing = world.b(EntityAbyssalEnderkin.class);
        Map<EntityAbyssalEnderkin, double[]> stations = new LinkedHashMap<>();
        for (Object object : new ArrayList<>(world.b)) if (object instanceof EntityAbyssalEnderkin) {
            EntityAbyssalEnderkin minion = (EntityAbyssalEnderkin) object;
            if (!minion.M()) continue;
            stations.put(minion, new double[] {minion.o, minion.p, minion.q});
            place(minion, boss.o + 1.6, boss.p, boss.q + 3);
        }
        begin.invoke(combat, Attack.RESONANCE);
        advance(advance, combat, Attack.RESONANCE.windup + 1);
        int responders = 0;
        for (Object object : world.b) if (object instanceof EntityAbyssalEnderkin
                && field(object, "resonanceTarget") == player) responders++;
        check(responders >= 1 && responders <= CustodianBalance.RESONANCE_LIMIT
                && world.b(EntityAbyssalEnderkin.class) == existing, "resonance rallies bounded surviving actors only: " + responders);
        advance(advance, combat, Attack.RESONANCE.recover);
        for (Map.Entry<EntityAbyssalEnderkin, double[]> station : stations.entrySet()) {
            double[] at = station.getValue();
            place(station.getKey(), at[0], at[1], at[2]);
        }
        System.out.println("PASS native same-site resonance with a finite response");
    }

    private static void admit(xd world, Site site, yw player) {
        int[] p = site.arenaCenter();
        place(player, p[0] + 0.5, p[1] + 1.4, p[2] + 4.5);
        world.a(2000L);
        GuardianEncounters.tick(world, player);
        GuardianEncounters.tick(world, player);
    }

    private static yw fixturePlayer(xd world) {
        yw player = new yw(world) { @Override public void ab() {} };
        check(world.a((nn) player), "fixture player inserted");
        return player;
    }

    private static void place(acq player, double x, double y, double z) {
        player.c(x, y, z, 0, 0);
        player.k.a(player, false);
        player.B();
    }

    private static EntityAbyssalCustodian actor(xd world) {
        EntityAbyssalCustodian boss = null;
        for (Object object : world.b) if (object instanceof EntityAbyssalCustodian && ((nn) object).M()) {
            check(boss == null, "exactly one living Custodian");
            boss = (EntityAbyssalCustodian) object;
        }
        check(boss != null, "living Custodian");
        return boss;
    }

    private static int pearls(xd world) {
        int count = 0;
        for (Object object : world.b) if (object instanceof fq && ((fq) object).a.c == PEARL) count += ((fq) object).a.a;
        return count;
    }

    private static void clearHurt(yw player) { player.Y = 0; }
    private static double square(double value) { return value * value; }
    private static void advance(Method advance, Object combat, int ticks) throws Exception {
        for (int i = 0; i < ticks && field(combat,"attack") != null; i++) {
            bo.b();
            // Reuse the vector pool as the native client does between ticks.
            bo.b(999, 999, 999);
            advance.invoke(combat);
        }
    }

    private static Field member(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private static Object field(Object owner, String name) throws Exception { return member(owner, name).get(owner); }
    private static void set(Object owner, String name, Object value) throws Exception { member(owner, name).set(owner, value); }
    private static Method method(Object owner, String name, Class<?>... parameters) throws Exception {
        Method method = owner.getClass().getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }
    private static void check(boolean met, String label) { if (!met) throw new AssertionError(label); }
}
