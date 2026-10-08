package io.github.gokulhk.spacesaver.core.domain.plan

import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.usecase.SuggestionGroup

/**
 * How the user shaped the plan.
 *
 * @property selections the preset picked per suggestion; others use their default.
 * @property excluded suggestions switched off.
 */
data class PlanChoices(
    val selections: Map<SuggestionGroup, ConversionOption> = emptyMap(),
    val excluded: Set<SuggestionGroup> = emptySet(),
)
