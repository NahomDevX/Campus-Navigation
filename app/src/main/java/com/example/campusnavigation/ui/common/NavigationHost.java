package com.example.campusnavigation.ui.common;

import com.example.campusnavigation.model.Building;

public interface NavigationHost {
    void openMapForBuilding(Building building);

    void openIndoorNavigation();
}
