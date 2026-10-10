# Progress

Task checklist for the MVP, mirroring the plan's Section 8. Tick a task when it meets the Definition of Done, and note anything a human should know.

## Phase 0 — Project bootstrap

- [x] **0.1 Repository and build skeleton**
  - Every module from the module map exists with a build script and a README. Convention plugins live in `build-logic/`. Versions are pinned in `gradle/libs.versions.toml`.
  - Smoke tests in `:core:domain` and `:app`. `./gradlew assembleDebug test` passes.
  - Note: `:core:testing` is a **pure Kotlin** module, so JVM modules (`:core:domain`) can use its fakes too. Android-only test helpers will get their own module if they are needed.
  - Note: `local.properties` (gitignored) must point `sdk.dir` at the Android SDK unless `ANDROID_HOME` is set.
- [x] **0.2 Code quality tooling**
  - `./gradlew check` runs unit tests, Android Lint (warnings as errors, all 16 modules), detekt (with Compose rules), ktlint via Spotless (with Compose rules), and Kover verification.
  - Coverage: `:core:domain` ≥ 90% lines, whole project ≥ 70% lines. Generated code (Hilt, Room, Compose singletons) and `@Composable`/`@Preview` functions are excluded.
  - Each tool was checked to make sure it fails the build on a violation.
- [x] **0.3 CI**
  - `.github/workflows/ci.yml`: on PRs and pushes to `main`, runs `check`, builds the debug APK, and uploads reports and the APK.
  - `.github/workflows/instrumented.yml`: nightly and on demand, runs instrumented tests on API 30 and API 37.
  - Note: the workflows have not run on GitHub yet because the repository has no remote.
- [x] **0.4 Open-source scaffolding**
  - LICENSE (Apache-2.0), CONTRIBUTING, CODE_OF_CONDUCT (Contributor Covenant 2.1), SECURITY, issue and PR templates, `.editorconfig`, ADR template and ADRs 0001–0005.
  - Open: `CODE_OF_CONDUCT.md` still says `[INSERT CONTACT METHOD]`. The maintainer needs to supply a contact.

## Phase 1 — Design system and theme

- [x] **1.1 Palette and color schemes**
  - `LightColors`, `DarkColors`, and `SpaceSaverColors` match plan Section 6.2; `PaletteTest` pins every value.
  - Note: Material roles the plan doesn't list (surface containers, inverse, dim/bright) are derived teal-greys so components never show Material's purple baseline. Values are in ADR-0005.
- [x] **1.2 Contrast test**
  - `ContrastTest` checks every on-color pair, text on all surface containers, outlines, and the extended tokens in both schemes. All pass; no hex adjustments were needed.
- [x] **1.3 Theme composable and theme mode**
  - `ThemeMode` (in `:core:model`) and `SpaceSaverTheme(themeMode)`. Robolectric tests cover SYSTEM following night mode, LIGHT/DARK overriding it, and status/navigation bar icon contrast. `MainActivity` uses the theme.
  - Note: JVM tests now run on JDK 21 so Robolectric can emulate API 37 (ADR-0006). Compilation stays on JDK 17.
- [x] **1.4 Typography, shapes, spacing tokens**
  - System-font type scale with tabular figures on `displaySmall`; shapes 8/12/16/28 dp; `Spacing` object. Pinned by `ThemeTokensTest`.
- [x] **1.5 Shared components**
  - All 11 components from plan Section 6.4, stateless, with light/dark/200% font previews (`@PreviewComponents`).
  - Behavior tests for every component (slider drag and accessibility action, storage bar spoken labels, plan expand/blocked, suggestion toggle, media row click/long-press/selection, dialog buttons, 48 dp touch target).
  - 15 Roborazzi screenshots (light above dark in one image; dialog separately). `verifyRoborazziDebug` runs in CI.
  - Note: components take pre-formatted `SizeText(display, spoken)`; the byte formatter arrives in `:core:ui` later. Thumbnails are slots, so the design system doesn't depend on Coil.

## Phase 2 — Domain models and pure logic

