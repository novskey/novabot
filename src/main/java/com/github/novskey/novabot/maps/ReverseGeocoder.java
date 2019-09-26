package com.github.novskey.novabot.maps;

import com.github.novskey.novabot.Util.CommandLineOptions;
import com.github.novskey.novabot.core.NovaBot;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.RequestDeniedException;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

import java.util.HashSet;

import static com.google.maps.model.AddressComponentType.*;

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

    public static void main(String[] args) {
        NovaBot novaBot = new NovaBot(CommandLineOptions.parse(args));
        novaBot.setup();
        novaBot.reverseGeocoder.geocodedLocation(-35.4553241474502,149.10350310045);
    }

    public GeocodedLocation geocodedLocation(double lat, double lon) {

        GeocodedLocation location = novaBot.dataManager.getGeocodedLocation(lat, lon);

        if(location != null) {
        	return location;
        }

        location = new GeocodedLocation();
        location.set("street_num","unkn");
        location.set("street","unkn");
        location.set("city","unkn");
        location.set("state","unkn");
        location.set("postal","unkn");
        location.set("neighborhood","unkn");
        location.set("sublocality","unkn");
        location.set("country","unkn");
        
        //Only geocode in scan locations.
        novaBot.novabotLog.info("Considering reverse-geocoding  " + lat+", "+lon);
        if (lat > 37.13 && lat < 37.99 && lon > -123.02 && lon < -121.45) {
            //OK to geocode.
        } else {
            novaBot.novabotLog.info("Skipping geocode @ " + lat+", "+lon);
            novaBot.dataManager.setGeocodedLocation(lat, lon, location);
        	return location;
        }
        
        //See if we have a suitably near by coordinate that was already reverse geocoded
        //Basically it should be close enough that the street address is (probably) the same.
        GeocodedLocation nearbyLocation = novaBot.dataManager.getNearbyGeocodedLocation(lat, lon, 0.000001);
    	if (nearbyLocation != null) {
    		location = nearbyLocation;
            novaBot.novabotLog.info("Re-using nearby reverse-geocode for " + lat+", "+lon);
    		novaBot.dataManager.setGeocodedLocation(lat, lon, location);
    		return location;
    	}
        
        synchronized (novaBot.getConfig().getGeocodingKeys()) {
        	if (novaBot.getConfig().getGeocodingKeys().isEmpty()) {
	            novaBot.novabotLog.info("Out of API keys.");
	            novaBot.dataManager.setGeocodedLocation(lat, lon, location);
	        	return location;
        	}
        }

        String key = getNextKey();
        final GeoApiContext context = novaBot.getConfig().getGeoApis().get(key);
        try {
            final GeocodingResult[] results = (GeocodingApi.reverseGeocode(context, new LatLng(lat, lon))).await();
            incRequests();
            if(results.length > 0) {
                HashSet<AddressComponentType> knownComponents = new HashSet<>();

                for (GeocodingResult result : results) {
                    if (knownComponents.size() == 8) break;

                    if(!location.getProperties().containsKey("address")) {
                        location.set("address", result.formattedAddress);
                    }

                    for (final AddressComponent addressComponent : result.addressComponents) {
                        final AddressComponentType[] types = addressComponent.types;

                        for (AddressComponentType type : types) {

                            switch (type) {
                                case STREET_NUMBER:
                                    if (!knownComponents.contains(STREET_NUMBER)) {
                                        location.set("street_num", addressComponent.shortName);
                                        knownComponents.add(STREET_NUMBER);
                                    }
                                    break;
                                case ROUTE:
                                    if (!knownComponents.contains(ROUTE)) {
                                        location.set("street", addressComponent.longName);
                                        knownComponents.add(ROUTE);
                                    }
                                    break;
                                case LOCALITY:
                                    if (!knownComponents.contains(LOCALITY)) {
                                        location.set("city", addressComponent.longName);
                                        knownComponents.add(LOCALITY);
                                    }
                                    break;
                                case ADMINISTRATIVE_AREA_LEVEL_1:
                                    if (!knownComponents.contains(ADMINISTRATIVE_AREA_LEVEL_1)) {
                                        location.set("state", addressComponent.shortName);
                                        knownComponents.add(ADMINISTRATIVE_AREA_LEVEL_1);
                                    }
                                    break;
                                case POSTAL_CODE:
                                    if (!knownComponents.contains(POSTAL_CODE)) {
                                        location.set("postal", addressComponent.longName);
                                        knownComponents.add(POSTAL_CODE);
                                    }
                                    break;
                                case NEIGHBORHOOD:
                                    if (!knownComponents.contains(NEIGHBORHOOD)) {
                                        location.set("neighborhood", addressComponent.longName);
                                        knownComponents.add(NEIGHBORHOOD);
                                    }
                                    break;
                                case SUBLOCALITY:
                                    if (!knownComponents.contains(SUBLOCALITY)) {
                                        location.set("sublocality", addressComponent.longName);
                                        knownComponents.add(SUBLOCALITY);
                                    }
                                    break;
                                case COUNTRY:
                                    if (!knownComponents.contains(COUNTRY)) {
                                        location.set("country", addressComponent.longName);
                                        knownComponents.add(COUNTRY);
                                    }
                                    break;
                            }
                        }
                    }
                }

                novaBot.dataManager.setGeocodedLocation(lat, lon, location);
            }
        }
        catch (com.google.maps.errors.OverDailyLimitException e){
            novaBot.novabotLog.info(String.format("Exceeded daily geocoding limit with key %s, removing from rotation. Enable key again with !reload.", key));
            synchronized (novaBot.getConfig().getGeocodingKeys()) {
                novaBot.getConfig().getGeocodingKeys().remove(key);
            }
        }
        catch (RequestDeniedException e){
            novaBot.novabotLog.info(String.format("API key %s is not authorised to use the geocoding api, removing from rotation. Enable key again with !reload.", key));
            synchronized (novaBot.getConfig().getGeocodingKeys()) {
                novaBot.getConfig().getGeocodingKeys().remove(key);
            }
        }
        catch (Throwable e) {
            novaBot.novabotLog.error("Error executing geocodedLocation",e);
            //Prevent retrying the same location:
            novaBot.dataManager.setGeocodedLocation(lat, lon, location);
        }

        return location;
    }

    private synchronized String getNextKey() {
        synchronized (novaBot.getConfig().getGeocodingKeys()) {
            if (ReverseGeocoder.lastKey >= novaBot.getConfig().getGeocodingKeys().size() - 1) {
                ReverseGeocoder.lastKey = 0;
                return novaBot.getConfig().getGeocodingKeys().get(ReverseGeocoder.lastKey);
            }
            ++ReverseGeocoder.lastKey;
            return novaBot.getConfig().getGeocodingKeys().get(ReverseGeocoder.lastKey);
        }
    }
}
