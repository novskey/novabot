package com.github.novskey.novabot.parser;

import java.util.*;

public enum ArgType
{
    CommandName,
    CommandStr,
    CP,
    Egg,
    Float,
    GymName,
    Int,
    IV,
    ATKIV,
    DEFIV, 
    STAIV,
    PVPGreatRank,
    PVPUltraRank,
    Level,
    Locations,
    Rewards,
    Pokemon,
    Preset,
    Status,
    TimeUnit,
    Unknown;


    public static void main(final String[] args) {
        final HashSet<ArgType> hashSet = new HashSet<>(Arrays.asList(ArgType.CommandStr, ArgType.Pokemon, ArgType.Locations));
        System.out.println(setToString(hashSet));
    }

    @Override
    public String toString() {
        switch (this) {
            case Float:
                return "number";
            case Locations:
                return "location";
            case Rewards:
            	return "rewards";
            case Pokemon:
                return "pokemon";
            case Unknown:
                return "other";
            case Status:
                return "nest status";
            case TimeUnit:
                return "time unit";
            case Int:
                return "whole number";
            case IV: case ATKIV: case DEFIV: case STAIV:
                return "IV";
            case PVPGreatRank: case PVPUltraRank:
                return "rank";
            case Level:
                return "level";
            case GymName:
                return "gym name";
            case Egg:
                return "egg level";
            case CP:
                return "cp";
            case Preset:
                return "preset";
            case CommandStr:
                return "command";
            case CommandName:
                return "command name";
            default:
                return null;
        }
    }

    public static String setToString(final HashSet<ArgType> argTypes) {
        StringBuilder str = new StringBuilder();
        argTypes.remove(ArgType.CommandStr);
        final ArgType[] argsArray = new ArgType[argTypes.size()];
        argTypes.toArray(argsArray);
        if (argTypes.size() == 1) {
            return argsArray[0].toString();
        }
        for (int i = 0; i < argsArray.length; ++i) {
            if (i == argsArray.length - 1) {
                str.append("and ").append(argsArray[i].toString());
            }
            else {
                str.append((i == argsArray.length - 2) ? (argsArray[i].toString() + " ") : (argsArray[i].toString() + ", "));
            }
        }
        return str.toString();
    }
}
