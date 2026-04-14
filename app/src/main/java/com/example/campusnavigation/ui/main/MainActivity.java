package com.example.campusnavigation.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.campusnavigation.R;
import com.example.campusnavigation.model.Building;
import com.example.campusnavigation.ui.common.NavigationHost;
import com.example.campusnavigation.ui.dashboard.DashboardFragment;
import com.example.campusnavigation.ui.directory.DirectoryFragment;
import com.example.campusnavigation.ui.events.EventsFragment;
import com.example.campusnavigation.ui.indoor.IndoorNavigationFragment;
import com.example.campusnavigation.ui.map.MapFragment;
import com.example.campusnavigation.ui.settings.SettingsFragment;
import com.example.campusnavigation.util.PermissionHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements NavigationHost {
    private BottomNavigationView bottomNavigationView;
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> { });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_dashboard) {
                switchFragment(DashboardFragment.newInstance());
            } else if (item.getItemId() == R.id.nav_map) {
                switchFragment(MapFragment.newInstance(null));
            } else if (item.getItemId() == R.id.nav_directory) {
                switchFragment(DirectoryFragment.newInstance());
            } else if (item.getItemId() == R.id.nav_events) {
                switchFragment(EventsFragment.newInstance());
            } else if (item.getItemId() == R.id.nav_settings) {
                switchFragment(SettingsFragment.newInstance());
            }
            return true;
        });
        requestCorePermissions();
        if (savedInstanceState == null) {
            boolean openEvents = getIntent().getBooleanExtra("open_events", false);
            bottomNavigationView.setSelectedItemId(openEvents ? R.id.nav_events : R.id.nav_dashboard);
        }
    }

    private void requestCorePermissions() {
        permissionLauncher.launch(PermissionHelper.getLocationPermissions());
        permissionLauncher.launch(PermissionHelper.getBluetoothPermissions());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(PermissionHelper.getNotificationPermissions());
        }
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainNavHost, fragment)
                .commit();
    }

    @Override
    public void openMapForBuilding(Building building) {
        bottomNavigationView.setSelectedItemId(R.id.nav_map);
        switchFragment(MapFragment.newInstance(building));
    }

    @Override
    public void openIndoorNavigation() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainNavHost, IndoorNavigationFragment.newInstance())
                .addToBackStack("indoor")
                .commit();
    }
}
