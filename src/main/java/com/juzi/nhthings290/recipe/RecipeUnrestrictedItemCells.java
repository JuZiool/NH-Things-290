package com.juzi.nhthings290.recipe;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.juzi.nhthings290.registry.ModItems;
import com.juzi.nhthings290.storage.UnrestrictedCellComponents;
import com.juzi.nhthings290.storage.UnrestrictedCellItem;

import cpw.mods.fml.common.registry.GameRegistry;

public final class RecipeUnrestrictedItemCells {

    private RecipeUnrestrictedItemCells() {}

    public static void register() {
        addRecipe(ModItems.itemCell1k, UnrestrictedCellComponents.forCapacity(1024));
        addRecipe(ModItems.itemCell4k, UnrestrictedCellComponents.forCapacity(4096));
        addRecipe(ModItems.itemCell16k, UnrestrictedCellComponents.forCapacity(16384));
        addRecipe(ModItems.itemCell64k, UnrestrictedCellComponents.forCapacity(65536));
        addRecipe(ModItems.itemCell256k, UnrestrictedCellComponents.forCapacity(262144));
        addRecipe(ModItems.itemCell1024k, UnrestrictedCellComponents.forCapacity(1048576));
        addRecipe(ModItems.itemCell4096k, UnrestrictedCellComponents.forCapacity(4194304));
        addRecipe(ModItems.itemCell16384k, UnrestrictedCellComponents.forCapacity(16777216));
    }

    private static void addRecipe(UnrestrictedCellItem outputItem, ItemStack component) {
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
