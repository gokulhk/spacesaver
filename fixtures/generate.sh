#!/usr/bin/env bash
# Generates the synthetic, CC0 media fixtures used by :core:media instrumented tests.
# Requires ffmpeg (with libx264 and the native AAC encoder). Output is committed, so contributors
# only need to run this when changing fixtures. See fixtures/README.md.
set -euo pipefail

OUT="$(cd "$(dirname "$0")/.." && pwd)/core/media/src/androidTest/assets"
mkdir -p "$OUT"
FF=(ffmpeg -hide_banner -loglevel error -y)

# Fixed metadata so metadata-preservation tests have known values.
CREATION_TIME="2024-06-11T18:15:02.000000Z"
LOCATION="+37.4220-122.0841/"

video() { # name width height bitrate
  "${FF[@]}" \
    -f lavfi -i "testsrc2=size=${2}x${3}:rate=30:duration=2" \
    -f lavfi -i "sine=frequency=440:sample_rate=48000:duration=2" \
    -c:v libx264 -preset veryfast -b:v "$4" -maxrate "$4" -bufsize "$4" -pix_fmt yuv420p \
    -c:a aac -b:a 128k \
    -metadata creation_time="$CREATION_TIME" -metadata location="$LOCATION" \
    -movflags +faststart \
    "$OUT/$1"
}

video video_4k_h264.mp4 3840 2160 20M
video video_1080p_h264.mp4 1920 1080 8M

# Photo-like JPEG: smooth multi-color gradients (many distinct colors, like a photo).
"${FF[@]}" -f lavfi -i "gradients=size=1600x1200:seed=7:nb_colors=6" -frames:v 1 -q:v 3 "$OUT/photo.jpg"

# Photo-like PNG and a flat, screenshot-like PNG.
"${FF[@]}" -f lavfi -i "gradients=size=800x600:seed=3:nb_colors=6" -frames:v 1 "$OUT/photo.png"
# Screenshot-like PNG: flat UI blocks plus an embedded picture, like a chat or feed. (A screenshot of
# only flat rectangles is unrealistically small as PNG and would make lossless WebP look worse.)
"${FF[@]}" -f lavfi -i "color=c=white:size=1080x2400" -f lavfi -i "gradients=size=984x700:seed=11:nb_colors=5" \
  -filter_complex "[0][1]overlay=48:600,drawbox=x=0:y=0:w=1080:h=220:color=0x0F766E:t=fill,drawbox=x=48:y=320:w=984:h=180:color=0xE3ECEA:t=fill,drawbox=x=48:y=1400:w=700:h=160:color=0xE3ECEA:t=fill,drawbox=x=332:y=1620:w=700:h=160:color=0xCCFBF1:t=fill,drawbox=x=48:y=2160:w=984:h=140:color=0x0F766E:t=fill" \
  -frames:v 1 "$OUT/screenshot.png"

ls -l "$OUT"
