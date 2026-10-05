package io.github.gokulhk.spacesaver.core.media

import io.github.gokulhk.spacesaver.core.domain.conversion.ConverterRegistry
import io.github.gokulhk.spacesaver.core.domain.conversion.MediaConverter
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import javax.inject.Inject

/**
 * Finds converters among those contributed to the Hilt multibinding `Set<MediaConverter>`
 * (plan Section 4.4). Supporting a new format means adding a converter and binding it
 * `@IntoSet`; nothing here changes.
 */
class DefaultConverterRegistry
    @Inject
    constructor(
        private val converters: Set<@JvmSuppressWildcards MediaConverter>,
    ) : ConverterRegistry {
        override fun converterFor(
            source: MediaFormat,
            target: MediaFormat,
        ): MediaConverter? = converters.firstOrNull { it.supports(source, target) }

        override fun targetsFor(source: MediaFormat): Set<MediaFormat> =
            MediaFormat.entries.filterTo(mutableSetOf()) { target -> converters.any { it.supports(source, target) } }
    }
