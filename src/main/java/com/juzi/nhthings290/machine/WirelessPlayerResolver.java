package com.juzi.nhthings290.machine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public final class WirelessPlayerResolver {

    public List<EntityPlayerMP> resolve(UUID ownerUuid, MinecraftServer server) {
        Map<UUID, EntityPlayerMP> players = new LinkedHashMap<UUID, EntityPlayerMP>();
        EntityPlayerMP owner = findOnlinePlayer(ownerUuid, server);
        if (owner != null) {
            players.put(owner.getUniqueID(), owner);
        }

        try {
            Class<?> universeClass = Class.forName("serverutils.lib.data.Universe");
            Object universe = universeClass.getMethod("getNullable")
                .invoke(null);
            if (universe == null) {
                return new ArrayList<EntityPlayerMP>(players.values());
            }
            Object forgePlayer = universeClass.getMethod("getPlayer", UUID.class)
                .invoke(universe, ownerUuid);
            if (forgePlayer == null) {
                return new ArrayList<EntityPlayerMP>(players.values());
            }
            Field teamField = forgePlayer.getClass()
                .getField("team");
            Object team = teamField.get(forgePlayer);
            if (team == null) {
                return new ArrayList<EntityPlayerMP>(players.values());
            }
            Method onlineMembers = team.getClass()
                .getMethod("getOnlineMembers");
            Collection<?> members = (Collection<?>) onlineMembers.invoke(team);
            for (Object member : members) {
                if (member instanceof EntityPlayerMP) {
                    EntityPlayerMP player = (EntityPlayerMP) member;
                    players.put(player.getUniqueID(), player);
                }
            }
        } catch (Exception ignored) {}
        return new ArrayList<EntityPlayerMP>(players.values());
    }

    @SuppressWarnings("unchecked")
    private EntityPlayerMP findOnlinePlayer(UUID ownerUuid, MinecraftServer server) {
        if (ownerUuid == null || server == null) {
            return null;
        }
        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : players) {
            if (ownerUuid.equals(player.getUniqueID())) {
                return player;
            }
        }
        return null;
    }
}
