package io.github.hayatoyagi.prvisualizer.ui.explorer.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun ExplorerStatusBadge(
    kind: ExplorerStatusKind,
    withLabel: Boolean,
    badgeSize: Dp,
    symbolFontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val textStyle = TextStyle(
        fontSize = symbolFontSize,
        fontWeight = if (kind.isConflict) FontWeight.ExtraBold else FontWeight.Bold,
        lineHeight = symbolFontSize,
    )
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .background(
                    color = kind.color.copy(alpha = if (kind.isConflict) 0.28f else 0.22f),
                    shape = if (kind.isConflict) RectangleShape else MaterialTheme.shapes.extraSmall,
                )
                .then(
                    if (kind.isConflict) Modifier.border(1.dp, kind.color, RectangleShape) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = kind.symbol,
                color = kind.color,
                style = textStyle,
                maxLines = 1,
            )
        }
        if (withLabel) {
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = kind.legendLabel,
                color = AppColors.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}
