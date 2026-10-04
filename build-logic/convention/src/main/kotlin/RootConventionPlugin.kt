import com.diffplug.gradle.spotless.SpotlessExtension
import io.github.gokulhk.spacesaver.buildlogic.excludeGeneratedAndUiEntryPoints
import io.github.gokulhk.spacesaver.buildlogic.libs
import io.github.gokulhk.spacesaver.buildlogic.pluginId
import io.github.gokulhk.spacesaver.buildlogic.version
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Minimum line coverage for the whole project, merged across all modules. The stricter
 * `:core:domain` threshold is declared in that module's build script.
 */
private const val OVERALL_MIN_LINE_COVERAGE_PERCENT = 70

/**
 * Convention for the root project: ktlint formatting via Spotless for every Kotlin and
 * Gradle file, and an aggregated Kover report with the overall coverage threshold.
 */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(this == rootProject) { "spacesaver.root must only be applied to the root project" }
            pluginManager.apply("base")
            pluginManager.apply(libs.pluginId("spotless"))
            pluginManager.apply(libs.pluginId("kover"))
            configureSpotless()
            configureCoverageAggregation()
        }
    }

    private fun Project.configureSpotless() {
        val ktlintVersion = libs.version("ktlint")
        val composeRules = "io.nlopez.compose.rules:ktlint:${libs.version("composeRules")}"
        extensions.configure<SpotlessExtension> {
            kotlin {
                target("**/*.kt")
                targetExclude("**/build/**", "**/.gradle/**")
                ktlint(ktlintVersion)
                    .setEditorConfigPath(rootProject.file(".editorconfig"))
                    .customRuleSets(listOf(composeRules))
            }
            kotlinGradle {
                target("**/*.gradle.kts")
                targetExclude("**/build/**", "**/.gradle/**")
                ktlint(ktlintVersion).setEditorConfigPath(rootProject.file(".editorconfig"))
            }
            format("misc") {
                target("**/*.md", "**/*.yml", "**/*.toml", ".gitignore", ".editorconfig")
                targetExclude("**/build/**", "**/.gradle/**")
                trimTrailingWhitespace()
                endWithNewline()
            }
        }
    }

    private fun Project.configureCoverageAggregation() {
        val modules = subprojects.filter { it.buildFile.exists() }
        dependencies {
            modules.forEach { add("kover", project(it.path)) }
        }
        extensions.configure<KoverProjectExtension> {
            reports {
                filters { excludeGeneratedAndUiEntryPoints() }
                verify {
                    rule("Overall line coverage") {
                        minBound(OVERALL_MIN_LINE_COVERAGE_PERCENT)
                    }
                }
            }
        }
    }
}
