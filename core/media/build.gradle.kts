plugins {
    alias(libs.plugins.spacesaver.android.library)
    alias(libs.plugins.spacesaver.hilt)
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.heifwriter)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.muxer)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
}
