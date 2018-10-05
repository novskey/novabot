package com.github.novskey.novabot.core;

import com.github.novskey.novabot.Util.CommandLineOptions;
import com.github.novskey.novabot.Util.StringLocalizer;
import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.api.ApiManager;
import com.github.novskey.novabot.data.DataManager;
import com.github.novskey.novabot.data.Preset;
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
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;
import org.apache.commons.lang3.RandomStringUtils;

import static com.github.novskey.novabot.Util.StringLocalizer.getLocalString;
import static com.github.novskey.novabot.core.Spawn.printFormat24hr;
import static com.github.novskey.novabot.core.Weather.None;
import static com.github.novskey.novabot.parser.ArgType.CommandName;

public class NovaBot {

    public final String NUMBER_1 = "\u0031\u20E3";
    public final String NUMBER_2 = "\u0032\u20E3";
    public final String NUMBER_3 = "\u0033\u20E3";
    public final String NUMBER_4 = "\u0034\u20E3";
    public final String NUMBER_5 = "\u0035\u20E3";
    public final Logger novabotLog = LoggerFactory.getLogger("novabot");
    public final ConcurrentHashMap<String, ZonedDateTime> lastUserRoleChecks = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final String configName;
    private final String geofences;
    private final String supporterLevels;
    private final String suburbsName;
    private final String gkeys;
    private final String formatting;
    private final String raidChannels;
    private final String pokeChannels;
    private final String presets;
    private final String globalFilter;
    public TextChannel roleLog;
    public Guild guild = null;
    private boolean testing = false;
    private CommandLineOptions cliopt;
    private Config config;
    private SuburbManager suburbs;
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
    private HashMap<String, JDA> notificationBots = new HashMap<>();
    private ArrayList<String> botTokens = new ArrayList<>();
    private int lastNotificationBot = 0;
    public DataManager dataManager;

