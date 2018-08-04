package com.github.novskey.novabot.notifier;

import com.github.novskey.novabot.core.NovaBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

class PokeNotifier implements Runnable
{
    private static final Logger notifierLog = LoggerFactory.getLogger("PokeNotifier");
    private final boolean testing;
    private final NotificationsManager manager;
    private final NovaBot novaBot;

    public PokeNotifier(NotificationsManager manager, final NovaBot novaBot, boolean testing) {
        this.manager = manager;
        this.novaBot = novaBot;
        this.testing = testing;
    }

    @Override
    public void run() {
        try {
            notifierLog.debug("Total threads: " + ManagementFactory.getThreadMXBean().getThreadCount());
            notifierLog.info("checking for pokemon and pokestops to notify");
            novaBot.dataManager.getNewPokemon();
            novaBot.dataManager.getUpdatedPokestops();
            notifierLog.debug("Done checking and adding to queue for processing");
        } catch(Exception e){
            notifierLog.error("An error ocurred in PokeNotifier", e);
        }
    }
}
