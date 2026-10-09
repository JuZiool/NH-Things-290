package com.juzi.nhthings290.storage;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UnrestrictedFluidCellStorageLogicTest {

    @Test
    public void convertsCapacityBytesToFluidAmount() {
        // 4096 mB per byte
        assertEquals(4194304L, UnrestrictedFluidCellStorageLogic.capacityForBytes(1024));
        assertEquals(16777216L, UnrestrictedFluidCellStorageLogic.capacityForBytes(4096));
        assertEquals(67108864L, UnrestrictedFluidCellStorageLogic.capacityForBytes(16384));
        assertEquals(268435456L, UnrestrictedFluidCellStorageLogic.capacityForBytes(65536));
    }

    @Test
    public void roundsUsedBytesUpLikeTheItemCell() {
        assertEquals(0L, UnrestrictedFluidCellStorageLogic.usedBytes(0));
        assertEquals(1L, UnrestrictedFluidCellStorageLogic.usedBytes(1));
        assertEquals(1L, UnrestrictedFluidCellStorageLogic.usedBytes(4096));
        assertEquals(2L, UnrestrictedFluidCellStorageLogic.usedBytes(4097));
    }

    @Test
    public void computesRemainingFluidFromTheExactAmountLedger() {
        assertEquals(4194304L, UnrestrictedFluidCellStorageLogic.remainingAmount(1024, 0));
        assertEquals(4194303L, UnrestrictedFluidCellStorageLogic.remainingAmount(1024, 1));
        assertEquals(0L, UnrestrictedFluidCellStorageLogic.remainingAmount(1024, 4194304L));
        assertEquals(0L, UnrestrictedFluidCellStorageLogic.remainingAmount(1024, 4194305L));
    }
}
