package com.zcf.virtualcam.ui;

// Usage: common settings page for module enable + allowlist.

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zcf.virtualcam.R;
import com.zcf.virtualcam.xposed.Config;
import com.zcf.virtualcam.xposed.ConfigLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommonFragment extends Fragment {

    private TextView configPathTextView;
    private Switch enabledSwitch;
    private Spinner modeSpinner;
    private EditText allowlistEditText;

    public CommonFragment() {
        super(R.layout.fragment_common);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        configPathTextView = view.findViewById(R.id.text_config_path);
        enabledSwitch = view.findViewById(R.id.switch_enabled);
        modeSpinner = view.findViewById(R.id.spinner_mode);
        allowlistEditText = view.findViewById(R.id.edit_allowlist);
        Button saveButton = view.findViewById(R.id.button_save_common);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.mode_items,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(adapter);

        configPathTextView.setText(ConfigLoader.DEFAULT_CONFIG_PATH);

        bindConfig(ConfigLoader.loadOrDefault(), enabledSwitch, modeSpinner, allowlistEditText);

        saveButton.setOnClickListener(v -> {
            if (!ensureStoragePermission()) {
                return;
            }
            Config base = ConfigLoader.loadOrDefault();
            Config updated = buildUpdatedConfig(base, enabledSwitch, modeSpinner, allowlistEditText);
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
        if (enabledSwitch == null || modeSpinner == null || allowlistEditText == null) {
            return;
        }
        bindConfig(ConfigLoader.loadOrDefault(), enabledSwitch, modeSpinner, allowlistEditText);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        configPathTextView = null;
        enabledSwitch = null;
        modeSpinner = null;
        allowlistEditText = null;
    }

    private static void bindConfig(
            @NonNull Config config,
            @NonNull Switch enabledSwitch,
            @NonNull Spinner modeSpinner,
            @NonNull EditText allowlistEditText
    ) {
        enabledSwitch.setChecked(config.enabled);
        int modeIndex = Config.MODE_ALL.equals(config.mode) ? 1 : 0;
        modeSpinner.setSelection(modeIndex);

        StringBuilder sb = new StringBuilder();
        for (String pkg : config.allowlist) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(pkg);
        }
        allowlistEditText.setText(sb.toString());
    }

    @NonNull
    private static Config buildUpdatedConfig(
            @NonNull Config base,
            @NonNull Switch enabledSwitch,
            @NonNull Spinner modeSpinner,
            @NonNull EditText allowlistEditText
    ) {
        String mode = modeSpinner.getSelectedItemPosition() == 1 ? Config.MODE_ALL : Config.MODE_ALLOWLIST;
        List<String> allowlist = new ArrayList<>();
        String[] lines = allowlistEditText.getText().toString().split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                allowlist.add(trimmed);
            }
        }

        return Config.builder(base)
                .setEnabled(enabledSwitch.isChecked())
                .setMode(mode)
                .setAllowlist(allowlist)
                .build();
    }

    private boolean ensureStoragePermission() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).ensureStoragePermission();
        }
        return false;
    }
}
