package com.github.novskey.novabot.rocketincident;

import static com.github.novskey.novabot.maps.Geofencing.getGeofence;

import java.awt.Color;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;

import com.github.novskey.novabot.core.Spawn;
import com.github.novskey.novabot.data.SpawnLocation;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import com.github.novskey.novabot.researchtask.ResearchTaskSpawn;

import lombok.Getter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;

public class RocketIncidentSpawn extends Spawn{
	private final HashMap<String, Message> builtMessages = new HashMap<>();
	@Getter
	public final String pokestop_name;
	@Getter
    public ZonedDateTime disappearTime;
    
	public RocketIncidentSpawn(double lat, double lon, String pokestop_name, ZonedDateTime disappearTime) {
		this.lat = lat;
        getProperties().put("lat", String.valueOf(lat));
		this.lon = lon;
		//Ugh, "lng"
        getProperties().put("lng", String.valueOf(lon));
		this.pokestop_name = pokestop_name;
        getProperties().put("pokestop_name", pokestop_name);
        this.disappearTime = disappearTime;
        
        //Generated properties:
        if (novaBot.suburbsEnabled()) {
            this.setGeocodedLocation(novaBot.reverseGeocoder.geocodedLocation(lat, lon));
            getGeocodedLocation().getProperties().forEach(getProperties()::put);
        }
        this.geofenceIdentifiers = getGeofence(lat, lon);
        this.setSpawnLocation(new SpawnLocation(getGeocodedLocation(), geofenceIdentifiers));
        getProperties().put("geofence", GeofenceIdentifier.listToString(geofenceIdentifiers));
        getProperties().put("gmaps", getGmapsLink());
        getProperties().put("applemaps", getAppleMapsLink());
	}

	public Message buildMessage(String formatFile) {
        if (builtMessages.get(formatFile) == null) {
            if (!getProperties().containsKey("city")) {
                novaBot.reverseGeocoder.geocodedLocation(lat, lon).getProperties().forEach(getProperties()::put);
            }

            final MessageBuilder messageBuilder = new MessageBuilder();
            final EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(Color.RED);

            formatKey = "rocketincident";
            embedBuilder.setDescription(novaBot.getConfig().formatStr(getProperties(), novaBot.getConfig().getBodyFormatting(formatFile, formatKey)));
                
            embedBuilder.setTitle(novaBot.getConfig().formatStr(getProperties(), novaBot.getConfig().getTitleFormatting(formatFile, formatKey)), novaBot.getConfig().formatStr(getProperties(), novaBot.getConfig().getTitleUrl(formatFile, formatKey)));
            //embedBuilder.setThumbnail(getIcon());
            if (novaBot.getConfig().showMap(formatFile, formatKey)) {
                embedBuilder.setImage(getImage(formatFile));
            }
            embedBuilder.setFooter(novaBot.getConfig().getFooterText(), null);
            embedBuilder.setTimestamp(Instant.now());
            messageBuilder.setEmbed(embedBuilder.build());

            String contentFormatting = novaBot.getConfig().getContentFormatting(formatFile, formatKey);

            if (contentFormatting != null && !contentFormatting.isEmpty()) {
                messageBuilder.append(novaBot.getConfig().formatStr(getProperties(), novaBot.getConfig().getContentFormatting(formatFile, formatKey)));
            }

            builtMessages.put(formatFile, messageBuilder.build());
        }
        return builtMessages.get(formatFile);
    }

	public ResearchTaskSpawn toResearchTaskSpawn() {
		return new ResearchTaskSpawn(lat, lon, pokestop_name,
				"Team Rocket", "Team Rocket", "Scanner"
		);
	}

}
