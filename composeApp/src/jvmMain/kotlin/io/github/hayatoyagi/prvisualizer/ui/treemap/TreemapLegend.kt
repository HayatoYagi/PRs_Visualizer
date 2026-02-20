package io.github.hayatoyagi.prvisualizer.ui.treemap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun TreemapLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = AppColors.tooltipBackground.copy(alpha = 0.9f),
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = AppColors.tooltipBorder,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LegendItem(color = AppColors.treemapAddition, label = "Addition")
        LegendItem(color = AppColors.treemapModification, label = "Modification")
        LegendItem(color = AppColors.treemapDeletion, label = "Deletion")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            color = AppColors.textTooltip,
            fontSize = 11.sp,
        )
    }
}
