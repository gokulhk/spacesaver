# `:core:datastore`

User settings with DataStore Preferences.

## Entry points

- `SettingsDataSource`: `settings: Flow<StoredSettings>` and setters.
- `SettingsDataStoreFactory`: creates the store (corrupt file → defaults). Used by `DataStoreModule` and tests.

Unknown or out-of-range stored values fall back to that field's default, so a downgrade never crashes.

## Dependencies

- **May depend on:** `:core:model`, DataStore.
- **Must not depend on:** `:core:domain` or UI code.
