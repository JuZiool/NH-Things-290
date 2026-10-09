package com.juzi.nhthings290;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = NHThings290.MOD_ID,
    name = NHThings290.MOD_NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:appliedenergistics2;required-after:ae2fc")
public final class NHThings290 {

    public static final String MOD_ID = "nhthings290";
    public static final String MOD_NAME = "NH-Things-290";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    @Mod.Instance(MOD_ID)
    public static NHThings290 instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("Loading {} {}", MOD_NAME, Tags.VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOG.info("{} initialized for GTNH 2.9.0-beta-3", MOD_NAME);
    }
}
