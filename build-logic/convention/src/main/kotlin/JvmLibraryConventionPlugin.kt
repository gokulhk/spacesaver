import com.android.build.api.dsl.Lint
import io.github.gokulhk.spacesaver.buildlogic.configureKotlin
import io.github.gokulhk.spacesaver.buildlogic.configureLint
import io.github.gokulhk.spacesaver.buildlogic.configureQuality
import io.github.gokulhk.spacesaver.buildlogic.libs
import io.github.gokulhk.spacesaver.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention for pure Kotlin modules (`:core:model`, `:core:domain`, `:core:testing`).
 * These modules must not depend on the Android framework; Android Lint still runs on
 * them through the standalone lint plugin.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-jvm"))
            pluginManager.apply(libs.pluginId("android-lint"))
            configureKotlin()
            extensions.configure<Lint> { configureLint(this) }
            configureQuality()
            addUnitTestDependencies()
        }
    }
}
