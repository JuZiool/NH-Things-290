package com.juzi.nhthings290.registry;

import com.juzi.nhthings290.recipe.RecipeUnrestrictedFluidCells;
import com.juzi.nhthings290.recipe.RecipeUnrestrictedItemCells;
import com.juzi.nhthings290.recipe.RecipeUnrestrictedShell;

public final class ModRecipes {

    private ModRecipes() {}

    public static void register() {
        RecipeUnrestrictedShell.register();
        RecipeUnrestrictedItemCells.register();
        RecipeUnrestrictedFluidCells.register();
    }
}
