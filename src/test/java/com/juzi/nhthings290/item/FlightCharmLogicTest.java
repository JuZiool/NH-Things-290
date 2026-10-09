package com.juzi.nhthings290.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FlightCharmLogicTest {

    @Test
    public void grantsFlightOnlyWhenSurvivalPlayerNeedsItAndHasEnoughFood() {
        assertTrue(FlightCharmLogic.shouldGrantFlight(false, false, true));
        assertFalse(FlightCharmLogic.shouldGrantFlight(true, false, true));
        assertFalse(FlightCharmLogic.shouldGrantFlight(false, true, true));
        assertFalse(FlightCharmLogic.shouldGrantFlight(false, false, false));
    }

    @Test
    public void revokesOnlyFlightOwnedByTheCharm() {
        assertTrue(FlightCharmLogic.shouldRevokeFlight(true, false));
        assertFalse(FlightCharmLogic.shouldRevokeFlight(false, false));
        assertFalse(FlightCharmLogic.shouldRevokeFlight(true, true));
    }

    @Test
    public void chargesOnlyForOwnedSurvivalFlight() {
        assertTrue(FlightCharmLogic.shouldCountFlight(true, true, false, true));
        assertFalse(FlightCharmLogic.shouldCountFlight(false, true, false, true));
        assertFalse(FlightCharmLogic.shouldCountFlight(true, false, false, true));
        assertFalse(FlightCharmLogic.shouldCountFlight(true, true, true, true));
        assertFalse(FlightCharmLogic.shouldCountFlight(true, true, false, false));
    }

    @Test
    public void chargesAndResetsOnTheSixHundredthEligibleTick() {
        assertFalse(FlightCharmLogic.shouldChargeOnNextTick(598, 600));
        assertTrue(FlightCharmLogic.shouldChargeOnNextTick(599, 600));
        assertEquals(599, FlightCharmLogic.nextTimer(598, 600));
        assertEquals(0, FlightCharmLogic.nextTimer(599, 600));
    }
}
