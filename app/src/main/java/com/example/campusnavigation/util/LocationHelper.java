package com.example.campusnavigation.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;

public class LocationHelper {
    public enum LocationStatus {
        SUCCESS,
        PERMISSION_DENIED,
        UNAVAILABLE,
        ERROR
    }

    public interface LocationResultCallback {
        void onResult(LocationStatus status, @Nullable LatLng location);
    }

    public interface LocationUpdateCallback {
        void onLocationUpdate(LatLng location);
    }

    private final Context context;
    private final FusedLocationProviderClient locationClient;
    @Nullable
    private LocationCallback activeLocationCallback;

    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.locationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void getCurrentLocationOnce(LocationResultCallback callback) {
        if (!PermissionHelper.hasLocationPermission(context)) {
            callback.onResult(LocationStatus.PERMISSION_DENIED, null);
            return;
        }

        fetchCurrentLocation(callback);
    }

    @SuppressLint("MissingPermission")
    private void fetchCurrentLocation(LocationResultCallback callback) {
        CancellationTokenSource cts = new CancellationTokenSource();
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onResult(LocationStatus.SUCCESS,
                                new LatLng(location.getLatitude(), location.getLongitude()));
                    } else {
                        locationClient.getLastLocation()
                                .addOnSuccessListener(lastLoc -> {
                                    if (lastLoc != null) {
                                        callback.onResult(LocationStatus.SUCCESS,
                                                new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude()));
                                    } else {
                                        callback.onResult(LocationStatus.UNAVAILABLE, null);
                                    }
                                })
                                .addOnFailureListener(error -> callback.onResult(LocationStatus.ERROR, null));
                    }
                })
                .addOnFailureListener(error -> callback.onResult(LocationStatus.ERROR, null));
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates(LocationUpdateCallback callback) {
        if (!PermissionHelper.hasLocationPermission(context)) {
            return;
        }

        stopLocationUpdates();

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .build();

        activeLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) {
                    return;
                }
                android.location.Location location = locationResult.getLastLocation();
                callback.onLocationUpdate(new LatLng(location.getLatitude(), location.getLongitude()));
            }
        };

        locationClient.requestLocationUpdates(request, activeLocationCallback, Looper.getMainLooper());
    }

    public void stopLocationUpdates() {
        if (activeLocationCallback != null) {
            locationClient.removeLocationUpdates(activeLocationCallback);
            activeLocationCallback = null;
        }
    }
}
