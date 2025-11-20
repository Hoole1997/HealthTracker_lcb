package com.healthtracker.blood.suger.utils;

import android.annotation.SuppressLint;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

public class WebViewUtils {

    @SuppressLint("RequiresFeature")
    public static void initWebViewSetting(WebView webView, boolean jsEnable, boolean allowDark) {
        WebSettings settings = getWebSettings(webView, jsEnable);
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, allowDark);
            }
        } catch (Exception ignored) {

        }
        webView.removeJavascriptInterface("searchBoxJavaBridge_");
        webView.removeJavascriptInterface("accessibility");
        webView.removeJavascriptInterface("accessibilityTraversal");
    }

    private static @NonNull WebSettings getWebSettings(WebView webView, boolean jsEnable) {
        WebSettings settings = webView.getSettings();
        settings.setAllowFileAccess(false);
        settings.setJavaScriptEnabled(jsEnable);
        settings.setGeolocationEnabled(true);
        settings.setSupportZoom(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSavePassword(false);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        return settings;
    }
}
