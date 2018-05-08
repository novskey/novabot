package com.github.novskey.novabot.pokemon;

import com.github.novskey.novabot.core.Location;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

public class Pokemon {
    private static final double[] cpMultipliers = new double[]{0.094, 0.16639787, 0.21573247, 0.25572005, 0.29024988,
                                                               0.3210876, 0.34921268, 0.37523559, 0.39956728, 0.42250001,
                                                               0.44310755, 0.46279839, 0.48168495, 0.49985844, 0.51739395,
                                                               0.53435433, 0.55079269, 0.56675452, 0.58227891, 0.59740001,
                                                               0.61215729, 0.62656713, 0.64065295, 0.65443563, 0.667934,
                                                               0.68116492, 0.69414365, 0.70688421, 0.71939909, 0.7317,
                                                               0.73776948, 0.74378943, 0.74976104, 0.75568551, 0.76156384,
                                                               0.76739717, 0.7731865, 0.77893275, 0.78463697, 0.79030001};
    private static ArrayList<String> VALID_NAMES;
    private static JsonObject baseStats;
    private static JsonObject pokemonInfo;
    private static JsonObject movesInfo;
    private static JsonObject formInfo;

    private static final Logger LOGGER = LoggerFactory.getLogger("Pokemon");

    static {
        try {
            JsonParser parser = new JsonParser();

            JsonElement element;
            try {
                element = parser.parse(new FileReader("static/data/base_stats.json"));

                if (element.isJsonObject()) {
                    baseStats = element.getAsJsonObject();
                }
            } catch (FileNotFoundException e) {
                LOGGER.error("Couldn't find file static/data/base_stats.json, aborting");
                System.exit(0);
            }

            try{
                element = parser.parse(new FileReader("static/data/pokemon.json"));

                if (element.isJsonObject()) {
                    pokemonInfo = element.getAsJsonObject();
                }

                VALID_NAMES = getPokemonNames(pokemonInfo);
            } catch (FileNotFoundException e) {
                LOGGER.error("Couldn't find ile static/data/pokemon.json, aborting");
                System.exit(0);
            }

            try{
                element = parser.parse(new FileReader("static/data/moves.json"));

                if (element.isJsonObject()) {
                    movesInfo = element.getAsJsonObject();
                }
            } catch (FileNotFoundException e) {
                LOGGER.error("Couldn't find static/data/moves.json, aborting");
                System.exit(0);
            }

            try{
                element = parser.parse(new FileReader("static/data/forms.json"));

                if (element.isJsonObject()) {
                    formInfo = element.getAsJsonObject();
                }
            } catch (FileNotFoundException e) {
                LOGGER.error("Couldn't find static/data/forms.json, aborting");
                System.exit(0);
            }

        }catch (Exception e){
            LOGGER.error("Error initialising Pokemon class",e);
        }
    }

    public final String name;
    public float miniv;
    public float maxiv;
    public int minlvl;
    public int maxlvl;
    public int maxcp;
    public int mincp;
    private Location location;

    public Pokemon(final String name) {
        this.miniv = 0.0f;
        this.maxiv = 100.0f;
        if (nameToID(name.toLowerCase()) == 0) {
            if (name.toLowerCase().equals("nidoran f")) {
                this.name = "nidoranf";
            } else if (name.toLowerCase().equals("nidoran m")) {
                this.name = "nidoranm";
            } else {
                this.name = null;
            }
        } else {
            this.name = name.toLowerCase();
        }
    }

    public Pokemon(final int id, final float min_iv, final float max_iv) {
		this(idToName(id));
        this.miniv = min_iv;
        this.maxiv = max_iv;
    }

    public Pokemon(final String pokeName, final Location location, final float miniv, final float maxiv, int minlvl, int maxlvl, int mincp, int maxcp) {
        this(pokeName);
        this.location = location;
        this.miniv = miniv;
        this.maxiv = maxiv;
        this.minlvl = minlvl;
        this.maxlvl = maxlvl;
        this.mincp = mincp;
        this.maxcp = maxcp;
    }

    public Pokemon(final int id) {
		this(idToName(id));
        this.miniv = 0.0f;
        this.maxiv = 100.0f;
    }

    public Pokemon(final int id, final Location location, final float miniv, final float maxiv) {
		this(idToName(id));
        this.location = location;
        this.miniv = miniv;
        this.maxiv = maxiv;
    }

    public Pokemon(int pokemonId, Location location, float minIv, float maxIv, int minLvl, int maxLvl, int minCp, int maxCp) {
        this(Pokemon.idToName(pokemonId),location,minIv,maxIv,minLvl,maxLvl,minCp,maxCp);
    }

    public static String getFilterName(int id) {

        if (id <= 0) return "";

        if (id > 2010) return "Unown";

        return Pokemon.pokemonInfo.getAsJsonObject(Integer.toString(id)).get("name").getAsString();
    }

    public int getID() {
//        System.out.println("getting id of " + this.name);
        return nameToID(this.name);
    }

    public static String getIcon(final int id, Integer form) {
        String url = "https://bitbucket.org/anzmap/sprites/raw/HEAD/";
        if (form != null && form != 0){
            url = url +  id + "-" + form;
        } else {
            url += id;
        }
        return url + ".png?4";
    }

