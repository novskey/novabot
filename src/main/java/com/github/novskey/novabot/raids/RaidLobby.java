package com.github.novskey.novabot.raids;

import com.github.novskey.novabot.Util.StringLocalizer;
import com.github.novskey.novabot.Util.UtilityFunctions;
import com.github.novskey.novabot.core.AlertChannel;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.core.ScheduledExecutor;
import com.github.novskey.novabot.core.Types;
import com.github.novskey.novabot.data.SettingsDBManager;
import com.github.novskey.novabot.maps.TimeZones;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Owner on 2/07/2017.
 */
public class RaidLobby {

	private static final Logger raidLobbyLog = LoggerFactory.getLogger("raid-lobbies");
	String roleId = null;
	String channelId = null;
	public String[] lobbyChatIds = null;

	public final String lobbyCode;

	public RaidSpawn spawn;

	public final HashSet<RaidLobbyMember> members = new HashSet<RaidLobbyMember>();
	private NovaBot novaBot;

	ScheduledExecutor shutDownService = null;
	ScheduledExecutor stopService = null;

	public long nextTimeLeftUpdate = 15;
	public String inviteCode;
	private boolean delete = false;
	private boolean created = false;
	private boolean stopped = false;

	public RaidLobby(RaidSpawn raidSpawn, String lobbyCode, String[] lobbyChatIds, NovaBot novaBot, boolean restored) {
		this.spawn = raidSpawn;
		this.spawn.setLobbyCode(lobbyCode);
		this.lobbyCode = lobbyCode;
		this.novaBot = novaBot;
		this.lobbyChatIds = lobbyChatIds;

		long timeLeft;
		if (spawn != null) {
			timeLeft = Duration.between(ZonedDateTime.now(UtilityFunctions.UTC), spawn.raidEnd).toMillis();
		} else {
			timeLeft = 0;
		}
		double minutes = timeLeft / 1000 / 60;

		while (minutes <= nextTimeLeftUpdate) {
			nextTimeLeftUpdate -= 5;
		}

		if (!restored) {
			novaBot.dataManager.newLobby(lobbyCode, spawn.gymId, channelId, roleId, nextTimeLeftUpdate, inviteCode, members, lobbyChatIds);
		}
		stop((int) timeLeft / 1000);
	}

	public RaidLobby(RaidSpawn spawn, String lobbyCode, NovaBot novaBot, String channelId, String roleId, String inviteCode, String[] lobbyChatIds, boolean restored) {
		this(spawn, lobbyCode, lobbyChatIds, novaBot, restored);
		this.channelId = channelId;
		this.roleId = roleId;
		this.inviteCode = inviteCode;

		long timeLeft;
		if (spawn != null) {
			timeLeft = Duration.between(ZonedDateTime.now(UtilityFunctions.UTC), spawn.raidEnd).toMillis();
		} else {
			end(0);
			return;
		}
		
		if (channelId != null && roleId != null) {
			created = true;
            if (nextTimeLeftUpdate == 15) {
                getChannel()
                        .sendMessageFormat("%s %s %s %s!", getRole(), StringLocalizer.getLocalString("RaidEndSoonMessage"),
                                15, StringLocalizer.getLocalString("Minutes"))
                        .queueAfter(timeLeft - (15 * 60 * 1000), TimeUnit.MILLISECONDS);
            }

            getChannel()
                    .sendMessageFormat("%s, %s %s %s", getRole(), StringLocalizer.getLocalString("RaidHasEndedMessage"), 15,
                            StringLocalizer.getLocalString("Minutes"))
                    .queueAfter(timeLeft, TimeUnit.MILLISECONDS);
		}

	}

	public void alertRaidNearlyOver() {
		getChannel().sendMessageFormat("%s %s %s!", getRole(), StringLocalizer.getLocalString("RaidEndSoonMessage"),
				spawn.timeLeft(spawn.raidEnd)).queue();
		novaBot.dataManager.updateLobby(lobbyCode, (int) nextTimeLeftUpdate, inviteCode, roleId, channelId, members, spawn.gymId, lobbyChatIds);
	}

