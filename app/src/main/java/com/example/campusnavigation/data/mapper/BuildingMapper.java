package com.example.campusnavigation.data.mapper;

import com.example.campusnavigation.data.local.entity.BuildingEntity;
import com.example.campusnavigation.model.Building;

import java.util.ArrayList;
import java.util.List;

public final class BuildingMapper {
    private BuildingMapper() {
    }

    public static BuildingEntity toEntity(Building building) {
        BuildingEntity entity = new BuildingEntity();
        entity.id = building.getId();
        entity.name = building.getName();
        entity.description = building.getDescription();
        entity.type = building.getType();
        entity.latitude = building.getLatitude();
        entity.longitude = building.getLongitude();
        entity.favorite = building.isFavorite();
        return entity;
    }

    public static Building toModel(BuildingEntity entity) {
        return new Building(entity.id, entity.name, entity.description, entity.type, entity.latitude, entity.longitude, entity.favorite);
    }

    public static List<Building> toModels(List<BuildingEntity> entities) {
        List<Building> models = new ArrayList<>();
        for (BuildingEntity entity : entities) {
            models.add(toModel(entity));
        }
        return models;
    }

    public static List<BuildingEntity> toEntities(List<Building> models) {
        List<BuildingEntity> entities = new ArrayList<>();
        for (Building model : models) {
            entities.add(toEntity(model));
        }
        return entities;
    }
}
