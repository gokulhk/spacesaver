# `:core:work`

Runs batches in the background with WorkManager (plan Phase 5).

## Entry points

- `BatchWorker`: a `@HiltWorker` that runs `BatchRunner` in the foreground and publishes progress (`KEY_PROGRESS`, `KEY_COMPLETED_ITEMS`, `KEY_TOTAL_ITEMS`, `KEY_CURRENT_ITEM`).
- `WorkManagerBatchScheduler`: implements the domain's `BatchScheduler`. One unique piece of work per batch; expedited unless the batch is charging-only.
- `BatchNotifications`: the silent, ongoing progress notification.
- `ForegroundServiceTypes`: `mediaProcessing` on API 35+, `dataSync` before. See `docs/spikes/foreground-work.md`.

The Application must provide WorkManager's configuration with `HiltWorkerFactory` (see `SpaceSaverApplication`).

## Dependencies

- **May depend on:** `:core:domain`, `:core:model`, WorkManager, Hilt Work.
- **Must not depend on:** `:core:data` or `:core:media` directly; it receives their implementations through domain ports.
