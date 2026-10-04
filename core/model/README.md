# `:core:model`

Pure Kotlin types shared by every layer.

## Contents

- `ByteSize`: non-negative bytes, SI units (ADR-0004), `format(locale)`.
- `Bitrate`, `Resolution` (short/long edge, even-dimension scaling).
- `MediaItem` with `VideoDetails` and `AudioTrack`; `MediaId`; `MediaType`; `ImageContent`.
- `MediaFormat` with `fromMimeType(mime, videoCodec)`. Video formats name the codec, not the container.
- `VideoPreset` and `VideoCodec` (plan Section 5.3 table).
- `ThemeMode`, `ImageFormatPreference` (persisted settings vocabulary).

## Dependencies

- **May depend on:** nothing (Kotlin and `java.*` only).
- **Must not depend on:** Android framework classes or other modules.
