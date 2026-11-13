import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import codechicken.nei.NEIConfig;
import codechicken.nei.NEIController;
import codechicken.nei.NEIUtils;
import codechicken.nei.ItemPanelStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;
import zed.tekkitoverhaul.survival.tactical.EntityBreacher;
import zed.tekkitoverhaul.survival.tactical.EntitySkirmisher;
import zed.tekkitoverhaul.survival.tactical.TacticalHostile;
import zed.tekkitoverhaul.survival.tactical.TacticalHostiles;

/** Runs against the reobfuscated mod and pack on the local game thread. */
public final class Alpha7RuntimeProbe implements ITickHandler {
    private static final int X = 34, Y = 121, Z = 215;
    private static String save, mode;
    private static File directory;
    private static Field focus, target, impacts;
    private boolean done, aimed, retreated, moved;
    private boolean capturedWindup, capturedShot, capturedPressureWindup, capturedPressure, capturedPursuit;
    private int stage, shots, firstShot, health, measured, peakEntities;
    private int aimFrame, pressureFrame, pursuitFrame;
    private long since, previousTick, elapsedNanos;
    private double initialX, initialZ;
    private EntitySkirmisher skirmisher;
    private EntityBreacher breacher;
    private acq[] lineup;
    private final Set<nm> observed = Collections.newSetFromMap(new IdentityHashMap<nm, Boolean>());

    /** Keep prototype class resolution after FML has added the mod JAR to the classpath. */
    public static final class Bootstrap implements ITickHandler {
        private boolean ready;

        public static void main(String[] args) throws Exception {
            File game = new File(args[0]).getCanonicalFile();
            String world = args[1];
            if (!"survival-hud-runtime".equals(game.getParentFile().getParentFile().getName())
                    || !world.matches("ZTO-Test-[a-f0-9]{32}")
                    || !new File(game, "saves/" + world + "/.zto-disposable-test").isFile()) {
                throw new IllegalArgumentException("Use a marked save in the isolated Tekkit Dev copy");
            }
            Field gameDirectory = Minecraft.class.getDeclaredField("aj");
            gameDirectory.setAccessible(true);
            gameDirectory.set(null, game);
            System.setProperty("zto.alpha7.save", world);
            System.setProperty("zto.alpha7.mode", args[2]);
            FMLCommonHandler.instance().registerTickHandler(new Bootstrap());
            Minecraft.main(new String[] {"ZTOTest"});
        }

        public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
        public String getLabel() { return "Alpha 7 bootstrap"; }
        public void tickStart(EnumSet<TickType> ticks, Object... data) {}
        public void tickEnd(EnumSet<TickType> ticks, Object... data) {
            if (ready) return;
            Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
            if (game == null || !(game.s instanceof xt)) return;
            FMLCommonHandler.instance().registerTickHandler(new Alpha7RuntimeProbe());
            ready = true;
        }
    }

    public EnumSet<TickType> ticks() { return EnumSet.of(TickType.RENDER); }
    public String getLabel() { return "Alpha 7 tactical runtime"; }

