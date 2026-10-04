package io.github.gokulhk.spacesaver.buildlogic

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasUnitTest
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.dependencies

/**
 * Robolectric for every Android module, so framework-dependent code (Room, MediaStore, Compose)
 * is tested on the JVM. Also pins the emulated SDK; see [configureRobolectricSdk].
 */
internal fun Project.configureRobolectric() {
    dependencies {
        add("testImplementation", libs.library("robolectric"))
        add("testImplementation", libs.library("androidx-test-core"))
        add("testImplementation", libs.library("androidx-test-ext-junit"))
    }
    configureRobolectricSdk()
}

/**
 * Generates `robolectric.properties` on every unit test classpath so all JVM tests emulate the
 * same Android SDK (`robolectricSdk` in the version catalog) without per-test `@Config`.
 */
private fun Project.configureRobolectricSdk() {
    val generate =
        tasks.register("generateRobolectricProperties", GenerateRobolectricPropertiesTask::class.java) {
            sdk.set(libs.version("robolectricSdk"))
            outputDirectory.set(layout.buildDirectory.dir("generated/robolectric"))
        }
    extensions.configure(AndroidComponentsExtension::class.java) {
        onVariants { variant ->
            (variant as? HasUnitTest)
                ?.unitTest
                ?.sources
                ?.resources
                ?.addGeneratedSourceDirectory(generate, GenerateRobolectricPropertiesTask::outputDirectory)
        }
    }
}

/** Writes a `robolectric.properties` file that pins the emulated SDK level. */
@CacheableTask
abstract class GenerateRobolectricPropertiesTask : DefaultTask() {
    /** The Android API level Robolectric emulates. */
    @get:Input
    abstract val sdk: Property<String>

    /** Directory that becomes a unit test resource root. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /** Writes the properties file. */
    @TaskAction
    fun generate() {
        outputDirectory
            .file("robolectric.properties")
            .get()
            .asFile
            .writeText("sdk=${sdk.get()}\n")
    }
}
