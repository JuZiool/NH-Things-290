package com.juzi.nhthings290.item;

final class FlightCharmLogic {

    private FlightCharmLogic() {}

    static boolean shouldGrantFlight(boolean allowFlying, boolean creative, boolean hasEnoughFood) {
        return !allowFlying && !creative && hasEnoughFood;
    }

    static boolean shouldRevokeFlight(boolean ownsFlight, boolean creative) {
        return ownsFlight && !creative;
    }

    static boolean shouldCountFlight(boolean ownsFlight, boolean isFlying, boolean creative, boolean hasEnoughFood) {
        return ownsFlight && isFlying && !creative && hasEnoughFood;
    }

    static boolean shouldChargeOnNextTick(int timer, int interval) {
        return timer + 1 >= interval;
    }

    static int nextTimer(int timer, int interval) {
        return shouldChargeOnNextTick(timer, interval) ? 0 : timer + 1;
    }
}
