package com.github.novskey.novabot.maps;

import com.github.novskey.novabot.core.NovaBot;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.RequestDeniedException;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

public class ReverseGeocoder {

    private static int lastKey;
    private final NovaBot novaBot;

    private int requests = 0;

    private synchronized
    void incRequests(){
        requests++;
    }

    public ReverseGeocoder(NovaBot novaBot) {
        this.novaBot = novaBot;
    }

    public int getRequests() {
        return requests;
    }

//    public static void main(final String[] args) {
////        novaBot.dataManager.novabotdbConnect();
//        novaBot.testing = true;
//        loadConfig();
//
//        novaBot.dataManager.novabotdbConnect();
//
//        geocodedLocation(-43.4939774,172.7176463);
////        novaBot.dataManager.setGeocodedLocation(-35.405055,149.1270075,geocodedLocation(-35.405055, 149.1270075));
//    }

    public GeocodedLocation geocodedLocation(double lat, double lon) {

        GeocodedLocation location = novaBot.dataManager.getGeocodedLocation(lat, lon);

        if(location != null)  return location;

        location = new GeocodedLocation();
        location.set("street_num","unkn");
        location.set("street","unkn");
        location.set("city","unkn");
        location.set("state","unkn");
        location.set("postal","unkn");
        location.set("neighborhood","unkn");
        location.set("sublocality","unkn");
        location.set("country","unkn");

        String key = getNextKey();
        final GeoApiContext context = novaBot.getConfig().getGeoApis().get(key);
        try {
            final GeocodingResult[] results = (GeocodingApi.reverseGeocode(context, new LatLng(lat, lon))).await();
            incRequests();
            if(results.length > 0) {
                for (final AddressComponent addressComponent : results[0].addressComponents) {
                    final AddressComponentType[] types = addressComponent.types;

                    for (AddressComponentType type : types) {

                        switch (type) {
                            case STREET_NUMBER:
                                location.set("street_num", addressComponent.longName);
                                break;
                            case ROUTE:
                                location.set("street", addressComponent.longName);
                                break;
                            case LOCALITY:
                                location.set("city", addressComponent.longName);
                                break;
                            case ADMINISTRATIVE_AREA_LEVEL_1:
                                location.set("state", addressComponent.shortName);
                            case POSTAL_CODE:
                                location.set("postal", addressComponent.longName);
                                break;
                            case NEIGHBORHOOD:
                                location.set("neighborhood", addressComponent.longName);
                                break;
                            case SUBLOCALITY:
                                location.set("sublocality", addressComponent.longName);
                                break;
                            case COUNTRY:
                                location.set("country", addressComponent.longName);
                                break;
                        }
                    }
                }
                location.set("address",String.format("%s %s", location.getProperties().get("number"), location.getProperties().get("street")));
                novaBot.dataManager.setGeocodedLocation(lat, lon, location);
            }

        }
        catch (com.google.maps.errors.OverDailyLimitException e){
            novaBot.novabotLog.info(String.format("Exceeded daily geocoding limit with key %s, removing from rotation. Enable key again with !reload.", key));
            novaBot.getConfig().getGeocodingKeys().remove(key);
        }
        catch (RequestDeniedException e){
            novaBot.novabotLog.info(String.format("API key %s is not authorised to use the geocoding api, removing from rotation. Enable key again with !reload.", key));
            novaBot.getConfig().getGeocodingKeys().remove(key);
        }
        catch (Exception e) {
            novaBot.novabotLog.error("Error executing geocodedLocation",e);
        }

        return location;
    }

    private String getNextKey() {
        if (ReverseGeocoder.lastKey >= novaBot.getConfig().getGeocodingKeys().size() - 1) {
            ReverseGeocoder.lastKey = 0;
            return novaBot.getConfig().getGeocodingKeys().get(ReverseGeocoder.lastKey);
        }
        ++ReverseGeocoder.lastKey;
        return novaBot.getConfig().getGeocodingKeys().get(ReverseGeocoder.lastKey);
    }
}
