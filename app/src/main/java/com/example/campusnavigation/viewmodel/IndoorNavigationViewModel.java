package com.example.campusnavigation.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.campusnavigation.model.BeaconNode;
import com.example.campusnavigation.model.IndoorRoute;
import com.example.campusnavigation.util.IndoorNavigationSimulator;

import java.util.List;

public class IndoorNavigationViewModel extends ViewModel {
    private static final long SIMULATION_INTERVAL_MS = 4000L;

    private final IndoorNavigationSimulator simulator = new IndoorNavigationSimulator();
    private final MutableLiveData<String> beaconStatus = new MutableLiveData<>();
    private final MutableLiveData<IndoorRoute> route = new MutableLiveData<>();
    private final MutableLiveData<BeaconNode> currentNode = new MutableLiveData<>();
    private final MutableLiveData<List<BeaconNode>> beaconNodes = new MutableLiveData<>(simulator.getBeaconNodes());
    private final Handler simulationHandler = new Handler(Looper.getMainLooper());
    private final Runnable simulationRunnable = new Runnable() {
        @Override
        public void run() {
            scan();
            simulationHandler.postDelayed(this, SIMULATION_INTERVAL_MS);
        }
    };
    private boolean simulationRunning;

    public LiveData<String> getBeaconStatus() {
        return beaconStatus;
    }

    public LiveData<IndoorRoute> getRoute() {
        return route;
    }

    public LiveData<BeaconNode> getCurrentNode() {
        return currentNode;
    }

    public LiveData<List<BeaconNode>> getBeaconNodes() {
        return beaconNodes;
    }

    public void scan() {
        BeaconNode node = simulator.scanNearestBeacon();
        currentNode.setValue(node);
        beaconStatus.setValue("Blue dot locked to " + node.getRoomName() + " via simulated beacon " + node.getId() + " (" + node.getSimulatedRssi() + " dBm)");
    }

    public void startSimulation() {
        if (simulationRunning) {
            return;
        }
        simulationRunning = true;
        if (currentNode.getValue() == null) {
            scan();
        }
        simulationHandler.postDelayed(simulationRunnable, SIMULATION_INTERVAL_MS);
    }

    public void stopSimulation() {
        simulationRunning = false;
        simulationHandler.removeCallbacks(simulationRunnable);
    }

    public void startNavigation(String currentRoom, String destinationRoom) {
        route.setValue(simulator.buildRoute(currentRoom, destinationRoom));
    }

    @Override
    protected void onCleared() {
        stopSimulation();
        super.onCleared();
    }
}
