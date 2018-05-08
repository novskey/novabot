package com.github.novskey.novabot.core;

import com.github.novskey.novabot.Util.StringLocalizer;
import com.github.novskey.novabot.data.Preset;
import com.github.novskey.novabot.pokemon.Pokemon;
import com.github.novskey.novabot.raids.Raid;

import java.util.*;

public class UserPref {
    private final TreeMap<String, TreeSet<Pokemon>> pokemonPrefs = new TreeMap<>();
    private final TreeMap<String, TreeMap<String,TreeSet<Raid>>> raidPrefs = new TreeMap<>();
    private final TreeMap<String, TreeSet<String>> presetPrefs = new TreeMap<>();
    private final NovaBot novaBot;

    public UserPref(NovaBot novaBot) {
        this.novaBot = novaBot;
    }

    public void addPokemon(final Pokemon pokemon) {
        Location location = pokemon.getLocation();

        if (location == null) return;


        if (!this.pokemonPrefs.containsKey(location.toWords())) {
            final TreeSet<Pokemon> set = new TreeSet<>(Comparator.comparing(Pokemon::toString));
            set.add(pokemon);
            this.pokemonPrefs.put(location.toWords(), set);
        } else {
            this.pokemonPrefs.get(location.toWords()).add(pokemon);
        }
    }

    public void addPreset(String presetName, Location location) {

        if (!novaBot.getConfig().getPresets().containsKey(presetName)) return;

        if (location == null) return;

        if (!this.presetPrefs.containsKey(location.toWords())) {
            TreeSet<String> set = new TreeSet<>(Comparator.comparing(String::toString));
            set.add(presetName);
            this.presetPrefs.put(location.toWords(), set);
        } else {
            presetPrefs.get(location.toWords()).add(presetName);
        }
    }

    public void addRaid(final Raid raid) {
        Location location = raid.location;

        if (location == null) return;

        if (!this.raidPrefs.containsKey(location.toWords())) {
            final TreeMap<String,TreeSet<Raid>> map = new TreeMap<>(Comparator.comparing(String::toString));
            TreeSet<Raid> set = new TreeSet<>(Comparator.comparing(Raid::toString));
            set.add(raid);
            map.put(raid.gymName,set);
            this.raidPrefs.put(location.toWords(), map);
        } else {
            if (this.raidPrefs.get(location.toWords()).containsKey(raid.gymName)) {
                this.raidPrefs.get(location.toWords()).get(raid.gymName).add(raid);
            } else {
                TreeSet<Raid> set = new TreeSet<>(Comparator.comparing(Raid::toString));
                set.add(raid);
                this.raidPrefs.get(location.toWords()).put(raid.gymName,set);
            }
        }
    }

    public String allPokemonToString() {
        StringBuilder str = new StringBuilder();
        for (String locname : this.pokemonPrefs.keySet()) {
            Location location = Location.fromString(locname, novaBot);

            String locStr = locname;
            if (location != null) {
                locStr = location.toWords();
            }
            str.append("**").append(locname).append("**:\n");
            for (final Pokemon pokemon : this.pokemonPrefs.get(locStr)) {
                str.append(String.format("    %s%n", pokePrefString(pokemon)));
            }
            str.append("\n");
        }
        return str.toString();
    }

    public String allPresetsToString() {
        StringBuilder str = new StringBuilder();
        for (String locname : presetPrefs.keySet()) {
            Location location = Location.fromString(locname, novaBot);

            String locStr = locname;
            if (location != null) {
                locStr = location.toWords();
            }
            str.append("**").append(locname).append("**:\n");
            for (String preset : presetPrefs.get(locStr)) {
                str.append(String.format("    %s%n", preset));
            }
            str.append("\n");
        }
        return str.toString();
    }

    public String allRaidsToString() {
        StringBuilder str = new StringBuilder();
        for (String locname : this.raidPrefs.keySet()) {
            Location location = Location.fromString(locname, novaBot);

            String locStr = locname;
            if (location != null) {
                locStr = location.toWords();
            }
            str.append("**").append(locname).append("**:\n");
            for (Map.Entry<String, TreeSet<Raid>> entry : raidPrefs.get(locStr).entrySet()) {
                String gymName = entry.getKey();
                TreeSet<Raid> raids = entry.getValue();

                if (!gymName.equals("")){
                    str.append(String.format("  **%s**:\n", gymName));
                }

                for (Raid raid : raids) {
                    if (!gymName.equals("")) {
                        str.append("  ");
                    }
                    str.append("  ");

                    if (raid.bossId != 0) {
                        str.append(String.format("%s %s", Pokemon.idToName(raid.bossId), StringLocalizer.getLocalString("Raids")));
                    }else{
                        if (raid.eggLevel != 0){
                            str.append(String.format("%s %s %s", StringLocalizer.getLocalString("Level"), raid.eggLevel, StringLocalizer.getLocalString("Eggs")));
                        }else {
                            str.append(String.format("%s %s %s", StringLocalizer.getLocalString("Level"), raid.raidLevel, StringLocalizer.getLocalString("Raids")));
                        }
                    }
                    str.append("\n");
                }
                str.append("\n");
            }
            str.append("\n");
        }
        return str.toString();
    }

