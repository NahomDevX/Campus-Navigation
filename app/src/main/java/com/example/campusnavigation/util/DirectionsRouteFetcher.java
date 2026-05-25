package com.example.campusnavigation.util;

import android.os.Handler;
import android.os.Looper;

import com.example.campusnavigation.BuildConfig;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DirectionsRouteFetcher {
    public static class DirectionsResult {
        private final List<LatLng> path;
        private final double distanceMeters;
        private final int durationSeconds;

        public DirectionsResult(List<LatLng> path, double distanceMeters, int durationSeconds) {
            this.path = path;
            this.distanceMeters = distanceMeters;
            this.durationSeconds = durationSeconds;
        }

        public List<LatLng> getPath() {
            return path;
        }

        public double getDistanceMeters() {
            return distanceMeters;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }
    }

    public interface DirectionsCallback {
        void onSuccess(DirectionsResult result);

        void onFailure();
    }

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public void fetchRoute(LatLng start, LatLng end, DirectionsCallback callback) {
        if (!isApiKeyConfigured()) {
            MAIN_HANDLER.post(callback::onFailure);
            return;
        }

        AppExecutors.io().execute(() -> {
            try {
                DirectionsResult result = requestDirections(start, end);
                if (result == null || result.getPath().isEmpty()) {
                    MAIN_HANDLER.post(callback::onFailure);
                } else {
                    MAIN_HANDLER.post(() -> callback.onSuccess(result));
                }
            } catch (Exception exception) {
                MAIN_HANDLER.post(callback::onFailure);
            }
        });
    }

    private DirectionsResult requestDirections(LatLng start, LatLng end) throws Exception {
        String origin = String.format(Locale.US, "%.6f,%.6f", start.latitude, start.longitude);
        String destination = String.format(Locale.US, "%.6f,%.6f", end.latitude, end.longitude);
        String encodedOrigin = URLEncoder.encode(origin, StandardCharsets.UTF_8.name());
        String encodedDestination = URLEncoder.encode(destination, StandardCharsets.UTF_8.name());
        String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin="
                + encodedOrigin
                + "&destination="
                + encodedDestination
                + "&mode=walking&key="
                + BuildConfig.MAPS_API_KEY;

        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod("GET");

        String responseBody;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            responseBody = builder.toString();
        } finally {
            connection.disconnect();
        }

        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        if (!"OK".equalsIgnoreCase(root.get("status").getAsString())) {
            return null;
        }

        JsonArray routes = root.getAsJsonArray("routes");
        if (routes == null || routes.size() == 0) {
            return null;
        }

        JsonObject route = routes.get(0).getAsJsonObject();
        JsonObject leg = route.getAsJsonArray("legs").get(0).getAsJsonObject();
        double distanceMeters = leg.getAsJsonObject("distance").get("value").getAsDouble();
        int durationSeconds = leg.getAsJsonObject("duration").get("value").getAsInt();
        String encodedPolyline = route.getAsJsonObject("overview_polyline").get("points").getAsString();
        List<LatLng> path = decodePolyline(encodedPolyline);
        return new DirectionsResult(path, distanceMeters, durationSeconds);
    }

    private static boolean isApiKeyConfigured() {
        String key = BuildConfig.MAPS_API_KEY;
        return key != null
                && !key.isEmpty()
                && !"YOUR_MAP_KEY".equals(key)
                && !"YOUR_MAP_API_KEY".equals(key);
    }

    static List<LatLng> decodePolyline(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < encoded.length()) {
            int shift = 0;
            int result = 0;
            int value;
            do {
                value = encoded.charAt(index++) - 63;
                result |= (value & 0x1f) << shift;
                shift += 5;
            } while (value >= 0x20);
            lat += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));

            shift = 0;
            result = 0;
            do {
                value = encoded.charAt(index++) - 63;
                result |= (value & 0x1f) << shift;
                shift += 5;
            } while (value >= 0x20);
            lng += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));

            polyline.add(new LatLng(lat / 1E5, lng / 1E5));
        }
        return polyline;
    }
}
