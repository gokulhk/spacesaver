# `:app`

Application entry point: `SpaceSaverApplication` (Hilt), `MainActivity`, the navigation host, and wiring of domain ports to data implementations.

- **May depend on:** Every feature module, `:core:data`, `:core:work`, `:core:designsystem`, `:core:ui`, `:core:domain`.
- **Must not depend on:** Business logic. Anything beyond wiring belongs in a core or feature module.
- **Status:** Phase 0: placeholder screen. Navigation arrives in Phase 7 (Task 7.8).
