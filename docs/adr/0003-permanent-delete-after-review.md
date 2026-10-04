# ADR-0003: Permanently delete originals after review instead of using the trash

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

Android 11+ offers `MediaStore.createTrashRequest`, which moves files to a trash that is emptied after 30 days. It looks safer, but trashed files **still occupy storage** until they expire. SpaceSaver plans batches by free space: if originals went to the trash, the next batch would have no room, which defeats the app's purpose.

## Decision

After a batch finishes, the user reviews every converted item (before/after comparison, per-item accept or reject). Only after that, and only through the system consent dialog from `MediaStore.createDeleteRequest`, are accepted originals deleted permanently.

Safety comes from the review step and the conversion safeguards instead of the trash:

- Outputs are verified (they decode, have the expected dimensions, and are smaller) before review.
- Metadata (date taken, location, orientation) is preserved so outputs keep their place in the gallery.
- If the user cancels the system dialog, nothing is deleted and the batch stays awaiting review.
- "Keep both" and "Stop here" are always available.

## Consequences

- Freed space is available immediately, so batches can chain.
- A mistake after confirmation cannot be undone from the trash. The UI must make "permanently" explicit in the confirm action.
- The savings ledger records space only when it is actually freed.
