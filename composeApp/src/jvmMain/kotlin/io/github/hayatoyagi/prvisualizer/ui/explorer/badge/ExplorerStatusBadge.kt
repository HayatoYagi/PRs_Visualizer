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
import androidx.compose.ui.unit.sp
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

enum class ExplorerBadgeSize(
    val badgeDp: Dp,
    val fontSp: TextUnit,
    val conflictFontSp: TextUnit,
) {
    Legend(14.dp, 9.sp, 10.sp),
    Row(16.dp, 11.sp, 11.sp),
}

@Composable
fun ExplorerStatusBadge(
    kind: ExplorerStatusKind,
    withLabel: Boolean,
    size: ExplorerBadgeSize,
    modifier: Modifier = Modifier,
) {
    val fontSize = if (kind.isConflict) size.conflictFontSp else size.fontSp
    val textStyle = TextStyle(
        fontSize = fontSize,
        fontWeight = if (kind.isConflict) FontWeight.ExtraBold else FontWeight.Bold,
        lineHeight = fontSize,
    )
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(size.badgeDp)
                .background(
                    color = kind.color.copy(alpha = if (kind.isConflict) 0.28f else 0.22f),
                    shape = if (kind.isConflict) RectangleShape else MaterialTheme.shapes.extraSmall,
                ).then(
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
