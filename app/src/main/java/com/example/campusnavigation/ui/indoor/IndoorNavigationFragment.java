package com.example.campusnavigation.ui.indoor;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusnavigation.R;
import com.example.campusnavigation.model.BeaconNode;
import com.example.campusnavigation.viewmodel.IndoorNavigationViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class IndoorNavigationFragment extends Fragment {
    private static final int MAX_BEACON_X = 8;
    private static final int MAX_BEACON_Y = 3;

    private IndoorNavigationViewModel viewModel;
    private View blueDotView;
    private FrameLayout indoorMapFrame;
    private TextView detectedRoomText;
    private TextInputEditText currentRoomInput;

    public static IndoorNavigationFragment newInstance() {
        return new IndoorNavigationFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_indoor_navigation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(IndoorNavigationViewModel.class);
        currentRoomInput = view.findViewById(R.id.currentRoomInput);
        TextInputEditText destinationRoomInput = view.findViewById(R.id.destinationRoomInput);
        TextView beaconStatusText = view.findViewById(R.id.beaconStatusText);
        TextView routeText = view.findViewById(R.id.indoorRouteText);
        detectedRoomText = view.findViewById(R.id.detectedRoomText);
        indoorMapFrame = view.findViewById(R.id.indoorMapFrame);
        blueDotView = view.findViewById(R.id.blueDotView);
        MaterialButton scanButton = view.findViewById(R.id.scanButton);
        MaterialButton startButton = view.findViewById(R.id.startIndoorNavigationButton);

        scanButton.setOnClickListener(v -> viewModel.scan());
        startButton.setOnClickListener(v -> {
            String current = valueOf(currentRoomInput);
            String destination = valueOf(destinationRoomInput);
            if (TextUtils.isEmpty(current) || TextUtils.isEmpty(destination)) {
                Toast.makeText(requireContext(), R.string.enter_rooms, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.startNavigation(current, destination);
        });

        viewModel.getBeaconStatus().observe(getViewLifecycleOwner(), beaconStatusText::setText);
        viewModel.getCurrentNode().observe(getViewLifecycleOwner(), node -> {
            if (node == null) {
                return;
            }
            currentRoomInput.setText(node.getRoomName());
            detectedRoomText.setText(getString(R.string.detected_room, node.getRoomName()));
            renderBlueDot(node);
        });
        viewModel.getRoute().observe(getViewLifecycleOwner(), route -> {
            StringBuilder builder = new StringBuilder();
            for (String step : route.getRouteSteps()) {
                builder.append("• ").append(step).append('\n');
            }
            routeText.setText(builder.toString().trim());
        });
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.startSimulation();
    }

    @Override
    public void onPause() {
        viewModel.stopSimulation();
        super.onPause();
    }

    private void renderBlueDot(BeaconNode node) {
        indoorMapFrame.post(() -> {
            float x = ((float) node.getX() / MAX_BEACON_X) * (indoorMapFrame.getWidth() - blueDotView.getWidth());
            float y = ((float) node.getY() / MAX_BEACON_Y) * (indoorMapFrame.getHeight() - blueDotView.getHeight());
            blueDotView.animate()
                    .x(x)
                    .y(y)
                    .setDuration(450L)
                    .start();
        });
    }
}
