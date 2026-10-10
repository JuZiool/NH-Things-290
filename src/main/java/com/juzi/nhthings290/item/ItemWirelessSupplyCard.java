package com.juzi.nhthings290.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.juzi.nhthings290.NHThings290;
import com.juzi.nhthings290.machine.WirelessChargingStation;
import com.juzi.nhthings290.machine.WirelessLinkData;
import com.juzi.nhthings290.machine.WirelessLinkData.Position;
import com.juzi.nhthings290.machine.WirelessLinkData.Station;
import com.juzi.nhthings290.machine.WirelessPowerLogic;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public final class ItemWirelessSupplyCard extends Item {

    private static final String STATION_TAG = "WirelessStation";

    public ItemWirelessSupplyCard() {
        setUnlocalizedName("wireless_supply_card");
        setTextureName(NHThings290.MOD_ID + ":wireless_supply_card");
        setCreativeTab(NHThings290.CREATIVE_TAB);
        setMaxStackSize(1);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IGregTechTileEntity)) return false;
        if (world.isRemote) return false;
        IGregTechTileEntity target = (IGregTechTileEntity) tile;
        if (player.isSneaking()) {
            if (target.getMetaTileEntity() instanceof WirelessChargingStation) {
                WirelessChargingStation station = (WirelessChargingStation) target.getMetaTileEntity();
                getTag(stack).setTag(
                    STATION_TAG,
                    station.reference()
                        .write());
                message(player, "recorded", station.getLocalName(), x, y, z);
            } else {
                message(player, "select_station");
            }
            return true;
        }

        Station stored = selection(stack);
        if (stored == null) {
            message(player, "select_station");
            return true;
        }
        if (stored.position.dimension != world.provider.dimensionId) {
            message(player, "different_dimension");
            return true;
        }
        Position sourcePosition = stored.position;
        if (!world.blockExists(sourcePosition.x, sourcePosition.y, sourcePosition.z)) {
            message(player, "station_unloaded");
            return true;
        }
        TileEntity sourceTile = world.getTileEntity(sourcePosition.x, sourcePosition.y, sourcePosition.z);
        if (!(sourceTile instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) sourceTile).getMetaTileEntity() instanceof WirelessChargingStation)) {
            message(player, "station_missing");
            return true;
        }
        WirelessChargingStation source = (WirelessChargingStation) ((IGregTechTileEntity) sourceTile)
            .getMetaTileEntity();
        if (!stored.equals(source.reference())) {
            message(player, "station_missing");
            return true;
        }
        if (!WirelessChargingStation.isEligibleTarget(target)) {
            message(player, "unsupported");
            return true;
        }
        if (target.getInputVoltage() > source.maxEUInput()) {
            message(player, "voltage_too_high");
            return true;
        }
        if (!WirelessPowerLogic
            .inRange(sourcePosition.x, sourcePosition.y, sourcePosition.z, x, y, z, source.radius())) {
            message(player, "out_of_range", source.radius());
            return true;
        }
        Position position = new Position(world.provider.dimensionId, x, y, z);
        WirelessLinkData links = WirelessLinkData.get(world);
        boolean connected = links.toggleBinding(position, stored);
        if (connected) links.setMachineId(position, stored, target.getMetaTileID());
        message(player, connected ? "connected" : "disconnected", x, y, z);
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && player.isSneaking() && getMovingObjectPositionFromPlayer(world, player, false) == null) {
            if (stack.hasTagCompound()) stack.getTagCompound()
                .removeTag(STATION_TAG);
            message(player, "cleared");
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        Station station = selection(stack);
        lines.add(StatCollector.translateToLocal("nhthings290.wireless.card.select"));
        lines.add(StatCollector.translateToLocal("nhthings290.wireless.card.connect"));
        lines.add(StatCollector.translateToLocal("nhthings290.wireless.card.clear"));
        if (station != null) {
            lines.add(
                StatCollector
                    .translateToLocalFormatted("nhthings290.wireless.card.selected", station.position.toString()));
        }
    }

    public static Station selection(ItemStack stack) {
        return stack.hasTagCompound() ? Station.read(
            stack.getTagCompound()
                .getCompoundTag(STATION_TAG))
            : null;
    }

    private static NBTTagCompound getTag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }

    private static void message(EntityPlayer player, String key, Object... arguments) {
        player.addChatMessage(new ChatComponentTranslation("nhthings290.wireless.card." + key, arguments));
    }
}
