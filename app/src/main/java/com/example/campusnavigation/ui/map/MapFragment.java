package com.example.campusnavigation.ui.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusnavigation.R;
import com.example.campusnavigation.model.Building;
import com.example.campusnavigation.model.CampusEvent;
import com.example.campusnavigation.util.LocationHelper;
import com.example.campusnavigation.viewmodel.BuildingViewModel;
import com.example.campusnavigation.viewmodel.EventViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final String ARG_SELECTED_BUILDING_ID = "selected_building_id";
    private static final LatLng DIRE_DAWA_CAMPUS = new LatLng(9.5931, 41.8661);

    private GoogleMap googleMap;
    private AutoCompleteTextView searchBar;
    private MaterialCardView routeCard;
    private TextView routeDestinationText;
    private TextView routeEtaText;
    private TextView routeInstructionsText;
    private MaterialButton startRouteButton;
    private FloatingActionButton myLocationFab;
    private BuildingViewModel buildingViewModel;
    private EventViewModel eventViewModel;
    private Building selectedBuilding;
    private Polyline currentPolyline;
    private final List<Building> cachedBuildings = new ArrayList<>();
    private final List<CampusEvent> activeEvents = new ArrayList<>();
    private final List<Building> suggestionBuildings = new ArrayList<>();
    private ArrayAdapter<String> suggestionAdapter;
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> enableLocationIfPossible());

    public static MapFragment newInstance(@Nullable Building building) {
        MapFragment fragment = new MapFragment();
        if (building != null) {
            Bundle args = new Bundle();
            args.putString(ARG_SELECTED_BUILDING_ID, building.getId());
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        searchBar = view.findViewById(R.id.mapSearchBar);
        routeCard = view.findViewById(R.id.routeCard);
        routeDestinationText = view.findViewById(R.id.routeDestinationText);
        routeEtaText = view.findViewById(R.id.routeEtaText);
        routeInstructionsText = view.findViewById(R.id.routeInstructionsText);
        startRouteButton = view.findViewById(R.id.startRouteButton);
        myLocationFab = view.findViewById(R.id.myLocationFab);

        suggestionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        searchBar.setAdapter(suggestionAdapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        buildingViewModel = new ViewModelProvider(requireActivity()).get(BuildingViewModel.class);
        eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
        buildingViewModel.sync();
        eventViewModel.sync();

        buildingViewModel.getBuildings().observe(getViewLifecycleOwner(), resource -> {
            cachedBuildings.clear();
            if (resource != null && resource.getData() != null) {
                cachedBuildings.addAll(resource.getData());
            }
            resolveSelectedBuildingFromArgs();
            renderMapState();
        });
        buildingViewModel.getSuggestions().observe(getViewLifecycleOwner(), this::updateSuggestions);
        eventViewModel.getActiveEvents().observe(getViewLifecycleOwner(), events -> {
            activeEvents.clear();
            if (events != null) {
                activeEvents.addAll(events);
            }
            renderMapState();
        });

        myLocationFab.setOnClickListener(v -> centerOnMyLocation());
        startRouteButton.setOnClickListener(v -> startNavigationMode());

        searchBar.setOnLongClickListener(v -> {
            showMapTypeMenu(v);
            return true;
        });
        searchBar.setOnItemClickListener((parent, itemView, position, id) -> {
            if (position >= 0 && position < suggestionBuildings.size()) {
                focusOnBuilding(suggestionBuildings.get(position), true);
            }
        });
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim();
                if (query.isEmpty()) {
                    clearSelection();
                } else {
                    buildingViewModel.fetchSuggestions(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DIRE_DAWA_CAMPUS, 15f));
        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Building) {
                focusOnBuilding((Building) tag, true);
                return true;
            }
            if (tag instanceof CampusEvent) {
                navigateToEvent((CampusEvent) tag);
                return true;
            }
            return false;
        });

        enableLocationIfPossible();
        renderMapState();
    }

    private void enableLocationIfPossible() {
        if (googleMap == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            myLocationFab.setVisibility(View.VISIBLE);
        } else {
            myLocationFab.setVisibility(View.GONE);
            locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void centerOnMyLocation() {
        if (googleMap == null) {
            return;
        }
        new LocationHelper(requireContext()).getLastLocation(userLocation ->
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17f)), DIRE_DAWA_CAMPUS);
    }

    private void renderMapState() {
        if (googleMap == null) {
            return;
        }
        googleMap.clear();
        currentPolyline = null;
        for (Building building : cachedBuildings) {
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(building.getLatitude(), building.getLongitude()))
                    .title(building.getName())
                    .snippet(building.getDescription())
                    .icon(BitmapDescriptorFactory.defaultMarker(getHueForType(building.getType()))));
            if (marker != null) {
                marker.setTag(building);
            }
        }
        for (CampusEvent event : activeEvents) {
            Building eventBuilding = findBuildingForEvent(event);
            if (eventBuilding == null) {
                continue;
            }
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(eventBuilding.getLatitude(), eventBuilding.getLongitude()))
                    .title(event.getName())
                    .snippet(getString(R.string.active_event) + ": " + eventBuilding.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
            if (marker != null) {
                marker.setTag(event);
            }
        }
        if (selectedBuilding != null) {
            updateRoutePreview(selectedBuilding, false);
        }
    }

    private void updateSuggestions(List<Building> suggestions) {
        suggestionBuildings.clear();
        suggestionAdapter.clear();
        if (suggestions != null) {
            suggestionBuildings.addAll(suggestions);
            for (Building building : suggestions) {
                suggestionAdapter.add(building.getName());
            }
        }
        suggestionAdapter.notifyDataSetChanged();
        if (searchBar.hasFocus() && !suggestionBuildings.isEmpty()) {
            searchBar.showDropDown();
        }
    }

    private void resolveSelectedBuildingFromArgs() {
        if (selectedBuilding != null || getArguments() == null) {
            return;
        }
        String selectedId = getArguments().getString(ARG_SELECTED_BUILDING_ID);
        if (selectedId == null) {
            return;
        }
        for (Building building : cachedBuildings) {
            if (selectedId.equals(building.getId())) {
                selectedBuilding = building;
                searchBar.setText(building.getName(), false);
                break;
            }
        }
    }

    private void clearSelection() {
        selectedBuilding = null;
        routeCard.setVisibility(View.GONE);
        routeInstructionsText.setText("");
        if (currentPolyline != null) {
            currentPolyline.remove();
            currentPolyline = null;
        }
    }

    private void focusOnBuilding(Building building, boolean animateCamera) {
        selectedBuilding = building;
        searchBar.setText(building.getName(), false);
        updateRoutePreview(building, animateCamera);
    }

    private void updateRoutePreview(Building building, boolean animateCamera) {
        if (googleMap == null) {
            return;
        }
        LatLng destination = new LatLng(building.getLatitude(), building.getLongitude());
        if (animateCamera) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destination, 17f));
        }
        new LocationHelper(requireContext()).getLastLocation(userLocation -> {
            if (currentPolyline != null) {
                currentPolyline.remove();
            }
            currentPolyline = googleMap.addPolyline(new PolylineOptions()
                    .add(userLocation, destination)
                    .color(Color.BLUE)
                    .width(10f)
                    .geodesic(true));
            routeCard.setVisibility(View.VISIBLE);
            routeDestinationText.setText(getString(R.string.navigation_to, building.getName()));
            double distance = calculateDistance(userLocation, destination);
            int minutes = (int) Math.ceil(distance / 80d);
            routeEtaText.setText(getString(R.string.eta) + ": " + minutes + " min walk");
            routeInstructionsText.setText(buildInstructionPreview(userLocation, destination, building.getName(), false));
        }, DIRE_DAWA_CAMPUS);
    }

    private void startNavigationMode() {
        if (selectedBuilding == null || googleMap == null) {
            return;
        }
        LatLng destination = new LatLng(selectedBuilding.getLatitude(), selectedBuilding.getLongitude());
        new LocationHelper(requireContext()).getLastLocation(userLocation -> {
            routeInstructionsText.setText(buildInstructionPreview(userLocation, destination, selectedBuilding.getName(), true));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(userLocation)
                    .zoom(19f)
                    .tilt(55f)
                    .bearing(calculateBearing(userLocation, destination))
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            Toast.makeText(requireContext(), R.string.navigation_mode_enabled, Toast.LENGTH_SHORT).show();
        }, DIRE_DAWA_CAMPUS);
    }

    private void navigateToEvent(CampusEvent event) {
        Building building = findBuildingForEvent(event);
        if (building == null) {
            return;
        }
        focusOnBuilding(building, true);
        routeInstructionsText.setText(event.getName() + "\n" + buildInstructionPreview(
                DIRE_DAWA_CAMPUS,
                new LatLng(building.getLatitude(), building.getLongitude()),
                building.getName(),
                true));
    }

    private Building findBuildingForEvent(CampusEvent event) {
        for (Building building : cachedBuildings) {
            if (!TextUtils.isEmpty(event.getBuildingId()) && event.getBuildingId().equals(building.getId())) {
                return building;
            }
            if (!TextUtils.isEmpty(event.getBuildingName()) && event.getBuildingName().equalsIgnoreCase(building.getName())) {
                return building;
            }
        }
        return null;
    }

    private String buildInstructionPreview(LatLng userLocation, LatLng destination, String buildingName, boolean detailed) {
        double distanceMeters = calculateDistance(userLocation, destination);
        int roundedMeters = (int) Math.round(distanceMeters / 10d) * 10;
        String direction = roundedMeters > 250 ? "Head across campus toward " : "Continue toward ";
        if (!detailed) {
            return direction + buildingName + ". Stay on the main pedestrian paths for about " + roundedMeters + " meters.";
        }
        return direction + buildingName
                + ". Follow the blue line, keep the destination ahead of you, and continue for about "
                + roundedMeters + " meters before turning into the building entrance.";
    }

    private double calculateDistance(LatLng start, LatLng end) {
        float[] results = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        return results[0];
    }

    private float calculateBearing(LatLng start, LatLng end) {
        Location startLocation = new Location("start");
        startLocation.setLatitude(start.latitude);
        startLocation.setLongitude(start.longitude);
        Location endLocation = new Location("end");
        endLocation.setLatitude(end.latitude);
        endLocation.setLongitude(end.longitude);
        return startLocation.bearingTo(endLocation);
    }

    private float getHueForType(String type) {
        if (type == null) {
            return BitmapDescriptorFactory.HUE_RED;
        }
        switch (type.toLowerCase(Locale.US)) {
            case "library":
                return BitmapDescriptorFactory.HUE_AZURE;
            case "cafeteria":
                return BitmapDescriptorFactory.HUE_ORANGE;
            case "department":
                return BitmapDescriptorFactory.HUE_GREEN;
            default:
                return BitmapDescriptorFactory.HUE_RED;
        }
    }

    private void showMapTypeMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add(0, 1, 0, "Normal");
        popupMenu.getMenu().add(0, 2, 1, "Satellite");
        popupMenu.getMenu().add(0, 3, 2, "Terrain");
        popupMenu.setOnMenuItemClickListener(item -> {
            if (googleMap == null) {
                return false;
            }
            if (item.getItemId() == 1) {
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            } else if (item.getItemId() == 2) {
                googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            } else if (item.getItemId() == 3) {
                googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            }
            return true;
        });
        popupMenu.show();
    }
}