	public void createInvite() {
		if (channelId != null) {
			Channel channel = getChannel();
	
			if (channel != null) {
				channel.createInvite().queue(invite -> {
					inviteCode = invite.getCode();
					novaBot.invites.add(invite);
					novaBot.dataManager.updateLobby(lobbyCode, (int) nextTimeLeftUpdate, inviteCode, roleId, channelId, members, spawn.gymId, lobbyChatIds);
				});
			}
		}
	}

	public void stop(int delay) {

		Runnable stopTask = () -> {
			stopped = true;
			end(15);
			updateLobbyChat();
		};

		stopService = new ScheduledExecutor(1);
		stopService.schedule(stopTask, delay, TimeUnit.SECONDS);
		stopService.shutdown();
	}

	public void end(int delay) {

		delete = true;

		Runnable shutDownTask = () -> {
			if (delete) {
				if (channelId != null && roleId != null) {
					getChannel().delete().queue();
					getRole().delete().queue();
				}
				raidLobbyLog.info(String.format("Ended raid lobby %s", lobbyCode));
				if (spawn == null) {
					novaBot.dataManager.endLobby(lobbyCode, null);
				} else {
					novaBot.dataManager.endLobby(lobbyCode, spawn.gymId);
				}
			}
		};

		shutDownService = new ScheduledExecutor(1);

		shutDownService.schedule(shutDownTask, delay, TimeUnit.MINUTES);
		shutDownService.shutdown();
	}

	public int memberCount() {
		int count = 0;
		for (RaidLobbyMember member : members) {
			count += member.count;
		}
		return count;
	}

	public Message getBossInfoMessage() {
		EmbedBuilder embedBuilder = new EmbedBuilder();

		embedBuilder.setTitle(String.format("%s - %s %s %s", spawn.getProperties().get("pkmn"),
				UtilityFunctions.capitaliseFirst(StringLocalizer.getLocalString("Level")),
				spawn.getProperties().get("level"), StringLocalizer.getLocalString("Raid")));
		embedBuilder.addField(WordUtils.capitalizeFully(StringLocalizer.getLocalString("GymOwners")),
				String.format("%s %s", spawn.getProperties().get("team_name"), spawn.getProperties().get("team_icon")),
				false);
		embedBuilder.addField(StringLocalizer.getLocalString("CP"), spawn.getProperties().get("cp"), false);
		embedBuilder.addField(WordUtils.capitalizeFully(StringLocalizer.getLocalString("MoveSet")),
				String.format("%s%s - %s%s", spawn.getProperties().get("quick_move"),
						spawn.getProperties().get("quick_move_type_icon"), spawn.getProperties().get("charge_move"),
						spawn.getProperties().get("charge_move_type_icon")),
				false);
		embedBuilder.addField(WordUtils.capitalizeFully(StringLocalizer.getLocalString("MaxCatchableCp")),
				spawn.getProperties().get("lvl20cp"), false);
		embedBuilder.addField(WordUtils.capitalizeFully(StringLocalizer.getLocalString("MaxCatchableCpWithBonus")),
				spawn.getProperties().get("lvl25cp"), false);
		StringBuilder weaknessEmoteStr = new StringBuilder();

		for (String s : Raid.getBossWeaknessEmotes(spawn.bossId)) {
			Emote emote = Types.emotes.get(s);
			weaknessEmoteStr.append(emote == null ? "" : emote.getAsMention());
		}

		embedBuilder.addField(WordUtils.capitalizeFully(StringLocalizer.getLocalString("WeakTo")),
				weaknessEmoteStr.toString(), true);

		StringBuilder strengthEmoteStr = new StringBuilder();

		for (String s : Raid.getBossStrengthsEmote(spawn.getMove_1(), spawn.getMove_2())) {
			Emote emote = Types.emotes.get(s);
			strengthEmoteStr.append(emote == null ? "" : emote.getAsMention());
		}

		embedBuilder.addField(WordUtils.capitalizeFully(StringLocalizer.getLocalString("StrongAgainst")),
				strengthEmoteStr.toString(), true);

		embedBuilder.setThumbnail(spawn.getIcon());

		MessageBuilder messageBuilder = new MessageBuilder().setEmbed(embedBuilder.build());

		return messageBuilder.build();
	}

