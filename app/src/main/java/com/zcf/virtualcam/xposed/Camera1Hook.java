package com.zcf.virtualcam.xposed;

import android.hardware.Camera;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

import java.util.concurrent.atomic.AtomicBoolean;

public final class Camera1Hook {

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private Camera1Hook() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        XposedBridge.hookAllMethods(Camera.class, "takePicture", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Config config = ConfigLoader.get();
                if (!config.enabled || !config.enablePhoto) {
                    return;
                }

                int jpegIndex = findLastJpegCallbackIndex(param.args);
                if (jpegIndex < 0) {
                    return;
                }

                Object original = param.args[jpegIndex];
                if (!(original instanceof Camera.PictureCallback)) {
                    return;
                }
                Camera.PictureCallback originalCb = (Camera.PictureCallback) original;
                if (originalCb == null) {
                    return;
                }

                param.args[jpegIndex] = (Camera.PictureCallback) (data, camera) -> {
                    try {
                        Config latest = ConfigLoader.get();
                        if (!latest.enabled || !latest.enablePhoto) {
                            originalCb.onPictureTaken(data, camera);
                            return;
                        }
                        byte[] replacement = FileOps.readCachedBytes(latest.photoPath, 10 * 1024 * 1024);
                        if (replacement != null && replacement.length > 0) {
                            originalCb.onPictureTaken(replacement, camera);
                            return;
                        }
                        originalCb.onPictureTaken(data, camera);
                    } catch (Throwable t) {
                        Logger.log("Camera1 PictureCallback 处理异常:", t);
                        try {
                            originalCb.onPictureTaken(data, camera);
                        } catch (Throwable t2) {
                            Logger.log("Camera1 PictureCallback 兜底回调异常:", t2);
                        }
                    }
                };
            }
        });
    }

    private static int findLastJpegCallbackIndex(Object[] args) {
        if (args == null) {
            return -1;
        }
        for (int i = args.length - 1; i >= 0; i--) {
            if (args[i] instanceof Camera.PictureCallback) {
                return i;
            }
        }
        return -1;
    }
}
