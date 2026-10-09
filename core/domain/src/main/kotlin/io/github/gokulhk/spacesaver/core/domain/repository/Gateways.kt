package io.github.gokulhk.spacesaver.core.domain.repository

import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.estimate.CalibrationTable
import io.github.gokulhk.spacesaver.core.domain.estimate.ConversionPair
import io.github.gokulhk.spacesaver.core.domain.estimate.ProcessingSpeed
import io.github.gokulhk.spacesaver.core.domain.execution.BatchProgress
import io.github.gokulhk.spacesaver.core.domain.plan.PlanChoices
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

/** Whether the user approved a deletion in the system dialog. */
enum class DeletionOutcome {
    /** Approved; the files are gone. */
    DELETED,

    /** The user cancelled the dialog; nothing was deleted. */
    DECLINED,
}

/** Port: deleting files (plan Section 6.1). */
interface DeletionGateway {
    /**
     * Permanently deletes files the app didn't create. Shows the system consent dialog
     * (`MediaStore.createDeleteRequest`) and suspends until the user responds.
     */
    suspend fun requestUserDeletion(uris: List<String>): DeletionOutcome

    /** Deletes files SpaceSaver wrote (e.g. rejected outputs); no consent dialog is needed. */
    suspend fun deleteOwnFiles(uris: List<String>)

    /** Which of [uris] still exist, e.g. originals not deleted elsewhere since they were converted. */
    suspend fun existing(uris: List<String>): Set<String>
}

/** Port: what the device's encoders can do (plan Task 4.2). */
interface EncoderCapabilities {
    /** Whether a hardware HEVC video encoder exists. */
    suspend fun hasHardwareHevcEncoder(): Boolean

    /** Whether HEIC images can be encoded. */
    suspend fun supportsHeicEncoding(): Boolean
}

/** A measurement from a completed conversion, used to calibrate estimates (plan Section 5.4). */
sealed interface CalibrationSample {
    /**
     * Output/original size ratio.
     *
     * @property pair the conversion.
     * @property ratio output size divided by original size.
     */
    data class Ratio(
        val pair: ConversionPair,
        val ratio: Double,
    ) : CalibrationSample

    /**
     * Seconds of processing per second of video.
     *
     * @property factor the measured factor.
     */
    data class VideoSpeed(
        val factor: Double,
    ) : CalibrationSample

    /**
     * Seconds of processing for one image.
     *
     * @property seconds the measured time.
     */
    data class ImageSpeed(
        val seconds: Double,
    ) : CalibrationSample
}

/** Port: measured compression ratios and processing speed. */
interface CalibrationRepository {
    /** Calibrated ratios per conversion pair. */
    fun observeCalibration(): Flow<CalibrationTable>

    /** Calibrated processing speed. */
    fun observeProcessingSpeed(): Flow<ProcessingSpeed>

    /** Stores a measurement; old samples are pruned so calibration follows recent behavior. */
    suspend fun record(sample: CalibrationSample)
}

/**
 * A converted output after publishing.
 *
 * @property mediaId its MediaStore ID.
 * @property uri its content URI.
 * @property relativePath its folder.
 * @property displayName its file name.
 * @property size its size.
 * @property dateModified its modification time.
 * @property format its format.
 */
data class PublishedOutput(
    val mediaId: MediaId,
    val uri: String,
    val relativePath: String?,
    val displayName: String,
    val size: ByteSize,
    val dateModified: Instant,
    val format: MediaFormat,
)

/** Port: checks, publishes, and cleans up converter outputs (plan Section 5.8). */
interface OutputGateway {
    /**
     * Checks a pending output of [original] converted with [spec]: it decodes, has the expected
     * dimensions, and is smaller. Returns its size, or an `OutputVerificationFailed` error.
     */
    suspend fun verify(
        original: MediaItem,
        outputUri: String,
        spec: ConversionSpec,
    ): DomainResult<ByteSize>

    /** Makes a verified output visible to other apps. */
    suspend fun publish(outputUri: String): PublishedOutput

    /** Deletes an output SpaceSaver wrote. */
    suspend fun discard(outputUri: String)

    /** URIs of SpaceSaver's outputs still pending (not yet published), e.g. after the process died. */
    suspend fun pendingOutputs(): List<String>
}

/** Port: runs batches in the background (WorkManager, plan Task 5.3). */
interface BatchScheduler {
    /** Starts processing [batchId], optionally only while charging. */
    fun enqueue(
        batchId: BatchId,
        chargingOnly: Boolean,
    )

    /** Whether work for [batchId] is waiting or running, so nothing else should touch it. */
    suspend fun isScheduled(batchId: BatchId): Boolean

    /** Stops [batchId]'s work, if any. */
    suspend fun cancel(batchId: BatchId)

    /** Live progress of [batchId] while its work runs; null while waiting or once finished. */
    fun observeProgress(batchId: BatchId): Flow<BatchProgress?>
}

/** Port: the device's current time zone, read on each call so "today" follows travel. */
fun interface ZoneProvider {
    /** The current zone. */
    fun zone(): ZoneId
}

/**
 * Port: files the user added to the plan from Browse ("Convert"). They join the plan even when
 * their suggestion is switched off. Persisted, so they survive restarts.
 */
interface PlanAdditionsRepository {
    /** IDs of the added files, re-emitted on change. */
    val additions: Flow<Set<MediaId>>

    /** Adds [ids] to the plan. */
    suspend fun add(ids: Set<MediaId>)
}

/**
 * Port: the user's plan choices on Home and Plan detail, which suggestions are switched off and
 * which preset each uses. Kept for the app process only; a new session starts from the defaults.
 */
interface PlanChoicesRepository {
    /** Current choices, re-emitted on change. */
    val choices: Flow<PlanChoices>

    /** Replaces the choices with [transform] applied to the current ones. */
    suspend fun update(transform: (PlanChoices) -> PlanChoices)
}
