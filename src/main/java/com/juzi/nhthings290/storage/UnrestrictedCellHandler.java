package com.juzi.nhthings290.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import appeng.api.implementations.tiles.IChestOrDrive;
import appeng.api.storage.ICellHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.client.texture.ExtraBlockTextures;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;

/**
 * AE drive/chest integration. TileDrive may pass MEPassThrough wrappers into
 * getStatusForCell/cellIdleDrain, so those methods use the ItemStack NBT stats
 * and never cast the inventory argument to a concrete cell class.
 */
public class UnrestrictedCellHandler implements ICellHandler {

    @Override
    public boolean isCell(ItemStack stack) {
        return stack != null && (stack.getItem() instanceof UnrestrictedCellItem
            || stack.getItem() instanceof UnrestrictedFluidCellItem);
    }

    @Override
    public IMEInventoryHandler<?> getCellInventory(ItemStack stack, ISaveProvider provider, StorageChannel channel) {
        if (stack == null) {
            return null;
        }
        if (stack.getItem() instanceof CellInventoryProvider) {
            CellInventoryProvider item = (CellInventoryProvider) stack.getItem();
            if ((item instanceof UnrestrictedCellItem && channel != StorageChannel.ITEMS)
                || (item instanceof UnrestrictedFluidCellItem && channel != StorageChannel.FLUIDS)) {
                return null;
            }
            try {
                return item.getCellInventory(stack, provider, null);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    @Override
    public IIcon getTopTexture_Light() {
        return ExtraBlockTextures.BlockMEChestItems_Light.getIcon();
    }

    @Override
    public IIcon getTopTexture_Medium() {
        return ExtraBlockTextures.BlockMEChestItems_Medium.getIcon();
    }

    @Override
    public IIcon getTopTexture_Dark() {
        return ExtraBlockTextures.BlockMEChestItems_Dark.getIcon();
    }

    @Override
    public void openChestGui(EntityPlayer player, IChestOrDrive chest, ICellHandler handler, IMEInventoryHandler inv,
        ItemStack stack, StorageChannel channel) {
        if (chest instanceof TileEntity && channel == StorageChannel.ITEMS) {
            Platform.openGUI(player, (TileEntity) chest, chest.getUp(), GuiBridge.GUI_ME);
        }
    }

    @Override
    public int getStatusForCell(ItemStack stack, IMEInventory inventory) {
        // Prefer the live inventory when AE hands us the real cell object.
        if (inventory instanceof UnrestrictedItemCellInventory) {
            return ((UnrestrictedItemCellInventory) inventory).getStatusForCell();
        }
        if (inventory instanceof UnrestrictedFluidCellInventory) {
            return ((UnrestrictedFluidCellInventory) inventory).getStatusForCell();
        }
        if (inventory instanceof UnrestrictedItemCellHandler) {
            return ((UnrestrictedItemCellHandler) inventory).getCellStatus();
        }
        if (inventory instanceof UnrestrictedFluidCellHandler) {
            return ((UnrestrictedFluidCellHandler) inventory).getCellStatus();
        }
        // TileDrive commonly passes MEPassThrough; fall back to item NBT stats.
        return statusFromStack(stack);
    }

    @Override
    public double cellIdleDrain(ItemStack stack, IMEInventory inventory) {
        if (inventory instanceof UnrestrictedItemCellInventory) {
            return ((UnrestrictedItemCellInventory) inventory).getIdleDrain();
        }
        if (inventory instanceof UnrestrictedFluidCellInventory) {
            return ((UnrestrictedFluidCellInventory) inventory).getIdleDrain();
        }
        if (inventory instanceof UnrestrictedItemCellHandler) {
            return ((UnrestrictedItemCellHandler) inventory).inventory()
                .getIdleDrain();
        }
        if (inventory instanceof UnrestrictedFluidCellHandler) {
            return ((UnrestrictedFluidCellHandler) inventory).inventory()
                .getIdleDrain();
        }
        return 1.0D;
    }

    /**
     * Drive indicator codes: 1 empty, 2 has room, 4 full.
     * Stats are written to the item NBT by CellStorageAccess / CellFluidStorageAccess.
     */
    static int statusFromStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return 1;
        }
        long usedBytes = 0L;
        long totalBytes = 0L;
        if (stack.getItem() instanceof UnrestrictedCellItem) {
            usedBytes = CellStorageAccess.getUsedBytes(stack);
            totalBytes = ((UnrestrictedCellItem) stack.getItem()).getCapacity();
        } else if (stack.getItem() instanceof UnrestrictedFluidCellItem) {
            usedBytes = CellFluidStorageAccess.getUsedBytes(stack);
            totalBytes = ((UnrestrictedFluidCellItem) stack.getItem()).getCapacity();
        } else {
            return 1;
        }
        if (usedBytes <= 0L) {
            return 1;
        }
        return usedBytes < totalBytes ? 2 : 4;
    }
}
