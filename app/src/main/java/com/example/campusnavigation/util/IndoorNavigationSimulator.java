package com.example.campusnavigation.util;

import com.example.campusnavigation.model.BeaconNode;
import com.example.campusnavigation.model.IndoorRoute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IndoorNavigationSimulator {
    private final List<BeaconNode> beaconNodes;
    private int scanIndex = -1;

    public IndoorNavigationSimulator() {
        beaconNodes = Arrays.asList(
                new BeaconNode("b1", "Entrance", 0, 0, -42),
                new BeaconNode("b2", "Registrar", 2, 1, -55),
                new BeaconNode("b3", "Computer Lab", 4, 2, -61),
                new BeaconNode("b4", "Library Wing", 6, 3, -58),
                new BeaconNode("b5", "Lecture Hall A", 8, 1, -63)
        );
    }

    public BeaconNode scanNearestBeacon() {
        scanIndex = (scanIndex + 1) % beaconNodes.size();
        return beaconNodes.get(scanIndex);
    }

    public IndoorRoute buildRoute(String currentRoom, String destinationRoom) {
        List<String> steps = new ArrayList<>();
        steps.add("Start from " + currentRoom);
        steps.add("Follow the main corridor towards the student services junction");
        steps.add("Use the beacon strongest signal handoff to align with " + destinationRoom);
        steps.add("Destination reached: " + destinationRoom);
        return new IndoorRoute(currentRoom, destinationRoom, steps);
    }

    public List<BeaconNode> getBeaconNodes() {
        return beaconNodes;
    }
}
