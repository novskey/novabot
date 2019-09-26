package com.github.novskey.novabot.parser;

import lombok.Data;

import java.util.HashSet;
import java.util.TreeSet;

import static com.github.novskey.novabot.parser.ArgType.CommandStr;

@Data
public class Command {
    HashSet<ArgType> validArgTypes;
    HashSet<TreeSet<ArgType>> validArgCombinations;
    private HashSet<ArgType> requiredArgTypes;
    boolean allowDuplicateArgs;
    private Argument[] arguments;

    public Command() {
        this.validArgTypes = new HashSet<>();
        this.requiredArgTypes = new HashSet<>();
        this.validArgCombinations = new HashSet<>();
        this.allowDuplicateArgs = false;
    }

    public Command addValidArgCombination(final TreeSet<ArgType> validCombination) {
        this.validArgCombinations.add(validCombination);
        return this;
    }

    public String getHelpMessage() {
        StringBuilder str = new StringBuilder();

        str.append("Valid argument types:\n\n");
        for (ArgType validArgType : validArgTypes) {
            if(validArgType == CommandStr) continue;
            str.append(String.format("  %s%n",(validArgType)));
        }
        str.append("\nValid argument combinations:\n\n");
        for (TreeSet<ArgType> validArgCombination : validArgCombinations) {
            str.append(String.format("  %s%n",(argCombinationToString(validArgCombination))));
        }
        return str.toString();
    }

    private String argCombinationToString(TreeSet<ArgType> validArgCombination) {
        StringBuilder str = new StringBuilder();

        if (validArgCombination.size() == 1 && validArgCombination.contains(CommandStr)){
            str.append("nothing");
        }else{
            int i = 0;
            for (ArgType argType : validArgCombination) {
                if (argType == CommandStr) continue;
                if (i != 0) {
                    str.append(", ");
                }
                str.append(argType);
                i++;
            }
        }

        return str.toString();
    }

    public HashSet<TreeSet<ArgType>> getValidArgCombinations() {
        return validArgCombinations;
    }

    public Command setValidArgCombinations(final HashSet<TreeSet<ArgType>> validArgCombinations){
        this.validArgCombinations = validArgCombinations;
        return this;
    }

    public Command setValidArgTypes(final HashSet<ArgType> validArgTypes) {
        this.validArgTypes = validArgTypes;
        return this;
    }

    public Command setRequiredArgTypes(final HashSet<ArgType> requiredArgTypes) {
        this.requiredArgTypes = requiredArgTypes;
        return this;
    }

    public HashSet<ArgType> getValidArgTypes() {
        return this.validArgTypes;
    }

    public HashSet<ArgType> getRequiredArgTypes() {
        return this.requiredArgTypes;
    }

    public boolean meetsRequirements(final Argument[] args) {
        final boolean meetsRequirements = true;
        for (final ArgType argType : this.requiredArgTypes) {
            boolean containsThisArg = false;
            for (final Argument arg : args) {
                if (arg.getType() == argType) {
                    containsThisArg = true;
                }
            }
            if (!containsThisArg) {
                return false;
            }
        }
        return true;
    }

    public boolean validCombination(HashSet<ArgType> argTypes) {
        boolean match = false;
        for (TreeSet<ArgType> validArgCombination : validArgCombinations) {
            match = true;
            if (argTypes.size() == validArgCombination.size() || allowDuplicateArgs) {
                for (ArgType argType : validArgCombination) {
                    if (!argTypes.contains(argType)) {
                        match = false;
                    }
                }
            } else {
                match = false;
            }
            if (match){
                break;
            }
        }
        return match;
    }
}
