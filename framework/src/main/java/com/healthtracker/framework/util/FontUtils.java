package com.healthtracker.framework.util;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;

import java.util.Locale;


/**
 * Created by raoyongchao on 16/5/25.
 */
public class FontUtils {

    private static FontUtils instance;
    private Typeface regular_type;
    private Typeface regular_medium_type;
    private Typeface regular_light;
    private Typeface regular_bold;

    public synchronized static FontUtils getInstance() {
        if (instance == null) {
            instance = new FontUtils();
        }
        return instance;
    }

    /**
     * 后去RobotoMedium
     *
     * @return
     */
    public Typeface getRobotoMedium() {
        if (regular_medium_type == null) {
            try {
                regular_medium_type = Typeface.createFromFile("/system/fonts/Roboto-Medium.ttf");
            } catch (Exception e) {
                e.printStackTrace();
                regular_medium_type = Typeface.DEFAULT;
            }
        }
        return regular_medium_type;
    }

    public Typeface getRobotoRegular() {
        if (regular_type == null) {
            try {
                regular_type = Typeface.createFromFile("/system/fonts/Roboto-Regular.ttf");
            } catch (Exception e) {
                e.printStackTrace();
                regular_type = Typeface.DEFAULT;
            }
        }
        return regular_type;
    }

    public Typeface getRobotoLight() {
        if (regular_light == null) {
            try {
                regular_light = Typeface.createFromFile("/system/fonts/Roboto-Light.ttf");
            } catch (Exception e) {
                e.printStackTrace();
                regular_light = Typeface.DEFAULT;
            }
        }
        return regular_light;
    }

    public Typeface getRobotoBold() {
        if (regular_bold == null) {
            try {
                regular_bold = Typeface.createFromFile("/system/fonts/Roboto-Bold.ttf");
            } catch (Exception e) {
                e.printStackTrace();
                regular_bold = Typeface.DEFAULT_BOLD;
            }
        }
        return regular_bold;
    }




    private String getCurrentLang(Context context) {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            locale = context.getResources().getConfiguration().locale;
        }
        if (locale != null) {
            String language = locale.getLanguage();
            return !TextUtils.isEmpty(language) ? language.toLowerCase() : "";
        } else {
            return "en";
        }
    }
}
