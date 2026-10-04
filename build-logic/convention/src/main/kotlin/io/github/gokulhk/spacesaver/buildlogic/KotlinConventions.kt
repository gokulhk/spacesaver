package io.github.gokulhk.spacesaver.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** JDK version used for compilation, toolchains, and bytecode targets across all modules. */
internal val Project.jvmTargetVersion: Int
    get() = libs.version("jvmTarget").toInt()

/**
 * Applies the shared Kotlin setup: JDK toolchain, JVM bytecode target, and
 * warnings-as-errors so that no new compiler warnings slip in.
 */
internal fun Project.configureKotlin() {
    extensions.configure<KotlinBaseExtension> {
        jvmToolchain(jvmTargetVersion)
    }
    tasks.withType(KotlinJvmCompile::class.java).configureEach {
        compilerOptions.applySharedOptions(jvmTargetVersion)
    }
}

private fun KotlinJvmCompilerOptions.applySharedOptions(jvmTarget: Int) {
    this.jvmTarget.set(JvmTarget.fromTarget(jvmTarget.toString()))
    allWarningsAsErrors.set(true)
}
