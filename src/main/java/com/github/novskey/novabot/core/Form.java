package com.github.novskey.novabot.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;

public class Form {

    //Taken from P static/js/map.js
    private static final String[] forms = new String[] {"unset", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "!", "?", ("Normal"), ("Sunny"), ("Rainy"), ("Snowy"), ("Normal"), ("Attack"), ("Defense"), ("Speed"), "1", "2", "3", "4", "5", "6", "7", "8", "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Alola"), "", ("Frost"), ("Fan"), ("Mow"), ("Wash"), ("Heat"), ("Plant"), ("Sandy"), ("Trash"), ("Altered"), ("Origin"), ("Sky"), ("Land"), ("Overcast"), ("Sunny"), ("West sea"), ("East sea"), ("West sea"), ("East sea"), ("Normal"), ("Fighting"), ("Flying"), ("Poison"), ("Ground"), ("Rock"), ("Bug"), ("Ghost"), ("Steel"), ("Fire"), ("Water"), ("Grass"), ("Electric"), ("Psychic"), ("Ice"), ("Dragon"), ("Dark"), ("Fairy"), ("Plant"), ("Sandy"), ("Trash"), "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", ("Armored"), ("A-intro"), "", ("Red Striped"), ("Blue Striped"), ("Standard"), ("Zen"), ("Incarnate"), ("Therian"), ("Incarnate"), ("Therian"), ("Incarnate"), ("Therian"), ("Normal"), ("Black"), ("White"), ("Ordinary"), ("Resolute"), ("Aria"), ("Pirouette"), "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ("Spring"), ("Summer"), ("Autumn"), ("Winter"), ("Spring"), ("Summer"), ("Autumn"), ("Winter"), ("Normal"), ("Shock"), ("Burn"), ("Chill"), ("Douse"), "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""};
    private static final HashMap<String, String> formMap = new HashMap();
    static {
    	int i;
    	for(i = 0; i < forms.length; i++) {
    		try {
    			int parseInt = Integer.parseInt(forms[i]);
    			continue; //Skip the numeric forms -- too confusing
    		} catch (NumberFormatException e) {
    			//OK!
    		}
    		if (forms[i].equals("") || forms[i].equals("unset")) {
    			continue;
    		}
    		//The unown forms have their own handling:
    		if (forms[i].length() <= 1) {
    			continue;
    		}
    		//System.out.println(forms[i] + " " + i);
    		formMap.put(forms[i].toLowerCase(), forms[i]);
    	}
    }
    public static void main(String[] args) {
    	System.out.println(formMap);
    	System.out.println(formMap.containsKey("alola"));
    	System.out.println(getFormsList());
    	outer: for(String form : new String[] {"Alola", "Trash", "Zen"}) {
    		for(int i = 0; i < forms.length; i++) {
    			if (forms[i].equals(form)) {
        			System.out.println(form + " " + i);
        			continue outer;
    			}
    		}
    	}
    }
	public static String fromID(Integer form) {
		if (form == null || form == 0 || form >= forms.length){
            return "";
        }
        return forms[form];
	}
	
	public static String fromString(String form) {
		return formMap.get(form);
	}
	
	public static String getFormsList() {
		return new TreeSet(formMap.keySet()).toString().replace("[","").replace("]","");
	}
    
}
