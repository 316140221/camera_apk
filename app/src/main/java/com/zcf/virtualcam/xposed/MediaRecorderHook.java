package com.zcf.virtualcam.xposed;

import android.media.MediaRecorder;
import android.os.Build;

import java.io.File;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class MediaRecorderHook {

    private MediaRecorderHook() {
    }

    private static final Map<MediaRecorder, String> OUTPUT_PATHS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile boolean installed = false;

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;

        XposedHelpers.findAndHookMethod(MediaRecorder.class, "setOutputFile", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.thisObject instanceof MediaRecorder && param.args != null && param.args.length == 1) {
                    Object path = param.args[0];
                    if (path instanceof String) {
                        OUTPUT_PATHS.put((MediaRecorder) param.thisObject, (String) path);
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod(MediaRecorder.class, "setOutputFile", FileDescriptor.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!(param.thisObject instanceof MediaRecorder) || param.args == null || param.args.length != 1) {
                    return;
                }
                Object fdObj = param.args[0];
                if (!(fdObj instanceof FileDescriptor)) {
                    return;
                }
                String resolved = tryResolvePathFromFd((FileDescriptor) fdObj);
                if (resolved != null) {
                    OUTPUT_PATHS.put((MediaRecorder) param.thisObject, resolved);
                }
            }
        });

        XposedBridge.hookAllMethods(MediaRecorder.class, "stop", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.hasThrowable()) {
                    return;
                }
                if (!(param.thisObject instanceof MediaRecorder)) {
                    return;
                }
                MediaRecorder recorder = (MediaRecorder) param.thisObject;
                Config config = ConfigLoader.get();
                if (!config.enabled || !config.enableVideo) {
                    return;
                }
                String output = OUTPUT_PATHS.remove(recorder);
                if (output == null || output.trim().isEmpty()) {
                    return;
                }
                if (output.equals(config.videoPath)) {
                    return;
                }
                try {
                    FileOps.replaceFile(new File(config.videoPath), new File(output));
                } catch (Exception e) {
                    Logger.log("覆盖视频失败:", e);
                }
            }
        });
    }

    private static String tryResolvePathFromFd(FileDescriptor fd) {
        try {
            Field field = FileDescriptor.class.getDeclaredField("descriptor");
            field.setAccessible(true);
            int rawFd = field.getInt(fd);
            if (rawFd < 0) {
                return null;
            }
            if (Build.VERSION.SDK_INT >= 21) {
                return android.system.Os.readlink("/proc/self/fd/" + rawFd);
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
