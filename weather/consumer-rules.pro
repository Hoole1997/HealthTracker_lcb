# Weather Module ProGuard Rules

# Keep data models used by Gson
-keep class com.android.common.weather.model.** { *; }

# Keep Retrofit interface
-keep interface com.android.common.weather.network.WeatherApiService { *; }

# Keep MD5Utils if accessed via reflection or to be safe
-keep class com.android.common.weather.MD5Utils { *; }

-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type



