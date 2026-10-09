package com.juzi.nhthings290.item;

import net.minecraft.item.Item;

import com.juzi.nhthings290.NHThings290;

/** Base crafting component for the unrestricted storage cells. */
public class ItemUnrestrictedShell extends Item {

    public ItemUnrestrictedShell() {
        ((Item) this).setUnlocalizedName("unrestricted_shell");
        ((Item) this).setTextureName(NHThings290.MOD_ID + ":unrestricted_shell");
        ((Item) this).setMaxStackSize(64);
        setCreativeTab(NHThings290.CREATIVE_TAB);
    }
}
