package convention.config

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApkSigningConfig
import org.gradle.api.Project
import java.util.Properties

/**
 * Each channel owns its sign.properties and certificate. Local Debug/Release share
 * GPSPhoto's test key; Google always uses this project's own key, without cross-channel fallbacks.
 */
fun Project.configureReleaseSigning(android: ApplicationExtension) {
    for (channel in listOf("google", "local")) {
        val signing = android.signingConfigs.create("${channel}Channel")
        configureChannelSigning(channel, signing)
        android.productFlavors.getByName(channel).signingConfig = signing
    }
    // Build-type signing has higher priority than flavor signing. Clear AGP's default
    // so Local Debug really uses the checked-in test certificate, not ~/.android/debug.keystore.
    android.buildTypes.getByName("debug").signingConfig = null
}

private fun Project.configureChannelSigning(channel: String, signing: ApkSigningConfig) {
    val directory = rootProject.file("app/src/$channel")
    val properties = Properties().apply {
        directory.resolve("sign.properties").inputStream().use(::load)
    }
    // Google keeps the CI variable names; Local overrides are isolated from CI credentials.
    val prefix = if (channel == "google") "ANDROID_SIGNING" else "LOCAL_ANDROID_SIGNING"
    fun override(suffix: String): String? =
        providers.gradleProperty("${prefix}_$suffix").orNull?.takeIf { it.isNotEmpty() }
            ?: providers.environmentVariable("${prefix}_$suffix").orNull?.takeIf { it.isNotEmpty() }
    fun configured(key: String): String = requireNotNull(properties.getProperty(key)?.takeIf { it.isNotEmpty() }) {
        "Missing $key in app/src/$channel/sign.properties"
    }

    // Paths in sign.properties are relative to that channel; explicit -P/env paths
    // remain relative to the repository root for compatibility with CI.
    signing.storeFile = override("STORE_FILE")?.let { rootProject.file(it) }
        ?: directory.resolve(configured("storeFile"))
    signing.storePassword = override("STORE_PASSWORD") ?: configured("storePassword")
    signing.keyAlias = override("KEY_ALIAS") ?: configured("keyAlias")
    signing.keyPassword = override("KEY_PASSWORD") ?: configured("keyPassword")
    // File/password validation happens in AGP's signing task, so IDE sync does not require a Google key.
}
