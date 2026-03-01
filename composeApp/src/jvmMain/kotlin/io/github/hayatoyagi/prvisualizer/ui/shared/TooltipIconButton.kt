package io.github.hayatoyagi.prvisualizer.ui.shared

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    tooltip: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val defaultProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()
    val density = LocalDensity.current
    val edgePaddingPx = with(density) { 8.dp.roundToPx() }
    val positionProvider = remember(defaultProvider, edgePaddingPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val pos = defaultProvider.calculatePosition(anchorBounds, windowSize, layoutDirection, popupContentSize)
                val minX = edgePaddingPx
                val maxX = (windowSize.width - popupContentSize.width - edgePaddingPx).coerceAtLeast(minX)
                val clampedX = pos.x.coerceIn(minX, maxX)
                return IntOffset(clampedX, pos.y)
            }
        }
    }

    TooltipBox(
        positionProvider = positionProvider,
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            enabled = enabled,
            onClick = onClick,
        ) {
            content()
        }
    }
}
