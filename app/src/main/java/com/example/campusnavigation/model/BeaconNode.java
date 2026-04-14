package com.example.campusnavigation.model;

public class BeaconNode {
    private final String id;
    private final String roomName;
    private final int x;
    private final int y;
    private final int simulatedRssi;

    public BeaconNode(String id, String roomName, int x, int y, int simulatedRssi) {
        this.id = id;
        this.roomName = roomName;
        this.x = x;
        this.y = y;
        this.simulatedRssi = simulatedRssi;
    }

    public String getId() {
        return id;
    }

    public String getRoomName() {
        return roomName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getSimulatedRssi() {
        return simulatedRssi;
    }
}
