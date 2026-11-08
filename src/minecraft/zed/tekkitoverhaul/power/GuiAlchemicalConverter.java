package zed.tekkitoverhaul.power;

import java.util.Locale;
import zed.tekkitoverhaul.client.GuiMachinePanel;

/** Live overview of the collector link and conversion buffer. */
final class GuiAlchemicalConverter extends GuiMachinePanel {
    private final TileAlchemicalConverter converter;
    private int mark;

    GuiAlchemicalConverter(TileAlchemicalConverter converter) {
        super("Alchemical Converter", converter);
        this.converter = converter;
        this.mark = converter.strongestCollectorMark();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (mc.currentScreen == this) mark = converter.strongestCollectorMark();
    }

    @Override
    protected void drawPanel() {
        status(42, mark == 0 ? "No Energy Collector connected" : "Highest connected collector: MK " + mark,
                mark == 0 ? 0xFFFFD187 : 0xFF86D6B9);
        status(62, mark == 0 ? "Place a lit EE2 Energy Collector against any face."
                : "Maximum output: " + TileAlchemicalConverter.outputLimit(mark) + " EU per packet.", 0xFFD8E4ED);
        int stored = converter.storedUnits();
        String emc = String.format(Locale.ROOT, "%.4f / %d EMC",
                (double) stored / AlchemicalBuffer.UNITS_PER_EMC,
                AlchemicalBuffer.CAPACITY / AlchemicalBuffer.UNITS_PER_EMC);
        meter(92, "EMC buffer", stored, AlchemicalBuffer.CAPACITY, 0xFFB2A2F3, emc);
        status(120, "1 EMC converts to "
                + AlchemicalBuffer.UNITS_PER_EMC / AlchemicalBuffer.UNITS_PER_EU + " EU.", 0xFFD8E4ED);
        status(140, "Connect IC2 cables to any free face.", 0xFFD8E4ED);
    }
}
