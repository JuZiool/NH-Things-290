package com.juzi.nhthings290.storage;

/** Capacity arithmetic shared by unrestricted fluid cells and their tests. */
public final class UnrestrictedFluidCellStorageLogic {

    public static final long AMOUNT_PER_BYTE = 8192L;

    private UnrestrictedFluidCellStorageLogic() {}

    public static long capacityForBytes(long bytes) {
        if (bytes <= 0) return 0;
        if (bytes > Long.MAX_VALUE / AMOUNT_PER_BYTE) return Long.MAX_VALUE;
        return bytes * AMOUNT_PER_BYTE;
    }

    public static long usedBytes(long storedAmount) {
        if (storedAmount <= 0) return 0;
        long bytes = storedAmount / AMOUNT_PER_BYTE;
        return storedAmount % AMOUNT_PER_BYTE == 0 ? bytes : bytes + 1;
    }

    public static long remainingAmount(long totalBytes, long storedAmount) {
        long capacity = capacityForBytes(totalBytes);
        if (storedAmount >= capacity) return 0;
        return capacity - Math.max(0, storedAmount);
    }
}
