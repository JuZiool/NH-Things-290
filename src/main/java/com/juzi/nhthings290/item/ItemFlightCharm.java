package com.juzi.nhthings290.item;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import com.juzi.nhthings290.NHThings290;

import baubles.api.BaubleType;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemFlightCharm extends Item implements IBaubleExpanded {

    private static final int FOOD_COST_INTERVAL = 600;
    private static final int MIN_FOOD_LEVEL = 4;
    private static final float EXHAUSTION_COST = 8.0F;

    private static final String TAG_FLY_TIMER = "flyTimer";
    private static final String TAG_FLIGHT_OWNER = "flightOwner";

    public ItemFlightCharm() {
        ((Item) this).setUnlocalizedName("flight_charm");
        ((Item) this).setTextureName(NHThings290.MOD_ID + ":flight_charm");
        ((Item) this).setMaxStackSize(1);
        setCreativeTab(NHThings290.CREATIVE_TAB);
    }

    @Override
    public String[] getBaubleTypes(ItemStack stack) {
        return new String[] { BaubleExpandedSlots.charmType };
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.UNIVERSAL;
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase player) {
        if (player.worldObj.isRemote || !(player instanceof EntityPlayer)) return;

        EntityPlayer entityPlayer = (EntityPlayer) player;
        updateFlightPermission(stack, entityPlayer);
        tickFlightConsumption(stack, entityPlayer);
    }

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase player) {
        if (!player.worldObj.isRemote && player instanceof EntityPlayer) {
            updateFlightPermission(stack, (EntityPlayer) player);
        }
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase player) {
        if (!player.worldObj.isRemote && player instanceof EntityPlayer) {
            revokeOwnedFlight(stack, (EntityPlayer) player);
        }
    }

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase player) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack stack, EntityLivingBase player) {
        return true;
    }

    @Override
    public void onPlayerLoad(ItemStack stack, EntityLivingBase player) {
        if (!player.worldObj.isRemote && player instanceof EntityPlayer) {
            updateFlightPermission(stack, (EntityPlayer) player);
        }
    }

    private void updateFlightPermission(ItemStack stack, EntityPlayer player) {
        if (player.capabilities.isCreativeMode) {
            clearFlightOwner(stack);
            return;
        }

        boolean hasEnoughFood = player.getFoodStats()
            .getFoodLevel() >= MIN_FOOD_LEVEL;
        if (!hasEnoughFood) {
            revokeOwnedFlight(stack, player);
            return;
        }

        if (FlightCharmLogic
            .shouldGrantFlight(player.capabilities.allowFlying, player.capabilities.isCreativeMode, hasEnoughFood)) {
            player.capabilities.allowFlying = true;
            setFlightOwner(stack, player);
            player.sendPlayerAbilities();
        } else if (!ownsFlight(stack, player)) {
            clearFlightOwner(stack);
        }
    }

    private void revokeOwnedFlight(ItemStack stack, EntityPlayer player) {
        boolean ownsFlight = ownsFlight(stack, player);
        clearFlightOwner(stack);
        if (!FlightCharmLogic.shouldRevokeFlight(ownsFlight, player.capabilities.isCreativeMode)) return;

        player.capabilities.allowFlying = false;
        player.capabilities.isFlying = false;
        player.sendPlayerAbilities();
    }

    private void tickFlightConsumption(ItemStack stack, EntityPlayer player) {
        boolean hasEnoughFood = player.getFoodStats()
            .getFoodLevel() >= MIN_FOOD_LEVEL;
        if (!FlightCharmLogic.shouldCountFlight(
            ownsFlight(stack, player),
            player.capabilities.isFlying,
            player.capabilities.isCreativeMode,
            hasEnoughFood)) {
            return;
        }

        NBTTagCompound tag = getOrCreateTag(stack);
        int timer = tag.getInteger(TAG_FLY_TIMER);
        if (FlightCharmLogic.shouldChargeOnNextTick(timer, FOOD_COST_INTERVAL)) {
            player.getFoodStats()
                .addExhaustion(EXHAUSTION_COST);
        }
        tag.setInteger(TAG_FLY_TIMER, FlightCharmLogic.nextTimer(timer, FOOD_COST_INTERVAL));
    }

    private boolean ownsFlight(ItemStack stack, EntityPlayer player) {
        return stack.hasTagCompound() && player.getUniqueID()
            .toString()
            .equals(
                stack.getTagCompound()
                    .getString(TAG_FLIGHT_OWNER));
    }

    private void setFlightOwner(ItemStack stack, EntityPlayer player) {
        getOrCreateTag(stack).setString(
            TAG_FLIGHT_OWNER,
            player.getUniqueID()
                .toString());
    }

    private void clearFlightOwner(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound()
                .removeTag(TAG_FLIGHT_OWNER);
        }
    }

    private NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            addTooltip(lines, "item.flight_charm.tooltip.detail1");
            addTooltip(lines, "item.flight_charm.tooltip.detail2");
            addTooltip(lines, "item.flight_charm.tooltip.detail3");
        } else {
            addTooltip(lines, "item.flight_charm.tooltip.shift");
        }
    }

    @SuppressWarnings("unchecked")
    private static void addTooltip(List lines, String translationKey) {
        lines.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(translationKey));
    }
}
