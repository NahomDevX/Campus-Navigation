package com.example.campusnavigation.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.campusnavigation.R;
import com.example.campusnavigation.data.local.AppDatabase;
import com.example.campusnavigation.data.local.entity.EventEntity;
import com.example.campusnavigation.data.mapper.EventMapper;
import com.example.campusnavigation.model.CampusEvent;
import com.example.campusnavigation.util.AppExecutors;
import com.example.campusnavigation.util.Resource;
import com.example.campusnavigation.util.SampleDataLoader;
import com.example.campusnavigation.worker.EventReminderWorker;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class EventRepository {
    private final Context context;
    private final AppDatabase database;
    private final PreferencesRepository preferencesRepository;
    private final FirebaseFirestore firestore;

    public EventRepository(Context context, AppDatabase database, PreferencesRepository preferencesRepository) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.preferencesRepository = preferencesRepository;
        this.firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<Resource<List<CampusEvent>>> observeEvents() {
        MediatorLiveData<Resource<List<CampusEvent>>> liveData = new MediatorLiveData<>();
        liveData.addSource(database.eventDao().observeAll(), entities -> liveData.setValue(Resource.success(EventMapper.toModels(entities))));
        return liveData;
    }

    public void syncEvents(BuildingRepository.SyncCallback callback) {
        firestore.collection("events").get()
                .addOnSuccessListener(snapshot -> {
                    List<CampusEvent> events = snapshot.toObjects(CampusEvent.class);
                    if (events.isEmpty()) {
                        events = SampleDataLoader.loadEvents(context);
                    }
                    List<CampusEvent> finalEvents = events;
                    AppExecutors.io().execute(() -> {
                        database.eventDao().insertAll(EventMapper.toEntities(finalEvents));
                        scheduleNotifications(finalEvents);
                        if (callback != null) {
                            callback.onComplete(true, context.getString(R.string.campus_sync_complete));
                        }
                    });
                })
                .addOnFailureListener(error -> AppExecutors.io().execute(() -> {
                    List<CampusEvent> fallback;
                    if (database.eventDao().count() == 0) {
                        fallback = SampleDataLoader.loadEvents(context);
                        database.eventDao().insertAll(EventMapper.toEntities(fallback));
                    } else {
                        fallback = EventMapper.toModels(database.eventDao().getAll());
                    }
                    scheduleNotifications(fallback);
                    if (callback != null) {
                        callback.onComplete(false, context.getString(R.string.campus_sync_failed));
                    }
                }));
    }

    private void scheduleNotifications(List<CampusEvent> events) {
        if (!preferencesRepository.areNotificationsEnabled()) {
            return;
        }
        for (CampusEvent event : events) {
            long delay = event.getEventTimeMillis() - System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30);
            if (delay <= 0) {
                continue;
            }
            Data data = new Data.Builder()
                    .putString(EventReminderWorker.KEY_EVENT_NAME, event.getName())
                    .putString(EventReminderWorker.KEY_LOCATION, event.getBuildingName())
                    .build();
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(EventReminderWorker.class)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag("event_" + event.getId())
                    .build();
            WorkManager.getInstance(context).enqueue(request);
        }
    }
}
