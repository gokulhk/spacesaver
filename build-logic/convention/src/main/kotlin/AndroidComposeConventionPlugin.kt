import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import io.github.gokulhk.spacesaver.buildlogic.library
import io.github.gokulhk.spacesaver.buildlogic.libs
import io.github.gokulhk.spacesaver.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies

/**
 * Enables Jetpack Compose on an Android application or library module and adds the
 * Compose BOM, Material 3, and preview tooling. Unit tests get the Compose UI test APIs (on
 * top of Robolectric from the base Android conventions), so Compose behavior tests run on the
 * JVM without an emulator.
 * Apply after the application or library convention.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("compose"))
            pluginManager.withPlugin(libs.pluginId("android-application")) {
                enableCompose(extensions.getByType(ApplicationExtension::class.java))
            }
            pluginManager.withPlugin(libs.pluginId("android-library")) {
                enableCompose(extensions.getByType(LibraryExtension::class.java))
            }
        }
    }

    private fun Project.enableCompose(commonExtension: CommonExtension) {
        commonExtension.buildFeatures.compose = true
        dependencies {
            val bom = platform(libs.library("androidx-compose-bom"))
            add("implementation", bom)
            add("androidTestImplementation", bom)
            add("testImplementation", bom)
            add("implementation", libs.library("androidx-compose-material3"))
            add("implementation", libs.library("androidx-compose-ui"))
            add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
            add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
            add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
            // Compose UI test pulls in Espresso 3.5, which calls InputManager APIs removed in SDK 36+.
            add("testImplementation", libs.library("androidx-test-espresso-core"))
        }
        tasks.withType(Test::class.java).configureEach {
            // Real rendering (instead of no-op stubs) so layout, drawing, and screenshots work.
            systemProperty("robolectric.graphicsMode", "NATIVE")
        }
    }
}
