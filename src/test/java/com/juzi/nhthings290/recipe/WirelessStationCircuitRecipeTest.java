package com.juzi.nhthings290.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.oredict.OreDictionary;

import org.junit.Test;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.OreDictItemStack;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;

public class WirelessStationCircuitRecipeTest {

    @Test
    public void sameTierVariantsAndLateRegistrationsMatchWhileOtherTiersDoNot() throws Exception {
        // Forge requires LaunchClassLoader; keep its registry bootstrap separate from other tests.
        String[] entries = System.getProperty("java.class.path")
            .split(File.pathSeparator);
        URL[] urls = new URL[entries.length];
        for (int index = 0; index < entries.length; index++) urls[index] = new File(entries[index]).toURI()
            .toURL();
        PrintStream stdout = System.out;
        PrintStream stderr = System.err;
        Map<String, Object> blackboard = Launch.blackboard;
        try (LaunchClassLoader loader = new LaunchClassLoader(urls)) {
            Launch.blackboard = new HashMap<>();
            Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
            loader.addClassLoaderExclusion("org.junit.");
            loader.loadClass(getClass().getName() + "$RecipeCheck")
                .getMethod("run")
                .invoke(null);
        } finally {
            Launch.blackboard = blackboard;
            System.setOut(stdout);
            System.setErr(stderr);
        }
    }

    public static final class RecipeCheck {

        public static void run() throws Exception {
            Field side = FMLRelaunchLog.class.getDeclaredField("side");
            side.setAccessible(true);
            side.set(null, Side.SERVER);
            Loader.injectData(
                "10",
                "13",
                "4",
                "1614",
                "1.7.10",
                "9.05",
                new File(System.getProperty("java.io.tmpdir")),
                Collections.emptyList());
            Bootstrap.func_151354_b();
            Item circuit = new Item().setHasSubtypes(true);
            GameRegistry.registerItem(circuit, "nhthings290_wireless_recipe_test_circuit");
            Materials[] tiers = { Materials.LV, Materials.MV, Materials.HV, Materials.EV, Materials.IV };
            for (int index = 0; index < tiers.length; index++) {
                OreDictItemStack ingredient = RecipeWirelessStations.circuitIngredient(tiers[index], 2);
                int first = index * 4;
                int second = first + 1;
                int late = first + 2;
                int wrong = first + 3;
                OreDictionary.registerOre(ingredient.mOreName, new ItemStack(circuit, 1, first));
                OreDictionary.registerOre(ingredient.mOreName, new ItemStack(circuit, 1, second));
                OreDictionary.registerOre(
                    OrePrefixes.circuit.get(Materials.ULV)
                        .toString(),
                    new ItemStack(circuit, 1, wrong));
                GTRecipeBuilder recipe = GTRecipeBuilder.builder()
                    .itemInputs(ingredient);
                assertEquals(ingredient, recipe.getItemInputOreDict(0));
                assertEquals(2, recipe.getItemInputBasic(0).stackSize);
                GTRecipe.RecipeItemInput input = new GTRecipe.RecipeItemInput(
                    OreDictionary.getOreID(ingredient.mOreName),
                    recipe.getItemInputBasic(0),
                    ingredient.mAmount,
                    false);
                assertTrue(input.matchesRecipe(null, new ItemStack(circuit, 2, first)));
                assertTrue(input.matchesRecipe(null, new ItemStack(circuit, 2, second)));
                assertFalse(input.matchesRecipe(null, new ItemStack(circuit, 2, wrong)));
                OreDictionary.registerOre(ingredient.mOreName, new ItemStack(circuit, 1, late));
                assertTrue(input.matchesRecipe(null, new ItemStack(circuit, 2, late)));
            }
            OreDictItemStack cardCircuit = RecipeWirelessStations.circuitIngredient(Materials.LV, 1);
            assertEquals(
                OrePrefixes.circuit.get(Materials.LV)
                    .toString(),
                cardCircuit.mOreName);
            assertEquals(1, cardCircuit.mAmount);
        }
    }
}
