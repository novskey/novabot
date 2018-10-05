package com.github.novskey.novabot.data;

import com.github.novskey.novabot.core.Location;
import com.github.novskey.novabot.core.Spawn;
import com.github.novskey.novabot.core.UserPref;
import com.github.novskey.novabot.maps.GeocodedLocation;
import com.github.novskey.novabot.pokemon.PokeSpawn;
import com.github.novskey.novabot.pokemon.Pokemon;
import com.github.novskey.novabot.raids.Raid;
import com.github.novskey.novabot.raids.RaidSpawn;
import com.github.novskey.novabot.raids.RaidLobbyMember;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Paris on 17/01/2018.
 */
public interface IDataBase {
    void addPokemon(String userID, Pokemon pokemon);

    void addPreset(String userID, String preset, Location location);

    void addRaid(String userID, Raid raid);

    User addUser(String userID, String botToken);

    void clearPreset(String id, String[] presets);

    void clearLocationsPresets(String id, Location[] locations);

    void clearLocationsPokemon(String id, Location[] locations);

    void clearLocationsRaids(String id, Location[] locations);

    void clearPokemon(String id, ArrayList<Pokemon> pokemons);

    void clearRaid(String id, ArrayList<Raid> raids);

    void clearTokens(ArrayList<String> toRemove);

    int countPokemon(String id, Pokemon[] potentialPokemon, boolean countLocations);

    int countPresets(String userID, ArrayList<Preset> presetsList, boolean countLocations);

    int countRaids(String id, Raid[] potentialRaids, boolean countLocations);

    void deletePokemon(String userID, Pokemon pokemon);

    void deletePreset(String userId, String preset, Location location);

    void deleteRaid(String userID, Raid raid);

    void endLobby(String lobbyCode, String gymId);

    GeocodedLocation getGeocodedLocation(double lat, double lon);

    User getUser(String id);

    ArrayList<String> getUserIDsToNotify(RaidSpawn raidSpawn);

    ArrayList<String> getUserIDsToNotify(String preset, Spawn spawn);

    ArrayList<String> getUserIDsToNotify(PokeSpawn pokeSpawn);

    UserPref getUserPref(String id);

    int highestRaidLobbyId();

    void logNewUser(String userID);

    void newLobby(String lobbyCode, String gymId, String channelId, String roleId, long nextTimeLeftUpdate, String inviteCode, HashSet<RaidLobbyMember> members, String[] lobbyChatIds);

    boolean notContainsUser(String userID);

    void pauseUser(String id);

    void resetPokemon(String id);

    void resetPresets(String id);

    void resetRaids(String id);

    void resetUser(String id);

    void setBotToken(String id, String nextUserBotToken);

    void setGeocodedLocation(double lat, double lon, GeocodedLocation location);

    void unPauseUser(String id);

    void updateLobby(String lobbyCode, int nextTimeLeftUpdate, String inviteCode, String roleId, String channelId, HashSet<RaidLobbyMember> members, String gymId, String[] lobbyChatIds);

    int purgeUnknownSpawnpoints();

    ZoneId getZoneId(double lat, double lon);

    void setZoneId(double lat, double lon, ZoneId zoneId);

    void saveToken(String userId, String token, int hours);

    void clearTokens(String userId);

    void verifyUser(String id);

}
