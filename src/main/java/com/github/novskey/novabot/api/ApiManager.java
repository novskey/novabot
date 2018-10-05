package com.github.novskey.novabot.api;

import com.github.novskey.novabot.Util.StringLocalizer;
import com.github.novskey.novabot.core.Config;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.notifier.RaidNotificationSender;
import com.github.novskey.novabot.raids.RaidLobby;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.github.novskey.novabot.raids.RaidSpawn;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiManager {

    private static final Logger apiLog = LoggerFactory.getLogger("ApiLog");
    private static NovaBot novaBot = null;
    private static RaidLobbySerializer serializer = null;

    public static void setup(int port, NovaBot nBot) {
        novaBot = nBot;
        serializer = new RaidLobbySerializer(novaBot);
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/lobby", new ApiSearchHandler());
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            apiLog.error("Failed to start server. Got error: " + e.getMessage());
        }
    }

    static class ApiSearchHandler extends ApiManager.ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) {
            try {
                String query = t.getRequestURI().getQuery();
                if (query == null) {
                    errorRespones(t, "invalid_token", 401);
                    return;
                }
                Map<String, String> params = queryToMap(t.getRequestURI().getQuery());
                String token = params.get("token");
                String user = checkToken(token);

                if (user != null) {
                    String gymId = params.get("gymid");
                    String lobbyCode = params.get("id");

                    if (lobbyCode != null || gymId != null) {

                        if (lobbyCode != null) {
                            try {
                                lobbyCode = String.format("%04d", Integer.parseInt(lobbyCode));
                            } catch (Exception e) {
                                errorRespones(t, "invalid_params", 400);
                                return;
                            }
                        }

                        RaidLobby lobby;
                        if (lobbyCode != null) {
                            lobby = novaBot.lobbyManager.getLobby(lobbyCode);
                        } else {
                            lobby = novaBot.lobbyManager.getLobbyByGymId(gymId);
                        }

                        if (lobby == null) {

                            if (gymId != null) { // get raid from gym and create lobby if raid exists
                                RaidSpawn raidSpawn = novaBot.dataManager.getRaidForGym(gymId);
                                if (raidSpawn != null) {
                                    raidSpawn.setLobbyCode(RaidNotificationSender.getNextId());
                                    raidSpawn = novaBot.lobbyManager.newRaid(raidSpawn.getLobbyCode(), raidSpawn);
                                    lobby = novaBot.lobbyManager.getLobby(raidSpawn.getLobbyCode());
                                }
                            }

                            if (lobby == null) {
                                errorRespones(t, "lobby_not_found", 400);
                                return;
                            }
                        }

                        String action = params.get("action");
                        if (action == null) {
                            action = "list";
                        }

                        if (action.equals("signup")) {

                            if (lobby.containsUser(user)) {
                                errorRespones(t, "user_in_lobby", 400);
                                return;
                            }

                            // COUNT
                            int count;
                            if (params.get("count") != null) {
                                try {
                                    count = Integer.parseInt(params.get("count"));
                                } catch (Exception e) {
                                    errorRespones(t, "invalid_params", 400);
                                    return;
                                }
                                if (count <= 0 || count > 10) {
                                    errorRespones(t, "invalid_params", 400);
                                }
                            } else {
                                count = 1;
                            }


                            // TIME
                            String time = params.get("time");
                            if (time != null) {
                                String[] splited = time.split(":");
                                if (splited.length != 2) {
                                    errorRespones(t, "invalid_params", 400);
                                }
                                ;

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
                                    errorRespones(t, "invalid_params", 400);
                                    return;
                                }

                                Config config = novaBot.getConfig();
                                int startHour = lobby.spawn.battleStart.withZoneSameInstant(config.getTimeZone()).getHour();
                                int startMinute = lobby.spawn.battleStart.withZoneSameInstant(config.getTimeZone()).getMinute();
                                int endHour = lobby.spawn.raidEnd.withZoneSameInstant(config.getTimeZone()).getHour();
                                int endMinute = lobby.spawn.raidEnd.withZoneSameInstant(config.getTimeZone()).getMinute();

                                if (hour > endHour ||
                                        hour < startHour ||
                                        (hour == endHour && minute > endMinute) ||
                                        (hour == startHour && minute < startMinute)
                                        ) {
                                    errorRespones(t, "time_outside_raid", 400);
                                    return;
                                }

                                String userTime = String.format("%02d", hour) + ":" + String.format("%02d", minute);
                                lobby.joinLobby(user, count, userTime, true);
                            } else {
                                lobby.joinLobby(user, count, null, true);
                            }
                            String numberString = "";
                            if (count > 1) {
                                numberString = " (+" + (count - 1) + ")";
                            }
                            okRespones(t);
                            return;
                        } else if (action.equals("settime")) {
                            if (!lobby.containsUser(user)) {
                                errorRespones(t, "user_not_in_lobby", 400);
                                return;
                            }

                            String time = params.get("time");
                            String[] splited = time.split(":");
                            if (splited.length != 2) {
                                errorRespones(t, "invalid_params", 400);
                            }
                            ;

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
                                errorRespones(t, "invalid_params", 400);
                                return;
                            }

                            Config config = novaBot.getConfig();
                            int startHour = lobby.spawn.battleStart.withZoneSameInstant(config.getTimeZone()).getHour();
                            int startMinute = lobby.spawn.battleStart.withZoneSameInstant(config.getTimeZone()).getMinute();
                            int endHour = lobby.spawn.raidEnd.withZoneSameInstant(config.getTimeZone()).getHour();
                            int endMinute = lobby.spawn.raidEnd.withZoneSameInstant(config.getTimeZone()).getMinute();

                            if (hour > endHour ||
                                    hour < startHour ||
                                    (hour == endHour && minute > endMinute) ||
                                    (hour == startHour && minute < startMinute)
                                    ) {
                                errorRespones(t, "time_outside_raid", 400);
                                return;
                            }

                            String userTime = String.format("%02d", hour) + ":" + String.format("%02d", minute);
                            lobby.setTime(user, userTime);
                            okRespones(t);
                            return;
                        } else if (action.equals("setcount")) {
                            if (!lobby.containsUser(user)) {
                                errorRespones(t, "user_not_in_lobby", 400);
                                return;
                            }

                            int count;
                            try {
                                count = Integer.parseInt(params.get("count"));
                            } catch (Exception e) {
                                errorRespones(t, "invalid_params", 400);
                                return;
                            }
                            if (count <= 0 || count > 10) {
                                errorRespones(t, "invalid_params", 400);
                            }
                            lobby.setCount(user, count);
                            okRespones(t);
                            return;
                        } else if (action.equals("leave")) {
                            if (!lobby.containsUser(user)) {
                                errorRespones(t, "user_not_in_lobby", 400);
                                return;
                            }

                            lobby.leaveLobby(user);
                            okRespones(t);
                            return;
                        } else {
                            lobbyResponse(t, lobby, user);
                            return;
                        }

                    } else {
                        ArrayList<RaidLobby> lobbies = novaBot.lobbyManager.getActiveLobbies();
                        lobbiesResponse(t, lobbies, user);
                        return;
                    }
                } else {
                    errorRespones(t, "invalid_token", 401);
                    return;
                }
            } catch (Exception e) {
                apiLog.error("Found unhandled error: " + e.getMessage());
                e.printStackTrace();
                try {
                    errorRespones(t, "internal_server_error", 500);
                } catch (Exception e2) {
                    t.close();
                }
            }
        }
    }


    static class ApiHandler {

        protected void lobbyResponse(HttpExchange t, RaidLobby lobby, String user) throws IOException {
            String response = serializer.serialize(lobby, user);
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        protected void lobbiesResponse(HttpExchange t, ArrayList<RaidLobby> lobbies, String user) throws IOException {
            String response = serializer.serialize(lobbies, user);
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        protected void okRespones(HttpExchange t) throws IOException {
            String response = "{\"status\": \"ok\"}";
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        protected void errorRespones(HttpExchange t, String error, int code) throws IOException {
            String response = "{\"status\": \"error\", \"error\": \""+error+"\"}";
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(code, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        protected String checkToken(String token) {
            Token realToken = novaBot.getDataManager().getToken(token);
            if (realToken != null) {
                if (realToken.isValid()) {
                    return realToken.getUserId();
                }
            }

            return null;
        }

        protected Map<String, String> queryToMap(String query) {
            Map<String, String> result = new HashMap<>();
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                } else {
                    result.put(entry[0], "");
                }
            }
            return result;
        }
    }
}
