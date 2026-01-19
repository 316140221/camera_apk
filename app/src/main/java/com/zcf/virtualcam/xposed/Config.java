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
    public final boolean enableLocation;
    @NonNull
    public final String mode;
    @NonNull
    public final List<String> allowlist;
    @NonNull
    public final String photoPath;
    @NonNull
    public final String videoPath;
    public final double latitude;
    public final double longitude;

    public Config(
            boolean enabled,
            boolean enablePhoto,
            boolean enableVideo,
            boolean enableLocation,
            @NonNull String mode,
            @NonNull List<String> allowlist,
            @NonNull String photoPath,
            @NonNull String videoPath,
            double latitude,
            double longitude
    ) {
        this.enabled = enabled;
        this.enablePhoto = enablePhoto;
        this.enableVideo = enableVideo;
        this.enableLocation = enableLocation;
        this.mode = mode;
        this.allowlist = Collections.unmodifiableList(new ArrayList<>(allowlist));
        this.photoPath = photoPath;
        this.videoPath = videoPath;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @NonNull
    public static Config defaultDisabled() {
        return new Config(
                false,
                true,
                true,
                false,
                MODE_ALLOWLIST,
                Collections.emptyList(),
                ConfigLoader.DEFAULT_PHOTO_PATH,
                ConfigLoader.DEFAULT_VIDEO_PATH,
                ConfigLoader.DEFAULT_LATITUDE,
                ConfigLoader.DEFAULT_LONGITUDE
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

    @NonNull
    public static Builder builder(@NonNull Config base) {
        return new Builder(base);
    }

    public static final class Builder {
        private boolean enabled;
        private boolean enablePhoto;
        private boolean enableVideo;
        private boolean enableLocation;
        private String mode;
        private List<String> allowlist;
        private String photoPath;
        private String videoPath;
        private double latitude;
        private double longitude;

        private Builder(@NonNull Config base) {
            this.enabled = base.enabled;
            this.enablePhoto = base.enablePhoto;
            this.enableVideo = base.enableVideo;
            this.enableLocation = base.enableLocation;
            this.mode = base.mode;
            this.allowlist = new ArrayList<>(base.allowlist);
            this.photoPath = base.photoPath;
            this.videoPath = base.videoPath;
            this.latitude = base.latitude;
            this.longitude = base.longitude;
        }

        @NonNull
        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @NonNull
        public Builder setEnablePhoto(boolean enablePhoto) {
            this.enablePhoto = enablePhoto;
            return this;
        }

        @NonNull
        public Builder setEnableVideo(boolean enableVideo) {
            this.enableVideo = enableVideo;
            return this;
        }

        @NonNull
        public Builder setEnableLocation(boolean enableLocation) {
            this.enableLocation = enableLocation;
            return this;
        }

        @NonNull
        public Builder setMode(@NonNull String mode) {
            this.mode = mode;
            return this;
        }

        @NonNull
        public Builder setAllowlist(@NonNull List<String> allowlist) {
            this.allowlist = new ArrayList<>(allowlist);
            return this;
        }

        @NonNull
        public Builder setPhotoPath(@NonNull String photoPath) {
            this.photoPath = photoPath;
            return this;
        }

        @NonNull
        public Builder setVideoPath(@NonNull String videoPath) {
            this.videoPath = videoPath;
            return this;
        }

        @NonNull
        public Builder setLatitude(double latitude) {
            this.latitude = latitude;
            return this;
        }

        @NonNull
        public Builder setLongitude(double longitude) {
            this.longitude = longitude;
            return this;
        }

        @NonNull
        public Config build() {
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
        }
    }
}
