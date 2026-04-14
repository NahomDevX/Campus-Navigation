package com.example.campusnavigation.ui.settings;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.campusnavigation.R;
import com.example.campusnavigation.ui.auth.AuthActivity;
import com.example.campusnavigation.viewmodel.BuildingViewModel;
import com.example.campusnavigation.viewmodel.EventViewModel;
import com.example.campusnavigation.viewmodel.SettingsViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

public class SettingsFragment extends Fragment {
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
        BuildingViewModel buildingViewModel = new ViewModelProvider(requireActivity()).get(BuildingViewModel.class);
        EventViewModel eventViewModel = new ViewModelProvider(requireActivity()).get(EventViewModel.class);
        MaterialSwitch notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        MaterialSwitch darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        MaterialButtonToggleGroup languageToggle = view.findViewById(R.id.languageToggle);
        MaterialButton syncButton = view.findViewById(R.id.syncButton);
        MaterialButton logoutButton = view.findViewById(R.id.logoutButton);

        notificationsSwitch.setChecked(settingsViewModel.notificationsEnabled());
        darkModeSwitch.setChecked(settingsViewModel.darkModeEnabled());
        if ("am".equals(settingsViewModel.getLanguage())) {
            languageToggle.check(R.id.amharicButton);
        } else {
            languageToggle.check(R.id.englishButton);
        }

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> settingsViewModel.setNotificationsEnabled(isChecked));
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> settingsViewModel.setDarkModeEnabled(isChecked));
        languageToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            String code = checkedId == R.id.amharicButton ? "am" : "en";
            settingsViewModel.setLanguage(code);
            updateLocale(code);
        });
        syncButton.setOnClickListener(v -> {
            buildingViewModel.sync();
            eventViewModel.sync();
            Toast.makeText(requireContext(), R.string.campus_sync_complete, Toast.LENGTH_SHORT).show();
        });
        logoutButton.setOnClickListener(v -> {
            settingsViewModel.logout();
            startActivity(new Intent(requireContext(), AuthActivity.class));
            requireActivity().finish();
        });
    }

    private void updateLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(requireContext().getResources().getConfiguration());
        configuration.setLocale(locale);
        requireContext().getResources().updateConfiguration(configuration, requireContext().getResources().getDisplayMetrics());
        requireActivity().recreate();
    }
}
