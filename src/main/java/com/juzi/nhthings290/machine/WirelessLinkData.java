package com.juzi.nhthings290.machine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import com.juzi.nhthings290.NHThings290;

/** One persistent source per target. Station UUIDs prevent a replacement block inheriting old links. */
public final class WirelessLinkData extends WorldSavedData {

    private static final String DATA_NAME = NHThings290.MOD_ID + "_wireless_links";
    private final Map<Position, Link> links = new LinkedHashMap<>();
    private long revision;

    public WirelessLinkData() {
        super(DATA_NAME);
    }

    public WirelessLinkData(String name) {
        super(name);
    }

    public static WirelessLinkData get(World world) {
        MapStorage storage = world.mapStorage;
        WirelessLinkData data = (WirelessLinkData) storage.loadData(WirelessLinkData.class, DATA_NAME);
        if (data == null) {
            data = new WirelessLinkData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public boolean bind(Position target, Station station) {
        if (target.dimension != station.position.dimension || target.equals(station.position)) return false;
        if (isBound(target, station)) return false;
        links.put(target, new Link(station, -1));
        changed();
        return true;
    }

    /** Toggles the selected source and returns whether the target is now connected to it. */
    public boolean toggleBinding(Position target, Station station) {
        if (unbind(target, station)) return false;
        return bind(target, station);
    }

    public boolean isBound(Position target, Station station) {
        Link link = links.get(target);
        return link != null && station.equals(link.station);
    }

    public int machineId(Position target, Station station) {
        Link link = links.get(target);
        return link != null && station.equals(link.station) ? link.machineId : -1;
    }

    public void setMachineId(Position target, Station station, int machineId) {
        Link link = links.get(target);
        if (link != null && station.equals(link.station) && machineId >= 0 && machineId != link.machineId) {
            link.machineId = machineId;
            markDirty();
        }
    }

    public boolean unbind(Position target, Station station) {
        if (!isBound(target, station)) return false;
        links.remove(target);
        changed();
        return true;
    }

    public int removeMissingTargets(Station station, Predicate<Position> isMissing) {
        int originalSize = links.size();
        links.entrySet()
            .removeIf(entry -> station.equals(entry.getValue().station) && isMissing.test(entry.getKey()));
        int removed = originalSize - links.size();
        if (removed > 0) changed();
        return removed;
    }

    public List<Position> targets(Station station) {
        List<Position> result = new ArrayList<>();
        for (Map.Entry<Position, Link> link : links.entrySet()) {
            if (station.equals(link.getValue().station)) result.add(link.getKey());
        }
        return result;
    }

    public long revision() {
        return revision;
    }

    private void changed() {
        revision++;
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        links.clear();
        NBTTagList list = tag.getTagList("Links", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            Station station = Station.read(entry.getCompoundTag("Station"));
            Position target = Position.read(entry.getCompoundTag("Target"));
            if (station != null && target.dimension == station.position.dimension && !target.equals(station.position)) {
                links.put(target, new Link(station, entry.hasKey("MachineId", 3) ? entry.getInteger("MachineId") : -1));
            }
        }
        revision++;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Position, Link> link : links.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag(
                "Target",
                link.getKey()
                    .write());
            entry.setTag("Station", link.getValue().station.write());
            entry.setInteger("MachineId", link.getValue().machineId);
            list.appendTag(entry);
        }
        tag.setTag("Links", list);
    }

    private static final class Link {

        private final Station station;
        private int machineId;

        private Link(Station station, int machineId) {
            this.station = station;
            this.machineId = machineId;
        }
    }

    public static final class Position {

        public final int dimension;
        public final int x;
        public final int y;
        public final int z;

        public Position(int dimension, int x, int y, int z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Dimension", dimension);
            tag.setInteger("X", x);
            tag.setInteger("Y", y);
            tag.setInteger("Z", z);
            return tag;
        }

        public static Position read(NBTTagCompound tag) {
            return new Position(
                tag.getInteger("Dimension"),
                tag.getInteger("X"),
                tag.getInteger("Y"),
                tag.getInteger("Z"));
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Position)) return false;
            Position position = (Position) other;
            return dimension == position.dimension && x == position.x && y == position.y && z == position.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, x, y, z);
        }

        @Override
        public String toString() {
            return dimension + ": " + x + ", " + y + ", " + z;
        }
    }

    public static final class Station {

        public final Position position;
        public final UUID id;

        public Station(Position position, UUID id) {
            this.position = position;
            this.id = id;
        }

        public NBTTagCompound write() {
            NBTTagCompound tag = position.write();
            tag.setString("UUID", id.toString());
            return tag;
        }

        public static Station read(NBTTagCompound tag) {
            if (!tag.hasKey("Dimension", 3) || !tag.hasKey("X", 3) || !tag.hasKey("Y", 3) || !tag.hasKey("Z", 3))
                return null;
            try {
                return new Station(Position.read(tag), UUID.fromString(tag.getString("UUID")));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Station)) return false;
            Station station = (Station) other;
            return position.equals(station.position) && id.equals(station.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, id);
        }
    }
}
