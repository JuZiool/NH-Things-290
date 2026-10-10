package com.juzi.nhthings290.machine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import com.juzi.nhthings290.machine.WirelessLinkData.Position;
import com.juzi.nhthings290.machine.WirelessLinkData.Station;

public class WirelessLinkDataTest {

    @Test
    public void rebindReplacesOldSourceAndRepeatedClickDoesNotDuplicateTarget() {
        WirelessLinkData links = new WirelessLinkData();
        Position target = new Position(0, 10, 64, 0);
        Station first = station(0);
        Station second = station(1);
        assertTrue(links.bind(target, first));
        long revision = links.revision();
        assertFalse(links.bind(target, first));
        assertEquals(revision, links.revision());
        assertTrue(links.bind(target, second));
        assertTrue(
            links.targets(first)
                .isEmpty());
        assertEquals(
            1,
            links.targets(second)
                .size());
        assertFalse(links.unbind(target, first));
        assertTrue(links.isBound(target, second));
        assertTrue(links.unbind(target, second));
        assertTrue(
            links.targets(second)
                .isEmpty());
    }

    @Test
    public void bindingsSurviveWorldSaveAndReplacementStationDoesNotInheritThem() {
        WirelessLinkData original = new WirelessLinkData();
        Position target = new Position(-1, -10, 65, 20);
        Station source = new Station(new Position(-1, 0, 64, 0), UUID.randomUUID());
        assertTrue(original.bind(target, source));
        NBTTagCompound saved = new NBTTagCompound();
        original.writeToNBT(saved);
        WirelessLinkData loaded = new WirelessLinkData();
        loaded.readFromNBT(saved);
        assertTrue(loaded.isBound(target, source));
        assertEquals(
            target,
            loaded.targets(source)
                .get(0));
        Station replacement = new Station(source.position, UUID.randomUUID());
        assertTrue(
            loaded.targets(replacement)
                .isEmpty());
        assertFalse(loaded.isBound(target, replacement));
        assertTrue(loaded.bind(target, replacement));
        assertTrue(
            loaded.targets(source)
                .isEmpty());
    }

    @Test
    public void crossDimensionAndSelfLinksAreRejected() {
        WirelessLinkData links = new WirelessLinkData();
        Station source = station(0);
        assertFalse(links.bind(source.position, source));
        assertFalse(links.bind(new Position(-1, 0, 64, 0), source));
        assertTrue(
            links.targets(source)
                .isEmpty());
    }

    @Test
    public void cardReferenceRoundTripsAndMalformedReferenceIsIgnored() {
        Station original = station(0);
        assertEquals(original, Station.read(original.write()));
        assertEquals(null, Station.read(new NBTTagCompound()));
        NBTTagCompound broken = original.write();
        broken.setString("UUID", "not-a-uuid");
        assertEquals(null, Station.read(broken));
    }

    @Test
    public void markerClicksDisconnectPersistentlyAndCanReconnect() {
        WirelessLinkData links = new WirelessLinkData();
        Position target = new Position(0, 10, 64, 0);
        Station source = station(0);
        assertTrue(links.toggleBinding(target, source));
        assertTrue(links.isBound(target, source));
        links.setDirty(false);
        long revision = links.revision();
        assertFalse(links.toggleBinding(target, source));
        assertFalse(links.isBound(target, source));
        assertEquals(revision + 1, links.revision());
        assertTrue(links.isDirty());
        NBTTagCompound saved = new NBTTagCompound();
        links.writeToNBT(saved);
        WirelessLinkData loaded = new WirelessLinkData();
        loaded.readFromNBT(saved);
        assertFalse(loaded.isBound(target, source));
        assertTrue(
            loaded.targets(source)
                .isEmpty());
        assertTrue(loaded.toggleBinding(target, source));
        assertTrue(loaded.isBound(target, source));
        assertEquals(
            1,
            loaded.targets(source)
                .size());
    }

    @Test
    public void selectingAnotherStationRebindsBeforeNextClickDisconnects() {
        WirelessLinkData links = new WirelessLinkData();
        Position target = new Position(0, 10, 64, 0);
        Station first = station(0);
        Station second = station(1);
        assertTrue(links.toggleBinding(target, first));
        assertTrue(links.toggleBinding(target, second));
        assertFalse(links.isBound(target, first));
        assertTrue(
            links.targets(first)
                .isEmpty());
        assertTrue(links.isBound(target, second));
        assertEquals(
            1,
            links.targets(second)
                .size());
        assertFalse(links.toggleBinding(target, second));
        assertTrue(
            links.targets(second)
                .isEmpty());
    }

