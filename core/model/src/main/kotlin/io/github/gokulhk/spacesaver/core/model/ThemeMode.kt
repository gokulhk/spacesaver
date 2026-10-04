package io.github.gokulhk.spacesaver.core.model

/** The user's theme preference from Settings (plan Section 6.1). Defaults to [SYSTEM]. */
enum class ThemeMode {
    /** Follow the system's light or dark setting. */
    SYSTEM,

    /** Always use the light theme. */
    LIGHT,

    /** Always use the dark theme. */
    DARK,
}
