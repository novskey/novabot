package com.github.novskey.novabot.notifier;

import com.github.novskey.novabot.core.NotificationLimit;
import com.github.novskey.novabot.core.NovaBot;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.PermissionException;

import java.util.function.Consumer;

class NotificationSender {

    public final String WHITE_GREEN_CHECK = "\u2705";

    NovaBot novaBot;

    boolean checkSupporterStatus(User user) {
        NotificationLimit limit = novaBot.getConfig().getNotificationLimit(novaBot.guild.getMember(user));

        boolean passedChecks = true;

        int pokeCount = novaBot.dataManager.countPokemon(user.getId(), null, novaBot.getConfig().countLocationsInLimits());
        if (limit.pokemonLimit != null && pokeCount > limit.pokemonLimit) {
            resetUser(user,limit);
            passedChecks = false;
        }

        if (passedChecks) {
            int presetCount = novaBot.dataManager.countPresets(user.getId(), null, novaBot.getConfig().countLocationsInLimits());
            if (limit.presetLimit != null && presetCount > limit.presetLimit) {
                resetUser(user,limit);
                passedChecks = false;

            }

            if (passedChecks) {
                int raidCount = novaBot.dataManager.countRaids(user.getId(), null, novaBot.getConfig().countLocationsInLimits());
                if (limit.raidLimit != null && raidCount > limit.raidLimit) {
                    resetUser(user,limit);
                    passedChecks = false;
                }
            }
        }
        return passedChecks;
    }

    private void resetUser(User user, NotificationLimit newLimit) {
        novaBot.dataManager.resetUser(user.getId());

        user.openPrivateChannel().queue(

                success -> {
                    success.sendMessageFormat("Hi %s, I noticed that recently your supporter status has changed. As a result I have cleared your settings. At your current level you can add up to %s to your settings.", user, newLimit.toWords()).queue(
                            s -> novaBot.novabotLog.info("Successfully sent message to " + user.getName()),
                            e -> {
                                if (e instanceof PermissionException) {
                                    PermissionException pe = (PermissionException) e;
                                    Permission missingPermission = pe.getPermission();
                                    novaBot.novabotLog.info("On openPrivateChannel: PermissionError: " + e.getMessage() + " \n Missing permission: " + missingPermission.getName() + "\nUser: " + user.getName());
                                } else
                                    novaBot.novabotLog.info("Unknown error opening private channel with user: " + user.getName() + "\n" + e.getMessage() + "\nUser: " + user.getName());
                            }
                    );
                },
                err -> {
                    if (err instanceof PermissionException) {
                        PermissionException pe = (PermissionException) err;
                        Permission missingPermission = pe.getPermission();
                        novaBot.novabotLog.info("On openPrivateChannel: PermissionError: " + err.getMessage() + " \n Missing permission: " + missingPermission.getName() + "\nUser: " + user.getName());
                    } else
                        novaBot.novabotLog.info("Unknown error opening private channel with user: " + user.getName() + "\n" + err.getMessage() + "\nUser: " + user.getName());
                }
        );

        if (novaBot.getConfig().loggingEnabled()) {
            novaBot.roleLog.sendMessageFormat("%s's supporter status has changed, requiring a reset of their settings. They have been informed via PM.", user).queue();
        }
    }
}
