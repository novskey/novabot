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
                return novaBot.suburbsEnabled() && geocodedLocation != null && geocodedLocation.getProperties().get(novaBot.getConfig().getGoogleSuburbField()).equalsIgnoreCase(location.getSuburb());
        }

        return false;
    }

}
