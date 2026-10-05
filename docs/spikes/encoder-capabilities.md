# Spike: encoder capabilities (Task 4.2)

**Question:** How do we reliably decide (a) whether to encode video as HEVC or H.264, and (b) whether to offer HEIC for photos?

## Approach

`AndroidEncoderCapabilities` reads `MediaCodecList(REGULAR_CODECS)` once, maps each `MediaCodecInfo` to a plain `CodecDescription`, and applies `EncoderCapabilityRules` (pure, unit-tested):

- **Hardware HEVC encoder**: an encoder for `video/hevc` that is `isHardwareAccelerated`, not `isSoftwareOnly`, and not an `isAlias` (aliases are old names such as `OMX.*` for the same codec).
- **HEIC encoding**: a dedicated `image/vnd.android.heic` encoder, **or** a hardware HEVC encoder (`HeifWriter` tiles through HEVC when no HEIC encoder exists). Software HEVC alone does not count: encoding a 12 MP photo would take seconds.

These APIs (`isHardwareAccelerated`, `isSoftwareOnly`, `isAlias`) exist from API 29, below our minSdk 30.

## Findings

### Emulator: `SpaceSaver_Test_API_36` (API 36.1, arm64, Google Play image)

Evidence: `EncoderCapabilitiesSpikeTest` logs (tag `EncoderSpike`).

| Encoder | Types | Hardware | Software-only | Alias |
|---|---|---|---|---|
| `c2.android.avc.encoder` | video/avc | no | yes | no |
| `c2.android.hevc.encoder` | video/hevc | no | yes | no |
| `c2.android.av1.encoder`, `vp8`, `vp9`, `apv`, `h263`, `mpeg4` | video | no | yes | no |
| `c2.android.aac.encoder` (+ amr, flac, opus) | audio | no | yes | no |
| `OMX.google.*` | same as above | no | yes | **yes** |

Result: `hasHardwareHevcEncoder = false`, `supportsHeicEncoding = false` (no `image/vnd.android.heic` encoder). On the emulator SpaceSaver therefore converts video to **H.264** and photos to **WebP**.

### Physical devices

**Not verified.** The maintainer chose the emulator for this phase. Expected on current phones (to confirm when a device is available, by running `EncoderCapabilitiesSpikeTest` and reading the `EncoderSpike` log):

- Qualcomm (`c2.qti.hevc.encoder`), Exynos (`c2.exynos.hevc.encoder`), MediaTek (`c2.mtk.hevc.encoder`), and Pixel/Tensor (`c2.google.hevc.encoder` or `c2.exynos.*`) report hardware HEVC encoders.
- Most devices since Android 10 expose `c2.*.heic.encoder` or support `HeifWriter` through hardware HEVC.

## Decision

Use the rules above. They fail safe: if detection is wrong in the "no" direction, the app produces slightly larger H.264/WebP files; it never chooses a codec the device can't run quickly.

## Follow-ups

- Run the spike test on at least one physical phone and add its table here (plan Task 4.2 asks for one).
- `HeicImageConverter` is exercised on the emulator through software HEVC by `ImageConverterTest` (see results there), even though the app won't offer HEIC on that device.
