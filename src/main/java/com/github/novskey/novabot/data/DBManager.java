package com.github.novskey.novabot.data;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.*;
import com.github.novskey.novabot.maps.GeocodedLocation;
import com.github.novskey.novabot.pokemon.PokeSpawn;
import com.github.novskey.novabot.pokemon.Pokemon;
import com.github.novskey.novabot.raids.Raid;
import com.github.novskey.novabot.raids.RaidLobby;
import com.github.novskey.novabot.raids.RaidSpawn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.*;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class DBManager implements IDataBase {
    private final Logger dbLog = LoggerFactory.getLogger("DB");
    private ZonedDateTime lastChecked;

    private RotatingSet<Integer> hashCodes;

    Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone(UtilityFunctions.UTC));

    public final HashMap<String, RaidSpawn> knownRaids = new HashMap<>();
    private ZonedDateTime lastCheckedRaids;
    private StringBuilder blacklistQMarks = null;
    private final NovaBot novaBot;
    private java.lang.String scanUrl;
    private String nbUrl;

    private static final String MySQL_DRIVER = "com.mysql.jdbc.Driver";
    private static final String PgSQL_DRIVER = "org.postgresql.Driver";
    private com.github.novskey.novabot.data.DataSource novaBotDataSource;
    private com.github.novskey.novabot.data.DataSource scanDataSource;

    public DBManager(NovaBot novaBot) {
        this.novaBot = novaBot;
        hashCodes = new RotatingSet<>(novaBot.getConfig().getMaxStoredHashes());
        lastChecked = ZonedDateTime.now(UtilityFunctions.UTC);
        lastCheckedRaids = ZonedDateTime.now(UtilityFunctions.UTC);
    }

    @Override
    public void addPokemon(final String userID, final Pokemon pokemon) {

        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO pokemon (user_id, id, max_iv, min_iv, max_lvl, min_lvl, max_cp, min_cp, location) " +
                             "VALUES (?,?,?,?,?,?,?,?,?)")) {
            statement.setString(1, userID);
            statement.setInt(2, pokemon.getID());
            statement.setDouble(3, pokemon.maxiv);
            statement.setDouble(4, pokemon.miniv);
            statement.setInt(5,pokemon.maxlvl);
            statement.setInt(6,pokemon.minlvl);
            statement.setInt(7,pokemon.maxcp);
            statement.setInt(8,pokemon.mincp);
            statement.setString(9, pokemon.getLocation().toDbString());

            dbLog.info(statement.toString());
            statement.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            dbLog.warn(e.getMessage());
        } catch (SQLException e2) {
            dbLog.error("Error executing addPokemon",e2);
        }
    }

    @Override
    public void addPreset(final String userID, String preset, Location location) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("" +
                     "INSERT INTO preset (user_id, preset_name, location) " +
                     "VALUES (?,?,?)")) {
            statement.setString(1, userID);
            statement.setString(2, preset);
            statement.setString(3, location.toDbString());

            dbLog.info(statement.toString());
            statement.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            dbLog.warn(e.getMessage());
        } catch (SQLException e2) {
            dbLog.error("Error executing addPreset",e2);
        }
    }

    @Override
    public void addRaid(final String userID, final Raid raid) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO raid VALUES (?,?,?)")) {
            statement.setString(1, userID);
            statement.setDouble(2, raid.bossId);
            statement.setString(3, raid.location.toDbString());

            dbLog.debug(statement.toString());
            statement.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            dbLog.warn(e.getMessage());
        } catch (SQLException e2) {
            dbLog.error("Error executing addRaid", e2);
        }
    }

    @Override
    public void addUser(final String userID) {
        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("INSERT INTO users (id) VALUES (%s);", "'" + userID + "'"));
        } catch (SQLIntegrityConstraintViolationException e3) {
            dbLog.warn("Cannot add duplicate");
        } catch (SQLException e) {
            dbLog.error("Error executing addUser", e);
        }
    }


    @Override
    public void clearPreset(String id, String[] presets) {
        StringBuilder namesString = new StringBuilder("(");
        for (int i = 0; i < presets.length; ++i) {
            if (i == presets.length - 1) {
                namesString.append("'").append(presets[i]).append("'");
            } else {
                namesString.append("'").append(presets[i]).append("'").append(",");
            }
        }
        namesString.append(")");

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("DELETE FROM preset WHERE user_id=%s AND preset_name IN %s;", "'" + id + "'", namesString.toString()));
        } catch (SQLException e) {
            dbLog.error("Error executing clearPreset",e);
        }
    }

    @Override
    public void clearLocationsPresets(String id, Location[] locations) {
        StringBuilder locationsString = new StringBuilder("(");
        for (int i = 0; i < locations.length; ++i) {
            if (i == locations.length - 1) {
                locationsString.append("'").append(locations[i].toString().replace("'", "\\'")).append("'");
            } else {
                locationsString.append("'").append(locations[i].toString().replace("'", "\\'")).append("',");
            }
        }
        locationsString.append(")");

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("DELETE FROM preset WHERE user_id=%s AND location IN %s;", "'" + id + "'", locationsString.toString()));
        } catch (SQLException e) {
            dbLog.error("Error executing clearLocationsPresets",e);
        }
    }

    @Override
    public void clearLocationsPokemon(final String id, final Location[] locations) {
        StringBuilder locationsString = new StringBuilder("(");
        for (int i = 0; i < locations.length; ++i) {
            if (i == locations.length - 1) {
                locationsString.append("'").append(locations[i].toString().replace("'", "\\'")).append("'");
            } else {
                locationsString.append("'").append(locations[i].toString().replace("'", "\\'")).append("',");
            }
        }
        locationsString.append(")");

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("DELETE FROM pokemon WHERE user_id=%s AND location IN %s;", "'" + id + "'", locationsString.toString()));
        } catch (SQLException e) {
            dbLog.error("Error executing clearLocationsPokemon",e);
        }
    }

    @Override
    public void clearLocationsRaids(String id, Location[] locations) {
        StringBuilder locationsString = new StringBuilder("(");
        for (int i = 0; i < locations.length; ++i) {
            if (i == locations.length - 1) {
                locationsString.append("'").append(locations[i].toString().replace("'", "\\'")).append("'");
            } else {
                locationsString.append("'").append(locations[i].toString().replace("'", "\\'")).append("',");
            }
        }
        locationsString.append(")");

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("DELETE FROM raid WHERE user_id=%s AND location IN %s;", "'" + id + "'", locationsString.toString()));
        } catch (SQLException e) {
            dbLog.error("Error executing clearLocationsRaids",e);
        }
    }

    @Override
    public void clearPokemon(final String id, final ArrayList<Pokemon> pokemons) {
        StringBuilder idsString = new StringBuilder("(");
        for (int i = 0; i < pokemons.size(); ++i) {
            if (i == pokemons.size() - 1) {
                idsString.append(pokemons.get(i).getID());
            } else {
                idsString.append(pokemons.get(i).getID()).append(",");
            }
        }
        idsString.append(")");

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("DELETE FROM pokemon WHERE user_id=%s AND id IN %s;", "'" + id + "'", idsString.toString()));
        } catch (SQLException e) {
            dbLog.error("Error executing clearPokemon",e);
        }
    }

    @Override
    public void clearRaid(final String id, final ArrayList<Raid> raids) {
        StringBuilder idsString = new StringBuilder("(");
        for (int i = 0; i < raids.size(); ++i) {
            if (i == raids.size() - 1) {
                idsString.append(raids.get(i).bossId);
            } else {
                idsString.append(raids.get(i).bossId).append(",");
            }
        }
        idsString.append(")");

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("DELETE FROM raid WHERE user_id=%s AND boss_id IN %s;", "'" + id + "'", idsString.toString()));
        } catch (SQLException e) {
            dbLog.error("Error executing clearRaid",e);
        }
    }

    @Override
    public int countPokemon(final String id, boolean countLocations) {
        int pokemon = 0;

        String sql;

        if (countLocations) {
            sql = "SELECT count(id) FROM pokemon WHERE user_id=?";
        } else {
            sql = "SELECT count(DISTINCT(id)) FROM pokemon WHERE user_id=?";
        }

        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                pokemon = rs.getInt(1);
            }
        } catch (SQLException e) {
            dbLog.error("Error executing countPokemon",e);
        }
        return pokemon;
    }

