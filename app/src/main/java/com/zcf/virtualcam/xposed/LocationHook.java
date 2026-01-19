package com.zcf.virtualcam.xposed;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public final class LocationHook {

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final String DEFAULT_PROVIDER = "gps";
    private static final float DEFAULT_ACCURACY = 5.0f;
    private static final double DEFAULT_ALTITUDE = 0.0d;
    private static final float DEFAULT_SPEED = 0.0f;
    private static final float DEFAULT_BEARING = 0.0f;

    private LocationHook() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        hookLocationManager();
        hookLocation();
        hookSettings();
    }

    private static void hookLocationManager() {
        XposedBridge.hookAllMethods(LocationManager.class, "getLastKnownLocation", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Config config = getActiveConfig();
                if (config == null) {
                    return;
                }
                Location origin = param.getResult() instanceof Location ? (Location) param.getResult() : null;
                param.setResult(buildLocation(origin, config));
            }
        });

        XposedBridge.hookAllMethods(LocationManager.class, "requestLocationUpdates", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Config config = getActiveConfig();
                if (config == null) {
                    return;
                }
                int index = findListenerIndex(param.args);
                if (index < 0) {
                    return;
                }
                LocationListener original = (LocationListener) param.args[index];
                if (original == null) {
                    return;
                }
                param.args[index] = wrapListener(original);
            }
        });

        XposedBridge.hookAllMethods(LocationManager.class, "requestSingleUpdate", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Config config = getActiveConfig();
                if (config == null) {
                    return;
                }
                int index = findListenerIndex(param.args);
                if (index < 0) {
                    return;
                }
                LocationListener original = (LocationListener) param.args[index];
                if (original == null) {
                    return;
                }
                param.args[index] = wrapListener(original);
            }
        });
    }

    private static void hookLocation() {
        XposedBridge.hookAllMethods(Location.class, "getLatitude", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Config config = getActiveConfig();
                if (config != null) {
                    param.setResult(config.latitude);
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "getLongitude", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Config config = getActiveConfig();
                if (config != null) {
                    param.setResult(config.longitude);
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "getAccuracy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(DEFAULT_ACCURACY);
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "getAltitude", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(DEFAULT_ALTITUDE);
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "getSpeed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(DEFAULT_SPEED);
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "getBearing", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(DEFAULT_BEARING);
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "getProvider", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(DEFAULT_PROVIDER);
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "getTime", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(System.currentTimeMillis());
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "getElapsedRealtimeNanos", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(SystemClock.elapsedRealtimeNanos());
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "hasAccuracy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(true);
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "hasAltitude", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(true);
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "hasSpeed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(true);
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "hasBearing", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(true);
                }
            }
        });

        XposedBridge.hookAllMethods(Location.class, "isFromMockProvider", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() != null) {
                    param.setResult(false);
                }
            }
        });
    }

    private static void hookSettings() {
        XposedBridge.hookAllMethods(Settings.Secure.class, "getInt", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (getActiveConfig() == null) {
                    return;
                }
                if (param.args == null || param.args.length < 2) {
                    return;
                }
                Object nameObj = param.args[1];
                if (!(nameObj instanceof String)) {
                    return;
                }
                String name = (String) nameObj;
                if ("mock_location".equals(name) || "allow_mock_location".equals(name)) {
                    param.setResult(0);
                }
            }
        });
    }

    @Nullable
    private static Config getActiveConfig() {
        Config config = ConfigLoader.get();
        if (!config.enabled || !config.enableLocation) {
            return null;
        }
        return config;
    }

    private static int findListenerIndex(Object[] args) {
        if (args == null) {
            return -1;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof LocationListener) {
                return i;
            }
        }
        return -1;
    }

    private static LocationListener wrapListener(LocationListener original) {
        return new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Config config = getActiveConfig();
                if (config == null) {
                    original.onLocationChanged(location);
                    return;
                }
                original.onLocationChanged(buildLocationInPlace(location, config));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                original.onStatusChanged(provider, status, extras);
            }

            @Override
            public void onProviderEnabled(String provider) {
                original.onProviderEnabled(provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                original.onProviderDisabled(provider);
            }
        };
    }

    private static Location buildLocation(@Nullable Location base, Config config) {
        return buildLocationInPlace(base != null ? new Location(base) : null, config);
    }

    private static Location buildLocationInPlace(@Nullable Location target, Config config) {
        if (target == null) {
            target = new Location(DEFAULT_PROVIDER);
        } else if (target.getProvider() == null) {
            target.setProvider(DEFAULT_PROVIDER);
        }
        target.setLatitude(config.latitude);
        target.setLongitude(config.longitude);
        target.setAccuracy(DEFAULT_ACCURACY);
        target.setAltitude(DEFAULT_ALTITUDE);
        target.setSpeed(DEFAULT_SPEED);
        target.setBearing(DEFAULT_BEARING);
        target.setTime(System.currentTimeMillis());
        target.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        return target;
    }
}
