package com.github.novskey.novabot.researchtask;

import com.github.novskey.novabot.Util.StringLocalizer;
import com.github.novskey.novabot.core.Location;
import com.github.novskey.novabot.core.Types;
import com.github.novskey.novabot.pokemon.Pokemon;
import com.github.novskey.novabot.pokemon.Pokemon.PokemonBuilder;

import lombok.Builder;
import lombok.Getter;

import java.util.*;

/**
 * Created by Owner on 27/06/2017.
 */
@Builder(toBuilder = true)
public class ResearchTask {

	@Getter
    public String reward = "";
	@Getter
    public Location location;


    public ResearchTask(){

    }

    public ResearchTask(String reward, Location location){
        this.reward = reward;
        this.location = location;
    }

    public static String getResearchTasksString(ResearchTask[] researchTasks) {
        StringBuilder str = new StringBuilder();

        HashSet<ResearchTask> uniqueTasks = new HashSet<>();

        for (ResearchTask obj: researchTasks) {
        	uniqueTasks.add(new ResearchTask(obj.reward,Location.ALL));
        }

        int i = 0;
        for (ResearchTask obj : uniqueTasks ) {
            if(i != 0){
                str.append(", ");
                if (i == uniqueTasks.size() - 1){
                    str.append("and ");
                }
            }
            str.append(String.format("%s %s", obj.reward, StringLocalizer.getLocalString("ResearchTasks")));
            i++;
        }
        return str.toString();
    }

    @Override
    public int hashCode() {
        return reward.hashCode() *
                (location == null ? 1 : location.toDbString().hashCode())
               ;
    }

    @Override
    public boolean equals(Object obj) {
        if (!obj.getClass().equals(this.getClass())) return false;
        ResearchTask other = (ResearchTask) obj;
        return other.location.toDbString().equals(this.location.toDbString()) && other.reward.equals(this.reward);
    }

    @Override
    public String toString() {
        return String.format("RESEARCHTASK: %s,%s",reward,location);
    }
}
