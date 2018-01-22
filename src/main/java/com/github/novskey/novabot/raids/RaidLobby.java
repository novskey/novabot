package com.github.novskey.novabot.raids;

import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.core.ScheduledExecutor;
import com.github.novskey.novabot.core.Types;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Owner on 2/07/2017.
 */
public class RaidLobby {

    private static final Logger raidLobbyLog = LoggerFactory.getLogger("raid-lobbies");
    String roleId = null;
    String channelId = null;

    public final String lobbyCode;

    public RaidSpawn spawn;

    private final HashSet<String> memberIds = new HashSet<>();
    private NovaBot novaBot;

    ScheduledExecutor shutDownService = null;

    public long nextTimeLeftUpdate = 15;
    public String inviteCode;
    private boolean delete = false;
    private boolean created = false;

    public RaidLobby(RaidSpawn raidSpawn, String lobbyCode, NovaBot novaBot, boolean restored) {
        this.spawn = raidSpawn;
        this.lobbyCode = lobbyCode;
        this.novaBot = novaBot;

        long timeLeft = Duration.between(ZonedDateTime.now(UtilityFunctions.UTC), spawn.raidEnd).toMillis();

        double minutes = timeLeft / 1000 / 60;

        while(minutes <= nextTimeLeftUpdate){
            nextTimeLeftUpdate -= 5;
        }

        if(!restored) {
            novaBot.dataManager.newLobby(lobbyCode, spawn.gymId, memberCount(), channelId, roleId, nextTimeLeftUpdate, inviteCode);
        }
    }

    public RaidLobby(RaidSpawn spawn, String lobbyCode, NovaBot novaBot, String channelId, String roleId, String inviteCode, boolean restored) {
        this(spawn, lobbyCode, novaBot, restored);
        this.channelId = channelId;
        this.roleId = roleId;
        this.inviteCode = inviteCode;

        long timeLeft = Duration.between(ZonedDateTime.now(UtilityFunctions.UTC), spawn.raidEnd).toMillis();

        if(channelId != null && roleId != null){
            created = true;
        }else{
            return;
        }

        if(nextTimeLeftUpdate == 15){
            getChannel().sendMessageFormat("%s %s %s %s!",
                    getRole(),
                    novaBot.getLocalString("RaidEndSoonMessage"),
                    15,
                    novaBot.getLocalString("Minutes")).queueAfter(
                    timeLeft - (15*60*1000),TimeUnit.MILLISECONDS);
        }

        getChannel().sendMessageFormat("%s, %s %s %s",
                getRole(),
                novaBot.getLocalString("RaidEndedMessage"),
                15,
                novaBot.getLocalString("Minutes")).
                queueAfter(timeLeft,TimeUnit.MILLISECONDS,success -> end(15));


    }

    public void alertRaidNearlyOver() {
        getChannel().sendMessageFormat("%s %s %s!",
                getRole(),
                novaBot.getLocalString("RaidEndSoonMessage"),
                spawn.timeLeft(spawn.raidEnd)).queue();
        novaBot.dataManager.updateLobby(lobbyCode, memberCount(), (int) nextTimeLeftUpdate, inviteCode);
    }

    public void createInvite() {
        Channel channel = getChannel();

        if (channel != null) {
            channel.createInvite().queue(invite -> {
                inviteCode = invite.getCode();
                novaBot.invites.add(invite);
                novaBot.dataManager.updateLobby(lobbyCode, memberCount(), (int) nextTimeLeftUpdate, inviteCode);
            });
        }
    }

    public void end(int delay) {
        if(channelId == null || roleId == null) return;

        delete = true;

        Runnable shutDownTask = () -> {
            if(delete) {
                getChannel().delete().queue();
                getRole().delete().queue();
                raidLobbyLog.info(String.format("Ended raid lobby %s", lobbyCode));
                novaBot.lobbyManager.activeLobbies.remove(lobbyCode);
                novaBot.dataManager.endLobby(lobbyCode);
            }
        };

        shutDownService = new ScheduledExecutor(1);

        shutDownService.schedule(shutDownTask, delay, TimeUnit.MINUTES);
        shutDownService.shutdown();
    }

