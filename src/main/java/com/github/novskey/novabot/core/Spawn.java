package com.github.novskey.novabot.core;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.data.SpawnLocation;
import com.github.novskey.novabot.maps.GeocodedLocation;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import lombok.*;
import net.dv8tion.jda.core.entities.Message;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

@Data
public class Spawn {
    protected static final DateTimeFormatter printFormat24hr = DateTimeFormatter.ofPattern("HH:mm:ss");
    protected static final DateTimeFormatter printFormat12hr = DateTimeFormatter.ofPattern("hh:mm:ss a");
    @Setter
    public static NovaBot novaBot = null;
    private static int lastKey;

    @Getter
    private static int requests = 0;

    static {
        lastKey = 0;
    }

    private final HashMap<String, String> properties = new HashMap<>();
    protected final HashMap<String, Message> builtMessages = new HashMap<>();
    private Integer move_1;
    private Integer move_2;
    protected double lat;
    protected double lon;
    protected String formatKey = "pokemon";
    protected ArrayList<GeofenceIdentifier> geofenceIdentifiers = new ArrayList<>();
    private String imageUrl;
    private ZoneId timeZone;
    private SpawnLocation spawnLocation;
    private GeocodedLocation geocodedLocation = null;

    private synchronized static void incRequests(){
        requests++;
    }

    protected static String getWeather(int weather) {
        switch(weather){
            case 0:
                return novaBot.getLocalString("None");
            case 1:
                return novaBot.getLocalString("Clear");
            case 2:
                return novaBot.getLocalString("Rain");
            case 3:
                return novaBot.getLocalString("PartlyCloudy");
            case 4:
                return novaBot.getLocalString("Cloudy");
            case 5:
                return novaBot.getLocalString("Windy");
            case 6:
                return novaBot.getLocalString("Snow");
            case 7:
                return novaBot.getLocalString("Fog");
            default:
                return "unkn";
        }
    }

    public ArrayList<GeofenceIdentifier> getGeofences() {
        return geofenceIdentifiers;
    }

    public String getImage(String formatFile) {
        if (this.imageUrl == null) {
            if(novaBot.getConfig().getStaticMapKeys().size() == 0){
                return "https://raw.githubusercontent.com/novskey/novabot/dev/static/no-api-keys-remaining.png";
            }else {
                incRequests();
                return this.imageUrl = "https://maps.googleapis.com/maps/api/staticmap?" + String.format("zoom=%s&size=%sx%s&markers=color:red|%s,%s&key=%s", novaBot.getConfig().getMapZoom(formatFile, formatKey), novaBot.getConfig().getMapWidth(formatFile, formatKey), novaBot.getConfig().getMapHeight(formatFile, formatKey), this.lat, this.lon, getNextKey());
            }
        }
        return this.imageUrl;
    }

    public String getSuburb() {
        return getProperties().get("city");
    }

    public static void main(String[] args) {
        System.out.println(printFormat24hr.format(UtilityFunctions.getCurrentTime(UtilityFunctions.UTC)));
    }

    protected String getAppleMapsLink() {
        return String.format("http://maps.apple.com/maps?daddr=%s,%s&z=10&t=s&dirflg=w", this.lat, this.lon);
    }

    protected String getGmapsLink() {
        return String.format("https://www.google.com/maps?q=loc:%s,%s", this.lat, this.lon);
    }

    private synchronized static String getNextKey() {
        if (lastKey >= novaBot.getConfig().getStaticMapKeys().size() - 1) {
            lastKey = 0;
            return novaBot.getConfig().getStaticMapKeys().get(lastKey);
        }
        ++lastKey;
        return novaBot.getConfig().getStaticMapKeys().get(lastKey);
    }

    public SpawnLocation getLocation() {
        return getSpawnLocation();
    }
}
