package zed.tekkitoverhaul.distillation;

import net.minecraft.src.GuiButton;
import net.minecraft.src.TileEntity;
import zed.tekkitoverhaul.client.GuiMachinePanel;

/** Local process overview and part-specific controls. */
final class GuiDistillationPanel extends GuiMachinePanel {
    private static final int TEXT = 0xFFD8E4ED;
    private static final int NOTICE = 0xFFFFD187;
    private final DistillationPartType part;
    private final String transferResult;
    private TileEntityDistillationBoiler boiler;

    GuiDistillationPanel(DistillationPartType part, TileEntity tile, TileEntityDistillationBoiler boiler,
                         String transferResult) {
        super(part.displayName, tile);
        this.part = part;
        this.boiler = boiler;
        this.transferResult = transferResult;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (part == DistillationPartType.THERMOMETER) {
            for (DistillateType type : DistillateType.values()) {
                controlList.add(new GuiButton(type.id, left + 14 + (type.id % 2) * 111,
                        top + 78 + (type.id / 2) * 23, 107, 20,
                        type.displayName + " " + type.temperatureCelsius + " C"));
            }
            updateButtons();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (mc.currentScreen != this) return;
        if (part != DistillationPartType.BOILER) {
            boiler = DistillationStructure.boilerFor(tile.worldObj, tile.xCoord, tile.yCoord, tile.zCoord, part);
        }
        if (part == DistillationPartType.THERMOMETER) updateButtons();
    }

    private void updateButtons() {
        for (Object control : controlList) {
            GuiButton button = (GuiButton) control;
            DistillateType type = DistillateType.fromId(button.id);
            if (type != null) button.enabled = boiler != null && boiler.getProcess().selectedType() != type;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        DistillateType type = DistillateType.fromId(button.id);
        if (type == null) {
            super.actionPerformed(button);
        } else if (boiler != null && !boiler.isInvalid()) {
            boiler.select(type.id);
            updateButtons();
        }
    }

    @Override
    protected void drawPanel() {
        if (part == DistillationPartType.HEATER) {
            TileEntityDistillationHeater heater = (TileEntityDistillationHeater) tile;
            meter(61, "Stored power", heater.getStoredEnergy(), LvHeaterBuffer.CAPACITY_EU, "EU", 0xFFF0BC58);
            status(91, "Connect an IC2 cable to a free face. Accepts up to "
                    + LvHeaterBuffer.MAX_PACKET_EU + " EU per packet.", TEXT);
            status(123, boiler == null ? "Place the boiler directly above the heater."
                    : "The boiler draws " + FractionationProcess.EU_PER_TICK + " EU/t while processing.", TEXT);
            return;
        }
        if (boiler == null) {
            status(44, "Boiler not linked", NOTICE);
            status(66, "Place this component in the distillation structure. Open the boiler panel to check missing parts.", TEXT);
            return;
        }

        FractionationProcess process = boiler.getProcess();
        DistillateType selected = process.selectedType();
        status(39, boiler.getProcessStatus(), boiler.isFormed() ? TEXT : NOTICE);

        switch (part) {
            case BOILER:
                drawBoiler(process);
                break;
            case COLUMN:
                status(60, "Selected fraction: " + selected.displayName, TEXT);
                status(74, "Setpoint: " + selected.temperatureCelsius + " C", TEXT);
                meter(97, "Batch progress", process.getProgressTicks(), selected.durationTicks, "ticks", 0xFF63BCA0);
                status(127, "Use the thermometer to select a different fraction.", TEXT);
                break;
            case CONDENSER:
                meter(66, "Cooling water", process.getCoolantMB(), FractionationProcess.TANK_CAPACITY_MB, "mB", 0xFF61A9E3);
                status(97, "Water per batch: " + selected.coolantPerBatchMB + " mB", TEXT);
                status(119, "Right-click with a water bucket or connect a BuildCraft liquid pipe.", TEXT);
                break;
            case RECEIVER:
                DistillateType product = DistillateType.fromId(process.getOutputTypeId());
                status(58, "Contents: " + (product == null ? "Empty" : product.displayName), TEXT);
                meter(78, "Stored distillate", process.getDistillateMB(), FractionationProcess.TANK_CAPACITY_MB, "mB", 0xFF63BCA0);
                status(110, "Right-click with an empty bucket or extract through a powered wooden waterproof pipe.", TEXT);
                break;
            case THERMOMETER:
                status(57, "Selected: " + selected.displayName + " (" + selected.temperatureCelsius + " C)", NOTICE);
                meter(132, "Batch progress", process.getProgressTicks(), selected.durationTicks, "ticks", 0xFF63BCA0);
                break;
            default:
                break;
        }
        if (transferResult != null) status(164, transferResult, NOTICE);
    }

    private void drawBoiler(FractionationProcess process) {
        meter(59, "Crude oil", process.getCrudeMB(), FractionationProcess.TANK_CAPACITY_MB, "mB", 0xFFBA8B65);
        meter(83, "Cooling water", process.getCoolantMB(), FractionationProcess.TANK_CAPACITY_MB, "mB", 0xFF61A9E3);
        meter(107, "Distillate", process.getDistillateMB(), FractionationProcess.TANK_CAPACITY_MB, "mB", 0xFF63BCA0);
        if (!boiler.isFormed()) {
            status(131, "Missing: " + boiler.getMissingPartNames(), NOTICE);
        } else {
            status(131, "Right-click with a BuildCraft oil bucket to load crude.", TEXT);
        }
    }
}
