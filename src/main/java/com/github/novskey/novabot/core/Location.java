package com.github.novskey.novabot.core;

import com.github.novskey.novabot.maps.GeofenceIdentifier;

import java.util.ArrayList;

public class Location {
    public static final Location ALL = new Location();
    public static String all;
    public final LocationType locationType;
    public ArrayList<GeofenceIdentifier> geofenceIdentifiers = new ArrayList<>();
    private String suburb;

    private Location(final String suburb) {
        this.locationType = LocationType.Suburb;
        this.suburb = suburb;
    }

    public Location(final GeofenceIdentifier geofenceIdentifier) {
        this.locationType = LocationType.Geofence;
        this.geofenceIdentifiers = new ArrayList<>();
        this.geofenceIdentifiers.add(geofenceIdentifier);
    }


    public Location(ArrayList<GeofenceIdentifier> identifiers) {
        this.locationType = LocationType.Geofence;
        this.geofenceIdentifiers = identifiers;
    }

    private Location() {
        this.locationType = LocationType.All;
    }

    @Override
    public boolean equals(Object obj) {
        assert obj.getClass().getName().equals(this.getClass().getName());
        Location loc = (Location) obj;
        if(loc.geofenceIdentifiers == null || this.geofenceIdentifiers == null){
            System.out.println("null");
        }
        return loc.locationType == this.locationType &&
                loc.geofenceIdentifiers.equals(this.geofenceIdentifiers) &&
                (loc.locationType != LocationType.Suburb || loc.suburb.equals(suburb));
    }

    public static Location fromDbString(String str, NovaBot novaBot) {
        if (novaBot.getConfig().isAllowAllLocation() && str.equals("all")) return Location.ALL;

        if (novaBot.getConfig().useGeofences()) {
            ArrayList<GeofenceIdentifier> identifiers = GeofenceIdentifier.fromString(str);

            if (identifiers.size() != 0) {
                return new Location(identifiers);
            }
        }

        if (novaBot.suburbs.isSuburb(str)) {
            return new Location(str);
        }
        if (str.equals("civic")) {
            return new Location("city");
        }
        novaBot.novabotLog.warn(str + ", from db string is null");
        return null;
    }

    public static Location fromString(final String str, NovaBot novaBot) {

        if (novaBot.getConfig().isAllowAllLocation() && str.equalsIgnoreCase(novaBot.getLocalString("All"))) return Location.ALL;

        if (novaBot.getConfig().useGeofences()) {
            ArrayList<GeofenceIdentifier> identifiers = GeofenceIdentifier.fromString(str);

            if (identifiers.size() != 0) {
                return new Location(identifiers);
            }
        }

        if (novaBot.suburbs.isSuburb(str)) {
            return new Location(str);
        }
        return null;
    }

    public String getSuburb() {
        return this.suburb;
    }

    public static String listToString(final Location[] locations) {
        StringBuilder str = new StringBuilder();
        if (locations.length == 1) {
            return locations[0].toWords();
        }
        for (int i = 0; i < locations.length; ++i) {
            if (i == locations.length - 1) {
                str.append("and ").append(locations[i].toWords());
            } else {
                str.append((i == locations.length - 2) ? (locations[i].toWords() + " ") : (locations[i].toWords() + ", "));
            }
        }
        return str.toString();
    }

    public String toDbString() {

        switch (locationType) {
            case Suburb:
                return this.suburb;
            case Geofence:
                return GeofenceIdentifier.listToString(this.geofenceIdentifiers);
            case All:
                return "all";
        }

        return null;
    }

    @Override
    public String toString() {

        switch (locationType) {
            case Suburb:
                return this.suburb;
            case Geofence:
                return GeofenceIdentifier.listToString(this.geofenceIdentifiers);
            case All:
                return all;
        }

        return null;
    }

    public String toWords() {
        return toString();
    }
}
