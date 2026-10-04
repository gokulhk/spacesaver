plugins {
    alias(libs.plugins.spacesaver.jvm.library)
}

// Shared fakes, builders, and test rules. Pure Kotlin so both JVM and Android modules can use it.
dependencies {
    api(projects.core.domain)
    api(projects.core.model)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.truth)
}
