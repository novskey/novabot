package com.github.novskey.novabot.pokemon;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.core.Spawn;
import com.github.novskey.novabot.core.Types;
import com.github.novskey.novabot.data.SpawnLocation;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;

import java.awt.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static com.github.novskey.novabot.maps.Geofencing.getGeofence;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class PokeSpawn extends Spawn
{
    private static final DecimalFormat df;

    static {
        (df = new DecimalFormat("#.##")).setRoundingMode(RoundingMode.CEILING);

    }

    public ZonedDateTime disappearTime;
    public String form;
    public Integer id;
    public Float iv;
    public Integer cp;
    public Integer level;
    private Float weight;
    private Float height;
    private Integer gender;
    private String suburb;
    private Integer iv_attack;
    private Integer iv_defense;
    private Integer iv_stamina;


    public PokeSpawn(int i) {
        super();
        this.id = i;
        properties.put("pkmn",Pokemon.getFilterName(i));
    }

    public PokeSpawn(final int id, final double lat, final double lon, final ZonedDateTime disappearTime, final Integer attack, final Integer defense, final Integer stamina, final Integer move1, final Integer move2, final float weight, final float height, final Integer gender, final Integer form, Integer cp) {
        super();
        this.disappearTime = null;
        this.form = null;
        this.suburb = null;
        this.disappearTime = disappearTime;

        this.lat = lat;
        properties.put("lat", String.valueOf(lat));

        this.lon = lon;
        properties.put("lng", String.valueOf(lon));

        properties.put("time_left",timeLeft());

        this.id = id;
        properties.put("pkmn_id", String.valueOf(id));

        properties.put("pkmn",Pokemon.getFilterName(id));


        if (novaBot.suburbsEnabled()) {
            this.geocodedLocation = novaBot.reverseGeocoder.geocodedLocation(lat, lon);
            geocodedLocation.getProperties().forEach(properties::put);
        }

        this.geofenceIdentifiers = getGeofence(lat,lon);

        this.spawnLocation = new SpawnLocation(geocodedLocation, geofenceIdentifiers);

        properties.put("geofence", GeofenceIdentifier.listToString(geofenceIdentifiers));

        properties.put("gmaps",getGmapsLink());

        properties.put("applemaps",getAppleMapsLink());

        this.iv_attack = attack;
        properties.put("atk", String.valueOf(iv_attack));

        this.iv_defense = defense;
        properties.put("def", String.valueOf(iv_defense));

        this.iv_stamina = stamina;
        properties.put("sta", String.valueOf(iv_stamina));

        if (attack != null && defense != null && stamina != null){
            this.iv = (attack + defense + stamina) / 45.0f * 100.0f;
            properties.put("iv", getIv());
        }else{
            this.iv = null;
            properties.put("iv","unkn");
        }

        this.move_1 = move1;
        properties.put("quick_move", (move1 == null) ? "unkn" : Pokemon.moveName(move1));
        properties.put("quick_move_type",(move1 == null) ? "unkn" : Pokemon.getMoveType(move1));
        properties.put("quick_move_type_icon",(move1 == null) ? "unkn" : Types.getEmote(Pokemon.getMoveType(move1)));

        this.move_2 = move2;
        properties.put("charge_move", (move2 == null) ? "unkn" : Pokemon.moveName(move2));
        properties.put("charge_move_type", (move2 == null) ? "unkn" : Pokemon.getMoveType(move2));
        properties.put("charge_move_type_icon",(move1 == null) ? "unkn" : Types.getEmote(Pokemon.getMoveType(move2)));

        this.weight = weight;
        properties.put("weight", getWeight());

        this.height = height;
        properties.put("height", getHeight());

        properties.put("size",getSize());

        this.gender = gender;
        properties.put("gender", getGender());

        if (form != null && form != 0 && id == 201) {
            this.id = id * 10 + form;
        }
        if (form != null) {
            this.form = ((Pokemon.intToForm(form) == null) ? null : String.valueOf(Pokemon.intToForm(form)));
        }
        properties.put("form", (this.form == null ? "" : this.form));

        this.cp = cp;
        properties.put("cp", cp == null ? "?" : String.valueOf(cp));

        properties.put("lvl30cp", cp == null ? "?" : String.valueOf(Pokemon.maxCpAtLevel(id, 30)));
        properties.put("lvl35cp", cp == null ? "?" : String.valueOf(Pokemon.maxCpAtLevel(id, 35)));

        properties.put("weather","unkn");
    }

    public PokeSpawn(final int id, final double lat, final double lon, final ZonedDateTime disappearTime, final Integer attack, final Integer defense, final Integer stamina, final Integer move1, final Integer move2, final float weight, final float height, final Integer gender, final Integer form, Integer cp, double cpModifier) {
        this(id,lat,lon,disappearTime,attack,defense,stamina,move1,move2,weight,height,gender,form,cp);
        level = Pokemon.getLevel(cpModifier);
        properties.put("level", String.valueOf(level));
    }

    public PokeSpawn(final int id, final double lat, final double lon, final ZonedDateTime disappearTime, final Integer attack, final Integer defense, final Integer stamina, final Integer move1, final Integer move2, final float weight, final float height, final Integer gender, final Integer form, Integer cp, Integer level) {
        this(id,lat,lon,disappearTime,attack,defense,stamina,move1,move2,weight,height,gender,form,cp);
        this.level = level;
        properties.put("level", String.valueOf(level));
    }

    public PokeSpawn(int id, double lat, double lon, ZonedDateTime disappearTime, Integer attack, Integer defense, Integer stamina, Integer move1, Integer move2, int weight, int height, Integer gender, Integer form, Integer cp, Integer level, int weather) {
        this(id,lat,lon,disappearTime,attack,defense,stamina,move1,move2,weight,height,gender,form,cp,level);
        properties.replace("weather",getWeather(weather));
    }

    public PokeSpawn(int id, double lat, double lon, ZonedDateTime disappearTime, Integer attack, Integer defense, Integer stamina, Integer move1, Integer move2, float weight, float height, Integer gender, Integer form, Integer cp, double cpMod, int weather) {
        this(id,lat,lon,disappearTime,attack,defense,stamina,move1,move2,weight,height,gender,form,cp,cpMod);
        properties.replace("weather",getWeather(weather));
    }

    public Message buildMessage(String formatFile) {
        if(builtMessages.get(formatFile) == null) {

            if (!properties.containsKey("city")) {
                this.geocodedLocation = novaBot.reverseGeocoder.geocodedLocation(lat, lon);
                geocodedLocation.getProperties().forEach(properties::put);
            }

            if (!properties.containsKey("24h_time")){
                this.timeZone = novaBot.config.useGoogleTimeZones() ?  novaBot.timeZones.getTimeZone(lat,lon) : novaBot.config.getTimeZone();
                if(timeZone == null){
                    timeZone = novaBot.timeZones.getTimeZone(lat,lon);
                }

                properties.put("24h_time", getDespawnTime(printFormat24hr));
                properties.put("12h_time", getDespawnTime(printFormat12hr));
            }

            final MessageBuilder messageBuilder = new MessageBuilder();
            final EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(getColor());
            embedBuilder.setTitle(novaBot.config.formatStr(properties, (encountered()) ? novaBot.config.getEncounterTitleFormatting(formatFile) : (novaBot.config.getTitleFormatting(formatFile, "pokemon"))), novaBot.config.formatStr(properties, novaBot.config.getTitleUrl(formatFile, "pokemon")));
            embedBuilder.setDescription(novaBot.config.formatStr(properties, (encountered()) ? novaBot.config.getEncounterBodyFormatting(formatFile) : novaBot.config.getBodyFormatting(formatFile, "pokemon")));
            embedBuilder.setThumbnail(Pokemon.getIcon(this.id));
            if (novaBot.config.showMap(formatFile, "pokemon")) {
                embedBuilder.setImage(this.getImage(formatFile));
            }
            embedBuilder.setFooter(novaBot.config.getFooterText(), null);
            embedBuilder.setTimestamp(ZonedDateTime.now(UtilityFunctions.UTC));
            messageBuilder.setEmbed(embedBuilder.build());

            String contentFormatting = novaBot.config.getContentFormatting(formatFile, formatKey);
            if (contentFormatting != null && !contentFormatting.isEmpty()) {
                messageBuilder.append(novaBot.config.formatStr(properties, novaBot.config.getContentFormatting(formatFile, formatKey)));
            }

            builtMessages.put(formatFile,messageBuilder.build());
        }

        return builtMessages.get(formatFile);
    }

    public int getFilterId() {
        if (id >= 2011) {
            return 201;
        } else {
            return id;
        }
    }

    @Override
    public int hashCode() {
        int hash = (int) (lat * lon);

        hash *= novaBot.suburbs.indexOf(suburb);

        hash *= id;

        hash += (iv == null ? 0 : iv) * 1000;

        hash *= (gender == null ? 0 : gender) + 1;

        hash += (weight * height);

        hash += (move_1 == null ? 0 : move_1) * (move_2 == null ? 0 : move_2);

        hash += ((iv_attack == null ? 0 : iv_attack) + (iv_defense == null ? 0 : iv_defense) + (iv_stamina == null ? 0 : iv_stamina));

        hash += (cp == null ? 0 : cp);

        hash += disappearTime.hashCode();

        return hash;
    }

    @Override
    public String toString() {
        if (this.disappearTime == null) {
            return "[" + ((this.suburb == null) ? "null" : this.suburb) + "]" + Pokemon.idToName(this.id) + " " + PokeSpawn.df.format(this.iv) + "%, " + this.move_1 + ", " + this.move_2;
        }
        return "[" + ((this.suburb == null) ? "null" : this.suburb) + "]" + Pokemon.idToName(this.id) + " " + (iv != null ? PokeSpawn.df.format(this.iv) : "unkn") + "%,CP: " + this.cp + ", " + this.move_1 + ", " + this.move_2 + ", for " + this.timeLeft() + ", disappears at " + this.disappearTime;
    }

    public static void main(String[] args) {
        NovaBot novaBot = new NovaBot();
        novaBot.setup();
        Spawn.setNovaBot(novaBot);
        PokeSpawn pokeSpawn = new PokeSpawn(201);
        System.out.println(pokeSpawn.properties.get("pkmn"));
    }

    private boolean encountered() {
        return iv_attack != null;
    }

    private Color getColor() {
        if(iv == null || iv < 25)
            return new Color(0x9d9d9d);
        if(iv < 50)
            return new Color(0xffffff);
        if(iv < 81)
            return new Color(0x1eff00);
        if(iv < 90)
            return new Color(0x0070dd);
        if(iv < 100)
            return new Color(0xa335ee);
        if(iv == 100)
            return new Color(0xff8000);

        return Color.GRAY;
    }

    private String getDespawnTime(DateTimeFormatter printFormat) {
        return printFormat.format(disappearTime.withZoneSameInstant(timeZone));
    }

    private String getGender() {
        if (this.gender == null){
            return "?";
        }
        if (this.gender == 1) {
            return "\u2642";
        }
        if (this.gender == 2) {
            return "\u2640";
        }
        if (this.gender == 3) {
            return "\u26b2";
        }
        return "?";
    }

    private String getHeight() {
        return PokeSpawn.df.format(this.height);
    }

    private String getIv() {
        return PokeSpawn.df.format(this.iv);
    }

    private String getSize() {
        if ((weight == 0) && (height == 0)) {
            return "?";
        }

        return Pokemon.getSize(id,height,weight);
    }

    private String getWeight() {
        return PokeSpawn.df.format(this.weight);
    }

    private String timeLeft() {
        long diff = Duration.between(ZonedDateTime.now(UtilityFunctions.UTC), disappearTime).toMillis();

        String time = String.format("%02dm %02ds",
                                    MILLISECONDS.toMinutes(Math.abs(diff)),
                                    MILLISECONDS.toSeconds(Math.abs(diff)) -
                                    (MILLISECONDS.toMinutes(Math.abs(diff)) * 60)
                                   );

        if (diff < 0) {
            time = "-" + time;
        }

        return time;
    }
}
