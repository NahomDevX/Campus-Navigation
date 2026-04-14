package com.example.campusnavigation.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.campusnavigation.data.local.entity.EventEntity;

import java.util.List;

@Dao
public interface EventDao {
    @Query("SELECT * FROM events ORDER BY eventTimeMillis ASC")
    LiveData<List<EventEntity>> observeAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EventEntity> events);

    @Query("SELECT COUNT(*) FROM events")
    int count();

    @Query("SELECT * FROM events ORDER BY eventTimeMillis ASC")
    List<EventEntity> getAll();
}
