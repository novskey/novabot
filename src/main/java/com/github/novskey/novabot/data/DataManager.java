package com.github.novskey.novabot.data;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.*;
import com.github.novskey.novabot.maps.GeocodedLocation;
import com.github.novskey.novabot.pokemon.PokeSpawn;
import com.github.novskey.novabot.pokemon.Pokemon;
import com.github.novskey.novabot.raids.Raid;
import com.github.novskey.novabot.raids.RaidLobby;
import com.github.novskey.novabot.raids.RaidSpawn;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Paris on 18/01/2018.
 */
public class DataManager implements IDataBase {

    private final NovaBot novaBot;
    public final SettingsDBManager settingsDbManager;
    private final DBCache dbCache;


    public static final String MySQL_DRIVER = "com.mysql.jdbc.Driver";
    public static final String PgSQL_DRIVER = "org.postgresql.Driver";
    private final HashSet<ScanDBManager> scanDBManagers = new HashSet<>();


    public static RotatingSet<Integer> hashCodes = null;
    public ConcurrentHashMap<String, RaidSpawn> knownRaids = null;

    public synchronized void addHashCode(int hashCode) {
        hashCodes.syncAdd(hashCode);
    }

    public synchronized boolean containsHashCode(int hashCode) {
        return hashCodes.contains(hashCode);
    }

    public HashSet<String> getGymNames() {
        HashSet<String> gymNames = new HashSet<>();

        scanDBManagers.forEach(db -> gymNames.addAll(db.getGymNames()));

        return gymNames;
    }

    public RotatingSet<Integer> getHashCodes() {
        return hashCodes;
    }

    public ConcurrentHashMap<String,RaidSpawn> getKnownRaids() {
        return knownRaids;
    }

    public Map<String, Integer> getTokenUses() {
        HashMap<String, Integer> tokenUses = new HashMap<>();

        for (User user : dbCache.users.values()) {
            if (user.getBotToken() != null){
                tokenUses.merge(user.getBotToken(), 1, Integer::sum);
            }
        }

        return tokenUses;
    }

