plugins {
    alias(libs.plugins.spacesaver.jvm.library)
}

dependencies {
    api(projects.core.model)
    // Paging is pure Kotlin; the Browse port exposes PagingData so 50k-item libraries stay lazy.
    api(libs.androidx.paging.common)
    api(libs.kotlinx.coroutines.core)
    // @Inject constructors, so Hilt can build use cases without this module knowing about Hilt.
    implementation(libs.javax.inject)

    testImplementation(projects.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

kover {
    reports {
        verify {
            rule("Domain line coverage") {
                // Section 5 rules are the heart of the app and are pure Kotlin, so they are held to a
                // much higher bar than the 70% project-wide minimum.
                minBound(90)
            }
        }
    }
}
