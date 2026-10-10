package com.juzi.nhthings290.recipe;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.juzi.nhthings290.registry.ModItems;
import com.juzi.nhthings290.registry.ModMachines;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.OreDictItemStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class RecipeWirelessStations {

    private RecipeWirelessStations() {}

    public static void register() {
        ItemList[] hulls = { ItemList.Hull_LV, ItemList.Hull_MV, ItemList.Hull_HV, ItemList.Hull_EV, ItemList.Hull_IV };
        ItemList[] emitters = { ItemList.Emitter_LV, ItemList.Emitter_MV, ItemList.Emitter_HV, ItemList.Emitter_EV,
            ItemList.Emitter_IV };
        ItemList[] sensors = { ItemList.Sensor_LV, ItemList.Sensor_MV, ItemList.Sensor_HV, ItemList.Sensor_EV,
            ItemList.Sensor_IV };
        Materials[] circuits = { Materials.LV, Materials.MV, Materials.HV, Materials.EV, Materials.IV };
        for (int index = 0; index < hulls.length; index++) {
            GTRecipeBuilder.builder()
                .itemInputs(
                    require("machine hull", hulls[index].get(1)),
                    require("emitters", emitters[index].get(4)),
                    require("sensors", sensors[index].get(2)),
                    circuitIngredient(circuits[index], 2))
                .itemOutputs(require("wireless station", ModMachines.wirelessStations[index].copy()))
                .duration(30 * GTRecipeBuilder.SECONDS)
                .eut(GTValues.VP[index + 1])
                .addTo(RecipeMaps.assemblerRecipes);
        }
        GTRecipeBuilder.builder()
            .itemInputs(
                new ItemStack(Items.paper),
                require("rubber plate", GTOreDictUnificator.get(OrePrefixes.plate, Materials.Rubber, 1)),
                require("copper foils", GTOreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 2)),
                circuitIngredient(Materials.LV, 1))
            .itemOutputs(new ItemStack(ModItems.wirelessSupplyCard))
            .duration(5 * GTRecipeBuilder.SECONDS)
            .eut(GTValues.VP[1])
            .addTo(RecipeMaps.assemblerRecipes);
    }

    static OreDictItemStack circuitIngredient(Materials tier, int amount) {
        return new OreDictItemStack(
            OrePrefixes.circuit.get(tier)
                .toString(),
            amount);
    }

    private static ItemStack require(String name, ItemStack stack) {
        if (GTUtility.isStackInvalid(stack))
            throw new IllegalStateException("Missing wireless station ingredient: " + name);
        return stack;
    }
}
