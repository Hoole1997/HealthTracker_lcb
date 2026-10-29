# RestoreUtils 通过 Java 反射读取运行时注解和字段名。
# 仅导出这个框架的反射契约，不影响宿主应用的整体混淆/告警策略。
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keep @interface com.healthtracker.framework.util.Restore
-keepclassmembers class * {
    @com.healthtracker.framework.util.Restore <fields>;
}
