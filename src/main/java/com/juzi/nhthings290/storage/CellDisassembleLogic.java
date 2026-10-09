package com.juzi.nhthings290.storage;

/**
 * Pure disassembly policy for unrestricted cells.
 * World storage resolution is supplied by the caller so unit tests stay free of Minecraft.
 */
public final class CellDisassembleLogic {

    private CellDisassembleLogic() {}

    /**
     * Fail-closed: NBT stats must show empty, and live storage must resolve with count == 0.
     * storageResolved=false means world/mapStorage was unavailable.
     */
    public static boolean canDisassemble(long nbtAmount, long nbtBytes, long nbtTypes, boolean storageResolved,
        long storageAmount) {
        if (nbtAmount > 0L || nbtBytes > 0L || nbtTypes > 0L) {
            return false;
        }
        return storageResolved && storageAmount <= 0L;
    }
}