- [x] **2.1 Value objects** (`:core:model`)
  - `ByteSize` (SI units, non-negative, saturating `minusOrZero`, locale-aware formatting), `Bitrate`, `Resolution`, `MediaItem` (+ `VideoDetails`, `AudioTrack`), `MediaFormat.fromMimeType`, `VideoPreset`/`VideoCodec`, `ImageFormatPreference`.
  - Note: presets compare the **short edge**, so portrait 4K videos count as 4K.
  - Note: video formats identify the codec, not the container (an HEVC `.mov` is `MP4_HEVC`). Videos with an unknown codec are `VIDEO_OTHER`.
  - Note: MB and above always show one decimal, as specified (`"1.0 MB"`, `"128.0 GB"`). Say if whole numbers should drop the `.0`.
- [x] **2.2 Eligibility rules**
  - `VideoEligibility`, `ImageEligibility`, `SavingsThresholds` (20% and 5 MB / 200 KB), `PngContentClassifier` (unique-color ratio on a downscaled sample), `TargetSelection` (HEVC vs H.264, HEIC vs WebP).
  - Note: built after 2.3 because "saving < 20%" needs the estimators.
  - Note: unclassified PNGs are treated as graphics (lossless WebP), so nothing unclassified is ever compressed lossily.
- [x] **2.3 Estimators**
  - `VideoSavingsEstimator` (plan formula, source bitrate when lower, AAC passthrough or 128 kbps re-encode), `ImageSavingsEstimator` (priors with ±15% range, calibrated ratios exact), `RatioCalibrator` (median after dropping values beyond 3×IQR, at least 3 samples), `ProcessingSpeed`.
- [x] **2.4 Batch planner**
  - `BatchPlanner.planNext`, `BatchPlanConfig` (1.2 safety factor, 25 items), `ReservePolicy` (max(1 GB, 5%), user minimum 500 MB). Ties in savings are broken by media ID so plans are deterministic.
- [x] **2.5 Plan simulator**
  - `PlanSimulator` and `ConversionPlan` with per-batch savings, space needed, and duration.
- [x] **2.6 Batch state machine**
  - `BatchStateMachine` (all transitions table-tested), `ReviewOptions` ("Keep both" only when the next item fits), `DomainResult`/`DomainError`.
  - Note: added an item `CANCELLED` state (Task 5.1 needs it, the plan's list doesn't have it) and `DomainError.InvalidTransition`, `NothingToConvert`, `BatchNotFound`.
- [x] **2.7 Savings calculations**
  - `SavingsCalculator`, `SavingsEvent`, `SavingsSummary`. Outputs that aren't smaller are rejected, so savings can't be negative. Today's boundary is tested across zones and DST changes (including a day whose midnight didn't exist).
- [x] **2.8 Repository interfaces and use cases**
  - Ports: `MediaRepository`, `StorageRepository`, `SavingsRepository`, `BatchRepository`, `SettingsRepository`, `ConverterRegistry` (+ `MediaConverter`, plan 4.4), `DeletionGateway`, `EncoderCapabilities`, plus `CalibrationRepository`, `BatchScheduler`, and `ZoneProvider`.
  - Use cases: `ObserveSavingsSummary`, `ObserveStorageOverview`, `ObserveSuggestions`, `BuildConversionPlan`, `StartNextBatch`, `ResolveBatchReview`, `DeleteMediaItems`, `ObserveMediaBySize`; shared `StorageBudget`.
  - Fakes for every port, `TestClock`, `MainDispatcherRule`, and builders (`aVideo`, `anImage`, `aCandidate`, `aBatchItem`) in `:core:testing`.
  - Note: `SavingsRepository.observeTotals(todayStart)` returns lifetime and today together. Two separate flows briefly showed a half-updated banner.
  - Note: `:core:domain` depends on `paging-common` (pure Kotlin) so Browse stays paged for 50k+ items.
  - Coverage: `:core:domain` 95.5% lines, 91.2% branches.

## Phase 3 — Data layer

- [x] **3.1 Room database** (`:core:database`)
  - Entities `SavingsEventEntity`, `BatchEntity`/`BatchItemEntity`, `ConvertedFileEntity`, `CalibrationEntity` (table `calibration_samples`), DAOs, `SpaceSaverDatabase` v1, `Migrations.ALL`, Hilt `DatabaseModule`. Schema exported to `core/database/schemas/` (commit it).
  - Lifetime and today totals come from **one** SQL query. Batch items snapshot the original's metadata so a batch can be reviewed even if MediaStore changes.
  - Migration harness: opens v1 from the exported schema and checks the migration chain is contiguous.
  - Note: Room's plugin only feeds schemas to instrumented tests, so the convention also adds them to **debug** assets for Robolectric. Release builds never contain them.
