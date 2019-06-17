package com.github.novskey.novabot.Util;

import com.github.novskey.novabot.core.Location;
import com.github.novskey.novabot.core.NovaBot;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.novskey.novabot.pokemon.Pokemon;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Paris on 23/12/2017.
 */
public class PokeBotMigrator {

    static JsonObject userFilters;

    public static void main(String[] args) {
    	
    	throw new RuntimeException("Not yet implemented");
    	
    	/*

        if(args.length == 0){
            System.out.println("Please specify a user filters file to migrate.");
            return;
        }

        NovaBot novaBot = new NovaBot();
        novaBot.setup();

        JsonParser parser = new JsonParser();

        try {
            JsonElement element = parser.parse(new FileReader(args[0]));

            if (element.isJsonObject()) {
               userFilters = element.getAsJsonObject();
            }else{
                System.out.println("Invalid JSON");
                return;
            }

            for (Map.Entry<String, JsonElement> entry : userFilters.entrySet()) {
                String userId = entry.getKey();
                JsonObject filters = entry.getValue().getAsJsonObject();
                boolean paused = filters.get("paused").getAsBoolean();
                JsonArray locations = filters.get("areas").getAsJsonArray();
                JsonObject raids = filters.get("raids").getAsJsonObject();
                JsonObject pokemon = filters.get("pokemon").getAsJsonObject();

                ArrayList<Pokemon> pokeList = new ArrayList<>();

                for (Map.Entry<String, JsonElement> pokeEntry : pokemon.entrySet()) {
                    String pokeName = pokeEntry.getKey().toLowerCase();
                    JsonElement filterElement = pokeEntry.getValue();

                    float min_iv = 0;
                    float max_iv = 100;

                    JsonObject filterObject;
                    JsonElement minObj, maxObj;
                    if (filterElement.isJsonObject()){
                        filterObject = filterElement.getAsJsonObject();
                        minObj = filterObject.get("min_iv");
                        maxObj = filterObject.get("max_iv");

                        if(minObj != null){
                            min_iv = minObj.getAsFloat();
                        }

                        if(maxObj != null){
                            max_iv = maxObj.getAsFloat();
                        }
                    }

                    int min_cp = 0;
                    int max_cp = Integer.MAX_VALUE;

                    if (filterElement.isJsonObject()){
                        filterObject = filterElement.getAsJsonObject();
                        minObj = filterObject.get("min_cp");
                        maxObj = filterObject.get("max_cp");

                        if(minObj != null){
                            min_cp = minObj.getAsInt();
                        }

                        if(maxObj != null){
                            max_cp = maxObj.getAsInt();
                        }
                    }

                    int min_lvl = 0;
                    int max_lvl = 40;

                    if (filterElement.isJsonObject()){
                        filterObject = filterElement.getAsJsonObject();
                        minObj = filterObject.get("min_level");
                        maxObj = filterObject.get("max_level");

                        if(minObj != null){
                            min_lvl = minObj.getAsInt();
                        }

                        if(maxObj != null){
                            max_lvl = maxObj.getAsInt();
                        }
                    }

                    if(Pokemon.nameToID(pokeName) != 0) {
                        for (JsonElement location : locations) {
                            Location loc = Location.fromString(location.getAsString(),novaBot);
                            if (loc != null) {
                                pokeList.add(new Pokemon(pokeName, loc, min_iv, max_iv, min_lvl,max_lvl, min_cp, max_cp));
                            }
                        }
                    }
                }
                System.out.println(userId + " paused = " + paused);
                System.out.println(locations);
                System.out.println(pokeList);
                System.out.println("");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        */
    }

}
