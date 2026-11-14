package zed.tekkitoverhaul.emc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import net.minecraft.src.ModLoader;
import zed.tekkitoverhaul.crafting.AdvancedCraftingRegistry;

/** Runs once on the first client game tick after EE2 external integration. */
public final class EmcIntegration {
    private Ee2EmcAdapter ee2;
    private boolean done;

    public void onGameTick(File gameDirectory, String profileText) {
        if (done) return;
        try {
            if (ee2 == null) ee2 = new Ee2EmcAdapter();
            if (!ee2.isReady()) return;
            done = true;
            initialize(new File(gameDirectory, "config"), profileText);
        } catch (ClassNotFoundException absent) {
            done = true;
        } catch (Exception failure) {
            done = true;
            ModLoader.getLogger().log(Level.WARNING, "[ZTO] EMC initialization stopped", failure);
        } catch (LinkageError failure) {
            done = true;
            ModLoader.getLogger().log(Level.WARNING, "[ZTO] EMC initialization stopped", failure);
        }
    }

    private void initialize(File config, String profileText) throws IOException, ReflectiveOperationException {
        EmcProfile profile = profileText.isEmpty() ? null :
                EmcProfile.read(new BufferedReader(new StringReader(profileText)));
        int table = AdvancedCraftingRegistry.advancedTable.blockID;
        int foundry = AdvancedCraftingRegistry.foundry.blockID;
        Map<Integer, Map<Integer, Integer>> candidate = ee2.snapshot();
        EmcRules rules = new EmcRules(candidate);
        rules.defaultIfMissing(table, 128);
        rules.defaultIfMissing(foundry, 16);

        if (profile == null || profile.mode == EmcProfile.Mode.AUDIT) {
            if (profile != null && !profile.rules.isEmpty()) {
                throw new IOException("AUDIT profiles must contain only the header");
            }
            ee2.publish(candidate);
            ModLoader.getLogger().info("[ZTO] EMC compatibility values: table=" + candidate.get(table).get(0)
                    + ", foundry=" + candidate.get(foundry).get(0));
            if (profile != null) {
                File catalog = new File(config, "ZedsTekkitOverhaul-emc-baseline.tsv");
                EmcCatalog.write(catalog, ee2, candidate);
                ModLoader.getLogger().info("[ZTO] EMC baseline written to " + catalog);
                EmcCatalog.writeEnergyEvidence(new File(config, "ZedsTekkitOverhaul-emc-energy.txt"));
            }
            return;
        }

        Map<EmcProfile.Rule, Integer> resolved = new LinkedHashMap<EmcProfile.Rule, Integer>();
        for (EmcProfile.Rule rule : profile.rules) {
            try {
                int id = EmcIdentity.resolve(rule);
                rules.stage(rule, id, ee2.isCollectorFuel(id));
                resolved.put(rule, id);
            } catch (IllegalArgumentException failure) {
                throw invalid(rule, failure);
            }
        }
        for (Map.Entry<EmcProfile.Rule, Integer> entry : resolved.entrySet()) {
            try {
                rules.validate(entry.getKey(), entry.getValue());
            } catch (IllegalArgumentException failure) {
                throw invalid(entry.getKey(), failure);
            }
        }
        ee2.publish(candidate);
        ModLoader.getLogger().info("[ZTO] EMC applied " + resolved.size() + " profile rules");
    }

    private IOException invalid(EmcProfile.Rule rule, IllegalArgumentException failure) {
        return new IOException("line " + rule.line + " (" + rule.identity + "): " + failure.getMessage(), failure);
    }
}
