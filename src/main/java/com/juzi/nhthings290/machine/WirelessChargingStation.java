package com.juzi.nhthings290.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.modularui.api.drawable.Text;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.CycleButtonWidget;
import com.gtnewhorizons.modularui.common.widget.FakeSyncWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.juzi.nhthings290.NHThings290;
import com.juzi.nhthings290.machine.WirelessLinkData.Position;
import com.juzi.nhthings290.machine.WirelessLinkData.Station;

import baubles.api.BaublesApi;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.modularui.IAddUIWidgets;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicHull;
import gregtech.api.render.TextureFactory;
import gregtech.client.StructureErrorHighlightRenderer;
import gregtech.client.iconContainers.blocks.GTCustomBlockIconContainer;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;

@IMetaTileEntity.SkipGenerateDescription
public final class WirelessChargingStation extends MTEBasicHull implements IAddUIWidgets {

    private static final IIconContainer FRONT = GTCustomBlockIconContainer
        .create(NHThings290.MOD_ID, "machine/overlay_charging_station");

    private final WirelessPlayerResolver playerResolver = new WirelessPlayerResolver();
    private UUID stationId;
    private List<Position> targets = Collections.emptyList();
    private List<EntityPlayerMP> players = Collections.emptyList();
    private long linkRevision = Long.MIN_VALUE;
    private int serviceCursor;
    private int playerCursor;
    private int selectedIndex;
    private boolean enabled = true;
    private boolean machinesEnabled = true;
    private boolean playersEnabled = true;
    private boolean playersFirst = true;
    private long lastOutput;
    private int eligiblePlayers;
    private long clientStored;
    private int clientTargets;
    private int clientSelectedDimension;
    private int clientSelectedMachineId = -1;
    private String clientSelected = "-";
    private String clientTargetState = "none";

    public WirelessChargingStation(int id, String name, String regionalName, int tier) {
        super(id, name, regionalName, tier);
    }

    private WirelessChargingStation(String name, int tier, int slots, String[] description, ITexture[][][] textures) {
        super(name, tier, slots, description, textures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new WirelessChargingStation(mName, mTier, mInventory.length, mDescriptionArray, mTextures);
    }

    public Station reference() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (stationId == null) {
            stationId = UUID.randomUUID();
            markDirty();
        }
        return new Station(
            new Position(base.getWorld().provider.dimensionId, base.getXCoord(), base.getYCoord(), base.getZCoord()),
            stationId);
    }

    public int radius() {
        return WirelessPowerLogic.radius(mTier);
    }

    @Override
    public long maxEUInput() {
        return WirelessPowerLogic.voltage(mTier);
    }

    @Override
    public long maxAmperesIn() {
        return WirelessPowerLogic.AMPERAGE;
    }

    @Override
    public long maxEUStore() {
        return WirelessPowerLogic.bufferCapacity(mTier);
    }

    @Override
    public boolean isEnetOutput() {
        return false;
    }

    @Override
    public boolean isOutputFacing(ForgeDirection side) {
        return false;
    }

    @Override
    public boolean isValidSlot(int index) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity base, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public String getLocalName() {
        return text("name", GTValues.VN[mTier]);
    }

    @Override
    public String[] getDescription() {
        return new String[] { text("description", radius()),
            text("power", number(WirelessPowerLogic.tickBudget(mTier))),
            text(WirelessPowerLogic.canChargePlayers(mTier) ? "description_players" : "description_machines"),
            text("added_by", NHThings290.MOD_NAME) };
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity base, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.MACHINE_CASINGS[mTier][color + 1];
        return side == facing ? new ITexture[] { casing, TextureFactory.of(FRONT) } : new ITexture[] { casing };
    }