	public TextChannel getChannel() {
		return novaBot.jda.getTextChannelById(channelId);
	}

	public String getMaxCpMessage() {
		return String.format("%s %s, %s %s %s.", StringLocalizer.getLocalString("MaxCpMessageStart"),
				spawn.getProperties().get("lvl20cp"), StringLocalizer.getLocalString("And"),
				spawn.getProperties().get("lvl25cp"), StringLocalizer.getLocalString("MaxCpMessageEnd"));
	}

	public Message getStatusMessage() {
		EmbedBuilder embedBuilder = new EmbedBuilder();

		spawn.prepareTime();
		if (spawn.bossId != 0) {
			embedBuilder.setTitle(
					String.format("%s %s (%s %s) %s %s - %s %s", StringLocalizer.getLocalString("StatusTitleBossStart"),
							spawn.getProperties().get("pkmn"), StringLocalizer.getLocalString("Lvl"),
							spawn.getProperties().get("level"), StringLocalizer.getLocalString("In"),
							spawn.getProperties().get("city"), StringLocalizer.getLocalString("Lobby"), lobbyCode),
					spawn.getProperties().get("gmaps"));

			embedBuilder.setDescription(StringLocalizer.getLocalString("StatusDescription"));

			embedBuilder.addField(StringLocalizer.getLocalString("LobbyMembers"), getTimeString(), false);
			embedBuilder.addField(StringLocalizer.getLocalString(StringLocalizer.getLocalString("Address")),
					String.format("%s %s, %s", spawn.getProperties().get("street_num"),
							spawn.getProperties().get("street"), spawn.getProperties().get("city")),
					false);
			embedBuilder.addField(StringLocalizer.getLocalString("GymName"), spawn.getProperties().get("gym_name"),
					false);
			embedBuilder.addField(StringLocalizer.getLocalString("GymOwners"), String.format("%s %s",
					spawn.getProperties().get("team_name"), spawn.getProperties().get("team_icon")), false);
			embedBuilder.addField(StringLocalizer.getLocalString("RaidEndTime"),
					String.format("%s (%s)", spawn.getProperties().get("24h_end"), spawn.timeLeft(spawn.raidEnd)),
					false);
			embedBuilder.addField(StringLocalizer.getLocalString("BossMoveset"),
					String.format("%s%s - %s%s", spawn.getProperties().get("quick_move"),
							spawn.getProperties().get("quick_move_type_icon"), spawn.getProperties().get("charge_move"),
							spawn.getProperties().get("charge_move_type_icon")),
					false);
			embedBuilder.addField(StringLocalizer.getLocalString("MaxCatchableCp"),
					spawn.getProperties().get("lvl20cp"), false);
			embedBuilder.addField(StringLocalizer.getLocalString("MaxCatchableCpWithBonus"),
					spawn.getProperties().get("lvl25cp"), false);

			StringBuilder weaknessEmoteStr = new StringBuilder();

			for (String s : Raid.getBossWeaknessEmotes(spawn.bossId)) {
				Emote emote = Types.emotes.get(s);
				if (emote != null) {
					weaknessEmoteStr.append(emote.getAsMention());
				}
			}

			embedBuilder.addField(StringLocalizer.getLocalString("WeakTo"), weaknessEmoteStr.toString(), true);

			StringBuilder strengthEmoteStr = new StringBuilder();

			for (String s : Raid.getBossStrengthsEmote(spawn.getMove_1(), spawn.getMove_2())) {
				Emote emote = Types.emotes.get(s);
				strengthEmoteStr.append(emote == null ? "" : emote.getAsMention());
			}

			embedBuilder.addField(StringLocalizer.getLocalString("StrongAgainst"), strengthEmoteStr.toString(), true);

			embedBuilder.setThumbnail(spawn.getIcon());
			embedBuilder.setImage(spawn.getImage(novaBot.getFormatting()));
		} else {
			embedBuilder.setTitle(
					String.format("%s %s %s %s - %s %s", StringLocalizer.getLocalString("StatusTitleEggStart"),
							spawn.getProperties().get("level"), StringLocalizer.getLocalString("EggIn"),
							spawn.getProperties().get("city"), StringLocalizer.getLocalString("Lobby"), lobbyCode));

			embedBuilder.setDescription(StringLocalizer.getLocalString("StatusDescription"));

			embedBuilder.addField(StringLocalizer.getLocalString("LobbyMembers"), getTimeString(), false);
			embedBuilder.addField(StringLocalizer.getLocalString("Address"),
					String.format("%s %s, %s", spawn.getProperties().get("street_num"),
							spawn.getProperties().get("street"), spawn.getProperties().get("city")),
					false);
			embedBuilder.addField(StringLocalizer.getLocalString("GymName"), spawn.getProperties().get("gym_name"),
					false);
			embedBuilder.addField(StringLocalizer.getLocalString("GymOwners"), String.format("%s %s",
					spawn.getProperties().get("team_name"), spawn.getProperties().get("team_icon")), false);
			embedBuilder.addField(StringLocalizer.getLocalString("RaidStartTime"),
					String.format("%s (%s)", spawn.getProperties().get("24h_start"), spawn.timeLeft(spawn.battleStart)),
					false);

			embedBuilder.setThumbnail(spawn.getIcon());
			embedBuilder.setImage(spawn.getImage(novaBot.getFormatting()));
		}

		MessageBuilder messageBuilder = new MessageBuilder().setEmbed(embedBuilder.build());

		return messageBuilder.build();
	}

