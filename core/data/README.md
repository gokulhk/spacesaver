# `:core:data`

Implements the domain ports (adapters) on top of Room, DataStore, MediaStore, and storage statistics.

## Entry points

- Repositories: `MediaRepositoryImpl`, `StorageRepositoryImpl`, `SavingsRepositoryImpl`, `BatchRepositoryImpl`, `SettingsRepositoryImpl`, `CalibrationRepositoryImpl`, bound in `DataModule`.
- `MediaStoreScanner` and `MediaStorePagingSource`: the media library.
- `StorageStatsSource`: total and free bytes.
- Deletion: `AndroidDeletionGateway` + `DeletionRequests`. The gateway suspends until the app's root UI (`DeletionRequestHost` in `:app`) shows the system delete dialog and reports the answer.

## Testing

- `FakeMediaStoreProvider` (test sources) stands in for MediaStore: a real `ContentProvider` over in-memory SQLite that honors Bundle query arguments, so sorting and paging run through SQL.
- Room tests use in-memory databases; settings tests use a DataStore on a temporary file.

## Dependencies

- **May depend on:** `:core:domain`, `:core:model`, `:core:database`, `:core:datastore`, `:core:media`.
- **Must not depend on:** feature modules or UI code.
