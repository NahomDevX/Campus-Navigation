package com.example.campusnavigation.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
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

import java.util.concurrent.atomic.AtomicBoolean;

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

    private static final long SINGLE_FIX_TIMEOUT_MS = 15_000L;

    private final Context context;
    private final FusedLocationProviderClient locationClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private LocationCallback activeLocationCallback;
    @Nullable
    private LocationCallback singleFixCallback;
    @Nullable
    private Runnable singleFixTimeoutRunnable;

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
                        tryLastLocation(callback);
                    }
                })
                .addOnFailureListener(error -> tryLastLocation(callback));
    }

    @SuppressLint("MissingPermission")
    private void tryLastLocation(LocationResultCallback callback) {
        locationClient.getLastLocation()
                .addOnSuccessListener(lastLoc -> {
                    if (lastLoc != null) {
                        callback.onResult(LocationStatus.SUCCESS,
                                new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude()));
                    } else {
                        requestSingleLocationFix(callback);
                    }
                })
                .addOnFailureListener(error -> requestSingleLocationFix(callback));
    }

    @SuppressLint("MissingPermission")
    private void requestSingleLocationFix(LocationResultCallback callback) {
        cancelSingleFixRequest();

        AtomicBoolean delivered = new AtomicBoolean(false);
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .setMaxUpdates(1)
                .build();

        singleFixCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (delivered.compareAndSet(false, true)) {
                    cancelSingleFixRequest();
                    if (locationResult != null && locationResult.getLastLocation() != null) {
                        android.location.Location location = locationResult.getLastLocation();
                        callback.onResult(LocationStatus.SUCCESS,
                                new LatLng(location.getLatitude(), location.getLongitude()));
                    } else {
                        callback.onResult(LocationStatus.UNAVAILABLE, null);
                    }
                }
            }
        };

        locationClient.requestLocationUpdates(request, singleFixCallback, Looper.getMainLooper());

        singleFixTimeoutRunnable = () -> {
            if (delivered.compareAndSet(false, true)) {
                cancelSingleFixRequest();
                callback.onResult(LocationStatus.UNAVAILABLE, null);
            }
        };
        mainHandler.postDelayed(singleFixTimeoutRunnable, SINGLE_FIX_TIMEOUT_MS);
    }

    private void cancelSingleFixRequest() {
        if (singleFixTimeoutRunnable != null) {
            mainHandler.removeCallbacks(singleFixTimeoutRunnable);
            singleFixTimeoutRunnable = null;
        }
        if (singleFixCallback != null) {
            locationClient.removeLocationUpdates(singleFixCallback);
            singleFixCallback = null;
        }
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
        cancelSingleFixRequest();
        if (activeLocationCallback != null) {
            locationClient.removeLocationUpdates(activeLocationCallback);
            activeLocationCallback = null;
        }
    }
}
