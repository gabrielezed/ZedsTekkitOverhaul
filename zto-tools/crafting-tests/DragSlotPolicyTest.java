package zed.tekkitoverhaul.integration.inventory;

/** Guards plain slots that act as controls despite using the vanilla Slot class. */
public final class DragSlotPolicyTest {
    public static void main(String[] args) {
        check(DragSlotPolicy.isModInput("cpw.mods.ironchest.ContainerIronChestBase", 107, 144, 108));
        check(!DragSlotPolicy.isModInput("cpw.mods.ironchest.ContainerIronChestBase", 108, 144, 108));
        check(!DragSlotPolicy.isModInput("cpw.mods.ironchest.ContainerIronChestBase", 0, 143, 108));
        check(DragSlotPolicy.isModInput("ee.ContainerAlchChest", 90, 127, 91));
        check(DragSlotPolicy.isModInput("ee.ContainerCondenser", 1, 128, 92));
        check(DragSlotPolicy.isModInput("ee.ContainerCondenser", 91, 128, 92));
        check(!DragSlotPolicy.isModInput("ee.ContainerCondenser", 0, 128, 92));
        check(!DragSlotPolicy.isModInput("ee.ContainerTransmutation", 0, 50, 14));
        check(!DragSlotPolicy.isModInput("ee.ContainerCollector", 0, 47, 11));
        check(!DragSlotPolicy.isModInput("ee.ContainerPortableCrafting", 0, 46, 9));
        check(!DragSlotPolicy.isModInput("buildcraft.factory.ContainerAutoWorkbench", 0, 46, 9));
        check(DragSlotPolicy.isModInput("eloraam.base.ContainerAlloyFurnace", 8, 47, 11));
        check(!DragSlotPolicy.isModInput("eloraam.base.ContainerAlloyFurnace", 9, 47, 11));
        check(DragSlotPolicy.isModInput("eloraam.base.ContainerAdvBench", 0, 64, 9));
        check(DragSlotPolicy.isModInput("eloraam.base.ContainerAdvBench", 8, 64, 9));
        check(!DragSlotPolicy.isModInput("eloraam.base.ContainerAdvBench", 9, 64, 1));
        check(DragSlotPolicy.isModInput("eloraam.base.ContainerAdvBench", 10, 64, 27));
        check(DragSlotPolicy.isModInput("eloraam.base.ContainerAdvBench", 27, 64, 27));
        check(!DragSlotPolicy.isModInput("eloraam.base.ContainerAdvBench", 28, 64, 36));
        check(!DragSlotPolicy.isModInput("eloraam.base.ContainerAdvBench", 0, 63, 9));
        check(DragSlotPolicy.isModInput("eloraam.machine.ContainerBufferChest", 19, 56, 20));
        check(DragSlotPolicy.isModInput("eloraam.machine.ContainerDeploy", 8, 45, 9));
        check(DragSlotPolicy.isModInput("eloraam.machine.ContainerEject", 8, 45, 9));
        check(DragSlotPolicy.isModInput("eloraam.machine.ContainerBlueFurnace", 0, 38, 2));
        check(!DragSlotPolicy.isModInput("eloraam.machine.ContainerBlueFurnace", 1, 38, 2));
        check(DragSlotPolicy.isModInput("eloraam.machine.ContainerBlueAlloyFurnace", 8, 46, 10));
        check(!DragSlotPolicy.isModInput("eloraam.machine.ContainerBlueAlloyFurnace", 9, 46, 10));
        check(!DragSlotPolicy.isModInput("eloraam.machine.ContainerFilter", 0, 45, 9));
        check(DragSlotPolicy.isModInput("ic2.common.ContainerElectricMachine", 0, 43, 7));
        check(!DragSlotPolicy.isModInput("ic2.common.ContainerElectricMachine", 2, 43, 7));
        check(!DragSlotPolicy.isModInput("custom.UnknownContainer", 0, 45, 9));
        System.out.println("Inventory drag slot policy checks passed.");
    }

    private static void check(boolean passed) {
        if (!passed) throw new AssertionError("Unexpected drag slot policy");
    }
}
