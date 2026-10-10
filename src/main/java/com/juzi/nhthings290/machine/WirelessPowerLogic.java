package com.juzi.nhthings290.machine;

public final class WirelessPowerLogic {

    public static final int AMPERAGE = 16;
    private static final int BUFFER_TICKS = 400;

    private WirelessPowerLogic() {}

    public static long voltage(int tier) {
        requireTier(tier);
        return 32L << (2 * (tier - 1));
    }

    public static int radius(int tier) {
        requireTier(tier);
        return 16 << (tier - 1);
    }

    public static long tickBudget(int tier) {
        return voltage(tier) * AMPERAGE;
    }

    public static long bufferCapacity(int tier) {
        return tickBudget(tier) * BUFFER_TICKS;
    }

    public static boolean canChargePlayers(int tier) {
        requireTier(tier);
        return tier >= 4;
    }

    public static boolean inRange(int x, int y, int z, int targetX, int targetY, int targetZ, int radius) {
        long dx = (long) targetX - x;
        long dy = (long) targetY - y;
        long dz = (long) targetZ - z;
        if (Math.abs(dx) > radius || Math.abs(dy) > radius || Math.abs(dz) > radius) return false;
        return dx * dx + dy * dy + dz * dz <= (long) radius * radius;
    }

    public static long targetAmperage(int tier, long targetVoltage, long budget) {
        if (targetVoltage <= 0 || targetVoltage > voltage(tier) || budget <= 0) return 0;
        return budget / targetVoltage;
    }

    public static int nextCursor(int cursor, int size) {
        return size <= 0 ? 0 : (Math.floorMod(cursor, size) + 1) % size;
    }

    private static void requireTier(int tier) {
        if (tier < 1 || tier > 5) throw new IllegalArgumentException("Wireless station tier must be LV through IV");
    }
}
