package com.github.novskey.novabot.core;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class Form {

    //Taken from PMSF static/data/pokemon.json using jq:
	//jq 'to_entries |
	//map( .value.forms // [] | 
	//        map( {key:.protoform | tonumber,value:.nameform})
	//)| 
	//add'
    //private static final String[] forms = new String[] {"unset", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "!", "?", ("Normal"), ("Sunny"), ("Rainy"), ("Snowy"), ("Normal"), ("Attack"), ("Defense"), ("Speed"), "1", "2", "3", "4", "5", "6", "7", "8", "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Frost"), ("Fan"), ("Mow"), ("Wash"), ("Heat"), ("Plant"), ("Sandy"), ("Trash"), ("Altered"), ("Origin"), ("Sky"), ("Land"), ("Overcast"), ("Sunny"), ("West sea"), ("East sea"), ("West sea"), ("East sea"), ("Normal"), ("Fighting"), ("Flying"), ("Poison"), ("Ground"), ("Rock"), ("Bug"), ("Ghost"), ("Steel"), ("Fire"), ("Water"), ("Grass"), ("Electric"), ("Psychic"), ("Ice"), ("Dragon"), ("Dark"), ("Fairy"), ("Plant"), ("Sandy"), ("Trash"), "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", ("Armored"), ("A-intro"), "", ("Red Striped"), ("Blue Striped"), ("Standard"), ("Zen"), ("Incarnate"), ("Therian"), ("Incarnate"), ("Therian"), ("Incarnate"), ("Therian"), ("Normal"), ("Black"), ("White"), ("Ordinary"), ("Resolute"), ("Aria"), ("Pirouette"), "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ("Spring"), ("Summer"), ("Autumn"), ("Winter"), ("Spring"), ("Summer"), ("Autumn"), ("Winter"), ("Normal"), ("Shock"), ("Burn"), ("Chill"), ("Douse"), "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""};
    private static final String[] forms;
	private static final HashMap<String, String> formMap = new HashMap();
    static {
    	ArrayList<JsonObject> _forms = new ArrayList<JsonObject>();
    	int nForms = 0;
        JsonParser parser = new JsonParser();
        JsonElement forms_root;
		try {
			forms_root = parser.parse(new FileReader("static/data/forms.json"));
	        for (JsonElement _formPair : forms_root.getAsJsonArray()) {
	        	JsonObject formPair = _formPair.getAsJsonObject();
	        	_forms.add(formPair);
	        	int key = formPair.get("key").getAsInt();
	        	nForms = Math.max(nForms, key + 1);
	        }
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to load forms.json");
		}
		forms = new String[nForms];
		for (JsonObject formPair : _forms){
        	int key = formPair.get("key").getAsInt();
			forms[key] = formPair.get("value").getAsString();
		}
		
    	int i;
    	for(i = 0; i < forms.length; i++) {
    		try {
    			int parseInt = Integer.parseInt(forms[i]);
    			continue; //Skip the numeric forms -- too confusing
    		} catch (NumberFormatException e) {
    			//OK!
    		}
    		if (forms[i] == null || forms[i].equals("") || forms[i].equals("unset")) {
    			continue;
    		}
    		//The unown forms have their own handling:
    		if (forms[i].length() <= 1) {
    			continue;
    		}
    		//replace spaces
    		String formCode = forms[i].toLowerCase().replaceAll("\\s+","");
    		//System.out.println(forms[i] + " " + i);
    		formMap.put(formCode, forms[i]);
    	}
    }
    public static void main(String[] args) {
    	System.out.println(formMap);
    	System.out.println(formMap.containsKey("alolan"));
    	System.out.println(getFormsList());
    	outer: for(String form : new String[] {"Alolan", "Trash", "Zen"}) {
    		for(int i = 0; i < forms.length; i++) {
    			if (forms[i].equals(form)) {
        			System.out.println(form + " " + i);
        			continue outer;
    			}
    		}
    	}
    }
    /**
     * Returns empty string for unknown forms.
     */
	public static String fromID(Integer form) {
		if (form == null || form == 0 || form >= forms.length){
            return "";
        }
        String toRet = forms[form];
        if (toRet == null) {
        	toRet = "";
        }
		return toRet;
	}
	
	public static String fromString(String form) {
		return formMap.get(form);
	}
	
	public static String getFormsList() {
		return new TreeSet(formMap.keySet()).toString().replace("[","").replace("]","");
	}
    
}
