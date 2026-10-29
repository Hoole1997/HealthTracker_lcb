# Launcher SDK 的部分方法已经 native 虚拟化，R8 看不到 native 内保存的类名和方法名。
# 这些是运行时 ABI，不是普通的 Kotlin 调用；keepnames 无法防止方法被裁剪或内联。
# 保留标准库及 Kotlin 扩展运行时，覆盖 Intrinsics / Lambda / CollectionsKt / 协程等隐藏引用。
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# 加固的 UI 代码还会通过 JNI 按原签名访问 Jetpack（包括返回键、生命周期、Compose）。
# 不能只保留类名或构造函数，否则 addCallback 等方法仍会被改名/内联而导致 NoSuchMethodError。
-keep class androidx.** { *; }

# SDK 已经完成厂商混淆和加固，不再对其自身符号进行二次裁剪、重命名或合并。
# includedescriptorclasses 同时保护跨 JNI 签名中的类型。
-keep,includedescriptorclasses class com.health.tracker.launcher.tool.** { *; }
-keep,includedescriptorclasses class com.leafmotivation.quizguessoncolor.** { *; }
-keep,includedescriptorclasses class com.jiagu.sdk.** { *; }
