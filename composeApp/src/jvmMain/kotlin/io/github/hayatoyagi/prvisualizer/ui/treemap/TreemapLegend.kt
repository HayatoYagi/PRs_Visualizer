package io.github.hayatoyagi.prvisualizer.ui.treemap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private const val LEGEND_MIN_ALPHA = 0.18f
private const val LEGEND_MAX_ALPHA = 0.96f
private const val LEGEND_BORDER_ALPHA = 0.5f
private const val LEGEND_STRIPE_WIDTH = 3f
private const val LEGEND_STRIPE_SPACING = 12f

@Composable
fun TreemapLegend(modifier: Modifier = Modifier) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { isExpanded = !isExpanded }
            .background(
                color = AppColors.tooltipBackground.copy(alpha = 0.9f),
                shape = RoundedCornerShape(4.dp),
            )
            .border(
                width = 1.dp,
                color = AppColors.tooltipBorder,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LegendHeader(isExpanded = isExpanded)
        if (isExpanded) {
            ConflictLegendItem()
            LegendItem(color = AppColors.treemapAddition, label = "Addition")
            LegendItem(color = AppColors.treemapModification, label = "Modification")
            LegendItem(color = AppColors.treemapDeletion, label = "Deletion")
        }
    }
}

@Composable
private fun LegendHeader(
    isExpanded: Boolean,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 2.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Legend",
            color = AppColors.textTooltip,
            fontSize = 11.sp,
        )
        Text(
            text = if (isExpanded) "▾" else "▸",
            color = AppColors.textSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(12.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            color.copy(alpha = LEGEND_MIN_ALPHA),
                            color.copy(alpha = LEGEND_MAX_ALPHA),
                        ),
                    ),
                    shape = RoundedCornerShape(2.dp),
                )
                .border(
                    width = 1.dp,
                    color = color.copy(alpha = LEGEND_BORDER_ALPHA),
                    shape = RoundedCornerShape(2.dp),
                ),
        )
        Text(
            text = label,
            color = AppColors.textTooltip,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ConflictLegendItem() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.Transparent, shape = RoundedCornerShape(2.dp))
                .border(1.dp, AppColors.treemapConflictStripe.copy(alpha = LEGEND_BORDER_ALPHA), RoundedCornerShape(2.dp)),
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = AppColors.treemapConflictStripe,
                        start = Offset(x, size.height),
                        end = Offset(x + size.height, 0f),
                        strokeWidth = LEGEND_STRIPE_WIDTH,
                        cap = StrokeCap.Square,
                    )
                    x += LEGEND_STRIPE_SPACING
                }
            }
        }
        Text(
            text = "Conflict",
            color = AppColors.textTooltip,
            fontSize = 11.sp,
        )
    }
}
