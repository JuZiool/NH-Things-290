package com.juzi.nhthings290.machine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;

import org.junit.Test;

public class WirelessChargingStationTextureTest {

    @Test
    public void frontTextureInitializesAndPointsToPackagedResource() throws Exception {
        Field front = WirelessChargingStation.class.getDeclaredField("FRONT");
        front.setAccessible(true);
        Object icon = front.get(null);
        Field resource = icon.getClass()
            .getDeclaredField("iconResource");
        resource.setAccessible(true);
        assertEquals(
            "nhthings290:textures/blocks/machine/overlay_charging_station.png",
            resource.get(icon)
                .toString());
        assertNotNull(
            getClass().getResource("/assets/nhthings290/textures/blocks/machine/overlay_charging_station.png"));
    }
}
