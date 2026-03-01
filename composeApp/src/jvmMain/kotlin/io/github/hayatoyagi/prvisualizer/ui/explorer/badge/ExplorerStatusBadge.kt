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
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private const val CONFLICT_BADGE_ALPHA = 0.28f
private const val NORMAL_BADGE_ALPHA = 0.22f
private val CONFLICT_BORDER_WIDTH = 1.dp

/**
 * Displays a status badge in the explorer view.
 *
 * @param kind The kind of status badge to display
 * @param withLabel Whether to show the legend label
 * @param size The size variant for the badge
 * @param modifier Modifier for the badge
 */
@Composable
fun ExplorerStatusBadge(
    kind: ExplorerStatusKind,
    withLabel: Boolean,
    size: ExplorerBadgeSize,
    modifier: Modifier = Modifier,
) {
    val textStyle = badgeTextStyle(kind = kind, size = size)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = badgeModifier(kind = kind, size = size),
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

private fun badgeTextStyle(
    kind: ExplorerStatusKind,
    size: ExplorerBadgeSize,
): TextStyle {
    val fontSize = if (kind.isConflict) size.conflictFontSp else size.fontSp
    return TextStyle(
        fontSize = fontSize,
        fontWeight = if (kind.isConflict) FontWeight.ExtraBold else FontWeight.Bold,
        lineHeight = fontSize,
    )
}

@Composable
private fun badgeModifier(
    kind: ExplorerStatusKind,
    size: ExplorerBadgeSize,
): Modifier {
    val shape = if (kind.isConflict) RectangleShape else MaterialTheme.shapes.extraSmall
    val alpha = if (kind.isConflict) CONFLICT_BADGE_ALPHA else NORMAL_BADGE_ALPHA
    val base = Modifier
        .size(size.badgeDp)
        .background(color = kind.color.copy(alpha = alpha), shape = shape)
    return if (kind.isConflict) base.border(CONFLICT_BORDER_WIDTH, kind.color, RectangleShape) else base
}
