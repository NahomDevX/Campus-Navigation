package com.example.campusnavigation.util;

import android.content.Context;
import android.location.Location;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class CampusRouteCalculator {
    public static class RouteResult {
        private final List<LatLng> path;
        private final double distanceMeters;

        public RouteResult(List<LatLng> path, double distanceMeters) {
            this.path = path;
            this.distanceMeters = distanceMeters;
        }

        public List<LatLng> getPath() {
            return path;
        }

        public double getDistanceMeters() {
            return distanceMeters;
        }
    }

    private static class GraphNode {
        @SerializedName("id")
        String id;
        @SerializedName("lat")
        double lat;
        @SerializedName("lng")
        double lng;
    }

    private static class GraphEdge {
        @SerializedName("from")
        String from;
        @SerializedName("to")
        String to;
    }

    private static class GraphData {
        @SerializedName("nodes")
        List<GraphNode> nodes;
        @SerializedName("edges")
        List<GraphEdge> edges;
    }

    private static class Neighbor {
        final String nodeId;
        final double distance;

        Neighbor(String nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }

    private static class EdgeSnap {
        final LatLng projection;
        final String entryNodeId;
        final double distance;

        EdgeSnap(LatLng projection, String entryNodeId, double distance) {
            this.projection = projection;
            this.entryNodeId = entryNodeId;
            this.distance = distance;
        }
    }

    private final Map<String, GraphNode> nodesById = new HashMap<>();
    private final Map<String, List<Neighbor>> adjacency = new HashMap<>();
    private final List<GraphEdge> edgeList = new ArrayList<>();

    public CampusRouteCalculator(Context context) {
        loadGraph(context);
    }

    private void loadGraph(Context context) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("campus_graph.json"), StandardCharsets.UTF_8))) {
            GraphData data = new Gson().fromJson(reader, GraphData.class);
            if (data == null || data.nodes == null || data.edges == null) {
                return;
            }
            for (GraphNode node : data.nodes) {
                nodesById.put(node.id, node);
                adjacency.put(node.id, new ArrayList<>());
            }
            for (GraphEdge edge : data.edges) {
                GraphNode from = nodesById.get(edge.from);
                GraphNode to = nodesById.get(edge.to);
                if (from == null || to == null) {
                    continue;
                }
                double distance = distanceBetween(from.lat, from.lng, to.lat, to.lng);
                adjacency.get(edge.from).add(new Neighbor(edge.to, distance));
                adjacency.get(edge.to).add(new Neighbor(edge.from, distance));
                edgeList.add(edge);
            }
        } catch (Exception ignored) {
            // Graph unavailable; caller falls back to direct segment.
        }
    }

    public RouteResult calculateRoute(LatLng start, LatLng end) {
        if (nodesById.isEmpty()) {
            return directRoute(start, end);
        }

        // Snap the endpoints onto the nearest walkway segment so the connecting
        // legs run along the path network instead of cutting across buildings.
        EdgeSnap startSnap = nearestEdgeSnap(start);
        EdgeSnap endSnap = nearestEdgeSnap(end);

        String startNodeId = startSnap != null ? startSnap.entryNodeId : nearestNodeId(start);
        String endNodeId = endSnap != null ? endSnap.entryNodeId : nearestNodeId(end);
        if (startNodeId == null || endNodeId == null) {
            return directRoute(start, end);
        }

        List<String> nodePath = findPath(startNodeId, endNodeId);
        if (nodePath.isEmpty()) {
            return directRoute(start, end);
        }

        List<LatLng> path = new ArrayList<>();
        addPoint(path, start);
        if (startSnap != null) {
            addPoint(path, startSnap.projection);
        }
        for (String nodeId : nodePath) {
            GraphNode node = nodesById.get(nodeId);
            if (node != null) {
                addPoint(path, new LatLng(node.lat, node.lng));
            }
        }
        if (endSnap != null) {
            addPoint(path, endSnap.projection);
        }
        addPoint(path, end);

        return new RouteResult(path, pathDistance(path));
    }

    private static void addPoint(List<LatLng> path, LatLng point) {
        if (path.isEmpty()) {
            path.add(point);
            return;
        }
        LatLng last = path.get(path.size() - 1);
        if (distanceBetween(last, point) > 0.5d) {
            path.add(point);
        }
    }

    private EdgeSnap nearestEdgeSnap(LatLng point) {
        EdgeSnap best = null;
        for (GraphEdge edge : edgeList) {
            GraphNode a = nodesById.get(edge.from);
            GraphNode b = nodesById.get(edge.to);
            if (a == null || b == null) {
                continue;
            }
            LatLng pa = new LatLng(a.lat, a.lng);
            LatLng pb = new LatLng(b.lat, b.lng);
            LatLng projection = projectOnSegment(point, pa, pb);
            double distance = distanceBetween(point, projection);
            if (best == null || distance < best.distance) {
                String entry = distanceBetween(projection, pa) <= distanceBetween(projection, pb)
                        ? edge.from
                        : edge.to;
                best = new EdgeSnap(projection, entry, distance);
            }
        }
        return best;
    }

    public static double distanceToPolyline(LatLng point, List<LatLng> polyline) {
        if (polyline == null || polyline.isEmpty()) {
            return Float.MAX_VALUE;
        }
        if (polyline.size() == 1) {
            return distanceBetween(point, polyline.get(0));
        }

        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < polyline.size() - 1; i++) {
            minDistance = Math.min(minDistance, distanceToSegment(point, polyline.get(i), polyline.get(i + 1)));
        }
        return minDistance;
    }

    public static double remainingDistanceAlongPath(LatLng point, List<LatLng> polyline) {
        if (polyline == null || polyline.size() < 2) {
            return 0d;
        }

        int nearestSegment = 0;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < polyline.size() - 1; i++) {
            double segmentDistance = distanceToSegment(point, polyline.get(i), polyline.get(i + 1));
            if (segmentDistance < minDistance) {
                minDistance = segmentDistance;
                nearestSegment = i;
            }
        }

        double remaining = distanceBetween(point, polyline.get(nearestSegment + 1));
        for (int i = nearestSegment + 1; i < polyline.size() - 1; i++) {
            remaining += distanceBetween(polyline.get(i), polyline.get(i + 1));
        }
        return remaining;
    }

    public static int nearestSegmentIndex(LatLng point, List<LatLng> polyline) {
        if (polyline == null || polyline.size() < 2) {
            return 0;
        }
        int nearestSegment = 0;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < polyline.size() - 1; i++) {
            double segmentDistance = distanceToSegment(point, polyline.get(i), polyline.get(i + 1));
            if (segmentDistance < minDistance) {
                minDistance = segmentDistance;
                nearestSegment = i;
            }
        }
        return nearestSegment;
    }

    private List<String> findPath(String startId, String endId) {
        if (startId.equals(endId)) {
            return Collections.singletonList(startId);
        }

        Map<String, Double> cost = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<String> openSet = new PriorityQueue<>((a, b) -> {
            double costA = cost.getOrDefault(a, Double.MAX_VALUE);
            double costB = cost.getOrDefault(b, Double.MAX_VALUE);
            return Double.compare(costA, costB);
        });

        for (String nodeId : nodesById.keySet()) {
            cost.put(nodeId, Double.MAX_VALUE);
        }
        cost.put(startId, 0d);
        openSet.add(startId);

        while (!openSet.isEmpty()) {
            String current = openSet.poll();
            if (current.equals(endId)) {
                break;
            }
            List<Neighbor> neighbors = adjacency.get(current);
            if (neighbors == null) {
                continue;
            }
            for (Neighbor neighbor : neighbors) {
                double tentative = cost.get(current) + neighbor.distance;
                if (tentative < cost.getOrDefault(neighbor.nodeId, Double.MAX_VALUE)) {
                    cost.put(neighbor.nodeId, tentative);
                    previous.put(neighbor.nodeId, current);
                    openSet.remove(neighbor.nodeId);
                    openSet.add(neighbor.nodeId);
                }
            }
        }

        if (!previous.containsKey(endId) && !startId.equals(endId)) {
            return Collections.emptyList();
        }

        List<String> path = new ArrayList<>();
        String current = endId;
        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }
        return path;
    }

    @Nullable
    private String nearestNodeId(LatLng point) {
        String nearestId = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Map.Entry<String, GraphNode> entry : nodesById.entrySet()) {
            GraphNode node = entry.getValue();
            double distance = distanceBetween(point.latitude, point.longitude, node.lat, node.lng);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestId = entry.getKey();
            }
        }
        return nearestId;
    }

    private RouteResult directRoute(LatLng start, LatLng end) {
        List<LatLng> path = new ArrayList<>();
        path.add(start);
        path.add(end);
        return new RouteResult(path, distanceBetween(start, end));
    }

    private static double pathDistance(List<LatLng> path) {
        double total = 0d;
        for (int i = 0; i < path.size() - 1; i++) {
            total += distanceBetween(path.get(i), path.get(i + 1));
        }
        return total;
    }

    private static double distanceBetween(LatLng start, LatLng end) {
        return distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude);
    }

    private static double distanceBetween(double startLat, double startLng, double endLat, double endLng) {
        float[] results = new float[1];
        Location.distanceBetween(startLat, startLng, endLat, endLng, results);
        return results[0];
    }

    private static double distanceToSegment(LatLng point, LatLng start, LatLng end) {
        return distanceBetween(point, projectOnSegment(point, start, end));
    }

    private static LatLng projectOnSegment(LatLng point, LatLng start, LatLng end) {
        double dLat = end.latitude - start.latitude;
        double dLng = end.longitude - start.longitude;
        double denominator = dLat * dLat + dLng * dLng;
        if (denominator == 0d) {
            return start;
        }

        double t = ((point.latitude - start.latitude) * dLat
                + (point.longitude - start.longitude) * dLng) / denominator;
        t = Math.max(0d, Math.min(1d, t));

        return new LatLng(start.latitude + t * dLat, start.longitude + t * dLng);
    }
}
