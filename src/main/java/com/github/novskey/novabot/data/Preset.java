package com.github.novskey.novabot.data;

import com.github.novskey.novabot.core.Location;

import lombok.Builder;
import lombok.Getter;

/**
 * Created by Paris on 17/01/2018.
 */
@Builder(toBuilder = true)
public class Preset {

	@Getter
    public String presetName;
	@Getter
    public Location location;

    public Preset(String preset, Location location) {
        this.presetName = preset;
        this.location = location;
    }

    @Override
    public int hashCode() {
        return presetName.hashCode() *
                (location == null ? 1 : location.toDbString().toLowerCase().hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (!obj.getClass().equals(this.getClass())) return false;
        Preset preset = (Preset) obj;
        return preset.presetName.equals(this.presetName) && preset.location.equals(this.location);
    }
}
