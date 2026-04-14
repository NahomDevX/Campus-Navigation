package com.example.campusnavigation.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String buildingId;
    public String buildingName;
    public long eventTimeMillis;
    public String description;
}
