package com.juzi.nhthings290.storage;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.exceptions.AppEngException;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEItemStackType;

public class UnrestrictedItemCellInventory
    implements IMEInventory<IAEItemStack>, appeng.api.storage.ICellInventory<IAEItemStack> {

    private static final String CONTENTS = "NHContents";
    private final ItemStack cell;
    private final ISaveProvider provider;
    private final UnrestrictedCellItem type;
    private CellDataStorage storage;

    public UnrestrictedItemCellInventory(ItemStack cell, ISaveProvider provider, UnrestrictedCellItem type)
        throws AppEngException {
        if (cell == null || !(cell.getItem() instanceof UnrestrictedCellItem))
            throw new AppEngException("Not an unrestricted item cell");
        this.cell = cell;
        this.provider = provider;
        this.type = type;
    }

    public static UnrestrictedItemCellInventory read(ItemStack stack, UnrestrictedCellItem type) {
        try {
            return new UnrestrictedItemCellInventory(stack, null, type);
        } catch (AppEngException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private IItemList<IAEItemStack> contents() {
        if (storage == null) storage = CellStorageAccess.get(cell, provider);
        if (storage != null) return storage.getItems();
        return appeng.api.AEApi.instance()
            .storage()
            .createItemList();
    }

    private void save(BaseActionSource source) {
        if (storage != null && (provider != null || source != null)) {
            CellStorageAccess.save(cell, provider, storage, source);
        }
        if (provider != null) provider.saveChanges(this);
        else if (source != null) CellStorageAccess.notifyMachineSave(source);
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, BaseActionSource src) {
        if (input == null || input.getStackSize() <= 0 || type.isBlackListed(cell, input)) return input;
        CellDataStorage data = storage();
        if (data == null) return input;
        IAEItemStack stored = data.find(input);
        if (stored != null) {
            long available = getRemainingItemCount();
            long accepted = Math.min(input.getStackSize(), available);
            if (accepted <= 0) return input;
            if (mode == Actionable.MODULATE) {
                data.insert(input, accepted);
                save(src);
            }
            return accepted == input.getStackSize() ? null : copyWithSize(input, input.getStackSize() - accepted);
        }
        if (!canHoldNewItem()) return input;
        long available = getRemainingItemCount();
        long accepted = Math.min(input.getStackSize(), Math.max(0, available));
        if (accepted <= 0) return input;
        if (mode == Actionable.MODULATE) {
            data.insert(input, accepted);
            save(src);
        }
        return accepted == input.getStackSize() ? null : copyWithSize(input, input.getStackSize() - accepted);
    }

    private IAEItemStack copyWithSize(IAEItemStack source, long size) {
        IAEItemStack result = source.copy();
        result.setStackSize(size);
        return result;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        if (request == null || request.getStackSize() <= 0) return null;
        CellDataStorage data = storage();
        if (data == null) return null;
        IAEItemStack stored = data.find(request);
        if (stored == null) return null;
        long amount = Math.min(request.getStackSize(), stored.getStackSize());
        IAEItemStack result = copyWithSize(stored, amount);
        if (mode == Actionable.MODULATE) {
            data.extract(request, amount);
            save(src);
        }
        return result;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        return storage() == null ? out : storage().addAvailableItems(out);
    }

    @Override
    public IAEItemStack getAvailableItem(IAEItemStack request) {
        return storage() == null ? null : storage().getAvailableItem(request);
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    @Override
    public IAEStackType<?> getStackType() {
        return AEItemStackType.ITEM_STACK_TYPE;
    }

    @Override
    public ItemStack getItemStack() {
        return cell;
    }

    @Override
    public double getIdleDrain() {
        return type.getIdleDrain();
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
    public String getOreFilter() {
        return type.getOreFilter(cell);
    }

    /** GTO-style cells charge capacity by total item amount and never by item type. */
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
        return storage() == null ? 0 : storage().getUsedBytes();
    }

    @Override
    public long getTotalItemTypes() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getStoredItemCount() {
        return storage() == null ? 0 : storage().getStoredItemCount();
    }

    @Override
    public long getStoredItemTypes() {
        return storage() == null ? 0 : storage().getStoredItemTypes();
    }

    @Override
    public long getRemainingItemTypes() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getRemainingItemCount() {
        return Math.max(0, getTotalBytes() * 8L - getStoredItemCount());
    }

    @Override
    public long getRemainingItemsCountDist(IAEItemStack stack) {
        return getRemainingItemCount();
    }

    @Override
    public int getUnusedItemCount() {
        return 0;
    }

    @Override
    public int getStatusForCell() {
        if (getStoredItemCount() == 0) return 1;
        return canHoldNewItem() ? 2 : (getRemainingItemCount() > 0 ? 3 : 4);
    }

    private CellDataStorage storage() {
        if (storage == null) storage = CellStorageAccess.get(cell, provider);
        return storage;
    }
}
