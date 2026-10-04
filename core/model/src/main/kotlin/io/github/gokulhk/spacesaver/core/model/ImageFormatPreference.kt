package io.github.gokulhk.spacesaver.core.model

/** The user's preferred format for converted photos (Settings, plan Section 7.7). */
enum class ImageFormatPreference {
    /** HEIC: smallest files. Used only when the device can encode it; otherwise WebP. */
    HEIC,

    /** WebP: slightly larger, but opens in more apps and browsers. */
    WEBP,
}