//    public boolean shouldNotify(final String userID, final PokeSpawn pokeSpawn) {
//
//        try (Connection connection = getNbConnection();
//             Statement statement = connection.createStatement()) {
//            statement.executeQuery(String.format("SELECT * FROM pokemon WHERE ((user_id=%s) AND ((location=%s) OR (location='All')) AND (id=%s) AND (min_iv <= %s) AND (max_iv >= %s));", "'" + userID + "'", "'" + pokeSpawn.region + "'", pokeSpawn.id, pokeSpawn.iv, pokeSpawn.iv));
//            final ResultSet rs = statement.getResultSet();
//            if (!rs.next()) {
//                connection.close();
//                statement.close();
//                return false;
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return true;
//    }

    @Override
    public int countPresets(String userID, boolean countLocations) {
        int presets = 0;

        String sql;

        if (countLocations) {
            sql = "SELECT count(preset_name) FROM preset WHERE user_id=?";
        } else {
            sql = "SELECT count(DISTINCT(preset_name)) FROM preset WHERE user_id=?";
        }

        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, userID);

            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                presets = rs.getInt(1);
            }
        } catch (SQLException e) {
            dbLog.error("Error executing countPresets",e);
        }
        return presets;
    }

    @Override
    public int countRaids(final String id, boolean countLocations) {
        int raids = 0;

        String sql;

        if (countLocations) {
            sql = "SELECT count(boss_id) FROM raid WHERE user_id=?";
        } else {
            sql = "SELECT count(DISTINCT(boss_id)) FROM raid WHERE user_id=?";
        }

        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);
            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                raids = rs.getInt(1);
            }
        } catch (SQLException e) {
            dbLog.error("Error executing countRaids",e);
        }
        return raids;
    }

    public int countSpawns(int id, TimeUnit intervalType, int intervalLength) {
        int numSpawns = 0;

        String sql = null;
        switch (novaBot.getConfig().getScannerType()) {
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
                        (novaBot.getConfig().getScanProtocol().equals("postgresql") ?
                            "extract(epoch from (now() - INTERVAL '" + intervalLength + "' " + intervalType.toDbString() + "))":
                            "UNIX_TIMESTAMP(UTC_TIMESTAMP() - INTERVAL '" + intervalLength + "'" + intervalType.toDbString() + ")");
                break;
        }

        try (Connection connection = getScanConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            switch (novaBot.getConfig().getScannerType()){
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

    @Override
    public void deletePokemon(final String userID, final Pokemon pokemon) {

        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("" +
                     "DELETE FROM pokemon " +
                     "WHERE ((user_id=?) " +
                     "AND (LOWER(location)=LOWER(?)) " +
                     "AND (id=?) " +
                     "AND (min_iv=?) " +
                     "AND (max_iv=?)) " +
                     "AND (min_lvl=?) " +
                     "AND (max_lvl=?) " +
                     "AND (min_cp=?) " +
                     "AND (max_cp=?)")) {
            statement.setString(1, userID);
            statement.setString(2, (pokemon.getLocation().toString() == null) ? "All" : pokemon.getLocation().toString());
            statement.setDouble(3, pokemon.getID());
            statement.setDouble(4, pokemon.miniv);
            statement.setDouble(5, pokemon.maxiv);
            statement.setInt(6, pokemon.minlvl);
            statement.setInt(7, pokemon.maxlvl);
            statement.setInt(8, pokemon.mincp);
            statement.setInt(9, pokemon.maxcp);
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing deletePokemon",e);
        }
    }

    @Override
    public void deletePreset(String userId, String preset, Location location) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("" +
                     "DELETE FROM preset " +
                     "WHERE ((user_id=?) " +
                     "AND (LOWER(location)=LOWER(?)) " +
                     "AND (preset_name=?))")) {
            statement.setString(1, userId);
            statement.setString(2, location.toDbString());
            statement.setString(3, preset);

            dbLog.info(statement.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing deletePreset",e);
        }
    }

    @Override
    public void deleteRaid(final String userID, final Raid raid) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("" +
                     "DELETE FROM raid " +
                     "WHERE ((user_id=?) " +
                     "AND (boss_id=?) " +
                     "AND (LOWER(location)=LOWER(?)))")
        ) {
            statement.setString(1, userID);
            statement.setDouble(2, raid.bossId);
            statement.setString(3, raid.location.toDbString());
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing deleteRaid",e);
        }
    }

    @Override
    public void endLobby(String lobbyCode) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM raidlobby WHERE lobby_id = ?")
        ) {
            statement.setInt(1, Integer.parseInt(lobbyCode));
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing endLobby",e);
        }
    }

    public ArrayList<RaidLobby> getActiveLobbies() {
        ArrayList<RaidLobby> activeLobbies = new ArrayList<>();

        ArrayList<String> toDelete = null;


        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT lobby_id,gym_id,channel_id,role_id,next_timeleft_update,invite_code FROM raidlobby;")
        ) {

            ResultSet rs = statement.executeQuery();


            while (rs.next()) {
                String lobbyCode = String.format("%04d", rs.getInt(1));
                String gymId = rs.getString(2);
                String channelId = rs.getString(3);
                String roleId = rs.getString(4);
                int nextTimeLeftUpdate = rs.getInt(5);
                String inviteCode = rs.getString(6);

                dbLog.info(String.format("Found lobby with info %s,%s,%s,%s in the db, checking for known raid", lobbyCode, gymId, channelId, roleId));

                RaidSpawn spawn = knownRaids.get(gymId);

                if (spawn == null) {
                    dbLog.warn(String.format("Couldn't find a known raid for gym id %s which was found as an active raid lobby, queuing for deletion", gymId));
                    if (toDelete == null) {
                        toDelete = new ArrayList<>();
                    }

                    toDelete.add(lobbyCode);
                } else {
                    dbLog.info(String.format("Found a raid for gym id %s, lobby code %s", gymId, lobbyCode));
                    RaidLobby lobby = new RaidLobby(spawn, lobbyCode, novaBot, channelId, roleId, inviteCode, true);
                    lobby.nextTimeLeftUpdate = nextTimeLeftUpdate;
                    lobby.loadMembers();

                    activeLobbies.add(lobby);
                }
            }
        } catch (SQLException e) {
            dbLog.error("Error executing getActiveLobbies",e);
        }


        if (toDelete != null) {
            dbLog.info("Deleting non-existent lobbies");
            endLobbies(toDelete);
        }

        return activeLobbies;
    }

    public void getCurrentRaids() {
        dbLog.info("Checking which known gyms need to be updated");

        checkKnownRaids();

        dbLog.info("Done");
//        dbLog.log(DEBUG,knownRaids);

        dbLog.info("Getting new raids");

        StringBuilder knownIdQMarks = new StringBuilder();

        if (knownRaids.size() > 0) {
            if (novaBot.getConfig().getScannerType() == ScannerType.PhilMap || novaBot.getConfig().getScannerType() == ScannerType.RocketMap) {
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

        ArrayList<String> knownIds = new ArrayList<>();

        knownIds.addAll(knownRaids.keySet());

        if (knownRaids.containsKey(null)) {
            System.out.println("NULL, OH NO KNOWNRAIDS CONTAINS NULL? WHY???");
            System.out.println(knownRaids.get(null));
        }

        String sql = null;

        switch (novaBot.getConfig().getScannerType()) {
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
                        (novaBot.getConfig().getScanProtocol().equals("mysql")
                                ? "UNIX_TIMESTAMP(? + INTERVAL 1 MINUTE)"
                                : "extract(epoch from (?::timestamptz + INTERVAL '1' MINUTE))");
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
                        (novaBot.getConfig().getScanProtocol().equals("mysql")
                                ? "UNIX_TIMESTAMP(? + INTERVAL 1 MINUTE)"
                                : "extract(epoch from (?::timestamptz + INTERVAL '1' MINUTE))");
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
                if (novaBot.getConfig().getScanProtocol().equals("mysql")) {
                    statement.setString(i + 1, knownIds.get(i));
                } else {
                    statement.setInt(i + 1, Integer.parseInt(knownIds.get(i)));
                }
            }
            switch (novaBot.getConfig().getScannerType()) {
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

            while (rs.next()) {
                rows++;
                RaidSpawn raidSpawn = null;
                String gymId = null;

//                System.out.println("Current UTC time: " + ZonedDateTime.now(UtilityFunctions.UTC));
//                System.out.println("Current time based on config timezone : " + UtilityFunctions.getCurrentTime(novaBot.config.getTimeZone()));
                switch (novaBot.getConfig().getScannerType()) {
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

                novaBot.notificationsManager.raidQueue.add(raidSpawn);
            }
        } catch (SQLException e) {
            lastCheckedRaids = ZonedDateTime.now(UtilityFunctions.UTC);
            dbLog.error("Error executing getCurrentRaids",e);
        }
        dbLog.info(String.format("Returned %s rows", rows));

    }

    @Override
    public GeocodedLocation getGeocodedLocation(final double lat, final double lon) {
        GeocodedLocation geocodedLocation = null;

        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("" +
                     "SELECT suburb,street_num,street,state,postal,neighbourhood,sublocality,country " +
                     "FROM spawninfo " +
                     "WHERE lat = ? AND lon = ?")) {
            statement.setDouble(1, lat);
            statement.setDouble(2, lon);
            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {

                geocodedLocation = new GeocodedLocation();

                String city = rs.getString(1);
                if(rs.wasNull()){
                    city = "unkn";
                }
                geocodedLocation.set("city", city);

                String streetNum = rs.getString(2);
                if(rs.wasNull()){
                    streetNum = "unkn";
                }
                geocodedLocation.set("street_num", streetNum);

                String street = rs.getString(3);
                if(rs.wasNull()){
                    street = "unkn";
                }
                geocodedLocation.set("street", street);

                String state = rs.getString(4);
                if(rs.wasNull()){
                    state = "unkn";
                }
                geocodedLocation.set("state", state);

                String postal = rs.getString(5);
                if(rs.wasNull()){
                    postal = "unkn";
                }
                geocodedLocation.set("postal", postal);

                String neighbourhood = rs.getString(6);
                if(rs.wasNull()){
                    neighbourhood = "unkn";
                }
                geocodedLocation.set("neighborhood", neighbourhood);

                String sublocality = rs.getString(7);
                if(rs.wasNull()){
                    sublocality = "unkn";
                }
                geocodedLocation.set("sublocality", sublocality);

                String country = rs.getString(8);
                if(rs.wasNull()){
                    country = "unkn";
                }
                geocodedLocation.set("country", country);

            }
        } catch (SQLException e) {
            dbLog.error("Error executing getGeocodedLocation",e);
        }

        return geocodedLocation;
    }

    public void getNewPokemon() {
        dbLog.info("Getting new pokemon");

        final ArrayList<PokeSpawn> pokeSpawns = new ArrayList<>();

        if (blacklistQMarks == null) {
            blacklistQMarks = new StringBuilder("(");
            for (int i = 0; i < novaBot.getConfig().getBlacklist().size(); ++i) {
                blacklistQMarks.append("?");
                if (i != novaBot.getConfig().getBlacklist().size() - 1) {
                    blacklistQMarks.append(",");
                }
            }
            blacklistQMarks.append(")");
        }

        String sql = null;
        switch (novaBot.getConfig().getScannerType()) {
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
                        "WHERE pokemon_id NOT IN " + blacklistQMarks + " " +
                        "AND expire_timestamp > " +
                        (novaBot.getConfig().getScanProtocol().equals("mysql")
                                ? "UNIX_TIMESTAMP(now() - INTERVAL ? SECOND)"
                                : "extract(epoch from now())");
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
                        "WHERE pokemon_id NOT IN " + blacklistQMarks + " " +
                        "AND updated >= " +
                        (novaBot.getConfig().getScanProtocol().equals("mysql")
                                ? "UNIX_TIMESTAMP(? - INTERVAL 1 SECOND)"
                                : "extract(epoch from (?::timestamptz - INTERVAL '1' SECOND)) ") +
                        "AND expire_timestamp > " +
                        (novaBot.getConfig().getScanProtocol().equals("mysql")
                                ? "UNIX_TIMESTAMP(now() - INTERVAL ? SECOND)"
//                            : "extract(epoch from (now() - INTERVAL ? SECOND))");
                                : "extract(epoch from now())");
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
                        "WHERE pokemon_id NOT IN " + blacklistQMarks + " " +
                        "AND last_modified >= (? - INTERVAL 1 SECOND) " +
                        "AND disappear_time > (UTC_TIMESTAMP() - INTERVAL ? SECOND)";
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
                        "WHERE pokemon_id NOT IN " + blacklistQMarks + " " +
                        "AND last_modified >= (? - INTERVAL 1 SECOND) " +
                        "AND disappear_time > (UTC_TIMESTAMP() - INTERVAL ? SECOND)";
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
                        "       cp, " +
                        "       cp_multiplier, " +
                        "       weather_boosted_condition " +
                        "FROM pokemon " +
                        "WHERE pokemon_id NOT IN " + blacklistQMarks + " " +
                        "AND last_modified >= (? - INTERVAL 1 SECOND) " +
                        "AND disappear_time > (UTC_TIMESTAMP() - INTERVAL ? SECOND)";
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
                        "       cp, " +
                        "       cp_multiplier, " +
                        "       weather_id " +
                        "FROM pokemon " +
                        "WHERE pokemon_id NOT IN " + blacklistQMarks + " " +
                        "AND last_modified >= (? - INTERVAL 1 SECOND) " +
                        "AND disappear_time > (UTC_TIMESTAMP() - INTERVAL ? SECOND)";
                break;
        }

        int rows = 0;
        int newSpawns = 0;
        try (Connection connection = getScanConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= novaBot.getConfig().getBlacklist().size(); ++i) {
                statement.setInt(i, novaBot.getConfig().getBlacklist().get(i - 1));
            }

            switch (novaBot.getConfig().getScannerType()) {
                case RocketMap:
                case SloppyRocketMap:
                case SkoodatRocketMap:
                case PhilMap:
                    statement.setObject(novaBot.getConfig().getBlacklist().size() + 1, lastChecked.toLocalDateTime(), Types.TIMESTAMP);
                    break;
                case Hydro74000Monocle:
                    LocalDateTime localDateTime = lastChecked.withZoneSameInstant(novaBot.getConfig().getTimeZone()).toLocalDateTime();
                    String timeStamp = String.format("%s %s", localDateTime.toLocalDate(), localDateTime.toLocalTime());

                    statement.setString(novaBot.getConfig().getBlacklist().size() + 1, timeStamp);
//                    statement.setObject(novaBot.config.getBlacklist().size() + 1, lastChecked.withZoneSameInstant(novaBot.config.getTimeZone()).toLocalDateTime(), Types.TIMESTAMP);
                    break;
            }
            if (novaBot.getConfig().getScanProtocol().equals("mysql")) {
                statement.setString(novaBot.getConfig().getBlacklist().size() +
                                (novaBot.getConfig().getScannerType() == ScannerType.Monocle ? 1 : 2),
                        novaBot.getConfig().getMinSecondsLeft().toString());
            }

            dbLog.info("Executing query:" + statement);
            lastChecked = ZonedDateTime.now(UtilityFunctions.UTC);
            final ResultSet rs = statement.executeQuery();
            dbLog.info("Query complete");

            rows = 0;

            while (rs.next()) {
//                System.out.println("Current UTC time: " + ZonedDateTime.now(UtilityFunctions.UTC));
//                System.out.println("Current time based on config timezone : " + UtilityFunctions.getCurrentTime(novaBot.config.getTimeZone()));

                PokeSpawn pokeSpawn = null;

                switch (novaBot.getConfig().getScannerType()) {
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
                        cp = (Integer) rs.getObject(14);
                        cpMod = rs.getDouble(15);
                        int weather = rs.getInt(16);
                        pokeSpawn = new PokeSpawn(id, lat, lon, disappearTime, attack, defense, stamina, move1, move2, weight, height, gender, form, cp, cpMod, weather);
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

                    if (!hashCodes.contains(pokeSpawn.hashCode())) {
                        dbLog.debug("new pokemon, adding to list");
                        newSpawns++;
                        hashCodes.add(pokeSpawn.hashCode());

                        novaBot.notificationsManager.pokeQueue.add(pokeSpawn);
//                        pokeSpawns.add(pokeSpawn);
                    } else {
                        dbLog.debug("pokemon already seen, ignoring");
                    }
                } catch (Exception e) {
                    dbLog.error("Error executing getNewPokemon",e);
                }

                rows++;
            }
        } catch (SQLException e) {
            lastChecked = ZonedDateTime.now(UtilityFunctions.UTC);
            dbLog.error("Error executing getNewPokemon",e);
        }
        dbLog.info(String.format("Returned %s rows, %s new pokemon", rows, newSpawns));
//        return pokeSpawns;
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(final RaidSpawn raidSpawn) {
        final ArrayList<String> ids = new ArrayList<>();

        int geofences = raidSpawn.getGeofences().size();

        StringBuilder geofenceQMarks = new StringBuilder();
        for (int i = 0; i < geofences; ++i) {
            geofenceQMarks.append("?");
            if (i != geofences - 1) {
                geofenceQMarks.append(",");
            }
        }
        if (geofences > 0) geofenceQMarks.append(",");

        String sql = String.format(
                "SELECT DISTINCT(user_id) " +
                        "FROM raid " +
                        "WHERE (SELECT paused FROM users WHERE users.id = raid.user_id) = FALSE " +
                        "AND LOWER(location) IN (%s%s'all') " +
                        "AND boss_id=?;", geofenceQMarks.toString(), (novaBot.suburbsEnabled() ? "?, " : "")
        );

        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            for (int i = 0; i < geofences; i++) {
                statement.setString(i + 1, raidSpawn.getGeofences().get(i).name.toLowerCase());
            }
            int offset = 1;
            if (novaBot.suburbsEnabled()) {
                statement.setString(geofences + offset, raidSpawn.getProperties().get(novaBot.getConfig().getGoogleSuburbField()).toLowerCase());
                offset++;
            }
            statement.setInt(geofences + offset, raidSpawn.bossId);
            dbLog.debug(statement.toString());
            System.out.println(statement);
            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
        } catch (SQLException e) {
            dbLog.error("Error executing getUserIDsToNotify(RaidSpawn)",e);
        }
        dbLog.debug("Found " + ids.size() + " to notify");
        return ids;
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(String preset, Spawn spawn) {
        ArrayList<String> ids = new ArrayList<>();

        int geofences = spawn.getGeofences().size();

        StringBuilder geofenceQMarks = new StringBuilder();
        for (int i = 0; i < geofences; ++i) {
            geofenceQMarks.append("?");
            if (i != geofences - 1) {
                geofenceQMarks.append(",");
            }
        }
        if (geofences > 0) geofenceQMarks.append(",");

        String sql = String.format(
                "SELECT user_id " +
                        "FROM preset " +
                        "WHERE (SELECT paused FROM users WHERE users.id = preset.user_id) = FALSE " +
                        "AND (LOWER(location) IN (%s%s'all'))" +
                        "AND (preset_name = ?)", geofenceQMarks.toString(), novaBot.suburbsEnabled() ? "?," : ""
        );

        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql)
        ) {
            for (int i = 0; i < geofences; i++) {
                statement.setString(i + 1, spawn.getGeofences().get(i).name.toLowerCase());
            }
            int offset = 1;
            if (novaBot.suburbsEnabled()) {
                statement.setString(geofences + offset, spawn.getProperties().get(novaBot.getConfig().getGoogleSuburbField()).toLowerCase());
                offset++;
            }
            statement.setString(geofences + offset, preset);

            dbLog.info(statement.toString());
            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
        } catch (SQLException e) {
            dbLog.error("Error executing getUserIDsToNotify(Preset)",e);
        }

        return ids;
    }

    @Override
    public ArrayList<String> getUserIDsToNotify(final PokeSpawn pokeSpawn) {
        final ArrayList<String> ids = new ArrayList<>();

        int geofences = pokeSpawn.getGeofences().size();

        StringBuilder geofenceQMarks = new StringBuilder();
        for (int i = 0; i < geofences; ++i) {
            geofenceQMarks.append("?");
            if (i != geofences - 1) {
                geofenceQMarks.append(",");
            }
        }
        if (geofences > 0) geofenceQMarks.append(",");

        String sql = String.format(
                "SELECT DISTINCT(user_id) " +
                        "FROM pokemon " +
                        "WHERE (SELECT paused FROM users WHERE users.id = pokemon.user_id) = FALSE " +
                        "AND ((LOWER(location) IN (%s" + (novaBot.suburbsEnabled() ? "?," : "") + "'all')) " +
                        "AND (id=? OR id=?) " +
                        "AND (min_iv <= ?) " +
                        "AND (max_iv >= ?) " +
                        "AND (min_lvl <= ?) " +
                        "AND (max_lvl >= ?) " +
                        "AND (min_cp <= ?) " +
                        "AND (max_cp >= ?));", geofenceQMarks.toString()
        );

        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            for (int i = 0; i < geofences; i++) {
                statement.setString(i + 1, pokeSpawn.getGeofences().get(i).name.toLowerCase());
            }

            int offset = 1;

            if (novaBot.suburbsEnabled()) {
                statement.setString(geofences + offset, pokeSpawn.getProperties().get(novaBot.getConfig().getGoogleSuburbField()).toLowerCase());
                offset++;
            }
            statement.setInt(geofences + offset, pokeSpawn.id);
            offset++;
            statement.setInt(geofences + offset, (pokeSpawn.form != null) ? 201 : pokeSpawn.id);
            offset++;
            statement.setDouble(geofences + offset, (pokeSpawn.iv == null ? 0 : pokeSpawn.iv));
            offset++;
            statement.setDouble(geofences + offset, (pokeSpawn.iv == null ? 0 : pokeSpawn.iv));
            offset++;
            statement.setInt(geofences + offset, pokeSpawn.level == null ? 0 : pokeSpawn.level);
            offset++;
            statement.setInt(geofences + offset, pokeSpawn.level == null ? 0 : pokeSpawn.level);
            offset++;
            statement.setInt(geofences + offset, pokeSpawn.cp == null ? 0 : pokeSpawn.cp);
            offset++;
            statement.setInt(geofences + offset, pokeSpawn.cp == null ? 0 : pokeSpawn.cp);
            dbLog.debug(statement.toString());
            System.out.println(statement);
            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
        } catch (SQLException e) {
            dbLog.error("Error executing getUserIDsToNotify(PokeSpawn)",e);
        }
        dbLog.debug("Found " + ids.size() + " to notify");
        return ids;
    }

    @Override
    public UserPref getUserPref(final String id) {
        final UserPref userPref = new UserPref(novaBot);

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement()) {
            statement.executeQuery(String.format("SELECT id,location,max_iv,min_iv,max_lvl,min_lvl,max_cp,min_cp FROM pokemon WHERE user_id=%s", "'" + id + "'"));
            ResultSet rs = statement.getResultSet();

            while (rs.next()) {
                final int pokemon_id = rs.getInt(1);
                final Location location = Location.fromDbString(rs.getString(2).toLowerCase(), novaBot);
                final float max_iv = rs.getFloat(3);
                final float min_iv = rs.getFloat(4);
                final int max_lvl = rs.getInt(5);
                final int min_lvl = rs.getInt(6);
                final int max_cp = rs.getInt(7);
                final int min_cp = rs.getInt(8);
                if (location == null) {
                    novaBot.novabotLog.warn("Location null fromDbString " + rs.getString(2).toLowerCase());
                } else {
                    userPref.addPokemon(new Pokemon(Pokemon.getFilterName(pokemon_id), location, min_iv, max_iv, min_lvl, max_lvl,min_cp,max_cp));
                }
            }

            statement.executeQuery(String.format("SELECT boss_id,location FROM raid WHERE user_id='%s'", id));

            rs = statement.getResultSet();

            while (rs.next()) {
                final int bossId = rs.getInt(1);
                final Location location = Location.fromDbString(rs.getString(2).toLowerCase(), novaBot);

                if (location == null){
                    dbLog.warn("Location is null, skipping raid setting");
                    continue;
                }
                userPref.addRaid(new Raid(bossId, location));
            }

            statement.executeQuery(String.format("SELECT preset_name, location FROM preset WHERE user_id = '%s'", id));

            rs = statement.getResultSet();

            while (rs.next()) {
                String presetName = rs.getString(1);
                Location location = Location.fromDbString(rs.getString(2).toLowerCase(), novaBot);

                if (location == null){
                    dbLog.warn("Location is null, skipping preset setting");
                    continue;
                }

                userPref.addPreset(presetName, location);
            }
        } catch (SQLException e) {
            dbLog.error("Error executing getUserPref",e);
        }
        return userPref;
    }

    @Override
    public int highestRaidLobbyId() {

        int highest = 0;

        try (Connection connection = getNbConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT MAX(lobby_id) FROM raidlobby;")
        ) {

            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                highest = rs.getInt(1);
            }

        } catch (SQLException e) {
            dbLog.error("Error executing highestRaidLobbyId",e);
        }

        dbLog.info("Next raid lobby ID will be " + highest);

        return highest;
    }

    @Override
    public void logNewUser(final String userID) {
        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement()) {
            final Date date = new Date();
            final Timestamp timestamp = new Timestamp(date.getTime());
            statement.executeUpdate(String.format("INSERT INTO users (id,joindate) VALUES ('%s','%s');", userID, timestamp));
        } catch (SQLIntegrityConstraintViolationException e) {
            dbLog.debug("Tried to add a duplicate user");
        } catch (SQLException e) {
            dbLog.error("Error executing logNewUser",e);
        }
    }

    public static void main(final String[] args) {

        NovaBot novaBot = new NovaBot();
        novaBot.setup();
//        novaBot.dbManager.novabotdbConnect();

        System.out.println(new PokeSpawn(
                143,
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
                0,
                200,
                .1).buildMessage("formatting.ini").getContentRaw());

        System.out.println(new RaidSpawn(
                "gym",
                "id",
                -35.265134, 149.122796,
                Team.Valor,
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(120), UtilityFunctions.UTC),
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(60), UtilityFunctions.UTC),
                383,
                155555,
                18,
                22,
                5).buildMessage("formatting.ini").getEmbeds().get(0).getDescription());

