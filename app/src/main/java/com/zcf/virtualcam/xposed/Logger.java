package com.zcf.virtualcam.xposed;

import android.util.Log;

import java.lang.reflect.Method;

public final class Logger {

    private static final String TAG = "VirtualCam";
    private static final Method XPOSED_LOG = resolveXposedLog();

    private Logger() {
    }

    public static void log(String message) {
        if (message == null) {
            message = "null";
        }
        if (XPOSED_LOG != null) {
            try {
                XPOSED_LOG.invoke(null, message);
                return;
            } catch (Throwable ignored) {
            }
        }
        Log.i(TAG, message);
    }

    public static void log(String message, Throwable t) {
        log(message + " " + String.valueOf(t));
    }

    private static Method resolveXposedLog() {
        try {
            Class<?> cls = Class.forName("de.robv.android.xposed.XposedBridge");
            return cls.getMethod("log", String.class);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