    public int memberCount() {
        return memberIds.size();
    }

    public Message getBossInfoMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle(String.format("%s - %s %s %s",
                spawn.getProperties().get("pkmn"),
                UtilityFunctions.capitaliseFirst(novaBot.getLocalString("Level")),
                spawn.getProperties().get("level"),
                novaBot.getLocalString("Raid")));
        embedBuilder.addField(WordUtils.capitalizeFully(novaBot.getLocalString("GymOwners")), String.format("%s %s", spawn.getProperties().get("team_name"), spawn.getProperties().get("team_icon")), false);
        embedBuilder.addField(novaBot.getLocalString("CP"), spawn.getProperties().get("cp"), false);
        embedBuilder.addField(WordUtils.capitalizeFully(novaBot.getLocalString("MoveSet")), String.format("%s%s - %s%s", spawn.getProperties().get("quick_move"), spawn.getProperties().get("quick_move_type_icon"), spawn.getProperties().get("charge_move"), spawn.getProperties().get("charge_move_type_icon")), false);
        embedBuilder.addField(WordUtils.capitalizeFully(novaBot.getLocalString("MaxCatchableCp")), spawn.getProperties().get("lvl20cp"), false);
        embedBuilder.addField(WordUtils.capitalizeFully(novaBot.getLocalString("MaxCatchableCpWithBonus")), spawn.getProperties().get("lvl25cp"), false);
        StringBuilder weaknessEmoteStr = new StringBuilder();

        for (String s : Raid.getBossWeaknessEmotes(spawn.bossId)) {
            Emote emote = Types.emotes.get(s);
            weaknessEmoteStr.append(emote == null ? "" : emote.getAsMention());
        }

        embedBuilder.addField(WordUtils.capitalizeFully(novaBot.getLocalString("WeakTo")), weaknessEmoteStr.toString(), true);

        StringBuilder strengthEmoteStr = new StringBuilder();

        for (String s : Raid.getBossStrengthsEmote(spawn.getMove_1(), spawn.getMove_2())) {
            Emote emote = Types.emotes.get(s);
            strengthEmoteStr.append(emote == null ? "" : emote.getAsMention());
        }

        embedBuilder.addField(WordUtils.capitalizeFully(novaBot.getLocalString("StrongAgainst")), strengthEmoteStr.toString(), true);

        embedBuilder.setThumbnail(spawn.getIcon());

        MessageBuilder messageBuilder = new MessageBuilder().setEmbed(embedBuilder.build());

