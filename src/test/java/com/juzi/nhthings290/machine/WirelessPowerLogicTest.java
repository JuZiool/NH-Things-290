package com.juzi.nhthings290.machine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WirelessPowerLogicTest {

    @Test
    public void progressionControlsRangePowerAndPlayerUnlock() {
        long[] budgets = { 512, 2048, 8192, 32768, 131072 };
        for (int tier = 1; tier <= 5; tier++) {
            assertEquals(16 << (tier - 1), WirelessPowerLogic.radius(tier));
            assertEquals(budgets[tier - 1], WirelessPowerLogic.tickBudget(tier));
            assertEquals(budgets[tier - 1] * 400, WirelessPowerLogic.bufferCapacity(tier));
            assertEquals(tier >= 4, WirelessPowerLogic.canChargePlayers(tier));
        }
    }

    @Test
    public void downconversionUsesOneSharedBudgetAndRejectsHigherVoltage() {
        long remaining = WirelessPowerLogic.tickBudget(4);
        long firstAmperes = WirelessPowerLogic.targetAmperage(4, 512, remaining);
        assertEquals(64, firstAmperes);
        remaining -= 8 * 512;
        assertEquals(56, WirelessPowerLogic.targetAmperage(4, 512, remaining));
        assertEquals(14, WirelessPowerLogic.targetAmperage(4, 2048, remaining));
        assertEquals(0, WirelessPowerLogic.targetAmperage(4, 8192, remaining));
        assertEquals(0, WirelessPowerLogic.targetAmperage(4, 0, remaining));
        assertEquals(0, WirelessPowerLogic.targetAmperage(4, 32, 31));
    }

    @Test
    public void rangeIsAnInclusiveSphereAndDoesNotOverflowOnLargeCoordinates() {
        assertTrue(WirelessPowerLogic.inRange(0, 0, 0, 16, 0, 0, 16));
        assertFalse(WirelessPowerLogic.inRange(0, 0, 0, 16, 1, 0, 16));
        assertFalse(WirelessPowerLogic.inRange(0, 0, 0, 12, 12, 0, 16));
        assertTrue(WirelessPowerLogic.inRange(30000000, 64, -30000000, 30000001, 64, -30000000, 16));
        assertFalse(WirelessPowerLogic.inRange(Integer.MIN_VALUE, 0, 0, Integer.MAX_VALUE, 0, 0, 256));
    }

    @Test
    public void firstTargetRotatesInsteadOfStarvingTheEndOfTheList() {
        int cursor = 0;
        for (int round = 0; round < 4; round++) {
            assertEquals(round % 3, cursor);
            cursor = WirelessPowerLogic.nextCursor(cursor, 3);
        }
        assertEquals(0, WirelessPowerLogic.nextCursor(cursor, 0));
        assertEquals(0, WirelessPowerLogic.nextCursor(100, 1));
    }
}
