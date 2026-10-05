# `:core:domain`

The business rules from plan Section 5, the ports (interfaces) the data layer implements, and the use cases features call. Pure Kotlin.

## Packages

| Package | Contents |
|---|---|
| `eligibility` | `VideoEligibility`, `ImageEligibility`, `SavingsThresholds`, `PngContentClassifier` |
| `estimate` | `VideoSavingsEstimator`, `ImageSavingsEstimator`, `RatioCalibrator`, `CompressionRatios`, `CalibrationTable`, `ProcessingSpeed` |
| `plan` | `BatchPlanner`, `BatchPlanConfig`, `ReservePolicy`, `PlanSimulator`, `ConversionPlan`, `StorageBudget` |
| `batch` | `BatchStateMachine`, statuses and events, `ReviewOptions` |
| `execution` | `BatchRunner`, `ConvertBatchItem`, `SpaceGuard`, `FreeSpaceMonitor`, `ConversionRecorder`, `CancelBatch`, `ReconcileBatches` |
| `savings` | `SavingsCalculator`, `SavingsEvent`, `SavingsSummary` |
| `conversion` | `MediaConverter`, `ConverterRegistry`, `ConversionSpec`, `ConversionOption`, `AudioPolicy`, `TargetSelection` |
| `repository` | Ports: media, storage, savings, batches, settings, deletion, encoder capabilities, calibration, scheduler, outputs, zone |
| `usecase` | One class per action, invoked with `operator fun invoke` |
| `result` | `DomainResult`, `DomainError` |

## Conventions

- Failures are `DomainResult.Failure(DomainError)`, never exceptions across layers.
- Time comes from an injected `Clock`; the time zone from `ZoneProvider`.
- Every threshold is a named constant with a KDoc explaining the value.
- Use cases are tested against the fakes in `:core:testing`.

## Dependencies

- **May depend on:** `:core:model`, kotlinx-coroutines, `paging-common` (pure Kotlin), `javax.inject`.
- **Must not depend on:** Android framework classes, `:core:data`, or any UI module.

Coverage threshold: **90% lines** (currently 95.5%).
