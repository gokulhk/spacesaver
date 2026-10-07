plugins {
    alias(libs.plugins.spacesaver.android.library)
    alias(libs.plugins.spacesaver.android.compose)
}

// Light/dark screenshot capture shared by the design system and feature modules. Used only from
// unit tests (testImplementation), never shipped.
dependencies {
    api(projects.core.designsystem)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
}
