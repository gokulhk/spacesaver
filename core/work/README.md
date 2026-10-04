# `:core:work`

Background batch execution: `BatchRunner`, free-space monitor, `BatchWorker` (WorkManager, foreground notification), resume and orphan cleanup.

- **May depend on:** `:core:domain`, `:core:model`, WorkManager.
- **Must not depend on:** UI code, `:core:data` implementations directly (use domain ports).
- **Status:** Arrives in Phase 5.
