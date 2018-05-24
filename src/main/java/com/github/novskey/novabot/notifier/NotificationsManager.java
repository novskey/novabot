package com.github.novskey.novabot.notifier;

import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.core.ScheduledExecutor;
import com.github.novskey.novabot.pokemon.PokeSpawn;
import com.github.novskey.novabot.raids.RaidSpawn;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class NotificationsManager {

    private final NovaBot novaBot;
    private final boolean testing;
    public final LinkedBlockingQueue<PokeSpawn> pokeQueue = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<RaidSpawn> raidQueue = new LinkedBlockingQueue<>();

    public NotificationsManager(NovaBot novaBot, boolean testing) {
        this.novaBot = novaBot;
        this.testing = testing;
    }

    public void start(){
        final ScheduledExecutor executor = new ScheduledExecutor(novaBot.getConfig().getDbThreads());

        if (novaBot.getConfig().useScanDb() && novaBot.getConfig().pokemonEnabled()) {

            executor.scheduleAtFixedRate(new PokeNotifier(this, novaBot, testing), 0L, novaBot.getConfig().getPokePollingDelay(), TimeUnit.SECONDS);

            for (int i = 1; i <= novaBot.getConfig().getPokemonThreads(); i++) {
                new Thread(new PokeNotificationSender(novaBot,i)).start();
            }
        }

        if (novaBot.getConfig().useScanDb() && novaBot.getConfig().raidsEnabled()) {

            executor.scheduleAtFixedRate(new RaidNotifier(this, novaBot), 0, novaBot.getConfig().getRaidPollingDelay(), TimeUnit.SECONDS);

            for (int i = 1; i <= novaBot.getConfig().getRaidThreads(); i++) {
                novaBot.novabotLog.info("Starting raid thread " + i);
                new Thread(new RaidNotificationSender(novaBot,i)).start();
            }
        }
    }

}
