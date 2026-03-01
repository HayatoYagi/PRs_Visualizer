package io.github.hayatoyagi.prvisualizer.ui.treemap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ui.shared.TooltipIconButton
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
internal fun TreemapZoomControlsOverlay(
    modifier: Modifier = Modifier,
    canZoomOut: Boolean,
    canZoomIn: Boolean,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = AppColors.backgroundHeader.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(8.dp),
                ),
        ) {
            TooltipIconButton(
                tooltip = "Zoom in",
                enabled = canZoomIn,
                onClick = onZoomIn,
            ) {
                Text("+", color = AppColors.textPrimary)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = AppColors.backgroundHeader.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(8.dp),
                ),
        ) {
            TooltipIconButton(
                tooltip = "Zoom out",
                enabled = canZoomOut,
                onClick = onZoomOut,
            ) {
                Text("−", color = AppColors.textPrimary)
            }
        }
    }
}