//        for (String gym : novaBot.dbManager.getUserIDsToNotify(new RaidSpawn("gym", "", -35.265134, 149.122796, Team.Uncontested, ZonedDateTime.now(), ZonedDateTime.now(), 383, 51000, 0, 0, 5))) {
//            System.out.println(gym);
//        }

//        try (Connection connection = novaBot.dbManager.getScanConnection();
//             Statement statement = connection.createStatement()) {
//            statement.executeQuery("SELECT extract(epoch FROM (now() - INTERVAL '60' SECOND))");
//
//            ResultSet rs = statement.getResultSet();
//
//            while (rs.next()) {
//                System.out.println(statement.getResultSet().getDouble(1));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        novaBot.dbManager.addPokemon("246738412315475969",new Pokemon(2,Location.ALL,0,100));
//        for (PokeSpawn pokeSpawn : novaBot.dbManager.getNewPokemon()) {
//            System.out.println(pokeSpawn);
//        }
//        for (String s : novaBot.dbManager.getUserIDsToNotify(new PokeSpawn(
//                143,
//                35,
//                149,
//                ZonedDateTime.ofInstant(Instant.now().plusSeconds(60),UtilityFunctions.UTC),
//                15,
//                15,
//                15,
//                "",
//                "",
//                0,
//                0,
//                0,
//                0,
//                200,
//                .1))) {
//            System.out.println(s);
//        }

        //        MessageListener.main(null);
