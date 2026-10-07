package io.github.gokulhk.spacesaver.feature.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.gokulhk.spacesaver.core.designsystem.component.MediaCategory
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.usecase.Suggestion
import io.github.gokulhk.spacesaver.core.domain.usecase.SuggestionGroup
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import io.github.gokulhk.spacesaver.core.ui.label

/** "23 videos in 4K". */
@Composable
internal fun suggestionTitle(suggestion: Suggestion): String {
    val count = suggestion.candidates.size
    val plural =
        when (suggestion.group) {
            SuggestionGroup.VIDEOS_4K -> R.plurals.home_suggestion_videos_4k
            SuggestionGroup.VIDEOS_FULL_HD -> R.plurals.home_suggestion_videos_full_hd
            SuggestionGroup.JPEG_PHOTOS -> R.plurals.home_suggestion_jpeg_photos
            SuggestionGroup.PNG_PHOTOS -> R.plurals.home_suggestion_png_photos
            SuggestionGroup.SCREENSHOTS -> R.plurals.home_suggestion_screenshots
        }
    return pluralStringResource(plural, count, count)
}

/** The card's icon and accent. */
internal val SuggestionGroup.category: MediaCategory
    get() =
        when (this) {
            SuggestionGroup.VIDEOS_4K, SuggestionGroup.VIDEOS_FULL_HD -> MediaCategory.VIDEO
            SuggestionGroup.JPEG_PHOTOS, SuggestionGroup.PNG_PHOTOS, SuggestionGroup.SCREENSHOTS -> MediaCategory.IMAGE
        }

/** "4K to Full HD", "HEIC". */
@Composable
internal fun presetLabel(option: ConversionOption): String =
    when (option) {
        is ConversionOption.Video -> stringResource(option.preset.labelRes)
        is ConversionOption.Image -> stringResource(option.target.presetLabelRes ?: option.target.label)
    }

/** A plain-language quality note for the preset sheet. */
@Composable
internal fun presetNote(option: ConversionOption): String =
    stringResource(
        when (option) {
            is ConversionOption.Video -> option.preset.noteRes
            is ConversionOption.Image -> option.target.presetNoteRes ?: R.string.preset_other_note
        },
    )

@get:StringRes
private val VideoPreset.labelRes: Int
    get() =
        when (this) {
            VideoPreset.UHD_TO_FHD -> R.string.preset_uhd_to_fhd
            VideoPreset.UHD_TO_HD -> R.string.preset_uhd_to_hd
            VideoPreset.FHD_TO_HD -> R.string.preset_fhd_to_hd
        }

@get:StringRes
private val VideoPreset.noteRes: Int
    get() =
        when (this) {
            VideoPreset.UHD_TO_FHD -> R.string.preset_uhd_to_fhd_note
            VideoPreset.UHD_TO_HD -> R.string.preset_uhd_to_hd_note
            VideoPreset.FHD_TO_HD -> R.string.preset_fhd_to_hd_note
        }

private val MediaFormat.presetLabelRes: Int?
    get() =
        when (this) {
            MediaFormat.HEIC -> R.string.preset_heic
            MediaFormat.WEBP_LOSSY -> R.string.preset_webp
            MediaFormat.WEBP_LOSSLESS -> R.string.preset_webp_lossless
            else -> null
        }

private val MediaFormat.presetNoteRes: Int?
    get() =
        when (this) {
            MediaFormat.HEIC -> R.string.preset_heic_note
            MediaFormat.WEBP_LOSSY -> R.string.preset_webp_note
            MediaFormat.WEBP_LOSSLESS -> R.string.preset_webp_lossless_note
            else -> null
        }
