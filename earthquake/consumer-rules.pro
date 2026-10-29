# Rules for consumers of this library module (if any)
# 地震模型通过 Serializable 在页面/保存状态之间传递；序列化依赖类名和字段名。
-keep class com.healthtracker.earthquake.model.** implements java.io.Serializable { *; }
