package com.github.novskey.novabot.core;

import com.github.novskey.novabot.Util.CommandLineOptions;
import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.data.DataManager;
import com.github.novskey.novabot.data.SpawnLocation;
import com.github.novskey.novabot.maps.Geofencing;
import com.github.novskey.novabot.maps.ReverseGeocoder;
import com.github.novskey.novabot.maps.TimeZones;
import com.github.novskey.novabot.notifier.NotificationsManager;
import com.github.novskey.novabot.notifier.RaidNotificationSender;
import com.github.novskey.novabot.parser.*;
import com.github.novskey.novabot.pokemon.Pokemon;
import com.github.novskey.novabot.raids.LobbyManager;
import com.github.novskey.novabot.raids.Raid;
import com.github.novskey.novabot.raids.RaidLobby;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.novskey.novabot.core.Spawn.printFormat24hr;

public class NovaBot {

    public final String WHITE_GREEN_CHECK = "\u2705";
    public final Logger novabotLog = LoggerFactory.getLogger("novabot");
    public final ConcurrentHashMap<String, ZonedDateTime> lastUserRoleChecks = new ConcurrentHashMap<>();
    public final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public final String configName;
    private final String geofences;
    public final String supporterLevels;
    private final String suburbsName;
    public final String gkeys;
    public final String formatting;
    public final String raidChannels;
    public final String pokeChannels;
    public final String presets;
    public TextChannel roleLog;
    public Guild guild = null;
    public boolean testing = false;
    private CommandLineOptions cliopt;
    public Config config;
    public SuburbManager suburbs;
    public ArrayList<Invite> invites = new ArrayList<>();
    public JDA jda;
    public LobbyManager lobbyManager;
    public MessageChannel userUpdatesLog;
    public Geofencing geofencing;
    public ReverseGeocoder reverseGeocoder;
    public Commands commands;
    public NotificationsManager notificationsManager;
    public Parser parser;
    private ResourceBundle messagesBundle;
    private ResourceBundle timeUnitsBundle;
    private HashMap<String, String> localStringCache = new HashMap<>(100);
    private String locale = "en";
    public TimeZones timeZones;
    private ArrayList<JDA> notificationBots = new ArrayList<>();
    private int lastNotificationBot = 0;
    public DataManager dataManager;
    
    public NovaBot(CommandLineOptions cliopt) {
        this.configName = cliopt.getConfig();
        this.geofences = cliopt.getGeofences();
        this.supporterLevels = cliopt.getSupporterLevels();
        this.suburbsName = cliopt.getSuburbs();
        this.gkeys = cliopt.getGkeys();
        this.formatting = cliopt.getFormatting();
        this.raidChannels = cliopt.getRaidChannels();
        this.pokeChannels = cliopt.getPokeChannels();
        this.presets = cliopt.getPresets();
        this.cliopt = cliopt;
    }

    public NovaBot() {
        this(new CommandLineOptions());
    }


    public void alertRaidChats(String[] raidChatIds, String message) {
        for (String raidChatId : raidChatIds) {
            guild.getTextChannelById(raidChatId).sendMessageFormat(message).queue(
                    m -> m.addReaction(WHITE_GREEN_CHECK).queue()
            );
        }
    }


    public void loadConfig() {
        config = new Config(testing ? "config.example.ini" : configName, gkeys, formatting, raidChannels, pokeChannels,
                supporterLevels, presets);
    }

    public void loadGeofences() {
        geofencing = new Geofencing(this);
        geofencing.loadGeofences(geofences);
    }

