package io.github.gokulhk.spacesaver.core.domain.savings

import io.github.gokulhk.spacesaver.core.domain.batch.ReviewAction
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.sumOfSize
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Creates ledger events and sums them (plan Section 5.2). Time comes from the injected [clock];
 * the zone is passed per call so "today" follows the user when they travel.
 */
class SavingsCalculator
    @Inject
    constructor(
        private val clock: Clock,
    ) {
        /**
         * The event for deleting an original after conversion: saves `original − output`. An output
         * that isn't smaller is rejected, so the ledger can never record negative savings.
         */
        fun conversionEvent(
            mediaId: MediaId,
            original: ByteSize,
            output: ByteSize,
        ): DomainResult<SavingsEvent> =
            if (output < original) {
                DomainResult.Success(SavingsEvent(SavingsType.CONVERSION, original - output, clock.instant(), mediaId))
            } else {
                DomainResult.Failure(
                    DomainError.OutputVerificationFailed("Output $output is not smaller than $original"),
                )
            }

        /** The event for deleting a file from Browse: saves its full [size]. */
        fun deletionEvent(
            mediaId: MediaId,
            size: ByteSize,
        ): SavingsEvent = SavingsEvent(SavingsType.DELETION, size, clock.instant(), mediaId)

        /**
         * Events for resolving a review with [action]. Only deleting originals frees space; keeping
         * both or stopping records nothing.
         */
        fun reviewEvents(
            action: ReviewAction,
            accepted: List<ConvertedSizes>,
        ): DomainResult<List<SavingsEvent>> {
            if (action != ReviewAction.DELETE_ORIGINALS_AND_CONTINUE) return DomainResult.Success(emptyList())
            val events = mutableListOf<SavingsEvent>()
            for (file in accepted) {
                when (val event = conversionEvent(file.mediaId, file.original, file.output)) {
                    is DomainResult.Success -> events += event.value
                    is DomainResult.Failure -> return event
                }
            }
            return DomainResult.Success(events)
        }

        /** The instant the current local day started in [zone], correct on DST-change days. */
        fun startOfToday(zone: ZoneId): Instant = LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toInstant()

        /** Lifetime and today totals of [events]. */
        fun summarize(
            events: List<SavingsEvent>,
            zone: ZoneId,
        ): SavingsSummary {
            val startOfToday = startOfToday(zone)
            return SavingsSummary(
                lifetime = events.sumOfSize { it.bytesSaved },
                today = events.filter { it.timestamp >= startOfToday }.sumOfSize { it.bytesSaved },
            )
        }
    }
