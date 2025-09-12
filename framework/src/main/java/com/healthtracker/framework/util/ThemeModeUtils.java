package com.healthtracker.framework.util;

import static com.healthtracker.framework.util.SysVersionKt.isLeast10;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeModeUtils {

    /**
     * 0--跟随系统
     * 1--白天
     * 2--夜间
     * 切换主题色(需要配置：configChanges="uiMode")
     *
     * @param mode
     */
    public static void setAppTheme(int mode) {
        if (isLeast10()) {
            if (mode == 1) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (mode == 2) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        }
    }

    /**
     * 获取app是否深色模式
     *
     * @return
     */
    public static boolean isAppDark(Context context) {
        if (isLeast10()) {
            int nightMode = AppCompatDelegate.getDefaultNightMode();
            //app是深色模式或者app是跟随系统模式并且系统是深色模式
            return nightMode == AppCompatDelegate.MODE_NIGHT_YES || nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && isSysDark(context);
        }
        return true;
    }

    /**
     * 获取系统是否是深色模式
     *
     * @param context
     * @return
     */
    private static boolean isSysDark(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
