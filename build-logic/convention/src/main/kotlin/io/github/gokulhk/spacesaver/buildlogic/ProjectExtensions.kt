package io.github.gokulhk.spacesaver.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/** The `libs` version catalog defined in `gradle/libs.versions.toml`. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Returns the version declared under `[versions]` with the given [alias]. */
internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

/** Returns the library declared under `[libraries]` with the given [alias]. */
internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> = findLibrary(alias).get()

/** Returns the plugin id declared under `[plugins]` with the given [alias]. */
internal fun VersionCatalog.pluginId(alias: String): String = findPlugin(alias).get().get().pluginId

/**
 * Base package and namespace for the whole app, read from the `spacesaver.namespace`
 * Gradle property so the maintainer can change the GitHub org in one place.
 */
internal val Project.baseNamespace: String
    get() = providers.gradleProperty("spacesaver.namespace").get()

/**
 * Namespace derived from the project path, for example `:core:designsystem` becomes
 * `<baseNamespace>.core.designsystem`.
 */
internal val Project.moduleNamespace: String
    get() = baseNamespace + path.replace(':', '.').replace('-', '_')
