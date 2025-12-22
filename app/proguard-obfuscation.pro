# ==================== 增强混淆规则 ====================
# 目标：最大化 APK 静态分析差异化
# 
# 此文件包含激进的混淆规则，用于：
# 1. 移除调试信息和元数据
# 2. 增强类名/方法名混淆
# 3. 优化代码结构

# ==================== 阶段3: 元数据清理 ====================

# 移除源文件名（使用空字符串替代）
-renamesourcefileattribute ''

# 不保留源文件和行号信息（最大化混淆）
# 注意：这会使 Crashlytics 堆栈跟踪难以阅读，但增加了混淆程度
# 如需调试，可注释以下行并启用 -keepattributes SourceFile,LineNumberTable
-keepattributes !SourceFile,!LineNumberTable

# 移除本地变量表（调试信息）
-keepattributes !LocalVariableTable,!LocalVariableTypeTable

# 移除方法参数名
-keepattributes !MethodParameters

# 移除内部类信息（除非必要）
# -keepattributes !InnerClasses

# ==================== 阶段2 & 4: 代码混淆增强 ====================

# 启用激进混淆
# 注意：-repackageclasses 会覆盖 -flattenpackagehierarchy，只使用一个
-repackageclasses ''
-allowaccessmodification

# 混淆优化
-overloadaggressively

# 使用自定义混淆字典（阶段4）
# 优先使用动态生成的字典（每次构建随机化）
# 如果动态字典不存在，则使用静态字典
-obfuscationdictionary proguard-dictionary-generated.txt
-classobfuscationdictionary proguard-dictionary-generated.txt
-packageobfuscationdictionary proguard-dictionary-generated.txt

# 移除未使用的类成员
-dontwarn **

# ==================== 必须保留的规则 ====================

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

# 保留 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
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

# ==================== Kotlin 相关 ====================

# Kotlin 协程
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlin 反射（如果使用）
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ==================== ViewBinding ====================

-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(android.view.LayoutInflater);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static *** bind(android.view.View);
}

# ==================== WorkManager ====================

-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ==================== 抑制警告 ====================

-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.Platform$Java8
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
