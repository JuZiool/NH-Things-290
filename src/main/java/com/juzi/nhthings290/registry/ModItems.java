package com.juzi.nhthings290.registry;

import com.juzi.nhthings290.item.ItemUnrestrictedShell;
import com.juzi.nhthings290.storage.UnrestrictedCellHandler;
import com.juzi.nhthings290.storage.UnrestrictedCellItem;
import com.juzi.nhthings290.storage.UnrestrictedFluidCellItem;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;

public final class ModItems {

    public static ItemUnrestrictedShell unrestrictedShell;

    public static UnrestrictedCellItem itemCell1k;
    public static UnrestrictedCellItem itemCell4k;
    public static UnrestrictedCellItem itemCell16k;
    public static UnrestrictedCellItem itemCell64k;
    public static UnrestrictedCellItem itemCell256k;
    public static UnrestrictedCellItem itemCell1024k;
    public static UnrestrictedCellItem itemCell4096k;
    public static UnrestrictedCellItem itemCell16384k;

    public static UnrestrictedFluidCellItem fluidCell1k;
    public static UnrestrictedFluidCellItem fluidCell4k;
    public static UnrestrictedFluidCellItem fluidCell16k;
    public static UnrestrictedFluidCellItem fluidCell64k;
    public static UnrestrictedFluidCellItem fluidCell256k;
    public static UnrestrictedFluidCellItem fluidCell1024k;
    public static UnrestrictedFluidCellItem fluidCell4096k;
    public static UnrestrictedFluidCellItem fluidCell16384k;

    private ModItems() {}

    public static void register() {
        unrestrictedShell = new ItemUnrestrictedShell();
        GameRegistry.registerItem(unrestrictedShell, "unrestricted_shell");

        itemCell1k = UnrestrictedCellItem.create("unrestricted_item_cell_1k", 1024);
        itemCell4k = UnrestrictedCellItem.create("unrestricted_item_cell_4k", 4096);
        itemCell16k = UnrestrictedCellItem.create("unrestricted_item_cell_16k", 16384);
        itemCell64k = UnrestrictedCellItem.create("unrestricted_item_cell_64k", 65536);
        itemCell256k = UnrestrictedCellItem.create("unrestricted_item_cell_256k", 262144);
        itemCell1024k = UnrestrictedCellItem.create("unrestricted_item_cell_1024k", 1048576);
        itemCell4096k = UnrestrictedCellItem.create("unrestricted_item_cell_4096k", 4194304);
        itemCell16384k = UnrestrictedCellItem.create("unrestricted_item_cell_16384k", 16777216);

        fluidCell1k = UnrestrictedFluidCellItem.create("unrestricted_fluid_cell_1k", 1024);
        fluidCell4k = UnrestrictedFluidCellItem.create("unrestricted_fluid_cell_4k", 4096);
        fluidCell16k = UnrestrictedFluidCellItem.create("unrestricted_fluid_cell_16k", 16384);
        fluidCell64k = UnrestrictedFluidCellItem.create("unrestricted_fluid_cell_64k", 65536);
        fluidCell256k = UnrestrictedFluidCellItem.create("unrestricted_fluid_cell_256k", 262144);
        fluidCell1024k = UnrestrictedFluidCellItem.create("unrestricted_fluid_cell_1024k", 1048576);
        fluidCell4096k = UnrestrictedFluidCellItem.create("unrestricted_fluid_cell_4096k", 4194304);
        fluidCell16384k = UnrestrictedFluidCellItem.create("unrestricted_fluid_cell_16384k", 16777216);

    }

    public static void registerCellHandler() {
        AEApi.instance()
            .registries()
            .cell()
            .addCellHandler(new UnrestrictedCellHandler());
    }
}
