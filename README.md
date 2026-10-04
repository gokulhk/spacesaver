# SpaceSaver

An open-source, offline-only Android app that helps you reclaim storage by finding heavy videos and images and safely compressing or converting them in space-aware batches.

> **Status:** early development. The full README (features, installation, FAQ) arrives with the first release.

## Privacy

- SpaceSaver declares **no internet permission**. Your media never leaves your device.
- Originals are only deleted after you review the results and confirm in the system dialog.

## Development

Requires JDK 17 (JDK 21 for tests; Gradle downloads both if missing), the Android SDK, and Android 11 (API 30) or later on the device or emulator.

```
./gradlew assembleDebug                 # build
./gradlew test                          # unit + Robolectric tests
./gradlew check                         # everything CI runs
./gradlew spotlessApply                 # auto-format
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for the workflow and [`docs/`](docs/) for architecture decisions and progress.

## License

[Apache License 2.0](LICENSE)
