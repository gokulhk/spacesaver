package io.github.gokulhk.spacesaver.core.domain.execution

import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionInput
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionResult
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpecResolver
import io.github.gokulhk.spacesaver.core.domain.conversion.ConverterRegistry
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.repository.OutputGateway
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.model.ByteSize
import java.time.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/** How one item's conversion ended. */
sealed interface ItemOutcome {
    /**
     * Converted, verified, and published.
     *
     * @property outputUri the output.
     * @property outputSize its verified size.
     */
    data class Converted(
        val outputUri: String,
        val outputSize: ByteSize,
    ) : ItemOutcome

    /**
     * Failed; any output was discarded.
     *
     * @property error why.
     */
    data class Failed(
        val error: DomainError,
    ) : ItemOutcome

    /** Free space was too low; nothing was kept. */
    data object SkippedNoSpace : ItemOutcome
}

/**
 * The pipeline for one item (plan Section 5.8): resolve the spec for this device, find a
 * converter, convert under a [SpaceGuard], verify, publish, and record. Any output that fails
 * verification is deleted, so nothing broken ever becomes visible.
 */
class ConvertBatchItem
    @Inject
    constructor(
        private val registry: ConverterRegistry,
        private val specResolver: ConversionSpecResolver,
        private val outputs: OutputGateway,
        private val spaceGuard: SpaceGuard,
        private val recorder: ConversionRecorder,
        private val clock: Clock,
    ) {
        /** Converts [item], reporting its progress from 0 to 1. */
        suspend operator fun invoke(
            item: BatchItem,
            onProgress: (Float) -> Unit,
        ): ItemOutcome {
            val spec = specResolver.resolve(item.original, item.option)
            val converter =
                registry.converterFor(item.original.format, spec.targetFormat)
                    ?: return ItemOutcome.Failed(DomainError.EncoderUnavailable(spec.targetFormat))
            val startedAt = clock.millis()
            val guarded =
                spaceGuard.run(item.estimatedOutput) {
                    converter.convert(ConversionInput(item.original), spec, onProgress)
                }
            return when (val result = (guarded as? Guarded.Completed)?.value) {
                null -> ItemOutcome.SkippedNoSpace
                is ConversionResult.Success -> publish(item, spec, result.outputUri, elapsedSince = startedAt)
                is ConversionResult.Failure -> ItemOutcome.Failed(result.error)
                ConversionResult.Cancelled -> ItemOutcome.Failed(DomainError.Cancelled)
            }
        }

        private suspend fun publish(
            item: BatchItem,
            spec: ConversionSpec,
            outputUri: String,
            elapsedSince: Long,
        ): ItemOutcome {
            val elapsed = (clock.millis() - elapsedSince).milliseconds
            return when (val verified = outputs.verify(item.original, outputUri, spec)) {
                is DomainResult.Failure -> {
                    outputs.discard(outputUri)
                    ItemOutcome.Failed(verified.error)
                }

                is DomainResult.Success -> {
                    val published = outputs.publish(outputUri)
                    recorder.record(item, spec, published.copy(size = verified.value), elapsed)
                    ItemOutcome.Converted(outputUri, verified.value)
                }
            }
        }
    }
