package com.example.campusnavigation;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.campusnavigation.data.local.AppDatabase;
import com.example.campusnavigation.data.repository.AuthRepository;
import com.example.campusnavigation.data.repository.BuildingRepository;
import com.example.campusnavigation.data.repository.EventRepository;
import com.example.campusnavigation.data.repository.PreferencesRepository;
import com.example.campusnavigation.util.NotificationHelper;

public class CampusNavigationApp extends Application {
    private static CampusNavigationApp instance;
    private AppDatabase database;
    private BuildingRepository buildingRepository;
    private EventRepository eventRepository;
    private AuthRepository authRepository;
    private PreferencesRepository preferencesRepository;

    public static CampusNavigationApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        database = AppDatabase.getInstance(this);
        preferencesRepository = new PreferencesRepository(this);
        AppCompatDelegate.setDefaultNightMode(preferencesRepository.isDarkModeEnabled()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
        NotificationHelper.createChannels(this);
        authRepository = new AuthRepository();
        buildingRepository = new BuildingRepository(this, database, preferencesRepository);
        eventRepository = new EventRepository(this, database, preferencesRepository);
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public BuildingRepository getBuildingRepository() {
        return buildingRepository;
    }

    public EventRepository getEventRepository() {
        return eventRepository;
    }

    public AuthRepository getAuthRepository() {
        return authRepository;
    }

    public PreferencesRepository getPreferencesRepository() {
        return preferencesRepository;
    }
}
