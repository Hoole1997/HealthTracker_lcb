# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep all public APIs
-keep public class net.corekit.adsdk.manager.AdSdkManager {
    public *;
}

-keep public class net.corekit.adsdk.core.** {
    public *;
}

-keep public class net.corekit.adsdk.config.** {
    public *;
}

-keep public class net.corekit.adsdk.event.** {
    public *;
}

# Keep AdMob SDK classes
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-dontwarn com.google.android.libraries.ads.mobile.sdk.**

# Keep coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep event callbacks
-keepclassmembers class * {
    @net.corekit.adsdk.event.AdEventListener *;
}