	public String getTeamMessage() {
		StringBuilder str = new StringBuilder(
				String.format("%s %s %s:\n\n", WordUtils.capitalize(StringLocalizer.getLocalString("ThereAre")),
						memberCount(), StringLocalizer.getLocalString("UsersInThisTeam")));

		for (RaidLobbyMember member : members) {
			str.append(String.format("  %s%n", novaBot.guild.getMemberById(member.memberId).getEffectiveName()));
		}

		return str.toString();
	}
	
	public void setTime(String userId, String time) {
		for (RaidLobbyMember member : members) {
			if (member.memberId.equals(userId)) {
				member.time = time;
			}
		}
		novaBot.dataManager.updateLobby(lobbyCode, (int) nextTimeLeftUpdate, inviteCode, roleId, channelId, members, spawn.gymId, lobbyChatIds);
		updateLobbyChat();
		sendTimes();
	}

	public void setTime(String userId, int hour, int minute) {
		String time = String.format("%02d", hour) + ":" + String.format("%02d", minute);
		setTime(userId, time);
	}
	
	private String getTimeString() {
		TreeMap<String, Integer> times = new TreeMap<String, Integer>();
		for (RaidLobbyMember member : members) {
			String time;
			if (member.time == null) {
				time = StringLocalizer.getLocalString("NoTime");
			} else {
				time = member.time;
			}
	
			if (times.containsKey(time)) {
				times.put(time, times.get(time) + member.count);
			} else {
				times.put(time, member.count);
			}
		}
		String timesString = "";
		boolean first = true;
		for(Map.Entry<String,Integer> time : times.entrySet()) {
			if (first) {
				first = false;
			} else {
				timesString += "\n";
			}
			timesString += time.getKey() + ": " + time.getValue();
		}
		return timesString;
	}
	
	private TreeSet<String> getTimes() {
		TreeSet<String> times = new TreeSet<String>();
		
		ZonedDateTime middle = spawn.battleStart.plusMinutes(20);		
		ZonedDateTime end = spawn.battleStart.plusMinutes(40);

		ZoneId zone = novaBot.getConfig().getTimeZone();
		int startHour = spawn.battleStart.withZoneSameInstant(zone).getHour();
		int startMinute = spawn.battleStart.withZoneSameInstant(zone).getMinute();
		int middleHour = middle.withZoneSameInstant(zone).getHour();
		int middleMinute = middle.withZoneSameInstant(zone).getMinute();
		int endHour = end.withZoneSameInstant(zone).getHour();
		int endMinute = end.withZoneSameInstant(zone).getMinute();

		times.add(String.format("%02d", startHour) + ":" + String.format("%02d", startMinute));
		times.add(String.format("%02d", middleHour) + ":" + String.format("%02d", middleMinute));
		times.add(String.format("%02d", endHour) + ":" + String.format("%02d", endMinute));

		for (RaidLobbyMember member : members) {
			if (member.time == null) {
				continue;
			}
	
			if (!times.contains(member.time)) {
				times.add(member.time);
			}
		}
		return times;
	}
	
