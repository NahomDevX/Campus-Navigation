package com.example.campusnavigation.data.mapper;

import com.example.campusnavigation.data.local.entity.EventEntity;
import com.example.campusnavigation.model.CampusEvent;

import java.util.ArrayList;
import java.util.List;

public final class EventMapper {
    private EventMapper() {
    }

    public static EventEntity toEntity(CampusEvent event) {
        EventEntity entity = new EventEntity();
        entity.id = event.getId();
        entity.name = event.getName();
        entity.buildingId = event.getBuildingId();
        entity.buildingName = event.getBuildingName();
        entity.eventTimeMillis = event.getEventTimeMillis();
        entity.description = event.getDescription();
        return entity;
    }

    public static CampusEvent toModel(EventEntity entity) {
        return new CampusEvent(entity.id, entity.name, entity.buildingId, entity.buildingName, entity.eventTimeMillis, entity.description);
    }

    public static List<CampusEvent> toModels(List<EventEntity> entities) {
        List<CampusEvent> models = new ArrayList<>();
        for (EventEntity entity : entities) {
            models.add(toModel(entity));
        }
        return models;
    }

    public static List<EventEntity> toEntities(List<CampusEvent> events) {
        List<EventEntity> entities = new ArrayList<>();
        for (CampusEvent event : events) {
            entities.add(toEntity(event));
        }
        return entities;
    }
}
