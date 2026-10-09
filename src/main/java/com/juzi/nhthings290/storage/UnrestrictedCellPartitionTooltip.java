package com.juzi.nhthings290.storage;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.util.Util;

import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.core.localization.GuiText;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client tooltip lines for Cell Workbench partitions, matching AE2 / AE2Things style.
 */
final class UnrestrictedCellPartitionTooltip {

    private UnrestrictedCellPartitionTooltip() {}

    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void appendItemPartition(ItemStack stack, UnrestrictedCellItem type, List lines) {
        if (stack == null || type == null) {
            return;
        }

        UpgradeFlags flags = readUpgrades(type.getUpgradesInventory(stack));
        String oreFilter = type.getOreFilter(stack);
        List<String> names = new ArrayList<String>();
        IInventory config = type.getConfigInventory(stack);
        for (int i = 0; i < config.getSizeInventory(); i++) {
            ItemStack slot = config.getStackInSlot(i);
            if (slot != null) {
                names.add(slot.getDisplayName());
            }
        }

        boolean orePartition = flags.oreFilter && oreFilter != null && !oreFilter.isEmpty();
        boolean itemPartition = !names.isEmpty();
        if (!orePartition && !itemPartition) {
            return;
        }

        if (orePartition) {
            lines.add(GuiText.PartitionedOre.getLocal() + " : " + oreFilter);
        } else {
            String mode = flags.inverter ? GuiText.Excluded.getLocal() : GuiText.Included.getLocal();
            String match = flags.fuzzy ? GuiText.Fuzzy.getLocal() : GuiText.Precise.getLocal();
            lines.add(GuiText.Partitioned.getLocal() + " - " + mode + " " + match);
            if (GuiScreen.isShiftKeyDown()) {
                lines.add(GuiText.Filter.getLocal() + ":");
                for (String name : names) {
                    lines.add("  " + name);
                }
            } else {
                lines.add(StatCollector.translateToLocal("nhthings290.cell.partition.shift"));
            }
        }

        if (flags.sticky) {
            lines.add(GuiText.Sticky.getLocal());
        }
    }

    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void appendFluidPartition(ItemStack stack, UnrestrictedFluidCellItem type, List lines) {
        if (stack == null || type == null) {
            return;
        }

        UpgradeFlags flags = readUpgrades(type.getUpgradesInventory(stack));
        List<String> names = new ArrayList<String>();
        IInventory config = type.getConfigInventory(stack);
        for (int i = 0; i < config.getSizeInventory(); i++) {
            ItemStack slot = config.getStackInSlot(i);
            FluidStack fluid = Util.getFluidFromItem(slot);
            if (fluid != null && fluid.getFluid() != null) {
                names.add(fluid.getLocalizedName());
            }
        }
        if (names.isEmpty()) {
            return;
        }

        String mode = flags.inverter ? GuiText.Excluded.getLocal() : GuiText.Included.getLocal();
        lines.add(GuiText.Partitioned.getLocal() + " - " + mode + " " + GuiText.Precise.getLocal());
        if (GuiScreen.isShiftKeyDown()) {
            lines.add(GuiText.Filter.getLocal() + ":");
            for (String name : names) {
                lines.add("  " + name);
            }
        } else {
            lines.add(StatCollector.translateToLocal("nhthings290.cell.partition.shift"));
        }
        if (flags.sticky) {
            lines.add(GuiText.Sticky.getLocal());
        }
    }

    private static UpgradeFlags readUpgrades(IInventory upgrades) {
        UpgradeFlags flags = new UpgradeFlags();
        if (upgrades == null) {
            return flags;
        }
        for (int x = 0; x < upgrades.getSizeInventory(); x++) {
            ItemStack is = upgrades.getStackInSlot(x);
            if (is == null || !(is.getItem() instanceof IUpgradeModule)) {
                continue;
            }
            Upgrades u = ((IUpgradeModule) is.getItem()).getType(is);
            if (u == null) {
                continue;
            }
            switch (u) {
                case FUZZY:
                    flags.fuzzy = true;
                    break;
                case INVERTER:
                    flags.inverter = true;
                    break;
                case ORE_FILTER:
                    flags.oreFilter = true;
                    break;
                case STICKY:
                    flags.sticky = true;
                    break;
                default:
                    break;
            }
        }
        return flags;
    }

    private static final class UpgradeFlags {

        boolean fuzzy;
        boolean inverter;
        boolean oreFilter;
        boolean sticky;
    }
}