	private String getClickTimeString() {
		TreeSet<String> times = getTimes();
		String timesString = "";
		int count = 0;
		for (String time : times) {
			timesString += String.valueOf(Character.toChars(0x0031 + count )) + String.valueOf(Character.toChars(0x20E3)) +  "=" + time + " ";
					
			count++;
		}
		return timesString;
	}
	
	public void sendTimes() {
		sendTimes(getRole());
	}
	
	public void sendTimes(IMentionable ping) {
		
		TreeSet<String> times = getTimes();
		String timesString = getTimeString();
		String timesClickString = getClickTimeString();
				
		getChannel()
		.sendMessageFormat("**%s %s\n%s**\n\n*%s*\n%s", ping,
				StringLocalizer.getLocalString("CurrentLobbyTimes") + ":",
				timesString,
				StringLocalizer.getLocalString("SetTimeInfo"),
				timesClickString
				)
		.queue(
            msg -> {
            		for (int i = 0; i < times.size(); i++) {
            			String string = String.valueOf(Character.toChars(0x0031 + i)) + String.valueOf(Character.toChars(0x20E3));
            			msg.addReaction(string).queue();
            		}
            }
        );
		
	}
	 
	public void setCount(String userId, int userCount) {
		for (RaidLobbyMember member : members) {
			if (member.memberId.equals(userId)) {
				member.count = userCount;
			}
		}
		novaBot.dataManager.updateLobby(lobbyCode, (int) nextTimeLeftUpdate, inviteCode, roleId, channelId, members, spawn.gymId, lobbyChatIds);
		updateLobbyChat();
		sendTimes();

	}
	
