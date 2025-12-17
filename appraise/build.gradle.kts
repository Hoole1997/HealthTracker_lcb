plugins {
    // 使用自定义插件
    alias(libs.plugins.android.library.convention)
}

android {
    namespace = "com.app.raise"
}

dependencies {
    api(project(":framework"))
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.lottie)
}
