package io.github.hayatoyagi.prvisualizer.color

import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import kotlin.random.Random

object PrColorAssigner {
    fun nextColor(assignedMap: Map<String, Color>): Color {
        // Avoid the 5 most recently assigned colors (map preserves insertion order)
        val recentColors = assignedMap.values
            .toList()
            .takeLast(5)
            .toSet()
        val availableColors = AppColors.authorPalette.filter { it !in recentColors }
        return if (availableColors.isNotEmpty()) {
            availableColors[Random.nextInt(availableColors.size)]
        } else {
            AppColors.authorPalette[Random.nextInt(AppColors.authorPalette.size)]
        }
    }
}
