package com.xshards;

import org.bukkit.Location;

public class AfkData {
    private Location lastLocation;

    public AfkData() {
        this.lastLocation = null; // Default to null
    }

    // Constructor to initialize with a location
    public AfkData(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    // Method to check if lastLocation is set
    public boolean hasLocation() {
        return lastLocation != null;
    }

    // Optionally override toString for easier debugging
    @Override
    public String toString() {
        return "AfkData{" +
                "lastLocation=" + lastLocation +
                '}';
    }
}