//
//        testing = true;
//
////        dbLog
//
//        loadConfig();
//        loadSuburbs();
//
//        novabotdbConnect();
//
//        System.out.println(getUserPref("107730875596169216").allPresetsToString());
//        System.out.println(config.findMatchingPresets(new RaidSpawn("raid","123",145,35,Team.Uncontested,DBManager.getCurrentTime(),DBManager.getCurrentTime(),3,1,1,1,3)));
//
//        System.out.println(getUserIDsToNotify("test",new RaidSpawn("raid","123",145,35,Team.Uncontested,DBManager.getCurrentTime(),DBManager.getCurrentTime(),1,1,1,1,2)));
//
//        PokeSpawn pokeSpawn = new PokeSpawn(143,35,149,new Timestamp(DBManager.getCurrentTime().getTime() + 504000), 15,15,15,"","",0,0,0,0,200,.1);
//
//        getUserIDsToNotify(pokeSpawn).forEach(System.out::println);
//        RaidSpawn spawn = new RaidSpawn("gym",
//                "12345", -35.265134, 149.122796,
//                new Timestamp(DBManager.getCurrentTime().getTime() + 504000),
//                new Timestamp(DBManager.getCurrentTime().getTime() + 6000000),
//                248,
//                11003,
//                "fire",
//                "fire blast",
//                4);
//
//        knownRaids.put(spawn.gymId, spawn);

