package com.example.campusnavigation.ui.map;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusnavigation.R;
import com.example.campusnavigation.model.Building;
import com.example.campusnavigation.model.CampusEvent;
import com.example.campusnavigation.ui.common.NavigationHost;
import com.example.campusnavigation.util.CampusRouteCalculator;
import com.example.campusnavigation.util.LocationHelper;
import com.example.campusnavigation.util.PermissionHelper;
import com.example.campusnavigation.util.RouteProvider;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final String ARG_SELECTED_BUILDING_ID = "selected_building_id";
    private static final LatLng DIRE_DAWA_CAMPUS = new LatLng(9.620186228099227, 41.84080857019687);
    private static final float DEVIATION_THRESHOLD_METERS = 35f;
    private static final float WALKING_SPEED_METERS_PER_MINUTE = 70f;

    private GoogleMap googleMap;
    private AutoCompleteTextView searchBar;
    private View defaultUiContainer;
    private View navigationUiContainer;
    private View locationPermissionContainer;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView routeDestinationText;
    private TextView routeEtaText;
    private TextView routeInstructionsText;
    private MaterialButton startRouteButton;
    private TextView navDestinationText;
    private TextView navEtaText;
    private TextView navInstructionsText;
    private MaterialButton endNavigationButton;
    private MaterialButton enableLocationButton;
    private FloatingActionButton myLocationFab;
    private View rootView;
    private BuildingViewModel buildingViewModel;
    private EventViewModel eventViewModel;
    private LocationHelper locationHelper;
    private RouteProvider routeProvider;
    private Building selectedBuilding;
    private Polyline currentPolyline;
    private final List<Building> cachedBuildings = new ArrayList<>();
    private final List<CampusEvent> activeEvents = new ArrayList<>();
    private final List<Building> suggestionBuildings = new ArrayList<>();
    private final Set<String> activeFilters = new HashSet<>();
    private ArrayAdapter<String> suggestionAdapter;
    private boolean pendingDirectoryFocus = false;
    private boolean isNavigating = false;
    private boolean isRerouting = false;
    private List<LatLng> activeRoute = new ArrayList<>();
    private LatLng navigationDestination;
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (PermissionHelper.hasLocationPermission(requireContext())) {
                    enableLocationIfPossible();
                } else {
                    showLocationPermissionSnackbar();
                }
            });

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
        rootView = view;
        searchBar = view.findViewById(R.id.mapSearchBar);
        defaultUiContainer = view.findViewById(R.id.defaultUiContainer);
        navigationUiContainer = view.findViewById(R.id.navigationUiContainer);
        locationPermissionContainer = view.findViewById(R.id.locationPermissionContainer);
        
        View bottomSheet = view.findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        routeDestinationText = view.findViewById(R.id.routeDestinationText);
        routeEtaText = view.findViewById(R.id.routeEtaText);
        routeInstructionsText = view.findViewById(R.id.routeInstructionsText);
        startRouteButton = view.findViewById(R.id.startRouteButton);
        
        navDestinationText = view.findViewById(R.id.navDestinationText);
        navEtaText = view.findViewById(R.id.navEtaText);
        navInstructionsText = view.findViewById(R.id.navInstructionsText);
        endNavigationButton = view.findViewById(R.id.endNavigationButton);
        
        enableLocationButton = view.findViewById(R.id.enableLocationButton);
        myLocationFab = view.findViewById(R.id.myLocationFab);

        locationHelper = new LocationHelper(requireContext());
        routeProvider = new RouteProvider(requireContext());

        setupFilters(view);

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
        endNavigationButton.setOnClickListener(v -> endNavigationMode());
        enableLocationButton.setOnClickListener(v -> locationPermissionLauncher.launch(PermissionHelper.getLocationPermissions()));

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
    public void onPause() {
        stopNavigationUpdates();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        stopNavigationUpdates();
        locationHelper = null;
        routeProvider = null;
        rootView = null;
        super.onDestroyView();
    }

    private void setupFilters(View view) {
        Chip chipAcademic = view.findViewById(R.id.chipAcademic);
        Chip chipLibrary = view.findViewById(R.id.chipLibrary);

        activeFilters.add("academic");
        activeFilters.add("library");

        chipAcademic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                activeFilters.add("academic");
            } else {
                activeFilters.remove("academic");
            }
            renderMapState();
        });

        chipLibrary.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                activeFilters.add("library");
            } else {
                activeFilters.remove("library");
            }
            renderMapState();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DIRE_DAWA_CAMPUS, 17f));
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

        myLocationFab.setVisibility(View.VISIBLE);
        enableLocationIfPossible();
        renderMapState();
    }

    @SuppressWarnings("MissingPermission")
    private void enableLocationIfPossible() {
        if (googleMap == null) {
            return;
        }
        if (PermissionHelper.hasLocationPermission(requireContext())) {
            googleMap.setMyLocationEnabled(true);
            if (locationPermissionContainer != null) {
                locationPermissionContainer.setVisibility(View.GONE);
            }
        }
    }

    private void centerOnMyLocation() {
        if (googleMap == null || locationHelper == null) {
            return;
        }
        if (!ensureLocationPermission()) {
            return;
        }

        locationHelper.getCurrentLocationOnce((status, userLocation) -> {
            if (!isAdded() || googleMap == null) {
                return;
            }
            switch (status) {
                case SUCCESS:
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17f));
                    break;
                case PERMISSION_DENIED:
                    showLocationPermissionSnackbar();
                    break;
                case UNAVAILABLE:
                case ERROR:
                default:
                    showLocationUnavailableSnackbar();
                    break;
            }
        });
    }

    private void renderMapState() {
        if (googleMap == null) {
            return;
        }
        googleMap.clear();
        currentPolyline = null;
        for (Building building : cachedBuildings) {
            if (!activeFilters.isEmpty() && !matchesFilter(building.getType())) {
                continue;
            }

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
            boolean animate = pendingDirectoryFocus;
            pendingDirectoryFocus = false;
            updateRoutePreview(selectedBuilding, animate);
        }
    }

    private boolean matchesFilter(String buildingType) {
        if (buildingType == null) {
            return false;
        }
        return activeFilters.contains(buildingType.toLowerCase(Locale.US));
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
                pendingDirectoryFocus = true;
                searchBar.setText(building.getName(), false);
                break;
            }
        }
    }

    private void clearSelection() {
        stopNavigationUpdates();
        selectedBuilding = null;
        navigationDestination = null;
        activeRoute.clear();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
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
        if (googleMap == null || routeProvider == null) {
            return;
        }
        LatLng destination = new LatLng(building.getLatitude(), building.getLongitude());
        if (animateCamera) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destination, 17f));
        }

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        routeDestinationText.setText(getString(R.string.navigation_to, building.getName()));

        if (!PermissionHelper.hasLocationPermission(requireContext())) {
            showDestinationOnlyPreview(building, destination);
            return;
        }

        locationHelper.getCurrentLocationOnce((status, userLocation) -> {
            if (!isAdded() || googleMap == null || selectedBuilding == null
                    || !selectedBuilding.getId().equals(building.getId())) {
                return;
            }

            switch (status) {
                case SUCCESS:
                    drawRoute(userLocation, destination, building.getName(), false);
                    break;
                case PERMISSION_DENIED:
                    showDestinationOnlyPreview(building, destination);
                    showLocationPermissionSnackbar();
                    break;
                case UNAVAILABLE:
                case ERROR:
                default:
                    showDestinationOnlyPreview(building, destination);
                    showLocationUnavailableSnackbar();
                    break;
            }
        });
    }

    private void showDestinationOnlyPreview(Building building, LatLng destination) {
        if (currentPolyline != null) {
            currentPolyline.remove();
            currentPolyline = null;
        }
        activeRoute.clear();
        routeEtaText.setText(getString(R.string.location_using_campus_preview));
        routeInstructionsText.setText(getString(R.string.navigation_to, building.getName()));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destination, 17f));
    }

    private void drawRoute(LatLng start, LatLng destination, String buildingName, boolean navigating) {
        routeProvider.fetchRoute(start, destination, result -> {
            if (!isAdded() || googleMap == null) {
                return;
            }

            activeRoute = new ArrayList<>(result.getPath());

            if (currentPolyline != null) {
                currentPolyline.remove();
            }
            currentPolyline = googleMap.addPolyline(new PolylineOptions()
                    .addAll(activeRoute)
                    .color(Color.BLUE)
                    .width(12f)
                    .geodesic(true)
                    .jointType(com.google.android.gms.maps.model.JointType.ROUND)
                    .startCap(new com.google.android.gms.maps.model.RoundCap())
                    .endCap(new com.google.android.gms.maps.model.RoundCap()));

            int minutes = result.getDurationSeconds() > 0
                    ? (int) Math.ceil(result.getDurationSeconds() / 60d)
                    : (int) Math.ceil(result.getDistanceMeters() / WALKING_SPEED_METERS_PER_MINUTE);
            routeEtaText.setText(getString(R.string.eta) + ": " + minutes + " min walk");
            routeInstructionsText.setText(buildInstructionPreview(start, destination, buildingName, navigating, activeRoute));
        });
    }

    private void startNavigationMode() {
        if (selectedBuilding == null || googleMap == null || locationHelper == null) {
            return;
        }
        if (!ensureLocationPermission()) {
            return;
        }

        LatLng destination = new LatLng(selectedBuilding.getLatitude(), selectedBuilding.getLongitude());
        locationHelper.getCurrentLocationOnce((status, userLocation) -> {
            if (!isAdded() || googleMap == null || selectedBuilding == null) {
                return;
            }

            switch (status) {
                case SUCCESS:
                    beginNavigationSession(userLocation, destination, selectedBuilding.getName());
                    break;
                case PERMISSION_DENIED:
                    showLocationPermissionSnackbar();
                    break;
                case UNAVAILABLE:
                case ERROR:
                default:
                    showLocationUnavailableSnackbar();
                    break;
            }
        });
    }

    private void beginNavigationSession(LatLng userLocation, LatLng destination, String buildingName) {
        navigationDestination = destination;
        isNavigating = true;

        defaultUiContainer.setVisibility(View.GONE);
        navigationUiContainer.setVisibility(View.VISIBLE);
        navDestinationText.setText(buildingName);
        
        if (requireActivity() instanceof NavigationHost) {
            ((NavigationHost) requireActivity()).setBottomNavigationVisibility(false);
        }

        drawRoute(userLocation, destination, buildingName, true);
        updateNavigationCamera(userLocation, destination);
        Toast.makeText(requireContext(), R.string.navigation_mode_enabled, Toast.LENGTH_SHORT).show();
        locationHelper.startLocationUpdates(this::onNavigationLocationUpdate);
    }
    
    private void endNavigationMode() {
        stopNavigationUpdates();
        
        navigationUiContainer.setVisibility(View.GONE);
        defaultUiContainer.setVisibility(View.VISIBLE);
        
        if (requireActivity() instanceof NavigationHost) {
            ((NavigationHost) requireActivity()).setBottomNavigationVisibility(true);
        }
        
        if (selectedBuilding != null) {
            updateRoutePreview(selectedBuilding, true);
        } else {
            clearSelection();
        }
    }

    private void onNavigationLocationUpdate(LatLng userLocation) {
        if (!isAdded() || googleMap == null || !isNavigating || navigationDestination == null) {
            return;
        }

        updateNavigationCamera(userLocation, navigationDestination);

        double remainingDistance = activeRoute.isEmpty()
                ? calculateDistance(userLocation, navigationDestination)
                : CampusRouteCalculator.remainingDistanceAlongPath(userLocation, activeRoute);
        int minutes = (int) Math.ceil(remainingDistance / WALKING_SPEED_METERS_PER_MINUTE);
        navEtaText.setText(getString(R.string.eta) + ": " + minutes + " min walk");

        String buildingName = selectedBuilding != null ? selectedBuilding.getName() : "";
        navInstructionsText.setText(buildInstructionPreview(
                userLocation,
                navigationDestination,
                buildingName,
                true,
                activeRoute));

        if (!activeRoute.isEmpty()
                && CampusRouteCalculator.distanceToPolyline(userLocation, activeRoute) > DEVIATION_THRESHOLD_METERS) {
            rerouteFromCurrentLocation(userLocation);
        }
    }

    private void rerouteFromCurrentLocation(LatLng userLocation) {
        if (isRerouting || navigationDestination == null || routeProvider == null || selectedBuilding == null) {
            return;
        }
        isRerouting = true;
        if (rootView != null) {
            Snackbar.make(rootView, R.string.route_rerouting, Snackbar.LENGTH_SHORT).show();
        }

        routeProvider.fetchRoute(userLocation, navigationDestination, result -> {
            isRerouting = false;
            if (!isAdded() || googleMap == null || !isNavigating) {
                return;
            }

            activeRoute = new ArrayList<>(result.getPath());

            if (currentPolyline != null) {
                currentPolyline.remove();
            }
            currentPolyline = googleMap.addPolyline(new PolylineOptions()
                    .addAll(activeRoute)
                    .color(Color.BLUE)
                    .width(12f)
                    .geodesic(true)
                    .jointType(com.google.android.gms.maps.model.JointType.ROUND)
                    .startCap(new com.google.android.gms.maps.model.RoundCap())
                    .endCap(new com.google.android.gms.maps.model.RoundCap()));
        });
    }

    private void updateNavigationCamera(LatLng userLocation, LatLng destination) {
        LatLng bearingTarget = destination;
        if (!activeRoute.isEmpty()) {
            int segmentIndex = CampusRouteCalculator.nearestSegmentIndex(userLocation, activeRoute);
            if (segmentIndex + 1 < activeRoute.size()) {
                bearingTarget = activeRoute.get(segmentIndex + 1);
            }
        }

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(userLocation)
                .zoom(19f)
                .tilt(55f)
                .bearing(calculateBearing(userLocation, bearingTarget))
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void stopNavigationUpdates() {
        if (locationHelper != null) {
            locationHelper.stopLocationUpdates();
        }
        isNavigating = false;
        isRerouting = false;
    }

    private void navigateToEvent(CampusEvent event) {
        Building building = findBuildingForEvent(event);
        if (building == null) {
            return;
        }
        focusOnBuilding(building, true);

        if (!PermissionHelper.hasLocationPermission(requireContext()) || locationHelper == null) {
            routeInstructionsText.setText(event.getName() + "\n" + getString(R.string.location_using_campus_preview));
            return;
        }

        locationHelper.getCurrentLocationOnce((status, userLocation) -> {
            if (!isAdded() || selectedBuilding == null) {
                return;
            }
            LatLng destination = new LatLng(building.getLatitude(), building.getLongitude());
            if (status == LocationHelper.LocationStatus.SUCCESS && userLocation != null) {
                routeInstructionsText.setText(event.getName() + "\n"
                        + buildInstructionPreview(userLocation, destination, building.getName(), true, activeRoute));
            } else {
                routeInstructionsText.setText(event.getName() + "\n" + getString(R.string.location_unavailable));
            }
        });
    }

    private boolean ensureLocationPermission() {
        if (PermissionHelper.hasLocationPermission(requireContext())) {
            enableLocationIfPossible();
            return true;
        }
        locationPermissionLauncher.launch(PermissionHelper.getLocationPermissions());
        return false;
    }

    private void showLocationPermissionSnackbar() {
        if (locationPermissionContainer != null) {
            locationPermissionContainer.setVisibility(View.VISIBLE);
        }
    }

    private void showLocationUnavailableSnackbar() {
        if (locationPermissionContainer != null) {
            locationPermissionContainer.setVisibility(View.VISIBLE);
        }
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

    private String buildInstructionPreview(LatLng userLocation, LatLng destination, String buildingName,
                                           boolean detailed, List<LatLng> route) {
        double distanceMeters = route == null || route.isEmpty()
                ? calculateDistance(userLocation, destination)
                : CampusRouteCalculator.remainingDistanceAlongPath(userLocation, route);
        int roundedMeters = (int) Math.round(distanceMeters / 10d) * 10;

        if (!detailed) {
            return "Walk toward " + buildingName + ". Follow the designated paths for about " + roundedMeters + " meters.";
        }

        if (route != null && route.size() >= 2) {
            int segmentIndex = CampusRouteCalculator.nearestSegmentIndex(userLocation, route);
            if (segmentIndex + 1 < route.size()) {
                float segmentBearing = calculateBearing(userLocation, route.get(segmentIndex + 1));
                float destinationBearing = calculateBearing(userLocation, destination);
                float bearingDelta = Math.abs(normalizeBearing(destinationBearing - segmentBearing));
                String turnHint = bearingDelta > 35f ? "Turn toward " + buildingName + ", then continue" : "Continue straight toward " + buildingName;
                return turnHint + " for about " + roundedMeters + " meters to the entrance.";
            }
        }

        return "Walk toward " + buildingName
                + ". Stay on the path and continue for about "
                + roundedMeters + " meters to the entrance.";
    }

    private float normalizeBearing(float bearing) {
        while (bearing > 180f) {
            bearing -= 360f;
        }
        while (bearing < -180f) {
            bearing += 360f;
        }
        return bearing;
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
            case "academic":
                return BitmapDescriptorFactory.HUE_ORANGE;
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
