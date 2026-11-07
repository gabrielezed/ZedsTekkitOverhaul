package zed.tekkitoverhaul.integration.ee2;

import java.lang.reflect.Method;
import zed.tekkitoverhaul.crafting.AdvancedCraftingRegistry;

/** Registra EMC esplicito per i nuovi blocchi senza dipendenze EE2 compile-time. */
public final class AdvancedCraftingEmcIntegration {
    private boolean done;

    public void onGameTick() {
        if (done || AdvancedCraftingRegistry.advancedTable == null) return;
        try {
            // EE2 registra le integrazioni esterne nel primo tick; attendiamo.
            Class<?> base = Class.forName("ee.EEBase");
            if (!base.getField("externalModsInitialized").getBoolean(null)) return;
            Class<?> maps = Class.forName("ee.EEMaps");
            Method get = maps.getMethod("getEMC", Integer.TYPE, Integer.TYPE);
            Method put = maps.getMethod("addEMC", Integer.TYPE, Integer.TYPE, Integer.TYPE);

            registerIfAbsent(get, put, AdvancedCraftingRegistry.advancedTable.blockID, 128);
            registerIfAbsent(get, put, AdvancedCraftingRegistry.foundry.blockID, 16);
            System.out.println("[ZTO] EMC Advanced Crafting Table=128, Foundry=16 (unless EE2 already assigned values)");
            done = true;
        } catch (ClassNotFoundException absent) {
            // Se EE2 non e' installata, non ripetere la ricerca ad ogni tick.
            done = true;
        } catch (Throwable failure) {
            System.err.println("[ZTO] EMC advanced crafting integration failed: " + failure);
            done = true;
        }
    }

    private void registerIfAbsent(Method get, Method put, int id, int value) throws Exception {
        Object current = get.invoke(null, new Object[] {
            Integer.valueOf(id), Integer.valueOf(0)
        });
        if (((Integer)current).intValue() <= 0) {
            put.invoke(null, new Object[] {
                Integer.valueOf(id), Integer.valueOf(0), Integer.valueOf(value)
            });
        }
    }
}
