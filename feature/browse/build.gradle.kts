plugins {
    alias(libs.plugins.spacesaver.android.feature)
}

dependencies {
    implementation(libs.androidx.paging.compose)
    testImplementation(libs.androidx.paging.testing)
}
