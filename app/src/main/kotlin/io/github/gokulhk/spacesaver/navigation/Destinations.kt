package io.github.gokulhk.spacesaver.navigation

import kotlinx.serialization.Serializable

/** Media access explanation and permission request; shown until media can be read. */
@Serializable
data object OnboardingDestination

/** The start destination and first bottom-bar tab. */
@Serializable
data object HomeDestination

/** Browse tab. */
@Serializable
data object BrowseDestination

/** Settings tab. */
@Serializable
data object SettingsDestination

/** The whole plan, opened from Home. */
@Serializable
data object PlanDetailDestination

/**
 * A batch's progress, opened from Home, a review, or the batch notification.
 *
 * @property batchId the batch.
 */
@Serializable
data class BatchProgressDestination(
    val batchId: Long,
)

/**
 * A batch's review.
 *
 * @property batchId the batch.
 */
@Serializable
data class ReviewDestination(
    val batchId: Long,
)

/** Open-source licenses, opened from Settings. */
@Serializable
data object LicensesDestination
