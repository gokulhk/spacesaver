plugins {
    alias(libs.plugins.spacesaver.android.library)
    alias(libs.plugins.spacesaver.hilt)
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