    public String allSettingsToString() {
        TreeMap<String, TreeSet<String>> prefMap = new TreeMap<>();

        raidPrefs.forEach((location, raidMap) -> {
            if (!prefMap.containsKey(location)) {
                final TreeSet<String> set = new TreeSet<>();


                for (Map.Entry<String, TreeSet<Raid>> entry : raidMap.entrySet()) {
                    String gymName = entry.getKey();
                    TreeSet<Raid> raids = entry.getValue();
                    StringBuilder str = new StringBuilder();

                    if (!gymName.equals("")){
                        str.append(String.format("  **%s**:\n", gymName));
                    }
                    int i = 0;

                    for (Raid raid : raids) {
                        if (!gymName.equals("")) {
                            str.append("  ");
                        }
                        str.append("  ");


                        if (raid.bossId != 0) {
                            str.append(String.format("%s %s", Pokemon.idToName(raid.bossId), StringLocalizer.getLocalString("Raids")));
                        }else{
                            if (raid.eggLevel != 0){
                                str.append(String.format("%s %s %s", StringLocalizer.getLocalString("Level"), raid.eggLevel, StringLocalizer.getLocalString("Eggs")));
                            }else {
                                str.append(String.format("%s %s %s", StringLocalizer.getLocalString("Level"), raid.raidLevel, StringLocalizer.getLocalString("Raids")));
                            }
                        }
                        if (i != raids.size() - 1) {
                            str.append("\n");
                        }
                        i++;
                    }
                    if (!gymName.equals("")) {
                        str.append("\n");
                    }

//                    for (Raid raid : raids) {
//                        StringBuilder str = new StringBuilder();
//                        if (raid.bossId != 0) {
//                            str.append(String.format("%s %s", Pokemon.idToName(raid.bossId), StringLocalizer.getLocalString("Raids")));
//                        }else{
//                            if (raid.eggLevel != 0){
//                                str.append(String.format("%s %s %s", StringLocalizer.getLocalString("Level"), raid.eggLevel, StringLocalizer.getLocalString("Eggs")));
//                            }else {
//                                str.append(String.format("%s %s %s", StringLocalizer.getLocalString("Level"), raid.raidLevel, StringLocalizer.getLocalString("Raids")));
//                            }
//                        }
//                        set.add(str.toString());
//                    }
                    set.add(str.toString());
                }
                prefMap.put(location, set);
            } else {
                for (Map.Entry<String, TreeSet<Raid>> entry : raidMap.entrySet()) {
                    String gymName = entry.getKey();
                    TreeSet<Raid> raids = entry.getValue();
                    StringBuilder str = new StringBuilder();

                    if (!gymName.equals("")){
                        str.append(String.format("  **%s**:\n", gymName));
                    }

                    int i = 0;
                    for (Raid raid : raids) {
                        if (!gymName.equals("")) {
                            str.append("  ");
                        }
                        str.append("  ");


                        if (raid.bossId != 0) {
                            str.append(String.format("%s %s", Pokemon.idToName(raid.bossId), StringLocalizer.getLocalString("Raids")));
                        }else{
                            if (raid.eggLevel != 0){
                                str.append(String.format("%s %s %s", StringLocalizer.getLocalString("Level"), raid.eggLevel, StringLocalizer.getLocalString("Eggs")));
                            }else {
                                str.append(String.format("%s %s %s", StringLocalizer.getLocalString("Level"), raid.raidLevel, StringLocalizer.getLocalString("Raids")));
                            }
                        }
                        if (i != raids.size() - 1) {
                            str.append("\n");
                        }
                        prefMap.get(location).add(str.toString());
                        i++;
                    }

//                    for (Raid raid : raids) {
//                        StringBuilder str = new StringBuilder();
//                        if (raid.bossId != 0) {
//                            str.append(String.format("%s %s", Pokemon.idToName(raid.bossId), StringLocalizer.getLocalString("Raids")));
//                        }else{
//                            if (raid.eggLevel != 0){
//                                str.append(String.format("%s %s %s", StringLocalizer.getLocalString("Level"), raid.eggLevel, StringLocalizer.getLocalString("Eggs")));
//                            }else {
//                                str.append(String.format("%s %s %s", StringLocalizer.getLocalString("Level"), raid.raidLevel, StringLocalizer.getLocalString("Raids")));
//                            }
//                        }
//                        set.add(str.toString());
//                    }
                }
            }
        });

        pokemonPrefs.forEach((location, pokemons) -> {
            if (!prefMap.containsKey(location)) {
                final TreeSet<String> set = new TreeSet<>();

                for (Pokemon pokemon : pokemons) {
                    set.add("  " + pokePrefString(pokemon));
                }
                prefMap.put(location, set);
            } else {
                for (Pokemon pokemon : pokemons) {
                    prefMap.get(location).add("  " + pokePrefString(pokemon));
                }
            }
        });

        presetPrefs.forEach((location, presets) -> {
            if (!prefMap.containsKey(location)) {
                TreeSet<String> set = new TreeSet<>();

                for (String preset : presets) {
                    set.add("  " + presetString(preset));
                }
                prefMap.put(location, set);
            } else {
                for (String preset : presets) {
                    prefMap.get(location).add("  " + presetString(preset));
                }
            }
        });

        StringBuilder str = new StringBuilder();
        for (String locname : prefMap.keySet()) {
            Location location = Location.fromString(locname, novaBot);

            String locStr = locname;
            if (location != null) {
                locStr = location.toWords();
            }
            str.append("**").append(locname).append("**:\n");
            for (final String string : prefMap.get(locStr)) {
                str.append(String.format("%s%n", string));
            }
            str.append("\n");
        }
        return str.toString();
    }

