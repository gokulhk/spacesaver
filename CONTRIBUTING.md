# Contributing to SpaceSaver

Thanks for helping people reclaim storage safely. This guide covers how to set up the project and what we expect from a contribution.

By participating you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Development setup

Prerequisites:

- Latest stable Android Studio
- JDK 17 to build and JDK 21 to run tests (ADR-0006). Gradle downloads either one automatically if it's missing
- Android SDK with the `compileSdk` platform from `gradle/libs.versions.toml` (Android Studio or Gradle installs it on first build)
- An emulator or device running Android 11 (API 30) or later

Clone the repository, open it in Android Studio, let Gradle sync, and run the `app` configuration.

Command-line equivalents:

```
./gradlew assembleDebug                 # build
./gradlew test                          # unit + Robolectric tests
./gradlew connectedDebugAndroidTest     # instrumented tests (device/emulator required)
./gradlew verifyRoborazziDebug          # screenshot tests
./gradlew recordRoborazziDebug          # update screenshot baselines (review the images!)
./gradlew check                         # everything CI runs
./gradlew spotlessApply                 # auto-format
```

## How we work

### Test-driven development

Every change starts with a failing test:

1. **Red:** write the test and watch it fail for the expected reason.
2. **Green:** write the minimum code that makes it pass.
3. **Refactor:** clean up while the tests stay green.

Never delete, skip, or weaken a test to make it pass. If a test is wrong, fix it deliberately and explain why in the PR.

### Rules that keep the app trustworthy

- Never add the `INTERNET` permission or a networking library.
- `:core:model` and `:core:domain` are pure Kotlin. No Android framework classes.
- Features depend on `:core:domain`, never on `:core:data` directly.
- No magic numbers: thresholds and factors live in named, documented constants.
- Inject `java.time.Clock`, coroutine dispatchers, and system services.
- Prefer fakes (in `:core:testing`) over mocks.
- Model failures with sealed result types, not exceptions crossing layers.
- Strings belong in `strings.xml`.

### Definition of Done

- [ ] Tests written first and now passing.
- [ ] `./gradlew check` passes locally.
- [ ] Public APIs documented with KDoc.
- [ ] No new lint, detekt, or ktlint warnings.
- [ ] Strings in `strings.xml`, never hardcoded in UI code.
- [ ] UI changes include Compose previews in light and dark themes and an updated screenshot test.
- [ ] `docs/PROGRESS.md` updated when a plan task is completed.

### Commits and branches

- Use [Conventional Commits](https://www.conventionalcommits.org/) with a module scope, for example `feat(domain): plan batches by estimated output size`.
- Types: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `build`, `ci`.
- Subject lines are imperative and under 72 characters. Add a body when the reason isn't obvious.
- Branch names: `feat/<short-topic>`, `fix/<short-topic>`, or `phase/<NN>-<name>` for plan phases.

### Pull requests

Fill in the PR template, link related issues, and keep PRs focused on one change. CI must be green before review.

See [`docs/testing.md`](docs/testing.md) for details on each test level.

## Architecture decisions

Significant decisions are recorded as ADRs in [`docs/adr/`](docs/adr/). To propose one, copy [`docs/adr/template.md`](docs/adr/template.md) to the next number, fill in Context, Decision, and Consequences, and open a PR.

## Reporting bugs and requesting features

Use the issue templates. For security problems, follow [SECURITY.md](SECURITY.md) instead.
