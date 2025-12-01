package com.healthtracker.blood.suger.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.Log;

public class AppUtils {
    public AppUtils() {
    }

    public static boolean hasInstalled(Context context, String packageName) {
        PackageInfo packageInfo = null;

        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 8192);
        } catch (Throwable var4) {
            packageInfo = null;
        }

        if (packageInfo == null) {
            Log.e("AppUtils", "No:" + packageName);
            return false;
        } else {
            return true;
        }
    }

    public static void openPlay(Context context, String url) {
        Intent intent = null;

        try {
            intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.android.vending");
            context.startActivity(intent);
        } catch (Exception var4) {
            var4.printStackTrace();
            intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }

    }
}
