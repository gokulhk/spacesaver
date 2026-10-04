plugins {
    alias(libs.plugins.spacesaver.android.library)
    alias(libs.plugins.spacesaver.hilt)
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.model)
}
