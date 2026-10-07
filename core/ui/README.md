# `:core:ui`

Shared UI helpers that know about domain types:

- Formatters: `SizeTextFormatter` (`ByteSize` to display and spoken text), `DurationTextFormatter`, and `ErrorMessageMapper` (the one place a `DomainError` becomes a message).
- Media permissions: `MediaAccess`, `MediaPermissions` (per API level), and readers for the current grants.
- `WhileUiSubscribed`, the sharing policy for screen `StateFlow`s.

- **May depend on:** `:core:designsystem`, `:core:model`, `:core:domain`.
- **Must not depend on:** `:core:data`.
