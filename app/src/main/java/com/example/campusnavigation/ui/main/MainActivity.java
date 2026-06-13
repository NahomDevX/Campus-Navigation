package com.example.campusnavigation.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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
import com.example.campusnavigation.viewmodel.BuildingViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements NavigationHost {
    private BottomNavigationView bottomNavigationView;
    private final Queue<String[]> pendingPermissions = new LinkedList<>();
    private String[] lastRequestedPermissions;
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionsResult);

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
            handleNavigationIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent);
    }

    private void handleNavigationIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        String destinationId = intent.getStringExtra("building_id");
        if (destinationId == null) {
            destinationId = intent.getStringExtra("destination_id");
        }
        Uri data = intent.getData();
        if (destinationId == null && data != null) {
            destinationId = data.getQueryParameter("building_id");
            if (destinationId == null) {
                destinationId = data.getQueryParameter("destination_id");
            }
        }
        if (TextUtils.isEmpty(destinationId)) {
            return;
        }

        BuildingViewModel buildingViewModel = new ViewModelProvider(this).get(BuildingViewModel.class);
        buildingViewModel.findBuildingById(destinationId, building -> runOnUiThread(() -> {
            if (building != null) {
                openMapForBuilding(building, true);
            } else {
                Toast.makeText(this, R.string.destination_not_found, Toast.LENGTH_LONG).show();
            }
        }));
    }

    private void requestCorePermissions() {
        pendingPermissions.clear();
        if (!PermissionHelper.hasLocationPermission(this)) {
            pendingPermissions.add(PermissionHelper.getLocationPermissions());
        }
        if (!PermissionHelper.hasBluetoothPermissions(this)) {
            pendingPermissions.add(PermissionHelper.getBluetoothPermissions());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !PermissionHelper.hasNotificationPermission(this)) {
            pendingPermissions.add(PermissionHelper.getNotificationPermissions());
        }
        requestNextPermission();
    }

    private void requestNextPermission() {
        String[] next = pendingPermissions.poll();
        if (next != null && next.length > 0) {
            lastRequestedPermissions = next;
            permissionLauncher.launch(next);
        }
    }

    private void onPermissionsResult(Map<String, Boolean> result) {
        if (lastRequestedPermissions != null
                && PermissionHelper.isLocationPermissionRequest(lastRequestedPermissions)
                && !PermissionHelper.werePermissionsGranted(lastRequestedPermissions, result)) {
            showLocationPermissionSnackbar();
        }
        requestNextPermission();
    }

    private void showLocationPermissionSnackbar() {
        Snackbar.make(bottomNavigationView, R.string.location_permission_denied, Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, v -> {
                    pendingPermissions.clear();
                    pendingPermissions.add(PermissionHelper.getLocationPermissions());
                    requestNextPermission();
                })
                .show();
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainNavHost, fragment)
                .commit();
    }

    @Override
    public void openMapForBuilding(Building building) {
        openMapForBuilding(building, false);
    }

    private void openMapForBuilding(Building building, boolean startNavigation) {
        bottomNavigationView.setSelectedItemId(R.id.nav_map);
        switchFragment(MapFragment.newInstance(building, startNavigation));
    }

    @Override
    public void openIndoorNavigation() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainNavHost, IndoorNavigationFragment.newInstance())
                .addToBackStack("indoor")
                .commit();
    }

    @Override
    public void setBottomNavigationVisibility(boolean visible) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(visible ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }
}
