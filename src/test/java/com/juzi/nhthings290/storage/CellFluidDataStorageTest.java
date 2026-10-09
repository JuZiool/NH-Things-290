package com.juzi.nhthings290.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.Fluid;

import org.junit.Test;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEFluidStackType;

public class CellFluidDataStorageTest {

    @Test
    public void inventorySeesFluidAgainAfterExtractingItCompletely() throws Exception {
        Fluid fluid = new Fluid("unrestricted_fluid_cell_test");
        IAEFluidStack input = fluidStack(fluid, 1000);
        UnrestrictedFluidCellItem type = new UnrestrictedFluidCellItem("unrestricted_fluid_cell_test", 1024);
        CellFluidDataStorage.FluidListFactory factory = new TestFluidListFactory();
        CellFluidDataStorage storage = new CellFluidDataStorage(UUID.randomUUID(), factory);
        UnrestrictedFluidCellInventory inventory = new UnrestrictedFluidCellInventory(
            new ItemStack(type),
            null,
            type,
            storage);

        assertNull(inventory.injectItems(input, Actionable.MODULATE, null));
        assertAvailable(inventory, input, 1000L);

        assertEquals(
            1000L,
            inventory.extractItems(fluidStack(fluid, 1000), Actionable.MODULATE, null)
                .getStackSize());
        assertEquals(0L, storage.getStoredFluidCount());
        assertEquals(0L, storage.getStoredFluidTypes());

        IAEFluidStack secondInput = fluidStack(fluid, 321);
        assertNull(inventory.injectItems(secondInput, Actionable.MODULATE, null));
        assertAvailable(inventory, secondInput, 321L);
        assertEquals(321L, inventory.getStoredFluidCount());
        assertEquals(1L, inventory.getStoredFluidTypes());
    }

    @Test
    public void nbtKeepsActualAmountsAndAllowsExtractingOverCapacityData() {
        final Fluid fluid = new Fluid("unrestricted_fluid_cell_nbt_test");
        CellFluidDataStorage.FluidListFactory factory = new TestFluidListFactory();
        CellFluidDataStorage storage = new CellFluidDataStorage(UUID.randomUUID(), factory);
        // One byte over a 1024-byte cell at 4096 mB/byte: 1024*4096+1
        storage.addImported(fluidStack(fluid, 4194305));

        NBTTagList saved = storage.writeToNBT();
        assertEquals(1, saved.tagCount());
        NBTTagCompound tag = saved.getCompoundTagAt(0);
        assertEquals(4194305L, tag.getLong("Cnt"));
        CellFluidDataStorage restored = new CellFluidDataStorage(
            UUID.randomUUID(),
            factory,
            new CellFluidDataStorage.FluidStackLoader() {

                @Override
                public IAEFluidStack load(NBTTagCompound savedTag) {
                    return fluidStack(fluid, savedTag.getLong("Cnt"));
                }
            });
        restored.readFromNBT(saved);
        assertEquals(4194305L, restored.getStoredFluidCount());
        assertEquals(1025L, restored.getUsedBytes());
        assertEquals(4194305L, restored.extract(fluidStack(fluid, 4194305), 4194305));
        assertEquals(0L, restored.getStoredFluidCount());
        assertEquals(0L, restored.getStoredFluidTypes());
    }

    @Test
    public void overCapacityImportedDataCanBeExtractedButCannotAcceptMore() throws Exception {
        Fluid fluid = new Fluid("unrestricted_fluid_cell_over_capacity_test");
        IAEFluidStack imported = fluidStack(fluid, 4194305);
        UnrestrictedFluidCellItem type = new UnrestrictedFluidCellItem(
            "unrestricted_fluid_cell_over_capacity_test",
            1024);
        CellFluidDataStorage storage = new CellFluidDataStorage(UUID.randomUUID(), new TestFluidListFactory());
        storage.addImported(imported);
        UnrestrictedFluidCellInventory inventory = new UnrestrictedFluidCellInventory(
            new ItemStack(type),
            null,
            type,
            storage);

        IAEFluidStack rejected = inventory.injectItems(fluidStack(fluid, 1), Actionable.SIMULATE, null);
        assertNotNull(rejected);
        assertEquals(1L, rejected.getStackSize());
        assertEquals(4194305L, inventory.getStoredFluidCount());
        assertEquals(0L, inventory.getRemainingFluidCount());
        assertEquals(
            4194305L,
            inventory.extractItems(fluidStack(fluid, 4194305), Actionable.MODULATE, null)
                .getStackSize());
    }

