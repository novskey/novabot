package com.github.novskey.novabot.parser;

import com.github.novskey.novabot.core.Location;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.pokemon.Pokemon;
import com.github.novskey.novabot.raids.Raid;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static com.github.novskey.novabot.Util.StringLocalizer.getLocalString;

@Data
public class UserCommand
{
    public final NovaBot novaBot;
    private Argument[] args;
    private final ArrayList<InputError> exceptions;

    public UserCommand(NovaBot novaBot) {
        this.exceptions = new ArrayList<>();
        this.novaBot = novaBot;
    }

    public HashSet<ArgType> getArgTypes() {
        HashSet<ArgType> argTypes = new HashSet<>();

        for (Argument arg : args) {
            argTypes.add(arg.getType());
        }
        return argTypes;
    }

    public ArrayList<InputError> getExceptions() {
        return this.exceptions;
    }

    public void setArgs(final Argument[] args) {
        this.args = args;
    }

    public Pokemon[] buildPokemon() {
        Location[] locations = {Location.ALL};
        float miniv = 0.0f;
        float maxiv = 100.0f;
        int minlvl= 0;
        int maxlvl = 40;
        int mincp = 0;
        int maxcp = Integer.MAX_VALUE;

        String[] pokeNames = new String[0];
        for (final Argument arg : this.args) {
            switch (arg.getType()) {
                case Locations:
                    locations = this.toLocations(arg.getParams());
                    break;
                case Pokemon:
                    pokeNames = this.toStrings(arg.getParams());
                    break;
                case IV:
                    miniv = (float)arg.getParams()[0];
                    if (arg.getParams().length == 2) {
                        maxiv = (float)arg.getParams()[1];
                        break;
                    }
                    break;
                case Level:
                    minlvl = (int)arg.getParams()[0];
                    if (arg.getParams().length == 2) {
                        maxlvl = (int)arg.getParams()[1];
                    }
                    break;
                case CP:
                    mincp = (int)arg.getParams()[0];
                    if (arg.getParams().length == 2) {
                        maxcp = (int)arg.getParams()[1];
                    }
                    break;
            }
        }
        final ArrayList<Pokemon> pokemons = new ArrayList<>();
        for (final String pokeName : pokeNames) {
            for (final Location location : locations) {
                System.out.println(pokeName);
                pokemons.add(new Pokemon(pokeName, location, miniv, maxiv, minlvl, maxlvl, mincp, maxcp));
            }
        }
        final Pokemon[] pokeArray = new Pokemon[pokemons.size()];
        return pokemons.toArray(pokeArray);
    }

    private Location[] toLocations(final Object[] params) {
        final Location[] locations = new Location[params.length];
        for (int i = 0; i < params.length; ++i) {
            locations[i] = (Location)params[i];
        }
        return locations;
    }

    private String[] toStrings(final Object[] params) {
        final String[] strings = new String[params.length];
        for (int i = 0; i < params.length; ++i) {
            strings[i] = params[i].toString();
        }
        return strings;
    }

    private Integer[] toIntegers(final Object[] params) {
        final Integer[] integers = new Integer[params.length];
        for (int i = 0; i < params.length; ++i) {
            integers[i] = (Integer) params[i];
        }
        return integers;
    }

    public Argument getArg(final int i) {
        return this.args[i];
    }

    public void addException(final InputError e) {
        this.exceptions.add(e);
    }

    public Argument[] getArgs() {
        return this.args;
    }

    public HashMap<ArgType, ArrayList<String>> getMalformedArgs() {
        final HashMap<ArgType, ArrayList<String>> malformed = new HashMap<>();
        for (final Argument arg : this.args) {
            if (arg.notFullyParsed()) {
                if (!malformed.containsKey(arg.getType())) {
                    final ArrayList<String> newList = new ArrayList<>();
                    malformed.put(arg.getType(), newList);
                }
                for (final String s : arg.getMalformed()) {
                    malformed.get(arg.getType()).add(s);
                }
            }
        }
        return malformed;
    }

    public boolean containsArg(final ArgType argType) {
        for (final Argument arg : this.args) {
            if (arg.getType() == argType) {
                return true;
            }
        }
        return false;
    }

    public Argument getArg(final ArgType argType) {
        for (final Argument arg : this.args) {
            if(arg == null) continue;
            if (arg.getType() == argType) {
                return arg;
            }
        }
        return null;
    }

