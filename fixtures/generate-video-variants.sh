#!/usr/bin/env bash
# Generates small video fixtures shaped like real phone recordings, for :core:media instrumented
# tests of orientation and audio. Separate from generate.sh so the original fixtures stay
# byte-for-byte unchanged. Requires ffmpeg (libx264, libx265, libopus, ac3). Output is committed.
set -euo pipefail

OUT="$(cd "$(dirname "$0")/.." && pwd)/core/media/src/androidTest/assets"
mkdir -p "$OUT"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
ff() { ffmpeg -hide_banner -loglevel error -y "$@"; }

SRC="testsrc2=size=1920x1080:rate=30:duration=2"
TONE="sine=frequency=440:sample_rate=48000:duration=2"
VIDEO=(-c:v libx264 -preset veryfast -b:v 1500k -maxrate 1500k -bufsize 1500k -pix_fmt yuv420p)

# Stereo AAC, the usual phone soundtrack.
ff -f lavfi -i "$SRC" -f lavfi -i "$TONE" -ac 2 "${VIDEO[@]}" -c:a aac -b:a 128k "$TMP/landscape_stereo.mp4"

# Portrait as phones store it: landscape frames plus a rotation in the container (display matrix).
ff -display_rotation:v:0 90 -i "$TMP/landscape_stereo.mp4" -c copy "$OUT/video_portrait_rot90_stereo.mp4"
ff -display_rotation:v:0 270 -i "$TMP/landscape_stereo.mp4" -c copy "$OUT/video_portrait_rot270_stereo.mp4"

# Portrait with no rotation flag: the frames themselves are 1080x1920.
ff -f lavfi -i "testsrc2=size=1080x1920:rate=30:duration=2" -f lavfi -i "$TONE" -ac 2 "${VIDEO[@]}" \
  -c:a aac -b:a 128k "$OUT/video_portrait_native_stereo.mp4"

# Other soundtracks on landscape video.
ff -f lavfi -i "$SRC" -f lavfi -i "$TONE" -ac 6 "${VIDEO[@]}" -c:a aac -b:a 384k "$OUT/video_51_aac.mp4"
ff -f lavfi -i "$SRC" -f lavfi -i "$TONE" -ac 2 "${VIDEO[@]}" -c:a ac3 -b:a 192k "$OUT/video_ac3.mp4"
ff -f lavfi -i "$SRC" -f lavfi -i "$TONE" -ac 2 "${VIDEO[@]}" -c:a libopus -b:a 128k -strict -2 "$OUT/video_opus.mp4"

# HEVC with stereo AAC: what most recent phones record.
ff -f lavfi -i "$SRC" -f lavfi -i "$TONE" -ac 2 -c:v libx265 -preset veryfast -b:v 1500k -pix_fmt yuv420p \
  -tag:v hvc1 -c:a aac -b:a 128k "$OUT/video_hevc_stereo.mp4"

# Two audio tracks (stereo plus a second mono one), as some multi-microphone recordings have.
ff -f lavfi -i "$SRC" -f lavfi -i "$TONE" -f lavfi -i "sine=frequency=880:sample_rate=48000:duration=2" \
  -map 0:v -map 1:a -map 2:a "${VIDEO[@]}" -c:a aac -b:a 128k -ac:a:0 2 -ac:a:1 1 "$OUT/video_two_audio_tracks.mp4"

# No audio track at all, like many screen recordings.
ff -f lavfi -i "$SRC" "${VIDEO[@]}" -an "$OUT/video_no_audio.mp4"

ls -l "$OUT"/video_*.mp4
