package com.github.novskey.novabot.data;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.*;
import com.github.novskey.novabot.pokemon.PokeSpawn;
import com.github.novskey.novabot.pokemon.Pokemon;
import com.github.novskey.novabot.raids.RaidSpawn;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.*;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static com.github.novskey.novabot.core.ScannerType.*;
import static com.github.novskey.novabot.data.DataManager.MySQL_DRIVER;
import static com.github.novskey.novabot.data.DataManager.PgSQL_DRIVER;

@Data
public class ScanDBManager  {

    private Logger dbLog;

    private final ScannerDb scannerDb;
    private ZonedDateTime lastChecked;
    private java.lang.String scanUrl;
    public final HashMap<String, RaidSpawn> knownRaids = new HashMap<>();
    private ZonedDateTime lastCheckedRaids;
    private StringBuilder blacklistQuery = new StringBuilder();
    private final NovaBot novaBot;

    private com.github.novskey.novabot.data.DataSource scanDataSource;
    private HashSet<String> gymNames = new HashSet<>();

    public ScanDBManager (NovaBot novabot, ScannerDb scannerDb, int id){
        this.novaBot = novabot;
        this.scannerDb = scannerDb;
        dbLog = LoggerFactory.getLogger("Scan-DB-" + id);
        lastChecked = ZonedDateTime.now(UtilityFunctions.UTC);
        lastCheckedRaids = ZonedDateTime.now(UtilityFunctions.UTC);
        scanDbConnect();
        if(novabot.getConfig().raidsEnabled()) {
            gymNames = fetchGymNames();
        }
    }

    public HashSet<String> fetchGymNames() {
        HashSet<String> names = new HashSet<>();

        String sql = null;
        switch (scannerDb.getScannerType()) {
            case RocketMap:
            case SloppyRocketMap:
            case SkoodatRocketMap:
            case PhilMap:
                sql = "SELECT name FROM gymdetails";
                break;
            case Hydro74000Monocle:
            case Monocle:
                sql = "SELECT name FROM forts";
                break;
        }

        try (Connection connection = getScanConnection();
             Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()){
                String gymName = rs.getString(1);
                if(gymName != null) {
                    names.add(gymName.toLowerCase().trim());
                }
            }

            dbLog.info(String.format("Found %d gym names", names.size()));
        } catch (SQLException e) {
            dbLog.error("Error executing getGymNames",e);
        }

