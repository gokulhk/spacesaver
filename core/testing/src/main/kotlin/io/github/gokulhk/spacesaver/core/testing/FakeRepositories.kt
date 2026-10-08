package io.github.gokulhk.spacesaver.core.testing

import androidx.paging.PagingData
import io.github.gokulhk.spacesaver.core.domain.estimate.CalibrationTable
import io.github.gokulhk.spacesaver.core.domain.estimate.ProcessingSpeed
import io.github.gokulhk.spacesaver.core.domain.plan.PlanChoices
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationRepository
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationSample
import io.github.gokulhk.spacesaver.core.domain.repository.MediaRepository
import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.domain.repository.PlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.PlanChoicesRepository
import io.github.gokulhk.spacesaver.core.domain.repository.SavingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.SettingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.StorageRepository
import io.github.gokulhk.spacesaver.core.domain.repository.StorageStats
import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsEvent
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import io.github.gokulhk.spacesaver.core.model.sumOfSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/** In-memory media library. */
class FakeMediaRepository(
    items: List<MediaItem> = emptyList(),
) : MediaRepository {
    private val library = MutableStateFlow(items)

    /** Every (type, sort) pair requested from [pagedMedia]. */
    val pagedRequests = mutableListOf<Pair<MediaType, MediaSort>>()

    /** Replaces the library. */
    fun setItems(items: List<MediaItem>) {
        library.value = items
    }

    override fun observeMedia(type: MediaType): Flow<List<MediaItem>> =
        library.map { all ->
            all.filter {
                it.type ==
                    type
            }
        }

    override fun pagedMedia(
        type: MediaType,
        sort: MediaSort,
    ): Flow<PagingData<MediaItem>> {
        pagedRequests += type to sort
        return flowOf(PagingData.from(library.value.filter { it.type == type }))
    }
}

/** In-memory storage figures. */
class FakeStorageRepository(
    stats: StorageStats = DEFAULT_STATS,
) : StorageRepository {
    private val state = MutableStateFlow(stats)

    /** How many times [refresh] was called. */
    var refreshCount = 0
        private set

    /** Replaces the figures. */
    fun setStats(stats: StorageStats) {
        state.value = stats
    }

    /** Changes only the free space, e.g. while a conversion fills the disk. */
    fun setFree(free: ByteSize) {
        state.update { it.copy(free = free) }
    }

    /** How many times [freeSpace] was polled. */
    var freeSpaceQueries = 0
        private set

    override suspend fun freeSpace(): ByteSize = state.value.free.also { freeSpaceQueries++ }

    override fun observeStorage(): Flow<StorageStats> = state

    override suspend fun currentStorage(): StorageStats = state.value

    override suspend fun refresh() {
        refreshCount++
    }

    /** Defaults. */
    companion object {
        /** A 128 GB phone with 40 GB free. */
        val DEFAULT_STATS =
            StorageStats(
                total = ByteSize.gigabytes(128),
                free = ByteSize.gigabytes(40),
                videos = ByteSize.gigabytes(42),
                images = ByteSize.gigabytes(12),
            )
    }
}

/** In-memory savings ledger. */
class FakeSavingsRepository : SavingsRepository {
    private val ledger = MutableStateFlow<List<SavingsEvent>>(emptyList())

    /** Every recorded event, in order. */
    val events: List<SavingsEvent> get() = ledger.value

    override fun observeTotals(todayStart: Instant): Flow<SavingsSummary> =
        ledger.map { events ->
            SavingsSummary(
                lifetime = events.sumOfSize { it.bytesSaved },
                today = events.filter { it.timestamp >= todayStart }.sumOfSize { it.bytesSaved },
            )
        }

    override suspend fun record(events: List<SavingsEvent>) {
        ledger.update { it + events }
    }
}

/** In-memory settings. */
class FakeSettingsRepository(
    initial: UserSettings = UserSettings.DEFAULT,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<UserSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

    override suspend fun setImageFormat(format: ImageFormatPreference) = state.update { it.copy(imageFormat = format) }

    override suspend fun setReserveOverride(reserve: ByteSize?) = state.update { it.copy(reserveOverride = reserve) }

    override suspend fun setChargingOnly(enabled: Boolean) = state.update { it.copy(chargingOnly = enabled) }

    override suspend fun markMediaAccessRequested() = state.update { it.copy(mediaAccessRequested = true) }
}

/** In-memory calibration data. */
class FakeCalibrationRepository(
    calibration: CalibrationTable = CalibrationTable.EMPTY,
    speed: ProcessingSpeed = ProcessingSpeed.DEFAULT,
) : CalibrationRepository {
    /** Current calibration; tests may replace it. */
    val calibration = MutableStateFlow(calibration)

    /** Current processing speed; tests may replace it. */
    val speed = MutableStateFlow(speed)

    override fun observeCalibration(): Flow<CalibrationTable> = calibration

    override fun observeProcessingSpeed(): Flow<ProcessingSpeed> = speed

    /** Every recorded sample, in order. */
    val samples = mutableListOf<CalibrationSample>()

    override suspend fun record(sample: CalibrationSample) {
        samples += sample
    }
}

/** In-memory plan additions. */
class FakePlanAdditionsRepository : PlanAdditionsRepository {
    private val state = MutableStateFlow(emptySet<MediaId>())

    override val additions: Flow<Set<MediaId>> = state

    override suspend fun add(ids: Set<MediaId>) = state.update { it + ids }
}

/** In-memory plan choices. */
class FakePlanChoicesRepository : PlanChoicesRepository {
    private val state = MutableStateFlow(PlanChoices())

    override val choices: Flow<PlanChoices> = state

    override suspend fun update(transform: (PlanChoices) -> PlanChoices) = state.update(transform)
}
