package com.github.novskey.novabot.data;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.Location;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.core.Spawn;
import com.github.novskey.novabot.core.UserPref;
import com.github.novskey.novabot.maps.GeocodedLocation;
import com.github.novskey.novabot.pokemon.PokeSpawn;
import com.github.novskey.novabot.pokemon.Pokemon;
import com.github.novskey.novabot.raids.Raid;
import com.github.novskey.novabot.raids.RaidSpawn;

import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 * Created by Paris on 17/01/2018.
 */
public class DBCache implements IDataBase {

    public ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Set<Preset>> presets = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Set<Pokemon>> pokemons = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Set<Raid>> raids = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, DbLobby> raidLobbies = new ConcurrentHashMap<>();
    public ConcurrentHashMap<SpawnPoint, SpawnInfo> spawnInfo = new ConcurrentHashMap<>();
    private NovaBot novaBot;

    public DBCache(NovaBot novaBot){
        this.novaBot = novaBot;
    }

    @Override
    public void addPokemon(String userID, Pokemon pokemon) {
        Set<Pokemon> settings = pokemons.get(userID);
        if(settings == null){
            settings = ConcurrentHashMap.newKeySet();
            pokemons.put(userID,settings);
        }
        pokemons.get(userID).add(pokemon);
    }

    @Override
    public void addPreset(String userID, String preset, Location location) {
        Set<Preset> settings = presets.get(userID);
        if(settings == null){
            settings = ConcurrentHashMap.newKeySet();
            presets.put(userID,settings);
        }
        presets.get(userID).add(new Preset(preset,location));
    }

    @Override
    public void addRaid(String userID, Raid raid) {
        Set<Raid> settings = raids.get(userID);
        if(settings == null){
            settings = ConcurrentHashMap.newKeySet();
            raids.put(userID,settings);
        }
        raids.get(userID).add(raid);
    }

    @Override
    public void addUser(String userID) {
        users.put(userID,new User(userID));
    }

    @Override
    public void clearPreset(String id, String[] presets) {
        Set<Preset> settings = this.presets.get(id);

        if (settings != null){
            HashSet<String> presetNames = new HashSet<>();
            Collections.addAll(presetNames, presets);
            settings.removeIf(preset -> presetNames.contains(preset.presetName));
        }
    }

    @Override
    public void clearLocationsPresets(String id, Location[] locations) {
        Set<Preset> settings = this.presets.get(id);

        if (settings != null){
            HashSet<String> locationNames = getLocationNames(locations);
            settings.removeIf(preset -> locationNames.contains(preset.location.toDbString().toLowerCase()));
        }
    }

    @Override
    public void clearLocationsPokemon(String id, Location[] locations) {
        Set<Pokemon> settings = pokemons.get(id);

        if (settings != null){
            HashSet<String> locationNames = getLocationNames(locations);
            settings.removeIf(pokemon -> locationNames.contains(pokemon.getLocation().toDbString().toLowerCase()));
        }
    }

    @Override
    public void clearLocationsRaids(String id, Location[] locations) {
        Set<Raid> settings = raids.get(id);

        if (settings != null){
            HashSet<String> locationNames = getLocationNames(locations);
            settings.removeIf(raid -> locationNames.contains(raid.location.toDbString().toLowerCase()));
        }
    }

    @Override
    public void clearPokemon(String id, ArrayList<Pokemon> pokemons) {
        Set<Pokemon> settings = this.pokemons.get(id);

        if (settings != null){
            HashSet<Pokemon> pokemonSet = new HashSet<>(pokemons);
            settings.removeIf(pokemonSet::contains);
        }
    }

    @Override
    public void clearRaid(String id, ArrayList<Raid> raids) {
        Set<Raid> settings = this.raids.get(id);

        if (settings != null){
            HashSet<Raid> raidSet = new HashSet<>(raids);
            settings.removeIf(raidSet::contains);
        }
    }

    @Override
    public int countPokemon(String id, boolean countLocations) {
        Set<Pokemon> settings = pokemons.get(id);

        int count = 0;
        if (settings != null){
            if (countLocations){
                count = settings.size();
            }else{
                HashSet<Pokemon> noDuplicateLocations = new HashSet<>();

                for (Pokemon pokemon : settings) {
                    Pokemon noLocation = new Pokemon(pokemon.name,Location.ALL, pokemon.miniv,pokemon.maxiv, pokemon.minlvl, pokemon.maxlvl, pokemon.mincp, pokemon.maxcp);
                    noDuplicateLocations.add(noLocation);
                }

                count = noDuplicateLocations.size();
            }
        }
        return count;
    }

