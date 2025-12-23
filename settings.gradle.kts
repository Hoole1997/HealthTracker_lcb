pluginManagement {
    includeBuild("build-common")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io") // activityGuard
        maven("https://maven.aliyun.com/nexus/content/groups/public/")
        maven("https://maven.aliyun.com/nexus/content/repositories/jcenter")
        maven("https://maven.aliyun.com/nexus/content/repositories/google")
        maven("https://maven.aliyun.com/nexus/content/repositories/gradle-plugin")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://artifact.bytedance.com/repository/pangle")
        }
        maven {
            setUrl("https://jitpack.io")
            maven("https://artifact.bytedance.com/repository/pangle/")
            maven("https://repo.dgtverse.cn/repository/maven-public/")
            maven("https://maven.aliyun.com/nexus/content/groups/public/")
            maven("https://maven.aliyun.com/nexus/content/repositories/jcenter")
            maven("https://maven.aliyun.com/nexus/content/repositories/google")
            maven("https://maven.aliyun.com/nexus/content/repositories/gradle-plugin")
            isAllowInsecureProtocol = false
        }
    }
}

rootProject.name = "HealthTracker"
include(":app")
include(":framework")
include(":monetize")
include(":core")
include(":metrics")
include(":appraise")