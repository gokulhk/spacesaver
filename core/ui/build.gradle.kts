plugins {
    alias(libs.plugins.spacesaver.android.library)
    alias(libs.plugins.spacesaver.android.compose)
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
}
