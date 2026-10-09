package com.juzi.nhthings290.storage;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Drive indicator status codes used by unrestricted cells:
 * 1 empty, 2 has room for more, 4 full.
 */
public class UnrestrictedCellStatusLogicTest {

    @Test
    public void nbtFallbackStatusFromUsedBytes() {
        assertEquals(1, statusFromUsedBytes(0, 1024));
        assertEquals(2, statusFromUsedBytes(1, 1024));
        assertEquals(2, statusFromUsedBytes(1023, 1024));
        assertEquals(4, statusFromUsedBytes(1024, 1024));
        assertEquals(4, statusFromUsedBytes(2000, 1024));
    }

    /** Mirrors UnrestrictedCellHandler.statusFromStack byte comparison. */
    private static int statusFromUsedBytes(long usedBytes, long totalBytes) {
        if (usedBytes <= 0L) {
            return 1;
        }
        return usedBytes < totalBytes ? 2 : 4;
    }
}
