package com.juzi.nhthings290.storage;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;

/** Resolves cell UUIDs and keeps lightweight client-visible statistics on the item. */
public final class CellStorageAccess {

    public static final String UUID_TAG = "nh_cell_uuid";
    public static final String USED_COUNT_TAG = "nh_used_amount";
    public static final String USED_TYPES_TAG = "nh_used_types";
    public static final String USED_BYTES_TAG = "nh_used_bytes";
    private static final String LEGACY_CONTENTS = "NHContents";

    private CellStorageAccess() {}

    public static CellDataStorage get(ItemStack cell, ISaveProvider provider) {
        if (cell == null) return null;
        World world = globalStorageWorld();
        if (world == null || world.isRemote) return null;

        NBTTagCompound tag = cell.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            cell.setTagCompound(tag);
        }
        UUID uuid = readUuid(tag);
        if (uuid == null) {
            uuid = UUID.randomUUID();
            tag.setString(UUID_TAG, uuid.toString());
        }

        CellStorageManager manager = CellStorageManager.get(world);
        if (manager == null) return null;
        CellDataStorage storage = manager.getOrCreate(uuid);
        migrateFromProviderWorld(uuid, storage, manager, provider);
        migrateLegacyContents(tag, storage, manager);
        updateStats(tag, storage);
        return storage;
    }

    public static void save(ItemStack cell, ISaveProvider provider, CellDataStorage storage) {
        save(cell, provider, storage, null);
    }

    public static void save(ItemStack cell, ISaveProvider provider, CellDataStorage storage, BaseActionSource source) {
        if (cell == null || storage == null) return;
        NBTTagCompound tag = cell.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            cell.setTagCompound(tag);
        }
        updateStats(tag, storage);
        World world = globalStorageWorld();
        CellStorageManager manager = CellStorageManager.get(world);
        if (manager != null) manager.markDirty();
        // provider/source only mark the host dirty for container NBT sync; never select storage world.
        notifyMachineSave(source);
        if (provider instanceof TileEntity) {
            ((TileEntity) provider).markDirty();
        }
    }

    /**
     * IO ports pass a null ISaveProvider and identify themselves through the action source.
     * Mark that host dirty after changing the cell stack so its container can sync the new NBT.
     */
    public static void notifyMachineSave(BaseActionSource source) {
        if (!(source instanceof MachineSource)) return;
        IActionHost host = ((MachineSource) source).via;
        if (host instanceof TileEntity) ((TileEntity) host).markDirty();
    }

    public static long getStoredItemCount(ItemStack cell) {
        return getStat(cell, USED_COUNT_TAG);
    }

    /** Returns true only after the server-side storage has confirmed that no items are stored. */
    public static boolean isEmpty(ItemStack cell, ISaveProvider provider) {
        CellDataStorage storage = get(cell, provider);
        return storage != null && storage.getStoredItemCount() <= 0;
    }

    /**
     * Fail-closed disassembly gate: NBT stats and global world storage must both confirm empty.
     * If world storage cannot be resolved, refuse disassembly.
     */
    public static boolean canDisassemble(ItemStack cell) {
        if (cell == null) return false;
        CellDataStorage storage = get(cell, null);
        return CellDisassembleLogic.canDisassemble(
            getStoredItemCount(cell),
            getUsedBytes(cell),
            getStoredItemTypes(cell),
            storage != null,
            storage == null ? 0L : storage.getStoredItemCount());
    }

    public static long getStoredItemTypes(ItemStack cell) {
        return getStat(cell, USED_TYPES_TAG);
    }

    public static long getUsedBytes(ItemStack cell) {
        return getStat(cell, USED_BYTES_TAG);
    }

    /**
     * Always use overworld / first available server world mapStorage so cells never
     * fork content across dimensions based on the host machine location.
     */
    static World globalStorageWorld() {
        if (FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient()) return null;
        World world = DimensionManager.getWorld(0);
        if (world != null) return world;
        MinecraftServer server = MinecraftServer.getServer();
        return server == null || server.worldServers == null || server.worldServers.length == 0 ? null
            : server.worldServers[0];
    }

    /**
     * One-time pull of pre-0.1.9a content that was written into a non-global dimension mapStorage.
     */
    private static void migrateFromProviderWorld(UUID uuid, CellDataStorage global, CellStorageManager globalManager,
        ISaveProvider provider) {
        if (global == null || global.getStoredItemCount() > 0 || !(provider instanceof TileEntity)) return;
        World providerWorld = ((TileEntity) provider).getWorldObj();
        World storageWorld = globalStorageWorld();
        if (providerWorld == null || storageWorld == null || providerWorld == storageWorld) return;
        if (providerWorld.provider != null && storageWorld.provider != null
            && providerWorld.provider.dimensionId == storageWorld.provider.dimensionId) {
            return;
        }
        CellStorageManager legacyManager = CellStorageManager.get(providerWorld);
        if (legacyManager == null) return;
        CellDataStorage legacy = legacyManager.getOrCreate(uuid);
        if (legacy == null || legacy.getStoredItemCount() <= 0) return;
        // Snapshot first: extract may dirty/rebuild the live list while iterating.
        ArrayList<IAEItemStack> snapshot = new ArrayList<IAEItemStack>();
        for (IAEItemStack stack : legacy.getItems()) {
            if (stack == null || stack.getStackSize() <= 0) continue;
            snapshot.add(stack.copy());
        }
        for (IAEItemStack stack : snapshot) {
            global.addImported(stack.copy());
            // Leave legacy entry empty after copy so reloads do not double-count.
            legacy.extract(stack, stack.getStackSize());
        }
        globalManager.markDirty();
        legacyManager.markDirty();
    }

    private static void migrateLegacyContents(NBTTagCompound tag, CellDataStorage storage, CellStorageManager manager) {
        if (!tag.hasKey(LEGACY_CONTENTS, 9)) return;
        if (storage.getStoredItemCount() <= 0) {
            NBTTagList legacy = tag.getTagList(LEGACY_CONTENTS, 10);
            for (int i = 0; i < legacy.tagCount(); i++) {
                IAEItemStack stack = AEItemStack.loadItemStackFromNBT(legacy.getCompoundTagAt(i));
                storage.addImported(stack);
            }
        }
        tag.removeTag(LEGACY_CONTENTS);
        manager.markDirty();
    }

    private static void updateStats(NBTTagCompound tag, CellDataStorage storage) {
        tag.setLong(USED_COUNT_TAG, storage.getStoredItemCount());
        tag.setLong(USED_TYPES_TAG, storage.getStoredItemTypes());
        tag.setLong(USED_BYTES_TAG, storage.getUsedBytes());
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
