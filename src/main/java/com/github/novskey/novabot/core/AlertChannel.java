package com.github.novskey.novabot.core;

import com.github.novskey.novabot.maps.GeofenceIdentifier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.HashSet;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class AlertChannel {

    private final String channelId;

    private TextChannel channel = null;

    @NonNull
    private String filterName;

    private HashSet<GeofenceIdentifier> geofences = null;
    private String formattingName;

    public AlertChannel(String channelId) {
        this.channelId = channelId;
    }

}
