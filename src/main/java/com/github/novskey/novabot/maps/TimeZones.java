package com.github.novskey.novabot.maps;

import com.github.novskey.novabot.core.NovaBot;
import com.google.maps.GeoApiContext;
import com.google.maps.TimeZoneApi;
import com.google.maps.errors.ApiException;
import com.google.maps.errors.OverDailyLimitException;
import com.google.maps.errors.RequestDeniedException;
import com.google.maps.model.LatLng;

import java.io.IOException;
import java.time.ZoneId;
import java.util.TimeZone;


/**
 * Created by Paris on 11/01/2018.
 */
public class TimeZones {

    private final NovaBot novaBot;
    private static int lastKey = 0;
    private static int requests = 0;

    private synchronized void incRequests(){
        requests++;
    }

    public TimeZones(NovaBot novaBot) {
        this.novaBot = novaBot;
    }

    public static void main(String[] args) {
        NovaBot novaBot = new NovaBot();
        novaBot.setup();
        ZoneId zoneId = novaBot.timeZones.getTimeZone(-35.2315056328764,149.137230948448);
        System.out.println(zoneId);
    }

    public ZoneId getTimeZone(double lat, double lon){

        ZoneId zoneId = novaBot.dataManager.getZoneId(lat, lon);

        if(zoneId != null)  return zoneId;

        String key = getNextKey();

        GeoApiContext context = novaBot.getConfig().getGeoApis().get(key);
        try {
            TimeZone timeZone = TimeZoneApi.getTimeZone(context, new LatLng(lat, lon)).await();
            incRequests();
            zoneId = timeZone.toZoneId();

            novaBot.dataManager.setZoneId(lat, lon, zoneId);

        } catch (OverDailyLimitException e) {
            novaBot.novabotLog.info(String.format("Exceeded daily time zone limit with key %s, removing from rotation. Enable key again with !reload.", key));
            novaBot.getConfig().getTimeZoneKeys().remove(key);
        }catch (RequestDeniedException e){
                novaBot.novabotLog.info(String.format("API key %s is not authorised to use the timezone api, removing from rotation. Enable key again with !reload.", key));
                novaBot.getConfig().getTimeZoneKeys().remove(key);
        } catch (ApiException | InterruptedException | IOException e) {
            novaBot.novabotLog.error("Error executing getTimeZone",e);
        }
        return zoneId;
    }

    private synchronized String getNextKey() {
        if (TimeZones.lastKey >= novaBot.getConfig().getTimeZoneKeys().size() - 1) {
            TimeZones.lastKey = 0;
            return novaBot.getConfig().getTimeZoneKeys().get(TimeZones.lastKey);
        }
        ++TimeZones.lastKey;
        return novaBot.getConfig().getTimeZoneKeys().get(TimeZones.lastKey);
    }

    public int getRequests() {
        return requests;
    }
}
