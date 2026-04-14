package com.example.campusnavigation.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.campusnavigation.R;
import com.example.campusnavigation.util.NotificationHelper;

public class EventReminderWorker extends Worker {
    public static final String KEY_EVENT_NAME = "event_name";
    public static final String KEY_LOCATION = "location";

    public EventReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String eventName = getInputData().getString(KEY_EVENT_NAME);
        String location = getInputData().getString(KEY_LOCATION);
        String body = getApplicationContext().getString(R.string.navigation_to, location == null ? "" : location);
        NotificationHelper.showEventReminder(getApplicationContext(), eventName, body);
        return Result.success();
    }
}
