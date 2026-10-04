# build-logic

Gradle convention plugins shared by every module. Module build scripts stay a few lines long; SDK levels, Kotlin options, lint, detekt, Kover, Compose, and Hilt setup live here.

| Plugin id | Applies to | What it configures |
|---|---|---|
| `spacesaver.android.application` | `:app` | Android application, SDK levels from the catalog, Kotlin and lint settings, detekt, Kover |
| `spacesaver.android.library` | Android `core/` modules | Same as above for libraries, plus Robolectric for JVM tests. Namespace is derived from the project path |
| `spacesaver.android.feature` | `feature/` modules | Library + Compose + Hilt, plus dependencies on domain and shared UI modules |
| `spacesaver.android.compose` | Any Android module with UI | Compose compiler, Compose BOM, Material 3, preview tooling, Compose UI test dependencies |
| `spacesaver.android.roborazzi` | Modules with screenshot tests | Roborazzi plugin and dependencies. Baselines in `src/test/screenshots/` |
| `spacesaver.android.room` | `:core:database` | Room + KSP; schemas exported to `schemas/` |
| `spacesaver.hilt` | Android modules using DI | Hilt with KSP |
| `spacesaver.jvm.library` | `:core:model`, `:core:domain`, `:core:testing` | Pure Kotlin/JVM, Android Lint, detekt, Kover |
| `spacesaver.root` | Root project | Spotless (ktlint + Compose rules), aggregated Kover report with the 70% overall threshold |

The base package and application ID come from the `spacesaver.namespace` property in `gradle.properties`.