- [x] **3.2 Settings DataStore** (`:core:datastore`)
  - `SettingsDataSource` + `StoredSettings`; unknown or out-of-range stored values fall back per field; a corrupt file is replaced with defaults.
- [x] **3.3 MediaStore scanner** (`:core:data`)
  - `MediaStoreScanner` (Bundle query args, size/date sort with `_ID` tie-break, change observation, revoked permission returns empty) and `MediaStorePagingSource` (offset keys).
  - Tested against `FakeMediaStoreProvider`, a real `ContentProvider` backed by in-memory SQLite, so sorting and paging go through SQL.
  - Note: MediaStore doesn't expose the audio codec; videos are assumed to have AAC audio (what phone cameras record) for estimates. Phase 4's converter inspects the real track.
- [x] **3.4 Storage stats**
  - `StorageStatsSource` wrapper (`StorageStatsManager`, `StatFs` fallback); `StorageRepositoryImpl` re-emits on `refresh()` and on library changes; free space is clamped to total.
- [x] **3.5 Repository implementations**
  - `MediaRepositoryImpl` (marks SpaceSaver outputs by ID or path/size/time fingerprint), `StorageRepositoryImpl`, `SavingsRepositoryImpl`, `BatchRepositoryImpl` (review applied in one transaction), `SettingsRepositoryImpl`, `CalibrationRepositoryImpl`; Hilt `DataModule`.
  - `:app` `AppModule` provides `Clock`, `ZoneProvider`, `@Dispatcher(IO/DEFAULT)`, `BatchPlanConfig`, `SavingsThresholds`. `DependencyGraphTest` builds the real Hilt graph on Robolectric.
  - Note: Robolectric now applies to every Android module (it used to be Compose-only).
  - Note: lint skips `:app` **test** sources because lint 9.2 crashes on `@HiltAndroidTest` classes. Production lint is unchanged.
  - Not yet implemented (later phases): `DeletionGateway` (6.1), `EncoderCapabilities` (4.2), `ConverterRegistry` (4.1), `BatchScheduler` (5.3).

## Phase 4 — Media engine

- [x] **4.0 Test fixtures**: `fixtures/generate.sh` (ffmpeg), 7.5 MB committed under `core/media/src/androidTest/assets/`. EXIF is stamped and the screenshot is drawn on-device by the tests (see `fixtures/README.md`).
- [x] **4.1 Converter registry**: `DefaultConverterRegistry` over a Hilt `@IntoSet` multibinding.
- [x] **4.2 Encoder capabilities ⚠️ SPIKE**: `EncoderCapabilityRules` (pure) + `AndroidEncoderCapabilities`; `ConversionSpecResolver` in the domain. Findings: `docs/spikes/encoder-capabilities.md`.
  - Note: the emulator has only software encoders, so the app picks H.264 and WebP there. **Physical device run still outstanding.**
- [x] **4.3 Video converter**: `Media3VideoConverter` writes straight into the pending MediaStore entry through a custom `StreamMp4MuxerFactory` (see `docs/spikes/video-output-path.md`). Tested: 4K → 1080p, smaller, H.264 + AAC tracks, progress reaches 1.0, cancellation leaves no output.
- [x] **4.4 Image converters**: `WebpImageConverter` (lossy/lossless) and `HeicImageConverter` (HeifWriter). Pixels stay unrotated; orientation is kept as metadata.
  - Note: for a picture-heavy screenshot, lossless WebP came out ~9% **larger** than Android's PNG. The verifier rejects such outputs. Text-heavy screenshots, the common case, shrink.
