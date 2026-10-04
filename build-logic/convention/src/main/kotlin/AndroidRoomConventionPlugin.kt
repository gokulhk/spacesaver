import androidx.room.gradle.RoomExtension
import com.android.build.api.variant.AndroidComponentsExtension
import io.github.gokulhk.spacesaver.buildlogic.library
import io.github.gokulhk.spacesaver.buildlogic.libs
import io.github.gokulhk.spacesaver.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room with KSP. Schemas are exported to `schemas/` and committed, so every schema change is
 * reviewed and migration tests can open old versions.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ksp"))
            pluginManager.apply(libs.pluginId("room"))
            val schemas = "$projectDir/schemas"
            extensions.configure<RoomExtension> {
                schemaDirectory(schemas)
            }
            // Room's plugin feeds schemas only to instrumented tests. Robolectric migration tests read
            // assets of the tested (debug) variant, since library unit tests don't package test assets.
            // Release builds never contain the schemas.
            extensions.configure(AndroidComponentsExtension::class.java) {
                onVariants(selector().withBuildType("debug")) { variant ->
                    variant.sources.assets?.addStaticSourceDirectory(schemas)
                }
            }
            dependencies {
                add("implementation", libs.library("androidx-room-runtime"))
                add("implementation", libs.library("androidx-room-ktx"))
                add("ksp", libs.library("androidx-room-compiler"))
                add("testImplementation", libs.library("androidx-room-testing"))
            }
        }
    }
}
