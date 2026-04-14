package com.example.campusnavigation.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.campusnavigation.R;
import com.example.campusnavigation.data.local.AppDatabase;
import com.example.campusnavigation.data.local.entity.BuildingEntity;
import com.example.campusnavigation.data.mapper.BuildingMapper;
import com.example.campusnavigation.model.Building;
import com.example.campusnavigation.util.AppExecutors;
import com.example.campusnavigation.util.Resource;
import com.example.campusnavigation.util.SampleDataLoader;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class BuildingRepository {
    private final Context context;
    private final AppDatabase database;
    private final PreferencesRepository preferencesRepository;
    private final FirebaseFirestore firestore;

    public BuildingRepository(Context context, AppDatabase database, PreferencesRepository preferencesRepository) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.preferencesRepository = preferencesRepository;
        this.firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<Resource<List<Building>>> observeBuildings(String query) {
        MediatorLiveData<Resource<List<Building>>> liveData = new MediatorLiveData<>();
        liveData.setValue(Resource.loading(null));
        LiveData<List<BuildingEntity>> source = query == null || query.trim().isEmpty()
                ? database.buildingDao().observeAll()
                : database.buildingDao().search(query.trim());
        liveData.addSource(source, entities -> liveData.setValue(Resource.success(BuildingMapper.toModels(entities))));
        return liveData;
    }

    public LiveData<List<Building>> observeFavorites() {
        MediatorLiveData<List<Building>> favorites = new MediatorLiveData<>();
        favorites.addSource(database.buildingDao().observeFavorites(), entities -> favorites.setValue(BuildingMapper.toModels(entities)));
        return favorites;
    }

    public void syncBuildings(SyncCallback callback) {
        firestore.collection("buildings").get()
                .addOnSuccessListener(snapshot -> {
                    List<Building> buildings = snapshot.toObjects(Building.class);
                    if (buildings.isEmpty()) {
                        buildings = SampleDataLoader.loadBuildings(context);
                    }
                    List<Building> finalBuildings = buildings;
                    AppExecutors.io().execute(() -> {
                        database.buildingDao().insertAll(BuildingMapper.toEntities(finalBuildings));
                        if (callback != null) {
                            callback.onComplete(true, context.getString(R.string.campus_sync_complete));
                        }
                    });
                })
                .addOnFailureListener(error -> AppExecutors.io().execute(() -> {
                    if (database.buildingDao().count() == 0) {
                        List<Building> offline = SampleDataLoader.loadBuildings(context);
                        if (!offline.isEmpty()) {
                            database.buildingDao().insertAll(BuildingMapper.toEntities(offline));
                        }
                    }
                    if (callback != null) {
                        callback.onComplete(false, context.getString(R.string.campus_sync_failed));
                    }
                }));
    }

    public void toggleFavorite(Building building) {
        AppExecutors.io().execute(() ->
                database.buildingDao().updateFavorite(building.getId(), !building.isFavorite()));
    }

    public void fetchSuggestions(String query, SuggestionsCallback callback) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            callback.onSuggestionsLoaded(java.util.Collections.emptyList());
            return;
        }
        firestore.collection("buildings")
                .orderBy("name")
                .startAt(capitalize(trimmed))
                .endAt(capitalize(trimmed) + "\uf8ff")
                .limit(8)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Building> suggestions = snapshot.toObjects(Building.class);
                    if (!suggestions.isEmpty()) {
                        callback.onSuggestionsLoaded(suggestions);
                    } else {
                        loadOfflineSuggestions(trimmed, callback);
                    }
                })
                .addOnFailureListener(error -> loadOfflineSuggestions(trimmed, callback));
    }

    public Building findBuildingById(String buildingId) {
        BuildingEntity entity = database.buildingDao().getById(buildingId);
        return entity == null ? null : BuildingMapper.toModel(entity);
    }

    private void loadOfflineSuggestions(String query, SuggestionsCallback callback) {
        AppExecutors.io().execute(() -> {
            List<BuildingEntity> entities = database.buildingDao().getSuggestions(query);
            callback.onSuggestionsLoaded(BuildingMapper.toModels(entities));
        });
    }

    private String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

    public interface SyncCallback {
        void onComplete(boolean success, String message);
    }

    public interface SuggestionsCallback {
        void onSuggestionsLoaded(List<Building> suggestions);
    }
}
