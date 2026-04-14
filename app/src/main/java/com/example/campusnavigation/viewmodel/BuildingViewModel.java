package com.example.campusnavigation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.campusnavigation.CampusNavigationApp;
import com.example.campusnavigation.data.repository.BuildingRepository;
import com.example.campusnavigation.model.Building;
import com.example.campusnavigation.util.Resource;
import com.example.campusnavigation.util.AppExecutors;

import java.util.List;

public class BuildingViewModel extends ViewModel {
    private final BuildingRepository repository = CampusNavigationApp.getInstance().getBuildingRepository();
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final LiveData<Resource<List<Building>>> buildings = Transformations.switchMap(searchQuery, repository::observeBuildings);
    private final MutableLiveData<String> syncMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Building>> suggestions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> offlineMode = new MutableLiveData<>(false);

    public LiveData<Resource<List<Building>>> getBuildings() {
        return buildings;
    }

    public LiveData<List<Building>> getFavorites() {
        return repository.observeFavorites();
    }

    public LiveData<String> getSyncMessage() {
        return syncMessage;
    }

    public LiveData<List<Building>> getSuggestions() {
        return suggestions;
    }

    public LiveData<Boolean> getOfflineMode() {
        return offlineMode;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void sync() {
        repository.syncBuildings((success, message) -> {
            offlineMode.postValue(!success);
            syncMessage.postValue(message);
        });
    }

    public void toggleFavorite(Building building) {
        repository.toggleFavorite(building);
    }

    public void fetchSuggestions(String query) {
        repository.fetchSuggestions(query, results -> suggestions.postValue(results));
    }

    public void findBuildingById(String buildingId, BuildingLookupCallback callback) {
        AppExecutors.io().execute(() -> callback.onResult(repository.findBuildingById(buildingId)));
    }

    public interface BuildingLookupCallback {
        void onResult(Building building);
    }
}
