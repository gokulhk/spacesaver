# `:core:database`

Room database: savings ledger, batches and items, converted files, calibration samples.

## Entry points

- `SpaceSaverDatabase` (version in `SpaceSaverDatabase.VERSION`) and `Migrations.ALL`.
- DAOs: `SavingsDao`, `BatchDao`, `ConvertedFileDao`, `CalibrationDao`.
- `DatabaseModule` (Hilt).

## Changing the schema

1. Bump `SpaceSaverDatabase.VERSION`.
2. Add a `Migration` to `Migrations.ALL`. Destructive migration is never allowed: the ledger matters to users.
3. Build to export `schemas/.../<version>.json` and commit it.
4. Add a test in `MigrationTest` that creates the old version, inserts rows, and calls `runMigrationsAndValidate`.

## Dependencies

- **May depend on:** `:core:model`, Room.
- **Must not depend on:** `:core:domain` or UI code. Rows use primitives and enum names; `:core:data` maps them.
