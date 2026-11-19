import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.FMLRegistry;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import zed.tekkitoverhaul.survival.aerial.AerialPredators;
import zed.tekkitoverhaul.survival.aerial.EntityMagpie;
import zed.tekkitoverhaul.survival.aerial.EntityMagpie.Pose;
import zed.tekkitoverhaul.survival.aerial.ForestWildlife;

/** Exercises the native item/entity lifecycle with a loaded, windowless pack world. */
public final class MagpieScavengeProbe {
    private static final int SEED = 295, FEATHER = 288, GOLD = 266;
    private static final int EGG_ID = 22011, EGG_STACK = EGG_ID + 256;

    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        Fixture world = new Fixture(root);
        Files.createFile(new File(root, "magpie-scavenging/.zto-disposable").toPath());
        FMLCommonHandler.instance().beginLoading(FMLClientHandler.instance());
        Field registeredMods = Loader.class.getDeclaredField("mods");
        registeredMods.setAccessible(true);
        registeredMods.set(Loader.instance(), Collections.emptyList());
        FMLRegistry.registerRegistry(new ClientRegistry());
        AerialPredators.register();
        Field random = nn.class.getDeclaredField("U");
        random.setAccessible(true);
        yw player = NaturalSpawnProbe.join(world);
        player.c(288.5, 4, 288.5, 0, 0);
        verifyTransfers(world, player, random);
        verifyGold(world, player);
        verifyEgg(world, player, random);
        verifyInventoryTheft(world, player, random);
        System.out.println("PASS magpie item conservation, native save/reload, release retry, retirement, natural gold and spawn egg");
    }

    private static void verifyInventoryTheft(Fixture world, yw player, Field random) throws Exception {
        player.aA = "MagpieProbe";
        player.c(300.5, 4, 290.5, 0, 0);
        player.ap.a(0, new aan(SEED, 4, 0));
        player.ap.a(9, new aan(264, 3, 0));
        aan tagged = new aan(SEED, 2, 0);
        ady identity = new ady();
        identity.a("Fixture", "protected");
        tagged.d(identity);
        player.ap.a(10, tagged);
        aan feathers = new aan(FEATHER, 3, 0);
        player.ap.a(11, feathers);
        int droppedBefore = count(world, FEATHER, false);

        EntityMagpie aborted = bird(world, random, 298.5, 290.5);
        waitForWarning(world, aborted);
        player.c(300.5, 4, 293.5, 0, 0);
        for (int t = 0; t < 20; ++t) tick(world, aborted);
        check(feathers.a == 3 && aborted.carriedItem() == null, "walking away aborts the warned theft");
        aborted.A();

        player.c(300.5, 4, 290.5, 0, 0);
        EntityMagpie swapped = bird(world, random, 298.5, 290.5);
        waitForWarning(world, swapped);
        player.ap.a(12, feathers);
        player.ap.a(11, null);
        for (int t = 0; t < 20; ++t) tick(world, swapped);
        check(feathers.a == 3 && swapped.carriedItem() == null, "moving the observed inventory stack aborts the theft");
        swapped.A();
        player.ap.a(11, feathers);
        player.ap.a(12, null);

        EntityMagpie creative = bird(world, random, 298.5, 290.5);
        player.aT.d = true;
        for (int t = 0; t < 65; ++t) tick(world, creative);
        check(feathers.a == 3 && creative.carriedItem() == null, "Creative inventory is protected");
        player.aT.d = false;
        creative.A();

        EntityMagpie bird = bird(world, random, 298.5, 290.5);
        waitForWarning(world, bird);
        check(feathers.a == 3 && bird.carriedItem() == null, "warning precedes inventory mutation");
        untilCarrying(world, bird);
        check(feathers.a == 2 && player.ap.a[0].a == 4 && player.ap.a[9].a == 3 && tagged.a == 2,
                "only one plain feather leaves the main inventory");
        check(bird.pose() == Pose.FLY && bird.carriedItem().c == FEATHER && bird.carriedItem().a == 1,
                "native flight starts with exactly one stolen item");

        ady saved = new ady();
        check(bird.c(saved) && "MagpieProbe".equals(saved.j("ZtoMagpieStolenFrom")),
                "native saved bird records the owner's name while carrying");
        world.b(Collections.singletonList(bird));
        world.n();
        EntityMagpie loaded = (EntityMagpie) aao.a(saved, world);
        check(loaded != null && world.a(loaded) && loaded.carriedItem() != null, "unloading retains the one carried item");
        untilReleased(world, loaded);
        check(count(world, FEATHER, false) == droppedBefore + 1, "native release returns exactly one feather after reload");
        player.c(loaded.o + 0.8, 4, loaded.q, 0, 0);
        for (int t = 0; t < 100; ++t) tick(world, loaded);
        check(feathers.a == 2 && loaded.carriedItem() == null && count(world, FEATHER, false) == droppedBefore + 1,
                "reload and recovery cannot reopen the bird's one theft opportunity");
        fq recovered = null;
        for (Object entry : world.b) if (entry instanceof fq) {
            fq item = (fq) entry;
            if (!item.G && item.a.c == FEATHER && item.a.o() == null) recovered = item;
        }
        check(recovered != null, "released feather is available for pickup");
        for (int i = 0; i < 10; ++i) world.a(recovered, true);
        recovered.a(player);
        check(recovered.G && feathers.a == 3 && count(world, FEATHER, false) == droppedBefore,
                "native item collision restores the original count without a duplicate");
        loaded.A();
        System.out.println("PASS magpie warning, player retreat, Creative and inventory protections, one-item theft, flight and exactly-once recovery");
    }

    private static void waitForWarning(Fixture world, EntityMagpie bird) {
        for (int t = 0; t < 45 && bird.pose() != Pose.PECK; ++t) tick(world, bird);
        check(bird.pose() == Pose.PECK, "bird walks into reach and visibly pecks before committing");
    }

    private static void verifyTransfers(Fixture world, yw player, Field random) throws Exception {
        EntityMagpie bird = bird(world, random, 281.5, 282.5);
        fq one = placeStack(world, bird, SEED, 1);
        untilCarrying(world, bird);
        check(one.G && one.a.a == 0, "a single stack transfers exactly once");
        checkStack(bird.carriedItem(), SEED, 1);
        double startX = bird.o, startZ = bird.q;
        untilReleased(world, bird);
        check(Math.hypot(bird.o - startX, bird.q - startZ) > 1.7 && countTagged(world, SEED) == 1,
                "flat ground release relocates a single intact seed");
        bird.A();

        EntityMagpie carrier = bird(world, random, 294.5, 282.5);
        fq many = placeStack(world, carrier, FEATHER, 3);
        untilCarrying(world, carrier);
        check(!many.G && many.a.a == 2, "a partial stack stays on the ground");
        checkStack(carrier.carriedItem(), FEATHER, 1);
        ady saved = new ady();
        check(carrier.c(saved), "save while holding one feather");
        world.b(Collections.singletonList(carrier));
        world.n();
        EntityMagpie reloaded = (EntityMagpie) aao.a(saved, world);
        check(reloaded != null && world.a(reloaded), "unload the original and insert the loaded bird");
        checkStack(reloaded.carriedItem(), FEATHER, 1);
        world.g(294, 4, 284, 18);
        untilReleased(world, reloaded);
        check(countTagged(world, FEATHER) == 3 && reloaded.p > 4.95,
                "a reachable low leaf perch gets one intact feather after reload");
        reloaded.A();

        EntityMagpie interrupted = bird(world, random, 301.5, 282.5);
        fq missing = placeStack(world, interrupted, SEED, 1);
        for (int t = 0; t < 12 && interrupted.pose() != Pose.PECK; ++t) tick(world, interrupted);
        check(interrupted.pose() == Pose.PECK, "bird notices the item before contact");
        missing.A();
        for (int t = 0; t < 20; ++t) tick(world, interrupted);
        check(interrupted.carriedItem() == null && countTagged(world, SEED) == 1,
                "an item that disappears during pecking does not transfer");
        interrupted.A();

        EntityMagpie victim = bird(world, random, 301.5, 291.5);
        placeStack(world, victim, SEED, 1);
        untilCarrying(world, victim);
        world.rejectItems = true;
        check(victim.a(md.a(player), 10), "native hit kills the carrying bird");
        checkStack(victim.carriedItem(), SEED, 1);
        for (int t = 0; t < 28; ++t) tick(world, victim);
        check(!victim.G && victim.carriedItem() != null && countTagged(world, SEED) == 1,
                "refused item spawn leaves ownership with the dying bird");
        world.rejectItems = false;
        for (int t = 0; t < 20 && !victim.G; ++t) tick(world, victim);
        victim.A();
        check(victim.G && victim.carriedItem() == null && countTagged(world, SEED) == 2,
                "death and retirement overlap without duplication, even after refusal");

        EntityMagpie retiring = bird(world, random, 300.5, 303.5);
        placeStack(world, retiring, SEED, 1);
        untilCarrying(world, retiring);
        player.c(900, 4, 900, 0, 0);
        for (int t = 0; t < 5 && !retiring.G; ++t) tick(world, retiring);
        check(retiring.G && retiring.carriedItem() == null && countTagged(world, SEED) == 3,
                "ordinary distance despawn resolves player-dropped items");
        player.c(288.5, 4, 288.5, 0, 0);

        EntityMagpie old = bird(world, random, 303.5, 304.5);
        placeStack(world, old, SEED, 1);
        untilCarrying(world, old);
        ady aged = new ady();
        check(old.c(aged), "save a carrying bird before age retirement");
        aged.a("ZtoMagpieAge", 5999);
        world.b(Collections.singletonList(old));
        world.n();
        EntityMagpie expired = (EntityMagpie) aao.a(aged, world);
        check(expired != null && world.a(expired), "loaded bird reaches its final age tick");
        tick(world, expired);
        check(expired.G && countTagged(world, SEED) == 4, "the age limit returns the player stack once");
        world.n();
    }

    private static void verifyGold(Fixture world, yw player) throws Exception {
        paint(world, 4);
        ForestWildlife wildlife = new ForestWildlife(true);
        wildlife.bind(world);
        world.rejectBirds = true;
        world.r = new Random(345L) {
            @Override public int nextInt(int bound) {
                if (bound == 100) return world.goldRolls++ == 1 ? 99 : 0;
                return super.nextInt(bound);
            }
        };
        pulse(wildlife, player, 100);
        check(world.goldRolls == 0 && wildlife.loaded() == 0, "failed insertion never rolls for gold");
        world.rejectBirds = false;
        pulse(wildlife, player, 200);
        EntityMagpie first = birds(world).get(0);
        checkGold(first);
        check(world.goldRolls == 1, "one roll after the first successful natural insertion");
        pulse(wildlife, player, 600);
        EntityMagpie second = birds(world).get(1);
        check(second.carriedItem() == null && world.goldRolls == 2, "non-matching natural roll leaves the beak empty");
        ady saved = new ady();
        check(first.c(saved), "save a natural gold carrier");
        world.b(Collections.singletonList(first));
        world.n();
        first = (EntityMagpie) aao.a(saved, world);
        check(first != null && world.a(first) && world.goldRolls == 2, "reload leaves the admission roll untouched");
        checkGold(first);
        second.A();
        first.c(297.5, 4, 288.5, -90, 0);
        player.c(295, 4, 288.5, 0, 0);
        player.l = player.o - 0.24;
        boolean escaped = false;
        for (int t = 0; t < 110 && first.carriedItem() != null; ++t) {
            tick(world, first);
            if (first.pose() == Pose.FLY && first.carriedItem() != null) escaped = true;
        }
        check(escaped && first.carriedItem() == null && count(world, GOLD, false) == 1,
                "a fast approach prompts a short gold-carrying escape and one release");
        first.A();
        player.c(288.5, 4, 288.5, 0, 0);

        pulse(wildlife, player, 600);
        EntityMagpie killed = birds(world).get(0);
        checkGold(killed);
        player.ap.a[0] = new aan(276, 1, 0);
        player.ap.a[0].a(jt.n, 3);
        player.ap.c = 0;
        killed.a(md.a(player), 15);
        for (int t = 0; t < 30; ++t) tick(world, killed);
        killed.A();
        check(count(world, GOLD, false) == 2, "Looting III death yields exactly one additional ingot");
        player.ap.a[0] = null;

        pulse(wildlife, player, 600);
        EntityMagpie untouched = birds(world).get(0);
        checkGold(untouched);
        player.c(900, 4, 900, 0, 0);
        tick(world, untouched);
        check(untouched.G && count(world, GOLD, false) == 2 && world.goldRolls == 4,
                "untouched gold expires with natural retirement and adds no ground item");
        player.c(288.5, 4, 288.5, 0, 0);
        world.n();
    }

    private static void verifyEgg(Fixture world, yw player, Field random) throws Exception {
        AerialPredators.registerMagpieEgg(EGG_ID);
        try {
            AerialPredators.registerMagpieEgg(EGG_ID);
            throw new AssertionError("registry accepted a collision");
        } catch (IllegalStateException occupied) {
            check(occupied.getMessage().contains("magpieEgg"), "collision names the configurable key");
        }
        yr egg = yr.e[EGG_STACK];
        ArrayList<aan> creative = new ArrayList<aan>();
        egg.addCreativeItems(creative);
        Properties names = new Properties();
        FMLCommonHandler.instance().handleLanguageLoad(names, "en_US");
        check(creative.size() == 1 && creative.get(0).c == EGG_STACK && egg.c()
                && names.containsValue("Magpie Spawn Egg"), "egg is localized and uses the native Creative catalog");
        boolean listed = false;
        // The pinned NEI creative scan reads this same container's item list.
        for (Object entry : new os(player).a) {
            if (entry instanceof aan && ((aan) entry).c == EGG_STACK) listed = true;
        }
        check(listed, "native Creative and NEI catalog source contains the egg");
        paint(world, 1);
        ForestWildlife disabled = new ForestWildlife(false);
        disabled.bind(world);
        pulse(disabled, player, 800);
        check(birds(world).isEmpty(), "no ambient admission in the disabled non-forest fixture");
        aan stack = new aan(egg, 2, 0);
        world.g(308, 4, 288, 20);
        check(!egg.a(stack, player, world, 308, 3, 288, 1) && stack.a == 2, "blocked placement preserves stack");
        check(!egg.a(stack, player, world, 8000, 3, 8000, 1) && stack.a == 2
                && !world.b(8000, 0, 8000, 8000, 0, 8000), "unloaded placement keeps the target chunk unavailable");
        check(egg.a(stack, player, world, 309, 3, 288, 1) && stack.a == 1, "Survival use consumes one safe egg");
        EntityMagpie spawned = birds(world).get(0);
        check(spawned.carriedItem() == null && "ZTO.Magpie".equals(aao.b(spawned)) && world.goldRolls == 4,
                "non-forest egg places the saved species empty-beaked and without a gold roll");
        check(!egg.a(stack, player, world, 309, 3, 288, 1) && stack.a == 1, "an occupied site rejects another egg");
        player.aT.d = true;
        world.g(310, 16, 288, 1);
        check(egg.a(stack, player, world, 310, 3, 288, 1) && stack.a == 1,
                "Creative egg can deliberately place a bird under deep cover and preserves the stack");
        world.g(310, 16, 288, 0);
        player.aT.d = false;
        forcePickup(spawned, random);
        placeStack(world, spawned, FEATHER, 1);
        untilCarrying(world, spawned);
        checkStack(spawned.carriedItem(), FEATHER, 1);
        spawned.A();
    }

    private static EntityMagpie bird(Fixture world, Field random, double x, double z) throws Exception {
        EntityMagpie bird = new EntityMagpie(world);
        bird.c(x, 4, z, 0, 0);
        forcePickup(bird, random);
        check(world.a(bird), "fixture magpie inserted");
        return bird;
    }

    private static void forcePickup(EntityMagpie bird, Field random) throws Exception {
        random.set(bird, new Random(19L) {
            @Override public int nextInt(int bound) {
                if (bound == 5 || bound == 8 || bound == 10) return 0;
                if (bound == 111) return 55;
                return super.nextInt(bound);
            }
        });
    }

    private static fq placeStack(Fixture world, EntityMagpie bird, int id, int size) {
        aan stack = new aan(id, size, 0);
        ady tag = new ady();
        tag.a("ZtoProbeIdentity", "owner item");
        stack.d(tag);
        fq item = new fq(world, bird.o + 0.4, 4.1, bird.q, stack);
        check(world.a(item), "native dropped stack inserted");
        return item;
    }

    private static void checkStack(aan stack, int id, int size) {
        check(stack != null && stack.c == id && stack.a == size && stack.o() != null
                && "owner item".equals(stack.o().j("ZtoProbeIdentity")), "exact stack and tag preserved");
    }

    private static void checkGold(EntityMagpie bird) {
        aan item = bird.carriedItem();
        check(item != null && item.c == GOLD && item.a == 1, "matching natural roll places exactly one gold ingot");
    }

    private static void untilCarrying(Fixture world, EntityMagpie bird) {
        for (int t = 0; t < 80 && bird.carriedItem() == null; ++t) tick(world, bird);
        check(bird.carriedItem() != null, "native tick eventually carries the chosen stack");
    }

    private static void untilReleased(Fixture world, EntityMagpie bird) {
        for (int t = 0; t < 125 && bird.carriedItem() != null; ++t) tick(world, bird);
        check(bird.carriedItem() == null, "a valid local release returns the carried item");
    }

    private static void tick(Fixture world, EntityMagpie bird) {
        wu.b();
        bo.b();
        world.a(bird, true);
    }

    private static List<EntityMagpie> birds(xd world) {
        List<EntityMagpie> birds = new ArrayList<EntityMagpie>();
        for (Object entry : world.b) if (entry instanceof EntityMagpie && !((nn) entry).G) birds.add((EntityMagpie) entry);
        return birds;
    }

    private static int countTagged(xd world, int id) { return count(world, id, true); }

    private static int count(xd world, int id, boolean tagged) {
        int result = 0;
        for (Object entry : world.b) {
            if (!(entry instanceof fq) || ((fq) entry).G) continue;
            aan item = ((fq) entry).a;
            if (item.c == id && (!tagged || item.o() != null && "owner item".equals(item.o().j("ZtoProbeIdentity")))) {
                result += item.a;
            }
        }
        return result;
    }

    private static void paint(xd world, int biome) {
        byte[] ids = new byte[256];
        Arrays.fill(ids, (byte) biome);
        for (int x = 15; x <= 21; ++x) for (int z = 15; z <= 21; ++z) world.d(x, z).a(ids.clone());
    }

    private static void pulse(ForestWildlife wildlife, yw player, int ticks) {
        for (int t = 0; t < ticks; ++t) wildlife.tick(player, false);
    }

    private static final class Fixture extends xd {
        boolean rejectItems, rejectBirds;
        int goldRolls;

        Fixture(File root) {
            super(new aia(root, "magpie-scavenging", false), "magpie-scavenging", NaturalSpawnProbe.SETTINGS);
            q = 2;
            a(6000L);
            q();
            for (int x = 14; x <= 22; ++x) for (int z = 14; z <= 22; ++z) d(x, z);
        }

        @Override public boolean a(nn entity) {
            return !(rejectItems && entity instanceof fq) && !(rejectBirds && entity instanceof EntityMagpie) && super.a(entity);
        }
        @Override public void a(nn entity, String sound, float volume, float pitch) {}
        @Override public void a(double x, double y, double z, String sound, float volume, float pitch) {}
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
