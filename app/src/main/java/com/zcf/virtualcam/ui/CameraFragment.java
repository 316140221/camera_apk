package com.zcf.virtualcam.ui;

// Usage: camera settings page for photo/video replacement.

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
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
import com.zcf.virtualcam.xposed.FileOps;

import java.io.File;
import java.io.IOException;

public class CameraFragment extends Fragment {

    private Switch photoSwitch;
    private Switch videoSwitch;
    private EditText photoPathEditText;
    private EditText videoPathEditText;
    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private ActivityResultLauncher<Intent> videoPickerLauncher;

    public CameraFragment() {
        super(R.layout.fragment_camera);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handlePickResult(result, true)
        );
        videoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handlePickResult(result, false)
        );
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        photoSwitch = view.findViewById(R.id.switch_photo);
        videoSwitch = view.findViewById(R.id.switch_video);
        photoPathEditText = view.findViewById(R.id.edit_photo_path);
        videoPathEditText = view.findViewById(R.id.edit_video_path);
        Button importPhotoButton = view.findViewById(R.id.button_import_photo);
        Button importVideoButton = view.findViewById(R.id.button_import_video);
        Button saveButton = view.findViewById(R.id.button_save_camera);

        bindConfig(ConfigLoader.loadOrDefault());

        importPhotoButton.setOnClickListener(v -> {
            if (!ensureStoragePermission()) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            photoPickerLauncher.launch(intent);
        });

        importVideoButton.setOnClickListener(v -> {
            if (!ensureStoragePermission()) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            videoPickerLauncher.launch(intent);
        });

        saveButton.setOnClickListener(v -> {
            if (!ensureStoragePermission()) {
                return;
            }
            Config base = ConfigLoader.loadOrDefault();
            Config updated = Config.builder(base)
                    .setEnablePhoto(photoSwitch.isChecked())
                    .setEnableVideo(videoSwitch.isChecked())
                    .setPhotoPath(photoPathEditText.getText().toString().trim())
                    .setVideoPath(videoPathEditText.getText().toString().trim())
                    .build();
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
        if (photoSwitch == null || videoSwitch == null || photoPathEditText == null || videoPathEditText == null) {
            return;
        }
        bindConfig(ConfigLoader.loadOrDefault());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        photoSwitch = null;
        videoSwitch = null;
        photoPathEditText = null;
        videoPathEditText = null;
    }

    private void bindConfig(@NonNull Config config) {
        photoSwitch.setChecked(config.enablePhoto);
        videoSwitch.setChecked(config.enableVideo);
        photoPathEditText.setText(config.photoPath);
        videoPathEditText.setText(config.videoPath);
    }

    private void handlePickResult(@NonNull ActivityResult result, boolean isPhoto) {
        if (result.getResultCode() != Activity.RESULT_OK) {
            return;
        }
        if (photoPathEditText == null || videoPathEditText == null) {
            return;
        }
        Intent data = result.getData();
        if (data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }

        if (isPhoto) {
            String dstPath = photoPathEditText.getText().toString().trim();
            if (dstPath.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.toast_photo_path_empty), Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                ensureParentDir(dstPath);
                FileOps.copyFromUri(requireContext(), uri, new File(dstPath));
                String msg = getString(R.string.toast_import_photo_success, dstPath);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                String msg = getString(R.string.toast_import_failed, e.getMessage());
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (!isPhoto) {
            String dstPath = videoPathEditText.getText().toString().trim();
            if (dstPath.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.toast_video_path_empty), Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                ensureParentDir(dstPath);
                FileOps.copyFromUri(requireContext(), uri, new File(dstPath));
                String msg = getString(R.string.toast_import_video_success, dstPath);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                String msg = getString(R.string.toast_import_failed, e.getMessage());
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean ensureStoragePermission() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).ensureStoragePermission();
        }
        return false;
    }

    private static void ensureParentDir(@NonNull String path) throws IOException {
        File parent = new File(path).getParentFile();
        if (parent == null) {
            return;
        }
        if (parent.exists()) {
            return;
        }
        if (!parent.mkdirs()) {
            throw new IOException("无法创建目录：" + parent.getAbsolutePath());
        }
    }
}