    @Test
    public void fluidTypesDoNotConsumeBytes() throws Exception {
        Fluid fluid = new Fluid("unrestricted_fluid_cell_type_test");
        UnrestrictedFluidCellItem type = new UnrestrictedFluidCellItem("unrestricted_fluid_cell_type_test", 1024);
        CellFluidDataStorage storage = new CellFluidDataStorage(UUID.randomUUID(), new TestFluidListFactory());
        UnrestrictedFluidCellInventory inventory = new UnrestrictedFluidCellInventory(
            new ItemStack(type),
            null,
            type,
            storage);

        assertEquals(0, type.getBytesPerType(new ItemStack(type)));
        assertEquals(0, inventory.getBytesPerType());
        assertEquals(4194304L, inventory.getRemainingFluidCount());
        assertEquals(0L, inventory.getStoredFluidTypes());
        assertNull(inventory.injectItems(fluidStack(fluid, 4096), Actionable.MODULATE, null));
        assertEquals(1L, inventory.getStoredFluidTypes());
        assertEquals(1023L, inventory.getFreeBytes());
    }

    private static IAEFluidStack fluidStack(Fluid fluid, long amount) {
        return new TestFluidStack(fluid, amount);
    }

    private static void assertAvailable(UnrestrictedFluidCellInventory inventory, IAEFluidStack expected,
        long expectedAmount) {
        IItemList<IAEFluidStack> available = new TestFluidList();
        inventory.getAvailableItems(available);
        IAEFluidStack actual = available.findPrecise(expected);
        assertNotNull(actual);
        assertEquals(expectedAmount, actual.getStackSize());
    }

    private static final class TestFluidListFactory implements CellFluidDataStorage.FluidListFactory {

        @Override
        public IItemList<IAEFluidStack> create() {
            return new TestFluidList();
        }
    }

    private static final class TestFluidList implements IItemList<IAEFluidStack> {

        private final List<IAEFluidStack> stacks = new ArrayList<IAEFluidStack>();

        @Override
        public void add(IAEFluidStack stack) {
            if (stack == null) return;
            IAEFluidStack existing = findPrecise(stack);
            if (existing == null) stacks.add(stack.copy());
            else existing.incStackSize(stack.getStackSize());
        }

        @Override
        public IAEFluidStack findPrecise(IAEFluidStack stack) {
            if (stack == null) return null;
            for (IAEFluidStack existing : stacks) {
                if (existing.getFluid() == stack.getFluid()) return existing;
            }
            return null;
        }

        @Override
        public Collection<IAEFluidStack> findFuzzy(IAEFluidStack stack, FuzzyMode mode) {
            IAEFluidStack found = findPrecise(stack);
            if (found == null) return new ArrayList<IAEFluidStack>();
            List<IAEFluidStack> result = new ArrayList<IAEFluidStack>();
            result.add(found);
            return result;
        }

        @Override
        public boolean isEmpty() {
            return stacks.isEmpty();
        }

        @Override
        public void addStorage(IAEFluidStack stack) {
            add(stack);
        }

        @Override
        public void addCrafting(IAEFluidStack stack) {
            add(stack);
        }

        @Override
        public void addRequestable(IAEFluidStack stack) {
            add(stack);
        }

        @Override
        public IAEFluidStack getFirstItem() {
            return isEmpty() ? null : stacks.get(0);
        }

        @Override
        public int size() {
            return stacks.size();
        }

        @Override
        public Iterator<IAEFluidStack> iterator() {
            return stacks.iterator();
        }

        @Override
        public void resetStatus() {}

        @Override
        public boolean hasWriteAccess() {
            return true;
        }

        @Override
        public IAEFluidStack[] toArray(IAEFluidStack[] array) {
            return stacks.toArray(array);
        }

