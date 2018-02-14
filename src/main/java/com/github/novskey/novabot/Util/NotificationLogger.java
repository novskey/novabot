package com.github.novskey.novabot.Util;

import com.github.novskey.novabot.core.NovaBot;
import net.dv8tion.jda.core.entities.User;

import java.util.function.Consumer;

public class NotificationLogger {
    private NovaBot nova;

    public NotificationLogger(NovaBot nova) {
        this.nova = nova;
    }

    public Consumer<Throwable> logPMBlocked(User user) {
        nova.novabotLog.info(user.getName() + " has messages blocked. Please inform them to pause notifications instead of blocking. I will now ensure user is paused.");
        nova.dataManager.pauseUser(user.getId());
        return null;
    }

}