    public void loadSuburbs() {
        suburbs = new SuburbManager(new File(suburbsName), this);
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));
        if (args.length % 2 != 0) {
            System.out.println("Uneven number of command line arguments. Make sure each argument has a matching value.");
            return;
        }

        CommandLineOptions cliopt = CommandLineOptions.parse(args);

        NovaBot novaBot = new NovaBot(cliopt);
        novaBot.setLocale(cliopt.getLocale());
        novaBot.setup();
        novaBot.start();
    }

    public void parseModMsg(Message msg, TextChannel channel) {
        if (msg.getContentDisplay().startsWith("!joindate")) {

            StringBuilder response = new StringBuilder(String.format("%s, the results of your search are:\n\n", msg.getAuthor().getAsMention()));

            List<User> mentionedUsers = msg.getMentionedUsers();

            for (User mentionedUser : mentionedUsers) {
                OffsetDateTime joinDate = guild.getMember(mentionedUser).getJoinDate();
                String formattedDate = joinDate.toInstant().atZone(config.getTimeZone()).format(formatter);

                response.append(String.format("  %s joined at %s", mentionedUser.getAsMention(), formattedDate));
            }

            channel.sendMessage(response.toString()).queue();
        }
    }

    public void parseMsg(final String msg, final User author, final MessageChannel channel) {

        if (!msg.startsWith(getLocalString("Prefix"))) {
            return;
        }

        if (msg.equals(getLocalString("ApiQuotasCommand"))){
            if (isAdmin(author)){
                channel.sendMessage(getApiQuotasMessage()).queue();
            }else {
                novabotLog.info(String.format("%s doesn't have the admin role required for !apiquotas",author.getName()));
            }
            return;
        }

        if (msg.equals(getLocalString("ReloadCommand"))) {
            if (isAdmin(author)) {
                loadConfig();
                loadSuburbs();
                loadGeofences();
            }
            channel.sendMessageFormat("%s, %s", author, getLocalString("ReloadMessage")).queue();
            return;
        }

        if (msg.equals(getLocalString("PauseCommand"))) {
            dataManager.pauseUser(author.getId());
            channel.sendMessageFormat("%s, %s", author, getLocalString("PauseMessage")).queue();
            return;
        }

        if (msg.equals(getLocalString("UnPauseCommand"))) {
            dataManager.unPauseUser(author.getId());
            channel.sendMessageFormat("%s, %s", author, getLocalString("UnPauseMessage")).queue();
            return;
        }

        if (msg.startsWith(getLocalString("JoinRaidCommand")) && config.isRaidOrganisationEnabled()) {
            String groupCode = msg.substring(msg.indexOf(" ") + 1).trim();

            RaidLobby lobby = lobbyManager.getLobby(groupCode);

            if (lobby == null) {
                channel.sendMessageFormat("%s %s", author, getLocalString("NoRaidLobbyMessage").replace("<lobbycode>", groupCode)).queue();
                return;
            } else {
                if (lobby.containsUser(author.getId())) {
                    channel.sendMessageFormat("%s %s", author, getLocalString("AlreadyInLobbyMessage")).queue();
                    return;
                }

                lobby.joinLobby(author.getId());

                String alertMsg = getLocalString("AlertRaidChatsMessage");
                alertMsg = alertMsg.replaceAll("<user>", author.getAsMention());
                alertMsg = alertMsg.replaceAll("<boss-or-egg>", (lobby.spawn.bossId == 0 ? String.format("%s %s %s", getLocalString("Level"), lobby.spawn.raidLevel, getLocalString("Egg")) : lobby.spawn.getProperties().get("pkmn")));
                alertMsg = alertMsg.replaceAll("<channel>", lobby.getChannel().getAsMention());
                alertMsg = alertMsg.replaceAll("<membercount>", String.valueOf(lobby.memberCount()));
                alertMsg = alertMsg.replaceAll("<lobbycode>", groupCode);

                alertRaidChats(config.getRaidChats(lobby.spawn.getGeofences()), alertMsg);

                String joinMsg = getLocalString("JoinRaidLobbyMessage");
                joinMsg = joinMsg.replaceAll("<channel>", lobby.getChannel().getAsMention());
                joinMsg = joinMsg.replaceAll("<lobbysize>", String.valueOf(lobby.memberCount()));
                channel.sendMessageFormat("%s %s", joinMsg).queue();
            }

            return;
        }

        if (msg.equals(getLocalString("SettingsCommand"))) {
            final UserPref userPref = dataManager.getUserPref(author.getId());
            novabotLog.debug("!settings");
            if (userPref == null || (userPref.isRaidEmpty() && userPref.isPokeEmpty() && userPref.isPresetEmpty())) {
                channel.sendMessage(author.getAsMention() + ", " + getLocalString("NoSettingsMessage")).queue();
            } else {
                String toSend = author.getAsMention() + ", " + getLocalString("SettingsMessageStart");
                toSend += userPref.allSettingsToString();
                final MessageBuilder builder = new MessageBuilder();
                builder.append(toSend);
                final Queue<Message> messages = builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE);
                for (final Message message : messages) {
                    channel.sendMessage(message).queue();
                }
            }
            return;
        } else if (msg.equals(getLocalString("PokemonSettingsCommand")) && config.pokemonEnabled()) {
            final UserPref userPref = dataManager.getUserPref(author.getId());
            novabotLog.debug("!pokemonsettings");
            if (userPref == null || userPref.isPokeEmpty()) {
                channel.sendMessage(author.getAsMention() + ", " + getLocalString("NoPokemonSettingsMessage")).queue();
            } else {
                String toSend = author.getAsMention() + ", " + getLocalString("PokemonSettingsMessageStart");
                toSend += userPref.allPokemonToString();
                final MessageBuilder builder = new MessageBuilder();
                builder.append(toSend);
                final Queue<Message> messages = builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE);
                for (final Message message : messages) {
                    channel.sendMessage(message).queue();
                }
            }
            return;
        } else if (msg.equals(getLocalString("RaidSettingsCommand")) && config.raidsEnabled()) {
            final UserPref userPref = dataManager.getUserPref(author.getId());
            novabotLog.debug("!raidsettings");
            if (userPref == null || userPref.isRaidEmpty()) {
                channel.sendMessage(author.getAsMention() + ", " + getLocalString("NoRaidSettingsMessage")).queue();
            } else {
                String toSend = author.getAsMention() + ", " + getLocalString("RaidSettingsMessageStart");
                toSend += userPref.allRaidsToString();
                final MessageBuilder builder = new MessageBuilder();
                builder.append(toSend);
                final Queue<Message> messages = builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE);
                for (final Message message : messages) {
                    channel.sendMessage(message).queue();
                }
            }
            return;
        } else if (config.presets.size() > 0 && (msg.equals(getLocalString("PresetSettingsCommand")))) {
            UserPref userPref = dataManager.getUserPref(author.getId());
            novabotLog.debug("!presetsettings");
            if (userPref == null || userPref.isPresetEmpty()) {
                channel.sendMessageFormat("%s, " + getLocalString("NoPresetSettingsMessage"), author).queue();
            } else {
                String toSend = String.format("%s, %s%s", author.getAsMention(), getLocalString("PresetSettingsMessage"), userPref.allPresetsToString());
                MessageBuilder builder = new MessageBuilder();
                builder.append(toSend);
                Queue<Message> messages = builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE);
                for (final Message message : messages) {
                    channel.sendMessage(message).queue();
                }
            }
            return;
        } else if (config.presets.size() > 0 && (msg.equals(getLocalString("PresetsCommand")) || msg.equals(getLocalString("PresetListCommand")))) {
            MessageBuilder builder = new MessageBuilder();
            builder.appendFormat("%s, %s%s", author, getLocalString("PresetListMessageStart"), config.getPresetsList());

            Queue<Message> messages = builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE);
            for (final Message message : messages) {
                channel.sendMessage(message).queue();
            }
            return;
        } else if (msg.equals(getLocalString("ResetCommand"))) {
            dataManager.resetUser(author.getId());
            channel.sendMessageFormat("%s, %s", author, getLocalString("ResetMessage")).queue();
            return;
        } else if (msg.equals(getLocalString("ResetPokemonCommand")) && config.pokemonEnabled()) {
            dataManager.resetPokemon(author.getId());
            channel.sendMessageFormat("%s, %s", author, getLocalString("ResetPokemonMessage")).queue();
            return;
        } else if (msg.equals(getLocalString("ResetRaidsCommand")) && config.raidsEnabled()) {
            dataManager.resetRaids(author.getId());
            channel.sendMessageFormat("%s, %s", author, getLocalString("ResetRaidsMessage")).queue();
            return;
        } else if (msg.equals(getLocalString("ResetPresetsCommand")) && config.presetsEnabled()) {
            dataManager.resetPresets(author.getId());
            channel.sendMessageFormat("%s, %s", author, getLocalString("ResetPresetsMessage")).queue();
            return;
//        }
//        }else if(msg.equals("!activeraids")){
//            ArrayList<GeofenceIdentifier> geofences = config.getRaidChatGeofences(channel.getId());
//
//            if(geofences.size() > 0 || channel.getType() == ChannelType.PRIVATE){
//
//                String noLobbiesMsg = null;
//
//                for (GeofenceIdentifier geofence : geofences) {
//
//                    ArrayList<RaidLobby> lobbies = lobbyManager.getLobbiesByGeofence(geofence);
//
//                    if(lobbies.size() == 0){
//                        if(noLobbiesMsg == null){
//                            noLobbiesMsg = String.format("%s, there are no active lobbies in %s", author.getAsMention(),geofence.name);
//                        }else{
//                            noLobbiesMsg += String.format(", %s",geofence.name);
//                        }
//                        continue;
//                    }
//
//                    channel.sendMessageFormat("%s, there are %s active lobbies in %s",author,lobbies.size(),geofence.name).queue();
//
//                    for (RaidLobby lobby : lobbies) {
//                        channel.sendMessage(lobby.getInfoMessage()).queue(m -> m.addReaction(WHITE_GREEN_CHECK).queue());
//                    }
//                }
//
//                if(noLobbiesMsg != null){
//                    channel.sendMessage(noLobbiesMsg).queue();
//                }
//
//                return;
//            }else{
//                channel.sendMessageFormat(
//                        "%s, I only accept the `!activeraids` command in PM or the following raid discussion channels:%n%n" +
//                        "%s",author,config.raidChatsList()).queue();
//                return;
//            }
        } else if (msg.equals(getLocalString("HelpCommand"))) {
            channel.sendMessageFormat(getLocalString("HelpMessageStart") +
                    (config.pokemonEnabled() ? getLocalString("HelpMessagePokemonCommands") : "") +
                    (config.raidsEnabled() ? getLocalString("HelpMessageRaidCommands") : "") +
                    (config.presets.size() > 0 ? getLocalString("HelpMessagePresetCommands") : "") +
                    getLocalString("HelpMessageOtherCommandsStart") +
                    (config.statsEnabled() ? getLocalString("HelpMessageStatsCommand") : "") +
                    (config.isRaidOrganisationEnabled()
                            && config.getRaidChatGeofences(channel.getLatestMessageId()).size() > 0
                            || channel.getType() == ChannelType.PRIVATE
                            ? getLocalString("HelpMessageJoinLobbyCommand")
                            : "") +
//                    (config.isRaidOrganisationEnabled()
//                            && config.getRaidChatGeofences(channel.getLatestMessageId()).size() > 0
//                            || channel.getType() == ChannelType.PRIVATE
//                            ? "!!activeraids\n"
//                            : "") +
                    (config.useGeofences() ? getLocalString("HelpMessageRegionCommands") : "") +
                    (suburbsEnabled() ? getLocalString("HelpMessageSuburbCommands") : "") +
                    getLocalString("HelpMessageOtherCommands")).queue();
            return;
        } else if (config.useGeofences() && (msg.equals(getLocalString("RegionListCommand")) || msg.equals(getLocalString("RegionsCommand")))) {
            MessageBuilder builder = new MessageBuilder().appendFormat("%s, %s%n%s", author, getLocalString("RegionListMessageStart"), Geofencing.getListMessage());
            builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE).forEach(m -> channel.sendMessage(m).queue());
            return;
        } else if (suburbsEnabled() && (msg.equals(getLocalString("SuburbListCommand")) || msg.equals(getLocalString("SuburbsCommand")))) {
            MessageBuilder builder = new MessageBuilder().appendFormat("%s, %s%n%s", author, getLocalString("SuburbListMessageStart"), suburbs.getListMessage());
            builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE).forEach(m -> channel.sendMessage(m).queue());
            return;
        }

        UserCommand userCommand;
        userCommand = parser.parseInput(msg);

        final ArrayList<InputError> exceptions = userCommand.getExceptions();

        final String cmdStr = (String) userCommand.getArg(0).getParams()[0];
        String matchingCommand = findMatch(cmdStr);

        if (matchingCommand != null && matchingCommand.equals("statscommand")){
            exceptions.remove(InputError.BlacklistedPokemon);
        }

        if (exceptions.size() > 0) {
            String errorMessage = author.getAsMention() + ", " + ((exceptions.size() > 1) ? getLocalString("ProblemsReadingInput") : getLocalString("ProblemReadingInput"));
            final InputError error = InputError.mostSevere(exceptions);
            errorMessage += error.getErrorMessage(userCommand);
            channel.sendMessage(errorMessage).queue();
            return;
        }



        if(matchingCommand == null){
            return;
        }

        if (matchingCommand.equals("statscommand")) {
            Pokemon[] pokemons = userCommand.buildPokemon();

            StringBuilder str = new StringBuilder(author.getAsMention() + ", " + getLocalString("StatsMessageStart"));

            for (Pokemon pokemon : pokemons) {

                com.github.novskey.novabot.core.TimeUnit timeUnit = (com.github.novskey.novabot.core.TimeUnit) userCommand.getArg(ArgType.TimeUnit).getParams()[0];

                int intervalLength = (int) userCommand.getArg(ArgType.Int).getParams()[0];

                int count = dataManager.countSpawns(pokemon.getID(), timeUnit, intervalLength);

                str.append(String.format("  %s %s%s %s %s %s%n%n",
                        count,
                        pokemon.name,
                        count == 1 ? "" : "s",
                        getLocalString("StatsMessageResult"),
                        intervalLength,
                        getLocalString(String.valueOf(timeUnit).toLowerCase())));

            }
            channel.sendMessage(str.toString()).queue();
            return;
        }

        if (matchingCommand.contains("raid")) {
            Raid[] raids = userCommand.buildRaids();

            ArrayList<String> nonRaidBosses = new ArrayList<>();

            for (Pokemon pokemon : userCommand.getUniquePokemon()) {
                if (!config.raidBosses.contains(pokemon.getID())) {
                    nonRaidBosses.add(Pokemon.idToName(pokemon.getID()));
                }
            }

            if (nonRaidBosses.size() != 0) {
                StringBuilder message = new StringBuilder(author.getAsMention() + getLocalString("ProblemReadingInput") +
                        getLocalString("NotPossibleRaidBossesError"));

                for (String nonRaidBoss : nonRaidBosses) {
                    message.append(String.format("  %s%n", nonRaidBoss));
                }

                channel.sendMessage(message.toString()).queue();
                return;
            }

            switch (matchingCommand) {
                case "addraidcommand": {
                    NotificationLimit limit = config.getNotificationLimit(guild.getMember(author));

                    boolean isSupporter = isSupporter(author.getId());

                    if (limit != null && limit.raidLimit != null && dataManager.countRaids(author.getId(), config.countLocationsInLimits()) + raids.length > limit.raidLimit) {
                        channel.sendMessageFormat("%s %s %s %s. " +
                                (limit.raidLimit > 0 ? getLocalString("ExceededNonZeroRaidLimitMessage") : ""),
                                author,
                                getLocalString("ExceedLimitMessageStart"),
                                limit.raidLimit,
                                getLocalString("ExceedRaidLimitMessageEnd")).queue();
                        return;
                    } else if (limit == null && isSupporter) {
                        novabotLog.error(String.format("LIMIT IS NULL: %s, is supporter: %s", author.getName(), true));
                    }

                    if (dataManager.notContainsUser(author.getId())) {
                        dataManager.addUser(author.getId());
                    }

                    for (Raid raid : raids) {
                        novabotLog.debug("adding raid " + raid);
                        dataManager.addRaid(author.getId(), raid);
                    }

                    String message2 = String.format("%s, %s %s",author.getAsMention(),getLocalString("YouWillNowBeNotifiedOf"),Pokemon.listToString(userCommand.getUniquePokemon()));

                    final Argument locationsArg = userCommand.getArg(ArgType.Locations);
                    Location[] locations = {Location.ALL};
                    if (locationsArg != null) {
                        locations = userCommand.getLocations();
                    }
                    message2 += String.format(" %s %s", getLocalString("RaidsIn"),Location.listToString(locations));
                    channel.sendMessage(message2).queue();

                    return;
                }
                case "delraidcommand": {
                    if (dataManager.notContainsUser(author.getId())) {
                        dataManager.addUser(author.getId());
                    }

                    for (Raid raid : raids) {
                        novabotLog.debug("deleting raid " + raid);
                        dataManager.deleteRaid(author.getId(), raid);
                    }

                    String message2 = String.format("%s, %s %s",author.getAsMention(), getLocalString("YouWillNoLongerBeNotifiedOf"),Pokemon.listToString(userCommand.getUniquePokemon()));

                    final Argument locationsArg = userCommand.getArg(ArgType.Locations);
                    Location[] locations = {Location.ALL};
                    if (locationsArg != null) {
                        locations = userCommand.getLocations();
                    }
                    message2 += String.format(" %s %s",getLocalString("RaidsIn"),Location.listToString(locations));
                    channel.sendMessage(message2).queue();

                    return;
                }
                case "clearraidcommand": {
                    if (dataManager.notContainsUser(author.getId())) {
                        dataManager.addUser(author.getId());
                    }
                    novabotLog.debug("clearing raids " + UtilityFunctions.arrayToString(raids));
                    dataManager.clearRaid(author.getId(), new ArrayList<>(Arrays.asList(raids)));

                    String message2 = String.format("%s %s %s %s %s", author.getAsMention(), getLocalString("YouWillNoLongerBeNotifiedOf"),Pokemon.listToString(userCommand.getUniquePokemon()), getLocalString("Raids"), getLocalString("InAnyLocations"));
                    channel.sendMessage(message2).queue();
                    return;
                }
                case "clearraidlocationcommand": {
                    final Location[] locations2 = userCommand.getLocations();
                    dataManager.clearLocationsRaids(author.getId(), locations2);
                    final String message2 = String.format("%s %s %s", author.getAsMention(), getLocalString("ClearRaidLocationMessage"),Location.listToString(locations2));
                    channel.sendMessage(message2).queue();
                    break;
                }
            }
        }

        if (matchingCommand.contains("poke")) {
            final Pokemon[] pokemons = userCommand.buildPokemon();

            switch (matchingCommand) {
                case "addpokemoncommand": {
                    NotificationLimit limit = config.getNotificationLimit(guild.getMember(author));

                    boolean isSupporter = isSupporter(author.getId());

                    if (limit != null && limit.pokemonLimit != null && dataManager.countPokemon(author.getId(), config.countLocationsInLimits()) + pokemons.length > limit.pokemonLimit) {
                        channel.sendMessageFormat("%s %s %s %s",
                                author,
                                getLocalString("ExceedLimitMessageStart"),
                                limit.pokemonLimit,
                                getLocalString("ExceedPokeLimitMessageEnd"),
                                (limit.pokemonLimit > 0 ? getLocalString("ExceededNonZeroPokeLimitMessage") : "")).queue();
                        return;
                    } else if (limit == null && isSupporter) {
                        novabotLog.error(String.format("LIMIT IS NULL: %s, is supporter: %s", author.getName(), isSupporter));
                    }

                    if (dataManager.notContainsUser(author.getId())) {
                        dataManager.addUser(author.getId());
                    }
                    for (final Pokemon pokemon : pokemons) {
                        novabotLog.debug("adding pokemon " + pokemon);
                        dataManager.addPokemon(author.getId(), pokemon);
                    }
                    String message2 = String.format("%s, %s %s",author.getAsMention(),getLocalString("YouWillNowBeNotifiedOf"),Pokemon.listToString(userCommand.getUniquePokemon()));
                    String ivMessage = userCommand.getIvMessage();
                    message2 += ivMessage;

                    String levelMessage = userCommand.getLevelMessage();
                    message2 += (!ivMessage.isEmpty() && !levelMessage.isEmpty() ? " and" : "") + levelMessage;

                    String cpMessage = userCommand.getCpMessage();
                    message2 += ((!ivMessage.isEmpty() || !levelMessage.isEmpty()) && !cpMessage.isEmpty() ? " and" : "") + cpMessage;

                    final Argument locationsArg = userCommand.getArg(ArgType.Locations);
                    Location[] locations = {Location.ALL};
                    if (locationsArg != null) {
                        locations = userCommand.getLocations();
                    }
                    message2 += String.format(" %s %s", getLocalString("In"),Location.listToString(locations));
                    channel.sendMessage(message2).queue();
                    return;
                }
                case "delpokemoncommand": {
                    for (final Pokemon pokemon : pokemons) {
                        dataManager.deletePokemon(author.getId(), pokemon);
                    }
                    String message2 = String.format("%s %s %s",author.getAsMention(),getLocalString("YouWillNoLongerBeNotifiedOf"),Pokemon.listToString(userCommand.getUniquePokemon()));
                    message2 += userCommand.getIvMessage();

                    final Argument locationsArg = userCommand.getArg(ArgType.Locations);
                    Location[] locations = {Location.ALL};
                    if (locationsArg != null) {
                        locations = userCommand.getLocations();
                    }
                    String ivMessage = userCommand.getIvMessage();
                    message2 += ivMessage;

                    String levelMessage = userCommand.getLevelMessage();
                    message2 += (!ivMessage.isEmpty() && !levelMessage.isEmpty() ? " and" : "") + levelMessage;

                    String cpMessage = userCommand.getCpMessage();
                    message2 += ((!ivMessage.isEmpty() || !levelMessage.isEmpty()) && !cpMessage.isEmpty() ? " and" : "") + cpMessage;

                    message2 += String.format(" %s %s", getLocalString("In"),Location.listToString(locations));

                    channel.sendMessage(message2).queue();
                    return;
                }
                case "clearpokemoncommand": {
                    dataManager.clearPokemon(author.getId(), new ArrayList<>(Arrays.asList(pokemons)));
                    final String message2 = String.format("%s %s %s %s",
                            author.getAsMention(),getLocalString("YouWillNoLongerBeNotifiedOf"),Pokemon.listToString(pokemons),getLocalString("InAnyLocations"));
                    channel.sendMessage(message2).queue();
                    return;
                }
                case "clearpokelocationcommand": {
                    final Location[] locations2 = userCommand.getLocations();
                    dataManager.clearLocationsPokemon(author.getId(), locations2);
                    final String message2 = String.format("%s %s %s",
                            author.getAsMention(),getLocalString("YouWillNoLongerBeNotifiedOfPokemonIn"),Location.listToString(locations2));
                    channel.sendMessage(message2).queue();
                }
            }
        } else if (matchingCommand.contains("preset")) {

            final Argument locationsArg = userCommand.getArg(ArgType.Locations);
            Location[] locations = {Location.ALL};
            if (locationsArg != null) {
                locations = userCommand.getLocations();
            }

            switch (matchingCommand) {
                case "loadpresetcommand": {
                    NotificationLimit limit = config.getNotificationLimit(guild.getMember(author));

                    boolean isSupporter = isSupporter(author.getId());

                    if (limit != null && limit.presetLimit != null && dataManager.countPresets(author.getId(), config.countLocationsInLimits()) + locations.length > limit.presetLimit) {
                        channel.sendMessageFormat("%s %s %s %s %s",
                                author,
                                getLocalString("ExceedLimitMessageStart"),
                                limit.presetLimit,
                                getLocalString("ExceedPresetLimitMessageEnd"),
                                (limit.presetLimit > 0 ? getLocalString("ExceededNonZeroPresetLimitMessage") : "")).queue();
                        return;
                    } else if (limit == null && isSupporter) {
                        novabotLog.error(String.format("LIMIT IS NULL: %s, is supporter: %s", author.getName(), isSupporter));
                    }

                    Object[] presetsObj = userCommand.getArg(ArgType.Preset).getParams();
                    String[] presets = Arrays.copyOf(presetsObj, presetsObj.length, String[].class);

                    for (String preset : presets) {
                        for (Location location : locations) {
                            dataManager.addPreset(author.getId(), preset, location);
                        }
                    }

                    String message = String.format("%s %s %s %s", 
                            author.getAsMention(),
                            getLocalString("LoadPresetMessageStart"),
                            UtilityFunctions.arrayToString(presets),
                            presets.length > 1 ? getLocalString("Presets") : getLocalString("Preset"));

                    message += String.format(" %s %s", getLocalString("In"),Location.listToString(locations));
                    channel.sendMessage(message).queue();
                    return;
                }
                case "delpresetcommand": {
                    Object[] presetsObj = userCommand.getArg(ArgType.Preset).getParams();
                    String[] presets = Arrays.copyOf(presetsObj, presetsObj.length, String[].class);

                    for (String preset : presets) {
                        for (Location location : locations) {
                            dataManager.deletePreset(author.getId(), preset, location);
                        }
                    }

                    String message = String.format("%s %s %s %s", 
                            author.getAsMention(),
                            getLocalString("DelPresetMessageStart"),
                            UtilityFunctions.arrayToString(presets),
                            presets.length > 1 ? getLocalString("Presets") : getLocalString("Preset"));

                    message += String.format(" %s %s", getLocalString("In"), Location.listToString(locations));
                    channel.sendMessage(message).queue();
                    return;
                }
                case "clearpresetcommand": {
                    Object[] presetsObj = userCommand.getArg(ArgType.Preset).getParams();
                    String[] presets = Arrays.copyOf(presetsObj, presetsObj.length, String[].class);
                    dataManager.clearPreset(author.getId(), presets);
                    channel.sendMessageFormat("%s %s %s %s %s.", 
                            author,
                            getLocalString("DelPresetMessageStart"),
                            UtilityFunctions.arrayToString(presets),
                            presets.length > 1 ? getLocalString("Presets") : getLocalString("Preset"),
                            getLocalString("InAnyLocations")
                            ).queue();
                    return;
                }
                case "clearpresetlocationcommand": {
                    locations = userCommand.getLocations();
                    dataManager.clearLocationsPresets(author.getId(), locations);
                    channel.sendMessageFormat("%s %s %s",
                            author,
                            getLocalString("ClearPresetLocationMessage"),
                            Location.listToString(locations)).queue();
                    return;
                }
            }
        } else if (matchingCommand.equals("clearlocationcommand")) {
            final Location[] locations2 = userCommand.getLocations();
            dataManager.clearLocationsPokemon(author.getId(), locations2);
            dataManager.clearLocationsRaids(author.getId(), locations2);
            final String message2 = String.format("%s %s %s",
                    author.getAsMention(),
                    getLocalString("ClearLocationMessage"),
                    Location.listToString(locations2));
            channel.sendMessage(message2).queue();
        }
    }

    private Message getApiQuotasMessage() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Google API Key Usage");
        int geocodingRequests = reverseGeocoder.getRequests();
        builder.addField("Reverse Geocoding",String.format("%s key%s active. %s request%s this session.",
                config.getGeocodingKeys().size(),
                config.getGeocodingKeys().size() == 1 ? "" : "s",
                geocodingRequests,
                geocodingRequests == 1 ? "" : "s"),
                false);
        int timeZoneRequests = timeZones.getRequests();
        builder.addField("Time Zones",String.format("%s key%s active. %s request%s this session.",
                config.getTimeZoneKeys().size(),
                config.getTimeZoneKeys().size() == 1 ? "" : "s",
                timeZoneRequests,
                timeZoneRequests == 1 ? "" : "s"),
                false);
        int mapRequests = Spawn.getRequests();
        builder.addField("Static Maps",String.format("%s key%s active. %s request%s this session.",
                config.getStaticMapKeys().size(),
                config.getStaticMapKeys().size() == 1? "" : "s",
                mapRequests,
                mapRequests == 1 ? "" : "s"),
                false);
        MessageBuilder messageBuilder = new MessageBuilder(builder.build());
        return messageBuilder.build();
    }

    private String findMatch(String cmdStr) {
        for (String key : messagesBundle.keySet()) {
            if(messagesBundle.getString(key).equals(cmdStr)){
                return key.toLowerCase();
            }
        }

        return null;
    }

    public String getLocalString(String key) {
        if(!localStringCache.containsKey(key)){
            localStringCache.put(key, formatStr(formatStr(getLocalString(getLocalString(key, messagesBundle), timeUnitsBundle), messagesBundle), messagesBundle));
        }
        return localStringCache.get(key);
    }

    private String getLocalString(String key, ResourceBundle resourceBundle) {
        if (resourceBundle.containsKey(key)) {
            return formatStr(resourceBundle.getString(key), resourceBundle);
        } else {
            return key;
        }
    }

    private String formatStr(String string, ResourceBundle resourceBundle) {
        for (String key : resourceBundle.keySet()) {
            if (resourceBundle.containsKey(key)) {
                string = string.replace("<" + key.toLowerCase() + ">", resourceBundle.getString(key));
            }
        }
        return string;
    }

    public void parseRaidChatMsg(User author, String msg, TextChannel textChannel) {
        if (!msg.startsWith(getLocalString("Prefix")) || author.isBot()) return;

        if (msg.startsWith(getLocalString("JoinRaidCommand"))) {
            String groupCode = msg.substring(msg.indexOf(" ") + 1).trim();

            RaidLobby lobby = lobbyManager.getLobby(groupCode);

            if (lobby == null) {
                textChannel.sendMessageFormat("%s sorry, there are no active raid lobbies with the lobby code `%s`", author, groupCode).queue();
            } else {
                if (lobby.containsUser(author.getId())) {
                    textChannel.sendMessageFormat("%s you are already in that raid lobby!", author).queue();
                    return;
                }

                lobby.joinLobby(author.getId());
                alertRaidChats(config.getRaidChats(lobby.spawn.getGeofences()), String.format(
                        "%s joined %s raid in %s. There are now %s users in the lobby. Join the lobby by clicking the ✅ or by typing `!joinraid %s`.",
                        author.getAsMention(),
                        (lobby.spawn.bossId == 0 ? String.format("lvl %s egg", lobby.spawn.raidLevel) : lobby.spawn.getProperties().get("pkmn")),
                        lobby.getChannel().getAsMention(),
                        lobby.memberCount(),
                        lobby.lobbyCode
                ));
            }

//        } else if (msg.equals("!activeraids")) {
//            ArrayList<GeofenceIdentifier> geofences = config.getRaidChatGeofences(textChannel.getId());
//
//            if (geofences.size() > 0 || textChannel.getType() == ChannelType.PRIVATE) {
//
//                StringBuilder noLobbiesMsg = null;
//
//                for (GeofenceIdentifier geofence : geofences) {
//
//                    ArrayList<RaidLobby> lobbies = lobbyManager.getLobbiesByGeofence(geofence);
//
//                    if (lobbies.size() == 0) {
//                        if (noLobbiesMsg == null) {
//                            noLobbiesMsg = new StringBuilder(String.format("%s, there are no active lobbies in %s", author.getAsMention(), geofence.name));
//                        } else {
//                            noLobbiesMsg.append(String.format(", %s", geofence.name));
//                        }
//                        continue;
//                    }
//
//                    textChannel.sendMessageFormat("%s, there are %s active lobbies in %s", author, lobbies.size(), geofence.name).queue();
//
//                    for (RaidLobby lobby : lobbies) {
//                        textChannel.sendMessage(lobby.getInfoMessage()).queue(m -> m.addReaction(WHITE_GREEN_CHECK).queue());
//                    }
//                }
//
//                if (noLobbiesMsg != null) {
//                    textChannel.sendMessage(noLobbiesMsg.toString()).queue();
//                }
//
//            }
        }
    }

    public void parseRaidLobbyMsg(User author, String msg, TextChannel textChannel) {
        if (!msg.startsWith(getLocalString("Prefix"))) return;

        RaidLobby lobby = lobbyManager.getLobbyByChannelId(textChannel.getId());

        if (msg.equals(getLocalString("HelpCommand"))) {
            textChannel.sendMessageFormat("%s " +
                    "```" +
                    "%s\n" +
                    "%s\n" +
                    "%s\n" +
                    "%s\n" +
                    (lobby.spawn.bossId != 0 ? "%s\n" : "") +
                    (lobby.spawn.bossId != 0 ? "%s\n" : "") +
                    "%s\n" +
                    "%s\n" +
                    "```",
                    getLocalString("RaidLobbyHelpStart"),
                    getLocalString("LeaveCommand"),
                    getLocalString("MapCommand"),
                    getLocalString("TimeLeftCommand"),
                    getLocalString("StatusCommand"),
                    getLocalString("BossCommand"),
                    getLocalString("MaxCpCommand"),
                    getLocalString("TeamCommand"),
                    getLocalString("CodeCommand")).queue();
            return;
        }

        if (msg.equals(getLocalString("LeaveCommand"))) {
            lobby.leaveLobby(author.getId());
            return;
        }

        if (msg.equals(getLocalString("MapCommand"))) {
            textChannel.sendMessage(lobby.spawn.getProperties().get("gmaps")).queue();
            return;
        }

        if (msg.equals(getLocalString("TimeLeftCommand"))) {

            if (lobby.spawn.bossId != 0) {
                textChannel.sendMessageFormat("%s %s (%s %s)",
                        getLocalString("RaidEndsAt"),
                        lobby.spawn.getDisappearTime(printFormat24hr),
                        lobby.spawn.timeLeft(lobby.spawn.raidEnd),
                        getLocalString("Remaining")).queue();
            } else {
                textChannel.sendMessageFormat("%s %s (%s %s)",
                        getLocalString("RaidStartsAt"),
                        lobby.spawn.getStartTime(printFormat24hr),
                        lobby.spawn.timeLeft(lobby.spawn.battleStart),
                        getLocalString("Remaining")).queue();
            }
            return;
        }

        if (msg.equals(getLocalString("StatusCommand"))) {
            textChannel.sendMessage(lobby.getStatusMessage()).queue();
            return;
        }

        if (msg.equals(getLocalString("BossCommand"))) {
            if (lobby.spawn.bossId == 0) {
                textChannel.sendMessageFormat("%s %s",
                        getLocalString("BossNotSpawnedYet"),
                        lobby.spawn.getProperties().get("24h_start")).queue();
            } else {
                textChannel.sendMessage(lobby.getBossInfoMessage()).queue();
            }
            return;
        }

        if (msg.equals(getLocalString("MaxCpCommand"))) {
            if (lobby.spawn.bossId == 0) {
                textChannel.sendMessageFormat("%s %s",
                        getLocalString("BossNotSpawnedYet"),
                        lobby.spawn.getProperties().get("24h_start")).queue();
            } else {
                textChannel.sendMessage(lobby.getMaxCpMessage()).queue();
            }
            return;
        }

        if (msg.equals(getLocalString("TeamCommand"))) {
            textChannel.sendMessage(lobby.getTeamMessage()).queue();
            return;
        }

        if (msg.equals(getLocalString("CodeCommand"))) {
            textChannel.sendMessageFormat("%s `%s %s`",
                    getLocalString("CodeMessageStart"),
                    getLocalString("JoinRaidCommand"),
                    lobby.lobbyCode).queue();
        }
    }

    public void setup() {
        messagesBundle = ResourceBundle.getBundle("Messages",new Locale(locale));
        timeUnitsBundle = ResourceBundle.getBundle("TimeUnits",new Locale(locale));
        TimeUnit.SetBundle(timeUnitsBundle);
        Team.setBundle(messagesBundle);
        SpawnLocation.novaBot = this;
        Location.all = getLocalString("All");
        loadConfig();
        loadSuburbs();

        if (config.useGeofences() && (geofencing == null || !geofencing.loaded)) {
            loadGeofences();
        }

        Spawn.setNovaBot(this);

        commands = new Commands(config);
        parser = new Parser(this);

        reverseGeocoder = new ReverseGeocoder(this);

        timeZones = new TimeZones(this);

        dataManager = new DataManager(this);

        if (config.isRaidOrganisationEnabled()) {
            lobbyManager = new LobbyManager(this);
            RaidNotificationSender.setNextId(dataManager.highestRaidLobbyId() + 1);
        }
    }

    public void shutDown() {
        novabotLog.info("Shutting down...");
        if (jda != null) {
            jda.shutdown();
        }
        System.exit(0);
    }

    private boolean isAdmin(User author) {
        for (Guild g : jda.getGuilds()) {
            Member member = g.getMember(author);
            if(member == null) continue;
            for (Role role : member.getRoles()) {
                if (role.getId().equals(config.getAdminRole())) return true;
            }
        }
        return false;
    }

    private boolean isSupporter(final String userID) {
        final Member member = guild.getMemberById(userID);

        if (member == null || member.getRoles() == null) return false;

        for (final Role role : member.getRoles()) {
            if (role == null) continue;
            if (config.getSupporterRoles().contains(role.getId())) return true;
        }
        return false;
    }

    public void start() {
        novabotLog.info("Connecting to db");
        novabotLog.info("Connected");
        novabotLog.info("Purging unknown spawnpoints so they can be geocoded again");
        int purged = dataManager.purgeUnknownSpawnpoints();
        novabotLog.info(String.format("Purged %s spawnpoints",purged));

        Game pokemonGo = Game.of(Game.GameType.DEFAULT, "Pokemon Go");
        try {
            novabotLog.info("Logging in main bot");
            jda = new JDABuilder(AccountType.BOT)
                    .setAutoReconnect(true)
                    .setGame(pokemonGo)
                    .setToken(config.getToken())
                    .buildBlocking();

            jda.addEventListener(new MessageListener(this,true));

            notificationBots.add(jda);
            if (config.getNotificationTokens().size() > 0){
                int botNum = 1;
                for (String token : config.getNotificationTokens()) {
                    if (!token.equals(config.getToken())) {
                        novabotLog.info("Logging in notification bot #" + botNum);
                        notificationBots.add(new JDABuilder(AccountType.BOT)
                                .setAutoReconnect(true)
                                .setGame(pokemonGo)
                                .setToken(token)
                                .addEventListener(new MessageListener(this, false))
                                .buildBlocking());
                        botNum++;
                    }
                }
            }

            for (Guild guild1 : jda.getGuilds()) {
                novabotLog.info("Connected to guild: " + guild1.getName());
                if (guild == null) {
                    guild = guild1;

                    if (config.getCommandChannelId() != null) {
                        TextChannel channel = guild.getTextChannelById(config.getCommandChannelId());
                        if (channel != null) {
                            if (config.showStartupMessage()) {
                                channel.sendMessage(getLocalString("StartUpMessage")).queue();
                            }
                        } else {
                            novabotLog.info(String.format("couldn't find command channel by id from config: %s", config.getCommandChannelId()));
                        }
                    }
                }
            }

            loadEmotes(guild, jda);

            guild.getMember(jda.getSelfUser()).getRoles().forEach(System.out::println);

            guild.getInvites().queue(success -> invites.addAll(success));

            if (config.loggingEnabled()) {
                roleLog = jda.getTextChannelById(config.getRoleLogId());
                userUpdatesLog = jda.getTextChannelById(config.getUserUpdatesId());
            }

            if (config.useScanDb()) {
                notificationsManager = new NotificationsManager(this, testing);
                notificationsManager.start();
            }

        } catch (LoginException | InterruptedException | RateLimitedException ex2) {
            novabotLog.error("Error starting bot",ex2);
        }


        novabotLog.info("connected");
    }

    public boolean suburbsEnabled() {
        return this.suburbs.notEmpty();
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }


    public void loadEmotes(Guild guild, JDA jda) {
        for (String type : Types.TYPES) {
            List<Emote> found = jda.getEmotesByName(type, true);
            String path = null;
            if (found.size() == 0) try {
                path = "static/icons/" + type + ".png";

                guild.getController().createEmote(type, Icon.from(new File(path))).queue(emote ->
                        Types.emotes.put(type, emote));
            } catch (IOException e) {
                novabotLog.warn(String.format("Couldn't find emote file: %s, ignoring.", path));
            }
            else {
                Types.emotes.put(type, found.get(0));
            }
        }
        novabotLog.info(String.format("Finished loading type emojis: %s", Types.emotes.toString()));

        for (Team team : Team.values()) {
            List<Emote> found = jda.getEmotesByName(team.toString().toLowerCase(), true);
            String path = null;
            if (found.size() == 0) try {
                path = "static/icons/" + team.toString().toLowerCase() + ".png";
                guild.getController().createEmote(team.toString().toLowerCase(), Icon.from(new File(path))).queue(emote ->
                        Team.emotes.put(team, emote));
            } catch (IOException e) {
                novabotLog.warn(String.format("Couldn't find emote file: %s, ignoring.", path));
            }
            else {
                Team.emotes.put(team, found.get(0));
            }
        }
        novabotLog.info(String.format("Finished loading team emojis: %s", Team.emotes.toString()));
    }

    public synchronized JDA getNextNotificationBot() {
        if (lastNotificationBot == notificationBots.size() - 1) {
            lastNotificationBot = 0;
            return notificationBots.get(lastNotificationBot);
        }
        ++lastNotificationBot;
        return notificationBots.get(lastNotificationBot);
    }
}