	public void joinLobby(String userId, int userCount, String userTime, boolean forceTime) {

		if (stopped)
			return;

		if (spawn.raidEnd.isBefore(ZonedDateTime.now(UtilityFunctions.UTC)))
			return;

		Member member = novaBot.guild.getMemberById(userId);

		if (member == null)
			return;

		if (delete)
			delete = false;

		TextChannel channel = null;

		if (!created) {
			roleId = novaBot.guild.getController().createRole().complete().getId();

			Role role = novaBot.jda.getRoleById(roleId);

			String channelName = spawn.getProperties().get("gym_name").replace(" ", "-").replaceAll("[^\\w-]", "");

			channelName = channelName.substring(0, Math.min(25, channelName.length()));

			role.getManagerUpdatable().getNameField().setValue(String.format("raid-%s", channelName))
					.getMentionableField().setValue(true).update().queue();

			if (novaBot.getConfig().getRaidLobbyCategory() == null) {
				channelId = novaBot.guild.getController().createTextChannel(String.format("raid-%s", channelName))
						.complete().getId();
			} else {
				channelId = novaBot.jda.getCategoryById(novaBot.getConfig().getRaidLobbyCategory())
						.createTextChannel(String.format("raid-%s", channelName)).complete().getId();
			}

			channel = novaBot.jda.getTextChannelById(channelId);

			if (channel.getPermissionOverride(novaBot.guild.getPublicRole()) == null) {
				channel.createPermissionOverride(novaBot.guild.getPublicRole()).setDeny(Permission.MESSAGE_READ)
						.complete();
			} else {
				channel.getPermissionOverride(novaBot.guild.getPublicRole()).getManagerUpdatable()
						.deny(Permission.MESSAGE_READ).update().complete();
			}

			channel.createPermissionOverride(role)
					.setAllow(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY).complete();

			if (channel.getPermissionOverride(novaBot.jda.getRoleById(novaBot.getConfig().novabotRole())) == null) {
				channel.createPermissionOverride(novaBot.jda.getRoleById(novaBot.getConfig().novabotRole()))
						.setAllow(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.CREATE_INSTANT_INVITE)
						.complete();
			} else {
				channel.getPermissionOverride(novaBot.jda.getRoleById(novaBot.getConfig().novabotRole()))
						.getManagerUpdatable()
						.grant(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.CREATE_INSTANT_INVITE)
						.update().complete();
			}

			channel.createInvite().queue(inv -> {
				inviteCode = inv.getCode();
				novaBot.invites.add(inv);
				novaBot.dataManager.updateLobby(lobbyCode, (int) nextTimeLeftUpdate, inviteCode, roleId, channelId, members, spawn.gymId, lobbyChatIds);
			});

			long timeLeft = Duration.between(ZonedDateTime.now(UtilityFunctions.UTC), spawn.raidEnd).toMillis();

			raidLobbyLog.info(String.format("First join for lobbyCode %s, created channel.", lobbyCode));

			if (nextTimeLeftUpdate == 15) {
				getChannel()
						.sendMessageFormat("%s %s %s %s!", getRole(),
								StringLocalizer.getLocalString("RaidEndSoonMessage"), 15,
								StringLocalizer.getLocalString("Minutes"))
						.queueAfter(timeLeft - (15 * 60 * 1000), TimeUnit.MILLISECONDS);
			}

			getChannel()
					.sendMessageFormat("%s, %s %s %s", getRole(), StringLocalizer.getLocalString("RaidHasEndedMessage"),
							15, StringLocalizer.getLocalString("Minutes"))
					.queueAfter(timeLeft, TimeUnit.MILLISECONDS);

			channel.sendMessage(getStatusMessage()).queue();
			created = true;
		}

		novaBot.guild.getController().addRolesToMember(member, novaBot.jda.getRoleById(roleId)).queue();

		if (channel == null) {
			channel = novaBot.jda.getTextChannelById(channelId);
		}

		if (shutDownService != null) {
			raidLobbyLog.info("Cancelling lobby shutdown");
			shutDownService.shutdown();
			shutDownService = null;
		}

		String numberString = "";
		if (userCount > 1) {
			numberString = " (+" + (userCount - 1) + ")";
		}
		String timeString = "";
		String userTimeReal = userTime;
		if (!forceTime && userTime == null) {
            TreeMap<String, Integer> times = new TreeMap<String, Integer>();
            for (RaidLobbyMember memberI : members) {
                if (memberI.time != null) {
                    if (times.containsKey(memberI.time)) {
                        times.put(memberI.time, times.get(memberI.time) + memberI.count);
                    } else {
                        times.put(memberI.time, memberI.count);
                    }
                }
            }
            int maxHour = -1;
            int maxMinute = -1;
            int maxCount = 0;
            for(Map.Entry<String,Integer> time : times.entrySet()) {
                if (time.getValue() >= maxCount) {
                    String[] splited = time.getKey().split(":");
                    int hour = Integer.parseInt(splited[0]);
                    int minute = Integer.parseInt(splited[1]);
                    int currentHour = ZonedDateTime.now().plusMinutes(5).withZoneSameInstant(novaBot.getConfig().getTimeZone()).getHour();
                    int currentMinute = ZonedDateTime.now().plusMinutes(5).withZoneSameInstant(novaBot.getConfig().getTimeZone()).getMinute();

                    if ((hour > currentHour) || (hour == currentHour && minute > currentMinute)) {
                        if (maxHour == -1 || ((hour < maxHour) || (hour == maxHour && minute < maxMinute))) {
							maxHour = hour;
							maxMinute = minute;
							maxCount = time.getValue();
						}
                    }
                }
            }
            if (maxHour != -1) {
				userTimeReal = String.format("%02d", maxHour) + ":" + String.format("%02d", maxMinute);
				timeString = StringLocalizer.getLocalString("AutoSetLobbyTime").replace("<time>", userTimeReal) + "\n";
			}

		}
        members.add(new RaidLobbyMember(userId, userCount, userTimeReal));

        channel.sendMessageFormat("%s%s, %s!\n%s %s %s. %s", member, numberString, StringLocalizer.getLocalString("WelcomeMessage"), timeString,
				WordUtils.capitalize(StringLocalizer.getLocalString("ThereAreNow")), memberCount(),
				StringLocalizer.getLocalString("UsersInTheLobby")).queue();

		sendTimes();

		updateLobbyChat();
		novaBot.dataManager.updateLobby(lobbyCode, (int) nextTimeLeftUpdate, inviteCode, roleId, channelId, members, spawn.gymId, lobbyChatIds);
	}