    public void tickStart(EnumSet<TickType> ticks, Object... data) {
        if (done || focus == null) return;
        Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
        try {
            focus.setLong(game.u, System.currentTimeMillis());
        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);
        }
    }

    public void tickEnd(EnumSet<TickType> ticks, Object... data) {
        if (done) return;
        Minecraft game = (Minecraft) FMLCommonHandler.instance().getMinecraftInstance();
        if (game == null) return;
        try {
            if (stage == 0) {
                directory = (File) field(Minecraft.class, "aj").get(null);
                save = System.getProperty("zto.alpha7.save");
                mode = System.getProperty("zto.alpha7.mode");
                focus = field(lr.class, "Y");
                target = field(TacticalHostile.class, "target");
                impacts = field(EntityBreacher.class, "impacts");
                check(!Boolean.getBoolean("zto.devBridge.enabled"), "diagnostics disabled");
                System.out.println("ALPHA7 Java " + System.getProperty("java.version"));
                open(game);
                arena(game);
                next(game);
            } else if (game.f.w() != previousTick) {
                previousTick = game.f.w();
                if ("stress".equals(mode)) stress(game);
                else if ("visual".equals(mode)) visual(game);
                else if (mode.startsWith("creative")) creative(game);
                else combat(game);
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            game.f();
            done = true;
        }
    }

    private void combat(Minecraft game) throws Exception {
        long age = game.f.w() - since;
        switch (stage) {
            case 1:
                if (age < 25) return;
                skirmisher = (EntitySkirmisher) spawn(game, TacticalHostiles.SKIRMISHER, 8, 0);
                initialZ = skirmisher.q;
                next(game);
                break;
            case 2:
                observeArrows(game);
                if (aimFrame < 8 && (skirmisher.isAiming() || shots > 0)
                        && (age % 2 == 0)) screenshot(game, "skirmisher-motion-" + aimFrame++);
                if (skirmisher.getAimPose(1) > 0.85F && !capturedWindup) {
                    screenshot(game, "skirmisher-windup");
                    capturedWindup = true;
                }
                if (shots > 0 && skirmisher.V - firstShot <= 3 && !capturedShot) {
                    screenshot(game, "skirmisher-release");
                    capturedShot = true;
                }
                aimed |= skirmisher.isAiming();
                moved |= Math.abs(skirmisher.q - initialZ) > 0.75D;
                if (shots < 2 && age < 180) return;
                check(aimed && moved && shots >= 2 && capturedWindup && capturedShot,
                        "Skirmisher aims, shoots twice and strafes");
                check(target.get(skirmisher) == game.h, "Skirmisher acquires the player");
                System.out.println("ALPHA7 Skirmisher aimed, strafed and fired two spaced shots");
                screenshot(game, "skirmisher-strafe");
                player(game, skirmisher.o - X - 2, skirmisher.q - Z);
                initialX = skirmisher.o;
                initialZ = skirmisher.q;
                next(game);
                break;
            case 3:
                retreated |= (skirmisher.o - initialX) > 0.75D && skirmisher.f(game.h) > 9.0D;
                if (!retreated && age < 60) return;
                check(retreated, "Skirmisher retreats from a close player");
                System.out.println("ALPHA7 Skirmisher retreated");
                game.f.b(skirmisher);
                clearArrows(game);
                player(game, 0.5D, 0.5D);
                skirmisher = (EntitySkirmisher) spawn(game, TacticalHostiles.SKIRMISHER, 8, 0);
                shots = 0;
                next(game);
                break;
            case 4:
                if (!skirmisher.isAiming() && age < 60) return;
                check(skirmisher.isAiming(), "Skirmisher prepares the covered shot");
                wall(game, 4, 1);
                next(game);
                break;
            case 5:
                observeArrows(game);
                if (age < 125) return;
                check(shots == 0 && !skirmisher.isAiming() && target.get(skirmisher) == null,
                        "cover cancels the shot and breaks pursuit");
                System.out.println("ALPHA7 cover cancelled the shot and pursuit");
                game.f.b(skirmisher);
                clearArrows(game);
                wall(game, 4, 0);
                breacher = (EntityBreacher) spawn(game, TacticalHostiles.BREACHER, 8, 0);
                game.h.l(20);
                health = game.h.bb();
                next(game);
                break;
            case 6:
                if (breacher.o < X + 8 && !capturedPursuit) {
                    screenshot(game, "breacher-pursuit");
                    capturedPursuit = true;
                }
                if (breacher.o < X + 8 && pursuitFrame < 5 && age % 3 == 0)
                    screenshot(game, "breacher-walk-" + pursuitFrame++);
                if (breacher.o > X + 6 && age < 65) return;
                check(breacher.o <= X + 6 && target.get(breacher) == game.h, "Breacher approaches in the open");
                wall(game, 4, 1);
                // Set the upper half first so lower-half neighbor updates see a complete door.
                game.f.b(X + 4, Y + 1, Z, 64, 8);
                game.f.d(X + 4, Y, Z, 64, 0);
                next(game);
                break;
            case 7:
                int pressure = impacts.getInt(breacher);
                float phase = breacher.i(1F);
                if (pressure == 1 && phase > 0 && phase <= 0.85F && pressureFrame < 8) {
                    int soundTick = field(EntityBreacher.class, "pressureSoundTick").getInt(breacher);
                    if (phase < 0.39F) check(soundTick == breacher.V + EntityBreacher.PRESSURE_CONTACT_TICKS
                            - Math.round(phase * 10), "sound awaits contact");
                    else check(soundTick == -1, "sound dispatched at contact");
                    screenshot(game, "breacher-pressure-motion-" + pressureFrame++);
                }
                if (pressure > 0 && !capturedPressureWindup && phase >= 0.1F && phase <= 0.3F) {
                    screenshot(game, "breacher-pressure-windup");
                    capturedPressureWindup = true;
                }
                if (pressure > 0 && !capturedPressure && phase >= 0.4F && phase <= 0.7F) {
                    screenshot(game, "breacher-pressure");
                    capturedPressure = true;
                }
                if ((pressure < 3 || !capturedPressure) && age < 205) return;
                check(capturedPressure, "pressure drives the attack pose");
                check(impacts.getInt(breacher) == 3, "Breacher applies exactly three impacts, got "
                        + impacts.getInt(breacher) + " at " + breacher.o + "," + breacher.p + "," + breacher.q
                        + " door=" + game.f.a(X + 4, Y, Z) + ":" + game.f.e(X + 4, Y, Z));
                check(game.f.a(X + 4, Y, Z) == 64 && game.f.e(X + 4, Y, Z) == 0
                        && game.f.a(X + 4, Y + 1, Z) == 64 && breacher.o > X + 4 && game.h.bb() == health,
                        "closed door and player behind it remain intact");
                System.out.println("ALPHA7 Breacher stopped after three impacts without changing the door");
                game.f.f(X + 4, Y, Z, 4);
                next(game);
                break;
            case 8:
                if (age < 165 && (breacher.o >= X + 4 || game.h.bb() >= health)) return;
                check(breacher.o < X + 4 && game.h.bb() < health,
                        "Breacher uses the reopened entrance and lands melee, x=" + breacher.o
                                + " health=" + game.h.bb() + "/" + health);
                check(game.f.a(X + 4, Y, Z) == 64 && game.f.e(X + 4, Y, Z) == 4,
                        "open door survives melee");
                System.out.println("ALPHA7 Breacher traversed the reopened entrance and landed melee");
                screenshot(game, "breacher-melee");
                skirmisher = (EntitySkirmisher) spawn(game, TacticalHostiles.SKIRMISHER, 7, -3);
                skirmisher.l(9);
                breacher.l(13);
                wall(game, 4, 0);
                game.f.g(X + 4, Y, Z, 0);
                game.f.g(X + 4, Y + 1, Z, 0);
                skirmisher.c(X + 5.5D, Y, Z - 0.5D, 90, 0);
                breacher.c(X + 5.5D, Y, Z + 1.2D, 90, 0);
                saveQuit(game);
                check(game.f == null && game.s instanceof xt, "Save and Quit returned to title");
                open(game);
                next(game);
                break;
            case 9:
                if (age < 10) return;
                List<TacticalHostile> loaded = mobs(game);
                check(loaded.size() == 2, "exactly two prototypes loaded from the save");
                screenshot(game, "reopened");
                for (TacticalHostile mob : loaded) {
                    check(mob.bb() == (mob instanceof EntitySkirmisher ? 9 : 13), "health persists through Save and Quit");
                    checkRenderer(game, mob);
                    check(mob.a(md.j, 100), "normal damage path kills the prototype");
                }
                next(game);
                break;
            case 10:
                if (age < 30) return;
                check(mobs(game).isEmpty(), "death removes both prototypes");
                System.out.println("ALPHA7 Save and Quit restored both entities and health, then death removed both");
                finish(game);
                break;
            default:
                throw new IllegalStateException("Unexpected combat stage");
        }
    }

    private void observeArrows(Minecraft game) {
        for (Object entity : game.f.b) {
            if (!(entity instanceof nm)) continue;
            nm arrow = (nm) entity;
            if (arrow.c != skirmisher || !observed.add(arrow)) continue;
            if (shots > 0) check(skirmisher.V - firstShot >= 62, "shot interval includes cooldown and windup");
            firstShot = skirmisher.V;
            check(arrow.n() == 1.5D, "projectile damage coefficient");
            shots++;
        }
    }

    private static void clearArrows(Minecraft game) {
        for (Object entity : game.f.b) if (entity instanceof nm) ((nm) entity).A();
    }

    private void stress(Minecraft game) throws Exception {
        long age = game.f.w() - since;
        if (stage == 1 && age >= 40) {
            for (int i = 0; i < 6; i++) {
                spawn(game, TacticalHostiles.SKIRMISHER, 8, i - 3);
                spawn(game, TacticalHostiles.BREACHER, -8, i - 3);
            }
            next(game);
        } else if (stage == 2) {
            long now = System.nanoTime();
            if (elapsedNanos != 0) measured++;
            if (elapsedNanos == 0) elapsedNanos = now;
            List<TacticalHostile> loaded = mobs(game);
            peakEntities = Math.max(peakEntities, loaded.size());
            if (age < 140) return;
            check(peakEntities == 12 && loaded.size() == 12, "all twelve prototypes survive the stress window");
            int active = 0;
            for (TacticalHostile mob : loaded) {
                if (target.get(mob) == game.h) active++;
            }
            check(active >= 8, "most prototypes still pursue the player");
            System.out.println("ALPHA7 stress ticks=" + age + " sampledTicks=" + measured
                    + " wallMillis=" + (now - elapsedNanos) / 1000000 + " active=" + active + " peak=" + peakEntities);
            screenshot(game, "stress");
            finish(game);
        }
    }

    private void visual(Minecraft game) throws Exception {
        long age = game.f.w() - since;
        if (stage == 1 && age >= 35) {
            skirmisher = (EntitySkirmisher) spawn(game, TacticalHostiles.SKIRMISHER, 5, -1);
            breacher = (EntityBreacher) spawn(game, TacticalHostiles.BREACHER, 5, 0);
            rv player = new rv(game.f, "ZTOTest");
            player.Z = null;
            ajg zombie = new ajg(game.f) {
                @Override
                public boolean T() { return false; } // Keep the reference silhouette visible in daylight.
            };
            lineup = new acq[] {player, skirmisher, breacher, new jg(game.f), zombie,
                    (acq) spawn(game, TacticalHostiles.SKIRMISHER, 6, 4),
                    (acq) spawn(game, TacticalHostiles.BREACHER, 6, 5)};
            lineUp();
            for (acq mob : lineup) if (!game.f.b.contains(mob)) check(game.f.a(mob), "lineup insertion");
            game.h.aT.a = true;
            player(game, 1.2D, 0.75D);
            game.A.D = true;
            game.A.O = 0.35F;
            next(game);
        } else if (stage >= 2 && stage <= 5) {
            if (age >= 12) {
                if (stage == 2) {
                    check(game.f.o(X + 5, Y + 1, Z) == 15, "daylight reaches the lineup");
                    screenshot(game, "lineup-daylight");
                    game.f.a(18000L);
                    torches(game, 50);
                } else if (stage == 3) {
                    check(game.f.o(X + 5, Y + 1, Z) >= 10, "torchlight reaches the lineup");
                    screenshot(game, "lineup-torchlight");
                    torches(game, 0);
                } else if (stage == 4) {
                    check(game.f.o(X + 5, Y + 1, Z) <= 5, "lineup is in low light");
                    screenshot(game, "lineup-low-light");
                    game.f.a(6000L);
                    for (int dz = -2; dz <= 1; dz++) game.f.g(X + 5, Y + 2, Z + dz, 1);
                } else {
                    check(clearance(skirmisher) < 0.75F && clearance(breacher) < 0.85F,
                            "two-block ceiling fits both render envelopes");
                    screenshot(game, "two-block-clearance");
                    game.A.D = false;
                    finish(game);
                    return;
                }
                next(game);
            }
            lineUp();
        }
    }

    private void creative(Minecraft game) throws Exception {
        long age = game.f.w() - since;
        if (stage == 1 && age >= 25) {
            check(mobs(game).isEmpty(), "fresh world has no naturally spawned Enderkin");
            game.c = new aff(game);
            aff.d(game.h);
            game.h.ap.a(0, null);
            game.h.ap.a(1, null);
            game.a(new sr(game.h));
            next(game);
            return;
        }
        if (stage == 2) {
            if (game.s instanceof sr && "creative".equals(mode) && age < 10) return;
            boolean backport = "creative".equals(mode);
            gb screen = (gb) game.s;
            check(backport ? screen.getClass().getName().endsWith(".GuiCreativeBackport") : screen instanceof sr,
                    "configured Creative screen is active");
            check(screen.controller != null, "NEI controller attached");
            checkEggCatalog(game, screen, backport);
            game.a((vp) null);
            check(mobs(game).isEmpty(), "catalog transfer did not spawn entities");
            for (int i = 0; i < 2; i++) {
                aan egg = game.h.ap.k_(i);
                int count = egg.a;
                int z = Z + i * 3;
                game.f.g(X + 6, Y, z, 1);
                check(!egg.a().a(egg, game.h, game.f, X + 6, Y - 1, z, 1), "solid target rejects egg");
                check(egg.a == count && mobs(game).size() == i * 2, "failed placement leaves inventory and world intact");
                game.f.g(X + 6, Y, z, 0);
                for (int x = X + 6; x <= X + 8; x += 2) {
                    check(game.c.a(game.h, game.f, egg, x, Y - 1, z, 1), "Creative controller uses egg");
                    check(egg.a == count, "Creative keeps the stack");
                    TacticalHostile mob = mobs(game).get(mobs(game).size() - 1);
                    check((i == 0 ? mob instanceof EntitySkirmisher : mob instanceof EntityBreacher), "egg spawns the right entity");
                    check(mob.bb() == (i == 0 ? 16 : 22), "egg initializes full health");
                    check(mob.o == x + 0.5D && mob.y.b == Y && mob.q == z + 0.5D, "egg uses the native face offset");
                    checkRenderer(game, mob);
                }
            }
            game.f.q = 0;
            aan egg = game.h.ap.k_(0);
            check(!egg.a().a(egg, game.h, game.f, X + 10, Y - 1, Z, 1), "Peaceful rejects the hostile egg");
            game.f.q = 2;
            System.out.println("ALPHA7 catalog, NEI search, hotbar transfer, repeated placement and rejection passed");
            next(game);
        } else if (stage == 3 && age >= 5) {
            screenshot(game, "creative-spawned");
            saveQuit(game);
            open(game);
            next(game);
        } else if (stage == 4 && age >= 10) {
            int ranged = 0, heavy = 0;
            for (TacticalHostile mob : mobs(game)) {
                if (TacticalHostiles.SKIRMISHER.equals(aao.b(mob))) ranged++;
                if (TacticalHostiles.BREACHER.equals(aao.b(mob))) heavy++;
                checkRenderer(game, mob);
            }
            check(ranged == 2 && heavy == 2, "all four egg-spawned entities persist with their save names");
            screenshot(game, "creative-reopened");
            finish(game);
        }
    }

    private void checkEggCatalog(Minecraft game, gb screen, boolean backport) throws Exception {
        String original = NEIConfig.getSearchExpression();
        try {
            for (int i = 0; i < 2; i++) {
                String name = i == 0 ? "Skirmisher" : "Breacher";
                NEIConfig.setSearchExpression(name);
                NEIUtils.loadItems();
                check(eggIndex(neiStacks(NEIController.items), name) >= 0, "NEI general catalog includes " + name);
                check(eggIndex(neiStacks(NEIController.visibleitems), name) >= 0, "NEI search includes " + name);
                yu slot;
                if (backport) {
                    Method tab = screen.getClass().getDeclaredMethod("selectTab", int.class);
                    tab.setAccessible(true);
                    tab.invoke(screen, 2);
                    List<?> entries = (List<?>) field(screen.getClass(), "entries").get(screen);
                    List<aan> stacks = new ArrayList<aan>();
                    for (Object entry : entries) stacks.add((aan) field(entry.getClass(), "stack").get(entry));
                    check(eggIndex(stacks, name) >= 0, "ZTO category includes " + name);
                    tab.invoke(screen, 5);
                    ((agu) field(screen.getClass(), "search").get(screen)).a("");
                    Method key = vp.class.getDeclaredMethod("a", char.class, int.class);
                    key.setAccessible(true);
                    for (char letter : name.toLowerCase(java.util.Locale.ROOT).toCharArray())
                        key.invoke(screen, letter, Keyboard.getKeyIndex(String.valueOf(Character.toUpperCase(letter))));
                    slot = (yu) screen.d.e.get(0);
                    check(matchesEgg(slot.b(), name) && ((List<?>) field(screen.getClass(), "entries").get(screen)).size() == 1,
                            "backport search returns one " + name);
                } else {
                    os catalog = (os) screen.d;
                    int index = eggIndex(catalog.a, name);
                    check(index >= 0, "native Creative catalog includes " + name);
                    int lastRow = catalog.a.size() / 8 - 8 + 1;
                    int row = Math.min(index / 8, lastRow);
                    catalog.a(row / (float) lastRow);
                    slot = (yu) catalog.e.get(index - row * 8);
                    check(matchesEgg(slot.b(), name), "native scroll exposes " + name);
                }
                yu hotbar = (yu) screen.d.e.get(screen.d.e.size() - 9 + i);
                Method click = gb.class.getDeclaredMethod("a", yu.class, int.class, int.class, boolean.class);
                click.setAccessible(true);
                click.invoke(screen, slot, slot.c, 0, false);
                check(matchesEgg(game.h.ap.k(), name), "catalog click acquires " + name);
                click.invoke(screen, hotbar, hotbar.c, 0, false);
                check(matchesEgg(game.h.ap.k_(i), name),
                        "catalog click transfers egg into its hotbar slot");
            }
        } finally {
            NEIConfig.setSearchExpression(original);
            NEIUtils.loadItems();
        }
    }

    private static List<aan> neiStacks(List<?> entries) throws Exception {
        List<aan> stacks = new ArrayList<aan>();
        Field item = field(ItemPanelStack.class, "item");
        for (Object entry : entries) {
            if (entry instanceof ItemPanelStack) stacks.add((aan) item.get(entry));
        }
        return stacks;
    }

    private static boolean matchesEgg(aan stack, String name) {
        return stack != null && (name + " Spawn Egg").equals(stack.a().d(stack));
    }

    private static int eggIndex(List<?> entries, String name) {
        int result = -1;
        for (int index = 0; index < entries.size(); index++) {
            if (!(entries.get(index) instanceof aan) || !matchesEgg((aan) entries.get(index), name)) continue;
            check(result == -1, "catalog lists only one " + name);
            result = index;
        }
        return result;
    }

    private void lineUp() {
        double[] positions = {-2.2, -1.1, 0.25, 1.55, 2.65, 3.75, 4.85};
        for (int i = 0; i < lineup.length; i++) {
            acq mob = lineup[i];
            mob.c(X + (i < 5 ? 5.5D : 6.6D), Y, Z + positions[i], 90, 0);
            mob.bd = mob.be = mob.bf = mob.bg = 90;
            mob.r = mob.s = mob.t = 0;
            mob.bL = mob.bM = mob.bN = 0;
        }
    }

    private static float clearance(TacticalHostile mob) throws Exception {
        um renderer = ahu.a.a(mob);
        Method measure = renderer.getClass().getDeclaredMethod("clearanceScale", acq.class, float.class);
        measure.setAccessible(true);
        return (Float) measure.invoke(renderer, mob, 1F);
    }

    private static void torches(Minecraft game, int id) {
        for (int dz = -3; dz <= 5; dz += 2) game.f.g(X + 4, Y, Z + dz, id);
    }

    private static nn spawn(Minecraft game, String name, int dx, int dz) {
        TacticalHostile mob = (TacticalHostile) aao.a(name, game.f);
        check(mob != null && name.equals(aao.b(mob)), "entity registry round trip " + name);
        mob.c(X + dx + 0.5D, Y, Z + dz + 0.5D, 90, 0);
        check(game.f.a(mob), "world insertion " + name);
        checkRenderer(game, mob);
        return mob;
    }

    private static void checkRenderer(Minecraft game, nn mob) {
        check(ahu.a.a(mob) instanceof fe, "native living renderer registry");
        check(ahu.a.a(mob).getClass().getName().contains("TacticalRenderer"), "custom renderer registry");
        check(TacticalHostile.class.getResource(mob.v_()) != null, "packaged skin " + mob.v_());
        try {
            BufferedImage skin = ImageIO.read(TacticalHostile.class.getResource(mob.v_()));
            check(skin.getHeight() == 64 && skin.getWidth() == (mob instanceof EntitySkirmisher ? 64 : 128),
                    "native UV atlas dimensions");
        } catch (java.io.IOException failure) {
            throw new AssertionError("texture decode", failure);
        }
        int texture = game.p.b(mob.v_());
        check(texture > 0 && GL11.glIsTexture(texture), "texture uploaded");
        check(game.p.b(mob.v_()) == texture, "texture binding is stable");
        check(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL reports no error");
    }

    private static List<TacticalHostile> mobs(Minecraft game) {
        List<TacticalHostile> found = new ArrayList<TacticalHostile>();
        for (Object entity : game.f.b) if (entity instanceof TacticalHostile) found.add((TacticalHostile) entity);
        return found;
    }

    private static void player(Minecraft game, double dx, double dz) {
        game.h.c(X + dx, Y, Z + dz, -90, 0);
        game.h.r = game.h.s = game.h.t = 0;
    }

    private static void arena(Minecraft game) {
        game.f.a(6000L);
        for (int dx = -23; dx <= 23; dx++) {
            for (int dz = -23; dz <= 23; dz++) game.f.g(X + dx, Y - 1, Z + dz, 1);
        }
        player(game, 0.5D, 0.5D);
    }

    private static void wall(Minecraft game, int dx, int id) {
        for (int dz = -17; dz <= 17; dz++) {
            for (int dy = 0; dy <= 2; dy++) game.f.g(X + dx, Y + dy, Z + dz, id);
        }
    }

    private static void screenshot(Minecraft game, String label) {
        check(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL before capture: " + label);
        String name = save + "-" + label + ".png";
        System.out.println("ALPHA7 " + label + " " + zh.a(directory, name, game.d, game.e));
        check(new File(directory, "screenshots/" + name).isFile(), "frame was saved: " + label);
        check(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL after capture: " + label);
    }

    private static void open(Minecraft game) throws Exception {
        field(Minecraft.class, "c").set(game, new aes(game));
        game.a(save, save, (fj) null);
        game.a((vp) null);
        game.f.q = 2;
    }

    private static void saveQuit(Minecraft game) throws Exception {
        lb menu = new lb();
        game.a(menu);
        Method action = lb.class.getDeclaredMethod("a", abp.class);
        action.setAccessible(true);
        action.invoke(menu, new abp(1, 0, 0, "Save and Quit"));
    }

    private void next(Minecraft game) { since = game.f.w(); stage++; }

    private void finish(Minecraft game) throws Exception {
        saveQuit(game);
        System.out.println("ALPHA7 PASS " + mode);
        done = true;
        game.f();
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
