package io.github.gokulhk.spacesaver.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/**
 * Applies the SDK levels, Java compatibility, and lint policy shared by every Android
 * module (application and libraries).
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    val javaVersion = JavaVersion.toVersion(jvmTargetVersion)
    commonExtension.apply {
        compileSdk = libs.version("compileSdk").toInt()
        defaultConfig.minSdk = libs.version("minSdk").toInt()
        compileOptions.sourceCompatibility = javaVersion
        compileOptions.targetCompatibility = javaVersion
        testOptions.unitTests.isIncludeAndroidResources = true
        configureLint(lint)
    }
    configureKotlin()
    configureRobolectric()
}

/**
 * Android Lint policy: every warning is an error, and the shared `config/lint/lint.xml`
 * holds the few deliberate exceptions (each documented in that file).
 */
internal fun Project.configureLint(lint: Lint) {
    lint.apply {
        warningsAsErrors = true
        abortOnError = true
        checkReleaseBuilds = true
        lintConfig = rootProject.file("config/lint/lint.xml")
        htmlReport = true
        xmlReport = false
        sarifReport = true
    }
}
