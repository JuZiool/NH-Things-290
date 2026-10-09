package com.juzi.nhthings290.recipe;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.juzi.nhthings290.storage.UnrestrictedFluidCellComponents;

public class UnrestrictedFluidCellRecipeSpecTest {

    @Test
    public void mapsAllFluidCellTiersToAe2FluidParts() {
        assertEquals(0, UnrestrictedFluidCellComponents.damageForCapacity(1024));
        assertEquals(1, UnrestrictedFluidCellComponents.damageForCapacity(4096));
        assertEquals(2, UnrestrictedFluidCellComponents.damageForCapacity(16384));
        assertEquals(3, UnrestrictedFluidCellComponents.damageForCapacity(65536));
        assertEquals(4, UnrestrictedFluidCellComponents.damageForCapacity(262144));
        assertEquals(5, UnrestrictedFluidCellComponents.damageForCapacity(1048576));
        assertEquals(6, UnrestrictedFluidCellComponents.damageForCapacity(4194304));
        assertEquals(7, UnrestrictedFluidCellComponents.damageForCapacity(16777216));
    }
}
