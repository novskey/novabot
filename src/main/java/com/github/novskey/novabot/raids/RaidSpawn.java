package com.github.novskey.novabot.raids;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.Spawn;
import com.github.novskey.novabot.core.Team;
import com.github.novskey.novabot.core.Types;
import com.github.novskey.novabot.data.SpawnLocation;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import com.github.novskey.novabot.pokemon.Pokemon;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import static com.github.novskey.novabot.maps.Geofencing.getGeofence;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by Owner on 27/06/2017.
 */
public class RaidSpawn extends Spawn {

    private static final String NORMAL_EGG = "https://raw.githubusercontent.com/ZeChrales/PogoAssets/master/static_assets/png/ic_raid_egg_normal.png";
    private static final String RARE_EGG = "https://raw.githubusercontent.com/ZeChrales/PogoAssets/master/static_assets/png/ic_raid_egg_rare.png";
    private static final String LEGENDARY_EGG = "https://raw.githubusercontent.com/ZeChrales/PogoAssets/master/static_assets/png/ic_raid_egg_legendary.png";
    private final HashMap<String, Message> builtMessages = new HashMap<>();
    public ZonedDateTime raidEnd;
    public ZonedDateTime battleStart;
    public int bossId;
    public int raidLevel;
    public String gymId;
    private String name;
    private int bossCp;
    private String imageUrl;
    private int lobbyCode;


    public RaidSpawn(int id, boolean egg) {
        super();
        if (egg) {
            raidLevel = id;
        } else {
            bossId = id;
        }
    }

    public RaidSpawn(String name, String gymId, double lat, double lon, Team team, ZonedDateTime raidEnd, ZonedDateTime battleStart, int bossId, int bossCp, int move_1, int move_2, int raidLevel) {
        this.name = name;
        properties.put("gym_name", name);

        this.gymId = gymId;

        this.lat = lat;
        properties.put("lat", String.valueOf(lat));

        this.lon = lon;
        properties.put("lng", String.valueOf(lon));

        properties.put("team_name", team.toString());

        properties.put("team_icon", team.getEmote());

        if (novaBot.suburbsEnabled()) {
            this.geocodedLocation = novaBot.reverseGeocoder.geocodedLocation(lat, lon);
            geocodedLocation.getProperties().forEach(properties::put);
        }

        this.geofenceIdentifiers = getGeofence(lat, lon);

        this.spawnLocation = new SpawnLocation(geocodedLocation, geofenceIdentifiers);

        properties.put("geofence", GeofenceIdentifier.listToString(geofenceIdentifiers));

        properties.put("gmaps", getGmapsLink());
        properties.put("applemaps", getAppleMapsLink());

        this.raidEnd = raidEnd;

        properties.put("time_left", timeLeft(raidEnd));

        this.battleStart = battleStart;
        properties.put("time_left_start", timeLeft(battleStart));


        this.bossId = bossId;
        this.bossCp = bossCp;
        this.move_1 = move_1;
        this.move_2 = move_2;

        if (bossId != 0) {
            properties.put("pkmn", Pokemon.getFilterName(bossId));
            properties.put("cp", String.valueOf(bossCp));
            properties.put("lvl20cp", String.valueOf(Pokemon.maxCpAtLevel(bossId, 20)));
            properties.put("lvl25cp", String.valueOf(Pokemon.maxCpAtLevel(bossId, 25)));
            properties.put("quick_move",(move_1 == 0) ? "unkn" : Pokemon.moveName(move_1));
            properties.put("quick_move_type_icon",(move_1 == 0) ? "unkn" : Types.getEmote(Pokemon.getMoveType(move_1)));
            properties.put("charge_move", (move_2 == 0) ? "unkn" : Pokemon.moveName(move_2));
            properties.put("charge_move_type_icon",(move_2 == 0) ? "unkn" : Types.getEmote(Pokemon.getMoveType(move_2)));
        }

        this.raidLevel = raidLevel;
        properties.put("level", String.valueOf(raidLevel));

        properties.put("lobbycode", "unkn");
    }

    public String timeLeft(ZonedDateTime until) {
        long diff = Duration.between(UtilityFunctions.getCurrentTime(UtilityFunctions.UTC),until).toMillis();

        String time;
        if (MILLISECONDS.toHours(diff) > 0) {
            time = String.format("%02dh %02dm %02ds", MILLISECONDS.toHours(Math.abs(diff)),
                    MILLISECONDS.toMinutes(Math.abs(diff)) -
                            (MILLISECONDS.toHours(Math.abs(diff)) * 60),
                    MILLISECONDS.toSeconds(Math.abs(diff)) -
                            MILLISECONDS.toMinutes(Math.abs(diff) * 60)
            );
        } else {
            time = String.format("%02dm %02ds",
                    MILLISECONDS.toMinutes(Math.abs(diff)),
                    MILLISECONDS.toSeconds(Math.abs(diff)) -
                            (MILLISECONDS.toMinutes(Math.abs(diff)) * 60)
            );
        }

        if (diff < 0) {
            time = "-" + time;
        }

        return time;
    }

    public RaidSpawn(int i, int level, boolean b) {
        this(i,b);
        this.raidLevel = level;
    }

