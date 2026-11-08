package zed.tekkitoverhaul.emc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.src.Block;
import net.minecraft.src.Item;
import zed.tekkitoverhaul.distillation.DistillateType;
import zed.tekkitoverhaul.distillation.FractionationProcess;

public final class EmcSelfTest {
    public static void main(String[] args) throws Exception {
        parsing();
        identitiesAndPrecedence();
        economics();
        new EmcIntegration().onGameTick(null);
        System.out.println("EMC self-test passed");
    }

    private static void parsing() throws Exception {
        check(read("# comment\nzto-emc 1 AUDIT\n").mode == EmcProfile.Mode.AUDIT, "audit header");
        reject("zto-emc 2 APPLY");
        reject("zto-emc 1 APPLY\nSET id:1 0 0");
        reject("zto-emc 1 APPLY\nSET id:1 0 " + (EmcProfile.MAX_VALUE + 1));
        reject("zto-emc 1 APPLY\nSET id:1 32768 1");
        reject("zto-emc 1 APPLY\nSET name:tile.stone * 1");
    }

    private static void identitiesAndPrecedence() throws Exception {
        check(Block.stone != null, "bootstrap the block registry before items");
        int id = Item.stick.shiftedIndex;
        String name = Item.stick.getItemName();
        EmcProfile profile = read("zto-emc 1 APPLY\nSET name:" + name + " 0 7\n"
                + "DISABLE id:" + id + " 1\nSET id:" + id + " 0 6");
        check(EmcIdentity.resolve(profile.rules.get(0)) == id, "name resolution");
        check(EmcIdentity.resolve(profile.rules.get(1)) == id, "numeric resolution");
        try {
            EmcIdentity.resolve(read("zto-emc 1 APPLY\nINHERIT name:missing_item_for_test 0").rules.get(0));
            throw new AssertionError("accepted missing identity");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("unknown"), "missing identity diagnostic");
        }
        int aliasId = Item.itemsList.length - 1;
        check(Item.itemsList[aliasId] == null, "test item slot is free");
        new Item(aliasId - 256) {}.setItemName(name.substring("item.".length()));
        try {
            EmcIdentity.resolve(profile.rules.get(0));
            throw new AssertionError("accepted ambiguous registry name");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("ambiguous"), "ambiguous identity diagnostic");
        } finally {
            Item.itemsList[aliasId] = null;
        }
        Map<Integer, Map<Integer, Integer>> values = new HashMap<Integer, Map<Integer, Integer>>();
        EmcRules rules = new EmcRules(values);
        rules.defaultIfMissing(id, 4);
        rules.defaultIfMissing(id, 5);
        check(values.get(id).get(0) == 4, "inherited positive entry wins over default");
        rules.stage(profile.rules.get(0), id, false);
        check(values.get(id).get(0) == 7, "explicit rule wins over default");
        rules.stage(profile.rules.get(1), id, false);
        try {
            rules.validate(profile.rules.get(1), id);
            throw new AssertionError("accepted disabling a variant with positive fallback");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("fallback"), "fallback diagnostic");
        }
        try {
            rules.stage(profile.rules.get(2), id, false);
            throw new AssertionError("accepted alias for previously resolved key");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("duplicate"), "duplicate diagnostic");
        }
        EmcProfile.Rule disable = read("zto-emc 1 APPLY\nDISABLE id:" + id + " *").rules.get(0);
        EmcRules whole = new EmcRules(values);
        whole.stage(disable, id, false);
        whole.validate(disable, id);
        check(!values.containsKey(id), "whole disable clears fallback and direct values");
    }

    private static void economics() {
        check(4 * 32 == 128, "crafting tables and 5x5 table break even");
        check(16 * 1 == 16, "stone and Foundry break even");
        check(8 * 200 == 1600, "coal provides eight uninterrupted vanilla smelts");
        check(64 * (2 - 1) == 64, "stone surcharge creates a repeatable premium with renewable energy");
        check(768 + 2048 == 2816, "empty bucket plus oil content");
        check(769 - 768 == 1 && 768 - 768 == 0, "water filling premium removed by candidate");
        check(4 * 512 == 2048 && 4 * 2048 == 8192, "fuel recipe ratios");
        check(EmcProfile.MAX_VALUE * 80L * 64 <= Integer.MAX_VALUE, "condenser stack scaling");
        for (DistillateType type : DistillateType.values()) {
            FractionationProcess process = new FractionationProcess();
            process.select(type);
            process.fillCrude(1000);
            process.fillCoolant(4 * type.coolantPerBatchMB);
            int eu = 0;
            for (int tick = 0; tick < type.durationTicks * 4; tick++) eu += process.tick(true, 8);
            check(process.getCrudeMB() == 0 && process.getCoolantMB() == 0, "batch inputs");
            check(process.drain(1000) == 1000 && process.drain(1) == 0, "one bucket, no byproduct");
            check(eu == type.durationTicks * 32, "EU per bucket");
        }
    }

    private static void reject(String input) throws Exception {
        try {
            read(input);
            throw new AssertionError("accepted invalid profile " + input);
        } catch (IOException expected) {
            check(expected.getMessage().contains("line "), "line diagnostic");
        }
    }

    private static EmcProfile read(String input) throws IOException {
        return EmcProfile.read(new BufferedReader(new StringReader(input)));
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
