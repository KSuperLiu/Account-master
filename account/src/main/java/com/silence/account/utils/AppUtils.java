package com.silence.account.utils;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 *
 */
public class AppUtils {
    private AppUtils() {
    }

    public static String getAppVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "1.0.0";
    }
}
