package com.example.campusnavigation.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

public final class PermissionHelper {
    private PermissionHelper() {
    }

    public static String[] getLocationPermissions() {
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    public static String[] getBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        }
        return new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
    }

    public static String[] getNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.POST_NOTIFICATIONS};
        }
        return new String[0];
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasBluetoothPermissions(Context context) {
        for (String permission : getBluetoothPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isLocationPermissionRequest(String[] permissions) {
        for (String permission : permissions) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)
                    || Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    public static boolean werePermissionsGranted(String[] permissions, java.util.Map<String, Boolean> result) {
        for (String permission : permissions) {
            Boolean granted = result.get(permission);
            if (granted == null || !granted) {
                return false;
            }
        }
        return true;
    }
}
