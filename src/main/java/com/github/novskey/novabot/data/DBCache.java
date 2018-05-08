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
        pokemons.computeIfAbsent(userID, k -> ConcurrentHashMap.newKeySet());
        pokemons.get(userID).add(pokemon);
    }

    @Override
    public void addPreset(String userID, String preset, Location location) {
        presets.computeIfAbsent(userID, k -> ConcurrentHashMap.newKeySet());
        presets.get(userID).add(new Preset(preset,location));
    }

    @Override
    public void addRaid(String userID, Raid raid) {
        raids.computeIfAbsent(userID, k -> ConcurrentHashMap.newKeySet());
        raids.get(userID).add(raid);
    }

    @Override
    public void addUser(String userID, String botToken) {
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

        HashSet<Integer> pokemonIds = new HashSet<>();
        pokemons.forEach(p -> pokemonIds.add(p.getID()));

        if (settings != null){
            settings.removeIf(pokemon -> pokemonIds.contains(pokemon.getID()));
        }
    }

    @Override
    public void clearRaid(String id, ArrayList<Raid> raids) {
        Set<Raid> settings = this.raids.get(id);

        HashSet<Integer> bossIds = new HashSet<>();
        raids.forEach(r -> bossIds.add(r.bossId));
        if (settings != null){
            settings.removeIf(raid -> bossIds.contains(raid.bossId));
        }
    }

    @Override
    public void clearTokens(ArrayList<String> toRemove) {
        users.values().forEach(user -> {
            if(toRemove.contains(user.botToken)){
                user.botToken = null;
            }
        });
    }

    @Override
    public int countPokemon(String id, Pokemon[] potentialPokemon, boolean countLocations) {
        Set<Pokemon> settings = pokemons.get(id);

        int count;
        if (settings == null) {
            settings = new HashSet<>();
        }

        if (countLocations){
            HashSet<Pokemon> temp = new HashSet<>(settings);
            if(potentialPokemon != null) {
                temp.addAll(Arrays.asList(potentialPokemon));
            }
            count = temp.size();
        }else{
            HashSet<Pokemon> noDuplicateLocations = new HashSet<>();

            for (Pokemon pokemon : settings) {
                Pokemon noLocation = new Pokemon(pokemon.name,Location.ALL, pokemon.miniv,pokemon.maxiv, pokemon.minlvl, pokemon.maxlvl, pokemon.mincp, pokemon.maxcp);
                noDuplicateLocations.add(noLocation);
            }

            if (potentialPokemon != null){
                for (Pokemon pokemon : potentialPokemon) {
                    Pokemon noLocation = new Pokemon(pokemon.name,Location.ALL, pokemon.miniv,pokemon.maxiv, pokemon.minlvl, pokemon.maxlvl, pokemon.mincp, pokemon.maxcp);
                    noDuplicateLocations.add(noLocation);
                }
            }

            count = noDuplicateLocations.size();
        }
        return count;
    }

    @Override
    public int countPresets(String userID, ArrayList<Preset> potentialPresets, boolean countLocations) {
        Set<Preset> settings = this.presets.get(userID);

        int count = 0;

        if (settings == null) {
            settings = new HashSet<>();
        }
        if (countLocations){
            HashSet<Preset> temp = new HashSet<>(settings);
            if(potentialPresets != null) {
                temp.addAll(potentialPresets);
            }
            count = temp.size();
        }else{
            HashSet<Preset> noDuplicateLocations = new HashSet<>();

            for (Preset preset : settings) {
                Preset noLocation = new Preset(preset.presetName, Location.ALL);
                noDuplicateLocations.add(noLocation);
            }

            if (potentialPresets != null){
                for (Preset preset : potentialPresets) {
                    Preset noLocation = new Preset(preset.presetName, Location.ALL);
                    noDuplicateLocations.add(noLocation);
                }
            }

            count = noDuplicateLocations.size();
        }
        return count;
    }

    @Override
    public int countRaids(String id, Raid[] potentialRaids, boolean countLocations) {
        Set<Raid> settings = raids.get(id);

        int count = 0;

        if (settings == null) {
            settings = new HashSet<>();
        }
        if (countLocations){
            HashSet<Raid> temp = new HashSet<>(settings);
            if (potentialRaids != null) {
                temp.addAll(Arrays.asList(potentialRaids));
            }
            count = temp.size();
        }else{
            HashSet<Raid> noDuplicateLocations = new HashSet<>();

            for (Raid raid : settings) {
                Raid noLocation = new Raid(raid.bossId, Location.ALL);
                noDuplicateLocations.add(noLocation);
            }

            if (potentialRaids != null){
                for (Raid raid : potentialRaids) {
                    Raid noLocation = new Raid(raid.bossId, Location.ALL);
                    noDuplicateLocations.add(noLocation);
                }
            }

            count = noDuplicateLocations.size();
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
    public User getUser(String id) {
        return users.get(id);
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(RaidSpawn raidSpawn) {
        ConcurrentMap<String, Set<Raid>> unPausedUsers = UtilityFunctions.concurrentFilterByKey(raids, id -> {
            User user = users.get(id);
            return !(user == null || user.paused);
        });

        return new ArrayList<>(UtilityFunctions.filterByValue(unPausedUsers, raidsSet -> {
            Stream<Raid> matchingIds = raidsSet.stream().filter(raidSetting ->
                                                                        (raidSetting.gymName.equals("") ||
                                                                         raidSetting.gymName.equalsIgnoreCase(raidSpawn.getProperties().get("gym_name"))) &&
                                                                        ((raidSpawn.bossId == 0 &&
                                                                         raidSetting.eggLevel == raidSpawn.raidLevel) ||
                                                                         (raidSpawn.bossId != 0 &&
                                                                         ((raidSetting.bossId == raidSpawn.bossId) ||
                                                                          (raidSetting.raidLevel == raidSpawn.raidLevel) ||
                                                                          (raidSetting.eggLevel == raidSpawn.raidLevel))))
            );
            return matchingIds.anyMatch(raid -> raidSpawn.getSpawnLocation ().intersect(raid.location));
        }).keySet());
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(String preset, Spawn spawn) {
        ConcurrentMap<String, Set<Preset>> unPausedUsers = UtilityFunctions.concurrentFilterByKey(presets, id -> {
            User user = users.get(id);
            return !(user == null || user.paused);
        });

        return new ArrayList<>(UtilityFunctions.filterByValue(unPausedUsers, presetsSet -> {
            Stream<Preset> matchingNames = presetsSet.stream().filter(p -> p.presetName.equals(preset));
            return matchingNames.anyMatch(pre -> spawn.getSpawnLocation().intersect(pre.location));
        }).keySet());
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(PokeSpawn pokeSpawn) {
        ConcurrentMap<String, Set<Pokemon>> unPausedUsers = UtilityFunctions.concurrentFilterByKey(pokemons, id -> {
            User user = users.get(id);
            return !(user == null || user.paused);
        });

        return new ArrayList<>(UtilityFunctions.filterByValue(unPausedUsers, pokeSet -> {
            Stream<Pokemon> matchingIds = pokeSet.stream().filter(
                    pokemonSetting ->
                            (pokemonSetting.getID() == pokeSpawn.id));
            return matchingIds.anyMatch(pokemonSetting -> {
                if (!pokeSpawn.getSpawnLocation().intersect(pokemonSetting.getLocation())) return false;

                float iv = pokeSpawn.iv == null ? 0 : pokeSpawn.iv;

                if (!(iv >= pokemonSetting.miniv && iv <= pokemonSetting.maxiv)) return false;

                int level = pokeSpawn.level == null ? 0 : pokeSpawn.level;

                if (!(level >= pokemonSetting.minlvl && level <= pokemonSetting.maxlvl)) return false;

                int cp = pokeSpawn.cp == null ? 0 : pokeSpawn.cp;

                return cp >= pokemonSetting.mincp && cp <= pokemonSetting.maxcp;

            });
        }).keySet());
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
    public void setBotToken(String id, String botToken) {
        users.get(id).setBotToken(botToken);
    }

    @Override
    public void setGeocodedLocation(double lat, double lon, GeocodedLocation location) {
        SpawnPoint point = new SpawnPoint(lat,lon);

        SpawnInfo info = spawnInfo.get(point);

        if (info == null){
            info = new SpawnInfo(point);
            spawnInfo.put(point,info);
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