	public boolean containsUser(String id) {
		for (RaidLobbyMember member : members) {
			if (member.memberId.equals(id)) {
				return true;
			}
		}
		return false;
	}
	
	public String timeForUser(String id) {
		for (RaidLobbyMember member : members) {
			if (member.memberId.equals(id)) {
				return member.time;
			}
		}
		return null;
	}

	public void alertEggHatched() {
		if (channelId != null) {
			getChannel().sendMessageFormat("%s %s %s!", getRole(), StringLocalizer.getLocalString("EggHatchedMessage"),
					spawn.getProperties().get("pkmn")).queue();
			getChannel().sendMessage(getStatusMessage()).queue();
		}
		updateLobbyChat();
	}

	public void leaveLobby(String id) {
		RaidLobbyMember memberToRemove = null;
		for (RaidLobbyMember member : members) {
			if (member.memberId.equals(id)) {
                		memberToRemove = member;
			}
		}
		if (memberToRemove != null) {
			members.remove(memberToRemove);
        	}
		novaBot.dataManager.updateLobby(lobbyCode, (int) nextTimeLeftUpdate, inviteCode, roleId, channelId, members, spawn.gymId, lobbyChatIds);

		if (memberCount() != 0) {
			sendTimes();
		}
		updateLobbyChat();

		novaBot.guild.getController().removeRolesFromMember(novaBot.guild.getMemberById(id), getRole()).queue();
		getChannel().sendMessageFormat("%s %s, %s %s %s.", novaBot.guild.getMemberById(id),
				StringLocalizer.getLocalString("LeftTheLobby"), StringLocalizer.getLocalString("ThereAreNow"),
				memberCount(), StringLocalizer.getLocalString("UsersInTheLobby")).queue();
	}

	public Message getInfoMessage() {
		EmbedBuilder embedBuilder = new EmbedBuilder();

		if (spawn.bossId != 0) {
			String timeLeft = spawn.timeLeft(spawn.raidEnd);

			embedBuilder.setTitle(
					String.format("[%s] %s (%s)- Lobby %s", spawn.getProperties().get("city"),
							spawn.getProperties().get("pkmn"), timeLeft, lobbyCode),
					spawn.getProperties().get("gmaps"));
			embedBuilder.setDescription(String.format(
					"Join the discord lobby to coordinate with other players by clicking the ✅ emoji below this post, or by typing `!joinraid %s` in any raid channel or PM with novabot.",
					lobbyCode));
			embedBuilder.addField("Team Size", String.valueOf(memberCount()), false);
			embedBuilder.addField(StringLocalizer.getLocalString("GymName"), spawn.getProperties().get("gym_name"),
					false);
			embedBuilder.addField(StringLocalizer.getLocalString("GymOwners"), String.format("%s %s",
					spawn.getProperties().get("team_name"), spawn.getProperties().get("team_icon")), false);
			embedBuilder.addField("Raid End",
					String.format("%s (%s remaining)", spawn.getProperties().get("24h_end"), timeLeft), false);

			embedBuilder.setThumbnail(spawn.getIcon());
		} else {
			String timeLeft = spawn.timeLeft(spawn.battleStart);

			embedBuilder.setTitle(
					String.format("[%s] Lvl %s Egg (Hatches in %s) - Lobby %s", spawn.getProperties().get("city"),
							spawn.getProperties().get("level"), timeLeft, lobbyCode),
					spawn.getProperties().get("gmaps"));
			embedBuilder.setDescription(String.format(
					"Join the discord lobby to coordinate with other players by clicking the number below this post, or by typing `!joinraid %s` in any raid channel or PM with novabot.",
					lobbyCode));
			embedBuilder.addField("Team Size", String.valueOf(memberCount()), false);
			embedBuilder.addField(StringLocalizer.getLocalString("GymName"), spawn.getProperties().get("gym_name"),
					false);
			embedBuilder.addField(StringLocalizer.getLocalString("GymOwners"), String.format("%s %s",
					spawn.getProperties().get("team_name"), spawn.getProperties().get("team_icon")), false);
			embedBuilder.addField("Raid Start",
					String.format("%s (%s remaining)", spawn.getProperties().get("24h_start"), timeLeft), false);

			embedBuilder.setThumbnail(spawn.getIcon());
		}
		MessageBuilder messageBuilder = new MessageBuilder().setEmbed(embedBuilder.build());

		return messageBuilder.build();
	}

