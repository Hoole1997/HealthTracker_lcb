/**
 * 动态生成混淆字典的 Gradle 脚本
 * 
 * 每次 Release 构建时生成随机的混淆字典，使每次构建的 APK 在静态分析时呈现不同特征
 */

import kotlin.random.Random

// 生成随机混淆字典
tasks.register("generateObfuscationDictionary") {
    group = "obfuscation"
    description = "Generate random obfuscation dictionary for each build"

    val dictionaryFile = file("proguard-dictionary-generated.txt")

    outputs.file(dictionaryFile)
    outputs.upToDateWhen { false } // 每次都重新生成

    doLast {
        val random = Random(System.currentTimeMillis())
        val dictionary = mutableListOf<String>()

        // 生成单字符名称 (a-z)
        ('a'..'z').forEach { dictionary.add(it.toString()) }

        // 生成双字符名称 (aa-zz)，随机顺序
        val twoCharNames = mutableListOf<String>()
        ('a'..'z').forEach { first ->
            ('a'..'z').forEach { second ->
                twoCharNames.add("$first$second")
            }
        }
        twoCharNames.shuffle(java.util.Random(System.currentTimeMillis()))
        dictionary.addAll(twoCharNames)

        // 生成三字符名称 (aaa-zzz)，随机选择一部分
        val threeCharNames = mutableListOf<String>()
        val chars = ('a'..'z').toList()
        repeat(500) {
            val name = buildString {
                repeat(3) {
                    append(chars[random.nextInt(chars.size)])
                }
            }
            if (name !in threeCharNames) {
                threeCharNames.add(name)
            }
        }
        threeCharNames.shuffle(java.util.Random(System.currentTimeMillis() + 1))
        dictionary.addAll(threeCharNames)

        // 添加一些看起来像真实代码的名称（增加混淆效果）
        // 注意：避免与双字符名称重复
        val fakeNames = listOf(
            "init", "get", "set", "run", "call", "exec", "load", "save",
            "read", "write", "open", "close", "start", "stop", "create",
            "delete", "update", "find", "check", "validate", "process",
            "handle", "parse", "format", "convert", "build", "make",
            "has", "can", "should", "will", "may"
        )
        // 过滤掉已存在的名称
        val uniqueFakeNames = fakeNames.filter { it !in dictionary }
        val shuffledFakeNames = uniqueFakeNames.shuffled(java.util.Random(System.currentTimeMillis() + 2))
        dictionary.addAll(shuffledFakeNames)

        // 写入字典文件
        dictionaryFile.writeText(dictionary.joinToString("\n"))

        logger.lifecycle("Generated obfuscation dictionary with ${dictionary.size} entries")
    }
}

// 在 Release 构建前生成字典
tasks.matching { it.name.contains("Release") && it.name.startsWith("minify") }.configureEach {
    dependsOn("generateObfuscationDictionary")
}
