package com.juzi.nhthings290.storage;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UnrestrictedFluidCellStorageLogicTest {

    @Test
    public void convertsCapacityBytesToFluidAmount() {
        // 8192 mB per byte
        assertEquals(8388608L, UnrestrictedFluidCellStorageLogic.capacityForBytes(1024));
        assertEquals(33554432L, UnrestrictedFluidCellStorageLogic.capacityForBytes(4096));
        assertEquals(134217728L, UnrestrictedFluidCellStorageLogic.capacityForBytes(16384));
        assertEquals(536870912L, UnrestrictedFluidCellStorageLogic.capacityForBytes(65536));
    }

    @Test
    public void roundsUsedBytesUpLikeTheItemCell() {
        assertEquals(0L, UnrestrictedFluidCellStorageLogic.usedBytes(0));
        assertEquals(1L, UnrestrictedFluidCellStorageLogic.usedBytes(1));
        assertEquals(1L, UnrestrictedFluidCellStorageLogic.usedBytes(8192));
        assertEquals(2L, UnrestrictedFluidCellStorageLogic.usedBytes(8193));
    }

    @Test
    public void computesRemainingFluidFromTheExactAmountLedger() {
        assertEquals(8388608L, UnrestrictedFluidCellStorageLogic.remainingAmount(1024, 0));
        assertEquals(8388607L, UnrestrictedFluidCellStorageLogic.remainingAmount(1024, 1));
        assertEquals(0L, UnrestrictedFluidCellStorageLogic.remainingAmount(1024, 8388608L));
        assertEquals(0L, UnrestrictedFluidCellStorageLogic.remainingAmount(1024, 8388609L));
    }
}
