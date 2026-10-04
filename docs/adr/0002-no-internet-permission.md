# ADR-0002: No internet permission

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

SpaceSaver reads a user's entire photo and video library. Users need to trust that their media never leaves the device. A promise in a privacy policy is weaker than a guarantee the operating system enforces.

## Decision

The app never declares `android.permission.INTERNET` and never includes a networking library. It also never requests `MANAGE_EXTERNAL_STORAGE`. Thumbnails, conversion, and estimation all run on-device.

A test (plan Task 8.2) parses the merged release manifest and fails the build if either permission appears, including through a transitive dependency.

## Consequences

- Without the permission, Android blocks network access for the app, so "offline" is provable.
- No crash reporting, analytics, or remote config. Bug reports rely on users sharing details manually.
- Every new dependency must be checked for manifest permissions it merges in.
- Distribution through F-Droid is straightforward (no anti-features).
