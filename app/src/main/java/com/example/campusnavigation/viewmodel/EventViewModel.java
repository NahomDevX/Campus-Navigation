package com.example.campusnavigation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.campusnavigation.CampusNavigationApp;
import com.example.campusnavigation.data.repository.EventRepository;
import com.example.campusnavigation.model.CampusEvent;
import com.example.campusnavigation.util.Resource;

import java.util.List;

public class EventViewModel extends ViewModel {
    private final EventRepository repository = CampusNavigationApp.getInstance().getEventRepository();
    private final LiveData<Resource<List<CampusEvent>>> events = repository.observeEvents();
    private final MutableLiveData<String> syncMessage = new MutableLiveData<>();
    private final LiveData<List<CampusEvent>> activeEvents = Transformations.map(events, resource -> {
        java.util.ArrayList<CampusEvent> results = new java.util.ArrayList<>();
        if (resource == null || resource.getData() == null) {
            return results;
        }
        long now = System.currentTimeMillis();
        long leadWindow = java.util.concurrent.TimeUnit.HOURS.toMillis(2);
        long tailWindow = java.util.concurrent.TimeUnit.MINUTES.toMillis(45);
        for (CampusEvent event : resource.getData()) {
            long delta = event.getEventTimeMillis() - now;
            if (delta <= leadWindow && delta >= -tailWindow) {
                results.add(event);
            }
        }
        return results;
    });

    public LiveData<Resource<List<CampusEvent>>> getEvents() {
        return events;
    }

    public LiveData<List<CampusEvent>> getActiveEvents() {
        return activeEvents;
    }

    public LiveData<String> getSyncMessage() {
        return syncMessage;
    }

    public void sync() {
        repository.syncEvents((success, message) -> syncMessage.postValue(message));
    }
}
