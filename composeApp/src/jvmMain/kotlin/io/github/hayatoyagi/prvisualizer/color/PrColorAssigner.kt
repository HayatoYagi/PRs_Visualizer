package io.github.hayatoyagi.prvisualizer.color

import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import kotlin.random.Random

object PrColorAssigner {
    private const val RECENT_COLORS_TO_AVOID = 5

    fun nextColor(assignedMap: LinkedHashMap<String, Color>): Color {
        // Avoid the 5 most recently assigned colors (insertion order guaranteed by LinkedHashMap)
        val recentColors = assignedMap.values
            .toList()
            .takeLast(RECENT_COLORS_TO_AVOID)
            .toSet()
        val availableColors = AppColors.authorPalette.filter { it !in recentColors }
        return if (availableColors.isNotEmpty()) {
            availableColors[Random.nextInt(availableColors.size)]
        } else {
            AppColors.authorPalette[Random.nextInt(AppColors.authorPalette.size)]
        }
    }
}
