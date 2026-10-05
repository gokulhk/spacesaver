# Spike: writing video output into MediaStore (Task 4.3)

**Question:** Media3 Transformer only accepts an output **path**. How do we write directly into a pending MediaStore entry, so a conversion doesn't need space for a second, temporary copy of the output?

## Tried

1. **`/proc/self/fd/N` from `openFileDescriptor(uri, "rw")`**: fails on API 36. SELinux denies `write` on `/proc/self/fd` (`avc: denied { write } for name="fd"`) because the muxer opens the path with create flags.
2. **The pending file's real path (`MediaColumns.DATA`)**: MediaProvider allows the owning app to open it ("Open with lower FS"), but Media3's muxer opens its output with exclusive create and fails with `EEXIST`, because MediaStore already created the file at insert time.
3. **A temp file in app cache, then copy**: works, but peak extra space is the output size *twice*, which breaks the planner's budget (cost = estimate × 1.2).

## Decision

**`StreamMp4MuxerFactory`**: a custom `Muxer.Factory` (`Transformer.Builder.setMuxerFactory`) that ignores the path and builds Media3's `Mp4Muxer` on `SeekableMuxerOutput.of(FileOutputStream(pfd.fileDescriptor))` for the pending entry. It drops metadata entries the MP4 muxer can't write, matching `InAppMp4Muxer`. Location and creation time are kept (see `metadata-preservation.md`).

Verified by `VideoConverterTest` (4K → 1080p, codec and audio tracks, progress, cancellation) and `MetadataPreservationTest` on the emulator.

## Notes

- Cancellation and failures must delete the pending entry from a `NonCancellable` context. Calling a suspending delete from an already-cancelled coroutine silently did nothing, and `VideoConverterTest.cancellationStopsWorkAndLeavesNoOutput` caught it.
- Images use a temp file in app cache: photos are small, and `ExifInterface` needs a plain seekable file to write EXIF.
