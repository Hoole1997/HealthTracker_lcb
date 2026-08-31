package convention.config

import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.VariantDimension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/** Each channel keeps its own snapshot so google/local can be built together safely. */
data class ChannelConfig(
    val name: String,
    val admob: Map<*, *>,
    val admobUnit: Map<*, *>,
    val gam: Map<*, *>,
    val gamUnit: Map<*, *>,
    val pangle: Map<*, *>,
    val pangleUnit: Map<*, *>,
    val topon: Map<*, *>,
    val toponUnit: Map<*, *>,
    val app: Map<*, *>,
    val urls: Map<*, *>,
    val analytics: Map<*, *>,
)

fun loadChannelConfig(
    name: String,
    project: Project,
): ChannelConfig {
    project.apply(from = project.rootProject.file("app/src/$name/config.gradle"))

    val admob = project.extensions.extraProperties["admob"] as Map<*, *>
    val gam = project.extensions.extraProperties["gam"] as Map<*, *>
    val pangle = project.extensions.extraProperties["pangle"] as Map<*, *>
    val topon = project.extensions.extraProperties["topon"] as Map<*, *>

    return ChannelConfig(
        name = name,
        admob = admob,
        admobUnit = admob["adUnitIds"] as Map<*, *>,
        gam = gam,
        gamUnit = gam["adUnitIds"] as Map<*, *>,
        pangle = pangle,
        pangleUnit = pangle["adUnitIds"] as Map<*, *>,
        topon = topon,
        toponUnit = topon["adUnitIds"] as Map<*, *>,
        app = project.extensions.extraProperties["app"] as Map<*, *>,
        urls = project.extensions.extraProperties["url"] as Map<*, *>,
        analytics = project.extensions.extraProperties["analytics"] as Map<*, *>,
    )
}

fun ApplicationProductFlavor.configureChannel(config: ChannelConfig, project: Project) {
    applicationId = config.app["applicationId"] as String
    val baseVersion = project.providers.gradleProperty("versionName").orNull
        ?.removePrefix("v") ?: config.app["versionName"].toString()
    require(baseVersion.matches(Regex("[0-9]+(\\.[0-9]+)*([.-][A-Za-z0-9]+)*"))) {
        "Invalid versionName for ${config.name}: $baseVersion"
    }
    val code = (project.providers.gradleProperty("versionCode").orNull
        ?: config.app["versionCode"]?.toString())?.toIntOrNull()
    require(code != null && code in 1..2100000000) {
        "versionCode for ${config.name} must be an integer between 1 and 2100000000"
    }
    versionCode = code
    versionName = baseVersion
    if (config.name == "local") versionNameSuffix = "-local"
    addChannelBuildConfig(config)
}

fun VariantDimension.addChannelBuildConfig(config: ChannelConfig) {
    val defaultUserChannel = config.analytics["defaultUserChannel"] ?: "default"

    buildConfigField("boolean", "showLog", (config.app["show_log"] as Boolean).toString())
    buildConfigField("String", "PRIVACY_POLICY", "\"${config.urls["privacyUrl"]}\"")
    buildConfigField("String", "FCM_URL", "\"${config.urls["fcmUrl"]}\"")
    buildConfigField("String", "FCM_PKG", "\"${config.urls["fcmPkg"]}\"")
    buildConfigField("String", "FEEDBACK_EMAIL", "\"${config.urls["email"]}\"")
    buildConfigField("String", "DEFAULT_USER_CHANNEL", "\"$defaultUserChannel\"")
    buildConfigField("String", "ADMOB_APPLICATION_ID", "\"${config.admob["applicationId"]}\"")
    buildConfigField("String", "ADMOB_SPLASH_ID", "\"${config.admobUnit["splash"]}\"")
    buildConfigField("String", "ADMOB_BANNER_ID", "\"${config.admobUnit["banner"]}\"")
    buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${config.admobUnit["interstitial"]}\"")
    buildConfigField("String", "ADMOB_NATIVE_ID", "\"${config.admobUnit["native"]}\"")
    buildConfigField("String", "ADMOB_FULL_NATIVE_ID", "\"${config.admobUnit["full_native"]}\"")
    buildConfigField("String", "ADMOB_REWARDED_ID", "\"${config.admobUnit["rewarded"]}\"")
    buildConfigField("String", "GAM_SPLASH_ID", "\"${config.gamUnit["splash"]}\"")
    buildConfigField("String", "GAM_BANNER_ID", "\"${config.gamUnit["banner"]}\"")
    buildConfigField("String", "GAM_INTERSTITIAL_ID", "\"${config.gamUnit["interstitial"]}\"")
    buildConfigField("String", "GAM_NATIVE_ID", "\"${config.gamUnit["native"]}\"")
    buildConfigField("String", "GAM_FULL_NATIVE_ID", "\"${config.gamUnit["full_native"]}\"")
    buildConfigField("String", "GAM_REWARDED_ID", "\"${config.gamUnit["rewarded"]}\"")
    buildConfigField("String", "PANGLE_APPLICATION_ID", "\"${config.pangle["applicationId"]}\"")
    buildConfigField("String", "PANGLE_SPLASH_ID", "\"${config.pangleUnit["splash"]}\"")
    buildConfigField("String", "PANGLE_BANNER_ID", "\"${config.pangleUnit["banner"]}\"")
    buildConfigField("String", "PANGLE_INTERSTITIAL_ID", "\"${config.pangleUnit["interstitial"]}\"")
    buildConfigField("String", "PANGLE_NATIVE_ID", "\"${config.pangleUnit["native"]}\"")
    buildConfigField("String", "PANGLE_FULL_NATIVE_ID", "\"${config.pangleUnit["full_native"]}\"")
    buildConfigField("String", "PANGLE_REWARDED_ID", "\"${config.pangleUnit["rewarded"]}\"")
    buildConfigField("String", "TOPON_APPLICATION_ID", "\"${config.topon["applicationId"]}\"")
    buildConfigField("String", "TOPON_APP_KEY", "\"${config.topon["appKey"]}\"")
    buildConfigField("String", "TOPON_SPLASH_ID", "\"${config.toponUnit["splash"]}\"")
    buildConfigField("String", "TOPON_BANNER_ID", "\"${config.toponUnit["banner"]}\"")
    buildConfigField("String", "TOPON_INTERSTITIAL_ID", "\"${config.toponUnit["interstitial"]}\"")
    buildConfigField("String", "TOPON_NATIVE_ID", "\"${config.toponUnit["native"]}\"")
    buildConfigField("String", "TOPON_FULL_NATIVE_ID", "\"${config.toponUnit["full_native"]}\"")
    buildConfigField("String", "TOPON_REWARDED_ID", "\"${config.toponUnit["rewarded"]}\"")
}
