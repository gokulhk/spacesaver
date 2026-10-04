plugins {
    `kotlin-dsl`
}

group = "io.github.gokulhk.spacesaver.buildlogic"

kotlin {
    jvmToolchain(
        libs.versions.jvmTarget
            .get()
            .toInt(),
    )
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id =
                libs.plugins.spacesaver.android.application
                    .get()
                    .pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id =
                libs.plugins.spacesaver.android.library
                    .get()
                    .pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id =
                libs.plugins.spacesaver.android.feature
                    .get()
                    .pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidCompose") {
            id =
                libs.plugins.spacesaver.android.compose
                    .get()
                    .pluginId
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidRoborazzi") {
            id =
                libs.plugins.spacesaver.android.roborazzi
                    .get()
                    .pluginId
            implementationClass = "AndroidRoborazziConventionPlugin"
        }
        register("androidRoom") {
            id =
                libs.plugins.spacesaver.android.room
                    .get()
                    .pluginId
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("hilt") {
            id =
                libs.plugins.spacesaver.hilt
                    .get()
                    .pluginId
            implementationClass = "HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id =
                libs.plugins.spacesaver.jvm.library
                    .get()
                    .pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("root") {
            id =
                libs.plugins.spacesaver.root
                    .get()
                    .pluginId
            implementationClass = "RootConventionPlugin"
        }
    }
}