    @Test
    public void cleanupPersistsMissingTargetRemovalAndRetainsUnloadedTargetsAndOtherStations() {
        WirelessLinkData links = new WirelessLinkData();
        Station source = station(0);
        Station other = station(1);
        Position removedMachine = new Position(0, 10, 64, 0);
        Position existingMachine = new Position(0, 11, 64, 0);
        Position unloadedMachine = new Position(0, 32, 64, 0);
        Position otherStationTarget = new Position(0, 12, 64, 0);
        links.bind(removedMachine, source);
        links.bind(existingMachine, source);
        links.bind(unloadedMachine, source);
        links.setMachineId(unloadedMachine, source, 117);
        links.bind(otherStationTarget, other);
        Set<Position> loaded = new HashSet<>(Arrays.asList(removedMachine, existingMachine, otherStationTarget));
        Set<Position> existing = new HashSet<>(Arrays.asList(existingMachine));
        long revision = links.revision();
        links.setDirty(false);
        assertEquals(
            1,
            links.removeMissingTargets(source, position -> loaded.contains(position) && !existing.contains(position)));
        assertEquals(revision + 1, links.revision());
        assertTrue(links.isDirty());
        assertFalse(links.isBound(removedMachine, source));
        assertTrue(links.isBound(existingMachine, source));
        assertTrue(links.isBound(unloadedMachine, source));
        assertTrue(links.isBound(otherStationTarget, other));
        NBTTagCompound saved = new NBTTagCompound();
        links.writeToNBT(saved);
        WirelessLinkData restored = new WirelessLinkData();
        restored.readFromNBT(saved);
        assertFalse(restored.isBound(removedMachine, source));
        assertTrue(restored.isBound(unloadedMachine, source));
        assertEquals(117, restored.machineId(unloadedMachine, source));
        assertEquals(
            2,
            restored.targets(source)
                .size());
        revision = restored.revision();
        restored.setDirty(false);
        assertEquals(
            0,
            restored
                .removeMissingTargets(source, position -> loaded.contains(position) && !existing.contains(position)));
        assertEquals(revision, restored.revision());
        assertFalse(restored.isDirty());
        loaded.add(unloadedMachine);
        assertEquals(
            1,
            restored
                .removeMissingTargets(source, position -> loaded.contains(position) && !existing.contains(position)));
        assertFalse(restored.isBound(unloadedMachine, source));
        assertTrue(restored.isBound(existingMachine, source));
        assertTrue(restored.isBound(otherStationTarget, other));
    }

    @Test
    public void machineTypeSurvivesSavingAndIsResetOnRebindOrDisconnect() {
        WirelessLinkData links = new WirelessLinkData();
        Position target = new Position(0, 10, 64, 0);
        Station first = station(0);
        Station second = station(1);
        links.bind(target, first);
        assertEquals(-1, links.machineId(target, first));
        long revision = links.revision();
        links.setDirty(false);
        links.setMachineId(target, first, 117);
        assertEquals(117, links.machineId(target, first));
        assertEquals(revision, links.revision());
        assertTrue(links.isDirty());
        links.setDirty(false);
        links.setMachineId(target, first, 117);
        assertFalse(links.isDirty());
        links.setMachineId(target, second, 118);
        assertEquals(117, links.machineId(target, first));
        assertEquals(-1, links.machineId(target, second));
        NBTTagCompound saved = new NBTTagCompound();
        links.writeToNBT(saved);
        WirelessLinkData restored = new WirelessLinkData();
        restored.readFromNBT(saved);
        assertEquals(117, restored.machineId(target, first));
        assertTrue(restored.bind(target, second));
        assertEquals(-1, restored.machineId(target, second));
        restored.setMachineId(target, second, 119);
        assertTrue(restored.unbind(target, second));
        assertEquals(-1, restored.machineId(target, second));
        restored.bind(target, second);
        assertEquals(-1, restored.machineId(target, second));
    }

    @Test
    public void oldConnectionsWithoutMachineTypeRemainBoundUntilIdentified() {
        WirelessLinkData original = new WirelessLinkData();
        Position target = new Position(0, 10, 64, 0);
        Station source = station(0);
        original.bind(target, source);
        original.setMachineId(target, source, 117);
        NBTTagCompound saved = new NBTTagCompound();
        original.writeToNBT(saved);
        saved.getTagList("Links", 10)
            .getCompoundTagAt(0)
            .removeTag("MachineId");
        WirelessLinkData restored = new WirelessLinkData();
        restored.readFromNBT(saved);
        assertTrue(restored.isBound(target, source));
        assertEquals(-1, restored.machineId(target, source));
        restored.setMachineId(target, source, 118);
        assertEquals(118, restored.machineId(target, source));
        assertEquals(
            1,
            restored.targets(source)
                .size());
    }

    private static Station station(int x) {
        return new Station(new Position(0, x, 64, 0), UUID.randomUUID());
    }
}
