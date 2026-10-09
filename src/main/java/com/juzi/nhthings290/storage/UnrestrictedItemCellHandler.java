package com.juzi.nhthings290.storage;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.storage.ICellCacheRegistry;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.storage.MEInventoryHandler;
import appeng.util.item.AEItemStack;
import appeng.util.item.AEItemStackType;
import appeng.util.prioitylist.FuzzyPriorityList;
import appeng.util.prioitylist.OreFilteredList;
import appeng.util.prioitylist.PrecisePriorityList;

/**
 * Mirrors AE2 {@code CellInventoryHandler} / AE2Things infinity cell partition setup so
 * Cell Workbench config + upgrades actually filter what the cell can accept.
 */
public class UnrestrictedItemCellHandler extends MEInventoryHandler<IAEItemStack> implements ICellCacheRegistry {

    private final UnrestrictedItemCellInventory cellInventory;

    public UnrestrictedItemCellHandler(UnrestrictedItemCellInventory inventory) {
        super(inventory, AEItemStackType.ITEM_STACK_TYPE);
        this.cellInventory = inventory;
        applyPartition(inventory);
    }

    private void applyPartition(UnrestrictedItemCellInventory ci) {
        IInventory upgrades = ci.getUpgradesInventory();
        IInventory config = ci.getConfigInventory();
        FuzzyMode fzMode = ci.getFuzzyMode();
        String filter = ci.getOreFilter();

        boolean hasInverter = false;
        boolean hasFuzzy = false;
        boolean hasOreFilter = false;
        boolean hasSticky = false;

        for (int x = 0; x < upgrades.getSizeInventory(); x++) {
            ItemStack is = upgrades.getStackInSlot(x);
            if (is != null && is.getItem() instanceof IUpgradeModule) {
                Upgrades u = ((IUpgradeModule) is.getItem()).getType(is);
                if (u != null) {
                    switch (u) {
                        case FUZZY:
                            hasFuzzy = true;
                            break;
                        case INVERTER:
                            hasInverter = true;
                            break;
                        case ORE_FILTER:
                            hasOreFilter = true;
                            break;
                        case STICKY:
                            hasSticky = true;
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        setWhitelist(hasInverter ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST);
        if (hasSticky) {
            setSticky(true);
        }

        if (hasOreFilter && filter != null && !filter.isEmpty()) {
            setPartitionList(new OreFilteredList(filter));
            return;
        }

        IItemList<IAEItemStack> priorityList = AEApi.instance()
            .storage()
            .createItemList();
        for (int x = 0; x < config.getSizeInventory(); x++) {
            ItemStack is = config.getStackInSlot(x);
            if (is != null) {
                priorityList.add(AEItemStack.create(is));
            }
        }
        if (!priorityList.isEmpty()) {
            if (hasFuzzy) {
                setPartitionList(new FuzzyPriorityList<IAEItemStack>(priorityList, fzMode));
            } else {
                setPartitionList(new PrecisePriorityList<IAEItemStack>(priorityList));
            }
        }
    }

    @Override
    public boolean canGetInv() {
        return true;
    }

    UnrestrictedItemCellInventory inventory() {
        return cellInventory;
    }

    @Override
    public long getTotalBytes() {
        return cellInventory.getTotalBytes();
    }

    @Override
    public long getFreeBytes() {
        return cellInventory.getFreeBytes();
    }

    @Override
    public long getUsedBytes() {
        return cellInventory.getUsedBytes();
    }

    @Override
    public long getTotalTypes() {
        return cellInventory.getTotalItemTypes();
    }

    @Override
    public long getFreeTypes() {
        return cellInventory.getRemainingItemTypes();
    }

    @Override
    public long getUsedTypes() {
        return cellInventory.getStoredItemTypes();
    }

    @Override
    public int getCellStatus() {
        int val = cellInventory.getStatusForCell();
        // Match AE: preformatted empty cells show as "types used" status.
        if (val == 1 && !getPartitionList().isEmpty()) {
            val = 2;
        }
        return val;
    }

    @Override
    public TYPE getCellType() {
        return TYPE.ITEM;
    }

    @Override
    public StorageChannel getStorageChannel() {
        return StorageChannel.ITEMS;
    }
}