    public boolean isPokeEmpty() {
        boolean[] empty = {true};
        pokemonPrefs.forEach((loc, obj) -> {
            if (obj.size() > 0) {
                empty[0] = false;
            }
        });
        return empty[0];
    }

    public boolean isPresetEmpty() {
        boolean[] empty = {true};
        presetPrefs.forEach((loc, obj) -> {
            if (obj.size() > 0) {
                empty[0] = false;
            }
        });
        return empty[0];
    }

    public boolean isRaidEmpty() {
        boolean[] empty = {true};
        raidPrefs.forEach((loc, obj) -> {
            if (obj.size() > 0) {
                empty[0] = false;
            }
        });
        return empty[0];
    }

    private String pokePrefString(Pokemon pokemon) {
        String str = pokemon.name;
        boolean iv = false, lvl = false;
        if (pokemon.miniv > 0.0f || pokemon.maxiv < 100.0f) {
            iv = true;
            if (pokemon.maxiv < 100.0f) {
                if (pokemon.miniv == pokemon.maxiv) {
                    str = str + " " + pokemon.miniv + "%";
                } else {
                    str = str + " " + pokemon.miniv + "-" + pokemon.maxiv + "%";
                }
            } else {
                str = str + " " + pokemon.miniv + ((pokemon.miniv == pokemon.maxiv) ? "%" : "%+");
            }
        }
        if (pokemon.minlvl > 0 || pokemon.maxlvl < 40) {
            lvl = true;
            if (iv){
                str += ",";
            }
            if (pokemon.maxlvl < 40) {
                if (pokemon.minlvl == pokemon.maxlvl) {
                    str = str + " level " + pokemon.minlvl + "";
                } else {
                    str = str + " level " + pokemon.minlvl + "-" + pokemon.maxlvl;
                }
            } else {
                str = str + " level " + pokemon.minlvl + ((pokemon.minlvl == pokemon.maxlvl) ? "" : "+");
            }
        }
        if (pokemon.mincp > 0 || pokemon.maxcp < Integer.MAX_VALUE) {
            if (iv || lvl){
                str += ",";
            }
            if (pokemon.maxcp < Integer.MAX_VALUE) {
                if (pokemon.mincp == pokemon.maxcp) {
                    str = str + " " + pokemon.mincp + "CP" + "";
                } else {
                    str = str + " " + pokemon.mincp + "-" + pokemon.maxcp + "CP";
                }
            } else {
                str = str + " " + pokemon.mincp + ((pokemon.mincp == pokemon.maxcp) ? "CP" : "CP+");
            }
        }
        return str;
    }

    private String presetString(String preset) {
        return preset + " preset";
    }

    public void addPreset(Preset preset) {
        addPreset(preset.presetName, preset.location);
    }
}
