package com.example.campusnavigation.model;

import com.google.firebase.firestore.DocumentId;

public class Building {
    @DocumentId
    private String id;
    private String name;
    private String description;
    private String type;
    private double latitude;
    private double longitude;
    private boolean favorite;

    public Building() {
    }

    public Building(String id, String name, String description, String type, double latitude, double longitude, boolean favorite) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.favorite = favorite;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    @Override
    public String toString() {
        return name + " • " + type;
    }
}