    public Pokemon[] getUniquePokemon() {
        Argument pokemonArg = getArg(ArgType.Pokemon);
        if(pokemonArg == null) {
            return new Pokemon[] {};
        }

        String[] pokeNames;
        pokeNames = this.toStrings(pokemonArg.getParams());
        final Pokemon[] pokemons = new Pokemon[pokeNames.length];
        for (int i = 0; i < pokeNames.length; ++i) {
            pokemons[i] = new Pokemon(pokeNames[i]);
        }
        return pokemons;
    }

    public boolean containsBlacklisted() {
        return this.getBlacklisted().size() > 0;
    }

    public ArrayList<String> getBlacklisted() {
        final ArrayList<String> blacklisted = new ArrayList<>();
        for (final Object o : this.getArg(ArgType.Pokemon).getParams()) {
            if (novaBot.getConfig().getBlacklist().contains(Pokemon.nameToID((String) o))) {
                blacklisted.add((String)o);
            }
        }
        return blacklisted;
    }

    public Location[] getLocations() {
        return this.toLocations(this.getArg(ArgType.Locations).getParams());
    }

    public String getIvMessage() {
        String message = "";
        if (containsArg(ArgType.IV)) {
            final Argument ivArg = getArg(ArgType.IV);
            if (ivArg.getParams().length == 1) {
                message = message + " " + ivArg.getParams()[0] + "% " + getLocalString("IvOrAbove");
            } else {
                message = message + " " + getLocalString("Between") + " " + ivArg.getParams()[0] + " " + getLocalString("And") + " " + ivArg.getParams()[1] + "% " + getLocalString("IV");
            }
        }
        return message;
    }


    public Raid[] buildRaids() {
        Location[] locations = { Location.ALL };
        String[] bossNames = {};
        String[] gymNames = {""};
        Integer[] eggLevels = {};
        Integer[] raidLevels = {};

        for (final Argument arg : this.args) {
            switch (arg.getType()) {
                case Locations:
                    locations = this.toLocations(arg.getParams());
                    break;
                case Pokemon:
                    bossNames = this.toStrings(arg.getParams());
                    break;
                case Egg:
                    eggLevels = this.toIntegers(arg.getParams());
                    break;
                case GymName:
                    gymNames = this.toStrings(arg.getParams());
                    break;
                case Level:
                    raidLevels = this.toIntegers(arg.getParams());
                    break;
            }
        }

        final ArrayList<Raid> raids = new ArrayList<>();
        for (final String bossName : bossNames) {
            for (String gymName : gymNames) {
                for (final Location location : locations) {
                    Raid raid = new Raid(Pokemon.nameToID(bossName),0,0, gymName,location);
                    System.out.println(raid);
                    raids.add(raid);
                }
            }
        }

        for (Integer eggLevel : eggLevels) {
            for (String gymName : gymNames) {
                for (final Location location : locations) {
                    Raid raid = new Raid(0,eggLevel,0, gymName,location);
                    System.out.println(raid);
                    raids.add(raid);
                }
            }
        }

        for (Integer raidLevel : raidLevels) {
            for (String gymName : gymNames) {
                for (final Location location : locations) {
                    Raid raid = new Raid(0,0,raidLevel, gymName,location);
                    System.out.println(raid);
                    raids.add(raid);
                }
            }
        }
        final Raid[] raidArray = new Raid[raids.size()];
        return raids.toArray(raidArray);
    }

    public String getCpMessage() {
        String message = "";
        if (containsArg(ArgType.CP)) {
            final Argument cpArg = getArg(ArgType.CP);
            if (cpArg.getParams().length == 1) {
                message = message + " " + cpArg.getParams()[0] + " " + getLocalString("CpOrAbove");
            }
            else {
                message = message + " " + getLocalString("Between") + " " + cpArg.getParams()[0] + " " + getLocalString("And") + " " + cpArg.getParams()[1] + " " + getLocalString("CP");
            }
        }
        return message;
    }

    public String getLevelMessage() {
        String message = "";
        if (containsArg(ArgType.Level)) {
            final Argument levelArg = getArg(ArgType.Level);
            if (levelArg.getParams().length == 1) {
                message = message + " " + getLocalString("Level") + " " + levelArg.getParams()[0] + " " + getLocalString("OrAbove");
            }
            else {
                message = message + " " + getLocalString("Between") + " " + getLocalString("Level") + " " + levelArg.getParams()[0] + " " + getLocalString("And") + " " + getLocalString("Level") + " " + levelArg.getParams()[1];
            }
        }
        return message;
    }
}
