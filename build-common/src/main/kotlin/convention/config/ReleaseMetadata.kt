package convention.config

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

/** Machine-readable CI metadata comes from the same resolved flavor values as the APK/AAB. */
abstract class WriteReleaseMetadata : DefaultTask() {
    @get:Input abstract val releaseVersionName: Property<String>
    @get:Input abstract val releaseVersionCode: Property<Int>
    @get:Input abstract val artifactName: Property<String>
    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun writeMetadata() {
        val destination = outputFile.get().asFile
        destination.parentFile.mkdirs()
        destination.writeText(
            "version_name=${releaseVersionName.get()}\n" +
                "version_code=${releaseVersionCode.get()}\n" +
                "aab_name=${artifactName.get()}\n"
        )
    }
}

fun Project.registerReleaseMetadata(android: ApplicationExtension) {
    val google = android.productFlavors.getByName("google")
    val versionName = requireNotNull(google.versionName)
    val versionCode = requireNotNull(google.versionCode)
    val projectName = rootProject.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    val aabName = "${projectName}_google_release_$versionName.aab"

    tasks.register<WriteReleaseMetadata>("writeGoogleReleaseMetadata") {
        group = "help"
        description = "Writes the resolved Google release version and artifact name for CI."
        releaseVersionName.set(versionName)
        releaseVersionCode.set(versionCode)
        artifactName.set(aabName)
        outputFile.set(layout.buildDirectory.file("outputs/release-metadata.properties"))
    }
    tasks.register("printGoogleReleaseVersionName") {
        group = "help"
        doLast { println(versionName) }
    }
    tasks.register("printGoogleReleaseAabName") {
        group = "help"
        doLast { println(aabName) }
    }
}
