/**
 * 动态生成混淆字典的 Gradle 脚本
 * 
 * 每次 Release 构建时生成随机的混淆字典，使每次构建的 APK 在静态分析时呈现不同特征
 * 
 * 差异化策略：
 * 1. 随机字典顺序 - 影响类名/方法名混淆结果
 * 2. 随机字符组合 - 生成不同的混淆名称
 * 3. 随机前缀/后缀 - 增加混淆多样性
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
        val seed = System.currentTimeMillis()
        val random = Random(seed)
        val javaRandom = java.util.Random(seed)
        val dictionary = mutableListOf<String>()

        // 随机选择字符集（增加差异化）
        val allChars = ('a'..'z').toList()
        val shuffledChars = allChars.shuffled(javaRandom)
        
        // 生成单字符名称，使用随机顺序的字符
        shuffledChars.forEach { dictionary.add(it.toString()) }

        // 生成双字符名称，随机顺序
        val twoCharNames = mutableListOf<String>()
        shuffledChars.forEach { first ->
            shuffledChars.forEach { second ->
                twoCharNames.add("$first$second")
            }
        }
        twoCharNames.shuffle(java.util.Random(seed + 1))
        dictionary.addAll(twoCharNames)

        // 生成三字符名称，随机选择
        val threeCharNames = mutableSetOf<String>()
        repeat(800) {
            val name = buildString {
                repeat(3) {
                    append(shuffledChars[random.nextInt(shuffledChars.size)])
                }
            }
            threeCharNames.add(name)
        }
        val shuffledThreeChar = threeCharNames.toMutableList()
        shuffledThreeChar.shuffle(java.util.Random(seed + 2))
        dictionary.addAll(shuffledThreeChar)

        // 生成带下划线的名称（看起来像私有变量）
        val underscoreNames = mutableSetOf<String>()
        repeat(200) {
            val name = "_" + buildString {
                repeat(random.nextInt(2, 5)) {
                    append(shuffledChars[random.nextInt(shuffledChars.size)])
                }
            }
            underscoreNames.add(name)
        }
        dictionary.addAll(underscoreNames.shuffled(java.util.Random(seed + 3)))

        // 添加看起来像真实代码的名称
        val fakeNames = listOf(
            "init", "get", "set", "run", "call", "exec", "load", "save",
            "read", "write", "open", "close", "start", "stop", "create",
            "delete", "update", "find", "check", "validate", "process",
            "handle", "parse", "format", "convert", "build", "make",
            "has", "can", "should", "will", "may", "data", "info", "item",
            "list", "map", "key", "value", "name", "type", "state", "flag",
            "count", "index", "size", "length", "result", "error", "code",
            "msg", "text", "str", "num", "val", "obj", "ref", "ptr", "buf"
        )
        val uniqueFakeNames = fakeNames.filter { it !in dictionary }
        dictionary.addAll(uniqueFakeNames.shuffled(java.util.Random(seed + 4)))

        // 生成数字后缀的名称（如 a0, b1, c2）
        val numberedNames = mutableSetOf<String>()
        repeat(300) {
            val char = shuffledChars[random.nextInt(shuffledChars.size)]
            val num = random.nextInt(0, 100)
            numberedNames.add("$char$num")
        }
        dictionary.addAll(numberedNames.shuffled(java.util.Random(seed + 5)))

        // 写入字典文件
        dictionaryFile.writeText(dictionary.joinToString("\n"))

        logger.lifecycle("Generated obfuscation dictionary with ${dictionary.size} entries (seed: $seed)")
    }
}

// 在 Release 构建前生成字典
tasks.matching { it.name.contains("Release") && it.name.startsWith("minify") }.configureEach {
    dependsOn("generateObfuscationDictionary")
}