    @Override
    public int countPresets(String userID, boolean countLocations) {
        Set<Preset> settings = presets.get(userID);

        int count = 0;
        if (settings != null){
            if (countLocations){
                count = settings.size();
            }else{
                HashSet<Preset> noDuplicateLocations = new HashSet<>();

                for (Preset preset : settings) {
                    Preset noLocation = new Preset(preset.presetName, Location.ALL);
                    noDuplicateLocations.add(noLocation);
                }

                count = noDuplicateLocations.size();
            }
        }
        return count;
    }

    @Override
    public int countRaids(String id, boolean countLocations) {
        Set<Raid> settings = raids.get(id);

        int count = 0;
        if (settings != null){
            if (countLocations){
                count = settings.size();
            }else{
                HashSet<Raid> noDuplicateLocations = new HashSet<>();

                for (Raid raid : settings) {
                    Raid noLocation = new Raid(raid.bossId, Location.ALL);
                    noDuplicateLocations.add(noLocation);
                }

                count = noDuplicateLocations.size();
            }
        }
        return count;
    }

    @Override
    public void deletePokemon(String userID, Pokemon pokemon) {
        Set<Pokemon> settings = pokemons.get(userID);

        if (settings != null){
            settings.remove(pokemon);
        }
    }

    @Override
    public void deletePreset(String userId, String preset, Location location) {
        Set<Preset> settings = presets.get(userId);

        if (settings != null){
            settings.remove(new Preset(preset, location));
        }
    }

    @Override
    public void deleteRaid(String userID, Raid raid) {
        Set<Raid> settings = raids.get(userID);

        if (settings != null){
            settings.remove(raid);
        }
    }

    @Override
    public void endLobby(String lobbyCode) {
        raidLobbies.remove(lobbyCode);
    }