        return messageBuilder.build();
    }

    public TextChannel getChannel() {
        return novaBot.jda.getTextChannelById(channelId);
    }

    public String getMaxCpMessage() {
        return String.format("%s %s, %s %s %s.",
                novaBot.getLocalString("MaxCpMessageStart"),
                spawn.getProperties().get("lvl20cp"),
                novaBot.getLocalString("And"),
                spawn.getProperties().get("lvl25cp"),
                novaBot.getLocalString("MaxCpMessageEnd"));
    }

    public Message getStatusMessage(){
        EmbedBuilder embedBuilder = new EmbedBuilder();

        if(spawn.bossId != 0) {
            embedBuilder.setTitle(String.format("Raid status for %s (lvl %s) in %s - Lobby %s",
                    spawn.getProperties().get("pkmn"),
                    spawn.getProperties().get("level"),
                    spawn.getProperties().get("city"),
                    lobbyCode),
                    spawn.getProperties().get("gmaps"));

            embedBuilder.setDescription("Type `!status` to see this message again, and `!help` to see all available raid lobby commands.");

            embedBuilder.addField("Lobby Members", String.valueOf(memberCount()), false);
            embedBuilder.addField("Address", String.format("%s %s, %s",
                    spawn.getProperties().get("street_num"),
                    spawn.getProperties().get("street"),
                    spawn.getProperties().get("city")
            ), false);
            embedBuilder.addField("Gym Name", spawn.getProperties().get("gym_name"), false);
            embedBuilder.addField("Gym Owners", String.format("%s %s",
                    spawn.getProperties().get("team_name"),
                    spawn.getProperties().get("team_icon")
            ), false);
            embedBuilder.addField("Raid End Time", String.format("%s (%s)",
                    spawn.getProperties().get("24h_end"),
                    spawn.timeLeft(spawn.raidEnd)),
                    false);
            embedBuilder.addField("Boss Moveset", String.format("%s%s - %s%s", spawn.getProperties().get("quick_move"), spawn.getProperties().get("quick_move_type_icon"), spawn.getProperties().get("charge_move"), spawn.getProperties().get("charge_move_type_icon")), false);
            embedBuilder.addField("Max catchable CP", spawn.getProperties().get("lvl20cp"), false);
            embedBuilder.addField("Max catchable CP (with weather bonus)", spawn.getProperties().get("lvl25cp"), false);

            StringBuilder weaknessEmoteStr = new StringBuilder();

            for (String s : Raid.getBossWeaknessEmotes(spawn.bossId)) {
                Emote emote = Types.emotes.get(s);
                if(emote != null) {
                    weaknessEmoteStr.append(emote.getAsMention());
                }
            }

            embedBuilder.addField("Weak To", weaknessEmoteStr.toString(), true);

            StringBuilder strengthEmoteStr = new StringBuilder();

            for (String s : Raid.getBossStrengthsEmote(spawn.getMove_1(), spawn.getMove_2())) {
                Emote emote = Types.emotes.get(s);
                strengthEmoteStr.append(emote == null ? "" : emote.getAsMention());
            }

            embedBuilder.addField("Strong Against", strengthEmoteStr.toString(), true);

            embedBuilder.setThumbnail(spawn.getIcon());
            embedBuilder.setImage(spawn.getImage("formatting.ini"));
        }else{
            embedBuilder.setTitle(String.format("Raid status for level %s egg in %s - Lobby %s",
                    spawn.getProperties().get("level"),
                    spawn.getProperties().get("city"),
                    lobbyCode));

            embedBuilder.setDescription("Type `!status` to see this message again, and `!help` to see all available raid lobby commands.");

            embedBuilder.addField("Lobby Members", String.valueOf(memberCount()), false);
            embedBuilder.addField("Address", String.format("%s %s, %s",
                    spawn.getProperties().get("street_num"),
                    spawn.getProperties().get("street"),
                    spawn.getProperties().get("city")
            ), false);
            embedBuilder.addField("Gym Name", spawn.getProperties().get("gym_name"), false);
            embedBuilder.addField("Gym Owners", String.format("%s %s", spawn.getProperties().get("team_name"), spawn.getProperties().get("team_icon")), false);
            embedBuilder.addField("Raid Start Time", String.format("%s (%s)",
                    spawn.getProperties().get("24h_start"),
                    spawn.timeLeft(spawn.battleStart)),
                    false);

            embedBuilder.setThumbnail(spawn.getIcon());
            embedBuilder.setImage(spawn.getImage("formatting.ini"));
        }

        MessageBuilder messageBuilder = new MessageBuilder().setEmbed(embedBuilder.build());

        return messageBuilder.build();
    }

    public String getTeamMessage() {
        StringBuilder str = new StringBuilder(String.format("There are %s users in this raid team:\n\n", memberCount()));

        for (String memberId : memberIds) {
            str.append(String.format("  %s%n", novaBot.guild.getMemberById(memberId).getEffectiveName()));
        }

        return str.toString();
    }

    public void joinLobby(String userId) {
        if (spawn.raidEnd.isBefore(ZonedDateTime.now(UtilityFunctions.UTC))) return;

        memberIds.add(userId);

        if (delete) delete = false;

        TextChannel channel = null;

        if (!created && shutDownService == null) {
            roleId = novaBot.guild.getController().createRole().complete().getId();

            Role role = novaBot.jda.getRoleById(roleId);


            String channelName = spawn.getProperties().get("gym_name").replace(" ", "-").replaceAll("[^\\w-]", "");

            channelName = channelName.substring(0, Math.min(25, channelName.length()));

            role.getManagerUpdatable()
                .getNameField().setValue(String.format("raid-%s", channelName))
                .getMentionableField().setValue(true)
                .update()
                .queue();

            if (novaBot.config.getRaidLobbyCategory() == null) {
                channelId = novaBot.guild.getController().createTextChannel(String.format("raid-%s", channelName)).complete().getId();
            }else{
                channelId = novaBot.jda.getCategoryById(novaBot.config.getRaidLobbyCategory()).createTextChannel(String.format("raid-%s",channelName)).complete().getId();
            }

            channel = novaBot.jda.getTextChannelById(channelId);

            if(channel.getPermissionOverride(novaBot.guild.getPublicRole()) == null){
                channel.createPermissionOverride(novaBot.guild.getPublicRole()).setDeny(Permission.MESSAGE_READ).complete();
            }else{
                channel.getPermissionOverride(novaBot.guild.getPublicRole()).getManagerUpdatable().deny(Permission.MESSAGE_READ).update().complete();
            }

            channel.createPermissionOverride(role).setAllow(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY).complete();

            if(channel.getPermissionOverride(novaBot.guild.getRoleById(novaBot.config.novabotRole())) == null){
                channel.createPermissionOverride(novaBot.guild.getRoleById(novaBot.config.novabotRole())).setAllow(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.CREATE_INSTANT_INVITE).complete();
            }else{
                channel.getPermissionOverride(novaBot.guild.getRoleById(novaBot.config.novabotRole())).getManagerUpdatable().grant(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.CREATE_INSTANT_INVITE).update().complete();
            }

            channel.createInvite().queue(inv -> {
                inviteCode = inv.getCode();
                novaBot.invites.add(inv);
            });

            long timeLeft = Duration.between(ZonedDateTime.now(UtilityFunctions.UTC),spawn.raidEnd).toMillis();

            raidLobbyLog.info(String.format("First join for lobbyCode %s, created channel.", lobbyCode));

            if (nextTimeLeftUpdate == 15) {
                getChannel().sendMessageFormat("%s the raid is going to end in %s minutes!", getRole(), 15).queueAfter(
                        timeLeft - (15 * 60 * 1000), TimeUnit.MILLISECONDS);
            }

            getChannel().sendMessageFormat("%s, the raid has ended and the raid lobby will be closed in %s minutes", getRole(), 15).
                    queueAfter(timeLeft, TimeUnit.MILLISECONDS, success -> end(15));

            channel.sendMessage(getStatusMessage()).queue();
            created = true;
        }

        Member member = novaBot.guild.getMemberById(userId);

        novaBot.guild.getController().addRolesToMember(member, novaBot.guild.getRoleById(roleId)).queue();


        if (channel == null) {
            channel = novaBot.guild.getTextChannelById(channelId);
        }

        if (shutDownService != null) {
            raidLobbyLog.info("Cancelling lobby shutdown");
            shutDownService.shutdown();
            shutDownService = null;
        }

        channel.sendMessageFormat("Welcome %s to the raid lobby!\nThere are now %s users in the lobby.", member, memberIds.size()).queue();

        novaBot.dataManager.updateLobby(lobbyCode, memberCount(), (int) nextTimeLeftUpdate, inviteCode);
    }

    public boolean containsUser(String id) {
        return memberIds.contains(id);
    }

    public void alertEggHatched() {
        if(channelId != null) {
            getChannel().sendMessageFormat("%s the raid egg has hatched into a %s!", getRole(), spawn.getProperties().get("pkmn")).queue();
            getChannel().sendMessage(getStatusMessage()).queue();
        }
    }

    public void leaveLobby(String id) {
        novaBot.guild.getController().removeRolesFromMember(novaBot.guild.getMemberById(id), getRole()).queue();
        memberIds.remove(id);
        getChannel().sendMessageFormat("%s left the lobby, there are now %s users in the lobby.", novaBot.guild.getMemberById(id), memberCount()).queue();

        novaBot.dataManager.updateLobby(lobbyCode, memberCount(), (int) nextTimeLeftUpdate, inviteCode);

        if (memberCount() == 0) {
            getChannel().sendMessageFormat("There are no users in the lobby, it will be closed in %s minutes", 10).queue();
            end(10);
        }
    }

    public Message getInfoMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        if(spawn.bossId != 0) {
            String timeLeft = spawn.timeLeft(spawn.raidEnd);

            embedBuilder.setTitle(String.format("[%s] %s (%s)- Lobby %s"
                    , spawn.getProperties().get("city")
                    , spawn.getProperties().get("pkmn")
                    , timeLeft
                    , lobbyCode), spawn.getProperties().get("gmaps"));
            embedBuilder.setDescription(String.format("Join the discord lobby to coordinate with other players by clicking the ✅ emoji below this post, or by typing `!joinraid %s` in any raid channel or PM with novabot.",lobbyCode));
            embedBuilder.addField("Team Size", String.valueOf(memberCount()), false);
            embedBuilder.addField("Gym Name", spawn.getProperties().get("gym_name"), false);
            embedBuilder.addField("Gym Owners", String.format("%s %s", spawn.getProperties().get("team_name"), spawn.getProperties().get("team_icon")), false);
            embedBuilder.addField("Raid End",String.format("%s (%s remaining)"
                    , spawn.getProperties().get("24h_end")
                    ,timeLeft
                    ),false);

            embedBuilder.setThumbnail(spawn.getIcon());
        }else{
            String timeLeft = spawn.timeLeft(spawn.battleStart);

            embedBuilder.setTitle(String.format("[%s] Lvl %s Egg (Hatches in %s) - Lobby %s"
                    , spawn.getProperties().get("city")
                    , spawn.getProperties().get("level")
                    , timeLeft
                    , lobbyCode), spawn.getProperties().get("gmaps"));
            embedBuilder.setDescription(String.format("Join the discord lobby to coordinate with other players by clicking the ✅ emoji below this post, or by typing `!joinraid %s` in any raid channel or PM with novabot.",lobbyCode));
            embedBuilder.addField("Team Size", String.valueOf(memberCount()), false);
            embedBuilder.addField("Gym Name", spawn.getProperties().get("gym_name"), false);
            embedBuilder.addField("Gym Owners", String.format("%s %s", spawn.getProperties().get("team_name"), spawn.getProperties().get("team_icon")), false);
            embedBuilder.addField("Raid Start",String.format("%s (%s remaining)"
                    , spawn.getProperties().get("24h_start")
                    ,timeLeft
            ),false);

            embedBuilder.setThumbnail(spawn.getIcon());
        }
        MessageBuilder messageBuilder = new MessageBuilder().setEmbed(embedBuilder.build());

        return messageBuilder.build();
    }

    public void loadMembers() {
        try {
            Role role = novaBot.guild.getRoleById(roleId);
            memberIds.addAll(novaBot.guild.getMembersWithRoles(role).stream().map(member -> member.getUser().getId()).collect(Collectors.toList()));
        }catch (NullPointerException e){
            raidLobbyLog.warn("Couldn't load members, couldnt find role by Id");
        }
    }

    private Role getRole() {
        return novaBot.jda.getRoleById(roleId);
    }
}
