package com.github.novskey.novabot.rocketincident;

import com.github.novskey.novabot.core.Location;

import lombok.Getter;

/**
 * Created by Owner on 27/06/2017.
 */
public class RocketIncident {

	@Getter
    public Location location;

	public RocketIncident(){

    }

    public RocketIncident(Location location){
        this.location = location;
    }

    public static String getRocketIncidentsString(RocketIncident[] rocketis) {
    	return "Rocket incidences";
    }

    @Override
    public int hashCode() {
        return (location == null ? 1 : location.toDbString().hashCode())
               ;
    }

    @Override
    public boolean equals(Object obj) {
        if (!obj.getClass().equals(this.getClass())) return false;
        RocketIncident other = (RocketIncident) obj;
        return other.location.toDbString().equals(this.location.toDbString());
    }

    @Override
    public String toString() {
        return String.format("ROCKETINCIDENT: %s",location);
    }
}
