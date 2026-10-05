package io.github.gokulhk.spacesaver.core.media.video

/** Forwards progress to [onProgress] only when it increases, clamped to 0..1, so it never goes backwards. */
internal class MonotonicProgress(
    private val onProgress: (Float) -> Unit,
) {
    private var last = 0f

    /** Reports [value] if it is higher than anything reported before. */
    fun report(value: Float) {
        val clamped = value.coerceIn(last, 1f)
        if (clamped > last) {
            last = clamped
            onProgress(clamped)
        }
    }
}
