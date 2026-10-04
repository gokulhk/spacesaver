import com.android.build.api.dsl.ApplicationExtension
import io.github.gokulhk.spacesaver.buildlogic.baseNamespace
import io.github.gokulhk.spacesaver.buildlogic.configureKotlinAndroid
import io.github.gokulhk.spacesaver.buildlogic.configureQuality
import io.github.gokulhk.spacesaver.buildlogic.libs
import io.github.gokulhk.spacesaver.buildlogic.pluginId
import io.github.gokulhk.spacesaver.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention for the `:app` module: Android application with shared SDK levels, Kotlin
 * settings, lint policy, detekt, and Kover.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-application"))
            extensions.configure<ApplicationExtension> {
                namespace = baseNamespace
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = libs.version("targetSdk").toInt()
                defaultConfig.testInstrumentationRunner = TEST_RUNNER
            }
            configureQuality()
            addUnitTestDependencies()
        }
    }
}
