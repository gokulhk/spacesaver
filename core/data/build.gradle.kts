plugins {
    alias(libs.plugins.spacesaver.android.library)
    alias(libs.plugins.spacesaver.hilt)
}

// Implements the domain ports (adapters) on top of Room, DataStore, MediaStore, and the media engine.
dependencies {
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.domain)
    implementation(projects.core.media)
    implementation(projects.core.model)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
