# Spike: foreground work for long conversions (Task 5.3)

**Question:** Which foreground service type and WorkManager setup let a batch run for many minutes, on API 30 through 37, and what are the limits?

## Findings

| API | Foreground service type | Permission | Time limit |
|---|---|---|---|
| 30–33 | `dataSync` (any type works; none is enforced) | `FOREGROUND_SERVICE` | none |
| 34 | `dataSync`: types are required, and there is no media-processing type yet | `FOREGROUND_SERVICE_DATA_SYNC` | none |
| 35+ | `mediaProcessing`, made for transcoding | `FOREGROUND_SERVICE_MEDIA_PROCESSING` | 6 hours per 24 hours; then `Service.onTimeout`, and WorkManager stops the worker |

`dataSync` on API 35+ has the same 6-hour limit, so `mediaProcessing` costs nothing extra and describes the work honestly (Play policy checks types against what apps do).

Our 25-item batches take minutes, far from the limit. If it is ever hit, WorkManager stops the worker; the batch is **resumable** (see below) and continues on the next attempt.

## Setup

- `core/work/src/main/AndroidManifest.xml` merges `foregroundServiceType="dataSync|mediaProcessing"` into WorkManager's `SystemForegroundService` and declares the permissions above plus `POST_NOTIFICATIONS`.
- `ForegroundServiceTypes.forBatches()` picks the type per API level; `BatchWorker` calls `setForeground` before working and on each progress update (low-importance channel, silent, ongoing).
- `WorkManagerBatchScheduler`:
  - Normal batches: **expedited** (`RUN_AS_NON_EXPEDITED_WORK_REQUEST` when out of quota), so they start right away.
  - Charging-only batches: regular work with `requiresCharging`. Expedited work can't have a charging constraint.
  - One unique work name per batch (`batch-<id>`, `KEEP`), so a double tap can't start two runners on the same batch.

## Interruptions vs. cancel

WorkManager stops workers for reasons other than the user: charger unplugged (charging-only), memory pressure, the time limit above, a reboot. In all of these `BatchRunner` just stops and leaves the interrupted item `CONVERTING`; the next attempt resets it to `QUEUED` and continues. The converter deletes its partial output on cancellation, and anything a process death leaves behind is deleted by `ReconcileBatches` on the next app start.

The user's **Cancel** is different: `CancelBatch` cancels the unique work and marks the running and remaining items and the batch `CANCELLED`.

## Not verified

- Behavior at the 6-hour limit (not practical to test). It relies on WorkManager 2.10+'s `onTimeout` handling.
- OEM battery savers (Xiaomi, Samsung) that kill foreground services more aggressively. To check on physical devices; the resumable design limits the damage.
