package com.example.campusnavigation.viewmodel;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModel;

import com.example.campusnavigation.CampusNavigationApp;
import com.example.campusnavigation.data.repository.AuthRepository;
import com.example.campusnavigation.data.repository.PreferencesRepository;

public class SettingsViewModel extends ViewModel {
    private final PreferencesRepository preferencesRepository = CampusNavigationApp.getInstance().getPreferencesRepository();
    private final AuthRepository authRepository = CampusNavigationApp.getInstance().getAuthRepository();

    public boolean notificationsEnabled() {
        return preferencesRepository.areNotificationsEnabled();
    }

    public boolean darkModeEnabled() {
        return preferencesRepository.isDarkModeEnabled();
    }

    public String getLanguage() {
        return preferencesRepository.getLanguage();
    }

    public void setNotificationsEnabled(boolean enabled) {
        preferencesRepository.setNotificationsEnabled(enabled);
    }

    public void setDarkModeEnabled(boolean enabled) {
        preferencesRepository.setDarkModeEnabled(enabled);
        AppCompatDelegate.setDefaultNightMode(enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    public void setLanguage(String languageCode) {
        preferencesRepository.setLanguage(languageCode);
    }

    public void logout() {
        authRepository.signOut();
    }
}
