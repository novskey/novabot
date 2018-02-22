package com.github.novskey.novabot.Util;

import com.github.novskey.novabot.core.NovaBot;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.Channel;

import java.util.function.Consumer;

public class NotificationLogger {
    private NovaBot nova;

    public NotificationLogger(NovaBot nova) {
        this.nova = nova;
    }

    public Consumer<Throwable> logPMBlocked(User user) {
        nova.novabotLog.info("Failed sending private message to user: " + user.getName() + ".  hasPMChannel=" + user.hasPrivateChannel());
        return null;
    }

    public Consumer<Throwable> logBadChannelPerms(String channel) {
        nova.novabotLog.info("Failed sending message to channel: " + channel);
        return null;
    }

}
