import io.github.gokulhk.spacesaver.buildlogic.library
import io.github.gokulhk.spacesaver.buildlogic.libs
import io.github.gokulhk.spacesaver.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Adds Roborazzi screenshot testing on top of the Compose convention. Baselines live in
 * `src/test/screenshots/` and are committed. Record them with `./gradlew recordRoborazziDebug`
 * and check them with `./gradlew verifyRoborazziDebug`.
 */
class AndroidRoborazziConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("roborazzi"))
            dependencies {
                add("testImplementation", libs.library("roborazzi"))
                add("testImplementation", libs.library("roborazzi-compose"))
                add("testImplementation", libs.library("roborazzi-junit-rule"))
            }
        }
    }
}
