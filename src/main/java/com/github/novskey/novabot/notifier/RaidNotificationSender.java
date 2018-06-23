package com.github.novskey.novabot.notifier;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.AlertChannel;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import com.github.novskey.novabot.raids.RaidLobby;
import com.github.novskey.novabot.raids.RaidSpawn;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * Created by Owner on 27/06/2017.
 */
public class RaidNotificationSender extends NotificationSender implements Runnable {


    public static final Logger notificationLog = LoggerFactory.getLogger("Raid-Notif-Sender");
    private final int id;
    private Logger localLog;


    private static int nextId = 1;

    public synchronized static int getNextId(){
        if (nextId == 10000){
            nextId = 1;
            return nextId;
        }else {
            nextId++;
            return nextId - 1;
        }
    }

    public RaidNotificationSender(NovaBot novaBot, int id) {
        this.novaBot = novaBot;
        this.id =id;
        localLog = LoggerFactory.getLogger("Raid-Notif-Sender-" + id);
    }

    public synchronized static void setNextId(int nextId) {
        RaidNotificationSender.nextId = nextId;
    }

    @Override
    public void run() {
        novaBot.novabotLog.info("Started raid thread " + id);
        try {
            while (novaBot.getConfig().raidsEnabled()) {
                localLog.info("Waiting to retrieve object from raidqueue");
                RaidSpawn raidSpawn = novaBot.notificationsManager.raidQueue.take();
                localLog.info("Checking " + raidSpawn);

                if (raidSpawn.battleStart.isBefore(ZonedDateTime.now(UtilityFunctions.UTC)) && raidSpawn.bossId == 0) {
                    localLog.info("Raid started but no boss Id, not posting");
                    continue;
                }

                if (raidSpawn.raidEnd.isBefore(ZonedDateTime.now(UtilityFunctions.UTC))) {
                    localLog.info("Raid already ended, not posting");
                    continue;
                }

                if (novaBot.getConfig().isRaidOrganisationEnabled()) {
                    RaidLobby lobbyFromId = novaBot.lobbyManager.getLobbyByGymId(raidSpawn.gymId);

                    if (lobbyFromId != null && lobbyFromId.spawn.bossId == 0) {
                        localLog.info("Raid already has a lobby, but the egg has now hatched, updating lobby");
                        raidSpawn.setLobbyCode(lobbyFromId.lobbyCode);
                        lobbyFromId.spawn = raidSpawn;
                        lobbyFromId.alertEggHatched();
                    } else {

                        if (raidSpawn.raidLevel >= 3) {

                            raidSpawn.setLobbyCode(getNextId());

                            novaBot.lobbyManager.newRaid(raidSpawn.getLobbyCode(), raidSpawn);
                        }
                    }
                }

                localLog.info("Checking if anyone wants: " + raidSpawn);

                HashSet<String> toNotify = new HashSet<>(novaBot.dataManager.getUserIDsToNotify(raidSpawn));

                ArrayList<String> matchingPresets = novaBot.getConfig().findMatchingPresets(raidSpawn);

                for (String preset : matchingPresets) {
                    toNotify.addAll(novaBot.dataManager.getUserIDsToNotify(preset, raidSpawn));
                }

                toNotify.forEach(id -> notifyUser(id, raidSpawn.buildMessage(novaBot.getFormatting()), raidSpawn.raidLevel >= 3 && novaBot.getConfig().isRaidOrganisationEnabled()));

                if (!novaBot.getConfig().isRaidChannelsEnabled()) continue;

                for (GeofenceIdentifier identifier : raidSpawn.getGeofences()) {
                    ArrayList<AlertChannel> channels = novaBot.getConfig().getRaidChannels(identifier);

                    if (channels == null) continue;

                    for (AlertChannel channel : channels) {
                        if (channel != null) {
                            checkAndPost(channel, raidSpawn);
                        }
                    }
                }

                ArrayList<AlertChannel> noGeofences = novaBot.getConfig().getNonGeofencedRaidChannels();

                if (noGeofences != null) {
                    for (AlertChannel channel : noGeofences) {
                        if (channel != null) {
                            checkAndPost(channel, raidSpawn);
                        }
                    }
                }
            }
        } catch (Exception e) {
            localLog.error("An error occurred in Raid-Notif-Sender", e);
        }
    }

    private void checkAndPost(AlertChannel channel, RaidSpawn raidSpawn) {
        localLog.info(String.format("Checking %s against filter %s", raidSpawn, channel.getFilterName()));
        if (novaBot.getConfig().matchesFilter(novaBot.getConfig().getRaidFilters().get(channel.getFilterName()), raidSpawn)) {
            localLog.info("Raid passed filter, posting to Discord");
            sendChannelAlert(raidSpawn.buildMessage(channel.getFormattingName()), channel.getChannelId(), raidSpawn.raidLevel);
        }
    }

