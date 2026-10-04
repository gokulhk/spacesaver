package io.github.gokulhk.spacesaver.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii from the plan, Section 6.3. Cards use `large`; bottom sheets use `extraLarge`.
 * `extraSmall` keeps the Material default.
 */
internal val SpaceSaverShapes: Shapes =
    Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )
