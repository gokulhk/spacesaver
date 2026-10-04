# `:core:testing`

Shared test code. Pure Kotlin, so JVM and Android modules can both use it. Add it as a `testImplementation` dependency only.

## Contents

- Fakes for every domain port: `FakeMediaRepository`, `FakeStorageRepository`, `FakeSavingsRepository`, `FakeSettingsRepository`, `FakeCalibrationRepository`, `FakeBatchRepository`, `FakeBatchScheduler`, `FakeDeletionGateway`, `FakeEncoderCapabilities`, `FakeConverter`. They record calls so tests assert on behavior without mocks.
- `TestClock` (move time by hand) and `MainDispatcherRule` (for ViewModel tests).
- Builders with realistic defaults: `aVideo(height = 2160, durationSec = 60)`, `anImage(format = PNG)`, `aCandidate(id, original, output)`, `aBatchItem(...)`.

## Dependencies

- **May depend on:** `:core:domain`, `:core:model`, JUnit, Truth, kotlinx-coroutines-test.
- **Must not depend on:** Android framework classes.