    private void notifyUser(final String userID, final Message message, boolean showTick) {
        final User user = novaBot.getUserJDA(userID).getUserById(userID);
        if (user == null) return;

        ZonedDateTime lastChecked = novaBot.lastUserRoleChecks.get(userID);
        ZonedDateTime currentTime = ZonedDateTime.now(UtilityFunctions.UTC);
        if (lastChecked == null || lastChecked.isBefore(currentTime.minusMinutes(10))) {
            localLog.info(String.format("Checking supporter status of %s", user.getName()));
            novaBot.lastUserRoleChecks.put(userID, currentTime);
            if (checkSupporterStatus(user)) {
                user.openPrivateChannel().queue(
                        success -> success.sendMessage(message).queue(
                                suc -> {
                                    if (showTick) {
                                        suc.addReaction(WHITE_GREEN_CHECK).queue();
                                    }
                                },
                                err -> {
                                    if (err instanceof PermissionException) {
                                        PermissionException pe = (PermissionException) err;
                                        Permission missingPermission = pe.getPermission();
                                        novaBot.novabotLog.info("On sendMessage: PermissionError: " + err.getMessage() + " \n Missing permission: " + missingPermission.getName() + "\nUser: " + user.getName());
                                    } else
                                        novaBot.novabotLog.info("Unknown error sending message to user: " + user.getName() + "\n" + err.getMessage() + "\nUser: " + user.getName());
                                }
                        ),
                        error -> {
                            if (error instanceof PermissionException) {
                                PermissionException pe = (PermissionException) error;
                                Permission missingPermission = pe.getPermission();
                                novaBot.novabotLog.info("On openPrivateChannel: PermissionError: " + error.getMessage() + " \n Missing permission: " + missingPermission.getName() + "\nUser: " + user.getName());
                            } else
                                novaBot.novabotLog.info("Unknown error opening private channel with user: " + user.getName() + "\n" + error.getMessage() + "\nUser: " + user.getName());
                        }
                );
            }
        } else {
            //Is not supporter:
            user.openPrivateChannel().queue(

                    success -> success.sendMessage(message).queue(

                            suc -> {
                                if (showTick) {
                                    suc.addReaction(WHITE_GREEN_CHECK).queue();
                                }
                            },
                            err -> {
                                if (err instanceof PermissionException) {
                                    PermissionException pe = (PermissionException) err;
                                    Permission missingPermission = pe.getPermission();
                                    novaBot.novabotLog.info("On sendMessage: PermissionError: " + err.getMessage() + " \n Missing permission: " + missingPermission.getName() + "\nUser: " + user.getName());
                                } else
                                    novaBot.novabotLog.info("Unknown error sending message to user: " + user.getName() + "\n" + err.getMessage() + "\nUser: " + user.getName());
                            }
                    ),
                    error -> {
                        if (error instanceof PermissionException) {
                            PermissionException pe = (PermissionException) error;
                            Permission missingPermission = pe.getPermission();
                            novaBot.novabotLog.info("On openPrivateChannel: PermissionError: " + error.getMessage() + " \n Missing permission: " + missingPermission.getName() + "\nUser: " + user.getName());
                        } else
                            novaBot.novabotLog.info("Unknown error opening private channel with user: " + user.getName() + "\n" + error.getMessage() + "\nUser: " + user.getName());
                    }
            );
        }
    }

    private void sendChannelAlert(Message message, String channelId, int raidLevel) {
        localLog.info("Sending public alert message to channel " + channelId);
        TextChannel channel = novaBot.getNextNotificationBot().getTextChannelById(channelId);

        if(channel == null){
            localLog.warn(String.format("Couldn't find from ID %s",channelId));
        }
        channel.sendMessage(message).queue(
                success -> {
                    if (novaBot.getConfig().isRaidOrganisationEnabled() && raidLevel >= 3) {
                        System.out.println(String.format("adding reaction to raid with raidlevel %s", raidLevel));
                        success.addReaction(WHITE_GREEN_CHECK).queue(
                                s -> {
                                    localLog.info("Added reaction checkmark to message");
                                },
                                error -> {
                                    if (error instanceof InsufficientPermissionException) {
                                        InsufficientPermissionException pe = (InsufficientPermissionException) error;
                                        Permission missingPermission = pe.getPermission();
                                        novaBot.novabotLog.info("Channel: " + channel.getName() + " (" + channelId + ") PermissionError: " + ((PermissionException) error).getMessage() + " \n Missing permission: " + missingPermission.getName());
                                    } else
                                        novaBot.novabotLog.info("Unknown error sending message to channel: " + channelId + "\n" + error.getMessage());
                                });
                    }
                },
                error -> {
                    if (error instanceof InsufficientPermissionException) {
                        InsufficientPermissionException pe = (InsufficientPermissionException) error;
                        Permission missingPermission = pe.getPermission();
                        novaBot.novabotLog.info("Channel: " + channel.getName() + " (" + channelId + ") PermissionError: " + ((PermissionException) error).getMessage() + " \n Missing permission: " + missingPermission.getName());
                    } else
                        novaBot.novabotLog.info("Unknown error sending message to channel: " + channelId + "\n" + error.getMessage());
                }
        );
    }
}
