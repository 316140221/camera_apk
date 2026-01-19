package com.zcf.virtualcam.ui;

// Usage: location settings page with manual input and map picker.

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zcf.virtualcam.R;
import com.zcf.virtualcam.xposed.Config;
import com.zcf.virtualcam.xposed.ConfigLoader;

import java.io.IOException;

public class LocationFragment extends Fragment {

    private Switch locationSwitch;
    private EditText latitudeEditText;
    private EditText longitudeEditText;
    private ActivityResultLauncher<Intent> locationPickerLauncher;

    public LocationFragment() {
        super(R.layout.fragment_location);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handlePickResult
        );
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        locationSwitch = view.findViewById(R.id.switch_location);
        latitudeEditText = view.findViewById(R.id.edit_latitude);
        longitudeEditText = view.findViewById(R.id.edit_longitude);
        Button pickButton = view.findViewById(R.id.button_pick_location);
        Button saveButton = view.findViewById(R.id.button_save_location);

        bindConfig(ConfigLoader.loadOrDefault());

        pickButton.setOnClickListener(v -> {
            Double lat = parseDoubleOrNull(latitudeEditText.getText().toString().trim());
            Double lng = parseDoubleOrNull(longitudeEditText.getText().toString().trim());
            if (lat == null) {
                lat = ConfigLoader.DEFAULT_LATITUDE;
            }
            if (lng == null) {
                lng = ConfigLoader.DEFAULT_LONGITUDE;
            }
            Intent intent = MapPickerActivity.createIntent(requireContext(), lat, lng);
            locationPickerLauncher.launch(intent);
        });

        saveButton.setOnClickListener(v -> {
            if (!ensureStoragePermission()) {
                return;
            }
            Config updated = buildUpdatedConfig();
            if (updated == null) {
                return;
            }
            try {
                ConfigLoader.saveToDisk(updated);
                String msg = getString(R.string.toast_save_success, ConfigLoader.DEFAULT_CONFIG_PATH);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                String msg = getString(R.string.toast_save_failed, e.getMessage());
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (locationSwitch == null || latitudeEditText == null || longitudeEditText == null) {
            return;
        }
        bindConfig(ConfigLoader.loadOrDefault());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        locationSwitch = null;
        latitudeEditText = null;
        longitudeEditText = null;
    }

    private void bindConfig(@NonNull Config config) {
        locationSwitch.setChecked(config.enableLocation);
        latitudeEditText.setText(String.valueOf(config.latitude));
        longitudeEditText.setText(String.valueOf(config.longitude));
    }

    @Nullable
    private Config buildUpdatedConfig() {
        boolean enableLocation = locationSwitch.isChecked();
        Double latitude = parseDoubleOrNull(latitudeEditText.getText().toString().trim());
        Double longitude = parseDoubleOrNull(longitudeEditText.getText().toString().trim());

        if (enableLocation) {
            if (latitude == null || longitude == null) {
                Toast.makeText(requireContext(), getString(R.string.toast_invalid_location), Toast.LENGTH_SHORT).show();
                return null;
            }
            if (!isLatitudeValid(latitude) || !isLongitudeValid(longitude)) {
                Toast.makeText(requireContext(), getString(R.string.toast_invalid_location_range), Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        if (latitude == null) {
            latitude = ConfigLoader.DEFAULT_LATITUDE;
        }
        if (longitude == null) {
            longitude = ConfigLoader.DEFAULT_LONGITUDE;
        }

        Config base = ConfigLoader.loadOrDefault();
        return Config.builder(base)
                .setEnableLocation(enableLocation)
                .setLatitude(latitude)
                .setLongitude(longitude)
                .build();
    }

    private void handlePickResult(@NonNull ActivityResult result) {
        if (result.getResultCode() != android.app.Activity.RESULT_OK) {
            return;
        }
        if (latitudeEditText == null || longitudeEditText == null) {
            return;
        }
        Intent data = result.getData();
        if (data == null) {
            return;
        }
        double lat = data.getDoubleExtra(MapPickerActivity.EXTRA_LATITUDE, ConfigLoader.DEFAULT_LATITUDE);
        double lng = data.getDoubleExtra(MapPickerActivity.EXTRA_LONGITUDE, ConfigLoader.DEFAULT_LONGITUDE);
        latitudeEditText.setText(String.valueOf(lat));
        longitudeEditText.setText(String.valueOf(lng));
    }

    private boolean ensureStoragePermission() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).ensureStoragePermission();
        }
        return false;
    }

    @Nullable
    private static Double parseDoubleOrNull(@NonNull String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isLatitudeValid(double latitude) {
        return latitude >= -90.0d && latitude <= 90.0d;
    }

    private static boolean isLongitudeValid(double longitude) {
        return longitude >= -180.0d && longitude <= 180.0d;
    }
}
