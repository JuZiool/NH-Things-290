package com.juzi.nhthings290.registry;

import net.minecraft.item.ItemStack;

import com.juzi.nhthings290.machine.WirelessChargingStation;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;

public final class ModMachines {

    public static final int FIRST_WIRELESS_STATION_ID = 31990;
    public static final ItemStack[] wirelessStations = new ItemStack[5];

    private ModMachines() {}

    public static void register() {
        for (int tier = 1; tier <= 5; tier++) {
            int id = FIRST_WIRELESS_STATION_ID + tier - 1;
            if (GregTechAPI.METATILEENTITIES[id] != null) {
                throw new IllegalStateException("Wireless station MetaTileEntity ID already occupied: " + id);
            }
            WirelessChargingStation station = new WirelessChargingStation(
                id,
                "nhthings290.wireless_station_" + GTValues.VN[tier].toLowerCase(java.util.Locale.ROOT),
                GTValues.VN[tier] + " Wireless Charging Station",
                tier);
            wirelessStations[tier - 1] = station.getStackForm(1);
        }
    }
}
