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
        maven("https://repo.dgtverse.cn/repository/maven-public")
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
        maven {
            url = uri("https://maven.pkg.github.com/toukaRemax/remax_sdk")
            credentials {
                username = "toukaRemax"
                password = "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"
            }
        }
    }
}

rootProject.name = "HealthTracker"
include(":app")
include(":framework")
include(":metrics")
include(":earthquake")
include(":weather")
include(":appraise")
