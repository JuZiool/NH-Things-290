package com.juzi.nhthings290.recipe;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.juzi.nhthings290.registry.ModItems;

import cpw.mods.fml.common.registry.GameRegistry;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

public final class RecipeFlightCharm {

    private RecipeFlightCharm() {}

    public static void register() {
        AspectList aspects = new AspectList().add(Aspect.AIR, 50)
            .add(Aspect.EARTH, 50)
            .add(Aspect.FIRE, 50)
            .add(Aspect.WATER, 50)
            .add(Aspect.ORDER, 50)
            .add(Aspect.ENTROPY, 50);

        ThaumcraftApi.addArcaneCraftingRecipe(
            "",
            new ItemStack(ModItems.flightCharm),
            aspects,
            "BAB",
            "CDC",
            "EFE",
            'A',
            requireItem("Thaumcraft", "ItemResource", 0),
            'B',
            requireItem("Thaumcraft", "ItemResource", 1),
            'C',
            requireItem("Thaumcraft", "ItemResource", 7),
            'D',
            requireItem("ThaumicExploration", "discountRing", 4),
            'E',
            requireBlock("Thaumcraft", "blockCrystal", 0),
            'F',
            requireItem("Thaumcraft", "ItemEldritchObject", 0));
    }

    private static ItemStack requireItem(String modId, String itemName, int metadata) {
        Item item = GameRegistry.findItem(modId, itemName);
        if (item == null) {
            throw new IllegalStateException("Missing flight charm ingredient: " + modId + ':' + itemName);
        }
        return new ItemStack(item, 1, metadata);
    }

    private static ItemStack requireBlock(String modId, String blockName, int metadata) {
        Block block = GameRegistry.findBlock(modId, blockName);
        if (block == null) {
            throw new IllegalStateException("Missing flight charm ingredient: " + modId + ':' + blockName);
        }
        return new ItemStack(block, 1, metadata);
    }
}
