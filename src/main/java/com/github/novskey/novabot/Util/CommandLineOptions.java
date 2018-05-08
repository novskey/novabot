package com.github.novskey.novabot.Util;

import lombok.*;

@Data
public class CommandLineOptions {
    private String config = "config.ini";
    private String geofences = "geofences.txt";
    private String supporterLevels = "supporterlevels.txt";
    private String suburbs = "suburbs.txt";
    private String gkeys = "gkeys.txt";
    private String formatting = "formatting.ini";
    private String raidChannels = "raidchannels.ini";
    private String pokeChannels = "pokechannels.ini";
    private String presets = "presets.ini";
    private String globalFilter = "globalfilter.json";
    private String locale = "en";

    public static CommandLineOptions parse(String[] args) {
        CommandLineOptions parsed = new CommandLineOptions();
        for (int i = 0; i < args.length; i += 2) {
            switch (args[i]) {
                case "-cf":
                    parsed.setConfig(args[i + 1]);
                    break;
                case "-gf":
                    parsed.setGeofences(args[i + 1]);
                    break;
                case "-glf":
                    parsed.setGlobalFilter(args[i + 1]);
                    break;
                case "-sl":
                    parsed.setSupporterLevels(args[i + 1]);
                    break;
                case "-s":
                    parsed.setSuburbs(args[i + 1]);
                    break;
                case "-gk":
                    parsed.setGkeys(args[i + 1]);
                    break;
                case "-f":
                    parsed.setFormatting(args[i + 1]);
                    break;
                case "-rc":
                    parsed.setRaidChannels(args[i + 1]);
                    break;
                case "-pc":
                    parsed.setPokeChannels(args[i + 1]);
                    break;
                case "-p":
                    parsed.setPresets(args[i + 1]);
                    break;
                case "-l":
                    parsed.setLocale(args[i + 1]);
                    break;
                default:
                    System.out.println("Unknown argument: " + args[i]);
                    System.out.println("Valid arguments: -cf, -gf, -sl, -s, -gk, -f, -rc, -pc, -p, -l");
                    System.exit(0);
            }
        }
        return parsed;
    }
}
