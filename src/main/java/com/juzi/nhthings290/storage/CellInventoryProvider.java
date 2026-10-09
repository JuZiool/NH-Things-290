package com.juzi.nhthings290.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.exceptions.AppEngException;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;

public interface CellInventoryProvider {

    IMEInventoryHandler<?> getCellInventory(ItemStack stack, ISaveProvider provider, EntityPlayer player)
        throws AppEngException;
}
