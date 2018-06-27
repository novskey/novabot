package com.github.novskey.novabot.raids;

import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.core.RotatingSet;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Owner on 2/07/2017.
 */
public class LobbyManager {

    final ConcurrentHashMap<String, RaidLobby> activeLobbies = new ConcurrentHashMap<>();
    private static final Logger lobbyManagerLog = LoggerFactory.getLogger("Lobby-Manager");

    private final RotatingSet<String> oldLobbyRoleIds = new RotatingSet<>(200,ConcurrentHashMap.newKeySet(200));
    private final NovaBot novaBot;

    public LobbyManager(NovaBot novaBot) {
        this.novaBot = novaBot;
    }

    public RaidLobby getLobby(String lobbyCode) {
        return activeLobbies.get(lobbyCode);
    }

    public boolean isLobbyChannel(String id) {
        Collection<RaidLobby> raidLobbies = activeLobbies.values();
        for (RaidLobby raidLobby : raidLobbies) {
            if (raidLobby.channelId == null) continue;
            if (raidLobby.channelId.equals(id))
                return true;
        }
        return false;
    }

    public RaidLobby getLobbyByChannelId(String id) {
        for (RaidLobby raidLobby : activeLobbies.values()) {
            if(raidLobby.channelId == null) continue;
            if(raidLobby.channelId.equals(id))
                return raidLobby;
        }
        return null;
    }

    public void endLobby(String lobbyCode) {
        RaidLobby lobby = getLobby(lobbyCode);

        if(lobby != null) {
            getLobby(lobbyCode).end(15);
        }
    }

    public RaidLobby getLobbyByGymId(String gymId) {
        for (Map.Entry<String, RaidLobby> raidLobbyEntry : activeLobbies.entrySet()) {
            if(raidLobbyEntry.getValue().spawn.gymId.equals(gymId)){
                return raidLobbyEntry.getValue();
            }
        }
        return null;
    }

    public boolean isLobbyRoleId(String id) {
        for (RaidLobby lobby : activeLobbies.values()) {
            if(lobby.roleId == null) continue;
            if(lobby.roleId.equals(id)) return true;
        }

        return oldLobbyRoleIds.contains(id);
    }

    public ArrayList<RaidLobby> getLobbiesByGeofence(GeofenceIdentifier geofence) {
        ArrayList<RaidLobby> lobbies = new ArrayList<>();

        for (RaidLobby lobby : activeLobbies.values()) {
            if(lobby.spawn.getGeofences().contains(geofence)){
                lobbies.add(lobby);
            }
        }

        return lobbies;
    }

    public ArrayList<RaidLobby> getActiveLobbies() {
        ArrayList<RaidLobby> lobbies = new ArrayList<>();

        for (RaidLobby lobby : activeLobbies.values()) {
            if(lobby.memberCount() > 0) {
                lobbies.add(lobby);
            }
        }

        return lobbies;
    }

    synchronized
    public RaidSpawn newRaid(String lobbyCode, RaidSpawn raidSpawn) {
        Boolean contains = false;
        for (RaidLobby activeLobby : activeLobbies.values()) {
            if (activeLobby.spawn.gymId.equals(raidSpawn.gymId)) {
                return activeLobby.spawn;
            }
        }
        activeLobbies.put(lobbyCode, new RaidLobby(raidSpawn, lobbyCode, null, novaBot, false));
        return raidSpawn;
    }

    public void addLobbies(ArrayList<RaidLobby> lobbies) {
        for (RaidLobby activeLobby : lobbies) {
            if(activeLobby.inviteCode == null){
                activeLobby.createInvite();
            }
            addLobby(activeLobby);
        }
    }

    private void addLobby(RaidLobby raidLobby) {
        activeLobbies.put(raidLobby.lobbyCode,raidLobby);
    }

    public void removeLobby(String lobbyCode) {
        activeLobbies.remove(lobbyCode);
    }
}
