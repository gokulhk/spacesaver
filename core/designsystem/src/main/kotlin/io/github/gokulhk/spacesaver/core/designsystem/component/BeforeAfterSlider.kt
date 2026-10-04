package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gokulhk.spacesaver.core.designsystem.R
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewComponents
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import kotlin.math.roundToInt

private val DividerWidth = 2.dp
private val HandleSize = 48.dp
private const val PERCENT = 100

/**
 * Compares an original and its converted output (plan Section 7.6). The [before] content shows
 * to the left of the split and [after] to the right. Drag anywhere to move the split.
 * Accessibility services can adjust it as a progress value.
 *
 * Stateless: the caller owns [fraction] and updates it from [onFractionChange].
 *
 * @param fraction share of the width showing [before], from 0 to 1.
 * @param onFractionChange called with the new fraction, already clamped to 0..1.
 * @param before the original, e.g. its image at the same zoom as [after].
 * @param after the converted output.
 */
@Composable
fun BeforeAfterSlider(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    before: @Composable () -> Unit,
    after: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnFractionChange by rememberUpdatedState(onFractionChange)
    val description = stringResource(R.string.before_after_description)
    val state = stringResource(R.string.before_after_state, (fraction * PERCENT).roundToInt())
    BoxWithConstraints(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.large)
                .pointerInput(Unit) {
                    fun update(x: Float) = currentOnFractionChange((x / size.width).coerceIn(0f, 1f))
                    detectHorizontalDragGestures(onDragStart = { update(it.x) }) { change, _ ->
                        change.consume()
                        update(change.position.x)
                    }
                }.semantics {
                    contentDescription = description
                    stateDescription = state
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    setProgress { target ->
                        currentOnFractionChange(target.coerceIn(0f, 1f))
                        true
                    }
                },
    ) {
        Box(Modifier.matchParentSize()) { after() }
        Box(
            Modifier
                .matchParentSize()
                .drawWithContent { clipRect(right = size.width * fraction) { this@drawWithContent.drawContent() } },
        ) { before() }
        SliderLabel(stringResource(R.string.before_after_before), Modifier.align(Alignment.TopStart))
        SliderLabel(stringResource(R.string.before_after_after), Modifier.align(Alignment.TopEnd))
        SplitHandle(offsetX = maxWidth * fraction, modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun SplitHandle(
    offsetX: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            Modifier
                .offset(x = offsetX - DividerWidth / 2)
                .width(DividerWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.inverseOnSurface),
        )
        Surface(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = offsetX - HandleSize / 2)
                    .size(HandleSize),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = SpaceSaverIcons.CompareHandle, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SliderLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(Spacing.Small),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
        )
    }
}

@PreviewComponents
@Composable
private fun BeforeAfterSliderPreview() {
    SpaceSaverTheme {
        BeforeAfterSlider(
            fraction = 0.4f,
            onFractionChange = {},
            before = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiaryContainer)) },
            after = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer)) },
            modifier = Modifier.size(width = 320.dp, height = 200.dp),
        )
    }
}
