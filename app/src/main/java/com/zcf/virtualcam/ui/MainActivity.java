package com.zcf.virtualcam.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zcf.virtualcam.R;
import com.zcf.virtualcam.xposed.Config;
import com.zcf.virtualcam.xposed.ConfigLoader;
import com.zcf.virtualcam.xposed.FileOps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_IMAGE = 1001;
    private static final int REQUEST_CODE_PICK_VIDEO = 1002;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 2001;

    private Switch enabledSwitch;
    private Switch photoSwitch;
    private Switch videoSwitch;
    private Spinner modeSpinner;
    private EditText allowlistEditText;
    private EditText photoPathEditText;
    private EditText videoPathEditText;
    private TextView configPathTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enabledSwitch = findViewById(R.id.switch_enabled);
        photoSwitch = findViewById(R.id.switch_photo);
        videoSwitch = findViewById(R.id.switch_video);
        modeSpinner = findViewById(R.id.spinner_mode);
        allowlistEditText = findViewById(R.id.edit_allowlist);
        photoPathEditText = findViewById(R.id.edit_photo_path);
        videoPathEditText = findViewById(R.id.edit_video_path);
        configPathTextView = findViewById(R.id.text_config_path);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.mode_items,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(adapter);

        configPathTextView.setText(ConfigLoader.DEFAULT_CONFIG_PATH);

        Config config = ConfigLoader.loadFromDisk();
        if (config == null) {
            config = Config.defaultDisabled();
        }
        bindConfigToUi(config);

        Button importPhotoButton = findViewById(R.id.button_import_photo);
        Button importVideoButton = findViewById(R.id.button_import_video);
        Button saveButton = findViewById(R.id.button_save);

        importPhotoButton.setOnClickListener(v -> {
            if (!ensureStoragePermission()) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        });

        importVideoButton.setOnClickListener(v -> {
            if (!ensureStoragePermission()) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_VIDEO);
        });

        saveButton.setOnClickListener(v -> {
            if (!ensureStoragePermission()) {
                return;
            }
            Config updated = readConfigFromUi();
            try {
                ConfigLoader.saveToDisk(updated);
                Toast.makeText(this, "已保存：" + ConfigLoader.DEFAULT_CONFIG_PATH, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindConfigToUi(@NonNull Config config) {
        enabledSwitch.setChecked(config.enabled);
        photoSwitch.setChecked(config.enablePhoto);
        videoSwitch.setChecked(config.enableVideo);

        int modeIndex = 0;
        if (Config.MODE_ALL.equals(config.mode)) {
            modeIndex = 1;
        }
        modeSpinner.setSelection(modeIndex);

        StringBuilder sb = new StringBuilder();
        for (String pkg : config.allowlist) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(pkg);
        }
        allowlistEditText.setText(sb.toString());
        photoPathEditText.setText(config.photoPath);
        videoPathEditText.setText(config.videoPath);
    }

    @NonNull
    private Config readConfigFromUi() {
        String mode = modeSpinner.getSelectedItemPosition() == 1 ? Config.MODE_ALL : Config.MODE_ALLOWLIST;
        List<String> allowlist = new ArrayList<>();
        String[] lines = allowlistEditText.getText().toString().split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                allowlist.add(trimmed);
            }
        }

        return new Config(
                enabledSwitch.isChecked(),
                photoSwitch.isChecked(),
                videoSwitch.isChecked(),
                mode,
                allowlist,
                photoPathEditText.getText().toString().trim(),
                videoPathEditText.getText().toString().trim()
        );
    }

    private boolean ensureStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        boolean hasRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        if (hasRead && hasWrite) {
            return true;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CODE_STORAGE_PERMISSION
        );
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CODE_STORAGE_PERMISSION) {
            return;
        }
        boolean granted = true;
        for (int r : grantResults) {
            granted = granted && (r == PackageManager.PERMISSION_GRANTED);
        }
        Toast.makeText(this, granted ? "权限已授予" : "未授予存储权限", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }

        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            String dstPath = photoPathEditText.getText().toString().trim();
            if (dstPath.isEmpty()) {
                Toast.makeText(this, "photoPath 为空", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                ensureParentDir(dstPath);
                FileOps.copyFromUri(this, uri, new File(dstPath));
                Toast.makeText(this, "已导入图片：" + dstPath, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "导入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (requestCode == REQUEST_CODE_PICK_VIDEO) {
            String dstPath = videoPathEditText.getText().toString().trim();
            if (dstPath.isEmpty()) {
                Toast.makeText(this, "videoPath 为空", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                ensureParentDir(dstPath);
                FileOps.copyFromUri(this, uri, new File(dstPath));
                Toast.makeText(this, "已导入视频：" + dstPath, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "导入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
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

