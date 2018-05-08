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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsDBManager implements IDataBase {
    private final Logger dbLog = LoggerFactory.getLogger("Settings-DB");

    Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone(UtilityFunctions.UTC));

    public final HashMap<String, RaidSpawn> knownRaids = new HashMap<>();
    private StringBuilder blacklistQuery = new StringBuilder();
    private final NovaBot novaBot;
    private java.lang.String scanUrl;
    private String nbUrl;

    private static final String MySQL_DRIVER = "com.mysql.jdbc.Driver";
    private static final String PgSQL_DRIVER = "org.postgresql.Driver";
    private com.github.novskey.novabot.data.DataSource novaBotDataSource;
    private com.github.novskey.novabot.data.DataSource scanDataSource;

    public SettingsDBManager(NovaBot novaBot) {
        this.novaBot = novaBot;
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
             PreparedStatement statement = connection.prepareStatement("INSERT INTO raid (user_id,boss_id,egg_level,raid_level,gym_name,location) VALUES (?,?,?,?,?,?)")) {
            statement.setString(1, userID);
            statement.setInt(2, raid.bossId);
            statement.setInt(3, raid.eggLevel);
            statement.setInt(4, raid.raidLevel);
            statement.setString(5,raid.gymName);
            statement.setString(6, raid.location.toDbString());

            dbLog.debug(statement.toString());
            statement.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            dbLog.warn(e.getMessage());
        } catch (SQLException e2) {
            dbLog.error("Error executing addRaid", e2);
        }
    }

    @Override
    public void addUser(final String userID, String botToken) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO users (id, bot_token) VALUES (?,?)"))
        {
            statement.setString(1,userID);
            statement.setString(2,botToken);
            statement.executeUpdate();
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
    public void clearTokens(ArrayList<String> toRemove) {
        StringBuilder tokensString = new StringBuilder("(");
        for (int i = 0; i < toRemove.size(); ++i) {
            if (i == toRemove.size() - 1) {
                tokensString.append(String.format("'%s'", toRemove.get(i)));
            } else {
                tokensString.append(String.format("'%s'", toRemove.get(i))).append(",");
            }
        }
        tokensString.append(")");

        int affected = 0;
        try (Connection connection = getNbConnection();
             Statement statement = connection.createStatement()) {
            affected = statement.executeUpdate(String.format("UPDATE users SET bot_token=NULL WHERE bot_token IN %s;", tokensString.toString()));
        } catch (SQLException e) {
            dbLog.error("Error executing clearTokens", e);
        }

        dbLog.info(String.format("Cleared %s invalid tokens", affected));
    }

    @Override
    public int countPokemon(final String id, Pokemon[] potentialPokemon, boolean countLocations) {
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

    @Override
    public int countPresets(String userID, ArrayList<Preset> presetsList, boolean countLocations) {
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
    public int countRaids(final String id, Raid[] potentialRaids, boolean countLocations) {
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

    @Override
    public User getUser(String id) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, paused, bot_token FROM users WHERE id = ?"))
        {
            statement.setString(1, id);
            final ResultSet rs = statement.getResultSet();
            if (!rs.next()) {
                String   userId = rs.getString(1);
                boolean  paused = rs.getBoolean(2);
                String botToken = rs.getString(3);

                return new User(userId,paused,botToken);
            }
        } catch (SQLException e) {
            dbLog.error("Error executing notContainsUser",e);
        }
        return null;
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

        String sql;
        if(raidSpawn.bossId != 0) {
            sql = String.format(
                    "SELECT DISTINCT(user_id) " +
                    "FROM raid " +
                    "WHERE (SELECT paused FROM users WHERE users.id = raid.user_id) = FALSE " +
                    "AND LOWER(location) IN (%s%s'all') " +
                    "AND (boss_id=? OR raid_level=?) " +
                    "AND gym_name IN ('',?);", geofenceQMarks.toString(), (novaBot.suburbsEnabled() ? "?, " : "")
                               );
        }else{
            sql = String.format(
                    "SELECT DISTINCT(user_id) " +
                    "FROM raid " +
                    "WHERE (SELECT paused FROM users WHERE users.id = raid.user_id) = FALSE " +
                    "AND LOWER(location) IN (%s%s'all') " +
                    "AND egg_level=? " +
                    "AND gym_name IN ('',?);", geofenceQMarks.toString(), (novaBot.suburbsEnabled() ? "?, " : ""));
        }

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
            if (raidSpawn.bossId != 0) {
                statement.setInt(geofences + offset, raidSpawn.bossId);
                offset++;
            }else {
                statement.setInt(geofences + offset, raidSpawn.raidLevel);
                offset++;
            }
            statement.setString(geofences + offset, raidSpawn.getProperties().get("gym_name"));

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
            statement.setInt(geofences + offset, pokeSpawn.id);
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

            statement.executeQuery(String.format("SELECT boss_id,egg_level,raid_level,gym_name,location FROM raid WHERE user_id='%s'", id));

            rs = statement.getResultSet();

            while (rs.next()) {
                final int bossId = rs.getInt(1);
                final int eggLevel = rs.getInt(2);
                final int raidLevel = rs.getInt(3);
                final String gymName = rs.getString(4);
                final Location location = Location.fromDbString(rs.getString(5).toLowerCase(), novaBot);

                if (location == null){
                    dbLog.warn("Location is null, skipping raid setting");
                    continue;
                }
                userPref.addRaid(new Raid(bossId, eggLevel, raidLevel, gymName, location));
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
//
//        PokeSpawn pokeSpawn = new PokeSpawn(
//                1,
//                -35.265134, 149.122796,
//                ZonedDateTime.ofInstant(Instant.now().plusSeconds(60), UtilityFunctions.UTC),
//                null,
//                null,
//                null,
//                null,
//                null,
//                0,
//                0,
//                null,
//                null,
//                null, null);
//
//        System.out.println(novaBot.dataManager.getUserPref("107730875596169216").allPokemonToString());
//        System.out.println(pokeSpawn.buildMessage("formatting.ini").getEmbeds().get(0).getDescription());
//        System.out.println(novaBot.dataManager.getUserIDsToNotify(pokeSpawn));

        System.out.println(novaBot.getDataManager().getUserPref("107730875596169216").allSettingsToString());

        RaidSpawn spawn = new RaidSpawn(
                "lincoln park gazebo",
                "id",
                -35.265134, 149.122796,
                Team.Valor,
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(120), UtilityFunctions.UTC),
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(60), UtilityFunctions.UTC),
                0,
                155555,
                18,
                22,
                5);

        System.out.println(novaBot.dataManager.getUserIDsToNotify(spawn));

        spawn = new RaidSpawn(
                "lincoln park gazebo",
                "id",
                -35.265134, 149.122796,
                Team.Valor,
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(120), UtilityFunctions.UTC),
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(60), UtilityFunctions.UTC),
                0,
                155555,
                18,
                22,
                4);

        System.out.println(novaBot.dataManager.getUserIDsToNotify(spawn));

        spawn = new RaidSpawn(
                "lincoln park gazebo",
                "id",
                -35.265134, 149.122796,
                Team.Valor,
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(120), UtilityFunctions.UTC),
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(60), UtilityFunctions.UTC),
                0,
                155555,
                18,
                22,
                3);

        System.out.println(novaBot.dataManager.getUserIDsToNotify(spawn));

        spawn = new RaidSpawn(
                "lincoln park gazebo",
                "id",
                -35.265134, 149.122796,
                Team.Valor,
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(120), UtilityFunctions.UTC),
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(60), UtilityFunctions.UTC),
                0,
                155555,
                18,
                22,
                2);

        System.out.println(novaBot.dataManager.getUserIDsToNotify(spawn));

        spawn = new RaidSpawn(
                "lincoln park gazebo",
                "id",
                -35.265134, 149.122796,
                Team.Valor,
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(120), UtilityFunctions.UTC),
                ZonedDateTime.ofInstant(Instant.now().plusSeconds(60), UtilityFunctions.UTC),
                0,
                155555,
                18,
                22,
                1);

        System.out.println(novaBot.dataManager.getUserIDsToNotify(spawn));
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

    @Override
    public void setBotToken(String id, String botToken) {
        try (Connection connection = getNbConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET bot_token = ? WHERE id = ?")
        ) {
            statement.setString(1, botToken);
            statement.setString(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            dbLog.error("Error executing pauseUser",e);
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
            ResultSet rs = statement.executeQuery("SELECT id, paused, bot_token FROM users");

            while (rs.next()){
                String id = rs.getString(1);
                boolean paused = rs.getBoolean(2);
                String botToken = rs.getObject(3, String.class);

                users.put(id, new User(id, paused,botToken));
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
            ResultSet rs = statement.executeQuery("SELECT user_id, boss_id, egg_level, raid_level, gym_name, location FROM raid");

            while (rs.next()){
                String userId = rs.getString(1);
                int bossId = rs.getInt(2);
                int eggLevel = rs.getInt(3);
                int raidLevel = rs.getInt(4);
                String gymName = rs.getString(5);
                Location location = Location.fromDbString(rs.getString(6).toLowerCase(),novaBot);

                if (location == null){
                    dbLog.warn("Location is null, not dumping raid setting");
                    continue;
                }

                Set<Raid> userSettings = raids.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
                userSettings.add(new Raid(bossId,eggLevel,raidLevel,gymName,location));
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
