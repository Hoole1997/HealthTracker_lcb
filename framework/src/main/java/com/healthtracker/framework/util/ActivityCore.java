package com.healthtracker.framework.util;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActivityCore {
    /**
     * 获取当前Activity Theme是不是透明的
     * 主要用于适配26 android O
     * theme 的style 中包含true行为，并设置了activity方向引引起的闪退：Only fullscreen activities can request orientation
     *
     * @param activity
     * @return
     */
    public static boolean isTranslucentOrFloating(Activity activity) {
        boolean isTranslucentOrFloating = false;
        try {
            int[] styleableRes = (int[]) Class.forName("com.android.internal.R$styleable").getField("Window").get(null);
            final TypedArray ta = activity.obtainStyledAttributes(styleableRes);
            Method m = ActivityInfo.class.getMethod("isTranslucentOrFloating", TypedArray.class);
            m.setAccessible(true);
            isTranslucentOrFloating = (boolean) m.invoke(null, ta);
            m.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isTranslucentOrFloating;
    }

    public static boolean fixOrientation(Activity activity) {
        try {
            Field field = Activity.class.getDeclaredField("mActivityInfo");
            field.setAccessible(true);
            ActivityInfo o = (ActivityInfo) field.get(activity);
            o.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            field.setAccessible(false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
