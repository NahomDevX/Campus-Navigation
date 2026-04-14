package com.example.campusnavigation.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.campusnavigation.data.local.dao.BuildingDao;
import com.example.campusnavigation.data.local.dao.EventDao;
import com.example.campusnavigation.data.local.entity.BuildingEntity;
import com.example.campusnavigation.data.local.entity.EventEntity;

@Database(entities = {BuildingEntity.class, EventEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "campus_navigation.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }

    public abstract BuildingDao buildingDao();

    public abstract EventDao eventDao();
}
