package com.example.campusnavigation.ui.directory;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.campusnavigation.R;
import com.example.campusnavigation.ui.adapter.BuildingAdapter;
import com.example.campusnavigation.ui.common.NavigationHost;
import com.example.campusnavigation.viewmodel.BuildingViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class DirectoryFragment extends Fragment {
    public static DirectoryFragment newInstance() {
        return new DirectoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_directory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextInputEditText searchEditText = view.findViewById(R.id.searchEditText);
        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.directorySwipeRefresh);
        RecyclerView recyclerView = view.findViewById(R.id.buildingsRecyclerView);
        BuildingViewModel viewModel = new ViewModelProvider(requireActivity()).get(BuildingViewModel.class);
        BuildingAdapter adapter = new BuildingAdapter(new BuildingAdapter.Listener() {
            @Override
            public void onBuildingSelected(com.example.campusnavigation.model.Building building) {
                if (requireActivity() instanceof NavigationHost) {
                    ((NavigationHost) requireActivity()).openMapForBuilding(building);
                }
            }

            @Override
            public void onFavoriteToggled(com.example.campusnavigation.model.Building building) {
                viewModel.toggleFavorite(building);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        viewModel.sync();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        swipeRefreshLayout.setOnRefreshListener(viewModel::sync);
        viewModel.getBuildings().observe(getViewLifecycleOwner(), resource -> {
            adapter.submitList(resource.getData());
            swipeRefreshLayout.setRefreshing(false);
        });
        viewModel.getSyncMessage().observe(getViewLifecycleOwner(), message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
    }
}
