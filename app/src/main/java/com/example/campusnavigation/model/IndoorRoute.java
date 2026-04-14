package com.example.campusnavigation.model;

import java.util.List;

public class IndoorRoute {
    private final String currentRoom;
    private final String destinationRoom;
    private final List<String> routeSteps;

    public IndoorRoute(String currentRoom, String destinationRoom, List<String> routeSteps) {
        this.currentRoom = currentRoom;
        this.destinationRoom = destinationRoom;
        this.routeSteps = routeSteps;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public String getDestinationRoom() {
        return destinationRoom;
    }

    public List<String> getRouteSteps() {
        return routeSteps;
    }
}
