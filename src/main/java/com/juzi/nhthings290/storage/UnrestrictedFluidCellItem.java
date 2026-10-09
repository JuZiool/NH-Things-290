package com.juzi.nhthings290.storage;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.juzi.nhthings290.NHThings290;

import appeng.api.config.FuzzyMode;
import appeng.api.exceptions.AppEngException;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.util.item.AEFluidStackType;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Fluid cell item for 2.9 Stack Type API.
 * Implements AE2 {@link IStorageCell} with fluid stack type instead of removed ae2fc IStorageFluidCell.
 */
public class UnrestrictedFluidCellItem extends Item implements IStorageCell, CellInventoryProvider {

    private final int capacity;
    private final String id;

    public UnrestrictedFluidCellItem(String id, int capacity) {
        this.id = id;
        this.capacity = capacity;
        ((Item) this).setMaxStackSize(1);
        ((Item) this).setUnlocalizedName(id);
        ((Item) this).setTextureName(NHThings290.MOD_ID + ":" + id);
        setCreativeTab(NHThings290.CREATIVE_TAB);
    }

    public void register() {
        GameRegistry.registerItem(this, id);
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public int getBytes(ItemStack cellItem) {
        return capacity;
    }

    @Override
    public long getBytesLong(ItemStack cellItem) {
        return capacity;
    }

    @Override
    public int BytePerType(ItemStack cellItem) {
        return 0;
    }

    @Override
    public int getBytesPerType(ItemStack cellItem) {
        return 0;
    }

    @Override
    public int getTotalTypes(ItemStack cellItem) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isBlackListed(ItemStack cellItem, appeng.api.storage.data.IAEItemStack requestedAddition) {
        return true;
    }

    @Override
    public boolean isBlackListed(IAEStack<?> stack) {
        if (!(stack instanceof IAEFluidStack)) {
            return true;
        }
        IAEFluidStack fluid = (IAEFluidStack) stack;
        return fluid.getFluid() == null;
    }

    public boolean isBlackListed(ItemStack cellItem, IAEFluidStack fluid) {
        return isBlackListed(fluid);
    }

    @Override
    public boolean storableInStorageCell() {
        return false;
    }

    @Override
    public boolean isStorageCell(ItemStack i) {
        return false;
    }

    @Override
    public double getIdleDrain() {
        return 1.0D;
    }

    @Override
    public double getIdleDrain(ItemStack is) {
        return getIdleDrain();
    }

    @Override
    public boolean isEditable(ItemStack is) {
        return true;
    }

    @Override
    public IInventory getUpgradesInventory(ItemStack is) {
        return new appeng.items.contents.CellUpgrades(is, 2);
    }

    @Override
    public IInventory getConfigInventory(ItemStack is) {
        // Default interface method wraps AE inventory; provide legacy IInventory via CellConfigLegacy path
        // by using the interface default through a temporary CellConfig for fluid type is not IInventory.
        // Use CellConfig + Legacy wrapper via AE stack inventory.
        return new appeng.items.contents.CellConfigLegacy(
            new appeng.items.contents.CellConfig(is),
            AEFluidStackType.FLUID_STACK_TYPE);
    }

    @Override
    public appeng.tile.inventory.IAEStackInventory getConfigAEInventory(ItemStack is) {
        return new appeng.items.contents.CellConfig(is);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        String fz = appeng.util.Platform.openNbtData(is)
            .getString("FuzzyMode");
        try {
            return FuzzyMode.valueOf(fz);
        } catch (Throwable t) {
            return FuzzyMode.IGNORE_ALL;
        }
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode mode) {
        appeng.util.Platform.openNbtData(is)
            .setString("FuzzyMode", mode.name());
    }

    @Override
    public IAEStackType<?> getStackType() {
        return AEFluidStackType.FLUID_STACK_TYPE;
    }

    public IMEInventoryHandler<IAEFluidStack> getInventoryHandler(ItemStack is, ISaveProvider provider,
        EntityPlayer player) throws AppEngException {
        return new UnrestrictedFluidCellHandler(new UnrestrictedFluidCellInventory(is, provider, this));
    }

    @Override
    public IMEInventoryHandler<?> getCellInventory(ItemStack is, ISaveProvider provider, EntityPlayer player)
        throws AppEngException {
        return getInventoryHandler(is, provider, player);
    }

    public StorageChannel getChannel() {
        return StorageChannel.FLUIDS;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!player.isSneaking() || world.isRemote) {
            return super.onItemRightClick(stack, world, player);
        }
        if (!CellFluidStorageAccess.canDisassemble(stack)) {
            return stack;
        }

        giveOrDrop(player, new ItemStack(com.juzi.nhthings290.registry.ModItems.unrestrictedShell));
        giveOrDrop(player, UnrestrictedFluidCellComponents.forCapacity(capacity));
        if (stack.stackSize <= 1) {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            stack.stackSize = 0;
        } else {
            stack.stackSize--;
        }
        return stack;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!player.isSneaking() || world.isRemote
            || player.inventory.getCurrentItem() != stack
            || !CellFluidStorageAccess.canDisassemble(stack)) {
            return false;
        }

        player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        giveOrDrop(player, new ItemStack(com.juzi.nhthings290.registry.ModItems.unrestrictedShell));
        giveOrDrop(player, UnrestrictedFluidCellComponents.forCapacity(capacity));
        if (player.inventoryContainer != null) {
            player.inventoryContainer.detectAndSendChanges();
        }
        return true;
    }

    private static void giveOrDrop(EntityPlayer player, ItemStack stack) {
        ItemStack remaining = stack.copy();
        player.inventory.addItemStackToInventory(remaining);
        if (remaining.stackSize > 0) {
            player.entityDropItem(remaining, 0.0F);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        super.addInformation(stack, player, lines, advanced);
        lines.add(
            StatCollector.translateToLocalFormatted(
                "nhthings290.cell.types",
                CellFluidStorageAccess.getStoredFluidTypes(stack)));
        lines.add(
            StatCollector.translateToLocalFormatted(
                "nhthings290.cell.space",
                CellFluidStorageAccess.getUsedBytes(stack),
                capacity));
        UnrestrictedCellPartitionTooltip.appendFluidPartition(stack, this, lines);
    }

    public static UnrestrictedFluidCellItem create(String id, int capacity) {
        UnrestrictedFluidCellItem item = new UnrestrictedFluidCellItem(id, capacity);
        item.register();
        return item;
    }
}
