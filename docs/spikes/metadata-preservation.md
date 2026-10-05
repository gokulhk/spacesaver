# Spike: metadata preservation (Task 4.5)

**Question:** Can converted files keep the original's capture date, location, orientation, and camera details, so they keep their place in gallery timelines?

Evidence: `MetadataPreservationTest` and `ImageConverterTest` (instrumented), run on the `SpaceSaver_Test_API_36` emulator (API 36.1). **Physical devices: not yet verified.**

## What works

| Format | Date taken + offset | GPS | Orientation | Make / model | `Software = SpaceSaver` | MediaStore `DATE_TAKEN` = original |
|---|---|---|---|---|---|---|
| JPEG → WebP | ✅ EXIF | ✅ EXIF | ✅ EXIF tag | ✅ | ✅ | ✅ |
| JPEG → HEIC | ✅ EXIF block | ✅ | ✅ HEIF rotation + EXIF tag | ✅ | ✅ | not separately tested (same EXIF path as WebP) |
| Video (H.264 / HEVC MP4) | ✅ `creation_time` | ✅ `©xyz` location | ✅ kept by Media3 | n/a | n/a (tracked in Room instead) | ✅ |

## How

- **Images:** `ExifMetadataCopier` reads the tags in `PRESERVED_TAGS` from the original.
  - **WebP:** they are written with `ExifInterface` onto the encoded temp file, before it's copied into MediaStore.
  - **HEIC:** `ExifInterface` writes them into a 1×1 JPEG, and its APP1 segment (`Exif\0\0` + TIFF data) is passed to `HeifWriter.addExifData`. This avoids hand-writing TIFF bytes.
- **Orientation:** pixels are decoded unrotated (`BitmapFactory`) and orientation stays as metadata, so EXIF orientation still describes the pixels correctly. For HEIC it is also set as HEIF rotation (`HeifWriter.Builder.setRotation`), which HEIF viewers honor.
- **Videos:** Media3 Transformer forwards the source's creation time and location metadata to the muxer. `StreamMp4MuxerFactory` keeps every entry the MP4 muxer supports, as Media3's `InAppMp4Muxer` does.
- **`DATE_TAKEN`:** apps can't set it directly on API 30+. MediaStore derives it when the pending output is published (`IS_PENDING = 0`), from EXIF `DateTimeOriginal` + `OffsetTimeOriginal` for images and from MP4 `creation_time` for videos. Carrying the metadata is therefore enough.

## What doesn't (yet)

- **File modification time (`DATE_MODIFIED`)** can't be set by apps through MediaStore, so outputs get the conversion time. Galleries sort by `DATE_TAKEN` (falling back to modification time only when there's no capture date), so items with metadata keep their place. Originals without any capture metadata will move to "today" in galleries that fall back to modification time. To revisit if users notice.
- **HEIC `DATE_TAKEN`** after publishing isn't tested separately on the emulator. The emulator has no hardware HEVC, so the app wouldn't choose HEIC there. Add it on a physical device.
- **Physical device verification** is outstanding, especially Samsung and Pixel galleries' handling of WebP and HEIC EXIF.
