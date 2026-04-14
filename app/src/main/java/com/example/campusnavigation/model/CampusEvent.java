package com.example.campusnavigation.model;

import com.google.firebase.firestore.DocumentId;

public class CampusEvent {
    @DocumentId
    private String id;
    private String name;
    private String buildingId;
    private String buildingName;
    private long eventTimeMillis;
    private String description;

    public CampusEvent() {
    }

    public CampusEvent(String id, String name, String buildingId, String buildingName, long eventTimeMillis, String description) {
        this.id = id;
        this.name = name;
        this.buildingId = buildingId;
        this.buildingName = buildingName;
        this.eventTimeMillis = eventTimeMillis;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(String buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public long getEventTimeMillis() {
        return eventTimeMillis;
    }

    public void setEventTimeMillis(long eventTimeMillis) {
        this.eventTimeMillis = eventTimeMillis;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return name + " at " + buildingName;
    }
}
