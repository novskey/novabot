package com.github.novskey.novabot.parser;

import com.github.novskey.novabot.core.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Commands {
    private HashMap<String, Command> commands;

    public Commands(Config config) {
        commands = new HashMap<>();
        final Command clearLocation = new Command()
                .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Locations)))
                .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Locations)))
                .setArgRange(1, 1);

        commands.put("!clearlocation", clearLocation);

        if(config.presetsEnabled()){
            Command loadPreset = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Preset,ArgType.Locations)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Preset)))
                    .setArgRange(1,3);

            Command delPreset = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Preset,ArgType.Locations)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Preset)))
                    .setArgRange(1,3);

            Command clearPreset = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Preset)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Preset)))
                    .setArgRange(1,2);

            Command clearPresetLocation = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Locations)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr,ArgType.Locations)))
                    .setArgRange(1,2);

            commands.put("!loadpreset", loadPreset);
            commands.put("!delpreset", delPreset);
            commands.put("!clearpreset", clearPreset);
            commands.put("!clearpresetlocation", clearPresetLocation);
        }

        if(config.pokemonEnabled()) {
            final Command addPokemon = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon, ArgType.Locations, ArgType.IV, ArgType.Level, ArgType.CP)))
                    .setArgRange(1, 5);

            if (config.isAllowAllLocation()){
                addPokemon.setRequiredArgTypes(new HashSet<>(Collections.singletonList(ArgType.Pokemon)));
            }else{
                addPokemon.setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.Pokemon,ArgType.Locations)));
            }

            final Command delPokemon = new Command()
                    .setValidArgTypes(addPokemon.getValidArgTypes())
                    .setRequiredArgTypes(addPokemon.getRequiredArgTypes())
                    .setArgRange(1, 5);

            final Command clearPokemon = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon)))
                    .setArgRange(1, 1);

            commands.put("!addpokemon", addPokemon);
            commands.put("!delpokemon", delPokemon);
            commands.put("!clearpokemon", clearPokemon);
            commands.put("!clearpokelocation", clearLocation);
        }

        if(config.raidsEnabled()){
            final Command addRaid = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon, ArgType.Locations)))
                    .setArgRange(1, 3);

            if (config.isAllowAllLocation()){
                addRaid.setRequiredArgTypes(new HashSet<>(Collections.singletonList(ArgType.Pokemon)));
            }else{
                addRaid.setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.Pokemon,ArgType.Locations)));
            }

            final Command delRaid = new Command()
                    .setValidArgTypes(addRaid.getValidArgTypes())
                    .setRequiredArgTypes(addRaid.getRequiredArgTypes())
                    .setArgRange(1, 3);

            Command clearRaid = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon)))
                    .setArgRange(1, 1);

            commands.put("!addraid", addRaid);
            commands.put("!delraid", delRaid);
            commands.put("!clearraid", clearRaid);
            commands.put("!clearraidlocation", clearLocation);
        }

        if(config.statsEnabled()) {
            final Command stats = new Command()
                    .setValidArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon, ArgType.Int, ArgType.TimeUnit)))
                    .setRequiredArgTypes(new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon, ArgType.Int, ArgType.TimeUnit)))
                    .setArgRange(1, 3);
            commands.put("!stats", stats);
        }
    }

    public Command get(final String firstArg) {
        return commands.get(firstArg);
    }

    public boolean isCommandWithArgs(final String s) {
        return commands.get(s) != null;
    }

}