    @Override
    public void onPostTick(IGregTechTileEntity base, long tick) {
        super.onPostTick(base, tick);
        if (!base.isServerSide()) return;
        refreshTargets();
        if (tick % 20 == 0) refreshTargetMachines();
        if (WirelessPowerLogic.canChargePlayers(mTier) && tick % 20 == 0) {
            players = playerResolver.resolve(base.getOwnerUuid(), MinecraftServer.getServer());
        }
        lastOutput = 0;
        eligiblePlayers = 0;
        if (!enabled || !base.isAllowedToWork()) {
            base.setActive(false);
            return;
        }
        long budget = Math.min(base.getStoredEU(), WirelessPowerLogic.tickBudget(mTier));
        if (budget > 0) {
            if (playersFirst) {
                lastOutput = chargePlayers(base, budget);
                lastOutput += supplyMachines(base, budget - lastOutput);
            } else {
                lastOutput = supplyMachines(base, budget);
                lastOutput += chargePlayers(base, budget - lastOutput);
            }
            if (lastOutput > 0) base.decreaseStoredEnergyUnits(lastOutput, true);
        }
        base.setActive(lastOutput > 0);
    }

    private void refreshTargets() {
        WirelessLinkData links = WirelessLinkData.get(getBaseMetaTileEntity().getWorld());
        if (links.revision() != linkRevision) {
            targets = links.targets(reference());
            linkRevision = links.revision();
            selectedIndex = targets.isEmpty() ? 0 : Math.floorMod(selectedIndex, targets.size());
        }
    }

