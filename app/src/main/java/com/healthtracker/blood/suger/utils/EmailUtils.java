package com.healthtracker.blood.suger.utils;

import android.content.Context;


public class EmailUtils {
    public final static String PACKAGE_GMAIL = "com.google.android.gm";
    public final static String PACKAGE_EMAIL_APP = "com.android.email";
    private static EmailUtils utils;

    private EmailUtils() {

    }

    public synchronized static EmailUtils getInstance() {
        if (utils == null) {
            utils = new EmailUtils();
        }
        return utils;
    }

    /**
     * 检测是否安装有gmail
     *
     * @param context
     */
    public boolean hasGmail(Context context) {
        return AppUtils.hasInstalled(context, PACKAGE_GMAIL);
    }

    /**
     * 检测是否有email客户端，如果没有Gmail的情况下要使用该方法
     *
     * @return
     */
    public boolean hasEmailApp(Context context) {
        return AppUtils.hasInstalled(context, PACKAGE_EMAIL_APP);
    }

}
