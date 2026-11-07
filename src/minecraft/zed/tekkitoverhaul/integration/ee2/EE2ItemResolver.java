package zed.tekkitoverhaul.integration.ee2;

import net.minecraft.src.Item;

/**
 * Retrieves the three public Item fields from EE2 1.4.6.5 by reflection.
 * This intentionally introduces no compile-time dependency on EE2.
 */
public final class EE2ItemResolver {

    private int alchemicalCoalId = -1;
    private int mobiusFuelId = -1;
    private int aeternalisFuelId = -1;

    public boolean resolve() {
        if (isResolved()) {
            return true;
        }

        try {
            Class<?> eeItem = Class.forName("ee.EEItem");
            int coal = readItemId(eeItem, "alchemicalCoal");
            int mobius = readItemId(eeItem, "mobiusFuel");
            int aeternalis = readItemId(eeItem, "aeternalisFuel");

            // Update IDs atomically: do not expose a partially resolved set.
            if (coal < 0 || mobius < 0 || aeternalis < 0) {
                return false;
            }

            alchemicalCoalId = coal;
            mobiusFuelId = mobius;
            aeternalisFuelId = aeternalis;

            System.out.println("[ZedsTekkitOverhaul] EE2 IDs: Alchemical=" + coal
                    + ", Mobius=" + mobius + ", Aeternalis=" + aeternalis);
        } catch (ClassNotFoundException ex) {
            // EE2 is absent in a plain MCP development client.
        } catch (Exception ex) {
            System.err.println("[ZedsTekkitOverhaul] EE2 reflection failed: " + ex);
        } catch (LinkageError err) {
            System.err.println("[ZedsTekkitOverhaul] EE2 class loading failed: " + err);
        }
        return isResolved();
    }

    public boolean isResolved() {
        return alchemicalCoalId >= 0 && mobiusFuelId >= 0
                && aeternalisFuelId >= 0;
    }

    public int getAlchemicalCoalId() {
        return alchemicalCoalId;
    }

    public int getMobiusFuelId() {
        return mobiusFuelId;
    }

    public int getAeternalisFuelId() {
        return aeternalisFuelId;
    }

    private static int readItemId(Class<?> eeItem, String fieldName)
            throws Exception {
        Object value = eeItem.getField(fieldName).get(null);
        if (!(value instanceof Item)) {
            return -1;
        }
        return ((Item) value).shiftedIndex;
    }
}
