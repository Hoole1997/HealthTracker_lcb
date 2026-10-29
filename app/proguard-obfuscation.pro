# 应用代码仍执行 R8 压缩、优化和重命名。
# ABI / 反射兼容规则不应混入策略文件，见另外两个 proguard 文件。
# 使用 R8 支持的默认优化策略，避免叠加旧 ProGuard 的激进接口合并等选项。
-repackageclasses ''

# 动态字典只影响允许混淆的应用代码，不能覆盖 SDK 运行时的 keep 约束。
-obfuscationdictionary proguard-dictionary-generated.txt
-classobfuscationdictionary proguard-dictionary-generated.txt
-packageobfuscationdictionary proguard-dictionary-generated.txt
