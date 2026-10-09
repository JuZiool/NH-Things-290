package com.juzi.nhthings290.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import com.juzi.nhthings290.NHThings290;

/** World-persistent UUID to cell-content mapping, following GTO's external storage design. */
public final class CellStorageManager extends WorldSavedData {

    private static final String DATA_NAME = NHThings290.MOD_ID + "_cell_storage";
    private static final String CELLS = "Cells";
    private static final String UUID_TAG = "UUID";
    private static final String CONTENTS = "Contents";
    private static final String FLUID_CELLS = "FluidCells";

    private final Map<UUID, CellDataStorage> storages = new HashMap<UUID, CellDataStorage>();
    private final Map<UUID, CellFluidDataStorage> fluidStorages = new HashMap<UUID, CellFluidDataStorage>();

    public CellStorageManager() {
        super(DATA_NAME);
    }

    public CellStorageManager(String name) {
        super(name);
    }

    public static CellStorageManager get(World world) {
        if (world == null || world.mapStorage == null) return null;
        MapStorage mapStorage = world.mapStorage;
        CellStorageManager manager = (CellStorageManager) mapStorage.loadData(CellStorageManager.class, DATA_NAME);
        if (manager == null) {
            manager = new CellStorageManager();
            mapStorage.setData(DATA_NAME, manager);
        }
        return manager;
    }

    public CellDataStorage getOrCreate(UUID uuid) {
        CellDataStorage storage = storages.get(uuid);
        if (storage == null) {
            storage = new CellDataStorage(uuid);
            storages.put(uuid, storage);
            markDirty();
        }
        return storage;
    }

    public CellFluidDataStorage getOrCreateFluid(UUID uuid) {
        CellFluidDataStorage storage = fluidStorages.get(uuid);
        if (storage == null) {
            storage = new CellFluidDataStorage(uuid);
            fluidStorages.put(uuid, storage);
            markDirty();
        }
        return storage;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        storages.clear();
        fluidStorages.clear();
        NBTTagList cells = tag.getTagList(CELLS, 10);
        for (int i = 0; i < cells.tagCount(); i++) {
            NBTTagCompound cell = cells.getCompoundTagAt(i);
            try {
                UUID uuid = UUID.fromString(cell.getString(UUID_TAG));
                CellDataStorage storage = new CellDataStorage(uuid);
                storage.readFromNBT(cell.getTagList(CONTENTS, 10));
                storages.put(uuid, storage);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed entries and keep the remaining storage usable.
            }
        }
        NBTTagList fluidCells = tag.getTagList(FLUID_CELLS, 10);
        for (int i = 0; i < fluidCells.tagCount(); i++) {
            NBTTagCompound cell = fluidCells.getCompoundTagAt(i);
            try {
                UUID uuid = UUID.fromString(cell.getString(UUID_TAG));
                CellFluidDataStorage storage = new CellFluidDataStorage(uuid);
                storage.readFromNBT(cell.getTagList(CONTENTS, 10));
                fluidStorages.put(uuid, storage);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed entries and keep the remaining storage usable.
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList cells = new NBTTagList();
        for (CellDataStorage storage : storages.values()) {
            if (storage.getStoredItemCount() <= 0) continue;
            NBTTagCompound cell = new NBTTagCompound();
            cell.setString(
                UUID_TAG,
                storage.getUuid()
                    .toString());
            cell.setTag(CONTENTS, storage.writeToNBT());
            cells.appendTag(cell);
        }
        tag.setTag(CELLS, cells);
        NBTTagList fluidCells = new NBTTagList();
        for (CellFluidDataStorage storage : fluidStorages.values()) {
            if (storage.getStoredFluidCount() <= 0) continue;
            NBTTagCompound cell = new NBTTagCompound();
            cell.setString(
                UUID_TAG,
                storage.getUuid()
                    .toString());
            cell.setTag(CONTENTS, storage.writeToNBT());
            fluidCells.appendTag(cell);
        }
        tag.setTag(FLUID_CELLS, fluidCells);
    }
}
