package com.example.campusnavigation.service;

import androidx.annotation.NonNull;

import com.example.campusnavigation.util.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CampusMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String title = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : "Campus update";
        String body = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : "You have a new campus notification.";
        NotificationHelper.showEventReminder(this, title, body);
    }
}