//        getActiveLobbies();


//        System.out.println(getUserIDsToNotify(spawn));
//        System.out.println(pokeSpawn);

//        System.out.println(getUserIDsToNotify(pokeSpawn));

//        RaidSpawn raidSpawn = new RaidSpawn("gym","123",-35,-149,)
    }

    @Override
    public void newLobby(String lobbyCode, String gymId, int memberCount, String channelId, String roleId, long nextTimeLeftUpdate, String inviteCode) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO raidlobby (lobby_id, gym_id, members, channel_id, role_id, next_timeleft_update,invite_code) VALUES (?,?,?,?,?,?,?)")
        ) {
            statement.setInt(1, Integer.parseInt(lobbyCode));
            statement.setString(2, gymId);
            statement.setInt(3, memberCount);
            statement.setString(4, channelId);
            statement.setString(5, roleId);
            statement.setInt(6, (int) nextTimeLeftUpdate);
            statement.setString(7, inviteCode);
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing newLobby",e);
        }
    }

    @Override
    public boolean notContainsUser(final String userID) {
        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement()) {
            statement.executeQuery(String.format("SELECT * FROM users WHERE id = %s;", "'" + userID + "'"));
            final ResultSet rs = statement.getResultSet();
            if (!rs.next()) {
                connection.close();
                statement.close();
                return true;
            }
        } catch (SQLException e) {
            dbLog.error("Error executing notContainsUser",e);
        }
        return false;
    }

    public void novabotdbConnect() {
        boolean mysql = novaBot.getConfig().getNbProtocol().equals("mysql");

        nbUrl = String.format("jdbc:%s://%s:%s/%s%s",
                novaBot.getConfig().getNbProtocol(),
                novaBot.getConfig().getNbIp(),
                novaBot.getConfig().getNbPort(),
                novaBot.getConfig().getNbDbName(),
                mysql ? "?useSSL=" + novaBot.getConfig().getNbUseSSL() : "");

        try {
            novaBotDataSource = com.github.novskey.novabot.data.DataSource.getInstance(
                    (mysql ? MySQL_DRIVER : PgSQL_DRIVER),
                    novaBot.getConfig().getNbUser(),
                    novaBot.getConfig().getNbPass(),
                    nbUrl,
                    novaBot.getConfig().getNbMaxConnections()
            );
        } catch (IOException | SQLException | PropertyVetoException e) {
            dbLog.error("Error executing novabotdbConnect",e);
        }

    }

    @Override
    public void pauseUser(String id) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET paused = TRUE WHERE id = ?")
        ) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing pauseUser",e);
        }
    }

    @Override
    public void resetPokemon(String id) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM pokemon WHERE user_id = ?")) {
            statement.setString(1,id);
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing resetPokemon",e);
        }
    }

    @Override
    public void resetPresets(String id) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM preset WHERE user_id = ?")) {
            statement.setString(1,id);
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing resetPresets",e);
        }
    }

    @Override
    public void resetRaids(String id) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM raid WHERE user_id = ?")) {
            statement.setString(1,id);
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing resetRaids",e);
        }
    }

    @Override
    public void resetUser(final String id) {
        resetRaids(id);
        resetPokemon(id);
        resetPresets(id);
    }

    public void scanDbConnect() {
        boolean mysql = novaBot.getConfig().getScanProtocol().equals("mysql");

        scanUrl = String.format("jdbc:%s://%s:%s/%s%s",
                novaBot.getConfig().getScanProtocol(),
                novaBot.getConfig().getScanIp(),
                novaBot.getConfig().getScanPort(),
                novaBot.getConfig().getScanDbName(),
                mysql ? "?useSSL=" + novaBot.getConfig().getScanUseSSL() : "");

        try {
            scanDataSource = com.github.novskey.novabot.data.DataSource.getInstance(
                    (mysql ? MySQL_DRIVER : PgSQL_DRIVER),
                    novaBot.getConfig().getScanUser(),
                    novaBot.getConfig().getScanPass(),
                    scanUrl,
                    novaBot.getConfig().getScanMaxConnections()
            );
        } catch (IOException | SQLException | PropertyVetoException e) {
            dbLog.error("Error executing scanDbConnect",e);
        }
    }

    @Override
    public void setGeocodedLocation(final double lat, final double lon, GeocodedLocation location) {

        dbLog.info("inserting location");
        dbLog.info(location.getProperties().toString());

        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("" +
                     "INSERT INTO spawninfo " +
                     "(lat,lon,suburb,street_num,street,state,postal,neighbourhood,sublocality,country) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?) " +
                     (novaBot.getConfig().getNbProtocol().equals("mysql") ? "ON DUPLICATE KEY UPDATE " : "ON CONFLICT (lat,lon) DO UPDATE SET  ") +
                     "suburb = ?," +
                     "street_num = ?," +
                     "street = ?," +
                     "state = ?," +
                     "postal = ?," +
                     "neighbourhood = ?," +
                     "sublocality = ?," +
                     "country = ?")) {
            statement.setDouble(1, lat);
            statement.setDouble(2, lon);
            statement.setString(3, location.getProperties().get("city"));
            statement.setString(4, location.getProperties().get("street_num"));
            statement.setString(5, location.getProperties().get("street"));
            statement.setString(6, location.getProperties().get("state"));
            statement.setString(7, location.getProperties().get("postal"));
            statement.setString(8, location.getProperties().get("neighborhood"));
            statement.setString(9, location.getProperties().get("sublocality"));
            statement.setString(10, location.getProperties().get("country"));
            statement.setString(11, location.getProperties().get("city"));
            statement.setString(12, location.getProperties().get("street_num"));
            statement.setString(13, location.getProperties().get("street"));
            statement.setString(14, location.getProperties().get("state"));
            statement.setString(15, location.getProperties().get("postal"));
            statement.setString(16, location.getProperties().get("neighborhood"));
            statement.setString(17, location.getProperties().get("sublocality"));
            statement.setString(18, location.getProperties().get("country"));

            System.out.println();
            statement.executeUpdate();

        } catch (SQLException e) {
            dbLog.error("Error executing setGeocodedLocation",e);
        }
    }

    @Override
    public void unPauseUser(String id) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET paused = FALSE WHERE id = ?")
        ) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing unPauseUser",e);
        }
    }

    @Override
    public void updateLobby(String lobbyCode, int memberCount, int nextTimeLeftUpdate, String inviteCode) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE raidlobby SET members = ?, next_timeleft_update = ?, invite_code = ? WHERE lobby_id = ?")
        ) {
            statement.setInt(1, memberCount);
            statement.setInt(2, nextTimeLeftUpdate);
            statement.setString(3, inviteCode);
            statement.setInt(4, Integer.parseInt(lobbyCode));
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing updateLobby",e);
        }
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

    private void endLobbies(ArrayList<String> toDelete) {

        StringBuilder qMarks = new StringBuilder();
        for (int i = 0; i < toDelete.size(); ++i) {
            qMarks.append("?");
            if (i != toDelete.size() - 1) {
                qMarks.append(",");
            }
        }

        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM raidlobby WHERE lobby_id IN (" + qMarks + ") ")
        ) {

            for (int i = 1; i <= toDelete.size(); i++) {
                statement.setInt(i, Integer.parseInt(toDelete.get(i - 1)));
            }

            statement.executeUpdate();


        } catch (SQLException e) {
            dbLog.error("Error executing endLobbies",e);
        }
    }

    private Connection getScanConnection() {
        try {
            return scanDataSource.getConnection();
        } catch (SQLException e) {
            dbLog.error("Error executing getScanConnection",e);
        }
        return null;
    }

    private Connection getNbConnection() {
        try {
            return novaBotDataSource.getConnection();
        } catch (SQLException e) {
            dbLog.error("Error executing getNbConnection",e);
        }
        return null;
    }

    @Override
    public int purgeUnknownSpawnpoints() {
        int rows = 0;
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM spawninfo WHERE country = 'unkn'")
        ) {
            rows = statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing purgeUnknownSpawnpoints",e);
        }
        return rows;
    }

    @Override
    public ZoneId getZoneId(double lat, double lon) {
        ZoneId zoneId = null;
        
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("" +
                     "SELECT timezone " +
                     "FROM spawninfo " +
                     "WHERE lat = ? AND lon = ?")) {
            statement.setDouble(1, lat);
            statement.setDouble(2, lon);
            final ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String timezone = rs.getString(1);
                if(rs.wasNull()){
                    zoneId = null;
                }else {
                    zoneId = ZoneId.of(timezone);
                }
            }
        } catch(SQLException e){
            dbLog.error("Error executing getZoneId",e);
        }

        return zoneId;
    }


  @Override
  public void setZoneId(double lat, double lon, ZoneId zoneId) {
      try (Connection connection = getNbConnection();
           PreparedStatement statement = connection.prepareStatement("" +
                   "INSERT INTO spawninfo " +
                   "(lat,lon,timezone)" +
                   "VALUES (?,?,?) " +
                   (novaBot.getConfig().getNbProtocol().equals("mysql") ? "ON DUPLICATE KEY UPDATE " : "ON CONFLICT (lat,lon) DO UPDATE SET  ") +
                   "timezone = ?"
                   ))
      {
          statement.setDouble(1,lat);
          statement.setDouble(2,lon);
          statement.setString(3,zoneId.toString());
          statement.setString(4,zoneId.toString());

          statement.executeUpdate();
      } catch (SQLException e) {
          dbLog.error("Error executing setZoneId",e);
      }
  }

    public ConcurrentHashMap<String, User> dumpUsers() {
        ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement())
        {
            ResultSet rs = statement.executeQuery("SELECT id, paused FROM users");

            while (rs.next()){
                String id = rs.getString(1);
                boolean paused = rs.getBoolean(2);

                users.put(id, new User(id, paused));
            }
        } catch (SQLException e) {
            dbLog.error("Error executing dumpUsers",e);
        }

        return users;
    }

    public ConcurrentHashMap<String, Set<Pokemon>> dumpPokemon() {
        ConcurrentHashMap<String, Set<Pokemon>> pokemons = new ConcurrentHashMap<>();

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement())
        {
            ResultSet rs = statement.executeQuery("SELECT user_id, id, max_iv, min_iv, max_lvl, min_lvl, max_cp, min_cp, location FROM pokemon");

            while (rs.next()){
                String userId = rs.getString(1);
                int pokemonId = rs.getInt(2);
                float maxIv = rs.getFloat(3);
                float minIv = rs.getFloat(4);
                int maxLvl = rs.getInt(5);
                int minLvl = rs.getInt(6);
                int maxCp = rs.getInt(7);
                int minCp = rs.getInt(8);
                Location location = Location.fromDbString(rs.getString(9).toLowerCase(),novaBot);
                if (location == null){
                    dbLog.warn("Location is null, not dumping pokemon setting");
                    continue;
                }

                Set<Pokemon> userSettings = pokemons.get(userId);
                if(userSettings == null){
                    userSettings =  ConcurrentHashMap.newKeySet();
                    pokemons.put(userId,userSettings);
                }
                userSettings.add(new Pokemon(pokemonId,location,minIv,maxIv,minLvl,maxLvl,minCp,maxCp));
            }
        } catch (SQLException e) {
            dbLog.error("Error executing dumpPokemon",e);
        }

        return pokemons;
    }

    public ConcurrentHashMap<String,Set<Raid>> dumpRaids() {
        ConcurrentHashMap<String, Set<Raid>> raids = new ConcurrentHashMap<>();

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement())
        {
            ResultSet rs = statement.executeQuery("SELECT user_id, boss_id, location FROM raid");

            while (rs.next()){
                String userId = rs.getString(1);
                int bossId = rs.getInt(2);
                Location location = Location.fromDbString(rs.getString(3).toLowerCase(),novaBot);

                if (location == null){
                    dbLog.warn("Location is null, not dumping raid setting");
                    continue;
                }

                Set<Raid> userSettings = raids.get(userId);
                if(userSettings == null){
                    userSettings = ConcurrentHashMap.newKeySet();
                    raids.put(userId,userSettings);
                }
                userSettings.add(new Raid(bossId,location));
            }
        } catch (SQLException e) {
            dbLog.error("Error executing dumpRaids",e);
        }

        return raids;
    }

    public ConcurrentHashMap<String,Set<Preset>> dumpPresets() {
        ConcurrentHashMap<String, Set<Preset>> presets = new ConcurrentHashMap<>();

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement())
        {
            ResultSet rs = statement.executeQuery("SELECT user_id, preset_name, location FROM preset");

            while (rs.next()){
                String userId = rs.getString(1);
                String presetName = rs.getString(2);
                Location location = Location.fromDbString(rs.getString(3).toLowerCase(),novaBot);

                if (location == null){
                    dbLog.warn("Location is null, not dumping preset setting");
                    continue;
                }

                Set<Preset> userSettings = presets.get(userId);
                if(userSettings == null){
                    userSettings = ConcurrentHashMap.newKeySet();
                    presets.put(userId,userSettings);
                }
                userSettings.add(new Preset(presetName,location));
            }
        } catch (SQLException e) {
            dbLog.error("Error executing dumpPresets",e);
        }

        return presets;
    }

    public ConcurrentHashMap<String,DbLobby> dumpRaidLobbies() {
        ConcurrentHashMap<String, DbLobby> lobbies = new ConcurrentHashMap<>();

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement())
        {
            ResultSet rs = statement.executeQuery("SELECT lobby_id, gym_id, members, role_id, channel_id, next_timeleft_update, invite_code FROM raidlobby");

            while (rs.next()){
                int lobbyID = rs.getInt(1);
                String gymId = rs.getString(2);
                int members = rs.getInt(3);
                String roleId = rs.getString(4);
                String channelId = rs.getString(5);
                int nextTimeLeftUpdate = rs.getInt(6);
                String inviteCode = rs.getString(7);

                lobbies.put(String.format("%04d", lobbyID), new DbLobby(gymId,members,channelId,roleId,nextTimeLeftUpdate,inviteCode));
            }
        } catch (SQLException e) {
            dbLog.error("Error executing dumpRaidLobbies",e);
        }

        return lobbies;
    }

    public ConcurrentHashMap<SpawnPoint,SpawnInfo> dumpSpawnInfo() {
        ConcurrentHashMap<SpawnPoint, SpawnInfo> spawnInfo = new ConcurrentHashMap<>();

        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement())
        {
            ResultSet rs = statement.executeQuery("SELECT lat,lon, timezone, suburb, street_num, street, state, postal, neighbourhood, sublocality, country FROM spawninfo");

            while (rs.next()){
                double lat = rs.getDouble(1);
                double lon = rs.getDouble(2);
                ZoneId zoneId;
                String timezone = rs.getString(3);
                if(rs.wasNull()){
                    zoneId = null;
                }else {
                    zoneId = ZoneId.of(timezone);
                }

                GeocodedLocation geocodedLocation = new GeocodedLocation();

                String city = rs.getString(4);
                if(rs.wasNull()){
                    city = "unkn";
                }
                geocodedLocation.set("city", city);

                String streetNum = rs.getString(5);
                if(rs.wasNull()){
                    streetNum = "unkn";
                }
                geocodedLocation.set("street_num", streetNum);

                String street = rs.getString(6);
                if(rs.wasNull()){
                    street = "unkn";
                }
                geocodedLocation.set("street", street);

                String state = rs.getString(7);
                if(rs.wasNull()){
                    state = "unkn";
                }
                geocodedLocation.set("state", state);

                String postal = rs.getString(8);
                if(rs.wasNull()){
                    postal = "unkn";
                }
                geocodedLocation.set("postal", postal);

                String neighbourhood = rs.getString(9);
                if(rs.wasNull()){
                    neighbourhood = "unkn";
                }
                geocodedLocation.set("neighborhood", neighbourhood);

                String sublocality = rs.getString(10);
                if(rs.wasNull()){
                    sublocality = "unkn";
                }
                geocodedLocation.set("sublocality", sublocality);

                String country = rs.getString(11);
                if(rs.wasNull()){
                    country = "unkn";
                }
                geocodedLocation.set("country", country);

                SpawnPoint point = new SpawnPoint(lat,lon);
                SpawnInfo info = new SpawnInfo(point);
                info.geocodedLocation= geocodedLocation;
                info.zoneId = zoneId;

                spawnInfo.put(point, info);
            }
        } catch (SQLException e) {
            dbLog.error("Error executing dumpSpawnInfo",e);
        }

        return spawnInfo;
    }
}
