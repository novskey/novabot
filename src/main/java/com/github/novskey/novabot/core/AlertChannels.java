package com.github.novskey.novabot.core;

import com.github.novskey.novabot.maps.GeofenceIdentifier;

import java.util.ArrayList;

public class AlertChannels extends ArrayList<AlertChannel> {

    public AlertChannels getChannelsByGeofence(GeofenceIdentifier identifier) {
        AlertChannels channels = null;

        for (AlertChannel alertChannel : this) {
            if (alertChannel.getGeofences() != null && alertChannel.getGeofences().contains(identifier)) {
                if (channels == null) channels = new AlertChannels();
                channels.add(alertChannel);
            }
        }
        return channels;
    }

    public AlertChannels getNonGeofencedChannels() {
        AlertChannels channels = null;

        for (AlertChannel alertChannel : this) {
            if (alertChannel.getGeofences() == null) {
                if (channels == null) channels = new AlertChannels();
                channels.add(alertChannel);
            }

        }

        return channels;
    }

}
