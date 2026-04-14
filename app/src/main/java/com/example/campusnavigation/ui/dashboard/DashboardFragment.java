package com.example.campusnavigation.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusnavigation.R;
import com.example.campusnavigation.ui.adapter.HighlightAdapter;
import com.example.campusnavigation.ui.common.NavigationHost;
import com.example.campusnavigation.util.Resource;
import com.example.campusnavigation.viewmodel.BuildingViewModel;
import com.example.campusnavigation.viewmodel.EventViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {
    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView buildingCountText = view.findViewById(R.id.buildingCountText);
        TextView eventCountText = view.findViewById(R.id.upcomingEventsCountText);
        MaterialButton indoorButton = view.findViewById(R.id.openIndoorNavigationButton);
        RecyclerView recyclerView = view.findViewById(R.id.highlightsRecyclerView);
        HighlightAdapter adapter = new HighlightAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        BuildingViewModel buildingViewModel = new ViewModelProvider(requireActivity()).get(BuildingViewModel.class);
        EventViewModel eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
        buildingViewModel.sync();
        eventViewModel.sync();

        buildingViewModel.getBuildings().observe(getViewLifecycleOwner(), resource -> {
            int count = resource.getData() == null ? 0 : resource.getData().size();
            buildingCountText.setText(String.valueOf(count));
            updateHighlights(adapter, resource, eventViewModel);
        });
        eventViewModel.getEvents().observe(getViewLifecycleOwner(), resource -> {
            int count = resource.getData() == null ? 0 : resource.getData().size();
            eventCountText.setText(String.valueOf(count));
            updateHighlights(adapter, buildingViewModel.getBuildings().getValue(), resource);
        });
        indoorButton.setOnClickListener(v -> {
            if (requireActivity() instanceof NavigationHost) {
                ((NavigationHost) requireActivity()).openIndoorNavigation();
            }
        });
    }

    private void updateHighlights(HighlightAdapter adapter, Resource<?> buildings, EventViewModel eventViewModel) {
        updateHighlights(adapter, buildings, eventViewModel.getEvents().getValue());
    }

    private void updateHighlights(HighlightAdapter adapter, Resource<?> buildingsResource, Resource<?> eventsResource) {
        List<HighlightAdapter.HighlightItem> highlights = new ArrayList<>();
        highlights.add(new HighlightAdapter.HighlightItem("Campus Sync", "Offline cache and Firebase sync are active"));
        if (buildingsResource != null && buildingsResource.getData() instanceof List) {
            List<?> list = (List<?>) buildingsResource.getData();
            if (!list.isEmpty()) {
                highlights.add(new HighlightAdapter.HighlightItem("Popular Destination", list.get(0).toString()));
            }
        }
        if (eventsResource != null && eventsResource.getData() instanceof List && !((List<?>) eventsResource.getData()).isEmpty()) {
            Object item = ((List<?>) eventsResource.getData()).get(0);
            highlights.add(new HighlightAdapter.HighlightItem("Next Event", item.toString()));
        }
        adapter.submitList(highlights);
    }
}
