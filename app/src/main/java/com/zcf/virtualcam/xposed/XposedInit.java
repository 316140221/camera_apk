package com.zcf.virtualcam.xposed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class XposedInit implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }
        if ("com.zcf.virtualcam".equals(lpparam.packageName)) {
            return;
        }

        Config config = ConfigLoader.get();
        if (!config.isPackageEnabled(lpparam.packageName)) {
            return;
        }

        try {
            Camera1Hook.install();
            MediaRecorderHook.install();
        } catch (Throwable t) {
            Logger.log("安装 Hook 失败:", t);
        }
    }
}
