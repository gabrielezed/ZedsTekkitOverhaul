package zed.tekkitoverhaul.integration.ee2;

/**
 * Integrates EE2 alchemical fuels with the legacy ModLoader fuel callback.
 * IC2 1.97's standard Generator obtains furnace burn time and divides by 4.
 * No direct IC2 dependency is needed.
 */
public final class EE2FuelIntegration {

    // These are the furnace burn times from EE2 1.4.6.5.
    private static final int ALCHEMICAL_COAL_TICKS = 25600;
    private static final int MOBIUS_FUEL_TICKS = 102400;
    private static final int AETERNALIS_FUEL_TICKS = 409600;

    private final EE2ItemResolver items = new EE2ItemResolver();

    private boolean postInitComplete;
    private boolean alchemicalLogged;
    private boolean mobiusLogged;
    private boolean aeternalisLogged;

    public void onModsLoaded() {
        items.resolve();
        postInitComplete = true;

        if (!items.isResolved()) {
            System.out.println("[ZedsTekkitOverhaul] EE2 fuels not resolved; "
                    + "this is normal in the plain MCP development client.");
        }
    }

    public int getBurnTime(int id, int metadata) {
        // Mirrors v0.3.0: allow an early lookup before modsLoaded().
        if (!postInitComplete && !items.isResolved()) {
            items.resolve();
        }

        if (items.getAlchemicalCoalId() >= 0 && id == items.getAlchemicalCoalId()) {
            if (!alchemicalLogged) {
                System.out.println("[ZedsTekkitOverhaul] Alchemical Coal accepted as fuel");
                alchemicalLogged = true;
            }
            return ALCHEMICAL_COAL_TICKS;
        }
        if (items.getMobiusFuelId() >= 0 && id == items.getMobiusFuelId()) {
            if (!mobiusLogged) {
                System.out.println("[ZedsTekkitOverhaul] Mobius Fuel accepted as fuel");
                mobiusLogged = true;
            }
            return MOBIUS_FUEL_TICKS;
        }
        if (items.getAeternalisFuelId() >= 0 && id == items.getAeternalisFuelId()) {
            if (!aeternalisLogged) {
                System.out.println("[ZedsTekkitOverhaul] Aeternalis Fuel accepted as fuel");
                aeternalisLogged = true;
            }
            return AETERNALIS_FUEL_TICKS;
        }
        return 0;
    }
}
