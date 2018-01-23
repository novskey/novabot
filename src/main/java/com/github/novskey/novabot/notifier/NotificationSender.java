package com.github.novskey.novabot.notifier;

import com.github.novskey.novabot.core.NotificationLimit;
import com.github.novskey.novabot.core.NovaBot;
import net.dv8tion.jda.core.entities.User;

class NotificationSender {

    public final String WHITE_GREEN_CHECK = "\u2705";

    NovaBot novaBot;

    boolean checkSupporterStatus(User user) {
        NotificationLimit limit = novaBot.getConfig().getNotificationLimit(novaBot.guild.getMember(user));

        boolean passedChecks = true;

        int pokeCount = novaBot.dataManager.countPokemon(user.getId(), novaBot.getConfig().countLocationsInLimits());
        if (limit.pokemonLimit != null && pokeCount > limit.pokemonLimit) {
            resetUser(user,limit);
            passedChecks = false;
        }

        if (passedChecks) {
            int presetCount = novaBot.dataManager.countPresets(user.getId(), novaBot.getConfig().countLocationsInLimits());
            if (limit.presetLimit != null && presetCount > limit.presetLimit) {
                resetUser(user,limit);
                passedChecks = false;

            }

            if (passedChecks) {
                int raidCount = novaBot.dataManager.countRaids(user.getId(), novaBot.getConfig().countLocationsInLimits());
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

        user.openPrivateChannel().queue(channel -> channel.sendMessageFormat("Hi %s, I noticed that recently your supporter status has changed." +
                " As a result I have cleared your settings. At your current level you can add up to %s to your settings.",user,newLimit.toWords()).queue());

        if (novaBot.getConfig().loggingEnabled()) {
            novaBot.roleLog.sendMessageFormat("%s's supporter status has changed, requiring a reset of their settings. They have been informed via PM.", user).queue();
        }
    }
}
