package com.juzi.nhthings290;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.juzi.nhthings290.registry.ModItems;
import com.juzi.nhthings290.registry.ModRecipes;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(
    modid = NHThings290.MOD_ID,
    name = NHThings290.MOD_NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:gregtech;required-after:appliedenergistics2;required-after:ae2fc;"
        + "required-after:Baubles|Expanded;required-after:Thaumcraft;required-after:ThaumicExploration")
public final class NHThings290 {

    public static final String MOD_ID = "nhthings290";
    public static final String MOD_NAME = "NH-Things-290";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(MOD_ID) {

        @Override
        @SideOnly(Side.CLIENT)
        public Item getTabIconItem() {
            return ModItems.unrestrictedShell == null ? Items.redstone : ModItems.unrestrictedShell;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public String getTranslatedTabLabel() {
            return MOD_NAME;
        }
    };

    @Mod.Instance(MOD_ID)
    public static NHThings290 instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModItems.register();
        LOG.info("Loading {} {}", MOD_NAME, Tags.VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModItems.registerCellHandler();
        ModRecipes.register();
        LOG.info("{} initialized for GTNH 2.9.0-beta-3", MOD_NAME);
    }
}