    public Message buildMessage(String formatFile) {

        if (builtMessages.get(formatFile) == null) {

            if (!properties.containsKey("city")) {
                novaBot.reverseGeocoder.geocodedLocation(lat, lon).getProperties().forEach(properties::put);
            }

            if (!properties.containsKey("24h_start")){
                this.timeZone = novaBot.config.useGoogleTimeZones() ?  novaBot.timeZones.getTimeZone(lat,lon) : novaBot.config.getTimeZone();
                if(timeZone == null){
                    timeZone = novaBot.timeZones.getTimeZone(lat,lon);
                }
                properties.put("24h_end", getDisappearTime(printFormat24hr));
                properties.put("12h_end", getDisappearTime(printFormat12hr));

                properties.put("24h_start", getStartTime(printFormat24hr));
                properties.put("12h_start", getStartTime(printFormat12hr));
            }

            final MessageBuilder messageBuilder = new MessageBuilder();
            final EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(getColor());

            if (bossId == 0) {
                formatKey = "raidEgg";
                embedBuilder.setDescription(novaBot.config.formatStr(properties, novaBot.config.getBodyFormatting(formatFile, formatKey) + (
                        raidLevel >= 3 && novaBot.config.isRaidOrganisationEnabled()
                                ? "\n\nJoin the discord lobby to coordinate with other players, and be alerted when this egg hatches. Join by clicking the ✅ emoji below this post, or by typing `!joinraid <lobbycode>` in any novabot channel."
                                : "")));
            } else {
                formatKey = "raidBoss";
                embedBuilder.setDescription(novaBot.config.formatStr(properties, novaBot.config.getBodyFormatting(formatFile, formatKey) + (
                        raidLevel >= 3 && novaBot.config.isRaidOrganisationEnabled()
                                ? "\n\nJoin the discord lobby to coordinate with other players by clicking the ✅ emoji below this post, or by typing `!joinraid <lobbycode>` in any novabot channel."
                                : "")));
            }
            embedBuilder.setTitle(novaBot.config.formatStr(properties, novaBot.config.getTitleFormatting(formatFile, formatKey)), novaBot.config.formatStr(properties, novaBot.config.getTitleUrl(formatFile, formatKey)));
            embedBuilder.setThumbnail(getIcon());
            if (novaBot.config.showMap(formatFile, formatKey)) {
                embedBuilder.setImage(getImage(formatFile));
            }
            embedBuilder.setFooter(novaBot.config.getFooterText(), null);
            embedBuilder.setTimestamp(Instant.now());
            messageBuilder.setEmbed(embedBuilder.build());

            String contentFormatting = novaBot.config.getContentFormatting(formatFile, formatKey);

            if (contentFormatting != null && !contentFormatting.isEmpty()) {
                messageBuilder.append(novaBot.config.formatStr(properties, novaBot.config.getContentFormatting(formatFile, formatKey)));
            }

            builtMessages.put(formatFile, messageBuilder.build());
        }
        return builtMessages.get(formatFile);
    }

    public String getDisappearTime(DateTimeFormatter printFormat) {
        return printFormat.format(raidEnd.withZoneSameInstant(timeZone));
    }

    public String getIcon() {
        if (bossId == 0) {
            switch (raidLevel) {
                case 1:
                case 2:
                    return NORMAL_EGG;
                case 3:
                case 4:
                    return RARE_EGG;
                case 5:
                    return LEGENDARY_EGG;
            }
        }
        return Pokemon.getIcon(bossId);
    }

    public String getLobbyCode() {
        return String.format("%04d", lobbyCode);
    }

    public void setLobbyCode(int id) {
        this.lobbyCode = id;

        properties.put("lobbycode", getLobbyCode());
    }

    public String getStartTime(DateTimeFormatter printFormat) {
        return printFormat.format(battleStart.withZoneSameInstant(timeZone));
    }

    public void setLobbyCode(String lobbyCode) {
        this.lobbyCode = Integer.parseInt(lobbyCode);
        properties.put("lobbycode", getLobbyCode());
    }

    public String timeLeft(Instant untilTime) {

        long diff = Duration.between(Instant.now(),untilTime).toMillis();

        String time;
        if (MILLISECONDS.toHours(diff) > 0) {
            time = String.format("%02dh %02dm %02ds", MILLISECONDS.toHours(Math.abs(diff)),
                                 MILLISECONDS.toMinutes(Math.abs(diff)) -
                                 (MILLISECONDS.toHours(Math.abs(diff)) * 60),
                                 MILLISECONDS.toSeconds(Math.abs(diff)) -
                                 MILLISECONDS.toMinutes(Math.abs(diff) * 60)
                                );
        } else {
            time = String.format("%02dm %02ds",
                                 MILLISECONDS.toMinutes(Math.abs(diff)),
                                 MILLISECONDS.toSeconds(Math.abs(diff)) -
                                 (MILLISECONDS.toMinutes(Math.abs(diff)) * 60)
                                );
        }

        if (diff < 0) {
            time = "-" + time;
        }

        return time;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", name, gymId, lat, lon, raidEnd, battleStart, bossId, bossCp, raidLevel, move_1, move_2);
    }

    private Color getColor() {
        switch (raidLevel) {
            case 1:
                return new Color(0x9d9d9d);
            case 2:
                return new Color(0xdb3b78);
            case 3:
                return new Color(0xff8000);
            case 4:
                return new Color(0xffe100);
            case 5:
                return new Color(0x00082d);
        }
        return Color.WHITE;
    }

    @Override
    public int hashCode() {
        return (int) (gymId.hashCode() * raidEnd.toInstant().toEpochMilli());
    }
}
