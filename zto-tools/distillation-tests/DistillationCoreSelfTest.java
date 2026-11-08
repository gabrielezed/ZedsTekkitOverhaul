import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.FractionationProcess;
import zed.tekkitoverhaul.distillation.LvHeaterBuffer;

public final class DistillationCoreSelfTest {
    private static int assertions;
    private static void check(boolean ok, String message) {
        ++assertions;
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        DistillateType[] all = DistillateType.values();
        check(all.length == 4, "Expected four distillate kinds");
        for (int i = 0; i < all.length; i++) {
            check(all[i].id == i && DistillateType.fromId(i) == all[i], "Unstable fluid identifier");
            if (i > 0) check(all[i].temperatureCelsius > all[i-1].temperatureCelsius, "Invalid temperature order");
        }
        check(DistillateType.fromId(99) == null, "Unexpected fluid ID");
        FractionationProcess f = new FractionationProcess();
        check(f.fillCrude(10000, false) == 8000 && f.getCrudeMB() == 0,
                "Oil pipe simulation must clamp without writing");
        check(f.fillCoolant(10000, false) == 8000 && f.getCoolantMB() == 0,
                "Water pipe simulation must clamp without writing");
        check(f.fillCrude(10000) == 8000, "Crude capacity not enforced");
        check(f.fillCrude(1000) == 0, "Overfill accepted");
        check(f.fillCoolant(1000) == 1000, "Water input failed");
        check(f.fillCoolant(8000, false) == 7000 && f.getCoolantMB() == 1000,
                "Water pipe simulation must respect remaining capacity");
        check(f.tick(false, 100) == 0, "Unformed structure processed");
        check(f.tick(true, 7) == 0, "Insufficient EU processed");
        check(f.tick(true, 8) == 8, "Process did not consume 8 EU/t");
        for (int i = 1; i < 160; i++) check(f.tick(true, 8) == 8, "Unexpected process stall");
        check(f.getCrudeMB() == 7750, "Incorrect crude consumption");
        check(f.getCoolantMB() == 960, "Incorrect water consumption");
        check(f.getDistillateMB() == 250, "Incorrect distillate output");
        check(f.getOutputTypeId() == DistillateType.GASOLINE.id, "Output type mismatch");
        f.select(DistillateType.DIESEL);
        check(f.tick(true, 8) == 0, "Mixed output products accepted");
        check(f.drain(250) == 250 && f.getOutputTypeId() == -1, "Output not emptied");
        check(f.tick(true, 8) == 8, "Next product did not start");
        check(f.getProgressTicks() == 1, "Wrong progress after switching");
        f.select(DistillateType.KEROSENE);
        check(f.getProgressTicks() == 0, "Temperature change did not reset progress");
        f.restore(-10, 999999, 250, 99, 99, -3);
        check(f.getCrudeMB() == 0 && f.getCoolantMB() == 8000, "NBT range check failed");
        check(f.getDistillateMB() == 0 && f.getOutputTypeId() == -1, "Invalid stored fluid accepted");
        check(f.getSelectedTypeId() == 0 && f.getProgressTicks() == 0, "Invalid selection accepted");
        LvHeaterBuffer heater = new LvHeaterBuffer();
        check(heater.inject(33) == 33, "Over-voltage packet accepted");
        check(heater.inject(32) == 0, "LV packet not accepted");
        check(!heater.consume(33), "Energy overdraft allowed");
        check(heater.consume(8), "EU deduction failed");
        check(heater.getStoredEU() == 24, "EU buffer corrupted");
        heater.restore(3995);
        check(heater.inject(32) == 27, "EnergyNet remainder semantics violated");
        check(heater.getStoredEU() == 4000, "Buffer overflow");
        check(heater.inject(8) == 8, "Full buffer did not reject EU");
        System.out.println("PASS: " + assertions + " distillation core assertions");
    }
}