	public void loadMembers(HashSet<RaidLobbyMember> prevMembers) {
		try {			
			Role role = novaBot.jda.getRoleById(roleId);
			
			final HashSet<String> memberIds = new HashSet<>();
			memberIds.addAll(novaBot.guild.getMembersWithRoles(role).stream().map(member -> member.getUser().getId()).collect(Collectors.toList()));
			members.clear();
			for (RaidLobbyMember member : prevMembers) {
				if (memberIds.contains(member.memberId)) {
					members.add(member);
				}
			}
		} catch (NullPointerException | IllegalArgumentException e) {
			raidLobbyLog.warn("Couldn't load members, couldnt find role by Id or ID was null");
		}
	}

	private void updateLobbyChat() {
		if (memberCount() == 0 || stopped) {
			if (lobbyChatIds != null && lobbyChatIds.length != 0) {
				String[] channelIds = novaBot.getConfig().getRaidChats(spawn.getGeofences());
				for (String lobbyChatId : lobbyChatIds) {
					for (String channelId: channelIds) {
						try {
							novaBot.guild.getTextChannelById(channelId).deleteMessageById(lobbyChatId).queue();
						} catch (Exception e) {}
					}
				}
				lobbyChatIds = null;
				novaBot.dataManager.updateLobby(lobbyCode, (int) nextTimeLeftUpdate, inviteCode, roleId, this.channelId, members, spawn.gymId, lobbyChatIds);
			}
		} else {
			Message message = spawn.buildMessage(novaBot.getFormatting(), members);
			if (lobbyChatIds != null && lobbyChatIds.length != 0) {
				String[] channelIds = novaBot.getConfig().getRaidChats(spawn.getGeofences());
				for (String lobbyChatId : lobbyChatIds) {
					for (String channelId: channelIds) {
						try {
							novaBot.guild.getTextChannelById(channelId).editMessageById(lobbyChatId, message).queue();
						} catch (Exception e) {}
					}
				}
			} else {
				String[] channelIds = novaBot.getConfig().getRaidChats(spawn.getGeofences());
				for (String channelId: channelIds) {
					novaBot.guild.getTextChannelById(channelId).sendMessage(message).queue(
							(m) -> {
								if (lobbyChatIds == null) {
									lobbyChatIds = new String[0];
								}
								lobbyChatIds = ArrayUtils.add(lobbyChatIds, m.getId());
								novaBot.dataManager.updateLobby(lobbyCode, (int) nextTimeLeftUpdate, inviteCode, roleId, this.channelId, members, spawn.gymId, lobbyChatIds);
								m.addReaction(novaBot.NUMBER_1).queue();
								m.addReaction(novaBot.NUMBER_2).queue();
								m.addReaction(novaBot.NUMBER_3).queue();
								m.addReaction(novaBot.NUMBER_4).queue();
								m.addReaction(novaBot.NUMBER_5).queue();
							}
					);
				}
			}
		}
    }

	private Role getRole() {
		return novaBot.jda.getRoleById(roleId);
	}
}
