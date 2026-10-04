plugins {
    alias(libs.plugins.spacesaver.android.application)
    alias(libs.plugins.spacesaver.android.compose)
    alias(libs.plugins.spacesaver.hilt)
}

android {
    defaultConfig {
        applicationId = providers.gradleProperty("spacesaver.namespace").get()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // R8 rules are verified against instrumented smoke tests in Phase 8 (task 8.5).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    lint {
        // Lint 9.2's K2 analysis crashes on @HiltAndroidTest classes (DependencyGraphTest), failing
        // the build. Tests are still covered by detekt and ktlint; production code keeps full lint.
        // Remove when AGP's lint handles Hilt tests.
        ignoreTestSources = true
    }

    // Reproducible builds for F-Droid: no build-time metadata embedded in the APK.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(projects.core.work)
    implementation(projects.feature.batch)
    implementation(projects.feature.browse)
    implementation(projects.feature.home)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.settings)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
