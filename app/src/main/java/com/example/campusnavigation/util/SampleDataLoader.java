package com.example.campusnavigation.util;

import android.content.Context;

import com.example.campusnavigation.model.Building;
import com.example.campusnavigation.model.CampusEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SampleDataLoader {
    private SampleDataLoader() {
    }

    public static List<Building> loadBuildings(Context context) {
        Type type = new TypeToken<ArrayList<Building>>() { }.getType();
        return loadJsonArray(context, "sample/buildings.json", type);
    }

    public static List<CampusEvent> loadEvents(Context context) {
        Type type = new TypeToken<ArrayList<CampusEvent>>() { }.getType();
        return loadJsonArray(context, "sample/events.json", type);
    }

    private static <T> List<T> loadJsonArray(Context context, String assetPath, Type type) {
        try (InputStream inputStream = context.getAssets().open(assetPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return new Gson().fromJson(reader, type);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
