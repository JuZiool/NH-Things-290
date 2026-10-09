package com.juzi.nhthings290.recipe;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.juzi.nhthings290.registry.ModItems;
import com.juzi.nhthings290.storage.UnrestrictedFluidCellComponents;
import com.juzi.nhthings290.storage.UnrestrictedFluidCellItem;

import cpw.mods.fml.common.registry.GameRegistry;

public final class RecipeUnrestrictedFluidCells {

    private RecipeUnrestrictedFluidCells() {}

    public static void register() {
        addRecipeIfAvailable(ModItems.fluidCell1k, UnrestrictedFluidCellComponents.forCapacity(1024));
        addRecipeIfAvailable(ModItems.fluidCell4k, UnrestrictedFluidCellComponents.forCapacity(4096));
        addRecipeIfAvailable(ModItems.fluidCell16k, UnrestrictedFluidCellComponents.forCapacity(16384));
        addRecipeIfAvailable(ModItems.fluidCell64k, UnrestrictedFluidCellComponents.forCapacity(65536));
        addRecipeIfAvailable(ModItems.fluidCell256k, UnrestrictedFluidCellComponents.forCapacity(262144));
        addRecipeIfAvailable(ModItems.fluidCell1024k, UnrestrictedFluidCellComponents.forCapacity(1048576));
        addRecipeIfAvailable(ModItems.fluidCell4096k, UnrestrictedFluidCellComponents.forCapacity(4194304));
        addRecipeIfAvailable(ModItems.fluidCell16384k, UnrestrictedFluidCellComponents.forCapacity(16777216));
    }

    private static void addRecipeIfAvailable(UnrestrictedFluidCellItem outputItem, ItemStack component) {
        if (component != null && component.getItem() != null) addRecipe(outputItem, component);
    }

    private static void addRecipe(UnrestrictedFluidCellItem outputItem, ItemStack component) {
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(outputItem),
                "SC",
                'S',
                new ItemStack(ModItems.unrestrictedShell),
                'C',
                component));
    }
}
