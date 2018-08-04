package com.github.novskey.novabot.core;

import java.util.HashMap;

class Format {

    private final HashMap<String, HashMap<String, String>> formatting = new HashMap<>();

    public Format() {
        formatting.put("pokemon", new HashMap<>());
        formatting.put("raidEgg", new HashMap<>());
        formatting.put("raidBoss", new HashMap<>());
        formatting.put("researchTask", new HashMap<>());
    }

    public void addFormatting(String type, String key, String value) {
        formatting.get(type).put(key, value);
    }

    public String getFormatting(String type, String key) {
        return formatting.get(type).get(key);
    }

}
