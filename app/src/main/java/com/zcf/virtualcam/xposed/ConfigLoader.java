package com.zcf.virtualcam.xposed;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ConfigLoader {

    public static final String DEFAULT_DIR = "/sdcard/VirtualCam";
    public static final String DEFAULT_CONFIG_PATH = DEFAULT_DIR + "/config.json";
    public static final String DEFAULT_PHOTO_PATH = DEFAULT_DIR + "/photo.jpg";
    public static final String DEFAULT_VIDEO_PATH = DEFAULT_DIR + "/video.mp4";
    public static final double DEFAULT_LATITUDE = 0.0d;
    public static final double DEFAULT_LONGITUDE = 0.0d;

    private static final long MIN_RELOAD_INTERVAL_MS = 500;

    private static volatile Cached cached;

    private ConfigLoader() {
    }

    public static Config get() {
        Cached local = cached;
        long now = SystemClock.uptimeMillis();
        if (local != null && (now - local.lastCheckedAtMs) < MIN_RELOAD_INTERVAL_MS) {
            return local.config;
        }

        File configFile = new File(DEFAULT_CONFIG_PATH);
        boolean exists = configFile.exists() && configFile.isFile();
        long lastModified = exists ? configFile.lastModified() : 0L;
        long length = exists ? configFile.length() : -1L;

        if (local != null && local.configExists == exists
                && local.configLastModified == lastModified
                && local.configLength == length) {
            cached = new Cached(local.config, now, exists, lastModified, length);
            return local.config;
        }

        Config disk = exists ? loadFromDisk(configFile) : null;
        if (disk == null) {
            disk = Config.defaultDisabled();
        }
        cached = new Cached(disk, now, exists, lastModified, length);
        return disk;
    }

    @NonNull
    public static Config loadOrDefault() {
        Config config = loadFromDisk();
        return config != null ? config : Config.defaultDisabled();
    }

    @Nullable
    public static Config loadFromDisk() {
        return loadFromDisk(new File(DEFAULT_CONFIG_PATH));
    }

    @Nullable
    private static Config loadFromDisk(File configFile) {
        if (!configFile.exists() || !configFile.isFile()) {
            return null;
        }
        try {
            byte[] bytes = FileOps.readAllBytes(configFile, 256 * 1024);
            if (bytes == null) {
                return null;
            }
            String json = new String(bytes, StandardCharsets.UTF_8);
            return fromJson(json);
        } catch (IOException e) {
            Logger.log("读取配置失败:", e);
            return null;
        }
    }

    public static void saveToDisk(Config config) throws IOException {
        File dir = new File(DEFAULT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建目录: " + DEFAULT_DIR);
        }

        JSONObject obj = new JSONObject();
        try {
            obj.put("enabled", config.enabled);
            obj.put("enablePhoto", config.enablePhoto);
            obj.put("enableVideo", config.enableVideo);
            obj.put("enableLocation", config.enableLocation);
            obj.put("mode", config.mode);
            JSONArray arr = new JSONArray();
            for (String pkg : config.allowlist) {
                arr.put(pkg);
            }
            obj.put("allowlist", arr);
            obj.put("photoPath", config.photoPath);
            obj.put("videoPath", config.videoPath);
            obj.put("latitude", config.latitude);
            obj.put("longitude", config.longitude);
        } catch (JSONException e) {
            throw new IOException("序列化配置失败: " + e.getMessage(), e);
        }

        File f = new File(DEFAULT_CONFIG_PATH);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f, false);
            fos.write(obj.toString().getBytes(StandardCharsets.UTF_8));
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }

        boolean exists = f.exists() && f.isFile();
        cached = new Cached(
                config,
                SystemClock.uptimeMillis(),
                exists,
                exists ? f.lastModified() : 0L,
                exists ? f.length() : -1L
        );
    }

    private static Config fromJson(String json) throws IOException {
        try {
            JSONObject obj = new JSONObject(json);
            boolean enabled = obj.optBoolean("enabled", false);
            boolean enablePhoto = obj.optBoolean("enablePhoto", true);
            boolean enableVideo = obj.optBoolean("enableVideo", true);
            boolean enableLocation = obj.optBoolean("enableLocation", false);
            String mode = obj.optString("mode", Config.MODE_ALLOWLIST);
            String photoPath = obj.optString("photoPath", DEFAULT_PHOTO_PATH);
            String videoPath = obj.optString("videoPath", DEFAULT_VIDEO_PATH);
            double latitude = sanitizeLatitude(obj.optDouble("latitude", DEFAULT_LATITUDE));
            double longitude = sanitizeLongitude(obj.optDouble("longitude", DEFAULT_LONGITUDE));

            List<String> allowlist = new ArrayList<>();
            JSONArray arr = obj.optJSONArray("allowlist");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    String pkg = arr.optString(i, "").trim();
                    if (!pkg.isEmpty()) {
                        allowlist.add(pkg);
                    }
                }
            }

            if (!Config.MODE_ALL.equals(mode) && !Config.MODE_ALLOWLIST.equals(mode)) {
                mode = Config.MODE_ALLOWLIST;
            }

            return new Config(
                    enabled,
                    enablePhoto,
                    enableVideo,
                    enableLocation,
                    mode,
                    allowlist,
                    photoPath,
                    videoPath,
                    latitude,
                    longitude
            );
        } catch (JSONException e) {
            throw new IOException("解析配置失败: " + e.getMessage(), e);
        }
    }

    private static double sanitizeLatitude(double latitude) {
        if (Double.isNaN(latitude) || latitude < -90.0d || latitude > 90.0d) {
            return DEFAULT_LATITUDE;
        }
        return latitude;
    }

    private static double sanitizeLongitude(double longitude) {
        if (Double.isNaN(longitude) || longitude < -180.0d || longitude > 180.0d) {
            return DEFAULT_LONGITUDE;
        }
        return longitude;
    }

    private static final class Cached {
        final Config config;
        final long lastCheckedAtMs;
        final boolean configExists;
        final long configLastModified;
        final long configLength;

        Cached(
                Config config,
                long lastCheckedAtMs,
                boolean configExists,
                long configLastModified,
                long configLength
        ) {
            this.config = config;
            this.lastCheckedAtMs = lastCheckedAtMs;
            this.configExists = configExists;
            this.configLastModified = configLastModified;
            this.configLength = configLength;
        }
    }
}
