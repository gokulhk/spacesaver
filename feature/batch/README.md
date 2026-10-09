# `:feature:batch`

Batch progress and batch review with before/after comparison. (Plan detail lives in `:feature:home`, which shares its plan text and helpers.) ViewModels expose `StateFlow<UiState>` and accept `UiEvent`s.

- **May depend on:** `:core:domain`, `:core:model`, `:core:designsystem`, `:core:ui` (set by the `spacesaver.android.feature` convention).
- **Must not depend on:** `:core:data` or other feature modules.
- **Status:** Arrives in Phase 7 (Task 7.4–7.6).
