package io.github.gokulhk.spacesaver.core.media.output

import io.github.gokulhk.spacesaver.core.model.MediaType

/**
 * Where converted files are written: one SpaceSaver album per media type, so outputs are easy to
 * find (and to exclude from backups) and never mix into the Camera folder.
 */
object OutputFolders {
    /** Folder for converted videos. */
    const val VIDEOS = "Movies/SpaceSaver/"

    /** Folder for converted images. */
    const val IMAGES = "Pictures/SpaceSaver/"

    /** The output folder for [type]. */
    fun of(type: MediaType): String =
        when (type) {
            MediaType.VIDEO -> VIDEOS
            MediaType.IMAGE -> IMAGES
        }
}
