package com.juzi.nhthings290.recipe;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.juzi.nhthings290.registry.ModItems;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class RecipeUnrestrictedShell {

    private RecipeUnrestrictedShell() {}

    public static void register() {
        GTRecipeBuilder.builder()
            .itemInputs(
                require("glass pane", new ItemStack(Blocks.glass_pane, UnrestrictedShellRecipeSpec.GLASS_PANES)),
                require(
                    "Certus Quartz plate",
                    GTOreDictUnificator.get(
                        OrePrefixes.plate,
                        Materials.CertusQuartz,
                        UnrestrictedShellRecipeSpec.CERTUS_QUARTZ_PLATES)),
                require(
                    "Titanium plate",
                    GTOreDictUnificator
                        .get(OrePrefixes.plate, Materials.Titanium, UnrestrictedShellRecipeSpec.TITANIUM_PLATES)),
                require(
                    "Stainless Steel plates",
                    GTOreDictUnificator.get(
                        OrePrefixes.plate,
                        Materials.StainlessSteel,
                        UnrestrictedShellRecipeSpec.STAINLESS_STEEL_PLATES)))
            .itemOutputs(require("unrestricted shell", new ItemStack(ModItems.unrestrictedShell)))
            .duration(UnrestrictedShellRecipeSpec.DURATION_TICKS)
            .eut(UnrestrictedShellRecipeSpec.EUT)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static ItemStack require(String ingredient, ItemStack stack) {
        if (GTUtility.isStackInvalid(stack)) {
            throw new IllegalStateException("Missing unrestricted shell ingredient: " + ingredient);
        }
        return stack;
    }
}
