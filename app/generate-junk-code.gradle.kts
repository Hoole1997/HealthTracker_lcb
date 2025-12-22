import kotlin.random.Random

tasks.register("generateJunkCode") {
    group = "obfuscation"
    description = "Generates random junk code to confuse static analysis"

    val outputDir = layout.buildDirectory.dir("generated/source/junk/kotlin").get().asFile
    
    // Always regenerate to ensure uniqueness per build
    outputs.upToDateWhen { false } 

    doLast {
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        val packageName = "com.healthtracker.blood.suger.junk"
        val packageDir = File(outputDir, packageName.replace('.', '/'))
        packageDir.mkdirs()

        val seed = System.currentTimeMillis()
        val random = Random(seed)
        val generatedClasses = mutableListOf<Pair<String, List<String>>>() // ClassName -> List<MethodName>
        val numberOfClasses = random.nextInt(50, 100) 

        fun randomString(length: Int = 10): String {
            val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
        }

        // Generate Junk Classes
        repeat(numberOfClasses) {
            val className = "Junk${randomString(6)}"
            val methods = mutableListOf<String>()
            
            val fileContent = StringBuilder()
            fileContent.append("package $packageName\n\n")
            fileContent.append("import android.util.Log\n\n")
            fileContent.append("class $className {\n")
            
            repeat(random.nextInt(3, 6)) {
                val methodName = "action${randomString(4)}"
                methods.add(methodName)
                
                fileContent.append("    fun $methodName() {\n")
                // Random logic bodies
                val logicType = random.nextInt(4)
                when (logicType) {
                    0 -> fileContent.append("        var x = ${random.nextInt(1000)}\n        x = (x * 3) + ${random.nextInt(100)}\n")
                    1 -> fileContent.append("        val s = \"${randomString()}\"\n        if (s.length > 5) { Log.v(\"Junk\", s) }\n")
                    2 -> fileContent.append("        val list = listOf(${random.nextInt()}, ${random.nextInt()})\n        if (list.size > 1) { /* no-op */ }\n")
                    3 -> fileContent.append("        val time = System.currentTimeMillis()\n        if (time % 2 == 0L) { return }\n")
                }
                fileContent.append("    }\n")
            }
            
            fileContent.append("}\n")
            File(packageDir, "$className.kt").writeText(fileContent.toString())
            generatedClasses.add(className to methods)
        }

        // Generate Loader
        val loaderClassName = "JunkCodeLoader"
        val loaderContent = StringBuilder()
        loaderContent.append("package $packageName\n\n")
        loaderContent.append("object $loaderClassName {\n")
        loaderContent.append("    fun load() {\n")
        loaderContent.append("        try {\n")
        
        // Chain calls randomly
        generatedClasses.shuffled(random).take(20).forEach { (className, methods) ->
             loaderContent.append("            val obj$className = $className()\n")
             if (methods.isNotEmpty()) {
                 val randomMethod = methods[random.nextInt(methods.size)]
                 loaderContent.append("            obj$className.$randomMethod()\n")
             }
        }
        
        loaderContent.append("        } catch (e: Exception) {\n")
        loaderContent.append("            // Ignore all errors\n")
        loaderContent.append("        }\n")
        loaderContent.append("    }\n")
        loaderContent.append("}\n")
        
        File(packageDir, "$loaderClassName.kt").writeText(loaderContent.toString())
        
        logger.lifecycle("Generated ${generatedClasses.size} junk classes in $packageDir")
    }
}
