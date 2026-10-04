package io.github.gokulhk.spacesaver.core.domain.repository

import io.github.gokulhk.spacesaver.core.domain.estimate.CalibrationTable
import io.github.gokulhk.spacesaver.core.domain.estimate.ProcessingSpeed
import kotlinx.coroutines.flow.Flow
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
}

/** Port: what the device's encoders can do (plan Task 4.2). */
interface EncoderCapabilities {
    /** Whether a hardware HEVC video encoder exists. */
    suspend fun hasHardwareHevcEncoder(): Boolean

    /** Whether HEIC images can be encoded. */
    suspend fun supportsHeicEncoding(): Boolean
}

/** Port: measured compression ratios and processing speed. */
interface CalibrationRepository {
    /** Calibrated ratios per conversion pair. */
    fun observeCalibration(): Flow<CalibrationTable>

    /** Calibrated processing speed. */
    fun observeProcessingSpeed(): Flow<ProcessingSpeed>
}

/** Port: runs batches in the background (WorkManager, plan Task 5.3). */
interface BatchScheduler {
    /** Starts processing [batchId], optionally only while charging. */
    fun enqueue(
        batchId: BatchId,
        chargingOnly: Boolean,
    )
}

/** Port: the device's current time zone, read on each call so "today" follows travel. */
fun interface ZoneProvider {
    /** The current zone. */
    fun zone(): ZoneId
}
