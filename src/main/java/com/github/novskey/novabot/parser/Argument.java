package com.github.novskey.novabot.parser;

import lombok.Data;

import java.util.*;

@Data
public class Argument
{
    private ArgType type;
    private Object[] params;
    private ArrayList<String> malformed;

    public void setType(final ArgType type) {
        this.type = type;
    }

    public void setParams(final Object[] params) {
        this.params = params;
    }

    public ArgType getType() {
        return this.type;
    }

    public Object[] getParams() {
        return this.params;
    }

    public boolean notFullyParsed() {
        return (malformed != null) && malformed.size() > 0;
    }

    public static ArgType getDuplicateArg(final Argument[] args) {
        final ArrayList<ArgType> argTypes = new ArrayList<>();
        for (final Argument arg : args) {
            if (argTypes.contains(arg.getType())) {
                return arg.getType();
            }
            argTypes.add(arg.getType());
        }
        return null;
    }

    public static boolean containsDuplicates(final Argument[] args) {
        return getDuplicateArg(args) != null;
    }

    public void setMalformed(final ArrayList<String> malformed) {
        this.malformed = malformed;
    }

    public ArrayList<String> getMalformed() {
        return this.malformed;
    }

    public static String malformedToString(final HashMap<ArgType, ArrayList<String>> malformedArgs) {

        final String[] str = {""};

        malformedArgs.forEach((key, value) -> {
            for (String s : value) {
                str[0] += "  " + s + "\n";
            }
        });

        return str[0];
    }
}