- [x] **4.5 Metadata preservation ⚠️ SPIKE**: EXIF date/offset, GPS, orientation, make/model and `Software=SpaceSaver` for WebP and HEIC; creation time and location for video; MediaStore `DATE_TAKEN` matches after publishing. `DATE_MODIFIED` can't be preserved. Findings: `docs/spikes/metadata-preservation.md`.
- [x] **4.6 Output writer and verification**: `MediaStoreOutputWriter` (pending rows in the original's folder, `_compressed` suffix, fallback folders), `OutputNaming`, `OutputVerification` (pure), `AndroidOutputProbe` (strict image decode; first **and last** video frame).
  - Note: converters now return a **pending** output; the Phase 5 runner verifies and publishes it.
  - Tests: 25 JVM tests in `:core:media`, 7 in the domain, and 27 instrumented tests, all passing on the `SpaceSaver_Test_API_36` emulator.

## Phase 5 — Batch execution

- [x] **5.1 Batch runner**: `BatchRunner` (state machine, sequential items, resumable) + `ConvertBatchItem` (resolve spec, find converter, convert under `SpaceGuard`, verify, publish, record) + `ConversionRecorder` (converted-file record, calibration samples) + `CancelBatch`.
  - Note: built after 5.2, since the runner's out-of-space test needs the monitor.
  - Note: **interruption is not cancellation.** When WorkManager stops the worker (charger unplugged, system limits) or the process dies, the batch stays resumable: the interrupted item is reset and redone on the next run. The user's Cancel is the separate `CancelBatch` use case, which marks the remaining items and the batch `CANCELLED`. (A first version treated every cancellation as the user's; designing the worker exposed the problem.)
  - New ports: `OutputGateway` (implemented by `AndroidOutputGateway`), `StorageRepository.freeSpace()`, batch item updates, `CalibrationRepository.record`, `BatchScheduler.isScheduled/cancel`; item event `RESET`.
- [x] **5.2 Free-space monitor**: `FreeSpaceMonitor` polls every 2 s; `SpaceGuard` refuses to start an item whose cost doesn't fit above the reserve, and cancels it if free space drops below the reserve mid-conversion (`SKIPPED_NO_SPACE`).
- [x] **5.3 WorkManager worker ⚠️ SPIKE**: `BatchWorker` (`@HiltWorker`, foreground, conflated progress updates), `BatchNotifications`, `WorkManagerBatchScheduler` (unique work per batch; expedited, or charging constraint), manifest permissions. Findings: `docs/spikes/foreground-work.md` (`mediaProcessing` on API 35+, `dataSync` before).
- [x] **5.4 Resume and orphan cleanup**: `ReconcileBatches` runs on app start: re-schedules interrupted batches without work, deletes this app's pending outputs no item references (only while nothing runs), reports batches awaiting review.
  - `SpaceSaverApplication` provides WorkManager's configuration (Hilt worker factory; default initializer removed) and launches reconcile in an application scope.
  - Tests: 22 domain tests for the runner, monitor, cancel and reconcile; 10 Robolectric tests in `:core:work`; new data and database tests; 3 new instrumented gateway tests (30/30 on the emulator); `DependencyGraphTest` builds the full graph and runs reconcile.

## Phase 6 — Deletion and savings ledger

- [x] **6.1 Deletion gateway**: `AndroidDeletionGateway` builds one `MediaStore.createDeleteRequest` (through the `DeleteRequestFactory` seam) and suspends on `DeletionRequests` until `DeletionRequestHost` (in `MainActivity`) shows the system dialog and reports `RESULT_OK` (deleted) or anything else (declined). Requests whose caller went away are skipped; dialogs show one at a time. Own files (rejected outputs) are deleted without a dialog.
  - Tests: broker (3), gateway with exact URIs and own-file deletion against the fake MediaStore (4), host with a fake `ActivityResultRegistry` (3), real `createDeleteRequest` on the emulator (1). The domain contract (approved → items deleted + ledger events; declined → nothing changes) is covered by the existing `ResolveBatchReview` and `DeleteMediaItems` tests.
- [x] **6.2 Ledger integration**: a review's statuses and savings events are written in one Room transaction (`BatchRepositoryImpl.applyReview`). Tests: a failing ledger write rolls back the status changes (checked by removing the transaction: the test fails), and the summary flow re-emits after a review. `DependencyGraphTest` now injects every domain use case.
  - Note: Browse deletion (`DeleteMediaItems`) records its events after the system deletes the files. A file deletion can't share a database transaction; a crash in between would lose that saving from the ledger, never record a false one.
- [x] **6.3 Midnight rollover**: `ObserveSavingsSummary` re-subscribes at every local midnight (`SavingsCalculator.timeUntilNextDay`, DST-safe). Tests use `SchedulerClock` (clock driven by virtual time) for Kolkata, UTC, and a 23-hour New York day.
  - Note: `delay` doesn't count deep sleep; a midnight passed while asleep is caught up when the UI re-collects on becoming visible (Phase 7 collects with lifecycle awareness).