    private HashMap<String, Integer> botTokenUses = new HashMap<>();

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
        this.globalFilter = cliopt.getGlobalFilter();
        this.cliopt = cliopt;
    }

    public NovaBot() {
        this(new CommandLineOptions());
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public String getGlobalFilter() {
        return globalFilter;
    }

    public JDA getUserJDA(String userID) {
        com.github.novskey.novabot.data.User user = dataManager.getUser(userID);

        String token = null;

        if (user == null){
            token = getNextUserBotToken();
            dataManager.addUser(userID,token);
        }else{
            if (user.getBotToken() == null){
                token = getNextUserBotToken();
                dataManager.setBotToken(userID,token);
            }else{
                token = user.getBotToken();
            }
        }

        if (token == null){
                novabotLog.error(String.format("Null JDA for userID = %s. %s", userID,notificationBots));
        }

        return (token == null ? null : notificationBots.get(token));
    }


    private void loadConfig() {
        if (testing) {
            cliopt.setConfig("config.example.ini");
        }
        setConfig(new Config(getConfigName(), getGkeys(), getFormatting(), getRaidChannels(), getPokeChannels(),
                             getSupporterLevels(), getPresets(), getGlobalFilter()));
    }

    private void loadGeofences() {
        geofencing = new Geofencing(this);
        geofencing.loadGeofences(geofences);
    }

    private void loadSuburbs() {
        setSuburbs(new SuburbManager(Paths.get(suburbsName), this));
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
        novaBot.getUserJDA("107730875596169216");
    }

    public void parseModMsg(Message msg, TextChannel channel) {
        if (msg.getContentDisplay().startsWith("!joindate")) {

            StringBuilder response = new StringBuilder(String.format("%s, the results of your search are:\n\n", msg.getAuthor().getAsMention()));

            List<User> mentionedUsers = msg.getMentionedUsers();

            for (User mentionedUser : mentionedUsers) {
                OffsetDateTime joinDate = guild.getMember(mentionedUser).getJoinDate();
                String formattedDate = joinDate.toInstant().atZone(getConfig().getTimeZone()).format(getFormatter());

                response.append(String.format("  %s joined at %s", mentionedUser.getAsMention(), formattedDate));
            }

            channel.sendMessage(response.toString()).queue();
        }
    }

    public void parseMsg(final String msg, final User author, final MessageChannel channel) {

        if (!msg.startsWith(getLocalString("Prefix"))) {
            return;
        }
        com.github.novskey.novabot.data.User user = dataManager.getUser(author.getId());

        if (user == null) {
            user = dataManager.addUser(author.getId(), getNextUserBotToken());
        }else{
            if (user.getBotToken() == null){
                dataManager.setBotToken(author.getId(),getNextUserBotToken());
            }
        }

        if (!user.isVerified() && channel.getType() == ChannelType.PRIVATE){
            return;
        }else if (!user.isVerified() && channel.getType() != ChannelType.PRIVATE){
            dataManager.verifyUser(author.getId());
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
                setup();
                channel.sendMessageFormat("%s, %s", author, getLocalString("ReloadMessage")).queue();
            }
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
        
        if (msg.startsWith(getLocalString("GetTokenCommand"))) {
        		char[] alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
            String token = RandomStringUtils.random(
            		128, 
            		0, 
            		alphabet.length, 
            		false,
            		false,
            		alphabet, 
            		new SecureRandom()
        		);
            final String hoursString = msg.substring(msg.indexOf(" ") + 1).trim();
            int hoursN = 8760;
            try {
            		int hours = Integer.parseInt(hoursString);
            		if (hours > 0) {
            			hoursN = hours;
            		}
            } catch (NumberFormatException e) {}
            final int hours = hoursN;
            
            dataManager.saveToken(author.getId(), token, hours);
            if (channel.getType() != ChannelType.PRIVATE) {
                channel.sendMessageFormat("%s, %s", author, getLocalString("SendPrivateMessage")).queue();
            }
            author.openPrivateChannel().queue((privateChannel) ->
            {
            		privateChannel.sendMessageFormat("%s, %s", author, getLocalString("GetTokenMessage").replace("<token>", token).replace("<hours>", String.valueOf(hours))).queue();
            });
            return;
        }

        if (msg.equals(getLocalString("ClearTokensCommand"))) {
            dataManager.clearTokens(author.getId());
            channel.sendMessageFormat("%s, %s", author, getLocalString("ClearTokensMessage")).queue();
            return;
        }

        if (msg.startsWith(getLocalString("JoinRaidCommand")) && getConfig().isRaidOrganisationEnabled()) {
        	
            String[] splited = msg.split("\\s+");
            String groupCode = "";
            	if (splited.length >= 2) {
            		groupCode = splited[1];
            	}
            	int groupSize = 1;
            	if (splited.length >= 3) {
            		try {
            			groupSize = Integer.parseInt(splited[2]);
            		} catch (NumberFormatException e) {}
            	}
            
            RaidLobby lobby = lobbyManager.getLobby(groupCode);

            if (lobby == null) {
                channel.sendMessageFormat("%s %s", author, getLocalString("NoRaidLobbyMessage").replace("<lobbycode>", groupCode)).queue();
                return;
            } else {
                if (lobby.containsUser(author.getId())) {
                    channel.sendMessageFormat("%s %s", author, getLocalString("AlreadyInLobbyMessage")).queue();
                    return;
                }

                lobby.joinLobby(author.getId(), groupSize, null, false);

                String joinMsg = getLocalString("JoinRaidLobbyMessage");
                joinMsg = joinMsg.replaceAll("<channel>", lobby.getChannel().getAsMention());
                joinMsg = joinMsg.replaceAll("<lobbysize>", String.valueOf(lobby.memberCount()));
                channel.sendMessageFormat("%s %s", author ,joinMsg).queue();
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
        } else if (msg.equals(getLocalString("PokemonSettingsCommand")) && getConfig().pokemonEnabled()) {
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
        } else if (msg.equals(getLocalString("RaidSettingsCommand")) && getConfig().raidsEnabled()) {
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
        } else if (getConfig().getPresets().size() > 0 && (msg.equals(getLocalString("PresetSettingsCommand")))) {
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
        } else if (getConfig().getPresets().size() > 0 && (msg.equals(getLocalString("PresetsCommand")) || msg.equals(getLocalString("PresetListCommand")))) {
            MessageBuilder builder = new MessageBuilder();
            builder.appendFormat("%s, %s%s", author, getLocalString("PresetListMessageStart"), getConfig().getPresetsList());

            Queue<Message> messages = builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE);
            for (final Message message : messages) {
                channel.sendMessage(message).queue();
            }
            return;
        } else if (msg.equals(getLocalString("ResetCommand"))) {
            dataManager.resetUser(author.getId());
            channel.sendMessageFormat("%s, %s", author, getLocalString("ResetMessage")).queue();
            return;
        } else if (msg.equals(getLocalString("ResetPokemonCommand")) && getConfig().pokemonEnabled()) {
            dataManager.resetPokemon(author.getId());
            channel.sendMessageFormat("%s, %s", author, getLocalString("ResetPokemonMessage")).queue();
            return;
        } else if (msg.equals(getLocalString("ResetRaidsCommand")) && getConfig().raidsEnabled()) {
            dataManager.resetRaids(author.getId());
            channel.sendMessageFormat("%s, %s", author, getLocalString("ResetRaidsMessage")).queue();
            return;
        } else if (msg.equals(getLocalString("ResetPresetsCommand")) && getConfig().presetsEnabled()) {
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
        } else if (getConfig().useGeofences() && (msg.equals(getLocalString("RegionListCommand")) || msg.equals(getLocalString("RegionsCommand")))) {
            MessageBuilder builder = new MessageBuilder().appendFormat("%s, %s%n%s", author, getLocalString("RegionListMessageStart"), Geofencing.getListMessage());
            builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE).forEach(m -> channel.sendMessage(m).queue());
            return;
        } else if (suburbsEnabled() && (msg.equals(getLocalString("SuburbListCommand")) || msg.equals(getLocalString("SuburbsCommand")))) {
            MessageBuilder builder = new MessageBuilder().appendFormat("%s, %s%n%s", author, getLocalString("SuburbListMessageStart"), getSuburbs().getListMessage());
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

        if (matchingCommand.equals("helpcommand")) {
            if(userCommand.getArgs().length == 1) {
                channel.sendMessageFormat(getLocalString("HelpMessageStart") +
                                          (getConfig().pokemonEnabled() ? getLocalString("HelpMessagePokemonCommands") : "") +
                                          (getConfig().raidsEnabled() ? getLocalString("HelpMessageRaidCommands") : "") +
                                          (getConfig().getPresets().size() > 0 ? getLocalString("HelpMessagePresetCommands") : "") +
                                          getLocalString("HelpMessageOtherCommandsStart") +
                                          (getConfig().statsEnabled() ? getLocalString("HelpMessageStatsCommand") : "") +
                                          (getConfig().isRaidOrganisationEnabled()
                                           && getConfig().getRaidChatGeofences(channel.getLatestMessageId()).size() > 0
                                           || channel.getType() == ChannelType.PRIVATE
                                           ? getLocalString("HelpMessageJoinLobbyCommand")
                                           : "") +
                                          (getConfig().useGeofences() ? getLocalString("HelpMessageRegionCommands") : "") +
                                          (suburbsEnabled() ? getLocalString("HelpMessageSuburbCommands") : "") +
                                          getLocalString("HelpMessageOtherCommands")).queue();
                return;
            }else{
                Argument commandName = userCommand.getArg(CommandName);
                channel.sendMessage(commands.get((String) commandName.getParams()[0]).getHelpMessage()).queue();
            }
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
                if (!getConfig().getRaidBosses().contains(pokemon.getID())) {
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
                    NotificationLimit limit = getConfig().getNotificationLimit(guild.getMember(author));

                    boolean isSupporter = isSupporter(author.getId());

                    if (limit != null && limit.raidLimit != null && dataManager.countRaids(author.getId(), raids, getConfig().countLocationsInLimits())> limit.raidLimit) {
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

                    for (Raid raid : raids) {
                        novabotLog.debug("adding raid " + raid);
                        dataManager.addRaid(author.getId(), raid);
                    }

                    if (raids.length == 0){
                        channel.sendMessageFormat("%s, please include either a raid boss name or an egg level",author).queue();
                        return;
                    }

                    String message2 = String.format("%s, %s %s", author.getAsMention(), getLocalString("YouWillNowBeNotifiedOf"), Raid.getRaidsString(raids));

                    final Argument locationsArg = userCommand.getArg(ArgType.Locations);
                    Location[] locations = {Location.ALL};
                    if (locationsArg != null) {
                        locations = userCommand.getLocations();
                    }
                    message2 += String.format(" %s%s %s", Raid.getGymNameString(raids),getLocalString("In"),Location.listToString(locations));
                    channel.sendMessage(message2).queue();

                    return;
                }
                case "delraidcommand": {
                    for (Raid raid : raids) {
                        novabotLog.debug("deleting raid " + raid);
                        dataManager.deleteRaid(author.getId(), raid);
                    }
                    if (raids.length == 0){
                        channel.sendMessageFormat("%s, please include either a raid boss name or an egg level",author).queue();
                        return;
                    }
                    String message2 = String.format("%s, %s %s", author.getAsMention(), getLocalString("YouWillNoLongerBeNotifiedOf"), Raid.getRaidsString(raids));

                    final Argument locationsArg = userCommand.getArg(ArgType.Locations);
                    Location[] locations = {Location.ALL};
                    if (locationsArg != null) {
                        locations = userCommand.getLocations();
                    }
                    message2 += String.format(" %s%s %s",Raid.getGymNameString(raids),getLocalString("In"),Location.listToString(locations));
                    channel.sendMessage(message2).queue();

                    return;
                }
                case "clearraidcommand": {
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
                    NotificationLimit limit = getConfig().getNotificationLimit(guild.getMember(author));

                    boolean isSupporter = isSupporter(author.getId());

                    if (limit != null && limit.pokemonLimit != null && dataManager.countPokemon(author.getId(), pokemons, getConfig().countLocationsInLimits()) > limit.pokemonLimit) {
                        channel.sendMessageFormat("%s %s %s %s",
                                author,
                                getLocalString("ExceedLimitMessageStart"),
                                limit.pokemonLimit,
                                getLocalString("ExceedPokeLimitMessageEnd"),
                                (limit.pokemonLimit > 0 ? getLocalString("ExceededNonZeroPokeLimitMessage") : "")).queue();
                        return;
                    } else if (limit == null && isSupporter) {
                        novabotLog.error(String.format("LIMIT IS NULL: %s, is supporter", author.getName()));
                    }

                    for (final Pokemon pokemon : pokemons) {
                        novabotLog.debug("adding pokemon " + pokemon);
                        dataManager.addPokemon(author.getId(), pokemon);
                    }
                    String message2 = String.format("%s, %s %s", author.getAsMention(), getLocalString("YouWillNowBeNotifiedOf"), Pokemon.listToString(userCommand.getUniquePokemon()));
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
                    String message2 = String.format("%s %s %s", author.getAsMention(), getLocalString("YouWillNoLongerBeNotifiedOf"), Pokemon.listToString(userCommand.getUniquePokemon()));
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
                                                          author.getAsMention(), getLocalString("YouWillNoLongerBeNotifiedOf"), Pokemon.listToString(pokemons), getLocalString("InAnyLocations"));
                    channel.sendMessage(message2).queue();
                    return;
                }
                case "clearpokelocationcommand": {
                    final Location[] locations2 = userCommand.getLocations();
                    dataManager.clearLocationsPokemon(author.getId(), locations2);
                    final String message2 = String.format("%s %s %s",
                                                          author.getAsMention(), getLocalString("YouWillNoLongerBeNotifiedOfPokemonIn"), Location.listToString(locations2));
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
                    NotificationLimit limit = getConfig().getNotificationLimit(guild.getMember(author));

                    boolean isSupporter = isSupporter(author.getId());

                    Object[] presetsObj = userCommand.getArg(ArgType.Preset).getParams();
                    String[] presets = Arrays.copyOf(presetsObj, presetsObj.length, String[].class);

                    ArrayList<Preset> potentialPresets = new ArrayList<>();

                    for (String preset : presets) {
                        for (Location location : locations) {
                            potentialPresets.add(new Preset(preset,location));
                        }
                    }

                    if (limit != null &&
                        limit.presetLimit != null &&
                        dataManager.countPresets(author.getId(), potentialPresets, getConfig().countLocationsInLimits())
                          > limit.presetLimit) {
                        channel.sendMessageFormat("%s %s %s %s %s",
                                                  author,
                                                  getLocalString("ExceedLimitMessageStart"),
                                                  limit.presetLimit,
                                                  getLocalString("ExceedPresetLimitMessageEnd"),
                                                  (limit.presetLimit > 0 ? getLocalString("ExceededNonZeroPresetLimitMessage") : "")).queue();
                        return;
                    } else if (limit == null && isSupporter) {
                        novabotLog.error(String.format("LIMIT IS NULL: %s, is supporter", author.getName()));
                    }

                    for (Preset potentialPreset : potentialPresets) {
                        dataManager.addPreset(author.getId(),potentialPreset.presetName,potentialPreset.location);
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

    private synchronized String getNextUserBotToken() {
        int min = Integer.MAX_VALUE;
        String minToken = null;

        for (Map.Entry<String, Integer> entry : botTokenUses.entrySet()) {
            if (entry.getValue() < min){
                min = entry.getValue();
                minToken = entry.getKey();
            }
        }

        if (minToken != null) {
            botTokenUses.merge(minToken, 1, Integer::sum);
        }

        if (minToken == null){
            novabotLog.warn("Null nextUserBotToken: " + botTokenUses);
        }

        return minToken;
    }

    private Message getApiQuotasMessage() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Google API Key Usage");
        int geocodingRequests = reverseGeocoder.getRequests();
        builder.addField("Reverse Geocoding",String.format("%s key%s active. %s request%s this session.",
                getConfig().getGeocodingKeys().size(),
                getConfig().getGeocodingKeys().size() == 1 ? "" : "s",
                geocodingRequests,
                geocodingRequests == 1 ? "" : "s"),
                false);
        int timeZoneRequests = timeZones.getRequests();
        builder.addField("Time Zones",String.format("%s key%s active. %s request%s this session.",
                getConfig().getTimeZoneKeys().size(),
                getConfig().getTimeZoneKeys().size() == 1 ? "" : "s",
                timeZoneRequests,
                timeZoneRequests == 1 ? "" : "s"),
                false);
        int mapRequests = Spawn.getRequests();
        builder.addField("Static Maps",String.format("%s key%s active. %s request%s this session.",
                getConfig().getStaticMapKeys().size(),
                getConfig().getStaticMapKeys().size() == 1? "" : "s",
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
    public void parseRaidChatMsg(User author, String msg, TextChannel textChannel) {
        if (!msg.startsWith(getLocalString("Prefix")) || author.isBot()) return;

        if (msg.startsWith(getLocalString("JoinRaidCommand"))) {
            
        		String[] splited = msg.split("\\s+");
    			String groupCode = "";
            	if (splited.length >= 2) {
            		groupCode = splited[1];
            	}
            	int groupSize = 1;
            	if (splited.length >= 3) {
            		try {
            			groupSize = Integer.parseInt(splited[2]);
            		} catch (NumberFormatException e) {}
            	}

            RaidLobby lobby = lobbyManager.getLobby(groupCode);

            if (lobby == null) {
                textChannel.sendMessageFormat("%s " + StringLocalizer.getLocalString("LobbyNoLobby"), author, groupCode).queue();
            } else {
                if (lobby.containsUser(author.getId())) {
                    textChannel.sendMessageFormat("%s " + StringLocalizer.getLocalString("LobbyAllreadyInLoby"), author).queue();
                    return;
                }

                lobby.joinLobby(author.getId(), groupSize, null, false);
		    		String numberString = "";
		    		if (groupSize > 1) {
		    			numberString = " (+" + (groupSize - 1) + ")";
		    		}
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
                    getLocalString("SetTimeCommand") + " HH:MM",
                    getLocalString("TimesCommand"),
                    getLocalString("SetCountCommand") + " X",
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
        
        if (msg.equals(getLocalString("TimesCommand"))) {
            lobby.sendTimes(author);
            return;
        }
        
        if (msg.startsWith(getLocalString("SetCountCommand"))) {
        		String countString = msg.substring(msg.indexOf(" ") + 1).trim();
        		
        		boolean sucess = false;
        		int count;
        		try {
    				count = Integer.parseInt(countString);
    				if (count > 0) {
    					sucess = true;
    				}
    				
    			} catch (NumberFormatException e) {
    				count = 1;
    			}
        	
        		if (sucess) {
        			lobby.getChannel()
    				.sendMessageFormat("%s %s", author,
    						StringLocalizer.getLocalString("CountSet"))
    				.queue();
            		lobby.setCount(author.getId(), count);
        		} else {
        			lobby.getChannel()
					.sendMessageFormat("%s %s %s", author,
							StringLocalizer.getLocalString("ProblemReadingInput"),
							StringLocalizer.getLocalString("WrongCountFormat"))
					.queue();
        		}
            return;
        }

        if (msg.equals(getLocalString("MapCommand"))) {
            textChannel.sendMessage(lobby.spawn.getProperties().get("gmaps")).queue();
            return;
        }
        
        if (msg.startsWith(getLocalString("SetTimeCommand"))) {
        		String time = msg.substring(msg.indexOf(" ") + 1).trim();
             
        		String[] splited = time.split(":");
        		if (splited.length != 2) {
        			lobby.getChannel()
					.sendMessageFormat("%s %s %s", author,
							StringLocalizer.getLocalString("ProblemReadingInput"),
							StringLocalizer.getLocalString("NoTimeSpecified"))
					.queue();
        			return;
        		};
        		
        		int hour;
        		int minute;
        		boolean valid = false;
    			try {
    				hour = Integer.parseInt(splited[0]);
    				minute = Integer.parseInt(splited[1]);
    				
    				if (hour < 0 || hour > 60 || minute < 0 || minute > 60) {
    					throw new Exception();
    				}
    				
    			} catch (Exception e) {
        			lobby.getChannel()
					.sendMessageFormat("%s %s %s", author,
							StringLocalizer.getLocalString("ProblemReadingInput"),
							StringLocalizer.getLocalString("WrongTimeFormat"))
					.queue();
        			return;
    			}
    			
    			int startHour = lobby.spawn.battleStart.withZoneSameInstant(config.getTimeZone()).getHour();
    			int startMinute = lobby.spawn.battleStart.withZoneSameInstant(config.getTimeZone()).getMinute();
    			int endHour = lobby.spawn.raidEnd.withZoneSameInstant(config.getTimeZone()).getHour();
    			int endMinute = lobby.spawn.raidEnd.withZoneSameInstant(config.getTimeZone()).getMinute();

    			if (		hour > endHour || 
    					hour < startHour || 
    					(hour == endHour && minute > endMinute) ||
    					(hour == startHour && minute < startMinute)
    				) {
    				lobby.getChannel()
					.sendMessageFormat("%s %s", author,
							StringLocalizer.getLocalString("TimeOutsideRaid"))
					.queue();
    			} else { 
    				lobby.getChannel()
					.sendMessageFormat("%s %s", author,
							StringLocalizer.getLocalString("TimeSet"))
					.queue();
	    			lobby.setTime(author.getId(), hour, minute);
    			}
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
        StringLocalizer.init(ResourceBundle.getBundle("Messages",new Locale(locale)),
                ResourceBundle.getBundle("TimeUnits",new Locale(locale)));
        TimeUnit.SetBundle(timeUnitsBundle);
        SpawnLocation.novaBot = this;
        Location.all = getLocalString("All");
        if (geofencing == null || !geofencing.loaded) {
            loadGeofences();
        }

        loadConfig();
        loadSuburbs();

        Spawn.setNovaBot(this);

        commands = new Commands(this);
        parser = new Parser(this);

        reverseGeocoder = new ReverseGeocoder(this);

        timeZones = new TimeZones(this);

        dataManager = new DataManager(this, config.getScannerDbs());

        botTokenUses.putAll(dataManager.getTokenUses());

        if (config.isRaidOrganisationEnabled()) {
            lobbyManager = new LobbyManager(this);
            RaidNotificationSender.setNextId(dataManager.highestRaidLobbyId() + 1);
        }

        if (config.isApiEnabled()) {
            ApiManager.setup(getConfig().getApiPort(), this);
        }
    }

    private boolean isAdmin(User author) {
        for (Guild g : jda.getGuilds()) {
            Member member = g.getMember(author);
            if(member == null) continue;
            for (Role role : member.getRoles()) {
                if (role.getId().equals(getConfig().getAdminRole())) return true;
            }
        }
        return false;
    }

    private boolean isSupporter(final String userID) {
        final Member member = guild.getMemberById(userID);

        if (member == null || member.getRoles() == null) return false;

        for (final Role role : member.getRoles()) {
            if (role == null) continue;
            if (getConfig().getSupporterRoles().contains(role.getId())) return true;
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
                    .setToken(getConfig().getToken())
                    .buildBlocking();

            jda.addEventListener(new MessageListener(this,true));

            notificationBots.put(getConfig().getToken(),jda);
            botTokens.add(getConfig().getToken());
            if(!botTokenUses.containsKey(getConfig().getToken())){
                botTokenUses.put(getConfig().getToken(), 0);
            }
            if (getConfig().getNotificationTokens().size() > 0){
                int botNum = 1;
                for (String token : getConfig().getNotificationTokens()) {
                    if (!token.equals(getConfig().getToken())) {
                        novabotLog.info("Logging in notification bot #" + botNum);
                        notificationBots.put(token, (new JDABuilder(AccountType.BOT)
                                .setAutoReconnect(true)
                                .setGame(pokemonGo)
                                .setToken(token)
                                .addEventListener(new MessageListener(this, false))
                                .buildBlocking()));
                        botNum++;
                    }
                    botTokens.add(token);
                    if(!botTokenUses.containsKey(token)){
                        botTokenUses.put(token, 0);
                    }
                }
            }

            ArrayList<String> toRemove = new ArrayList<>();
            for (String botToken : botTokenUses.keySet()) {
                if (!notificationBots.containsKey(botToken)){
                    novabotLog.info(String.format("Bot token %s from the DB no longer exists in config file, queuing for removal", botToken));
                    toRemove.add(botToken);
                }
            }

            botTokens.removeAll(toRemove);
            toRemove.forEach(botTokenUses::remove);
            if(toRemove.size() > 0) {
                dataManager.clearTokens(toRemove);
            }

            novabotLog.info("Loaded notification bots: " + notificationBots);

            for (Guild guild1 : jda.getGuilds()) {
                novabotLog.info("Connected to guild: " + guild1.getName());
                if (config.getMainGuild() == null && guild == null) {
                    guild = guild1;
                }

                loadEmotes(guild1, jda);
            }

            if (config.getMainGuild() != null){
                guild = jda.getGuildById(config.getMainGuild());
            }

            if (guild == null){
                novabotLog.error("No main guild was set, make sure your bot is connected to at least one discord server.");
                System.exit(0);
            }

            novabotLog.info(String.format("Set %s as main guild", guild.getName()));

            if (getConfig().getCommandChannelId() != null) {
                TextChannel channel = guild.getTextChannelById(getConfig().getCommandChannelId());
                if (channel != null) {
                    if (getConfig().showStartupMessage()) {
                        channel.sendMessage(getLocalString("StartUpMessage")).queue();
                    }
                } else {
                    novabotLog.info(String.format("couldn't find command channel by id from config: %s", getConfig().getCommandChannelId()));
                }
            }

            guild.getMember(jda.getSelfUser()).getRoles().forEach(System.out::println);

            guild.getInvites().queue(success -> invites.addAll(success));

            if (getConfig().loggingEnabled()) {
                roleLog = jda.getTextChannelById(getConfig().getRoleLogId());
                userUpdatesLog = jda.getTextChannelById(getConfig().getUserUpdatesId());
            }

            if (getConfig().useScanDb()) {
                notificationsManager = new NotificationsManager(this, testing);
                notificationsManager.start();
            }

        } catch (LoginException | InterruptedException ex2) {
            novabotLog.error("Error starting bot",ex2);
        }


        novabotLog.info("connected");
    }

    public boolean suburbsEnabled() {
        return this.getSuburbs().notEmpty();
    }

    private void setLocale(String locale) {
        this.locale = locale;
    }


    private void loadEmotes(Guild guild, JDA jda) {
        for (String type : Types.TYPES) {
            List<Emote> found = jda.getEmotesByName(type, true);
            String path = null;
            if (found.size() == 0) try {
                path = "static/icons/" + type + ".png";

                guild.getController().createEmote(type, Icon.from(Paths.get(path).toFile())).queue(emote ->
                        Types.emotes.put(type, emote));
            } catch (IOException | IllegalArgumentException e) {
                novabotLog.warn(String.format("Couldn't find emote file: %s, ignoring.", path));
            }
            else {
                Types.emotes.put(type, found.get(0));
            }
        }
        novabotLog.info(String.format("Finished loading type emojis: %s", Types.emotes.toString()));

        for (Team team : Team.values()) {
            List<Emote> found = jda.getEmotesByName(team.getStringForEmote(), true);
            String path = null;
            if (found.size() == 0) try {
                path = "static/icons/" + team.getStringForEmote().toLowerCase() + ".png";
                guild.getController().createEmote(team.getStringForEmote().toLowerCase(), Icon.from(Paths.get(path).toFile())).queue(emote ->
                        Team.emotes.put(team, emote));
            } catch (IOException | IllegalArgumentException e) {
                novabotLog.warn(String.format("Couldn't find emote file: %s, ignoring.", path));
            }
            else {
                Team.emotes.put(team, found.get(0));
            }
        }

        novabotLog.info(String.format("Finished loading team emojis: %s", Team.emotes.toString()));

        for (Weather weather : Weather.values()) {
            if (weather == None) continue;
            List<Emote> found = jda.getEmotesByName(weather.toEmoteName(), true);
            String path = null;
            if(found.size() == 0) try {
                path = "static/icons/" + weather.toEmoteName().toLowerCase() + ".png";
                guild.getController().createEmote(weather.toEmoteName().toLowerCase(), Icon.from(Paths.get(path).toFile())).queue(emote -> Weather.emotes.put(weather,emote));
            } catch (IOException | IllegalArgumentException e) {
                novabotLog.warn(String.format("Couldn't find emote file: %s, ignoring.", path));
            } else {
                Weather.emotes.put(weather, found.get(0));
            }
        }

        novabotLog.info(String.format("Finished loading weather emojis: %s", Weather.emotes.toString()));

    }

    public synchronized JDA getNextNotificationBot() {
        if (lastNotificationBot == botTokens.size() - 1) {
            lastNotificationBot = 0;
            return notificationBots.get(botTokens.get(lastNotificationBot));
        }
        ++lastNotificationBot;
        return notificationBots.get(botTokens.get(lastNotificationBot));
    }

    public DateTimeFormatter getFormatter() {
        return formatter;
    }

    private String getConfigName() {
        return configName;
    }

    public String getSupporterLevels() {
        return supporterLevels;
    }

    public String getGkeys() {
        return gkeys;
    }

    public String getFormatting() {
        return formatting;
    }

    public String getRaidChannels() {
        return raidChannels;
    }

    public String getPokeChannels() {
        return pokeChannels;
    }

    public String getPresets() {
        return presets;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public SuburbManager getSuburbs() {
        return suburbs;
    }

    public void setSuburbs(SuburbManager suburbs) {
        this.suburbs = suburbs;
    }

    public void parseReactionAdd(boolean mainBot, MessageReactionAddEvent event) {
        User author = event.getUser();
        MessageChannel channel = event.getChannel();
        com.github.novskey.novabot.data.User user = dataManager.getUser(author.getId());

        if (user == null) {
            user = dataManager.addUser(author.getId(), getNextUserBotToken());
        }else{
            if (user.getBotToken() == null){
                dataManager.setBotToken(author.getId(),getNextUserBotToken());
            }
        }

        if (!user.isVerified() && channel.getType() == ChannelType.PRIVATE){
            return;
        }else if (!user.isVerified() && channel.getType() != ChannelType.PRIVATE){
            dataManager.verifyUser(author.getId());
        }

        if (!mainBot || !getConfig().isRaidOrganisationEnabled()) return;

        if (event.getUser().isBot()) return;

        Message message = event.getChannel().getMessageById(event.getMessageId()).complete();

        String content;
        if (message.getEmbeds().size() > 0) {
            content = message.getEmbeds().get(0).getDescription();
        } else {
            content = message.getContentDisplay();
        }

        if (!message.getAuthor().isBot()) return;

        if (content.contains(StringLocalizer.getLocalString("SetTimeCommand"))) {
            novabotLog.debug("Set time reaction added to a bot message that contains an embed!");

            String reaction = event.getReactionEmote().getName();

            int commandIndex = content.indexOf(reaction) + reaction.length() + 1;
            String time = content.substring(commandIndex, commandIndex + 5).trim();
            String channeldID = message.getChannel().getId();
            String userId = event.getUser().getId();

            novabotLog.info("Message clicked was for time " + time + " in channeldId " + channeldID);

            message.getChannel()
                    .sendMessageFormat("%s %s", event.getUser(),
                            StringLocalizer.getLocalString("TimeSet"))
                    .queue();
            RaidLobby lobby = lobbyManager.getLobbyByChannelId(channeldID);
            lobby.setTime(userId, time);

        } else if (content.contains(StringLocalizer.getLocalString("JoinRaidCommand"))) {

            int groupCount = 1;
            if (event.getReactionEmote().getName().equals(NUMBER_1)) {
                groupCount = 1;
            } else if (event.getReactionEmote().getName().equals(NUMBER_2)) {
                groupCount = 2;
            } else if (event.getReactionEmote().getName().equals(NUMBER_3)) {
                groupCount = 3;
            } else if (event.getReactionEmote().getName().equals(NUMBER_4)) {
                groupCount = 4;
            } else if (event.getReactionEmote().getName().equals(NUMBER_5)) {
                groupCount = 5;
            } else {
                return;
            }

            novabotLog.debug("Join reaction added to a bot message that contains an embed!");

            int    joinIndex = content.indexOf(StringLocalizer.getLocalString("JoinRaidCommand")) + 10;
            String lobbyCode = content.substring(joinIndex, content.substring(joinIndex).indexOf(" ") + joinIndex).trim();

            novabotLog.info("Message clicked was for lobbycode " + lobbyCode + " with group size of " + groupCount);

            RaidLobby lobby = lobbyManager.getLobby(lobbyCode);

            if (lobby == null) {
                if (event.getChannelType() == ChannelType.PRIVATE) {
                    event.getChannel().sendMessageFormat("%s " + StringLocalizer.getLocalString("LobbyEnded"), event.getUser());
                }
                return;
            }

            if (!lobby.containsUser(event.getUser().getId())) {

                lobby.joinLobby(event.getUser().getId(), groupCount, null, false);

                if (event.getChannelType() == ChannelType.PRIVATE) {
                    event.getChannel().sendMessageFormat("%s " + StringLocalizer.getLocalString("LobbyPlaced"), event.getUser(), lobby.getChannel(), lobby.memberCount()).queue();
                }

                String numberString = "";
                if (groupCount > 1) {
                    numberString = " (+" + (groupCount - 1) + ")";
                }
            } else if (event.getChannelType() == ChannelType.PRIVATE) {
                event.getChannel().sendMessageFormat("%s " + StringLocalizer.getLocalString("LobbyAllreadyInLoby"), event.getUser()).queue();
            }
        }
    }
}
