package com.example.campusnavigation.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.campusnavigation.data.local.entity.BuildingEntity;

import java.util.List;

@Dao
public interface BuildingDao {
    @Query("SELECT * FROM buildings ORDER BY name ASC")
    LiveData<List<BuildingEntity>> observeAll();

    @Query("SELECT * FROM buildings WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    LiveData<List<BuildingEntity>> search(String query);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BuildingEntity> buildings);

    @Query("UPDATE buildings SET favorite = :favorite WHERE id = :buildingId")
    void updateFavorite(String buildingId, boolean favorite);

    @Query("SELECT * FROM buildings WHERE favorite = 1 ORDER BY name ASC")
    LiveData<List<BuildingEntity>> observeFavorites();

    @Query("SELECT * FROM buildings WHERE name LIKE :query || '%' ORDER BY name ASC LIMIT 8")
    List<BuildingEntity> getSuggestions(String query);

    @Query("SELECT * FROM buildings WHERE id = :buildingId LIMIT 1")
    BuildingEntity getById(String buildingId);

    @Query("SELECT COUNT(*) FROM buildings")
    int count();
}
