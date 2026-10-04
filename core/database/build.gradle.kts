plugins {
    alias(libs.plugins.spacesaver.android.library)
    alias(libs.plugins.spacesaver.android.room)
    alias(libs.plugins.spacesaver.hilt)
}

dependencies {
    implementation(projects.core.model)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
