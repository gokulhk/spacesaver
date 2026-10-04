import com.android.build.api.dsl.LibraryExtension
import io.github.gokulhk.spacesaver.buildlogic.configureKotlinAndroid
import io.github.gokulhk.spacesaver.buildlogic.configureQuality
import io.github.gokulhk.spacesaver.buildlogic.library
import io.github.gokulhk.spacesaver.buildlogic.libs
import io.github.gokulhk.spacesaver.buildlogic.moduleNamespace
import io.github.gokulhk.spacesaver.buildlogic.pluginId
import io.github.gokulhk.spacesaver.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** AndroidX test runner used by every module's instrumented tests. */
internal const val TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner"

/**
 * Convention for Android library modules under `core/` and `feature/`. The namespace is
 * derived from the project path, so modules do not declare one themselves.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-library"))
            extensions.configure<LibraryExtension> {
                namespace = moduleNamespace
                configureKotlinAndroid(this)
                defaultConfig.testInstrumentationRunner = TEST_RUNNER
            }
            configureQuality()
            addUnitTestDependencies()
        }
    }
}

/** JUnit 4 and Truth are available to every module's unit tests. */
internal fun Project.addUnitTestDependencies() {
    dependencies {
        add("testImplementation", libs.library("junit4"))
        add("testImplementation", libs.library("truth"))
    }
    val testLauncher =
        extensions.getByType(JavaToolchainService::class.java).launcherFor {
            languageVersion.set(JavaLanguageVersion.of(libs.version("testJvm").toInt()))
        }
    tasks.withType(Test::class.java).configureEach {
        javaLauncher.set(testLauncher)
        // Robolectric (SDK 36+) reads java.io.FileDescriptor internals through JDK-private APIs
        // that the module system hides by default.
        jvmArgs(
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
        )
        // Hilt generates test sources even in modules that have no tests yet (most modules
        // until their phase starts), which would otherwise trip Gradle's "no tests found" guard.
        failOnNoDiscoveredTests.set(false)
    }
}
