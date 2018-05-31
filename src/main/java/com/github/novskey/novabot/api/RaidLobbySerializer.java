package com.github.novskey.novabot.api;

import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.raids.RaidLobby;
import com.github.novskey.novabot.raids.RaidLobbyMember;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class RaidLobbySerializer {

    NovaBot novaBot;

    public RaidLobbySerializer(NovaBot novaBot) {
        this.novaBot = novaBot;
    }

    public String serialize(ArrayList<RaidLobby> value, String user) {

        Writer writer = new StringWriter();
        try (JsonGenerator generator = Json.createGenerator(writer)) {
            generator.writeStartObject();
                generator.write("status", "ok");
                generator.writeStartArray("lobbies");
                for (RaidLobby lobby: value) {
                    generator.writeStartObject();

                        generator.write("id", lobby.lobbyCode);
                        if (lobby.containsUser(user)) {
                            String time = lobby.timeForUser(user);
                            if (time == null) {
                                generator.write("in_lobby", true);
                            } else {
                                generator.write("in_lobby", time);
                            }
                        } else {
                            generator.write("in_lobby", false);
                        }
                        generator.write("member_count", lobby.memberCount());


                        TreeMap<String, Integer> times = new TreeMap<String, Integer>();
                        for (RaidLobbyMember member : lobby.members) {
                            String time;
                            if (member.time == null) {
                                time = "null";
                            } else {
                                time = member.time;
                            }

                            if (times.containsKey(time)) {
                                times.put(time, times.get(time) + member.count);
                            } else {
                                times.put(time, member.count);
                            }
                        }

                        generator.writeStartArray("times");
                        for(Map.Entry<String,Integer> time : times.entrySet()) {
                            generator.writeStartObject();
                            generator.write("time", time.getKey());
                            generator.write("count", time.getValue());
                            generator.writeEnd();
                        }
                        generator.writeEnd();
                    generator.writeEnd();
                }
                generator.writeEnd();
            generator.writeEnd();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return writer.toString();

    }

    public String serialize(RaidLobby value, String user) {

        Writer writer = new StringWriter();
        try (JsonGenerator generator = Json.createGenerator(writer)) {
            generator.writeStartObject();
                generator.write("status", "ok");
                generator.write("id", value.lobbyCode);
                if (value.containsUser(user)) {
                    String time = value.timeForUser(user);
                    if (time == null) {
                        generator.write("in_lobby", true);
                    } else {
                        generator.write("in_lobby", time);
                    }
                } else {
                    generator.write("in_lobby", false);
                }
                generator.writeStartObject("raid");
                    generator.write("raid_level", value.spawn.raidLevel);
                    if (value.spawn.bossId != 0) {
                        generator.write("boss_id", value.spawn.bossId);
                    } else {
                        generator.writeNull("boss_id");
                    }
                    generator.write("time_battle", value.spawn.battleStart.withZoneSameInstant(novaBot.getConfig().getTimeZone()).toEpochSecond());
                    generator.write("time_end", value.spawn.raidEnd.withZoneSameInstant(novaBot.getConfig().getTimeZone()).toEpochSecond());
                generator.writeEnd();
                generator.writeStartArray("members");
                for (RaidLobbyMember member: value.members) {
                    generator.writeStartObject();
                        generator.write("id", member.memberId);
                        generator.write("count", member.count);
                        if (member.time != null) {
                            generator.write("time", member.time);
                        } else {
                            generator.writeNull("time");
                        }
                    generator.writeEnd();
                }

                generator.writeEnd();
            generator.writeEnd();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return writer.toString();

    }

}
