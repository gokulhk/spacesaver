# ADR-0006: Run JVM tests on JDK 21, compile with JDK 17

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

The plan sets a JDK 17 toolchain. Robolectric, which runs Compose UI, screenshot, and (from Phase 3) database tests on the JVM, needs a **Java 21 runtime** to emulate Android SDK 35 and later. On Java 17 it can only emulate SDK 34, three releases behind our `targetSdk` (37).

Options considered:

1. Keep everything on 17 and pin Robolectric to SDK 34. Simple, but UI tests would run against an old platform.
2. Move the whole build to JDK 21. One JDK, but a larger departure from the plan.
3. Compile with JDK 17 (unchanged) and run only the test JVMs on JDK 21.

## Decision

Option 3. The Kotlin/Java toolchain and bytecode target stay at 17. Every `Test` task runs on a JDK 21 launcher (`testJvm` in `gradle/libs.versions.toml`). Robolectric emulates the SDK in `robolectricSdk` (37), which the Compose convention writes into a generated `robolectric.properties`.

The Foojay toolchain resolver is enabled in `settings.gradle.kts`, so Gradle downloads a missing JDK 17 or 21 automatically. CI installs both with `actions/setup-java`.

Robolectric on SDK 36+ also needs `--add-opens java.base/java.io` and `--add-exports java.base/jdk.internal.access`; the convention plugin adds them to test JVMs only.

## Consequences

- Tests exercise the same API level the app targets.
- Production bytecode and the build JDK are unchanged.
- Contributors need nothing extra installed; Gradle provisions JDK 21 on first test run (requires network once).
- The two JDK versions are a small source of confusion, documented in CONTRIBUTING and `docs/testing.md`.
