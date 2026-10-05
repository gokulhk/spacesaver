# `:core:media`

The media engine: converters, the converter registry, encoder capability detection, metadata preservation, and writing and verifying outputs in MediaStore.

## Entry points

- `DefaultConverterRegistry`: finds a `MediaConverter` for a (source, target) pair. Converters are contributed with `@Binds @IntoSet` in `MediaModule`.
- Converters: `Media3VideoConverter` (H.264/HEVC MP4), `WebpImageConverter` (lossy/lossless WebP), `HeicImageConverter` (HEIC). Each returns a **pending** MediaStore output.
- `AndroidEncoderCapabilities` + `EncoderCapabilityRules`: hardware HEVC and HEIC detection.
- `MediaStoreOutputWriter`: pending outputs, publish, discard. `OutputNaming`: file names.
- `AndroidOutputProbe` + `OutputVerification`: decode and check outputs before publishing.
- `ExifMetadataCopier`: EXIF carried to image outputs.

## Adding a format

1. Implement `MediaConverter` for the new (source, target) pairs.
2. Bind it `@IntoSet` in `MediaModule`.
3. Add instrumented tests with a fixture (see `fixtures/`).

The registry, planner, and UI pick it up through `ConverterRegistry.targetsFor(source)`.

## Testing

Pure rules (registry, capabilities, naming, verification) have JVM tests. Everything touching codecs and MediaStore is tested on a device or emulator: `./gradlew :core:media:connectedDebugAndroidTest`. Spike findings live in `docs/spikes/`.

## Dependencies

- **May depend on:** `:core:domain`, `:core:model`, Media3, HeifWriter, ExifInterface.
- **Must not depend on:** `:core:data` or UI code.