    public Location getLocation() {
        return this.location;
    }

    public static String getMoveType(int moveId) {
        return movesInfo.getAsJsonObject(Integer.toString(moveId)).get("type").getAsString();
    }

    public static String getSize(int id, float height, float weight) {
        float baseStats[] = getBaseStats(id);

        float weightRatio = weight / baseStats[0];
        float heightRatio = height / baseStats[1];

        float size = heightRatio + weightRatio;

        if (size < 1.5) {
            return "tiny";
        }
        if (size <= 1.75) {
            return "small";
        }
        if (size < 2.25) {
            return "normal";
        }
        if (size <= 2.5) {
            return "large";
        }
        return "big";
    }

    public static ArrayList<String> getTypes(int bossId) {
        JsonArray types = pokemonInfo.getAsJsonObject(Integer.toString(bossId)).getAsJsonArray("types");

        ArrayList<String> typesList = new ArrayList<>();

        for (JsonElement type : types) {
            typesList.add(type.getAsString());
        }
        return typesList;
    }

    @Override
    public int hashCode() {
        return (int) (name.hashCode() *
                        ((minlvl+1) * (maxlvl+1)) *
                        ((mincp + 1) * (maxcp+1)) *
                        ((miniv + 1) * (maxiv + 1)) *
                (location == null ? 1 : location.toString().hashCode()));
    }

    @Override
    public boolean equals(final Object obj) {
        assert obj.getClass().getName().equals(this.getClass().getName());
        final Pokemon poke = (Pokemon) obj;
        return poke.name.equals(this.name) &&
                poke.minlvl == this.minlvl &&
                poke.maxlvl == this.maxlvl &&
                poke.mincp == this.mincp &&
                poke.maxcp == this.maxcp &&
                poke.miniv == this.miniv &&
                poke.maxiv == this.maxiv &&
                poke.location.equals(this.location);
    }

    @Override
    public String toString() {
        return String.format("%s (%s,%s)iv (%s%s)cp (%s%s)lvl",name,miniv,maxiv,mincp,maxcp,minlvl,maxlvl);
    }

    public static String idToName(final int id) {
        switch (id) {
            case 2011: {
                return "unowna";
            }
            case 2012: {
                return "unownb";
            }
            case 2013: {
                return "unownc";
            }
            case 2014: {
                return "unownd";
            }
            case 2015: {
                return "unowne";
            }
            case 2016: {
                return "unownf";
            }
            case 2017: {
                return "unowng";
            }
            case 2018: {
                return "unownh";
            }
            case 2019: {
                return "unowni";
            }
            case 2020: {
                return "unownj";
            }
            case 2021: {
                return "unownk";
            }
            case 2022: {
                return "unownl";
            }
            case 2023: {
                return "unownm";
            }
            case 2024: {
                return "unownn";
            }
            case 2025: {
                return "unowno";
            }
            case 2026: {
                return "unownp";
            }
            case 2027: {
                return "unownq";
            }
            case 2028: {
                return "unownr";
            }
            case 2029: {
                return "unowns";
            }
            case 2030: {
                return "unownt";
            }
            case 2031: {
                return "unownu";
            }
            case 2032: {
                return "unownv";
            }
            case 2033: {
                return "unownw";
            }
            case 2034: {
                return "unownx";
            }
            case 2035: {
                return "unowny";
            }
            case 2036: {
                return "unownz";
            }
			default: {
				if (id - 1 < 0 || id - 1 >= Pokemon.VALID_NAMES.size()) {
					return null;
				}
				else {
					return Pokemon.VALID_NAMES.get(id - 1);
				}
            }
        }
    }

    public static String formToString(final Integer id, Integer form) {
        if (form == null || form == 0){
            return "";
        }

        JsonObject pokemonForms = formInfo.getAsJsonObject(Integer.toString(id));

        if (pokemonForms == null){
            return "";
        }

        JsonElement formName = pokemonForms.get(Integer.toString(form));

        if (formName == null){
            return "";
        }

        return formName.getAsString();
    }

    public static String listToString(final Pokemon[] pokemon) {
        StringBuilder str = new StringBuilder();
        if (pokemon.length == 1) {
            return pokemon[0].name;
        }
        for (int i = 0; i < pokemon.length; ++i) {
            if (i == pokemon.length - 1) {
                str.append("and ").append(pokemon[i].name);
            } else {
                str.append((i == pokemon.length - 2) ? (pokemon[i].name + " ") : (pokemon[i].name + ", "));
            }
        }
        return str.toString();
    }

    public static int maxCpAtLevel(int id, int level) {
        double multiplier = cpMultipliers[level - 1];
        double attack     = (baseAtk(id) + 15) * multiplier;
        double defense    = (baseDef(id) + 15) * multiplier;
        double stamina    = (baseSta(id) + 15) * multiplier;
        return (int) Math.max(10, Math.floor(Math.sqrt(attack * attack * defense * stamina) / 10));
    }

