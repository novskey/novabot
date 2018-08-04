package com.github.novskey.novabot.parser;

import com.github.novskey.novabot.Util.StringLocalizer;
import com.github.novskey.novabot.core.Config;
import com.github.novskey.novabot.core.NovaBot;

import java.util.*;

import static com.github.novskey.novabot.parser.ArgType.*;

public class Commands {
    private HashMap<String, Command> commands;

    public Commands(NovaBot novaBot) {
        Config config = novaBot.getConfig();
        commands = new HashMap<>();
        final Command clearLocation = new Command()
                .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Locations)))
                .setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Locations)))
                .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Locations)));

        commands.put(StringLocalizer.getLocalString("ClearLocationCommand"), clearLocation);

        if(config.presetsEnabled()){
            Command loadPreset = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Preset, Locations)));

            if (config.isAllowAllLocation()){
                loadPreset.addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Preset)))
                          .setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Preset)));
            }else{
                loadPreset.setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Preset, Locations)));
            }
            loadPreset.addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Preset, Locations)));

            Command delPreset = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Preset, Locations)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Preset)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Preset)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Preset, Locations)));

            Command clearPreset = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Preset)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Preset)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Preset)));

            Command clearPresetLocation = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Locations)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Locations)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Locations)));

            commands.put(StringLocalizer.getLocalString("LoadPresetCommand"), loadPreset);
            commands.put(StringLocalizer.getLocalString("DelPresetCommand"), delPreset);
            commands.put(StringLocalizer.getLocalString("ClearPresetCommand"), clearPreset);
            commands.put(StringLocalizer.getLocalString("ClearPresetLocationCommand"), clearPresetLocation);
        }

        if(config.pokemonEnabled()) {
            final Command addPokemon = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Pokemon, Locations, IV, Level, CP)));

            if (config.isAllowAllLocation()){
                addPokemon.setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Pokemon)))
                          .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon)));
            }else{
                addPokemon.setRequiredArgTypes(new HashSet<>(Arrays.asList(Pokemon, Locations)));
            }
            addPokemon.addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Locations)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Locations, IV)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Locations, IV, Level)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Locations, IV, CP)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Locations, IV, Level, CP)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Locations, Level)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Locations, Level, CP)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Locations, CP)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, IV)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, IV, Level)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, IV, CP)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, IV, Level, CP)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Level)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Level, CP)))
                      .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, CP)));

            final Command delPokemon = new Command()
                    .setValidArgTypes(addPokemon.getValidArgTypes())
                    .setRequiredArgTypes(addPokemon.getRequiredArgTypes())
                    .setValidArgCombinations(addPokemon.getValidArgCombinations());

            final Command clearPokemon = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Pokemon)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Pokemon)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon)));

            commands.put(StringLocalizer.getLocalString("AddPokemonCommand"), addPokemon);
            commands.put(StringLocalizer.getLocalString("DelPokemonCommand"), delPokemon);
            commands.put(StringLocalizer.getLocalString("ClearPokemonCommand"), clearPokemon);
            commands.put(StringLocalizer.getLocalString("ClearLocationCommand"), clearLocation);
        }

        if(config.raidsEnabled()){
            final Command addRaid = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Pokemon, Egg, Locations, Level, GymName)));

            if (config.isAllowAllLocation()){
                addRaid.setRequiredArgTypes(new HashSet<>(Collections.singletonList(CommandStr)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Egg)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Level)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Egg, Level)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Egg)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Egg, Level)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Level)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, GymName)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Egg, GymName)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Level, GymName)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Egg, Level, GymName)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Egg, GymName)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Egg, Level, GymName)))
                       .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Level, GymName)));
            }else{
                addRaid.setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Locations)));
            }

            addRaid.addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Egg, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Level, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Egg, Level, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Egg, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Egg, Level, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Level, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, GymName, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Egg, GymName, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Level, GymName, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Egg, Level, GymName, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Egg, GymName, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Egg, Level, GymName, Locations)))
                   .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Level, GymName, Locations)));

            final Command delRaid = new Command()
                    .setValidArgTypes(addRaid.getValidArgTypes())
                    .setRequiredArgTypes(addRaid.getRequiredArgTypes())
                    .setValidArgCombinations(addRaid.getValidArgCombinations());

            Command clearRaid = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Pokemon, Level, Egg)))
                    .setRequiredArgTypes(new HashSet<>(Collections.singletonList(CommandStr)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Level)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Egg)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Pokemon, Egg, Level)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Egg)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Egg, Level)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Level)));

            commands.put(StringLocalizer.getLocalString("AddRaidCommand"), addRaid);
            commands.put(StringLocalizer.getLocalString("DelRaidCommand"), delRaid);
            commands.put(StringLocalizer.getLocalString("ClearRaidCommand"), clearRaid);
            commands.put(StringLocalizer.getLocalString("ClearRaidLocationCommand"), clearLocation);
        }
        
        {
	        //Research tasks
        	final Command addrt = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Rewards, Locations)))
        			.setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Rewards, Locations)))
        			.addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Rewards, Locations)));

            final Command delrt = new Command()
                    .setValidArgTypes(addrt.getValidArgTypes())
                    .setRequiredArgTypes(addrt.getRequiredArgTypes())
                    .setValidArgCombinations(addrt.getValidArgCombinations());

            Command clearrt = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Rewards)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Rewards)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, Rewards)));
	
	        commands.put(StringLocalizer.getLocalString("AddResearchTaskCommand"), addrt);
	        commands.put(StringLocalizer.getLocalString("DelResearchTaskCommand"), delrt);
	        commands.put(StringLocalizer.getLocalString("ClearResearchTaskCommand"), clearrt);
        }

        if(config.statsEnabled()) {
            final Command stats = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, Pokemon, Int, TimeUnit)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(CommandStr, Pokemon, Int, TimeUnit)))
                    .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr,Pokemon, Int, TimeUnit)));
            commands.put(StringLocalizer.getLocalString("StatsCommand"), stats);
        }
        Command help = new Command()
                .setValidArgTypes(new HashSet<>(Arrays.asList(CommandStr, CommandName)))
                .setRequiredArgTypes(new HashSet<>(Collections.singleton(CommandStr)))
                .addValidArgCombination(new TreeSet<>(Arrays.asList(CommandStr, CommandName)))
                .addValidArgCombination(new TreeSet<>(Collections.singletonList(CommandStr)));
        commands.put(StringLocalizer.getLocalString("HelpCommand"),help);
    }

    public Command get(final String firstArg) {
        if(!firstArg.startsWith(StringLocalizer.getLocalString("Prefix"))){
            return commands.get(StringLocalizer.getLocalString("Prefix")+firstArg);
        }else{
            return commands.get(firstArg);
        }
    }

    public boolean isCommandWithArgs(final String s) {
        return commands.get(s) != null;
    }

    public boolean validName(String trimmed) {
        if(!trimmed.startsWith(StringLocalizer.getLocalString("Prefix"))){
            return commands.containsKey(StringLocalizer.getLocalString("Prefix")+trimmed);
        }else{
            return commands.containsKey(trimmed);
        }
    }
}
