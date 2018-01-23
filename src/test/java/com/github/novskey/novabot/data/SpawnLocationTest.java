package com.github.novskey.novabot.data;

import com.github.novskey.novabot.core.Config;
import com.github.novskey.novabot.core.Location;
import com.github.novskey.novabot.core.NovaBot;
import com.github.novskey.novabot.core.SuburbManager;
import com.github.novskey.novabot.maps.GeocodedLocation;
import com.github.novskey.novabot.maps.GeofenceIdentifier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;

import static org.mockito.Mockito.*;


public class SpawnLocationTest {
    @Mock
    GeocodedLocation geocodedLocation;
    @Mock
    ArrayList<GeofenceIdentifier> geofenceIdentifiers = new ArrayList<>();
    @Mock
    static NovaBot novaBot;
    @Mock
    Config config;
    @Mock
    HashMap<String, String> locationProps;
    @Mock
    SuburbManager suburbs;
    @InjectMocks
    SpawnLocation spawnLocation = new SpawnLocation(geocodedLocation, geofenceIdentifiers);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        spawnLocation.novaBot = novaBot;
    }

    @Test
    public void testIntersectAll() {

        when(novaBot.suburbsEnabled()).thenReturn(true);
        when(config.getGoogleSuburbField()).thenReturn("city");
        when(locationProps.get("city")).thenReturn("test");
        when(geocodedLocation.getProperties()).thenReturn(locationProps);
        when(novaBot.getConfig()).thenReturn(config);

        boolean result = spawnLocation.intersect(Location.ALL);
        Assert.assertEquals(true, result);
    }

    @Test
    public void testIntersectCityMatches() {

        when(novaBot.suburbsEnabled()).thenReturn(true);
        when(novaBot.getSuburbs()).thenReturn(suburbs);
        when(suburbs.isSuburb("test")).thenReturn(true);
        when(config.getGoogleSuburbField()).thenReturn("city");
        when(config.isAllowAllLocation()).thenReturn(false);
        when(locationProps.get("city")).thenReturn("test");
        when(geocodedLocation.getProperties()).thenReturn(locationProps);
        when(novaBot.getConfig()).thenReturn(config);

        boolean result = spawnLocation.intersect(Location.fromString("test", novaBot));
        Assert.assertEquals(true, result);
    }

    @Test
    public void testIntersectCityNoMatches() {

        when(novaBot.suburbsEnabled()).thenReturn(true);
        when(novaBot.getSuburbs()).thenReturn(suburbs);
        when(suburbs.isSuburb("test")).thenReturn(true);
        when(config.getGoogleSuburbField()).thenReturn("city");
        when(config.isAllowAllLocation()).thenReturn(false);
        when(locationProps.get("city")).thenReturn("test2");
        when(geocodedLocation.getProperties()).thenReturn(locationProps);
        when(novaBot.getConfig()).thenReturn(config);

        boolean result = spawnLocation.intersect(Location.fromString("test", novaBot));
        Assert.assertEquals(false, result);
    }
}
