package com.juzi.nhthings290.storage;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.data.IAEFluidStack;
import appeng.util.item.AEFluidStack;

/** Resolves fluid cell UUIDs and keeps lightweight client-visible statistics on the item. */
public final class CellFluidStorageAccess {

    public static final String UUID_TAG = CellStorageAccess.UUID_TAG;
    public static final String USED_COUNT_TAG = CellStorageAccess.USED_COUNT_TAG;
    public static final String USED_TYPES_TAG = CellStorageAccess.USED_TYPES_TAG;
    public static final String USED_BYTES_TAG = CellStorageAccess.USED_BYTES_TAG;
    private static final String LEGACY_CONTENTS = "NHContents";

    private CellFluidStorageAccess() {}

    public static CellFluidDataStorage get(ItemStack cell, ISaveProvider provider) {
        if (cell == null) return null;
        World world = globalStorageWorld();
        if (world == null || world.isRemote) return null;

        NBTTagCompound tag = getOrCreateTag(cell);
        UUID uuid = readUuid(tag);
        if (uuid == null) {
            uuid = UUID.randomUUID();
            tag.setString(UUID_TAG, uuid.toString());
        }

        CellStorageManager manager = CellStorageManager.get(world);
        if (manager == null) return null;
        CellFluidDataStorage storage = manager.getOrCreateFluid(uuid);
        migrateFromProviderWorld(uuid, storage, manager, provider);
        migrateLegacyContents(tag, storage, manager);
        updateStats(tag, storage);
        return storage;
    }

    public static void save(ItemStack cell, ISaveProvider provider, CellFluidDataStorage storage) {
        save(cell, provider, storage, null);
    }

    public static void save(ItemStack cell, ISaveProvider provider, CellFluidDataStorage storage,
        BaseActionSource source) {
        if (cell == null || storage == null) return;
        updateStats(getOrCreateTag(cell), storage);
        CellStorageManager manager = CellStorageManager.get(globalStorageWorld());
        if (manager != null) manager.markDirty();
        // provider/source only mark the host dirty for container NBT sync; never select storage world.
        notifyMachineSave(source);
        if (provider instanceof TileEntity) {
            ((TileEntity) provider).markDirty();
        }
    }

    public static void notifyMachineSave(BaseActionSource source) {
        CellStorageAccess.notifyMachineSave(source);
    }

    public static long getStoredFluidCount(ItemStack cell) {
        return getStat(cell, USED_COUNT_TAG);
    }

    public static long getStoredFluidTypes(ItemStack cell) {
        return getStat(cell, USED_TYPES_TAG);
    }

    public static long getUsedBytes(ItemStack cell) {
        return getStat(cell, USED_BYTES_TAG);
    }

    public static boolean isEmpty(ItemStack cell, ISaveProvider provider) {
        CellFluidDataStorage storage = get(cell, provider);
        return storage != null && storage.getStoredFluidCount() <= 0;
    }

    /**
     * Fail-closed disassembly gate: NBT stats and global world storage must both confirm empty.
     * If world storage cannot be resolved, refuse disassembly.
     */
    public static boolean canDisassemble(ItemStack cell) {
        if (cell == null) return false;
        CellFluidDataStorage storage = get(cell, null);
        return CellDisassembleLogic.canDisassemble(
            getStoredFluidCount(cell),
            getUsedBytes(cell),
            getStoredFluidTypes(cell),
            storage != null,
            storage == null ? 0L : storage.getStoredFluidCount());
    }

    static World globalStorageWorld() {
        return CellStorageAccess.globalStorageWorld();
    }

    private static void migrateFromProviderWorld(UUID uuid, CellFluidDataStorage global,
        CellStorageManager globalManager, ISaveProvider provider) {
        if (global == null || global.getStoredFluidCount() > 0 || !(provider instanceof TileEntity)) return;
        World providerWorld = ((TileEntity) provider).getWorldObj();
        World storageWorld = globalStorageWorld();
        if (providerWorld == null || storageWorld == null || providerWorld == storageWorld) return;
        if (providerWorld.provider != null && storageWorld.provider != null
            && providerWorld.provider.dimensionId == storageWorld.provider.dimensionId) {
            return;
        }
        CellStorageManager legacyManager = CellStorageManager.get(providerWorld);
        if (legacyManager == null) return;
        CellFluidDataStorage legacy = legacyManager.getOrCreateFluid(uuid);
        if (legacy == null || legacy.getStoredFluidCount() <= 0) return;
        // Snapshot first: fluid extract rebuilds the live list and would break a live iterator.
        ArrayList<IAEFluidStack> snapshot = new ArrayList<IAEFluidStack>();
        for (IAEFluidStack stack : legacy.getFluids()) {
            if (stack == null || stack.getStackSize() <= 0) continue;
            snapshot.add(stack.copy());
        }
        for (IAEFluidStack stack : snapshot) {
            global.addImported(stack.copy());
            legacy.extract(stack, stack.getStackSize());
        }
        globalManager.markDirty();
        legacyManager.markDirty();
    }

    private static void migrateLegacyContents(NBTTagCompound tag, CellFluidDataStorage storage,
        CellStorageManager manager) {
        if (!tag.hasKey(LEGACY_CONTENTS, 9)) return;
        if (storage.getStoredFluidCount() <= 0) {
            NBTTagList legacy = tag.getTagList(LEGACY_CONTENTS, 10);
            for (int i = 0; i < legacy.tagCount(); i++) {
                IAEFluidStack stack = AEFluidStack.loadFluidStackFromNBT(legacy.getCompoundTagAt(i));
                storage.addImported(stack);
            }
        }
        tag.removeTag(LEGACY_CONTENTS);
        manager.markDirty();
    }

    private static void updateStats(NBTTagCompound tag, CellFluidDataStorage storage) {
        tag.setLong(USED_COUNT_TAG, storage.getStoredFluidCount());
        tag.setLong(USED_TYPES_TAG, storage.getStoredFluidTypes());
        tag.setLong(USED_BYTES_TAG, storage.getUsedBytes());
    }

    private static NBTTagCompound getOrCreateTag(ItemStack cell) {
        NBTTagCompound tag = cell.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            cell.setTagCompound(tag);
        }
        return tag;
    }

    private static long getStat(ItemStack cell, String key) {
        return cell != null && cell.hasTagCompound() ? cell.getTagCompound()
            .getLong(key) : 0L;
    }

    private static UUID readUuid(NBTTagCompound tag) {
        if (!tag.hasKey(UUID_TAG, 8)) return null;
        try {
            return UUID.fromString(tag.getString(UUID_TAG));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
