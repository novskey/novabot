package com.github.novskey.novabot.notifier;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.AlertChannel;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import com.github.novskey.novabot.pokemon.PokeSpawn;
import com.github.novskey.novabot.pokemon.Pokemon;
import com.github.novskey.novabot.researchtask.ResearchTaskSpawn;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;


public class ResearchTaskNotificationSender extends NotificationSender implements Runnable {

    public static Logger notificationLog = LoggerFactory.getLogger("ResearchTask-Notif-Sender");
    private Logger localLog;

    public ResearchTaskNotificationSender(NovaBot novaBot, int id) {
        this.novaBot = novaBot;
        localLog = LoggerFactory.getLogger("ResearchTask-Notif-Sender-" + id);
    }

    @Override
    public void run() {
        try {
            while (true) {
                localLog.info("Waiting to retrieve object from researchTaskQueue");
		        ResearchTaskSpawn researchTaskSpawn = novaBot.notificationsManager.researchTaskQueue.take();
	            
		        /*
	            localLog.info("Checking against global filter: " + researchTaskSpawn.reward);
                if(novaBot.getConfig().useGlobalFilter()) {
                    if (novaBot.getConfig().passesGlobalFilter(researchTaskSpawn)) {
                        localLog.info("Passed global filter, continuing processing");
                    } else {
                        localLog.info("Didn't pass global filter, skipping spawn");
                        continue;
                    }
                }
                */

                localLog.info("Checking if anyone wants research task: " + researchTaskSpawn.getProperties());
                
                HashSet<String> toNotify = new HashSet<>(novaBot.dataManager.getUserIDsToNotify(researchTaskSpawn));
                ArrayList<String> matchingPresets = novaBot.getConfig().findMatchingPresets(researchTaskSpawn);

                for (String preset : matchingPresets) {
                    toNotify.addAll(novaBot.dataManager.getUserIDsToNotify(preset, researchTaskSpawn));
                }

                if (toNotify.size() == 0) {
                    localLog.info("no-one wants this research task");
                } else {
                    final Message message = researchTaskSpawn.buildMessage(novaBot.getFormatting());
                    toNotify.forEach(userID -> this.notifyUser(userID, message));
                }
	           
	            for (GeofenceIdentifier geofenceIdentifier : researchTaskSpawn.getGeofences()) {
	                ArrayList<AlertChannel> channels = novaBot.getConfig().getResearchTaskChannels(geofenceIdentifier);
	                if (channels == null) continue;
	                for (AlertChannel channel : channels) {
	                    if (channel != null) {
	                    	checkAndPostResearchTask(channel, researchTaskSpawn);
	                    }
	                }
	            }
	            ArrayList<AlertChannel> noGeofences = novaBot.getConfig().getNonGeofencedResearchTaskChannels();

	            if (noGeofences != null) {
	                for (AlertChannel channel : noGeofences) {
	                    if (channel != null) {
	                    	checkAndPostResearchTask(channel, researchTaskSpawn);
	                    }
	                }
	            }
            } //while loop
        } catch (Exception e) {
            localLog.error("An exception ocurred in researchtask-notif-sender", e);
        }

    }

    private void checkAndPostResearchTask(AlertChannel channel, ResearchTaskSpawn researchTaskSpawn) {
    	if (novaBot.getConfig().matchesFilter(novaBot.getConfig().getResearchTaskFilters().get(channel.getFilterName()), researchTaskSpawn, channel.getFilterName())) {
            localLog.info("Research task passed filter, posting to channel "+channel.getChannelId());
            sendChannelAlert(researchTaskSpawn.buildMessage(channel.getFormattingName()), channel.getChannelId());
        } else {
            localLog.info(String.format("Research task didn't pass %s filter, not posting", channel.getFilterName()));
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
