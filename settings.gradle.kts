pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Downloads a missing JDK toolchain (17 to compile, 21 to run tests) instead of failing.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "spacesaver"

include(":app")

include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:media")
include(":core:work")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")
include(":core:screenshot-testing")

include(":feature:onboarding")
include(":feature:home")
include(":feature:browse")
include(":feature:batch")
include(":feature:settings")
