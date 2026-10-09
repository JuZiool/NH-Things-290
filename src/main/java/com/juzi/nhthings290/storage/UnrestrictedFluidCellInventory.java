package com.juzi.nhthings290.storage;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.exceptions.AppEngException;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEFluidStackType;

/** Fluid cell inventory aligned to AE2 2.9 {@link ICellInventory}. */
public class UnrestrictedFluidCellInventory implements IMEInventory<IAEFluidStack>, ICellInventory<IAEFluidStack> {

    private final ItemStack cell;
    private final ISaveProvider provider;
    private final UnrestrictedFluidCellItem type;
    private CellFluidDataStorage storage;

    public UnrestrictedFluidCellInventory(ItemStack cell, ISaveProvider provider, UnrestrictedFluidCellItem type)
        throws AppEngException {
        if (cell == null || !(cell.getItem() instanceof UnrestrictedFluidCellItem)) {
            throw new AppEngException("Not an unrestricted fluid cell");
        }
        this.cell = cell;
        this.provider = provider;
        this.type = type;
    }

    UnrestrictedFluidCellInventory(ItemStack cell, ISaveProvider provider, UnrestrictedFluidCellItem type,
        CellFluidDataStorage storage) throws AppEngException {
        this(cell, provider, type);
        this.storage = storage;
    }

    public static UnrestrictedFluidCellInventory read(ItemStack stack, UnrestrictedFluidCellItem type) {
        try {
            return new UnrestrictedFluidCellInventory(stack, null, type);
        } catch (AppEngException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private IItemList<IAEFluidStack> contents() {
        CellFluidDataStorage data = storage();
        return data == null ? appeng.api.AEApi.instance()
            .storage()
            .createFluidList() : data.getFluids();
    }

    private void save(BaseActionSource source) {
        if (storage != null && (provider != null || source != null)) {
            CellFluidStorageAccess.save(cell, provider, storage, source);
        }
        if (provider != null) {
            provider.saveChanges(this);
        } else if (source != null) {
            CellFluidStorageAccess.notifyMachineSave(source);
        }
    }

    @Override
    public IAEFluidStack injectItems(IAEFluidStack input, Actionable mode, BaseActionSource src) {
        if (input == null || input.getStackSize() <= 0 || type.isBlackListed(cell, input)) {
            return input;
        }
        CellFluidDataStorage data = storage();
        if (data == null) {
            return input;
        }
        long available = getRemainingItemCount();
        long accepted = Math.min(input.getStackSize(), available);
        if (accepted <= 0) {
            return input;
        }
        if (mode == Actionable.MODULATE) {
            data.insert(input, accepted);
            save(src);
        }
        return accepted == input.getStackSize() ? null : copyWithSize(input, input.getStackSize() - accepted);
    }

    private IAEFluidStack copyWithSize(IAEFluidStack source, long size) {
        IAEFluidStack result = source.copy();
        result.setStackSize(size);
        return result;
    }

    @Override
    public IAEFluidStack extractItems(IAEFluidStack request, Actionable mode, BaseActionSource src) {
        if (request == null || request.getStackSize() <= 0) {
            return null;
        }
        CellFluidDataStorage data = storage();
        if (data == null) {
            return null;
        }
        IAEFluidStack stored = data.getFluids()
            .findPrecise(request);
        if (stored == null) {
            return null;
        }
        long amount = Math.min(request.getStackSize(), stored.getStackSize());
        IAEFluidStack result = copyWithSize(stored, amount);
        if (mode == Actionable.MODULATE) {
            data.extract(request, amount);
            save(src);
        }
        return result;
    }

    @Override
    public IItemList<IAEFluidStack> getAvailableItems(IItemList<IAEFluidStack> out) {
        for (IAEFluidStack stack : contents()) {
            out.add(stack);
        }
        return out;
    }

    @Override
    public IAEFluidStack getAvailableItem(IAEFluidStack request) {
        IAEFluidStack stack = contents().findPrecise(request);
        return stack == null ? null : stack.copy();
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.FLUIDS;
    }

    @Override
    public IAEStackType<?> getStackType() {
        return AEFluidStackType.FLUID_STACK_TYPE;
    }

    @Override
    public ItemStack getItemStack() {
        return cell;
    }

    @Override
    public double getIdleDrain() {
        return type.getIdleDrain(cell);
    }

    @Override
    public FuzzyMode getFuzzyMode() {
        return type.getFuzzyMode(cell);
    }

    @Override
    public IInventory getConfigInventory() {
        return type.getConfigInventory(cell);
    }

    @Override
    public IInventory getUpgradesInventory() {
        return type.getUpgradesInventory(cell);
    }

    @Override
    public int getBytesPerType() {
        return 0;
    }

    @Override
    public boolean canHoldNewItem() {
        return getRemainingItemCount() > 0;
    }

    @Override
    public long getTotalBytes() {
        return type.getCapacity();
    }

    @Override
    public long getFreeBytes() {
        return Math.max(0, getTotalBytes() - getUsedBytes());
    }

    @Override
    public long getUsedBytes() {
        return UnrestrictedFluidCellStorageLogic.usedBytes(getStoredItemCount());
    }

    @Override
    public long getTotalItemTypes() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getStoredItemCount() {
        return storage() == null ? 0 : storage().getStoredFluidCount();
    }

    @Override
    public long getStoredItemTypes() {
        return storage() == null ? 0 : storage().getStoredFluidTypes();
    }

    @Override
    public long getRemainingItemTypes() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getRemainingItemCount() {
        return UnrestrictedFluidCellStorageLogic.remainingAmount(getTotalBytes(), getStoredItemCount());
    }

    @Override
    public long getRemainingItemsCountDist(IAEFluidStack stack) {
        return getRemainingItemCount();
    }

    @Override
    public int getUnusedItemCount() {
        return 0;
    }

    @Override
    public int getStatusForCell() {
        if (getStoredItemCount() == 0) {
            return 1;
        }
        return canHoldNewItem() ? 2 : (getRemainingItemCount() > 0 ? 3 : 4);
    }

    @Override
    public String getOreFilter() {
        return "";
    }

    // Compatibility helpers used by partition tooltip / handler.
    public long getStoredFluidCount() {
        return getStoredItemCount();
    }

    public long getStoredFluidTypes() {
        return getStoredItemTypes();
    }

    public long getTotalFluidTypes() {
        return getTotalItemTypes();
    }

    public long getRemainingFluidTypes() {
        return getRemainingItemTypes();
    }

    public long getRemainingFluidCount() {
        return getRemainingItemCount();
    }

    public List<IAEFluidStack> getContents() {
        List<IAEFluidStack> result = new ArrayList<IAEFluidStack>();
        for (IAEFluidStack stack : contents()) {
            result.add(stack);
        }
        return result;
    }

    private CellFluidDataStorage storage() {
        if (storage == null) {
            storage = CellFluidStorageAccess.get(cell, provider);
        }
        return storage;
    }
}
