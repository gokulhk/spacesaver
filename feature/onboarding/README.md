# `:feature:onboarding`

Explains what the app does and requests media permissions, including the limited-access state on Android 14+. ViewModels expose `StateFlow<UiState>` and accept `UiEvent`s.

- **May depend on:** `:core:domain`, `:core:model`, `:core:designsystem`, `:core:ui` (set by the `spacesaver.android.feature` convention).
- **Must not depend on:** `:core:data` or other feature modules.
- **Status:** Arrives in Phase 7 (Task 7.1).
