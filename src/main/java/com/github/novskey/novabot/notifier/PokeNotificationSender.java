package com.github.novskey.novabot.notifier;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.AlertChannel;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import com.github.novskey.novabot.pokemon.PokeSpawn;
import com.github.novskey.novabot.pokemon.Pokemon;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;


public class PokeNotificationSender extends NotificationSender implements Runnable {

    public static Logger notificationLog = LoggerFactory.getLogger("Poke-Notif-Sender");
    private Logger localLog;

    public PokeNotificationSender(NovaBot novaBot, int id) {
        this.novaBot = novaBot;
        localLog = LoggerFactory.getLogger("Poke-Notif-Sender-" + id);
    }

    @Override
    public void run() {
        try {
            while (novaBot.getConfig().pokemonEnabled()) {
                PokeSpawn pokeSpawn = novaBot.notificationsManager.pokeQueue.take();
                localLog.info("Checking against global filter: " + pokeSpawn.id);

                if(novaBot.getConfig().useGlobalFilter()) {
                    if (novaBot.getConfig().passesGlobalFilter(pokeSpawn)) {
                        localLog.info("Passed global filter, continuing processing");
                    } else {
                        localLog.info("Didn't pass global filter, skipping spawn");
                        continue;
                    }
                }

                localLog.info("Checking if anyone wants: " + Pokemon.idToName(pokeSpawn.id));

                if (pokeSpawn.disappearTime.isBefore(ZonedDateTime.now(UtilityFunctions.UTC))) {
                    localLog.info("Already despawned, skipping");
                    continue;
                }

                HashSet<String> toNotify = new HashSet<>(novaBot.dataManager.getUserIDsToNotify(pokeSpawn));
                ArrayList<String> matchingPresets = novaBot.getConfig().findMatchingPresets(pokeSpawn);

                for (String preset : matchingPresets) {
                    toNotify.addAll(novaBot.dataManager.getUserIDsToNotify(preset, pokeSpawn));
                }
                
                //For special spawns, remove users not in one of the special spawns roles
                if (novaBot.getConfig().isSpecialSpawn(pokeSpawn.id)) {
                    toNotify.removeIf(userid -> !novaBot.isAllowedSpecialSpawns(userid));
                }

                if (toNotify.size() == 0) {
                    localLog.info("no-one wants this pokemon");
                } else {
                    if (novaBot.getConfig().matchesRestrictedFilter(pokeSpawn)) {
                    	//see matchesRestrictedFilter for how the restrictedSpawns list works.
                    	localLog.info(String.format("Skipping a spammy %.2f %s in restrictedSpawns.", pokeSpawn.iv, Pokemon.idToName(pokeSpawn.id)));
                    	continue;
                    }
                    
                	//For weirdo notifications that only one person wants, minimize API calls to reduce costs:
                	boolean minimizeAPICalls = toNotify.size() <= 1;
                		
                    final Message message = pokeSpawn.buildMessage(novaBot.getFormatting(), minimizeAPICalls, localLog);
                    localLog.info("Built message for pokespawn");

                    localLog.info(
                    		String.format("Notifying %-12s %-2d %-6.2f %-15s ", Pokemon.idToName(pokeSpawn.id), pokeSpawn.level, pokeSpawn.iv, pokeSpawn.getProperties().get("city")) 
                    		+ String.join(", ", toNotify)
                    );
                    toNotify.forEach(userID -> this.notifyUser(userID, message));
                }

                for (GeofenceIdentifier geofenceIdentifier : pokeSpawn.getGeofences()) {
                    ArrayList<AlertChannel> channels = novaBot.getConfig().getPokeChannels(geofenceIdentifier);
                    if (channels == null) continue;
                    for (AlertChannel channel : channels) {
                        if (channel != null) {
                            checkAndPost(channel, pokeSpawn);
                        }
                    }
                }
                ArrayList<AlertChannel> noGeofences = novaBot.getConfig().getNonGeofencedPokeChannels();

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
        if (novaBot.getConfig().matchesFilter(novaBot.getConfig().getPokeFilters().get(channel.getFilterName()), pokeSpawn, channel.getFilterName())) {
            localLog.info("Pokemon passed filter, posting to Discord");
            sendChannelAlert(pokeSpawn.buildMessage(channel.getFormattingName()), channel.getChannelId());
        } else {
            localLog.info(String.format("Pokemon didn't pass %s filter, not posting", channel.getFilterName()));
        }
    }

    private void notifyUser(final String userID, final Message message) {
        final User user = novaBot.getUserJDA(userID).getUserById(userID);
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
        novaBot.getNextNotificationBot().getTextChannelById(channelId).sendMessage(message).queue(m -> localLog.info("Successfully sent message."));
    }
}