    @Override
    public GeocodedLocation getGeocodedLocation(double lat, double lon) {
        SpawnInfo info = spawnInfo.get(new SpawnPoint(lat,lon));

        return (info == null ? null : info.geocodedLocation);
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(RaidSpawn raidSpawn) {
        ConcurrentMap<String, Set<Raid>> unPausedUsers = UtilityFunctions.concurrentFilterByKey(raids, id -> {
            User user = users.get(id);
            return !(user == null || user.paused);
        });

        ArrayList<String> userIds = new ArrayList<>();

        userIds.addAll(UtilityFunctions.filterByValue(unPausedUsers,raidsSet -> {
            Stream<Raid> matchingIds = raidsSet.stream().filter(r -> r.bossId == raidSpawn.bossId);
            return matchingIds.anyMatch(raid -> raidSpawn.getSpawnLocation().intersect(raid.location));
        }).keySet());

        return userIds;
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(String preset, Spawn spawn) {
        ConcurrentMap<String, Set<Preset>> unPausedUsers = UtilityFunctions.concurrentFilterByKey(presets, id -> {
            User user = users.get(id);
            return !(user == null || user.paused);
        });

        ArrayList<String> userIds = new ArrayList<>();

        userIds.addAll(UtilityFunctions.filterByValue(unPausedUsers,presetsSet -> {
            Stream<Preset> matchingNames = presetsSet.stream().filter(p -> p.presetName.equals(preset));
            return matchingNames.anyMatch(pre -> spawn.getSpawnLocation().intersect(pre.location));
        }).keySet());

        return userIds;
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(PokeSpawn pokeSpawn) {
        ConcurrentMap<String, Set<Pokemon>> unPausedUsers = UtilityFunctions.concurrentFilterByKey(pokemons, id -> {
            User user = users.get(id);
            return !(user == null || user.paused);
        });

        ArrayList<String> userIds = new ArrayList<>();

        userIds.addAll(UtilityFunctions.filterByValue(unPausedUsers,pokeSet -> {
            Stream<Pokemon> matchingIds = pokeSet.stream().filter(pokemon -> pokemon.getID() == pokeSpawn.id);
            return matchingIds.anyMatch(poke -> {
                if (!pokeSpawn.getSpawnLocation().intersect(poke.getLocation())) return false;

                float iv = pokeSpawn.iv == null ? 0 : pokeSpawn.iv;

                if (!(iv >= poke.miniv && iv <= poke.maxiv)) return false;

                int level = pokeSpawn.level == null ? 0 : pokeSpawn.level;

                if (!(level >= poke.minlvl && level <= poke.maxlvl)) return false;

                int cp = pokeSpawn.cp == null ? 0 : pokeSpawn.cp;

                return cp >= poke.mincp && cp <= poke.maxcp;

            });
        }).keySet());

        return userIds;
    }

    @Override
    public UserPref getUserPref(String id) {
        Set<Pokemon> pokeSettings = pokemons.get(id);
        Set<Raid> raidSettings = raids.get(id);
        Set<Preset> presetSettings = presets.get(id);

        UserPref userPref = new UserPref(novaBot);

        if(pokeSettings != null) pokeSettings.forEach(userPref::addPokemon);
        if(raidSettings != null) raidSettings.forEach(userPref::addRaid);
        if(presetSettings != null) presetSettings.forEach(userPref::addPreset);

        return userPref;
    }

    @Override
    public int highestRaidLobbyId() {
        Optional optional = raidLobbies.keySet().stream().map(Integer::valueOf).max(Integer::compare);

        if (optional.isPresent()){
            return (int) optional.get();
        }else{
            return 0;
        }
    }

    @Override
    public void logNewUser(String userID) {
        users.put(userID,new User(userID));
    }

    @Override
    public void newLobby(String lobbyCode, String gymId, int memberCount, String channelId, String roleId, long nextTimeLeftUpdate, String inviteCode) {
        raidLobbies.put(lobbyCode, new DbLobby(gymId,memberCount,channelId,roleId, (int) nextTimeLeftUpdate,inviteCode));
    }

    @Override
    public boolean notContainsUser(String userID) {
        return !users.containsKey(userID);
    }

    @Override
    public void pauseUser(String id) {
        User user = users.get(id);

        if (user == null) return;

        user.paused = true;
    }

    @Override
    public void resetPokemon(String id) {
        pokemons.remove(id);
    }

    @Override
    public void resetPresets(String id) {
        presets.remove(id);
    }

    @Override
    public void resetRaids(String id) {
        raids.remove(id);
    }

    @Override
    public void resetUser(String id) {
        resetPokemon(id);
        resetRaids(id);
        resetPresets(id);
    }

    @Override
    public void setGeocodedLocation(double lat, double lon, GeocodedLocation location) {
        SpawnPoint point = new SpawnPoint(lat,lon);

        SpawnInfo info = spawnInfo.get(point);

        if (info == null){
            info = new SpawnInfo(point);
        }
        info.geocodedLocation = location;
    }

    @Override
    public void unPauseUser(String id) {
        User user = users.get(id);

        if (user == null) return;

        user.paused = false;
    }

    @Override
    public void updateLobby(String lobbyCode, int memberCount, int nextTimeLeftUpdate, String inviteCode) {
        DbLobby lobby = raidLobbies.get(lobbyCode);

        if (lobby == null) return;

        lobby.memberCount = memberCount;
        lobby.nextTimeLeftUpdate = nextTimeLeftUpdate;
        lobby.inviteCode = inviteCode;
    }

    @Override
    public int purgeUnknownSpawnpoints() {
        int oldSize = spawnInfo.size();
        UtilityFunctions.concurrentFilterByValue(spawnInfo, info ->
                info.geocodedLocation != null && !info.geocodedLocation.getProperties().get("country").equals("unkn"));

        return oldSize - spawnInfo.size();
    }

    @Override
    public ZoneId getZoneId(double lat, double lon) {
        SpawnInfo info = spawnInfo.get(new SpawnPoint(lat,lon));

        return (info == null ? null : info.zoneId);
    }

    @Override
    public void setZoneId(double lat, double lon, ZoneId zoneId) {
        SpawnPoint point = new SpawnPoint(lat,lon);
        SpawnInfo info = spawnInfo.get(point);

        if (info == null){
            info = new SpawnInfo(point);
            spawnInfo.put(point,info);
        }
        info.zoneId = zoneId;
    }

    private HashSet<String> getLocationNames(Location[] locations) {
        HashSet<String> locationNames = new HashSet<>();
        for (Location location : locations) {
            locationNames.add(location.toDbString().toLowerCase());
        }
        return locationNames;
    }
}
