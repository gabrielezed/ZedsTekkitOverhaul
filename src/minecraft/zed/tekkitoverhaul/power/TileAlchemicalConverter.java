package zed.tekkitoverhaul.power;

import ee.IEEPowerNet;
import ic2.api.Direction;
import ic2.api.EnergyNet;
import ic2.api.IEnergySource;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;

/** Receives adjacent EE2 collector packets and emits IC2 power on all faces. */
public final class TileAlchemicalConverter extends TileEntity implements IEEPowerNet, IEnergySource {
    private final AlchemicalBuffer buffer = new AlchemicalBuffer();
    private boolean registered;

    int strongestCollectorMark() {
        int mark = 0;
        for (CollectorFace face : CollectorFace.PACKET_SIDES) {
            mark = Math.max(mark, collectorMark(face));
            if (mark == 3) break;
        }
        return mark;
    }

    private int collectorMark(CollectorFace face) {
        int x = xCoord + face.dx;
        int y = yCoord + face.dy;
        int z = zCoord + face.dz;
        if (worldObj == null || !worldObj.blockExists(x, y, z)) return 0;
        TileEntity source = worldObj.getBlockTileEntity(x, y, z);
        if (source == null || source.isInvalid()) return 0;
        String name = source.getClass().getName();
        if ("ee.TileCollector".equals(name)) return 1;
        if ("ee.TileCollector2".equals(name)) return 2;
        if ("ee.TileCollector3".equals(name)) return 3;
        return 0;
    }

    int storedUnits() { return buffer.storedUnits(); }

    public boolean receiveEnergy(int amount, byte side, boolean commit) {
        if (worldObj == null || worldObj.isRemote || isInvalid()
                || side < 0 || side >= CollectorFace.PACKET_SIDES.length) return false;
        CollectorFace face = CollectorFace.PACKET_SIDES[side];
        int mark = collectorMark(face);
        if (mark == 0) return false;
        boolean accepted = buffer.receive(worldObj.getWorldTime(), face, amount, inputLimit(mark), commit);
        if (accepted && commit) onInventoryChanged();
        return accepted;
    }

    public boolean sendEnergy(int amount, byte side, boolean commit) { return false; }
    public boolean passEnergy(int amount, byte side, boolean commit) { return false; }
    public void sendAllPackets(int amount) { }
    public int relayBonus() { return 0; }

    public boolean isAddedToEnergyNet() { return registered; }
    public int getMaxEnergyOutput() { return AlchemicalBuffer.MAX_PACKET_EU; }

    public boolean emitsEnergyTo(TileEntity receiver, Direction side) {
        return true;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote || isInvalid()) return;
        EnergyNet net = EnergyNet.getForWorld(worldObj);
        if (!registered) {
            net.addTileEntity(this);
            registered = true;
        }
        int mark = strongestCollectorMark();
        if (mark == 0) return;
        int sent = buffer.offer(worldObj.getWorldTime(), outputLimit(mark));
        if (sent == 0) return;
        int remainder = net.emitEnergyFrom(this, sent);
        buffer.finishOffer(sent, remainder);
        if (remainder < sent) onInventoryChanged();
    }

    @Override
    public void invalidate() {
        disconnect();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        disconnect();
        super.onChunkUnload();
    }

    private void disconnect() {
        if (registered && worldObj != null && !worldObj.isRemote) {
            EnergyNet.getForWorld(worldObj).removeTileEntity(this);
            registered = false;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        buffer.restore(tag.getInteger("AlchemicalUnits"));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("AlchemicalUnits", buffer.storedUnits());
    }

    private static int inputLimit(int mark) {
        if (mark == 1) return 16;
        if (mark == 2) return 48;
        return 160;
    }

    static int outputLimit(int mark) {
        if (mark == 1) return 1;
        if (mark == 2) return 3;
        return AlchemicalBuffer.MAX_PACKET_EU;
    }
}
