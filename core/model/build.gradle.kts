plugins {
    alias(libs.plugins.spacesaver.jvm.library)
}

dependencies {
    // Only for the @Dispatcher qualifier annotation; no DI framework code.
    implementation(libs.javax.inject)
}
