# Testing

| Level | Command | Notes |
|---|---|---|
| Unit + Robolectric | `./gradlew test` | Runs on a JDK 21 test JVM (ADR-0006); Robolectric emulates API 37 |
| Screenshots: verify | `./gradlew verifyRoborazziDebug` | Compares against baselines in `<module>/src/test/screenshots/`. CI runs this |
| Screenshots: record | `./gradlew recordRoborazziDebug` | Rewrites baselines. Review every changed image before committing |
| Instrumented | `./gradlew connectedDebugAndroidTest` | Needs a device or emulator on API 30+ |
| Everything CI runs | `./gradlew check` | Tests, lint, detekt, ktlint, coverage |

## Screenshot tests

- Each component screenshot stacks the **light theme above the dark theme** in one image, so both are reviewed together.
- Screenshots render at Pixel 5 size (`RobolectricDeviceQualifiers.Pixel5`) with Robolectric's native graphics.
- Comparison tolerates up to 1% changed pixels to absorb anti-aliasing differences between machines. A real UI change exceeds that.
- When verification fails, the diff images are in `<module>/build/outputs/roborazzi/` (CI uploads them in the `reports` artifact).
- Wrap screenshot content in a `Surface`, as real screens are, so text picks up the theme's content color.

Why not a parameterized light/dark run: Robolectric's `ParameterizedRobolectricTestRunner` produced blank captures for every test after the parameter switch, so the light/dark pair is rendered in a single capture instead.

## Android framework code on the JVM

Every Android module gets Robolectric (emulating API 37 on a JDK 21 test JVM, ADR-0006):

- **Room:** in-memory databases (`Room.inMemoryDatabaseBuilder`). Migration tests read exported schemas from `core/database/schemas/`.
- **MediaStore:** `FakeMediaStoreProvider` in `:core:data` tests, registered under the `media` authority.
- **DataStore:** a real store on a `TemporaryFolder` file; no Android needed.
- **Hilt:** `DependencyGraphTest` in `:app` builds the real graph with `HiltTestApplication`.

## Compose UI tests

Use the `androidx.compose.ui.test.junit4.v2` rule factories (`createComposeRule`, `createAndroidComposeRule`); the original factories are deprecated and warnings fail the build.
