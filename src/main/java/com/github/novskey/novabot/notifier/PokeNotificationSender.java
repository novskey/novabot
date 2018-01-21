package com.github.novskey.novabot.notifier;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.AlertChannel;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import com.github.novskey.novabot.pokemon.PokeSpawn;
import com.github.novskey.novabot.pokemon.Pokemon;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;


public class PokeNotificationSender extends NotificationSender implements Runnable {

    public static Logger notificationLog = LoggerFactory.getLogger("Poke-Notif-Sender");
    private JDA jdaInstance;
    private Logger localLog;

    public PokeNotificationSender(NovaBot novaBot, int id) {
        this.novaBot = novaBot;
        localLog = LoggerFactory.getLogger("Poke-Notif-Sender-" + id);
    }

    @Override
    public void run() {
        try {
            while (novaBot.config.pokemonEnabled()) {
                PokeSpawn pokeSpawn = novaBot.notificationsManager.pokeQueue.take();
                this.jdaInstance = novaBot.getNextNotificationBot();
                localLog.info("Checking if anyone wants: " + Pokemon.idToName(pokeSpawn.id));

                if (pokeSpawn.disappearTime.isBefore(ZonedDateTime.now(UtilityFunctions.UTC))) {
                    localLog.info("Already despawned, skipping");
                    continue;
                }

                HashSet<String> toNotify = new HashSet<>(novaBot.dataManager.getUserIDsToNotify(pokeSpawn));

                ArrayList<String> matchingPresets = novaBot.config.findMatchingPresets(pokeSpawn);

                for (String preset : matchingPresets) {
                    toNotify.addAll(novaBot.dataManager.getUserIDsToNotify(preset, pokeSpawn));
                }

                if (toNotify.size() == 0) {
                    localLog.info("no-one wants this pokemon");
                } else {
                    final Message message = pokeSpawn.buildMessage("formatting.ini");
                    localLog.info("Built message for pokespawn");

                    toNotify.forEach(userID -> this.notifyUser(userID, message));
                }

                for (GeofenceIdentifier geofenceIdentifier : pokeSpawn.getGeofences()) {
                    ArrayList<AlertChannel> channels = novaBot.config.getPokeChannels(geofenceIdentifier);
                    if (channels == null) continue;
                    for (AlertChannel channel : channels) {
                        if (channel != null) {
                            checkAndPost(channel, pokeSpawn);
                        }
                    }
                }
                ArrayList<AlertChannel> noGeofences = novaBot.config.getNonGeofencedPokeChannels();

                if (noGeofences != null) {
                    for (AlertChannel channel : noGeofences) {
                        if (channel != null) {
                            checkAndPost(channel, pokeSpawn);
                        }
                    }
                }
            }
        } catch (Exception e) {
            localLog.error("An exception ocurred in poke-notif-sender", e);
        }

    }

    private void checkAndPost(AlertChannel channel, PokeSpawn pokeSpawn) {
        if (novaBot.config.matchesFilter(novaBot.config.pokeFilters.get(channel.getFilterName()), pokeSpawn, channel.getFilterName())) {
            localLog.info("Pokemon passed filter, posting to Discord");
            sendChannelAlert(pokeSpawn.buildMessage(channel.getFormattingName()), channel.getChannelId());
        } else {
            localLog.info(String.format("Pokemon didn't pass %s filter, not posting", channel.getFilterName()));
        }
    }

    private void notifyUser(final String userID, final Message message) {
        final User user = jdaInstance.getUserById(userID);
        if (user == null) return;


        ZonedDateTime lastChecked = novaBot.lastUserRoleChecks.get(userID);
        ZonedDateTime currentTime = ZonedDateTime.now(UtilityFunctions.UTC);
        if (lastChecked == null || lastChecked.isBefore(currentTime.minusMinutes(10))) {
            localLog.info(String.format("Checking supporter status of %s", user.getName()));
            novaBot.lastUserRoleChecks.put(userID, currentTime);
            if (checkSupporterStatus(user)) {
                user.openPrivateChannel().queue(channel -> channel.sendMessage(message).queue());
            }
        } else {
            user.openPrivateChannel().queue(channel -> channel.sendMessage(message).queue());
        }
    }

    private void sendChannelAlert(Message message, String channelId) {
        localLog.info("Sending public alert message to channel " + channelId);
        jdaInstance.getTextChannelById(channelId).sendMessage(message).queue(m -> localLog.info("Successfully sent message."));
    }
}
