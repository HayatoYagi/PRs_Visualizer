package io.github.hayatoyagi.prvisualizer.ui.explorer.badge

import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

enum class ExplorerStatusKind(
    val symbol: String,
    val legendLabel: String,
    val color: Color,
    val isConflict: Boolean = false,
) {
    Conflict("!", "Conf", AppColors.treemapConflictStripe, isConflict = true),
    Addition("+", "Add", AppColors.treemapAddition),
    Modification("~", "Mod", AppColors.treemapModification),
    Deletion("-", "Del", AppColors.treemapDeletion),
}
