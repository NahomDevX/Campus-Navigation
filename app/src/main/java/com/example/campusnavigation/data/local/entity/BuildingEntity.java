package com.example.campusnavigation.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "buildings")
public class BuildingEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String description;
    public String type;
    public double latitude;
    public double longitude;
    public boolean favorite;
}