    private void refreshTargetMachines() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        WirelessLinkData links = WirelessLinkData.get(base.getWorld());
        Station station = reference();
        Set<Position> missing = new HashSet<>();
        for (Position position : targets) {
            if (position.dimension != base.getWorld().provider.dimensionId || !base.getWorld()
                .blockExists(position.x, position.y, position.z)) continue;
            TileEntity tile = base.getWorld()
                .getTileEntity(position.x, position.y, position.z);
            if (!(tile instanceof IGregTechTileEntity)) {
                missing.add(position);
                continue;
            }
            IGregTechTileEntity machine = (IGregTechTileEntity) tile;
            if (machine.getMetaTileEntity() != null) links.setMachineId(position, station, machine.getMetaTileID());
        }
        if (!missing.isEmpty()) links.removeMissingTargets(station, missing::contains);
        refreshTargets();
    }

    private long supplyMachines(IGregTechTileEntity base, long budget) {
        if (!machinesEnabled || targets.isEmpty() || budget <= 0) return 0;
        long used = 0;
        int size = targets.size();
        int start = Math.floorMod(serviceCursor, size);
        WirelessLinkData links = WirelessLinkData.get(base.getWorld());
        Station station = reference();
        for (int offset = 0; offset < size && used < budget; offset++) {
            Position position = targets.get((start + offset) % size);
            if (!links.isBound(position, station) || !"ready".equals(targetState(position))) continue;
            IGregTechTileEntity target = (IGregTechTileEntity) base.getWorld()
                .getTileEntity(position.x, position.y, position.z);
            long voltage = target.getInputVoltage();
            long amperes = WirelessPowerLogic.targetAmperage(mTier, voltage, budget - used);
            if (amperes <= 0) continue;
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                if (!target.inputEnergyFrom(side)) continue;
                long accepted = target.injectEnergyUnits(side, voltage, amperes);
                used += Math.max(0, Math.min(accepted, amperes)) * voltage;
                if (accepted > 0) break;
            }
        }
        serviceCursor = WirelessPowerLogic.nextCursor(start, size);
        return used;
    }

    public static boolean isEligibleTarget(IGregTechTileEntity target) {
        if (target.getMetaTileEntity() == null || target.getMetaTileEntity() instanceof WirelessChargingStation
            || target.getInputVoltage() <= 0) return false;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (target.inputEnergyFrom(side, false)) return true;
        }
        return false;
    }

    private String targetState(Position position) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (position.dimension != base.getWorld().provider.dimensionId) return "out_of_range";
        if (!WirelessPowerLogic.inRange(
            base.getXCoord(),
            base.getYCoord(),
            base.getZCoord(),
            position.x,
            position.y,
            position.z,
            radius())) return "out_of_range";
        if (!base.getWorld()
            .blockExists(position.x, position.y, position.z)) return "unloaded";
        TileEntity tile = base.getWorld()
            .getTileEntity(position.x, position.y, position.z);
        if (!(tile instanceof IGregTechTileEntity) || !isEligibleTarget((IGregTechTileEntity) tile))
            return "unsupported";
        IGregTechTileEntity target = (IGregTechTileEntity) tile;
        if (target.getInputVoltage() > maxEUInput()) return "voltage";
        if (target.getEUCapacity() > 0 && target.getStoredEU() >= target.getEUCapacity()) return "full";
        return "ready";
    }

    private long chargePlayers(IGregTechTileEntity base, long budget) {
        if (!playersEnabled || !WirelessPowerLogic.canChargePlayers(mTier)) return 0;
        List<ItemStack> stacks = new ArrayList<>();
        List<EntityPlayerMP> owners = new ArrayList<>();
        for (EntityPlayerMP player : players) {
            if (player.isDead || player.worldObj != base.getWorld()
                || !MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList.contains(player)
                || player.getDistanceSq(base.getXCoord() + 0.5D, base.getYCoord() + 0.5D, base.getZCoord() + 0.5D)
                    > (double) radius() * radius())
                continue;
            eligiblePlayers++;
            addItems(player, player.inventory, stacks, owners);
            IInventory baubles = BaublesApi.getBaubles(player);
            if (baubles != null) addItems(player, baubles, stacks, owners);
        }
        if (stacks.isEmpty() || budget <= 0) return 0;
        long used = 0;
        Set<EntityPlayerMP> chargedPlayers = new HashSet<>();
        int start = Math.floorMod(playerCursor, stacks.size());
        for (int offset = 0; offset < stacks.size() && used < budget; offset++) {
            int index = (start + offset) % stacks.size();
            ItemStack stack = stacks.get(index);
            IElectricItem electric = (IElectricItem) stack.getItem();
            if (electric.getTier(stack) > mTier) continue;
            double accepted = ElectricItem.manager.charge(stack, budget - used, mTier, false, false);
            used += (long) Math.ceil(Math.max(0.0D, Math.min(accepted, budget - used)));
            if (accepted > 0) chargedPlayers.add(owners.get(index));
        }
        for (EntityPlayerMP player : chargedPlayers) {
            player.inventory.markDirty();
            // Synchronize once per player, even when several of their electric items were charged.
            player.inventoryContainer.detectAndSendChanges();
            if (player.openContainer != player.inventoryContainer) player.openContainer.detectAndSendChanges();
        }
        playerCursor = WirelessPowerLogic.nextCursor(start, stacks.size());
        return used;
    }

    private static void addItems(EntityPlayerMP player, IInventory inventory, List<ItemStack> stacks,
        List<EntityPlayerMP> owners) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && stack.stackSize > 0 && stack.getItem() instanceof IElectricItem) {
                stacks.add(stack);
                owners.add(player);
            }
        }
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player) {
        if (base.isServerSide()) openGui(player);
        return true;
    }

    @Override
    protected boolean useMui2() {
        return false;
    }

    @Override
    public int getGUIHeight() {
        return 220;
    }

    @Override
    public void addUIWidgets(ModularWindow.Builder builder, UIBuildContext context) {
        builder.widget(
            TextWidget.dynamicString(this::statusText)
                .setSynced(false)
                .setTextAlignment(Alignment.TopLeft)
                .setScale(0.7F)
                .setPos(8, 18)
                .setSize(160, 64));
        builder.widget(
            TextWidget.dynamicString(this::selectedText)
                .setSynced(false)
                .setTextAlignment(Alignment.TopLeft)
                .setScale(0.7F)
                .setPos(8, 72)
                .setSize(160, 28));
        builder.widget(
            new FakeSyncWidget.LongSyncer(() -> getBaseMetaTileEntity().getStoredEU(), value -> clientStored = value));
        builder.widget(new FakeSyncWidget.LongSyncer(() -> lastOutput, value -> lastOutput = value));
        builder.widget(new FakeSyncWidget.IntegerSyncer(() -> targets.size(), value -> clientTargets = value));
        builder.widget(new FakeSyncWidget.IntegerSyncer(() -> eligiblePlayers, value -> eligiblePlayers = value));
        builder.widget(
            new FakeSyncWidget.IntegerSyncer(this::selectedDimension, value -> clientSelectedDimension = value));
        builder.widget(
            new FakeSyncWidget.IntegerSyncer(this::selectedMachineId, value -> clientSelectedMachineId = value));
        builder.widget(new FakeSyncWidget.StringSyncer(this::selectedPosition, value -> clientSelected = value));
        builder.widget(new FakeSyncWidget.StringSyncer(this::selectedState, value -> clientTargetState = value));
        builder.widget(new FakeSyncWidget.BooleanSyncer(() -> machinesEnabled, value -> machinesEnabled = value));
        builder.widget(new FakeSyncWidget.BooleanSyncer(() -> playersEnabled, value -> playersEnabled = value));
        builder.widget(new FakeSyncWidget.BooleanSyncer(() -> playersFirst, value -> playersFirst = value));
        builder.widget(new CycleButtonWidget().setToggle(() -> enabled, value -> {
            enabled = value;
            markDirty();
        })
            .setVariableBackground(GTUITextures.BUTTON_STANDARD, GTUITextures.BUTTON_STANDARD_PRESSED)
            .setTextureGetter(
                state -> state == 0 ? GTUITextures.OVERLAY_BUTTON_POWER_SWITCH_OFF
                    : GTUITextures.OVERLAY_BUTTON_POWER_SWITCH_ON)
            .addTooltip(text("toggle"))
            .setPos(8, 102)
            .setSize(18, 18));
        builder.widget(button("M", "toggle_machines", () -> machinesEnabled = !machinesEnabled).setPos(29, 102));
        if (WirelessPowerLogic.canChargePlayers(mTier)) {
            builder.widget(button("P", "toggle_players", () -> playersEnabled = !playersEnabled).setPos(50, 102));
            builder.widget(button("MP", "priority", () -> playersFirst = !playersFirst).setPos(71, 102));
        }
        builder.widget(button("<", "previous", () -> select(-1)).setPos(94, 102));
        builder.widget(button(">", "next", () -> select(1)).setPos(113, 102));
        builder.widget(button("X", "remove", this::removeSelected).setPos(132, 102));
        builder.widget(highlightButton().setPos(151, 102));
    }

    private ButtonWidget highlightButton() {
        ButtonWidget button = new ButtonWidget() {

            @Override
            @SideOnly(Side.CLIENT)
            public void readOnClient(int id, PacketBuffer buffer) {
                if (id != 10) {
                    super.readOnClient(id, buffer);
                    return;
                }
                int dimension = buffer.readInt();
                int x = buffer.readInt();
                int y = buffer.readInt();
                int z = buffer.readInt();
                if (getBaseMetaTileEntity().getWorld().provider.dimensionId == dimension) {
                    StructureErrorHighlightRenderer.highlight(x, y, z);
                }
            }
        };
        button.setOnClick((click, widget) -> {
            if (widget.isClient()) return;
            refreshTargets();
            if (targets.isEmpty()) return;
            Position target = targets.get(selectedIndex);
            button.syncToClient(10, buffer -> {
                buffer.writeInt(target.dimension);
                buffer.writeInt(target.x);
                buffer.writeInt(target.y);
                buffer.writeInt(target.z);
            });
        });
        button.setBackground(GTUITextures.BUTTON_STANDARD, GTUITextures.OVERLAY_BUTTON_BOUNDING_BOX)
            .setSize(18, 18)
            .addTooltip(text("highlight"));
        return button;
    }

    private ButtonWidget button(String label, String tooltip, Runnable action) {
        ButtonWidget button = new ButtonWidget().setOnClick((click, widget) -> {
            if (!widget.isClient()) {
                action.run();
                markDirty();
            }
        });
        button.setBackground(GTUITextures.BUTTON_STANDARD, new Text(label))
            .setSize(18, 18)
            .addTooltip(text(tooltip));
        return button;
    }

    private void select(int change) {
        refreshTargets();
        selectedIndex = targets.isEmpty() ? 0 : Math.floorMod(selectedIndex + change, targets.size());
    }

    private void removeSelected() {
        refreshTargets();
        if (targets.isEmpty()) return;
        WirelessLinkData.get(getBaseMetaTileEntity().getWorld())
            .unbind(targets.get(selectedIndex), reference());
        refreshTargets();
    }

    private int selectedMachineId() {
        if (targets.isEmpty()) return -1;
        return WirelessLinkData.get(getBaseMetaTileEntity().getWorld())
            .machineId(targets.get(Math.floorMod(selectedIndex, targets.size())), reference());
    }

    private String selectedMachineName() {
        int id = getBaseMetaTileEntity().isServerSide() ? selectedMachineId() : clientSelectedMachineId;
        if (id < 0 || id >= GregTechAPI.METATILEENTITIES.length || GregTechAPI.METATILEENTITIES[id] == null)
            return text("machine_unknown");
        return GregTechAPI.METATILEENTITIES[id].getLocalName();
    }

    private int selectedDimension() {
        return targets.isEmpty() ? 0 : targets.get(Math.floorMod(selectedIndex, targets.size())).dimension;
    }

    private String selectedPosition() {
        if (targets.isEmpty()) return "-";
        Position position = targets.get(Math.floorMod(selectedIndex, targets.size()));
        return position.x + ", " + position.y + ", " + position.z;
    }

    private String selectedState() {
        return targets.isEmpty() ? "none" : targetState(targets.get(Math.floorMod(selectedIndex, targets.size())));
    }

    private String selectedText() {
        boolean server = getBaseMetaTileEntity().isServerSide();
        if (server ? targets.isEmpty() : clientTargets == 0) return text("selected_empty");
        return text("selected_machine", selectedMachineName()) + "\n"
            + text(
                "selected",
                server ? selectedDimension() : clientSelectedDimension,
                server ? selectedPosition() : clientSelected)
            + "\n"
            + text("selected_state", text("target." + (server ? selectedState() : clientTargetState)));
    }

    private String statusText() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        long stored = base.isServerSide() ? base.getStoredEU() : clientStored;
        int count = base.isServerSide() ? targets.size() : clientTargets;
        return getLocalName() + " / "
            + text(enabled ? "enabled" : "disabled")
            + "\n"
            + text("energy", number(stored), number(maxEUStore()))
            + "\n"
            + text("output", number(lastOutput), number(WirelessPowerLogic.tickBudget(mTier)))
            + "\n"
            + text("range", radius(), count, eligiblePlayers)
            + "\n"
            + text(
                "modes",
                text(machinesEnabled ? "on" : "off"),
                text(WirelessPowerLogic.canChargePlayers(mTier) && playersEnabled ? "on" : "off"),
                text(playersFirst ? "players_first" : "machines_first"))
            + "\n"
            + text(
                "state",
                text(!enabled ? "disabled" : stored <= 0 ? "no_power" : lastOutput > 0 ? "active" : "waiting"));
    }

    public boolean isGivingInformation() {
        return true;
    }

    public String[] getInfoData() {
        return statusText().split("\n");
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        if (stationId != null) tag.setString("WirelessStationUUID", stationId.toString());
        tag.setBoolean("WirelessEnabled", enabled);
        tag.setBoolean("WirelessMachines", machinesEnabled);
        tag.setBoolean("WirelessPlayers", playersEnabled);
        tag.setBoolean("WirelessPlayersFirst", playersFirst);
        tag.setInteger("WirelessCursor", serviceCursor);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        try {
            stationId = UUID.fromString(tag.getString("WirelessStationUUID"));
        } catch (IllegalArgumentException ignored) {
            stationId = null;
        }
        enabled = !tag.hasKey("WirelessEnabled") || tag.getBoolean("WirelessEnabled");
        machinesEnabled = !tag.hasKey("WirelessMachines") || tag.getBoolean("WirelessMachines");
        playersEnabled = !tag.hasKey("WirelessPlayers") || tag.getBoolean("WirelessPlayers");
        playersFirst = !tag.hasKey("WirelessPlayersFirst") || tag.getBoolean("WirelessPlayersFirst");
        serviceCursor = tag.getInteger("WirelessCursor");
        linkRevision = Long.MIN_VALUE;
    }

    private static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String text(String key, Object... arguments) {
        return StatCollector.translateToLocalFormatted("nhthings290.wireless.station." + key, arguments);
    }
}
