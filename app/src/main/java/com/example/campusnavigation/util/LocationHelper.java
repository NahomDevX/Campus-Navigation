package com.example.campusnavigation.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;

public class LocationHelper {
    public interface LocationCallback {
        void onLocation(LatLng latLng);
    }

    private final Context context;
    private final FusedLocationProviderClient locationClient;

    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.locationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void getLastLocation(LocationCallback callback, LatLng fallback) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onLocation(fallback);
            return;
        }

        CancellationTokenSource cts = new CancellationTokenSource();
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                    } else {
                        locationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) {
                                callback.onLocation(new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude()));
                            } else {
                                callback.onLocation(fallback);
                            }
                        });
                    }
                })
                .addOnFailureListener(error -> callback.onLocation(fallback));
    }
}