    public static void main(String[] args) {
        NovaBot novaBot = new NovaBot();
        novaBot.setup();

        System.out.println(novaBot.dataManager.getUserIDsToNotify(new PokeSpawn(
                201,
                -35.265134, 149.122796,
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(60), UtilityFunctions.UTC),
                15,
                15,
                15,
                18,
                22,
                0,
                0,
                0,
                1,
                200,
                .1)));
    }

    public DataManager(NovaBot novaBot, HashSet<ScannerDb> scannerDbs){
        this.novaBot = novaBot;

        settingsDbManager = new SettingsDBManager(novaBot);
        settingsDbManager.novabotdbConnect();

        hashCodes = new RotatingSet<>(novaBot.getConfig().getMaxStoredHashes(), ConcurrentHashMap.newKeySet(novaBot.getConfig().getMaxStoredHashes()));
        knownRaids = new ConcurrentHashMap<>();

        int scannerDbId = 1;
        for (ScannerDb scannerDb : scannerDbs) {
            scanDBManagers.add(new ScanDBManager(novaBot,scannerDb,scannerDbId));
            scannerDbId++;
        }

        dbCache = new DBCache(novaBot);
        dbCache.users = settingsDbManager.dumpUsers();
        dbCache.pokemons = settingsDbManager.dumpPokemon();
        dbCache.raids = settingsDbManager.dumpRaids();
        dbCache.presets = settingsDbManager.dumpPresets();
        dbCache.raidLobbies = settingsDbManager.dumpRaidLobbies();
        dbCache.spawnInfo = settingsDbManager.dumpSpawnInfo();
    }

    @Override
    public void addPokemon(String userID, Pokemon pokemon) {
        dbCache.addPokemon(userID,pokemon);
        settingsDbManager.addPokemon(userID, pokemon);
    }

    @Override
    public void addPreset(String userID, String preset, Location location) {
        dbCache.addPreset(userID,preset,location);
        settingsDbManager.addPreset(userID, preset, location);
    }

    @Override
    public void addRaid(String userID, Raid raid) {
        dbCache.addRaid(userID,raid);
        settingsDbManager.addRaid(userID, raid);
    }

    @Override
    public void addUser(String userID, String botToken) {
        dbCache.addUser(userID, botToken);
        settingsDbManager.addUser(userID, botToken);
    }

    @Override
    public void clearPreset(String id, String[] presets) {
        dbCache.clearPreset(id, presets);
        settingsDbManager.clearPreset(id, presets);
    }

    @Override
    public void clearLocationsPresets(String id, Location[] locations) {
        dbCache.clearLocationsPresets(id, locations);
        settingsDbManager.clearLocationsPresets(id, locations);
    }

    @Override
    public void clearLocationsPokemon(String id, Location[] locations) {
        dbCache.clearLocationsPokemon(id, locations);
        settingsDbManager.clearLocationsPokemon(id, locations);
    }

    @Override
    public void clearLocationsRaids(String id, Location[] locations) {
        dbCache.clearLocationsRaids(id, locations);
        settingsDbManager.clearLocationsRaids(id, locations);
    }

    @Override
    public void clearPokemon(String id, ArrayList<Pokemon> pokemons) {
        dbCache.clearPokemon(id, pokemons);
        settingsDbManager.clearPokemon(id, pokemons);
    }

    @Override
    public void clearRaid(String id, ArrayList<Raid> raids) {
        dbCache.clearRaid(id, raids);
        settingsDbManager.clearRaid(id, raids);
    }

    @Override
    public void clearTokens(ArrayList<String> toRemove) {
        dbCache.clearTokens(toRemove);
        settingsDbManager.clearTokens(toRemove);
    }

    @Override
    public int countPokemon(String id, Pokemon[] potentialPokemon, boolean countLocations) {
        return dbCache.countPokemon(id, potentialPokemon, countLocations);
    }

    @Override
    public int countPresets(String userID, ArrayList<Preset> potentialPresets, boolean countLocations) {
        return dbCache.countPresets(userID, potentialPresets, countLocations);
    }

    @Override
    public int countRaids(String id, Raid[] potentialRaids, boolean countLocations) {
        return dbCache.countRaids(id, potentialRaids, countLocations);
    }

    @Override
    public void deletePokemon(String userID, Pokemon pokemon) {
        dbCache.deletePokemon(userID,pokemon);
        settingsDbManager.deletePokemon(userID, pokemon);
    }

    @Override
    public void deletePreset(String userId, String preset, Location location) {
        dbCache.deletePreset(userId, preset, location);
        settingsDbManager.deletePreset(userId, preset, location);
    }

    @Override
    public void deleteRaid(String userID, Raid raid) {
        dbCache.deleteRaid(userID, raid);
        settingsDbManager.deleteRaid(userID, raid);
    }

    @Override
    public void endLobby(String lobbyCode) {
        dbCache.endLobby(lobbyCode);
        settingsDbManager.endLobby(lobbyCode);
    }

    @Override
    public GeocodedLocation getGeocodedLocation(double lat, double lon) {
        return dbCache.getGeocodedLocation(lat, lon);
    }

    @Override
    public User getUser(String id) {
        return dbCache.getUser(id);
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(RaidSpawn raidSpawn) {
        return dbCache.getUserIDsToNotify(raidSpawn);
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(String preset, Spawn spawn) {
        return dbCache.getUserIDsToNotify(preset, spawn);
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(PokeSpawn pokeSpawn) {
        return dbCache.getUserIDsToNotify(pokeSpawn);
    }

    @Override
    public UserPref getUserPref(String id) {
        return dbCache.getUserPref(id);
    }

    @Override
    public int highestRaidLobbyId() {
        return dbCache.highestRaidLobbyId();
    }

    @Override
    public void logNewUser(String userID) {
        dbCache.logNewUser(userID);
        settingsDbManager.logNewUser(userID);
    }

    @Override
    public void newLobby(String lobbyCode, String gymId, int memberCount, String channelId, String roleId, long nextTimeLeftUpdate, String inviteCode) {
        dbCache.newLobby(lobbyCode, gymId, memberCount, channelId, roleId, nextTimeLeftUpdate, inviteCode);
        settingsDbManager.newLobby(lobbyCode, gymId, memberCount, channelId, roleId, nextTimeLeftUpdate, inviteCode);
    }

    @Override
    public boolean notContainsUser(String userID) {
        return dbCache.notContainsUser(userID);
    }

    @Override
    public void pauseUser(String id) {
        dbCache.pauseUser(id);
        settingsDbManager.pauseUser(id);
    }

    @Override
    public void resetPokemon(String id) {
        dbCache.resetPokemon(id);
        settingsDbManager.resetPokemon(id);
    }

    @Override
    public void resetPresets(String id) {
        dbCache.resetPresets(id);
        settingsDbManager.resetPresets(id);
    }

    @Override
    public void resetRaids(String id) {
        dbCache.resetRaids(id);
        settingsDbManager.resetRaids(id);
    }

    @Override
    public void resetUser(String id) {
        dbCache.resetUser(id);
        settingsDbManager.resetUser(id);
    }

    @Override
    public void setBotToken(String id, String botToken) {
        dbCache.setBotToken(id,botToken);
        settingsDbManager.setBotToken(id,botToken);
    }

    @Override
    public void setGeocodedLocation(double lat, double lon, GeocodedLocation location) {
        dbCache.setGeocodedLocation(lat, lon, location);
        settingsDbManager.setGeocodedLocation(lat, lon, location);
    }

    @Override
    public void unPauseUser(String id) {
        dbCache.unPauseUser(id);
        settingsDbManager.unPauseUser(id);
    }

    @Override
    public void updateLobby(String lobbyCode, int memberCount, int nextTimeLeftUpdate, String inviteCode) {
        dbCache.updateLobby(lobbyCode, memberCount, nextTimeLeftUpdate, inviteCode);
        settingsDbManager.updateLobby(lobbyCode, memberCount, nextTimeLeftUpdate, inviteCode);
    }

    @Override
    public int purgeUnknownSpawnpoints() {
        return dbCache.purgeUnknownSpawnpoints();
    }

    @Override
    public ZoneId getZoneId(double lat, double lon) {
        return dbCache.getZoneId(lat, lon);
    }

    @Override
    public void setZoneId(double lat, double lon, ZoneId zoneId) {
        dbCache.setZoneId(lat, lon, zoneId);
        settingsDbManager.setZoneId(lat, lon, zoneId);
    }

    public int countSpawns(int id, TimeUnit timeUnit, int intervalLength) {
        int sum = 0;

        for (ScanDBManager scanDBManager : scanDBManagers) {
            sum += scanDBManager.countSpawns(id,timeUnit,intervalLength);
        }

        return sum;
    }

    public ArrayList<RaidLobby> getActiveLobbies() {
        return settingsDbManager.getActiveLobbies();
    }

    public void getNewPokemon() {
        for (ScanDBManager scanDBManager : scanDBManagers) {
            new Thread(scanDBManager::getNewPokemon).start();
        }
    }

    public void getCurrentRaids(boolean firstRun) {
        for (ScanDBManager scanDBManager : scanDBManagers) {
            new Thread(() -> scanDBManager.getCurrentRaids(firstRun)).start();
        }
    }
}
