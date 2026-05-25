package com.example.campusnavigation.util;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

public class RouteProvider {
    public static class RouteResult {
        private final java.util.List<LatLng> path;
        private final double distanceMeters;
        private final int durationSeconds;
        private final boolean fromDirectionsApi;

        public RouteResult(java.util.List<LatLng> path, double distanceMeters, int durationSeconds, boolean fromDirectionsApi) {
            this.path = path;
            this.distanceMeters = distanceMeters;
            this.durationSeconds = durationSeconds;
            this.fromDirectionsApi = fromDirectionsApi;
        }

        public java.util.List<LatLng> getPath() {
            return path;
        }

        public double getDistanceMeters() {
            return distanceMeters;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }

        public boolean isFromDirectionsApi() {
            return fromDirectionsApi;
        }
    }

    public interface RouteCallback {
        void onRouteReady(RouteResult result);
    }

    private final DirectionsRouteFetcher directionsRouteFetcher;
    private final CampusRouteCalculator campusRouteCalculator;

    public RouteProvider(Context context) {
        this.directionsRouteFetcher = new DirectionsRouteFetcher();
        this.campusRouteCalculator = new CampusRouteCalculator(context);
    }

    public void fetchRoute(LatLng start, LatLng end, RouteCallback callback) {
        directionsRouteFetcher.fetchRoute(start, end, new DirectionsRouteFetcher.DirectionsCallback() {
            @Override
            public void onSuccess(DirectionsRouteFetcher.DirectionsResult result) {
                callback.onRouteReady(new RouteResult(
                        result.getPath(),
                        result.getDistanceMeters(),
                        result.getDurationSeconds(),
                        true));
            }

            @Override
            public void onFailure() {
                CampusRouteCalculator.RouteResult campusResult = campusRouteCalculator.calculateRoute(start, end);
                int durationSeconds = (int) Math.ceil(campusResult.getDistanceMeters() / 70d * 60d);
                callback.onRouteReady(new RouteResult(
                        campusResult.getPath(),
                        campusResult.getDistanceMeters(),
                        durationSeconds,
                        false));
            }
        });
    }
}
