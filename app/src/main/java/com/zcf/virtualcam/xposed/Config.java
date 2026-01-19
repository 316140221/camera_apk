package com.zcf.virtualcam.xposed;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Config {

    public static final String MODE_ALLOWLIST = "allowlist";
    public static final String MODE_ALL = "all";

    public final boolean enabled;
    public final boolean enablePhoto;
    public final boolean enableVideo;
    @NonNull
    public final String mode;
    @NonNull
    public final List<String> allowlist;
    @NonNull
    public final String photoPath;
    @NonNull
    public final String videoPath;

    public Config(
            boolean enabled,
            boolean enablePhoto,
            boolean enableVideo,
            @NonNull String mode,
            @NonNull List<String> allowlist,
            @NonNull String photoPath,
            @NonNull String videoPath
    ) {
        this.enabled = enabled;
        this.enablePhoto = enablePhoto;
        this.enableVideo = enableVideo;
        this.mode = mode;
        this.allowlist = Collections.unmodifiableList(new ArrayList<>(allowlist));
        this.photoPath = photoPath;
        this.videoPath = videoPath;
    }

    @NonNull
    public static Config defaultDisabled() {
        return new Config(
                false,
                true,
                true,
                MODE_ALLOWLIST,
                Collections.emptyList(),
                ConfigLoader.DEFAULT_PHOTO_PATH,
                ConfigLoader.DEFAULT_VIDEO_PATH
        );
    }

    public boolean isPackageEnabled(@NonNull String packageName) {
        if (!enabled) {
            return false;
        }
        if (MODE_ALL.equals(mode)) {
            return true;
        }
        for (String pkg : allowlist) {
            if (packageName.equals(pkg)) {
                return true;
            }
        }
        return false;
    }
}

