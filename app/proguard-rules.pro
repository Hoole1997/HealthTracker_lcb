# 应用级运行时兼容规则；混淆策略单独放在 proguard-obfuscation.pro。
# 保留可还原的崩溃堆栈，以及 Gson/注解反射所需的类型信息。
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,InnerClasses,EnclosingMethod,Exceptions
-renamesourcefileattribute SourceFile

# native 反向查找还需要参数/返回值类型的原名；仅 keepmembernames 不能防止裁剪。
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}

# JavaScript 桥按方法名访问；RestoreUtils 的约束由 framework consumer 规则导出。
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 无 SerializedName 的配置模型和本地 JSON 缓存按字段名读写。
-keep class com.daily.health.manager.config.models.** { *; }
-keep class com.daily.health.manager.data.enums.BloodSugarRanges { *; }

# R8 full mode 下匿名 TypeToken 子类及泛型签名必须一起保留。
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }


# 保留 Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 onClick 等 XML 引用的方法
-keepclassmembers class * {
    public void *(android.view.View);
}

# ==================== 第三方库保护 ====================

# Koin DI（反射）
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}

# Room 数据库
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Gson 序列化
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Retrofit（如果使用）
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ViewBinding 的静态工厂由基类/布局反射使用。
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(android.view.LayoutInflater);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static *** bind(android.view.View);
}

# ==================== WorkManager ====================

-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }


# 可选的 JVM/TLS 实现；不屏蔽应用或 SDK 的其他缺失依赖告警。
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn retrofit2.Platform$Java8
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# 生成代码由构建任务管理，维持既有保留行为。
-keep class com.daily.health.manager.junk.** { *; }
