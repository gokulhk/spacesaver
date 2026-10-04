plugins {
    alias(libs.plugins.spacesaver.android.library)
    alias(libs.plugins.spacesaver.android.compose)
    alias(libs.plugins.spacesaver.android.roborazzi)
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    // Only reached through SpaceSaverIcons; R8 strips unused icons from release builds.
    implementation(libs.androidx.compose.material.icons.extended)
}
