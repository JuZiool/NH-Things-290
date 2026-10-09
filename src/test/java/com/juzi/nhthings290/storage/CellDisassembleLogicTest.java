package com.juzi.nhthings290.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CellDisassembleLogicTest {

    @Test
    public void refusesWhenNbtReportsAnyUsage() {
        assertFalse(CellDisassembleLogic.canDisassemble(1, 0, 0, true, 0));
        assertFalse(CellDisassembleLogic.canDisassemble(0, 1, 0, true, 0));
        assertFalse(CellDisassembleLogic.canDisassemble(0, 0, 1, true, 0));
    }

    @Test
    public void refusesWhenWorldStorageMissing() {
        assertFalse(CellDisassembleLogic.canDisassemble(0, 0, 0, false, 0));
    }

    @Test
    public void refusesWhenWorldStorageStillHasItems() {
        assertFalse(CellDisassembleLogic.canDisassemble(0, 0, 0, true, 5));
    }

    @Test
    public void allowsOnlyWhenBothNbtAndStorageConfirmEmpty() {
        assertTrue(CellDisassembleLogic.canDisassemble(0, 0, 0, true, 0));
    }
}