        @Override
        public IAEStackType<IAEFluidStack> getStackType() {
            return AEFluidStackType.FLUID_STACK_TYPE;
        }
    }

    private static final class TestFluidStack implements IAEFluidStack {

        private final Fluid fluid;
        private long amount;

        private TestFluidStack(Fluid fluid, long amount) {
            this.fluid = fluid;
            this.amount = amount;
        }

        @Override
        public void add(IAEFluidStack stack) {
            amount += stack.getStackSize();
        }

        @Override
        public long getStackSize() {
            return amount;
        }

        @Override
        public IAEFluidStack setStackSize(long size) {
            amount = size;
            return this;
        }

        @Override
        public long getCountRequestable() {
            return 0;
        }

        @Override
        public IAEFluidStack setCountRequestable(long size) {
            return this;
        }

        @Override
        public boolean isCraftable() {
            return false;
        }

        @Override
        public IAEFluidStack setCraftable(boolean craftable) {
            return this;
        }

        @Override
        public IAEFluidStack reset() {
            amount = 0;
            return this;
        }

        @Override
        public boolean isMeaningful() {
            return amount > 0;
        }

        @Override
        public void incStackSize(long delta) {
            amount += delta;
        }

        @Override
        public void decStackSize(long delta) {
            amount -= delta;
        }

        @Override
        public void incCountRequestable(long delta) {}

        @Override
        public void decCountRequestable(long delta) {}

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            tag.setString("FluidName", fluid.getName());
            tag.setLong("Cnt", amount);
        }

        @Override
        public boolean fuzzyComparison(Object other, FuzzyMode mode) {
            return equals(other);
        }

        @Override
        public void writeToPacket(io.netty.buffer.ByteBuf buffer) {}

        @Override
        public IAEFluidStack copy() {
            return new TestFluidStack(fluid, amount);
        }

        @Override
        public IAEFluidStack empty() {
            return new TestFluidStack(fluid, 0);
        }

        @Override
        public appeng.api.storage.data.IAETagCompound getTagCompound() {
            return null;
        }

        @Override
        public boolean isItem() {
            return false;
        }

        @Override
        public boolean isFluid() {
            return true;
        }

        @Override
        public appeng.api.storage.StorageChannel getChannel() {
            return appeng.api.storage.StorageChannel.FLUIDS;
        }

        @Override
        public String getLocalizedName() {
            return fluid.getName();
        }

        @Override
        public net.minecraftforge.fluids.FluidStack getFluidStack() {
            return null;
        }

        @Override
        public Fluid getFluid() {
            return fluid;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof IAEFluidStack && ((IAEFluidStack) other).getFluid() == fluid;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(fluid);
        }

        @Override
        public float getUsedPercent() {
            return 0;
        }

        @Override
        public IAEFluidStack setUsedPercent(float percent) {
            return this;
        }

        @Override
        public long getCountRequestableCrafts() {
            return 0;
        }

        @Override
        public IAEFluidStack setCountRequestableCrafts(long count) {
            return this;
        }

        @Override
        public boolean isSameType(IAEFluidStack other) {
            return equals(other);
        }

        @Override
        public boolean isSameType(Object other) {
            return equals(other);
        }

        @Override
        public String getUnlocalizedName() {
            return fluid.getName();
        }

        @Override
        public String getDisplayName() {
            return fluid.getName();
        }

        @Override
        public String getModId() {
            return "test";
        }

        @Override
        public void setTagCompound(NBTTagCompound tag) {}

        @Override
        public boolean hasTagCompound() {
            return false;
        }

        @Override
        public ItemStack getItemStackForNEI() {
            return null;
        }

        @Override
        public void drawInGui(Minecraft mc, int x, int y) {}

        @Override
        public void drawOverlayInGui(Minecraft mc, int x, int y, boolean a, boolean b, boolean c, boolean d) {}

        @Override
        public void drawOnBlockFace(net.minecraft.world.World world) {}

        @Override
        public IAEStackType<IAEFluidStack> getStackType() {
            return AEFluidStackType.FLUID_STACK_TYPE;
        }

        @Override
        public int getAmountPerUnit() {
            return 1;
        }
    }
}
