package com.juzi.nhthings290.storage;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEItemStack;

/** Immutable identity for an AE item stack; quantity is deliberately excluded. */
public final class ItemKey {

    private final int itemId;
    private final int damage;
    private final String tag;

    private ItemKey(int itemId, int damage, String tag) {
        this.itemId = itemId;
        this.damage = damage;
        this.tag = tag;
    }

    public static ItemKey of(IAEItemStack stack) {
        if (stack == null || stack.getItemStack() == null) return null;
        ItemStack item = stack.getItemStack();
        return new ItemKey(
            Item.getIdFromItem(item.getItem()),
            item.getItemDamage(),
            item.hasTagCompound() ? item.getTagCompound()
                .toString() : "");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ItemKey)) return false;
        ItemKey key = (ItemKey) other;
        return itemId == key.itemId && damage == key.damage && tag.equals(key.tag);
    }

    @Override
    public int hashCode() {
        int result = itemId;
        result = 31 * result + damage;
        result = 31 * result + tag.hashCode();
        return result;
    }
}