        return names;
    }

    public int countSpawns(int id, TimeUnit intervalType, int intervalLength) {
        int numSpawns = 0;

        String sql = null;
        switch (scannerDb.getScannerType()) {
            case RocketMap:
            case SloppyRocketMap:
            case SkoodatRocketMap:
            case PhilMap:
                sql = "  SELECT COUNT(*) " +
                      "FROM pokemon " +
                      "WHERE pokemon_id = ? AND disappear_time > (UTC_TIMESTAMP() - INTERVAL ? " + intervalType.toDbString() + ")";
                break;
            case Hydro74000Monocle:
            case Monocle:
                sql = "  SELECT COUNT(*) " +
                      "FROM sightings " +
                      "WHERE pokemon_id = ? " +
                      "AND expire_timestamp > " +
                      (scannerDb.getProtocol().equals("postgresql") ?
                       "extract(epoch from (now() - INTERVAL '" + intervalLength + "' " + intervalType.toDbString() + "))":
                       "UNIX_TIMESTAMP(now() - INTERVAL '" + intervalLength + "'" + intervalType.toDbString() + ")");
                break;
        }

        try (Connection connection = getScanConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            switch (scannerDb.getScannerType()){
                case RocketMap:
                case SloppyRocketMap:
                case SkoodatRocketMap:
                case PhilMap:
                    statement.setInt(2,intervalLength);
                    break;
                default:
                    break;
            }

            dbLog.debug(statement.toString());
            System.out.println(statement);
            statement.executeQuery();

            ResultSet rs = statement.getResultSet();

            if (rs.next()) {
                numSpawns = rs.getInt(1);
            }
        } catch (SQLException e) {
            dbLog.error("Error executing countSpawns",e);
        }

        return numSpawns;
    }

    public void getCurrentRaids(boolean firstRun) {
        dbLog.info("Checking which known gyms need to be updated");

        checkKnownRaids();

        dbLog.info("Done");
//        dbLog.log(DEBUG,knownRaids);

        dbLog.info("Getting new raids");

        StringBuilder knownIdQMarks = new StringBuilder();

        if (knownRaids.size() > 0) {
            if (scannerDb.getScannerType() == ScannerType.PhilMap || scannerDb.getScannerType() == RocketMap || scannerDb.getScannerType() == SkoodatRocketMap || scannerDb.getScannerType() == SloppyRocketMap) {
                knownIdQMarks.append("gym.gym_id NOT IN (");
            } else {
                knownIdQMarks.append("forts.id NOT in (");
            }
            for (int i = 0; i < knownRaids.size(); ++i) {
                knownIdQMarks.append("?");
                if (i != knownRaids.size() - 1) {
                    knownIdQMarks.append(",");
                }
            }
            knownIdQMarks.append(") AND");
        }

        ArrayList<String> knownIds = new ArrayList<>(knownRaids.keySet());

        if (knownRaids.containsKey(null)) {
            System.out.println("NULL, OH NO KNOWNRAIDS CONTAINS NULL? WHY???");
            System.out.println(knownRaids.get(null));
        }

        String sql = null;

        switch (scannerDb.getScannerType()) {
            case SloppyRocketMap:
            case SkoodatRocketMap:
            case RocketMap:
                sql = "SELECT" +
                      "  gymdetails.name," +
                      "  gymdetails.gym_id," +
                      "  gym.latitude," +
                      "  gym.longitude," +
                      "  gym.team_id," +
                      "  raid.end AS end," +
                      "  raid.start AS battle," +
                      "  raid.pokemon_id," +
                      "  raid.cp, " +
                      "  raid.level, " +
                      "  raid.move_1, " +
                      "  raid.move_2 " +
                      "FROM gym" +
                      "  INNER JOIN gymdetails ON gym.gym_id = gymdetails.gym_id" +
                      "  INNER JOIN raid ON gym.gym_id = raid.gym_id " +
                      "WHERE " + knownIdQMarks + " raid.end > (? + INTERVAL 1 MINUTE)";
                break;
            case Monocle:
                sql = "SELECT" +
                      "  forts.id," +
                      "  forts.lat," +
                      "  forts.lon," +
                      "  fort_sightings.team," +
                      "  raids.time_end AS end," +
                      "  raids.time_battle AS battle," +
                      "  raids.pokemon_id," +
                      "  raids.level, " +
                      "  raids.move_1, " +
                      "  raids.move_2 " +
                      "FROM forts" +
                      "  INNER JOIN fort_sightings ON forts.id = fort_sightings.fort_id" +
                      "  INNER JOIN raids ON forts.id = raids.fort_id " +
                      "WHERE " + knownIdQMarks + " raids.time_end > " +
                      (scannerDb.getProtocol().equals("mysql")
                       ? "UNIX_TIMESTAMP(? + INTERVAL 1 MINUTE)"
                       : "extract(epoch from (?::timestamptz + INTERVAL '1' MINUTE))::int");
                break;
            case Hydro74000Monocle:
                sql = "SELECT forts.name," +
                      " forts.id," +
                      " forts.lat," +
                      " forts.lon," +
                      " fort_sightings.team," +
                      " raids.time_end AS END," +
                      " raids.time_battle AS battle," +
                      " raids.pokemon_id," +
                      " raids.cp," +
                      " raids.level," +
                      " raids.move_1," +
                      " raids.move_2 " +
                      "FROM forts " +
                      "INNER JOIN fort_sightings ON forts.id = fort_sightings.fort_id " +
                      "INNER JOIN raids ON forts.id = raids.fort_id " +
                      "WHERE " + knownIdQMarks + " raids.time_end > " +
                      (scannerDb.getProtocol().equals("mysql")
                       ? "UNIX_TIMESTAMP(? + INTERVAL 1 MINUTE)"
                       : "extract(epoch from (?::timestamptz + INTERVAL '1' MINUTE))::int");
                break;
            case PhilMap:
                sql = "SELECT" +
                      "  gymdetails.name," +
                      "  gymdetails.gym_id," +
                      "  gym.latitude," +
                      "  gym.longitude," +
                      "  gym.team_id," +
                      "  raidinfo.raid_end_ms AS end," +
                      "  raidinfo.raid_battle_ms AS battle," +
                      "  raidinfo.pokemon_id," +
                      "  raidinfo.cp, " +
                      "  raidinfo.raid_level, " +
                      "  raidinfo.move_1, " +
                      "  raidinfo.move_2 " +
                      "FROM gym" +
                      "  INNER JOIN gymdetails ON gym.gym_id = gymdetails.gym_id" +
                      "  INNER JOIN raidinfo ON gym.gym_id = raidinfo.gym_id " +
                      "WHERE " + knownIdQMarks + " raid_end_ms > (? + INTERVAL 1 MINUTE)";
                break;
        }

        int rows = 0;

        try (Connection connection = getScanConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            for (int i = 0; i < knownRaids.size(); i++) {
                if (scannerDb.getProtocol().equals("mysql")) {
                    statement.setString(i + 1, knownIds.get(i));
                } else {
                    statement.setInt(i + 1, Integer.parseInt(knownIds.get(i)));
                }
            }
            switch (scannerDb.getScannerType()) {
                case RocketMap:
                case SkoodatRocketMap:
                case SloppyRocketMap:
                case PhilMap:
                    statement.setObject(knownRaids.size() + 1, lastCheckedRaids.toLocalDateTime(), Types.TIMESTAMP);
                    break;
                case Monocle:
                case Hydro74000Monocle:
                    LocalDateTime localDateTime = lastCheckedRaids.withZoneSameInstant(novaBot.getConfig().getTimeZone()).toLocalDateTime();
                    String timeStamp = String.format("%s %s", localDateTime.toLocalDate(), localDateTime.toLocalTime());

                    statement.setString(knownRaids.size() + 1, timeStamp);
                    break;
            }

//            statement.setTimestamp(knownRaids.size() + 1, Timestamp.from(lastCheckedRaids.toInstant()), utcCalendar);


            dbLog.info("Executing query: " + statement);
            lastCheckedRaids = ZonedDateTime.now(UtilityFunctions.UTC);
//            dbLog.log(DEBUG,statement);
            final ResultSet rs = statement.executeQuery();
            dbLog.info("Query complete");


            if (firstRun) {
                dbLog.info("Not sending adding to processing queue on first run");
            }

            while (rs.next()) {
                rows++;
                RaidSpawn raidSpawn = null;
                String gymId = null;

//                System.out.println("Current UTC time: " + ZonedDateTime.now(UtilityFunctions.UTC));
//                System.out.println("Current time based on getConfig() timezone : " + UtilityFunctions.getCurrentTime(novaBot.getConfig().getTimeZone()));
                switch (scannerDb.getScannerType()) {
                    case RocketMap:
                    case SkoodatRocketMap:
                    case SloppyRocketMap:
                    case PhilMap:
                        String name = rs.getString(1);
                        gymId = rs.getString(2);
                        double lat = rs.getDouble(3);
                        double lon = rs.getDouble(4);
                        Team team = Team.fromId(rs.getInt(5));
                        ZonedDateTime raidEnd = ZonedDateTime.ofLocal(rs.getTimestamp(6).toLocalDateTime(), UtilityFunctions.UTC, null);
                        ZonedDateTime battleStart = ZonedDateTime.ofLocal(rs.getTimestamp(7).toLocalDateTime(), UtilityFunctions.UTC, null);
                        int bossId = rs.getInt(8);
                        int bossCp = rs.getInt(9);
                        int raidLevel = rs.getInt(10);
                        int move_1 = rs.getInt(11);
                        int move_2 = rs.getInt(12);

                        raidSpawn = new RaidSpawn(name, gymId, lat, lon, team, raidEnd, battleStart, bossId, bossCp, move_1, move_2, raidLevel);
                        break;
                    case Hydro74000Monocle:
                        name = rs.getString(1);
                        gymId = String.valueOf(rs.getInt(2));
                        lat = rs.getDouble(3);
                        lon = rs.getDouble(4);
                        team = Team.fromId((Integer) rs.getObject(5));
                        raidEnd = ZonedDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt(6)), UtilityFunctions.UTC);
                        battleStart = ZonedDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt(7)), UtilityFunctions.UTC);
                        bossId = rs.getInt(8);
                        bossCp = rs.getInt(9);
                        raidLevel = rs.getInt(10);
                        move_1 = rs.getInt(11);
                        move_2 = rs.getInt(12);

                        raidSpawn = new RaidSpawn(name, gymId, lat, lon, team, raidEnd, battleStart, bossId, bossCp, move_1, move_2, raidLevel);
                        break;
                    case Monocle:
                        gymId = String.valueOf(rs.getInt(1));
                        lat = rs.getDouble(2);
                        lon = rs.getDouble(3);
                        team = Team.fromId(rs.getInt(4));
                        raidEnd = ZonedDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt(5)), UtilityFunctions.UTC);
                        battleStart = ZonedDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt(6)), UtilityFunctions.UTC);
                        bossId = rs.getInt(7);
                        raidLevel = rs.getInt(8);
                        move_1 = rs.getInt(9);
                        move_2 = rs.getInt(10);

                        raidSpawn = new RaidSpawn("unkn", gymId, lat, lon, team, raidEnd, battleStart, bossId, (bossId > 0 ? Pokemon.getRaidBossCp(bossId, raidLevel) : 0), move_1, move_2, raidLevel);
                        break;
                }
                dbLog.debug(raidSpawn.toString());
                knownRaids.put(gymId, raidSpawn);

                if (!firstRun) {
                    novaBot.notificationsManager.raidQueue.add(raidSpawn);
                }
            }
        } catch (SQLException e) {
            lastCheckedRaids = ZonedDateTime.now(UtilityFunctions.UTC);
            dbLog.error("Error executing getCurrentRaids",e);
        }
        dbLog.info(String.format("Returned %s rows", rows));

    }

    public HashSet<String> getGymNames() {
        return gymNames;
    }

    public void getNewPokemon() {
        dbLog.info("Getting new pokemon");

        final ArrayList<PokeSpawn> pokeSpawns = new ArrayList<>();

        if (blacklistQuery.length() == 0 && novaBot.getConfig().getBlacklist().size() > 0) {
            blacklistQuery = new StringBuilder(" AND pokemon_id NOT IN (");
            for (int i = 0; i < novaBot.getConfig().getBlacklist().size(); ++i) {
                blacklistQuery.append("?");
                if (i != novaBot.getConfig().getBlacklist().size() - 1) {
                    blacklistQuery.append(",");
                }
            }
            blacklistQuery.append(")");
        }

        String sql = null;
        switch (scannerDb.getScannerType()) {
            case Monocle:
                sql = "" +
                      "SELECT pokemon_id," +
                      "       lat," +
                      "       lon," +
                      "       expire_timestamp," +
                      "       atk_iv," +
                      "       def_iv," +
                      "       sta_iv," +
                      "       move_1," +
                      "       move_2," +
                      "       display " +
                      "FROM sightings " +
                      "WHERE expire_timestamp > " +
                            (scannerDb.getProtocol().equals("mysql")
                             ? "UNIX_TIMESTAMP(now() - INTERVAL ? SECOND)"
                             : "extract(epoch from now())::int" + blacklistQuery
                     );
//                                : "extract(epoch from (now() - INTERVAL ? SECOND))");
                break;
            case Hydro74000Monocle:
                sql = "" +
                      "SELECT pokemon_id," +
                      "       lat," +
                      "       lon," +
                      "       expire_timestamp," +
                      "       atk_iv," +
                      "       def_iv," +
                      "       sta_iv," +
                      "       move_1," +
                      "       move_2," +
                      "       gender," +
                      "       form," +
                      "       cp," +
                      "       level, " +
                      "       weather_boosted_condition " +
                      "FROM sightings " +
                      "WHERE updated >= " +
                            (scannerDb.getProtocol().equals("mysql")
                             ? "UNIX_TIMESTAMP(? - INTERVAL 1 SECOND)"
                             : "extract(epoch from (?::timestamptz - INTERVAL '1' SECOND))::int ") +
                            "AND expire_timestamp > " +
                            (scannerDb.getProtocol().equals("mysql")
                             ? "UNIX_TIMESTAMP(now() - INTERVAL ? SECOND)"
                             : "extract(epoch from now())::int")
                      + blacklistQuery;
                break;
            case RocketMap:
                sql = "" +
                      "SELECT pokemon_id," +
                      "       latitude," +
                      "       longitude," +
                      "       disappear_time," +
                      "       individual_attack, " +
                      "       individual_defense," +
                      "       individual_stamina," +
                      "       move_1," +
                      "       move_2," +
                      "       weight," +
                      "       height," +
                      "       gender," +
                      "       form," +
                      "       cp, " +
                      "       cp_multiplier " +
                      "FROM pokemon " +
                      "WHERE last_modified >= (? - INTERVAL 1 SECOND) " +
                      "AND disappear_time > (UTC_TIMESTAMP() - INTERVAL ? SECOND)" + blacklistQuery;
                      ;
                break;
            case PhilMap:
                sql = "" +
                      "SELECT pokemon_id," +
                      "       latitude," +
                      "       longitude," +
                      "       disappear_time," +
                      "       individual_attack, " +
                      "       individual_defense," +
                      "       individual_stamina," +
                      "       move_1," +
                      "       move_2," +
                      "       weight," +
                      "       height," +
                      "       gender," +
                      "       form," +
                      "       cp, " +
                      "       cp_multiplier, " +
                      "       weather_boosted " +
                      "FROM pokemon " +
                      "WHERE last_modified >= (? - INTERVAL 1 SECOND) " +
                      "AND disappear_time > (UTC_TIMESTAMP() - INTERVAL ? SECOND)" + blacklistQuery;
                break;
            case SloppyRocketMap:
                sql = "" +
                      "SELECT pokemon_id," +
                      "       latitude," +
                      "       longitude," +
                      "       disappear_time," +
                      "       individual_attack, " +
                      "       individual_defense," +
                      "       individual_stamina," +
                      "       move_1," +
                      "       move_2," +
                      "       weight," +
                      "       height," +
                      "       gender," +
                      "       form," +
                      "       catch_prob_1," +
                      "       catch_prob_2," +
                      "       catch_prob_3," +
                      "       cp, " +
                      "       cp_multiplier, " +
                      "       weather_boosted_condition " +
                      "FROM pokemon " +
                      "WHERE last_modified >= (? - INTERVAL 1 SECOND) " +
                      "AND disappear_time > (UTC_TIMESTAMP() - INTERVAL ? SECOND)" +
                      blacklistQuery;
                break;
            case SkoodatRocketMap:
                sql = "" +
                      "SELECT pokemon_id," +
                      "       latitude," +
                      "       longitude," +
                      "       disappear_time," +
                      "       individual_attack, " +
                      "       individual_defense," +
                      "       individual_stamina," +
                      "       move_1," +
                      "       move_2," +
                      "       weight," +
                      "       height," +
                      "       gender," +
                      "       form," +
                      "       catch_prob_1," +
                      "       catch_prob_2," +
                      "       catch_prob_3," +
                      "       cp, " +
                      "       cp_multiplier, " +
                      "       weather_id " +
                      "FROM pokemon " +
                      "WHERE last_modified >= (? - INTERVAL 1 SECOND) " +
                      "AND disappear_time > (UTC_TIMESTAMP() - INTERVAL ? SECOND)" + blacklistQuery;
                break;
        }

        int rows = 0;
        int newSpawns = 0;
        try (Connection connection = getScanConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int offset = 1;

            switch (scannerDb.getScannerType()) {
                case RocketMap:
                case SloppyRocketMap:
                case SkoodatRocketMap:
                case PhilMap:
                    statement.setObject(offset, lastChecked.toLocalDateTime(), Types.TIMESTAMP);
                    offset++;
                    break;
                case Monocle:
                case Hydro74000Monocle:
                    LocalDateTime localDateTime = lastChecked.withZoneSameInstant(novaBot.getConfig().getTimeZone()).toLocalDateTime();
                    String timeStamp = String.format("%s %s", localDateTime.toLocalDate(), localDateTime.toLocalTime());

                    statement.setString(offset, timeStamp);
                    offset++;
//                    statement.setObject(novaBot.getConfig().getBlacklist().size() + 1, lastChecked.withZoneSameInstant(novaBot.getConfig().getTimeZone()).toLocalDateTime(), Types.TIMESTAMP);
                    break;
            }

            if (scannerDb.getProtocol().equals("mysql")) {
                statement.setString(offset,
                                    String.valueOf(novaBot.getConfig().getMinSecondsLeft()));
                offset++;
            }

            for (int i = 1; i <= novaBot.getConfig().getBlacklist().size(); ++i) {
                statement.setInt(offset, novaBot.getConfig().getBlacklist().get(i - 1));
                offset++;
            }

            dbLog.info("Executing query:" + statement);
            lastChecked = ZonedDateTime.now(UtilityFunctions.UTC);
            final ResultSet rs = statement.executeQuery();
            dbLog.info("Query complete");

            rows = 0;

            while (rs.next()) {
//                System.out.println("Current UTC time: " + ZonedDateTime.now(UtilityFunctions.UTC));
//                System.out.println("Current time based on getConfig() timezone : " + UtilityFunctions.getCurrentTime(novaBot.getConfig().getTimeZone()));

                PokeSpawn pokeSpawn = null;

                switch (scannerDb.getScannerType()) {
                    case RocketMap:
                        int id = rs.getInt(1);
                        double lat = rs.getDouble(2);
                        double lon = rs.getDouble(3);
                        ZonedDateTime disappearTime = ZonedDateTime.ofLocal(rs.getTimestamp(4).toLocalDateTime(), UtilityFunctions.UTC, null);
                        Integer attack = (Integer) rs.getObject(5);
                        Integer defense = (Integer) rs.getObject(6);
                        Integer stamina = (Integer) rs.getObject(7);
                        Integer move1 = (Integer) rs.getObject(8);
                        Integer move2 = (Integer) rs.getObject(9);
                        float weight = rs.getFloat(10);
                        float height = rs.getFloat(11);
                        Integer gender = (Integer) rs.getObject(12);
                        Integer form = (Integer) rs.getObject(13);
                        Integer cp = (Integer) rs.getObject(14);
                        double cpMod = rs.getDouble(15);
                        pokeSpawn = new PokeSpawn(id, lat, lon, disappearTime, attack, defense, stamina, move1, move2, weight, height, gender, form, cp, cpMod);
                        break;
                    case SloppyRocketMap:
                    case SkoodatRocketMap:
                        id = rs.getInt(1);
                        lat = rs.getDouble(2);
                        lon = rs.getDouble(3);
                        disappearTime = ZonedDateTime.ofLocal(rs.getTimestamp(4).toLocalDateTime(), UtilityFunctions.UTC, null);
                        attack = (Integer) rs.getObject(5);
                        defense = (Integer) rs.getObject(6);
                        stamina = (Integer) rs.getObject(7);
                        move1 = (Integer) rs.getObject(8);
                        move2 = (Integer) rs.getObject(9);
                        weight = rs.getFloat(10);
                        height = rs.getFloat(11);
                        gender = (Integer) rs.getObject(12);
                        form = (Integer) rs.getObject(13);
                        float catchprob1 = rs.getFloat(14);
                        float catchprob2 = rs.getFloat(15);
                        float catchprob3 = rs.getFloat(16);
                        cp = (Integer) rs.getObject(17);
                        cpMod = rs.getDouble(18);
                        int weather = rs.getInt(19);
                        pokeSpawn = new PokeSpawn(id, lat, lon, disappearTime, attack, defense, stamina, move1, move2, weight, height, gender, form, cp, cpMod, catchprob1, catchprob2, catchprob3, weather);
                        break;
                    case PhilMap:
                        id = rs.getInt(1);
                        lat = rs.getDouble(2);
                        lon = rs.getDouble(3);
                        disappearTime = ZonedDateTime.ofLocal(rs.getTimestamp(4).toLocalDateTime(), UtilityFunctions.UTC, null);
                        attack = (Integer) rs.getObject(5);
                        defense = (Integer) rs.getObject(6);
                        stamina = (Integer) rs.getObject(7);
                        move1 = (Integer) rs.getObject(8);
                        move2 = (Integer) rs.getObject(9);
                        weight = rs.getFloat(10);
                        height = rs.getFloat(11);
                        gender = (Integer) rs.getObject(12);
                        form = (Integer) rs.getObject(13);
                        cp = (Integer) rs.getObject(14);
                        cpMod = rs.getDouble(15);
                        weather = rs.getInt(16);
                        pokeSpawn = new PokeSpawn(id, lat, lon, disappearTime, attack, defense, stamina, move1, move2, weight, height, gender, form, cp, cpMod, weather);
                        break;
                    case Monocle:
                        id = rs.getInt(1);
                        lat = rs.getDouble(2);
                        lon = rs.getDouble(3);
                        disappearTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(rs.getLong(4)), UtilityFunctions.UTC);
                        attack = (Integer) rs.getObject(5);
                        defense = (Integer) rs.getObject(6);
                        stamina = (Integer) rs.getObject(7);
                        move1 = (Integer) rs.getObject(8);
                        move2 = (Integer) rs.getObject(9);
                        form = (Integer) rs.getObject(10);
                        pokeSpawn = new PokeSpawn(id, lat, lon, disappearTime, attack, defense, stamina, move1, move2, 0, 0, null, form, null, 0, -1);
                        break;
                    case Hydro74000Monocle:
                        id = rs.getInt(1);
                        lat = rs.getDouble(2);
                        lon = rs.getDouble(3);
                        disappearTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(rs.getLong(4)), UtilityFunctions.UTC);
                        attack = (Integer) rs.getObject(5);
                        defense = (Integer) rs.getObject(6);
                        stamina = (Integer) rs.getObject(7);
                        move1 = (Integer) rs.getObject(8);
                        move2 = (Integer) rs.getObject(9);
                        gender = (Integer) rs.getObject(10);
                        form = (Integer) rs.getObject(11);
                        cp = (Integer) rs.getObject(12);
                        Integer level = (Integer) rs.getObject(13);
                        weather = rs.getInt(14);
                        pokeSpawn = new PokeSpawn(id, lat, lon, disappearTime, attack, defense, stamina, move1, move2, 0, 0, gender, form, cp, level, weather);

                        break;
                }

                try {
                    dbLog.info(pokeSpawn.toString());
                    dbLog.info(Integer.toString(pokeSpawn.hashCode()));

                    if (!novaBot.getDataManager().containsHashCode(pokeSpawn.hashCode())) {
                        dbLog.debug("new pokemon, adding to list");
                        newSpawns++;
                        novaBot.getDataManager().addHashCode(pokeSpawn.hashCode());

                        novaBot.notificationsManager.pokeQueue.add(pokeSpawn);
//                        pokeSpawns.add(pokeSpawn);
                    } else {
                        dbLog.debug("pokemon already seen, ignoring");
                    }
                } catch (Exception e) {
                    dbLog.error("Error executing getNewPokemon",e);
                }
            }
        } catch (SQLException e) {
            lastChecked = ZonedDateTime.now(UtilityFunctions.UTC);
            dbLog.error("Error executing getNewPokemon",e);
        }
        dbLog.info(String.format("Returned %s rows, %s new pokemon", rows, newSpawns));
