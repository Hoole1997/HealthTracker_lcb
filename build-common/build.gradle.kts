import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "convention.plugins"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.stringfogPlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApp") {
            id = "android.app"
            implementationClass = "convention.plugins.AndroidAppConventionPlugin"
        }
        register("androidLibrary") {
            id = "android.library"
            implementationClass = "convention.plugins.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "android.compose"
            implementationClass = "convention.plugins.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "android.hilt"
            implementationClass = "convention.plugins.AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "android.room"
            implementationClass = "convention.plugins.AndroidRoomConventionPlugin"
        }
        register("androidFirebase") {
            id = "android.firebase"
            implementationClass = "convention.plugins.AndroidFirebaseConventionPlugin"
        }
        register("androidStringFog") {
            id = "android.stringfog"
            implementationClass = "convention.plugins.AndroidStringFogConventionPlugin"
        }
    }
}