## Phase 7 — Feature UI

- [x] Groundwork
  - `:core:ui`: `SizeTextFormatter` (SI display, spoken words via plurals, approximate "~"/"about"), `DurationTextFormatter` ("about 2 h 10 min"), `ErrorMessageMapper` (every `DomainError`), `WhileUiSubscribed`.
  - `ByteSize.formatParts` exposes the number and unit separately for spoken phrases.
  - New `:core:screenshot-testing` with light/dark component and screen capture; the design system migrated to it (baselines unchanged).
  - Domain: `ObservePendingReviews`; settings remember whether media access was ever requested (`mediaAccessRequested`).
- [x] 7.1 Onboarding/permissions
  - `MediaAccess` (full, limited, not requested, denied, permanently denied) resolved by a pure, table-tested function; `MediaPermissions` per API level (≤32 storage, 33 granular + notifications, 34+ user-selected).
  - "Permanently denied" = denied after a request with no rationale; the request is remembered in DataStore so it survives process death.
  - Screen re-reads permissions on every resume (the user may change them in system settings); full access continues automatically, limited access shows the banner and waits for "Continue".
  - Media permissions declared in `:feature:onboarding`'s manifest.
  - Verified on the emulator: notification and media dialogs, "Allow all", then home.
- [x] 7.2 Home
  - Banner, storage bar, pending-review cards, plan card (ready / blocked with "Free up X" / empty), suggestion cards with switches and the preset bottom sheet with quality notes.
  - The plan is rebuilt when included candidates or free space change; "Start batch 1" calls `StartNextBatch` and reports the batch or an error (snackbar).
  - Verified on the emulator with fixture media (two 4K videos, three JPEGs): preset change from 4K→Full HD to 4K→HD raised the plan from ~54.1 MB to ~64.7 MB.
  - The notification permission is asked for when a batch starts (not on onboarding); the batch starts whatever the answer.
  - Minimal navigation in `MainActivity`: onboarding until media can be read, then home. Starting a batch and opening a review stay on home until 7.5, 7.6 and 7.8.
