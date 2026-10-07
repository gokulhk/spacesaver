plugins {
    alias(libs.plugins.spacesaver.android.library)
    alias(libs.plugins.spacesaver.android.compose)
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    // DomainError, for ErrorMessageMapper. Pure Kotlin.
    api(projects.core.domain)
    implementation(libs.androidx.core.ktx)
}
