package com.github.novskey.novabot.Util;

import java.util.HashMap;
import java.util.ResourceBundle;

public class StringLocalizer {

    private static StringLocalizer instance;

    private HashMap<String, String> localStringCache;
    private ResourceBundle messagesBundle;
    private ResourceBundle timeUnitsBundle;

    private StringLocalizer() {
    }

    private StringLocalizer(ResourceBundle messagesBundle, ResourceBundle timeUnitsBundle) {
        this.messagesBundle = messagesBundle;
        this.timeUnitsBundle = timeUnitsBundle;
        this.localStringCache = new HashMap<>();
    }

    public static void init(ResourceBundle messagesBundle, ResourceBundle timeUnitsBundle) {
        instance = new StringLocalizer(messagesBundle, timeUnitsBundle);
    }

    public synchronized static String getLocalString(String key) {
        if (instance == null) {
            throw new RuntimeException("StringLocalizer not initalized");
        }

        if(!instance.localStringCache.containsKey(key)){
            instance.localStringCache.put(key,
                    formatStr(formatStr(getLocalString(getLocalString(key, instance.messagesBundle),
                            instance.timeUnitsBundle), instance.messagesBundle), instance.messagesBundle));
        }
        return instance.localStringCache.get(key);
    }

    private synchronized static String getLocalString(String key, ResourceBundle resourceBundle) {
        if (resourceBundle.containsKey(key)) {
            return formatStr(resourceBundle.getString(key), resourceBundle);
        } else {
            return key;
        }
    }

    private synchronized static String formatStr(String string, ResourceBundle resourceBundle) {
        for (String key : resourceBundle.keySet()) {
            if (resourceBundle.containsKey(key)) {
                string = string.replace("<" + key.toLowerCase() + ">", resourceBundle.getString(key));
            }
        }
        return string;
    }

}