- [x] 7.3 Browse
  - Videos/Images tabs with the category total, "Largest first"/"Newest first", and a paged list with system thumbnails (`MediaThumbnail`, MediaStore's cache; no image library).
  - Long press starts multi-select; taps toggle while selecting; back or ✕ clears. The bottom bar offers Convert and Delete.
  - Delete: in-app confirmation with the space freed, then the system dialog; approved deletions are recorded as DELETION savings and the list reloads (MediaStore doesn't notify the pager). A declined system dialog keeps the selection.
  - Convert: `AddToPlan` adds eligible files to the plan (`PlanAdditionsRepository`, persisted in DataStore since 7.4), and they stay in Home's plan even when their suggestion is switched off; ineligible files are reported.
  - Fix (found on the emulator): files in an unfinished batch were suggested again, so a second batch re-converted them while the first awaited review. Suggestions now leave out every file in an active batch (`BatchRepository.observeActiveBatches`, which also replaces `observeAwaitingReview`). Eligibility routing moved into `MediaEligibility`.
  - Minimal Home/Browse bottom bar in `SpaceSaverApp` until 7.8.
  - Verified on the emulator: thumbnails, tabs, delete via system dialog (list, total, and snackbar update), Convert with the JPEG suggestion off (plan ~52.8 → ~53.2 MB), and the notification prompt on "Start batch 1" with the permission revoked.
- [x] 7.4 Plan detail
  - The plan moved into the domain: `ObservePlan` (suggestions + switches + presets + additions + free space → `PlanOverview` with `PlanStatus` Empty/Ready/Blocked) and `UpdatePlanChoices`. Home and Plan detail share it, so a change on one shows on the other. Switches and presets stay for the app process (`InMemoryPlanChoicesRepository`).
  - Files added from Browse now survive restarts (`PlanAdditionsDataSource`, a string set in the settings DataStore; IDs are never reused by MediaStore, so stale entries are harmless).
  - Screen: totals, per-suggestion switch + preset chips + quality note, each batch with every item's estimated output ("From 620.0 MB → ~150.0 MB"), and a "Waiting for space" section with the free space each blocked item needs. Opened from "See full plan" on Home; back returns.
  - New design-system `MediaInfoRow`: `MediaListRow`'s layout without actions, for read-only lists.
  - Verified on the emulator: Plan detail with real thumbnails and estimates; an added photo stayed in the plan after a force-stop with its suggestion switched off; Home showed the same plan.
- [x] 7.5 Batch progress
  - Screen (`:feature:batch`): overall progress and percentage, elapsed time (ticks each second) and time left, each file's status with a bar while converting and the measured saving once done, Cancel with confirmation, and Review once converted. A batch waiting to start (e.g. for the charger) says so.
  - Live progress: the worker's progress data now includes the current file's fraction and the run's start time (`ProgressKeys`); `BatchScheduler.observeProgress` reads it back, and `ObserveBatchRun` combines it with item statuses and a time-left estimate from the calibrated speed.
  - Notification deep link: tapping the batch notification opens `spacesaver://batch/{id}` (`BatchDeepLink`), limited to this package. `MainActivity` (singleTop) handles it on a cold start and when already open.
  - Home shows a "Batch in progress" card (`ObserveRunningBatch`), and "Start batch 1" opens the progress screen.
  - Fix: cancelling after some files converted used to mark the batch CANCELLED (a final state), stranding those outputs with no way to review them. Now such a batch goes to review, with the rest marked cancelled; a batch with no outputs is cancelled; and a batch awaiting review can't be cancelled.
  - New design-system status `ProgressStatus.CANCELLED` ("Cancelled"); previously cancelled files read "Skipped, not enough space".
  - Verified on the emulator: live progress, Home → notification → back to the same screen (one activity), Cancel before and after the first file converted, and the running-batch card.
- [x] 7.6 Batch review
  - Screen (`:feature:batch`): each converted file with original → compressed size and its saving, a keep switch (on by default, saved as you toggle via `SetItemAccepted`), and the footer "Accepting 3 of 4 · frees 2.6 GB" with "Delete N originals permanently & continue", "Keep both & continue" (only while the next batch fits, with a note that it will be smaller), and "Stop here". With every file switched off the primary action reads "Discard all & continue".
  - Before/after viewer: photos as the centre crop at full resolution (one image pixel per screen pixel), videos as frames at a quarter, half, and three quarters through; both with the design-system slider. The drag band around the handle is excluded from the system back gesture.
  - `ObserveBatchReview` (decisions + available actions + remaining plan) and `CompleteReview` (applies the action, then starts the next batch, or finishes, or reports a cancelled delete dialog).
  - A cancelled system delete dialog keeps the review open with "Nothing was deleted. Your files are still here."
  - Opened from the progress screen's Review button and Home's pending-review cards; continuing goes to the next batch's progress, finishing returns Home.
  - Fixes found while testing: (1) originals kept with "Keep both" were suggested again, so "& continue" would convert them again at once; kept originals are now excluded (`BatchRepository.observeKeptOriginals`, replacing `unfinishedBatches`). (2) Originals deleted outside the app since conversion stayed in the review and would have been counted as savings a second time; they are now left out of the review, left out of the delete dialog, and finalized without savings (`DeletionGateway.existing`).
  - Verified on the emulator: a real review with thumbnails, video frame and full-resolution photo comparison, rejecting a file, declining then allowing the system dialog (originals deleted, rejected output discarded, savings +54.4 MB), and a review whose video originals were already gone.
- [x] 7.7 Settings
  - Screen (`:feature:settings`): Theme (System/Light/Dark), Photo format (HEIC/WebP; HEIC disabled with an explanation when the phone can't encode it), Free-space reserve (Default for this phone, or 1/2/5/10 GB, with an explanation), Only while charging (applies to batches started afterwards), the privacy statement ("no internet permission"), the version, and an open-source licenses page bundled with the app (library list plus the Apache License 2.0 text from `res/raw`).
  - Domain: `ObserveSettingsOverview` (settings + default reserve + HEIC support), `UpdateSettings`, `ObserveThemeMode`; `ReservePolicy.USER_CHOICES`.
  - The theme applies app-wide at once: `MainActivity` follows `MainViewModel.themeMode` through `AppTheme`, which draws nothing until the setting is read so a saved dark theme never flashes light (`AppThemeTest`).
  - Settings joined the bottom bar; the selected tab now lives in `SpaceSaverApp`, so returning from a screen opened on top (licenses, plan detail, progress, review) keeps the tab.
  - Verified on the emulator: dark theme applied to every screen at once and persisted across a restart; HEIC disabled with its explanation; licenses page; back from licenses returns to Settings.
- [x] 7.8 Navigation
  - Navigation Compose with type-safe `@Serializable` routes (`Destinations.kt`) in `SpaceSaverNavHost`, replacing the interim state-based switching.
  - Bottom bar on Home, Browse, and Settings only; tabs replace each other above Home and save/restore their state.
  - Back-stack rules: onboarding is removed once finished; a review replaces the progress screen it came from, and the next batch's progress replaces the review, so Back always leads Home.
  - Notification deep link `spacesaver://batch/{batchId}` (`BatchDeepLink.BASE_PATH`): on a cold start Navigation opens the batch with Home beneath; with the app open, `openBatchFromLink` puts it on top of the current screen and Back returns there.
  - Destinations take their content from `AppScreens` (production `FeatureScreens`), so `NavigationTest` checks every back-stack rule with stub screens and a `TestNavHostController`, without Hilt.
  - Fix: finished batches showed "Saves" for files whose compressed copy was discarded; they now read "Kept the original", "Kept both versions", or "Saved X".
  - Verified on the emulator: cold start from the link (progress, then Back to Home) and warm (opened above Settings, Back to Settings, same activity instance). `assembleRelease` succeeds with R8; running the minified build is Task 8.5.

## Device findings after Phase 7

Found by running on a physical phone (2026-10-10), each reproduced and fixed with regression tests:

- [x] **Portrait videos came out landscape.** Media3 encodes portrait video as landscape frames and carries the turn on the track format; its own muxer wrapper writes it to the file, but `StreamMp4MuxerFactory` (which replaces that wrapper) never did. Fixed there. Output verification had also been told to ignore rotation ("rotation metadata can swap the edges"), which is why this passed; it now compares orientation, using the dimensions a viewer sees for both the original and the output.
- [x] **Converted videos had no sound.** A batch item rebuilt from the database carries no audio details (only the duration of a video's details survives), `AudioPolicy.forTrack(null)` reads that as "no audio", and the converter then called `setRemoveAudio(true)`. The converter no longer removes audio on the plan's say-so. The earlier converter tests passed the audio policy directly, and the emulator test videos had no sound, so neither could show it. **Videos converted before this fix are silent.**
  - Safeguards added: output verification fails (original kept) when the converted file has no sound, fewer audio tracks, or fewer channels than the original; a video with several audio tracks is refused up front (Transformer keeps one track per video, so converting would drop the rest).
- [x] **Failures and rejections gave no reason.**
  - Browse "Convert": each file that can't be added is listed with its reason in a dialog (`IneligibleReason` made user-facing, plus `ALREADY_HANDLED`), replacing the "1 file can't be made smaller" snackbar.
  - Batch progress: a failed file shows why ("This phone can't read the video's sound format (AC-3)…", "This video has 2 audio tracks…"). Media3 decoder errors are mapped to `UnsupportedAudio` / `UnsupportedVideo` naming the codec. Reasons are stored with the item (`failure_reason`, `failure_detail`: Room schema version 2, additive migration; old items keep the general line).
  - Home ("Couldn't start the batch") and Review ("Couldn't finish the review") errors are dialogs too (`InfoDialog` in the design system). Snackbars remain only for confirmations.
- Verified on the emulator through the real UI with 4K phone-shaped videos: portrait (rotation flag and native) converted upright with stereo sound at the same level (-24.1 dB vs -24.09 dB); the two-track and AC-3 videos failed with their reasons; the 720p video was explained in the Browse dialog.

## Phase 8 — Hardening

- [ ] 8.0 Test hygiene: instrumented tests leave empty `DCIM/SpaceSaverTest*` folders on the device (200+ on the test AVD); clean them up in teardown and delete the existing ones. Also check orphan cleanup: a 0-byte `photo2_compressed.webp` was left on the test AVD after a cancelled batch
- [ ] 8.1 Performance
- [ ] 8.2 Privacy guard
- [ ] 8.3 Accessibility pass
- [ ] 8.4 Localization readiness
- [ ] 8.5 Release build

## Phase 9 — Documentation pass and release prep

- [ ] 9.1 KDoc and Dokka
- [ ] 9.2 README, CONTRIBUTING, ARCHITECTURE, module READMEs
- [ ] 9.3 Fastlane metadata for F-Droid
- [ ] 9.4 Release workflow
