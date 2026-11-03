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
        maven {
            url = uri("https://maven.app-plat.com/repository/maven-public")
            val localCredentials = java.util.Properties().apply {
                val file = file("build.config.properties")
                if (file.exists()) file.inputStream().use { load(it) }
            }
            credentials {
                username = providers.environmentVariable("APP_PLAT_MAVEN_USER")
                    .orElse(localCredentials.getProperty("appPlat.user", "")).get()
                password = providers.environmentVariable("APP_PLAT_MAVEN_PASSWORD")
                    .orElse(localCredentials.getProperty("appPlat.password", "")).get()
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "MavenCentralUnityAdsMediation"
                    url = uri("https://repo1.maven.org/maven2")
                }
            }
            filter {
                includeGroup("com.unity3d.ads-mediation")
            }
        }
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
        // ReMax 私有依赖发布在 GitHub Packages。必须放在 JitPack 前面，
        // 避免 com.github.toukaremax 坐标被 JitPack 拦截并返回 401。
        maven {
            url = uri("https://maven.pkg.github.com/toukaRemax/remax_sdk")
            val githubProperties = java.util.Properties()
            val githubPropertiesFile = rootDir.resolve("build.config.properties")
            if (githubPropertiesFile.exists()) {
                githubPropertiesFile.inputStream().use(githubProperties::load)
            }

            val githubUser = providers.environmentVariable("REMAX_GITHUB_USER")
                .orElse(providers.provider {
                    githubProperties.getProperty("github.user") ?: "toukaRemax"
                })
                .get()
            val githubToken = providers.environmentVariable("REMAX_GITHUB_TOKEN")
                .orElse(providers.environmentVariable("REMAX_SDK_TOKEN"))
                .orElse(providers.provider {
                    githubProperties.getProperty("github.token") ?: ""
                })
                .get()

            credentials {
                username = githubUser
                password = githubToken
            }
            content {
                includeGroup("com.github.toukaremax")
            }
        }
        maven {
            url = uri("https://jitpack.io")
            content {
                excludeGroup("com.github.toukaremax")
            }
        }
        maven("https://artifact.bytedance.com/repository/pangle/")
        // 私有 Maven 仓库的凭据仅从环境变量或本机配置读取。
        maven {
            url = uri("https://maven.app-plat.com/repository/maven-public")
            val localCredentials = java.util.Properties().apply {
                val file = file("build.config.properties")
                if (file.exists()) file.inputStream().use { load(it) }
            }
            credentials {
                username = providers.environmentVariable("APP_PLAT_MAVEN_USER")
                    .orElse(localCredentials.getProperty("appPlat.user", "")).get()
                password = providers.environmentVariable("APP_PLAT_MAVEN_PASSWORD")
                    .orElse(localCredentials.getProperty("appPlat.password", "")).get()
            }
        }
        maven("https://maven.aliyun.com/nexus/content/groups/public/")
        maven("https://maven.aliyun.com/nexus/content/repositories/jcenter")
        maven("https://maven.aliyun.com/nexus/content/repositories/google")
        maven("https://maven.aliyun.com/nexus/content/repositories/gradle-plugin")
    }
}

rootProject.name = "HealthTrackerLauncher"
include(":app")
include(":framework")
include(":metrics")
include(":earthquake")
include(":weather")
include(":appraise")
