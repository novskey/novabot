package com.github.novskey.novabot.data;

import com.github.novskey.novabot.core.Location;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.maps.GeocodedLocation;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import com.github.novskey.novabot.maps.Geofencing;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Paris on 18/01/2018.
 */
public class SpawnLocation {

    GeocodedLocation geocodedLocation;
    HashSet<GeofenceIdentifier> geofenceIdentifiers = new HashSet<>();
    public static NovaBot novaBot;

    public SpawnLocation(GeocodedLocation geocodedLocation, ArrayList<GeofenceIdentifier> geofenceIdentifiers){
        this.geocodedLocation = geocodedLocation;
        this.geofenceIdentifiers.addAll(geofenceIdentifiers);
    }

    public static void main(String[] args) {
        NovaBot novaBot = new NovaBot();
        novaBot.setup();
        SpawnLocation location = new SpawnLocation(novaBot.reverseGeocoder.geocodedLocation(-35.2463775554412,149.136513238137), Geofencing.getGeofence(-35.2463775554412,149.136513238137));
        System.out.println(location.intersect(Location.ALL));
        System.out.println(location.intersect(Location.fromString("downer",novaBot)));
        System.out.println(location.intersect(Location.fromString("gungahlinregion",novaBot)));
    }

    public boolean intersect(Location location){
        switch (location.locationType){
            case All:
                return true;
            case Geofence:
                for (GeofenceIdentifier geofenceIdentifier : location.geofenceIdentifiers) {
                    if (geofenceIdentifiers.contains(geofenceIdentifier)){
                        return true;
                    }
                }
                return false;
            case Suburb:
                return novaBot.suburbsEnabled() && geocodedLocation != null && geocodedLocation.getProperties().get(novaBot.config.getGoogleSuburbField()).equalsIgnoreCase(location.getSuburb());
        }

        return false;
    }

}
