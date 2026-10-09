package com.juzi.nhthings290.storage;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidPacket;
import com.glodblock.github.util.Util;

import appeng.api.AEApi;
import appeng.api.config.IncludeExclude;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.storage.ICellCacheRegistry;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import appeng.me.storage.MEInventoryHandler;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEFluidStackType;
import appeng.util.prioitylist.PrecisePriorityList;

/**
 * Fluid cell handler for 2.9: MEInventoryHandler now takes IAEStackType.
 */
public class UnrestrictedFluidCellHandler extends MEInventoryHandler<IAEFluidStack> implements ICellCacheRegistry {

    private final UnrestrictedFluidCellInventory cellInventory;

    public UnrestrictedFluidCellHandler(UnrestrictedFluidCellInventory inventory) {
        super(inventory, AEFluidStackType.FLUID_STACK_TYPE);
        this.cellInventory = inventory;
        applyPartition(inventory);
    }

    private void applyPartition(UnrestrictedFluidCellInventory ci) {
        IInventory config = ci.getConfigInventory();
        IItemList<IAEFluidStack> priorityList = AEApi.instance()
            .storage()
            .createFluidList();

        for (int x = 0; x < config.getSizeInventory(); x++) {
            ItemStack is = config.getStackInSlot(x);
            FluidStack fluid = Util.getFluidFromItem(is);
            if (fluid != null) {
                IAEFluidStack aeFluid = AEFluidStack.create(fluid);
                if (aeFluid != null) {
                    aeFluid.setStackSize(1);
                    priorityList.add(aeFluid);
                }
                if (is != null && !(is.getItem() instanceof ItemFluidPacket)) {
                    config.setInventorySlotContents(x, ItemFluidPacket.newDisplayStack(fluid));
                    config.markDirty();
                }
            }
        }

        if (!priorityList.isEmpty()) {
            setPartitionList(new PrecisePriorityList<IAEFluidStack>(priorityList));
        }

        IInventory upgrades = ci.getUpgradesInventory();
        boolean hasSticky = false;
        boolean hasInverter = false;
        for (int x = 0; x < upgrades.getSizeInventory(); x++) {
            ItemStack is = upgrades.getStackInSlot(x);
            if (is != null && is.getItem() instanceof IUpgradeModule) {
                Upgrades u = ((IUpgradeModule) is.getItem()).getType(is);
                if (u == Upgrades.STICKY) {
                    hasSticky = true;
                } else if (u == Upgrades.INVERTER) {
                    hasInverter = true;
                }
            }
        }

        setWhitelist(hasInverter ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST);
        if (hasSticky) {
            setSticky(true);
        }
    }

    @Override
    public boolean canGetInv() {
        return true;
    }

    UnrestrictedFluidCellInventory inventory() {
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
        return cellInventory.getTotalFluidTypes();
    }

    @Override
    public long getFreeTypes() {
        return cellInventory.getRemainingFluidTypes();
    }

    @Override
    public long getUsedTypes() {
        return cellInventory.getStoredFluidTypes();
    }

    @Override
    public int getCellStatus() {
        int val = cellInventory.getStatusForCell();
        if (val == 1 && !getPartitionList().isEmpty()) {
            val = 2;
        }
        return val;
    }

    @Override
    public TYPE getCellType() {
        return TYPE.FLUID;
    }

    @Override
    public StorageChannel getStorageChannel() {
        return StorageChannel.FLUIDS;
    }
}
