package com.github.novskey.novabot.core;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import com.github.novskey.novabot.maps.Geofencing;
import com.github.novskey.novabot.notifier.PokeNotificationSender;
import com.github.novskey.novabot.notifier.RaidNotificationSender;
import com.github.novskey.novabot.pokemon.PokeSpawn;
import com.github.novskey.novabot.pokemon.Pokemon;
import com.github.novskey.novabot.raids.RaidSpawn;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.maps.GeoApiContext;
import net.dv8tion.jda.core.entities.*;
import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.ZoneId;
import java.util.*;

/**
 * Created by Owner on 13/05/2017.
 */
@lombok.Data
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private static final String[] formatKeys = new String[]{"pokemon", "raidEgg", "raidBoss"};
    private static final String[] formattingVars = new String[]{"title", "titleUrl", "body", "content", "showMap", "mapZoom", "mapWidth", "mapHeight"};
    public final HashMap<String, JsonObject> pokeFilters = new HashMap<>();
    public final HashMap<String, JsonObject> raidFilters = new HashMap<>();
    private final HashMap<String, NotificationLimit> roleLimits = new HashMap<>();
    private final HashMap<String, Format> formats = new HashMap<>();
    private AlertChannels pokeChannels;
    private final AlertChannels raidChannels = new AlertChannels();
    public HashMap<String, String> presets = new HashMap<>();
    public ArrayList<Integer> raidBosses = new ArrayList<>(Arrays.asList(2, 5, 8, 11, 28, 31, 34, 38, 62, 65, 68, 71, 73, 76, 82, 91, 94, 105, 123, 129, 131, 137, 139, 143, 144, 145, 146, 150, 243, 244, 245, 248, 249, 302, 303, 359));
    private ArrayList<Integer> blacklist = new ArrayList<>();
    private ArrayList<String> notificationTokens = new ArrayList<>();
    private boolean logging = false;
    private boolean stats = true;
    private boolean startupMessage = false;
    private boolean countLocationsInLimits = true;
    private ScannerType scannerType = ScannerType.RocketMap;
    private boolean useScanDb = true;
    private boolean raidsEnabled = true;
    private boolean pokemonEnabled = true;
    private boolean raidOrganisationEnabled = true;
    private boolean useGoogleTimeZones = false;
    private boolean allowAllLocation = true;
    private String token = null;
    private ZoneId timeZone = ZoneId.systemDefault();
    private Integer minSecondsLeft = 60;
    private String footerText = null;
    private String googleSuburbField = "city";
    private String adminRole = null;
    private String commandChannelId = null;
    private String novabotRoleId = null;
    private String roleLogId = null;
    private String userUpdatesId = null;
    private String raidLobbyCategory = null;
    private String scanUser;
    private String scanPass;
    private String scanIp;
    private String scanPort = "3306";
    private String scanDbName;
    private String scanProtocol = "mysql";
    private String scanUseSSL = "false";
    private String nbUser;
    private String nbPass;
    private String nbIp;
    private String nbPort = "3306";
    private String nbDbName;
    private String nbProtocol = "mysql";
    private String nbUseSSL = "false";
    private long pokePollingDelay = 2;
    private long raidPollingDelay = 15;
    private int pokemonThreads = 2;
    private int raidThreads = 2;
    private int dbThreads = 2;
    private int maxStoredMessages = 1000000;
    private int maxStoredHashes = 500000;
    private NotificationLimit nonSupporterLimit = new NotificationLimit(null, null, null);
    private ArrayList<String> geocodingKeys = new ArrayList<>();
    private ArrayList<String> timeZoneKeys = new ArrayList<>();
    private ArrayList<String> staticMapKeys = new ArrayList<>();
    private HashMap<GeofenceIdentifier, String> raidChats = new HashMap<>();
    private HashMap<String, GeoApiContext> geoApis = new HashMap<>();
    private int nbMaxConnections = 8;
    private int scanMaxConnections = 8;

    public Config(String configName, String gkeys, String formatting, String raidChannelFile, String pokeChannelFile,
                  String supporterLevelFile, String presetsFile) {

        Ini configIni = null;
        try {
            configIni = new Ini(new File(configName));
        } catch (IOException e) {
            log.error(String.format("Couldn't find config file %s, aborting", configName));
            System.exit(1);
        }

        log.info("Configuring...");

        log.info(String.format("Loading %s...", configName));

        loadBaseConfig(configName, configIni);

        loadScannerDbConfig(configIni);

        loadNovaBotDbConfig(configIni);

        log.info("Finished loading " + configName);

        log.info(String.format("Loading %s...", gkeys));
        geocodingKeys = loadKeys(new File(gkeys));

        geoApis.clear();
        for (String s : geocodingKeys) {
            GeoApiContext api = new GeoApiContext();
            api.setApiKey(s);
            geoApis.put(s,api);
        }

        timeZoneKeys.clear();
        timeZoneKeys.addAll(geocodingKeys);
        staticMapKeys.clear();
        staticMapKeys.addAll(geocodingKeys);
        log.info("Finished loading " + gkeys);

        log.info(String.format("Loading %s...", formatting));
        loadFormatting(formatting);
        log.info("Finished loading " + formatting);


        if (raidsEnabled()) {
            log.info(String.format("Loading %s...", raidChannelFile));
            loadRaidChannels(raidChannelFile, formatting);
            log.info("Finished loading " + raidChannelFile);
        }

        log.info(String.format("Loading %s...", supporterLevelFile));
        loadSupporterRoles(supporterLevelFile);
        log.info("Finished loading " + supporterLevelFile);

        if (pokemonEnabled()) {
            log.info(String.format("Loading %s...", pokeChannelFile));
            pokeChannels = loadPokemonChannels(pokeChannelFile, formatting);
            log.info("Finished loading " + pokeChannelFile);
        } else {
            pokeChannels = new AlertChannels();
        }

        log.info(String.format("Loading %s...", presetsFile));
        loadPresets(presetsFile);
        log.info("Finished loading " + presetsFile);

        log.info("Finished configuring");
    }

    private void loadNovaBotDbConfig(Ini configIni) {
        Ini.Section novabotDb = configIni.get("novabot db");
        nbUser = novabotDb.get("user", nbUser);
        nbPass = novabotDb.get("password", nbPass);
        nbIp = novabotDb.get("ip", nbIp);
        nbPort = novabotDb.get("port", nbPort);
        nbDbName = novabotDb.get("dbName", nbDbName);
        nbProtocol = novabotDb.get("protocol", nbProtocol);
        nbUseSSL = novabotDb.get("useSSL",nbUseSSL);
    }

    private void loadScannerDbConfig(Ini configIni) {
        Ini.Section scannerDb = configIni.get("scanner db");
        scanUser = scannerDb.get("user", scanUser);
        scanPass = scannerDb.get("password", scanPass);
        scanIp = scannerDb.get("ip", scanIp);
        scanPort = scannerDb.get("port", scanPort);
        scanDbName = scannerDb.get("dbName", scanDbName);
        scanProtocol = scannerDb.get("protocol", scanProtocol);
        scanUseSSL = scannerDb.get("useSSL",scanUseSSL);
        scanMaxConnections = scannerDb.get("maxConnections",Integer.class,scanMaxConnections);
        nbMaxConnections = scannerDb.get("maxConnections",Integer.class,nbMaxConnections);

    }

    private void loadBaseConfig(String configName, Ini configIni) {
        Ini.Section config = configIni.get("config");

        token = config.get("token", token);

        if (token == null) {
            log.error(String.format("Couldn't find token in %s. novabot can't run without a bot token.", configName));
            System.exit(1);
        }

        String notificationTokensStr = config.get("notificationTokens","[]");

        notificationTokens = UtilityFunctions.parseList(notificationTokensStr);

        String blacklistStr = config.get("blacklist", "[]");

        blacklist.clear();
        UtilityFunctions.parseList(blacklistStr).forEach(str -> blacklist.add(Integer.valueOf(str)));

        String raidBossStr = config.get("raidBosses", "[2, 5, 8, 11, 28, 31, 34, 38, 62, 65, 68, 71, 73, 76, 82, 91, 94, 105, 123, 129, 131, 137, 139, 143, 144, 145, 146, 150, 243, 244, 245, 248, 249, 302, 303, 359]");

        raidBosses.clear();
        UtilityFunctions.parseList(raidBossStr).forEach(str -> raidBosses.add(Integer.valueOf(str)));

        useScanDb = config.get("useScanDb", Boolean.class, useScanDb);

        allowAllLocation = config.get("allowAllLocation", Boolean.class, allowAllLocation);

        scannerType = ScannerType.fromString(config.get("scannerType",scannerType.toString()));

        googleSuburbField = config.get("googleSuburbField", googleSuburbField);

        raidsEnabled = config.get("raids", Boolean.class, raidsEnabled);

        raidOrganisationEnabled = config.get("raidOrganisation", Boolean.class, raidOrganisationEnabled);

        pokemonEnabled = config.get("pokemon", Boolean.class, pokemonEnabled);

        pokePollingDelay = config.get("pokePollingDelay", Long.class, pokePollingDelay);

        pokemonThreads = config.get("pokemonThreads", Integer.class, pokemonThreads);

        raidPollingDelay = config.get("raidPollingDelay", Long.class, raidPollingDelay);

        raidThreads = config.get("raidThreads", Integer.class, raidThreads);

        dbThreads = config.get("dbThreads",Integer.class, dbThreads);

        nonSupporterLimit = NotificationLimit.fromString(config.get("nonSupporterLimit", "[n,n,n]"));

        countLocationsInLimits = config.get("countLocationsInLimits", Boolean.class, countLocationsInLimits);

        logging = config.get("logging", Boolean.class, logging);


        if (logging) {
            maxStoredMessages = config.get("maxStoredMessages",Integer.class, maxStoredMessages);

            roleLogId = config.get("roleLogChannel", roleLogId);

            userUpdatesId = config.get("userUpdatesChannel", userUpdatesId);
        }

        minSecondsLeft = config.get("minSecondsLeft",Integer.class, minSecondsLeft);

        maxStoredHashes = config.get("maxStoredHashes",Integer.class, maxStoredHashes);

        useGoogleTimeZones = config.get("useGoogleTimeZones", Boolean.class, useGoogleTimeZones);

        timeZone = ZoneId.of(config.get("timezone", String.valueOf(timeZone)));

        footerText = config.get("footerText", footerText);

        stats = config.get("stats", Boolean.class, stats);

        startupMessage = config.get("startupMessage", Boolean.class, startupMessage);

        adminRole = config.get("adminRole", adminRole);

        if (adminRole == null) {
            log.warn(String.format("Couldn't find adminRole in %s. !reload command won't work unless an adminRole is specified.", configName));
        }

        novabotRoleId = config.get("novabotRole", novabotRoleId);

        if (novabotRoleId == null) {
            log.warn(String.format("Couldn't find novabotRoleId in %s. A novabotRoleId must be specified in order to use raid organisation.", configName));
            if (!raidOrganisationEnabled) {
                log.error("Raid organisation enabled with no novabotRoleId");
                System.exit(1);
            }
        }

        commandChannelId = config.get("commandChannel", commandChannelId);

        if (commandChannelId == null) {
            log.warn("Couldn't find commandChannel in %s. novabot will only be able to accept commands in DM.", configName);
        }

        raidLobbyCategory = config.get("raidLobbyCategory",raidLobbyCategory);
    }

    public boolean countLocationsInLimits() {
        return countLocationsInLimits;
    }

    public ArrayList<String> findMatchingPresets(RaidSpawn raidSpawn) {
        ArrayList<String> matching = new ArrayList<>();

        for (Map.Entry<String, String> entry : presets.entrySet()) {
            if (matchesFilter(raidFilters.get(entry.getValue()), raidSpawn)) {
                matching.add(entry.getKey());
            }
        }
        return matching;
    }

    public ArrayList<String> findMatchingPresets(PokeSpawn pokeSpawn) {
        ArrayList<String> matching = new ArrayList<>();

        for (Map.Entry<String, String> entry : presets.entrySet()) {
            JsonObject filter = pokeFilters.get(entry.getValue());
            if (filter != null && matchesFilter(filter, pokeSpawn, entry.getValue())) {
                matching.add(entry.getKey());
            }
        }
        return matching;
    }

    public String formatStr(HashMap<String, String> properties, String toFormat) {
        if (toFormat == null) return toFormat;

        final String[] str = {toFormat};

        for (Map.Entry<String, String> stringStringEntry : properties.entrySet()) {
            if(stringStringEntry.getValue() == null || stringStringEntry.getKey() == null) continue;
            str[0] = str[0].replace(String.format("<%s>", stringStringEntry.getKey()), stringStringEntry.getValue());
        }

        return str[0];
    }

    public String getBodyFormatting(String fileName, String formatKey) {
        return formats.get(fileName).getFormatting(formatKey, "body");
    }

    public String getContentFormatting(String fileName, String formatKey) {
        return formats.get(fileName).getFormatting(formatKey, "content");
    }

    public String getEncounterBodyFormatting(String fileName) {
        return formats.get(fileName).getFormatting("pokemon", "encounteredBody");
    }

    public String getEncounterTitleFormatting(String fileName) {
        return formats.get(fileName).getFormatting("pokemon", "encounteredTitle");
    }

    public String getMapHeight(String fileName, String formatKey) {
        return formats.get(fileName).getFormatting(formatKey, "mapHeight");
    }

    public String getMapWidth(String fileName, String formatKey) {
        return formats.get(fileName).getFormatting(formatKey, "mapWidth");
    }

    public String getMapZoom(String fileName, String formatKey) {
        return formats.get(fileName).getFormatting(formatKey, "mapZoom");
    }

    public ArrayList<AlertChannel> getNonGeofencedPokeChannels() {
        return pokeChannels.getNonGeofencedChannels();
    }

    public ArrayList<AlertChannel> getNonGeofencedRaidChannels() {
        return raidChannels.getNonGeofencedChannels();
    }

    public NotificationLimit getNotificationLimit(Member member) {
        NotificationLimit largest = nonSupporterLimit;
        for (Role role : member.getRoles()) {
            NotificationLimit notificationLimit = roleLimits.get(role.getId());
            if (notificationLimit != null && notificationLimit.sumSize > largest.sumSize){
                largest = notificationLimit;
            }
        }
        return largest;
    }

    public ArrayList<AlertChannel> getPokeChannels(GeofenceIdentifier identifier) {
        return pokeChannels.getChannelsByGeofence(identifier);
    }

    public String getPresetsList() {
        StringBuilder list = new StringBuilder("```");

        for (String presetName : presets.keySet()) {
            list.append(String.format("  %s%n", presetName));
        }

        list.append("```");
        return list.toString();
    }

    public ArrayList<AlertChannel> getRaidChannels(GeofenceIdentifier identifier) {
        return raidChannels.getChannelsByGeofence(identifier);
    }

    public ArrayList<GeofenceIdentifier> getRaidChatGeofences(String id) {
        ArrayList<GeofenceIdentifier> geofenceIdentifiers = new ArrayList<>();

        for (Map.Entry<GeofenceIdentifier, String> entry : raidChats.entrySet()) {
            if (entry.getValue().equals(id)) {
                geofenceIdentifiers.add(entry.getKey());
            }
        }

        return geofenceIdentifiers;
    }

    public String[] getRaidChats(ArrayList<GeofenceIdentifier> geofences) {
        HashSet<String> chatIds = new HashSet<>();

        for (Map.Entry<GeofenceIdentifier, String> entry : raidChats.entrySet()) {
            boolean added = false;
            for (GeofenceIdentifier geofence : geofences) {
                if (added) break;
                if (entry.getKey().equals(geofence)) {
                    chatIds.add(entry.getValue());
                    added = true;
                }
            }
        }

        String[] chatIdStrings = new String[chatIds.size()];
        return chatIds.toArray(chatIdStrings);
    }

    public List<String> getSupporterRoles() {
        String[] rolesArr = new String[roleLimits.size()];
        roleLimits.keySet().toArray(rolesArr);
        return Arrays.asList(rolesArr);
    }

    public String getTitleFormatting(String fileName, String formatKey) {
        return formats.get(fileName).getFormatting(formatKey, "title");
    }

    public String getTitleUrl(String fileName, String formatKey) {
        return formats.get(fileName).getFormatting(formatKey, "titleUrl");
    }

    public boolean isRaidChannel(String id) {
        for (String s : raidChats.values()) {
            if (id.equals(s)) return true;
        }
        return false;
    }

    public boolean isRaidChannelsEnabled() {
        return raidChannels.size() > 0;
    }



    public boolean loggingEnabled() {
        return logging;
    }

    public static void main(String[] args) {
        NovaBot novaBot = new NovaBot();
        novaBot.setup();

        PokeSpawn pokeSpawn = new PokeSpawn(248);
        System.out.println(novaBot.config.matchesFilter(novaBot.config.pokeFilters.get("ultrarare.json"),pokeSpawn,"ultrarare.json"));

    }

    public boolean matchesFilter(JsonObject filter, RaidSpawn raidSpawn) {
        RaidNotificationSender.notificationLog.info("Filter: " + filter);
        String searchStr = raidSpawn.gymId;

        JsonElement raidFilter = searchFilter(filter,searchStr);
        RaidNotificationSender.notificationLog.info(searchStr + ": " + raidFilter);

        if (raidFilter == null) {
            RaidNotificationSender.notificationLog.info(String.format("couldn't find filter for '%s'",searchStr));
            searchStr = raidSpawn.properties.get("gym_name");
            raidFilter = searchFilter(filter, searchStr);
            RaidNotificationSender.notificationLog.info(searchStr + ": " + raidFilter);

            if (raidFilter == null) {
                RaidNotificationSender.notificationLog.info(String.format("couldn't find filter for '%s'", searchStr));

                searchStr = "Default";
                raidFilter = searchFilter(filter, searchStr);
                RaidNotificationSender.notificationLog.info(searchStr + ": " + raidFilter);

                if (raidFilter == null){
                    RaidNotificationSender.notificationLog.info("no default block in filter, moving on");
                    return false;
                }
            }
        }

        if (raidFilter.isJsonObject()) {
            searchStr = (raidSpawn.bossId >= 1) ? Pokemon.getFilterName(raidSpawn.bossId) : "Egg" + raidSpawn.raidLevel;

            JsonElement subFilter = searchFilter(raidFilter.getAsJsonObject(),searchStr);
            RaidNotificationSender.notificationLog.info(searchStr + ": " + subFilter);

            if (subFilter != null){
                if (subFilter.getAsBoolean()) {
                    RaidNotificationSender.notificationLog.info(String.format("Raid enabled in filter block '%s', posting to discord", searchStr));
                    return true;
                }else {
                    RaidNotificationSender.notificationLog.info(String.format("Raid not enabled in filter block '%s', ignoring spawn", searchStr));
                    return false;
                }
            } else {
                subFilter = searchFilter(raidFilter.getAsJsonObject(),"Level"+raidSpawn.raidLevel);
                RaidNotificationSender.notificationLog.info(searchStr + ": " + subFilter);

                if(subFilter != null && subFilter.getAsBoolean()){
                    RaidNotificationSender.notificationLog.info(String.format("Raid enabled in filter block '%s', posting to discord", "Level"+ raidSpawn.raidLevel));
                    return true;
                }else {
                    RaidNotificationSender.notificationLog.info(String.format("Raid not enabled in filter block '%s', ignoring spawn", "Level"+ raidSpawn.raidLevel));
                    return false;
                }
            }
        } else {
            if (raidFilter.getAsBoolean()) {
                RaidNotificationSender.notificationLog.info(String.format("Raid enabled in filter block '%s', posting to discord", searchStr));
                return true;
            }else{
                RaidNotificationSender.notificationLog.info(String.format("Raid not enabled in filter block '%s', ignoring spawn", searchStr));
                return false;
            }
        }
    }

    public boolean matchesFilter(JsonObject filter, PokeSpawn pokeSpawn, String filterName) {
        JsonElement pokeFilter = searchFilter(filter, UtilityFunctions.capitaliseFirst(Pokemon.getFilterName(pokeSpawn.getFilterId())));
        if (pokeFilter == null) {
            PokeNotificationSender.notificationLog.info(String.format("pokeFilter %s is null for %s", filterName, pokeSpawn.properties.get("pkmn")));
            pokeFilter = searchFilter(filter, "Default");

            if (pokeFilter == null) {
                return false;
            }
        }

        if (pokeFilter.isJsonArray()) {
            JsonArray array = pokeFilter.getAsJsonArray();
            for (JsonElement element : array) {
                if (processElement(element, pokeSpawn,filterName)) return true;
            }
        }
        return processElement(pokeFilter,pokeSpawn,filterName);
    }

    private boolean processElement(JsonElement pokeFilter, PokeSpawn pokeSpawn, String filterName) {
        if (pokeFilter.isJsonObject()) {
            JsonObject obj = pokeFilter.getAsJsonObject();

            JsonElement maxObj = obj.get("max_iv");
            JsonElement minObj = obj.get("min_iv");

            float max = maxObj == null ? Integer.MAX_VALUE : maxObj.getAsFloat();
            float min = minObj == null ? Integer.MIN_VALUE : minObj.getAsFloat();

            if ((pokeSpawn.iv == null ? -1 : pokeSpawn.iv) <= max && (pokeSpawn.iv == null ? -1 : pokeSpawn.iv) >= min) {
                PokeNotificationSender.notificationLog.info(String.format("Pokemon between specified ivs (%s,%s)", infOrNum(min), infOrNum(max)));
            } else {
                PokeNotificationSender.notificationLog.info(String.format("Pokemon (%s%%) not between specified ivs (%s,%s). filter %s", pokeSpawn.iv, infOrNum(min), infOrNum(max), filterName));
                return false;
            }

            maxObj = obj.get("max_cp");
            minObj = obj.get("min_cp");

            max = maxObj == null ? Integer.MAX_VALUE : maxObj.getAsFloat();
            min = minObj == null ? Integer.MIN_VALUE : minObj.getAsFloat();

            if ((pokeSpawn.cp == null ? -1 : pokeSpawn.cp) <= max && (pokeSpawn.cp == null ? -1 : pokeSpawn.cp) >= min) {
                PokeNotificationSender.notificationLog.info(String.format("Pokemon between specified cp (%s,%s)", infOrNum(min), infOrNum(max)));
            } else {
                PokeNotificationSender.notificationLog.info(String.format("Pokemon (%sCP) not between specified cp (%s,%s)", pokeSpawn.cp, infOrNum(min), infOrNum(max)));
                return false;
            }

            maxObj = obj.get("max_level");
            minObj = obj.get("min_level");

            max = maxObj == null ? Integer.MAX_VALUE : maxObj.getAsInt();
            min = minObj == null ? Integer.MIN_VALUE : minObj.getAsFloat();

            if ((pokeSpawn.level == null ? -1 : pokeSpawn.level) <= max && (pokeSpawn.level == null ? -1 : pokeSpawn.level) >= min) {
                PokeNotificationSender.notificationLog.info(String.format("Pokemon between specified level (%s,%s)", infOrNum(min), infOrNum(max)));
            } else {
                PokeNotificationSender.notificationLog.info(String.format("Pokemon (level %s) not between specified level (%s,%s)", pokeSpawn.level, infOrNum(min), infOrNum(max)));
                return false;
            }

            JsonArray sizes = obj.getAsJsonArray("size");

            if (sizes != null) {
                String  spawnSize = pokeSpawn.properties.get("size");
                boolean passed    = false;

                for (JsonElement size : sizes) {
                    if (size.getAsString().equals(spawnSize)) {
                        PokeNotificationSender.notificationLog.info(String.format("Pokemon size %s passed filter", spawnSize));
                        passed = true;
                        break;
                    }
                }

                if (!passed) {
                    PokeNotificationSender.notificationLog.info(String.format("Pokemon size %s did not pass filter", spawnSize));
                    return false;
                }
            }
            return true;
        } else {
            if (pokeFilter.getAsBoolean()) {
                PokeNotificationSender.notificationLog.info("Pokemon enabled in filter, posting to Discord");
                return true;
            } else {
                PokeNotificationSender.notificationLog.info("Pokemon not enabled in filter, not posting");
                return false;
            }
        }
    }

    private String infOrNum(float num) {
        if(num == Integer.MIN_VALUE){
            return "-inf";
        }else if (num == Integer.MAX_VALUE){
            return "inf";
        }else{
            return String.valueOf(num);
        }
    }

    public String novabotRole() {
        return novabotRoleId;
    }

    public boolean pokemonEnabled() {
        return pokemonEnabled;
    }


    public boolean presetsEnabled() {
        return presets.size() > 0;
    }

    public boolean raidsEnabled() {
        return raidsEnabled;
    }

    public boolean showMap(String fileName, String formatKey) {
        return Boolean.parseBoolean(formats.get(fileName).getFormatting(formatKey, "showMap"));
    }

    public boolean showStartupMessage() {
        return startupMessage;
    }

    public boolean statsEnabled() {
        return stats;
    }


    public boolean useGeofences() {
        return Geofencing.notEmpty();
    }

    public boolean useScanDb() {
        return useScanDb;
    }

    private void loadFilter(String filterName, HashMap<String, JsonObject> filterMap) {
        JsonObject filter = null;
        JsonParser parser = new JsonParser();

        try {
            JsonElement element = parser.parse(new FileReader(filterName));

            if (element.isJsonObject()) {
                filter = element.getAsJsonObject();
            }

            if (filterMap.put(filterName, filter) == null) {
                log.info(String.format("Loaded filter %s", filterName));
            }
        } catch (FileNotFoundException e) {
            log.warn(String.format("Couldn't find filter file %s, aborting.",filterName));
            System.exit(0);
        }
    }

    private void loadFormatting(String fileName) {

        Ini formatting;
        try {
            formatting = new Ini(new File(fileName));

            Format format = new Format();

            for (String formatKey : formatKeys) {
                Ini.Section section = formatting.get(formatKey);

                for (String var : formattingVars) {
                    format.addFormatting(formatKey, var, section.get(var));
                }

                if (formatKey.equals("pokemon")) {
                    format.addFormatting(formatKey, "encounteredBody", section.get("encounteredBody"));
                    format.addFormatting(formatKey, "encounteredTitle", section.get("encounteredTitle"));
                }
            }

            formats.put(fileName, format);
        } catch (FileNotFoundException e) {
            log.warn(String.format("Couldn't find formatting file %s", fileName));
        } catch (IOException e) {
            log.error(String.format("Error loading formatting file %s", fileName),e);
        }
    }

    private HashMap<GeofenceIdentifier, String> loadGeofencedChannels(File file, HashMap<GeofenceIdentifier, String> map) {
        Scanner sc = null;
        try {
            sc = new Scanner(file);
            while (sc.hasNext()) {
                String line = sc.nextLine().toLowerCase();

                String[] split = line.split("=");

                ArrayList<GeofenceIdentifier> geofenceIdentifiers = GeofenceIdentifier.fromString(split[0].trim());

                String channelId = split[1].trim();

                for (GeofenceIdentifier geofenceIdentifier : geofenceIdentifiers) {
                    map.put(geofenceIdentifier, channelId);
                }

            }
        } catch (FileNotFoundException e) {
            log.warn(String.format("Couldn't find %s", file.toString()));
        }


        return map;
    }

    private ArrayList<String> loadKeys(File gkeys) {

        ArrayList<String> keys = new ArrayList<>();

        try {
            Scanner in = new Scanner(gkeys);

            while (in.hasNext()) {
                String key = in.nextLine();
                keys.add(key);
            }

        } catch (FileNotFoundException e) {
            log.warn(String.format("Couldn't find gkeys file %s. Aborting", gkeys.getName()));
            System.exit(0);
        }

        return keys;
    }

    private AlertChannels loadPokemonChannels(String pokeChannelsFile, String formatting) {

        AlertChannels pokeChannelsRet = new AlertChannels();

        File file = new File(pokeChannelsFile);

        try (Scanner in = new Scanner(file)) {

            String                      channelId           = null;
            String                      filterName          = null;
            String                      formattingName      = formatting;
            HashSet<GeofenceIdentifier> geofenceIdentifiers = null;

            boolean first = true;

            while (in.hasNext()) {
                String line = in.nextLine().toLowerCase();

                if (line.length() == 0 || line.charAt(0) == ';') {
                    continue;
                }

                if (line.charAt(0) == '[') {
                    AlertChannel channel;

                    if (channelId != null) {
                        channel = new AlertChannel(channelId);

                        if (filterName != null) {
                            channel.setFilterName(filterName);

                            channel.setGeofences(geofenceIdentifiers);

                            channel.setFormattingName(formattingName);

                            pokeChannelsRet.add(channel);
                        } else {
                            System.out.println("couldn't find filter name");
                        }

                    } else if (!first) {
                        System.out.println("couldn't find channel id");
                    }

                    int end = line.indexOf("]");
                    channelId = line.substring(1, end).trim();

                    first = false;
                } else {
                    int equalsIndex = line.indexOf("=");

                    if (!(equalsIndex == -1)) {
                        String parameter = line.substring(0, equalsIndex).trim();
                        String value     = line.substring(equalsIndex + 1).trim();

                        switch (parameter) {
                            case "geofences":
                                if (value.equals("all")) {
                                    geofenceIdentifiers = null;
                                    continue;
                                }
                                geofenceIdentifiers = new HashSet<>();

                                ArrayList<String> geofences;

                                if (value.charAt(0) == '[') {
                                    geofences = UtilityFunctions.parseList(value);
                                } else {
                                    geofences = new ArrayList<>();
                                    geofences.add(value);
                                }

                                for (String s : geofences) {
                                    geofenceIdentifiers.addAll(GeofenceIdentifier.fromString(s));
                                }
                                break;
                            case "filter":
                                filterName = value;

                                if (!pokeFilters.containsKey(filterName)) {
                                    loadFilter(filterName, pokeFilters);
                                }
                                break;
                            case "formatting":
                                formattingName = value;

                                if (!formats.containsKey(formattingName)) {
                                    loadFormatting(formattingName);
                                }
                                break;
                        }
                    }
                }
            }

            AlertChannel channel;
            if (channelId != null) {
                channel = new AlertChannel(channelId);

                if (filterName != null) {
                    channel.setFilterName(filterName);

                    channel.setFormattingName(formattingName);

                    channel.setGeofences(geofenceIdentifiers);

                    pokeChannelsRet.add(channel);
                } else {
                    System.out.println("couldn't find filter name");
                }

            } else {
                System.out.println("couldn't find channel id");
            }

        } catch (FileNotFoundException e) {
            log.warn(String.format("Couldn't find pokechannels file: %s, ignoring.", pokeChannelsFile));
        }
        return pokeChannelsRet;
    }

    private void loadPresets(String novaBotPresetsFile) {
        File file = new File(novaBotPresetsFile);

        try (Scanner in = new Scanner(file)) {

            String  presetName = null;
            String  filterName = null;
            Boolean pokemon    = null;

            boolean first = true;

            while (in.hasNext()) {
                String line = in.nextLine().toLowerCase();

                if (line.length() == 0 || line.charAt(0) == ';') {
                    continue;
                }

                if (line.charAt(0) == '[') {

                    if (presetName != null) {
                        parseBlock(presetName, filterName, pokemon);
                    } else if (!first) {
                        System.out.println("couldn't find preset name");
                    }

                    int end = line.indexOf("]");
                    presetName = line.substring(1, end).trim();

                    first = false;
                } else {
                    int equalsIndex = line.indexOf("=");

                    if (!(equalsIndex == -1)) {

                        String parameter = line.substring(0, equalsIndex).trim();
                        String value     = line.substring(equalsIndex + 1).trim();

                        switch (parameter) {
                            case "type":
                                pokemon = value.equals("pokemon");
                                break;
                            case "filter":
                                filterName = value;
                                break;
                        }
                    }
                }
            }

            if (presetName != null) {
                parseBlock(presetName, filterName, pokemon);
            } else {
                System.out.println("couldn't find preset name");
            }

        } catch (FileNotFoundException e) {
            log.warn(String.format("Couldn't find %s, ignoring", novaBotPresetsFile));
        }
    }

    private void loadRaidChannels(String raidChannelsFile, String formatting) {

        File file = new File(raidChannelsFile);

        try (Scanner in = new Scanner(file)) {

            String                      channelId           = null;
            String                      filterName          = null;
            String                      formattingName      = formatting;
            String                      chatId              = null;
            HashSet<GeofenceIdentifier> geofenceIdentifiers = null;

            boolean first = true;

            while (in.hasNext()) {
                String line = in.nextLine().toLowerCase();

                if (line.length() == 0 || line.charAt(0) == ';') {
                    continue;
                }

                if (line.charAt(0) == '[') {
                    RaidChannel channel;

                    if (channelId != null) {
                        channel = new RaidChannel(channelId);

                        if (filterName != null) {
                            channel.setFilterName(filterName);

                            channel.setGeofences(geofenceIdentifiers);

                            channel.setFormattingName(formattingName);

                            channel.chatId = chatId;

                            if (chatId != null && geofenceIdentifiers != null) {
                                for (GeofenceIdentifier geofenceIdentifier : geofenceIdentifiers) {
                                    raidChats.put(geofenceIdentifier, chatId);
                                }
                            }

                            raidChannels.add(channel);
                        } else {
                            System.out.println("couldn't find filter name");
                        }

                    } else if (!first) {
                        System.out.println("couldn't find channel id");
                    }

                    int end = line.indexOf("]");
                    channelId = line.substring(1, end).trim();

                    first = false;
                } else {
                    int equalsIndex = line.indexOf("=");

                    if (!(equalsIndex == -1)) {
                        String parameter = line.substring(0, equalsIndex).trim();
                        String value     = line.substring(equalsIndex + 1).trim();

                        switch (parameter) {
                            case "geofences":
                                if (value.equals("all")) {
                                    geofenceIdentifiers = null;
                                    continue;
                                }
                                geofenceIdentifiers = new HashSet<>();

                                ArrayList<String> geofences;

                                if (value.charAt(0) == '[') {
                                    geofences = UtilityFunctions.parseList(value);
                                } else {
                                    geofences = new ArrayList<>();
                                    geofences.add(value);
                                }

                                for (String s : geofences) {
                                    geofenceIdentifiers.addAll(GeofenceIdentifier.fromString(s));
                                }
                                break;
                            case "filter":
                                filterName = value;

                                if (!raidFilters.containsKey(filterName)) {
                                    loadFilter(filterName, raidFilters);
                                }
                                break;
                            case "formatting":
                                formattingName = value;

                                if (!formats.containsKey(formattingName)) {
                                    loadFormatting(formattingName);
                                }
                                break;
                            case "chat":
                                chatId = value;
                                break;
                        }
                    }
                }
            }

            RaidChannel channel;
            if (channelId != null) {
                channel = new RaidChannel(channelId);

                if (filterName != null) {
                    channel.setFilterName(filterName);

                    channel.setFormattingName(formattingName);

                    channel.setGeofences(geofenceIdentifiers);

                    channel.chatId = chatId;

                    if (chatId != null && geofenceIdentifiers != null) {
                        for (GeofenceIdentifier geofenceIdentifier : geofenceIdentifiers) {
                            raidChats.put(geofenceIdentifier, chatId);
                        }
                    }

                    raidChannels.add(channel);
                } else {
                    System.out.println("couldn't find filter name");
                }

            } else {
                System.out.println("couldn't find channel id");
            }

        } catch (FileNotFoundException e) {
            log.warn(String.format("Couldn't find raidchannels file: %s, ignoring.", raidChannelsFile));
        }
    }

    private void loadSupporterRoles(String supporterLevelsFile) {
        File file = new File(supporterLevelsFile);

        try {
            Scanner sc = new Scanner(file);

            while (sc.hasNext()) {
                String line = sc.nextLine().toLowerCase();

                String[] split = line.split("=");

                String roleId = split[0].trim();

                roleLimits.put(roleId, NotificationLimit.fromString(line));
            }
        } catch (FileNotFoundException e) {
            log.warn(String.format("Couldn't find %s, ignoring", supporterLevelsFile));
        }
    }

    private void parseBlock(String presetName, String filterName, Boolean pokemon) {
        if (filterName != null) {
            if (pokemon != null) {
                if (pokemon) {
                    if (!pokeFilters.containsKey(filterName)) {
                        loadFilter(filterName, pokeFilters);
                    }
                } else {
                    if (!raidFilters.containsKey(filterName)) {
                        loadFilter(filterName, raidFilters);
                    }
                }

                presets.put(presetName, filterName);
            } else {
                System.out.println("couldn't find type value");
            }
        } else {
            System.out.println("couldn't find filter name");
        }
    }

    private JsonElement searchFilter(JsonObject filter, String search) {
        if (filter == null || search == null) return null;
        return filter.get(search);
    }

    public boolean useGoogleTimeZones() {
        return useGoogleTimeZones;
    }

}