//        return pokeSpawns;
    }

    private Connection getScanConnection() {
        try {
            return scanDataSource.getConnection();
        } catch (SQLException e) {
            dbLog.error("Error executing getScanConnection",e);
        }
        return null;
    }


    private void checkKnownRaids() {

        ArrayList<String> toRemove = new ArrayList<>();

        knownRaids.forEach((gymId, raid) -> {
            if (lastCheckedRaids.isAfter(raid.battleStart) && raid.bossId == 0) {
                dbLog.debug(String.format("%s egg has hatched, removing from known raids so we can find out boss pokemon", gymId));
                toRemove.add(gymId);
                dbLog.debug("Queued for removal " + raid);
            }

            if (lastCheckedRaids.isAfter(raid.raidEnd)) {
                dbLog.debug(String.format("%s raid has ended, removing from known raids so we can find new raids on this gym", gymId));
                toRemove.add(gymId);
                dbLog.debug("Queued for removal " + raid);
            }
        });

        toRemove.forEach(knownRaids::remove);

        dbLog.debug("Removed all queued gyms");
    }

    public void scanDbConnect() {
        boolean mysql = scannerDb.getProtocol().equals("mysql");

        scanUrl = String.format("jdbc:%s://%s:%s/%s%s",
                                scannerDb.getProtocol(),
                                scannerDb.getIp(),
                                scannerDb.getPort(),
                                scannerDb.getDbName(),
                                mysql ? "?useSSL=" + scannerDb.getUseSSL() : "");

        try {
            scanDataSource = com.github.novskey.novabot.data.DataSource.getInstance(
                    (mysql ? MySQL_DRIVER : PgSQL_DRIVER),
                    scannerDb.getUser(),
                    scannerDb.getPass(),
                    scanUrl,
                    scannerDb.getMaxConnections()
                                                                                   );
        } catch (IOException | SQLException | PropertyVetoException e) {
            dbLog.error("Error executing scanDbConnect",e);
        }
    }

}
