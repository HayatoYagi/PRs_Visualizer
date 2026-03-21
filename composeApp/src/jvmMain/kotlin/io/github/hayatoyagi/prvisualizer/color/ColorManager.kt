package io.github.hayatoyagi.prvisualizer.color

import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.state.ColorState
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

/**
 * Manages PR color assignment and cycling logic.
 * Extracts color-related responsibilities from VisualizerViewModel.
 */
class ColorManager(
    private var colorState: ColorState = ColorState(),
    private val onStateChanged: (ColorState) -> Unit,
) {
    /**
     * Ensures that all PRs have assigned colors.
     * Only assigns colors to PRs that don't already have one.
     */
    fun ensurePrColors(prs: List<PullRequest>) {
        val prsNeedingColors = prs.filter { !colorState.prColorMap.containsKey(it.id) }
        if (prsNeedingColors.isNotEmpty()) {
            val newMap = LinkedHashMap(colorState.prColorMap)
            prsNeedingColors.forEach { pr ->
                newMap[pr.id] = PrColorAssigner.nextColor(newMap)
            }
            colorState = colorState.copy(prColorMap = newMap)
            onStateChanged(colorState)
        }
    }

    /**
     * Reassigns colors to all PRs, generating a new random color for each.
     */
    fun shufflePrColors(prs: List<PullRequest>) {
        val newMap = LinkedHashMap<String, Color>()
        prs.forEach { pr ->
            newMap[pr.id] = PrColorAssigner.nextColor(newMap)
        }
        colorState = colorState.copy(prColorMap = newMap)
        onStateChanged(colorState)
    }

    /**
     * Cycles the color for a specific PR to the next color in the palette.
     */
    fun cyclePrColor(prId: String) {
        val currentColor = colorState.prColorMap[prId]
        val currentIndex = if (currentColor != null) {
            AppColors.authorPalette.indexOf(currentColor)
        } else {
            -1
        }
        val nextIndex = (currentIndex + 1) % AppColors.authorPalette.size
        colorState = colorState.copy(
            prColorMap = colorState.prColorMap + (prId to AppColors.authorPalette[nextIndex]),
        )
        onStateChanged(colorState)
    }

    /**
     * Updates the internal color state. Called by ViewModel when state changes externally.
     */
    fun updateState(newColorState: ColorState) {
        colorState = newColorState
    }
}
