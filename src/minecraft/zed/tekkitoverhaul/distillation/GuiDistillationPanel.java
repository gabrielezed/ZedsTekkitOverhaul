package zed.tekkitoverhaul.distillation;

import net.minecraft.src.GuiButton;
import net.minecraft.src.TileEntity;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.client.GuiMachinePanel;

/** Local process overview and part-specific controls. */
final class GuiDistillationPanel extends GuiMachinePanel {
    private static final int TEXT = 0xFF30302E;
    private static final int WARNING = 0xFF6C3528;
    private static final int RUNNING = 0xFF284C38;
    private static final int OIL = 0xFF926844;
    private static final int WATER = 0xFF5283A3;
    private static final int PRODUCT = 0xFF64834F;
    private static final int PROGRESS = 0xFF577D69;
    private static final int CAPACITY = FractionationProcess.TANK_CAPACITY_MB;

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
    protected void drawPanelBackground(String title) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.renderEngine.bindTexture(mc.renderEngine.getTexture("/zed/gui/distillation.png"));
        drawTexturedModalRect(left, top, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        fontRenderer.drawString(title, left + 14, top + 11, TEXT);
    }

    @Override
    protected void drawPanel() {
        if (transferResult != null)
            fontRenderer.drawSplitString(transferResult, left + 14, top + 177, 143, TEXT);
        if (boiler == null) {
            status(32, "Boiler not linked", WARNING);
            if (part == DistillationPartType.HEATER) drawHeater();
            status(136, part == DistillationPartType.HEATER ? "Place the boiler directly above the heater."
                    : "Place this component at its position in the boiler assembly.", WARNING);
            return;
        }

        DistillationState state = boiler.visualState();
        FractionationProcess process = boiler.getProcess();
        DistillateType selected = process.selectedType();
        boolean assembled = state != DistillationState.INCOMPLETE;
        status(32, assembled ? "Formed  /  " + state.label : state.label,
                state == DistillationState.ACTIVE ? RUNNING
                        : state == DistillationState.IDLE ? TEXT : WARNING);

        switch (part) {
            case BOILER:
                status(48, selected.displayName + "  /  Target " + selected.temperatureCelsius + " C", TEXT);
                gauge(62, "Crude oil", process.getCrudeMB(), CAPACITY, "mB", OIL);
                gauge(82, "Cooling water", process.getCoolantMB(), CAPACITY, "mB", WATER);
                DistillateType contents = DistillateType.fromId(process.getOutputTypeId());
                gauge(102, contents == null ? "Receiver" : contents.displayName,
                        process.getDistillateMB(), CAPACITY, "mB", PRODUCT);
                progress(122, process);
                diagnostic(142, state);
                break;
            case HEATER:
                drawHeater();
                diagnostic(132, state);
                break;
            case COLUMN:
                status(52, "Linked to boiler", TEXT);
                status(69, selected.displayName + "  /  Target " + selected.temperatureCelsius + " C", TEXT);
                progress(91, process);
                diagnostic(127, state);
                break;
            case CONDENSER:
                gauge(56, "Cooling water", process.getCoolantMB(), CAPACITY, "mB", WATER);
                status(81, "Per batch " + selected.coolantPerBatchMB + " mB  /  "
                        + (process.getCoolantMB() >= selected.coolantPerBatchMB ? "Enough water" : "Refill needed"), TEXT);
                status(101, "Accepts water buckets and BuildCraft liquid pipes.", TEXT);
                diagnostic(125, state);
                break;
            case RECEIVER:
                DistillateType product = DistillateType.fromId(process.getOutputTypeId());
                status(48, product == null ? "Receiver empty" : "Stored fraction  " + product.displayName, TEXT);
                gauge(63, "Stored distillate", process.getDistillateMB(), CAPACITY, "mB", PRODUCT);
                status(85, "Free space  " + (CAPACITY - process.getDistillateMB()) + " mB", TEXT);
                int paused = tile instanceof TileEntityDistillationReceiver
                        ? ((TileEntityDistillationReceiver)tile).disruptionTicksRemaining() : 0;
                status(102, paused > 0 ? "BuildCraft extraction disrupted (" + paused + " ticks left)."
                        : "Extract with a bucket or a powered wooden waterproof pipe.", paused > 0 ? WARNING : TEXT);
                diagnostic(127, state);
                break;
            case THERMOMETER:
                status(55, selected.displayName + "  /  Target " + selected.temperatureCelsius + " C", TEXT);
                progress(130, process);
                diagnostic(152, state);
                break;
            default:
                break;
        }
    }

    private void drawHeater() {
        TileEntityDistillationHeater heater = (TileEntityDistillationHeater)tile;
        gauge(56, "Stored energy", heater.getStoredEnergy(), LvHeaterBuffer.CAPACITY_EU, "EU", OIL);
        int demand = FractionationProcess.EU_PER_TICK;
        int draw = boiler != null && boiler.visualState() == DistillationState.ACTIVE ? demand : 0;
        status(86, "Batch demand  " + demand + " EU/t   /   Last tick  " + draw + " EU", TEXT);
        status(106, "IC2 LV cable on a free face, max " + LvHeaterBuffer.MAX_PACKET_EU + " EU per packet.", TEXT);
    }

    private void diagnostic(int y, DistillationState state) {
        if (state == DistillationState.INCOMPLETE) {
            status(y, "Missing  " + boiler.getMissingPartNames(), WARNING);
        } else if (state != DistillationState.ACTIVE) {
            status(y, state.action, state == DistillationState.IDLE ? TEXT : WARNING);
        }
    }

    private void progress(int y, FractionationProcess process) {
        gauge(y, "Batch progress", process.getProgressTicks(), process.selectedType().durationTicks, "ticks", PROGRESS);
    }

    private void gauge(int y, String label, int amount, int max, String unit, int color) {
        int start = left + 14;
        int end = left + PANEL_WIDTH - 14;
        int textY = top + y;
        String value = amount + " / " + max + " " + unit;
        fontRenderer.drawString(label, start, textY, TEXT);
        fontRenderer.drawString(value, end - fontRenderer.getStringWidth(value), textY, TEXT);
        drawRect(start, textY + 10, end, textY + 17, 0xFF53524D);
        drawRect(start + 1, textY + 11, end - 1, textY + 16, 0xFF74716A);
        int fill = Math.min(max, Math.max(0, amount)) * (end - start - 2) / max;
        if (fill > 0) drawRect(start + 1, textY + 11, start + 1 + fill, textY + 16, color);
    }
}
