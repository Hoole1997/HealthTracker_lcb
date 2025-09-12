package com.healthtracker.framework.util;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class NetWorkUtils {

    /**
     * 判断wifi是否
     *
     * @param context
     * @return
     */
    public static boolean isWifi(Context context) {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (mNetworkInfo.isAvailable()) {
            mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null && mNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI && mNetworkInfo.isConnected()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断网络是否可用
     *
     * @param context
     * @return
     */
    public static boolean isAvailable(Context context) {
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isAvailable();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isSimCardInserted(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            return telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
        }
        return false;
    }

    public static void toNetSettingPage(Context context) {
        if (isSimCardInserted(context)) {
            context.startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
        } else {
            context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
    }
}
