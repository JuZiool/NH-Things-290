package com.juzi.nhthings290.registry;

import com.juzi.nhthings290.recipe.RecipeFlightCharm;
import com.juzi.nhthings290.recipe.RecipeUnrestrictedFluidCells;
import com.juzi.nhthings290.recipe.RecipeUnrestrictedItemCells;
import com.juzi.nhthings290.recipe.RecipeUnrestrictedShell;
import com.juzi.nhthings290.recipe.RecipeWirelessStations;

public final class ModRecipes {

    private ModRecipes() {}

    public static void register() {
        RecipeWirelessStations.register();
        RecipeFlightCharm.register();
        RecipeUnrestrictedShell.register();
        RecipeUnrestrictedItemCells.register();
        RecipeUnrestrictedFluidCells.register();
    }
}
