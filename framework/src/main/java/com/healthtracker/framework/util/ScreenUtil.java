package com.healthtracker.framework.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class ScreenUtil {

    public static float dp2px(float dp) {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return displayMetrics.density * dp + 0.5F;
    }

    public static int px2dp(float px) {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return (int) (px / displayMetrics.density + 0.5F);
    }

    public static int screenWidth() {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }

    public static int screenHeight() {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return displayMetrics.heightPixels;
    }

    public static float getDensity() {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return displayMetrics.density;
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 是否是手机宽度小于等于480的手机
     *
     * @return
     */
    public static boolean isWidthLess480() {
        return Resources.getSystem().getDisplayMetrics().widthPixels <= 480;
    }

    public static boolean isWidthDpLess300(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels/dm.density <= 300;
    }

    /**
     * 开启全屏模式
     *
     * @param activity 针对的页面
     */
    public static void openFullScreenMode(Activity activity) {
        if (null != activity) {
            openFullScreenMode(activity.getWindow());
        }
    }

    public static void openFullScreenMode(Window window) {
        if (null != window) {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
            View decorView = window.getDecorView();
            if (null != decorView) {
                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);
                openFullScreenModel(window);
            }
        }
    }

    /**
     * 兼容9.0刘海屏
     *
     * @param activity activity
     */
    private static void openFullScreenModel(Activity activity) {
        try {
            openFullScreenModel(activity.getWindow());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void openFullScreenModel(Window window) {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setAttributes(lp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