    public static int nameToID(final String pokeName) {
        switch (pokeName) {
            case "unowna": {
                return 2011;
            }
            case "unownb": {
                return 2012;
            }
            case "unownc": {
                return 2013;
            }
            case "unownd": {
                return 2014;
            }
            case "unowne": {
                return 2015;
            }
            case "unownf": {
                return 2016;
            }
            case "unowng": {
                return 2017;
            }
            case "unownh": {
                return 2018;
            }
            case "unowni": {
                return 2019;
            }
            case "unownj": {
                return 2020;
            }
            case "unownk": {
                return 2021;
            }
            case "unownl": {
                return 2022;
            }
            case "unownm": {
                return 2023;
            }
            case "unownn": {
                return 2024;
            }
            case "unowno": {
                return 2025;
            }
            case "unownp": {
                return 2026;
            }
            case "unownq": {
                return 2027;
            }
            case "unownr": {
                return 2028;
            }
            case "unowns": {
                return 2029;
            }
            case "unownt": {
                return 2030;
            }
            case "unownu": {
                return 2031;
            }
            case "unownv": {
                return 2032;
            }
            case "unownw": {
                return 2033;
            }
            case "unownx": {
                return 2034;
            }
            case "unowny": {
                return 2035;
            }
            case "unownz": {
                return 2036;
            }
			default: {
                return Pokemon.VALID_NAMES.indexOf(pokeName) + 1;
            }
        }
    }

    private static double baseAtk(int id) {
        return baseStats.getAsJsonObject(Integer.toString(id)).get("attack").getAsDouble();
    }

    private static double baseDef(int id) {
        return baseStats.getAsJsonObject(Integer.toString(id)).get("defense").getAsDouble();
    }

    private static double baseSta(int id) {
        return baseStats.getAsJsonObject(Integer.toString(id)).get("stamina").getAsDouble();
    }

    public static String moveName(int id) {
        JsonObject moveObj = movesInfo.getAsJsonObject(Integer.toString(id));
        if(moveObj == null){
            System.out.println(String.format("move not found in json for id %s", id));
            return "unkn";
        }else {
            return moveObj.get("name").getAsString();
        }
    }

    public static void main(String[] args) {

        for (Integer integer : new Integer[]{13, 16, 19, 21, 23, 29, 32, 41, 48, 60, 98, 118, 120, 161, 163, 165, 167, 177, 183, 194}) {
            System.out.println(Pokemon.idToName(integer));
        }

//        NovaBot novaBot = new NovaBot();
//        novaBot.setup();
//        novaBot.start();
//        PrivateChannel channel = novaBot.jda.getUserById("107730875596169216").openPrivateChannel().complete();
//
//        ArrayList<Pair<Integer,Integer>> pairs = new ArrayList<>();
//        pairs.add(Pair.of(351,29));
//        pairs.add(Pair.of(351,30));
//        pairs.add(Pair.of(351,31));
//        pairs.add(Pair.of(351,32));
//        pairs.add(Pair.of(351,0));
//        pairs.add(Pair.of(351,null));
//
//        for (Pair<Integer, Integer> pair : pairs) {
//            MessageBuilder builder = new MessageBuilder(getFilterName(pair.getKey()) + " " + formToString(pair.getKey(), pair.getValue()));
//            EmbedBuilder embedBuilder = new EmbedBuilder();
//            embedBuilder.setThumbnail(getIcon(pair.getKey(),pair.getValue()));
//            builder.setEmbed(embedBuilder.build());
//            channel.sendMessage(builder.build()).queue();
//        }

    }

    private static float[] getBaseStats(int id) {
        JsonObject statsObj = baseStats.getAsJsonObject(Integer.toString(id));

        float stats[] = new float[2];

        stats[0] = statsObj.get("weight").getAsFloat();
        stats[1] = statsObj.get("height").getAsFloat();

        return stats;
    }

    static int getLevel(double cpModifier) {
        double unRoundedLevel;

        if (cpModifier < 0.734) {
            unRoundedLevel = (58.35178527 * cpModifier * cpModifier - 2.838007664 * cpModifier + 0.8539209906);
        } else {
            unRoundedLevel = 171.0112688 * cpModifier - 95.20425243;
        }

        return (int) Math.round(unRoundedLevel);
    }

    private static ArrayList<String> getPokemonNames(JsonObject pokemonInfo) {
        ArrayList<String> names = new ArrayList<>();

        for (int i = 1; i <= 721; i++) {
            JsonObject pokeObj = pokemonInfo.getAsJsonObject(Integer.toString(i));
            if (pokeObj != null) names.add(pokeObj.get("name").getAsString().toLowerCase());
        }
        return names;
    }

    public static int getRaidBossCp(int bossId, int raidLevel) {
        int stamina = 600;

        switch (raidLevel){
            case 1:
                stamina = 600;
                break;
            case 2:
                stamina = 1800;
                break;
            case 3:
                stamina = 3000;
                break;
            case 4:
                stamina = 7500;
                break;
            case 5:
                stamina = 12500;
                break;
        }
        return (int) Math.floor(((baseAtk(bossId) + 15) * Math.sqrt(baseDef(bossId) + 15) * Math.sqrt(stamina)) / 10);
    }

}
