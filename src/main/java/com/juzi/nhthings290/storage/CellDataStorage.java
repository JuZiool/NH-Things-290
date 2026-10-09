package com.juzi.nhthings290.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEItemStack;

/** One UUID-backed item collection used by an unrestricted cell. */
public final class CellDataStorage {

    private final UUID uuid;
    private final Map<ItemKey, IAEItemStack> items = new HashMap<ItemKey, IAEItemStack>();
    private IItemList<IAEItemStack> availableCache;
    private boolean cacheDirty = true;
    private long storedItemCount;

    public CellDataStorage(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public IItemList<IAEItemStack> getItems() {
        rebuildCacheIfNeeded();
        return availableCache;
    }

    public IAEItemStack find(IAEItemStack stack) {
        IAEItemStack stored = items.get(ItemKey.of(stack));
        return stored == null ? null : stored;
    }

    public void addImported(IAEItemStack stack) {
        if (stack == null || stack.getStackSize() <= 0) return;
        ItemKey key = ItemKey.of(stack);
        IAEItemStack stored = items.get(key);
        if (stored == null) {
            stored = stack.copy();
            items.put(key, stored);
        } else {
            stored.incStackSize(stack.getStackSize());
        }
        storedItemCount += stack.getStackSize();
        cacheDirty = true;
    }

    public long insert(IAEItemStack stack, long amount) {
        if (stack == null || amount <= 0) return 0;
        ItemKey key = ItemKey.of(stack);
        IAEItemStack stored = items.get(key);
        if (stored == null) {
            stored = stack.copy();
            stored.setStackSize(amount);
            items.put(key, stored);
        } else {
            stored.incStackSize(amount);
        }
        storedItemCount += amount;
        cacheDirty = true;
        return amount;
    }

    public long extract(IAEItemStack stack, long amount) {
        if (stack == null || amount <= 0) return 0;
        ItemKey key = ItemKey.of(stack);
        IAEItemStack stored = items.get(key);
        if (stored == null) return 0;
        long extracted = Math.min(amount, stored.getStackSize());
        stored.decStackSize(extracted);
        storedItemCount -= extracted;
        if (stored.getStackSize() <= 0) items.remove(key);
        cacheDirty = true;
        return extracted;
    }

    public IAEItemStack getAvailableItem(IAEItemStack stack) {
        IAEItemStack stored = find(stack);
        return stored == null ? null : stored.copy();
    }

    public IItemList<IAEItemStack> addAvailableItems(IItemList<IAEItemStack> out) {
        rebuildCacheIfNeeded();
        for (IAEItemStack stack : availableCache) out.add(stack);
        return out;
    }

    public long getStoredItemCount() {
        return storedItemCount;
    }

    public long getStoredItemTypes() {
        return items.size();
    }

    public long getUsedBytes() {
        return (getStoredItemCount() + 7L) / 8L;
    }

    public void readFromNBT(NBTTagList list) {
        items.clear();
        storedItemCount = 0;
        if (list == null) return;
        for (int i = 0; i < list.tagCount(); i++) {
            IAEItemStack stack = AEItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
            addImported(stack);
        }
        cacheDirty = true;
    }

    public NBTTagList writeToNBT() {
        NBTTagList list = new NBTTagList();
        for (IAEItemStack stack : items.values()) {
            if (stack.getStackSize() <= 0) continue;
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            list.appendTag(tag);
        }
        return list;
    }

    private void rebuildCacheIfNeeded() {
        if (!cacheDirty && availableCache != null) return;
        availableCache = AEApi.instance()
            .storage()
            .createItemList();
        for (IAEItemStack stack : items.values()) availableCache.add(stack);
        cacheDirty = false;
    }
}
