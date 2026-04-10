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
        // Mintegral 仓库 (Pangle 适配器依赖)
        maven {
            setUrl("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
        }
        // TopOn 仓库
        maven {
            setUrl("https://jfrog.anythinktech.com/artifactory/overseas_sdk")
        }
        //Ironsource
        maven {
            setUrl("https://android-sdk.is.com/")
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
        maven("https://repo.dgtverse.cn/repository/maven-public")
    }
}

rootProject.name = "HealthTracker"
include(":app")
include(":framework")
include(":monetize")
include(":core")
include(":metrics")
include(":earthquake")
include(":weather")
include(":appraise")