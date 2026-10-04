import io.github.gokulhk.spacesaver.buildlogic.library
import io.github.gokulhk.spacesaver.buildlogic.libs
import io.github.gokulhk.spacesaver.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Sets up Hilt dependency injection with KSP code generation on an Android module.
 * Apply after the application or library convention.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ksp"))
            pluginManager.apply(libs.pluginId("hilt"))
            dependencies {
                add("implementation", libs.library("hilt-android"))
                add("ksp", libs.library("hilt-compiler"))
                add("testImplementation", libs.library("hilt-android-testing"))
                add("kspTest", libs.library("hilt-compiler"))
            }
        }
    }
}
